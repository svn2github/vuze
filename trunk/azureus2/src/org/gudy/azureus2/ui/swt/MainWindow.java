/*
 * Created on 25 juin 2003
 *  
 */
package org.gudy.azureus2.ui.swt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.FileImporter;
import org.gudy.azureus2.core3.disk.FileImporter.FolderWatcher;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.BlockedIp;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.logging.LGAlertListener;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.*;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.donations.DonationWindow2;
import org.gudy.azureus2.ui.swt.wizard.WizardListener;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.systray.SystemTray;
import org.gudy.azureus2.ui.swt.auth.*;
import org.gudy.azureus2.ui.swt.sharing.*;
import org.gudy.azureus2.ui.swt.sharing.progress.*;

import snoozesoft.systray4j.SysTrayMenu;

/**
 * @author Olivier
 *  
 */
public class MainWindow implements GlobalManagerListener, ParameterListener, IconBarEnabler {

  public static final String VERSION = Constants.AZUREUS_VERSION;
  private static final int DONATIONS_ASK_AFTER = 168;
  
  private String latestVersion = ""; //$NON-NLS-1$
  private String latestVersionFileName = null;

  private static MainWindow window;
  private static SplashWindow splash_maybe_null;

  private static boolean jarDownloaded = false;
  private static boolean updateJar = false;

  private GUIUpdater updater;

  private static int instanceCount = 0;

  private Display display;
  private Shell mainWindow;
  private Menu menuBar;
  private Menu languageMenu = null;
  private IconBar iconBar;

  //NICO handle swt on macosx
  public static boolean isAlreadyDead = false;
  public static boolean isDisposeFromListener = false;
  
  public static Color[] blues = new Color[5];
  public static Color black;
  public static Color blue;
  public static Color grey;
  public static Color red;
  public static Color white;
  private static Color background;
  
  public static Color red_ConsoleView;
  public static Color red_ManagerItem;
  public static Cursor handCursor;

  private boolean useCustomTab;
  
  //private TabFolder folder;
  private Composite folder;
  
  private CLabel statusText;
  private CLabel ipBlocked;
  private CLabel statusDown;
  private CLabel statusUp;

  private GlobalManager 			globalManager;
  
  private AuthenticatorWindow		auth_window;

  private UserAlerts				user_alerts;
    
  private Tab 	mytorrents;
  private IView viewMyTorrents;
  
  private Tab 	my_tracker_tab;
  private IView my_tracker_view;
  
  private Tab 	my_shares_tab;
  private IView my_shares_view;
  
  private Tab 	stats_tab;
  
  
  private Tab console;
  private Tab config;
  private Tab irc;

  private MenuItem selectedLanguageItem;

  private TrayWindow tray;
  private SystemTray trayIcon;

  private HashMap downloadViews;
  private HashMap downloadBars;

  private StartServer startServer;

  public static final long AUTO_UPDATE_CHECK_PERIOD = 23*60*60*1000;	// 23 hours

  private Shell			current_upgrade_window;
  private Timer			version_check_timer;
  
  private FolderWatcher folderWatcher = null;

  private boolean		initialisation_complete;
  private List			alert_queue = new ArrayList();
  private List			alert_history	= new ArrayList();
  

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
  
              ipBlocked.setText("IPs: " + IpFilter.getInstance().getNbRanges() + " - " + IpFilter.getInstance().getNbIpsBlocked());
              statusDown.setText("D: " + DisplayFormatters.formatByteCountToKiBEtcPerSec(globalManager.getStats().getDownloadAverage())); //$NON-NLS-1$
              statusUp.setText("U: " + DisplayFormatters.formatByteCountToKiBEtcPerSec(globalManager.getStats().getUploadAverage())); //$NON-NLS-1$
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
            
            if (trayIcon != null)
              trayIcon.refresh();
            
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


  public MainWindow(GlobalManager gm, StartServer server) {
    if (window != null) {
      if(!COConfigurationManager.getBooleanParameter("Add URL Silently", false))
        setVisible(true);
      return;
    }
    
    LGLogger.log("MainWindow start");

    window = this;

    try{
    	LGLogger.addAlertListener(
    			new LGAlertListener()
				{
    				public void
					alertRaised(
						int		type,
						String	message )
					{
    					synchronized( alert_queue ){
    						
    						if ( !initialisation_complete ){
    							
    							alert_queue.add( new Object[]{ new Integer(type), message });
    							
    							return;
    						}
    					}
    					
    					showAlert( type, message );
    				}
					
					public void
					alertRaised(
						String		message,
						Throwable	exception )
					{
						showErrorMessageBox( message, exception );
					}
    			});
    	
	COConfigurationManager.checkConfiguration();

    auth_window = new AuthenticatorWindow();
    
    new CertificateTrustWindow();
    
    user_alerts = new UserAlerts(gm);
       
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
    
    // set to true to enable SWT leak checking
    if (false) {
      DeviceData data = new DeviceData();
      data.tracking = true;
      display = new Display(data);
      Sleak sleak = new Sleak();
      sleak.open();
    }
    else {
      display = new Display();
    }

    ImageRepository.loadImagesForSplashWindow(display);
    
    if (COConfigurationManager.getBooleanParameter("Show Splash", true)) {
    	
      showSplashWindow();
    }
    
  	if ( splash_maybe_null != null ){
  		splash_maybe_null.setNumTasks(4);
  	}
    splashNextTask();

    Locale[] locales = MessageText.getLocales();
    String savedLocaleString = COConfigurationManager.getStringParameter("locale", Locale.getDefault().toString()); //$NON-NLS-1$
    Locale savedLocale;
    if (savedLocaleString.length() == 5) {
      savedLocale = new Locale(savedLocaleString.substring(0, 2), savedLocaleString.substring(3, 5));
    } else if (savedLocaleString.length() == 2) {
      savedLocale = new Locale(savedLocaleString);
    } else {
      savedLocale = Locale.getDefault();
    }
    MessageText.changeLocale(savedLocale);

    setSplashTask("splash.loadingImages");
        
    ImageRepository.loadImages(display);
    
    splashNextTask();
    setSplashTask("splash.initializeGui");
    
    
    this.startServer = server;
    this.globalManager = gm;
    mytorrents = null;
    my_tracker_tab	= null;
    console = null;
    config = null;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    

    if (instanceCount == 0) {      
      try {
        allocateBlues();
        
        black = new Color(display, new RGB(0, 0, 0));
        blue = new Color(display, new RGB(0, 0, 170));
        grey = new Color(display, new RGB(170, 170, 170));
        red = new Color(display, new RGB(255, 0, 0));
        white = new Color(display, new RGB(255, 255, 255));
        background = new Color(display , new RGB(248,248,248));
        red_ConsoleView = new Color(display, new RGB(255, 192, 192));
        red_ManagerItem = new Color(display, new RGB(255, 68, 68));
        handCursor = new Cursor(display, SWT.CURSOR_HAND);
      } catch (Exception e) {
        LGLogger.log(LGLogger.ERROR, "Error allocating colors");
        e.printStackTrace();
      }
    }
    instanceCount++;

    //The Main Window
    mainWindow = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE | SWT.MAX | SWT.MIN);
    mainWindow.setText("Azureus"); //$NON-NLS-1$
    mainWindow.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    
    GridData gridData;
    /*CoolBar coolbar = new CoolBar(mainWindow,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    coolbar.setLayoutData(coolbar);
    CoolItem item = new CoolItem(coolbar,SWT.NULL);
    Decorations decoMenu = new Decorations(coolbar,SWT.NULL);
    */
    
  try {
    //The Main Menu
    menuBar = new Menu(mainWindow, SWT.BAR);
    mainWindow.setMenuBar(menuBar);
    //The File Menu
    MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
    Messages.setLanguageText(fileItem, "MainWindow.menu.file"); //$NON-NLS-1$
    Menu fileMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    fileItem.setMenu(fileMenu);
    
    MenuItem file_new = new MenuItem(fileMenu, SWT.CASCADE);
    Messages.setLanguageText(file_new, "MainWindow.menu.file.open"); //$NON-NLS-1$
    
    MenuItem file_share= new MenuItem(fileMenu, SWT.CASCADE);
    Messages.setLanguageText(file_share, "MainWindow.menu.file.share"); //$NON-NLS-1$
    
    MenuItem file_create = new MenuItem(fileMenu, SWT.NULL);
    Messages.setLanguageText(file_create, "MainWindow.menu.file.create"); //$NON-NLS-1$

    MenuItem file_configure = new MenuItem(fileMenu, SWT.NULL);
    Messages.setLanguageText(file_configure, "MainWindow.menu.file.configure"); //$NON-NLS-1$

    new MenuItem(fileMenu, SWT.SEPARATOR);

    MenuItem file_export = new MenuItem(fileMenu, SWT.NULL);
    Messages.setLanguageText(file_export, "MainWindow.menu.file.export"); //$NON-NLS-1$

    MenuItem file_import = new MenuItem(fileMenu, SWT.NULL);
    Messages.setLanguageText(file_import, "MainWindow.menu.file.import"); //$NON-NLS-1$

    new MenuItem(fileMenu, SWT.SEPARATOR);

    MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
    Messages.setLanguageText(file_exit, "MainWindow.menu.file.exit"); //$NON-NLS-1$

    	// file->open submenus
    
    Menu newMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    file_new.setMenu(newMenu);

    MenuItem file_new_torrent = new MenuItem(newMenu, SWT.NULL);
    Messages.setLanguageText(file_new_torrent, "MainWindow.menu.file.open.torrent"); //$NON-NLS-1$
    file_new_torrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openTorrent();
      }
    });
    
    MenuItem file_new_torrent_no_default = new MenuItem(newMenu, SWT.NULL);
    Messages.setLanguageText(file_new_torrent_no_default, "MainWindow.menu.file.open.torrentnodefault"); //$NON-NLS-1$
    file_new_torrent_no_default.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openTorrentNoDefaultSave(false);
      }      
    });

    MenuItem file_new_torrent_for_seeding = new MenuItem(newMenu, SWT.NULL);
    Messages.setLanguageText(file_new_torrent_for_seeding, "MainWindow.menu.file.open.torrentforseeding"); //$NON-NLS-1$
    file_new_torrent_for_seeding.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openTorrentNoDefaultSave(true);
      }      
    });

    MenuItem file_new_url = new MenuItem(newMenu,SWT.NULL);
    Messages.setLanguageText(file_new_url, "MainWindow.menu.file.open.url"); //$NON-NLS-1$
    file_new_url.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openUrl();
      }
    });
    MenuItem file_new_folder = new MenuItem(newMenu, SWT.NULL);
    Messages.setLanguageText(file_new_folder, "MainWindow.menu.file.folder"); //$NON-NLS-1$
    file_new_folder.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        openDirectory();
      }
    });

    	// file->share submenus
    
    Menu shareMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    file_share.setMenu(shareMenu);

    MenuItem file_share_file = new MenuItem(shareMenu, SWT.NULL);
    Messages.setLanguageText(file_share_file, "MainWindow.menu.file.share.file");
    file_share_file.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
    		ShareUtils.shareFile( mainWindow );
    	}
    });
    
    MenuItem file_share_dir = new MenuItem(shareMenu, SWT.NULL);
    Messages.setLanguageText(file_share_dir, "MainWindow.menu.file.share.dir");
    file_share_dir.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
    		ShareUtils.shareDir( mainWindow );
    	}
    });
    
    MenuItem file_share_dircontents = new MenuItem(shareMenu, SWT.NULL);
    Messages.setLanguageText(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
    file_share_dircontents.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
    		ShareUtils.shareDirContents( mainWindow, false );
    	}
    });
    MenuItem file_share_dircontents_rec = new MenuItem(shareMenu, SWT.NULL);
    Messages.setLanguageText(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
    file_share_dircontents_rec.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
    		ShareUtils.shareDirContents( mainWindow, true );
    	}
    });
       	// file->create
    
    file_create.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new NewTorrentWizard(display);
      }
    });

    file_configure.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new ConfigureWizard(display);
      }
    });

    file_export.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new ExportTorrentWizard(display);
      }
    });

    file_import.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new ImportTorrentWizard(display);
      }
    });

    file_exit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        dispose();
      }
    });

    //The View Menu
    MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
    Messages.setLanguageText(viewItem, "MainWindow.menu.view"); //$NON-NLS-1$
    Menu viewMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    viewItem.setMenu(viewMenu);

    MenuItem view_torrents = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_torrents, "MainWindow.menu.view.mytorrents"); //$NON-NLS-1$
    view_torrents.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
		showMyTorrents();
      }
    });

	MenuItem view_tracker = new MenuItem(viewMenu, SWT.NULL);
	Messages.setLanguageText(view_tracker, "MainWindow.menu.view.mytracker"); //$NON-NLS-1$
	view_tracker.addListener(SWT.Selection, new Listener() {
	  public void handleEvent(Event e) {
		showMyTracker();
	  }
	});
	
	MenuItem view_shares = new MenuItem(viewMenu, SWT.NULL);
	Messages.setLanguageText(view_shares, "MainWindow.menu.view.myshares"); //$NON-NLS-1$
	view_shares.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event e) {
			showMyShares();
		}
	});
	
    MenuItem view_config = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_config, "MainWindow.menu.view.configuration"); //$NON-NLS-1$
    view_config.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (config == null)
          config = new Tab(new ConfigView());
        else
          config.setFocus();
      }
    });

    MenuItem view_console = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_console, "MainWindow.menu.view.console"); //$NON-NLS-1$
    view_console.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (console == null)
          console = new Tab(new ConsoleView());
        else
          console.setFocus();
      }
    });

    MenuItem view_irc = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_irc, "MainWindow.menu.view.irc"); //$NON-NLS-1$
    view_irc.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (irc == null)
          irc = new Tab(new IrcView());
        else
          irc.setFocus();
      }
    });

    MenuItem view_stats = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_stats, "MainWindow.menu.view.stats"); //$NON-NLS-1$
    view_stats.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (stats_tab == null)
          stats_tab = new Tab(new SpeedView(globalManager));
        else
          stats_tab.setFocus();
      }
    });

    new MenuItem(viewMenu, SWT.SEPARATOR);
    
    view_plugin = new MenuItem(viewMenu, SWT.CASCADE);
    Messages.setLanguageText(view_plugin, "MainWindow.menu.view.plugins"); //$NON-NLS-1$
    pluginMenu = new Menu(mainWindow,SWT.DROP_DOWN);
    view_plugin.setEnabled(false);
    view_plugin.setMenu(pluginMenu);
    
    new MenuItem(viewMenu, SWT.SEPARATOR);

    MenuItem view_closeDetails = new MenuItem(viewMenu, SWT.NULL);
    Messages.setLanguageText(view_closeDetails, "MainWindow.menu.closealldetails"); //$NON-NLS-1$
    view_closeDetails.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        Tab.closeAllDetails();
      }
    });

    addCloseDownloadBarsToMenu(viewMenu);

    createLanguageMenu(menuBar, mainWindow, locales);

    //The Help Menu
    MenuItem helpItem = new MenuItem(menuBar, SWT.CASCADE);
    Messages.setLanguageText(helpItem, "MainWindow.menu.help"); //$NON-NLS-1$
    Menu helpMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    helpItem.setMenu(helpMenu);

    MenuItem help_about = new MenuItem(helpMenu, SWT.NULL);
    Messages.setLanguageText(help_about, "MainWindow.menu.help.about"); //$NON-NLS-1$
    help_about.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        AboutWindow.show(display);
      }
    });
    
    MenuItem help_faq = new MenuItem(helpMenu, SWT.NULL);
    Messages.setLanguageText(help_faq, "MainWindow.menu.help.faq"); //$NON-NLS-1$
      help_faq.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            String faqString = "http://azureus.sourceforge.net/faq.php";
            Program.launch(faqString);
          }
        });
    
    if ( !SystemProperties.isJavaWebStartInstance()){
    	
    MenuItem help_checkupdate = new MenuItem(helpMenu, SWT.NULL);
    Messages.setLanguageText(help_checkupdate, "MainWindow.menu.help.checkupdate"); //$NON-NLS-1$
    help_checkupdate.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        checkForNewVersion();
      }
    });
    }

    new MenuItem(helpMenu,SWT.SEPARATOR);
    
    MenuItem help_donate = new MenuItem(helpMenu, SWT.NULL);
    Messages.setLanguageText(help_donate, "MainWindow.menu.help.donate"); //$NON-NLS-1$
    help_donate.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new DonationWindow2(MainWindow.this.display).show();
        //String donationString = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&no_note=1&tax=0&currency_code=EUR";
        //Program.launch(donationString);
      }
    });
  } catch (Exception e) {
    LGLogger.log(LGLogger.ERROR, "Error while creating menu items");
    e.printStackTrace();
  }

    createDropTarget(mainWindow);

    GridLayout mainLayout = new GridLayout();
    mainLayout.numColumns = 1;
    mainLayout.marginHeight = 0;
    mainLayout.marginWidth = 0;
    mainLayout.horizontalSpacing = 0;
    mainLayout.verticalSpacing = 0;
    mainWindow.setLayout(mainLayout);
    
    this.iconBar = new IconBar(mainWindow);
    this.iconBar.setCurrentEnabler(this);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    this.iconBar.setLayoutData(gridData);
    
    gridData = new GridData(GridData.FILL_BOTH);
    if(!useCustomTab) {
      folder = new TabFolder(mainWindow, SWT.V_SCROLL);
    } else {
      folder = new CTabFolder(mainWindow, SWT.NULL);
    }
    folder.setLayoutData(gridData);
    
    Tab.setFolder(folder);   
    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        if(display != null && ! display.isDisposed())
          display.asyncExec(new Runnable() {
	          public void run() {
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
      ((CTabFolder)folder).setLayoutData(gridData);
      ((CTabFolder)folder).addCTabFolderListener(new CTabFolderAdapter() {
        public void itemClosed(CTabFolderEvent event) {
          Tab.closed((CTabItem) event.item);
          event.doit = true;
        }
      });
      ((CTabFolder)folder).addSelectionListener(selectionAdapter);

      Display display = folder.getDisplay();
      ((CTabFolder)folder).setSelectionBackground(new Color[] {display.getSystemColor(SWT.COLOR_LIST_BACKGROUND) },
                                                  new int[0]);
      ((CTabFolder)folder).setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
    }

    LGLogger.log("Initializing GUI complete");
    splashNextTask();
    setSplashTask( "splash.initializePlugins");

    PluginInitializer.getSingleton(globalManager,splash_maybe_null).initializePlugins();        

    LGLogger.log("Initializing Plugins complete");
    splashNextTask();
    setSplashTask( "splash.openViews");
    
    showMyTorrents();

  	if ( TRHostFactory.create().getTorrents().length > 0 ){  		
  		showMyTracker();
  	}
	
    if (COConfigurationManager.getBooleanParameter("Open Console", false))
      console = new Tab(new ConsoleView());    

    gridData = new GridData(GridData.FILL_HORIZONTAL);

    Composite statusBar = new Composite(mainWindow, SWT.SHADOW_IN);
    statusBar.setLayoutData(gridData);
    GridLayout layout_status = new GridLayout();
    layout_status.numColumns = 4;
    layout_status.horizontalSpacing = 1;
    layout_status.verticalSpacing = 0;
    layout_status.marginHeight = 0;
    layout_status.marginWidth = 0;
    statusBar.setLayout(layout_status);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    statusText = new CLabel(statusBar, SWT.SHADOW_IN);
    statusText.setLayoutData(gridData);

		checkForNewVersion();

		gridData = new GridData();
		gridData.widthHint = 105;
		ipBlocked = new CLabel(statusBar, SWT.SHADOW_IN);
		ipBlocked.setText("IPs:"); //$NON-NLS-1$
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
    statusDown.setText("D:"); //$NON-NLS-1$
    statusDown.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 105;
    statusUp = new CLabel(statusBar, SWT.SHADOW_IN);
    statusUp.setText("U:"); //$NON-NLS-1$
    statusUp.setLayoutData(gridData);
    
    final Menu menuUpSpeed = new Menu(mainWindow,SWT.POP_UP);
    menuUpSpeed.addListener(SWT.Show,new Listener() {
      public void handleEvent(Event e) {
        MenuItem[] items = menuUpSpeed.getItems();
        for(int i = 0 ; i < items.length ; i++) {
         items[i].dispose(); 
        }
        
        int upLimit = COConfigurationManager.getIntParameter("Max Upload Speed",0);
        int index = findIndex(upLimit/1024,ConfigView.upRates);
        
        MenuItem item = new MenuItem(menuUpSpeed,SWT.RADIO);
        item.setText(MessageText.getString("ConfigView.unlimited"));
        item.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            COConfigurationManager.setParameter("Max Upload Speed",0); 
          }
        });
        if(index == 0) item.setSelection(true);
        
        int start = index - 10;
        if(start < 1) start = 1;
        for(int i = start ; i < start + 20 && i < ConfigView.upRates.length ; i++) {
          final int fi = i;
          item = new MenuItem(menuUpSpeed,SWT.RADIO);
          item.setText(ConfigView.upRates[i] + " KB/s");
          item.addListener(SWT.Selection,new Listener() {
            public void handleEvent(Event e) {
             COConfigurationManager.setParameter("Max Upload Speed",ConfigView.upRates[fi]*1024); 
            }
          });
          if(i == index) item.setSelection(true);
        }
      }           
      
      private int findIndex(int value,int values[]) {
        for(int i = 0 ; i < values.length ;i++) {
          if(values[i] == value)
            return i;
        }
        return 0;
      }
      
    });
    
    statusUp.setMenu(menuUpSpeed);
    
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
    
    closeSplashWindow();
    
    // share progress window
    
    new ProgressWindow();
    
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
    
    mainWindow.open();
    mainWindow.forceActive();
    updater = new GUIUpdater();
    updater.start();

    boolean available = false;
    try {
      available = SysTrayMenu.isAvailable();
    }
    catch (NoClassDefFoundError e) {}

    if (available)
      trayIcon = new SystemTray(this);
    else
      tray = new TrayWindow(this);

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

    if (COConfigurationManager.getBooleanParameter("Start Minimized", false))
      minimizeToTray(null);

    if (COConfigurationManager.getBooleanParameter("Password enabled", false)) {
      mainWindow.setVisible(false);
      PasswordWindow.showPasswordWindow(display);
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
    Tab.addTabKeyListenerToComposite(folder);
    
    gm.startChecker();
    
    new Thread("Init Complete")
    {
    	public void
    	run()
    	{
    		PluginInitializer.initialisationComplete();
    		
    		synchronized( alert_queue ){
    			
    			initialisation_complete	= true;
    			
    			for (int i=0;i<alert_queue.size();i++){
    				
    				Object[]	x = (Object[])alert_queue.get(i);
    				
    				int		type 	= ((Integer)x[0]).intValue();
    				String	message = (String)x[1];
    				
    				showAlert( type, message );
    			}
    		}
    	}
    }.start();
    
    checkForDonationPopup();
    
  }catch( Throwable e ){
		e.printStackTrace();
	} }

  private void
  showAlert(
  		int	type,
		String	message )
  {
  	if ( alert_history.contains( message )){
  		return;
  	}
  	
  	alert_history.add( message );
  	
	if ( type == LGLogger.AT_COMMENT ){
			
		showCommentMessageBox( message );
		
	}else if ( type == LGLogger.AT_WARNING ){
		
		showWarningMessageBox( message );
			       						
	}else{
				
	    showErrorMessageBox( message );
	}
  }
  
  
	private void startFolderWatcher() {
    if(folderWatcher == null)
      folderWatcher = FileImporter.getFolderWatcher();
	  folderWatcher.startIt();
  }

  private void stopFolderWatcher() {
    if(folderWatcher != null) {
      folderWatcher.stopIt();
      folderWatcher.interrupt();
      folderWatcher = null;
    }
  }

  public void allocateBlues() {
    int r = 0;
    int g = 128;
    int b = 255;
    try {
      r = COConfigurationManager.getIntParameter("Color Scheme.red",r);
      g = COConfigurationManager.getIntParameter("Color Scheme.green",g);
      b = COConfigurationManager.getIntParameter("Color Scheme.blue",b);
      for(int i = 0 ; i < 5 ; i++) {
        Color toBeDisposed = blues[i];
        blues[i] = new Color(display,r+((255-r)*(4-i))/4,g+((255-g)*(4-i))/4,b+((255-b)*(4-i))/4);
        if(toBeDisposed != null && ! toBeDisposed.isDisposed()) {
          toBeDisposed.dispose();
        }
      }
    } catch (Exception e) {
      LGLogger.log(LGLogger.ERROR, "Error allocating colors");
      e.printStackTrace();
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
    mainWindow.setVisible(false);
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

  private void createLanguageMenu(Menu menu, Decorations decoMenu, Locale[] locales) {
    if (languageMenu != null) {
      MenuItem[] items = languageMenu.getItems();
      for (int i = 0; i < items.length; i++)
        items[i].dispose();
    } else {
      MenuItem languageItem = new MenuItem(menu, SWT.CASCADE);
      Messages.setLanguageText(languageItem, "MainWindow.menu.language"); //$NON-NLS-1$
      languageMenu = new Menu(decoMenu, SWT.DROP_DOWN);
      languageItem.setMenu(languageMenu);
    }

    MenuItem[] items = new MenuItem[locales.length];

    for (int i = 0; i < locales.length; i++) {
      //      System.out.println("found Locale: " + locales[i]);
      items[i] = new MenuItem(languageMenu, SWT.RADIO);
      createLanguageMenuitem(items[i], locales[i]);
    }

    Locale currentLocale = MessageText.getCurrentLocale();
      for (int i = 0; i < items.length; i++) {
        items[i].setSelection(currentLocale.equals(items[i].getData()));
        }
        
    new MenuItem(languageMenu, SWT.SEPARATOR);
    MenuItem itemRefresh = new MenuItem(languageMenu, SWT.PUSH);
    Messages.setLanguageText(itemRefresh, "MainWindow.menu.language.refresh");
    itemRefresh.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        MenuItem item = (MenuItem)event.widget;
        createLanguageMenu(menuBar, mainWindow, MessageText.getLocales());
        if (MessageText.changeLocale(MessageText.getCurrentLocale(), true)) {
          setSelectedLanguageItem(selectedLanguageItem);
        }
      }
    });
  }

  private void setStatusVersion() {
    if (statusText != null && !statusText.isDisposed())
      statusText.setText("Azureus " + VERSION + " / " + MessageText.getString("MainWindow.status.latestversion") + " : " + latestVersion); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  private void setStatusVersionFromOtherThread() {
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      public void run() {
        setStatusVersion();
      }
    });
  }

  private void createLanguageMenuitem(MenuItem language, final Locale locale) {
    language.setData(locale);
    language.setText(locale.getDisplayName(locale));
    language.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (isSelectedLanguageDifferent(e.widget)) {
          if (MessageText.changeLocale(locale)) {
            COConfigurationManager.setParameter("locale", locale.toString()); //$NON-NLS-1$
            COConfigurationManager.save();
            setSelectedLanguageItem((MenuItem) e.widget);
          }
          else {
            ((MenuItem) e.widget).setSelection(false);
            selectSelectedLanguageItem();
          }
        }
      }
    });
    language.setSelection(MessageText.isCurrentLocale(locale));
    if (language.getSelection())
      selectedLanguageItem = language;
  }

  private synchronized void setSelectedLanguageItem(MenuItem newLanguage) {
    selectedLanguageItem = newLanguage;
    Messages.updateLanguageForControl(mainWindow);
    updateMenuText(menuBar);
    if (statusText != null)
      statusText.update();
    if (folder != null) {
      if(useCustomTab) {
        ((CTabFolder)folder).update();
      } else {
        ((TabFolder)folder).update();
      }
    }
    if (trayIcon != null) {
      trayIcon.updateLanguage();
      trayIcon.refresh();
    }
    if (tray != null)
      tray.updateLanguage();

    Tab.updateLanguage();

    setStatusVersion();
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
  	if (trayIcon != null) {
  		trayIcon.refresh();
  	} 
  }
 
  private void showSplashWindow() {
    if (splash_maybe_null == null && display != null) {
      splash_maybe_null = new SplashWindow();
      splash_maybe_null.show(display);
    }
  }

  private void setSplashPercentage( int p ){
  	if ( splash_maybe_null != null ){
		splash_maybe_null.setPercentDone(p);
  	}
  }

  private void splashNextTask(){
  	if ( splash_maybe_null != null ){
		splash_maybe_null.nextTask();
  	}
  }
  
  private void setSplashTask( String s ){
	if ( splash_maybe_null != null ){
		splash_maybe_null.setCurrentTask(MessageText.getString(s));
	}
  }


  private void closeSplashWindow() {
    if (splash_maybe_null != null) {
      splash_maybe_null.close();
      splash_maybe_null = null;
    }
  }

  private void showUpgradeWindow() {
    final Shell s = new Shell(mainWindow, SWT.CLOSE | SWT.PRIMARY_MODAL);

	current_upgrade_window = s;

    s.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    s.setText(MessageText.getString("MainWindow.upgrade.assistant")); //$NON-NLS-1$
    s.setSize(250, 300);
    s.setLayout(new GridLayout(3, true));
    GridData gridData;
    s.setLayoutData(gridData = new GridData());
    //    gridData.horizontalIndent = 10;

    Group gInfo = new Group(s, SWT.NULL);
    gInfo.setLayout(new GridLayout());
    Messages.setLanguageText(gInfo, "MainWindow.upgrade.section.info"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gInfo.setLayoutData(gridData);
    gridData.horizontalSpan = 3;

    Label label = new Label(gInfo, SWT.CENTER);
    int posMessage = latestVersion.indexOf(" (");
    String newVersion = posMessage >= 0 ? latestVersion.substring(0, posMessage) : latestVersion;
    label.setText(MessageText.getString("MainWindow.upgrade.newerversion") + ": " + newVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    FontData[] fontData = label.getFont().getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setStyle(SWT.BOLD);
    }
    Font fontLastestVer = new Font(display, fontData);
    label.setFont(fontLastestVer);
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    label = new Label(gInfo, SWT.LEFT);
    label.setText(MessageText.getString("MainWindow.upgrade.explanation") + ".\n"); //$NON-NLS-1$ //$NON-NLS-2$
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    label = new Label(s, SWT.LEFT);
    label.setText("\n"); //$NON-NLS-1$
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    Group gManual = new Group(s, SWT.NULL);
    gManual.setLayout(new GridLayout());
    Messages.setLanguageText(gManual, "MainWindow.upgrade.section.manual"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gManual.setLayoutData(gridData);
    gridData.horizontalSpan = 3;

    label = new Label(gManual, SWT.NULL);
    label.setText(MessageText.getString("MainWindow.upgrade.explanation.manual") + ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
    label.setLayoutData(gridData = new GridData());

    final String downloadLink;
    if (latestVersionFileName == null) {
      downloadLink = "http://azureus.sourceforge.net/Azureus2.jar"; //$NON-NLS-1$
    }
    else {
      downloadLink = "http://prdownloads.sourceforge.net/azureus/" + latestVersionFileName + "?download";
    }
    final Label linklabel = new Label(gManual, SWT.NULL);
    linklabel.setText(downloadLink);
    linklabel.setCursor(handCursor);
    linklabel.setForeground(blue);
    linklabel.setLayoutData(gridData = new GridData());

    linklabel.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
        Program.launch(downloadLink);
      }
      public void mouseDown(MouseEvent arg0) {
        Program.launch(downloadLink);
      }
    });

    label = new Label(s, SWT.LEFT);
    label.setText("\n"); //$NON-NLS-1$
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    Group gAutomatic = new Group(s, SWT.NULL);
    gAutomatic.setLayout(new GridLayout());
    Messages.setLanguageText(gAutomatic, "MainWindow.upgrade.section.automatic"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gAutomatic.setLayoutData(gridData);
    gridData.horizontalSpan = 3;

    final Label step1 = new Label(gAutomatic, SWT.LEFT);
    step1.setText("- " + MessageText.getString("MainWindow.upgrade.step1")); //$NON-NLS-1$ //$NON-NLS-2$
    step1.setForeground(blue);
    step1.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    final Label step2 = new Label(gAutomatic, SWT.LEFT);
    step2.setText("- " + MessageText.getString("MainWindow.upgrade.step2") + "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    step2.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    final Label hint = new Label(gAutomatic, SWT.LEFT);
    hint.setText(MessageText.getString("MainWindow.upgrade.hint1") + "."); //$NON-NLS-1$ //$NON-NLS-2$
    hint.setLayoutData(gridData = new GridData(GridData.FILL_HORIZONTAL));
    gridData.horizontalSpan = 3;

    label = new Label(gAutomatic, SWT.LEFT);
    label.setText("\n"); //$NON-NLS-1$
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    final ProgressBar progressBar = new ProgressBar(gAutomatic, SWT.SMOOTH);
    progressBar.setLayoutData(gridData = new GridData(GridData.FILL_HORIZONTAL));
    gridData.horizontalSpan = 3;
    progressBar.setToolTipText(MessageText.getString("MainWindow.upgrade.tooltip.progressbar")); //$NON-NLS-1$

    label = new Label(s, SWT.LEFT);
    label.setText("\n"); //$NON-NLS-1$
    label.setLayoutData(gridData = new GridData());
    gridData.horizontalSpan = 3;

    final Button next = new Button(s, SWT.PUSH);
    next.setText(" " + MessageText.getString("Button.next") + " > "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$

    gridData = new GridData();
    next.setLayoutData(gridData);

    final Button finish = new Button(s, SWT.PUSH);
    finish.setText(" " + MessageText.getString("Button.finish") + " "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
    finish.setLayoutData(new GridData());

    final Button cancel = new Button(s, SWT.PUSH);
    cancel.setText(" " + MessageText.getString("Button.cancel") + " "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
    cancel.setLayoutData(new GridData());

    SelectionAdapter update = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        downloadJar(progressBar, hint);
        if (jarDownloaded) {
          if (event.widget == finish) {
            updateJar = true;
			current_upgrade_window = null;
            s.dispose();
            dispose();
          }
          else {
            next.setEnabled(false);
            step1.setForeground(black);
            step2.setForeground(blue);
            s.setDefaultButton(finish);
            hint.setText(MessageText.getString("MainWindow.upgrade.hint2") + "."); //$NON-NLS-1$ //$NON-NLS-2$
            hint.setForeground(black);
            hint.pack();
            linklabel.setEnabled(false);
          }
        }
        else {
          if (event.widget == finish) {
			current_upgrade_window = null;
            s.dispose();
          }
          else {
            hint.setText(MessageText.getString("MainWindow.upgrade.error.downloading.hint") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
            hint.setForeground(red);
            hint.pack();
            next.setEnabled(false);
            finish.setEnabled(false);
          }
        }
      }
    };

    next.addSelectionListener(update);
    finish.addSelectionListener(update);

    cancel.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        s.dispose();
		current_upgrade_window = null;
      }
    });

    s.pack();

    Rectangle parent = mainWindow.getBounds();
    Rectangle child = s.getBounds();
    child.x = parent.x + (parent.width - child.width) / 2;
    child.y = parent.y + (parent.height - child.height) / 2;
    s.setBounds(child);

    closeSplashWindow();
    s.open();
    s.setFocus();

    while (!s.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    } //end while
    if (fontLastestVer != null && !fontLastestVer.isDisposed()) {
      fontLastestVer.dispose();
    }
  }

  private void updateJar() {
    FileOutputStream out = null;
    InputStream in = null;
    try {
      String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
      String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
      String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
      String javaPath = System.getProperty("java.home")
                      + System.getProperty("file.separator")
                      + "bin"
                      + System.getProperty("file.separator");
      
     
      //remove any trailing slashes
      if (libraryPath.endsWith("\\")) {
        libraryPath = libraryPath.substring(0, libraryPath.length() -1);
      }
      
      File logFile = new File( userPath, "update.log" );
      FileWriter log = new FileWriter( logFile, true );
      
      log.write(new Date(System.currentTimeMillis()).toString() + "\n");
      log.write("updateJar:: classPath=" + classPath
                         + " libraryPath=" + libraryPath
                         + " userPath=" + userPath + "\n");
    
      //File updaterJar = FileUtil.getApplicationFile("Updater.jar"); //$NON-NLS-1$
      File updaterJar = new File(userPath, "Updater.jar");
      
      log.write("updateJar:: looking for " + updaterJar.getAbsolutePath() + "\n");
      
      if (!updaterJar.isFile()) {
        log.write("updateJar:: downloading new Updater.jar file .....");
        URL reqUrl = new URL("http://azureus.sourceforge.net/Updater.jar"); //$NON-NLS-1$
        HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
        con.connect();
        in = con.getInputStream();
        out = new FileOutputStream(updaterJar);
        byte[] buffer = new byte[2048];
        int c;
        while ((c = in.read(buffer)) != -1) {
          out.write(buffer, 0, c);
          display.readAndDispatch();
        }
        log.write("done\n");
      }
      else log.write("updateJar:: using existing Updater.jar file\n");

      String exec = javaPath + "java -classpath \"" + updaterJar.getAbsolutePath()
                  + "\" org.gudy.azureus2.update.Updater \"" + classPath
                  + "\" \"" + libraryPath
                  + "\" \"" + userPath + "\"";

      log.write("updateJar:: executing command: " + exec + "\n");
      if (log != null) log.close();
      
      Runtime.getRuntime().exec(exec);
    }
    catch (Exception e1) {
      e1.printStackTrace();
      updateJar = false;
    }
    finally {
      try {
        if (out != null) out.close();
        if (in != null) in.close();
      }
      catch (Exception e) {}
    }
  }

  private String readUrl(URL url) {
    String result = "";
    InputStream in = null;
    try {
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.connect();
      in = con.getInputStream();
      final ByteArrayOutputStream message = new ByteArrayOutputStream();
      byte[] data = new byte[1024];
      int nbRead = 0;
      while (nbRead >= 0) {
        try {
          nbRead = in.read(data);
          if (nbRead >= 0)
            message.write(data, 0, nbRead);
          Thread.sleep(20);
        }
        catch (Exception e) {
          nbRead = -1;
        }
        display.readAndDispatch();
      }
      result = message.toString();
    }
    catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
    }
    catch (Exception ignore) {}
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (Exception e) {}
        in = null;
      }
    }

    return result;
  }

  private void downloadJar(final ProgressBar progressBar, final Label hint) {
    if (jarDownloaded) {
      progressBar.setSelection(progressBar.getMaximum());
      return;
    }

    FileOutputStream fos = null;
    InputStream in = null;
    FileWriter log = null;
    try {
      String userPath = System.getProperty("user.dir");
      File logFile = new File( userPath, "update.log" );
      log = new FileWriter( logFile, true );
      
      //File originFile = FileUtil.getApplicationFile("Azureus2.jar"); //$NON-NLS-1$
      File originFile = new File(userPath, "Azureus2.jar");
      File newFile = new File(originFile.getParentFile(), "Azureus2-new.jar"); //$NON-NLS-1$
      
      log.write("downloadJar:: originFile=" + originFile.getAbsolutePath()
                           + " newFile=" + newFile.getAbsolutePath() + "\n");
      
      URL reqUrl = null;
      if (latestVersionFileName == null) {
        reqUrl = new URL("http://azureus.sourceforge.net/Azureus2.jar"); //$NON-NLS-1$
      }
      else {
        //New update method, using sourceforge mirrors.
        URL mirrorsUrl = new URL("http://prdownloads.sourceforge.net/azureus/" + latestVersionFileName + "?download");
        String mirrorsHtml = readUrl(mirrorsUrl);
        List mirrors = new ArrayList();
        String pattern = "/azureus/" + latestVersionFileName + "?use_mirror=";
        int position = mirrorsHtml.indexOf(pattern);
        while (position > 0) {
          int end = mirrorsHtml.indexOf(">", position);
          if (end < 0) {
            position = -1;
          }
          else {
            String mirror = mirrorsHtml.substring(position, end);
            //System.out.println(mirror);
            mirrors.add(mirror);
            position = mirrorsHtml.indexOf(pattern, position + 1);
          }
        }

        //Grab a random mirror
        if (mirrors.size() == 0)
          return;
        int random = (int) (Math.random() * mirrors.size());
        String mirror = (String) (mirrors.get(random));

        URL mirrorUrl = new URL("http://prdownloads.sourceforge.net" + mirror);
        String mirrorHtml = readUrl(mirrorUrl);
        pattern = "<META HTTP-EQUIV=\"refresh\" content=\"5; URL=";
        position = mirrorHtml.indexOf(pattern);
        //System.out.println("position="+position);
        if (position < 0)
          return;
        int end = mirrorHtml.indexOf("\">", position);
        if (end < 0)
          return;
        reqUrl = new URL(mirrorHtml.substring(position + pattern.length(), end));
        //System.out.println(reqUrl.toString());
      }

      if (reqUrl == null)
        return;
      hint.setText(MessageText.getString("MainWindow.upgrade.downloadingfrom") + reqUrl);
      
      log.write("downloadJar:: downloading new Azureus jar from " + reqUrl + " .....");
      
      HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
      con.connect();
      in = con.getInputStream();
      fos = new FileOutputStream(newFile);

      progressBar.setMinimum(0);
      progressBar.setMaximum(100);

      final InputStream input = in;
      final FileOutputStream output = fos;

      final long length = con.getContentLength();
      final byte[] buffer = new byte[8192];
      int c;
      long bytesDownloaded = 0L;
      while ((c = input.read(buffer)) != -1) {
        output.write(buffer, 0, c);
        bytesDownloaded += c;
        int progress = (int) Math.round(((double) bytesDownloaded / length) * 100);
        progressBar.setSelection(progress <= 100 ? progress : 100);
        progressBar.update();
        display.readAndDispatch();
      }
      log.write("done\n");
      
      jarDownloaded = true;
    }
    catch (MalformedURLException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
      	if (log != null) log.close();
        if (fos != null) fos.close();
        if (in != null) in.close();
      }
      catch (Exception e) {}
    }
  }

  public static void updateMenuText(Object menu) {
    if (menu == null)
      return;
    if (menu instanceof Menu) {
      MenuItem[] menus = ((Menu) menu).getItems();
      for (int i = 0; i < menus.length; i++) {
        updateMenuText(menus[i]);
      }
    }
    else if (menu instanceof MenuItem) {
      MenuItem item = (MenuItem) menu;
      if (item.getData() != null) {
        if (item.getData() instanceof String) {
          item.setText(MessageText.getString((String) item.getData()));
          updateMenuText(item.getMenu());
        }
      }
    }
  }

  private boolean isSelectedLanguageDifferent(Widget newLanguage) {
    return selectedLanguageItem != newLanguage;
  }

  private void selectSelectedLanguageItem() {
    selectedLanguageItem.setSelection(true);
  }

  protected void addCloseDownloadBarsToMenu(Menu menu) {
    MenuItem view_closeAll = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(view_closeAll, "MainWindow.menu.closealldownloadbars"); //$NON-NLS-1$
    view_closeAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        closeDownloadBars();
      }
    });
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
          openTorrent(source.getAbsolutePath(), startInStoppedState);
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
        		openTorrentsFromDirectory(dir_name, startInStoppedState);
        	}
        }
      }
    } else {
      openUrl(((URLTransfer.URLType)event.data).linkURL);
    }
  }

  public void waitForClose() {
    while (!mainWindow.isDisposed()) {
      try {
        if (!display.readAndDispatch())
          display.sleep();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (tray != null)
      tray.dispose();
    if (irc != null)
      irc.dispose();

    display.dispose();
  }

  public static void main(String args[]) {
  	
  	COConfigurationManager.setSystemProperties();
  	
    LocaleUtil lu = new LocaleUtilSWT();
    LocaleUtil.setLocaleUtilChooser(lu);
    GlobalManager gm = 
    	GlobalManagerFactory.create(); 
 
    		
    MainWindow mw = new MainWindow(gm, null);
    mw.waitForClose();
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
      if (trayIcon != null)
        trayIcon.showIcon();
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
    isAlreadyDead = true; //NICO try to never die twice...
    if (this.trayIcon != null)
      SysTrayMenu.dispose();
    stopFolderWatcher();
    if (startServer != null)
      startServer.stopIt();
    updater.stopIt();
    globalManager.stopAll();
    
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

    if (instanceCount-- == 0) {
      for (int i = 0; i < blues.length; i++) {
        if (blues[i] != null && !blues[i].isDisposed())
          blues[i].dispose();
      }
      if (grey != null && !grey.isDisposed())
        grey.dispose();
      if (black != null && !black.isDisposed())
        black.dispose();
      if (blue != null && !blue.isDisposed())
        blue.dispose();
      if (red != null && !red.isDisposed())
        red.dispose();
      if (white != null && !white.isDisposed())
        white.dispose();
      if (red_ConsoleView != null && !red_ConsoleView.isDisposed())
        red_ConsoleView.dispose();
      if (red_ManagerItem != null && !red_ManagerItem.isDisposed())
        red_ManagerItem.dispose();
      if (handCursor != null && !handCursor.isDisposed())
        handCursor.dispose();
    }
    if (updateJar){
    
      updateJar();
    }
    
    	// problem with closing down web start as AWT threads don't close properly

	if ( SystemProperties.isJavaWebStartInstance()){    	
 	
    	System.exit(1);
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

  public void openTorrent(final String fileName) {
    openTorrent(fileName, false);
  }

  public void openTorrent(final String fileName, final boolean startInStoppedState) {
    try {
      if (!FileUtil.isTorrentFile(fileName)){
      	
        LGLogger.log( "MainWindow::openTorrent: file it not a torrent file, sharing" );

        ShareUtils.shareFile( fileName );
        
        return;
      }
    } catch (Exception e) {
    	
      LGLogger.log( "MainWindow::openTorrent: check fails", e );

      return;
    }

    if(display != null && ! display.isDisposed())
      display.asyncExec(new Runnable() {
        public void run() {
          if(!COConfigurationManager.getBooleanParameter("Add URL Silently", false))
            mainWindow.setActive();
          
          new Thread() {
            public void run() {
              try{
	              String savePath = getSavePath(fileName);
	              if (savePath == null){
	                LGLogger.log( "MainWindow::openTorrent: save path not set, aborting" );
	
	                return;
	              }
	              // set to STATE_WAITING if we don't want to startInStoppedState
	              // so that auto-open details will work (even if the torrent
	              // immediately goes to queued)
	              
	              LGLogger.log( "MainWindow::openTorrent: adding download" );
	
	              globalManager.addDownloadManager(fileName, savePath, 
	                                               startInStoppedState ? DownloadManager.STATE_STOPPED 
	                                                                   : DownloadManager.STATE_WAITING);
              }catch( Throwable e ){
              	
                LGLogger.log( "MainWindow::openTorrent: torrent addition fails", e );

              }
            }
          }
          .start();
        }
      });
  }
  
  public String getSavePath(String fileName) {
    return getSavePathSupport(fileName,true,false);
  }
  
  protected String 
  getSavePathSupport(
  	String fileName,
  	boolean useDefault,
  	boolean forSeeding) 
  {
  		// This *musn't* run on the swt thread as the torrent decoder stuff can need to 
  		// show a window...
  		
	final String[] savePath = {""};

    boolean useDefDataDir = COConfigurationManager.getBooleanParameter("Use default data dir", true);
    
    if (useDefDataDir) savePath[0] = COConfigurationManager.getStringParameter("Default save path", "");
    
    if (savePath[0].length() == 0 || ! useDefault) {

      boolean singleFile = false;
      
      String singleFileName = ""; //$NON-NLS-1$

      try {
        TOTorrent torrent = TorrentUtils.readFromFile(fileName);
        
        singleFile = torrent.isSimpleTorrent();
        
        LocaleUtilDecoder	locale_decoder = LocaleUtil.getTorrentEncoding( torrent );
        		
        singleFileName = locale_decoder.decodeString(torrent.getName());
        
        singleFileName = FileUtil.convertOSSpecificChars( singleFileName );
      }
      catch (Exception e) {
        e.printStackTrace();
      }

    
	  final boolean f_singleFile 		= singleFile;
	  final boolean f_forSeeding = forSeeding;
	  final String  f_singleFileName 	= singleFileName;

	  final Semaphore	sem = new Semaphore();
	  
	  display.asyncExec(new Runnable() {
		   public void run()
		   {
		   	  try{
			      if (f_singleFile) {
			      	int style = (f_forSeeding) ? SWT.OPEN : SWT.SAVE;
			        FileDialog fDialog = new FileDialog(mainWindow, SWT.SYSTEM_MODAL | style);
			        
			        fDialog.setFilterPath(COConfigurationManager.getStringParameter("Default Path", "")); //$NON-NLS-1$ //$NON-NLS-2$
			        fDialog.setFileName(f_singleFileName);
			        fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        savePath[0] = fDialog.open();
			      }
			      else {
			        DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.SYSTEM_MODAL);
			        dDialog.setFilterPath(COConfigurationManager.getStringParameter("Default Path", "")); //$NON-NLS-1$ //$NON-NLS-2$
			        dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			        savePath[0] = dDialog.open();
			      }
			      if (savePath[0] != null){
			      			      	
			      	COConfigurationManager.setParameter("Default Path", savePath[0]); //$NON-NLS-1$
			      	
			      	COConfigurationManager.save();
			      }
		   	  }finally{
		   	  	sem.release();
		   	  }
		   }
	  });
 
    
      sem.reserve();
    }
    
    return savePath[0];
  }

  public void openTorrents(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,true);
  }

  public void openTorrentsForSeeding(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,false,true);
  }
  
  public void openTorrents(
  	final String path, 
  	final String fileNames[],
  	final boolean useDefault )
  {
  	openTorrents(path,fileNames,useDefault,false);
  }

  public void openTorrents(
  	final String path, 
  	final String fileNames[],
  	final boolean useDefault,
  	final boolean forSeeding )
  {
	display.asyncExec(new Runnable() {
		 public void run()
		 {
			mainWindow.setActive();

 			new Thread(){
		      public void run() {
		        String separator = System.getProperty("file.separator"); //$NON-NLS-1$
		        for (int i = 0; i < fileNames.length; i++) {
		          if (!FileUtil.getCanonicalFileName(fileNames[i]).endsWith(".torrent")) {
		            if (!FileUtil.getCanonicalFileName(fileNames[i]).endsWith(".tor")) {
		              continue;
		            }
		          }
		          String savePath = getSavePathSupport(path + separator + fileNames[i],useDefault,forSeeding);
		          if (savePath == null)
		            continue;
		          globalManager.addDownloadManager(path + separator + fileNames[i], savePath);
		        }
		      }
		    }.start();
		 }
	});
  }

  public void openTorrentsFromDirectory(String directoryName) {
    openTorrentsFromDirectory(directoryName, false);
  }

  public void openTorrentsFromDirectory(String directoryName, final boolean startInStoppedState) {
    File f = new File(directoryName);
    if (!f.isDirectory())
      return;
    final File[] files = f.listFiles(new FileFilter() {
      public boolean accept(File arg0) {
        if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".torrent")) //$NON-NLS-1$
          return true;
        if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".tor")) //$NON-NLS-1$
          return true;
        return false;
      }
    });
    if (files.length == 0)
      return;

    DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    if (COConfigurationManager.getBooleanParameter("Use default data dir", true)) {
      String default_path = COConfigurationManager.getStringParameter("Default save path", "");
      if (default_path.length() > 0) {
        dDialog.setFilterPath(default_path);
      }
    }

    dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles")); //$NON-NLS-1$
    final String savePath = dDialog.open();
    if (savePath == null)
      return;

    new Thread() {
      public void run() {
        for (int i = 0; i < files.length; i++)
          globalManager.addDownloadManager(files[i].getAbsolutePath(), savePath, 
                                           startInStoppedState ? DownloadManager.STATE_STOPPED 
                                                               : DownloadManager.STATE_QUEUED);
      }
    }
    .start();
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
  public Tab getIrc() {
    return irc;
  }

  /**
	 * @param tab
	 */
  public void setIrc(Tab tab) {
    irc = tab;
  }

  /**
	 * @return
	 */
  public TrayWindow getTray() {
    return tray;
  }

  /**
   * @return Returns the background.
   */
  public Color getBackground() {
    return background;
  }

  /**
   * @param background The background to set.
   */
  public void setBackground(Color background) {
    if(MainWindow.background != null && !MainWindow.background.isDisposed()) {
      Color old = MainWindow.background;
      MainWindow.background = background;
      old.dispose();
    }
    
  }

  /**
   * @return Returns the useCustomTab.
   */
  public boolean isUseCustomTab() {
    return useCustomTab;
  }    
  
  
  MenuItem view_plugin;
  Menu pluginMenu;
  
  Map pluginTabs = new HashMap();
  
  public void addPluginView(final PluginView view) {
    MenuItem item = new MenuItem(pluginMenu,SWT.NULL);
    item.setText(view.getPluginViewName());
    item.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        Tab tab = (Tab) pluginTabs.get(view.getPluginViewName());
        if(tab != null) {
          tab.setFocus();
        } else {
          tab = new Tab(view);
          pluginTabs.put(view.getPluginViewName(),tab);         
        }
      }
    });
    view_plugin.setEnabled(true);
  }
  
  public void removeActivePluginView(final PluginView view) {
    pluginTabs.remove(view.getPluginViewName());
  }
  
  public void openUrl() {
    openUrl(null);
  }

  public void openUrl(String linkURL) {
    if(linkURL != null && linkURL.length() > 20 && COConfigurationManager.getBooleanParameter("Add URL Silently", false))
      new FileDownloadWindow(display, linkURL);
    else
      new OpenUrlWindow(display, linkURL);
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
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
     openTorrent();
     return;
    }
    if(itemKey.equals("open_no_default")) {
      openTorrentNoDefaultSave(false);
      return;
    }
    if(itemKey.equals("open_for_seeding")) {
      openTorrentNoDefaultSave(true);
      return;
    }
    if(itemKey.equals("open_url")) {
      openUrl();
      return;
    }
    if(itemKey.equals("open_folder")) {
      openDirectory();
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
  
  private void openTorrent() {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    openTorrents(fDialog.getFilterPath(), fDialog.getFileNames());
  }
  
  private void openTorrentNoDefaultSave(boolean forSeeding) {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    openTorrents(fDialog.getFilterPath(), fDialog.getFileNames(),false,forSeeding);
  }
  
  private void openDirectory() {
    DirectoryDialog fDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    
    if ( COConfigurationManager.getBooleanParameter( "Save Torrent Files", true )){
      
      String	default_path = COConfigurationManager.getStringParameter( "General_sDefaultTorrent_Directory", "" );
      
      if ( default_path.length() > 0 ){
        
        fDialog.setFilterPath(default_path);
      }
    }

    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.folder")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    openTorrentsFromDirectory(fileName);
  }
  
  public void refreshIconBar() {
    iconBar.setCurrentEnabler(this);
  }

  private synchronized void
  checkForNewVersion()
  {
  	if ( version_check_timer == null ){
  		
  		version_check_timer = new Timer("Auto-update timer",1);

		version_check_timer.addPeriodicEvent( 
			AUTO_UPDATE_CHECK_PERIOD,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	ev )
				{
					checkForNewVersion();
				}
			});
  	}

	Shell current_window = current_upgrade_window;

	if ( current_window == null || current_window.isDisposed()){

		new VersionChecker().start();
	}
  }
  
  private class 
  VersionChecker 
  	extends Thread 
  {	
  	public VersionChecker() {
  		super("Version Checker");
  		
  		setDaemon(true);
  	}
  	public void run() {
		// System.out.println( "Update check start" );

  		latestVersion = MessageText.getString("MainWindow.status.checking") + "..."; //$NON-NLS-1$ //$NON-NLS-2$
  		setStatusVersionFromOtherThread();
  		ByteArrayOutputStream message = new ByteArrayOutputStream(); //$NON-NLS-1$

  		int nbRead = 0;
  		HttpURLConnection con = null;
  		InputStream is = null;
  		try {
  			String id = COConfigurationManager.getStringParameter("ID",null);        
  			String url = "http://azureus.sourceforge.net/version.php";
  			if(id != null && COConfigurationManager.getBooleanParameter("Send Version Info")) {
  				url += "?id=" + id + "&version=" + Constants.AZUREUS_VERSION;
  			}
  			URL reqUrl = new URL(url); //$NON-NLS-1$
  			con = (HttpURLConnection) reqUrl.openConnection();
  			con.connect();
  			is = con.getInputStream();
  			//        int length = con.getContentLength();
  			//        System.out.println(length);
  			byte[] data = new byte[1024];
  			while (nbRead >= 0) {
  				nbRead = is.read(data);
  				if (nbRead >= 0) {
  					message.write(data, 0, nbRead);
  				}
  			}
  			Map decoded = BDecoder.decode(message.toByteArray());
  			latestVersion = new String((byte[]) decoded.get("version")); //$NON-NLS-1$
  			byte[] bFileName = (byte[]) decoded.get("filename"); //$NON-NLS-1$
  			if (bFileName != null)
  				latestVersionFileName = new String(bFileName);

  			if (display == null || display.isDisposed())
  				return;
  			display.asyncExec(new Runnable() {
  				public void run() {
  					if (statusText.isDisposed())
  						return;
  					if (!SystemProperties.isJavaWebStartInstance() &&  VERSION.compareTo(latestVersion) < 0) {
  						latestVersion += " (" + MessageText.getString("MainWindow.status.latestversion.clickupdate") + ")";
  						setStatusVersion();
  						statusText.setForeground(red);
  						statusText.setCursor(handCursor);
  						statusText.addMouseListener(new MouseAdapter() {
  							public void mouseDoubleClick(MouseEvent arg0) {
  								showUpgradeWindow();
  							}
  							public void mouseDown(MouseEvent arg0) {
  								showUpgradeWindow();
  							}
  						});
  						if (COConfigurationManager.getBooleanParameter("Auto Update", true)) {
  							showUpgradeWindow();
  						}
  					}
  					else {
  						setStatusVersion();
  					}
  				}
  			});
  		}
  		catch (Exception e) {
  			if (display == null || display.isDisposed())
  				return;
  			display.asyncExec(new Runnable() {
  				public void run() {
  					if (statusText.isDisposed())
  						return;
  					latestVersion = MessageText.getString("MainWindow.status.unknown"); //$NON-NLS-1$
  					setStatusVersion();
  				}
  			});
  		}
  		finally {
  			if (is != null) {
  				try {
  					is.close();
  				}
  				catch (IOException e1) {}
  				is = null;
  			}
  			if (con != null) {
  				con.disconnect();
  				con = null;
  			}
  		}
  	}
  }
  
  private void showBlockedIps() {
    StringBuffer sb = new StringBuffer();
    BlockedIp[] blocked = IpFilter.getInstance().getBlockedIps();
    String inRange = MessageText.getString("ConfigView.section.ipfilter.list.inrange");
    String notInRange = MessageText.getString("ConfigView.section.ipfilter.list.notinrange");    
    for(int i=0;i<blocked.length;i++){
      BlockedIp bIp = blocked[i];
      sb.append(DisplayFormatters.formatTimeStamp(bIp.getBlockedTime()));
      sb.append("\t");
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
  
  
  public static void
  showErrorMessageBoxUsingResourceString(
  	String		title_key,
	Throwable	error )
  {
  	showErrorMessageBox(MessageText.getString(title_key), error);
  }
  
  public static void
  showErrorMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_ERROR, "AlertMessageBox.error", message );
  }
  
  public static void
  showWarningMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_WARNING, "AlertMessageBox.warning", message );
  }
  
  public static void
  showCommentMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_INFORMATION, "AlertMessageBox.information", message );
  }
  
  public static void
  showErrorMessageBox(
  	String		title,
	Throwable	error )
  {
  	String error_message = error.getMessage();
  	
  	if ( error_message == null ){
  		
  		error_message = error.toString();
  	}
  
  	showMessageBox( SWT.ICON_ERROR, title, error_message );
  } 
  
  public static void
  showMessageBoxUsingResourceString(
  	int			type,
    String		key,
	String		message )
  {
  	showMessageBox( type,MessageText.getString(key), message );
  }
  
  public static void
  showMessageBox(
  	final int			type,
    final String		title,
	final String		message )
  {
  	Display display = getWindow().getDisplay();
  
 	display.asyncExec(new Runnable() {
		public void 
		run()
		{
			MessageBox mb = new MessageBox(MainWindow.getWindow().getShell(), type | SWT.OK );
    	
			mb.setText(title);
  	  	  	
			mb.setMessage(	message );
  	  	
			mb.open();
		}
 	});
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
}
