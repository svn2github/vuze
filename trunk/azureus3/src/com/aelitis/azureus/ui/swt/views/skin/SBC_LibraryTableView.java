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

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;

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
	implements UIUpdatable, IconBarEnabler
{
	private final static String ID = "SBC_LibraryTableView";

	private IView view;

	private Composite viewComposite;

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

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return ID;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (view != null) {
			view.refresh();
		}
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		try {
  		if (view != null) {
  			return view.isEnabled(itemKey);
  		}
		} catch (Throwable t) {
			Debug.out(t);
		}
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		try {
  		if (view != null) {
  			return view.isSelected(itemKey);
  		}
		} catch (Throwable t) {
			Debug.out(t);
		}
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(String itemKey) {
		try {
  		if (view != null) {
  			view.itemActivated(itemKey);
  		}
		} catch (Throwable t) {
			Debug.out(t);
		}
	}
}
