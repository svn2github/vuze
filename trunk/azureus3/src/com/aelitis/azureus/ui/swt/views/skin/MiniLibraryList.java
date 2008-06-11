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

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
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

	private SWTSkinObjectText soTitle;

	private SWTSkinObject soData;

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		soData = skinObject;
		SelectedContentManager.changeCurrentlySelectedContent(PREFIX, null);

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
				lblCountArea, soData, PREFIX, TorrentListView.VIEW_MY_MEDIA, true,
				true);
		
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

		return null;
	}

	public TorrentListView getView() {
		return view;
	}
}
