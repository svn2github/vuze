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
  private ArrayList usedBuffers;
  private ArrayList freeBuffers;

  // Here's the logic of SIZE, piece packet are assumed to be
  // the biggest packet (only bitfield could be bigger, but that'd require
  // a torrent of more than 32768 * 8 pieces)
  // 1 : free or not
  // 4 : length in BT protocol (int)
  // 1 : command in BT protocol (byte)
  // 4 : pieceNumber in BT protocol (int)
  // 4 : offset in BT protocol (int)    
  private static final int SIZE = (1 + 4 + 1 + 4 + 4 + 65536);

  // The pool initial capacity
  // as allocating a direct Bytebuffer may requires some time, this is important
  // that pool isn't too small
  // Let's assume 1Mb initial mem use is not a problem
  // 32k a buffer, that's 40 buffers
  private static final int INITIAL_CAPACITY = 1009;

  private ByteBufferPool() {
    usedBuffers = new ArrayList(INITIAL_CAPACITY);
    freeBuffers = new ArrayList(INITIAL_CAPACITY);
  }

  private ByteBuffer allocateNewBuffer() {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE+1);
      usedBuffers.add(buffer);
      //System.out.println("Pool Size :" + buffers.size());
      return buffer;
    } catch (Exception e) {
      System.out.println("Memory allocation failed:");
      e.printStackTrace();
      System.out.println("freeMemory=" + Runtime.getRuntime().freeMemory() + ", totalMemory=" + Runtime.getRuntime().totalMemory() + ", maxMemory=" + Runtime.getRuntime().maxMemory());
      return null;
    }
  }

  public static synchronized ByteBufferPool getInstance() {
    if (pool != null)
      return pool;
    pool = new ByteBufferPool();
    return pool;
  }

  public synchronized ByteBuffer getFreeBuffer() {
    if(freeBuffers.size() == 0) {
      return allocateNewBuffer();
    } else {
      return (ByteBuffer) freeBuffers.remove(freeBuffers.size()-1);
    }
  }

  public synchronized void freeBuffer(ByteBuffer buffer) {
    if(usedBuffers.remove(buffer)) {
      buffer.clear();
      freeBuffers.add(buffer);
    }
  }

  public synchronized void clearFreeBuffers() {
    freeBuffers.clear();
  }
}
