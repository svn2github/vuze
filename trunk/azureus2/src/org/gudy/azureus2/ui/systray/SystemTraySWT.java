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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.PasswordWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

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
    
    trayItem.setImage(ImageRepository.getImage("azureus"));
    trayItem.setVisible(true);
    
    menu = new Menu(mainWindow.getShell(),SWT.POP_UP);
    
    final MenuItem itemShow = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemShow,"SystemTray.menu.show");
    
    
    MenuItem itemSeparator = new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemCloseAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemCloseAll,"SystemTray.menu.closealldownloadbars");
    
    itemSeparator = new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemStartAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStartAll,"SystemTray.menu.startalldownloads");
    
    final MenuItem itemStopAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStopAll,"SystemTray.menu.stopalldownloads");
    
    final MenuItem itemPause = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemPause,"SystemTray.menu.pausedownloads");
    
    final MenuItem itemResume = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemResume,"SystemTray.menu.resumedownloads");
    
    itemSeparator = new MenuItem(menu,SWT.SEPARATOR);
    
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
    
    final Object[] currentPauseData = { null };
    
    itemPause.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	currentPauseData[0] = SystemTraySWT.this.mainWindow.getGlobalManager().pauseDownloads();
        }
      });
      
    itemResume.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
          SystemTraySWT.this.mainWindow.getGlobalManager().resumeDownloads(currentPauseData[0]);
        }
      });
      
    
    menu.addMenuListener(
          	new MenuListener()
    		{
          		public void
    			menuShown(
    				MenuEvent	_menu )
          		{
          			boolean	can_resume = 	currentPauseData[0] != null &&
          			SystemTraySWT.this.mainWindow.getGlobalManager().canResumeDownloads(currentPauseData[0]);
          			
          			itemResume.setEnabled(can_resume);
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
        SystemTraySWT.this.mainWindow.dispose();
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
    synchronized (managers) {
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
        int state = manager.getState();
        if (state == DownloadManager.STATE_DOWNLOADING)
          downloading++;
        if (state == DownloadManager.STATE_SEEDING)
          seeding++;
      }
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
