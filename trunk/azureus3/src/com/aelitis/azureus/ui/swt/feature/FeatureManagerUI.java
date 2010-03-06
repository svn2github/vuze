package com.aelitis.azureus.ui.swt.feature;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;

public class FeatureManagerUI
{
	protected static final int DLG_HEIGHT = 320;

	public static boolean enabled = !Constants.isUnix
			&& System.getProperty("fm.ui", "0").equals("1");

	private static FeatureManager featman;

	private static VuzeMessageBox validatingBox;

	public static void registerWithFeatureManager() {
		if (!enabled) {
			return;
		}
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {

			public void azureusCoreRunning(AzureusCore core) {
				PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();
				featman = pi.getUtilities().getFeatureManager();

				featman.addListener(new FeatureManagerUIListener(featman));
			}
		});
	}

	public static void openLicenceEntryWindow(final boolean trytwo) {
		if (!enabled) {
			return;
		}
		String tryNo = (trytwo ? "2" : "1");
		final SWTSkinObjectTextbox[] key = new SWTSkinObjectTextbox[1];
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.title"),
				MessageText.getString("dlg.auth.enter.line.try." + tryNo),
				new String[] {
					MessageText.getString("Button.validate"),
					MessageText.getString("Button.cancel")
				}, 0);

		box.setSubTitle(MessageText.getString("dlg.auth.enter.subtitle.try."
				+ tryNo));
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.vp");
		if (trytwo) {
			box.setTextIconResource("image.warn.big");
		}

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				shell.setSize(shell.getSize().x, DLG_HEIGHT);

				SWTSkin skin = soExtra.getSkin();
				skin.setAutoSizeOnLayout(false);
				skin.createSkinObject("dlg.register", "dlg.register", soExtra);
				
				if (trytwo) {
					SWTSkinObjectText link = (SWTSkinObjectText) skin.getSkinObject("register-link", soExtra);
					link.setText(MessageText.getString("dlg.auth.enter.link"));
					link.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
						public boolean urlClicked(URLInfo urlInfo) {
							Utils.launch("http://google.com");
							return true;
						}
					});
				}

				key[0] = (SWTSkinObjectTextbox) skin.getSkinObject("key", soExtra);
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					try {
						Licence licence = featman.addLicence(key[0].getText());
						int initialState = licence.getState();
						if (initialState == Licence.LS_AUTHENTICATED) {
							openLicenceSuccessWindow();
						} else if (initialState == Licence.LS_CANCELLED
								|| initialState == Licence.LS_INVAID_KEY
								|| initialState == Licence.LS_REVOKED) {
							openLicenceFailedWindow(initialState);
						}
					} catch (PluginException e) {
						Logger.log(new LogAlert(true, LogAlert.AT_ERROR, "Adding Licence",
								e));
					}
				}
			}
		});
	}

	public static void openLicenceSuccessWindow() {
		if (!enabled) {
			return;
		}

		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.title"),
				MessageText.getString("dlg.auth.success.line1"), new String[] {
					MessageText.getString("Button.getstarted"),
				}, 0);
		box.setSubTitle(MessageText.getString("dlg.auth.success.subtitle"));
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.vp");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				shell.setSize(shell.getSize().x, DLG_HEIGHT);

				SWTSkin skin = soExtra.getSkin();
				skin.setAutoSizeOnLayout(false);
				skin.createSkinObject("dlg.register.success", "dlg.register.success",
						soExtra);
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					// TODO
				}
			}
		});
	}

	protected static void openLicenceFailedWindow(int licenceState) {
		openLicenceEntryWindow(true);
	}

	public static void openLicenceValidatingWindow() {
		if (!enabled || validatingBox != null) {
			return;
		}

		validatingBox = new VuzeMessageBox(
				MessageText.getString("dlg.auth.validating.subtitle"), null, null, 0);
		validatingBox.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		validatingBox.setIconResource("image.vp");

		validatingBox.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.register.validating",
						"dlg.register.validating", soExtra);
				shell.setSize(shell.getSize().x, DLG_HEIGHT);
				skin.setAutoSizeOnLayout(false);
			}
		});

		validatingBox.open(null);
	}

	public static void closeLicenceValidatingWindow() {
		if (validatingBox != null) {
			validatingBox.close(0);
			validatingBox = null;
		}
	}
}
