/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core2;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core.Request;

/**
 * @author Olivier
 * 
 */
public class DataQueueItem {
  private Request request;
  
  private ByteBuffer buffer;
  private boolean loading;
  
  public DataQueueItem(Request request)
  {
    this.request = request;
  }
  
  public boolean isLoaded()
  {
    return (buffer != null);
  }
  
  /**
   * @return
   */
  public boolean isLoading() {
    return loading;
  }

  /**
   * @param b
   */
  public void setLoading(boolean b) {
    loading = b;
  }

  /**
   * @return
   */
  public ByteBuffer getBuffer() {
    return buffer;
  }

  /**
   * @return
   */
  public Request getRequest() {
    return request;
  }

  /**
   * @param buffer
   */
  public void setBuffer(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  /**
   * @param request
   */
  public void setRequest(Request request) {
    this.request = request;
  }

}
