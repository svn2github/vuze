/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent.impl;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.net.URL;

import org.gudy.azureus2.core3.torrent.*;

public class TOTorrentAnnounceURLSetImpl 
	implements TOTorrentAnnounceURLSet
{
	protected URL[]	urls;
	
	protected
	TOTorrentAnnounceURLSetImpl(
		URL[]		_urls )
	{
		urls	= _urls;
	}
	
	public URL[]
	getAnnounceURLs()
	{
		return( urls );
	}
	
	
	public void
	setAnnounceURLs(
		URL[]	_urls )
	{
		urls	= _urls;
	}
}