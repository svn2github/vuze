/*
 * Created on 02-Aug-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.disk.impl.piecemapper.impl;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapper;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapperFile;

import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.FileUtil;

/**
 * @author parg
 *
 */


public class 
PieceMapperImpl 
	implements DMPieceMapper
{
	private TOTorrent			torrent;
	
	private long 			total_length;
	private int				piece_count;
	private int				piece_length;
	private int				last_piece_length;
	
	protected ArrayList btFileList = new ArrayList();

	
	public
	PieceMapperImpl(
		TOTorrent		_torrent )
	{
		torrent 		= _torrent;
		
		piece_length	= (int)torrent.getPieceLength();
		
		piece_count		= torrent.getNumberOfPieces();
		
		TOTorrentFile[]	files = torrent.getFiles();
		
		for (int i=0;i<files.length;i++){
			
			total_length	+= files[i].getLength();
		}
		
		last_piece_length  	= (int) (total_length - ((long) (piece_count - 1) * (long)piece_length));
	}
	
	public void
	construct(
		LocaleUtilDecoder	_locale_decoder,
		String				_save_name )
	
		throws UnsupportedEncodingException
	{
			//build something to hold the filenames/sizes
		
		TOTorrentFile[] torrent_files = torrent.getFiles();

		if ( torrent.isSimpleTorrent()){
			 								
			buildFileLookupTables( torrent_files[0], _save_name );

		}else{

			buildFileLookupTables( torrent_files, _locale_decoder );
		}
	}
	
	// method for simple torrents
	
	protected void 
	buildFileLookupTables(
		TOTorrentFile			torrent_file, 
		String					fileName )
	{
		btFileList.add(new PieceMapperImpl.fileInfo(torrent_file,"", fileName, total_length ));
	}
	
	protected void 
	buildFileLookupTables(
		TOTorrentFile[]			torrent_files, 
		LocaleUtilDecoder 		locale_decoder ) 
	
		throws UnsupportedEncodingException
	{
		char	separator = File.separatorChar;
				
		 //for each file
         
		for (int i = 0; i < torrent_files.length; i++) {
        	
			buildFileLookupTable(torrent_files[i], locale_decoder, separator);
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
	
	private void 
	buildFileLookupTable(
		TOTorrentFile		torrent_file, 
		LocaleUtilDecoder 	locale_decoder, 
		final char 			separator) 
	
		throws UnsupportedEncodingException
	{
		long fileLength  = torrent_file.getLength();

		//build the path
        
		byte[][]	path_components = torrent_file.getPathComponents();

		/* replaced the following two calls:
		StringBuffer pathBuffer = new StringBuffer(256);
		pathBuffer.setLength(0);
		*/
		StringBuffer pathBuffer = new StringBuffer(0);

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
			new fileInfo(
				torrent_file,
				pathBuffer.toString(),
				last_comp,
				fileLength));
	}
	
	
	
	public DMPieceList[] 
	getPieceMap()
	{
		DMPieceList[]	pieceMap = new DMPieceList[piece_count];


		//for every piece, except the last one
		//add files to the piece list until we have built enough space to hold the piece
		//see how much space is available in the file
		//if the space available isnt 0
		//add the file to the piece->file mapping list
		//if there is enough space available, stop  

			//fix for 1 piece torrents
	
		int	modified_piece_length	= piece_length;
		
		if (total_length < modified_piece_length) {
			
			modified_piece_length = (int)total_length;
		}

		long fileOffset = 0;
		int currentFile = 0;
		for (int i = 0;(1 == piece_count && i < piece_count) || i < piece_count - 1; i++) {
			ArrayList pieceToFileList = new ArrayList();
			int usedSpace = 0;
			while (modified_piece_length > usedSpace) {
				fileInfo tempFile = (fileInfo)btFileList.get(currentFile);
				long length = tempFile.getLength();

				//get the available space
				long availableSpace = length - fileOffset;

				PieceMapEntryImpl tempPieceEntry = null;

				//how much space do we need to use?                               
				if (availableSpace <= (modified_piece_length - usedSpace)) {
					//use the rest of the file's space
						tempPieceEntry =
							new PieceMapEntryImpl(tempFile.getFileInfo(), fileOffset, (int)availableSpace //safe to convert here
	);

					//update the used space
					usedSpace += availableSpace;
					//update the file offset
					fileOffset = 0;
					//move the the next file
					currentFile++;
				} else //we don't need to use the whole file
					{
					tempPieceEntry = new PieceMapEntryImpl(tempFile.getFileInfo(), fileOffset, modified_piece_length - usedSpace);

					//update the file offset
					fileOffset += modified_piece_length - usedSpace;
					//udate the used space
					usedSpace += modified_piece_length - usedSpace;
				}

				//add the temp pieceEntry to the piece list
				pieceToFileList.add(tempPieceEntry);
			}

			//add the list to the map
			pieceMap[i] = PieceListImpl.convert(pieceToFileList);
		}

		//take care of final piece if there was more than 1 piece in the torrent
		if (piece_count > 1) {
			pieceMap[piece_count - 1] =
				PieceListImpl.convert(
						buildLastPieceToFileList(
									btFileList, 
									currentFile, 
									fileOffset ));

		}
		
		return( pieceMap );
	}

	

	private List 
	buildLastPieceToFileList(
		List file_list, 
		int currentFile, 
		long fileOffset )
	{
		ArrayList pieceToFileList = new ArrayList();
		int usedSpace = 0;
		while (last_piece_length > usedSpace) {
			fileInfo tempFile = (fileInfo)file_list.get(currentFile);
			long length = tempFile.getLength();

			//get the available space
			long availableSpace = length - fileOffset;

			PieceMapEntryImpl tempPieceEntry = null;

			//how much space do we need to use?                               
			if (availableSpace <= (piece_length - usedSpace)) {
				//use the rest of the file's space
				tempPieceEntry = new PieceMapEntryImpl(tempFile.getFileInfo(), fileOffset, (int)availableSpace);

				//update the used space
				usedSpace += availableSpace;
				//update the file offset
				fileOffset = 0;
				//move the the next file
				currentFile++;
			} else //we don't need to use the whole file
				{
				tempPieceEntry = new PieceMapEntryImpl(tempFile.getFileInfo(), fileOffset, last_piece_length - usedSpace);

				//update the file offset
				fileOffset += piece_length - usedSpace;
				//udate the used space
				usedSpace += piece_length - usedSpace;
			}

			//add the temp pieceEntry to the piece list
			pieceToFileList.add(tempPieceEntry);
		}

		return pieceToFileList;
	}

	public long
	getTotalLength()
	{
		return( total_length );
	}

	public int
	getLastPieceLength()
	{
		return( last_piece_length );
	}
	
	public DMPieceMapperFile[]
	getFiles()
	{
		DMPieceMapperFile[]	res = new DMPieceMapperFile[ btFileList.size()];
		
		btFileList.toArray( res );
		
		return( res );
	}
	
	protected static class 
	fileInfo 
		implements DMPieceMapperFile
	{
		private DiskManagerFileInfoImpl		file;
		private TOTorrentFile				torrent_file;
		private String 						path;
		private String 						name;
		private long 						length;

		public 
		fileInfo(
			TOTorrentFile	_torrent_file,
			String 			_path, 
			String 			_name, 
			long 			_length) 
		{
			torrent_file	= _torrent_file;
			path			= _path;
			length 			= _length;
			name 			= _name;
		}
		
		public long getLength() {
			return length;
		}
		public File
		getDataFile()
		{
			return( new File( path, name ));
		}
		public TOTorrentFile
		getTorrentFile()
		{
			return( torrent_file );
		}
		public DiskManagerFileInfoImpl getFileInfo() {
			return file;
		}
		public void setFileInfo(DiskManagerFileInfoImpl _file) {
			file = _file;
		}
	}
}
