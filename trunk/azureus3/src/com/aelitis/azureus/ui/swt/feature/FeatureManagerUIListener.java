package com.aelitis.azureus.ui.swt.feature;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureManagerListener;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence.LicenceInstallationListener;

public class FeatureManagerUIListener
	implements FeatureManagerListener
{

	private final FeatureManager featman;
	private boolean hasPendingAuth;

	public FeatureManagerUIListener(FeatureManager featman) {
		System.out.println("FEAT:");
		this.featman = featman;
	}

	public void licenceAdded(final Licence licence) {
		System.out.println("FEAT: Licence Added");

		if (licence.getState() == Licence.LS_PENDING_AUTHENTICATION) {
			FeatureManagerUI.openLicenceValidatingWindow();
		}

		if (licence.isFullyInstalled()) {
			return;
		}
		licence.addInstallationListener(new LicenceInstallationListener() {

			public void reportProgress(String licenceKey, String install, int percent) {
			}

			public void reportActivity(String licenceKey, String install,
					String activity) {
				System.out.println("FEAT: ACTIVITY: " + install + ": " + activity);
			}

			public void failed(String licenceKey, PluginException error) {
			}

			public void complete(String licenceKey) {
			}
		});
	}

	public void licenceChanged(Licence licence) {
		int state = licence.getState();
		System.out.println("FEAT: License State Changed: " + state);
		if (state == Licence.LS_PENDING_AUTHENTICATION) {
			hasPendingAuth = true;
			FeatureManagerUI.openLicenceValidatingWindow();
		} else if (state == Licence.LS_INVAID_KEY) {
			FeatureManagerUI.openLicenceFailedWindow(state);
		} else {
			FeatureManagerUI.closeLicenceValidatingWindow();
			if (state == Licence.LS_AUTHENTICATED) {
				if (hasPendingAuth) {
					hasPendingAuth = false;
					FeatureManagerUI.openLicenceSuccessWindow();
				}
			}
		}
	}

	public void licenceRemoved(Licence licence) {
	}

}
