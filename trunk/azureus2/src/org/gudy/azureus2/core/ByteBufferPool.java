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
  private ArrayList buffers;

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
  private static final int INITIAL_CAPACITY = 0;

  private ByteBufferPool() {
    buffers = new ArrayList(INITIAL_CAPACITY);
  }

  private ByteBufferPool(int initialCapacity) {
    this();
    for (int i = 0; i < initialCapacity; i++)
      allocateNewBuffer();
  }

  private ByteBuffer allocateNewBuffer() {
    Logger.getLogger().log(
      componentID,
      evtAllocation,
      Logger.INFORMATION,
      "Allocating new Buffer -- Current Pool Size : "
        + (buffers.size() + 1)
        + " buffers");
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE+1);
      buffers.add(buffer);
      return buffer;
    } catch (Exception e) {
      Logger.getLogger().log(
        componentID,
        evtAllocation,
        Logger.ERROR,
        "Memory allocation failed. Reason : " + e);
      return null;
    }
  }

  public static synchronized ByteBufferPool getInstance() {
    if (pool != null)
      return pool;
    pool = new ByteBufferPool(INITIAL_CAPACITY);
    return pool;
  }

  public synchronized ByteBuffer getFreeBuffer() {
    Logger.getLogger().log(
      componentID,
      evtAllocation,
      Logger.INFORMATION,
      "Giving 1 free Buffer");
    //return ByteBuffer.allocateDirect(SIZE);       
    for (int i = 0; i < buffers.size(); i++) {
      ByteBuffer buffer = (ByteBuffer) buffers.get(i);
      if (buffer.position() == SIZE) {
        buffer.position(0);
        return buffer;
      }
    }
    ByteBuffer buffer = allocateNewBuffer();
    buffer.position(0);
    return buffer;
  }

  public synchronized void freeBuffer(ByteBuffer buffer) {
    int index = buffers.indexOf(buffer);
		if(index == -1) return;
/*
    try {
		if(index == -1) throw new Exception("freeBuffer: buffer not registered");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
*/
    Logger.getLogger().log(
      componentID,
      evtAllocation,
      Logger.INFORMATION,
      "Freeing 1 buffer");
    buffer.limit(buffer.capacity());
    buffer.position(SIZE);
  }
}
