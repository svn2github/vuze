/*
 * Created on Jan 30, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
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
public class DirectByteBufferPool {

  private static DirectByteBufferPool pool;
  
  // There is no point in allocating buffers smaller than 4K,
  // as direct ByteBuffers are page-aligned to the underlying
  // system, which is 4096 byte pages under most OS's.
  // If we want to save memory, we can distribute smaller-than-4K
  // buffers by using the slice() method to break up a standard buffer
  // into smaller chunks, but that's more work.
  private static final int START_POWER = 12;    // 4096
  private static final int END_POWER   = 25;    // 33554432
  
  public static final int MAX_SIZE = BigInteger.valueOf(2).pow(END_POWER).intValue();
  
  private final Map buffersMap;
  private final Object poolsLock;
  private final Timer compactionTimer;
  
  private static final long COMPACTION_CHECK_PERIOD = 10*60*1000; //10 min
  private static final long MAX_FREE_BYTES = 10*1024*1024; //10 MB
  
  
  private DirectByteBufferPool() {
    buffersMap = new LinkedHashMap(END_POWER - START_POWER + 1);
    
    //create the buffer pool for each buffer size
    for (int p=START_POWER; p <= END_POWER; p++) {
        Integer size = new Integer(BigInteger.valueOf(2).pow(p).intValue());
        ArrayList bufferPool = new ArrayList();
        buffersMap.put(size, bufferPool);
    }

    poolsLock = new Object();
    
    //initiate periodic timer to check free memory usage
    compactionTimer = new Timer("BufferPool Checker");
    compactionTimer.addPeriodicEvent(
        COMPACTION_CHECK_PERIOD,
        new TimerEventPerformer() {
          public void perform( TimerEvent ev ) {
            checkMemoryUsage();
          }
        }
     );

  }

  
  /**
   * Allocate and return a new direct ByteBuffer.
   */
  private ByteBuffer allocateNewBuffer(final int _size) {
    try {
      return ByteBuffer.allocateDirect(_size);
    }
    catch (OutOfMemoryError e) {
       Debug.out("Running garbage collector...");
       clearBufferPools();
       runGarbageCollection();

       try {
          return ByteBuffer.allocateDirect(_size);
       } catch (OutOfMemoryError ex) {
       	 Debug.out("Memory allocation failed: Out of direct memory space\n"
                  + "TO FIX: Use the -XX:MaxDirectMemorySize=512m command line option.");
          return null;
       }
    }
  }

  
  private static synchronized DirectByteBufferPool getInstance() {
    if (pool == null)  pool = new DirectByteBufferPool();
    return pool;
  }

  
  /**
   * Retrieve a buffer from the buffer pool of size at least
   * <b>length</b>, and no larger than <b>DirectByteBufferPool.MAX_SIZE</b>
   */
  public static ByteBuffer getFreeBuffer(final int _length) {
    if (_length < 1) {
        Debug.out("requested length [" +_length+ "] < 1");
        return null;
    }

    if (_length > MAX_SIZE) {
        Debug.out("requested length [" +_length+ "] > MAX_SIZE [" +MAX_SIZE+ "]");
        return null;
    }

    return DirectByteBufferPool.getInstance().getBuffer(_length);
  }
  
  
  /**
   * Retrieve an appropriate buffer from the free pool, or
   * create a new one if the pool is empty.
   */
  private ByteBuffer getBuffer(final int _length) {
    Integer reqVal = new Integer(_length);
    
    //loop through the buffer pools to find a buffer big enough
    Iterator it = buffersMap.keySet().iterator();
    while (it.hasNext()) {
      Integer keyVal = (Integer)it.next();

      //check if the buffers in this pool are big enough
      if (reqVal.compareTo(keyVal) <= 0) {
        ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
            
        ByteBuffer buff;
        synchronized ( poolsLock ) { //make sure we don't remove a buffer when running compaction
          //if there are no free buffers in the pool, create a new one.
          //otherwise use one from the pool
          if (bufferPool.isEmpty()) {
            buff = allocateNewBuffer(keyVal.intValue());
          }
          else {
            synchronized ( bufferPool ) {
              buff = (ByteBuffer)bufferPool.remove(bufferPool.size() - 1);
            }
          }
        }
        buff.clear();   //scrub the buffer
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
  public static void freeBuffer(ByteBuffer _buffer) {
    DirectByteBufferPool.getInstance().free(_buffer);
  }
  
  
  /**
   * Return the given buffer to the appropriate pool.
   */
  private void free(ByteBuffer _buffer) {
    Integer buffSize = new Integer(_buffer.capacity());
    
    ArrayList bufferPool = (ArrayList)buffersMap.get(buffSize);
    
    if (bufferPool != null) {
      //no need to sync around 'poolsLock', as adding during compaction is ok
      synchronized ( bufferPool ) {
        bufferPool.add(_buffer);
      }
    }
    else {
      Debug.out("Invalid buffer given; could not find proper buffer pool");
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
  
  
  /**
   * Checks memory usage of free buffers in buffer pools,
   * and calls the compaction method if necessary.
   */
  private void checkMemoryUsage() {
    long bytesUsed = 0;
    
    synchronized( poolsLock ) {
      
      //count up total bytes used by free buffers
      Iterator it = buffersMap.keySet().iterator();
      while (it.hasNext()) {
        Integer keyVal = (Integer)it.next();
        ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
      
        bytesUsed += keyVal.intValue() * bufferPool.size();
      }
      
      //compact buffer pools if they use too much memory
      if (bytesUsed > MAX_FREE_BYTES) {
        compactFreeBuffers(bytesUsed);
      }
      
    }
  }
  
  
  /**
   * Fairly removes free buffers from the pools to limit memory usage.
   */
  private void compactFreeBuffers(final long bytes_used) {
    final int numPools = buffersMap.size();
    long bytesToFree = 0;
    int maxPoolSize = 0;
    
    int[] buffSizes = new int[numPools];
    int[] poolSizes = new int[numPools];
    int[] numToFree = new int[numPools];

    
    //fill size arrays
    int pos = 0;
    Iterator it = buffersMap.keySet().iterator();
    while (it.hasNext()) {
      Integer keyVal = (Integer)it.next();
      ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
      
      buffSizes[pos] = keyVal.intValue();
      poolSizes[pos] = bufferPool.size();
      numToFree[pos] = 0;
      
      //find initial max value
      if (poolSizes[pos] > maxPoolSize) maxPoolSize = poolSizes[pos];
      
      pos++;
    }
    
    //calculate the number of buffers to free from each pool
    while( bytesToFree < (bytes_used - MAX_FREE_BYTES) ) {
      for (int i=0; i < numPools; i++) {
        //if the pool size is as large as the current max size
        if (poolSizes[i] == maxPoolSize) {
          //update counts
          numToFree[i]++;
          poolSizes[i]--;
          bytesToFree += buffSizes[i];
        }
      }
      //reduce max size for next round
      maxPoolSize--;
    }
    
    //free buffers from the pools
    pos = 0;
    it = buffersMap.values().iterator();
    while (it.hasNext()) {
      //for each pool
      ArrayList bufferPool = (ArrayList)it.next();
      synchronized( bufferPool ) {
        int size = bufferPool.size();
        //remove the buffers from the end
        for (int i=(size - 1); i >= (size - numToFree[pos]); i--) {
          bufferPool.remove(i);
        }
      }
      
      pos++;
    }
    
    //force garbage collection if we've free'd more than the max
    //i.e. we had used more than 2X the max
    if (bytesToFree > MAX_FREE_BYTES) {
      runGarbageCollection();
    }
  }

}
