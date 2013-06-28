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
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;


/**
 * @author TuxPaper
 * @created Jun 21, 2006
 *
 */
public class SWTSkinObjectExpandItem
	extends SWTSkinObjectContainer implements ExpandListener
{
	private ExpandItem expandItem;
	private boolean expanded;
	private boolean textOverride;
	private Composite composite;

	public SWTSkinObjectExpandItem(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "expanditem", parent);
		
		createExpandItem();
	}

	private void createExpandItem() {
		if (!(parent instanceof SWTSkinObjectExpandBar)) {
			return;
		}
		
		SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;

		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}
		
		final ExpandBar expandBar = soExpandBar.getExpandbar();
		expandItem = new ExpandItem(expandBar, style);

		expandBar.addExpandListener(this);

		System.out.println("FOO");
		composite = createComposite(soExpandBar.getComposite());
		expandItem.setControl(composite);
		composite.setLayoutData(null);
		composite.setData("skin.layedout", true);
		
		composite.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				resizeComposite();
			}
		});
		expandBar.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				/* 
				 * The following is done asynchronously to allow the Text's width
				 * to be changed before re-calculating its preferred height. 
				 */
				event.display.asyncExec(new Runnable() {
					public void run() {
						resizeComposite();
					}
				});
			}
		});
	}
	
	private void resizeComposite() {
		SWTSkinObjectExpandBar soExpandBar = (SWTSkinObjectExpandBar) parent;
		final ExpandBar expandBar = soExpandBar.getExpandbar();
		if (composite.isDisposed()) return;
		Point size = composite.computeSize(expandBar.getClientArea().width, SWT.DEFAULT, true);
		if (expandItem.getHeight() != size.y) {
			expandItem.setHeight(size.y);
		}
	}
	
	public ExpandItem getExpandItem() {
		return expandItem;
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	public void setExpanded(final boolean expand) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				expandItem.setExpanded(expand);
				expanded = expand;
			}
		});
	}

	public void itemCollapsed(ExpandEvent e) {
		expanded = false;
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				resizeComposite();
			}
		});
	}

	public void itemExpanded(ExpandEvent e) {
		expanded = true;
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				resizeComposite();
			}
		});
	}

	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String, int, boolean)
	public String switchSuffix(String suffix, int level, boolean walkUp, boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";
		String text = properties.getStringValue(sPrefix + suffix);
		if (text != null) {
			setText(text, true);
		}

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
