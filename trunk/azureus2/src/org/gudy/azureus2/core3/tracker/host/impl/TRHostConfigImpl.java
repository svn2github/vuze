/*
 * File    : TRHostConfigImpl.java
 * Created : 06-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TRHostConfigImpl 
{
	protected TRHostImpl	host;
	
	protected
	TRHostConfigImpl(
		TRHostImpl	_host )
	{
		host	= _host;
	}
	
	protected void
	loadConfig(
		TRHostTorrentFinder		finder ) 
	{
		FileInputStream 		fin = null;
		
		BufferedInputStream 	bin = null;
	   
	   	try{
		 
			File configFile = FileUtil.getApplicationFile("tracker.config");
		 
			fin = new FileInputStream(configFile);
		 
			bin = new BufferedInputStream(fin);
		 
			Map map = BDecoder.decode(bin);
		 
			List torrents = (List) map.get("torrents");
		 
			if (torrents == null){
		
				return;
		 	}
		 
		 	Iterator  iter = torrents.iterator();
		 
		 	while (iter.hasNext()) {
		 	
		   		Map t_map = (Map) iter.next();
		   
 				byte[]	hash = (byte[])t_map.get("hash");
			 
			 	int	state = ((Long)t_map.get("status")).intValue();
			 	
			 	if ( state == TRHostTorrent.TS_FAILED ){
			 		
			 		state = TRHostTorrent.TS_STOPPED;
			 	}
			 	
			 	TOTorrent	torrent = finder.lookupTorrent( hash );
			 	
			 	if ( torrent != null ){
			 		
			 		host.addTorrent( torrent, state, true );
			 		
			 	}else{
					if ( COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
		 		
			 			host.addExternalTorrent( hash, state );
					}
			 	}
		   	}
		}catch (FileNotFoundException e) {
			
		 	//Do nothing
		 	
	   	}catch (Exception e) {
		 
			e.printStackTrace();
			  
	   	}finally{
			try{
		   		if (bin != null)
					 bin.close();
		 	}catch (Exception e) {}
		 	
		 	try{
		   		if (fin != null)
			 		fin.close();
		 	}catch (Exception e) {}
	   	}
	}

	protected void
	saveConfig()
	{
	   	Map map = new HashMap();
	   
	   	List list = new ArrayList();
	   
	   	TRHostTorrent[]	torrents = host.getTorrents();
	   
	   	for (int i = 0; i < torrents.length; i++){
	   	
	  	 	try{
	  
				TRHostTorrent torrent = (TRHostTorrent)torrents[i];
				
				if ( torrent.isPersistent()){
					
					Map t_map = new HashMap();
			 	
					t_map.put("hash", torrent.getTorrent().getHash());
					
					t_map.put("status", new Long(torrent.getStatus()));
		
					list.add(t_map);
				}
			 	
	  	 	}catch( TOTorrentException e ){
	  	 		
	  	 		e.printStackTrace();
	  	 	}
	   	}
	   	
	   	map.put("torrents", list);
	   	
	   		//encode the data
	   		
	   	byte[] torrentData = BEncoder.encode(map);
	   	
	   		//open a file stream
	   		
	   	FileOutputStream fos = null;
	   	
	   	try{
		 	fos = new FileOutputStream(FileUtil.getApplicationFile("tracker.config"));
		 	
		 		//write the data out
		 		
		 	fos.write(torrentData);
		 	
	   	}catch (Exception e){
	   		
		 	e.printStackTrace();
	   	}finally{
	   		
		 	try {
		   		if (fos != null)
			 		fos.close();
		 	}catch (Exception e) {}
	   	}
	}
}
