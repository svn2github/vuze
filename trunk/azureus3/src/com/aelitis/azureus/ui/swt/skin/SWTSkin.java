/*
 * Created on May 29, 2006 4:01:04 PM
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

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.skin.SkinProperties;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class SWTSkin
{
	static boolean DEBUGLAYOUT = false; //System.getProperty("debuglayout") != null;

	private Map mapImageLoaders = new HashMap();

	private SWTSkinProperties skinProperties;

	private Listener handCursorListener;

	private Listener ontopPaintListener;

	// Key = Skin Object ID; Value = Control
	private HashMap mapIDsToControls = new HashMap();

	// Key = TabSet ID; Value = List of Control
	private HashMap mapTabSetToControls = new HashMap();

	// Key = Widget ID; Value = Control
	private HashMap mapPublicViewIDsToControls = new HashMap();

	private HashMap mapPublicViewIDsToListeners = new HashMap();

	private AEMonitor mapPublicViewIDsToListeners_mon = new AEMonitor(
			"SWTSkin::mapPublicViewIDsToListeners");

	private ArrayList ontopImages = new ArrayList();

	private Shell shell;

	private boolean bLayoutComplete = false;

	/**
	 * 
	 */
	public SWTSkin() {
		skinProperties = new SWTSkinPropertiesImpl();
		ImageLoaderFactory.createInstance(Display.getDefault(), skinProperties);

		ontopPaintListener = new Listener() {
			public void handleEvent(Event event) {
				for (Iterator iter = ontopImages.iterator(); iter.hasNext();) {
					SWTSkinObject skinObject = (SWTSkinObject) iter.next();

					Control control = skinObject.getControl();
					if (control == null) {
						continue;
					}
					Rectangle bounds = control.getBounds();
					Point point = control.toDisplay(0, 0);
					bounds.x = point.x;
					bounds.y = point.y;

					Rectangle eventBounds = event.getBounds();
					point = ((Control) event.widget).toDisplay(0, 0);
					eventBounds.x += point.x;
					eventBounds.y += point.y;

					//System.out.println(eventBounds + ";" + bounds);

					if (eventBounds.intersects(bounds)) {
						Point dst = new Point(bounds.x - point.x, bounds.y - point.y);

						//System.out.println("Painting on " + event.widget + " at " + dst);
						Image image = (Image) control.getData("image");
						// TODO: Clipping otherwise alpha will multiply
						//event.gc.setClipping(eventBounds);
						event.gc.drawImage(image, dst.x, dst.y);
					}
				}
			}
		};
	}

	public ImageLoader getImageLoader(SkinProperties properties) {
		ImageLoader loader = (ImageLoader) mapImageLoaders.get(properties);

		if (loader != null) {
			return loader;
		}

		loader = new ImageLoader(Display.getDefault(), properties);
		mapImageLoaders.put(properties, loader);

		return loader;
	}

	public void addToControlMap(SWTSkinObject skinObject) {
		String sID = skinObject.getSkinObjectID();
		if (DEBUGLAYOUT) {
			System.out.println("addToControlMap: " + sID + " : " + skinObject);
		}
		addToArrayMap(mapIDsToControls, sID, skinObject);

		// For SWT layout -- add a reverse lookup
		Control control = skinObject.getControl();
		if (control != null) {
			control.setData("ConfigID", skinObject.getConfigID());
			control.setData("SkinID", sID);
		}
	}

	private void addToArrayMap(Map arrayMap, Object key, SWTSkinObject object) {
		Object existing = arrayMap.get(key);
		if (existing instanceof SWTSkinObject[]) {
			SWTSkinObject[] existingObjects = (SWTSkinObject[]) existing;

			boolean bAlreadyPresent = false;
			for (int i = 0; i < existingObjects.length; i++) {
				//System.out.println(".." + existingObjects[i]);
				if (existingObjects[i].equals(object)) {
					bAlreadyPresent = true;
					System.err.println("already present: " + key + "; " + object
							+ "; existing: " + existingObjects[i]);
					break;
				}
			}

			if (!bAlreadyPresent) {
				int length = existingObjects.length;
				SWTSkinObject[] newObjects = new SWTSkinObject[length + 1];
				System.arraycopy(existingObjects, 0, newObjects, 0, length);
				newObjects[length] = object;

				arrayMap.put(key, newObjects);
				//				System.out.println("addToArrayMap: " + key + " : " + object + " #"
				//						+ (length + 1));
			}
		} else {
			arrayMap.put(key, new SWTSkinObject[] { object
			});
		}
	}

	private Object getFromArrayMap(Map arrayMap, Object key, SWTSkinObject parent) {
		if (parent == null) {
			return null;
		}

		SWTSkinObject[] objects = (SWTSkinObject[]) arrayMap.get(key);
		if (objects == null) {
			return null;
		}

		for (int i = 0; i < objects.length; i++) {
			SWTSkinObject object = objects[i];
			SWTSkinObject thisParent = object;
			while (thisParent != null) {
				if (thisParent.equals(parent)) {
					return object;
				}
				thisParent = thisParent.getParent();
			}
		}

		return null;
	}

	private void setSkinObjectViewID(SWTSkinObject skinObject, String sViewID) {
		addToArrayMap(mapPublicViewIDsToControls, sViewID, skinObject);
	}

	public void dumpObjects() {
		System.out.println("=====");
		FormData formdata;
		for (Iterator iter = mapIDsToControls.keySet().iterator(); iter.hasNext();) {
			String sID = (String) iter.next();
			Control control = getSkinObjectByID(sID).getControl();

			formdata = (FormData) control.getLayoutData();

			System.out.println(sID);

			sID += ".attach.";

			if (formdata.left != null) {
				System.out.println(sID + "left=" + getAttachLine(formdata.left));
			}
			if (formdata.right != null) {
				System.out.println(sID + "right=" + getAttachLine(formdata.right));
			}
			if (formdata.top != null) {
				System.out.println(sID + "top=" + getAttachLine(formdata.top));
			}
			if (formdata.bottom != null) {
				System.out.println(sID + "bottom=" + getAttachLine(formdata.bottom));
			}
		}
	}

	public SWTSkinObject getSkinObjectByID(String sID) {
		SWTSkinObject[] objects = (SWTSkinObject[]) mapIDsToControls.get(sID);
		if (objects == null) {
			return null;
		}

		return objects[0];
	}

	protected SWTSkinObject getSkinObjectByID(String sID, SWTSkinObject parent) {
		if (parent == null) {
			// XXX Search for parent is shell directly
			return getSkinObjectByID(sID);
		}

		return (SWTSkinObject) getFromArrayMap(mapIDsToControls, sID, parent);
	}

	public SWTSkinObject getSkinObject(String sViewID) {
		SWTSkinObject[] objects = (SWTSkinObject[]) mapPublicViewIDsToControls.get(sViewID);
		if (objects == null) {
			return null;
		}

		return objects[0];
	}

	public SWTSkinObject getSkinObject(String sViewID, SWTSkinObject parent) {
		if (parent == null) {
			// XXX Search for parent is shell directly
			return getSkinObject(sViewID);
		}

		return (SWTSkinObject) getFromArrayMap(mapPublicViewIDsToControls, sViewID,
				parent);
	}

	public SWTSkinTabSet getTabSet(String sID) {
		return (SWTSkinTabSet) mapTabSetToControls.get(sID);
	}

	public void activateTab(SWTSkinObject skinObjectInTab) {
		if (skinObjectInTab == null) {
			return;
		}

		for (Iterator iter = mapTabSetToControls.values().iterator(); iter.hasNext();) {
			SWTSkinTabSet tabset = (SWTSkinTabSet) iter.next();

			SWTSkinObjectTab[] tabs = tabset.getTabs();
			boolean bHasSkinObject = false;
			for (int i = 0; i < tabs.length; i++) {
				SWTSkinObjectTab tab = tabs[i];
				SWTSkinObject[] activeWidgets = tab.getActiveWidgets();
				for (int j = 0; j < activeWidgets.length; j++) {
					SWTSkinObject object = activeWidgets[j];
					//System.out.println("check " + tab + ";" + object + " for " + skinObjectInTab);
					if (hasSkinObject(object, skinObjectInTab)) {
						//System.out.println("FOUND");
						tabset.setActiveTab(tab);
						return;
					}
				}
			}
		}
		System.out.println("NOT FOUND");
	}

	private boolean hasSkinObject(SWTSkinObject start, SWTSkinObject skinObject) {
		if (start instanceof SWTSkinObjectContainer) {
			SWTSkinObject[] children = ((SWTSkinObjectContainer) start).getChildren();
			for (int i = 0; i < children.length; i++) {
				SWTSkinObject object = children[i];
				//System.out.println("  check " + object + " in " + start + " for " + skinObject);
				if (hasSkinObject(object, skinObject))
					return true;
			}
		}
		//System.out.println("  check " + start + " for " + skinObject);
		return skinObject.equals(start);
	}

	public SWTSkinTabSet getTabSet(SWTSkinObject skinObject) {
		String sTabSetID = skinObject.getProperties().getStringValue(
				skinObject.getConfigID() + ".tabset");
		return getTabSet(sTabSetID);
	}

	public boolean setActiveTab(String sTabSetID, String sTabID) {
		SWTSkinTabSet tabSet = getTabSet(sTabSetID);
		if (tabSet == null) {
			System.err.println(sTabSetID);
			return false;
		}

		return tabSet.setActiveTab(sTabID);
	}

	public void initialize(Shell shell, String startID) {

		this.shell = shell;
		FormLayout layout = new FormLayout();
		shell.setLayout(layout);
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

		String[] sMainGroups = skinProperties.getStringArray(startID + ".widgets");
		if (sMainGroups == null) {
			System.out.println("NO " + startID + ".widgets!!");
			sMainGroups = new String[] {};
		}

		for (int i = 0; i < sMainGroups.length; i++) {
			String sID = sMainGroups[i];

			if (DEBUGLAYOUT) {
				System.out.println("Container: " + sID);
			}

			linkIDtoParent(skinProperties, sID, sID, null, false, true);
		}
	}

	/**
	 * 
	 */
	private void addPaintListenerToAll(Control control) {
		// XXX: Bug: When paint listener is set to shell, browser widget will flicker on OSX when resizing
		if (!(control instanceof Shell)) {
			control.addListener(SWT.Paint, ontopPaintListener);
		}

		if (control instanceof Composite) {
			Composite composite = (Composite) control;

			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				addPaintListenerToAll(children[i]);
			}
		}
	}

	public void layout() {
		DEBUGLAYOUT = false;
		if (DEBUGLAYOUT) {
			System.out.println("==== Start Apply Layout");
		}
		// Apply layout data from skin
		for (Iterator iter = mapIDsToControls.keySet().iterator(); iter.hasNext();) {
			String sID = (String) iter.next();
			SWTSkinObject[] objects = (SWTSkinObject[]) mapIDsToControls.get(sID);

			if (DEBUGLAYOUT) {
				System.out.println("Apply Layout for " + objects.length + " " + sID);
			}

			for (int i = 0; i < objects.length; i++) {
				attachControl(objects[i]);
			}
		}

		for (Iterator iter = mapTabSetToControls.values().iterator(); iter.hasNext();) {
			SWTSkinTabSet tabset = (SWTSkinTabSet) iter.next();
			tabset.clean();
		}

		// Disabled due to Browser flickering
		//addPaintListenerToAll(shell);

		bLayoutComplete = true;
		if (DEBUGLAYOUT) {
			System.out.println("==== End Apply Layout");
		}
	}

	/**
	 * @param controlToLayout
	 * @param id
	 */
	private void attachControl(SWTSkinObject skinObject) {
		if (skinObject == null) {
			return;
		}

		Control controlToLayout = skinObject.getControl();

		if (controlToLayout == null) {
			return;
		}

		String sConfigID = skinObject.getConfigID();
		SWTSkinProperties properties = skinObject.getProperties();

		final String[] sDirections = {
			"top",
			"bottom",
			"left",
			"right"
		};

		// Because layout data is cached, we can't just set the data's properties
		// We need to create a brand new FormData.

		Object data = controlToLayout.getLayoutData();
		if (data != null && !(data instanceof FormData)) {
			return;
		}
		FormData oldFormData = (FormData) controlToLayout.getLayoutData();
		if (oldFormData == null) {
			oldFormData = new FormData();
		}

		FormData newFormData = new FormData(oldFormData.width, oldFormData.height);

		String templateID = properties.getStringValue(sConfigID
				+ ".attach.template");
		if (templateID == null) {
			//templateID = skinObject.getSkinObjectID();
		}

		for (int i = 0; i < sDirections.length; i++) {
			Control control = null;
			int offset = 0;
			int percent = 0;
			String sAlign = null;
			int align = SWT.DEFAULT;

			// grab any defaults from existing attachment
			FormAttachment attachment;
			switch (i) {
				case 0:
					attachment = oldFormData.top;
					break;

				case 1:
					attachment = oldFormData.bottom;
					break;

				case 2:
					attachment = oldFormData.left;
					break;

				case 3:
					attachment = oldFormData.right;
					break;

				default:
					attachment = null;
			}

			if (attachment != null) {
				control = attachment.control;
				offset = attachment.offset;
				align = attachment.alignment;
				// XXX Assumed: Denominator is 100
				percent = attachment.numerator;
			}

			// parse skin config

			String suffix = ".attach." + sDirections[i];
			String prefix = sConfigID;
			String[] sParams;

			sParams = properties.getStringArray(sConfigID + suffix);
			if (sParams == null && templateID != null) {
				sParams = properties.getStringArray(templateID + suffix);
				prefix = templateID;
			}

			if (sParams == null) {
				if (attachment != null) {
					if (control == null) {
						attachment = new FormAttachment(percent, offset);
					} else {
						attachment = new FormAttachment(control, offset, align);
					}
				}

			} else if (sParams.length == 0
					|| (sParams.length == 1 && sParams[0].length() == 0)) {
				attachment = null;
			} else {

				if (sParams[0].length() > 0 && Character.isDigit(sParams[0].charAt(0))) {
					// Percent, Offset
					try {
						percent = Integer.parseInt(sParams[0]);
					} catch (Exception e) {
					}

					if (sParams.length > 1) {
						try {
							offset = Integer.parseInt(sParams[1]);
						} catch (Exception e) {
						}
					}

					attachment = new FormAttachment(percent, offset);

				} else {
					// Object, Offset, Alignment
					String sWidget = sParams[0];

					SWTSkinObject configSkinObject = getSkinObjectByID(sWidget);
					int iNextPos;
					if (configSkinObject != null) {
						control = configSkinObject.getControl();

						iNextPos = 1;
					} else {
						iNextPos = 0;

						if (sWidget.length() != 0) {
							System.err.println("ERROR: Trying to attach " + sDirections[i]
									+ " of widget '" + skinObject + "' to non-existant widget '"
									+ sWidget + "'.  Attachment Parameters: "
									+ properties.getStringValue(prefix + suffix));
						}
					}

					for (int j = iNextPos; j < sParams.length; j++) {
						if (sParams[j].length() > 0) {
							char c = sParams[j].charAt(0);
							if (Character.isDigit(c) || c == '-') {
								try {
									offset = Integer.parseInt(sParams[j]);
								} catch (Exception e) {
								}
							} else {
								sAlign = sParams[j];
							}
						}
					}

					if (sAlign != null) {
						align = SWTSkinUtils.getAlignment(sAlign, align);
					}

					attachment = new FormAttachment(control, offset, align);
				}
			}

			if (controlToLayout.getData("DEBUG") != null && attachment != null) {
				if (controlToLayout instanceof Group) {
					Group group = (Group) controlToLayout;
					String sValue = properties.getStringValue(prefix + suffix);
					String sText = group.getText() + "; "
							+ sDirections[i].substring(0, 1) + "="
							+ (sValue == null ? "(def)" : sValue);
					if (sText.length() < 20) {
						group.setText(sText);
					}
					group.setToolTipText(sText);
				}
			}

			if (DEBUGLAYOUT) {
				System.out.println("Attach: " + sConfigID + suffix + ": "
						+ properties.getStringValue(prefix + suffix) + "/" + attachment);
			}

			// create new attachment
			switch (i) {
				case 0:
					newFormData.top = attachment;
					break;

				case 1:
					newFormData.bottom = attachment;
					break;

				case 2:
					newFormData.left = attachment;
					break;

				case 3:
					newFormData.right = attachment;
					break;
			}

		}

		if (Constants.isWindows && (controlToLayout instanceof Browser) && false) {
			if (newFormData.top != null) {
				newFormData.top.offset -= 2;
			}
			if (newFormData.right != null) {
				newFormData.right.offset += 2;
			}
			if (newFormData.left != null) {
				newFormData.left.offset -= 2;
			}
			if (newFormData.bottom != null) {
				newFormData.bottom.offset += 2;
			}
		}

		newFormData.height = properties.getIntValue(sConfigID + ".height",
				newFormData.height);
		newFormData.width = properties.getIntValue(sConfigID + ".width",
				newFormData.width);
		controlToLayout.setLayoutData(newFormData);
	}

	private SWTSkinObject createContainer(SWTSkinProperties properties,
			String sID, final String sConfigID, SWTSkinObject parentSkinObject,
			boolean bForceCreate, boolean bPropogate, SWTSkinObject intoSkinObject) {
		String[] sItems = properties.getStringArray(sConfigID + ".widgets");
		if (sItems == null && !bForceCreate) {
			return null;
		}

		if (DEBUGLAYOUT) {
			System.out.println("createContainer: " + sID + ";"
					+ properties.getStringValue(sConfigID + ".widgets"));
		}

		SWTSkinObject skinObject = getSkinObjectByID(sID, parentSkinObject);

		if (skinObject == null) {
			if (intoSkinObject == null) {
				skinObject = new SWTSkinObjectContainer(this, properties, sID,
						sConfigID, parentSkinObject);
				addToControlMap(skinObject);
			} else {
				skinObject = intoSkinObject;
			}
		} else {
			if (!(skinObject instanceof SWTSkinObjectContainer)) {
				return skinObject;
			}
		}

		if (!bPropogate) {
			bPropogate = properties.getIntValue(sConfigID + ".propogate", 0) == 1;
		}

		if (!bPropogate && (parentSkinObject instanceof SWTSkinObjectContainer)) {
			bPropogate = ((SWTSkinObjectContainer) parentSkinObject).getPropogation();
		}
		if (bPropogate) {
			((SWTSkinObjectContainer) skinObject).setPropogation(true);
		}

		if (sItems != null) {
			String[] paramValues = null;
			if (properties instanceof SWTSkinPropertiesParam) {
				paramValues = ((SWTSkinPropertiesParam) properties).getParamValues();
			}
			// Cloning is only for one level.  Children get the original properties
			// object
			if (properties instanceof SWTSkinPropertiesClone) {
				properties = ((SWTSkinPropertiesClone) properties).getOriginalProperties();
			}

			// Propogate any parameter values.
			// XXX This could get ugly, we should could the # of 
			//     SWTSkinPropertiesParam to determine if this needs optimizing
			//     ie. if a top container has paramValues, every child will get a new
			//         object.  How would this affect memory/performace?
			if (paramValues != null) {
				properties = new SWTSkinPropertiesParamImpl(properties, paramValues);
			}

			for (int i = 0; i < sItems.length; i++) {
				String sItemID = sItems[i];
				linkIDtoParent(properties, sItemID, sItemID, skinObject, false, true);
			}
		}

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	private SWTSkinObject createSash(SWTSkinProperties properties, String sID,
			String sConfigID, SWTSkinObject parentSkinObject, final boolean bVertical) {
		int style = bVertical ? SWT.VERTICAL : SWT.HORIZONTAL;

		final String[] sItems = properties.getStringArray(sConfigID + ".widgets");

		SWTSkinObject skinObject;

		Composite createOn;
		if (parentSkinObject == null) {
			createOn = shell;
		} else {
			createOn = (Composite) parentSkinObject.getControl();
		}

		if (sItems == null) {
			// No widgets, so it's a sash
			Sash sash = new Sash(createOn, style);
			skinObject = new SWTSkinObjectBasic(this, properties, sash, sID,
					sConfigID, "sash", parentSkinObject);
			addToControlMap(skinObject);

			sash.setBackground(sash.getDisplay().getSystemColor(SWT.COLOR_RED));

			sash.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					Sash sash = (Sash) e.widget;
					final boolean FASTDRAG = true;

					if (FASTDRAG && e.detail == SWT.DRAG) {
						return;
					}

					Rectangle parentArea = sash.getParent().getClientArea();

					FormData formData = (FormData) sash.getLayoutData();
					formData.left = new FormAttachment(e.x * 100 / parentArea.width);
					sash.getParent().layout();
				}
			});
		} else {
			// Widgets exist, so use a SashForm to split them
			final SashForm sashForm = new SashForm(createOn, style);
			skinObject = new SWTSkinObjectContainer(this, properties, sashForm, sID,
					sConfigID, "sash", parentSkinObject);
			addToControlMap(skinObject);

			int iSashWidth = properties.getIntValue(sConfigID + ".sash.width", -1);

			if (iSashWidth > 0) {
				sashForm.SASH_WIDTH = iSashWidth;
			}

			for (int i = 0; i < sItems.length; i++) {
				String sChildID = sItems[i];
				linkIDtoParent(properties, sChildID, sChildID, skinObject, false, true);
			}
		}

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	private SWTSkinObject createMySash(SWTSkinProperties properties,
			final String sID, String sConfigID, String[] typeParams,
			SWTSkinObject parentSkinObject, final boolean bVertical) {

		SWTSkinObject skinObject = new SWTSkinObjectSash(this, properties, sID,
				sConfigID, typeParams, parentSkinObject, bVertical);
		addToControlMap(skinObject);

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	/**
	 * Create a tab using a template.
	 * <p>
	 * (objectid).view.template.(sTemplateKey)=(Reference to Template skin object)
	 * 
	 * @param sID ID to give the new tab
	 * @param sTemplateKey Template Key to read to get the tab's template skin object
	 * @param tabHolder Where to read the template key from
	 * 
	 * @return The new tab, or null if tab could not be created
	 */
	public SWTSkinObjectTab createTab(String sID, String sTemplateKey,
			SWTSkinObject tabHolder) {
		String sTemplateID = SWTSkinTabSet.getTemplateID(this, tabHolder,
				sTemplateKey);

		if (sTemplateID == null) {
			return null;
		}

		SWTSkinObject skinObject = linkIDtoParent(skinProperties, sID, sTemplateID,
				tabHolder, true, true);

		//		SWTSkinObjectTab skinObject = _createTab(skinProperties, sID, sTemplateID,
		//				tabHolder);
		if (bLayoutComplete && skinObject != null) {
			((Composite) skinObject.getControl()).getParent().layout(true);
		}
		if (skinObject instanceof SWTSkinObjectTab) {
			return (SWTSkinObjectTab) skinObject;
		}

		System.err.println(skinObject + " not a SWTSkinObjectTab! Template: "
				+ sTemplateID);
		return null;
	}

	/**
	 * @param configID
	 */
	private SWTSkinObjectTab _createTab(SWTSkinProperties properties, String sID,
			String sConfigID, SWTSkinObject parentSkinObject) {
		//System.out.println("createTab " + sID + ", " + sConfigID + ", " + sParentID);

		SWTSkinObjectTab skinObjectTab = new SWTSkinObjectTab(this, properties,
				sID, sConfigID, parentSkinObject);
		createContainer(properties, sID, sConfigID, parentSkinObject, true, true,
				skinObjectTab);

		addToControlMap(skinObjectTab);

		String sTabSet = properties.getStringValue(sConfigID + ".tabset", "default");

		SWTSkinTabSet tabset = (SWTSkinTabSet) mapTabSetToControls.get(sTabSet);
		if (tabset == null) {
			tabset = new SWTSkinTabSet(this, sTabSet);
			mapTabSetToControls.put(sTabSet, tabset);
			if (DEBUGLAYOUT) {
				System.out.println("New TabSet: " + sTabSet);
			}
		}
		tabset.addTab(skinObjectTab);
		if (DEBUGLAYOUT) {
			System.out.println("Tab " + sID + " added");
		}

		if (bLayoutComplete) {
			attachControl(skinObjectTab);
		}

		return skinObjectTab;
	}

	private SWTSkinObject createTextLabel(SWTSkinProperties properties,
			String sID, String sConfigID, String[] typeParams,
			SWTSkinObject parentSkinObject) {
		SWTSkinObject skinObject = new SWTSkinObjectText2(this, properties, sID,
				sConfigID, typeParams, parentSkinObject);
		addToControlMap(skinObject);

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	public Shell getShell() {
		return shell;
	}

	//	private void createTextWidget(final String sConfigID) {
	//		SWTSkinObject parent = getParent(sConfigID);
	//		if (parent == null) {
	//			return;
	//		}
	//
	//		SWTTextPaintListener listener = new SWTTextPaintListener(this, parent.getControl(),
	//				sConfigID);
	//		parent.getControl().addPaintListener(listener);
	//
	//		//addToControlMap(listener, sConfigID);
	//
	//		return;
	//	}

	private String getAttachLine(FormAttachment attach) {
		String s = "";
		if (attach.control != null) {
			s += attach.control.getData("ConfigID");
			if (attach.offset != 0 || attach.alignment != SWT.DEFAULT) {
				s += "," + attach.offset;
			}
			if (attach.alignment != SWT.DEFAULT) {
				if (attach.alignment == SWT.LEFT) {
					s += ",left";
				} else if (attach.alignment == SWT.RIGHT) {
					s += ",right";
				} else if (attach.alignment == SWT.TOP) {
					s += ",top";
				} else if (attach.alignment == SWT.BOTTOM) {
					s += ",bottom";
				} else if (attach.alignment == SWT.CENTER) {
					s += ",center";
				}
			}
		} else {
			s += (int) (100.0 * attach.numerator / attach.denominator) + ","
					+ attach.offset;
		}
		return s;
	}

	protected Listener getHandCursorListener(Display display) {
		if (handCursorListener == null) {
			final Cursor handCursor = new Cursor(display, SWT.CURSOR_HAND);
			handCursorListener = new Listener() {
				public void handleEvent(Event event) {
					if (event.type == SWT.MouseEnter) {
						((Control) event.widget).setCursor(handCursor);
					}
					if (event.type == SWT.MouseExit) {
						((Control) event.widget).setCursor(null);
					}
				}
			};
		}

		return handCursorListener;
	}

	public SWTSkinObject createSkinObject(String sID, String sConfigID,
			SWTSkinObject parentSkinObject) {
		SWTSkinObject skinObject = linkIDtoParent(skinProperties, sID, sConfigID,
				parentSkinObject, true, true);
		if (bLayoutComplete) {
			((Composite) skinObject.getControl()).getParent().layout(true);
		}
		return skinObject;
	}

	private SWTSkinObject linkIDtoParent(SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parentSkinObject,
			boolean bForceCreate, boolean bAddView) {
		SWTSkinObject skinObject = null;
		try {
			String[] sTypeParams = properties.getStringArray(sConfigID + ".type");
			String sType;
			if (sTypeParams != null && sTypeParams.length > 0) {
				sType = sTypeParams[0];
				bForceCreate = true;
			} else {
				// best guess
				sType = null;

				String sImageLoc = properties.getStringValue(sConfigID);
				if (sImageLoc != null) {
					sType = "image";
				} else {
					String sText = properties.getStringValue(sConfigID + ".text");
					if (sText != null) {
						sType = "text";
					} else {
						String sWidgets = properties.getStringValue(sConfigID + ".widgets");
						if (sWidgets != null || bForceCreate) {
							sType = "container";
						}
					}
				}

				if (sType == null) {
					if (DEBUGLAYOUT) {
						System.err.println("no type defined for " + sConfigID);
					}
					return null;
				}

				sTypeParams = new String[] { sType
				};
			}

			if (sType.equals("image")) {
				skinObject = createImageLabel(properties, sID, sConfigID, null,
						sTypeParams, parentSkinObject);
			} else if (sType.equals("image2")) {
				skinObject = createImageLabel2(properties, sID, parentSkinObject);
			} else if (sType.equals("container2")) {
				skinObject = createContainer2(properties, sID, sConfigID,
						parentSkinObject, bForceCreate, false, null);
			} else if (sType.equals("container")) {
				skinObject = createContainer(properties, sID, sConfigID,
						parentSkinObject, bForceCreate, false, null);
			} else if (sType.equals("text")) {
				skinObject = createTextLabel(properties, sID, sConfigID, sTypeParams,
						parentSkinObject);
			} else if (sType.equals("tab")) {
				skinObject = _createTab(properties, sID, sConfigID, parentSkinObject);
			} else if (sType.equals("v-sash")) {
				skinObject = createSash(properties, sID, sConfigID, parentSkinObject,
						true);
			} else if (sType.equals("h-sash")) {
				skinObject = createSash(properties, sID, sConfigID, parentSkinObject,
						false);
			} else if (sType.equals("v-mysash")) {
				skinObject = createMySash(properties, sID, sConfigID, sTypeParams,
						parentSkinObject, true);
			} else if (sType.equals("h-mysash")) {
				skinObject = createMySash(properties, sID, sConfigID, sTypeParams,
						parentSkinObject, false);
			} else if (sType.equals("clone")) {
				skinObject = createClone(properties, sID, sConfigID, sTypeParams,
						parentSkinObject);
			} else if (sType.equals("hidden")) {
				return null;
			} else if (sType.equals("browser")) {
				skinObject = createBrowser(properties, sID, sConfigID, parentSkinObject);
			} else {
				System.err.println(sConfigID + ": Invalid type of " + sType);
			}

			if (bAddView) {
				String sViewID = properties.getStringValue(sConfigID + ".view");
				if (sViewID != null) {
					setSkinObjectViewID(skinObject, sViewID);
					if (skinObject instanceof SWTSkinObjectBasic) {
						((SWTSkinObjectBasic) skinObject).setViewID(sViewID);
					}
				}
			}

			if (bLayoutComplete) {
				attachControl(skinObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return skinObject;
	}

	/**
	 * @param properties
	 * @param sid
	 * @param sConfigID
	 * @param parentSkinObject
	 * @return
	 */
	private SWTSkinObject createBrowser(SWTSkinProperties properties, String sID,
			String sConfigID, SWTSkinObject parentSkinObject) {

		SWTSkinObject skinObject = new SWTSkinObjectBrowser(this, properties, sID,
				sConfigID, parentSkinObject);
		addToControlMap(skinObject);

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	private SWTSkinObject createClone(SWTSkinProperties properties, String sID,
			String sConfigID, String[] typeParams, SWTSkinObject parentSkinObject) {
		//System.out.println("Create Clone " + sID + " == " + sConfigID + " for " + parentSkinObject);
		if (sConfigID == "") {
			System.err.println("XXXXXXXX " + sID + " has no config ID.."
					+ Debug.getStackTrace(false, false));
		}

		String[] sCloneParams;
		if (typeParams.length > 1) {
			int size = typeParams.length - 1;
			sCloneParams = new String[size];
			System.arraycopy(typeParams, 1, sCloneParams, 0, size);
		} else {
			sCloneParams = properties.getStringArray(sConfigID + ".clone");
			if (sCloneParams == null || sCloneParams.length < 1) {
				return null;
			}
		}

		if (properties instanceof SWTSkinPropertiesClone) {
			properties = ((SWTSkinPropertiesClone) properties).getOriginalProperties();
		}

		//System.out.println(sCloneParams[0]);
		SWTSkinPropertiesClone cloneProperties = new SWTSkinPropertiesClone(
				properties, sConfigID, sCloneParams);

		return linkIDtoParent(cloneProperties, sID, "", parentSkinObject, false,
				false);
	}

	private SWTSkinObject createImageLabel(SWTSkinProperties properties,
			String sID, String sConfigID, String sImageID, String[] typeParams,
			SWTSkinObject parentSkinObject) {
		if (sImageID == null) {
			sImageID = sConfigID;
			if (typeParams.length > 1) {
				properties.addProperty(sConfigID + ".image", typeParams[1]);
			}
		}
		SWTSkinObjectImage skinObject = new SWTSkinObjectImage(this, properties,
				sID, sConfigID, sImageID, parentSkinObject);
		addToControlMap(skinObject);

		return skinObject;
	}

	private SWTSkinObject createContainer2(SWTSkinProperties properties,
			String sID, final String sConfigID, SWTSkinObject parentSkinObject,
			boolean bForceCreate, boolean bPropogate, SWTSkinObject intoSkinObject) {
		String[] sItems = properties.getStringArray(sConfigID + ".widgets");
		if (sItems == null && !bForceCreate) {
			return null;
		}

		if (DEBUGLAYOUT) {
			System.out.println("createContainer: " + sID + ";"
					+ properties.getStringValue(sConfigID + ".widgets"));
		}

		SWTSkinObject skinObject = getSkinObjectByID(sID, parentSkinObject);

		if (skinObject == null) {
			if (intoSkinObject == null) {
				skinObject = new SWTSkinObjectImageContainer(this, properties, sID,
						sConfigID, parentSkinObject);
				addToControlMap(skinObject);
			} else {
				skinObject = intoSkinObject;
			}
		} else {
			if (!(skinObject instanceof SWTSkinObjectContainer)) {
				return skinObject;
			}
		}

		if (!bPropogate) {
			bPropogate = properties.getIntValue(sConfigID + ".propogate", 0) == 1;
		}

		if (!bPropogate && (parentSkinObject instanceof SWTSkinObjectContainer)) {
			bPropogate = ((SWTSkinObjectContainer) parentSkinObject).getPropogation();
		}
		if (bPropogate) {
			((SWTSkinObjectContainer) skinObject).setPropogation(true);
		}

		if (sItems != null) {
			String[] paramValues = null;
			if (properties instanceof SWTSkinPropertiesParam) {
				paramValues = ((SWTSkinPropertiesParam) properties).getParamValues();
			}
			// Cloning is only for one level.  Children get the original properties
			// object
			if (properties instanceof SWTSkinPropertiesClone) {
				properties = ((SWTSkinPropertiesClone) properties).getOriginalProperties();
			}

			// Propogate any parameter values.
			// XXX This could get ugly, we should could the # of 
			//     SWTSkinPropertiesParam to determine if this needs optimizing
			//     ie. if a top container has paramValues, every child will get a new
			//         object.  How would this affect memory/performace?
			if (paramValues != null) {
				properties = new SWTSkinPropertiesParamImpl(properties, paramValues);
			}

			for (int i = 0; i < sItems.length; i++) {
				String sItemID = sItems[i];
				linkIDtoParent(properties, sItemID, sItemID, skinObject, false, true);
			}
		}

		if (bLayoutComplete) {
			attachControl(skinObject);
		}

		return skinObject;
	}

	private SWTSkinObject createImageLabel2(SWTSkinProperties properties,
			String sConfigID, SWTSkinObject parentSkinObject) {
		Composite createOn;
		if (parentSkinObject == null) {
			createOn = shell;
		} else {
			createOn = (Composite) parentSkinObject.getControl();
		}

		final Canvas drawable = new Canvas(createOn, SWT.NO_BACKGROUND);
		drawable.setVisible(false);

		ImageLoader imageLoader = getImageLoader(properties);
		Image image = imageLoader.getImage(sConfigID);
		if (ImageLoader.isRealImage(image)) {
			image = imageLoader.getImage(sConfigID + ".image");
		}
		drawable.setData("image", image);

		SWTSkinObjectBasic skinObject = new SWTSkinObjectBasic(this, properties,
				drawable, sConfigID, sConfigID, "image", parentSkinObject);
		addToControlMap(skinObject);

		ontopImages.add(skinObject);

		return skinObject;
	}

	public SWTSkinProperties getSkinProperties() {
		return skinProperties;
	}

	public void addListener(String viewID, SWTSkinObjectListener listener) {
		mapPublicViewIDsToListeners_mon.enter();
		try {
			Object existing = mapPublicViewIDsToListeners.get(viewID);

			if (existing instanceof List) {
				List list = (List) existing;
				list.add(listener);
			} else {
				ArrayList list = new ArrayList();
				list.add(listener);
				mapPublicViewIDsToListeners.put(viewID, list);
			}
		} finally {
			mapPublicViewIDsToListeners_mon.exit();
		}
	}

	public SWTSkinObjectListener[] getSkinObjectListeners(String viewID) {
		if (viewID == null) {
			return new SWTSkinObjectListener[0];
		}

		mapPublicViewIDsToListeners_mon.enter();
		try {
			Object existing = mapPublicViewIDsToListeners.get(viewID);

			if (existing instanceof List) {
				List list = (List) existing;
				return (SWTSkinObjectListener[]) list.toArray(new SWTSkinObjectListener[0]);
			} else {
				return new SWTSkinObjectListener[0];
			}
		} finally {
			mapPublicViewIDsToListeners_mon.exit();
		}
	}

	public static void main(String[] args) {
		java.util.Date d = new java.util.Date();
		long t = d.getTime();

		t -= (1156 * 24 * 60 * 60 * 1000l);
		t -= (6 * 60 * 60 * 1000l);
		t -= (17 * 60 * 1000l);

		Date then = new Date(t);

		System.out.println(d + ";" + then);
	}
}
