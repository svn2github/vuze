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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AERunnableObject;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.CompositeMinSize;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;


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

	private String[] sTypeParams = null;

	private int minWidth;

	private int minHeight;

	public SWTSkinObjectContainer(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, String[] sTypeParams, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "container", parent);
		this.sTypeParams = sTypeParams;
		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}
		createComposite(createOn);
	}

	public SWTSkinObjectContainer(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "container", parent);
		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}
		createComposite(createOn);
	}

	public SWTSkinObjectContainer(SWTSkin skin, SWTSkinProperties properties,
			Control control, String sID, String sConfigID, String type,
			SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, type, parent);

		if (control != null) {
			triggerListeners(SWTSkinObjectListener.EVENT_CREATED);
			setControl(control);
		}
	}

	protected Composite createComposite(Composite createOn) {
		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}
		if (properties.getBooleanValue(sConfigID + ".doublebuffer", false)) {
			style |= SWT.DOUBLE_BUFFERED;
		}

		minWidth = properties.getIntValue(sConfigID + ".minwidth", -1);
		minHeight = properties.getIntValue(sConfigID + ".minheight", -1);

		Composite parentComposite;
		if (skin.DEBUGLAYOUT) {
			System.out.println("linkIDtoParent: Create Composite " + sID + " on "
					+ createOn);
			parentComposite = new Group(createOn, style);
			((Group) parentComposite).setText(sConfigID == null ? sID : sConfigID);
			parentComposite.setData("DEBUG", "1");
		} else {
			if (sTypeParams == null || sTypeParams.length < 2
					|| !sTypeParams[1].equalsIgnoreCase("group")) {
  			// Lovely SWT has a default size of 64x64 if no children have sizes.
  			// Let's fix that..
  			parentComposite = new CompositeMinSize(createOn, style);
  			((CompositeMinSize) parentComposite).setMinSize(new Point(minWidth, minHeight));
			} else {
  			parentComposite = new Group(createOn, style);
			}
		}
		
		// setting INHERIT_FORCE here would make the BG of a text box be
		// this parent's BG (on Win7 at least)
		//parentComposite.setBackgroundMode(SWT.INHERIT_FORCE);

		parentComposite.setLayout(new FormLayout());
		control = parentComposite;

		setControl(control);
		
		return parentComposite;
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setControl(org.eclipse.swt.widgets.Control)
	public void setControl(Control control) {
		bPropogateDown = properties.getIntValue(sConfigID + ".propogateDown", 1) == 1;
		
		super.setControl(control);
	}

	protected void setViewID(String viewID) {
		super.setViewID(viewID);
		if (skin.DEBUGLAYOUT && control != null) {
			((Group) control).setText("[" + viewID + "]");
		}
	}

	public SWTSkinObject[] getChildren() {
		if (isDisposed()) {
			return new SWTSkinObject[0];
		}
		SWTSkinObject[] so = (SWTSkinObject[]) Utils.execSWTThreadWithObject(
				"getChildren", new AERunnableObject() {

					public Object runSupport() {
						if (control.isDisposed()) {
							return new SWTSkinObject[0];
						}
						Control[] swtChildren = ((Composite) control).getChildren();
						ArrayList<SWTSkinObject> list = new ArrayList<SWTSkinObject>(swtChildren.length);
						for (int i = 0; i < swtChildren.length; i++) {
							Control childControl = swtChildren[i];
							SWTSkinObject so = (SWTSkinObject) childControl.getData("SkinObject");
							if (so != null) {
								list.add(so);
							}
						}

						return list.toArray(new SWTSkinObject[list.size()]);
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
	public String switchSuffix(final String suffix, final int level, boolean walkUp, boolean walkDown) {
		String sFullsuffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (bPropogateDown && walkDown && suffix != null && control != null
				&& !control.isDisposed()) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTSkinObject[] children = getChildren();
					for (int i = 0; i < children.length; i++) {
						children[i].switchSuffix(suffix, level, false);
					}
				}
			});
		}
		return sFullsuffix;
	}

	public void setPropogation(boolean propogate) {
		bPropogate = propogate;
		if (skin.DEBUGLAYOUT) {
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

	protected boolean superSetIsVisible(boolean visible, boolean walkup) {
		boolean changed = super.setIsVisible(visible, walkup);
		return changed;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setIsVisible(boolean)
	protected boolean setIsVisible(boolean visible, boolean walkup) {
		if (Utils.isThisThreadSWT() && !control.isDisposed()
				&& !control.getShell().isVisible()) {
			return false;
		}
		boolean changed = super.setIsVisible(visible, walkup && visible);

		if (!changed) {
			return false;
		}
		
		// Currently we ignore "changed" and set visibility on children to ensure
		// things display
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				SWTSkinObject[] children = getChildren();
				if (children.length == 0) {
					return;
				}
				if (SWTSkin.DEBUG_VISIBILITIES) {
					System.out.println(">> setIsVisible for " + children.length
							+ " children of " + SWTSkinObjectContainer.this);
				}
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof SWTSkinObjectBasic) {
						SWTSkinObjectBasic child = ((SWTSkinObjectBasic) children[i]);
						Control childControl = child.getControl();
						if (childControl != null && !childControl.isDisposed()) {
							//child.setIsVisible(visible, false);
							//System.out.println("child control " + child + " is " + (childControl.isVisible() ? "visible" : "invisible"));
							child.setIsVisible(childControl.isVisible(), false);
						}
					}
				}
				getComposite().layout();
				if (SWTSkin.DEBUG_VISIBILITIES) {
					System.out.println("<< setIsVisible for " + children.length
							+ " children");
				}
			}
		});
		return changed;
	}

	public void childAdded(SWTSkinObject soChild) {
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#obfusticatedImage(org.eclipse.swt.graphics.Image, org.eclipse.swt.graphics.Point)
	public Image obfusticatedImage(Image image) {
		if (!isVisible()) {
			return image;
		}

		if (getSkinView() instanceof ObfusticateImage) {
			image = ((ObfusticateImage) getSkinView()).obfusticatedImage(image);
		}
		
		Control[] swtChildren = ((Composite) control).getChildren();
		for (int i = 0; i < swtChildren.length; i++) {
			Control childControl = swtChildren[i];

			SWTSkinObject so = (SWTSkinObject) childControl.getData("SkinObject");
			if (so instanceof ObfusticateImage) {
				ObfusticateImage oi = (ObfusticateImage) so;
				oi.obfusticatedImage(image);
			} else if (so == null) { 
				ObfusticateImage oi = (ObfusticateImage) childControl.getData("ObfusticateImage");
				if (oi != null) {
					oi.obfusticatedImage(image);
					continue;
				}
				if (childControl instanceof Composite) {
					obfusticatedImage((Composite) childControl, image);
				}
			}
		}

		return super.obfusticatedImage(image);
	}

	private void obfusticatedImage(Composite c, Image image) {
		if (c == null || c.isDisposed() || !c.isVisible()) {
			return;
		}
		Control[] children = c.getChildren();
		for (Control childControl : children) {
			if (!childControl.isVisible()) {
				continue;
			}
			ObfusticateImage oi = (ObfusticateImage) childControl.getData("ObfusticateImage");
			if (oi != null) {
				oi.obfusticatedImage(image);
				continue;
			}
			if (childControl instanceof Composite) {
				obfusticatedImage((Composite) childControl, image);
			}
		}
	}
}
