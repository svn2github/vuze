/**
 * Created on Jan 8, 2009
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

package com.aelitis.azureus.ui.swt.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI.ContentNetworkImageLoadedListener;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.ContentNetworkUtils;

/**
 * @author TuxPaper
 * @created Jan 8, 2009
 *
 */
public class ContentNetworkUIManagerWindow
{
	public ContentNetworkUIManagerWindow() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				open();
			}
		});
	}

	protected void open() {
		final SkinnedDialog dlg = new SkinnedDialog("skin3_manageCN",
				"manageCN.body");
		SWTSkin skin = dlg.getSkin();

		SWTSkinObjectButton soButton = (SWTSkinObjectButton) skin.getSkinObject("close");
		if (soButton != null) {
			soButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					dlg.close();
				}
			});
		}

		SWTSkinObjectContainer soListArea = (SWTSkinObjectContainer) skin.getSkinObject("list-area");
		if (soListArea != null) {
			Composite parent = (Composite) soListArea.getControl();
			ContentNetwork[] networks = ContentNetworkManagerFactory.getSingleton().getContentNetworks();
			Button lastButton = null; 
			for (int i = 0; i < networks.length; i++) {
				final ContentNetwork cn = networks[i];
				Object prop = cn.getProperty(ContentNetwork.PROPERTY_REMOVEABLE);
				boolean removable = (prop instanceof Boolean)
						? ((Boolean) prop).booleanValue() : false;
				if (removable) {
					final Button button = new Button(parent, SWT.CHECK);
					button.setText(cn.getName());

					prop = cn.getPersistentProperty(ContentNetwork.PP_SHOW_IN_MENU);
					boolean show = (prop instanceof Boolean)
							? ((Boolean) prop).booleanValue() : true;

					button.setSelection(show);
					
					FormData fd = new FormData();
					if (lastButton != null) {
						fd.top = new FormAttachment(lastButton, 3);
					} else {
						fd.top = new FormAttachment(0, 5);
					}
					fd.left = new FormAttachment(0, 5);
					fd.right = new FormAttachment(100, -5);
					button.setLayoutData(fd);
					

					ContentNetworkUI.loadImage(cn.getID(),
							new ContentNetworkImageLoadedListener() {
								public void contentNetworkImageLoaded(Long contentNetworkID,
										Image image, boolean wasReturned) {
									if (image != null && image.getBounds().height < 50) {
										button.setImage(image);
									}
								}
							});

					lastButton = button;

					button.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							Button button = (Button) e.widget;
							boolean show = button.getSelection();
							cn.setPersistentProperty(ContentNetwork.PP_SHOW_IN_MENU,
									new Boolean(show));
							if (!show) {
								cn.setPersistentProperty(ContentNetwork.PP_AUTH_PAGE_SHOWN,
										Boolean.FALSE);
								// turn off notification window
								cn.setPersistentProperty(ContentNetwork.PP_ACTIVE,
										Boolean.FALSE);

								SideBarEntrySWT entry = SideBar.getEntry(ContentNetworkUtils.getTarget(cn));
								if (entry.isInTree()) {
									entry.getSidebar().closeEntry(entry.getId());
								}
							} else {
								// Uncomment to bring up sidebar entry on checking option
								//String target = ContentNetworkUtils.getTarget(cn);
								//SideBarEntrySWT entry = SideBar.getEntry(target);
								//if (!entry.isInTree()) {
								//	entry.getSidebar().showEntryByTabID(target);
								//}
							}
						}

						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
				}
			}
		}

		dlg.addCloseListener(new SkinnedDialogClosedListener() {
			public void skinDialogClosed(SkinnedDialog dialog) {
			}
		});

		dlg.open();
	}
}
