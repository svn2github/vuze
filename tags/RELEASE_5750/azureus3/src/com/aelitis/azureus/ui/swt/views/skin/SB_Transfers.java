/**
 * Created on Oct 21, 2010
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.CategoryAdderWindow;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.views.PeersGeneralView;
import org.gudy.azureus2.ui.swt.views.utils.CategoryUIUtils;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.torrent.HasBeenOpenedListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiSWTMenuHackListener;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

/**
 * Transfers Sidebar aka "My Torrents" aka "Files"
 * @author TuxPaper
 * @created Oct 21, 2010
 *
 */
public class SB_Transfers
{
	private static final Object AUTO_CLOSE_KEY 	= new Object();
	private static final Object TAG_DATA_KEY	= new Object();
	
	private static final String ID_VITALITY_ACTIVE = "image.sidebar.vitality.dl";

	private static final String ID_VITALITY_ALERT = "image.sidebar.vitality.alert";

	public static class stats
	{
		int numSeeding = 0;

		int numDownloading = 0;

		int numQueued = 0;
		
		int numComplete = 0;

		int numIncomplete = 0;

		int numErrorComplete = 0;

		String errorInCompleteTooltip;

		int numErrorInComplete = 0;

		String errorCompleteTooltip;

		int numUnOpened = 0;

		int numStoppedIncomplete = 0;

		boolean includeLowNoise;
		
		long	newestIncompleteDownloadTime = 0;
		
		private boolean
		sameAs(
			stats		other )
		{
			return( 
					numSeeding 						== other.numSeeding &&
					numDownloading 					== other.numDownloading &&
					numQueued 						== other.numQueued &&
					numComplete 					== other.numComplete &&
					numIncomplete 					== other.numIncomplete &&
					numErrorComplete 				== other.numErrorComplete &&
					numErrorInComplete 				== other.numErrorInComplete &&
					numUnOpened 					== other.numUnOpened &&
					numStoppedIncomplete 			== other.numStoppedIncomplete &&
					newestIncompleteDownloadTime 	== other.newestIncompleteDownloadTime );
		}
		
		private void
		copyFrom(
			stats		other )
		{
			numSeeding 							= other.numSeeding;
			numDownloading 						= other.numDownloading;
			numQueued 							= other.numQueued;			
			numComplete 						= other.numComplete;
			numIncomplete 						= other.numIncomplete;
			numErrorComplete 					= other.numErrorComplete;
			errorInCompleteTooltip 				= other.errorInCompleteTooltip;
			numErrorInComplete 					= other.numErrorInComplete;
			errorCompleteTooltip 				= other.errorCompleteTooltip;
			numUnOpened	 						= other.numUnOpened;
			numStoppedIncomplete				= other.numStoppedIncomplete;
			includeLowNoise 					= other.includeLowNoise;
			newestIncompleteDownloadTime	 	= other.newestIncompleteDownloadTime;
		}
	};

	private static Object	statsLock = new Object();
	
	private static stats statsWithLowNoise = new stats();

	private static stats statsNoLowNoise = new stats();
	
	private static CopyOnWriteList<countRefreshListener> listeners = new CopyOnWriteList<countRefreshListener>();

	private static boolean first = true;

	private static AzureusCore	core;
	private static long			coreCreateTime;
	
	static {
		statsNoLowNoise.includeLowNoise = false;
		statsWithLowNoise.includeLowNoise = true;
	}

	public static void setup(final MultipleDocumentInterface mdi) {

		MdiEntryCreationListener libraryCreator = new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromSkinRef(
						SideBar.SIDEBAR_HEADER_TRANSFERS,
						SideBar.SIDEBAR_SECTION_LIBRARY, "library", "{sidebar."
								+ SideBar.SIDEBAR_SECTION_LIBRARY + "}", null, null, false,
						"");
				entry.setImageLeftID("image.sidebar.library");
				return entry;
			}
		};
		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY, libraryCreator);
		mdi.registerEntry("library", libraryCreator);
		mdi.registerEntry("minilibrary", libraryCreator);

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						return createDownloadingEntry(mdi);
					}
				});

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						return createSeedingEntry(mdi);
					}
				});

		mdi.registerEntry(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						return createUnopenedEntry(mdi);
					}
				});
		

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				setupViewTitleWithCore(core);
			}
		});
		
		PlatformTorrentUtils.addHasBeenOpenedListener(new HasBeenOpenedListener() {
			public void hasBeenOpenedChanged(DownloadManager dm, boolean opened) {
				recountUnopened();
				refreshAllLibraries();
			}
		});

		addMenuUnwatched(SideBar.SIDEBAR_SECTION_LIBRARY);

		mdi.addListener(new MdiEntryLoadedListener() {
			public void mdiEntryLoaded(MdiEntry entry) {
				if (MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS.equals(entry.getId())) {
					addHeaderMenu();
				}
			}
		});
	}

	protected static void addHeaderMenu() {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		
		final MenuManager menuManager = uim.getMenuManager();
		
		MenuItem menuItem;

		menuItem = menuManager.addMenuItem("sidebar."
				+ MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
				"MyTorrentsView.menu.setCategory.add");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				new CategoryAdderWindow(null);
			}
		});
		
			// cats in sidebar
		
		menuItem.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setVisible(COConfigurationManager.getBooleanParameter("Library.CatInSideBar"));
			}
		});

		menuItem = menuManager.addMenuItem("sidebar."
				+ MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
				"ConfigView.section.style.CatInSidebar");
		menuItem.setStyle(MenuItem.STYLE_CHECK);
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				boolean b = COConfigurationManager.getBooleanParameter("Library.CatInSideBar");
				COConfigurationManager.setParameter("Library.CatInSideBar", !b);
			}
		});
		menuItem.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setVisible(CategoryManager.getCategories().length > 0);
				menu.setData(Boolean.valueOf(COConfigurationManager.getBooleanParameter("Library.CatInSideBar")));
			}
		});
				
			// show tags in sidebar
		
		TagUIUtils.setupSideBarMenus( menuManager );
	}

	protected static MdiEntry createUnopenedEntry(MultipleDocumentInterface mdi) {
		MdiEntry infoLibraryUn = mdi.createEntryFromSkinRef(
				SideBar.SIDEBAR_HEADER_TRANSFERS,
				SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED, "library",
				"{sidebar.LibraryUnopened}", null, null, false,
				SideBar.SIDEBAR_SECTION_LIBRARY);
		infoLibraryUn.setImageLeftID("image.sidebar.unopened");

		addMenuUnwatched(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		infoLibraryUn.setViewTitleInfo(new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT
						&& statsNoLowNoise.numUnOpened > 0) {
					return "" + statsNoLowNoise.numUnOpened;
				}
				return null;
			}
		});
		return infoLibraryUn;
	}

	private static void addMenuUnwatched(String id) {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		MenuItem menuItem = menuManager.addMenuItem("sidebar." + id,
				"v3.activity.button.watchall");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				CoreWaiterSWT.waitForCore(TriggerInThread.ANY_THREAD,
						new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								GlobalManager gm = core.getGlobalManager();
								List<?> downloadManagers = gm.getDownloadManagers();
								for (Iterator<?> iter = downloadManagers.iterator(); iter.hasNext();) {
									DownloadManager dm = (DownloadManager) iter.next();

									if (!PlatformTorrentUtils.getHasBeenOpened(dm)
											&& dm.getAssumedComplete()) {
										PlatformTorrentUtils.setHasBeenOpened(dm, true);
									}
								}
							}
						});
			}
		});
	}

	/**
	 * @param mdi
	 * @return
	 *
	 * @since 4.5.1.1
	 */
	protected static MdiEntry createSeedingEntry(MultipleDocumentInterface mdi) {
		ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return null; //numSeeding + " of " + numComplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return MessageText.getString("sidebar.LibraryCD.tooltip",
							new String[] {
								"" + statsNoLowNoise.numComplete,
								"" + statsNoLowNoise.numSeeding
							});
				}
				return null;
			}
		};

		MdiEntry entry = mdi.createEntryFromSkinRef(
				SideBar.SIDEBAR_HEADER_TRANSFERS, SideBar.SIDEBAR_SECTION_LIBRARY_DL,
				"library", "{sidebar.LibraryDL}",
				titleInfoSeeding, null, false, null);
		entry.setImageLeftID("image.sidebar.downloading");

		MdiEntryVitalityImage vitalityImage = entry.addVitalityImage(ID_VITALITY_ALERT);
		vitalityImage.setVisible(false);

		entry.setViewTitleInfo(titleInfoSeeding);

		return entry;
	}

	protected static MdiEntry createDownloadingEntry(MultipleDocumentInterface mdi) {
		final MdiEntry[] entry_holder = { null };

		ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			private long	max_incomp_dl_time;
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					if ( COConfigurationManager.getBooleanParameter( "Request Attention On New Download" )){

						if ( coreCreateTime > 0 ){
							
							if ( max_incomp_dl_time == 0 ){
								
								max_incomp_dl_time = coreCreateTime;
							}
							
							if ( statsNoLowNoise.newestIncompleteDownloadTime > max_incomp_dl_time ){
											
								MdiEntry entry = entry_holder[0];
								
								if ( entry != null ){
									
									max_incomp_dl_time = statsNoLowNoise.newestIncompleteDownloadTime;
															
									entry.requestAttention();
								}
							}
						}
					}
					
					int	current = statsNoLowNoise.numIncomplete;

					if (current > 0)
						return current + ""; // + " of " + numIncomplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return MessageText.getString("sidebar.LibraryDL.tooltip",
							new String[] {
								"" + statsNoLowNoise.numIncomplete,
								"" + statsNoLowNoise.numDownloading
							});
				}

				return null;
			}
		};
		MdiEntry entry = mdi.createEntryFromSkinRef(
				SideBar.SIDEBAR_HEADER_TRANSFERS, SideBar.SIDEBAR_SECTION_LIBRARY_DL,
				"library", "{sidebar.LibraryDL}",
				titleInfoDownloading, null, false, null);
		
		entry_holder[0] = entry;
		
		entry.setImageLeftID("image.sidebar.downloading");

		MdiEntryVitalityImage vitalityImage = entry.addVitalityImage(ID_VITALITY_ACTIVE);
		vitalityImage.setVisible(false);

		vitalityImage = entry.addVitalityImage(ID_VITALITY_ALERT);
		vitalityImage.setVisible(false);

		return entry;
	}

	protected static void 
	setupViewTitleWithCore(AzureusCore _core) {
		
		synchronized( SB_Transfers.class ){
			if (!first) {
				return;
			}
			first = false;
			
			core			= _core;
			coreCreateTime 	= core.getCreateTime();
		}
		
		final CategoryListener categoryListener = new CategoryListener() {
			
			public void downloadManagerRemoved(Category cat, DownloadManager removed) {
				RefreshCategorySideBar(cat);
			}
			
			public void downloadManagerAdded(Category cat, DownloadManager manager) {
				RefreshCategorySideBar(cat);
			}
		};

		COConfigurationManager.addAndFireParameterListener("Library.CatInSideBar",
				new ParameterListener() {
					private CategoryManagerListener categoryManagerListener;

					public void parameterChanged(String parameterName) {
						if (Utils.isAZ2UI()) {
							return;
						}
						
						Category[] categories = CategoryManager.getCategories();
						if (categories.length == 0) {
							return;
						}

						boolean catInSidebar = COConfigurationManager.getBooleanParameter("Library.CatInSideBar");
						if (catInSidebar) {
							if (categoryManagerListener != null) {
								return;
							}

							categoryManagerListener = new CategoryManagerListener() {

								public void categoryRemoved(Category category) {
									removeCategory(category);
								}

								public void categoryChanged(Category category) {
									RefreshCategorySideBar(category);
								}

								public void categoryAdded(Category category) {
									Category[] categories = CategoryManager.getCategories();
									if (categories.length == 3) {
		  							for (Category cat : categories) {
		  								setupCategory(cat);
		  							}
									} else {
										setupCategory(category);
									}
								}
							};
							CategoryManager.addCategoryManagerListener(categoryManagerListener);
							if (categories.length > 2) {
  							for (Category category : categories) {
  								category.addCategoryListener(categoryListener);
  								setupCategory(category);
  							}
							}

						} else {

							if (categoryManagerListener != null) {
								CategoryManager.removeCategoryManagerListener(categoryManagerListener);
								categoryManagerListener = null;
							}
							for (Category category : categories) {
								category.removeCategoryListener(categoryListener);
								removeCategory(category);
							}
						}
					}
				});

		final TagListener tagListener = 
			new TagListener() 
			{
				public void
				taggableAdded(
					Tag			tag,
					Taggable	tagged )
				{
					RefreshTagSideBar( tag );
				}
				
				public void 
				taggableSync(
					Tag 		tag ) 
				{
					RefreshTagSideBar( tag );
				}
				
				public void
				taggableRemoved(
					Tag			tag,
					Taggable	tagged )
				{
					RefreshTagSideBar( tag );
				}
		};

		COConfigurationManager.addAndFireParameterListener("Library.TagInSideBar",
				new ParameterListener(){
					private TagManagerListener	tagManagerListener;
					private TagTypeListener 	tagTypeListenerListener;

					public void parameterChanged(String parameterName) {
						//if (Utils.isAZ2UI()) {
						//	return;
						//}

						boolean tagInSidebar = COConfigurationManager.getBooleanParameter("Library.TagInSideBar");
						
						if ( tagInSidebar ){
							
							if ( tagManagerListener != null ){
								
								return;
							}

							tagTypeListenerListener = 
								new TagTypeListener()
								{
									public void 
									tagTypeChanged(
										TagType		tag_type )
									{
										for ( Tag tag: tag_type.getTags()){
											
											if ( tag.isVisible()){
												
												setupTag( tag );
												
											}else{
									
												RefreshTagSideBar( tag );
											}
										}
									}
										
									@Override
									public void tagEventOccurred(TagEvent event ) {
										int	type = event.getEventType();
										Tag	tag = event.getTag();
										if ( type == TagEvent.ET_TAG_ADDED ){
											tagAdded( tag );
										}else if ( type == TagEvent.ET_TAG_CHANGED ){
											tagChanged( tag );
										}else if ( type == TagEvent.ET_TAG_REMOVED ){
											tagRemoved( tag );
										}else if ( type == TagEvent.ET_TAG_ATTENTION_REQUESTED ){
											MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
											if (mdi == null) {
												return;
											}

											MdiEntry entry = mdi.getEntry("Tag." + tag.getTagType().getTagType() + "." + tag.getTagID());

											if (entry != null) {
												mdi.showEntry( entry );
											}
										}
									}
									
									public void
									tagAdded(
										Tag			tag )
									{
										if ( tag.isVisible()){
											
											setupTag( tag );
											
											tag.addTagListener( tagListener, false );
										}
									}
									
									public void
									tagChanged(
										Tag			tag )
									{
										RefreshTagSideBar( tag );
									}
									
									public void
									tagRemoved(
										Tag			tag )
									{
										removeTag( tag );
									}
								};
							
							tagManagerListener = 
								new TagManagerListener()
								{
									public void
									tagTypeAdded(
										TagManager		manager,
										TagType			tag_type )
									{
										if ( tag_type.getTagType() != TagType.TT_DOWNLOAD_CATEGORY ){
											
											tag_type.addTagTypeListener( tagTypeListenerListener, true );
										}
									}
									
									public void
									tagTypeRemoved(
										TagManager		manager,
										TagType			tag_type )
									{
										for ( Tag t: tag_type.getTags()){
																						
											removeTag( t );
										}
									}
								};

							TagManagerFactory.getTagManager().addTagManagerListener( tagManagerListener, true );
						
						}else{
						
							if ( tagManagerListener != null ){

								TagManagerFactory.getTagManager().removeTagManagerListener( tagManagerListener );
						
								List<TagType> tag_types = TagManagerFactory.getTagManager().getTagTypes();
								
								for ( TagType tt: tag_types ){
									
									if ( tt.getTagType() != TagType.TT_DOWNLOAD_CATEGORY ){
										
										tt.removeTagTypeListener( tagTypeListenerListener );
									}
									
									for ( Tag t: tt.getTags()){
										
										t.removeTagListener( tagListener );
										
										removeTag( t );
									}
								}
								
								tagManagerListener		= null;
								tagTypeListenerListener = null;
							}
						}
					}
				});

		
		
		
		final GlobalManager gm = core.getGlobalManager();
		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				stateChanged(dm, state, statsNoLowNoise);
				stateChanged(dm, state, statsWithLowNoise);
			}

			public void stateChanged(DownloadManager dm, int state, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				synchronized( statsLock ){
					updateDMCounts(dm);
	
					boolean complete = dm.getAssumedComplete();
					Boolean wasErrorStateB = (Boolean) dm.getUserData("wasErrorState");
					boolean wasErrorState = wasErrorStateB == null ? false
							: wasErrorStateB.booleanValue();
					boolean isErrorState = state == DownloadManager.STATE_ERROR;
					if (isErrorState != wasErrorState) {
						int rel = isErrorState ? 1 : -1;
						if (complete) {
							stats.numErrorComplete += rel;
						} else {
							stats.numErrorInComplete += rel;
						}
						updateErrorTooltip(gm,stats);
						dm.setUserData("wasErrorState", Boolean.valueOf(isErrorState));
					}
					refreshAllLibraries();
				}
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				completionChanged(dm, completed, statsNoLowNoise);
				completionChanged(dm, completed, statsWithLowNoise);
			}

			public void completionChanged(DownloadManager dm, boolean completed,
					stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}

				synchronized( statsLock ){
					int dm_state = updateDMCounts(dm);
					
					if (completed) {
						stats.numComplete++;
						stats.numIncomplete--;
						if (dm_state == DownloadManager.STATE_ERROR) {
							stats.numErrorComplete++;
							stats.numErrorInComplete--;
						}
						if (dm_state == DownloadManager.STATE_STOPPED) {
							statsNoLowNoise.numStoppedIncomplete--;
						}
	
					} else {
						stats.numComplete--;
						stats.numIncomplete++;
	
						if (dm_state == DownloadManager.STATE_ERROR) {
							stats.numErrorComplete--;
							stats.numErrorInComplete++;
						}
						if (dm_state == DownloadManager.STATE_STOPPED) {
							statsNoLowNoise.numStoppedIncomplete++;
						}
					}
					recountUnopened();
					updateErrorTooltip( gm, stats);
					refreshAllLibraries();
				}
			}
		};

		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				downloadManagerRemoved(dm, statsNoLowNoise);
				downloadManagerRemoved(dm, statsWithLowNoise);
			}

			public void downloadManagerRemoved(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}
				
				synchronized( statsLock ){
					recountUnopened();
					if (dm.getAssumedComplete()) {
						stats.numComplete--;
						Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
						if (wasDownloadingB != null && wasDownloadingB.booleanValue()) {
							stats.numDownloading--;
						}
					} else {
						stats.numIncomplete--;
						Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
						if (wasSeedingB != null && wasSeedingB.booleanValue()) {
							stats.numSeeding--;
						}
					}
	
					Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
					boolean wasStopped = wasStoppedB == null ? false
							: wasStoppedB.booleanValue();
					if (wasStopped) {
						if (!dm.getAssumedComplete()) {
							stats.numStoppedIncomplete--;
						}
					}
					Boolean wasQueuedB = (Boolean) dm.getUserData("wasQueued");
					boolean wasQueued = wasQueuedB == null ? false
							: wasQueuedB.booleanValue();
					if (wasQueued) {
						stats.numQueued--;
					}
					refreshAllLibraries();
				}
				
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				dm.addListener(dmListener, false);
				
				synchronized( statsLock ){
					recountUnopened();
	
					downloadManagerAdded(dm, statsNoLowNoise);
					downloadManagerAdded(dm, statsWithLowNoise);
					refreshAllLibraries();
				}
			}

			public void downloadManagerAdded(DownloadManager dm, stats stats) {
				if (!stats.includeLowNoise
						&& PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
					return;
				}
				boolean assumed_complete = dm.getAssumedComplete();
				
				synchronized( statsLock ){
					if ( dm.isPersistent() && dm.getTorrent() != null && !assumed_complete ){	// ignore borked torrents as their create time is inaccurate
						stats.newestIncompleteDownloadTime = Math.max( stats.newestIncompleteDownloadTime, dm.getCreationTime());
					}
					int dm_state = dm.getState();
					if (assumed_complete) {
						stats.numComplete++;
						if (dm_state == DownloadManager.STATE_SEEDING) {
							stats.numSeeding++;
						}
					} else {
						stats.numIncomplete++;
						if (dm_state == DownloadManager.STATE_DOWNLOADING) {
							dm.setUserData("wasDownloading", Boolean.TRUE);
							stats.numDownloading++;
						} else {
							dm.setUserData("wasDownloading", Boolean.FALSE);
						}
					}
				}
			}
		}, false);
		
		resetStats( gm, dmListener, statsWithLowNoise, statsNoLowNoise );

		refreshAllLibraries();
		
		SimpleTimer.addPeriodicEvent(
			"header:refresh",
			60*1000,
			new TimerEventPerformer() {
				
				public void 
				perform(
					TimerEvent event) 
				{
					stats withNoise = new stats();
					stats noNoise 	= new stats();
					
					noNoise.includeLowNoise 	= false;
					withNoise.includeLowNoise 	= true;
					
					synchronized( statsLock ){
						
						resetStats( gm, null, withNoise, noNoise );
						
						boolean	fixed = false;
						
						if ( !withNoise.sameAs( statsWithLowNoise )){
							statsWithLowNoise.copyFrom( withNoise );
							fixed = true;
						}
						
						if ( !noNoise.sameAs( statsNoLowNoise )){
							statsNoLowNoise.copyFrom( noNoise );
							fixed = true;
						}
						
						if ( fixed ){
													
							updateErrorTooltip(gm,statsWithLowNoise);
							updateErrorTooltip(gm,statsNoLowNoise);
							
							refreshAllLibraries();
						}
					}
				}
			});
	}

	private static void
	resetStats(
		GlobalManager				gm,
		DownloadManagerListener		listener,
		stats						statsWithLowNoise,
		stats						statsNoLowNoise )
	{
		List<DownloadManager> downloadManagers = gm.getDownloadManagers();
		for (Iterator<DownloadManager> iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = iter.next();
			boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);
			boolean assumed_complete = dm.getAssumedComplete();
			if ( dm.isPersistent() && dm.getTorrent() != null && !assumed_complete ){	// ignore borked torrents as their create time is inaccurate
				long createTime = dm.getCreationTime();
				statsWithLowNoise.newestIncompleteDownloadTime = Math.max( statsWithLowNoise.newestIncompleteDownloadTime, createTime);
				if (!lowNoise) {
					statsNoLowNoise.newestIncompleteDownloadTime = Math.max( statsNoLowNoise.newestIncompleteDownloadTime, createTime);
				}
			}
			if ( listener != null ){
				dm.addListener(listener, false);
			}
			
			int dm_state = dm.getState();
			if (dm_state == DownloadManager.STATE_STOPPED) {
				dm.setUserData("wasStopped", Boolean.TRUE);
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete++;
				}
				if (!lowNoise) {
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete++;
					}
				}
			} else {
				dm.setUserData("wasStopped", Boolean.FALSE);
			}
			
			if (dm_state == DownloadManager.STATE_QUEUED) {
				dm.setUserData("wasQueued", Boolean.TRUE);
				statsWithLowNoise.numQueued++;
				if (!lowNoise) {
					statsNoLowNoise.numQueued++;
				}
			} else {
				dm.setUserData("wasQueued", Boolean.FALSE);
			}
			if (dm.getAssumedComplete()) {
				statsWithLowNoise.numComplete++;
				if (!lowNoise) {
					statsNoLowNoise.numComplete++;
				}
				if (dm_state == DownloadManager.STATE_SEEDING) {
					dm.setUserData("wasSeeding", Boolean.TRUE);
					statsWithLowNoise.numSeeding++;
					if (!lowNoise) {
						statsNoLowNoise.numSeeding++;
					}
				} else {
					dm.setUserData("wasSeeding", Boolean.FALSE);
				}
			} else {
				statsWithLowNoise.numIncomplete++;
				if (!lowNoise) {
					statsNoLowNoise.numIncomplete++;
				}
				if (dm_state == DownloadManager.STATE_DOWNLOADING) {
					statsWithLowNoise.numDownloading++;
					if (!lowNoise) {
						statsNoLowNoise.numDownloading++;
					}
				}
			}
			
			if (!PlatformTorrentUtils.getHasBeenOpened(dm) && dm.getAssumedComplete()) {
				statsNoLowNoise.numUnOpened++;
			}
		}
		
		statsWithLowNoise.numUnOpened = statsNoLowNoise.numUnOpened;
	}
	
	private static void 
	updateErrorTooltip(GlobalManager gm, stats stats) 
	{
		if (stats.numErrorComplete < 0) {
			stats.numErrorComplete = 0;
		}
		if (stats.numErrorInComplete < 0) {
			stats.numErrorInComplete = 0;
		}

		if (stats.numErrorComplete > 0 || stats.numErrorInComplete > 0) {

			String comp_error = null;
			String incomp_error = null;

			List<?> downloads = gm.getDownloadManagers();

			for (int i = 0; i < downloads.size(); i++) {

				DownloadManager download = (DownloadManager) downloads.get(i);

				if (download.getState() == DownloadManager.STATE_ERROR) {

					if (download.getAssumedComplete()) {

						if (comp_error == null) {

							comp_error = download.getDisplayName() + ": "
									+ download.getErrorDetails();
						} else {

							comp_error += "...";
						}
					} else {
						if (incomp_error == null) {

							incomp_error = download.getDisplayName() + ": "
									+ download.getErrorDetails();
						} else {

							incomp_error += "...";
						}
					}
				}
			}

			stats.errorCompleteTooltip = comp_error;
			stats.errorInCompleteTooltip = incomp_error;
		}
	}
	
		// category stuff
	
	private static void RefreshCategorySideBar(Category category) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		MdiEntry entry = mdi.getEntry("Cat."
				+ Base32.encode(category.getName().getBytes()));
		if (entry == null) {
			return;
		}

		ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
	}

	private static void setupCategory(final Category category) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		String name = category.getName();
		String id = "Cat." + Base32.encode(name.getBytes());
		if (category.getType() != Category.TYPE_USER) {
			name = "{" + name + "}";
		}

		ViewTitleInfo viewTitleInfo = new ViewTitleInfo() {

			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					if (statsNoLowNoise.numIncomplete > 0) {
						List<?> dms = category.getDownloadManagers(null);
						if (dms != null) {
							return "" + dms.size();
						}
					}
				}
				return null;
			}
		};

		MdiEntry entry = mdi.createEntryFromSkinRef(
				MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS, id, "library",
				name, viewTitleInfo, category, false, null);
		if (entry != null) {
			entry.setImageLeftID("image.sidebar.library");

			entry.addListener(new MdiEntryDropListener() {
				public boolean mdiEntryDrop(MdiEntry entry, Object payload) {
					if (!(payload instanceof String)) {
						return false;
					}

					String dropped = (String) payload;
					String[] split = Constants.PAT_SPLIT_SLASH_N.split(dropped);
					if (split.length > 1) {
						String type = split[0];
						if (type.startsWith("DownloadManager")) {
							GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
							for (int i = 1; i < split.length; i++) {
								String hash = split[i];

								try {
									DownloadManager dm = gm.getDownloadManager(new HashWrapper(
											Base32.decode(hash)));

									if (dm != null) {
										TorrentUtil.assignToCategory(new Object[] {
											dm
										}, category);
									}

								} catch (Throwable t) {

								}
							}
						}
					}

					return true;
				}
			});
		}

		if (entry instanceof SideBarEntrySWT) {
			final SideBarEntrySWT entrySWT = (SideBarEntrySWT) entry;
			entrySWT.addListener(new MdiSWTMenuHackListener() {
				public void menuWillBeShown(MdiEntry entry, Menu menuTree) {
					CategoryUIUtils.createMenuItems(menuTree, category);
				}
			});
		}

	}

	private static void removeCategory(Category category) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		MdiEntry entry = mdi.getEntry("Cat."
				+ Base32.encode(category.getName().getBytes()));

		if (entry != null) {
			entry.close(true);
		}
	}

		// tag stuff
	
	private static void RefreshTagSideBar(Tag tag) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		MdiEntry entry = mdi.getEntry("Tag." + tag.getTagType().getTagType() + "." + tag.getTagID());
		
		if ( entry == null ){
			
			if ( tag.isVisible()){
				
				setupTag( tag );
			}
			
			return;
		}

		if ( !tag.isVisible()){
			
			removeTag( tag );
			
			return;
		}
		
		String old_title = entry.getTitle();
		
		String tag_title = tag.getTagName( true );
		
		if ( !old_title.equals( tag_title )){
		
			entry.setTitle( tag_title );
		}
		
		ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		
		Object[] tag_data = (Object[])entry.getUserData( TAG_DATA_KEY );
		
		if ( tag_data != null ){
			
			boolean[]	auto_tag 		= tag.isTagAuto();
			boolean[]	old_auto_tag 	= (boolean[])tag_data[1];
			
			if ( !Arrays.equals( auto_tag, old_auto_tag )){
				
				tag_data[1] = auto_tag;
				
				if ( auto_tag[0] && auto_tag[1] ){
					
					entry.removeListener((MdiEntryDropListener)tag_data[0]);
					
				}else{
					
					entry.addListener((MdiEntryDropListener)tag_data[0]);
				}
			}
		}
	}

	private static Object	tag_setup_lock = new Object();
	
	public static MdiEntry 
	setupTag(
		final Tag tag ) 
	{
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if ( mdi == null ){
			
			return null;
		}

			/*
			 * Can get hit here concurrently due to various threads interacting with tags...
			 */
		
		synchronized( tag_setup_lock ){
			
			String id = "Tag." + tag.getTagType().getTagType() + "." + tag.getTagID();
	
			if ( mdi.getEntry( id ) != null ){
				
				return null;
			}
			
				// find where to locate this in the sidebar
					
			TreeMap<Tag,String>	name_map = new TreeMap<Tag,String>(TagUIUtils.getTagComparator());
			
			name_map.put( tag, id );
			
			for ( Tag t: tag.getTagType().getTags()){
				
				if ( t.isVisible()){
					
					String tid = "Tag." + tag.getTagType().getTagType() + "." + t.getTagID();
	
					if ( mdi.getEntry( tid ) != null ){
						
						name_map.put( t, tid );
					}
				}
			}
			
			String	prev_id = null;
			
			for ( String this_id: name_map.values()){
						
				if ( this_id == id ){
					
					break;
				}
				
				prev_id = this_id;
			}
			
			if ( prev_id == null && name_map.size() > 1 ){
							
				Iterator<String>	it = name_map.values().iterator();
				
				it.next();
				
				prev_id = "~" + it.next();
			}
			
			boolean auto = tag.getTagType().isTagTypeAuto();
					
			ViewTitleInfo viewTitleInfo = 
				new ViewTitleInfo() 
				{
					public Object 
					getTitleInfoProperty(
						int pid )
					{
						if ( pid == TITLE_INDICATOR_TEXT ){
							
							return( String.valueOf( tag.getTaggedCount()));
							
						}else if ( pid == TITLE_INDICATOR_COLOR ){
							
							TagType tag_type = tag.getTagType();
							
							int[] def_color = tag_type.getColorDefault();
							
							int[] tag_color = tag.getColor();
							
							if ( tag_color != def_color ){
								
								return( tag_color );
							}
							
						}else if ( pid == TITLE_INDICATOR_TEXT_TOOLTIP ){
							
							return( TagUIUtils.getTagTooltip( tag ));	
						}
						
						return null;
					}
				};
	
			MdiEntry entry;
			
			boolean closable = auto;
			
			if ( tag.getTaggableTypes() == Taggable.TT_DOWNLOAD ){
				
				closable = true;
				
				String name = tag.getTagName( true );
	
				entry = mdi.createEntryFromSkinRef(
						MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS, id, "library",
						name, viewTitleInfo, tag, closable, prev_id);
			}else{
				
				entry = mdi.createEntryFromEventListener(
							MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS, 
							new PeersGeneralView( tag ), id, closable, null, prev_id);
				
				entry.setViewTitleInfo( viewTitleInfo );
			}
			
			if ( closable ){
				
				entry.addListener(
					new MdiCloseListener()
					{
						public void 
						mdiEntryClosed(
							MdiEntry 	entry, 
							boolean 	userClosed )
						{
							if ( userClosed && entry.getUserData( AUTO_CLOSE_KEY ) == null ){
								
									// userClosed isn't all we want - it just means we're not closing the app... So to prevent
									// a deselection of 'show tags in sidebar' 'user-closing' the entries we need this test
								
								if ( COConfigurationManager.getBooleanParameter("Library.TagInSideBar")){
								
									tag.setVisible( false );
								}
							}
						}
					});
			}
			
			if (entry != null) {
				String image_id = tag.getImageID();
				
				if ( image_id != null ){
					entry.setImageLeftID( image_id );
				}else if ( tag.getTagType().getTagType() == TagType.TT_PEER_IPSET ){
					entry.setImageLeftID("image.sidebar.tag-red");
				}else if ( tag.getTagType().isTagTypePersistent()){
					entry.setImageLeftID("image.sidebar.tag-green");
				}else{
					entry.setImageLeftID("image.sidebar.tag-blue");
				}
			}
	
			if (entry instanceof SideBarEntrySWT) {
				final SideBarEntrySWT entrySWT = (SideBarEntrySWT) entry;
				entrySWT.addListener(new MdiSWTMenuHackListener() {
					public void menuWillBeShown(MdiEntry entry, Menu menuTree) {
						TagUIUtils.createSideBarMenuItems(menuTree, tag);
					}
				});
			}
			
			if ( !auto && entry != null ){	
		
				MdiEntryDropListener dl = new MdiEntryDropListener() {
					public boolean mdiEntryDrop(MdiEntry entry, Object payload) {
						if (!(payload instanceof String)) {
							return false;
						}
						
						boolean[] auto = tag.isTagAuto();
						
						if ( auto[0] && auto[1] ){
							
							return( false );
						}
						
						final String dropped = (String) payload;
	
						new AEThread2("Tagger") {
							@Override
							public void run() {
								dropTorrentOnTag(tag, dropped);
							}
						}.start();
		
						return true;
					}
	
					private void dropTorrentOnTag(Tag tag, String dropped) {
						String[] split = Constants.PAT_SPLIT_SLASH_N.split(dropped);
						if (split.length <= 1) {
							return;
						}
	
						String type = split[0];
						if (!type.startsWith("DownloadManager") && !type.startsWith( "DiskManagerFileInfo" )) {
							return;
						}
						GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
						List<DownloadManager> listDMs = new ArrayList<DownloadManager>();
						boolean doAdd = false;
						for (int i = 1; i < split.length; i++) {
							String hash = split[i];
							
							int sep = hash.indexOf( ";" );	// for files
							
							if ( sep != -1 ){
								
								hash = hash.substring( 0, sep );
							}
							
							try {
								DownloadManager dm = gm.getDownloadManager(new HashWrapper(
										Base32.decode(hash)));
								
								if ( dm != null ){
									
									listDMs.add(dm);
									
									if (!doAdd && !tag.hasTaggable(dm)) {
										doAdd = true;
									}
								}
							}catch ( Throwable t ){
								
							}
							
							boolean	auto[] = tag.isTagAuto();
							
							for (DownloadManager dm : listDMs) {
								if ( doAdd ){
									
									if ( !auto[0] ){
									
										tag.addTaggable( dm );
									}
								}else{
									
									if ( !( auto[0] && auto[1] )){
									
										tag.removeTaggable( dm );
									}
								}
							}
						}
					}
				};
				
				boolean[] tag_auto = tag.isTagAuto();
				
				entry.setUserData( TAG_DATA_KEY, new Object[]{ dl, tag_auto });
				
				if ( !( tag_auto[0] && tag_auto[1] )){
					
					entry.addListener( dl );
				}
			}
			return entry;
		}
	}

	private static void removeTag(Tag tag) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		MdiEntry entry = mdi.getEntry("Tag." + tag.getTagType().getTagType() + "." + tag.getTagID());

		if (entry != null) {
			
			entry.setUserData( AUTO_CLOSE_KEY, "" );
			
			entry.close( true );
		}
	}
	
	
		// -------------------
	
	private static int updateDMCounts(DownloadManager dm) {
		boolean isSeeding;
		boolean isDownloading;
		boolean isQueued;
		boolean isStopped;

		Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
		boolean wasSeeding = wasSeedingB == null ? false
				: wasSeedingB.booleanValue();
		Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
		boolean wasDownloading = wasDownloadingB == null ? false
				: wasDownloadingB.booleanValue();
		Boolean wasStoppedB = (Boolean) dm.getUserData("wasStopped");
		boolean wasStopped = wasStoppedB == null ? false
				: wasStoppedB.booleanValue();
		Boolean wasQueuedB = (Boolean) dm.getUserData("wasQueued");
		boolean wasQueued = wasQueuedB == null ? false
				: wasQueuedB.booleanValue();

		int dm_state = dm.getState();
		
		if (dm.getAssumedComplete()) {
			isSeeding = dm_state == DownloadManager.STATE_SEEDING;
			isDownloading = false;
		} else {
			isDownloading = dm_state == DownloadManager.STATE_DOWNLOADING;
			isSeeding = false;
		}

		isStopped 	= dm_state == DownloadManager.STATE_STOPPED;
		isQueued	= dm_state == DownloadManager.STATE_QUEUED;
		
		boolean lowNoise = PlatformTorrentUtils.isAdvancedViewOnly(dm);

		if (isDownloading != wasDownloading) {
			if (isDownloading) {
				statsWithLowNoise.numDownloading++;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading++;
				}
			} else {
				statsWithLowNoise.numDownloading--;
				if (!lowNoise) {
					statsNoLowNoise.numDownloading--;
				}
			}
			dm.setUserData("wasDownloading", Boolean.valueOf(isDownloading));
		}

		if (isSeeding != wasSeeding) {
			if (isSeeding) {
				statsWithLowNoise.numSeeding++;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding++;
				}
			} else {
				statsWithLowNoise.numSeeding--;
				if (!lowNoise) {
					statsNoLowNoise.numSeeding--;
				}
			}
			dm.setUserData("wasSeeding", Boolean.valueOf(isSeeding));
		}

		if (isStopped != wasStopped) {
			if (isStopped) {
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete++;
				}
				if (!lowNoise) {
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete++;
					}
				}
			} else {
				if (!dm.getAssumedComplete()) {
					statsWithLowNoise.numStoppedIncomplete--;
				}
				if (!lowNoise) {
					if (!dm.getAssumedComplete()) {
						statsNoLowNoise.numStoppedIncomplete--;
					}
				}
			}
			dm.setUserData("wasStopped", Boolean.valueOf(isStopped));
		}

		if (isQueued != wasQueued) {
			if (isQueued) {
				statsWithLowNoise.numQueued++;
				if (!lowNoise) {
					statsNoLowNoise.numQueued++;
				}
			} else {
				statsWithLowNoise.numQueued--;
				if (!lowNoise) {
					statsNoLowNoise.numQueued--;
				}
			}
			dm.setUserData("wasQueued", Boolean.valueOf(isQueued));
		}
		return( dm_state );
	}

	private static void recountUnopened() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return;
		}
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		List<?> dms = gm.getDownloadManagers();
		statsNoLowNoise.numUnOpened = 0;
		for (Iterator<?> iter = dms.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (!PlatformTorrentUtils.getHasBeenOpened(dm) && dm.getAssumedComplete()) {
				statsNoLowNoise.numUnOpened++;
			}
		}
		statsWithLowNoise.numUnOpened = statsNoLowNoise.numUnOpened;
	}

	protected static void addCountRefreshListener(countRefreshListener l) {
		l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		listeners.add(l);
	}

	public static void triggerCountRefreshListeners() {
		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */

	private static FrequencyLimitedDispatcher refresh_limiter = new FrequencyLimitedDispatcher(
			new AERunnable() {
				public void runSupport() {
					refreshAllLibrariesSupport();
				}
			}, 250);

	static {
		refresh_limiter.setSingleThreaded();
	}

	private static void refreshAllLibraries() {
		refresh_limiter.dispatch();
	}

	private static void refreshAllLibrariesSupport() {
		for (countRefreshListener l : listeners) {
			l.countRefreshed(statsWithLowNoise, statsNoLowNoise);
		}
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}

		if (statsNoLowNoise.numIncomplete > 0) {
			MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
			if (entry == null) {
				mdi.loadEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY_DL, false);
			}
		} else {
			MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
			if (entry != null) {
				entry.close(true);
			}
		}
		MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				String imageID = vitalityImage.getImageID();
				if (imageID == null) {
					continue;
				}
				if (imageID.equals(ID_VITALITY_ACTIVE)) {
					vitalityImage.setVisible(statsNoLowNoise.numDownloading > 0);

				} else if (imageID.equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorInComplete > 0);
					if (statsNoLowNoise.numErrorInComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorInCompleteTooltip);
					}
				}
			}
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}

		entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (entry != null) {
			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				MdiEntryVitalityImage vitalityImage = vitalityImages[i];
				String imageID = vitalityImage.getImageID();
				if (imageID == null) {
					continue;
				}
				if (imageID.equals(ID_VITALITY_ALERT)) {
					vitalityImage.setVisible(statsNoLowNoise.numErrorComplete > 0);
					if (statsNoLowNoise.numErrorComplete > 0) {
						vitalityImage.setToolTip(statsNoLowNoise.errorCompleteTooltip);
					}
				}
			}
		}

		entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		if (entry != null) {
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}
	}

	public static String getTableIdFromFilterMode(int torrentFilterMode,
			boolean big) {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_COMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_COMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_INCOMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableManager.TABLE_MYTORRENTS_ALL_BIG;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			return big ? TableManager.TABLE_MYTORRENTS_UNOPENED_BIG
					: TableManager.TABLE_MYTORRENTS_UNOPENED;
		}
		return null;
	}

	protected static interface countRefreshListener
	{
		public void countRefreshed(stats statsWithLowNoise, stats statsNoLowNoise);
	}
}
