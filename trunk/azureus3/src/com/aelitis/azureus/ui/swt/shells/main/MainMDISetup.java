package com.aelitis.azureus.ui.swt.shells.main;

import java.util.Map;

import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.views.ConfigView;
import org.gudy.azureus2.ui.swt.views.LoggerView;
import org.gudy.azureus2.ui.swt.views.PeersSuperView;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.*;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.FeatureUtils;

public class MainMDISetup
{
	public static void setupSideBar(final MultipleDocumentInterfaceSWT mdi,
			final MdiListener l) {
		if (Utils.isAZ2UI()) {
			setupSidebarClassic(mdi);
		} else {
			setupSidebarVuzeUI(mdi);
		}

		mdi.registerEntry(SideBar.SIDEBAR_TORRENT_DETAILS_PREFIX + ".*",
				new MdiEntryCreationListener2() {
					public MdiEntry createMDiEntry(MultipleDocumentInterface mdi,
							String id, Object datasource, Map<?, ?> params) {
						return createTorrentDetailEntry(mdi, id, datasource);
					}
				});

		PluginInitializer.getDefaultInterface().getUIManager().addUIListener(
				new UIManagerListener2() {
					public void UIDetached(UIInstance instance) {
					}

					public void UIAttached(UIInstance instance) {
					}

					public void UIAttachedComplete(UIInstance instance) {

						PluginInitializer.getDefaultInterface().getUIManager().removeUIListener(
								this);

						MdiEntry currentEntry = mdi.getCurrentEntry();
						if (currentEntry != null) {
							// User or another plugin selected an entry
							return;
						}

						final String CFG_STARTTAB = "v3.StartTab";
						final String CFG_STARTTAB_DS = "v3.StartTab.ds";
						String startTab;
						String datasource = null;
						boolean showWelcome = COConfigurationManager.getBooleanParameter("v3.Show Welcome");
						if (ConfigurationChecker.isNewVersion()) {
							showWelcome = true;
						}

						ContentNetwork startupCN = ContentNetworkManagerFactory.getSingleton().getStartupContentNetwork();
						if (startupCN == null || !startupCN.isServiceSupported(ContentNetwork.SERVICE_WELCOME)) {
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
							datasource = COConfigurationManager.getStringParameter(
									CFG_STARTTAB_DS, null);
						}
						if (startTab.equals(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS)) {
							SBC_PlusFTUX.setSourceRef("lastview");
						}
						if (!mdi.loadEntryByID(startTab, true, false, datasource)) {
							mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
						}
						if (l != null) {
							mdi.addListener(l);
						}
					}
				});
		;

		COConfigurationManager.addAndFireParameterListener(
				"Beta Programme Enabled", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						boolean enabled = COConfigurationManager.getBooleanParameter("Beta Programme Enabled");
						if (enabled) {
							mdi.loadEntryByID(
									MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM, false);
						}
					}
				});

		mdi.registerEntry(StatsView.VIEW_ID, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromEventListener(
						MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, new StatsView(),
						id, true, null, null);
				return entry;
			}
		});

		mdi.registerEntry(PeersSuperView.VIEW_ID, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromEventListener(
						MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS, new PeersSuperView(),
						id, true, null, null);
				// TODO: come up with a better icon?
				entry.setImageLeftID("image.sidebar.plugin");
				return entry;
			}
		});
		
		mdi.registerEntry(LoggerView.VIEW_ID, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromEventListener(
						MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, new LoggerView(),
						id, true, null, null);
				return entry;
			}
		});

		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_TAGS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
								MultipleDocumentInterface.SIDEBAR_SECTION_TAGS, "tagsview",
								"{mdi.entry.tagsoverview}", null, null, true, null);
						// TODO: Don't steal blue icon
						entry.setImageLeftID("image.sidebar.tag-blue");
						return entry;
					}
				});
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		if (pi != null) {
			UIManager uim = pi.getUIManager();
			if (uim != null) {
				MenuItem menuItem = uim.getMenuManager().addMenuItem(
						MenuManager.MENU_MENUBAR, "tags.view.heading");
				menuItem.addListener(new MenuItemListener() {
					public void selected(MenuItem menu, Object target) {
						UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
								MultipleDocumentInterface.SIDEBAR_SECTION_TAGS);
					}
				});
			}
		}

		//		System.out.println("Activate sidebar " + startTab + " took "
		//				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		//		startTime = SystemTime.getCurrentTime();
	}

	private static void setupSidebarClassic(final MultipleDocumentInterfaceSWT mdi) {
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

		mdi.registerEntry(ConfigView.VIEW_ID, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromEventListener(
						MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, new ConfigView(),
						id, true, null, null);
				return entry;
			}
		});
	}

	private static void setupSidebarVuzeUI(final MultipleDocumentInterfaceSWT mdi) {
		MdiEntry entry;

		String[] preferredOrder = new String[] {
			MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
			MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
			MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
			MultipleDocumentInterface.SIDEBAR_HEADER_DEVICES,
			MultipleDocumentInterface.SIDEBAR_HEADER_SUBSCRIPTIONS,
			MultipleDocumentInterface.SIDEBAR_HEADER_DVD,
			MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
		};
		mdi.setPreferredOrder(preferredOrder);

		for (int i = 0; i < preferredOrder.length; i++) {
			String id = preferredOrder[i];
			mdi.registerEntry(id, new MdiEntryCreationListener() {
				public MdiEntry createMDiEntry(String id) {
					MdiEntry entry = mdi.createHeader(id, "sidebar." + id, null);
					
					if ( entry == null ){
						
						return( null );
					}
					
					entry.setDefaultExpanded(true);

					if (id.equals(MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS)) {
						entry.addListener(new MdiChildCloseListener() {
							public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
									boolean user) {
								if (mdi.getChildrenOf(parent.getId()).size() == 0) {
									parent.close(true);
								}
							}
						});

						PluginInterface pi = PluginInitializer.getDefaultInterface();
						UIManager uim = pi.getUIManager();
						MenuManager menuManager = uim.getMenuManager();
						MenuItem menuItem;

						menuItem = menuManager.addMenuItem("sidebar."
								+ MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								"label.plugin.options");

						menuItem.addListener(new MenuItemListener() {
							public void selected(MenuItem menu, Object target) {
								UIFunctions uif = UIFunctionsManager.getUIFunctions();

								if (uif != null) {

									uif.openView(UIFunctions.VIEW_CONFIG, "plugins");
								}
							}
						});
					}

					return entry;
				}
			});
		}

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

		if ( COConfigurationManager.getBooleanParameter( "Show Options In Side Bar" )){
			
			mdi.registerEntry(ConfigView.VIEW_ID, new MdiEntryCreationListener() {
				public MdiEntry createMDiEntry(String id) {
					MdiEntry entry = mdi.createEntryFromEventListener(
							MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, new ConfigView(),
							id, true, null, null);
					return entry;
				}
			});
		}
			
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS,
								MultipleDocumentInterface.SIDEBAR_SECTION_ABOUTPLUGINS,
								"main.generic.browse", "{mdi.entry.about.plugins}", null, null,
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

								if (!FeatureUtils.hasFullBurn()) {
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

		SB_Transfers.setup(mdi);
		new SB_Vuze(mdi);
		new SB_Discovery(mdi);

		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY, false);
		mdi.loadEntryByID(
				MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY_UNOPENED, false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_SUBSCRIPTIONS,
				false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_DEVICES, false);
		mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_ACTIVITIES, false);
	}

	protected static MdiEntry createTorrentDetailEntry(
			MultipleDocumentInterface mdi, String id, Object ds) {
		if (ds == null) {
			return null;
		}
		final MdiEntry torrentDetailEntry = mdi.createEntryFromSkinRef(
				SideBar.SIDEBAR_HEADER_TRANSFERS, id, "torrentdetails", "", null, ds,
				true, null);

		final ViewTitleInfo viewTitleInfo = new ViewTitleInfo() {

			public Object getTitleInfoProperty(int propertyID) {
				Object ds = ((BaseMdiEntry) torrentDetailEntry).getDatasourceCore();
				if (propertyID == TITLE_EXPORTABLE_DATASOURCE) {
					return DataSourceUtils.getHash(ds);
				} else if (propertyID == TITLE_LOGID) {
					return "DMDetails";
				} else if (propertyID == TITLE_IMAGEID) {
					return "image.sidebar.details";
				}

				DownloadManager manager = SBC_TorrentDetailsView.dataSourceToDownloadManager(ds);
				if (manager == null) {
					return null;
				}

				if (propertyID == TITLE_TEXT) {
					if (Utils.isAZ2UI()) {
						int completed = manager.getStats().getCompleted();
						return DisplayFormatters.formatPercentFromThousands(completed)
								+ " : " + manager.getDisplayName();
					}

					return manager.getDisplayName();
				}

				if (propertyID == TITLE_INDICATOR_TEXT && !Utils.isAZ2UI()) {
					int completed = manager.getStats().getCompleted();
					if (completed != 1000) {
						return (completed / 10) + "%";
					}
				} else if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					String s = "";
					int completed = manager.getStats().getCompleted();
					if (completed != 1000) {
						s = (completed / 10) + "% Complete\n";
					}
					String eta = DisplayFormatters.formatETA(manager.getStats().getSmoothedETA());
					if (eta.length() > 0) {
						s += MessageText.getString("TableColumn.header.eta") + ": " + eta
								+ "\n";
					}

					return manager.getDisplayName() + ( s.length()==0?"":( ": " + s));
				}
				return null;
			}
		};

		if (torrentDetailEntry instanceof MdiEntrySWT) {
			((MdiEntrySWT) torrentDetailEntry).addListener(new MdiSWTMenuHackListener() {
				public void menuWillBeShown(MdiEntry entry, Menu menuTree) {
					// todo: This even work?
					TableView<?> tv = SelectedContentManager.getCurrentlySelectedTableView();
					menuTree.setData("TableView", tv);
					DownloadManager manager = SBC_TorrentDetailsView.dataSourceToDownloadManager(torrentDetailEntry.getDatasource());
					if (manager != null) {
						menuTree.setData("downloads", new DownloadManager[] {
							manager
						});
					}
					menuTree.setData("is_detailed_view", new Boolean(true));

					MenuFactory.buildTorrentMenu(menuTree);
				}
			});
		}

		torrentDetailEntry.addListener(new MdiEntryDatasourceListener() {
			public void mdiEntryDatasourceChanged(MdiEntry entry) {
				Object newDataSource = entry.getDatasource();
				if (newDataSource instanceof String) {
					final String s = (String) newDataSource;
					if (!AzureusCoreFactory.isCoreRunning()) {
						AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								torrentDetailEntry.setDatasource(DataSourceUtils.getDM(s));
							}
						});
						return;
					}
				}

				ViewTitleInfoManager.refreshTitleInfo(viewTitleInfo);
			}
		});
		torrentDetailEntry.setViewTitleInfo(viewTitleInfo);

		return torrentDetailEntry;
	}
}
