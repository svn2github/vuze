/*
 * File    : PluginManagerImpl.java
 * Created : 14-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local;

/**
 * @author parg
 *
 */

import java.util.*;
import java.lang.reflect.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.update.Restarter;

public class 
PluginManagerImpl 
	extends PluginManager
{
	protected static boolean	running		= false;
	protected static int		ui_type		= PluginManager.UI_NONE;
	
	protected static PluginManagerImpl	singleton;
	
	protected static synchronized PluginManagerImpl
	getSingleton(
		PluginInitializer	pi )
	{
		if ( singleton == null ){
			
			singleton = new PluginManagerImpl( pi );
		}
		
		return( singleton );
	}
	
	public static void
	startAzureus(
		int			_ui_type,
		Properties	properties )
	{
		synchronized( PluginManagerImpl.class ){
			
			if ( running ){
				
				throw( new RuntimeException( "Azureus is already running"));
			}
			
			running	= true;
			
			ui_type	= _ui_type;
		}
		
			// there's a small window here when an immediate "stop" wouldn't work coz
			// this code would carry on after the stop and start. However, can't easily
			// fix this here...
		
		if ( ui_type == PluginManager.UI_NONE ){
		
				// can't invoke directly as the ui.common stuff isn't part of the core distribution
				// org.gudy.azureus2.ui.common.Main.main( new String[]{"--ui=console"});
			
			try{
				Class	main = Class.forName("org.gudy.azureus2.ui.common.Main");
				
				Method method = main.getMethod( "main", new Class[]{ String[].class });
				
				method.invoke( null, new Object[]{new String[]{"--ui=console"}});
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
					// some idiot (me) forgot to add the exception to the i/f and now we
					// can't add it as is stuffs existing plugins...
				
				throw( new RuntimeException( "Azureus failed to start", e ));
			}
		}else if ( ui_type == PluginManager.UI_SWT ){
				
			if ( properties != null ){
				
				String	mi = (String)properties.get( PluginManager.PR_MULTI_INSTANCE );
				
				if ( mi != null && mi.equalsIgnoreCase("true")){
					
					System.setProperty( org.gudy.azureus2.ui.swt.Main.PR_MULTI_INSTANCE, "true" );
				}
			}
			
			org.gudy.azureus2.ui.swt.Main.main(new String[0]);
		}
	}
	
	public static synchronized void
	stopAzureus()
	
		throws PluginException
	{
		if ( !running ){
			
			throw( new RuntimeException( "Azureus is not running"));
		}
		
		if ( ui_type == PluginManager.UI_NONE ){
			
				// can't invoke directly as the ui.common stuff isn't part of the core distribution
				// org.gudy.azureus2.ui.common.Main.shutdown();
			
			try{
				Class	main = Class.forName("org.gudy.azureus2.ui.common.Main");
				
				Method method = main.getMethod( "shutdown", new Class[]{});
				
				method.invoke( null, null );
				
			}catch( Throwable e ){
							
				throw( new PluginException( "PluginManager: Azureus close action failed", e));
			}
		}else if ( ui_type == PluginManager.UI_SWT ){
			
			final Semaphore			sem 	= new Semaphore();
			final PluginException[]	error 	= {null};
			
			try{
				MainWindow.getWindow().getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							try{
						
								if ( !MainWindow.getWindow().dispose()){
									
									error[0] = new PluginException( "PluginManager: Azureus close action failed");
								}	
							}finally{
									
								sem.release();
							}
						}
					});
			}catch( Throwable e ){
				
				error[0]	= new PluginException( "PluginManager: closeAzureus fails", e );
				
				sem.release();
			}
			
			sem.reserve();
			
			if ( error[0] != null ){
	
				// removed reporting of error 
			}
		}
		
		running	= false;
	}
	
	public static void
	restartAzureus()
	
		throws PluginException
	{
		if ( !running ){
			
			throw( new RuntimeException( "Azureus is not running"));
		}
		
		if ( ui_type != PluginManager.UI_SWT ){
			
			throw( new RuntimeException( "Only SWT type Azureus restart supported"));
		}
		
		final Semaphore			sem 	= new Semaphore();
		final PluginException[]	error 	= {null};
			
		try{
			MainWindow.getWindow().getDisplay().asyncExec(
				new Runnable()
				{
					public void
					run()
					{
						try{				
							if ( !MainWindow.getWindow().dispose()){
									
								error[0] = new PluginException( "PluginManager: Azureus close action failed");
							}	
							
							Restarter.restartForUpgrade();
						}finally{
									
							sem.release();
						}
					}
				});
		}catch( Throwable e ){
				
			error[0]	= new PluginException( "PluginManager: closeAzureus fails", e );
				
			sem.release();
		}
			
		sem.reserve();
			
		if ( error[0] != null ){

			// removed reporting of error 
		
		}
			
		running	= false;
	}	
	
		/**
		 * When AZ is started directly (i.e. not via a plugin) this method is called
		 * so that the running state is correctly understood
		 * @param type
		 */
	
	public static void
	setStartType(
		int			_type )
	{
		ui_type		= _type;
		
		running		= true;
	}
	
	public static void
	registerPlugin(
		Class		plugin_class )
	{
		PluginInitializer.queueRegistration( plugin_class );
	}
	
	public static PluginInterface
	getPluginInterfaceByID(
		String		id )
	{
		PluginInterface[]	p = getPluginInterfaces();
		
		for (int i=0;i<p.length;i++){
			
			if ( p[i].getPluginID().equals( id )){
				
				return( p[i]);
			}
		}
		
		return( null );
	}
	
	public static PluginInterface
	getPluginInterfaceByClass(
		Class		c )
	{
		PluginInterface[]	p = getPluginInterfaces();
		
		for (int i=0;i<p.length;i++){
			
			if ( p[i].getPlugin().getClass().equals( c )){
				
				return( p[i]);
			}
		}
		
		return( null );
	}
	
	public static PluginInterface[]
	getPluginInterfaces()
	{
		List	l = PluginInitializer.getPluginInterfaces();
		
		PluginInterface[]	res = new PluginInterface[l.size()];
		
		l.toArray(res);
		
		return( res );
	}
	
	protected PluginInitializer		pi;
	
	protected
	PluginManagerImpl(
		PluginInitializer		_pi )
	{
		pi		= _pi;
	}
	
	public PluginInterface[]
	getPlugins()
	{
		return( pi.getPlugins());
	}
}
