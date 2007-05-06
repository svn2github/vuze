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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.gudy.azureus2.core3.util.Debug;

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

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		Composite parentComposite;
		if (SWTSkin.DEBUGLAYOUT) {
			System.out.println("linkIDtoParent: Create Composite " + sID + " on "
					+ createOn);
			parentComposite = new Group(createOn, style);
			((Group) parentComposite).setText(sConfigID);
			parentComposite.setData("DEBUG", "1");
		} else {
			parentComposite = new Composite(createOn, style);
		}

		parentComposite.setLayout(new FormLayout());
		control = parentComposite;

		setControl(control);
	}

	protected void setViewID(String viewID) {
		super.setViewID(viewID);
		if (SWTSkin.DEBUGLAYOUT) {
			((Group) control).setText("[" + viewID + "]");
		}
	}

	// TODO: Need find child(view id)
	public SWTSkinObject[] getChildren() {
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
	public String switchSuffix(String suffix, int level, boolean walkUp) {
		String sFullsuffix = super.switchSuffix(suffix, level, walkUp);

		if (bPropogate && suffix != null && control != null && !control.isDisposed()) {
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

	/**
	 * Trigger the children listeners too!
	 */
	public void triggerListeners(int eventType, Object params) {
		try {
			super.triggerListeners(eventType, params);
		} catch (Exception e) {
			Debug.out("Trigger of Skin Event "
					+ SWTSkinObjectListener.NAMES[eventType] + " caused an error", e);
		}

		SWTSkinObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			try {
				children[i].triggerListeners(eventType, params);
			} catch (Exception e) {
				Debug.out("Trigger of Skin Event "
						+ SWTSkinObjectListener.NAMES[eventType]
						+ " for Container child has caused an error", e);
			}
		}
	}
}
