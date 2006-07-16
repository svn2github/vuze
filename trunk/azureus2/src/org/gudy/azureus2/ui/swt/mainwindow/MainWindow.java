 /*
 * Created on Jun 25, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Modified Apr 17, 2004 by Olivier Chalouhi (OSX system menu)
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.components.ColorUtils;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.donations.DonationWindow2;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.sharing.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;
import org.gudy.azureus2.ui.swt.wizard.WizardListener;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import java.util.*;

/**
 * @author Olivier
 * Runnable : so that GUI initialization is done via asyncExec(this)
 * STProgressListener : To make it visible once initialization is done
 */
public class 
MainWindow
	extends AERunnable
	implements 	GlobalManagerListener, DownloadManagerListener, 
				ParameterListener, IconBarEnabler, AEDiagnosticsEvidenceGenerator,
				ObfusticateShell
{
	private static final LogIDs LOGID = LogIDs.GUI;
  
  private static MainWindow window;

  private Initializer initializer;  
  private GUIUpdater updater;

  private AzureusCore			azureus_core;
  
  private GlobalManager       	globalManager;

  //NICO handle swt on macosx
  public static boolean isAlreadyDead = false;
  public static boolean isDisposeFromListener = false;  

  private Display display;
  private Composite parent;
  private Shell shell;
  
  private MainMenu mainMenu;
  
  private IconBar iconBar;
  
  private boolean useCustomTab;
  private Composite folder;
      
  
  /** 
   * Handles initializing and refreshing the status bar (w/help of GUIUpdater)
   */
  private MainStatusBar mainStatusBar;
  
  private TrayWindow downloadBasket;

  private SystemTraySWT systemTraySWT;
  
  private HashMap downloadViews;
  private AEMonitor	downloadViews_mon			= new AEMonitor( "MainWindow:dlviews" );

  HashMap 	downloadBars;
  AEMonitor	downloadBars_mon			= new AEMonitor( "MainWindow:dlbars" );

     
  private Tab 	mytorrents;
  private Tab 	my_tracker_tab;
  private Tab 	my_shares_tab;
  private Tab 	stats_tab;
  private Tab 	console;
  
  private Tab 			config;
  private ConfigView	config_view;
  
  protected AEMonitor	this_mon			= new AEMonitor( "MainWindow" );

  private UISWTInstanceImpl uiSWTInstanceImpl;

  private ArrayList events;

	private UIFunctions uiFunctions;

  public
  MainWindow(
  	AzureusCore		_azureus_core,
	Initializer 	_initializer,
	ArrayList events) 
  { 
		try{
  		if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));
	    
  		AEDiagnostics.addEvidenceGenerator( this );
		
	    azureus_core	= _azureus_core;
	    
	    globalManager = azureus_core.getGlobalManager();
	    
	    initializer = _initializer;
	    
	    display = SWTThread.getInstance().getDisplay();
	    
	    window = this;
	    
	    this.events = events;
	    
			display.asyncExec(this);
	    
  	}catch( AzureusCoreException e ){
  		
  		Debug.printStackTrace( e );
  	}
  }
  
  public MainWindow(AzureusCore _azureus_core, Initializer _initializer,
			Shell shell, Composite parent) {
		this.shell = shell;
		this.parent = parent;

		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = _azureus_core;

			globalManager = azureus_core.getGlobalManager();

			initializer = _initializer;

			display = SWTThread.getInstance().getDisplay();

			window = this;

  		runSupport();
			//display.asyncExec(this);

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}
  
  public void runSupport() {
    FormData formData;
    final int NUM_TASKS = 8;
    int iTaskNo = 0;
    
    try{
    	uiFunctions = UIFunctionsManager.getUIFunctions();
    	if (uiFunctions == null) {
    		uiFunctions = new UIFunctionsImpl(this);
    		UIFunctionsManager.setUIFunctions(uiFunctions);
    	}
    	
			globalManager.loadExistingTorrentsNow(null, true);
       
    useCustomTab = COConfigurationManager.getBooleanParameter("useCustomTab");
    

    COConfigurationManager.addParameterListener( "config.style.useSIUnits", this );
  
    mytorrents = null;
    my_tracker_tab	= null;
    console = null;
    config = null;
    config_view = null;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    
    Control attachToTopOf = null;
    Control controlAboveFolder = null;
    Control controlBelowFolder = null;
    
    //The Main Window
			if (shell == null) {
				shell = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE
						| SWT.MAX | SWT.MIN);
				shell.setData("class", this);
				shell.setText("Azureus"); //$NON-NLS-1$
				Utils.setShellIcon(shell);

				if (parent == null) {
					parent = shell;
				}

				// register window
				ShellManager.sharedManager().addWindow(shell);

				
				mainMenu = new MainMenu(this);

		    
				FormLayout mainLayout = new FormLayout();
				mainLayout.marginHeight = 0;
				mainLayout.marginWidth = 0;
				try {
					mainLayout.spacing = 0;
				} catch (NoSuchFieldError e) { /* Pre SWT 3.0 */
				}
				shell.setLayout(mainLayout);

				Utils.linkShellMetricsToConfig(shell, "window");

				//NICO catch the dispose event from file/quit on osx
				shell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent event) {
						if (!isAlreadyDead) {
							isDisposeFromListener = true;
							if (shell != null) {
								shell.removeDisposeListener(this);
								dispose(false, false);
							}
							isAlreadyDead = true;
						}
					}
				});

				shell.addShellListener(new ShellAdapter() {
					public void shellClosed(ShellEvent event) {
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter("Close To Tray",
										true)) {

							minimizeToTray(event);
						} else {
							event.doit = dispose(false, false);
						}
					}

					public void shellIconified(ShellEvent event) {
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter(
										"Minimize To Tray", false)) {

							minimizeToTray(event);
						}
					}

				});

				shell.addListener(SWT.Deiconify, new Listener() {
					public void handleEvent(Event e) {
						if (Constants.isOSX
								&& COConfigurationManager.getBooleanParameter(
										"Password enabled", false)) {
							e.doit = false;
							shell.setVisible(false);
							PasswordWindow.showPasswordWindow(display);
						}
					}
				});


				// Separator between menu and icon bar
				Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
				formData = new FormData();
				formData.top = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
				separator.setLayoutData(formData);

				attachToTopOf = separator;

				mainStatusBar = new MainStatusBar();
				Composite statusBar = mainStatusBar.initStatusBar(azureus_core,
						globalManager, display, shell);


				controlAboveFolder = attachToTopOf;
				controlBelowFolder = statusBar;

			}
	
    try {
    	Utils.createTorrentDropTarget(parent, true);
    } catch (Throwable e) {
    	Logger.log(new LogEvent(LOGID, "Drag and Drop not available", e));
    }
    

    
    try {
	    this.iconBar = new IconBar(parent);
	    this.iconBar.setCurrentEnabler(this);
	    
	    formData = new FormData();
	    if (attachToTopOf != null) {
	    	formData.top = new FormAttachment(attachToTopOf);
	    } else {
		    formData.top = new FormAttachment(0, 0);
	    }
	    formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
	    formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
	    this.iconBar.setLayoutData(formData);

	    attachToTopOf = iconBar.getCoolBar();

	    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
	    
	    formData = new FormData();
	    formData.top = new FormAttachment(attachToTopOf);
	    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
	    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
	    separator.setLayoutData(formData);
	    
			controlAboveFolder = separator;
    } catch (Exception e) {
    	Logger.log(new LogEvent(LOGID, "Creating Icon Bar", e));
    }

    
    if(!useCustomTab) {
      folder = new TabFolder(parent, SWT.V_SCROLL);
    } else {
      folder = new CTabFolder(parent, SWT.CLOSE | SWT.FLAT);
      final Color bg = ColorUtils.getShade(folder.getBackground(), (Constants.isOSX) ? -25 : -6);
      final Color fg = ColorUtils.getShade(folder.getForeground(), (Constants.isOSX) ? 25 : 6);
      folder.setBackground(bg);
      folder.setForeground(fg);
      ((CTabFolder)folder).setBorderVisible(false);
      folder.addDisposeListener(new DisposeListener() {
          public void widgetDisposed(DisposeEvent event) {
              bg.dispose();
              fg.dispose();
          }
      });
    }    
    
		formData = new FormData();
		if (controlAboveFolder == null) {
			formData.top = new FormAttachment(0,0); 
		} else {
			formData.top = new FormAttachment(controlAboveFolder);
		}
		
		if (controlBelowFolder == null) {
			formData.bottom = new FormAttachment(100,0); 
		} else {
			formData.bottom = new FormAttachment(controlBelowFolder);
		}
		formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
		formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
		folder.setLayoutData(formData);
		
    Tab.initialize(this, folder);
    
    folder.getDisplay().addFilter(SWT.KeyDown, new Listener() {
				public void handleEvent(Event event) {
					// Another window has control, skip filter
					Control focus_control = display.getFocusControl();
					if (focus_control != null && focus_control.getShell() != shell)
						return;

					int key = event.character;
					if ((event.stateMask & SWT.MOD1) != 0 && event.character <= 26
							&& event.character > 0)
						key += 'a' - 1;

					// ESC or CTRL+F4 closes current Tab
					if (key == SWT.ESC
							|| (event.keyCode == SWT.F4 && event.stateMask == SWT.CTRL)) {
						Tab.closeCurrent();
						event.doit = false;
					} else if (event.keyCode == SWT.F6
							|| (event.character == SWT.TAB && (event.stateMask & SWT.CTRL) != 0)) {
						// F6 or Ctrl-Tab selects next Tab
						// On Windows the tab key will not reach this filter, as it is
						// processed by the traversal TRAVERSE_TAB_NEXT.  It's unknown
						// what other OSes do, so the code is here in case we get TAB
						if ((event.stateMask & SWT.SHIFT) == 0) {
							event.doit = false;
							Tab.selectNextTab(true);
							// Shift+F6 or Ctrl+Shift+Tab selects previous Tab
						} else if (event.stateMask == SWT.SHIFT) {
							Tab.selectNextTab(false);
							event.doit = false;
						}
					} else if (key == 'l' && (event.stateMask & SWT.MOD1) != 0) {
						// Ctrl-L: Open URL
						OpenTorrentWindow.invokeURLPopup(shell, globalManager);
						event.doit = false;
					}
				}
			});

    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent event) {
        if(display != null && ! display.isDisposed())
        	Utils.execSWTThread(new AERunnable() {
	          public void runSupport() {
              if(useCustomTab) {
                CTabItem item = (CTabItem) event.item;
                if(item != null && ! item.isDisposed() && ! folder.isDisposed()) {
                  try {
                  ((CTabFolder)folder).setSelection(item);
                  Control control = item.getControl();
                  if (control != null) {
                    control.setVisible(true);
                    control.setFocus();
                  }
                  } catch(Throwable e) {
                  	Debug.printStackTrace( e );
                    //Do nothing
                  }
                }
              }
              if (iconBar != null)
									iconBar.setCurrentEnabler(MainWindow.this);
	          }
          });       
      }
    };
    
    if(!useCustomTab) {
      ((TabFolder)folder).addSelectionListener(selectionAdapter);
    } else {
      try {
        ((CTabFolder)folder).setMinimumCharacters( 75 );
      } catch (Exception e) {
      	Logger.log(new LogEvent(LOGID, "Can't set MIN_TAB_WIDTH", e));
      }      
      //try {
      ///  TabFolder2ListenerAdder.add((CTabFolder)folder);
      //} catch (NoClassDefFoundError e) {
        ((CTabFolder)folder).addCTabFolderListener(new CTabFolderAdapter() {          
          public void itemClosed(CTabFolderEvent event) {
            Tab.closed((CTabItem) event.item);
            event.doit = true;
            ((CTabItem) event.item).dispose();
          }
        });
      //}

      ((CTabFolder)folder).addSelectionListener(selectionAdapter);

      try {
        ((CTabFolder)folder).setSelectionBackground(
                new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND), 
                             display.getSystemColor(SWT.COLOR_LIST_BACKGROUND), 
                             display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND) },
                new int[] {10, 90}, true);
      } catch (NoSuchMethodError e) {
        /** < SWT 3.0M8 **/
        ((CTabFolder)folder).setSelectionBackground(new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND) },
                                                    new int[0]);
      }
      ((CTabFolder)folder).setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

      try {
        /* Pre 3.0M8 doesn't have Simple-mode (it's always simple mode)
           in 3.0M9, it was called setSimpleTab(boolean)
           in 3.0RC1, it's called setSimple(boolean)
           Prepare for the future, and use setSimple()
         */
        ((CTabFolder)folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
      } catch (NoSuchMethodError e) { 
        /** < SWT 3.0RC1 **/ 
      }
    }

    
    if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Initializing GUI complete"));
   
    globalManager.addListener(this);

			azureus_core.getPluginManager().firePluginEvent(
					PluginEvent.PEV_CONFIGURATION_WIZARD_STARTS);

			if (!COConfigurationManager
					.getBooleanParameter("Wizard Completed", false)) {
				ConfigureWizard wizard = new ConfigureWizard(getAzureusCore(), display);


				wizard.addListener(new WizardListener() {
					public void closed() {
						azureus_core.getPluginManager().firePluginEvent(
								PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES);
					}
				});
			} else {

				azureus_core.getPluginManager().firePluginEvent(
						PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES);
			}

			// attach the UI to plugins
			// Must be done before initializing views, since plugins may register
			// table columns and other objects
			uiSWTInstanceImpl = new UISWTInstanceImpl(azureus_core);

			if (azureus_core.getTrackerHost().getTorrents().length > 0) {
				showMyTracker();
			}

			showMyTorrents();

			//  share progress window

			new ProgressWindow();

			if (COConfigurationManager.getBooleanParameter("Open Console", false)) {
				showConsole();
			}
			events = null;

			if (COConfigurationManager.getBooleanParameter("Open Config", false)) {
				showConfig();
			}

			if (COConfigurationManager.getBooleanParameter("Open Stats On Start",
					false)) {
				showStats();
			}

			COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);

			updater = new GUIUpdater(this);
			updater.start();

		} catch (Throwable e) {
			Debug.printStackTrace(e);
		}

    showMainWindow();
}
  
	private void showMainWindow() {
		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager
				.getBooleanParameter("Enable System Tray")
				&& (!Constants.isOSX || SWT.getVersion() > 3300);
		boolean bPassworded = COConfigurationManager.getBooleanParameter(
				"Password enabled", false);
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter(
						"Start Minimized", false));

		if (!bStartMinimize) {
	    shell.layout();
	    shell.open();
			if (!Constants.isOSX) {
				shell.forceActive();
			}
		} else if (Constants.isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}

		if (bEnableTray) {

			try {
				systemTraySWT = new SystemTraySWT();

			} catch (Throwable e) {

				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"Upgrade to SWT3.0M8 or later for system tray support."));
			}

			if (bStartMinimize) {
				minimizeToTray(null);
			}
			//Only show the password if not started minimized
			//Correct bug #878227
			else {
				if (bPassworded) {
					minimizeToTray(null);
					setVisible(true); // invokes password
				}
			}
		}

		COConfigurationManager.addAndFireParameterListener("Show Download Basket",
				this);

		checkForWhatsNewWindow();

		// check file associations   
		AssociationChecker.checkAssociations();
		DonationWindow2.checkForDonationPopup();
	}


  protected void showMyTracker() {
  	if (my_tracker_tab == null) {
  		my_tracker_tab = new Tab(new MyTrackerView(azureus_core));
  		my_tracker_tab.getView().getComposite().addDisposeListener(new DisposeListener() {
      	public void widgetDisposed(DisposeEvent e) {
      		my_tracker_tab = null;
      	}
      });
  	} else {
  		my_tracker_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  protected void 
  showMyShares() 
  {
  	if (my_shares_tab == null) {
  		my_shares_tab = new Tab(new MySharesView(azureus_core));
  		my_shares_tab.getView().getComposite().addDisposeListener(new DisposeListener() {
      	public void widgetDisposed(DisposeEvent e) {
      		my_shares_tab = null;
      	}
      });
  	} else {
  		my_shares_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  protected void showMyTorrents() {
    if (mytorrents == null) {
    	MyTorrentsSuperView view = new MyTorrentsSuperView(azureus_core);
      mytorrents = new Tab(view);
      mytorrents.getView().getComposite().addDisposeListener(new DisposeListener() {
      	public void widgetDisposed(DisposeEvent e) {
      		mytorrents = null;
      	}
      });
    } else {
      mytorrents.setFocus();
    }
    refreshIconBar();
  }
	
  private void minimizeToTray(ShellEvent event) {
    //Added this test so that we can call this method with null parameter.
    if (event != null)
      event.doit = false;
    if(Constants.isOSX) {
    	shell.setMinimized(true);
    } else {  
    	shell.setVisible(false);
    }
    if (downloadBasket != null)
      downloadBasket.setVisible(true);
    try{
    	downloadBars_mon.enter();
      Iterator iter = downloadBars.values().iterator();
      while (iter.hasNext()) {
        MinimizedWindow mw = (MinimizedWindow) iter.next();
        mw.setVisible(true);
      }
    }finally{
    	downloadBars_mon.exit();
    }
  }
  
  private void
  updateComponents()
  {
  	if (mainStatusBar != null)
  		mainStatusBar.refreshStatusText();

  	if (folder != null) {
  		if(useCustomTab) {
  			((CTabFolder)folder).update();
  		} else {
  			((TabFolder)folder).update();
  		}
  	}
  }

  protected void closeDownloadBars() {
    Utils.execSWTThread(new AERunnable() {

      public void runSupport() {
        if (display == null || display.isDisposed())
          return;

        try{
        	downloadBars_mon.enter();
        
          Iterator iter = downloadBars.keySet().iterator();
          while (iter.hasNext()) {
            DownloadManager dm = (DownloadManager) iter.next();
            MinimizedWindow mw = (MinimizedWindow) downloadBars.get(dm);
            mw.close();
            iter.remove();
          }
        }finally{
        	
        	downloadBars_mon.exit();
        }
      }

    });
  }

  public boolean
  destroyRequest()
  {
	  Logger.log(new LogEvent(LOGID, "MainWindow::destroyRequest"));

	  if ( COConfigurationManager.getBooleanParameter("Password enabled", false )){
		  
	  	if (!PasswordWindow.showPasswordWindow(display)) {
		  	Logger.log(new LogEvent(LOGID, "    denied - password is enabled"));
	
			  return false;
	  	}
	  }
	  
	  Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					dispose( false, false );
				}
			});
	  return true;
  }

	// globalmanagerlistener
	
  public void
  destroyed()
  {
  }
  
  public void
  destroyInitiated()
  {
  }				
  
  public void seedingStatusChanged( boolean seeding_only_mode ){
  }       
  
  public void 
  downloadManagerAdded(
  	final DownloadManager created) 
  {
    
    DonationWindow2.checkForDonationPopup();
      
    created.addListener(this);
  }

  protected void openManagerView(DownloadManager downloadManager) {
    try{
    	downloadViews_mon.enter();
    
      if (downloadViews.containsKey(downloadManager)) {
        Tab tab = (Tab) downloadViews.get(downloadManager);
        tab.setFocus();
        refreshIconBar();
      }
      else {
        Tab tab = new Tab(new ManagerView(azureus_core, downloadManager));
        downloadViews.put(downloadManager, tab);
      }
    }finally{
    	
    	downloadViews_mon.exit();
    }
  }

  protected void removeManagerView(DownloadManager downloadManager) {
    try{
    	downloadViews_mon.enter();
      
    	downloadViews.remove(downloadManager);
    }finally{
    	
    	downloadViews_mon.exit();
    }
  }

   public void downloadManagerRemoved(DownloadManager removed) {
    try{
    	downloadViews_mon.enter();
    
      if (downloadViews.containsKey(removed)) {
        final Tab tab = (Tab) downloadViews.get(removed);
        Utils.execSWTThread(new AERunnable(){
          public void runSupport() {
            if (display == null || display.isDisposed())
              return;

            tab.dispose();
          }
        });

      }
    }finally{
    	
    	downloadViews_mon.exit();
    }
  }

  public Display getDisplay() {
    return this.display;
  }

  public Shell getShell() {
    return shell;
  }

  public void setVisible(final boolean visible) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (visible) {
					if (COConfigurationManager.getBooleanParameter("Password enabled",
							false)) {
						if (!PasswordWindow.showPasswordWindow(display)) {
							shell.setVisible(false);
							return;
						}
					}
				}

				shell.setVisible(visible);
				if (visible) {
					if (downloadBasket != null) {
						downloadBasket.setVisible(false);
						downloadBasket.setMoving(false);
					}

					/*
					 if (trayIcon != null)
					 trayIcon.showIcon();
					 */
					shell.forceActive();
					shell.setMinimized(false);
				}

			}
		});
	}

  public boolean isVisible() {
    return shell.isVisible();
  }

  public boolean 
  dispose(
  	boolean	for_restart,
	boolean	close_already_in_progress ) 
  {
    if(COConfigurationManager.getBooleanParameter("confirmationOnExit", false) && !getExitConfirmation(for_restart))
      return false;
    
    if(systemTraySWT != null) {
      systemTraySWT.dispose();
    }
    
    // close all tabs
    Tab.closeAllTabs();

    isAlreadyDead = true; //NICO try to never die twice...
    /*
    if (this.trayIcon != null)
      SysTrayMenu.dispose();
    */

    if(updater != null){
    	
      updater.stopIt();
    }
    
    if (initializer != null) {
    	initializer.stopIt( for_restart, close_already_in_progress );
    }

    //NICO swt disposes the mainWindow all by itself (thanks... ;-( ) on macosx
    if(!shell.isDisposed() && !isDisposeFromListener) {
    	shell.dispose();
    }
      
    
    COConfigurationManager.removeParameterListener( "config.style.useSIUnits", this );
    COConfigurationManager.removeParameterListener( "Show Download Basket", this );
    COConfigurationManager.removeParameterListener( "GUI_SWT_bFancyTab", this );
    
    
    	// problem with closing down web start as AWT threads don't close properly
	if ( SystemProperties.isJavaWebStartInstance()){    	
 	
		Thread close = new AEThread( "JWS Force Terminate")
			{
				public void
				runSupport()
				{
					try{
						Thread.sleep(2500);
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
					
					SESecurityManager.exitVM(1);
				}
			};
			
		close.setDaemon(true);
		
		close.start();
    	
    }
    
    return true;
  }

  /**
   * @return true, if the user choosed OK in the exit dialog
   *
   * @author Rene Leonhardt
   */
  private boolean 
  getExitConfirmation(
  	boolean	for_restart) {
    MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
    
    mb.setText(MessageText.getString(
    		for_restart?"MainWindow.dialog.restartconfirmation.title":"MainWindow.dialog.exitconfirmation.title"));
    
    mb.setMessage(MessageText.getString(
    		for_restart?"MainWindow.dialog.restartconfirmation.text":"MainWindow.dialog.exitconfirmation.text"));
    if(mb.open() == SWT.YES)
      return true;
    return false;
  }

  public GlobalManager getGlobalManager() {
    return globalManager;
  }

  /**
	 * @return
	 */
  public static MainWindow getWindow() {
    return window;
  }

  /**
	 * @return
	 */
  public HashMap getDownloadBars() {
    return downloadBars;
  }

  /**
	 * @return
	 */
  public TrayWindow getTray() {
    return downloadBasket;
  }



  /**
   * @return Returns the useCustomTab.
   */
  public boolean isUseCustomTab() {
    return useCustomTab;
  }    
  
  
  
  
  Map pluginTabs = new HashMap();
  
  
  protected void openPluginView(String sParentID, String sViewID, UISWTViewEventListener l,
			Object dataSource, boolean bSetFocus) {
  	
  	UISWTViewImpl view = null;
  	try {
  		view = new UISWTViewImpl(sParentID, sViewID, l);
  	} catch (Exception e) {
  		Tab tab = (Tab) pluginTabs.get(sViewID);
  		if (tab != null) {
  			tab.setFocus();
  		}
			return;
  	}
		view.dataSourceChanged(dataSource);

		Tab tab = new Tab(view, bSetFocus);

 		pluginTabs.put(sViewID, tab);
	}
  
  /**
   * Close all plugin views with the specified ID
   * 
   * @param sViewID
   */
  public void closePluginViews(String sViewID) {
  	Item[] items;

		if (folder instanceof CTabFolder)
			items = ((CTabFolder) folder).getItems();
		else if (folder instanceof TabFolder)
			items = ((TabFolder) folder).getItems();
		else
			return;

		for (int i = 0; i < items.length; i++) {
			IView view = Tab.getView(items[i]);
			if (view instanceof UISWTViewImpl) {
				String sID = ((UISWTViewImpl) view).getViewID();
				if (sID != null && sID.equals(sViewID)) {
					try {
						closePluginView(view);
					} catch (Exception e) {
						Debug.printStackTrace(e);
					}
				}
			}
		} // for
  }
  
  /**
   * Get all open Plugin Views
   * 
   * @return open plugin views
   */
  protected UISWTView[] getPluginViews() {
  	Item[] items;

		if (folder instanceof CTabFolder)
			items = ((CTabFolder) folder).getItems();
		else if (folder instanceof TabFolder)
			items = ((TabFolder) folder).getItems();
		else
			return new UISWTView[0];

		ArrayList views = new ArrayList();
		
		for (int i = 0; i < items.length; i++) {
			IView view = Tab.getView(items[i]);
			if (view instanceof UISWTViewImpl) {
				views.add(view);
			}
		} // for
		
		return (UISWTView[])views.toArray(new UISWTView[0]);
  }

  protected void openPluginView(final AbstractIView view, final String name) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Tab tab = (Tab) pluginTabs.get(name);
				if (tab != null) {
					tab.setFocus();
				} else {
					tab = new Tab(view);
					pluginTabs.put(name, tab);
				}
			}
		});
	}
  
  protected void 
  closePluginView( 
	IView	view) 
  {
	  Item	tab = Tab.getTab( view );
	  
	  if ( tab != null ){
		  
		  Tab.closed( tab );
	  }
  }
  
  public void removeActivePluginView( String view_name ) {
    pluginTabs.remove(view_name);
  }
  
 


  
  public void parameterChanged(String parameterName) {
    if( parameterName.equals( "Show Download Basket" ) ) {
      if (COConfigurationManager.getBooleanParameter("Show Download Basket")) {
        if(downloadBasket == null) {
          downloadBasket = new TrayWindow(this);
          downloadBasket.setVisible(true);
        }
      } else if(downloadBasket != null) {
        downloadBasket.setVisible(false);
        downloadBasket = null;
      }
    }
    
    if( parameterName.equals( "GUI_SWT_bFancyTab" ) && 
        folder instanceof CTabFolder && 
        folder != null && !folder.isDisposed()) {
      try {
        ((CTabFolder)folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
      } catch (NoSuchMethodError e) { 
        /** < SWT 3.0RC1 **/ 
      }
    }
    
    if( parameterName.equals( "config.style.useSIUnits" ) ) {
      updateComponents();
    }
  }
  
 


  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("open"))
      return true;
    if(itemKey.equals("new"))
      return true;
    IView currentView = getCurrentView();
    if(currentView != null)
      return currentView.isEnabled(itemKey);
    return false;
  }

  public boolean isSelected(String itemKey) {   
    return false;
  }

  public void itemActivated(String itemKey) {   
    if(itemKey.equals("open")) {        
     TorrentOpener.openTorrentWindow();
     return;
    }
    if(itemKey.equals("new")) {
      new NewTorrentWizard(getAzureusCore(),display);
      return;
    }
    IView currentView = getCurrentView();
    if(currentView != null)
      currentView.itemActivated(itemKey);    
  }
  
  IView getCurrentView() {
	  try {
	    if(!useCustomTab) {
	      TabItem[] selection = ((TabFolder)folder).getSelection();
				if(selection.length > 0)  {
				  return Tab.getView(selection[0]);
				}
			  return null;
	    }
      return Tab.getView(((CTabFolder)folder).getSelection());
	  }
	  catch (Exception e) {
	    return null;
	  }
  }

	public void refreshIconBar() {
		if (iconBar != null)
			iconBar.setCurrentEnabler(this);
	}

  public void close() {
      getShell().close();
  }

  public void closeViewOrWindow() {
      if(getCurrentView() != null)
        Tab.closeCurrent();
      else
          close();
  }

  protected ConfigView showConfig() {
    if (config == null){
      config_view = new ConfigView( azureus_core );
      config = new Tab(config_view);
      config_view.getComposite().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					config = null;
					config_view = null;
				}
			});
    }else{
      config.setFocus();
    }
    return config_view;
  }
  

  protected boolean showConfig(String id) {
    if (config == null){
      config_view = new ConfigView( azureus_core );
      config = new Tab(config_view);
    }else{
      config.setFocus();
    }
    if (id == null) {
    	return true;
    }
    return config_view.selectSection(id);
  }
  

  
  public void showConsole() {
    if (console == null) {
      console = new Tab(new LoggerView(events));
      console.getView().getComposite().addDisposeListener(new DisposeListener() {
      	public void widgetDisposed(DisposeEvent e) {
      		console = null;
      	}
      });
    } else {
      console.setFocus();
    }
  }
  
  protected void showStats() {
    if (stats_tab == null) {
      stats_tab = new Tab(new StatsView(globalManager,azureus_core));
      stats_tab.getView().getComposite().addDisposeListener(new DisposeListener() {
      	public void widgetDisposed(DisposeEvent e) {
					stats_tab = null;
				}
			});
    } else {
      stats_tab.setFocus();
    }
  }

  protected void showStatsDHT() {
  	showStats();
  	if (stats_tab == null) {
  		return;
  	}
		((StatsView) stats_tab.getView()).showDHT();
  }
  
  protected void showStatsTransfers() {
  	showStats();
  	if (stats_tab == null) {
  		return;
  	}
		((StatsView) stats_tab.getView()).showTransfers();
  }

  public void setSelectedLanguageItem() 
  {
  	try{
  		this_mon.enter();
  	
	    Messages.updateLanguageForControl(shell);
	    
	    if ( systemTraySWT != null ){
	    	systemTraySWT.updateLanguage();
	    }
	    
	  	if (mainStatusBar != null) {
	  		mainStatusBar.refreshStatusText();
  		}

	    
	    if (folder != null) {
	      if(useCustomTab) {
	        ((CTabFolder)folder).update();
	      } else {
	        ((TabFolder)folder).update();
	      }
	    }
	
	    if (downloadBasket != null){
	      downloadBasket.updateLanguage();
	    }
	    
	    Tab.updateLanguage();
	  
	  	if (mainStatusBar != null) {
	  		mainStatusBar.updateStatusText();
  		}
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  public MainMenu getMenu() {
    return mainMenu;
  }
  
  
    
  /**
   * MUST be called by the SWT Thread
   * @param updateWindow the updateWindow or null if no update is available
   */
  public void setUpdateNeeded(UpdateWindow updateWindow) {
    if (mainStatusBar != null) {
    	mainStatusBar.setUpdateNeeded(updateWindow);
    }
  }
  
  //DownloadManagerListener implementation

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
    // Do Nothing
  }
  
  public void
  filePriorityChanged( DownloadManager download, org.gudy.azureus2.core3.disk.DiskManagerFileInfo file )
  {	  
  }
  
  public void downloadComplete(DownloadManager manager) {
    // Do Nothing

  }

  public void positionChanged(DownloadManager download, int oldPosition,
      int newPosition) {
    // Do Nothing

  }

  public void stateChanged(final DownloadManager manager, int state) {
    // if state == STARTED, then open the details window (according to config)
    if(state == DownloadManager.STATE_DOWNLOADING || state == DownloadManager.STATE_SEEDING) {
        if(display != null && !display.isDisposed()) {
        	Utils.execSWTThread(new AERunnable() {
            public void runSupport() {
            	if (display == null || display.isDisposed())
            		return;

              if (COConfigurationManager.getBooleanParameter("Open Details",false)) {
                openManagerView(manager);
              }
              
              if (COConfigurationManager.getBooleanParameter("Open Bar", false)) {
                try{
                	downloadBars_mon.enter();
                
                	if(downloadBars.get(manager) == null) {
                	  MinimizedWindow mw = new MinimizedWindow(manager, shell);
                	
                	  downloadBars.put(manager, mw);
                	}
                }finally{
                	
                	downloadBars_mon.exit();
                }
              }
            }
          });
        }
    }
  }
  
  public AzureusCore
  getAzureusCore()
  {
  	return( azureus_core );
  }
  
  
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println("SWT UI");

		try {
			writer.indent();

			writer.println("SWT Version:" + SWT.getVersion() + "/"
					+ SWT.getPlatform());

			writer.println("MyTorrents");

			Tab t = mytorrents;
			if (t != null) {
				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}

			t = my_tracker_tab;
			if (t != null) {
				writer.println("MyTracker");

				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}

			t = my_shares_tab;
			if (t != null) {
				writer.println("MyShares");

				try {
					writer.indent();

					t.generateDiagnostics(writer);
				} finally {

					writer.exdent();
				}
			}
			
			TableColumnManager.getInstance().generateDiagnostics(writer);
		} finally {

			writer.exdent();
		}
	}
  
  private void checkForWhatsNewWindow() {
    try {
      int version = WelcomeWindow.WELCOME_VERSION;
      int latestDisplayed = COConfigurationManager.getIntParameter("welcome.version.lastshown",0);
      if(latestDisplayed < version) {
        new WelcomeWindow();
        COConfigurationManager.setParameter("welcome.version.lastshown",version);
        COConfigurationManager.save();
      }      
    } catch(Exception e) {
      //DOo Nothing
    }    
  }
  
  public UISWTInstanceImpl getUISWTInstanceImpl() {
  	return uiSWTInstanceImpl;
  }

	/**
	 * @param string
	 */
	protected void setStatusText(String string) {
		// TODO Auto-generated method stub
		if (mainStatusBar != null)
			mainStatusBar.setStatusText(string);
	}

	public SystemTraySWT getSystemTraySWT() {
		return systemTraySWT;
	}

	public MainStatusBar getMainStatusBar() {
		return mainStatusBar;
	}

	public Image generateObfusticatedImage() {
		Image image;

		IView[] allViews = Tab.getAllViews();
		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];
			
			if (view instanceof ObfusticateTab) {
				Item tab = Tab.getTab(view);
				tab.setText(((ObfusticateTab)view).getObfusticatedHeader());
				folder.update();
			}
		}
		
		Rectangle clientArea = shell.getClientArea();
		image = new Image(display, clientArea.width, clientArea.height);
		
		GC gc = new GC(shell);
		try {
			gc.copyArea(image, clientArea.x, clientArea.y);
		} finally {
			gc.dispose();
		}
		
		IView currentView = getCurrentView();

		if (currentView instanceof ObfusticateImage) {
			Point ofs = shell.toDisplay(clientArea.x, clientArea.y);
			((ObfusticateImage)currentView).obfusticatedImage(image, ofs);
		}

		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];
			
			if (view instanceof ObfusticateTab) {
				view.refresh();
			}
		}

		return image;
	}
	
	private static Point getStoredWindowSize() {
		Point size = null;

		boolean isMaximized = COConfigurationManager.getBooleanParameter(
				"window.maximized", false);
		if (isMaximized) {
			Display current = Display.getCurrent();
			if (current != null) {
				Rectangle clientArea = current.getClientArea();
				size = new Point(clientArea.width, clientArea.height);
				return size;
			}
		}

		String windowRectangle = COConfigurationManager.getStringParameter(
				"window.rectangle", null);
		if (windowRectangle != null) {
			String[] values = windowRectangle.split(",");
			if (values.length == 4) {
				try {
					size = new Point(Integer.parseInt(values[2]),
							Integer.parseInt(values[3]));
				} catch (Exception e) {
				}
			}
		}
		return size;
	}
	
	public static void addToVersionCheckMessage(final Map map) {
		try {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					Point size = null;
					
					if (window != null) {
						final Shell shell = window.getShell();
						if (shell != null && !shell.getMinimized()) {
							size = shell.getSize();
						}
					}

					if (size == null) {
						size = getStoredWindowSize();
						if (size == null) {
							return;
						}
					}
					map.put("mainwindow.w", new Long(size.x));
					map.put("mainwindow.h", new Long(size.y));
				}

			}, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
