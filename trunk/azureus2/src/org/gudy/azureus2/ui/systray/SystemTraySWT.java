/*
 * File    : SystemTraySWT.java
 * Created : 2 avr. 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.systray;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.PasswordWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

import java.util.List;

/**
 * @author Olivier Chalouhi
 *
 */
public class SystemTraySWT {
  
  MainWindow mainWindow;
  Display display;
  
  Tray tray;
  TrayItem trayItem;
  
  Menu menu;
  
  public SystemTraySWT(MainWindow mainWindow) {
    this.mainWindow = mainWindow;
    this.display = mainWindow.getDisplay();
    
    tray = display.getSystemTray();
    trayItem = new TrayItem(tray,SWT.NULL);
    
    if( ! Constants.isOSX) {
      trayItem.setImage(ImageRepository.getImage("azureus"));  
    }    
    trayItem.setVisible(true);
    
    menu = new Menu(mainWindow.getShell(),SWT.POP_UP);
    
    final MenuItem itemShow = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemShow,"SystemTray.menu.show");
    
    
    new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemCloseAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemCloseAll,"SystemTray.menu.closealldownloadbars");
    
    new MenuItem(menu,SWT.SEPARATOR);

    createUploadLimitMenu(menu);
    createDownloadLimitMenu(menu);

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemStartAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStartAll,"SystemTray.menu.startalltransfers");
    
    final MenuItem itemStopAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStopAll,"SystemTray.menu.stopalltransfers");
    
    final MenuItem itemPause = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemPause,"SystemTray.menu.pausetransfers");
    
    final MenuItem itemResume = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemResume,"SystemTray.menu.resumetransfers");

    new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemExit = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemExit,"SystemTray.menu.exit");
    
    itemShow.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        show();
      }
    });
    
    itemStartAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.getGlobalManager().startAllDownloads();
      }
    });
    
    itemStopAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.getGlobalManager().stopAllDownloads();
      }
    });
    
    
    itemPause.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	SystemTraySWT.this.mainWindow.getGlobalManager().pauseDownloads();
        }
      });
      
    itemResume.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
          SystemTraySWT.this.mainWindow.getGlobalManager().resumeDownloads();
        }
      });
      
    
    menu.addMenuListener(
          	new MenuListener()
    		{
          		public void
    			menuShown(
    				MenuEvent	_menu )
          		{
          		  itemPause.setEnabled( SystemTraySWT.this.mainWindow.getGlobalManager().canPauseDownloads() );
          			
          			itemResume.setEnabled( SystemTraySWT.this.mainWindow.getGlobalManager().canResumeDownloads() );
          		}
          		
        		public void
    			menuHidden(
    				MenuEvent	_menu )
          		{
          			
          		}
    		});
    
    itemCloseAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.closeDownloadBars();
      }
    });
    
    itemExit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.dispose(false,false);
      }
    });
    
    
    
    trayItem.addListener(SWT.DefaultSelection,new Listener() {
      public void handleEvent(Event arg0) {
        show();
      }
    });
    
    trayItem.addListener(SWT.MenuDetect,new Listener() {
      public void handleEvent(Event arg0) {
        menu.setVisible(true);
      }
    });               
  }

    /**
     * Creates the global upload limit context menu item
     * @param parent The system tray contextual menu
     */
    private final void createUploadLimitMenu(final Menu parent)
    {
        final MenuItem uploadSpeedItem = new MenuItem(parent, SWT.CASCADE);
        uploadSpeedItem.setText(MessageText.getString("GeneralView.label.maxuploadspeed"));

        final Menu uploadSpeedMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
        createLimitMenuItems("Max Upload Speed KBs", 1024*1024, uploadSpeedMenu); // 1MiB - see MyTorrentsView
        uploadSpeedItem.setMenu(uploadSpeedMenu);
    }

    /**
     * Creates the global download limit context menu item
     * @param parent The system tray contextual menu
     */
    private final void createDownloadLimitMenu(final Menu parent)
    {
        final MenuItem downloadSpeedItem = new MenuItem(parent, SWT.CASCADE);
        downloadSpeedItem.setText(MessageText.getString("GeneralView.label.maxdownloadspeed"));

        final Menu downloadSpeedMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
        createLimitMenuItems("Max Download Speed KBs", 1024*1024, downloadSpeedMenu); // 1MiB starting point
        downloadSpeedItem.setMenu(downloadSpeedMenu);
    }

    /**
     * Creates the submenu items for bandwidth limit
     * @param configKey Configuration key to get initial max bandwidth
     * @param maxInitialValue
     * @param parent Parent menu to populate the items in
     */
    private final void createLimitMenuItems(final String configKey, int maxInitialValue, final Menu parent) {
        final int speedPartitions = 10;
        final MenuItem[] items = new MenuItem[speedPartitions + 1];

        int maxBandwidth = COConfigurationManager.getIntParameter(configKey,0) * 1024;

        items[0] = new MenuItem(parent, SWT.CHECK);
        items[0].setText(MessageText.getString("MyTorrentsView.menu.setSpeed.unlimited"));
        items[0].setData("maxkb", new Integer(0));
        items[0].setSelection(maxBandwidth == 0);
        items[0].addListener(SWT.Selection, getLimitMenuItemListener(items, configKey));

        // set default for calculations
        if(maxBandwidth == 0) maxBandwidth = maxInitialValue;
        for (int i = 2; i < speedPartitions + 2; i++) {
            final int bandwidth = maxBandwidth / speedPartitions * (speedPartitions + 2 - i);
            items[i - 1] = new MenuItem(parent, SWT.CHECK);
            items[i - 1].setData("maxkb", new Integer(bandwidth));
            items[i - 1].setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(bandwidth));
            items[i - 1].setSelection(bandwidth == maxBandwidth);
            items[i - 1].addListener(SWT.Selection, getLimitMenuItemListener(items, configKey));
        }
    }

    /**
     * Gets the selection listener of a upload or download limit menu item (including unlimited)
     * @param items The array of limit menu items
     * @param configKey The configuration key
     * @return The selection listener
     */
   private final Listener getLimitMenuItemListener(final MenuItem[] items, final String configKey)
   {
       return new Listener() {
           public void handleEvent(Event event) {
               for(int i = 0; i < items.length; i++) {
                    if(items[i] == event.widget)
                    {
                        items[i].setSelection(true);
                        COConfigurationManager.setParameter(configKey, ((Integer)items[i].getData("maxkb")).intValue() / 1024);
                    }
                    else {
                        items[i].setSelection(false);
                    }
                }

               // do not recompute speed limit partitions to avoid items jumping around the place from the user's POV
           }
       };
   }

    public void dispose() {
    if(trayItem != null && !trayItem.isDisposed()) {
      trayItem.dispose();
    }
  }
  
  public void update() {    
    if(trayItem.isDisposed()) 
      return;
    List managers = mainWindow.getGlobalManager().getDownloadManagers();
    //StringBuffer toolTip = new StringBuffer("Azureus - ");//$NON-NLS-1$
    StringBuffer toolTip = new StringBuffer();
    int seeding = 0;
    int downloading = 0;
    
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
        int state = manager.getState();
        if (state == DownloadManager.STATE_DOWNLOADING)
          downloading++;
        if (state == DownloadManager.STATE_SEEDING)
          seeding++;
      }
 
    
    toolTip.append(seeding);
    toolTip.append(MessageText.getString("SystemTray.tooltip.seeding"));
    toolTip.append(downloading);
    toolTip.append(MessageText.getString("SystemTray.tooltip.downloading") + MessageText.getString("ConfigView.download.abbreviated") + " "); 
    toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.getGlobalManager().getStats().getDownloadAverage()));
    toolTip.append(", " + MessageText.getString("ConfigView.upload.abbreviated") + " ");
    toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.getGlobalManager().getStats().getUploadAverage()));
    
    
    trayItem.setToolTipText(toolTip.toString());      
    trayItem.setImage(ImageRepository.getImage("azureus"));   
    trayItem.setVisible(true);    
  }
  
  private void show() {
    if (!COConfigurationManager.getBooleanParameter("Password enabled",false))          
      SystemTraySWT.this.mainWindow.setVisible(true);
    else
      PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
  }
  
  public Menu getMenu() {
    return menu;
  }
}
