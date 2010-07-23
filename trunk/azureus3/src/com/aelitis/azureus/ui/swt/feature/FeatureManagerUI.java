package com.aelitis.azureus.ui.swt.feature;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
//import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SBC_PlusFTUX;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;
import com.aelitis.azureus.util.ConstantsVuze;

public class FeatureManagerUI
{
	public static boolean enabled = !Constants.isUnix
			//&& FeatureAvailability.ENABLE_PLUS()
			|| System.getProperty("fm.ui", "0").equals("1");

	private static FeatureManager featman;

	private static VuzeMessageBox validatingBox;

	private static VuzeMessageBox entryWindow;

	private static FeatureManagerUIListener fml;
	
	public static void registerWithFeatureManager() {
		if (!enabled) {
			return;
		}
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {

			public void azureusCoreRunning(AzureusCore core) {
				PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();
				featman = pi.getUtilities().getFeatureManager();

				fml = new FeatureManagerUIListener(featman);
				featman.addListener(fml);
				Licence[] licences = featman.getLicences();
				for (Licence licence : licences) {
					fml.licenceAdded(licence);
				}
				

				UIManager ui_manager = pi.getUIManager();

				ui_manager.addUIListener(new UIManagerListener() {
					public void UIDetached(UIInstance instance) {
					}

					public void UIAttached(UIInstance instance) {
						if (!(instance instanceof UISWTInstance)) {
							return;
						}

						if (!Utils.isAZ2UI()) {
							addFreeBurnUI();
						}
					}
				});
			}
		});
	}

	private static void addFreeBurnUI() {
		final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry mainMdiEntry = mdi.createEntryFromSkinRef(null,
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
								"main.burn.ftux", MessageText.getString("mdi.entry.dvdburn"),
								null, null, true, -1);
						mainMdiEntry.setImageLeftID("image.sidebar.dvdburn");
						mainMdiEntry.setExpanded(true);

						MdiEntry entryAddDVD = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
								"burn-new", "main.burn.ftux",
								MessageText.getString("mdi.entry.dvdburn.new"), null, null,
								false, -1);
						entryAddDVD.setImageLeftID("image.sidebar.dvdburn.add");
						entryAddDVD.setExpanded(true);

						entryAddDVD.addListener(new MdiEntryDropListener() {
							public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
								openTrialAskWindow();
								return true;
							}
						});

						MenuManager menuManager = PluginInitializer.getDefaultInterface().getUIManager().getMenuManager();
						MenuItem menuHide = menuManager.addMenuItem(
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
								"popup.error.hide");
						menuHide.addListener(new MenuItemListener() {
							public void selected(MenuItem menu, Object target) {
								mdi.closeEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO);
							}
						});

						return mainMdiEntry;
					}
				});

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						String title = MessageText.getString(FeatureManagerUI.hasFullLicence()
								? "mdi.entry.plus.full" : "mdi.entry.plus.free");
						int index = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME) == null
								? 0 : 1;
						MdiEntry entry = mdi.createEntryFromSkinRef(null,
								MultipleDocumentInterface.SIDEBAR_SECTION_PLUS,
								"main.area.plus", title, null, null, true, index);
						entry.setImageLeftID("image.sidebar.plus");
						return entry;
					}
				});
		if (ConfigurationChecker.isNewVersion()
				&& !ConfigurationChecker.isNewInstall() && !hasFullLicence()) {
			mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
		}
	}

	public static void openTrialAskWindow() {
		VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.try.trial.title"),
				MessageText.getString("dlg.try.trial.text"), new String[] {
					MessageText.getString("Button.turnon"),
					MessageText.getString("Button.cancel")
				}, 0);
		box.setButtonVals(new Integer[] {
			SWT.OK,
			SWT.CANCEL,
		});
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.burn.dlg.header");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				String id = "dlg.register.trialask";
				SWTSkinObject so = skin.createSkinObject(id, id, soExtra);

				SWTSkinObjectText soLink = (SWTSkinObjectText) skin.getSkinObject(
						"link", so);
				if (soLink != null) {
					soLink.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
						public boolean urlClicked(URLInfo urlInfo) {
							String url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(
									"plus_tos.start", true);
							Utils.launch(url);
							return true;
						}
					});
				}
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == SWT.OK) {
					SimpleTimer.addEvent("createTrial", SystemTime.getCurrentTime(),
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									createTrial();
								}
							});
				}
			}
		});
	}

	public static void createTrial() {
		try {
			Licence[] trial = featman.createLicences(new String[] {
				"dvdburn_trial"
			});
		} catch (PluginException e) {
			String s = "Creating Trial: " + Debug.getNestedExceptionMessage(e);
			new MessageBoxShell("Trial Error", s).open(null);
			Logger.log(new LogAlert(true, s, e));
		}
	}

	public static void openLicenceEntryWindow(final boolean trytwo, 
			final String prefillWith) {
		if (!enabled) {
			return;
		}
		
		if (entryWindow != null) {
			return;
		}
		
		try {
  		String tryNo = (trytwo ? "2" : "1");
  		final SWTSkinObjectTextbox[] key = new SWTSkinObjectTextbox[1];
  		entryWindow = new VuzeMessageBox(
  				MessageText.getString("dlg.auth.title"),
  				MessageText.getString("dlg.auth.enter.line.try." + tryNo),
  				new String[] {
  					MessageText.getString("Button.agree"),
  					MessageText.getString("Button.cancel")
  				}, 0);
			entryWindow.setButtonVals(new Integer[] {
				SWT.OK,
				SWT.CANCEL,
			});
  
  		entryWindow.setSubTitle(MessageText.getString("dlg.auth.enter.subtitle.try."
  				+ tryNo));
  		entryWindow.addResourceBundle(FeatureManagerUI.class,
  				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
  		entryWindow.setIconResource("image.vp");
  		if (trytwo) {
  			entryWindow.setTextIconResource("image.warn.big");
  		}
  
  		entryWindow.setListener(new VuzeMessageBoxListener() {
  			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
  				SWTSkin skin = soExtra.getSkin();
  				skin.createSkinObject("dlg.register", "dlg.register", soExtra);
  
  				SWTSkinObjectText link = (SWTSkinObjectText) skin.getSkinObject(
  						"register-link", soExtra);
  				link.setText(MessageText.getString(trytwo ? "dlg.auth.enter.link.try.2" : "dlg.auth.enter.link.try.1"));
  				link.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
  					public boolean urlClicked(URLInfo urlInfo) {
  						if (trytwo) {
    						String url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(
    								"upgrade.start", true);
    						Utils.launch(url);
  						} else {
  		  				SBC_PlusFTUX.setSourceRef("dlg-activation");
  
  		  				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
  							mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
  							entryWindow.close(-2);
  						}
  						return true;
  					}
  				});
  
  				SWTSkinObjectText linkTOS = (SWTSkinObjectText) skin.getSkinObject(
  						"tos-link", soExtra);
  				if (linkTOS != null) {
    				linkTOS.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
  						public boolean urlClicked(URLInfo urlInfo) {
								String url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(
										"plus_tos.start", true);
								Utils.launch(url);
  							return true;
  						}
    				});
  				}
  
  				key[0] = (SWTSkinObjectTextbox) skin.getSkinObject("key", soExtra);
  				if (key[0] != null) {
  					if (prefillWith != null) {
  						key[0].setText(prefillWith);
  					} else if (!trytwo) {
    					licenceDetails details = getFullFeatureDetails();
    					if (details != null && details.state != Licence.LS_INVALID_KEY) {
    						key[0].setText(details.key);
    						if (key[0].getControl() instanceof Text) {
    							((Text)key[0].getControl()).selectAll();
    						}
    						final SWTSkinObjectText soExpirey = (SWTSkinObjectText) skin.getSkinObject("register-expirey");
    						if (soExpirey != null) {
    							key[0].getControl().addListener(SWT.Modify, new Listener() {
    								public void handleEvent(Event event) {
    									soExpirey.setText("");
    								}
    							});
    							if (details.state == Licence.LS_CANCELLED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.cancelled"));
    							} else if (details.state == Licence.LS_REVOKED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.revoked"));
    							} else if (details.state == Licence.LS_ACTIVATION_DENIED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.denied"));
    							} else {
      							soExpirey.setText(MessageText.getString("dlg.auth.enter.expiry",
      									new String[] {
      										DisplayFormatters.formatCustomDateOnly(details.expirey)
      									}));
    							} 
    						}
    					}
  					}
  				}
  			}
  		});
  
  		entryWindow.open(new UserPrompterResultListener() {
  			public void prompterClosed(int result) {
  				entryWindow = null;
  				if (result == SWT.OK) {
  					try {
  						Licence licence = featman.addLicence(key[0].getText());
  						int initialState = licence.getState();
  						if (initialState == Licence.LS_AUTHENTICATED) {
  							if ( !licence.isFullyInstalled()){
  								fml.licenceAdded(licence);	// open installing window
  							}else{
  								openLicenceSuccessWindow();
  							}
  						}else if (initialState == Licence.LS_PENDING_AUTHENTICATION ) {
  							fml.licenceAdded(licence);	// open validating window
  						} else if (initialState == Licence.LS_INVALID_KEY) {
  							openLicenceFailedWindow(initialState, key[0].getText());
  						} else if (initialState == Licence.LS_ACTIVATION_DENIED) {
  							openLicenceActivationDeniedWindow(licence);
  						} else if (initialState == Licence.LS_CANCELLED) {
  							openLicenceCancelledWindow(licence);
  						} else if (initialState == Licence.LS_REVOKED) {
  							openLicenceRevokedWindow(licence);
  						}
  					} catch (PluginException e) {
  						Logger.log(new LogAlert(true, LogAlert.AT_ERROR, "Adding Licence",
  								e));
  					}
  				}
  			}
  		});
		} catch (Exception e) {
			entryWindow = null;
		}
	}

	public static void openLicenceSuccessWindow() {
		if (!enabled) {
			return;
		}

		if (hasFullLicence()) {
			openFullLicenceSuccessWindow();
		} else {
			openTrialLicenceSuccessWindow();
		}
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private static void openTrialLicenceSuccessWindow() {
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.trial.success.subtitle"),
				MessageText.getString("dlg.auth.trial.success.line1"), new String[] {
					MessageText.getString("Button.goLibrary"),
				}, 0);
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.burn.dlg.header");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.register.trial.success",
						"dlg.register.trial.success", soExtra);
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					SBC_PlusFTUX.setSourceRef("dlg-trial-installed");

					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY);
				}
			}
		});
	}

	private static void openFullLicenceSuccessWindow() {
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
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.register.success", "dlg.register.success",
						soExtra);
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
  				SBC_PlusFTUX.setSourceRef("dlg-plus-installed");

  				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
				}
			}
		});
	}
	
	public static void openLicenceRevokedWindow(final Licence licence) {
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.revoked"),
				MessageText.getString("dlg.auth.revoked.line1"), new String[] {
					MessageText.getString("Button.close"),
				}, 0);
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.vp");
		box.setTextIconResource("image.warn.big");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				SWTSkinObject so = skin.createSkinObject("dlg.register.revoked",
						"dlg.register.revoked", soExtra);

				SWTSkinObjectText soLink = (SWTSkinObjectText) skin.getSkinObject(
						"link", so);
				if (soLink != null) {
					soLink.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
						public boolean urlClicked(URLInfo urlInfo) {
							String url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(
									"licence_revoked.start?key="
											+ UrlUtils.encode(licence.getKey()), true);
							Utils.launch(url);
							return true;
						}
					});
				}
			}
		});

		box.open(null);
	}

	public static void openLicenceActivationDeniedWindow(final Licence licence) {
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.denied"),
				MessageText.getString("dlg.auth.denied.line1"), new String[] {
					MessageText.getString("Button.close"),
				}, 0);
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.vp");
		box.setTextIconResource("image.warn.big");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				SWTSkinObject so = skin.createSkinObject("dlg.register.denied",
						"dlg.register.denied", soExtra);

				SWTSkinObjectText soLink = (SWTSkinObjectText) skin.getSkinObject(
						"link", so);
				if (soLink != null) {
					soLink.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
						public boolean urlClicked(URLInfo urlInfo) {
							String url = ConstantsVuze.getDefaultContentNetwork().getExternalSiteRelativeURL(
									"licence_denied.start?key="
											+ UrlUtils.encode(licence.getKey()), true);
							Utils.launch(url);
							return true;
						}
					});
				}
			}
		});

		box.open(null);
	}

	public static void openLicenceCancelledWindow(final Licence licence) {
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString("dlg.auth.cancelled"),
				MessageText.getString("dlg.auth.cancelled.line1"), new String[] {
					MessageText.getString("Button.close"),
				}, 0);
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource("image.vp");
		box.setTextIconResource("image.warn.big");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				SWTSkinObject so = skin.createSkinObject("dlg.register.cancelled",
						"dlg.register.cancelled", soExtra);
			}
		});

		box.open(null);
	}


	protected static void openLicenceFailedWindow(int licenceState, String code) {
		openLicenceEntryWindow(true, code);
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
			}
		});

		validatingBox.open(
			new UserPrompterResultListener()
			{
				public void 
				prompterClosed(
					int result ) 
				{
					validatingBox = null;
				}
			});
	}

	public static void closeLicenceValidatingWindow() {
		if (validatingBox != null) {
			validatingBox.close(0);
			validatingBox = null;
		}
	}
	
	public static String getMode() {
		boolean isFull = hasFullLicence();
		boolean isTrial = hasFullBurn() && !isFull;
		return isFull ? "plus" : isTrial ? "trial" : "free";
	}

	public static boolean hasFullLicence() {
		if (featman == null) {
			//Debug.out("featman null");
			Set<String> featuresInstalled = UtilitiesImpl.getFeaturesInstalled();
			return featuresInstalled.contains("dvdburn");
		}

		boolean full = false;
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			int state = fd.getLicence().getState();
			if (state == Licence.LS_CANCELLED || state == Licence.LS_REVOKED) {
				continue;
			}
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

	public static class licenceDetails {
		public licenceDetails(long expirey, String key, int state) {
			this.expirey = expirey;
			this.key = key;
			this.state = state;
		}
		long expirey;
		String key;
		int state;
	}

	public static licenceDetails getFullFeatureDetails() {
		if (featman == null) {
			Debug.out("featman null");
			return null;
		}

		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			long now = SystemTime.getCurrentTime();
			Long lValidUntil = (Long) fd.getProperty(FeatureDetails.PR_VALID_UNTIL);
			if (lValidUntil != null && lValidUntil.longValue() >= now) {
				return new licenceDetails(lValidUntil.longValue(),
						fd.getLicence().getKey(), fd.getLicence().getState());
			}
			Long lValidOfflineUntil = (Long) fd.getProperty(FeatureDetails.PR_OFFLINE_VALID_UNTIL);
			if (lValidOfflineUntil != null && lValidOfflineUntil.longValue() >= now) {
				return new licenceDetails(lValidOfflineUntil.longValue(),
						fd.getLicence().getKey(), fd.getLicence().getState());
			}
		}

		Licence bestLicence = null;
		Licence[] licences = featman.getLicences();
		for (Licence licence : licences) {
			FeatureDetails[] details = licence.getFeatures();
			boolean isTrial = false;
			for (FeatureDetails fd : details) {
				Object property = fd.getProperty(FeatureDetails.PR_IS_TRIAL);
				if ((property instanceof Number) && ((Number)property).intValue() == 1) {
					isTrial = true;
					break;
				}
			}
			if (isTrial) {
				continue;
			}
			int state = licence.getState();
			if (state == Licence.LS_AUTHENTICATED) {
				bestLicence = licence;
				break;
			} else {
				bestLicence = licence;
			}
		}

		if (bestLicence != null) {
			return new licenceDetails(0, bestLicence.getKey(), bestLicence.getState());
		}

		return null;
	}
	
	public static boolean isTrialLicence(Licence licence) {
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
	
	public static long getRemaining() {
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn_trial");
		if (featureDetails == null) {
			return 0;
		}
		for (FeatureDetails fd : featureDetails) {
			long remainingUses = getRemainingUses(fd);
			if (remainingUses >= 0) {
				return remainingUses;
			}
		}
		return 0;
	}

	private static long getRemainingUses(FeatureDetails fd) {
		if (fd == null) {
			return 0;
		}
		Long lRemainingUses = (Long) fd.getProperty(FeatureDetails.PR_TRIAL_USES_REMAINING);
		long remainingUses = lRemainingUses == null ? -1
				: lRemainingUses.longValue();
		return remainingUses;
	}

	/**
	 * @return
	 */
	public static boolean hasFullBurn() {
		
		PluginInterface pi = PluginInitializer.getDefaultInterface().getPluginState().isInitialisationComplete()
				? AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
						"azburn_v") : null;
		if (pi == null) {
			// maybe not added yet.. use featman
			Set<String> featuresInstalled = UtilitiesImpl.getFeaturesInstalled();
			return featuresInstalled.contains("dvdburn_trial") && !featuresInstalled.contains("dvdburn");
		}
		return pi.getPluginState().isOperational();
	}
}
