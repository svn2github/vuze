/*
 * Created on 25 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core.BDecoder;
import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.ui.systray.SystemTray;

import snoozesoft.systray4j.SysTrayMenu;

/**
 * @author Olivier
 * 
 */
public class MainWindow implements IComponentListener {

  private static final String VERSION = "2.0.0.9"; //$NON-NLS-1$
  private String latestVersion = ""; //$NON-NLS-1$

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
  private SystemTray trayIcon;

  private HashMap downloadViews;
  private HashMap downloadBars;

  private StartServer startServer;

  private class Updater extends Thread {
    boolean finished = false;
    int waitTime = 250;

    public Updater() {
      super("GUI updater"); //$NON-NLS-1$
    }

    public void run() {
      while (!finished) {
        final IView view;
        update();
        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private void update() {
      final IView view;

      try {
        view = Tab.getView(folder.getSelection());
      }
      catch (Exception e) {
        return;
      }

      if (view != null) {
        display.asyncExec(new Runnable() {
          public void run() {
            if (!mainWindow.isDisposed() && mainWindow.isVisible()) {
              view.refresh();
              Tab.refresh();
              statusDown.setText("D: " + globalManager.getDownloadSpeed());
              statusUp.setText("U: " + globalManager.getUploadSpeed());
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
          }
        });
      }
    }
    public void stopIt() {
      finished = true;
    }
  }

  private class VersionChecker extends Thread {
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
      String message = ""; //$NON-NLS-1$
      int nbRead = 0;
      try {
        URL reqUrl = new URL("http://www.gudy.org/azureus/version.php"); //$NON-NLS-1$
        HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
        con.connect();
        InputStream is = con.getInputStream();
        int length = con.getContentLength();
        //System.out.println(length);
        byte[] data = new byte[1024];

        while (nbRead != -1) {
          nbRead = is.read(data);
          if (nbRead != -1)
            message += new String(data, 0, nbRead, "ISO-8859-1"); //$NON-NLS-1$
          Thread.sleep(10);
        }
        Map decoded = BDecoder.decode(message.getBytes("ISO-8859-1")); //$NON-NLS-1$
        latestVersion = new String((byte[]) decoded.get("version"), "ISO-8859-1"); //$NON-NLS-1$ //$NON-NLS-2$
        if (display == null || display.isDisposed())
          return;
        display.asyncExec(new Runnable() {
          public void run() {
            if (statusText.isDisposed())
              return;
            statusText.setText("Azureus " + VERSION + " / " + Messages.getString("MainWindow.status.latestversion") + " : " + latestVersion); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
            statusText.setText("Azureus " + VERSION + " / " + Messages.getString("MainWindow.status.latestversion") + " : " + Messages.getString("MainWindow.status.unknown")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
          }
        });
      }
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
    mainWindow.setText("Azureus"); //$NON-NLS-1$
    mainWindow.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    //The Main Menu
    menuBar = new Menu(mainWindow, SWT.BAR);
    mainWindow.setMenuBar(menuBar);
    //The File Menu
    MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
    fileItem.setText(Messages.getString("MainWindow.menu.file")); //$NON-NLS-1$
    Menu fileMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    fileItem.setMenu(fileMenu);
    MenuItem file_new = new MenuItem(fileMenu, SWT.CASCADE);
    file_new.setText(Messages.getString("MainWindow.menu.file.open")); //$NON-NLS-1$
    new MenuItem(fileMenu, SWT.SEPARATOR);
    MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
    file_exit.setText(Messages.getString("MainWindow.menu.file.exit")); //$NON-NLS-1$

    Menu newMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    file_new.setMenu(newMenu);
    MenuItem file_new_torrent = new MenuItem(newMenu, SWT.NULL);
    file_new_torrent.setText(".torrent " + Messages.getString("file")); //$NON-NLS-1$ //$NON-NLS-2$
    file_new_torrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
        fDialog.setFilterExtensions(new String[] { "*.torrent" }); //$NON-NLS-1$
        fDialog.setFilterNames(new String[] { "*.torrent" }); //$NON-NLS-1$
        fDialog.setText(Messages.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
        String fileName = fDialog.open();
        if (fileName == null)
          return;
        openTorrents(fDialog.getFilterPath(), fDialog.getFileNames());
      }
    });
    // MenuItem file_new_url = new MenuItem(newMenu,SWT.NULL);
    //file_new_url.setText("URL");
    MenuItem file_new_folder = new MenuItem(newMenu, SWT.NULL);
    file_new_folder.setText(Messages.getString("MainWindow.menu.file.folder")); //$NON-NLS-1$
    file_new_folder.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        DirectoryDialog fDialog = new DirectoryDialog(mainWindow, SWT.NULL);
        fDialog.setText(Messages.getString("MainWindow.dialog.choose.folder")); //$NON-NLS-1$
        String fileName = fDialog.open();
        if (fileName == null)
          return;
        openTorrentsFromDirectory(fileName);
      }
    });

    file_exit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        dispose();
      }
    });

    //The View Menu
    MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
    viewItem.setText(Messages.getString("MainWindow.menu.view")); //$NON-NLS-1$
    Menu viewMenu = new Menu(mainWindow, SWT.DROP_DOWN);
    viewItem.setMenu(viewMenu);

    MenuItem view_torrents = new MenuItem(viewMenu, SWT.NULL);
    view_torrents.setText(Messages.getString("MainWindow.menu.view.mytorrents")); //$NON-NLS-1$
    view_torrents.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (mytorrents == null)
          mytorrents = new Tab(new MyTorrentsView(globalManager));
        else
          mytorrents.setFocus();
      }
    });

    MenuItem view_config = new MenuItem(viewMenu, SWT.NULL);
    view_config.setText(Messages.getString("MainWindow.menu.view.configuration")); //$NON-NLS-1$
    view_config.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (config == null)
          config = new Tab(new ConfigView());
        else
          config.setFocus();
      }
    });

    MenuItem view_console = new MenuItem(viewMenu, SWT.NULL);
    view_console.setText(Messages.getString("MainWindow.menu.view.console")); //$NON-NLS-1$
    view_console.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if (console == null)
          console = new Tab(new ConsoleView());
        else
          console.setFocus();
      }
    });

    new MenuItem(viewMenu, SWT.SEPARATOR);

    /*
    	MenuItem view_closeDetails = new MenuItem(viewMenu, SWT.NULL);
    	view_closeDetails.setText("Close all Details");
    	view_closeDetails.addListener(SWT.Selection, new Listener() {
    		public void handleEvent(Event e) {
    			synchronized (downloadViews) {
    //				Tab.closeAll();
    
    //				Iterator iter = downloadViews.values().iterator();
    //				while (iter.hasNext()) {
    //					Tab tab = (Tab) iter.next();
    //					Tab.closed(tab.getTabItem());
    //				}
    
    				Tab[] tab_items =
    					(Tab[]) downloadViews.values().toArray(new Tab[downloadViews.size()]);
    				for (int i = 0; i < tab_items.length; i++) {
    					Tab.closed(tab_items[i].getTabItem());
    				}
    
    				downloadViews.clear();
    			}
    		}
    	});
    */
    addCloseDownloadBarsToMenu(viewMenu);

    createDropTarget(mainWindow);

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
    statusText.setText("Azureus " + VERSION + " / " + Messages.getString("MainWindow.status.latestversion") + " : " + Messages.getString("MainWindow.status.checking") + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    statusText.setLayoutData(gridData);

    Thread versionChecker = new VersionChecker();
    versionChecker.start();

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

    mainWindow.open();
    updater = new Updater();
    updater.start();

    boolean available = false;
    try {
      available = SysTrayMenu.isAvailable();
    }
    catch (NoClassDefFoundError e) {}

    if (available) {
      trayIcon = new SystemTray(this, "azureus.ico"); //$NON-NLS-1$
    }
    else
      tray = new TrayWindow(this);

    mainWindow.addListener(SWT.Close, new Listener() {
      public void handleEvent(Event event) {
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
    });

  }

  protected void addCloseDownloadBarsToMenu(Menu menu) {
    MenuItem view_closeAll = new MenuItem(menu, SWT.NULL);
    view_closeAll.setText(Messages.getString("MainWindow.menu.closealldownloadbars")); //$NON-NLS-1$
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
    DropTarget dropTarget = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY);
    dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
      public void drop(DropTargetEvent event) {
        final String[] sourceNames = (String[]) event.data;
        if (sourceNames == null)
          event.detail = DND.DROP_NONE;
        if (event.detail == DND.DROP_NONE)
          return;
        for (int i = 0;(i < sourceNames.length); i++) {
          final File source = new File(sourceNames[i]);
          if (source.isFile())
            openTorrent(source.getAbsolutePath());
          else if (source.isDirectory())
            openTorrentsFromDirectory(source.getAbsolutePath());
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
    if (tray != null)
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
    if (((DownloadManager) created).getState() == DownloadManager.STATE_STOPPED)
      return;
    if (ConfigurationManager.getInstance().getBooleanParameter("Open Details", true)) //$NON-NLS-1$
      openManagerView((DownloadManager) created);
    if (ConfigurationManager.getInstance().getBooleanParameter("Open Bar", false)) { //$NON-NLS-1$
      synchronized (downloadBars) {
        MinimizedWindow mw = new MinimizedWindow((DownloadManager) created, mainWindow);
        downloadBars.put(created, mw);
      }
    }
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
  }

  public void dispose() {
    if (this.trayIcon != null)
      SysTrayMenu.dispose();

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
        String savePath = getSavePath(_fileName);
        if (savePath == null)
          return;
        globalManager.addDownloadManager(new DownloadManager(globalManager, _fileName, savePath));
      }
    });
  }

  public String getSavePath(String fileName) {
    String savePath = ConfigurationManager.getInstance().getStringParameter("Default save path", ""); //$NON-NLS-1$ //$NON-NLS-2$
    if (savePath.length() == 0) {
      mainWindow.setActive();
      boolean singleFile = false;
      String singleFileName = "";
      try {
        byte[] buf = new byte[1024];
        int nbRead;
        StringBuffer metaInfo = new StringBuffer();
        FileInputStream fis = new FileInputStream(fileName);
        while ((nbRead = fis.read(buf)) > 0)
          metaInfo.append(new String(buf, 0, nbRead, "ISO-8859-1")); //$NON-NLS-1$
        fis.close();
        Map map = (Map) BDecoder.decode(metaInfo.toString().getBytes("ISO-8859-1"));
        Map info = (Map) map.get("info");
        Object test = info.get("length");
        if (test != null) {        
          singleFile = true;
          singleFileName = new String((byte[]) info.get("name"), "ISO-8859-1");
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      if (singleFile) {
        FileDialog fDialog = new FileDialog(mainWindow, SWT.SYSTEM_MODAL);
        fDialog.setFilterPath(ConfigurationManager.getInstance().getStringParameter("Default Path", ""));
        fDialog.setFileName(singleFileName);
        fDialog.setText(Messages.getString("MainWindow.dialog.choose.savepath"));
        savePath = fDialog.open();

      }
      else {
        DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.SYSTEM_MODAL);
        dDialog.setFilterPath(ConfigurationManager.getInstance().getStringParameter("Default Path", "")); //$NON-NLS-1$ //$NON-NLS-2$
        dDialog.setText(Messages.getString("MainWindow.dialog.choose.savepath")); //$NON-NLS-1$
        savePath = dDialog.open();
      }
      if (savePath == null)
        return null;
      ConfigurationManager.getInstance().setParameter("Default Path", savePath); //$NON-NLS-1$
      ConfigurationManager.getInstance().save();
    }
    return savePath;
  }

  public void openTorrents(final String path, final String fileNames[]) {
    display.asyncExec(new Runnable() {
      public void run() {

        String separator = System.getProperty("file.separator"); //$NON-NLS-1$
        for (int i = 0; i < fileNames.length; i++) {
          String savePath = getSavePath(path + separator + fileNames[i]);
          if (savePath == null)
            continue;
          globalManager.addDownloadManager(
            new DownloadManager(globalManager, path + separator + fileNames[i], savePath));
        }
      }
    });
  }

  public void openTorrentsFromDirectory(String directoryName) {
    File f = new File(directoryName);
    if (!f.isDirectory())
      return;
    File[] files = f.listFiles(new FileFilter() {
      public boolean accept(File arg0) {
        if (arg0.getName().endsWith(".torrent")) //$NON-NLS-1$
          return true;
        return false;
      }
    });
    if (files.length == 0)
      return;
    DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    dDialog.setText(Messages.getString("MainWindow.dialog.choose.savepath_forallfiles")); //$NON-NLS-1$
    String savePath = dDialog.open();
    if (savePath == null)
      return;
    for (int i = 0; i < files.length; i++)
      globalManager.addDownloadManager(new DownloadManager(globalManager, files[i].getAbsolutePath(), savePath));
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
