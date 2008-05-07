/**
 * Created on Jan 28, 2008 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.currentlyselectedcontent.CurrentContent;
import com.aelitis.azureus.ui.swt.currentlyselectedcontent.CurrentlySelectedContentManager;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;

/**
 * @author TuxPaper
 * @created Jan 26, 2008
 *
 */
public class MiniLibraryList
extends SkinView
{
	private static String PREFIX = "minilibrary-";

	private TorrentListView view;

	private SWTSkinButtonUtility btnAdd;

	private SWTSkinButtonUtility btnStop;

	private SWTSkinButtonUtility btnDelete;

	private SWTSkinButtonUtility btnDetails;

	private SWTSkinButtonUtility btnPlay;

	private SWTSkinButtonUtility btnShare;

	private SWTSkinButtonUtility btnComments;

	private SWTSkinButtonUtility btnColumnSetup;

	private SWTSkinObjectText soTitle;

	private SWTSkinObject soData;

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		soData = skinObject;
		soData.addListener(new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
					CurrentlySelectedContentManager.changeCurrentlySelectedContent(getCurrentlySelectedContent());
				}
				return null;
			}
		});

		final SWTSkin skin = skinObject.getSkin();
		AzureusCore core = AzureusCoreFactory.getSingleton();

		Composite cData = (Composite) skinObject.getControl();
		Composite cHeaders = null;
		SWTSkinObjectText lblCountArea = null;

		skinObject = skin.getSkinObject(PREFIX + "list-headers");
		if (skinObject != null) {
			cHeaders = (Composite) skinObject.getControl();
		}

		skinObject = skin.getSkinObject(PREFIX + "titlextra");
		if (skinObject instanceof SWTSkinObjectText) {
			lblCountArea = (SWTSkinObjectText) skinObject;
		}

		view = new TorrentListView(core, skin, skin.getSkinProperties(), cHeaders,
				lblCountArea, cData, TorrentListView.VIEW_MY_MEDIA, true, true);
		
		view.addSelectionListener(new TableSelectionAdapter() {
			public void selected(TableRowCore[] row) {
				selectionChanged();
			}
		
			public void deselected(TableRowCore[] rows) {
				selectionChanged();
			}
		
			public void selectionChanged() {
				if (soData.isVisible()) {
					CurrentlySelectedContentManager.changeCurrentlySelectedContent(getCurrentlySelectedContent());
				}
			}
		}, false);
		
		if (Constants.isCVSVersion()) {
  		SWTSkinObject skinObjectTab = skin.getSkinObject(SkinConstants.VIEWID_MINILIBRARY_TAB);
  		if (skinObjectTab instanceof SWTSkinObjectContainer){
  			SWTSkinObjectContainer soTab = (SWTSkinObjectContainer) skinObjectTab;
  			SWTSkinObject[] children = soTab.getChildren();
  			for (int i = 0; i < children.length; i++) {
  				SWTSkinObject child = children[i];
  				if (child instanceof SWTSkinObjectText) {
  					soTitle = (SWTSkinObjectText) child;
  					break;
  				}
  			}
  		}
  
  
  		if (soTitle != null) {
    		view.addListener(new TorrentListViewListener() {
    			public void countChanged() {
    				String s = MessageText.getString("v3.MainWindow.tab.minilibrary");
    				int count = view.size(false);
    				if (count > 0) {
    					s += " - " + count;
    				}
    				soTitle.setText(s);
    			}
    		});
  		}
		}

		skinObject = skin.getSkinObject(PREFIX + "add");
		if (skinObject instanceof SWTSkinObject) {
			btnAdd = new SWTSkinButtonUtility(skinObject);

			btnAdd.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					TorrentOpener.openTorrentWindow();
				}
			});
		}

		btnColumnSetup = TorrentListViewsUtils.addColumnSetupButton(skin, PREFIX, view);
		
		btnShare = TorrentListViewsUtils.addShareButton(skin, PREFIX, view);
		btnStop = TorrentListViewsUtils.addStopButton(skin, PREFIX, view);
		btnDetails = TorrentListViewsUtils.addDetailsButton(skin, PREFIX, view);
		btnComments = TorrentListViewsUtils.addCommentsButton(skin, PREFIX, view);
		btnPlay = TorrentListViewsUtils.addPlayButton(skin, PREFIX, view, true,
				true);
		btnDelete = TorrentListViewsUtils.addDeleteButton(skin, PREFIX, view);

		SWTSkinButtonUtility[] buttonsNeedingRow = {
			btnDelete,
		};
		SWTSkinButtonUtility[] buttonsNeedingPlatform = {
			btnDetails,
			btnComments,
			btnShare,
		};
		SWTSkinButtonUtility[] buttonsNeedingSingleSelection = {
			btnDetails,
			btnComments,
			btnShare,
		};
		TorrentListViewsUtils.addButtonSelectionDisabler(view, buttonsNeedingRow,
				buttonsNeedingPlatform, buttonsNeedingSingleSelection, btnStop);

		return null;
	}

	public TorrentListView getView() {
		return view;
	}
	
	public CurrentContent[] getCurrentlySelectedContent() {
		List listContent = new ArrayList();
		Object[] selectedDataSources = view.getSelectedDataSources(true);
		for (int i = 0; i < selectedDataSources.length; i++) {
			DownloadManager dm = (DownloadManager) selectedDataSources[i];
			if (dm != null) {
				CurrentContent currentContent = new CurrentContent(dm);
				listContent.add(currentContent);
			}
		}
		return (CurrentContent[]) listContent.toArray(new CurrentContent[listContent.size()]);
	}
}
