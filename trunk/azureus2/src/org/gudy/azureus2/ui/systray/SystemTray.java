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
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;

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
	
	int refreshFactor = 0;
  
  private static final String[] menuItems = {"show", "closealldownloadbars", null, "exit"};

	public SystemTray(MainWindow main) {
		this.main = main;
		URL iconUrl = ClassLoader.getSystemResource("org/gudy/azureus2/ui/icons/azureus.ico"); //$NON-NLS-1$
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
		toolTip.append(MessageText.getString("SystemTray.tooltip.seeding")); //$NON-NLS-1$
		toolTip.append(downloading);
		toolTip.append(MessageText.getString("SystemTray.tooltip.downloading") + "D: "); //$NON-NLS-1$
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
		if (cmd.equals("exit")) { //$NON-NLS-1$
			dispose();
		} else if (cmd.equals("show")) { //$NON-NLS-1$
			show();
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
