/*
 * Created on 15 juil. 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.systray;

import java.net.URL;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.PasswordWindow;

import snoozesoft.systray4j.SysTrayMenu;
import snoozesoft.systray4j.SysTrayMenuAdapter;
import snoozesoft.systray4j.SysTrayMenuEvent;
import snoozesoft.systray4j.SysTrayMenuIcon;
import snoozesoft.systray4j.SysTrayMenuItem;

/**
 * @author oc
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SystemTray extends SysTrayMenuAdapter {

	MainWindow main;
	SysTrayMenu menu;
	private static SystemTray systemTray = null; 
	int refreshFactor = 0;
  
  private static final String[] menuItems = {"show", null, "closealldownloadbars", null, "startalldownloads", "stopalldownloads", null, "exit"};

	public SystemTray(MainWindow main) {
		this.main = main;
		URL iconUrl = SystemTray.class.getClassLoader().getResource("org/gudy/azureus2/ui/icons/azureus.ico"); //$NON-NLS-1$
		SysTrayMenuIcon icon = new SysTrayMenuIcon(iconUrl);		
		menu = new SysTrayMenu(icon);
    SysTrayMenuItem item;
    for (int i = menuItems.length-1; i >= 0; i--) {
      if (menuItems[i] != null) {
        item = new SysTrayMenuItem(MessageText.getString("SystemTray.menu." + menuItems[i]), menuItems[i]); //$NON-NLS-1$ //$NON-NLS-2$
        item.addSysTrayMenuListener(this);
        menu.addItem(item);
      } else
        menu.addSeparator();
    }
		icon.addSysTrayMenuListener(this);
    systemTray = this;
	}

  public void updateLanguage() {
    for (int i = menu.getItemCount()-1; i >= 0; i--) {
      if(menu.getItemAt(i) != null)
        menu.getItemAt(i).setLabel(MessageText.getString("SystemTray.menu." + menu.getItemAt(i).getActionCommand()));
    }
  }

	public void refresh() {
		refreshFactor++;
		if((refreshFactor%20) != 0)
			return;
		List managers = main.getGlobalManager().getDownloadManagers();
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
		toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(main.getGlobalManager().getStats().getDownloadAverage()));
		toolTip.append(", " + MessageText.getString("ConfigView.upload.abbreviated") + " ");
		toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(main.getGlobalManager().getStats().getUploadAverage()));

		menu.setToolTip(toolTip.toString());
//    if(!menu.isIconVisible())
    menu.showIcon();
	}
	/* (non-Javadoc)
	 * @see snoozesoft.systray4j.SysTrayMenuListener#iconLeftDoubleClicked(snoozesoft.systray4j.SysTrayMenuEvent)
	 */
	public void iconLeftDoubleClicked(SysTrayMenuEvent event) {
		show();
	}

	/* (non-Javadoc)
	 * @see snoozesoft.systray4j.SysTrayMenuAdapter#menuItemSelected(snoozesoft.systray4j.SysTrayMenuEvent)
	 */
	public void menuItemSelected(SysTrayMenuEvent event) {
		String cmd = event.getActionCommand();
		if (cmd.equals("exit")) { //$NON-NLS-1$
			dispose();
		} else if (cmd.equals("show")) { //$NON-NLS-1$
			show();
    } else if (cmd.equals("startalldownloads")) { //$NON-NLS-1$
      main.getGlobalManager().startAllDownloads();
    } else if (cmd.equals("stopalldownloads")) { //$NON-NLS-1$
      main.getGlobalManager().stopAllDownloads();
    } else if (cmd.equals("closealldownloadbars")) { //$NON-NLS-1$
      main.closeDownloadBars();
    }
	}

	private void show() {
		Display display = main.getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
        if (!COConfigurationManager.getBooleanParameter("Password enabled",false))          
				  main.setVisible(true);
        else
          PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
			}
		});
	}

	private void dispose() {
    menu.getIcon().removeSysTrayMenuListener(this);
    menu.hideIcon();
		Display display = main.getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				if(!main.dispose()) {
          menu.getIcon().addSysTrayMenuListener(systemTray);
          menu.showIcon();
        }
			}
		});
	}

  public void showIcon() {
    menu.showIcon();
  }
}
