/*
 * Created on 12 juin 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 
 * This class is a singleton that handles allocation of direct ByteBuffers.
 * It allways tries to find a free buffer before creating a new one.
 * 
 * @author Olivier
 *
 */
public class ByteBufferPool {

  public final static int componentID = 3;
  public final static int evtAllocation = 0; // Allocation Info    

  private static ByteBufferPool pool;
  

  // Here's the logic of SIZE, piece packet are assumed to be
  // the biggest packet (only bitfield could be bigger, but that'd require
  // a torrent of more than 65536 * 8 pieces)
  // 4 : length in BT protocol (int)
  // 1 : command in BT protocol (byte)
  // 4 : pieceNumber in BT protocol (int)
  // 4 : offset in BT protocol (int) 
  
  private static final int LENGTH_8K = 8192;
  private static final int LENGTH_16K = 16384;
  private static final int LENGTH_32K = 32768;
  private static final int LENGTH_64K = 65536;
     
  private static final int SIZE_8K = (4 + 1 + 4 + 4 + LENGTH_8K + 1);
  private static final int SIZE_16K = (4 + 1 + 4 + 4 + LENGTH_16K + 1);
  private static final int SIZE_32K = (4 + 1 + 4 + 4 + LENGTH_32K + 1);
  private static final int SIZE_64K = (4 + 1 + 4 + 4 + LENGTH_64K + 1);
  
  public static final int MAX_SIZE = SIZE_64K;

  private static final int INITIAL_CAPACITY = 1009;
  private ArrayList freeBuffers8k;
  private ArrayList freeBuffers16k;
  private ArrayList freeBuffers32k;
  private ArrayList freeBuffers64k;  
  
  private ByteBufferPool() {
    freeBuffers8k = new ArrayList(INITIAL_CAPACITY);
    freeBuffers16k = new ArrayList(INITIAL_CAPACITY);
    freeBuffers32k = new ArrayList(INITIAL_CAPACITY);
    freeBuffers64k = new ArrayList(INITIAL_CAPACITY);
  }

  private ByteBuffer allocateNewBuffer(int size) {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(size);
      return buffer;
    }
    catch (OutOfMemoryError e) {
       System.out.println("Running garbage collector...");
       System.runFinalization();
       System.gc();
       try {
          ByteBuffer buffer = ByteBuffer.allocateDirect(size);
          return buffer;
       } catch (OutOfMemoryError ex) {
         System.out.println("Memory allocation failed: Out of direct memory space");
          System.out.println("TO FIX: Use the -XX:MaxDirectMemorySize=xxxxxx command line option.");
          return null;
       }
    }
  }

  public static synchronized ByteBufferPool getInstance() {
    if (pool != null)
      return pool;
    pool = new ByteBufferPool();
    return pool;
  }

  public ByteBuffer getFreeBuffer(int length) {
    int size = 0;
    ArrayList buffPool = null;
    if(length <= SIZE_8K) {
      size = SIZE_8K;
      buffPool = freeBuffers8k;
    } else if(length <= SIZE_16K) {
      size = SIZE_16K;
      buffPool = freeBuffers16k;
    } else if(length <= SIZE_32K) {
      size = SIZE_32K; 
      buffPool = freeBuffers32k;
    } else if(length <= SIZE_64K) {
      size = SIZE_64K;
      buffPool = freeBuffers64k;
    }
    
    if(size == 0 || buffPool == null) {
      System.out.println("ByteBufferPool::getFreeBuffer:: null pool OR requested length too big: " + length);
      return null;      
    }
        
    synchronized (this) {
      ByteBuffer buff;
      if (buffPool.size() == 0) {
        buff = allocateNewBuffer(size);
      }
      else {
        buff = (ByteBuffer) buffPool.remove(buffPool.size() - 1);
      }
      buff.clear();   //scrub the buffer
      return buff;
    }
    
  }

  public void freeBuffer(ByteBuffer buffer) {
    
    synchronized (this) {
      if (buffer.capacity() == SIZE_8K) {
        if(!freeBuffers8k.contains(buffer))
          freeBuffers8k.add(buffer);
      } else if(buffer.capacity() == SIZE_16K) {
        if(!freeBuffers16k.contains(buffer))
          freeBuffers16k.add(buffer);      
      } else if(buffer.capacity() == SIZE_32K) {
        if(!freeBuffers32k.contains(buffer))
          freeBuffers32k.add(buffer);      
      } else if(buffer.capacity() == SIZE_64K) {
        if(!freeBuffers64k.contains(buffer))
          freeBuffers64k.add(buffer);      
      } else {
        System.out.println("Wrong buffer passed back to ByteBufferPool::freeBuffer");
        return;
      }       
    }
  }


}
