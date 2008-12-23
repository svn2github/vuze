/*
 * Created on Jun 26, 2006 11:38:47 AM
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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.gudy.azureus2.core3.util.AERunnableObject;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * A SWTSkinObject that contains other SWTSkinObjects
 * 
 * @author TuxPaper
 * @created Jun 26, 2006
 *
 */
public class SWTSkinObjectContainer
	extends SWTSkinObjectBasic
{
	boolean bPropogate = false;

	boolean bPropogateDown = false;

	public SWTSkinObjectContainer(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "container", parent);
		createComposite();
	}

	public SWTSkinObjectContainer(SWTSkin skin, SWTSkinProperties properties,
			Control control, String sID, String sConfigID, String type,
			SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, type, parent);

		if (control != null) {
			setControl(control);
		}
	}

	private void createComposite() {
		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}
		if (properties.getBooleanValue(sConfigID + ".doublebuffer", false)) {
			style |= SWT.DOUBLE_BUFFERED;
		}

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		final int minWidth = properties.getIntValue(sConfigID + ".minwidth", -1);

		Composite parentComposite;
		if (SWTSkin.DEBUGLAYOUT) {
			System.out.println("linkIDtoParent: Create Composite " + sID + " on "
					+ createOn);
			parentComposite = new Group(createOn, style);
			((Group) parentComposite).setText(sConfigID == null ? sID : sConfigID);
			parentComposite.setData("DEBUG", "1");
		} else {
			// Lovely SWT has a default size of 64x64 if no children have sizes.
			// Let's fix that..
			parentComposite = new Composite(createOn, style) {
				// @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
				public Point computeSize(int wHint, int hHint, boolean changed) {
					Point size = super.computeSize(wHint, hHint, changed);
					if (getChildren().length == 0 && (size.x == 64 || size.y == 64)) {
						Object ld = getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							if (fd.width != 0 && fd.height != 0) {
								Rectangle trim = computeTrim (0, 0, fd.width, fd.height);
								return new Point(trim.width, trim.height);
							}
						}
						return new Point(1, 1);
					}
					if (minWidth > 0 && size.x < minWidth) {
						size.x = minWidth;
					}
					return size;
				}
				
				// @see org.eclipse.swt.widgets.Control#computeSize(int, int)
				public Point computeSize(int wHint, int hHint) {
					Point size = super.computeSize(wHint, hHint);
					if (getChildren().length == 0 && (size.x == 64 || size.y == 64)) {
						Object ld = getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							if (fd.width != 0 && fd.height != 0) {
								Rectangle trim = computeTrim (0, 0, fd.width, fd.height);
								return new Point(trim.width, trim.height);
							}
						}
						return new Point(1, 1);
					}
					if (minWidth > 0 && size.x < minWidth) {
						size.x = minWidth;
					}
					return size;
				}
			};
		}

		parentComposite.setLayout(new FormLayout());
		control = parentComposite;

		setControl(control);
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setControl(org.eclipse.swt.widgets.Control)
	public void setControl(Control control) {
		bPropogateDown = properties.getIntValue(sConfigID + ".propogateDown", 1) == 1;
		
		super.setControl(control);
	}

	protected void setViewID(String viewID) {
		super.setViewID(viewID);
		if (SWTSkin.DEBUGLAYOUT && control != null) {
			((Group) control).setText("[" + viewID + "]");
		}
	}

	public SWTSkinObject[] getChildren() {
		SWTSkinObject[] so = (SWTSkinObject[]) Utils.execSWTThreadWithObject(
				"getChildren", new AERunnableObject() {

					public Object runSupport() {
						if (control.isDisposed()) {
							return new SWTSkinObject[0];
						}
						Control[] swtChildren = ((Composite) control).getChildren();
						ArrayList list = new ArrayList(swtChildren.length);
						for (int i = 0; i < swtChildren.length; i++) {
							Control childControl = swtChildren[i];
							SWTSkinObject so = (SWTSkinObject) childControl.getData("SkinObject");
							if (so != null) {
								list.add(so);
							}
						}

						return (SWTSkinObject[]) list.toArray(new SWTSkinObject[list.size()]);
					}
				}, 2000);
		if (so == null) {
			System.err.println("Tell Tux to fix this " + Debug.getCompressedStackTrace());
			return oldgetChildren();
		}
		return so;
	}

	// TODO: Need find child(view id)
	public SWTSkinObject[] oldgetChildren() {
		String[] widgets = properties.getStringArray(sConfigID + ".widgets");
		if (widgets == null) {
			return new SWTSkinObject[0];
		}

		ArrayList list = new ArrayList();
		for (int i = 0; i < widgets.length; i++) {
			String id = widgets[i];
			SWTSkinObject skinObject = skin.getSkinObjectByID(id, this);
			if (skinObject != null) {
				list.add(skinObject);
			}
		}

		SWTSkinObject[] objects = new SWTSkinObject[list.size()];
		objects = (SWTSkinObject[]) list.toArray(objects);

		return objects;
	}

	public Composite getComposite() {
		return (Composite) control;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String)
	public String switchSuffix(String suffix, int level, boolean walkUp, boolean walkDown) {
		String sFullsuffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (bPropogateDown && walkDown && suffix != null && control != null
				&& !control.isDisposed()) {
			SWTSkinObject[] children = getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].switchSuffix(suffix, level, false);
			}
		}
		return sFullsuffix;
	}

	public void setPropogation(boolean propogate) {
		bPropogate = propogate;
		if (SWTSkin.DEBUGLAYOUT) {
			((Group) control).setText(((Group) control).getText()
					+ (bPropogate ? ";P" : ""));
		}
	}

	public boolean getPropogation() {
		return bPropogate;
	}
	
	public void setDebugAndChildren(boolean b) {
		setDebug(true);
		SWTSkinObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof SWTSkinObjectContainer) {
				((SWTSkinObjectContainer)children[i]).setDebugAndChildren(b);
			} else {
				children[i].setDebug(b);
			}
		}
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setIsVisible(boolean)
	protected void setIsVisible(boolean visible, boolean walkup) {
		super.setIsVisible(visible, walkup);
		
		SWTSkinObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof SWTSkinObjectBasic) {
				SWTSkinObjectBasic child = ((SWTSkinObjectBasic)children[i]);
				Control childControl = child.getControl();
				if (childControl != null && !childControl.isDisposed()) {
					child.setIsVisible(childControl.isVisible(), false);
				}
			}
		}
	}
}
