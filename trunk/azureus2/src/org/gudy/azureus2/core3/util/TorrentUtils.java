/*
 * File    : TorrentUtils.java
 * Created : 13-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.*;

public class 
TorrentUtils 
{
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				return( new HashMap());
			}
		};
		
	
		/**
		 * If you set "create_delegate" to true then you must understand that this results
		 * is piece hashes being discarded and then re-read from the torrent file if needed
		 * Therefore, if you delete the original torrent file you're going to get errors
		 * if you access the pieces after this (and they've been discarded)
		 * @param file
		 * @param create_delegate
		 * @return
		 * @throws TOTorrentException
		 */
	
	public static TOTorrent
	readFromFile(
		File		file,
		boolean		create_delegate )
		
		throws TOTorrentException
	{
		TOTorrent torrent;
   
		try{
			torrent = TOTorrentFactory.deserialiseFromBEncodedFile(file);
			
		}catch (TOTorrentException e){
      
			Debug.printStackTrace( e );
			
			File torrentBackup = new File(file.getParent(), file.getName() + ".bak");
			
			if( torrentBackup.exists()){
				
				torrent = TOTorrentFactory.deserialiseFromBEncodedFile(torrentBackup);
				
					// use the original torrent's file name so that when this gets saved
					// it writes back to the original and backups are made as required
					// - set below
			}else{
				
				throw e;
			}
		}
				
		torrent.setAdditionalStringProperty("torrent filename", file.toString());
		
		if ( create_delegate ){
			
			return( new torrentDelegate( torrent, file ));
			
		}else{
			
			return( torrent );
		}
	}

	public static void
	writeToFile(
		final TOTorrent		torrent )
	
		throws TOTorrentException 
	{
	   try{
	   		torrent.getMonitor().enter();
	    	
	   			// we've got to re-obtain the pieces here in case they've been thrown
	   			// away to save memory *before* we rename the torrent file!
	   		
	   		torrent.getPieces();
	   		
	    	String str = torrent.getAdditionalStringProperty("torrent filename");
	    	
	    	if (str == null){
	    		
	    		throw (new TOTorrentException("TorrentUtils::writeToFile: no 'torrent filename' attribute defined", TOTorrentException.RT_FILE_NOT_FOUND));
	    	}
	    		    	
	    	File torrent_file = new File(str);
	    	
	    	if (COConfigurationManager.getBooleanParameter("Save Torrent Backup", false) && torrent_file.exists()) {
	    		
	    		File torrent_file_bak = new File(str + ".bak");
	    		
	    		try{
	    			
	    			if (torrent_file_bak.exists()){
	    				
	    				torrent_file_bak.delete();
	    			}
	    			
	    			torrent_file.renameTo(torrent_file_bak);
	    			
	    		}catch( SecurityException e){
	    			
	    			Debug.printStackTrace( e );
	    		}
	    	}
	      
	    	torrent.serialiseToBEncodedFile(torrent_file);
			
	   	}finally{
	   		
	   		torrent.getMonitor().exit();
	   	}
	}
	
	public static void
	writeToFile(
		TOTorrent		torrent,
		File			file )
	
		throws TOTorrentException 
	{		
		torrent.setAdditionalStringProperty("torrent filename", file.toString());
		
		writeToFile( torrent );
	}
	
	public static void
	delete(
		TOTorrent 		torrent )
	
		throws TOTorrentException 
	{
	   try{
	   		torrent.getMonitor().enter();
	    	
	    	String str = torrent.getAdditionalStringProperty("torrent filename");
	    	
	    	if ( str == null ){
	    		
	    		throw( new TOTorrentException("TorrentUtils::delete: no 'torrent filename' attribute defined", TOTorrentException.RT_FILE_NOT_FOUND));
	    	}
	    	
	    	if ( !new File(str).delete()){
	    		
	    		throw( new TOTorrentException("TorrentUtils::delete: failed to delete '" + str + "'", TOTorrentException.RT_WRITE_FAILS));
	    	}
		
	    	new File( str + ".bak" ).delete();
	    	
	    }finally{
	    	
	    	torrent.getMonitor().exit();
	    }
	}
	
	public static String
	exceptionToText(
		TOTorrentException	e )
	{
		return( exceptionToText( e, false ));
	}
	
	public static String
	exceptionToText(
		TOTorrentException	e,
		boolean				verbose )
	{
		String	errorDetail;
		
		int	reason = e.getReason();
  			
		if ( reason == TOTorrentException.RT_FILE_NOT_FOUND ){
 	     	        		 		
			errorDetail = MessageText.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
	        		
		}else if ( reason == TOTorrentException.RT_ZERO_LENGTH ){
	     
			errorDetail = MessageText.getString("DownloadManager.error.fileempty"); //$NON-NLS-1$
	        
		}else if ( reason == TOTorrentException.RT_TOO_BIG ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.filetoobig"); //$NON-NLS-1$
			        
		}else if ( reason == TOTorrentException.RT_DECODE_FAILS ){
	 
			errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo" ) + " (" + e.getMessage() + ")"; 
	 		        
		}else if ( reason == TOTorrentException.RT_UNSUPPORTED_ENCODING ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_READ_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_HASH_FAILS ){
			
			errorDetail = MessageText.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_CANCELLED ){
			
			errorDetail = MessageText.getString("DownloadManager.error.operationcancancelled");
			
		}else{
	 	     
			errorDetail = e.getMessage();
		}
		
		if ( verbose ){
			
			errorDetail += "(" + e.getMessage() + ")";
		}
		
		return( errorDetail );
	}
	
	public static List
	announceGroupsToList(
		TOTorrent	torrent )
	{
		List	groups = new ArrayList();
		
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
		
		if ( sets.length == 0 ){
		
			List	s = new ArrayList();
			
			s.add( torrent.getAnnounceURL().toString());
			
			groups.add(s);
		}else{
			
			for (int i=0;i<sets.length;i++){
			
				List	s = new ArrayList();
								
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				for (int j=0;j<urls.length;j++){
				
					s.add( urls[j].toString());
				}
				
				if ( s.size() > 0 ){
					
					groups.add(s);
				}
			}
		}
		
		return( groups );
	}
	
	public static void
	listToAnnounceGroups(
		List		groups,
		TOTorrent	torrent )
	{
		try{
			TOTorrentAnnounceURLGroup tg = torrent.getAnnounceURLGroup();
			
			if ( groups.size() == 1 ){
				
				List	set = (List)groups.get(0);
				
				if ( set.size() == 1 ){
					
					torrent.setAnnounceURL( new URL((String)set.get(0)));
					
					tg.setAnnounceURLSets( new TOTorrentAnnounceURLSet[0]);
					
					return;
				}
			}
			
			
			Vector	g = new Vector();
			
			for (int i=0;i<groups.size();i++){
				
				List	set = (List)groups.get(i);
				
				URL[]	urls = new URL[set.size()];
				
				for (int j=0;j<set.size();j++){
				
					urls[j] = new URL((String)set.get(j));
				}
				
				if ( urls.length > 0 ){
					
					g.add( tg.createAnnounceURLSet( urls ));
				}
			}
			
			TOTorrentAnnounceURLSet[]	sets = new TOTorrentAnnounceURLSet[g.size()];
			
			g.copyInto( sets );
			
			tg.setAnnounceURLSets( sets );
			
			if ( sets.length == 0 ){
			
					// hmm, no valid urls at all
				
				torrent.setAnnounceURL( new URL( "http://no.valid.urls.defined/announce"));
			}
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		String		first_url )
	{
		try{
			
			announceGroupsInsertFirst( torrent, new URL( first_url ));
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		URL			first_url )
	{
		announceGroupsInsertFirst( torrent, new URL[]{ first_url });
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		URL[]		first_urls )
	{
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[] sets = group.getAnnounceURLSets();

		TOTorrentAnnounceURLSet set1 = group.createAnnounceURLSet( first_urls );
		
		
		if ( sets.length > 0 ){
			
			TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[sets.length+1];
			
			new_sets[0] = set1;
			
			System.arraycopy( sets, 0, new_sets, 1, sets.length );
			
			group.setAnnounceURLSets( new_sets );
					
		}else{
			
			TOTorrentAnnounceURLSet set2 = group.createAnnounceURLSet(new URL[]{torrent.getAnnounceURL()});
			
			group.setAnnounceURLSets(
				new  TOTorrentAnnounceURLSet[]{ set1, set2 });
		}
	}
	
	public static void
	announceGroupsInsertLast(
		TOTorrent	torrent,
		URL[]		first_urls )
	{
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[] sets = group.getAnnounceURLSets();

		TOTorrentAnnounceURLSet set1 = group.createAnnounceURLSet( first_urls );
		
		
		if ( sets.length > 0 ){
			
			TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[sets.length+1];
			
			new_sets[sets.length] = set1;
			
			System.arraycopy( sets, 0, new_sets, 0, sets.length );
			
			group.setAnnounceURLSets( new_sets );
					
		}else{
			
			TOTorrentAnnounceURLSet set2 = group.createAnnounceURLSet(new URL[]{torrent.getAnnounceURL()});
			
			group.setAnnounceURLSets(
				new  TOTorrentAnnounceURLSet[]{ set2, set1 });
		}
	}
		
	public static void
	announceGroupsSetFirst(
		TOTorrent	torrent,
		String		first_url )
	{
		List	groups = announceGroupsToList( torrent );
		
		boolean	found = false;
	
		outer:
		for (int i=0;i<groups.size();i++){
			
			List	set = (List)groups.get(i);
			
			for (int j=0;j<set.size();j++){
		
				if ( first_url.equals(set.get(j))){
			
					set.remove(j);
					
					set.add(0, first_url);
					
					groups.remove(set);
					
					groups.add(0,set);
	
					found = true;
					
					break outer;
				}
			}
		}
		
		if ( !found ){
			
			System.out.println( "TorrentUtils::announceGroupsSetFirst - failed to find '" + first_url + "'" );
		}
		
		listToAnnounceGroups( groups, torrent );
	}
	
	public static boolean
	mergeAnnounceURLs(
		TOTorrent 	new_torrent,
		TOTorrent	dest_torrent )
	{
		if ( new_torrent == null || dest_torrent == null ){
			
			return( false);
		}
		
		List	new_groups 	= announceGroupsToList( new_torrent );
		List 	dest_groups = announceGroupsToList( dest_torrent );
		
		List	groups_to_add = new ArrayList();
		
		for (int i=0;i<new_groups.size();i++){
			
			List new_set = (List)new_groups.get(i);
			
			boolean	match = false;
			
			for (int j=0;j<dest_groups.size();j++){
				
				List dest_set = (List)dest_groups.get(j);
				
				boolean same = new_set.size() == dest_set.size();
				
				if ( same ){
					
					for (int k=0;k<new_set.size();k++){
						
						String new_url = (String)new_set.get(k);
						
						if ( !dest_set.contains(new_url)){
							
							same = false;
							
							break;
						}
					}
				}
				
				if ( same ){
					
					match = true;
					
					break;
				}
			}
			
			if ( !match ){
		
				groups_to_add.add( new_set );
			}
		}
		
		if ( groups_to_add.size() == 0 ){
			
			return( false );
		}
		
		for (int i=0;i<groups_to_add.size();i++){
			
			dest_groups.add(i,groups_to_add.get(i));
		}
		
		listToAnnounceGroups( dest_groups, dest_torrent );
		
		return( true );
	}
	
	public static void
	setResumeDataCompletelyValid(
		TOTorrent		torrent,
		String			data_location )
	{
		DiskManagerFactory.setResumeDataCompletelyValid( torrent, data_location );
	}
	
	public static String
	getLocalisedName(
		TOTorrent		torrent )
	{
		try{
			
			LocaleUtilDecoder decoder = LocaleUtil.getSingleton().getTorrentEncodingIfAvailable( torrent );
			
			if ( decoder == null ){
				
				return( new String(torrent.getName(),Constants.DEFAULT_ENCODING));
			}
			
			return( decoder.decodeString(torrent.getName()));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( new String( torrent.getName()));
		}
	}
	
	public static void
	setTLSTorrentHash(
		byte[]		hash )
	{
		((Map)tls.get()).put( "hash", hash );
	}
	
	public static TOTorrent
	getTLSTorrent()
	{
		byte[]	hash = (byte[])((Map)tls.get()).get("hash");
		
		if ( hash != null ){
			
			try{
				AzureusCore	core = AzureusCoreFactory.getSingleton();
				
				List	managers = core.getGlobalManager().getDownloadManagers();
				
				for (int i=0;i<managers.size();i++){
					
					DownloadManager	dm = (DownloadManager)managers.get(i);
					
					TOTorrent	torrent = dm.getTorrent();
					
					if ( torrent != null ){
						
						if ( Arrays.equals(torrent.getHash(),hash)){
							
							return( torrent );
						}
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
	
		// this class exists to minimise memory requirements by discarding the piece hash values
		// when "idle" 
	
	static final int	PIECE_HASH_TIMEOUT	= 120*1000;
	
	static Map	torrent_delegates = new WeakHashMap();
	
	static{
		SimpleTimer.addPeriodicEvent(
			PIECE_HASH_TIMEOUT/2,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					long	now = SystemTime.getCurrentTime();
					
					synchronized( torrent_delegates ){
						
						Iterator it = torrent_delegates.keySet().iterator();
						
						while( it.hasNext()){
							
							((torrentDelegate)it.next()).checkTimeout(now);
						}
					}
				}
			});
	}
	
	protected static class
	torrentDelegate
		implements TOTorrent
	{
		protected TOTorrent		delegate;
		protected File			file;
		
		protected long			last_pieces_read_time	= SystemTime.getCurrentTime();
		
		protected
		torrentDelegate(
			TOTorrent		_delegate,
			File			_file )
		{
			delegate		= _delegate;
			file			= _file;
			
			synchronized( torrent_delegates ){
				
				torrent_delegates.put( this, null );
			}
		}
		
		public byte[]
		getName()
		{
			return( delegate.getName());
		}
				
		public boolean
		isSimpleTorrent()
		{
			return( delegate.isSimpleTorrent());
		}
				
		public byte[]
		getComment()
		{
			return( delegate.getComment());		
		}

		public void
		setComment(
			String		comment )
		{
			delegate.setComment( comment );
		}
				
		public long
		getCreationDate()
		{
			return( delegate.getCreationDate());
		}
		
		public void
		setCreationDate(
			long		date )
		{
			delegate.setCreationDate( date );
		}
		
		public byte[]
		getCreatedBy()
		{
			return( delegate.getCreatedBy());
		}
		
		public URL
		getAnnounceURL()
		{
			return( delegate.getAnnounceURL());
		}

		public void
		setAnnounceURL(
			URL		url )
		{
			delegate.setAnnounceURL( url );
		}
			
		
		public TOTorrentAnnounceURLGroup
		getAnnounceURLGroup()
		{
			return( delegate.getAnnounceURLGroup());
		}
		 
		protected void
		checkTimeout(
			long		now )
		{
				// handle clock changes backwards
			
			if ( now < last_pieces_read_time ){
				
				last_pieces_read_time	= now;
				
			}else{
			
				try{
					if(		now - last_pieces_read_time > PIECE_HASH_TIMEOUT &&
							delegate.getPieces() != null ){
						
						try{
							getMonitor().enter();
							
							// System.out.println( "clearing pieces for '" + new String(getName()) + "'");

							delegate.setPieces( null );
						}finally{
							
							getMonitor().exit();
						}
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		public byte[][]
		getPieces()
		
			throws TOTorrentException
		{
			byte[][]	res = delegate.getPieces();
			
			last_pieces_read_time	= SystemTime.getCurrentTime();
		
			if ( res == null ){
						 
				// System.out.println( "recovering pieces for '" + new String(getName()) + "'");
				
				TOTorrent	temp = readFromFile( file, false );
					
				res	= temp.getPieces();
					
				delegate.setPieces( res );
			}
			
			return( res );
		}

		public void
		setPieces(
			byte[][]	pieces )
		
			throws TOTorrentException
		{
			throw( new TOTorrentException( "Unsupported Operation", TOTorrentException.RT_WRITE_FAILS ));
		}
		
		public long
		getPieceLength()
		{
			return( delegate.getPieceLength());
		}

		public int
		getNumberOfPieces()
		{
			return( delegate.getNumberOfPieces());
		}
		
		public long
		getSize()
		{
			return( delegate.getSize());
		}
		
		public TOTorrentFile[]
		getFiles()
		{
			return( delegate.getFiles());
		}
				 
		public byte[]
		getHash()
					
			throws TOTorrentException
		{
			return( delegate.getHash());
		}
		
		public HashWrapper
		getHashWrapper()
					
			throws TOTorrentException
		{
			return( delegate.getHashWrapper());
		}
		
		public boolean
		hasSameHashAs(
			TOTorrent		other )
		{
			return( delegate.hasSameHashAs( other ));
		}
				
		public void
		setAdditionalStringProperty(
			String		name,
			String		value )
		{
			delegate.setAdditionalStringProperty( name, value );
		}
			
		public String
		getAdditionalStringProperty(
			String		name )
		{
			return( delegate.getAdditionalStringProperty( name ));
		}
			
		public void
		setAdditionalByteArrayProperty(
			String		name,
			byte[]		value )
		{
			delegate.setAdditionalByteArrayProperty( name, value );
		}
			
		public byte[]
		getAdditionalByteArrayProperty(
			String		name )
		{
			return( delegate.getAdditionalByteArrayProperty( name ));
		}
		
		public void
		setAdditionalLongProperty(
			String		name,
			Long		value )
		{
			delegate.setAdditionalLongProperty( name, value );
		}
			
		public Long
		getAdditionalLongProperty(
			String		name )
		{
			return( delegate.getAdditionalLongProperty( name ));
		}
			
		
		public void
		setAdditionalListProperty(
			String		name,
			List		value )
		{
			delegate.setAdditionalListProperty( name, value );
		}
			
		public List
		getAdditionalListProperty(
			String		name )
		{
			return( delegate.getAdditionalListProperty( name ));
		}
			
		public void
		setAdditionalMapProperty(
			String		name,
			Map			value )
		{
			delegate.setAdditionalMapProperty( name, value );
		}
			
		public Map
		getAdditionalMapProperty(
			String		name )
		{
			return( delegate.getAdditionalMapProperty( name ));
		}
		
		public Object
		getAdditionalProperty(
			String		name )
		{
			return( delegate.getAdditionalProperty( name ));
		}

		public void
		removeAdditionalProperty(
			String name )
		{
			delegate.removeAdditionalProperty( name );
		}
		
		
		public void
		removeAdditionalProperties()
		{
			delegate.removeAdditionalProperties();
		}
		

		public void
		serialiseToBEncodedFile(
			File		target_file )
			  
			throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   		
		   		getPieces();
			
		   		delegate.serialiseToBEncodedFile( target_file );
		   		
			}finally{
				
				getMonitor().exit();
			}
		}


		public Map
		serialiseToMap()
			  
			throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   		
		   		getPieces();
			
		   		return( delegate.serialiseToMap());
		   		
			}finally{
				
				getMonitor().exit();
			}
		}


		public void
		serialiseToXMLFile(
				File		target_file )
			  
		   throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   		
		   		getPieces();
			
		   		delegate.serialiseToXMLFile( target_file );
			
			}finally{
				
				getMonitor().exit();
			}
		}

		public AEMonitor
		getMonitor()
		{
	   		return( delegate.getMonitor());
		}


		public void
		print()
		{
			delegate.print();
		}
	}
}
