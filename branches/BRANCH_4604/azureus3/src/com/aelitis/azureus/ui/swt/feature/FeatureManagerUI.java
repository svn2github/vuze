package com.aelitis.azureus.ui.swt.feature;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
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
	private static final Integer BUTTON_UPGRADE = 0x1000;

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

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						String title = FeatureManagerUI.hasFullLicence()
								? "{mdi.entry.plus.full}" : "{mdi.entry.plus.free}";
						String placeBelow = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME) == null
								? "" : MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME;
						
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
								MultipleDocumentInterface.SIDEBAR_SECTION_PLUS,
								"main.area.plus", title, null, null, true, placeBelow);
						entry.setImageLeftID("image.sidebar.plus");
						return entry;
					}
				});

		MdiEntry existingEntry = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_HEADER_DVD);
		if (existingEntry != null) {
			// abandon all hope, something already added DVD stuff
			return;
		}
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {

						MdiEntry entryAbout = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_DVD,
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO, "main.burn.ftux",
								"{mdi.entry.about.dvdburn}", null, null,
								false, null);
						entryAbout.setImageLeftID("image.sidebar.dvdburn");
						entryAbout.setExpanded(true);

						entryAbout.addListener(new MdiEntryDropListener() {
							public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
								openTrialAskWindow();
								return true;
							}
						});

						MenuManager menuManager = PluginInitializer.getDefaultInterface().getUIManager().getMenuManager();
						MenuItem menuHide = menuManager.addMenuItem("Sidebar." +
								MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
								"popup.error.hide");
						menuHide.addListener(new MenuItemListener() {
							public void selected(MenuItem menu, Object target) {
								mdi.closeEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO);
							}
						});

						return entryAbout;
					}
				});
		
		mdi.addListener(new MdiEntryLoadedListener() {
			public void mdiEntryLoaded(MdiEntry entry) {
				if (!entry.getId().equals(MultipleDocumentInterface.SIDEBAR_HEADER_DVD)) {
					return;
				}
				MdiEntryVitalityImage addSub = entry.addVitalityImage("image.sidebar.subs.add");
				addSub.addListener(new MdiEntryVitalityImageListener() {
					public void mdiEntryVitalityImage_clicked(int x, int y) {
						openTrialAskWindow();
					}
				});
			}
		});

		if (ConfigurationChecker.isNewVersion()
				&& !ConfigurationChecker.isNewInstall() && !hasFullLicence()) {
			SBC_PlusFTUX.setSourceRef("startup");
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
		} catch (Throwable e) {
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
    					if (details != null && details.licence.getState() != Licence.LS_INVALID_KEY) {
    						key[0].setText(details.licence.getKey());
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

    							int state = details.licence.getState();
    							if (state == Licence.LS_CANCELLED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.cancelled"));
    							} else if (state == Licence.LS_REVOKED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.revoked"));
    							} else if (state == Licence.LS_ACTIVATION_DENIED) {
    								soExpirey.setText(MessageText.getString("dlg.auth.enter.denied"));
    							} else {
      							soExpirey.setText(MessageText.getString("dlg.auth.enter.expiry",
      									new String[] {
      										DisplayFormatters.formatCustomDateOnly(details.expiry)
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
  					} catch (Throwable e) {
  						
  						String s = Debug.getNestedExceptionMessage(e);
  						
  						MessageBoxShell mb = new MessageBoxShell(
    							SWT.ICON_ERROR | SWT.OK,
    							"Licence Addition Error",
    							s );
  						
  						mb.open();
  						
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

	public static void openStreamPlusWindow(final String referal) {
		String msgidPrefix;
		String buttonID;
		long plusExpiryTimeStamp = FeatureManagerUI.getPlusExpiryTimeStamp();
		if (plusExpiryTimeStamp < 0
				|| plusExpiryTimeStamp >= SystemTime.getCurrentTime()) {
			msgidPrefix = "dlg.stream.plus.";
			buttonID = "Button.upgrade";
		} else {
			buttonID = "Button.renew";
			msgidPrefix = "dlg.stream.plus.renew.";
			if (!MessageText.keyExistsForDefaultLocale(msgidPrefix + "text")) {
				msgidPrefix = "dlg.stream.plus.";
			}
		}
		final String f_msgidPrefix = msgidPrefix;
		final VuzeMessageBox box = new VuzeMessageBox(
				MessageText.getString(msgidPrefix + "title"),
				MessageText.getString(msgidPrefix + "text"), new String[] {
					MessageText.getString(buttonID),
					MessageText.getString("Button.cancel"),
				}, 0);
		box.setButtonVals(new Integer[] {
			BUTTON_UPGRADE,
			SWT.CANCEL
		});

		box.setSubTitle(MessageText.getString(msgidPrefix + "subtitle"));
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_streamplus");
		box.setIconResource("image.header.streamplus");

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.stream.plus", "dlg.stream.plus", soExtra);
				SWTSkinObject soSubText = skin.getSkinObject("trial-info", soExtra);
				if (soSubText instanceof SWTSkinObjectText) {
					((SWTSkinObjectText) soSubText).setTextID(f_msgidPrefix + "subtext");
				}
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == BUTTON_UPGRADE) {
					SBC_PlusFTUX.setSourceRef("dlg-stream" + (referal == null ? "" : "-" + referal));

					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
				}
			}
		});
	}
	
	public static String appendFeatureManagerURLParams(String url) {
		long remainingUses = FeatureManagerUI.getRemaining();
		long plusExpiryTimeStamp = FeatureManagerUI.getPlusExpiryTimeStamp();
		String plusRenewalCode = FeatureManagerUI.getPlusRenewalCode();

		String newURL = url + (url.contains("?") ? "&" : "?");
		newURL += "mode=" + FeatureManagerUI.getMode();
		if (plusExpiryTimeStamp != 0) {
			newURL += "&remaining_plus="
					+ (plusExpiryTimeStamp - SystemTime.getCurrentTime());
		}
		newURL += "&remaining=" + remainingUses;
		if (plusRenewalCode != null) {
			newURL += "&renewal_code=" + plusRenewalCode;
		}

		return newURL;
	}
	
	public static String getMode() {
		boolean isFull = hasFullLicence();
		boolean isTrial = hasFullBurn() && !isFull;
		return isFull ? "plus" : isTrial ? "trial" : "free";
	}
	
	public static long getPlusExpiryTimeStamp() {
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		if (fullFeatureDetails == null || fullFeatureDetails.expiry == 0) {
			return 0;
		}
		return fullFeatureDetails.expiry;
	}

	public static String getPlusRenewalCode() {
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		if (fullFeatureDetails == null || fullFeatureDetails.expiry == 0) {
			return null;
		}

		return fullFeatureDetails.getRenewalKey();
	}

	public static boolean hasFullLicence() {
		if (featman == null) {
			//Debug.out("featman null");
			Set<String> featuresInstalled = UtilitiesImpl.getFeaturesInstalled();
			return featuresInstalled.contains("dvdburn");
		}

		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			int state = fd.getLicence().getState();
			if (state == Licence.LS_CANCELLED || state == Licence.LS_REVOKED) {
				continue;
			}
			long now = SystemTime.getCurrentTime();
			Long lValidUntil = (Long) fd.getProperty(FeatureDetails.PR_VALID_UNTIL);
			Long lValidOfflineUntil = (Long) fd.getProperty(FeatureDetails.PR_OFFLINE_VALID_UNTIL);
			
			if (lValidUntil == null && lValidOfflineUntil == null) {
				continue;
			}

			if (lValidUntil != null && lValidUntil.longValue() < now) {
				continue;
			}
			if (lValidOfflineUntil != null && lValidOfflineUntil.longValue() < now) {
				continue;
			}

			return true;
		}

		return false;
	}

	public static class licenceDetails {
		private final Licence licence;
		long expiry;

		public licenceDetails(long expirey, Licence licence) {
			this.expiry = expirey;
			this.licence = licence;
		}
		
		public String getRenewalKey() {
			FeatureDetails[] features = licence.getFeatures();
			if (features == null) {
				return null;
			}
			for (FeatureDetails fd : features) {
				Object property = fd.getProperty(FeatureDetails.PR_RENEWAL_KEY);
				if (property instanceof String) {
					return (String) property;
				}
			}
			return null;
		}
	}

	public static licenceDetails getFullFeatureDetails() {
		if (featman == null) {
			Debug.out("featman null");
			return null;
		}

		TreeMap<Long, Licence> mapOrder = new TreeMap<Long, Licence>(
				Collections.reverseOrder());
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			Licence licence = fd.getLicence();
			int state = licence.getState();
			if (state == Licence.LS_ACTIVATION_DENIED) {
				mapOrder.put(1L, licence);
				continue;
			} else if (state == Licence.LS_CANCELLED) {
				mapOrder.put(2L, licence);
				continue;
			} else if (state == Licence.LS_INVALID_KEY) {
				mapOrder.put(3L, licence);
				continue;
			} else if (state == Licence.LS_REVOKED) {
				mapOrder.put(4L, licence);
				continue;
			} else if (state == Licence.LS_PENDING_AUTHENTICATION) {
				mapOrder.put(5L, licence);
				continue;
			}

			long now = SystemTime.getCurrentTime();
			Long lValidUntil = (Long) fd.getProperty(FeatureDetails.PR_VALID_UNTIL);
			Long lValidOfflineUntil = (Long) fd.getProperty(FeatureDetails.PR_OFFLINE_VALID_UNTIL);

			if (lValidUntil == null && lValidOfflineUntil == null) {
				continue;
			}

			long minValidUntil = -1;
			long maxValidUntil = -1;
			if (lValidUntil != null) {
				minValidUntil = maxValidUntil = lValidUntil.longValue();
				if (minValidUntil < now) {
					mapOrder.put(minValidUntil, licence);
					continue;
				}
			}
			if (lValidOfflineUntil != null) {
				long validOfflineUntil = lValidOfflineUntil.longValue();
				if (validOfflineUntil < now) {
					mapOrder.put(minValidUntil, licence);
					continue;
				}
				if (minValidUntil == -1 || validOfflineUntil < minValidUntil) {
					minValidUntil = validOfflineUntil;
				}
				if (maxValidUntil == -1 || validOfflineUntil > maxValidUntil) {
					maxValidUntil = validOfflineUntil;
				}
			}

			mapOrder.put(maxValidUntil, licence);
		}

		if (mapOrder.size() == 0) {
			return null;
		}

		Long firstKey = mapOrder.firstKey();
		Licence licence = mapOrder.get(firstKey);
		return new licenceDetails(firstKey.longValue(), licence);
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
