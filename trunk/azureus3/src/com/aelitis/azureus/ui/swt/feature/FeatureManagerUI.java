package com.aelitis.azureus.ui.swt.feature;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.*;

public class FeatureManagerUI
{
	protected static final int DLG_HEIGHT = 320;

	public static boolean enabled = !Constants.isUnix
			&& FeatureAvailability.areInternalFeaturesEnabled();

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

				UIManager ui_manager = pi.getUIManager();

				ui_manager.addUIListener(new UIManagerListener() {
					public void UIDetached(UIInstance instance) {
					}

					public void UIAttached(UIInstance instance) {
						if (!(instance instanceof UISWTInstance)) {
							return;
						}

						final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
						mdi.registerEntry(
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
								new MdiEntryCreationListener() {
									public MdiEntry createMDiEntry(String id) {
										MdiEntry mainMdiEntry = mdi.createEntryFromSkinRef(null,
												MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
												"burn.ftux", "DVD Burn", null, null, true, -1);
										mainMdiEntry.setImageLeftID("image.sidebar.dvdburn");
										mainMdiEntry.setExpanded(true);
										mainMdiEntry.addListener(new MdiEntryOpenListener() {
											public void mdiEntryOpen(MdiEntry entry) {
												SWTSkinObject so = ((BaseMdiEntry) entry).getSkinObject();
												if (so instanceof SWTSkinObjectBrowser) {
													SWTSkinObjectBrowser soBrowser = (SWTSkinObjectBrowser) so;
													soBrowser.setURL("http://www2.vuze.com/client/plus/burn.php?view=main");
												}
											}
										});

										MdiEntry entryAddDVD = mdi.createEntryFromSkinRef(
												MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
												"burn-free-new", "burn.ftux", "Create New DVD", null,
												null, false, -1);
										entryAddDVD.setImageLeftID("image.sidebar.dvdburn.add");
										entryAddDVD.setExpanded(true);
										entryAddDVD.addListener(new MdiEntryOpenListener() {
											public void mdiEntryOpen(MdiEntry entry) {
												SWTSkinObject so = ((BaseMdiEntry) entry).getSkinObject();
												if (so instanceof SWTSkinObjectBrowser) {
													SWTSkinObjectBrowser soBrowser = (SWTSkinObjectBrowser) so;
													soBrowser.setURL("http://www2.vuze.com/client/plus/burn.php?view=add");
												}
											}
										});

										entryAddDVD.addListener(new MdiEntryDropListener() {
											public boolean mdiEntryDrop(MdiEntry entry,
													Object droppedObject) {
												new VuzeMessageBox("Foo", "Bar", new String[] {
													"Trial",
													"Button2"
												}, 0).open(new UserPrompterResultListener() {
													public void prompterClosed(int result) {
														createTrial();
													}
												});
												return true;
											}
										});

										return mainMdiEntry;
									}
								});
					}
				});
			}
		});
	}

	private static void createTrial() {
		try {
			Licence[] trial = featman.createLicences(new String[] {
				"dvdburn_trial"
			});
		} catch (PluginException e) {
			Logger.log(new LogAlert(true, "Creating Trial", e));
		}
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
					SWTSkinObjectText link = (SWTSkinObjectText) skin.getSkinObject(
							"register-link", soExtra);
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
  				PlusFTUXView sv = (PlusFTUXView) SkinViewManager.getByClass(PlusFTUXView.class);
  				if (sv != null) {
  					sv.setSourceRef("plus-success");
  				}
  				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
  				mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
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

	public static boolean hasFullLicence() {
		if (featman == null) {
			return false;
		}

		boolean full = false;
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			long now = SystemTime.getCurrentTime();
			Long lValidUntil = (Long) fd.getProperty(FeatureDetails.PR_VALID_UNTIL);
			if (lValidUntil != null && lValidUntil.longValue() >= now) {
				full = true;
				break;
			}
			Long lValidOfflineUntil = (Long) fd.getProperty(FeatureDetails.PR_OFFLINE_VALID_UNTIL);
			if (lValidOfflineUntil != null && lValidOfflineUntil.longValue() >= now) {
				full = true;
				break;
			}
		}

		return full;
	}

	public static boolean hasTrialLicence(Licence licence) {
		if (featman == null) {
			return false;
		}

		// if any of the FeatureDetails is a trial, return true

		boolean trial = false;
		FeatureDetails[] featureDetails = licence.getFeatures();
		for (FeatureDetails fd : featureDetails) {
			trial = isTrial(fd);
			if (trial) {
				break;
			}
		}

		return trial;
	}

	public static boolean isTrial(FeatureDetails fd) {
		Long lIsTrial = (Long) fd.getProperty(FeatureDetails.PR_IS_TRIAL);
		return lIsTrial == null ? false : lIsTrial.longValue() != 0;
	}

	/**
	 * @return
	 */
	public static boolean hasFullBurn() {
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"azburn_v");
		return pi != null && pi.getPluginState().isOperational();
	}
}
