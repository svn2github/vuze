/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.views.TorrentListView;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 * TODO Code similaries between MiniRecentList, MiniDownloadList, ManageCdList, 
 *     and ManageDlList.  Need to combine
 */
public class MiniDownloadList
	extends SkinView
{
	private static String PREFIX = "minidownload-";

	private TorrentListView view;

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		final SWTSkin skin = skinObject.getSkin();
		AzureusCore core = AzureusCoreFactory.getSingleton();

		SWTSkinObject soData = skinObject;
		
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
				lblCountArea, soData, PREFIX, TorrentListView.VIEW_DOWNLOADING, true,
				true);

		return null;
	}
}
