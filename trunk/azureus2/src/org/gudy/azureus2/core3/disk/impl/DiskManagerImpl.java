/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Created on Oct 18, 2003
 * Created by Paul Gardner
 * Modified Apr 13, 2004 by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */

package org.gudy.azureus2.core3.disk.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.file.*;
import org.gudy.azureus2.core3.disk.impl.piecepicker.*;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.impl.resume.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.ReadRequestListener;

/**
 * 
 * The disk Wrapper.
 * 
 * @author Tdv_VgA
 *
 */
public class 
DiskManagerImpl
	implements DiskManagerHelper, FMFileOwner 
{  

	private String	dm_name	= "";
	private boolean started = false;
  
	private int state_set_via_method;
	private String errorMessage = "";

	private int pieceLength;
	private int lastPieceLength;

	//  private int[] _priorityPieces;

	ArrayList btFileList = new ArrayList();
	private byte[][] piecesHash;
	private int nbPieces;
	private long totalLength;
	private boolean pieceDone[];
	private int percentDone;
	private long allocated;

	private long remaining;

	private String path;
	private String fileName = "";
    
	private	TOTorrent		torrent;


	private DMReader				reader;
	private DMWriterAndChecker		writer_and_checker;
	
	private RDResumeHandler			resume_handler;
	private DMPiecePicker			piece_picker;
	
	
	private String rootPath = null;

	//The map that associate
	private PieceList[] pieceMap;

	private DiskManagerFileInfoImpl[] files;
    private DownloadManager dmanager;

    private PEPeerManager manager;

	private boolean alreadyMoved = false;

	// DiskManager listeners
	
	private static final int LDT_STATECHANGED		= 1;
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"DiskM:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DiskManagerListener	listener = (DiskManagerListener)_listener;
					
					if (type == LDT_STATECHANGED) {
					  int params[] = (int[])value;
  					listener.stateChanged(params[0], params[1]);
  				}
				}
			});		
	
	public 
	DiskManagerImpl(
		TOTorrent			_torrent, 
		String 				_path, 
		DownloadManager 	_dmanager) 
	{
    torrent 	= _torrent;
    path 		= _path;
    dmanager 	= _dmanager;
 
    setState( INITIALIZING );
    
    percentDone = 0;
    
	if (torrent == null) {
		setState( FAULTY );
		return;
	}
    
    //Insure that save folder exists
    /*
    File testPath = new File(path);
    if(testPath.isFile()) {
      testPath = testPath.getParentFile();
      if(!testPath.exists()) {
        this.errorMessage = MessageText.getString("DiskManager.saveNotFound");
        setState( FAULTY );
        return;
      }
    } else if(testPath.isDirectory()) {
      if(!testPath.exists()) {
        this.errorMessage = MessageText.getString("DiskManager.saveNotFound");
        setState( FAULTY );
        return;
      }
    }*/
   
		LocaleUtilDecoder	locale_decoder = null;
		try{
			locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
	
			fileName = "";
		
			File f = new File(path);
			
			if (f.isDirectory()) {
				fileName = locale_decoder.decodeString( torrent.getName());
			} else {
			  fileName = f.getName();
			  path = f.getParent();
			}
			
			fileName = FileUtil.convertOSSpecificChars( fileName );

			dm_name	= ByteFormatter.nicePrint(torrent.getHash(),true);
			
		}catch( TOTorrentException e ){
			this.errorMessage = TorrentUtils.exceptionToText(e) + " (Initialize1)";
			setState( FAULTY );
			return;
		}catch( UnsupportedEncodingException e ){
			this.errorMessage = e.getMessage() + " (Initialize1)";
			setState( FAULTY );
			return;
		}

		//build something to hold the filenames/sizes
		TOTorrentFile[] torrent_files = torrent.getFiles();

		if ( torrent.isSimpleTorrent()){
			
			totalLength = torrent_files[0].getLength();
  				
			rootPath = "";
			
			btFileList.add(new BtFile("", fileName, totalLength));
			
		} else {
			
			char separator = System.getProperty("file.separator").charAt(0);

				//get the root
			
			rootPath = fileName + separator;

				//:: Directory patch 08062003 - Tyler
				//	check for a user selecting the full path
			
			String fullPath = path + separator;
			
			int fullPathIndex = fullPath.lastIndexOf(rootPath);
			
			if (fullPathIndex >= 0 && fullPathIndex == (fullPath.length() - rootPath.length())) {
				
				rootPath = ""; //null out rootPath
			}

			buildFileLookupTables( torrent_files, btFileList, locale_decoder, separator);
			
			if (getState() == FAULTY){
			
				return;
			}
		}

		remaining 	= totalLength;

		piecesHash 	= torrent.getPieces();        

		nbPieces 	= piecesHash.length;
		
		pieceLength		 	= (int)torrent.getPieceLength();
		
		lastPieceLength  	= (int) (totalLength - ((long) (nbPieces - 1) * (long)pieceLength));
		
		reader 				= DMAccessFactory.createReader(this);
		writer_and_checker 	= DMAccessFactory.createWriterAndChecker(this,reader);
		
		resume_handler		= new RDResumeHandler( this, writer_and_checker );
	
		piece_picker		= DMPiecePickerFactory.create( this );
	}

	public void start() {
		if (started)
			return;

		started = true;

       
    Thread init = new AEThread("DiskManager:start") {
			public void run() {
				startSupport();
				if (DiskManagerImpl.this.getState() == DiskManager.FAULTY) {
					stopIt();
				}
			}
		};
		init.setPriority(Thread.MIN_PRIORITY);
		init.start();
	}

	private void startSupport() {
		//  create the pieces map
		pieceMap = new PieceList[nbPieces];

		pieceDone = new boolean[nbPieces];
		
		//if the data file is already in the completed files dir, we want to use it
		boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
		String completedDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
   
		if (moveWhenDone && completedDir.length() > 0) {
		  //if the data file already resides in the completed files dir
			
			String	path_copy = path;
			
			path = FileUtil.smartPath(completedDir, fileName);
			
			if (filesExist()){
				
				alreadyMoved = true;
				
			}else{
				
				path = path_copy;
		  }
		}


		writer_and_checker.start();
		
		reader.start();
		
		//allocate / check every file
		//fileArray = new RandomAccessFile[btFileList.size()];
		files = new DiskManagerFileInfoImpl[btFileList.size()];
      
		int newFiles = allocateFiles();
      
		if (getState() == FAULTY) return;
    
        path = FileUtil.smartPath(path, fileName);

		constructPieceMap(btFileList);

		constructFilesPieces();
		
		piece_picker.start();
		
		resume_handler.start();
		  
		if (newFiles == 0){
			
			resume_handler.checkAllPieces(false);
			
		}else if (newFiles != btFileList.size()){
			
				//	if not a fresh torrent, check pieces ignoring fast resume data
			
			resume_handler.checkAllPieces(true);
		}
		
			//3.Change State   
		
		setState( READY );
	}

	// no changes made here, just refactored the code out from initialize() - Moti
	private void constructPieceMap(ArrayList btFileList) {
		//for every piece, except the last one
		//add files to the piece list until we have built enough space to hold the piece
		//see how much space is available in the file
		//if the space available isnt 0
		//add the file to the piece->file mapping list
		//if there is enough space available, stop  

		//fix for 1 piece torrents
		if (totalLength < pieceLength) {
			pieceLength = (int)totalLength; //ok to convert
		}

		long fileOffset = 0;
		int currentFile = 0;
		for (int i = 0;(1 == nbPieces && i < nbPieces) || i < nbPieces - 1; i++) {
			ArrayList pieceToFileList = new ArrayList();
			int usedSpace = 0;
			while (pieceLength > usedSpace) {
				BtFile tempFile = (BtFile)btFileList.get(currentFile);
				long length = tempFile.getLength();

				//get the available space
				long availableSpace = length - fileOffset;

				PieceMapEntry tempPieceEntry = null;

				//how much space do we need to use?                               
				if (availableSpace < (pieceLength - usedSpace)) {
					//use the rest of the file's space
						tempPieceEntry =
							new PieceMapEntry(tempFile.getFileInfo(), fileOffset, (int)availableSpace //safe to convert here
	);

					//update the used space
					usedSpace += availableSpace;
					//update the file offset
					fileOffset = 0;
					//move the the next file
					currentFile++;
				} else //we don't need to use the whole file
					{
					tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, pieceLength - usedSpace);

					//update the file offset
					fileOffset += pieceLength - usedSpace;
					//udate the used space
					usedSpace += pieceLength - usedSpace;
				}

				//add the temp pieceEntry to the piece list
				pieceToFileList.add(tempPieceEntry);
			}

			//add the list to the map
			pieceMap[i] = PieceList.convert(pieceToFileList);
		}

		//take care of final piece if there was more than 1 piece in the torrent
		if (nbPieces > 1) {
			pieceMap[nbPieces - 1] =
				PieceList.convert(this.buildPieceToFileList(btFileList, currentFile, fileOffset, lastPieceLength));
		}
	}

	// refactored out of initialize() - Moti
	private void 
	buildFileLookupTables(
		TOTorrentFile[]	torrent_files, 
		ArrayList btFileList, 
		LocaleUtilDecoder locale_decoder, 
		final char separator) {
 
		 //for each file
         
		for (int i = 0; i < torrent_files.length; i++) {
        	
			long fileLength = buildFileLookupTable(torrent_files[i], btFileList, locale_decoder, separator);

			if (getState() == FAULTY)
				return;

			//increment the global length 
			totalLength += fileLength;
		}
	}

	/**
	 * Builds the path stored in fileDictionay, saving it in btFileList
	 * @param fileDictionay
	 * @param btFileList
	 * @param localeUtil
	 * @param separator
	 * @return the length of the file as stored in fileDictionay
	 */
	// refactored out of initialize() - Moti
	// code further refactored for readibility
	private long 
	buildFileLookupTable(
		TOTorrentFile		torrent_file, 
		ArrayList 			btFileList, 
		LocaleUtilDecoder 	locale_decoder, 
		final char 			separator) 
	{
		long fileLength  = torrent_file.getLength();

		//build the path
        
		byte[][]	path_components = torrent_file.getPathComponents();

		/* replaced the following two calls:
		StringBuffer pathBuffer = new StringBuffer(256);
		pathBuffer.setLength(0);
		*/
		StringBuffer pathBuffer = new StringBuffer(0);

	    try{

			int lastIndex = path_components.length - 1;
			for (int j = 0; j < lastIndex; j++) {
				//attach every element  
				
				String	comp = locale_decoder.decodeString( path_components[j]);
				
				comp = FileUtil.convertOSSpecificChars( comp );
				
				pathBuffer.append(comp);
				pathBuffer.append(separator);
			}
	
			//no, then we must be a part of the path
			//add the file entry to the file holder list      
			
			String	last_comp = locale_decoder.decodeString(path_components[lastIndex]);
			
			last_comp = FileUtil.convertOSSpecificChars( last_comp );
			
			btFileList.add(
				new BtFile(
					pathBuffer.toString(),
					last_comp,
					fileLength));
		}catch( UnsupportedEncodingException e ){
			this.errorMessage = e.getMessage() + " (buildFileLookupTable)";
			setState( FAULTY );
		}
 
		return fileLength;
	}

  
	private List buildPieceToFileList(List btFileList, int currentFile, long fileOffset, int pieceSize) {
		ArrayList pieceToFileList = new ArrayList();
		int usedSpace = 0;
		while (pieceSize > usedSpace) {
			BtFile tempFile = (BtFile)btFileList.get(currentFile);
			long length = tempFile.getLength();

			//get the available space
			long availableSpace = length - fileOffset;

			PieceMapEntry tempPieceEntry = null;

			//how much space do we need to use?                               
			if (availableSpace < (pieceLength - usedSpace)) {
				//use the rest of the file's space
				tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, (int)availableSpace);

				//update the used space
				usedSpace += availableSpace;
				//update the file offset
				fileOffset = 0;
				//move the the next file
				currentFile++;
			} else //we don't need to use the whole file
				{
				tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, pieceSize - usedSpace);

				//update the file offset
				fileOffset += pieceLength - usedSpace;
				//udate the used space
				usedSpace += pieceLength - usedSpace;
			}

			//add the temp pieceEntry to the piece list
			pieceToFileList.add(tempPieceEntry);
		}

		return pieceToFileList;
	}



	private static class BtFile {
		private DiskManagerFileInfoImpl _file;
		private String _path;
		private String _name;
		private String _originalName = null;
		private long _length;
		private static final String[] unsupportedChars = { "[\\/:?*]" };
		// 0 = Windows: \ / : ? * and any other Unicode letters ('?')

		public BtFile(String path, String name, long length) {
			_path = path;
			_length = length;
			_name = name;

			String newName = name.replace('"', '\'');

			if ( Constants.isWindows ){
				newName = newName.replaceAll(unsupportedChars[0], "_");
			}

			if (!name.equals(newName)) {
				_name = newName;
				_originalName = name;
			}

		}
		public long getLength() {
			return _length;
		}
		public String getPath() {
			return _path;
		}
		public boolean isNameOriginal() {
			return _originalName == null;
		}
		public String getOriginalName() {
			return _originalName == null ? _name : _originalName;
		}
		public String getName() {
			return _name;
		}
		public DiskManagerFileInfoImpl getFileInfo() {
			return _file;
		}
		public void setFileInfo(DiskManagerFileInfoImpl file) {
			_file = file;
		}
	}



	public boolean filesExist() {
	  if (rootPath == null) {
			this.errorMessage = "filesExist() called while rootPath null!";
			setState( FAULTY );
	    return false;
	  }
		//String basePath = path + System.getProperty("file.separator") + rootPath;
		
		// ok, we sometimes end up here with
		// path=d:\temp, rootPath=test\
		// for an original path of d:\temp\test
		// So we need to make sure that the basePath ends up back as
		// d:\temp\test\
		
		String	tempRoot 	= rootPath;
		
		if ( tempRoot.endsWith(File.separator)){
			
			tempRoot = tempRoot.substring(0,tempRoot.length()-1);
		}
		
		String basePath = FileUtil.smartFullName( path, tempRoot );
		
		if ( !basePath.endsWith(File.separator)){
			
			basePath += File.separator;
		}
		
		//System.out.println( "path=" + path + ", root=" + rootPath + ", base = " + basePath );
		
		for (int i = 0; i < btFileList.size(); i++) {
			//get the BtFile
			BtFile tempFile = (BtFile)btFileList.get(i);
			//get the path
			String tempPath = basePath + tempFile.getPath();
			//get file name
			String tempName = tempFile.getName();
			//System.out.println( "\ttempPath="+tempPath+",tempName="+tempName );
			//get file length
			long length = tempFile.getLength();

			File f = new File(tempPath, tempName);

			if (!f.exists()) {
			  errorMessage = tempPath + tempName + " not found.";
			  return false;
			}
			else if (f.length() != length) {
			  errorMessage = tempPath + tempName + " not correct size.";
				return false;
			}
		}
		return true;
	}
	
	private int allocateFiles() {
		setState( ALLOCATING );
		allocated = 0;
		int numNewFiles = 0;
		// String basePath = path + System.getProperty("file.separator") + rootPath;
		
		// ok, we sometimes end up here with
		// path=d:\temp, rootPath=test\
		// for an original path of d:\temp\test
		// So we need to make sure that the basePath ends up back as
		// d:\temp\test\
		
		String	tempRoot 	= rootPath;
		
		if ( tempRoot.endsWith(File.separator)){
			
			tempRoot = tempRoot.substring(0,tempRoot.length()-1);
		}
		
		String basePath = FileUtil.smartFullName( path, tempRoot );
		
		if ( !basePath.endsWith(File.separator)){
			
			basePath += File.separator;
		}
		
		for (int i = 0; i < btFileList.size(); i++) {
			//get the BtFile
			final BtFile tempFile = (BtFile)btFileList.get(i);
			//get the path
			final String tempPath = basePath + tempFile.getPath();
			//get file name
			final String tempName = tempFile.getName();
			//get file length
			final long length = tempFile.getLength();

			final File f = new File(tempPath, tempName);

			DiskManagerFileInfoImpl fileInfo;
			
			try{
				fileInfo = new DiskManagerFileInfoImpl( this, f );
				
			}catch ( FMFileManagerException e ){
				
				this.errorMessage = (e.getCause()!=null?e.getCause().getMessage():e.getMessage()) + " (allocateFiles:" + f.toString() + ")";
				
				setState( FAULTY );
				
				return -1;
			}
						
			int separator = tempName.lastIndexOf(".");
			
			if (separator == -1){
				separator = 0;
			}
			
			fileInfo.setExtension(tempName.substring(separator));
			
			//Added for Feature Request
			//[ 807483 ] Prioritize .nfo files in new torrents
			//Implemented a more general way of dealing with it.
			String extensions = COConfigurationManager.getStringParameter("priorityExtensions","");
			if(!extensions.equals("")) {
				boolean bIgnoreCase = COConfigurationManager.getBooleanParameter("priorityExtensionsIgnoreCase");
				StringTokenizer st = new StringTokenizer(extensions,";");
				while(st.hasMoreTokens()) {
					String extension = st.nextToken();
					extension = extension.trim();
					if(!extension.startsWith("."))
						extension = "." + extension;
					boolean bHighPriority = (bIgnoreCase) ? 
														  fileInfo.getExtension().equalsIgnoreCase(extension) : 
														  fileInfo.getExtension().equals(extension);
					if (bHighPriority)
						fileInfo.setPriority(true);
				}
			}
			
			fileInfo.setLength(length);
			fileInfo.setDownloaded(0);
			
      //do file allocation
			if( f.exists() ) {  //file already exists

        //make sure the existing file length isn't too large
        if( f.length() > length ) {
          this.errorMessage = "Existing data file length too large [" +f.length()+ ">" +length+ "]: " + f.getAbsolutePath();
          setState( FAULTY );
          return -1;
        }
        
			  try {
          fileInfo.setAccessMode( DiskManagerFileInfo.READ );
			  }
        catch (FMFileManagerException e) {
          this.errorMessage = (e.getCause() != null
              ? e.getCause().getMessage()
              : e.getMessage())
              + " (allocateFiles existing:" + f.toString() + ")";
          setState( FAULTY );
          return -1;
        }
        allocated += length;
      }
      else {  //we need to allocate it
        
        //make sure it hasn't previously been allocated
        if( dmanager.isDataAlreadyAllocated() ) {
          this.errorMessage = "Data file missing: " + f.getAbsolutePath();
          setState( FAULTY );
          return -1;
        }
        
        try {
          File directory = new File( tempPath );
          if( !directory.exists() ) {
            if( !directory.mkdirs() ) throw new Exception( "directory creation failed: " +directory);
          }
          f.getCanonicalPath();  //TEST: throws Exception if filename is not supported by os
          fileInfo.setAccessMode( DiskManagerFileInfo.WRITE );
          if( COConfigurationManager.getBooleanParameter("Enable incremental file creation") ) {
            //do incremental stuff
            fileInfo.getFMFile().setLength( 0 );
          }
          else {  //fully allocate
            if( COConfigurationManager.getBooleanParameter("Zero New") ) {  //zero fill
              if( !writer_and_checker.zeroFile( fileInfo, length ) ) {
                setState( FAULTY );
                return -1;
              }
            }
            else {  //reserve the full file size with the OS file system
              fileInfo.getFMFile().setLength( length );
              allocated += length;
            }
          }
        }
        catch ( Exception e ) {
          try {  fileInfo.getFMFile().close();  }
          catch (FMFileManagerException ex) {  ex.printStackTrace();  }
          this.errorMessage = (e.getCause() != null
              ? e.getCause().getMessage()
              : e.getMessage())
              + " (allocateFiles new:" + f.toString() + ")";
          setState( FAULTY );
          return -1;
        }
        numNewFiles++;
      }

			//add the file to the array
			files[i] = fileInfo;

			//setup this files RAF reference
			tempFile.setFileInfo(files[i]);
		}
    
    loadFilePriorities();
    
    dmanager.setDataAlreadyAllocated( true );
    
		return numNewFiles;
	}	
	
  
	public void 
	enqueueReadRequest( 
		DiskManagerRequest request, 
		ReadRequestListener listener ) 
	{
		reader.enqueueReadRequest( request, listener );
	}


	public int getNumberOfPieces() {
		return nbPieces;
	}

	public boolean[] getPiecesStatus() {
		return pieceDone;
	}

	public int getPercentDone() {
		return percentDone;
	}
	
	public void
	setPercentDone(
		int			num )
	{
		percentDone	= num;
	}
	
	public long getRemaining() {
		return remaining;
	}
	public void
	setRemaining(
		long		num )
	{
		remaining	= num;
	}
	
	public long
	getAllocated()
	{
		return( allocated );
	}
	
	public void
	setAllocated(
		long		num )
	{
		allocated	= num;
	}
	

	

	public int getPieceLength() {
		return pieceLength;
	}

	public long getTotalLength() {
		return totalLength;
	}

	public int getLastPieceLength() {
		return lastPieceLength;
	}

	public int getState() {
		return state_set_via_method;
	}

	public void
	setState(
		int		_state ) 
	{
		if ( state_set_via_method != _state ){
		  int params[] = {state_set_via_method, _state};
			state_set_via_method = _state;
			
			listeners.dispatch( LDT_STATECHANGED, params);
		}
	}
	
	public String getFileName() {
		return fileName;
	}


	public void setPeerManager(PEPeerManager manager) {
		this.manager = manager;
	}

	public void stopIt() 
	{
        		
    	writer_and_checker.stop();
    	
		reader.stop();
		
		resume_handler.stop();
		
		piece_picker.stop();
		
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				try {
					if (files[i] != null) {
						
						files[i].getFMFile().close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
 
  }


	public void computeFilesDone(int pieceNumber) {
		for (int i = 0; i < files.length; i++){
			
			DiskManagerFileInfoImpl	this_file = files[i];
			
			PieceList pieceList = pieceMap[pieceNumber];
			//for each piece

			for (int k = 0; k < pieceList.size(); k++) {
				//get the piece and the file 
				PieceMapEntry tempPiece = pieceList.get(k);
				
				if ( this_file == tempPiece.getFile()){
					
					long done = this_file.getDownloaded();
					
					done += tempPiece.getLength();
					
					this_file.setDownloaded(done);
					
					if (	done == this_file.getLength() &&
							this_file.getAccessMode() == DiskManagerFileInfo.WRITE){
						
					
						try{
							this_file.setAccessMode( DiskManagerFileInfo.READ );
							
						}catch (Exception e) {
							
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public String[][] getFilesStatus() {
		String[][] result = new String[files.length][2];
		for (int i = 0; i < result.length; i++) {
			
			DiskManagerFileInfoImpl	this_file = files[i];
			
			try {
        result[i][0] = this_file.getFile().getCanonicalPath();
      }
      catch (Exception e) { Debug.out("Unable to resolve canonical path for " + this_file.getName()); }

			result[i][1] = "";
			
			long length = this_file.getLength();
			
			long done = 0;
			
			for (int j = 0; j < nbPieces; j++) {
				if (!pieceDone[j])
					continue;
				//get the piece list
				PieceList pieceList = pieceMap[j];
				//for each piece

				for (int k = 0; k < pieceList.size(); k++) {
					
					//get the piece and the file
					
					PieceMapEntry tempPiece = pieceList.get(k);
					
					if (this_file == tempPiece.getFile()) {
						
						done += tempPiece.getLength();
					}
				}
			}
			int percent = 1000;
			if (length != 0)
				percent = (int) ((1000 * done) / length);
			result[i][1] = done + "/" + length + " : " + (percent / 10) + "." + (percent % 10) + " % ";
		}
		return result;
	}

	/**
	 * @return
	 */
	public DiskManagerFileInfo[] getFiles() {
		return files;
	}


	private void constructFilesPieces() {
		for (int i = 0; i < pieceMap.length; i++) {
			PieceList pieceList = pieceMap[i];
			//for each piece

			for (int j = 0; j < pieceList.size(); j++) {
				//get the piece and the file 
				DiskManagerFileInfoImpl fileInfo = (pieceList.get(j)).getFile();
				if (fileInfo.getFirstPieceNumber() == -1)
					fileInfo.setFirstPieceNumber(i);
				fileInfo.setNbPieces(fileInfo.getNbPieces() + 1);
			}
		}
	}

	/**
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	public void
	setErrorMessage(
		String	str )
	{
		errorMessage	= str;
	}
	
 
	/**
	 * @return
	 */
	
	public PieceList
	getPieceList(
		int		piece_number )
	{
		return( pieceMap[piece_number] );
	}
	
	public boolean[]
	getPiecesDone()
	{
		return( pieceDone );
	}
	
	public byte[]
	getPieceHash(
		int	piece_number )
	{
		return( piecesHash[ piece_number ]);
	}
	
	public PEPiece[] 
	getRecoveredPieces() 
	{
		return( resume_handler.getRecoveredPieces());
	}
	

	
	public DiskManagerRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( reader.createRequest( pieceNumber, offset, length ));
	}
	
  
	  public void 
	   aSyncCheckPiece(
	   		int pieceNumber) 
	  {
	  	writer_and_checker.aSyncCheckPiece( pieceNumber );
	  }
	  
	  public boolean isChecking() {
	    return ( writer_and_checker.isChecking());
	  }
	  
	 
		public void 
		writeBlock(
			int pieceNumber, 
			int offset, 
			DirectByteBuffer data,
			PEPeer sender)
		{
			writer_and_checker.writeBlock( pieceNumber, offset, data, sender );
		}
		
		public boolean 
		checkBlock(
			int pieceNumber, 
			int offset, 
			DirectByteBuffer data) 
		{
			return( writer_and_checker.checkBlock( pieceNumber, offset, data ));
		}
		
		public boolean 
		checkBlock(
			int pieceNumber, 
			int offset, 
			int length) 
		{
			return( writer_and_checker.checkBlock( pieceNumber, offset, length ));
		}
		
		public void 
		dumpResumeDataToDisk(
			boolean savePartialPieces, 
			boolean invalidate )
		{
			if ( resume_handler != null ){
				
				resume_handler.dumpResumeDataToDisk( savePartialPieces, invalidate );
			}
		}
		
  /**
   * Moves files to the CompletedFiles directory.
   * Returns a string path to the new torrent file.
   */
  public String moveCompletedFiles() {
    String fullPath;
    String subPath;
    String rPath = path;
    File destDir;
    String returnName = "";
    
    	// don't move non-persistent files as these aren't managed by us
    
    if (!dmanager.isPersistent()){
    	
    	return( returnName );
    }
    
    //make sure the torrent hasn't already been moved
    synchronized(this) {
      if (alreadyMoved) return returnName;
      alreadyMoved = true;
    }
    
    boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
    if (!moveWhenDone) return returnName;
    
    String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
    if (moveToDir.length() == 0) return returnName;

    try {
      //make sure the 'path' var isn't refering to multi-file torrent dir
      if (rPath.endsWith(fileName)) {
        File fTest = new File(rPath);
        if(fTest.isDirectory()) rPath = fTest.getParent();
      }
      
      boolean moveOnlyInDefault = COConfigurationManager.getBooleanParameter("Move Only When In Default Save Dir");
      if (moveOnlyInDefault) {
        String defSaveDir = COConfigurationManager.getStringParameter("Default save path");

        
        if (!rPath.equals(defSaveDir)) {
          LGLogger.log(LGLogger.INFORMATION, "Not moving-on-complete since data is not within default save dir");
          return returnName;
        }
      }
      
      	// first of all check that no destination files already exist
      
      File[]	new_files 	= new File[files.length];
      File[]	old_files	= new File[files.length];
      
      for (int i=0; i < files.length; i++) {
        synchronized (files[i]) {
          
          File old_file = files[i].getFile();
          
          old_files[i]	= old_file;
          
          //get old file's parent path
          fullPath = old_file.getParent();
          
           //compute the file's sub-path off from the default save path
          subPath = fullPath.substring(fullPath.indexOf(rPath) + rPath.length());
    
          //create the destination dir
          destDir = new File(moveToDir + subPath);
     
          destDir.mkdirs();
          
          //create the destination file pointer
          File newFile = new File(destDir, old_file.getName());

          new_files[i]	= newFile;
          
          if (newFile.exists()) {
          	
            String msg = "" + old_file.getName() + " already exists in MoveTo destination dir";
            
            LGLogger.log(LGLogger.ERROR,msg);
            
            LGLogger.logAlertUsingResource( 
            		LGLogger.AT_ERROR, "DiskManager.alert.movefileexists", 
            		new String[]{ old_file.getName() } );
            
            Debug.out(msg);
            
            return returnName;
          }
        }
      }
      
      for (int i=0; i < files.length; i++){
      	
        synchronized (files[i]) {          
 
          File old_file = files[i].getFile();

          File new_file = new_files[i];
          
          try{
          	
          	files[i].moveFile( new_file );
           	
            files[i].setAccessMode(DiskManagerFileInfo.READ);
            
          }catch( FMFileManagerException e ){
          	
            String msg = "Failed to move " + old_file.getName() + " to destination dir";
            
            LGLogger.log(LGLogger.ERROR,msg);
            
            LGLogger.logAlertUsingResource( 
            		LGLogger.AT_ERROR, "DiskManager.alert.movefilefails", 
            		new String[]{ old_file.getName(),
            		e.getCause()==null?e.getMessage():e.getCause().getMessage()} );

            Debug.out(msg);
            
            	// try some recovery by moving any moved files back...
            
            for (int j=0;j<i;j++){
            
            	try{
            		files[j].moveFile( old_files[j]);

            		files[j].setAccessMode(DiskManagerFileInfo.READ);
         		
            	}catch( FMFileManagerException f ){
              
            		LGLogger.logAlertUsingResource( 
                    		LGLogger.AT_ERROR, "DiskManager.alert.movefilerecoveryfails", 
                    		new String[]{ files[j].toString(),
                    		f.getCause()==null?f.getMessage():f.getCause().getMessage()} );
           		
            	}
            }
            
            return returnName;
          }
        }
      }
      
      //remove the old dir
      File tFile = new File(rPath, fileName);
      
      if (	tFile.isDirectory() && 
      		!moveToDir.equals(rPath)){
      	
      	deleteDataFiles(torrent, tFile.toString());
      }

      
      //update internal path
      path = FileUtil.smartPath(moveToDir, fileName);
      
      //move the torrent file as well
      boolean moveTorrent = COConfigurationManager.getBooleanParameter("Move Torrent When Done", true);
      if (moveTorrent) {
        synchronized (torrent) {
          String oldFullName = torrent.getAdditionalStringProperty("torrent filename");
          File oldTorrentFile = new File(oldFullName);
          String oldFileName = oldTorrentFile.getName();
          File newTorrentFile = new File(moveToDir, oldFileName);
          if (!newTorrentFile.equals(oldTorrentFile)) {
            //save torrent to new file
            torrent.serialiseToBEncodedFile(newTorrentFile);
            //remove old torrent file
            oldTorrentFile.delete();
            //update torrent meta-info to point to new torrent file
            torrent.setAdditionalStringProperty("torrent filename", newTorrentFile.getCanonicalPath());
            returnName = newTorrentFile.getCanonicalPath();
          }
        }
      }
    } catch (Exception e) { e.printStackTrace(); }

    return returnName;
  }
   
    
 
  public String
  getName()
  {
  	return( dm_name );
  }
  
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}


  public void
  addListener(
  	DiskManagerListener	l )
  {
 	listeners.addListener( l );

	  int params[] = {getState(), getState()};
  		
  	listeners.dispatch( l, LDT_STATECHANGED, params);
  }
  
  public void
  removeListener(
  	DiskManagerListener	l )
  {
  	listeners.removeListener(l);
  }

  /** Deletes all data files associated with torrent.
   * Currently, deletes all files, then tries to delete the path recursively
   * if the paths are empty.  An unexpected result may be that a empty
   * directory that the user created will be removed.
   *
   * TODO: only remove empty directories that are created for the torrent
   */
  
	public static void 
	deleteDataFiles(
		TOTorrent torrent, 
		String sPath) 
	{
		if (torrent == null){
	  
			return;
		}
	  	  
		try{
			LocaleUtilDecoder locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );

			TOTorrentFile[] files = torrent.getFiles();

				// delete all files, then empty directories

			for (int i=0;i<files.length;i++){
			
				byte[][]path_comps = files[i].getPathComponents();
			
				String	path_str = sPath + File.separator;
				
				for (int j=0;j<path_comps.length;j++){
					
					try{
						
						String comp = locale_decoder.decodeString( path_comps[j] );
						
						comp = FileUtil.convertOSSpecificChars( comp );
						
						path_str += (j==0?"":File.separator) + comp;
					
					}catch( UnsupportedEncodingException e ){
						System.out.println( "file - unsupported encoding!!!!");	
					}
				}
			
				File file = new File(path_str);
				
				if (file.exists() && !file.isDirectory()){
			  
					try{
						file.delete();
					}catch (Exception e){
						
						Debug.out(e.toString());
					}
				}
			}
		
			if (!torrent.isSimpleTorrent()){
				
				File fPath = new File(sPath);
				
				FileUtil.recursiveEmptyDirDelete(fPath);
			}
		}catch( Throwable e ){
		
			e.printStackTrace();
		}
	}
  
    
  
  private void loadFilePriorities() {
  	//  TODO: remove this try/catch.  should only be needed for those upgrading from previous snapshot
    try {
    	if ( files == null ) return;
    	List file_priorities = (List)dmanager.getData( "file_priorities" );
    	if ( file_priorities == null ) return;
    	for (int i=0; i < files.length; i++) {
    		DiskManagerFileInfo file = files[i];
    		if (file == null) return;
    		int priority = ((Long)file_priorities.get( i )).intValue();
    		if ( priority == 0 ) file.setSkipped( true );
    		else if (priority == 1) file.setPriority( true );
    	}
    }
    catch (Throwable t) {t.printStackTrace();}
  }
  
  
  public void storeFilePriorities() {
    if ( files == null ) return;
    List file_priorities = new ArrayList();
    for (int i=0; i < files.length; i++) {
      DiskManagerFileInfo file = files[i];
      if (file == null) return;
      boolean skipped = file.isSkipped();
      boolean priority = file.isPriority();
      int value = -1;
      if ( skipped ) value = 0;
      else if ( priority ) value = 1;
      file_priorities.add( i, new Long(value));            
    }
    dmanager.setData( "file_priorities", file_priorities );
  }
  
  public DownloadManager getDownloadManager() {
    return dmanager;
  }
  
  public PEPeerManager getPeerManager() {
    return manager;
  }
  
  	public void 
	computePriorityIndicator()
  	{
  		piece_picker.computePriorityIndicator();
  	}
  	
	public int 
	getPieceNumberToDownload(
		boolean[] _piecesRarest)
	{
		return( piece_picker.getPiecenumberToDownload( _piecesRarest ));
	}
}