/*
 * Created on May 29, 2006 2:13:41 PM
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
package com.aelitis.azureus.ui.swt;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.auth.CryptoWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.PreUpdateChecker;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.core.versioncheck.VersionCheckClientListener;
import com.aelitis.azureus.ui.*;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcherSWT;
import com.aelitis.azureus.ui.swt.devices.DeviceManagerUI;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.search.SearchUI;
import com.aelitis.azureus.ui.swt.shells.main.MainWindowFactory;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI;
import com.aelitis.azureus.ui.swt.utils.UIMagnetHandler;
import com.aelitis.azureus.util.InitialisationFunctions;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 * @notes
 * The old Initializer would store up LogEvents if the UI had the console set
 * to auto-open, and send the events to the mainwindow when it was initialized 
 * This Initializer doesn't do this (yet)
 	    final ArrayList logEvents = new ArrayList();
	    ILogEventListener logListener = null;
	    if (COConfigurationManager.getBooleanParameter("Open Console", false)) {
	    	logListener = new ILogEventListener() {
					public void log(LogEvent event) {
						logEvents.add(event);
					}
	    	};
	    	Logger.addListener(logListener);
	    }
	    final ILogEventListener finalLogListener = logListener;
 *
 * The old initializer sets a semaphore when it starts loading IPFilters,
 * and on AzureusCoreListener.coreStarted would:
						IpFilterManager ipFilterManager = azureus_core.getIpFilterManager();
						if (ipFilterManager != null) {
							String s = MessageText.getString("splash.loadIpFilters");
	  					do {
	  						reportCurrentTask(s);
	  						s += ".";
	  					} while (!semFilterLoader.reserve(3000));
						}
 */
public class Initializer
	implements IUIIntializer
{
	// Whether to initialize the UI before the core has been started
	private static boolean STARTUP_UIFIRST = System.getProperty("ui.startfirst", "1").equals("1");

	// Used in debug to find out how long initialization took
	public static final long startTime = System.currentTimeMillis();

	private StartServer startServer;

	private final AzureusCore core;

	private final String[] args;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	private AEMonitor listeners_mon = new AEMonitor("Initializer:l");

	private int curPercent = 0;

  private AESemaphore semFilterLoader = new AESemaphore("filter loader");
  
	private AESemaphore init_task = new AESemaphore("delayed init");

	private MainWindowFactory.MainWindowInitStub windowInitStub;;
	
	private static Initializer lastInitializer;

	/**
	 * Main Initializer.  Usually called by reflection via
	 * org.gudy.azureus2.ui.swt.Main(String[])
	 * @param core
	 * @param args
	 */
	public Initializer(final AzureusCore core, StartServer startServer, String[] args) {
		this.core = core;
		this.args = args;
		this.startServer = startServer;
		lastInitializer = this;

    Thread filterLoaderThread = new AEThread("filter loader", true) {
			public void runSupport() {
				try {
					core.getIpFilterManager().getIPFilter();
				} finally {
					semFilterLoader.releaseForever();
				}
			}
		};
		filterLoaderThread.setPriority(Thread.MIN_PRIORITY);
		filterLoaderThread.start();

    try {
      SWTThread.createInstance(this);
    } catch(SWTThreadAlreadyInstanciatedException e) {
    	Debug.printStackTrace( e );
    }
	}
	
	private void cleanupOldStuff() {
		File v3Shares = new File(SystemProperties.getUserPath(), "v3shares");
		if (v3Shares.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(v3Shares);
		}
		File dirFriends = new File(SystemProperties.getUserPath(), "friends");
		if (dirFriends.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(dirFriends);
		}
		File dirMedia = new File(SystemProperties.getUserPath(), "media");
		if (dirMedia.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(dirMedia);
		}
		deleteConfig("v3.Friends.dat");
		deleteConfig("unsentdata.config");
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				new AEThread2("cleanupOldStuff", true) {
					public void run() {
						GlobalManager gm = core.getGlobalManager();
						List dms = gm.getDownloadManagers();
						for (Object o : dms) {
							DownloadManager dm = (DownloadManager) o;
							if (dm != null) {
								String val = PlatformTorrentUtils.getContentMapString(
										dm.getTorrent(), "Ad ID");
								if (val != null) {
									try {
										gm.removeDownloadManager(dm, true, true);
									} catch (Exception e) {
									}
								}
							}
						}
					}
				}.start();
			}
		});
	}

	private void deleteConfig(String name) {
		try {
  		File file = new File(SystemProperties.getUserPath(), name);
  		if (file.exists()) {
  			file.delete();
  		}
		} catch (Exception e) {
		}
		try {
  		File file = new File(SystemProperties.getUserPath(), name + ".bak");
  		if (file.exists()) {
  			file.delete();
  		}
		} catch (Exception e) {
		}
	}

	public void runInSWTThread() {
		UISwitcherUtil.calcUIMode();
		
		try {
  		initializePlatformClientMessageContext();
		} catch (Exception e) {
			Debug.out(e);
		}
		new AEThread2("cleanupOldStuff", true) {
			public void run() {
				cleanupOldStuff();
			}
		}.start();

		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");

		if (!uiClassic) {
			PlatformConfigMessenger.login(0);
		}
		
		VersionCheckClient.getSingleton().addVersionCheckClientListener(true,
				new VersionCheckClientListener() {
					public void versionCheckStarted(String reason) {
						if (VersionCheckClient.REASON_UPDATE_CHECK_START.equals(reason)
								|| VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC.equals(reason)) {
							PlatformConfigMessenger.sendVersionServerMap(VersionCheckClient.constructVersionCheckMessage(reason));
						}
					}
				});

		FeatureManagerUI.registerWithFeatureManager();

		COConfigurationManager.setBooleanDefault("ui.startfirst", true);
		STARTUP_UIFIRST = STARTUP_UIFIRST
				&& COConfigurationManager.getBooleanParameter("ui.startfirst", true);
		
		if (!STARTUP_UIFIRST) {
			return;
		}

		// Ensure colors initialized
		Colors.getInstance();

		UIConfigDefaultsSWT.initialize();

		UIConfigDefaultsSWTv3.initialize(core);
		
		checkInstallID();

		windowInitStub = MainWindowFactory.createAsync( Display.getDefault(), this );
	}

	/**
	 * 
	 *
	 * @since 4.4.0.5
	 */
	private void checkInstallID() {
		String storedInstallID = COConfigurationManager.getStringParameter("install.id", null);
		String installID = "";
		File file = FileUtil.getApplicationFile("installer.log");
		if (file != null) {
			try {
				String s = FileUtil.readFileAsString(file, 1024);
				String[] split = s.split("[\r\n]");
				for (int i = 0; i < split.length; i++) {
					int posEquals = split[i].indexOf('=');
					if (posEquals > 0 && split[i].length() > posEquals + 1) {
						installID = split[i].substring(posEquals + 1);
					}
				}
			} catch (IOException e) {
			}
		}
		
		if (storedInstallID == null || !storedInstallID.equals(installID)) {
			COConfigurationManager.setParameter("install.id", installID);
		}
	}

	public void run() {
		
		DelayedTask delayed_task = UtilitiesImpl.addDelayedTask( "SWT Initialisation", new Runnable()
				{
					public void
					run()
					{
						init_task.reserve();
					}
				});

		delayed_task.queueFirst();
		
		// initialise the SWT locale util
		long startTime = SystemTime.getCurrentTime();

		new LocaleUtilSWT(core);
		
		final Display display = SWTThread.getInstance().getDisplay();

		new UIMagnetHandler(core);
		
		if (!STARTUP_UIFIRST) {
			// Ensure colors initialized
			Colors.getInstance();

			UIConfigDefaultsSWT.initialize();
			UIConfigDefaultsSWTv3.initialize(core);
		} else {
			COConfigurationManager.setBooleanDefault("Show Splash", false);
		}

		if (COConfigurationManager.getBooleanParameter("Show Splash")) {
			display.asyncExec(new AERunnable() {
				public void runSupport() {
					new SplashWindow(display, Initializer.this);
				}
			});
		}

		System.out.println("Locale Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			private GlobalManager gm;

			public void 
			componentCreated(
				AzureusCore 			core,
				AzureusCoreComponent 	component )
			{
				Initializer.this.reportPercent(curPercent + 1);

				
				if (component instanceof GlobalManager){
					
					reportCurrentTaskByKey("splash.initializePlugins");

					gm = (GlobalManager) component;

					InitialisationFunctions.earlyInitialisation(core);
					
				} else if (component instanceof PluginInterface) {
					PluginInterface pi = (PluginInterface) component;
					// text says initializing, but it's actually initialized.  close enough
					String s = MessageText.getString("splash.plugin.init") + " "
							+ pi.getPluginName() + " v" + pi.getPluginVersion();
					reportCurrentTask(s);
				}
			}

			// @see com.aelitis.azureus.core.AzureusCoreLifecycleAdapter#started(com.aelitis.azureus.core.AzureusCore)
			public void started(AzureusCore core) {
				boolean	main_window_will_report_complete = false;
				
				try {
	
					InitialisationFunctions.lateInitialisation(core);
					if (gm == null) {
						return;
					}
	
					// Ensure colors initialized
					Colors.getInstance();
	
					Initializer.this.reportPercent(curPercent + 1);
					new UserAlerts(gm);
	
					reportCurrentTaskByKey("splash.initializeGui");
	
					Initializer.this.reportPercent(curPercent + 1);
					
					main_window_will_report_complete = true;
					
					if (STARTUP_UIFIRST) {
						windowInitStub.init(core);
					} else {
						MainWindowFactory.create( core, Display.getDefault(), Initializer.this );
					}
					
					reportCurrentTaskByKey("splash.openViews");
	
					SWTUpdateChecker.initialize();
	
					PreUpdateChecker.initialize(core,
							COConfigurationManager.getStringParameter("ui"));
	
					UpdateMonitor.getSingleton(core); // setup the update monitor
	
					//Tell listeners that all is initialized :
					Alerts.initComplete();
	
					//Finally, open torrents if any.
					for (int i = 0; i < args.length; i++) {
	
						String arg = args[i];
						
						if ( arg.equalsIgnoreCase( "--open" )){
							
								// can get this here so skip as not a torrent!
							
						}else{
							try {
								TorrentOpener.openTorrent( arg );
		
							} catch (Throwable e) {
		
								Debug.printStackTrace(e);
							}
						}
					}
				}
				finally{
					
					if ( !main_window_will_report_complete ){
						init_task.release();
					}
				}

			}

			public void stopping(AzureusCore core) {
				Alerts.stopInitiated();
			}

			public void stopped(AzureusCore core) {
			}

			public boolean syncInvokeRequired() {
				return (true);
			}

			public boolean
			requiresPluginInitCompleteBeforeStartedEvent()
			{
				return( false );
			}
			
			public boolean stopRequested(AzureusCore _core)
					throws AzureusCoreException {
				return handleStopRestart(false);
			}

			public boolean restartRequested(final AzureusCore core) {
				return handleStopRestart(true);
			}

		});

		reportCurrentTaskByKey("splash.initializeCore");

		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");

  		try{
  			new SearchUI();
  			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
  		
  		try{
			new SubscriptionManagerUI();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}

  		
		if (!uiClassic){
	  		try{
	  			new DeviceManagerUI( core );
	  				
	  		}catch( Throwable e ){
	  				
	  			Debug.printStackTrace(e);
	  		}
		}
		
		if ( core.canStart()){
			
			core.start();
	
			reportPercent(50);
	
			System.out.println("Core Initializing took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();
	
			reportCurrentTaskByKey("splash.initializeUIElements");
	
			// Ensure colors initialized
			Colors.getInstance();
	
			reportPercent(curPercent + 1);
			Alerts.init();
	
			reportPercent(curPercent + 1);
			ProgressWindow.register(core);
	
			reportPercent(curPercent + 1);
			new SWTNetworkSelection();
	
			reportPercent(curPercent + 1);
			new AuthenticatorWindow();
			new CryptoWindow();
			
			reportPercent(curPercent + 1);
			new CertificateTrustWindow();
	
			InstallPluginWizard.register(core, display);
			
				// finally check if an explicit open has been requested in case hidden in tray atm
			
			for (int i = 0; i < args.length; i++) {
				
				String arg = args[i];
				
				if ( arg.equalsIgnoreCase( "--open" )){
					
					UIFunctions uif = UIFunctionsManager.getUIFunctions();
					
					if ( uif != null ){
					
						uif.bringToFront();
					}
					
					break;
				}
			}
		}else{
			
			final AESemaphore sem = new AESemaphore( "waiter" );
			
			Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						MessageBoxShell mb = 
							new MessageBoxShell( 
								MessageText.getString( "msgbox.force.close.title" ),
								MessageText.getString( 
									"msgbox.force.close.text",
									new String[]{ core.getLockFile().getAbsolutePath() }),
								new String[]{ MessageText.getString("Button.ok") },
								0 );
						
						mb.setIconResource( "error" );
						
						mb.setModal( true );
						
						mb.open(
							new UserPrompterResultListener() 
							{
								
								public void 
								prompterClosed(
									int 	result ) 
								{
									sem.releaseForever();
								}
							});
					}
				});
			
			sem.reserve();
			
			SESecurityManager.exitVM( 1 );
		}
	}

	public void stopIt(boolean isForRestart, boolean isCloseAreadyInProgress)
			throws AzureusCoreException {
		if (core != null && !isCloseAreadyInProgress) {

			if (isForRestart) {

				core.checkRestartSupported();
			}
		}

		try {

			//			Cursors.dispose();

			try {
				UIFunctionsManager.getUIFunctions().getUIUpdater().stopIt();
			} catch (Exception e) {
				Debug.out(e);
			}

			if (false) {
				// No Unix as it will dispose before isTerminated is set, causing
				// a 'user close' flag to be incorrectly set and used
  			Utils.execSWTThread(new AERunnable() {
  				public void runSupport() {
  					SWTThread instance = SWTThread.getInstance();
  					if (instance == null || instance.isTerminated()) {
  						return;
  					}
  					Shell anyShell = Utils.findAnyShell();
  					Point location = null;
  					if (anyShell != null) {
  						Rectangle bounds = anyShell.getBounds();
  						location = new Point(bounds.x, bounds.y);
  					}
  					Shell[] shells = instance.getDisplay().getShells();
  					for (Shell shell : shells) {
  						if (!shell.isDisposed()) {
  							shell.dispose();
  						}
  					}
      			Shell shell = new Shell(instance.getDisplay(), SWT.BORDER | SWT.TITLE);
      			Utils.setShellIcon(shell);
      			shell.setText("Shutting Down Vuze..");
      			shell.setSize(200, 0);
      			if (location != null) {
      				shell.setLocation(location);
      			}
      			shell.open();
  				}
  			});
			}

		} finally {

			try{
				if ( core != null && !isCloseAreadyInProgress) {
	
					try {
						if (isForRestart) {
	
							core.restart();
	
						} else {
	
							long lStopStarted = System.currentTimeMillis();
							System.out.println("core.stop");
							core.stop();
							System.out.println("core.stop done in "
									+ (System.currentTimeMillis() - lStopStarted));
						}
					} catch (Throwable e) {
	
						// don't let any failure here cause the stop operation to fail
	
						Debug.out(e);
					}
				}
			}finally{
				
					// do this after closing core to minimise window when the we aren't 
					// listening and therefore another Azureus start can potentially get
					// in and screw things up
				
				if (startServer != null) {
					startServer.stopIt();
				}
			}

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTThread.getInstance().terminate();
				}
			});

		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#addListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void addListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.add(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#removeListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void removeListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.remove(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	public void reportCurrentTask(String currentTaskString) {
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportCurrentTask(currentTaskString);
				} catch (Exception e) {
					// ignore
				}
			}
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTaskByKey(String key) {
		reportCurrentTask(MessageText.getString(key));
	}
	
	public void increaseProgress() {
		if (curPercent < 100) {
			reportPercent(curPercent + 1);
		}
	}
	
	// @see com.aelitis.azureus.ui.IUIIntializer#abortProgress()
	public void abortProgress() {
		reportPercent(101);
	}

	public void reportPercent(int percent) {
		if (curPercent > percent) {
			return;
		}

		curPercent = percent;
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportPercent(percent);
				} catch (Exception e) {
					// ignore
				}
			}

			if (percent > 100) {
				listeners.clear();
			}
		} finally {

			listeners_mon.exit();
		}
	}
	
	public void
	initializationComplete()
	{
		core.getPluginManager().firePluginEvent( PluginEvent.PEV_INITIALISATION_UI_COMPLETES );

		// Old Initializer would delay 8500

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
			  new DelayedEvent( 
					  "SWTInitComplete:delay",
					  500,
					  new AERunnable()
					  {
						  public void
						  runSupport()
						  {
						  	/*
						  	try {
									String captureSnapshot = new Controller().captureSnapshot(ProfilingModes.SNAPSHOT_WITH_HEAP);
									System.out.println(captureSnapshot);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								*/
						  	//System.out.println("Release Init. Task");
							  init_task.release();
						  }
					  });
			}
		});
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	private void initializePlatformClientMessageContext() {
		ClientMessageContext clientMsgContext = PlatformMessenger.getClientMessageContext();
		if (clientMsgContext != null) {
			clientMsgContext.setMessageDispatcher(new MessageDispatcherSWT(clientMsgContext));
			clientMsgContext.addMessageListener(new TorrentListener());
			clientMsgContext.addMessageListener(new VuzeListener());
			clientMsgContext.addMessageListener(new DisplayListener(null));
			clientMsgContext.addMessageListener(new ConfigListener(null));
		}
		PluginInitializer.getDefaultInterface().addEventListener(new PluginEventListener() {
			public void handleEvent(PluginEvent ev) {
				try {
  				int type = ev.getType();
  				String event = null;
  				if (type == PluginEvent.PEV_PLUGIN_INSTALLED) {
  					event = "installed";
  				} else if (type == PluginEvent.PEV_PLUGIN_UNINSTALLED) {
  					event = "uninstalled";
  				}
  				if (event != null && (ev.getValue() instanceof String)) {
  					PlatformConfigMessenger.logPlugin(event, (String) ev.getValue());
  				}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		});
	}

  public static boolean
  handleStopRestart(
  	final boolean	restart )
  {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT != null) {
			return functionsSWT.dispose(restart, true);
		}

		return false;
	}
	
	public static Initializer getLastInitializer() {
		return lastInitializer;
	}
}
