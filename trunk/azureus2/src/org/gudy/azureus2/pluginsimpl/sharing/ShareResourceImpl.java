/*
 * File    : ShareResourceImpl.java
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

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.util.*;

public abstract class 
ShareResourceImpl
	implements ShareResource, Comparable
{
	protected static Md5Hasher	hasher = new Md5Hasher();
	
	protected ShareManagerImpl		manager;
	protected int					type;
	
	protected
	ShareResourceImpl(
		ShareManagerImpl	_manager,
		int					_type )
	{
		manager	= _manager;
		type 	= _type;
	}
	
	protected abstract void
	serialiseResource(
		Map		map );
	
	public int
	getType()
	{
		return( type );
	}
	
	
	public void
	delete()
	
		throws ShareException
	{
		manager.delete(this);
	}
	
	protected abstract void
	deleteInternal();
	
	protected byte[]
	getFingerPrint(
		File		file )
	
		throws ShareException
	{
		try{
			String	finger_print = getFingerPrintSupport( file );
							
			return( hasher.calculateHash(finger_print.getBytes()));
			
		}catch( ShareException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new ShareException( "ShareResource::getFingerPrint: fails", e ));
		}
	}
	
	protected String
	getFingerPrintSupport(
		File		file )
	
		throws ShareException
	{
		try{
			if ( file.isFile()){
				
				long	mod 	= file.lastModified();
				long	size	= file.length();
			
				String	finger_print = file.getName()+":"+mod+":"+size;
			
				return( finger_print );
				
			}else{
				
				String	res = "";
				
				File[]	dir_file_list = file.listFiles();
								
				List file_list = new ArrayList(Arrays.asList(dir_file_list));
				
				Collections.sort(file_list);
				
				for (int i=0;i<file_list.size();i++){
					
					File	f = (File)file_list.get(i);;
					
					String	file_name = f.getName();
					
					if ( !(file_name.equals( "." ) || file_name.equals( ".." ))){
						
						res += ":" + getFingerPrintSupport( f );
					}
				}
				
				return( res );
			}
			
		}catch( Throwable e ){
			
			throw( new ShareException( "ShareResource::getFingerPring: fails", e ));
		}
	}
	protected String
	getNewTorrentLocation()
	
		throws ShareException
	{
		return( manager.getNewTorrentLocation());
	}
	
	protected void
	writeTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		manager.writeTorrent( item );
	}
	
	protected void
	readTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		manager.readTorrent( item );
	}	
	
	protected void
	deleteTorrent(
		ShareItemImpl		item )
	{
		manager.deleteTorrent( item );
	}
	
	protected abstract void
	checkConsistency()
	
		throws ShareException;
}
