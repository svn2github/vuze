/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.disk;

import java.nio.ByteBuffer;


/**
 * @author Olivier
 * 
 */
public class DiskManagerDataQueueItem {
  private DiskManagerRequest request;
  
  private ByteBuffer buffer;
  private boolean loading;
  
  public DiskManagerDataQueueItem(DiskManagerRequest request)
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
  public DiskManagerRequest getRequest() {
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
  public void setRequest(DiskManagerRequest request) {
    this.request = request;
  }

}
