/**
 * Created on Nov 15, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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

package org.gudy.azureus2.ui.swt.views.utils;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.ViewUtils;
import org.gudy.azureus2.ui.swt.views.ViewUtils.SpeedAdapter;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDownload;
import com.aelitis.azureus.core.tag.TagFeature;
import com.aelitis.azureus.core.tag.TagFeatureRateLimit;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * @author TuxPaper
 * @created Nov 15, 2010
 *
 */
public class TagUIUtils
{
	public static void setupCategoryMenu(final Menu menu, final Tag tag) {
		menu.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX)
					return;

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)

				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed())
							return;
						MenuItem[] items = menu.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++)
					items[i].dispose();

				bShown = true;

				createMenuItems(menu, tag);
			}
		});
	}

	public static void createMenuItems(final Menu menu, final Tag tag) {

		TagType	tag_type = tag.getTagType();
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )) {

			final TagFeatureRateLimit	tf_rate_limit = (TagFeatureRateLimit)tag;
			
			boolean	has_up 		= tf_rate_limit.supportsTagUploadLimit();
			boolean	has_down 	= tf_rate_limit.supportsTagDownloadLimit();
			
			if ( has_up || has_down ){
				
				long maxDownload = COConfigurationManager.getIntParameter(
						"Max Download Speed KBs", 0) * 1024;
				long maxUpload = COConfigurationManager.getIntParameter(
						"Max Upload Speed KBs", 0) * 1024;
	
				int down_speed 	= tf_rate_limit.getTagDownloadLimit();
				int up_speed 	= tf_rate_limit.getTagUploadLimit();
	
				ViewUtils.addSpeedMenu(menu.getShell(), menu, has_up, has_down, true, true, false,
						down_speed == 0, down_speed, down_speed, maxDownload, false,
						up_speed == 0, up_speed, up_speed, maxUpload, 1, new SpeedAdapter() {
							public void setDownSpeed(int val) {
								tf_rate_limit.setTagDownloadLimit(val);
							}
	
							public void setUpSpeed(int val) {
								tf_rate_limit.setTagUploadLimit(val);
	
							}
						});
			}
		}

		/*
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		List<?> managers = category.getDownloadManagers(gm.getDownloadManagers());

		final DownloadManager dms[] = managers.toArray(new DownloadManager[managers.size()]);

		boolean start = false;
		boolean stop = false;

		for (int i = 0; i < dms.length; i++) {

			DownloadManager dm = dms[i];

			stop = stop || ManagerUtils.isStopable(dm);

			start = start || ManagerUtils.isStartable(dm);

		}

		// Queue

		final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue");
		Utils.setMenuItemImage(itemQueue, "start");
		itemQueue.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
				List<?> managers = category.getDownloadManagers(gm.getDownloadManagers());

				Object[] dms = managers.toArray();
				TorrentUtil.queueDataSources(dms, true);
			}
		});
		itemQueue.setEnabled(start);

		// Stop

		final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop");
		Utils.setMenuItemImage(itemStop, "stop");
		itemStop.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
				List<?> managers = category.getDownloadManagers(gm.getDownloadManagers());

				Object[] dms = managers.toArray();
				TorrentUtil.stopDataSources(dms);
			}
		});
		itemStop.setEnabled(stop);

		// share with friends

		PluginInterface bpi = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaceByClass(
				BuddyPlugin.class);

		int cat_type = category.getType();

		if (bpi != null && cat_type != Category.TYPE_UNCATEGORIZED) {

			final BuddyPlugin buddy_plugin = (BuddyPlugin) bpi.getPlugin();

			if (buddy_plugin.isEnabled()) {

				final Menu share_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
				final MenuItem share_item = new MenuItem(menu, SWT.CASCADE);
				Messages.setLanguageText(share_item, "azbuddy.ui.menu.cat.share");
				share_item.setMenu(share_menu);

				List<BuddyPluginBuddy> buddies = buddy_plugin.getBuddies();

				if (buddies.size() == 0) {

					final MenuItem item = new MenuItem(share_menu, SWT.CHECK);

					item.setText(MessageText.getString("general.add.friends"));

					item.setEnabled(false);

				} else {
					final String cname;

					if (cat_type == Category.TYPE_ALL) {

						cname = "All";

					} else {

						cname = category.getName();
					}

					final boolean is_public = buddy_plugin.isPublicCategory(cname);

					final MenuItem itemPubCat = new MenuItem(share_menu, SWT.CHECK);

					Messages.setLanguageText(itemPubCat, "general.all.friends");

					itemPubCat.setSelection(is_public);

					itemPubCat.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							if (is_public) {

								buddy_plugin.removePublicCategory(cname);

							} else {

								buddy_plugin.addPublicCategory(cname);
							}
						}
					});

					new MenuItem(share_menu, SWT.SEPARATOR);

					for (final BuddyPluginBuddy buddy : buddies) {

						if (buddy.getNickName() == null) {

							continue;
						}

						final boolean auth = buddy.isLocalRSSCategoryAuthorised(cname);

						final MenuItem itemShare = new MenuItem(share_menu, SWT.CHECK);

						itemShare.setText(buddy.getName());

						itemShare.setSelection(auth || is_public);

						if (is_public) {

							itemShare.setEnabled(false);
						}

						itemShare.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
								if (auth) {

									buddy.removeLocalAuthorisedRSSCategory(cname);

								} else {

									buddy.addLocalAuthorisedRSSCategory(cname);
								}
							}
						});

					}
				}
			}
		}

		// auto-transcode

		AZ3Functions.provider provider = AZ3Functions.getProvider();

		if (provider != null && category.getType() != Category.TYPE_ALL) {

			AZ3Functions.provider.TranscodeTarget[] tts = provider.getTranscodeTargets();

			if (tts.length > 0) {

				final Menu t_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
				final MenuItem t_item = new MenuItem(menu, SWT.CASCADE);
				Messages.setLanguageText(t_item, "cat.autoxcode");
				t_item.setMenu(t_menu);

				String existing = category.getStringAttribute(Category.AT_AUTO_TRANSCODE_TARGET);

				for (AZ3Functions.provider.TranscodeTarget tt : tts) {

					AZ3Functions.provider.TranscodeProfile[] profiles = tt.getProfiles();

					if (profiles.length > 0) {

						final Menu tt_menu = new Menu(t_menu.getShell(), SWT.DROP_DOWN);
						final MenuItem tt_item = new MenuItem(t_menu, SWT.CASCADE);
						tt_item.setText(tt.getName());
						tt_item.setMenu(tt_menu);

						for (final AZ3Functions.provider.TranscodeProfile tp : profiles) {

							final MenuItem p_item = new MenuItem(tt_menu, SWT.CHECK);

							p_item.setText(tp.getName());

							boolean	selected = existing != null	&& existing.equals(tp.getUID());
							
							if ( selected ){
								
								Utils.setMenuItemImage(tt_item, "blackdot");
							}
							p_item.setSelection(selected );

							p_item.addListener(SWT.Selection, new Listener() {
								public void handleEvent(Event event) {
									category.setStringAttribute(
											Category.AT_AUTO_TRANSCODE_TARGET, p_item.getSelection()
													? tp.getUID() : null);
								}
							});
						}
					}
				}
			}
		}

		// rss feed
		
		final MenuItem rssOption = new MenuItem(menu, SWT.CHECK );

		rssOption.setSelection( category.getBooleanAttribute( Category.AT_RSS_GEN ));
		
		Messages.setLanguageText(rssOption, "cat.rss.gen");
		rssOption.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean set = rssOption.getSelection();
				category.setBooleanAttribute( Category.AT_RSS_GEN, set );
			}
		});
		
		// upload priority
		
		if ( 	cat_type != Category.TYPE_UNCATEGORIZED &&
				cat_type != Category.TYPE_ALL ){
			
			final MenuItem upPriority = new MenuItem(menu, SWT.CHECK );
	
			upPriority.setSelection( category.getIntAttribute( Category.AT_UPLOAD_PRIORITY ) > 0 );
			
			Messages.setLanguageText(upPriority, "cat.upload.priority");
			upPriority.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					boolean set = upPriority.getSelection();
					category.setIntAttribute( Category.AT_UPLOAD_PRIORITY, set?1:0 );
				}
			});
		}
		
		*/
		
		// options

		if ( tag instanceof TagDownload ){
			
			MenuItem itemOptions = new MenuItem(menu, SWT.PUSH);
	
			final List<DownloadManager> dms = ((TagDownload)tag).getTaggedDownloads();

			Messages.setLanguageText(itemOptions, "cat.options");
			itemOptions.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	
					uiFunctions.openView(UIFunctions.VIEW_DM_MULTI_OPTIONS, dms.toArray( new DownloadManager[dms.size()]));
				}
			});
	
			if (dms.size() == 0) {
	
				itemOptions.setEnabled(false);
			}
		}
		
		if ( !tag.getTagType().isTagTypeAuto()){
			
			new MenuItem( menu, SWT.SEPARATOR);
			
			MenuItem itemRename = new MenuItem(menu, SWT.PUSH);
						
			Messages.setLanguageText(itemRename, "MyTorrentsView.menu.rename");
			itemRename.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
							"TagRenameWindow.title", "TagRenameWindow.message");
					
					entryWindow.setPreenteredText( tag.getTagName( true ), false );
					entryWindow.selectPreenteredText( true );
					
					entryWindow.prompt();
					
					if ( entryWindow.hasSubmittedInput()){
						
						try{
							tag.setTagName( entryWindow.getSubmittedInput().trim());
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			});
			
			MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
			
			Utils.setMenuItemImage(itemDelete, "delete");
			
			Messages.setLanguageText(itemDelete, "FileItem.delete");
			itemDelete.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tag.removeTag();
				}
			});
		}
	}
}
