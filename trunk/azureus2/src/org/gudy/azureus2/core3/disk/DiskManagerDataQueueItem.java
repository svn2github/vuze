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
public interface 
DiskManagerDataQueueItem 
{
	public boolean isLoaded();
	
	public boolean isLoading();
		
	public ByteBuffer getBuffer();
  
	public void setBuffer(ByteBuffer buffer);
	
	public DiskManagerRequest getRequest();
}
