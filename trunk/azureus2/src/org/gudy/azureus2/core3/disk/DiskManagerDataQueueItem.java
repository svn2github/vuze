/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.disk;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;


/**
 * @author Olivier
 * 
 */
public interface 
DiskManagerDataQueueItem 
{
	public boolean isLoaded();
	
	public boolean isLoading();
		
	public DirectByteBuffer getBuffer();
  
	public void setBuffer(DirectByteBuffer buffer);
	
	public DiskManagerRequest getRequest();
}
