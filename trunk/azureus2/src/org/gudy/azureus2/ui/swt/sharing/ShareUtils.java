/*
 * File    : ShareUtils.java
 * Created : 08-Jan-2004
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

package org.gudy.azureus2.ui.swt.sharing;

/**
 * @author parg
 *
 */

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;

import org.gudy.azureus2.pluginsimpl.PluginInitializer;

public class 
ShareUtils 
{
	public static void
	shareFile(
		final Shell		shell )
	{
		new Thread()
		{
			public void
			run()
			{
				Display display = shell.getDisplay();
				
				final String[] file_to_share = {null};
						
				final Semaphore	sem = new Semaphore();
					
				display.asyncExec(new Runnable() {
					public void run()
					{
						try{
							FileDialog dialog = new FileDialog(shell, SWT.SYSTEM_MODAL | SWT.OPEN);
							
							dialog.setFilterPath(COConfigurationManager.getStringParameter("Default Path", ""));
													
							dialog.setText(MessageText.getString("MainWindow.dialog.share.sharefile"));
							
							file_to_share[0] = dialog.open();
				
							if (file_to_share[0] != null){
								
								COConfigurationManager.setParameter("Default Path", file_to_share[0]);
								
								COConfigurationManager.save();
							}
						}finally{
							
							sem.release();
						}
					}
				});
				
				sem.reserve();
				
				String	target = file_to_share[0];
				
				if ( target != null ){
					
					shareFile( target );
				}
			}
		}.start();
	}

	public static void
	shareDir(
		Shell		shell )
	{
		shareDirSupport( shell, false, false );
	}
	
	public static void
	shareDirContents(
		Shell		shell,
		boolean		recursive )
	{
		shareDirSupport( shell, true, recursive );
	}
	
	protected static void
	shareDirSupport(
		final Shell		shell,
		final boolean	contents,
		final boolean	recursive )
	{
		new Thread()
		{
			public void
			run()
			{
				Display display = shell.getDisplay();
				
				final String[] dir_to_share = {null};
				
				final Semaphore	sem = new Semaphore();
				
				display.asyncExec(new Runnable() {
					public void run()
					{
						try{
							DirectoryDialog dialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
							
							dialog.setFilterPath(COConfigurationManager.getStringParameter("Default Path", ""));
							
							dialog.setText( 
										contents?
										MessageText.getString("MainWindow.dialog.share.sharedircontents") + 
												(recursive?"("+MessageText.getString("MainWindow.dialog.share.sharedircontents.recursive")+")":""):
										MessageText.getString("MainWindow.dialog.share.sharedir"));
							
							dir_to_share[0] = dialog.open();
							
							if (dir_to_share[0] != null){
								
								COConfigurationManager.setParameter("Default Path", dir_to_share[0]);
								
								COConfigurationManager.save();
							}
						}finally{
							
							sem.release();
						}
					}
				});
				
				sem.reserve();
				
				String	target = dir_to_share[0];
				
				if ( target != null ){
					
					if ( contents ){
						
						shareDirContents( target, recursive );
						
					}else{
						
						shareDir( target );
					}
				}
			}
		}.start();
	}
	
	public static void
	shareFile(
		final String	file_name )
	{
		new Thread()
		{
			public void
			run()
			{
				try{
					PluginInitializer.getDefaultInterface().getShareManager().addFile(new File(file_name));
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void
	shareDir(
		final String	file_name )
	{
		new Thread()
		{
			public void
			run()
			{
				try{
					PluginInitializer.getDefaultInterface().getShareManager().addDir(new File(file_name));
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public static void
	shareDirContents(
		final String	file_name,
		final boolean	recursive )
	{
		new Thread()
		{
			public void
			run()
			{
				try{
					PluginInitializer.getDefaultInterface().getShareManager().addDirContents(new File(file_name), recursive);
			
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}.start();
	}
}
