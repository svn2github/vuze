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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.disk.*;

public class 
TorrentUtils 
{
	public static TOTorrent
	readFromFile(
		String		file_name )
		
		throws TOTorrentException
	{
		TOTorrent torrent;
   
		try{
			torrent = TOTorrentFactory.deserialiseFromBEncodedFile(new File(file_name));
			
		}catch (TOTorrentException e){
      
			e.printStackTrace();
			
			File torrentBackup = new File(file_name + ".bak");
			
			if( torrentBackup.exists()){
				
				torrent = TOTorrentFactory.deserialiseFromBEncodedFile(torrentBackup);
				
					// use the original torrent's file name so that when this gets saved
					// it writes back to the original and backups are made as required
					// - set below
			}else{
				
				throw e;
			}
		}
		
		torrent.setAdditionalStringProperty("torrent filename", file_name );
		
		return( torrent );
	}

	public static void
	writeToFile(
		TOTorrent		torrent )
	
		throws TOTorrentException 
	{
	    synchronized( torrent ){
	    	
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
	    			
	    			e.printStackTrace();
	    		}
	    	}
	      
	    	torrent.serialiseToBEncodedFile(torrent_file);
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
	 
			errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo"); //$NON-NLS-1$
	 		        
		}else if ( reason == TOTorrentException.RT_UNSUPPORTED_ENCODING ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_READ_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_HASH_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
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
			
			e.printStackTrace();
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
			
			e.printStackTrace();
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
	
	public static void
	setDefaultTorrentEncoding(
		TOTorrent		torrent )
	{
		LocaleUtil.setDefaultTorrentEncoding( torrent );
	}
	
	public static String
	getLocalisedName(
		TOTorrent		torrent )
	{
		try{
			
			LocaleUtilDecoder decoder = LocaleUtil.getTorrentEncodingIfAvailable( torrent );
			
			if ( decoder == null ){
				
				return( new String(torrent.getName(),Constants.DEFAULT_ENCODING));
			}
			
			return( decoder.decodeString(torrent.getName()));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( new String( torrent.getName()));
		}
	}
}
