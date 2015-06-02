/*
 * Created on Sep 13, 2012
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */


package com.aelitis.azureus.ui.swt.shells.main;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenOptions;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;
import org.gudy.azureus2.ui.swt.UIExitUtilsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.ui.*;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.mdi.TabbedMDI;
import com.aelitis.azureus.ui.swt.mdi.TabbedMdiInterface;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

public class 
MainWindowDelayStub 
	implements MainWindow
{
	private Display			display;
	private IUIIntializer	initialiser;
	
	private Shell			shell;
	
	private AzureusCore		core;
	private AESemaphore		core_sem = new AESemaphore("");
	
	private volatile MainWindow		main_window;
	
	private SystemTraySWT	swt_tray;
	
	private volatile UIFunctionsSWT	delayed_uif = new UIFunctionsSWTImpl();
	
	public
	MainWindowDelayStub(
		AzureusCore 			_core, 
		Display 				_display,
		IUIIntializer			_uiInitializer )
	{
		core		= _core;
		display		= _display;
		initialiser	= _uiInitializer;
		
		init();
		
		core_sem.releaseForever();
	}
	
	public
	MainWindowDelayStub(
		Display 		_display, 
		IUIIntializer 	_uiInitializer )
	{
		display		= _display;
		initialiser	= _uiInitializer;
		
		init();
	}
	
	private void
	init()
	{		
		final AESemaphore sem = new AESemaphore( "shell:create" );
		
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					try{
						shell = new Shell(display, SWT.SHELL_TRIM);

						UIFunctionsManagerSWT.setUIFunctions( delayed_uif );
						
						boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray");

						if ( bEnableTray ){
						
							swt_tray = SystemTraySWT.getTray();
						}
						
						MainHelpers.initTransferBar();
						
						if ( initialiser != null ){
						
							initialiser.initializationComplete();
							
							initialiser.abortProgress();
						}
						
						AERunStateHandler.addListener(
							new AERunStateHandler.RunStateChangeListener()
							{
								private boolean	handled = false;
								
								public void 
								runStateChanged(
									long run_state )
								{
									if ( AERunStateHandler.isDelayedUI() || handled ){
										
										return;
									}
									
									handled = true;
									
									checkMainWindow();
								}
							}, false );
					}finally{
						
						sem.release();
					}
				}
			});
		
		sem.reserve();
	}
	
	private void
	log(
		String	str )
	{
		Debug.out( str );
	}
	
	public void
	init(
		AzureusCore		_core )
	{
		core	= _core;
				
		core_sem.releaseForever();
	}
		
	
		// barp
	
	private interface
	Fixup
	{
		public void
		fix(
			MainWindow mw );
	}
	
	private interface
	Fixup2
	{
		public Object
		fix(
			MainWindow mw );
	}
	
	private interface
	Fixup3
	{
		public void
		fix(
			UIFunctionsSWT uif );
	}
	
	private interface
	Fixup4
	{
		public Object
		fix(
			UIFunctionsSWT uif );
	}
	
	private void
	checkMainWindow()
	{
		boolean	activated = false;
		
		synchronized( this ){
			
			if ( main_window == null ){
				
				final AESemaphore wait_sem = new AESemaphore( "cmw" );
				
				AzureusCoreLifecycleListener listener = 
					new AzureusCoreLifecycleAdapter()
					{
						public void
						componentCreated(
							AzureusCore				core,
							AzureusCoreComponent	component )
						{
							if ( component instanceof UIFunctions ){
								
								wait_sem.release();
							}
						}
					};
					
				core.addLifecycleListener( listener );
				
				main_window = new MainWindowImpl( core, display, null );
				
				if ( !wait_sem.reserve( 30*1000 )){
					
					Debug.out( "Gave up waiting for UIFunction component to be created" );
				}
				
				activated = true;
			}
		}
		
		if ( activated ){
			
			AERunStateHandler.setResourceMode( AERunStateHandler.RS_ALL_ACTIVE );
		}
	}
	
	private void
	fixup(
		Fixup	f )
	{
		core_sem.reserve();
		
		checkMainWindow();
		
		f.fix( main_window );
	}
	
	private Object
	fixup(
		Fixup2	f )
	{
		core_sem.reserve();
		
		checkMainWindow();
		
		return( f.fix( main_window ));
	}
	
	private void
	fixup(
		Fixup3	f )
	{
		core_sem.reserve();
					
		checkMainWindow();
		
		UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		if ( uif == delayed_uif ){
			
			Debug.out( "eh?" );
			
		}else{
			
			f.fix( uif );
		}
	}
	
	private Object
	fixup(
		Fixup4	f )
	{
		core_sem.reserve();
					
		checkMainWindow();
		
		UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		if ( uif == delayed_uif ){
			
			Debug.out( "eh?" );
			
			return( null );
			
		}else{
			
			return( f.fix( uif ));
		}
	}
	
		// toot
	
	public Shell
	getShell()
	{		
		return( shell );
	}

	public IMainMenu 
	getMainMenu()
	{
		return((IMainMenu)fixup( new Fixup2(){public Object fix( MainWindow mw){ return( mw.getMainMenu()); }}));
	}
	
	
	public IMainStatusBar 
	getMainStatusBar()
	{
		if ( main_window != null ){
			
			return( main_window.getMainStatusBar());
		}
				
		return( null );
	}
	
	public boolean
	isReady()
	{
		log( "isReady" );
		
		return( false );
	}
	
	public void 
	setVisible(
		final boolean visible, 
		final boolean tryTricks )
	{
		fixup( new Fixup(){public void fix( MainWindow mw){ mw.setVisible( visible, tryTricks ); }});
	}
	
	public UISWTInstanceImpl
	getUISWTInstanceImpl()
	{
		log( "getUISWTInstanceImpl" );
		
		return( null );
	}

	public void 
	setSelectedLanguageItem()
	{
		log( "setSelectedLanguageItem" );	
	}
	
	public boolean 
	dispose(
		final boolean for_restart,
		final boolean close_already_in_progress )
	{
		if ( main_window != null ){
			
			return( main_window.dispose(for_restart, close_already_in_progress));
		}
		
		log( "dispose" );
		
		UIExitUtilsSWT.uiShutdown();

		if ( swt_tray != null ){
			
			swt_tray.dispose();
		}
		
		try{
			AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(core.getGlobalManager());
			
			if ( transfer_bar != null ){
				
				transfer_bar.forceSaveLocation();
			}
		}catch( Exception ignore ){
		}

		if (!SWTThread.getInstance().isTerminated()) {
			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					if (!SWTThread.getInstance().isTerminated()) {
						SWTThread.getInstance().getInitializer().stopIt( for_restart, false);
					}
				}
			});
		}

		return true;
	}
	
	
	
	public boolean 
	isVisible(
		int windowElement)
	{
		log( "isVisible" );
		
		return( false );	
	}

	public void 
	setVisible(
		int 		windowElement, 
		boolean 	value )
	{
		log( "setVisible" );	
	}

	public void 
	setHideAll(
		boolean hide) 
	{
		log( "setHideAll" );	
	}
	
	public Rectangle 
	getMetrics(
		int windowElement)
	{
		log( "getMetrics" );	

		return( null );
	}
	
	private class
	UIFunctionsSWTImpl
		implements UIFunctionsSWT
	{
		public void 
		bringToFront()
		{	
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.bringToFront(); }});
		}

		public void 
		bringToFront(
			final boolean tryTricks)
		{	
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.bringToFront( tryTricks ); }});
		}

		public int 
		getVisibilityState() 
		{
			UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
			
			if ( uif != null && uif != this ){
				
				return( uif.getVisibilityState());
			}
			
			return( VS_TRAY_ONLY );
		}
		
		public void
		runOnUIThread(
			final int			ui_type,
			final Runnable		runnable )
		{
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.runOnUIThread( ui_type, runnable ); }});
		}
		
		public void 
		refreshLanguage()
		{	
			log( "refreshLanguage" );
		}

		public void 
		refreshIconBar()
		{	
			log( "refreshIconBar" );
		}

		public void 
		setStatusText(
			String string )
		{	
		}

		public void 
		setStatusText(
			int statustype, 
			String string, 
			UIStatusTextClickListener l)
		{	
			log( "setStatusText" );
		}

		public boolean 
		dispose(
			boolean for_restart, 
			boolean close_already_in_progress )
		{
			return( MainWindowDelayStub.this.dispose( for_restart, close_already_in_progress ));
		}

		public boolean 
		viewURL(
			String url, 
			String target, 
			int w, 
			int h, 
			boolean allowResize,
			boolean isModal)
		{
			log( "viewURL" );
			
			return( false );
		}

		public boolean 
		viewURL(
			String url, 
			String target, 
			double wPct, 
			double hPct,
			boolean allowResize, 
			boolean isModal)
		{
			log( "viewURL" );
			
			return( false );
		}

		public void 
		viewURL(
			String url, 
			String target, 
			String sourceRef)
		{
			log( "viewURL" );			
		}


		public UIFunctionsUserPrompter 
		getUserPrompter(
			String 		title, 
			String 		text,
			String[] 	buttons, 
			int 		defaultOption)
		{
			log( "getUserPrompter" );
			
			return( null );
		}

		public void 
		promptUser(
			String title, 
			String text, 
			String[] buttons,
			int defaultOption, 
			String rememberID, 
			String rememberText,
			boolean bRememberByDefault, 
			int autoCloseInMS, 
			UserPrompterResultListener l)
		{
			log( "promptUser" );
		}
		

		public UIUpdater 
		getUIUpdater()
		{			
			return( UIUpdaterSWT.getInstance());
		}


		public void 
		openView(
			final int 		viewID, 
			final Object 	datasource )
		{
				// OSX Vuze->Preferences menu
			
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.openView( viewID, datasource ); }});
		}
		
		public void 
		doSearch(
			String searchText )
		{
			log( "doSearch" );
		}
		
		public void 
		doSearch(
			String searchText, 
			boolean toSubscribe )
		{
			log( "doSearch" );
		}

		public void
		installPlugin(
			String			plugin_id,
			String			resource_prefix,
			actionListener	listener )
		{
			log( "installPlugin" );
		}
		

		public void
		performAction(
			final int				action_id,
			final Object			args,
			final actionListener	listener )
		{
				// auto-update restart prompt (for example)
			
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.performAction( action_id, args, listener ); }});
		}
		
		public MultipleDocumentInterface 
		getMDI()
		{
			log( "getMDI" );
			
			return( null );
		}


		public void 
		forceNotify(
			int iconID, 
			String title, 
			String text, 
			String details,
			Object[] relatedObjects, 
			int timeoutSecs)
		{
			log( "forceNotify" );
		}
		
		
		public Shell 
		getMainShell()
		{			
			return( shell );
		}


		public void 
		addPluginView(
			String viewID, 
			UISWTViewEventListener l)
		{
			log( "addPluginView" );
		}


		public void 
		closeDownloadBars()
		{
		}

		public boolean 
		isGlobalTransferBarShown()
		{
			if (!AzureusCoreFactory.isCoreRunning()) {
				return false;
			}
			
			return AllTransfersBar.getManager().isOpen(
					AzureusCoreFactory.getSingleton().getGlobalManager());
		}

		public void 
		showGlobalTransferBar() 
		{
			AllTransfersBar.open(getMainShell());
		}

		public void 
		closeGlobalTransferBar() 
		{
			AllTransfersBar.closeAllTransfersBar();
		}


		public UISWTView[] 
		getPluginViews()
		{
			log( "getPluginViews" );
			
			return( new UISWTView[0] );
		}


		public void 
		openPluginView(
			String sParentID, 
			String sViewID,
			UISWTViewEventListener l, 
			Object dataSource, 
			boolean bSetFocus)
		{
			log( "openPluginView" );
		}

		public void 
		openPluginView(
			final UISWTViewCore view, 
			final String name)
		{
			log( "openPluginView" );
		}


		public void 
		removePluginView(
			String viewID)
		{
			log( "removePluginView" );
		}


		public void 
		closePluginView(
			UISWTViewCore view)
		{
			log( "closePluginView" );
		}

		public void 
		closePluginViews(
			String sViewID )
		{
			log( "closePluginViews" );
		}

		public UISWTInstance 
		getUISWTInstance()
		{
			log( "getUISWTInstance" );
			
			return( null );
		}

		public void 
		refreshTorrentMenu()
		{
			log( "refreshTorrentMenu" );
		}

		public IMainStatusBar 
		getMainStatusBar()
		{			
			return( null );
		}


		public IMainMenu 
		createMainMenu(
			final Shell shell)
		{
				// OSX Vuze->About menu

			return((IMainMenu)fixup( new Fixup4(){public Object fix( UIFunctionsSWT uif){ return( uif.createMainMenu( shell )); }}));
		}

		public IMainWindow 
		getMainWindow()
		{
			return( MainWindowDelayStub.this );
		}

		public void 
		closeAllDetails()
		{
			log( "closeAllDetails" );
		}


		public boolean 
		hasDetailViews()
		{
			log( "hasDetailViews" );
			
			return( false );
		}
		
		public Shell 
		showCoreWaitDlg()
		{
			return( null );
		}
		
		public boolean 
		isProgramInstalled(
			final String extension, 
			final String name ) 
		{
			return((Boolean)fixup( new Fixup4(){public Object fix( UIFunctionsSWT uif){ return( uif.isProgramInstalled( extension, name )); }}));
		}
		
		public MultipleDocumentInterfaceSWT 
		getMDISWT()
		{
			log( "getMDISWT" );
			
			return( null );
		}

		public void 
		promptForSearch()
		{
			log( "promptForSearch" );
		}

		public UIToolBarManager 
		getToolBarManager()
		{
			log( "getToolBarManager" );
			
			return( null );
		}
		
		public void
		openRemotePairingWindow()
		{
			log( "openRemotePairingWindow" );
		}
		
		public void
		playOrStreamDataSource(
			Object 		ds, 
			String 		referal,
			boolean 	launch_already_checked, 
			boolean 	complete_only )
		{
			log( "playOrStreamDataSource" );
		}
		
		public void 
		setHideAll( 
			boolean hidden) 
		{
			log( "setHideAll" );
		}

		public void 
		showErrorMessage(
			String 		keyPrefix, 
			String 		details,
			String[] 	textParams) 
		{
			log( "showErrorMessage" );
		}
		
		// @see com.aelitis.azureus.ui.UIFunctions#showCreateTagDialog(org.gudy.azureus2.ui.swt.views.utils.TagUIUtils.TagReturner)
		public void showCreateTagDialog(
				TagReturner tagReturner) 
		{
			log( "showAddTagDialog" );
		}

		public boolean 
		addTorrentWithOptions(
			final boolean 				force,
			final TorrentOpenOptions 	torrentOptions) 
		{
			return((Boolean)fixup( new Fixup4(){public Object fix( UIFunctionsSWT uif){ return( uif.addTorrentWithOptions( force, torrentOptions )); }}));
		}
				
		public void 
		openTorrentOpenOptions(
			final Shell shell, 
			final String sPathOfFilesToOpen,
			final String[] sFilesToOpen, 
			final boolean defaultToStopped, 
			final boolean forceOpen) 
		{
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.openTorrentOpenOptions( shell, sPathOfFilesToOpen, sFilesToOpen, defaultToStopped, forceOpen);}});
		}

		public void 
		openTorrentOpenOptions(
			final Shell 				shell,
			final String 				sPathOfFilesToOpen, 
			final String[] 				sFilesToOpen,
			final Map<String, Object> 	options ) 
		{
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.openTorrentOpenOptions( shell, sPathOfFilesToOpen, sFilesToOpen, options);}});
		}
		
		public void 
		openTorrentWindow() 
		{
			fixup( new Fixup3(){public void fix( UIFunctionsSWT uif){ uif.openTorrentWindow(); }});
		}
		
		/* (non-Javadoc)
		 * @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#createTabbedMDI(org.eclipse.swt.widgets.Composite)
		 */
		public TabbedMdiInterface createTabbedMDI(Composite parent, String id) {
			log( "createTabbedMDI" );
			return null;
		}
	}
}
