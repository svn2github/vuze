/*
 * Created on 25 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.GlobalManager;

/**
 * @author Olivier
 * 
 */
public class MainWindow implements IComponentListener {

  private static final String VERSION = "2.0.0.2";

  private static MainWindow window;

  private Updater updater;

  private Display display;
  private Shell mainWindow;
  private Menu menuBar;
  public static Color blue;
  public static Color white;
  private CTabFolder folder;
  private CLabel statusText;
  private CLabel statusDown;
  private CLabel statusUp;

  private GlobalManager globalManager;

  private Tab mytorrents;
  private Tab console;
  private Tab config;

  private TrayWindow tray;

  private HashMap downloadViews;
  private HashMap downloadBars;

  private StartServer startServer;

  private class Updater extends Thread {
    boolean finished = false;
    int waitTime = 250;

    public Updater() {
      super("GUI updater");
    }

    public void run() {
      while (!finished) {
        final IView view = Tab.getView(folder.getSelection());

        if (view != null) {
          display.asyncExec(new Runnable() {
            public void run() {
              if (!mainWindow.isDisposed() && mainWindow.isVisible()) {            
                view.refresh();
                Tab.refresh();
                statusDown.setText("D: " + globalManager.getDownloadSpeed());
                statusUp.setText("U: " + globalManager.getUploadSpeed());
              }
              tray.refresh();
              synchronized (downloadBars) {
                Iterator iter = downloadBars.values().iterator();
                while (iter.hasNext()) {
                  MinimizedWindow mw = (MinimizedWindow) iter.next();
                  mw.refresh();
                }
              }
            }
          });
        }

        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      finished = true;
    }
  }

  public MainWindow(GlobalManager gm, StartServer server) {
    if (window != null)
      return;
    window = this;
    this.startServer = server;
    this.globalManager = gm;
    mytorrents = null;
    console = null;
    config = null;
    downloadViews = new HashMap();
    downloadBars = new HashMap();
    //The display
    display = new Display();
    ImageRepository.loadImages(display);
    
    blue = new Color(display, new RGB(128, 128, 255));
    white = new Color(display, new RGB(255, 255, 255));
    //The Main Window    
    mainWindow = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE);
    mainWindow.setText("Azureus");
    mainWindow.setImage(ImageRepository.getImage("azureus"));
    //The Main Menu
    menuBar = new Menu(mainWindow, SWT.BAR);
    mainWindow.setMenuBar(menuBar);
    //The File Menu
    MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
    fileItem.setText("File");
    Menu fileMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    fileItem.setMenu(fileMenu);
    MenuItem file_new = new MenuItem(fileMenu, SWT.CASCADE);
    file_new.setText("Open");
    new MenuItem(fileMenu, SWT.SEPARATOR);
    MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
    file_exit.setText("Exit");

    Menu newMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    file_new.setMenu(newMenu);
    MenuItem file_new_torrent = new MenuItem(newMenu, SWT.NULL);
    file_new_torrent.setText(".torrent File");
    file_new_torrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN);
        fDialog.setFilterExtensions(new String[] { "*.torrent" });
        fDialog.setFilterNames(new String[] { "*.torrent" });
        fDialog.setText("Choose the torrent file");
        String fileName = fDialog.open();
        if (fileName == null)
          return;
        openTorrent(fileName);
      }
    });
    // MenuItem file_new_url = new MenuItem(newMenu,SWT.NULL);
    //file_new_url.setText("URL");
    MenuItem file_new_folder = new MenuItem(newMenu, SWT.NULL);
    file_new_folder.setText("Folder");
    file_new_folder.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        DirectoryDialog fDialog = new DirectoryDialog(mainWindow, SWT.NULL);
        fDialog.setText("Choose the directory containing the torrent files");
        String fileName = fDialog.open();
        if (fileName == null)
          return;
        File f = new File(fileName);
        if (!f.isDirectory())
          return;
        File[] files = f.listFiles(new FileFilter() {
          public boolean accept(File arg0) {
            if (arg0.getName().endsWith(".torrent"))
              return true;
            return false;
          }
        });
        if (files.length == 0)
          return;
        DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.NULL);
        dDialog.setText("Choose the save path for ALL the files");
        String savePath = dDialog.open();
        if (savePath == null)
          return;
        for (int i = 0; i < files.length; i++)
          globalManager.addDownloadManager(new DownloadManager(globalManager, files[i].getAbsolutePath(), savePath));
      }
    });

    file_exit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        dispose();
      }
    });

    //The View Menu
    MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
    viewItem.setText("Views");
    Menu viewMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    viewItem.setMenu(viewMenu);

    MenuItem view_torrents = new MenuItem(viewMenu, SWT.NULL);
    view_torrents.setText("My Torrents");
    view_torrents.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (mytorrents == null)
          mytorrents = new Tab(new MyTorrentsView(globalManager));
        else
          mytorrents.setFocus();
      }
    });

    MenuItem view_config = new MenuItem(viewMenu, SWT.NULL);
    view_config.setText("Configuration");
    view_config.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if(config == null)
          config = new Tab(new ConfigView());
        else
          config.setFocus();
      }
    });

    MenuItem view_console = new MenuItem(viewMenu, SWT.NULL);
    view_console.setText("Console");
    view_console.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (console == null)
          console = new Tab(new ConsoleView());
        else
          console.setFocus();
      }
    });

    GridLayout mainLayout = new GridLayout();
    mainLayout.numColumns = 1;
    mainLayout.marginHeight = 0;
    mainLayout.marginWidth = 0;
    mainWindow.setLayout(mainLayout);
    //mainWindow.setBackground(white);

    GridData gridData;

    gridData = new GridData(GridData.FILL_BOTH);
    folder = new CTabFolder(mainWindow, SWT.NULL);
    Tab.setFolder(folder);

    folder.setSelectionBackground(new Color[] { white }, new int[0]);
    folder.setLayoutData(gridData);
    folder.addCTabFolderListener(new CTabFolderAdapter() {
      public void itemClosed(CTabFolderEvent event) {
        Tab.closed((CTabItem) event.item);
        event.doit = true;
      }
    });

    mytorrents = new Tab(new MyTorrentsView(globalManager));

    gridData = new GridData(GridData.FILL_HORIZONTAL);

    Composite statusBar = new Composite(mainWindow, SWT.SHADOW_IN);
    statusBar.setLayoutData(gridData);
    GridLayout layout_status = new GridLayout();
    layout_status.numColumns = 3;
    layout_status.horizontalSpacing = 1;
    layout_status.verticalSpacing = 0;
    layout_status.marginHeight = 0;
    layout_status.marginWidth = 0;
    statusBar.setLayout(layout_status);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    statusText = new CLabel(statusBar, SWT.SHADOW_IN);
    statusText.setText("Azureus " + VERSION);
    statusText.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 90;
    statusDown = new CLabel(statusBar, SWT.SHADOW_IN);
    statusDown.setText("D:");
    statusDown.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 90;
    statusUp = new CLabel(statusBar, SWT.SHADOW_IN);
    statusUp.setText("U:");
    statusUp.setLayoutData(gridData);

    globalManager.addListener(this);

    /*for(int i = 0 ; i < args.length / 2 ; i++)
      globalManager.addDownloadManager(new DownloadManager(args[2*i],args[2*i+1]));
    */

    mainWindow.open();
    updater = new Updater();
    updater.start();

    tray = new TrayWindow(this);

    mainWindow.addListener(SWT.Close, new Listener() {
      public void handleEvent(Event event) {
        event.doit = false;
        mainWindow.setVisible(false);
        tray.setVisible(true);
        synchronized (downloadBars) {
                        Iterator iter = downloadBars.values().iterator();
                        while (iter.hasNext()) {
                          MinimizedWindow mw = (MinimizedWindow) iter.next();
                          mw.setVisible(true);
                        }
                      }
      }
    });
  }

  public void waitForClose() {
    while (!mainWindow.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }

    white.dispose();
    blue.dispose();
    tray.dispose();
    display.dispose();
  }

  public static void main(String args[]) {
    GlobalManager gm = new GlobalManager();
    MainWindow mw = new MainWindow(gm, null);
    mw.waitForClose();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void objectAdded(Object created) {
    if (!(created instanceof DownloadManager))
      return;
    openManagerView((DownloadManager) created);
  }

  public void openManagerView(DownloadManager downloadManager) {
    synchronized (downloadViews) {
      if (downloadViews.containsKey(downloadManager)) {
        Tab tab = (Tab) downloadViews.get(downloadManager);
        tab.setFocus();
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

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void objectRemoved(Object removed) {
    if (!(removed instanceof DownloadManager))
      return;
    synchronized (downloadViews) {
      if (downloadViews.containsKey(removed)) {
        Tab tab = (Tab) downloadViews.get(removed);
        tab.dispose();
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
  }

  public void dispose() {
    if (startServer != null)
      startServer.stopIt();
    updater.stopIt();
    globalManager.stopAll();
    mainWindow.dispose();
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

  /**
   * @return
   */
  public static MainWindow getWindow() {
    return window;
  }

  public void openTorrent(String fileName) {
    final String _fileName = fileName;
    display.asyncExec(new Runnable() {
      public void run() {
        mainWindow.setFocus();
        DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.NULL);
        dDialog.setFilterPath(ConfigurationManager.getInstance().getStringParameter("Default Path",""));
        dDialog.setText("Choose the save path");
        String savePath = dDialog.open();
        if (savePath == null)
          return;
        ConfigurationManager.getInstance().setParameter("Default Path",savePath);
        ConfigurationManager.getInstance().save();        
        globalManager.addDownloadManager(new DownloadManager(globalManager, _fileName, savePath));
      }
    });
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

}
