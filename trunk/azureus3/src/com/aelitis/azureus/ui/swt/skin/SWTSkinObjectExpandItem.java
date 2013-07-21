/*
 * Created on Jun 21, 2006 1:22:57 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author TuxPaper
 * @created Jun 21, 2006
 *
 */
public class SWTSkinObjectExpandItem
	extends SWTSkinObjectContainer
	implements ExpandListener
{
	private ExpandItem expandItem;

	private boolean expanded;

	private boolean textOverride;

	private Composite composite;

	private boolean fillHeight;

	public SWTSkinObjectExpandItem(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "expanditem", parent);

		createExpandItem();
	}

	@SuppressWarnings("deprecation")
	private void createExpandItem() {
		if (!(parent instanceof SWTSkinObjectExpandBar)) {
			return;
		}

		final SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;

		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}

		final ExpandBar expandBar = soExpandBar.getExpandbar();
		expandBar.addExpandListener(this);

		expandItem = new ExpandItem(expandBar, style);

		String lastExpandStateID = "ui.skin." + sConfigID + ".expanded";
		if (COConfigurationManager.hasParameter(lastExpandStateID, true)) {
			boolean lastExpandState = COConfigurationManager.getBooleanParameter(
					lastExpandStateID, false);
			setExpanded(lastExpandState);
		} else if (properties.getBooleanValue(sConfigID + ".expanded", false)) {
			setExpanded(true);
		}

		composite = createComposite(soExpandBar.getComposite());
		expandItem.setControl(composite);
		composite.setLayoutData(null);
		composite.setData("skin.layedout", true);

		soExpandBar.addExpandItem(this);

		expandItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				soExpandBar.removeExpandItem(SWTSkinObjectExpandItem.this);
			}
		});

		//		composite.addListener(SWT.Modify, new Listener() {
		//			public void handleEvent(Event event) {
		//				System.out.println(SWTSkinObjectExpandItem.this + "] composite modify");
		//				SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
		//				soExpandBar.handleResize(expandItem);
		//			}
		//		});
	}

	protected void resizeComposite() {
		//System.out.println(SWTSkinObjectExpandItem.this + "] resize "
		//		+ composite.getSize() + ";" + Debug.getCompressedStackTrace());
		SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
		final ExpandBar expandBar = soExpandBar.getExpandbar();
		if (composite.isDisposed()) {
			return;
		}

		if (!composite.isVisible()) {
			return;
		}

		Rectangle clientArea = expandBar.getClientArea();

		int newHeight;
		if (properties.getBooleanValue(sConfigID + ".fillheight", false)) {
			Control[] children = expandBar.getChildren();
			Rectangle lastItemBounds = children[children.length - 1].getBounds();
			if (!children[children.length - 1].isVisible()) {
				lastItemBounds.height = 0;
			}

			newHeight = clientArea.height
					- (lastItemBounds.y + lastItemBounds.height)
					+ composite.getClientArea().height - (expandBar.getSpacing());
			//			System.out.println("fill " + clientArea + ";last=" + lastItemBounds
			//					+ " to " + newHeight);
		} else {
			newHeight = composite.computeSize(clientArea.width, SWT.DEFAULT, true).y;
			expandBar.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		}

		if (expandItem.getHeight() != newHeight) {
			expandItem.setHeight(newHeight);
		}
	}

	public ExpandItem getExpandItem() {
		return expandItem;
	}

	public boolean isExpanded() {
		return expanded;
	}

	private void setExpandedVariable(boolean expand) {
		expanded = expand;
		String lastExpandStateID = "ui.skin." + sConfigID + ".expanded";
		COConfigurationManager.setParameter(lastExpandStateID, expand);
	}

	public void setExpanded(final boolean expand) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				expandItem.setExpanded(expand);
				setExpandedVariable(expand);
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
						soExpandBar.handleResize(expandItem);
					}
				});
			}
		});
	}

	public void itemCollapsed(ExpandEvent e) {
		if (e.item == expandItem) {
			setExpandedVariable(false);

			Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
					soExpandBar.handleResize(expandItem);
				}
			});
		}
	}

	public void itemExpanded(ExpandEvent e) {
		if (e.item == expandItem) {
			setExpandedVariable(true);
			Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
					soExpandBar.handleResize(expandItem);
				}
			});
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String, int, boolean)
	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";
		String text = properties.getStringValue(sPrefix + suffix);
		if (text != null) {
			setText(text, true);
		}

		fillHeight = properties.getBooleanValue(sConfigID + ".fillheight", false);

		return suffix;
	}

	public void setText(final String text) {
		setText(text, false);
	}

	/**
	 * @param text
	 *
	 * @since 3.1.1.1
	 */
	private void setText(final String text, boolean auto) {
		if (!auto) {
			textOverride = true;
		} else if (textOverride) {
			return;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (expandItem != null && !expandItem.isDisposed()) {
					expandItem.setText(text);
				}
			}
		});

	}

	public boolean fillsHeight() {
		return fillHeight;
	}

	public void dispose() {
		super.dispose();
		if (parent instanceof SWTSkinObjectExpandBar) {
			SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
			ExpandBar expandbar = soExpandBar.getExpandbar();
			if (expandbar != null && !expandbar.isDisposed()) {
				expandbar.removeExpandListener(this);
			}
		}
	}
}
