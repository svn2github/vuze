package com.aelitis.azureus.ui.swt.feature;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureManagerListener;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence.LicenceInstallationListener;

import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.views.skin.PlusFTUXView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;

public class FeatureManagerUIListener
	implements FeatureManagerListener
{
	private final static boolean DEBUG = true;

	private final FeatureManager featman;

	private boolean hasPendingAuth;

	public FeatureManagerUIListener(FeatureManager featman) {
		System.out.println("FEAT:");
		this.featman = featman;
	}

	public void licenceAdded(final Licence licence) {
		updateSidebar();

		if (DEBUG) {
			System.out.println("FEAT: Licence Added");
		}

		if (licence.getState() == Licence.LS_PENDING_AUTHENTICATION) {
			hasPendingAuth = true;
			FeatureManagerUI.openLicenceValidatingWindow();
		}

		if (licence.isFullyInstalled()) {
			return;
		}

		licence.addInstallationListener(new LicenceInstallationListener() {

			public void start(String licence_key) {
				if (DEBUG) {
					System.out.println("FEATINST: START!");
				}
				new FeatureManagerInstallWindow(licence).open();
			}

			public void reportProgress(String licenceKey, String install, int percent) {
				if (DEBUG) {
					System.out.println("FEATINST: " + install + ": " + percent);
				}
			}

			public void reportActivity(String licenceKey, String install,
					String activity) {
				if (DEBUG) {
					System.out.println("FEAT: ACTIVITY: " + install + ": " + activity);
				}
			}

			public void failed(String licenceKey, PluginException error) {
				Logger.log(new LogAlert(true, "Error Installing " + licenceKey, error));
			}

			public void complete(String licenceKey) {
				if (hasPendingAuth) {
					hasPendingAuth = false;
					FeatureManagerUI.openLicenceSuccessWindow();
				}
			}

		});
	}

	public void licenceChanged(Licence licence) {
		updateSidebar();
		int state = licence.getState();
		if (DEBUG) {
			System.out.println("FEAT: License State Changed: " + state);
		}
		if (state == Licence.LS_PENDING_AUTHENTICATION) {
			hasPendingAuth = true;
			FeatureManagerUI.openLicenceValidatingWindow();
		} else if (state == Licence.LS_INVAID_KEY) {
			FeatureManagerUI.openLicenceFailedWindow(state);
		} else {
			FeatureManagerUI.closeLicenceValidatingWindow();
			if (state == Licence.LS_AUTHENTICATED) {
				if (hasPendingAuth && licence.isFullyInstalled()) {
					hasPendingAuth = false;
					if (!FeatureManagerUI.hasTrialLicence(licence)) {
						FeatureManagerUI.openLicenceSuccessWindow();
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	private void updateSidebar() {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getEntrySWT(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
			if (entry != null) {
				boolean hasFullLicence = FeatureManagerUI.hasFullLicence();
				String title = MessageText.getString(hasFullLicence
						? "mdi.entry.plus.full" : "mdi.entry.plus.free");
				entry.setTitle(title);
				PlusFTUXView view = (PlusFTUXView) SkinViewManager.getByClass(PlusFTUXView.class);
				if (view != null) {
					view.setHasFullLicence(hasFullLicence);
				}
			}
		}
	}

	public void licenceRemoved(Licence licence) {
		updateSidebar();
	}

}
