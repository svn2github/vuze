/*
 * File    : ShareConfigImpl.java
 * Created : 31-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.sharing;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.sharing.*;

public class 
ShareConfigImpl 
{
	protected ShareManagerImpl		manager;
	
	protected boolean				saving_suspended;
	protected boolean				save_outstanding;
	
	protected void
	loadConfig(
		ShareManagerImpl	_manager )
	{
		manager	= _manager;
		
		FileInputStream 		fin = null;
		
		BufferedInputStream 	bin = null;
		
		try{
			
			File configFile = FileUtil.getApplicationFile("sharing.config");
			
			fin = new FileInputStream(configFile);
			
			bin = new BufferedInputStream(fin);
			
			Map map = BDecoder.decode(bin);
			
			List resources = (List) map.get("resources");
			
			if (resources == null){
				
				return;
			}
			
			Iterator  iter = resources.iterator();
			
			while (iter.hasNext()) {
				
				Map r_map = (Map) iter.next();
				
				manager.deserialiseResource( r_map );
			}
		}catch (FileNotFoundException e) {
			
			//Do nothing
			
		}catch (Exception e) {
			
			e.printStackTrace();
			
		}finally{
			try{
				if (bin != null){
						
					bin.close();
				}
			}catch (Exception e){
			}
			
			try{
				if (fin != null){
					
					fin.close();
				}
			}catch(Exception e){
			}
		}
	}

	protected synchronized void
	saveConfig()
	
		throws ShareException
	{
		if ( saving_suspended ){
			
			save_outstanding = true;
			
			return;
		}
		
		Map map = new HashMap();
		
		List list = new ArrayList();
		
		map.put("resources", list);
		
		ShareResource[]	shares = manager.getShares();
		
		for (int i=0;i<shares.length;i++){
			
			Map	m = new HashMap();
			
			((ShareResourceImpl)shares[i]).serialiseResource( m );
			
			list.add( m );
		}
		
		
			//open a file stream
		
		FileOutputStream fos = null;
		
		try{
			//encode the data
			
			byte[] torrentData = BEncoder.encode(map);
			
			fos = new FileOutputStream(FileUtil.getApplicationFile("sharing.config"));
			
			//write the data out
			
			fos.write(torrentData);
			
		}catch (Exception e){
			
			e.printStackTrace();
			
			throw( new ShareException("ShareConfig::saveConfig failed", e ));
		}finally{
			
			try {
				if (fos != null){
					
					fos.close();
				}
				
			}catch (Exception e){
				
				throw( new ShareException("ShareConfig::saveConfig failed", e ));
				
			}
		}
	}
	
	protected synchronized void
	suspendSaving()
	{
		saving_suspended	= true;
	}
	
	protected synchronized void
	resumeSaving()
		throws ShareException
	{
		saving_suspended	= false;
		
		if ( save_outstanding ){
			
			save_outstanding	= false;
			
			saveConfig();
		}
	}
}

