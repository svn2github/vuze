/*
 * File    : DiskManagerDataQueueItemImpl.java
 * Created : 18-Oct-2003
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.disk.impl;

/*
 * Created on 4 juil. 2003
 *
 */
 

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.disk.*;


/**
 * @author Olivier
 * 
 */
public class 
DiskManagerDataQueueItemImpl
	implements DiskManagerDataQueueItem 
{
  private DiskManagerRequest request;
  
  private ByteBuffer buffer;
  private boolean loading;
  
  public DiskManagerDataQueueItemImpl(DiskManagerRequest request)
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
