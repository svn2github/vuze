/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.net.URL;

public interface 
TOTorrentAnnounceURLGroup 
{
	public TOTorrentAnnounceURLSet[]
	getAnnounceURLSets();
	
	public void
	setAnnounceURLSets(
		TOTorrentAnnounceURLSet[]	sets );
		
		/**
		 * This method will create a new set. It is not added into the current set, this 
		 * must be done by the caller
		 *  
		 * @param urls the URLs for the new set
		 * @return	the newly created set
		 */
		
	public TOTorrentAnnounceURLSet
	createAnnounceURLSet(
		URL[]	urls );
}
