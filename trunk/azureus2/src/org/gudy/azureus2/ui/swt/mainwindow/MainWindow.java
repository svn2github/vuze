 /*
 * Created on Jun 25, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Modified Apr 17, 2004 by Olivier Chalouhi (OSX system menu)
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.mainwindow;

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
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.update.UpdateCheckerListener;
import org.gudy.azureus2.plugins.update.UpdateManagerListener;

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
import org.gudy.azureus2.ui.swt.update.UpdateWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.systray.SystemTraySWT;
import org.gudy.azureus2.ui.swt.sharing.progress.*;

/**
 * @author Olivier
 * Runnable : so that GUI initialization is done via asyncExec(this)
 * STProgressListener : To make it visible once initialization is done
 */
public class 
MainWindow
	extends AERunnable
	implements GlobalManagerListener, DownloadManagerListener, ParameterListener, IconBarEnabler, AzureusCoreListener {
  
  private static MainWindow window;

  private Initializer initializer;  
  private GUIUpdater updater;

  private AzureusCore			azureus_core;
  
  //Package visibility for GUIUpdater
 GlobalManager       	globalManager;

  //NICO handle swt on macosx
  public static boolean isAlreadyDead = false;
  public static boolean isDisposeFromListener = false;  

  private Display display;
  private Shell mainWindow;
  
  private MainMenu mainMenu;
  
  private IconBar iconBar;
  
  private boolean useCustomTab;
  private Composite folder;
      
  
  private UpdateWindow updateWindow;
  
  private Composite statusArea;
  StackLayout layoutStatusAera;
  
  private CLabel statusText;
  private String statusTextKey = "";
    
  private Composite statusUpdate;
  private Label statusUpdateLabel;
  private ProgressBar statusUpdateProgressBar;
  private Button statusUpdateButton;
  
  //Package visibility for GUIUpdater
  CLabel ipBlocked;
  CLabel statusDown;
  CLabel statusUp;
  
  private TrayWindow tray;
  SystemTraySWT systemTraySWT;
  
  private HashMap downloadViews;
  private AEMonitor	downloadViews_mon			= new AEMonitor( "MainWindow:dlviews" );

  HashMap 	downloadBars;
  AEMonitor	downloadBars_mon			= new AEMonitor( "MainWindow:dlbars" );

     
  private Tab 	mytorrents;
  private Tab 	my_tracker_tab;
  private Tab 	my_shares_tab;
  private Tab 	stats_tab;
  private Tab console;
  private Tab config;
  
  protected AEMonitor	this_mon			= new AEMonitor( "MainWindow" );

  
  public 
  MainWindow(
  	AzureusCore		_azureus_core,
	Initializer 	initializer) 
  { 
  	try{
	    LGLogger.log("MainWindow start");
	    
	    azureus_core	= _azureus_core;
	    
	    this.globalManager = azureus_core.getGlobalManager();
	    
	    this.initializer = initializer;
	    
	    this.display = SWTThread.getInstance().getDisplay();
	    
	    window = this;
	    
	    initializer.addListener(this);
	    
	    display.syncExec(this);
	    
  	}catch( AzureusCoreException e ){
  		
  		Debug.printStackTrace( e );
  	}
  }
  
  public void runSupport() {
    try{
       
    useCustomTab = COConfigurationManager.getBooleanParameter("useCustomTab");
    

    COConfigurationManager.addParameterListener( "config.style.useSIUnits", this );
  
    mytorrents = null;
    my_tracker_tab	= null;
    console = null;
    config = null;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    
    //The Main Window
    mainWindow = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE | SWT.MAX | SWT.MIN);
    mainWindow.setText("Azureus"); //$NON-NLS-1$
    mainWindow.setImages(new Image[] {
        ImageRepository.getImage("azureus"),
        ImageRepository.getImage("azureus32"),
        ImageRepository.getImage("azureus64")
    }); //$NON-NLS-1$
    
    
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
          display.asyncExec(new AERunnable() {
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
        Debug.printStackTrace( e );
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
                             folder.getBackground() },
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
    
    //Composite with StackLayout
    statusArea = new Composite(statusBar, SWT.NULL);
    statusArea.setLayoutData(gridData);
    
    layoutStatusAera = new StackLayout();
    statusArea.setLayout(layoutStatusAera);
    
    //Either the Status Text
    statusText = new CLabel(statusArea, SWT.SHADOW_IN);
    int height = statusText.computeSize(150,SWT.DEFAULT).y;
    
    Listener listener = new Listener() {
      public void handleEvent(Event e) {
        if(updateWindow != null) {
          updateWindow.show();
        }
      }
    };
    
    statusText.addListener(SWT.MouseUp,listener);
    statusText.addListener(SWT.MouseDoubleClick,listener);
    
    //Or a composite with a label, a progressBar and a button
    statusUpdate = new Composite(statusArea, SWT.NULL);
    statusUpdate.setSize(SWT.DEFAULT,height);
    FormLayout layoutStatusUpdate = new FormLayout();
    layoutStatusUpdate.marginHeight = 0;
    layoutStatusUpdate.marginWidth = 0;
    try {
      layoutStatusUpdate.spacing = 5;
    } catch (NoSuchFieldError e) { /* Pre SWT 3.0 */ }
    statusUpdate.setLayout(layoutStatusUpdate);
    
    statusUpdateLabel = new Label(statusUpdate,SWT.NULL);
    Messages.setLanguageText(statusUpdateLabel, "MainWindow.statusText.checking");
    statusUpdateProgressBar = new ProgressBar(statusUpdate,SWT.HORIZONTAL);
    /*statusUpdateButton = new Button(statusUpdate,SWT.PUSH);
    Messages.setLanguageText(statusUpdateButton,"Button.cancel");*/
    int ctrlHeight,top;
    
    formData = new FormData();
    formData.left = new FormAttachment(0, 0);
    ctrlHeight = statusUpdateLabel.computeSize(100,SWT.DEFAULT).y;
    top = (height - ctrlHeight) / 2;
    formData.top = new FormAttachment(0,top);
    formData.width = 150;
    formData.height = height;
    statusUpdateLabel.setLayoutData(formData);
    
    /*formData = new FormData();
    formData.right = new FormAttachment(100);
    ctrlHeight = statusUpdateButton.computeSize(100,SWT.DEFAULT).y;
    top = (height - ctrlHeight) / 2;
    //formData.top = new FormAttachment(0,top);
    formData.width = 100;
    formData.height = height;
    statusUpdateButton.setLayoutData(formData);*/
    
    formData = new FormData();
    formData.left = new FormAttachment(statusUpdateLabel);
    formData.right = new FormAttachment(100,0);
    ctrlHeight = statusUpdateProgressBar.computeSize(100,SWT.DEFAULT).y;
    top = (height - ctrlHeight) / 2;
    formData.top = new FormAttachment(0,top);
    formData.height = height;    
    statusUpdateProgressBar.setLayoutData(formData);    
    
    layoutStatusAera.topControl = statusText;
    
    
    
    gridData = new GridData();

    if( Constants.isLinux ) gridData.widthHint = 255;
    else gridData.widthHint = 225;
    
    ipBlocked = new CLabel(statusBar, SWT.SHADOW_IN);
    ipBlocked.setText("{} IPs:"); //$NON-NLS-1$
    ipBlocked.setLayoutData(gridData);
    Messages.setLanguageText(ipBlocked,"MainWindow.IPs.tooltip");
    ipBlocked.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
       BlockedIpsWindow.showBlockedIps(azureus_core, MainWindow.this.mainWindow);
      }
    });
    
    gridData = new GridData();
    gridData.widthHint = Constants.isOSX ? 150 : ( Constants.isLinux ? 140 : 130 );
    statusDown = new CLabel(statusBar, SWT.SHADOW_IN);
    statusDown.setText(/*MessageText.getString("ConfigView.download.abbreviated") +*/ "n/a");
    statusDown.setLayoutData(gridData);
    Messages.setLanguageText(statusDown,"MainWindow.status.updowndetails.tooltip");

    Listener lStats = new Listener() {
    	public void handleEvent(Event e) {
    		showStats();
    	}
    };
    
    gridData = new GridData();
    gridData.widthHint = Constants.isOSX ? 150 : ( Constants.isLinux ? 140 : 130 );
    statusUp = new CLabel(statusBar, SWT.SHADOW_IN);
    statusUp.setText(/*MessageText.getString("ConfigView.upload.abbreviated") +*/ "n/a");
    statusUp.setLayoutData(gridData);
    Messages.setLanguageText(statusUp,"MainWindow.status.updowndetails.tooltip");
   
    statusDown.addListener(SWT.MouseDoubleClick,lStats);
    statusUp.addListener(SWT.MouseDoubleClick,lStats);
    
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
            COConfigurationManager.save();
          }
        });
        if(upLimit == 0) item.setSelection(true);
        
        final Listener speedChangeListener = new Listener() {
              public void handleEvent(Event e) {
                int iSpeed = ((Long)((MenuItem)e.widget).getData("speed")).intValue();
                COConfigurationManager.setParameter("Max Upload Speed KBs", iSpeed);
                COConfigurationManager.save();
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
    
    
    final Menu menuDownSpeed = new Menu(mainWindow,SWT.POP_UP);    
    menuDownSpeed.addListener(SWT.Show,new Listener() {
      public void handleEvent(Event e) {
        MenuItem[] items = menuDownSpeed.getItems();
        for(int i = 0 ; i < items.length ; i++) {
         items[i].dispose(); 
        }
        
        int downLimit = COConfigurationManager.getIntParameter("Max Download Speed KBs",0);
        
        MenuItem item = new MenuItem(menuDownSpeed,SWT.RADIO);
        item.setText(MessageText.getString("ConfigView.unlimited"));
        item.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            COConfigurationManager.setParameter("Max Download Speed KBs",0); 
            COConfigurationManager.save();
          }
        });
        if(downLimit == 0) item.setSelection(true);
        
        final Listener speedChangeListener = new Listener() {
              public void handleEvent(Event e) {
                int iSpeed = ((Long)((MenuItem)e.widget).getData("speed")).intValue();
                COConfigurationManager.setParameter("Max Download Speed KBs", iSpeed);
                COConfigurationManager.save();
              }
            };

        int iRel = 0;
        for (int i = 0; i < 12; i++) {
          int[] iAboveBelow;
          if (iRel == 0) {
            iAboveBelow = new int[] { downLimit };
          } else {
            iAboveBelow = new int[] { downLimit - iRel, downLimit + iRel };
          }
          for (int j = 0; j < iAboveBelow.length; j++) {
            if (iAboveBelow[j] >= 5) {
              item = new MenuItem(menuDownSpeed, SWT.RADIO, 
                                  (j == 0) ? 1 : menuDownSpeed.getItemCount());
              item.setText(iAboveBelow[j] + " KB/s");
              item.setData("speed", new Long(iAboveBelow[j]));
              item.addListener(SWT.Selection, speedChangeListener);
  
              if (downLimit == iAboveBelow[j]) item.setSelection(true);
            }
          }
          
          iRel += (iRel >= 10) ? 10 : (iRel >= 6) ? 2 : 1;
        }
        
      }
    });    
    statusDown.setMenu(menuDownSpeed);
    
    
    
    LGLogger.log("Initializing GUI complete");
   
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
      public void 
	  shellClosed(ShellEvent event) 
      {
        if (	systemTraySWT != null &&
        		COConfigurationManager.getBooleanParameter("Enable System Tray") && 
        		COConfigurationManager.getBooleanParameter("Close To Tray", true)){
        	
          minimizeToTray(event);
        }
        else {
          event.doit = dispose();
        }
      }

      public void 
	  shellIconified(ShellEvent event) 
      {
        if ( 	systemTraySWT != null &&
        		COConfigurationManager.getBooleanParameter("Enable System Tray") &&
        		COConfigurationManager.getBooleanParameter("Minimize To Tray", false)) {
        	
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
    Debug.printStackTrace( e );
	}
}

  private void openMainWindow() {
    
    //  share progress window    
    new ProgressWindow();
    
    addUpdateListener();
    
    if ( azureus_core.getTrackerHost().getTorrents().length > 0 ){     
    	
      showMyTracker();
    }
    
    showMyTorrents();

    if (COConfigurationManager.getBooleanParameter("Open Console", false)) {
      showConsole();
    }
    
    if (COConfigurationManager.getBooleanParameter("Open Config", false)) {
      showConfig();
    }
  
    mainWindow.open();
    mainWindow.forceActive();
    updater = new GUIUpdater(azureus_core,this);
    updater.start();

     
    

    if ( COConfigurationManager.getBooleanParameter("Enable System Tray")){
    	
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
    }

    azureus_core.getPluginManager().firePluginEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_STARTS );
    
    if (!COConfigurationManager.getBooleanParameter("Wizard Completed", false)) {
    	ConfigureWizard	wizard = new ConfigureWizard(getAzureusCore(),display);
    	
    	wizard.addListener(
    		new WizardListener()
    		{
    			public void
    			closed()
    			{
   					azureus_core.getPluginManager().firePluginEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES );
    			}
    		});
    }else{
    	
    	azureus_core.getPluginManager().firePluginEvent( PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES );
    }
  
    if (COConfigurationManager.getBooleanParameter("Show Download Basket", false)) { //$NON-NLS-1$
      if(tray == null)
        tray = new TrayWindow(this);
      tray.setVisible(true);
    }
    COConfigurationManager.addParameterListener("Show Download Basket", this);
    COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);
    
    Tab.addTabKeyListenerToComposite(folder);
    
    // globalManager.startChecker();
    
    	// check file associations   
    DonationWindow2.checkForDonationPopup();
  }


  public void showMyTracker() {
  	if (my_tracker_tab == null) {
  		my_tracker_tab = new Tab(new MyTrackerView(azureus_core));
  	} else {
  		my_tracker_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  public void 
  showMyShares() 
  {
  	if (my_shares_tab == null) {
  		my_shares_tab = new Tab(new MySharesView(azureus_core));
  	} else {
  		my_shares_tab.setFocus();
  		refreshIconBar();
  	}
  }
  
  public void showMyTorrents() {
    if (mytorrents == null) {
      mytorrents = new Tab(new MyTorrentsSuperView(azureus_core));
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
  
  public void setStatusText(String keyedSentence) {
    this.statusTextKey = keyedSentence==null?"":keyedSentence;        
    updateStatusText();
  }
  
  private void updateStatusText() {
    if (display == null || display.isDisposed())
      return;
    final String text;
    if(updateWindow != null) {
      text = this.statusTextKey + " MainWindow.updateavail";
    } else {
      text = this.statusTextKey;
    }
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        if (statusText != null && !statusText.isDisposed()) {      
          statusText.setText(MessageText.getStringForSentence(text));
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
  }

  public void closeDownloadBars() {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable() {

      public void runSupport() {
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
        TorrentOpener.openDroppedTorrents(azureus_core,event);
      }
    });
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
    
    DonationWindow2.checkForDonationPopup();
      
    created.addListener(this);
    /*
	if (display != null && !display.isDisposed()){
	
	   display.asyncExec(new AERunnable() {
			public void
			runSupport()
			{
			    if (COConfigurationManager.getBooleanParameter("Open Details")){
			    
			      openManagerView(created);
			    }
			    
			    if (COConfigurationManager.getBooleanParameter("Open Bar", false)) {
			      try{
			      	downloadBars_mon.enter();
			      	
			        MinimizedWindow mw = new MinimizedWindow(created, mainWindow);
			        downloadBars.put(created, mw);
			      }finally{
			      
			      	downloadBars_mon.exit();
			      }
			    }
			}
	   });
    }
    */
  }

  public void openManagerView(DownloadManager downloadManager) {
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

  public void removeManagerView(DownloadManager downloadManager) {
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
        if (display == null || display.isDisposed())
          return;
        display.asyncExec(new AERunnable(){
          public void runSupport() {
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
  


  
  public void parameterChanged(String parameterName) {
    if( parameterName.equals( "Show Download Basket" ) ) {
      if (COConfigurationManager.getBooleanParameter("Show Download Basket")) {
        if(tray == null) {
          tray = new TrayWindow(this);
          tray.setVisible(true);
        }
      } else if(tray != null) {
        tray.setVisible(false);
        tray = null;
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
      TorrentOpener.openUrl(azureus_core);
      return;
    }
    if(itemKey.equals("open_folder")) {
      TorrentOpener.openDirectory();
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

  
  

  
  

  
  public void showConfig() {
    if (config == null)
      config = new Tab(new ConfigView( azureus_core ));
    else
      config.setFocus();
  }
  

  

  
  public void showConsole() {
    if (console == null)
      console = new Tab(new ConsoleView());
    else
      console.setFocus();
  }
  
  public void showStats() {
    if (stats_tab == null)
      stats_tab = new Tab(new StatsView(globalManager));
    else
      stats_tab.setFocus();
  }

  public void setSelectedLanguageItem() 
  {
  	try{
  		this_mon.enter();
  	
	    Messages.updateLanguageForControl(mainWindow.getShell());
	    
	    if ( systemTraySWT != null ){
	    	Messages.updateLanguageForControl(systemTraySWT.getMenu());
	    }
	    
	    if (statusText != null){
	    
	    	statusText.update();
	    }
	    
	    if (folder != null) {
	      if(useCustomTab) {
	        ((CTabFolder)folder).update();
	      } else {
	        ((TabFolder)folder).update();
	      }
	    }
	
	    if (tray != null){
	      tray.updateLanguage();
	    }
	    
	    Tab.updateLanguage();
	  
	    setStatusText(statusTextKey);
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  public MainMenu getMenu() {
    return mainMenu;
  }
  
  /*
   * STProgressListener implementation, used for startup.
   */
  
  public void reportCurrentTask(String task) {}
  
  /**
   * A percent > 100 means the end of the startup process
   */
  public void reportPercent(int percent) {
    if(percent > 100) {
      if(display == null || display.isDisposed())
        return;
      display.asyncExec(new AERunnable(){
        public void runSupport() {
          openMainWindow();
        }
      });
    }
  }
  
  private void switchStatusToUpdate() {
    if(display != null && ! display.isDisposed())
      display.asyncExec(new AERunnable(){
        public void runSupport() {
           	if ( statusArea == null || statusArea.isDisposed()){
        		return;
        	}
           	layoutStatusAera.topControl = statusUpdate;
           	statusArea.layout();
        }
      });
  }
  
  private void switchStatusToText() {
    if(display != null && ! display.isDisposed())
      display.asyncExec(new AERunnable() {
        public void runSupport() {
        	if ( statusArea == null || statusArea.isDisposed()){
        		return;
        	}
        	layoutStatusAera.topControl = statusText;
        	statusArea.layout();
        }
      });
  }
  
  private void setNbChecks(final int nbChecks) {
    if(display != null && ! display.isDisposed())
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          if(statusUpdateProgressBar == null || statusUpdateProgressBar.isDisposed())
            return;
          statusUpdateProgressBar.setMinimum(0);
          statusUpdateProgressBar.setMaximum(nbChecks);
          statusUpdateProgressBar.setSelection(0);
        }
      });
  }
  
  private void setNextCheck() {
    if(display != null && ! display.isDisposed())
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          if(statusUpdateProgressBar == null || statusUpdateProgressBar.isDisposed())
            return;
          statusUpdateProgressBar.setSelection(statusUpdateProgressBar.getSelection() + 1);
        }
      });
  }
  
  private void addUpdateListener() {
  	azureus_core.getPluginManager().getDefaultPluginInterface().getUpdateManager().addListener(new UpdateManagerListener () {
      public void checkInstanceCreated(UpdateCheckInstance instance) {
        
        switchStatusToUpdate();
        instance.addListener(new UpdateCheckInstanceListener () {
          public void cancelled(UpdateCheckInstance instance) {
            switchStatusToText();
          }
          public void complete(UpdateCheckInstance instance) {
            switchStatusToText();
          }
        });
        UpdateChecker[] checkers = instance.getCheckers();
        setNbChecks(checkers.length);
        UpdateCheckerListener listener = new UpdateCheckerListener() {
          public void cancelled(UpdateChecker checker) {
            //setNextCheck();
          }
          
          public void completed(UpdateChecker checker) {
            setNextCheck();
          }
          
          public void failed(UpdateChecker checker) {
            setNextCheck();
          }
          
        };
        for(int i = 0 ; i < checkers.length ; i++) {
          checkers[i].addListener(listener);
        }
      }
    });
  }  
  
    
  /**
   * MUST be called by the SWT Thread
   * @param updateWindow the updateWindow or null if no update is available
   */
  public void setUpdateNeeded(UpdateWindow updateWindow) {
    this.updateWindow = updateWindow;
    if(updateWindow != null) {
      statusText.setCursor(Cursors.handCursor);    
      statusText.setForeground(Colors.colorWarning);      
      updateStatusText();
    } else {
      statusText.setCursor(null); 
      statusText.setForeground(null);
      updateStatusText();
    }
  }
  
  //DownloadManagerListener implementation

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
    // Do Nothing
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
          display.asyncExec(new AERunnable() {
            public void runSupport() {
              if (COConfigurationManager.getBooleanParameter("Open Details",false)) {
                openManagerView(manager);
              }
              
              if (COConfigurationManager.getBooleanParameter("Open Bar", false)) {
                try{
                	downloadBars_mon.enter();
                
                	MinimizedWindow mw = new MinimizedWindow(manager, mainWindow);
                	
                	downloadBars.put(manager, mw);
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
}
