/*
 * Created on Apr 26, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.diskmanager.test;

import java.io.*;
import java.nio.channels.*;
import java.util.Random;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.diskmanager.MemoryMappedFile;

/**
 *
 */
public class MemoryMappedFileTester {
  static long MAX_SIZE = 1L*1024*1024*1024;
  //static long MAX_SIZE = 237*1024*1024;
  static int BUFF_SIZE = 5*1024*1024;

  static DirectByteBuffer dbb = DirectByteBufferPool.getBuffer( BUFF_SIZE );
  static byte[] raw = new byte[ BUFF_SIZE ];
  static Random rand = new Random();
  
	public static void main(String[] args) {
    try {
    	File fraf = new File( "e:\\fraf.bin" );
    	File fchan = new File( "e:\\fchan.bin" );
    	File fmapd = new File( "e:\\fmapd.bin" );
    
    	RandomAccessFile raf = new RandomAccessFile( fraf, "rw" );
      FileChannel fc = new RandomAccessFile( fchan, "rw" ).getChannel();
      MemoryMappedFile mmf = new MemoryMappedFile( fmapd );
      mmf.setAccessMode( MemoryMappedFile.MODE_READ_WRITE );
     
      long written = 0;
      long traf = 0;
      long tchan = 0;
      long tmmf = 0;
      int loop = 1;
      
      while( written < MAX_SIZE ) {
        System.out.print("|");  if (loop % 80 == 0) System.out.println();
        refreshBuffers();
        long start_pos = new Float(rand.nextFloat()*(MAX_SIZE-BUFF_SIZE)).longValue();
///////////////////////////////////////////////////////
        long start = System.currentTimeMillis();
        //raf.seek( start_pos );  raf.write( raw );
        traf += System.currentTimeMillis() - start;
///////////////////////////////////////////////////////
        start = System.currentTimeMillis();
        //fc.write( dbb.buff, start_pos );
        tchan += System.currentTimeMillis() - start;
///////////////////////////////////////////////////////
        start = System.currentTimeMillis();
        mmf.write( dbb, 0, start_pos, dbb.limit() );
        tmmf += System.currentTimeMillis() - start;
///////////////////////////////////////////////////////
        written += raw.length;
        loop++;
      }
      
      System.out.println();
      System.out.println("RandomAccessFile = " + traf);
      System.out.println("FileChannel = " + tchan);
      System.out.println("MemoryMappedFile = " + tmmf);
      System.out.println("Cache H: " +MemoryMappedFile.cache_hits+ " M: " +MemoryMappedFile.cache_misses);
      
    
    }
    catch (Throwable t) { t.printStackTrace(); }
	}
  

  private static void refreshBuffers() {
    rand.nextBytes( raw );
    dbb.clear();
    dbb.put( raw );
    dbb.flip();
  }
  

}
