/**
 * Created on Jul 20, 2008
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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 *
 * 3.2 TODO: Link in az3 buttons
 * 3.2 TODO: Implement az2 button actions
 * 3.2 TODO: Implement disabling
 */
public class ToolBarView
	extends SkinView
{
	private static toolbarButtonListener buttonListener;

	private Map items = new LinkedHashMap();

	ToolBarItem lastItem = null;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		buttonListener = new toolbarButtonListener();

		// ==NEW
		ToolBarItem item;
		item = new ToolBarItem("open", "image.toolbar.open") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==OPEN
		item = new ToolBarItem("new", "image.toolbar.new") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==TOP
		item = new ToolBarItem("top", "image.toolbar.top") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==UP
		item = new ToolBarItem("up", "image.toolbar.up") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==down
		item = new ToolBarItem("down", "image.toolbar.down") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==bottom
		item = new ToolBarItem("bottom", "image.toolbar.bottom") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==run
		item = new ToolBarItem("run", "image.toolbar.run") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==start
		item = new ToolBarItem("start", "image.toolbar.start") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==stop
		item = new ToolBarItem("stop", "image.toolbar.stop") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		// ==remove
		item = new ToolBarItem("remove", "image.toolbar.remove") {
			public void triggerToolBarItem() {
				Utils.openMessageBox(null, SWT.OK, "Hi", getId());
			}
		};
		addToolBarItem(item);

		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
			public void currentlySectedContentChanged(
					ISelectedContent[] currentContent) {
				String[] itemsNeedingSelection = {
					"up",
					"down",
					"top",
					"bottom",
					"run",
					"remove"
				};

				//int numSelection = currentContent.length;
				boolean hasSelection = currentContent.length > 0;
				ToolBarItem item;
				for (int i = 0; i < itemsNeedingSelection.length; i++) {
					String itemID = itemsNeedingSelection[i];
					item = getToolBarItem(itemID);

					if (item != null) {
						item.setEnabled(hasSelection);
					}
				}

				boolean canStart = false;
				boolean canStop = false;
				for (int i = 0; i < currentContent.length; i++) {
					ISelectedContent content = currentContent[i];
					DownloadManager dm = content.getDM();
					if (!canStart && ManagerUtils.isStartable(dm)) {
						canStart = true;
					}
					if (!canStop && ManagerUtils.isStopable(dm)) {
						canStop = true;
					}
				}
				item = getToolBarItem("start");
				if (item != null) {
					item.setEnabled(canStart);
				}
				item = getToolBarItem("stop");
				if (item != null) {
					item.setEnabled(canStop);
				}
				
			}
		});

		return null;
	}

	/**
	 * @param itemID
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	protected ToolBarItem getToolBarItem(String itemID) {
		return (ToolBarItem) items.get(itemID);
	}

	public void addToolBarItem(final ToolBarItem item) {
		SWTSkinObject so = skin.createSkinObject("toolbar:" + item.getId(),
				"toolbar.area.item", soMain);
		if (so != null) {
			if (lastItem != null) {
				Control lastControl = lastItem.getSkinButton().getSkinObject().getControl();
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl);
			}

			so.setData("toolbaritem", item);
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so);
			btn.setImage(item.getImageID());
			btn.addSelectionListener(buttonListener);
			item.setSkinButton(btn);
			Utils.relayout(so.getControl().getParent());

			lastItem = item;
			items.put(item.getId(), item);
		}
	}

	private static class toolbarButtonListener
		extends ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.triggerToolBarItem();
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.setEnabled(!disabled);
		}
	}
}
