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

public class 
TOTorrentAnnounceURLGroupImpl
implements TOTorrentAnnounceURLGroup 
{
	protected TOTorrentAnnounceURLSet[]		sets;
	
	protected
	TOTorrentAnnounceURLGroupImpl()
	{
		sets = new TOTorrentAnnounceURLSet[0];
	}

	protected void
	addSet(
		TOTorrentAnnounceURLSet	set )
	{
		TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[sets.length+1];
		
		System.arraycopy( sets, 0, new_sets, 0, sets.length );
		
		new_sets[new_sets.length-1] = set;
		
		sets = new_sets;
	}
	
	public TOTorrentAnnounceURLSet[]
	getAnnounceURLSets()
	{
		return( sets );
	}
	
	public void
	setAnnounceURLSets(
		TOTorrentAnnounceURLSet[]	_sets )
	{
		sets = _sets;	
	}
		
	
	public TOTorrentAnnounceURLSet
	createAnnounceURLSet(
		URL[]	urls )
	{
		return( new TOTorrentAnnounceURLSetImpl( urls ));	
	}
	
}
