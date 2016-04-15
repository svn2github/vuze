/**
 * Created on Jul 2, 2008
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.ViewUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;

/**
 * @author TuxPaper
 * @created Jul 2, 2008
 *
 */
public class SBC_LibraryView
	extends SkinView implements UIPluginViewToolBarListener
{
	private final static String ID = "library-list";

	public final static int MODE_BIGTABLE = 0;

	public final static int MODE_SMALLTABLE = 1;

	public static final int TORRENTS_ALL = 0;

	public static final int TORRENTS_COMPLETE = 1;

	public static final int TORRENTS_INCOMPLETE = 2;

	public static final int TORRENTS_UNOPENED = 3;

	private final static String[] modeViewIDs = {
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_BIG,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_SMALL
	};

	private final static String[] modeIDs = {
		"library.table.big",
		"library.table.small"
	};

	private static boolean	header_show_uptime;
	private static boolean	header_show_rates;
	
	private static volatile OverallStats 	totalStats;
	
	private static volatile int					selection_count;
	private static volatile long				selection_size;
	private static volatile long				selection_done;
	private static volatile DownloadManager[]	selection_dms = {};
	

	static{
		SimpleTimer.addPeriodicEvent(
			"SBLV:updater",
			60*1000,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event ) 
				{
					if ( header_show_uptime ){
						
						SB_Transfers.triggerCountRefreshListeners();
					}
				}
			});
		
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"MyTorrentsView.showuptime", "MyTorrentsView.showrates"
			},
			new ParameterListener()
			{
				private TimerEventPeriodic rate_event;
				
				public void 
				parameterChanged(
					String name ) 
				{
					header_show_uptime 	= COConfigurationManager.getBooleanParameter( "MyTorrentsView.showuptime" );
					header_show_rates	= COConfigurationManager.getBooleanParameter( "MyTorrentsView.showrates" );

					SB_Transfers.triggerCountRefreshListeners();
					
					synchronized( this ){
						
						if ( header_show_rates ){
							
							if ( rate_event == null ){
							
								rate_event = SimpleTimer.addPeriodicEvent(
									"SBLV:rate-updater",
									1*1000,
									new TimerEventPerformer()
									{
										public void 
										perform(
											TimerEvent event ) 
										{
											SB_Transfers.triggerCountRefreshListeners();
										}
									});
							}
						}else{
							
							if ( rate_event != null ){
								
								rate_event.cancel();
								
								rate_event = null;
							}
						}
					}
				}
			});
		
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core) 
				{
					totalStats = StatsFactory.getStats();
				}
			});
		
		SelectedContentManager.addCurrentlySelectedContentListener(
			new SelectedContentListener()
			{
				public void 
				currentlySelectedContentChanged(
					ISelectedContent[] 	currentContent, 
					String 				viewId ) 
				{
					selection_count = currentContent.length;
					
					long	total_size 	= 0;
					long	total_done	= 0;
					
					ArrayList<DownloadManager>	dms = new ArrayList<DownloadManager>( currentContent.length );
					
					for ( ISelectedContent sc: currentContent ){
						
						DownloadManager dm = sc.getDownloadManager();
						
						if ( dm != null ){
							
							dms.add( dm );
							
							int	file_index = sc.getFileIndex();
							
							if ( file_index == -1 ){
							
								DiskManagerFileInfo[] file_infos = dm.getDiskManagerFileInfoSet().getFiles();
								
								for ( DiskManagerFileInfo file_info: file_infos ){
									
									if ( !file_info.isSkipped()){
										
										total_size 	+= file_info.getLength();
										total_done	+= file_info.getDownloaded();
									}
								}
							}else{
								
								DiskManagerFileInfo file_info = dm.getDiskManagerFileInfoSet().getFiles()[file_index];
								
								if ( !file_info.isSkipped()){
								
									total_size 	+= file_info.getLength();
									total_done	+= file_info.getDownloaded();
								}
							}
						}
					}
					
					selection_size	= total_size;
					selection_done	= total_done;
					
					selection_dms	= dms.toArray( new DownloadManager[ dms.size()]);
					
					SB_Transfers.triggerCountRefreshListeners();
				}
			});
	}
	
	private int viewMode = -1;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private int torrentFilterMode = TORRENTS_ALL;

	private String torrentFilter;

	private SWTSkinObject soWait;

	private SWTSkinObject soWaitProgress;

	private SWTSkinObjectText soWaitTask;

	private int waitProgress = 0;

	private SWTSkinObjectText soLibraryInfo;

	private Object datasource;

	private MdiEntry currentEntry;

	public void setViewMode(int viewMode, boolean save) {
		if (viewMode >= modeViewIDs.length || viewMode < 0
				|| viewMode == this.viewMode) {
			return;
		}

		if ( !COConfigurationManager.getBooleanParameter( "Library.EnableSimpleView" )){

			viewMode = MODE_SMALLTABLE;
		}
		
		int oldViewMode = this.viewMode;

		this.viewMode = viewMode;

		if (oldViewMode >= 0 && oldViewMode < modeViewIDs.length) {
			SWTSkinObject soOldViewArea = getSkinObject(modeViewIDs[oldViewMode]);
			//SWTSkinObject soOldViewArea = skin.getSkinObjectByID(modeIDs[oldViewMode]);
			if (soOldViewArea != null) {
				soOldViewArea.setVisible(false);
			}
		}

		SelectedContentManager.clearCurrentlySelectedContent();

		SWTSkinObject soViewArea = getSkinObject(modeViewIDs[viewMode]);
		if (soViewArea == null) {
			soViewArea = skin.createSkinObject(modeIDs[viewMode] + torrentFilterMode,
					modeIDs[viewMode], soListArea);
			soViewArea.getControl().setData( "SBC_LibraryView:ViewMode", viewMode );
			skin.layout();
			soViewArea.setVisible(true);
			soViewArea.getControl().setLayoutData(Utils.getFilledFormData());
		} else {
			soViewArea.setVisible(true);
		}

		if (save) {
			COConfigurationManager.setParameter(torrentFilter + ".viewmode", viewMode);
		}

		String entryID = null;
		if (torrentFilterMode == TORRENTS_ALL) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY;
		} else if (torrentFilterMode == TORRENTS_COMPLETE) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_CD;
		} else if (torrentFilterMode == TORRENTS_INCOMPLETE) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_DL;
		} else if (torrentFilterMode == TORRENTS_UNOPENED) {
			entryID = SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED;
		}
		
		if (entryID != null) {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			currentEntry = mdi.getEntry(entryID);
			if (currentEntry != null) {
				currentEntry.setLogID(entryID + "-" + viewMode);
			}
		}

		SB_Transfers.triggerCountRefreshListeners();
	}


	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(final SWTSkinObject skinObject, Object params) {
		soWait = null;
		try {
			soWait = getSkinObject("library-wait");
			soWaitProgress = getSkinObject("library-wait-progress");
			soWaitTask = (SWTSkinObjectText) getSkinObject("library-wait-task");
			if (soWaitProgress != null) {
				soWaitProgress.getControl().addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						Control c = (Control) e.widget;
						Point size = c.getSize();
						e.gc.setBackground(ColorCache.getColor(e.display, "#23a7df"));
						int breakX = size.x * waitProgress / 100;
						e.gc.fillRectangle(0, 0, breakX, size.y);
						e.gc.setBackground(ColorCache.getColor(e.display, "#cccccc"));
						e.gc.fillRectangle(breakX, 0, size.x - breakX, size.y);
					}
				});
			}

			soLibraryInfo = (SWTSkinObjectText) getSkinObject("library-info");
			
			if (soLibraryInfo != null) {

				SB_Transfers.addCountRefreshListener(
					new SB_Transfers.countRefreshListener() 
					{						
						final Map<Composite,ExtraInfoProvider>	extra_info_map = new HashMap<Composite,ExtraInfoProvider>();
						
						{
							soLibraryInfo.getControl().getParent().setData( "ViewUtils:ViewTitleExtraInfo", 
									new ViewUtils.ViewTitleExtraInfo()
									{
										public void
										update(
											Composite	reporter,
											int			count,
											int			active )
										{
											ExtraInfoProvider	provider = getProvider( reporter );
											
											if ( provider == null ){
												
												return;
											}
																																	
											if ( provider.value != count || provider.active != active ){
											
												provider.value 	= count;
												provider.active	= active;
												
												if ( viewMode == provider.view_mode && provider.enabled ){
												
													SB_Transfers.triggerCountRefreshListeners();
												}
											}
										}
											
										public void
										setEnabled(
											Composite	reporter,
											boolean		enabled )
										{
											ExtraInfoProvider	provider = getProvider( reporter );
											
											if ( provider == null ){
												
												return;
											}	
											
											if ( provider.enabled != enabled ){

												provider.enabled = enabled;
												
												if ( viewMode == provider.view_mode ){
												
													SB_Transfers.triggerCountRefreshListeners();
												}
											}
										}
										
										private ExtraInfoProvider
										getProvider(
											Composite	reporter )
										{
											synchronized( extra_info_map ){
												
												ExtraInfoProvider provider = extra_info_map.get( reporter );
												
												if ( provider != null ){
													
													return( provider );
												}
													
												Composite temp = reporter;
												
												while( temp != null ){
												
													Integer vm = (Integer)temp.getData( "SBC_LibraryView:ViewMode" );
													
													if ( vm != null ){
														
														provider = new ExtraInfoProvider( vm );
														
														extra_info_map.put( reporter, provider );
														
														return( provider );
													}
													
													temp = temp.getParent();
												}
												
												Debug.out( "No view mode found for " + reporter );
												
												return( null );
											}
										}
									});
						}
						
						// @see com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.countRefreshListener#countRefreshed(com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats, com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView.stats)
						public void 
						countRefreshed(
								SB_Transfers.stats statsWithLowNoise,
								SB_Transfers.stats statsNoLowNoise) 
						{
							SB_Transfers.stats stats = viewMode == MODE_SMALLTABLE? statsWithLowNoise : statsNoLowNoise;
							
							String s;
							
								// seeding and downloading Tag views were changed to filter appropriately
								// but that broke the header display - fixed by forcing through the 'TORRENTS_ALL'
								// leg for Tag based views
							
							if ( torrentFilterMode == TORRENTS_ALL || (datasource instanceof Tag)){
								
								if (datasource instanceof Category) {
									Category cat = (Category) datasource;

									String id = "library.category.header";
									
									s = MessageText.getString(id,
											new String[] {
											(cat.getType() != Category.TYPE_USER)
													? MessageText.getString(cat.getName())
													: cat.getName()
									});
									
								}else if (datasource instanceof Tag) {
									
									Tag tag = (Tag) datasource;

									String id = "library.tag.header";

									s = MessageText.getString(id,
											new String[] {
												tag.getTagName( true ) });
									
									String desc = tag.getDescription();
									
									if ( desc != null ){
										
										s += " - " + desc;
									}
												
								} else {
									String id = "library.all.header";
									if (stats.numComplete + stats.numIncomplete != 1) {
										id += ".p";
									}
									s = MessageText.getString(id,
											new String[] {
											String.valueOf(stats.numComplete + stats.numIncomplete),
											String.valueOf(stats.numSeeding + stats.numDownloading),
									});
								
									if ( stats.numQueued > 0 ){
										
										s += ", " + 
										MessageText.getString(
												"label.num_queued", new String[]{ String.valueOf( stats.numQueued )});
									}
								}
								
							}else if (torrentFilterMode == TORRENTS_INCOMPLETE) {
								String id = "library.incomplete.header";
								if (stats.numDownloading != 1) {
									id += ".p";
								}
								int numWaiting = Math.max( stats.numIncomplete - stats.numDownloading, 0 );
								s = MessageText.getString(id,
										new String[] {
										String.valueOf(stats.numDownloading),
										String.valueOf(numWaiting),
								});
								
							} else if ( torrentFilterMode == TORRENTS_UNOPENED ||  torrentFilterMode == TORRENTS_COMPLETE ) {
									// complete filtering currently uses same display text as unopened
								String id = "library.unopened.header";
								if (stats.numUnOpened != 1) {
									id += ".p";
								}
								s = MessageText.getString(id,
										new String[] {
										String.valueOf(stats.numUnOpened),
								});
							}else{
								
								s = "";
							}
							
							synchronized( extra_info_map ){
								
								int		filter_total 	= 0;
								int		filter_active	= 0;
								
								boolean	filter_enabled 	= false;

								for ( ExtraInfoProvider provider: extra_info_map.values()){
									
									if ( viewMode == provider.view_mode ){
							
										if ( provider.enabled ){
												
											filter_enabled = true;
											filter_total	+= provider.value;
											filter_active	+= provider.active;
										}
									}
								}
								
								if ( filter_enabled ){
									
									String extra = 
										MessageText.getString(
												"filter.header.matches2",
												new String[]{ String.valueOf( filter_total ), String.valueOf( filter_active )});
									
									s += " " + extra;
								}
							}
							
							if ( selection_count > 1 ){
								
								s += ", " + 
										MessageText.getString(
										"label.num_selected", new String[]{ String.valueOf( selection_count )});
								
								String	size_str = null;
								String	rate_str = null;
								
								if ( selection_size > 0 ){
									
									if ( selection_size == selection_done ){
										
										size_str = DisplayFormatters.formatByteCountToKiBEtc( selection_size );
										
									}else{
										
										size_str = DisplayFormatters.formatByteCountToKiBEtc( selection_done ) + "/" + DisplayFormatters.formatByteCountToKiBEtc( selection_size );

									}
								}
								
								DownloadManager[] dms = selection_dms;
								
								if ( header_show_rates && dms.length > 1 ){
										
									long	total_data_up 		= 0;
									long	total_prot_up 		= 0;
									long	total_data_down		= 0;
									long	total_prot_down		= 0;
									
									for ( DownloadManager dm: dms ){
										
										DownloadManagerStats dm_stats = dm.getStats();
										
										total_prot_up += dm_stats.getProtocolSendRate();
										total_data_up += dm_stats.getDataSendRate();
										total_prot_down += dm_stats.getProtocolReceiveRate();
										total_data_down += dm_stats.getDataReceiveRate();
									}
								
									rate_str = 
											MessageText.getString( "ConfigView.download.abbreviated") + DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(total_data_down, total_prot_down) + " " +
											MessageText.getString( "ConfigView.upload.abbreviated") + DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(total_data_up, total_prot_up);
								}
								
								if ( size_str != null || rate_str != null ){
									
									String temp;
									
									if ( size_str == null ){
									
										temp = rate_str;
										
									}else if ( rate_str == null ){
										
										temp = size_str;
										
									}else{
										
										temp = size_str + "; " + rate_str;
									}
									
									s += " (" + temp + ")";
								}
							}

							if ( header_show_uptime && totalStats != null ){
								
								long up_secs = (totalStats.getSessionUpTime()/60)*60;
								
								String	op;
								
								if ( up_secs < 60 ){
									
									up_secs = 60;
									
									op = "<";
									
								}else{
									
									op = " ";
								}
								
								String up_str = TimeFormatter.format2( up_secs, false );
									
								if ( s.equals( "" )){
									Debug.out( "eh" );
								}
								s += "; " + 
									MessageText.getString(
										"label.uptime_coarse",
										new String[]{ op, up_str } );
							}
														
							soLibraryInfo.setText(s);
						}
						
						class
						ExtraInfoProvider
						{	
							int			view_mode;
							boolean		enabled;
							int			value;
							int			active;
							
							private
							ExtraInfoProvider(
								int	vm )
							{
								view_mode	= vm;
							}
						}
					});
				
			}
		} catch (Exception e) {
		}

		//AzureusCore core = AzureusCoreFactory.getSingleton();
		if (!AzureusCoreFactory.isCoreRunning()) {
			if (soWait != null) {
				soWait.setVisible(true);
				//soWait.getControl().getParent().getParent().getParent().layout(true, true);
			}
			final Initializer initializer = Initializer.getLastInitializer();
			if (initializer != null) {
				initializer.addListener(new InitializerListener() {
					public void reportPercent(final int percent) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (soWaitProgress != null && !soWaitProgress.isDisposed()) {
									waitProgress = percent;
									soWaitProgress.getControl().redraw();
									soWaitProgress.getControl().update();
								}
							}
						});
						if (percent > 100) {
							initializer.removeListener(this);
						}
					}

					public void reportCurrentTask(String currentTask) {
						if (soWaitTask != null && !soWaitTask.isDisposed()) {
							soWaitTask.setText(currentTask);
						}
					}
				});
			}
		}

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				PluginInterface pi = PluginInitializer.getDefaultInterface();
				final UIManager uim = pi.getUIManager();
				uim.addUIListener(new UIManagerListener() {
					public void UIDetached(UIInstance instance) {
					}

					public void UIAttached(UIInstance instance) {
						if (instance instanceof UISWTInstance) {
							uim.removeUIListener(this);
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									if (soWait != null) {
										soWait.setVisible(false);
									}
									if ( !skinObject.isDisposed()){
									
										setupView(core, skinObject);
									}
								}
							});
						}
					}
				});
			}
		});

		return null;
	}
	
	

	private void setupView(AzureusCore core, SWTSkinObject skinObject) {
		torrentFilter = skinObject.getSkinObjectID();
		if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_DL)) {
			torrentFilterMode = TORRENTS_INCOMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_CD)) {
			torrentFilterMode = TORRENTS_COMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED)) {
			torrentFilterMode = TORRENTS_UNOPENED;
		}
		
		if (datasource instanceof Tag) {
			Tag tag = (Tag) datasource;
			TagType tagType = tag.getTagType();
			if (tagType.getTagType() == TagType.TT_DOWNLOAD_STATE) {
				int tagID = tag.getTagID(); // see GlobalManagerImp.tag_*
				if (tagID == 1 || tagID == 3 || tagID == 11) {
					torrentFilterMode = TORRENTS_INCOMPLETE;
				} else if (tagID == 2 || tagID == 4 || tagID == 10) {
					torrentFilterMode = TORRENTS_COMPLETE;
				}
			}
		}

		soListArea = getSkinObject(ID + "-area");

		soListArea.getControl().setData("TorrentFilterMode",
				new Long(torrentFilterMode));
		soListArea.getControl().setData("DataSource", datasource);

		setViewMode(
				COConfigurationManager.getIntParameter(torrentFilter + ".viewmode"),
				false);

		SWTSkinObject so;
		so = getSkinObject(ID + "-button-smalltable");
		if (so != null) {
			btnSmallTable = new SWTSkinButtonUtility(so);
			btnSmallTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_SMALLTABLE, true);
				}
			});
		}

		so = getSkinObject(ID + "-button-bigtable");
		if (so != null) {
			btnBigTable = new SWTSkinButtonUtility(so);
			btnBigTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_BIGTABLE, true);
				}
			});
		} 
		
		SB_Transfers.setupViewTitleWithCore(core);
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		long stateSmall = UIToolBarItem.STATE_ENABLED;
		long stateBig = UIToolBarItem.STATE_ENABLED;
		if (viewMode == MODE_BIGTABLE) {
			stateBig |= UIToolBarItem.STATE_DOWN;
		} else {
			stateSmall |= UIToolBarItem.STATE_DOWN;
		}
		list.put("modeSmall", stateSmall);
		list.put("modeBig", stateBig);
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		String itemKey = item.getID();

		if (itemKey.equals("modeSmall")) {
			if (isVisible()) {
				setViewMode(MODE_SMALLTABLE, true);
				return true;
			}
		}
		if (itemKey.equals("modeBig")) {
			if (isVisible()) {
				setViewMode(MODE_BIGTABLE, true);
				return true;
			}
		}
		return false;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		return super.skinObjectHidden(skinObject, params);
	}
	
	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		datasource = params;
		if (soListArea != null) {
			Control control = soListArea.getControl();
  		
			if ( !control.isDisposed()){
			
				control.setData("DataSource", params);
			}
		}
		
		return null;
	}

	public int getViewMode() {
		return viewMode;
	}

	protected void
	addHeaderInfoExtender(
		HeaderInfoExtender	extender )
	{
		
	}
	
	protected void
	removeHeaderInfoExtender(
		HeaderInfoExtender	extender )
	{
		
	}
	
	protected void
	refreshHeaderInfo()
	{
		SB_Transfers.triggerCountRefreshListeners();
	}
	
	protected interface
	HeaderInfoExtender
	{
		
	}
}
