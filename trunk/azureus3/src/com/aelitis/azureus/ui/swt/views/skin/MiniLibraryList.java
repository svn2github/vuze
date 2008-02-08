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

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.TorrentListView;

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

	public Object showSupport(SWTSkinObject skinObject, Object params) {
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

		skinObject = skin.getSkinObject(PREFIX + "add");
		if (skinObject instanceof SWTSkinObjectContainer) {
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
			btnStop,
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
}
