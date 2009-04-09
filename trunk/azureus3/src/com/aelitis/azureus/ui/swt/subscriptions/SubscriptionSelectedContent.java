/*
 * Created on Apr 8, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.subscriptions;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.IconBarEnabler;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;

public class 
SubscriptionSelectedContent 
	extends ToolBarEnablerSelectedContent
{
	private Subscription		subs;
	
	private TOTorrent			torrent;
	
	protected
	SubscriptionSelectedContent(
		IconBarEnabler		_enabler,
		Subscription		_subs )
	{
		super( _enabler );
		
		subs	= _subs;
	}
	
	public Subscription
	getSubscription()
	{
		return( subs );
	}
	
	public String 
	getDisplayName() 
	{
		return( MessageText.getString( "subscriptions.column.name" ) + ": " + subs.getName());
	}
	
	public String
	getHash()
	{
		return( subs.getID());
	}
	
	public TOTorrent
	getTorrent()
	{
		synchronized( this ){
			
			if ( torrent == null ){
				
					// hack alert - we embed the vuze-file into a torrent to allow it to go through
					// the normal share route, then pick it out again when the recipient 'downloads' it
				
				try{
				
					VuzeFile vf = subs.getVuzeFile();
				
					File f1 = AETemporaryFileHandler.createTempFile();
					
					File f = new File( f1.getParent(), "Update Vuze to access this share_" + f1.getName());
					
					f1.delete();
					
					try{
					
						vf.write( f );
					
						TOTorrentCreator cr = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( f, new URL( "dht://" ));
						
						TOTorrent temp = cr.create();
						
						Map	vuze_map 	= vf.exportToMap();
						Map	torrent_map = temp.serialiseToMap();
						
						torrent_map.putAll( vuze_map );
						
						torrent = TOTorrentFactory.deserialiseFromMap( torrent_map );
					}finally{
						
						f.delete();
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		return( torrent );
	}
}
