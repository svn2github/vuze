/*
 * File    : FMFileManagerImpl.java
 * Created : 12-Feb-2004
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

package com.aelitis.azureus.core.diskmanager.file.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.File;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.diskmanager.file.*;

public class 
FMFileManagerImpl
	implements FMFileManager
{
	public static final boolean DEBUG	= false;
	
	protected static FMFileManagerImpl	singleton;
	
	public static synchronized FMFileManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new FMFileManagerImpl();
		}
		
		return( singleton );
	}
	
	protected LinkedHashMap		map;
	protected boolean			limited;
	protected int				limit_size;
	
	protected AESemaphore		close_queue_sem;
	protected List				close_queue;
	
	protected List				files;
	
	protected 
	FMFileManagerImpl()
	{
		limit_size = COConfigurationManager.getIntParameter( "File Max Open" );
		
		limited		= limit_size > 0;
	
		if ( DEBUG ){
			
			System.out.println( "FMFileManager::init: limit = " + limit_size );
			
			files = new ArrayList();
		}
		
		map	= new LinkedHashMap( limit_size, (float)0.75, true );	// ACCESS order selected - this means oldest

		if ( limited ){
			
			close_queue_sem	= new AESemaphore("FMFileManager::closeqsem");
			
			close_queue		= new LinkedList();
			
			Thread	t = new AEThread("FMFileManager::closeQueueDispatcher")
				{
					public void
					run()
					{
						closeQueueDispatch();
					}
				};
			
			t.setDaemon(true);
			
			t.start();
		}
	}
	
	public FMFile
	createFile(
		FMFileOwner	owner,
		File		file )
	
		throws FMFileManagerException
	{
		FMFile	res;
		
		if ( limited ){

			res = new FMFileLimited(owner,this, file);
			
		}else{
			
			res = new FMFileUnlimited( owner, file);
		}
		
		if (DEBUG){
			
			synchronized( files ){
				
				files.add( res );
			}
		}
		
		return( res );
	}
	
	
	protected void
	getSlot(
		FMFileLimited	file )
	{
			// must close the oldest file outside sync block else we'll get possible deadlock
		
		FMFileLimited	oldest_file = null;
		
		synchronized( map ){
		
			if (DEBUG ){
				System.out.println( "FMFileManager::getSlot: " + file.getFile().toString() +", map_size = " + map.size());
			}
			
			if ( map.size() >= limit_size ){
				
				Iterator it = map.keySet().iterator();
				
				oldest_file = (FMFileLimited)it.next();
				
				it.remove();
			}
			
			map.put( file, file );
		}
		
		if ( oldest_file != null ){
			
			closeFile( oldest_file );			

		}
	}
	
	protected void
	releaseSlot(
		FMFileLimited	file )
	{
		if ( DEBUG ){
			System.out.println( "FMFileManager::releaseSlot: " + file.getFile().toString());
		}
		
		synchronized( map ){
			
			map.remove( file );	
		}
	}
	
	protected void
	usedSlot(
		FMFileLimited	file )
	{	
		if ( DEBUG ){
			System.out.println( "FMFileManager::usedSlot: " + file.getFile().toString());
		}
		
		synchronized( map ){
		
				// only update if already present - might not be due to delay in
				// closing files
			
			if ( map.containsKey( file )){
				
				map.put( file, file );		// update MRU
			}
		}
	}
	
	protected void
	closeFile(
		FMFileLimited	file )
	{
		if ( DEBUG ){
			System.out.println( "FMFileManager::closeFile: " + file.getFile().toString());
		}
		synchronized( close_queue ){
			
			close_queue.add( file );
		}
		
		close_queue_sem.release();
	}
	
	protected void
	closeQueueDispatch()
	{
		while(true){
			
			if ( DEBUG ){
				
				close_queue_sem.reserve(1000);
				
			}else{
				
				close_queue_sem.reserve();
			}
			
			FMFileLimited	file = null;
			
			synchronized( close_queue ){
			
				if ( close_queue.size() > 0 ){
					
					file = (FMFileLimited)close_queue.remove(0);

					if ( DEBUG ){
						
						System.out.println( "FMFileManager::closeQ: " + file.getFile().toString() + ", rem = " + close_queue.size());
					}
				}
			}
			
			if ( file != null ){
				
				try{
					file.close(false);
				
				}catch( Throwable e ){
				
					e.printStackTrace();
				}
			}
			
			if ( DEBUG ){
				
				synchronized( files ){
					
					int	open = 0;
					
					for (int i=0;i<files.size();i++){

						FMFileLimited	f = (FMFileLimited)files.get(i);
						
						if ( f.isOpen()){
							
							open++;
						}
					}
					
					System.out.println( "INFO: files = " + files.size() + ", open = " + open );
				}
			}
		}
	}
}
