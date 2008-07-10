/**
 * Created on Jul 3, 2008
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.IconBar;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.utils.UIUpdatable;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * Classic My Torrents view with IconBar wrapped in a SkinView
 * 
 * @author TuxPaper
 * @created Jul 3, 2008
 *
 */
public class SBC_LibraryTableView
	extends SkinView
	implements UIUpdatable
{
	private final static String ID = "SBC_LibraryTableView";

	private IView view;

	private Composite viewComposite;

	private IconBar iconBar;

	private int torrentFilterMode = SBC_LibraryView.TORRENTS_ALL;

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		Object data = skinObject.getParent().getControl().getData(
				"TorrentFilterMode");
		if (data instanceof Long) {
			torrentFilterMode = (int) ((Long) data).longValue();
		}

		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			view = new MyTorrentsView(
					AzureusCoreFactory.getSingleton(),
					true,
					TableColumnCreator.createIncompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE));
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			view = new MyTorrentsView(
					AzureusCoreFactory.getSingleton(),
					false,
					TableColumnCreator.createIncompleteDM(TableManager.TABLE_MYTORRENTS_INCOMPLETE));
		} else {
			view = new MyTorrentsSuperView();
		}

		SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
				skin.getSkinProperties(), ID, "", soMain);

		skin.layout();

		viewComposite = soContents.getComposite();
		viewComposite.setBackground(viewComposite.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		viewComposite.setForeground(viewComposite.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_FOREGROUND));
		viewComposite.setLayoutData(Utils.getFilledFormData());
		viewComposite.setLayout(new GridLayout());

		view.initialize(viewComposite);

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		Object o = super.skinObjectShown(skinObject, params);

		SWTSkinObject so = skin.getSkinObject("library-list-bottom");
		if (so != null && iconBar == null) {
			iconBar = new IconBar((Composite) so.getControl());
			iconBar.setLayoutData(Utils.getFilledFormData());
			iconBar.getComposite().getParent().layout();
			iconBar.setCurrentEnabler(view);
		}

		return o;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#hide(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		if (iconBar != null) {
			iconBar.delete();
			iconBar = null;
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return ID;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (view != null) {
			view.refresh();
			if (iconBar != null) {
				iconBar.setCurrentEnabler(view);
			}
		}
	}
}
