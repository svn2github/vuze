package com.aelitis.azureus.ui.swt.shells.main;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.views.skin.SBC_ActivityTableView;
import com.aelitis.azureus.ui.swt.views.skin.SBC_PlusFTUX;
import com.aelitis.azureus.ui.swt.views.skin.SB_Transfers;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

public class MainMDISetup
{
	public static void setupSideBar(final MultipleDocumentInterface mdi, final MdiListener l) {
		if (Utils.isAZ2UI()) {
			setupSidebarClassic(mdi);
		} else {
			setupSidebarVuze(mdi);
		}

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				final String CFG_STARTTAB = "v3.StartTab";
				String startTab;
				boolean showWelcome = COConfigurationManager.getBooleanParameter("v3.Show Welcome");
				if (ConfigurationChecker.isNewVersion()) {
					showWelcome = true;
				}

				ContentNetwork startupCN = ContentNetworkManagerFactory.getSingleton().getStartupContentNetwork();
				if (!startupCN.isServiceSupported(ContentNetwork.SERVICE_WELCOME)) {
					showWelcome = false;
				}

				if (showWelcome) {
					startTab = SideBar.SIDEBAR_SECTION_WELCOME;
				} else {
					if (!COConfigurationManager.hasParameter(CFG_STARTTAB, true)) {
						COConfigurationManager.setParameter(CFG_STARTTAB,
								SideBar.SIDEBAR_SECTION_LIBRARY);
					}
					startTab = COConfigurationManager.getStringParameter(CFG_STARTTAB);
					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

					if (mdi == null || mdi.getEntry(startTab) == null) {
						startTab = SideBar.SIDEBAR_SECTION_LIBRARY;
					}
				}
				if (startTab.equals(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS)) {
					SBC_PlusFTUX.setSourceRef("lastview");
				}
				mdi.showEntryByID(startTab);
				if (l != null) {
					mdi.addListener(l);
				}
			}
		});
		
		COConfigurationManager.addAndFireParameterListener(
				"Beta Programme Enabled", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						boolean enabled = COConfigurationManager.getBooleanParameter("Beta Programme Enabled");
						if (enabled) {
							mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM);
						}
					}
		});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
								MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM,
								"main.area.beta", "{Sidebar.beta.title}",
								null, null, true, MultipleDocumentInterface.SIDEBAR_POS_FIRST);
						return entry;
					}
				});

		//		System.out.println("Activate sidebar " + startTab + " took "
		//				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		//		startTime = SystemTime.getCurrentTime();
	}
	
	private static void setupSidebarClassic(final MultipleDocumentInterface mdi) {
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY,
				new MdiEntryCreationListener() {

					public MdiEntry createMDiEntry(String id) {
						boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals(
								"az2");
						String title = uiClassic ? "{MyTorrentsView.mytorrents}"
								: ("{sidebar."
										+ MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY + "}");
						MdiEntry entry = mdi.createEntryFromSkinRef(null,
								MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY, "library",
								title, null, null, false,
								MultipleDocumentInterface.SIDEBAR_POS_FIRST);
						entry.setImageLeftID("image.sidebar.library");
						return entry;
					}
				});

		mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY);
	}

	
	private static void setupSidebarVuze(final MultipleDocumentInterface mdi) {
		MdiEntry entry;

		String[] preferredOrder = new String[] {
			MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
			MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
			MultipleDocumentInterface.SIDEBAR_HEADER_DEVICES,
			MultipleDocumentInterface.SIDEBAR_HEADER_SUBSCRIPTIONS,
			MultipleDocumentInterface.SIDEBAR_HEADER_DVD,
			MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
		};
		mdi.setPreferredOrder(preferredOrder);

		boolean[] disableCollapses = {
			true,
			true,
			false,
			false,
			false,
			false
		};
		for (int i = 0; i < preferredOrder.length; i++) {
			String id = preferredOrder[i];
			final boolean disableCollapse = disableCollapses[i];
			mdi.registerEntry(id, new MdiEntryCreationListener() {
				public MdiEntry createMDiEntry(String id) {
					MdiEntry entry = mdi.createHeader(id, "sidebar." + id, null);
					if (disableCollapse) {
						entry.setCollapseDisabled(true);
					} else {
						entry.setDefaultExpanded(true);
					}

					if (id.equals(MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS)) {
						entry.addListener(new MdiChildCloseListener() {
							public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
									boolean user) {
								if (mdi.getChildrenOf(parent.getId()).size() == 0) {
									parent.close(true);
								}
							}
						});
					}

					return entry;
				}
			});
		}

		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY, false);
		mdi.loadEntryByID(
				MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY_UNOPENED, false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
				false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_DEVICES, false);

		entry = mdi.createEntryFromSkinRef(
				MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
				ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork()),
				"main.area.browsetab", "{sidebar.VuzeHDNetwork}",
				null, null, false, null);
		entry.setImageLeftID("image.sidebar.vuze");

		/*
		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		if (cnm != null) {
			ContentNetwork[] contentNetworks = cnm.getContentNetworks();
			for (ContentNetwork cn : contentNetworks) {
				if (cn == null) {
					continue;
				}
				if (cn.getID() == ConstantsVuze.getDefaultContentNetwork().getID()) {
					cn.setPersistentProperty(ContentNetwork.PP_ACTIVE, Boolean.TRUE);
					continue;
				}

				Object oIsActive = cn.getPersistentProperty(ContentNetwork.PP_ACTIVE);
				boolean isActive = (oIsActive instanceof Boolean)
						? ((Boolean) oIsActive).booleanValue() : false;
				if (isActive) {
					mdi.createContentNetworkSideBarEntry(cn);
				}
			}
		}
		*/

		if (Constants.isWindows && FeatureAvailability.isGamesEnabled()) {
			mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_GAMES,
					new MdiEntryCreationListener() {
						public MdiEntry createMDiEntry(String id) {
							MdiEntry entry = mdi.createEntryFromSkinRef(
									MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
									MultipleDocumentInterface.SIDEBAR_SECTION_GAMES,
									"main.generic.browse",
									"{mdi.entry.games}", null, null, true,
									null);
							((BaseMdiEntry) entry).setPreferredAfterID(ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork()));
							String url = ConstantsVuze.getDefaultContentNetwork().getSiteRelativeURL(
									"starts/games.start", false);
							entry.setDatasource(url);
							entry.setImageLeftID("image.sidebar.games");
							return entry;
						}
					});
			mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_GAMES, false,
					true);
		}

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
								"main.generic.browse",
								"{mdi.entry.about.plugins}", null, null,
								true, MultipleDocumentInterface.SIDEBAR_POS_FIRST);
						String url = ConstantsVuze.getDefaultContentNetwork().getSiteRelativeURL(
								"plugins", true);
						entry.setDatasource(url);
						entry.setImageLeftID("image.sidebar.plugin");
						return entry;
					}
				});
		//loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS, true, false);

		// building plugin views needs UISWTInstance, which needs core.
		final int burnInfoShown = COConfigurationManager.getIntParameter(
				"burninfo.shown", 0);
		if (burnInfoShown == 0) {
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
				public void azureusCoreRunning(AzureusCore core) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (FeatureManagerUI.enabled) {
								// blah, can't add until plugin initialization is done

								mdi.loadEntryByID(
										MultipleDocumentInterface.SIDEBAR_SECTION_PLUS, false);

								if (!FeatureManagerUI.hasFullBurn()) {
									mdi.loadEntryByID(
											MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO,
											false);
								}

								COConfigurationManager.setParameter("burninfo.shown",
										burnInfoShown + 1);
							}
						}
					});
				}
			});
		}

		SBC_ActivityTableView.setupSidebarEntry();

		SB_Transfers.setup(mdi);
	}
}
