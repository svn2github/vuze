/*
 * Created on 16-May-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.update;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.plugins.update.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

public class 
UpdateInstallerImpl
	implements UpdateInstaller
{
		// change these and you'll need to change the Updater!!!!
	
	protected static final String	UPDATE_DIR 	= "updates";
	protected static final String	ACTIONS		= "install.act";
	
	protected File	install_dir;
	
	protected static void
	checkForFailedInstalls()
	{
		try{
			File	update_dir = new File( getUserDirSupport() + File.separator + UPDATE_DIR );
			
			File[]	dirs = update_dir.listFiles();
			
			boolean	found_failure = false;
			
			for (int i=0;i<dirs.length;i++){
				
				File	dir = dirs[i];
				
				if ( dir.isDirectory()){
					
						// if somethings here then the install failed
					
					found_failure	= true;
					
					FileUtil.recursiveDelete( dir );
				}
			}
			
			if ( found_failure ){
				
				LGLogger.logAlert( 
						LGLogger.AT_ERROR, 
						"Installation of at least one component failed - see 'update.log' for details" );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected
	UpdateInstallerImpl()
	
		throws UpdateException
	{
		synchronized( UpdateInstallerImpl.class){
			
				// updates are in general user-specific (e.g. plugin updates) so store here
				// obviously core ones will affect all users
			
			String	update_dir = getUserDir() + File.separator + UPDATE_DIR;
			
			for (int i=1;i<1024;i++){
				
				File	try_dir = new File( update_dir + File.separator + "inst_" + i );
								
				if ( !try_dir.exists()){
					
					if ( !try_dir.mkdirs()){
		
						throw( new UpdateException( "Failed to create a temporary installation dir"));
					}
					
					install_dir	= try_dir;
					
					break;
				}
			}
			
			if ( install_dir == null ){
				
				throw( new UpdateException( "Failed to find a temporary installation dir"));
			}
		}
	}
	
	public void
	addResource(
		String      resource_name,
		InputStream   is )
  
    	throws UpdateException
	{
		addResource(resource_name,is,true);
	}
  
	public void
	addResource(
		String			resource_name,
		InputStream		is,
		boolean 		closeInputStream)
	
		throws UpdateException
	{
		try{
			File	target_file = new File(install_dir, resource_name );
		
			FileUtil.copyFile( is, new FileOutputStream( target_file ),closeInputStream);
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateInstaller: resource addition fails", e ));
		}
	}
		
	public String
	getInstallDir()
	{
		String	str = SystemProperties.getApplicationPath();
		
		if ( str.endsWith(File.separator)){
			
			str = str.substring(0,str.length()-1);
		}
		
		return( str );
	}
		
	public String
	getUserDir()
	{
		return( getUserDirSupport());
	}
	
	protected static String
	getUserDirSupport()
	{
		String	str = SystemProperties.getUserPath();
	
		if ( str.endsWith(File.separator)){
			
			str = str.substring(0,str.length()-1);
		}
		
		return( str );	
	}
	
	public void
	addMoveAction(
		String		from_file_or_resource,
		String		to_file )
	
		throws UpdateException
	{
		// System.out.println( "move action:" + from_file_or_resource + " -> " + to_file );
		
		if ( from_file_or_resource.indexOf(File.separator) == -1 ){
			
			from_file_or_resource = install_dir.toString() + File.separator + from_file_or_resource;
		}
		
		appendAction( "move," + from_file_or_resource  + "," + to_file );
	}
  
  
  public void
  addChangeRightsAction(
    String    rights,
    String    to_file )
  
    throws UpdateException
  { 
    appendAction( "chmod," + rights  + "," + to_file );
  }
  
	
	protected void
	appendAction(
		String		data )
	
		throws UpdateException
	{
		PrintWriter	pw = null;
	
		try{		
			
			pw = new PrintWriter(new FileWriter( install_dir.toString() + File.separator + ACTIONS, true ));

			pw.println( data );
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "Failed to write actions file", e ));
			
		}finally{
			
			if ( pw != null ){
		
				try{
		
					pw.close();
					
				}catch( Throwable e ){
	
					throw( new UpdateException( "Failed to write actions file", e ));
				}
			}
		}
	}
}
