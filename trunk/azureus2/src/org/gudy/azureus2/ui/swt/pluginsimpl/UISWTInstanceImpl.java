/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

import java.awt.Panel;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;



import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIException;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIInstanceFactory;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIManagerEventListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;

import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.*;

import com.aelitis.azureus.core.AzureusCore;

public class 
UISWTInstanceImpl
	implements UIInstanceFactory, UISWTInstance, UIManagerEventListener
{
	private AzureusCore		core;
	
	private Map awt_view_map 	= new WeakHashMap();
	private Map config_view_map = new WeakHashMap();
	
	private Map views = new HashMap();
	
	private Map	plugin_map	= new WeakHashMap();
	
	private boolean bUIAttaching;
	
	
	public 
	UISWTInstanceImpl(
		AzureusCore		_core )
	{
		core		= _core;
		
		try{
			UIManager	ui_manager = core.getPluginManager().getDefaultPluginInterface().getUIManager();
			
			ui_manager.addUIEventListener( this );
			
			bUIAttaching = true;
			
			ui_manager.attachUI( this );
			
			bUIAttaching = false;
			
		}catch( UIException e ){
			
			Debug.printStackTrace(e);
		}
	}
  
	public UIInstance
	getInstance(
		PluginInterface		plugin_interface )
	{
		UIInstance	instance = (UIInstance)plugin_map.get( plugin_interface );
		
		if ( instance == null ){
			
			instance = new instanceWrapper( plugin_interface, this );
			
			plugin_map.put( plugin_interface, instance );
		}
		
		return( instance );
	}
	
	public boolean
	eventOccurred(
		final UIManagerEvent	event )
	{
		boolean	done = true;
		
		final Object	data = event.getData();
		
		switch( event.getType()){
		
			case UIManagerEvent.ET_SHOW_TEXT_MESSAGE:
			{
				getDisplay().asyncExec(
					new Runnable()
					{
						public void 
						run()
						{
							String[]	params = (String[])data;
							
							new TextViewerWindow( params[0], params[1], params[2] );
						}
					});
				
				break;
			}
			
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE:
			{	
				TorrentOpener.openTorrent(core, ((File)data).toString());

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_URL:
			{
				Display	display = MainWindow.getWindow().getDisplay();
				
				display.syncExec(
						new AERunnable()
						{
							public void
							runSupport()
							{
								URL[]	urls = (URL[])data;
								
								new FileDownloadWindow(
										core,
										MainWindow.getWindow().getDisplay(),
										urls[0].toString(), urls[1]==null?null:urls[1].toString());
							}
						});
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					
					// property bundles can't handle spaces in keys
					String sViewID = model.getName().replaceAll(" ", ".");
					BasicPluginViewImpl view = new BasicPluginViewImpl(model);
					addView(UISWTInstance.VIEW_MAIN, sViewID, view);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					// property bundles can't handle spaces in keys
					String sViewID = model.getName().replaceAll(" ", ".");
					removeViews(UISWTInstance.VIEW_MAIN, sViewID);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = new BasicPluginConfigImpl(model);
					   
					config_view_map.put( model, view );
					
					model.getPluginInterface().addConfigSection( view );
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = (BasicPluginConfigImpl)config_view_map.get( model );
					   
					if ( view != null ){
						
						model.getPluginInterface().removeConfigSection( view );
					}
				}
				
				break;
			}
			case UIManagerEvent.ET_COPY_TO_CLIPBOARD:
			{
				ClipboardCopy.copyToClipBoard((String)data);
				
				break;
			}
			case UIManagerEvent.ET_OPEN_URL:
			{
				Program.launch(((URL)data).toExternalForm());
				
				break;
			}
			default:
			{
				done	= false;
				
				break;
			}
		}
		
		return( done );
	}
	
	public Display 
	getDisplay() 
	{
		return SWTThread.getInstance().getDisplay();
	}
  
	public Image
	loadImage(
		String	resource )
	{
		throw( new RuntimeException( "plugin specific instance required" ));
	}
	
	protected Image  
	loadImage(
		PluginInterface	pi,
		String 			res ) 
	{
		InputStream is = pi.getPluginClassLoader().getResourceAsStream( res);
		
		if ( is != null ){
		        
			ImageData imageData = new ImageData(is);
		    
			return new Image(getDisplay(), imageData);
		}
		
		return null;
	}
	
	public UISWTGraphic 
	createGraphic(
		Image img) 
	{
		return new UISWTGraphicImpl(img);
	}
  

	public void 
	addView(
		final UISWTPluginView view, 
		boolean bAutoOpen )
	{
	  	try{
		    final MainWindow window = MainWindow.getWindow();
		    
		    if ( window != null ){
		    
		      window.getMenu().addPluginView(view);
		      
		      if ( bAutoOpen ){
		    	  
		    	  Display	display = window.getDisplay();
		    	  
		    	  if ( display.getThread() == Thread.currentThread()){
		    		  
		    		  window.openPluginView(view);
		    		  
		    	  }else{
		    		  display.asyncExec(
		    			  new AERunnable()
		    			  {
		    				  public void 
		    				  runSupport() 
		    				  {
		    					  window.openPluginView(view);
		    				  }
		    			  });
		    	  }
		      }
		    }
	  	}catch( Throwable e ){
	  		// SWT not available prolly
	  	}
	} 
  

	public void
	removeView(
		UISWTPluginView		view )
	{
	  	try{
		    final MainWindow window = MainWindow.getWindow();
		    
		    if ( window != null ){

		    	window.getMenu().removePluginView( view );
		    }
	  	}catch( Throwable e ){
	  		// SWT not available prolly
	  	}
	}

	public void
	addView(
		final UISWTAWTPluginView	view,
		boolean						auto_open )
	{
		UISWTPluginView	v = 
			new UISWTPluginView()
			{
				Composite		composite;
				Component		component;
				
				boolean	first_paint = true;
				
				public String
				getPluginViewName()
				{
					return( view.getPluginViewName());
				}
				
				public String 
				getFullTitle() 
				{
					return( view.getPluginViewName());
				}
				 
				public void 
				initialize(
					Composite _composite )
				{
					first_paint	= true;
					
					composite	= _composite;
					
					Composite frame_composite = new Composite(composite, SWT.EMBEDDED);
	
					GridData data = new GridData(GridData.FILL_BOTH);
					
					frame_composite.setLayoutData(data);
	
					Frame	f = SWT_AWT.new_Frame(frame_composite);
	
					BorderLayout	layout = 
						new BorderLayout()
						{
							public void 
							layoutContainer(Container parent)
							{
								try{
									super.layoutContainer( parent );
								
								}finally{
									if ( first_paint ){
										
										first_paint	= false;
											
										view.open( component );
									}
								}
							}
						};
					
					Panel	pan = new Panel( layout );
	
					f.add( pan );
							
					component	= view.create();
					
					pan.add( component, BorderLayout.CENTER );
				}
				
				public Composite 
				getComposite()
				{
					return( composite );
				}
				
				public void 
				delete() 
				{
					super.delete();
					
					view.delete( component );
				}
			};
			
		awt_view_map.put( view, v );
		
		addView( v, auto_open );
	}
	
	public void
	removeView(
		UISWTAWTPluginView		view )
	{
		UISWTPluginView	v = (UISWTPluginView)awt_view_map.remove(view );
		
		if ( v != null ){
			
			removeView( v );
		}
	}
	
	public void
	detach()
	
		throws UIException
	{
		throw( new UIException( "not supported" ));
	}


	public void addView(String sParentID, final String sViewID,
			final UISWTViewEventListener l) {
		Map subViews = (Map) views.get(sParentID);
		if (subViews == null) {
			subViews = new HashMap();
			views.put(sParentID, subViews);
		}

		subViews.put(sViewID, l);

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						final MainWindow window = MainWindow.getWindow();

						if (window != null)
							window.getMenu().addPluginView(sViewID, l);
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
	}
	
	// TODO: Remove views from PeersView, etc
	public void removeViews(String sParentID, final String sViewID) {
		Map subViews = (Map) views.get(sParentID);
		if (subViews == null)
			return;

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						final MainWindow window = MainWindow.getWindow();

						if (window != null)
							window.getMenu().removePluginViews(sViewID);
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
		subViews.remove(sViewID);
	}

	public void openMainView(final String sViewID,
			final UISWTViewEventListener l, final Object dataSource) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MainWindow.getWindow().openPluginView(UISWTInstance.VIEW_MAIN, sViewID,
						l, dataSource, !bUIAttaching);
			}
		});
	}

	public UISWTView[] getOpenViews(String sParentID) {
		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			try {
				final MainWindow window = MainWindow.getWindow();
	
				if (window != null)
					return 	window.getPluginViews();
			} catch (Throwable e) {
				// SWT not available prolly
			}
		}
		return new UISWTView[0];
	}

	// Core Functions
	// ==============
	
	public Map getViewListeners(String sParentID) {
		return (Map)views.get(sParentID);
	}
	
	
	protected static class
	instanceWrapper
		implements UISWTInstance
	{
		private PluginInterface			pi;
		private UISWTInstanceImpl		delegate;
		
		protected
		instanceWrapper(
			PluginInterface		_pi,
			UISWTInstanceImpl	_delegate )
		{
			pi			= _pi;
			delegate	= _delegate;
		}
		
		public void
		detach()
		
			throws UIException
		{
			delegate.detach();
		}
	
		public Display
		getDisplay()
		{
			return( delegate.getDisplay());
		}
		
		public Image
		loadImage(
			String	resource )
		{
			return( delegate.loadImage( pi, resource ));
		}
		
		public UISWTGraphic 
		createGraphic(
			Image img )
		{
			return( delegate.createGraphic( img ));
		}
		
		public void 
		addView(String sParentID, String sViewID, UISWTViewEventListener l)
		{
			delegate.addView( sParentID, sViewID, l );
		}

		public void 
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource)
		{
			delegate.openMainView( sViewID, l, dataSource );
		}

		public void 
		removeViews(String sParentID, String sViewID)
		{
			delegate.removeViews(sParentID, sViewID );
		}


		public UISWTView[] 
		getOpenViews(String sParentID)
		{
			return( delegate.getOpenViews(sParentID));
		}


		public void 
		addView(UISWTPluginView view, boolean autoOpen)
		{
			delegate.addView( view, autoOpen );
		}


		public void 
		removeView(UISWTPluginView view)
		{
			delegate.removeView( view );
		}

		public void 
		addView(UISWTAWTPluginView view, boolean auto_open)
		{
			delegate.addView( view, auto_open );
		}


		public void 
		removeView(UISWTAWTPluginView view)
		{
			delegate.removeView( view );
		}
	}
}
