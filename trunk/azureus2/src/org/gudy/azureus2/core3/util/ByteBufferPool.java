/*
 * Created on 12 Jun 2003 by Olivier Chalouhi
 *
 * Modified on 15 Jan 2004 by Alon Rohter
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.util.*;
import java.math.*;


/**
 * This class handles allocation of direct ByteBuffers.
 * It always tries to find a free buffer in the buffer pool
 * before creating a new one.
 */
public class ByteBufferPool {

  private static ByteBufferPool pool;
  
  // Here's the logic of ADD_SIZE:
  // 'PIECE' messages contain data, plus:
  // 4 : length in BT protocol (int)
  // 1 : command in BT protocol (byte)
  // 4 : pieceNumber in BT protocol (int)
  // 4 : offset in BT protocol (int) 
  private static final int ADD_SIZE = 4 + 1 + 4 + 4;
  
  private static final int START_POWER = 10;    // 1024
  private static final int END_POWER   = 20;    // 1048576
  
  public static final int MAX_SIZE = BigInteger.valueOf(2).pow(END_POWER).intValue() + ADD_SIZE;
  
  private final Map buffersMap;
  
  //private int maxFree = 0;
  //private long bytesAllocated = 0;
  //private long bytesSent = 0;
  //private long bytesReturned = 0;
  
  
  private ByteBufferPool() {
    buffersMap = new LinkedHashMap(END_POWER - START_POWER + 1);
    
    //create the buffer pool for each buffer size
    for (int p=START_POWER; p <= END_POWER; p++) {
        Integer size = new Integer(BigInteger.valueOf(2).pow(p).intValue() + ADD_SIZE);
        ArrayList bufferPool = new ArrayList();
        buffersMap.put(size, bufferPool);
    }
  }

  
  /**
   * Allocate and return a new direct ByteBuffer.
   */
  private ByteBuffer allocateNewBuffer(int size) {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(size);
      //bytesAllocated += size;
      return buffer;
    }
    catch (OutOfMemoryError e) {
       Debug.out("Running garbage collector...");
       clearBufferPools();
       runGarbageCollection();

       try {
          ByteBuffer buffer = ByteBuffer.allocateDirect(size);
          return buffer;
       } catch (OutOfMemoryError ex) {
       	 Debug.out("Memory allocation failed: Out of direct memory space\n"
                  + "TO FIX: Use the -XX:MaxDirectMemorySize=xxxxxx command line option.");
          return null;
       }
    }
  }

  
  private static synchronized ByteBufferPool getInstance() {
    if (pool == null) {
    	pool = new ByteBufferPool();
    }
    return pool;
  }

  
  /**
   * Retrieve a buffer from the buffer pool of size at least
   * <b>length</b>, and no larger than <b>ByteBufferPool.MAX_SIZE</b>
   */
  public static ByteBuffer getFreeBuffer(int length) {
    if (length < 1) {
        Debug.out("requested length [" +length+ "] < 1");
        return null;
    }

    if (length > MAX_SIZE) {
        Debug.out("requested length [" +length+ "] > MAX_SIZE [" +MAX_SIZE+ "]");
        return null;
    }

    ByteBufferPool bbp = ByteBufferPool.getInstance();
    return bbp.getBuffer(length);
  }
  
  
  /**
   * Retrieve an appropriate buffer from the free pool, or
   * create a new one if the pool is empty.
   */
  private ByteBuffer getBuffer(int length) {
    //System.out.print("requested = " + length);
    
    Integer reqVal = new Integer(length);

    //loop through the buffer pools to find a buffer big enough
    Iterator it = buffersMap.keySet().iterator();
    while (it.hasNext()) {
        Integer keyVal = (Integer)it.next();

        //check if the buffers in this pool are big enough
        if (reqVal.compareTo(keyVal) <= 0) {
            ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
            
            ByteBuffer buff;
            synchronized (bufferPool) {
                //if there are no free buffers in the pool, create a new one.
                //otherwise use one from the pool
                if (bufferPool.isEmpty()) {
                    buff = allocateNewBuffer(keyVal.intValue());
                }
                else {
                    buff = (ByteBuffer)bufferPool.remove(bufferPool.size() - 1);
                }
            }
            buff.clear();   //scrub the buffer
            //System.out.println(" , given = " + buff.capacity());
            //bytesSent += buff.capacity();
            return buff;
        }
    }
    
    //we should never get here
    Debug.out("Unable to find an appropriate buffer pool");
    return null;
  }

  
  /**
   * Return a buffer to the buffer pool.
   * *** Remember to set the local buffer reference to
   * null after returning the buffer to the pool ***
   */
  public static void freeBuffer(ByteBuffer buffer) {
    ByteBufferPool bbp = ByteBufferPool.getInstance();
    bbp.free(buffer);
  }
  
  
  /**
   * Return the given buffer to the appropriate pool.
   */
  private void free(ByteBuffer buffer) {
    Integer buffSize = new Integer(buffer.capacity());
    
    //bytesReturned += buffer.capacity();
    
    ArrayList bufferPool = (ArrayList)buffersMap.get(buffSize);
    
    synchronized (bufferPool) {
    	if (bufferPool != null) {
    		bufferPool.add(buffer);
        
        //System.out.print("returned buffer of " +buffer.capacity());
        //System.out.print(" , " +bufferPool.size()+ " free buffers in pool");
        //if (bufferPool.size() > maxFree) maxFree = bufferPool.size();
        //System.out.println(" , max free buffers ever = " +maxFree);
        
        //System.out.println("bytes sent = "+bytesSent+", returned = "+bytesReturned+", allocated = "+bytesAllocated);
        
    	}
    	else {
        Debug.out("Invalid buffer given; could not find proper buffer pool");
    	}
    }
  }
  
  
  /**
   * Clears the free buffer pools so that currently
   * unused buffers can be garbage collected.
   */
  private void clearBufferPools() {
    Iterator it = buffersMap.values().iterator();
    while (it.hasNext()) {
        ArrayList bufferPool = (ArrayList)it.next();
        bufferPool.clear();
    }
  }
  
  
  /**
   * Force system garbage collection.
   */
  private void runGarbageCollection() {
    System.runFinalization();
    System.gc();
  }


}
