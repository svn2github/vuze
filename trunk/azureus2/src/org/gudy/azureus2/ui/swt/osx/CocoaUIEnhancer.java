package org.gudy.azureus2.ui.swt.osx;

import org.gudy.azureus2.ui.swt.UIExitUtilsSWT;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

/**
 * You can exclude this file (or this whole path) for non OSX builds, otherwise
 * you need to add AppleJavaExtensions.jar to your build path
 * 
 * Hook some Cocoa specific abilities:
 * - App->About        <BR>
 * - App->Preferences  <BR>
 * - App->Restart      <BR>
 * - App->Exit         <BR>
 * <BR>
 * - OpenDocument  (possible limited to only files?) <BR>
 * - OpenApplication (click on doc)<BR>
 * <BR>
 * Compared to CarbonUIEnhancer, we are missing:
 * - New App menu items (NAT Test, Speed Test, etc)
 * - ToggleBar Button
 *
 */
public class CocoaUIEnhancer
{
	private Application fApplication = Application.getApplication();

	public CocoaUIEnhancer() {
		initAppMenu();
	}

	private void initAppMenu() {
		fApplication.addAboutMenuItem();
		fApplication.setEnabledAboutMenu(true);
		fApplication.setEnabledPreferencesMenu(true);
		fApplication.addApplicationListener(new ApplicationListener() {
			
			public void handleReOpenApplication(ApplicationEvent arg0) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.dispose(true, false);
				}
			}
			
			public void handleQuit(ApplicationEvent event) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.dispose(false, false);
				} else {
					UIExitUtilsSWT.setSkipCloseCheck(true);
				}
				event.setHandled(true);
			}
			
			public void handlePrintFile(ApplicationEvent arg0) {
			}
			
			public void handlePreferences(ApplicationEvent event) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.openView(UIFunctions.VIEW_CONFIG, null);
				}
				event.setHandled(true);
			}
			
			public void handleOpenFile(ApplicationEvent event) {
				String fileNames[] = new String[] { event.getFilename() };
				TorrentOpener.openTorrents(fileNames);
				event.setHandled(true);
			}
			
			public void handleOpenApplication(ApplicationEvent event) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.bringToFront();
				}
				event.setHandled(true);
			}
			
			public void handleAbout(ApplicationEvent event) {
				AboutWindow.show();
				event.setHandled(true);
			}
		});
	}

	
}
