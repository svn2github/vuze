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

import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.SHA1Az;


/**
 * 
 */
public class SHA1Verification {
  
  public static final String dirname = System.getProperty("user.home") + System.getProperty("file.separator") + "testdir";
  
  public static void main(String[] args) {
    if (! new File( dirname ).exists())  createTestFiles();
    //runTests();
  }

  public static void createTestFiles() {
    try {
      System.out.println("Creating test files ... ");
      Random rand = new Random();
      String rootname = "f-";
      long[] sizes = { 1, 4, 10, 16, 25, 32, 75, 127, 179, 250, 512, 1003, 1023, 1024, 1025,
        							 1500, 2011, 2910, 3500, 5120, 8700, 10250, 50001, 77777, 100030, 210000,
        							 512000, 1024000, 1024001, 5120003, 10000000, 25000007, 100000000,
        							 255000000, 512000000, 756000000};
    
      File testdir = new File( dirname );
      testdir.mkdirs();
   

      File file = new File( testdir, rootname + "allzero");
      System.out.println( file.getName() + "...");
      FileChannel fc = new RandomAccessFile( file, "rw" ).getChannel();
      long size = 1025*1024*9;
      long position = 0;
      while ( position < size ) {
        long remaining = size - position;
        if ( remaining > 1024000 ) remaining = 1024000;
        byte[] buffer = new byte[(int)remaining];
        Arrays.fill( buffer , (byte)0 );
        ByteBuffer bb = ByteBuffer.wrap( buffer );
        position += fc.write( bb );
      }
      fc.close();
      
      
      
      for (int i=0; i < sizes.length; i++) {
        size = sizes[i];
        file = new File( testdir, rootname + String.valueOf( size ));
        System.out.println( file.getName() + "...");
        fc = new RandomAccessFile( file, "rw" ).getChannel();
        
        position = 0;
        while ( position < size ) {
          long remaining = size - position;
          if ( remaining > 1024000 ) remaining = 1024000;
          byte[] buffer = new byte[(int)remaining];
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
    
    SHA1 sha1Jmule = new SHA1();
    SHA1Az sha1Gudy = new SHA1Az();
    
    ByteBuffer dBuffer = ByteBuffer.allocateDirect( BUFF_MAX_SIZE );
    ByteBuffer fBuffer = ByteBuffer.allocateDirect( 20 );
    
    fBuffer.order(ByteOrder.BIG_ENDIAN);
    
    for (int i=0; i < BUFF_MAX_SIZE; i++) {
      byte b = (byte)(Math.random() * 255);
      dBuffer.put( b );
    }
    
    //allow time for setting thread to high-priority
    try { Thread.sleep(10000); } catch (Exception ignore) {}
    

    for (int t=0; t < TESTS.length; t++) {
      
      int buffsize = TESTS[t] * 1024;
      dBuffer.limit( buffsize );
      
      int loops = LOOPS[t];
    
    	String info = " [" + buffsize/1024 + "KB, " + loops + "x] = ";
      
    	float totalMBytes = ((float)buffsize / (1024 * 1024)) * loops;
    
    
    	System.out.print("JMule SHA1");
    	long jds = System.currentTimeMillis();
    	for (int i=0; i < loops; i++) {
    		dBuffer.position(0);
        dBuffer.limit( buffsize );
        fBuffer.position(0);
        sha1Jmule.update( dBuffer );
    		sha1Jmule.finalDigest( fBuffer );
    	}
    	long jde = System.currentTimeMillis();
  
    	long jdt = (jde - jds);
    	float jdspeed = totalMBytes / (jdt / 1000);
      
    	System.out.println(info + jdt + " ms @ " + jdspeed + " MB/s");

    
    	System.out.print("Gudy SHA1 ");
    	long gds = System.currentTimeMillis();
    	for (int i=0; i < loops; i++) {
        dBuffer.position(0);
        dBuffer.limit( buffsize );
    		sha1Gudy.reset();
        sha1Gudy.update( dBuffer );
    		sha1Gudy.digest();
    	}
    	long gde = System.currentTimeMillis();
    
    	long gdt = (gde - gds);
    	float gdspeed = totalMBytes / (gdt / 1000);
    	System.out.println(info + gdt + " ms @ " + gdspeed + " MB/s");
   
      System.out.println();
    
    }
    
    System.out.println("DONE");
    
    
	}

}
