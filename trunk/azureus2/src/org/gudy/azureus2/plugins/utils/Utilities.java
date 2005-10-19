/*
 * File    : Utilities.java
 * Created : 24-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.plugins.utils;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.utils.resourceuploader.ResourceUploaderFactory;
import org.gudy.azureus2.plugins.utils.security.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;
import org.gudy.azureus2.plugins.utils.xml.rss.*;

public interface 
Utilities 
{
	public String
	getAzureusUserDir();
	
	public String
	getAzureusProgramDir();
	
	public boolean
	isCVSVersion();
	
	public boolean
	isWindows();
	
	public boolean
	isLinux();
	
	public boolean
	isOSX();
	
	public InputStream
	getImageAsStream(
		String	image_name );
	
	public Semaphore
	getSemaphore();
  
    public Monitor getMonitor();
	
	public ByteBuffer
	allocateDirectByteBuffer(
		int		size );
	
	public void
	freeDirectByteBuffer(
		ByteBuffer	buffer );
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		int		size );
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		byte[]	data  );
	
	public Formatters
	getFormatters();
	
	public LocaleUtilities
	getLocaleUtilities();
	
	public UTTimer
	createTimer(
		String		name );

	public UTTimer
	createTimer(
		String		name,
		boolean		lightweight );
	
		/**
		 * create and run a thread for the target. This will be a daemon thread so that
		 * its existence doesn't interfere with Azureus closedown
		 * @param name
		 * @param target
		 */
	
	public void
	createThread(
		String		name,
		Runnable	target );
	
		/**
		 * create a child process and executes the supplied command line. The child process
		 * will not inherit any open handles on Windows, which does happen if Runtime is
		 * used directly. This relies on the Platform plugin, if this is not installed then
		 * this will fall back to using Runtime.exec 
		 * @param command_line
		 */
	
	public void
	createProcess(
		String		command_line )
	
		throws PluginException;
	
	public ResourceDownloaderFactory
	getResourceDownloaderFactory();
	
	public ResourceUploaderFactory
	getResourceUploaderFactory();

	public SESecurityManager
	getSecurityManager();
	
	public SimpleXMLParserDocumentFactory
	getSimpleXMLParserDocumentFactory();
	
	public RSSFeed
	getRSSFeed(
		URL		feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException;
	
	public RSSFeed
	getRSSFeed(
		ResourceDownloader	feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException;
	
		/**
		 * Returns a public IP address of the machine or null if it can't be determined
		 */
	
	public InetAddress
	getPublicAddress();
	
		/**
		 * attempts a reverse DNS lookup of an address, null if it fails
		 * @param address
		 * @return
		 */
	
	public String
	reverseDNSLookup(
		InetAddress		address );
  
  
  /**
   * Get the current system time, like System.currentTimeMillis(),
   * only the time lookup is cached for performance reasons.
   * @return current system time
   */
	public long getCurrentSystemTime();
  
  	public ByteArrayWrapper
	createWrapper(
		byte[]		data );
  	
  	
  	/**
  	 * create a dispatcher that will queue runnable items until either the limit
  	 * is reached or the dispatcher hasn't had an entry added for the defined idle time
  	 * @param idle_dispatch_time	milliseconds
  	 * @param max_queue_size		0 -> infinite
  	 * @return
  	 */
  	
  	public AggregatedDispatcher
	createAggregatedDispatcher(
		long	idle_dispatch_time,
		long	max_queue_size );
  	
 	public AggregatedList
	createAggregatedList(
		AggregatedListAcceptor	acceptor,
		long					idle_dispatch_time,
		long					max_queue_size );
 	
 	public Map
 	readResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		boolean	use_backup );
 	
	public void
 	writeResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		Map		data,
 		boolean	use_backup );	
}


