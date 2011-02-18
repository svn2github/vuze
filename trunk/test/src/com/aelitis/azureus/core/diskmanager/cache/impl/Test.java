/*
 * Created on 04-Aug-2004
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

package com.aelitis.azureus.core.diskmanager.cache.impl;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author parg
 *
 */


public class 
Test 
{
	public static void
	main(
		String	[]args )
	{
		System.setProperty("azureus.log.stdout","1");
		
		Logger.addListener(new ILogEventListener() {
			public void log(LogEvent event) {
				System.out.println(event.text);
			}
		});
		
		try{
			CacheFileManagerImpl	manager = (CacheFileManagerImpl)CacheFileManagerFactory.getSingleton();
			
			//manager.initialise( false, 8*1024*1024 );
	
			//new Test().writeTest(manager);
			
			manager.initialise( true, true, true, 10*1024*1024, 1024 );

			new Test().pieceReorderTest(manager);
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	pieceReorderTest(
		CacheFileManagerImpl	manager )
	{
		try{
			Random random = new Random(0);
			
			int	num_files 	= 100;
			int	piece_size	= 1024;
			int file_size_average	= piece_size*30;
			
			int	chunk_fixed_size 	= 0;
			int chunk_random_size	= 1024;
			
			int	write_order	= 2;		// 0 = linear forwards; 1 = linear backwards; 2 = random;
			
			
			
			int[]	file_sizes = new int[num_files];
			
			for ( int i=0;i<num_files;i++){
				
				file_sizes[i] = random.nextInt( 2*file_size_average ) + 1;
			}
		
			final File	control_dir		= new File("C:\\temp\\filetestcontrol" );
			
			FileUtil.recursiveDelete( control_dir );
			
			control_dir.mkdirs();
	
			File	torrent_file 	= new File("C:\\temp\\filetest.torrent" );
			
			torrent_file.delete();
			
			File	source_file_or_dir;
			File	target_file_or_dir;
			
			if ( num_files == 1 ){
				
				source_file_or_dir = new File("C:\\temp\\filetest1.dat" );
				target_file_or_dir = new File("C:\\temp\\filetest2.dat" );
				
				source_file_or_dir.delete();
				target_file_or_dir.delete();
			}else{
				
				source_file_or_dir = new File("C:\\temp\\filetest1.dir" );
				target_file_or_dir = new File("C:\\temp\\filetest2.dir" );
				
				FileUtil.recursiveDelete( source_file_or_dir );
				FileUtil.recursiveDelete( target_file_or_dir );
				
				source_file_or_dir.mkdirs();
				target_file_or_dir.mkdirs();
			}
			
			File[]	source_files = new File[ num_files ];
			File[]	target_files = new File[ num_files ];
			
			RandomAccessFile[]	source_file_rafs = new RandomAccessFile[ num_files ];
			
			final TOTorrent torrent;
			
			for ( int i=0; i<num_files; i++ ){
				
				File	source_file;
				File	target_file;
				
				if  ( num_files == 1 ){
					
					source_file = source_file_or_dir;
					target_file = target_file_or_dir;
					
				}else{
					
					source_file = new File( source_file_or_dir, "file" + i );
					target_file = new File( target_file_or_dir, "file" + i );
				}
				
				source_files[i] = source_file;
				target_files[i]	= target_file;
				
				FileOutputStream fos = new FileOutputStream( source_file );
				
				byte[]	buffer = new byte[64*1024];
				
				int	rem = file_sizes[i];
				
				while( rem > 0 ){
				
					random.nextBytes( buffer );
				
					int	to_write = rem>buffer.length?buffer.length:rem;
					
					fos.write( buffer, 0, to_write );
					
					rem -= to_write;
				}
				
				fos.close();
				
				source_file_rafs[i] = new RandomAccessFile( source_file, "r" );
			}
			
			torrent = 
					TOTorrentFactory.createFromFileOrDirWithFixedPieceLength(
						source_file_or_dir,
						new URL( "http://a.b.c/" ),
						piece_size ).create();
			
			final TOTorrentFile[] torrent_files = torrent.getFiles();
			
				// unfortunately the torrent's file order may not be ours...
			
			for ( int i=0;i<torrent_files.length;i++){

				TOTorrentFile tf = torrent_files[i];
				
				String rel_path = tf.getRelativePath();
				
				boolean	found = false;
				
				for (int j=0;j<source_files.length;j++){
					
					if ( source_files[j].getName().equals( rel_path )){
				
						found = true;
					
						if ( j != i ){
							
							int	temp = file_sizes[i];
							file_sizes[i] = file_sizes[j];
							file_sizes[j] = temp;
							
							File femp = source_files[i];
							source_files[i] = source_files[j];
							source_files[j] = femp;
							
							femp = target_files[i];
							target_files[i] = target_files[j];
							target_files[j] = femp;
							
							RandomAccessFile remp = source_file_rafs[i];
							source_file_rafs[i] = source_file_rafs[j];
							source_file_rafs[j] = remp;
						}
						break;
					}
				}

				if ( !found ){
					
					Debug.out( "eh?" );
					
					return;
				}
			}
			
			CacheFile[] cache_files = new CacheFile[ torrent_files.length ];
			
			for ( int i=0;i<torrent_files.length;i++){
				
				final int f_i = i;
				
				File target_file		= target_files[i];
				final File source_file	= source_files[i];
				
				
				System.out.println( "file " + i + ": e_size=" + file_sizes[i] + ", t_size=" + torrent_files[i].getLength() + ", d_size=" + source_file.length());
				
				cache_files[i] = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( source_file.getAbsolutePath());
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( torrent_files[f_i] );
						}
						public File 
						getCacheFileControlFileDir() 
						{
							return( control_dir );
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NO_CACHE );
						}
					},
					target_file, CacheFile.CT_PIECE_REORDER );
			
				cache_files[i].setAccessMode( CacheFile.CF_WRITE );
			}
			
			List<Chunk> chunks = new ArrayList<Chunk>();
			
			List<Chunk>[] piece_map = new List[ torrent.getNumberOfPieces()];
			
			{
				long	pos 			= 0;
				int		file_index		= 0;
				long	file_offset		= 0;
				
				long	total_size 	= torrent.getSize();
				long	rem			= total_size;
				
	
				while( rem > 0 ){
					
					long chunk_length;
					
					if ( chunk_fixed_size != 0 ){
						
						chunk_length = chunk_fixed_size;
						
					}else{
						
						chunk_length = random.nextInt( chunk_random_size ) + 1;
					}
					
					if ( rem < chunk_length ){
						
						chunk_length = (int)rem;
					}
									
					List<ChunkSlice> slices = new ArrayList<ChunkSlice>();
					
					Chunk chunk = new Chunk( pos, chunk_length, slices );
					
					chunks.add( chunk );
						
					while( chunk_length > 0 ){
					
						long	file_size = file_sizes[ file_index ];
	
						long 	file_rem = file_size - file_offset;
						
						long	avail = Math.min( file_rem, chunk_length );
						
						if ( avail > 0 ){
			
							int	piece_start	= (int)( pos/piece_size );

							rem -= avail;
							pos	+= avail;

							int	piece_end	= (int)((pos-1)/piece_size );
							
							slices.add( new ChunkSlice( file_index, file_offset, avail, piece_start, piece_end ));
						}
						
						chunk_length -= avail;
			
						if ( chunk_length > 0 ){
							
							file_offset	= 0;
							
							file_index++;
							
						}else{
							
							file_offset += avail;
							
							break;
						}
					}
					
					int	piece_start = slices.get(0).getPieceStart();
					int	piece_end 	= slices.get(slices.size()-1).getPieceEnd();
					
					for (int i=piece_start;i<=piece_end;i++){
						
						if ( piece_map[i] == null ){
							
							piece_map[i] = new ArrayList<Chunk>();
						}
						
						piece_map[i].add( chunk );
					}
					
					chunk.setPieces( piece_start, piece_end );				

					System.out.println( chunk.getString());		
				}
			}
			
			for (int i=0;i<piece_map.length;i++){
				
				System.out.println( i + ": " + piece_map[i].size());
			}
			
			while ( chunks.size() > 0 ){
				
				Chunk chunk;
				
				if ( write_order == 0 ){
					
					chunk = chunks.remove( 0 );
					
				}else if ( write_order == 1 ){
						
					chunk = chunks.remove( chunks.size() - 1 );
						
				}else{
					
					chunk = chunks.remove( random.nextInt( chunks.size()));
				}
				
				System.out.println( "Processing chunk " + chunk.getString());
				
				List<ChunkSlice> slices = new ArrayList<ChunkSlice>( chunk.getSlices());
				
				if ( write_order == 1 ){
					
					Collections.reverse( slices );
				}
				
				for ( ChunkSlice slice: slices ){
					
					int		file_index 	= slice.getFileIndex();
					long	file_offset	= slice.getFileOffset();
					long	length		= slice.getLength();
					

					System.out.println( "Processing slice " + slice.getString() + "[file size=" + file_sizes[file_index]);
					
					DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, (int)length );
					
					try{
						RandomAccessFile raf = source_file_rafs[ file_index ];
						
						raf.seek( file_offset );
						
						raf.getChannel().read( buffer.getBuffer( DirectByteBuffer.SS_EXTERNAL ));
						
						buffer.flip( DirectByteBuffer.SS_EXTERNAL );
						
						cache_files[file_index].write( buffer, file_offset );
					
					}finally{
						
						buffer.returnToPool();
					}
				}
				
				chunk.setDone();
				
				int	chunk_piece_start 	= chunk.getPieceStart();
				int	chunk_piece_end		= chunk.getPieceEnd();
				
				for ( int i=chunk_piece_start;i<=chunk_piece_end;i++){
					
					List<Chunk> pieces = piece_map[i];
					
					boolean complete = true;
					
					for ( Chunk c: pieces ){
						
						if ( !c.isDone()){
							
							complete = false;
							
							break;
						}
					}
					
					if ( complete ){
						
						for ( ChunkSlice slice: slices ){

							if ( i >= slice.getPieceStart() && i <= slice.getPieceEnd()){

								long	piece_offset = i*piece_size;
								int		piece_length;

								if ( i < piece_map.length - 1 ){
									
									piece_length = piece_size;
									
								}else{
									
									long	total = torrent.getSize();
									
									piece_length = (int)( total - ( total/piece_size )*piece_size );
									
									if ( piece_length == 0 ){
										
										piece_length = piece_size;
									}
								}
														
								DirectByteBuffer	piece_data = 
									DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, piece_length );

								long	pos			= 0;
								int		file_index	= 0;
								int		rem			= piece_length;
								
								while( rem > 0 ){
									
									long	file_size = file_sizes[ file_index ];
									
									long	file_end = pos + file_size;
									
									long	avail = file_end - piece_offset;

									if ( avail > 0 ){
										
										int	to_use = (int)Math.min( avail, rem );
										
										long file_offset = piece_offset - pos;
										
										int	lim = piece_data.limit( DirectByteBuffer.AL_OTHER );
										
										piece_data.limit( DirectByteBuffer.AL_OTHER, piece_data.position( DirectByteBuffer.AL_OTHER ) + to_use );
										
										cache_files[ file_index ].read( piece_data, file_offset, CacheFile.CP_NONE );
										
										piece_data.limit( DirectByteBuffer.AL_OTHER, lim );

										piece_offset += to_use;
										
										rem -= to_use;
									}
									
									file_index++;
									
									pos += file_size;
								}
								
								try{
									cache_files[ slice.getFileIndex()].setPieceComplete(
										i,
										piece_data );	
									
								}finally{
									
									piece_data.returnToPool();
								}
							}
						}
					}
				}
			}
			
			for (int i=0;i<num_files;i++){
				
				source_file_rafs[i].close();
				
				cache_files[i].close();
				
				byte[]	buffer1 = new byte[256*1024];
				byte[]	buffer2 = new byte[256*1024];
				
				if ( source_files[i].length() != target_files[i].length()){
					
					System.err.println( "File sizes differ for " + i );
				}
				
				FileInputStream	fis1 = new FileInputStream( source_files[i] );
				FileInputStream	fis2 = new FileInputStream( target_files[i] );
				
				long	len = file_sizes[i];
				long	pos	= 0;
				
				boolean	failed = false;
				
				while( len > 0 ){
					
					int	avail = (int)Math.min( len, buffer1.length );
					
					int r1 = fis1.read( buffer1, 0, avail );
					int r2 = fis2.read( buffer2, 0, avail );
					
					if ( r1 != r2 ){
						
						System.err.println( "read lens different: file=" + i + ",pos=" + pos );
						
						failed = true;
						
						break;
						
					}else{
						
						if ( Arrays.equals( buffer1, buffer2 )){
							
							len -= r1;
							pos += r1;
						}else{
							
							int diff_at = -1;
							
							for (int j=0;j<avail;j++){
								
								if ( buffer1[j] != buffer2[j] ){
									
									diff_at = j;
									
									break;
								}
							}
							
							System.err.println( "mismatch: file=" + i + ",pos=" + pos + " + " + diff_at );
							
							failed = true;
							
							break;
						}
					}
				}
				
				if ( !failed ){
					
					System.out.println( "file " + i + ": matched " + pos + " of " + file_sizes[i] );
					
				}
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public void
	writeTest(
		CacheFileManagerImpl	manager )
	{
		try{
			final File	f = new File("C:\\temp\\cachetest.dat" );
			
			f.delete();
			
			CacheFile	cf = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file " + f.toString() );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFileDir() 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					f, CacheFile.CT_LINEAR );
			
			cf.setAccessMode( CacheFile.CF_WRITE );
			
			long	start = System.currentTimeMillis();
			
			int		loop	= 10000;
			int		block	= 1*1024;
			
			for (int i=0;i<loop;i++){
				
				DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, block);
				
				cf.writeAndHandoverBuffer( buffer, i*block);
			}
			
			cf.close();
			
			long 	now = System.currentTimeMillis();
			
			long	total = loop*block;
			
			long	elapsed = now - start;
			
			System.out.println( "time = " + elapsed + ", speed = " + (total/elapsed));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	manualTest(
		CacheFileManager	manager )
	{
		try{
			final File	f = new File("C:\\temp\\cachetest.dat" );
			
			f.delete();
			
			CacheFile	cf = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file " + f.toString() );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFileDir() 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					f, CacheFile.CT_LINEAR );
			DirectByteBuffer	write_buffer1 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			DirectByteBuffer	write_buffer2 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			DirectByteBuffer	write_buffer3 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			
			cf.writeAndHandoverBuffer( write_buffer2, 512 );
				
			cf.flushCache();
			
			cf.writeAndHandoverBuffer( write_buffer3, 1024 );
			cf.writeAndHandoverBuffer( write_buffer1, 0 );
			
			cf.flushCache();
			
			write_buffer1 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			cf.writeAndHandoverBuffer( write_buffer1, 0 );

			cf.flushCache();
				
			cf.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	randomTest(
		CacheFileManager	manager )
	{
		try{			
			CacheFile[]	files = new CacheFile[3];
			
			byte[][]	file_data	= new byte[3][];
			
			for (int i=0;i<files.length;i++){
				
				final	int f_i = i;
			
				file_data[i] = new byte[randomInt(200000)];
				
				files[i] = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file" + f_i );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFileDir() 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					new File( "C:\\temp\\cachetest" + i + ".dat" ), CacheFile.CT_LINEAR);
				
				files[i].setAccessMode( CacheFile.CF_WRITE );
				
				DirectByteBuffer bb = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,file_data[i].length);
				
				bb.put( DirectByteBuffer.SS_CACHE, file_data[i]);
				
				bb.position(DirectByteBuffer.SS_CACHE, 0);
				
				files[i].write(bb,0);
			}
			
			int	quanitize_to					= 100;
			int quanitize_to_max_consec_write	= 1;
			int quanitize_to_max_consec_read	= 3;
			
			for (int x=0;x<10000000;x++){
				
				int	file_index = randomInt(files.length);
				
				CacheFile	cf = files[file_index];
				
				byte[]	bytes = file_data[ file_index ];
				
				
				int	p1 = randomInt( bytes.length );
				int p2 = randomInt( bytes.length );
				
				p1 = (p1/quanitize_to)*quanitize_to;
				p2 = (p2/quanitize_to)*quanitize_to;
				
				if ( p1 == p2 ){
					
					continue;
				}
				
				int start 	= Math.min(p1,p2);
				int len	 	= Math.max(p1,p2) - start;
				
				int	function = randomInt(100);
				
				if ( function < 30){
					
					if ( len > quanitize_to*quanitize_to_max_consec_read ){
						
						len = quanitize_to*quanitize_to_max_consec_read;
					}
					
					DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER,len );
					
					System.out.println( "read:" + start + "/" + len );
					
					cf.read( buffer, start, CacheFile.CP_READ_CACHE );
					
					buffer.position(DirectByteBuffer.SS_CACHE, 0);
					
					byte[]	data_read = new byte[len];
					
					buffer.get( DirectByteBuffer.SS_CACHE, data_read );
					
					for (int i=0;i<data_read.length;i++){
						
						if ( data_read[i] != bytes[ i+start ]){
							
							throw( new Exception( "data read mismatch" ));
						}
					}
					
					buffer.returnToPool();
					
				}else if ( function < 80 ){
					if ( len > quanitize_to*quanitize_to_max_consec_write ){
						
						len = quanitize_to*quanitize_to_max_consec_write;
					}
					
					System.out.println( "write:" + start + "/" + len );
					
					DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER,len );
					
					for (int i=0;i<len;i++){
						
						bytes[start+i] = (byte)randomInt(256);
						
						buffer.put( DirectByteBuffer.SS_CACHE, bytes[start+i]);
					}
					
					buffer.position(DirectByteBuffer.SS_CACHE, 0);
					
					cf.writeAndHandoverBuffer( buffer, start );
					
				}else if ( function < 90 ){
					
					cf.flushCache();
					
				}else if ( function < 91 ){
					
					cf.clearCache();
					
					//System.out.println( "closing file" );
					
					//cf.close();
				}
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	static int
	randomInt(
		int	num )
	{
		return( (int)(Math.random()*num ));
	}
	
	protected static class
	Chunk
	{
		private long				offset;
		private long				length;
		private List<ChunkSlice>	slices;
		
		private int					piece_start;
		private int					piece_end;
		
		private boolean				is_done;
		
		protected
		Chunk(
			long				_offset,
			long				_length,
			List<ChunkSlice>	_slices )
		{
			offset	= _offset;
			length	= _length;
			slices	= _slices;
		}
		
		protected List<ChunkSlice>
		getSlices()
		{
			return( slices );
		}
		
		protected void
		setPieces(
			int		_piece_start,
			int		_piece_end )
		{
			piece_start	= _piece_start;
			piece_end	= _piece_end;
		}
		
		protected int
  		getPieceStart()
  		{
  			return( piece_start );
  		}
		
		protected int
  		getPieceEnd()
  		{
			return( piece_end );
  		}
		
		protected void
		setDone()
		{
			is_done	= true;
		}
		
		protected boolean
		isDone()
		{
			return( is_done );
		}
		
		protected String
		getString()
		{
			String	str = "";
			
			for ( ChunkSlice s: slices ){
				
				str += (str.length()==0?"":",") + s.getString();
			}
			
			return( "offset=" + offset + ",length=" + length + ",slices={" + str + "}" );
		}
	}
	
	protected static class
	ChunkSlice
	{
		private int		file_index;
		private long	file_offset;
		private long	length;
		
		private int		piece_start;
		private int		piece_end;
		
		protected
		ChunkSlice(
			int		_file_index,
			long	_file_offset,
			long	_length,
			int		_piece_start,
			int		_piece_end )
		{
			file_index	= _file_index;
			file_offset	= _file_offset;
			length		= _length;
			
			piece_start	= _piece_start;
			piece_end	= _piece_end;
		}
		
		protected int
		getFileIndex()
		{
			return( file_index );
		}
		
		protected long
		getFileOffset()
		{
			return( file_offset );
		}
		
		protected long
		getLength()
		{
			return( length );
		}
		
		protected int
		getPieceStart()
		{
			return( piece_start );
		}
		
		protected int
		getPieceEnd()
		{
			return( piece_end );
		}
		
		protected String
		getString()
		{
			return( "fi=" + file_index + ",fo=" + file_offset + ",len=" + length + ",ps=" + piece_start + ",pe=" + piece_end);
		}
	}
}
