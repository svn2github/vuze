/*
 * Created on Apr 4, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util.test;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.util.SHA1Az;
import java.security.MessageDigest;

/**
 * 
 */
public class SHA1Verification {
  
  public static final String dirname = "D:" + System.getProperty("file.separator") + "testdir";
  
  public static void main(String[] args) {
    if (! new File( dirname ).exists())  createTestFiles();
    runTests();
  }

  public static void createTestFiles() {
    try {
      System.out.println("Creating test files ... ");
      Random rand = new Random();
      String rootname = "f-";
      
      long[] sizes = { 0, 1, 3347483648L};
    
      File testdir = new File( dirname );
      testdir.mkdirs();
   

      
      for (int i=0; i < sizes.length; i++) {
        long size = sizes[i];
        File file = new File( testdir, rootname + String.valueOf( size ));
        System.out.println( file.getName() + "...");
        FileChannel fc = new RandomAccessFile( file, "rw" ).getChannel();
        
        long position = 0;
        while ( position < size ) {
          long remaining = size - position;
          if ( remaining > 1024000 ) remaining = 1024000;
          byte[] buffer = new byte[ new Long(remaining).intValue() ];
          rand.nextBytes( buffer );
          ByteBuffer bb = ByteBuffer.wrap( buffer );
          position += fc.write( bb );
        }
        
        fc.close();
      }
      System.out.println("DONE\n");
    }
    catch (Exception e) { e.printStackTrace(); }
  }
  
  
	public static void runTests() {
    try {
    
      //SHA1 sha1Jmule = new SHA1();
      MessageDigest sha1Sun = MessageDigest.getInstance("SHA-1");
      SHA1Az sha1Gudy = new SHA1Az();
      //SHA1Az shaGudyResume = new SHA1Az();
    
      ByteBuffer buffer = ByteBuffer.allocate( 1024 * 1024 );
    
      File dir = new File( dirname );
      File[] files = dir.listFiles();

      for (int i=0; i < files.length; i++) {
        FileChannel fc = new RandomAccessFile( files[i], "r" ).getChannel();
        
        System.out.println("Testing " + files[i].getName() + " ...");
        
        while( fc.position() < fc.size() ) {
         fc.read( buffer );
         buffer.flip();
         
         byte[] raw = new byte[ buffer.limit() ];
         System.arraycopy( buffer.array(), 0, raw, 0, raw.length );

         sha1Gudy.update( buffer );
         sha1Gudy.saveState();
         ByteBuffer bb = ByteBuffer.allocate(9731); bb.put( (byte)9 ); bb.flip();
         sha1Gudy.digest(bb );
         sha1Gudy.restoreState();
         
         sha1Sun.update( raw );
         
         buffer.clear();
        }
        
        byte[] sun = sha1Sun.digest();
        sha1Sun.reset();
        
        byte[] gudy = sha1Gudy.digest();
        sha1Gudy.reset();
        
        if ( Arrays.equals( sun, gudy ) ) {
          System.out.println("  SHA1-Gudy: OK");
        }
        else {
          System.out.println("  SHA1-Gudy: FAILED");
        }
        
        buffer.clear();
        fc.close();
        System.out.println();
      }
    
    }
    catch (Throwable e) { e.printStackTrace(); }
  }
	

}
