 /*
 * Created on Jun 25, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Modified Apr 17, 2004 by Olivier Chalouhi (OSX system menu)
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.TorrentFolderWatcher;
import org.gudy.azureus2.core3.disk.TorrentFolderWatcher.FolderWatcher;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.BlockedIp;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.startup.STProgressListener;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.local.*;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.donations.DonationWindow2;
import org.gudy.azureus2.ui.swt.wizard.WizardListener;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.BlockedIpsWindow;
import org.gudy.azureus2.ui.swt.IconBar;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.PasswordWindow;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.TrayWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.systray.SystemTraySWT;
import org.gudy.azureus2.ui.swt.sharing.*;
import org.gudy.azureus2.ui.swt.sharing.progress.*;

//import snoozesoft.systray4j.SysTrayMenu;

/**
 * @author Olivier
 * Runnable : so that GUI initialization is done via asyncExec(this)
 * STProgressListener : To make it visible once initialization is done
 */
public class MainWindow implements GlobalManagerListener, ParameterListener, IconBarEnabler, STProgressListener, Runnable {

  public static final String VERSION = Constants.AZUREUS_VERSION;
  private static final int DONATIONS_ASK_AFTER = 168;
  private static final int RECOMMENDED_SWT_VERSION = 3044; // M8 (M7 = 3038)

  private static MainWindow window;

  private GUIUpdater updater;

  private Display display;
  private Shell mainWindow;
  
  private IconBar iconBar;

  //NICO handle swt on macosx
  public static boolean isAlreadyDead = false;
  public static boolean isDisposeFromListener = false;  

  private boolean useCustomTab;
  
  //private TabFolder folder;
  private Composite folder;
  
  private MainMenu mainMenu;
  
  private CLabel statusText;
  private String statusTextKey;
  
  private CLabel ipBlocked;
  private CLabel statusDown;
  private CLabel statusUp;

  private GlobalManager 			globalManager;
    
  private Tab 	mytorrents;
  private IView viewMyTorrents;
  
  private Tab 	my_tracker_tab;
  private IView my_tracker_view;
  
  private Tab 	my_shares_tab;
  private IView my_shares_view;
  
  private Tab 	stats_tab;
  
  
  private Tab console;
  private Tab config;

  

  private TrayWindow tray;
  //private SystemTray trayIcon;
  private SystemTraySWT systemTraySWT;
  

  private HashMap downloadViews;
  private HashMap downloadBars;

  private Initializer initializer;
  
  private FolderWatcher folderWatcher = null;
  
  

  private class GUIUpdater extends Thread implements ParameterListener {
    boolean finished = false;
    boolean refreshed = true;
    
    int waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
    boolean alwaysRefreshMyTorrents = COConfigurationManager.getBooleanParameter("config.style.refreshMT");
    
    public GUIUpdater() {
      super("GUI updater"); //$NON-NLS-1$
      setPriority(Thread.MAX_PRIORITY);
      COConfigurationManager.addParameterListener("GUI Refresh", this);
      COConfigurationManager.addParameterListener("config.style.refreshMT", this);
    }

    public void run() {
      while (!finished) {
        if(refreshed)
          update();
        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * @param parameterName the name of the parameter that has changed
     * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
     */
    public void parameterChanged(String parameterName) {
      waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
      alwaysRefreshMyTorrents = COConfigurationManager.getBooleanParameter("config.style.refreshMT");
    }

    private void update() {
      refreshed = false;
      if (display != null && !display.isDisposed())
        display.asyncExec(new Runnable() {
        public void run() {
          try {
            IView view = null;
            if (!mainWindow.isDisposed() && mainWindow.isVisible() && !mainWindow.getMinimized()) {
  
              view = getCurrentView();
              
              if (view != null) {
                view.refresh();
                Tab.refresh();
              }
  
              ipBlocked.setText( "{"+DisplayFormatters.formatDateShort(IpFilter.getInstance().getLastUpdateTime()) + "} IPs: " + IpFilter.getInstance().getNbRanges() + " - " + IpFilter.getInstance().getNbIpsBlocked());
              statusDown.setText(MessageText.getString("ConfigView.download.abbreviated") + " " + DisplayFormatters.formatByteCountToKiBEtcPerSec(globalManager.getStats().getDownloadAverage())); //$NON-NLS-1$
              statusUp.setText(MessageText.getString("ConfigView.upload.abbreviated") + " " + DisplayFormatters.formatByteCountToKiBEtcPerSec(globalManager.getStats().getUploadAverage())); //$NON-NLS-1$
  					}
  
            if (!mainWindow.isDisposed() && alwaysRefreshMyTorrents) {            
              if (mytorrents != null) {
                try {
                  viewMyTorrents = Tab.getView(mytorrents.getTabItem());
                } catch (Exception e) {
                  viewMyTorrents = null;
                }
                if (viewMyTorrents != null && viewMyTorrents != view) {
                  viewMyTorrents.refresh();
                }
              }
            }
            
            //if (trayIcon != null)
            //  trayIcon.refresh();
            
            if(systemTraySWT != null)
              systemTraySWT.update();
            
            synchronized (downloadBars) {
              Iterator iter = downloadBars.values().iterator();
              while (iter.hasNext()) {
                MinimizedWindow mw = (MinimizedWindow) iter.next();
                mw.refresh();
              }
            }
          } catch (Exception e) {
            LGLogger.log(LGLogger.ERROR, "Error while trying to update GUI");
            e.printStackTrace();
          } finally {
            refreshed = true;
          }
        }        
      });
    }

    public void stopIt() {
      finished = true;
      COConfigurationManager.removeParameterListener("GUI Refresh", this);
      COConfigurationManager.removeParameterListener("config.style.refreshMT", this);
    }
  }


  public MainWindow(GlobalManager gm, Initializer initializer) {    
    LGLogger.log("MainWindow start");
    this.globalManager = gm;
    this.initializer = initializer;
    this.display = SWTThread.getInstance().getDisplay();
    window = this;
    initializer.addListener(this);
    display.syncExec(this);
  }
  
  public void run() {
    try{
       
    useCustomTab = COConfigurationManager.getBooleanParameter("useCustomTab");
    

    COConfigurationManager.addParameterListener( "config.style.useSIUnits",
    	new ParameterListener()
    		{
    			public void
    			parameterChanged(
    				String	value )
    			{
    				updateComponents();
    			}
    	});
  
    mytorrents = null;
    my_tracker_tab	= null;
    console = null;
    config = null;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    
    //The Main Window
    mainWindow = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE | SWT.MAX | SWT.MIN);
    mainWindow.setText("Azureus"); //$NON-NLS-1$
    mainWindow.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    
    
    //The Torrent Opener
    TorrentOpener.init(mainWindow,globalManager);
    
    mainMenu = new MainMenu(this);
    mainMenu.
    buildMenu(MessageText.getLocales());

    createDropTarget(mainWindow);

    FormLayout mainLayout = new FormLayout(); 
    FormData formData;
    
    mainLayout.marginHeight = 0;
    mainLayout.marginWidth = 0;
    try {
      mainLayout.spacing = 0;
    } catch (NoSuchFieldError e) { /* Pre SWT 3.0 */ }
    mainWindow.setLayout(mainLayout);
    
    Label separator = new Label(mainWindow,SWT.SEPARATOR | SWT.HORIZONTAL);
    formData = new FormData();
    formData.top = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
    separator.setLayoutData(formData);

    this.iconBar = new IconBar(mainWindow);
    this.iconBar.setCurrentEnabler(this);
    
    formData = new FormData();
    formData.top = new FormAttachment(separator);
    formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
    this.iconBar.setLayoutData(formData);

    separator = new Label(mainWindow,SWT.SEPARATOR | SWT.HORIZONTAL);

    formData = new FormData();
    formData.top = new FormAttachment(iconBar.getCoolBar());
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    separator.setLayoutData(formData);
        
    if(!useCustomTab) {
      folder = new TabFolder(mainWindow, SWT.V_SCROLL);
    } else {
      folder = new CTabFolder(mainWindow, SWT.CLOSE | SWT.FLAT);
    }    
    
    Tab.setFolder(folder);   
    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent event) {
        if(display != null && ! display.isDisposed())
          display.asyncExec(new Runnable() {
	          public void run() {
              if(useCustomTab) {
                CTabItem item = (CTabItem) event.item;
                if(item != null && ! item.isDisposed() && ! folder.isDisposed()) {
                  try {
                  ((CTabFolder)folder).setTabPosition(((CTabFolder)folder).indexOf(item));
                  ((CTabFolder)folder).setSelection(item);
                  } catch(Throwable e) {
                    //Do nothing
                  }
                }
              }    
	            iconBar.setCurrentEnabler(MainWindow.this);
	          }
          });       
      }
    };
    
    if(!useCustomTab) {
      Tab.addTabKeyListenerToComposite(folder);
      ((TabFolder)folder).addSelectionListener(selectionAdapter);
    } else {
      try {
        ((CTabFolder)folder).MIN_TAB_WIDTH = 75;
      } catch (Exception e) {
        LGLogger.log(LGLogger.ERROR, "Can't set MIN_TAB_WIDTH");
        e.printStackTrace();
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

      try {
        ((CTabFolder)folder).setUnselectedCloseVisible(false);
      } catch (NoSuchMethodError e) { /** < SWT 3.0M8 **/ }
      ((CTabFolder)folder).addSelectionListener(selectionAdapter);

      try {
        ((CTabFolder)folder).setSelectionBackground(
                new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND), 
                             display.getSystemColor(SWT.COLOR_LIST_BACKGROUND), 
                             folder.getBackground() },
                new int[] {10, 90}, true);
      } catch (NoSuchMethodError e) { 
        /** < SWT 3.0M8 **/ 
        ((CTabFolder)folder).setSelectionBackground(new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND) },
                                                    new int[0]);
      }
      ((CTabFolder)folder).setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

      try {
        ((CTabFolder)folder).setSimpleTab(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
      } catch (NoSuchMethodError e) { 
        /** < SWT 3.0M8 **/ 
      }
    }
    

    Composite statusBar = new Composite(mainWindow, SWT.SHADOW_IN);
    formData = new FormData();
    formData.bottom = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
    formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
    statusBar.setLayoutData(formData);
    
    formData = new FormData();
    formData.top = new FormAttachment(separator);
    formData.bottom = new FormAttachment(statusBar);
    formData.left = new FormAttachment(0, 0);  // 2 params for Pre SWT 3.0
    formData.right = new FormAttachment(100, 0);  // 2 params for Pre SWT 3.0
    folder.setLayoutData(formData);
    

    GridLayout layout_status = new GridLayout();
    layout_status.numColumns = 4;
    layout_status.horizontalSpacing = 1;
    layout_status.verticalSpacing = 0;
    layout_status.marginHeight = 0;
    layout_status.marginWidth = 0;
    statusBar.setLayout(layout_status);

    GridData gridData;
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    statusText = new CLabel(statusBar, SWT.SHADOW_IN);
    statusText.setLayoutData(gridData);

    

    gridData = new GridData();
    gridData.widthHint = 220;
    ipBlocked = new CLabel(statusBar, SWT.SHADOW_IN);
    ipBlocked.setText("{} IPs:"); //$NON-NLS-1$
    ipBlocked.setLayoutData(gridData);
    Messages.setLanguageText(ipBlocked,"MainWindow.IPs.tooltip");
    ipBlocked.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
       showBlockedIps();
      }
    });
    
    gridData = new GridData();
    gridData.widthHint = 105;
    statusDown = new CLabel(statusBar, SWT.SHADOW_IN);
    statusDown.setText(/*MessageText.getString("ConfigView.download.abbreviated") +*/ "n/a");
    statusDown.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 105;
    statusUp = new CLabel(statusBar, SWT.SHADOW_IN);
    statusUp.setText(/*MessageText.getString("ConfigView.upload.abbreviated") +*/ "n/a");
    statusUp.setLayoutData(gridData);
    
    final Menu menuUpSpeed = new Menu(mainWindow,SWT.POP_UP);
    
    menuUpSpeed.addListener(SWT.Show,new Listener() {
      public void handleEvent(Event e) {
        MenuItem[] items = menuUpSpeed.getItems();
        for(int i = 0 ; i < items.length ; i++) {
         items[i].dispose(); 
        }
        
        int upLimit = COConfigurationManager.getIntParameter("Max Upload Speed KBs",0);
        
        MenuItem item = new MenuItem(menuUpSpeed,SWT.RADIO);
        item.setText(MessageText.getString("ConfigView.unlimited"));
        item.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            COConfigurationManager.setParameter("Max Upload Speed KBs",0); 
          }
        });
        if(upLimit == 0) item.setSelection(true);
        
        final Listener speedChangeListener = new Listener() {
              public void handleEvent(Event e) {
                int iSpeed = ((Long)((MenuItem)e.widget).getData("speed")).intValue();
                COConfigurationManager.setParameter("Max Upload Speed KBs", iSpeed);
              }
            };

        int iRel = 0;
        for (int i = 0; i < 12; i++) {
          int[] iAboveBelow;
          if (iRel == 0) {
            iAboveBelow = new int[] { upLimit };
          } else {
            iAboveBelow = new int[] { upLimit - iRel, upLimit + iRel };
          }
          for (int j = 0; j < iAboveBelow.length; j++) {
            if (iAboveBelow[j] >= 5) {
              item = new MenuItem(menuUpSpeed, SWT.RADIO, 
                                  (j == 0) ? 1 : menuUpSpeed.getItemCount());
              item.setText(iAboveBelow[j] + " KB/s");
              item.setData("speed", new Long(iAboveBelow[j]));
              item.addListener(SWT.Selection, speedChangeListener);
  
              if (upLimit == iAboveBelow[j]) item.setSelection(true);
            }
          }
          
          iRel += (iRel >= 10) ? 10 : (iRel >= 6) ? 2 : 1;
        }
        
      }
    });
    
    statusUp.setMenu(menuUpSpeed);
    
    LGLogger.log("Initializing GUI complete");
    
    
    
    

	  	if ( TRHostFactory.create().getTorrents().length > 0 ){  		
	  		showMyTracker();
	  	}
	  	
	  	showMyTorrents();
	
    if (COConfigurationManager.getBooleanParameter("Open Console", false))
      console = new Tab(new ConsoleView());    
    
    VersionChecker.checkForNewVersion();
    
    globalManager.addListener(this);

    boolean isMaximized = COConfigurationManager.getBooleanParameter("window.maximized", mainWindow.getMaximized());
    mainWindow.setMaximized(isMaximized);
    
    String windowRectangle = COConfigurationManager.getStringParameter("window.rectangle", null);
    if (null != windowRectangle) {
      int i = 0;
      int[] values = new int[4];
      StringTokenizer st = new StringTokenizer(windowRectangle, ",");
      try {
        while (st.hasMoreTokens() && i < 4) {
          values[i++] = Integer.valueOf(st.nextToken()).intValue();
          if (values[i - 1] < 0)
            values[i - 1] = 0;
        }
        if (i == 4) {
          mainWindow.setBounds(values[0], values[1], values[2], values[3]);
        }
      }
      catch (Exception e) {}
    }
    
    if (COConfigurationManager.getBooleanParameter("Open Config", false))
      config = new Tab(new ConfigView());
    
    //NICO catch the dispose event from file/quit on osx
    mainWindow.addDisposeListener(new DisposeListener() {
    	public void widgetDisposed(DisposeEvent event) {
    		if (!isAlreadyDead) {
    			isDisposeFromListener = true;
    			if (mainWindow != null) {
    				mainWindow.removeDisposeListener(this);
    				dispose();
    			}
    			isAlreadyDead = true;
    		}
    	}      
    });
        
    mainWindow.layout();
    
    mainWindow.addShellListener(new ShellAdapter() {
      public void shellClosed(ShellEvent event) {
        if (COConfigurationManager.getBooleanParameter("Close To Tray", true)) { //$NON-NLS-1$
          minimizeToTray(event);
        }
        else {
          event.doit = dispose();
        }
      }

      public void shellIconified(ShellEvent event) {
        if (COConfigurationManager.getBooleanParameter("Minimize To Tray", false)) { //$NON-NLS-1$
          minimizeToTray(event);
        }
      }
      
    });
    
    mainWindow.addListener(SWT.Deiconify, new Listener() {
      public void handleEvent(Event e) {
        if (Constants.isOSX && COConfigurationManager.getBooleanParameter("Password enabled", false)) {
          e.doit = false;
        		mainWindow.setVisible(false);
        		PasswordWindow.showPasswordWindow(display);
        }
      }
    });
       
  }catch( Throwable e ){
    System.out.println("Initialize Error");
		e.printStackTrace();
	}
}

  private void openMainWindow() {
//  share progress window
    
    new ProgressWindow();
    
    mainWindow.open();
    mainWindow.forceActive();
    updater = new GUIUpdater();
    updater.start();

    try {
      systemTraySWT = new SystemTraySWT(this);
    } catch (Throwable e) {
      LGLogger.log(LGLogger.ERROR, "Upgrade to SWT3.0M8 or later for system tray support.");
    }
    
    


    if (COConfigurationManager.getBooleanParameter("Start Minimized", false)) {
      minimizeToTray(null);
    }
    //Only show the password if not started minimized
    //Correct bug #878227
    else {
	    if (COConfigurationManager.getBooleanParameter("Password enabled", false)) {
	      minimizeToTray(null);
	      PasswordWindow.showPasswordWindow(display);
	    }
    }

    PluginInitializer.fireEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_STARTS );
    
    if (!COConfigurationManager.getBooleanParameter("Wizard Completed", false)) {
    	ConfigureWizard	wizard = new ConfigureWizard(display);
    	
    	wizard.addListener(
    		new WizardListener()
    		{
    			public void
    			closed()
    			{
    				PluginInitializer.fireEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES );
    			}
    		});
    }else{
    	PluginInitializer.fireEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES );
    }

    if (COConfigurationManager.getBooleanParameter("Show Download Basket", false)) { //$NON-NLS-1$
      if(tray == null)
        tray = new TrayWindow(this);
      tray.setVisible(true);
    }
    COConfigurationManager.addParameterListener("Show Download Basket", this);
    startFolderWatcher();
    COConfigurationManager.addParameterListener("Watch Torrent Folder", this);
    COConfigurationManager.addParameterListener("Watch Torrent Folder Path", this);
    COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);
    Tab.addTabKeyListenerToComposite(folder);
    
    globalManager.startChecker();
    
    	// check file associations   
    checkForDonationPopup();
  }

  private void startFolderWatcher() {
    if(folderWatcher == null)
      folderWatcher = TorrentFolderWatcher.getFolderWatcher();
	  folderWatcher.startIt();
  }

  private void stopFolderWatcher() {
    if(folderWatcher != null) {
      folderWatcher.stopIt();
      folderWatcher.interrupt();
      folderWatcher = null;
    }
  }

  

  public void showMyTracker() {
  	if (my_tracker_tab == null) {
  		if (my_tracker_view == null) {
  			my_tracker_view = new MyTrackerView(globalManager);
  		}
  		my_tracker_tab = new Tab(my_tracker_view);
  	} else {
  		my_tracker_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  public void 
  showMyShares() 
  {
  	if (my_shares_tab == null) {
  		if (my_shares_view == null) {
  			my_shares_view = new MySharesView(globalManager);
  		}
  		my_shares_tab = new Tab(my_shares_view);
  	} else {
  		my_shares_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  public void showMyTorrents() {
    if (mytorrents == null) {
      if (viewMyTorrents == null)
        mytorrents = new Tab(new MyTorrentsSuperView(globalManager));
      else
        mytorrents = new Tab(viewMyTorrents);
    } else
      mytorrents.setFocus();
    	refreshIconBar();
  }
	
  private void minimizeToTray(ShellEvent event) {
    //Added this test so that we can call this method will null parameter.
    if (event != null)
      event.doit = false;
    if(Constants.isOSX) {
      mainWindow.setMinimized(true);
    } else {  
      mainWindow.setVisible(false);
    }
    if (tray != null)
      tray.setVisible(true);
    synchronized (downloadBars) {
      Iterator iter = downloadBars.values().iterator();
      while (iter.hasNext()) {
        MinimizedWindow mw = (MinimizedWindow) iter.next();
        mw.setVisible(true);
      }
    }
  }
  
  public void setStatusText(String keyedSentence) {
    this.statusTextKey = keyedSentence;
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      public void run() {
    if (statusText != null && !statusText.isDisposed()) {
      String sText = "";
      int iSWTVer = SWT.getVersion();
      if (iSWTVer < RECOMMENDED_SWT_VERSION) {
        String sParam[] = {"SWT v"+ iSWTVer};
        sText += MessageText.getString("MainWindow.status.tooOld", sParam) + " ";
        
        if (!statusText.getForeground().equals(Colors.red)) {
					statusText.setCursor(Cursors.handCursor);
					statusText.addMouseListener(new MouseAdapter() {
						public void mouseDown(MouseEvent arg0) {
						  String url = "http://azureus.sourceforge.net/wiki/index.php/UpdateSWT";
	            Program.launch(url);
						}
					});
				}
      }      
      statusText.setText(MessageText.getStringForSentence(statusTextKey));
    }
      }
    });
  }

  private void
  updateComponents()
  {
  	if (statusText != null)
  		statusText.update();
  	if (folder != null) {
  		if(useCustomTab) {
  			((CTabFolder)folder).update();
  		} else {
  			((TabFolder)folder).update();
  		}
  	}
    /*
  	if (trayIcon != null) {
  		trayIcon.refresh();
  	} 
    */
  }

  public void closeDownloadBars() {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {

      public void run() {
        synchronized (downloadBars) {
          Iterator iter = downloadBars.keySet().iterator();
          while (iter.hasNext()) {
            DownloadManager dm = (DownloadManager) iter.next();
            MinimizedWindow mw = (MinimizedWindow) downloadBars.get(dm);
            mw.close();
            iter.remove();
          }
        }
      }

    });
  }

  private void createDropTarget(final Control control) {
    DropTarget dropTarget = new DropTarget(control, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
    dropTarget.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
      public void dragOver(DropTargetEvent event) {
        if(URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.detail = DND.DROP_LINK;
        }
      }
      public void drop(DropTargetEvent event) {
        openDroppedTorrents(event);
      }
    });
  }

  public void openDroppedTorrents(DropTargetEvent event) {
		if(event.data == null)
			return;
    if(event.data instanceof String[]) {
      final String[] sourceNames = (String[]) event.data;
      if (sourceNames == null)
        event.detail = DND.DROP_NONE;
      if (event.detail == DND.DROP_NONE)
        return;
      boolean startInStoppedState = event.detail == DND.DROP_COPY;
      for (int i = 0;(i < sourceNames.length); i++) {
        final File source = new File(sourceNames[i]);
        if (source.isFile())
          TorrentOpener.openTorrent(source.getAbsolutePath(), startInStoppedState, true );
        else if (source.isDirectory()){
        	
        	String	dir_name = source.getAbsolutePath();
        	
        	String	drop_action = COConfigurationManager.getStringParameter("config.style.dropdiraction", "0");
        
        	if ( drop_action.equals("1")){
        		ShareUtils.shareDir(dir_name);
        	}else if ( drop_action.equals("2")){
        		ShareUtils.shareDirContents( dir_name, false );
        	}else if ( drop_action.equals("3")){
        		ShareUtils.shareDirContents( dir_name, true );
        	}else{
        		TorrentOpener.openTorrentsFromDirectory(dir_name, startInStoppedState);
        	}
        }
      }
    } else {
      TorrentOpener.openUrl(((URLTransfer.URLType)event.data).linkURL);
    }
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
  
  public void 
  downloadManagerAdded(
  	final DownloadManager created) 
  {
    if ( created.getState() == DownloadManager.STATE_STOPPED || 
         created.getState() == DownloadManager.STATE_QUEUED ||
         created.getState() == DownloadManager.STATE_ERROR ||
         created.getState() == DownloadManager.STATE_SEEDING )
      return;
    
    checkForDonationPopup();
      
	if (display != null && !display.isDisposed()){
	
	   display.asyncExec(new Runnable() {
			public void
			run()
			{
			    if (COConfigurationManager.getBooleanParameter("Open Details")){
			    
			      openManagerView(created);
			    }
			    
			    if (COConfigurationManager.getBooleanParameter("Open Bar", false)) {
			      synchronized (downloadBars) {
			        MinimizedWindow mw = new MinimizedWindow(created, mainWindow);
			        downloadBars.put(created, mw);
			      }
			    }
			}
	   });
    }
  }

  public void openManagerView(DownloadManager downloadManager) {
    synchronized (downloadViews) {
      if (downloadViews.containsKey(downloadManager)) {
        Tab tab = (Tab) downloadViews.get(downloadManager);
        tab.setFocus();
        refreshIconBar();
      }
      else {
        Tab tab = new Tab(new ManagerView(downloadManager));
        downloadViews.put(downloadManager, tab);
      }
    }
  }

  public void removeManagerView(DownloadManager downloadManager) {
    synchronized (downloadViews) {
      downloadViews.remove(downloadManager);
    }
  }

   public void downloadManagerRemoved(DownloadManager removed) {
    synchronized (downloadViews) {
      if (downloadViews.containsKey(removed)) {
        final Tab tab = (Tab) downloadViews.get(removed);
        if (display == null || display.isDisposed())
          return;
        display.asyncExec(new Runnable() {
          public void run() {
            tab.dispose();
          }
        });

      }
    }
  }

  public Display getDisplay() {
    return this.display;
  }

  public Shell getShell() {
    return mainWindow;
  }

  public void setVisible(boolean visible) {
    mainWindow.setVisible(visible);
    if (visible) {
      if (tray != null)
        tray.setVisible(false);
      /*
      if (trayIcon != null)
        trayIcon.showIcon();
      */
      mainWindow.forceActive();
      mainWindow.setMinimized(false);
    }
  }

  public boolean isVisible() {
    return mainWindow.isVisible();
  }

  public boolean dispose() {
    if(COConfigurationManager.getBooleanParameter("confirmationOnExit", false) && !getExitConfirmation())
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
    stopFolderWatcher();
    initializer.stopIt();
    if(updater != null)
      updater.stopIt();
    
    COConfigurationManager.setParameter("window.maximized", mainWindow.getMaximized());
    // unmaximize to get correct window rect
    if (mainWindow.getMaximized())
      mainWindow.setMaximized(false);

    Rectangle windowRectangle = mainWindow.getBounds();
    COConfigurationManager.setParameter(
      "window.rectangle",
      windowRectangle.x + "," + windowRectangle.y + "," + windowRectangle.width + "," + windowRectangle.height);
    COConfigurationManager.save();

    //NICO swt disposes the mainWindow all by itself (thanks... ;-( ) on macosx
    if(!mainWindow.isDisposed() && !isDisposeFromListener) {
    	mainWindow.dispose();
    }
      
    //if (updateJar){
    //  updateJar();
    //}
    
    	// problem with closing down web start as AWT threads don't close properly

  
    
	if ( SystemProperties.isJavaWebStartInstance()){    	
 	
		Thread close = new Thread( "JWS Force Terminate")
			{
				public void
				run()
				{
					try{
						Thread.sleep(2500);
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
					
					System.exit(1);
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
  private boolean getExitConfirmation() {
    MessageBox mb = new MessageBox(mainWindow, SWT.ICON_WARNING | SWT.YES | SWT.NO);
    mb.setText(MessageText.getString("MainWindow.dialog.exitconfirmation.title"));
    mb.setMessage(MessageText.getString("MainWindow.dialog.exitconfirmation.text"));
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
  public Tab getConsole() {
    return console;
  }

  /**
	 * @return
	 */
  public Tab getMytorrents() {
	return mytorrents;
  }
  
  public Tab getMyTracker() {
	return my_tracker_tab;
  }

  /**
	 * @param tab
	 */
  public void setConsole(Tab tab) {
    console = tab;
  }

  /**
	 * @param tab
	 */
  public void setMytorrents(Tab tab) {
	mytorrents = tab;
  }
  
  public void setMyTracker(Tab tab) {
  	my_tracker_tab = tab;
  }
  
  public void setMyShares(Tab tab) {
  	my_shares_tab = tab;
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
  public Tab getConfig() {
    return config;
  }

  /**
	 * @param tab
	 */
  public void setConfig(Tab tab) {
    config = tab;
  }

  /**
	 * @return
	 */
  public Tab getStats() {
    return stats_tab;
  }

  /**
   * @param tab
   */
  public void setStats(Tab tab) {
    stats_tab = tab;
  }

  /**
	 * @return
	 */
  public TrayWindow getTray() {
    return tray;
  }



  /**
   * @return Returns the useCustomTab.
   */
  public boolean isUseCustomTab() {
    return useCustomTab;
  }    
  
  
  
  
  Map pluginTabs = new HashMap();
  

  
  public void openPluginView(final PluginView view) {
    Tab tab = (Tab) pluginTabs.get(view.getPluginViewName());
    if(tab != null) {
      tab.setFocus();
    } else {
      tab = new Tab(view);
      pluginTabs.put(view.getPluginViewName(),tab);         
    }
  }
  
  public void removeActivePluginView(final PluginView view) {
    pluginTabs.remove(view.getPluginViewName());
  }
  
  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    //System.out.println("parameterChanged:"+parameterName);
    if (COConfigurationManager.getBooleanParameter("Show Download Basket", false)) { //$NON-NLS-1$
      if(tray == null) {
        tray = new TrayWindow(this);
        tray.setVisible(true);
      }
    } else if(tray != null) {
      tray.setVisible(false);
      tray = null;
    }
    if (COConfigurationManager.getBooleanParameter("Watch Torrent Folder", false)) //$NON-NLS-1$
      startFolderWatcher();
    else
      stopFolderWatcher();
    if("Watch Torrent Folder Path".equals(parameterName))
      startFolderWatcher();
    
    if (parameterName.equals("GUI_SWT_bFancyTab") && 
        folder instanceof CTabFolder && 
        folder != null && !folder.isDisposed()) {
      try {
        ((CTabFolder)folder).setSimpleTab(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
        
      } catch (NoSuchMethodError e) { 
        /** < SWT 3.0M8 **/ 
      }
    }     
  }
  
 
  /**
   * 
   */
  

  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("open"))
      return true;
    if(itemKey.equals("open_no_default"))
      return true;
    if(itemKey.equals("open_url"))
      return true;
    if(itemKey.equals("open_folder"))
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
     TorrentOpener.openTorrent();
     return;
    }
    if(itemKey.equals("open_no_default")) {
      TorrentOpener.openTorrentNoDefaultSave(false);
      return;
    }
    if(itemKey.equals("open_for_seeding")) {
      TorrentOpener.openTorrentNoDefaultSave(true);
      return;
    }
    if(itemKey.equals("open_url")) {
      TorrentOpener.openUrl();
      return;
    }
    if(itemKey.equals("open_folder")) {
      TorrentOpener.openDirectory();
      return;
    }
    if(itemKey.equals("new")) {
      new NewTorrentWizard(display);
      return;
    }
    IView currentView = getCurrentView();
    if(currentView != null)
      currentView.itemActivated(itemKey);    
  }
  
  private IView getCurrentView() {
	  try {
	    if(!useCustomTab) {
	      TabItem[] selection = ((TabFolder)folder).getSelection();
				if(selection.length > 0)  {
				  return Tab.getView(selection[0]);
				}
				else {
				  return null;
				}
	    } else {
	      return Tab.getView(((CTabFolder)folder).getSelection());
	    }
	  }
	  catch (Exception e) {
	    return null;
	  }
  }
  
  public void refreshIconBar() {
    iconBar.setCurrentEnabler(this);
  }

  
  
  private void showBlockedIps() {
    StringBuffer sb = new StringBuffer();
    BlockedIp[] blocked = IpFilter.getInstance().getBlockedIps();
    String inRange = MessageText.getString("ConfigView.section.ipfilter.list.inrange");
    String notInRange = MessageText.getString("ConfigView.section.ipfilter.list.notinrange");    
    for(int i=0;i<blocked.length;i++){
      BlockedIp bIp = blocked[i];
      sb.append(DisplayFormatters.formatTimeStamp(bIp.getBlockedTime()));
      sb.append("\t[");
      sb.append( bIp.getTorrentName() );
      sb.append("] \t");
      sb.append(bIp.getBlockedIp());
      IpRange range = bIp.getBlockingRange();
      if(range == null) {
        sb.append(' ');
        sb.append(notInRange);
        sb.append('\n');
      } else {
        sb.append(' ');
        sb.append(inRange);
        sb.append(range.toString());
        sb.append('\n');
      }
    }
    if(mainWindow == null || mainWindow.isDisposed())
      return;
    BlockedIpsWindow.show(mainWindow.getDisplay(),sb.toString());
  }
  
  
  private synchronized void checkForDonationPopup() {
   //Check if user has already donated first
   boolean alreadyDonated = COConfigurationManager.getBooleanParameter("donations.donated",false);
   if(alreadyDonated)
     return;
   
   //Check for last asked version
   String lastVersionAsked = COConfigurationManager.getStringParameter("donations.lastVersion","");
        
   long upTime = StatsFactory.getStats().getUpTime();
   int hours = (int) (upTime / (60*60)); //secs * mins
   
   //Ask every DONATIONS_ASK_AFTER hours.
   int nextAsk = (COConfigurationManager.getIntParameter("donations.nextAskTime",0) + 1) * DONATIONS_ASK_AFTER;
   
   //if donations.nextAskTime == -1 , then no more ask for same version
   if(nextAsk == 0) {
    if(lastVersionAsked.equals(VERSION)) {
     return; 
    }
    else {
     //Set the re-ask so that we ask in the next %DONATIONS_ASK_AFTER hours
      COConfigurationManager.setParameter("donations.nextAskTime",hours / DONATIONS_ASK_AFTER);
      COConfigurationManager.save();

      return;
    }
   }
   
   //If we're still under the ask time, return
   if(hours < nextAsk)
    return;
   
   //Here we've got to ask !!!
   COConfigurationManager.setParameter("donations.nextAskTime",hours / DONATIONS_ASK_AFTER);
   COConfigurationManager.save();

   if(display != null && !display.isDisposed()) {
    display.asyncExec( new Runnable() {
			public void run() {
        new DonationWindow2(display).show();    
			}
    });
   }   	     
  }
  
  public void showConfig() {
    if (config == null)
      config = new Tab(new ConfigView());
    else
      config.setFocus();
  }
  

  
  public void reportCurrentTask(String task) {}
  
  public void reportPercent(int percent) {
    if(percent > 100) {
      if(display == null || display.isDisposed())
        return;
      display.asyncExec(new Runnable() {
        public void run() {
          openMainWindow();
        }
      });
    }
  }
  
  public void showConsole() {
    if (console == null)
      console = new Tab(new ConsoleView());
    else
      console.setFocus();
  }
  
  public void showStats() {
    if (stats_tab == null)
      stats_tab = new Tab(new SpeedView(globalManager));
    else
      stats_tab.setFocus();
  }

  public synchronized void setSelectedLanguageItem() {   
    Messages.updateLanguageForControl(mainWindow.getShell());
    Messages.updateLanguageForControl(systemTraySWT.getMenu());    
    if (statusText != null)
      statusText.update();
    if (folder != null) {
      if(useCustomTab) {
        ((CTabFolder)folder).update();
      } else {
        ((TabFolder)folder).update();
      }
    }
    /*
    if (trayIcon != null) {
      trayIcon.updateLanguage();
      trayIcon.refresh();
    }
    */
    if (tray != null)
      tray.updateLanguage();
  
    Tab.updateLanguage();
  
    setStatusText(statusTextKey);
  }
  
  public MainMenu getMenu() {
    return mainMenu;
  }
}
