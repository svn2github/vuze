/*
 * Created on 12 juin 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core;

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
  private ArrayList freeBuffers;

  // Here's the logic of SIZE, piece packet are assumed to be
  // the biggest packet (only bitfield could be bigger, but that'd require
  // a torrent of more than 65536 * 8 pieces)
  // 4 : length in BT protocol (int)
  // 1 : command in BT protocol (byte)
  // 4 : pieceNumber in BT protocol (int)
  // 4 : offset in BT protocol (int)    
  private static final int SIZE = (4 + 1 + 4 + 4 + 65536 + 1);

  private static final int INITIAL_CAPACITY = 1009;

  private ByteBufferPool() {
    freeBuffers = new ArrayList(INITIAL_CAPACITY);
  }

  private ByteBuffer allocateNewBuffer() {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE);
      return buffer;
    }
    catch (OutOfMemoryError e) {
       System.out.println("Running garbage collector...");
       System.runFinalization();
       System.gc();
       try {
          ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE);
          return buffer;
       } catch (OutOfMemoryError ex) {
          System.out.println("Memory allocation failed: Out of direct memory space (max=" + sun.misc.VM.maxDirectMemory() + ")");
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

  public ByteBuffer getFreeBuffer() {
    //return ByteBuffer.allocate(SIZE);
    synchronized (this) {
      if (freeBuffers.size() == 0) {
        return allocateNewBuffer();
      }
      else {
        return (ByteBuffer) freeBuffers.remove(freeBuffers.size() - 1);
      }
    }
    
  }

  public void freeBuffer(ByteBuffer buffer) {
    
    synchronized (this) {
      if (buffer.capacity() != SIZE) {
        System.out.println("Wrong buffer passed back to ByteBufferPool::freeBuffer");
        return;
      }       

      if(!freeBuffers.contains(buffer))
        freeBuffers.add(buffer);
    }
  }

  public synchronized void clearFreeBuffers() {
    //Gudy : Really wrong to want to free those ...
    //freeBuffers.clear();
  }
}
