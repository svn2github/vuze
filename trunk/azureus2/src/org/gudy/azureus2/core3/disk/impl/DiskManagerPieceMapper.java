/*
 * Created on 02-Aug-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.disk.impl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.disk.*;

import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.FileUtil;

/**
 * @author parg
 *
 */


public class 
DiskManagerPieceMapper 
{
	protected DiskManagerHelper	disk_manager;
	
	protected long 				total_length;
	protected int				piece_count;
	protected int				piece_length;
	protected int				last_piece_length;
	
	protected ArrayList btFileList = new ArrayList();

	
	protected
	DiskManagerPieceMapper(
		DiskManagerHelper	_disk_manager )
	{
		disk_manager	= _disk_manager;
		
		TOTorrent	torrent = disk_manager.getTorrent();
		
		piece_length	= (int)torrent.getPieceLength();
		
		piece_count		= torrent.getPieces().length;
		
		TOTorrentFile[]	files = torrent.getFiles();
		
		for (int i=0;i<files.length;i++){
			
			total_length	+= files[i].getLength();
		}
		
		last_piece_length  	= (int) (total_length - ((long) (piece_count - 1) * (long)piece_length));

	}
	
	// method for simple torrents
	
	protected void 
	buildFileLookupTables(
		TOTorrentFile			torrent_file, 
		String					fileName )
	{
		btFileList.add(new DiskManagerPieceMapper.fileInfo(torrent_file,"", fileName, total_length ));
	}
	
	protected void 
	buildFileLookupTables(
		TOTorrentFile[]			torrent_files, 
		LocaleUtilDecoder 		locale_decoder ) 
	{
		char	separator = File.separatorChar;
				
		 //for each file
         
		for (int i = 0; i < torrent_files.length; i++) {
        	
			buildFileLookupTable(torrent_files[i], locale_decoder, separator);

			if (disk_manager.getState() == DiskManager.FAULTY){
			
				return;
			}
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
				new fileInfo(
					torrent_file,
					pathBuffer.toString(),
					last_comp,
					fileLength));
		}catch( UnsupportedEncodingException e ){

			disk_manager.setErrorMessage( e.getMessage() + " (buildFileLookupTable)" );
			
			disk_manager.setState( DiskManager.FAULTY );
		}
	}

  
	
	
	
	protected PieceList[] 
	constructPieceMap()
	{
		PieceList[]	pieceMap = new PieceList[piece_count];


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

				PieceMapEntry tempPieceEntry = null;

				//how much space do we need to use?                               
				if (availableSpace < (modified_piece_length - usedSpace)) {
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
					tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, modified_piece_length - usedSpace);

					//update the file offset
					fileOffset += modified_piece_length - usedSpace;
					//udate the used space
					usedSpace += modified_piece_length - usedSpace;
				}

				//add the temp pieceEntry to the piece list
				pieceToFileList.add(tempPieceEntry);
			}

			//add the list to the map
			pieceMap[i] = PieceList.convert(pieceToFileList);
		}

		//take care of final piece if there was more than 1 piece in the torrent
		if (piece_count > 1) {
			pieceMap[piece_count - 1] =
				PieceList.convert(
						buildLastPieceToFileList(
									btFileList, 
									currentFile, 
									fileOffset ));

		}
		
		return( pieceMap );
	}

	

	private List 
	buildLastPieceToFileList(
		List btFileList, 
		int currentFile, 
		long fileOffset )
	{
		ArrayList pieceToFileList = new ArrayList();
		int usedSpace = 0;
		while (last_piece_length > usedSpace) {
			fileInfo tempFile = (fileInfo)btFileList.get(currentFile);
			long length = tempFile.getLength();

			//get the available space
			long availableSpace = length - fileOffset;

			PieceMapEntry tempPieceEntry = null;

			//how much space do we need to use?                               
			if (availableSpace < (piece_length - usedSpace)) {
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
				tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, last_piece_length - usedSpace);

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

	protected long
	getTotalLength()
	{
		return( total_length );
	}

	protected int
	getLastPieceLength()
	{
		return( last_piece_length );
	}
	
	protected List
	getFileList()
	{
		return( btFileList );
	}
	
	protected static class 
	fileInfo 
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
		public String getPath() {
			return path;
		}
		public String getName() {
			return name;
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
