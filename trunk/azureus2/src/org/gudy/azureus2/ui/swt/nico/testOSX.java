/*
 * Created on 25 juin 2003
 *  
 */
package org.gudy.azureus2.ui.swt.nico;

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
import java.util.Properties;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.pluginsimpl.PluginInitializer;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.systray.SystemTray;
import org.gudy.azureus2.ui.swt.auth.*;

import snoozesoft.systray4j.SysTrayMenu;

/**
 * @author Olivier
 *  
 */
public class testOSX extends  Object{

  public static final String VERSION = Constants.AZUREUS_VERSION;
  private String latestVersion = ""; //$NON-NLS-1$
  private String latestVersionFileName = null;

  private static testOSX window;

  private static boolean jarDownloaded = false;
  private static boolean updateJar = false;

 
 private static int instanceCount = 0;

  private Display display;
  private Shell mainWindow;
  private Menu menuBar;

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
    
  private IView viewMyTorrents;
  
  private IView my_tracker_view;
  
  
  
 
  private MenuItem selectedLanguageItem;

  private SystemTray trayIcon;

  private HashMap downloadViews;
  private HashMap downloadBars;

 
  public static final long AUTO_UPDATE_CHECK_PERIOD = 23*60*60*1000;	// 23 hours

  private Shell			current_upgrade_window;
  private Timer			version_check_timer;
  
  private FolderWatcher folderWatcher = null;


  public testOSX(GlobalManager gm) {
    if (window != null) {
      if(!COConfigurationManager.getBooleanParameter("Add URL Silently", false))
        setVisible(true);
      return;
    }
    
    try{
	    
    window = this;
     this.globalManager = gm;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    

    if (instanceCount == 0) {      
     black = new Color(display, new RGB(0, 0, 0));
      blue = new Color(display, new RGB(0, 0, 170));
      grey = new Color(display, new RGB(170, 170, 170));
      red = new Color(display, new RGB(255, 0, 0));
      white = new Color(display, new RGB(255, 255, 255));
      background = new Color(display , new RGB(248,248,248));
      red_ConsoleView = new Color(display, new RGB(255, 192, 192));
      red_ManagerItem = new Color(display, new RGB(255, 68, 68));
      handCursor = new Cursor(display, SWT.CURSOR_HAND);
    }
    instanceCount++;

    //The Main Window
    mainWindow = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE | SWT.MAX | SWT.MIN);
    mainWindow.setText("Azureus"); //$NON-NLS-1$
    
    Listener printer = new Listener() { 
    	public void handleEvent(Event evt) { 
    		System.out.println("-->" + evt.type); 
    	}
    };
    mainWindow.addListener(SWT.Close,printer);
    mainWindow.addListener(SWT.Dispose,printer);
    mainWindow.addListener(SWT.KeyDown,printer);
    mainWindow.addListener(SWT.KeyUp,printer);
    
      
    mainWindow.open();
    mainWindow.forceActive();
    
    mainWindow.addDisposeListener(new DisposeListener() {
    	public void widgetDisposed(DisposeEvent arg0) {
    		System.out.println("NICO disposelistener 002\n");
    		if (mainWindow != null) {
    			System.out.println("NICO disposelistener 002a\n");
    			mainWindow.removeDisposeListener(this);
    			System.out.println("NICO disposelistener 002b\n");
    			dispose();
    			System.out.println("NICO disposelistener 002c\n");
    		}
    		System.out.println("NICO disposelistener 003\n");
    	}      
    });
    

    mainWindow.addShellListener(new ShellAdapter() {
      public void shellClosed(ShellEvent event) {
        if (COConfigurationManager.getBooleanParameter("Close To Tray", true)) { //$NON-NLS-1$
        }
        else {
          dispose();
        }
      }

      public void shellIconified(ShellEvent event) {
        if (COConfigurationManager.getBooleanParameter("Minimize To Tray", false)) { //$NON-NLS-1$
        }
      }
    });

   }catch( Throwable e ){
		e.printStackTrace();
	} }


  public void waitForClose() {
  	while (!mainWindow.isDisposed()) {
  		try {
  			if (!display.readAndDispatch())
  				display.sleep();
  		}
  		catch (Exception e) {
  			System.out.println("NICO001\n");
  			e.printStackTrace();
  			System.out.println("NICO001b\n");
  		}
  	}

 
  	System.out.println("NICO003\n");
  	display.dispose();
  	System.out.println("NICO004\n");
  }
  
  public static void main(String args[]) {
 //   LocaleUtil lu = new LocaleUtilSWT();
 //   LocaleUtil.setLocaleUtilChooser(lu);
 
    		
    testOSX mw = new testOSX( null);
    mw.waitForClose();
  }

	// globalmanagerlistener
	
  public void
  destroyed()
  {
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
     mainWindow.forceActive();
      mainWindow.setMinimized(false);
    }
  }

  public boolean isVisible() {
    return mainWindow.isVisible();
  }

  public boolean dispose() {
     globalManager.stopAll();

    Rectangle windowRectangle = mainWindow.getBounds();
 
   mainWindow.dispose();

 
    	// problem with closing down web start as AWT threads don't close properly

	if ( FileUtil.isJavaWebStart()){    	
 	
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

   /**
	 * @return
	 */
  public static testOSX getWindow() {
    return window;
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
    return false;
  }

  public boolean isSelected(String itemKey) {   
    return false;
  }

  public void itemActivated(String itemKey) {   
   }
  
   


}
