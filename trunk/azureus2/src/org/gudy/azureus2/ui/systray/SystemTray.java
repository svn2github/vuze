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
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.ui.swt.MainWindow;

import snoozesoft.systray4j.SysTrayMenu;
import snoozesoft.systray4j.SysTrayMenuAdapter;
import snoozesoft.systray4j.SysTrayMenuEvent;
import snoozesoft.systray4j.SysTrayMenuIcon;
import snoozesoft.systray4j.SysTrayMenuItem;
import snoozesoft.systray4j.SysTrayMenuListener;

/**
 * @author oc
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SystemTray extends SysTrayMenuAdapter {

	MainWindow main;
	String fileName;
	SysTrayMenu menu;
	int file;
	
	int refreshFactor = 0;

	public SystemTray(MainWindow main, String fileName) {
		this.main = main;
		this.fileName = fileName;
		URL iconUrl = ClassLoader.getSystemResource("org/gudy/azureus2/ui/icons/azureus.ico");
		SysTrayMenuIcon icon = new SysTrayMenuIcon(iconUrl);		
		menu = new SysTrayMenu(icon);		
		SysTrayMenuItem item = new SysTrayMenuItem("Exit", "exit");
		item.addSysTrayMenuListener(this);
		menu.addItem(item);
		menu.addSeparator();
    item = new SysTrayMenuItem("Close All Download Bars", "close_all_download_bars");
    item.addSysTrayMenuListener(this);
//    menu.addItem(item); // org.eclipse.swt.SWTException: Invalid thread access
		item = new SysTrayMenuItem("Show Azureus", "show");
		item.addSysTrayMenuListener(this);
		menu.addItem(item);
		icon.addSysTrayMenuListener(this);
	}

	public void refresh() {
		refreshFactor++;
		if((refreshFactor%20) != 0)
			return;
		List managers = main.getGlobalManager().getDownloadManagers();
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
		toolTip.append(" seeding, ");
		toolTip.append(downloading);
		toolTip.append(" downloading, D: ");
		toolTip.append(main.getGlobalManager().getDownloadSpeed());
		toolTip.append(", U: ");
		toolTip.append(main.getGlobalManager().getUploadSpeed());

		menu.setToolTip(toolTip.toString());
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
		if (cmd.equals("exit")) {
			dispose();
		} else if (cmd.equals("show")) {
			show();
    } else if (cmd.equals("close_all_download_bars")) {
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
				main.setVisible(true);
			}
		});
	}

	private void dispose() {
		Display display = main.getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				main.dispose();
			}
		});
	}

}
