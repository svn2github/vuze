/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
import java.util.LinkedList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.file.*;
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
	implements DiskManager, ParameterListener, FMFileOwner 
{  
	private static Semaphore	global_write_queue_block_sem;
	private static Semaphore	global_check_queue_block_sem;
	
	static{
		int	write_limit_blocks = COConfigurationManager.getIntParameter("DiskManager Write Queue Block Limit", 0);

		global_write_queue_block_sem = new Semaphore(write_limit_blocks);
		
		if ( write_limit_blocks == 0 ){
			
			global_write_queue_block_sem.releaseForever();
		}
		
		int	check_limit_pieces = COConfigurationManager.getIntParameter("DiskManager Check Queue Piece Limit", 0);

		global_check_queue_block_sem = new Semaphore(check_limit_pieces);
		
		if ( check_limit_pieces == 0 ){
			
			global_check_queue_block_sem.releaseForever();
		}
		
		// System.out.println( "global writes = " + write_limit_blocks + ", global checks = " + check_limit_pieces );
	}

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

	private DirectByteBuffer allocateAndTestBuffer;
	
	private List 		writeQueue;
	private List 		checkQueue;
	private Semaphore	writeCheckQueueSem;
	private Object		writeCheckQueueLock	= new Object();
	
	private List		readQueue;
	private Semaphore	readQueueSem;

	private DiskWriteThread writeThread;
	private DiskReadThread readThread;

	private String rootPath = null;

	//The map that associate
	private PieceList[] pieceMap;
	private int pieceCompletion[];
	private BitSet[] priorityLists;
	//private int[][] priorityLists;

	private DiskManagerFileInfoImpl[] files;
  private DownloadManager dmanager;

  private PEPeerManager manager;
	private SHA1Hasher hasher;
  private Md5Hasher md5;
  private DirectByteBuffer md5Result;
	private boolean bOverallContinue = true;
	private PEPiece[] pieces;
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
	
  private static boolean useFastResume = COConfigurationManager.getBooleanParameter("Use Resume", true);
  private static boolean firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
  
	public DiskManagerImpl(TOTorrent	_torrent, String path, DownloadManager dmanager) {
    setState( INITIALIZING );
    this.percentDone = 0;
    this.torrent = _torrent;
    this.path = path;
    this.dmanager = dmanager;
    initialize1();
	}

	public void start() {
		if (started)
			return;

		started = true;
		md5 = new Md5Hasher();
    md5Result = DirectByteBufferPool.getBuffer( 16 );
    
		hasher = new SHA1Hasher();
    
    // add configuration parameter listeners
    COConfigurationManager.addParameterListener("Use Resume", this);
    COConfigurationManager.addParameterListener("Prioritize First Piece", this);
    
    Thread init = new AEThread("DiskManager:start") {
			public void run() {
				initialize();
				if (DiskManagerImpl.this.getState() == DiskManager.FAULTY) {
					stopIt();
				}
			}
		};
		init.setPriority(Thread.MIN_PRIORITY);
		init.start();
	}

	private void initialize1() {
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
  		remaining = totalLength;
			rootPath = "";
			btFileList.add(new BtFile("", fileName, totalLength));
		} else {
			final char separator = System.getProperty("file.separator").charAt(0);

			//get the root
			rootPath = fileName + separator;

			//:: Directory patch 08062003 - Tyler
			//check for a user selecting the full path
			String fullPath = path + separator;
			int fullPathIndex = fullPath.lastIndexOf(rootPath);
			if (fullPathIndex >= 0 && fullPathIndex == (fullPath.length() - rootPath.length())) {
				rootPath = ""; //null out rootPath
			}

			buildFileLookupTables( torrent_files, btFileList, locale_decoder, separator);

  		remaining = totalLength;
			if (getState() == FAULTY)
				return;
		}
	}

	private void initialize() {
		pieceLength = (int)torrent.getPieceLength();

		piecesHash = torrent.getPieces();
        
		nbPieces = piecesHash.length;

		//  create the pieces map
		pieceMap = new PieceList[nbPieces];
		pieceCompletion = new int[nbPieces];
		priorityLists = new BitSet[100];
		//    priorityLists = new int[10][nbPieces + 1];

		// the piece numbers for getPiecenumberToDownload
		//    _priorityPieces = new int[nbPieces + 1];

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

		//Create the ByteBuffer for checking (size : pieceLength)
        allocateAndTestBuffer = DirectByteBufferPool.getBuffer(pieceLength);
    
		allocateAndTestBuffer.limit(pieceLength);
		for (int i = 0; i < allocateAndTestBuffer.limit(); i++) {
			allocateAndTestBuffer.put((byte)0);
		}
		allocateAndTestBuffer.position(0);

		//Create the new Queue
		
		writeQueue 			= new LinkedList();
		checkQueue 			= new LinkedList();
		writeCheckQueueSem	= new Semaphore();
		
		//check_queue_block_sem	= new Semaphore();
		
		readQueue 		= new LinkedList();
		readQueueSem	= new Semaphore();
		
		writeThread = new DiskWriteThread();
		writeThread.start();
		readThread = new DiskReadThread();
		readThread.start();

		lastPieceLength = (int) (totalLength - ((long) (nbPieces - 1) * (long)pieceLength));

		//allocate / check every file
		//fileArray = new RandomAccessFile[btFileList.size()];
		files = new DiskManagerFileInfoImpl[btFileList.size()];
      
		int newFiles = this.allocateFiles();
      
		if (getState() == FAULTY) return;
    
        path = FileUtil.smartPath(path, fileName);

		constructPieceMap(btFileList);

		constructFilesPieces();

		//check all pieces if no new files were created
		if (newFiles == 0) checkAllPieces(false);
		//if not a fresh torrent, check pieces ignoring fast resume data
		else if (newFiles != btFileList.size()) checkAllPieces(true);
    
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

	private static class FlyWeightInteger {
    private static Integer[] array = new Integer[1024];

    final static Integer getInteger(final int value) {
      Integer tmp = null;
      
      if (value >= array.length) {
        Integer[] arrayNew = new Integer[value + 256];
        System.arraycopy(array, 0, arrayNew, 0, array.length);
        array = arrayNew;
      }
      else {
        tmp = array[value];
      }
      
      if (tmp == null) {
        tmp = new Integer(value);
        array[value] = tmp;
      }
      
      return tmp;
    }
	}
  
  
  private static class DiskReadRequest {
    private final DiskManagerRequest request;
    private final ReadRequestListener listener;
    
    private DiskReadRequest( DiskManagerRequest r, ReadRequestListener l ) {
      this.request = r;
      this.listener = l;
    }
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

	public class QueueElement {
		private int pieceNumber;
		private int offset;
		private DirectByteBuffer data;
    private PEPeer sender; 

		public QueueElement(int pieceNumber, int offset, DirectByteBuffer data, PEPeer sender) {
			this.pieceNumber = pieceNumber;
			this.offset = offset;
			this.data = data;
      this.sender = sender;
		}  

		public int getPieceNumber() {
			return this.pieceNumber;
		}

		public int getOffset() {
			return this.offset;
		}

		public DirectByteBuffer getData() {
			return this.data;
		}
    
    public PEPeer getSender() {
      return this.sender;
	}
	}

	public class DiskReadThread extends Thread {
		private boolean bReadContinue = true;

		public DiskReadThread() {
			super("Disk Reader");
			setDaemon(true);
		}

		public void run() {
			
			while (bReadContinue){	
		
				try{
					int	entry_count = readQueueSem.reserveSet( 10 );
					
					for (int i=0;i<entry_count;i++){
						
            DiskReadRequest drr;
						
						synchronized( readQueue ){
							
							if ( !bReadContinue){
														
								break;
							}
						
							drr = (DiskReadRequest)readQueue.remove(0);
						}
		
						DiskManagerRequest request = drr.request;
		
						DirectByteBuffer buffer = readBlock(request.getPieceNumber(), request.getOffset(), request.getLength());
            
						if (buffer != null) {
              drr.listener.readCompleted( request, buffer );              
						}
            else {
              String err_msg = "Failed loading piece " +request.getPieceNumber()+ ":" +request.getOffset()+ "->" +(request.getOffset() + request.getLength());
						  LGLogger.log( LGLogger.ERROR, err_msg );
						  System.out.println( err_msg );
						}
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					Debug.out( "DiskReadThread: error occurred during processing: " + e.toString());
				}
			}
		}

		public void stopIt() {
			synchronized( readQueue ){
				
				bReadContinue = false;
			}
			
			readQueueSem.releaseForever();
						
			while (readQueue.size() != 0) {
        readQueue.remove(0);
			}
		}
	}

	public class DiskWriteThread extends Thread {
		private boolean bWriteContinue = true;

		public DiskWriteThread() {
			super("Disk Writer & Checker");
			setDaemon(true);
		}

		public void run() 
		{
			while (bWriteContinue){
				
				try{
					int	entry_count = writeCheckQueueSem.reserveSet( 64 );
					
					for (int i=0;i<entry_count;i++){
						
						QueueElement	elt;
						boolean			elt_is_write;
						
						synchronized( writeCheckQueueLock ){
							
							if ( !bWriteContinue){
															
								break;
							}
							
							if ( writeQueue.size() > checkQueue.size()){
								
								elt	= (QueueElement)writeQueue.remove(0);
								
								// System.out.println( "releasing global write slot" );
	
								global_write_queue_block_sem.release();
								
								elt_is_write	= true;
								
							}else{
								
								elt	= (QueueElement)checkQueue.remove(0);
								
								global_check_queue_block_sem.release();
														
								elt_is_write	= false;
							}
						}
		
						if ( elt_is_write ){
							
								//Do not allow to write in a piece marked as done.
							
							int pieceNumber = elt.getPieceNumber();
							
							if(!pieceDone[pieceNumber]){
								
							  if ( dumpBlockToDisk(elt)){
							  
							  	manager.blockWritten(elt.getPieceNumber(), elt.getOffset(),elt.getSender());
							  	
							  }else{
							  	
							  		// could try and recover if, say, disk full. however, not really
							  		// worth the effort as user intervention is no doubt required to
							  		// fix the problem 
							  	
								elt.data.returnToPool();
										
								elt.data = null;
								  
								stopIt();
								
								setState( FAULTY );
								
							  }
							  
							}else{
		  
								elt.data.returnToPool();
								
							  elt.data = null;
							}
							
						}else{
							
						  boolean correct = checkPiece(elt.getPieceNumber());

						  if(!correct){
						  	
						    MD5CheckPiece(elt.getPieceNumber(),false);
						    
						    LGLogger.log(0, 0, LGLogger.ERROR, "Piece " + elt.getPieceNumber() + " failed hash check.");
						    
						  }else{
						  	
						    LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece " + elt.getPieceNumber() + " passed hash check.");
						    
						    if(manager.needsMD5CheckOnCompletion(elt.getPieceNumber())){
						    	
						      MD5CheckPiece(elt.getPieceNumber(),true);
						    }
						  }
		
						  manager.asyncPieceChecked(elt.getPieceNumber(), correct);
					  }
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					Debug.out( "DiskWriteThread: error occurred during processing: " + e.toString());
				}
        
			}
		}

		public void stopIt(){
			
			synchronized( writeCheckQueueLock ){
				
				bWriteContinue = false;
			}
			
			writeCheckQueueSem.releaseForever();
			
			while (writeQueue.size() != 0){
				
				// System.out.println( "releasing global write slot (tidy up)" );

				global_write_queue_block_sem.release();
				
				QueueElement elt = (QueueElement)writeQueue.remove(0);
				
				elt.data.returnToPool();
				
				elt.data = null;
			}
			
			while (checkQueue.size() != 0){
				
				// System.out.println( "releasing global write slot (tidy up)" );

				global_check_queue_block_sem.release();
				
				QueueElement elt = (QueueElement)checkQueue.remove(0);
			}
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
              if( !zeroFile( fileInfo, length ) ) {
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


  private boolean zeroFile( DiskManagerFileInfoImpl file, long length ) {
    FMFile	fm_file = file.getFMFile();
		long written = 0;
		synchronized (file){
		  try{
		    if( length == 0 ) { //create a zero-length file if it is listed in the torrent
          fm_file.setLength( 0 );
        }
        else {
          while (written < length && bOverallContinue) {
            allocateAndTestBuffer.limit(allocateAndTestBuffer.capacity());
            if ((length - written) < allocateAndTestBuffer.remaining())
              allocateAndTestBuffer.limit((int) (length - written));
             int deltaWriten = fm_file.write(allocateAndTestBuffer, written);
             allocateAndTestBuffer.position(0);
             written += deltaWriten;
             allocated += deltaWriten;
             percentDone = (int) ((allocated * 1000) / totalLength);
          }
        }
				if (!bOverallContinue) {
				   fm_file.close();
				   return false;
				}
		  } catch (Exception e) {  e.printStackTrace();  }
		}
		return true;
	}
  

   public void 
   aSyncCheckPiece(
   		int pieceNumber) 
   {  		
   		global_check_queue_block_sem.reserve();
   		
  		synchronized( writeCheckQueueLock ){
 		
   			checkQueue.add(new QueueElement(pieceNumber, 0, null, null));
    	}
   		
   		writeCheckQueueSem.release();
   }
  

	private synchronized boolean checkPiece(int pieceNumber) {
    
    if( COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" ) ) {
      try{  Thread.sleep( 100 );  }catch(Exception e) { e.printStackTrace(); }
    }
           
    if (bOverallContinue == false) return false;

		allocateAndTestBuffer.position(0);

		int length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

		allocateAndTestBuffer.limit(length);

		//get the piece list
		PieceList pieceList = pieceMap[pieceNumber];

		//for each piece
		for (int i = 0; i < pieceList.size(); i++) {
			//get the piece and the file 
			PieceMapEntry tempPiece = pieceList.get(i);
            

			try {
                    
					   //if the file is large enough
				if ( tempPiece.getFile().getFMFile().getSize() >= tempPiece.getOffset()){
					
					tempPiece.getFile().getFMFile().read(allocateAndTestBuffer, tempPiece.getOffset());
					
				}else{
						   //too small, can't be a complete piece
					
					allocateAndTestBuffer.clear();
					
					pieceDone[pieceNumber] = false;
					
					return false;
				}
			}catch (Exception e){
				
				e.printStackTrace();
			}
		}

		try {
      
      if (bOverallContinue == false) return false;
      
			allocateAndTestBuffer.position(0);

			byte[] testHash = hasher.calculateHash(allocateAndTestBuffer.getBuffer());
			int i = 0;
			for (i = 0; i < 20; i++) {
				if (testHash[i] != piecesHash[pieceNumber][i])
					break;
			}
			if (i >= 20) {
				//mark the piece as done..
				if (!pieceDone[pieceNumber]) {
					pieceDone[pieceNumber] = true;
					remaining -= length;
					computeFilesDone(pieceNumber);
				}
				return true;
			}
			if(pieceDone[pieceNumber]) {
			  pieceDone[pieceNumber] = false;
			  remaining += length;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

		// RESUME DATA STUFF STARTS.....
	
	private void checkAllPieces(boolean newfiles) {
		setState( CHECKING );
		int startPos = 0;
		
		boolean resumeEnabled = useFastResume;
		//disable fast resume if a new file was created
		if (newfiles) resumeEnabled = false;
		
		boolean	resume_data_complete = false;
		
		if (resumeEnabled) {
			boolean resumeValid = false;
			byte[] resumeArray = null;
			Map partialPieces = null;
			Map resumeMap = torrent.getAdditionalMapProperty("resume");
			
			if (resumeMap != null) {
				
				// see bug 869749 for explanation of this mangling
				
				/*
				System.out.println( "Resume map");
				
				Iterator it = resumeMap.keySet().iterator();
				
				while( it.hasNext()){
					
					System.out.println( "\tmap:" + ByteFormatter.nicePrint((String)it.next()));
				}
				*/
				
				String mangled_path;
				
				try{
					mangled_path = new String(path.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
					
					// System.out.println( "resume: path = " + ByteFormatter.nicePrint(path )+ ", mangled_path = " + ByteFormatter.nicePrint(mangled_path));
					
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					mangled_path = this.path;
				}
				
				Map resumeDirectory = (Map)resumeMap.get(mangled_path);
				
				if ( resumeDirectory == null ){
					
						// unfortunately, if the torrent hasn't been saved and restored then the
						// mangling with not yet have taken place. So we have to also try the 
						// original key (see 878015)
					
					resumeDirectory = (Map)resumeMap.get(path);
				}
				
				if ( resumeDirectory != null ){
					
					try {
						
						resumeArray = (byte[])resumeDirectory.get("resume data");
						partialPieces = (Map)resumeDirectory.get("blocks");
						resumeValid = ((Long)resumeDirectory.get("valid")).intValue() == 1;
						
							// if the torrent download is complete we don't need to invalidate the
							// resume data
						
						if ( isTorrentResumeDataComplete( torrent, path )){
							
							resume_data_complete	= true;
									
						}else{
							
							resumeDirectory.put("valid", new Long(0));
							
							saveTorrent();
						}
						
					}catch(Exception ignore){
						/* ignore */ 
					}
					
				}else{
					
					// System.out.println( "resume dir not found");
				}
			}
			
			if (resumeEnabled && (resumeArray != null) && (resumeArray.length <= pieceDone.length)) {
				startPos = resumeArray.length;
				for (int i = 0; i < resumeArray.length && bOverallContinue; i++) { //parse the array
					percentDone = ((i + 1) * 1000) / nbPieces;
					//mark the pieces
					if (resumeArray[i] == 0) {
						if (!resumeValid) pieceDone[i] = checkPiece(i);
					}
					else {
						computeFilesDone(i);
						pieceDone[i] = true;
						if (i < nbPieces - 1) {
							remaining -= pieceLength;
						}
						if (i == nbPieces - 1) {
							remaining -= lastPieceLength;
						}
					}
				}
				
				if (partialPieces != null && resumeValid) {
					pieces = new PEPiece[nbPieces];
					Iterator iter = partialPieces.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry key = (Map.Entry)iter.next();
						int pieceNumber = Integer.parseInt((String)key.getKey());
						PEPiece piece;
						if (pieceNumber < nbPieces - 1)
							piece = PEPieceFactory.create(manager, getPieceLength(), pieceNumber);
						else
							piece = PEPieceFactory.create(manager, getLastPieceLength(), pieceNumber);
						List blocks = (List)partialPieces.get(key.getKey());
						Iterator iterBlock = blocks.iterator();
						while (iterBlock.hasNext()) {
							piece.setWritten(null,((Long)iterBlock.next()).intValue());
						}
						pieces[pieceNumber] = piece;
					}
				}
			}
		}
		
		for (int i = startPos; i < nbPieces && bOverallContinue; i++) {
			percentDone = ((i + 1) * 1000) / nbPieces;
			checkPiece(i);
		}
		
			//dump the newly built resume data to the disk/torrent
		
		if (bOverallContinue && resumeEnabled && !resume_data_complete){
			
			dumpResumeDataToDisk(false, false);
		}
	}
	
	public void 
	dumpResumeDataToDisk(
		boolean savePartialPieces, 
		boolean invalidate )
	{
		if(!useFastResume)
		  return;
    
		boolean	was_complete = isTorrentResumeDataComplete( torrent, path );
		
		//build the piece byte[] 
		byte[] resumeData = new byte[pieceDone.length];
		for (int i = 0; i < resumeData.length; i++) {
		  if (invalidate) resumeData[i] = (byte)0;
		  else resumeData[i] = pieceDone[i] ? (byte)1 : (byte)0;
		}

		//Attach the resume data
		Map resumeMap = new HashMap();
		torrent.setAdditionalMapProperty("resume", resumeMap);

	  Map resumeDirectory = new HashMap();
	  
	  	// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
	  	// currently fixed by mangling such that decode works
	  
	  // System.out.println( "writing resume data: key = " + ByteFormatter.nicePrint(path));
	  
	  resumeMap.put(path, resumeDirectory);
	  
	  resumeDirectory.put("resume data", resumeData);
	  Map partialPieces = new HashMap();
	
	  if (savePartialPieces  && !invalidate) {
	    if (pieces == null && manager != null)
			pieces = manager.getPieces();
	    if(pieces != null) {
	      for (int i = 0; i < pieces.length; i++) {
	        PEPiece piece = pieces[i];
	        if (piece != null && piece.getCompleted() > 0) {
	          boolean[] downloaded = piece.getWritten();
	          List blocks = new ArrayList();
	          for (int j = 0; j < downloaded.length; j++) {
	            if (downloaded[j])
	              blocks.add(new Long(j));
	          }
	          partialPieces.put("" + i, blocks);
	        }
	      }
	      resumeDirectory.put("blocks", partialPieces);
	    }
	    resumeDirectory.put("valid", new Long(1));
	  } else {
	    resumeDirectory.put("valid", new Long(0));
	  }
		
	  boolean	is_complete = isTorrentResumeDataComplete( torrent, path );
	  
	  if ( was_complete && is_complete ){
	 
	  		// no change, no point in writing
	  		  	
	  }else{
	  	
	  	saveTorrent();
	  }
	}

	public static void
	setTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		data_dir )
	{
		int	piece_count = torrent.getPieces().length;
		
		byte[] resumeData = new byte[piece_count];
		
		for (int i = 0; i < resumeData.length; i++) {
			
			resumeData[i] = (byte)1;
		}

		Map resumeMap = new HashMap();
		
		torrent.setAdditionalMapProperty("resume", resumeMap);

		Map resumeDirectory = new HashMap();
		
		// We *really* shouldn't be using a localised string as a Map key (see bug 869749)
		// currently fixed by mangling such that decode works
		
		resumeMap.put(data_dir, resumeDirectory);
		
		resumeDirectory.put("resume data", resumeData);
		
		Map partialPieces = new HashMap();
		
		resumeDirectory.put("blocks", partialPieces);
		
		resumeDirectory.put("valid", new Long(1));	
	}
	
	public static boolean
	isTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		data_dir )
	{
		try{
			int	piece_count = torrent.getPieces().length;
		
			Map resumeMap = torrent.getAdditionalMapProperty("resume");
		
			if (resumeMap != null) {
			
					// see bug 869749 for explanation of this mangling
				
				String mangled_path;
				
				try{
					mangled_path = new String(data_dir.getBytes(Constants.DEFAULT_ENCODING),Constants.BYTE_ENCODING);
									
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					mangled_path = data_dir;
				}
				
				Map resumeDirectory = (Map)resumeMap.get(mangled_path);
				
				if ( resumeDirectory == null ){
					
					// unfortunately, if the torrent hasn't been saved and restored then the
					// mangling with not yet have taken place. So we have to also try the 
					// original key (see 878015)
					
					resumeDirectory = (Map)resumeMap.get(data_dir);
				}
				
				if (resumeDirectory != null) {
										
					byte[] 	resume_data =  (byte[])resumeDirectory.get("resume data");
					Map		blocks		= (Map)resumeDirectory.get("blocks");
					boolean	valid		= ((Long)resumeDirectory.get("valid")).intValue() == 1;
					
						// any partial pieced -> not complete
					if ( blocks == null || blocks.size() > 0 ){
						
						return( false );
					}
					
					if ( valid && resume_data.length == piece_count ){
						
						for (int i=0;i<resume_data.length;i++){
	
							if ( resume_data[i] == 0 ){
								
									// missing piece
								
								return( false );
							}
						}
						
						return( true );
					}
				}
			}
		}catch( Throwable e ){
		
			e.printStackTrace();
		}	
		
		return( false );
	}
	
		// RESUME DATA STUFF ENDS
	
	private void 
	saveTorrent() 
	{
		try{
			TorrentUtils.writeToFile( torrent );
						
		} catch (TOTorrentException e) {
			
			e.printStackTrace();
		}
	}

  
  public void enqueueReadRequest( DiskManagerRequest request, ReadRequestListener listener ) {
    DiskReadRequest drr = new DiskReadRequest( request, listener );
    synchronized( readQueue ) {
      readQueue.add( drr );
    }
    readQueueSem.release();
  }
  

	public DirectByteBuffer readBlock(int pieceNumber, int offset, int length) {

		DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( length );

		if (buffer == null) { // Fix for bug #804874
			System.out.println("DiskManager::readBlock:: ByteBufferPool returned null buffer");
			return null;
		}

		long previousFilesLength = 0;
		int currentFile = 0;
		PieceList pieceList = pieceMap[pieceNumber];

		// temporary fix for bug 784306
		if (pieceList.size() == 0) {
			System.out.println("no pieceList entries for " + pieceNumber);
			return buffer;
		}

		long fileOffset = pieceList.get(0).getOffset();
		while (currentFile < pieceList.size() && pieceList.getCumulativeLengthToPiece(currentFile) < offset) {
			previousFilesLength = pieceList.getCumulativeLengthToPiece(currentFile);
			currentFile++;
			fileOffset = 0;
		}

		// update the offset (we're in the middle of a file)
		fileOffset += offset - previousFilesLength;
		// noError is only used for error reporting, it could probably be removed
		boolean noError = true;
		while (buffer.hasRemaining()
			&& currentFile < pieceList.size()
			&& (noError = readFileInfoIntoBuffer(pieceList.get(currentFile).getFile(), buffer, fileOffset))) {

			currentFile++;
			fileOffset = 0;
		}

		if (!noError) {
			// continue the error report
			//PieceMapEntry tempPiece = pieceList.get(currentFile);
			//System.out.println("ERROR IN READ BLOCK (CONTINUATION FROM READ FILE INFO INTO BUFFER): *Debug Information*");
			//System.out.println("BufferLimit: " + buffer.limit());
			//System.out.println("BufferRemaining: " + buffer.remaining());
			//System.out.println("PieceNumber: " + pieceNumber);
			//System.out.println("Offset: " + fileOffset);
			//System.out.println("Length  " + length);
			//System.out.println("PieceLength: " + tempPiece.getLength());
			//System.out.println("PieceOffset: " + tempPiece.getOffset());
			//System.out.println("TotalNumPieces(this.nbPieces): " + this.nbPieces);


			// Stop, because if it happened once, it will probably happen everytime
			// Especially in the case of a CD being removed
			stopIt();
			setState( FAULTY );
		}

		buffer.position(0);
		return buffer;
	}

	// refactored out of readBlock() - Moti
	// reads a file into a buffer, returns true when no error, otherwise false.
	private boolean 
	readFileInfoIntoBuffer(
		DiskManagerFileInfoImpl file, 
		DirectByteBuffer buffer, 
		long offset) 
	{
		try{
			file.getFMFile().read( buffer, offset );
			
			return( true );
			
		}catch( FMFileManagerException e ){
			
			this.errorMessage	= (e.getCause()!=null?e.getCause().getMessage():e.getMessage());
			
			return( false );
		}
	}

	public void 
	writeBlock(
		int pieceNumber, 
		int offset, 
		DirectByteBuffer data,
		PEPeer sender) 
	{		
		global_write_queue_block_sem.reserve();
		
		// System.out.println( "reserved global write slot (buffer = " + data.limit() + ")" );
		
		synchronized( writeCheckQueueLock ){
			
			writeQueue.add(new QueueElement(pieceNumber, offset, data,sender));
		}
		
		writeCheckQueueSem.release();
	}

  
	public boolean checkBlock(int pieceNumber, int offset, DirectByteBuffer data) {
		if (pieceNumber < 0) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: pieceNumber="+pieceNumber+" < 0");
			return false;
    }
		if (pieceNumber >= this.nbPieces) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: pieceNumber="+pieceNumber+" >= this.nbPieces="+this.nbPieces);
			return false;
    }
		int length = this.pieceLength;
		if (pieceNumber == nbPieces - 1) {
			length = this.lastPieceLength;
    }
		if (offset < 0) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" < 0");
			return false;
    }
		if (offset > length) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" > length="+length);
			return false;
    }
		int size = data.remaining();
		if (offset + size > length) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" + size="+size+" > length="+length);
			return false;
    }
		return true;
	}
  

	public boolean checkBlock(int pieceNumber, int offset, int length) {
		if (length > 65536) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: length="+length+" > 65536");
		  return false;
		}
		if (pieceNumber < 0) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" < 0");
		  return false;
      }
		if (pieceNumber >= this.nbPieces) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" >= this.nbPieces="+this.nbPieces);
		  return false;
      }
		int pLength = this.pieceLength;
		if (pieceNumber == this.nbPieces - 1)
			pLength = this.lastPieceLength;
		if (offset < 0) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" < 0");
		  return false;
		}
		if (offset > pLength) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" > pLength="+pLength);
		  return false;
		}
		if (offset + length > pLength) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" + length="+length+" > pLength="+pLength);
		  return false;
		}
		if(!this.pieceDone[pieceNumber]) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" not done");
		  return false;
		}
		return true;
	}

		/**
		 * @param e
		 * @return FALSE if the write failed for some reason. Error will have been reported
		 * and queue element set back to initial state to allow a re-write attempt later
		 */
	
	private boolean 
	dumpBlockToDisk(
		QueueElement queue_entry ) 
	{
		int pieceNumber 	= queue_entry.getPieceNumber();
		int offset		 	= queue_entry.getOffset();
		DirectByteBuffer buffer 	= queue_entry.getData();
		int	initial_buffer_position = buffer.position();

		PieceMapEntry current_piece = null;
		
		try{
			int previousFilesLength = 0;
			int currentFile = 0;
			PieceList pieceList = pieceMap[pieceNumber];
			current_piece = pieceList.get(currentFile);
			long fileOffset = current_piece.getOffset();
			while ((previousFilesLength + current_piece.getLength()) < offset) {
				previousFilesLength += current_piece.getLength();
				currentFile++;
				fileOffset = 0;
				current_piece = pieceList.get(currentFile);
			}
	
			//Now tempPiece points to the first file that contains data for this block
			while (buffer.hasRemaining()) {
				current_piece = pieceList.get(currentFile);
	
				if (current_piece.getFile().getAccessMode() == DiskManagerFileInfo.READ){
		
					LGLogger.log(0, 0, LGLogger.INFORMATION, "Changing " + current_piece.getFile().getName() + " to read/write");
						
					current_piece.getFile().setAccessMode( DiskManagerFileInfo.WRITE );
				}
				
				int realLimit = buffer.limit();
					
				long limit = buffer.position() + ((current_piece.getFile().getLength() - current_piece.getOffset()) - (offset - previousFilesLength));
	       
				if (limit < realLimit) {
					buffer.limit((int)limit);
				}
	
				if ( buffer.hasRemaining() ){

					current_piece.getFile().getFMFile().write( buffer, fileOffset + (offset - previousFilesLength));
				}
					
				buffer.limit(realLimit);
				
				currentFile++;
				fileOffset = 0;
				previousFilesLength = offset;
			}
			
			buffer.returnToPool();
			
			return( true );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			String file_name = current_piece==null?"<unknown>":current_piece.getFile().getName();
						
			errorMessage = (e.getCause()!=null?e.getCause().getMessage():e.getMessage()) + " (dumpBlockToDisk) when processing file '" + file_name + "'";
			
			LGLogger.logAlert( LGLogger.AT_ERROR, errorMessage );
			
			buffer.position(initial_buffer_position);
			
			return( false );
		}
	}
    

	public int getPiecesNumber() {
		return nbPieces;
	}

	public boolean[] getPiecesStatus() {
		return pieceDone;
	}

	public int getPercentDone() {
		return percentDone;
	}

	public long getRemaining() {
		return remaining;
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

	protected void
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

	public void stopIt() {
        
		bOverallContinue = false; 

		// remove configuration parameter listeners
	 COConfigurationManager.removeParameterListener("Use Resume", this);
    COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
		
		if (writeThread != null)
			writeThread.stopIt();
		if (readThread != null)
			readThread.stopIt();
    
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
    
    if (allocateAndTestBuffer != null) {
    	allocateAndTestBuffer.returnToPool();
      allocateAndTestBuffer = null;
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

	public void computePriorityIndicator() {
		for (int i = 0; i < pieceCompletion.length; i++) {
		  
		   //if the piece is already complete, skip computation
		   if (pieceDone[i]) {
		     pieceCompletion[i] = -1;
		     continue;
		   }
      
			PieceList pieceList = pieceMap[i];
			int completion = -1;
			
			for (int k = 0; k < pieceList.size(); k++) {
				//get the piece and the file 
				DiskManagerFileInfoImpl fileInfo = (pieceList.get(k)).getFile();
				
				//If the file isn't skipped
				if(fileInfo.isSkipped()) {
					continue;
				}

				//if this is the first piece of the file
				if (firstPiecePriority && i == fileInfo.getFirstPieceNumber()) {
				  if (fileInfo.isPriority()) completion = 99;
				  else completion = 97;
				}
        
        //if the file is high-priority
				else if (fileInfo.isPriority()) {
				  completion = 98;
				}
				
				//If the file is started but not completed
				else {
				  int percent = 0;
				  if (fileInfo.getLength() != 0) {
				    percent = (int) ((fileInfo.getDownloaded() * 100) / fileInfo.getLength());
				  }
				  if (percent < 100) {
				    completion = percent;
				  }
				}
			}
      
			pieceCompletion[i] = completion;
		}

		for (int i = 0; i < priorityLists.length; i++) {
			BitSet list = priorityLists[i];
			if (list == null) {
				list = new BitSet(pieceCompletion.length);
			} else {
				list.clear();
			}
			priorityLists[i]=list;
		}
		
		int priority;
		for (int j = 0; j < pieceCompletion.length; j++) {
			priority = pieceCompletion[j];
			if (priority >= 0) {
				priorityLists[priority].set(j);
			}
		}
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

  
  /*
	// searches from 0 to searchLength-1
    public static int binarySearch(int[] a, int key, int searchLength) {
		int low = 0;
		int high = searchLength - 1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			int midVal = a[mid];

			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return - (low + 1); // key not found.
	}
  */

	public int getPiecenumberToDownload(boolean[] _piecesRarest) {
		//Added patch so that we try to complete most advanced files first.
		List _pieces = new ArrayList();
    
		for (int i = 99; i >= 0; i--) {

		  if (priorityLists[i].isEmpty()) {
		    //nothing is set for this priority, so skip
		    continue;
		  }
		  
		  //Switch comments to enable sequential piece picking.
		  //int k = 0;
		  //for (int j = 0; j < nbPieces && k < 50; j++) {
      
		  for (int j = 0; j < nbPieces ; j++) {
		    if (_piecesRarest[j] && priorityLists[i].get(j)) {
		      _pieces.add( FlyWeightInteger.getInteger(j) );
		      //k++;
		    }
		  }
		  
		  if (_pieces.size() != 0) {
				break;
		  }
		}

		if (_pieces.size() == 0) {
		  return -1;
		}

		return ((Integer)_pieces.get((int) (Math.random() * _pieces.size()))).intValue();
	}

	/*
	  public int getPiecenumberToDownload(boolean[] _piecesRarest) {
		int pieceNumber;
		//Added patch so that we try to complete most advanced files first.
		_priorityPieces[nbPieces] = 0;
		for (int i = priorityLists.length - 1; i >= 0; i--) {
		  for (int j = 0; j < nbPieces; j++) {
			if (_piecesRarest[j] && binarySearch(priorityLists[i], j, priorityLists[i][nbPieces]) >= 0) {
			  _priorityPieces[_priorityPieces[nbPieces]++] = j;
			}
		  }
		  if (_priorityPieces[nbPieces] != 0)
			break;
		}
      
		if (_priorityPieces[nbPieces] == 0)
		  System.out.println("Size 0");
      
		int nPiece = (int) (Math.random() * _priorityPieces[nbPieces]);
		pieceNumber = _priorityPieces[nPiece];
		return pieceNumber;
	  }
	*/
	/**
	 * @return
	 */
	public PEPiece[] getPieces() {
		return pieces;
	}
	
	public DiskManagerRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( new DiskManagerRequestImpl( pieceNumber, offset, length ));
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
   
    
  private byte[] computeMd5Hash(DirectByteBuffer buffer) {
    md5.reset();
    int position = buffer.position();
    md5.update(buffer.getBuffer());
    buffer.position(position);
    md5Result.position(0);
    md5.finalDigest(md5Result.getBuffer());
    byte[] result = new byte[16];
    md5Result.position(0);
    for(int i = 0 ; i < result.length ; i++) {
      result[i] = md5Result.get();
    }    
    return result;    
  }
  
  private void MD5CheckPiece(int pieceNumber,boolean correct) {
    PEPiece piece = manager.getPieces()[pieceNumber];
    if(piece == null) {
      return;
    }
    PEPeer[] writers = piece.getWriters();
    int offset = 0;
    for(int i = 0 ; i < writers.length ; i++) {
      int length = piece.getBlockSize(i);
      PEPeer peer = writers[i];
      if(peer != null) {
        DirectByteBuffer buffer = readBlock(pieceNumber,offset,length);
        byte[] hash = computeMd5Hash(buffer);
        buffer.returnToPool();
        buffer = null;
        piece.addWrite(i,peer,hash,correct);        
      }
      offset += length;
    }        
  }
  
  public boolean isChecking() {
    return (checkQueue.size() != 0);
  }


  public String
  getName()
  {
  	return( dm_name );
  }
  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    useFastResume = COConfigurationManager.getBooleanParameter("Use Resume", true);
    firstPiecePriority = COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
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

  public TOTorrent getTOTorrent() {
    return torrent;
  }
}