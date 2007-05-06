/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.swt.utils.ImageLoader;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class SWTSkinObjectBasic
	implements SWTSkinObject
{
	protected Control control;

	protected String type;

	protected String sConfigID;

	protected SWTBGImagePainter painter;

	protected SWTSkinProperties properties;

	protected String sID;

	// XXX Might be wise to force this to SWTSkinObjectContainer
	protected SWTSkinObject parent;

	protected SWTSkin skin;

	protected String[] suffixes = null;

	protected ArrayList listeners = new ArrayList();

	protected AEMonitor listeners_mon = new AEMonitor(
			"SWTSkinObjectBasic::listener");

	private String sViewID;

	/**
	 * @param properties TODO
	 * 
	 */
	public SWTSkinObjectBasic(SWTSkin skin, SWTSkinProperties properties,
			Control control, String sID, String sConfigID, String type,
			SWTSkinObject parent) {
		this(skin, properties, sID, sConfigID, type, parent);
		setControl(control);
	}

	public SWTSkinObjectBasic(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, String type, SWTSkinObject parent) {
		this.skin = skin;
		this.properties = properties;
		this.sConfigID = sConfigID;
		this.sID = sID;
		this.type = type;
		this.parent = parent;
	}

	public void setControl(Control control) {
		this.control = control;
		control.setData("ConfigID", sConfigID);
		control.setData("SkinObject", this);

		SWTSkinUtils.addMouseImageChangeListeners(control);
		switchSuffix("", 1, false);
	}

	public Control getControl() {
		return control;
	}

	public String getType() {
		return type;
	}

	public String getConfigID() {
		return sConfigID;
	}

	public String getSkinObjectID() {
		return sID;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#getParent()
	public SWTSkinObject getParent() {
		return parent;
	}

	public void setBackground(String sConfigID, String sSuffix) {
		Image imageBG;
		Image imageBGLeft;
		Image imageBGRight;
		
		if (sConfigID == null) {
			return;
		}

		ImageLoader imageLoader = skin.getImageLoader(properties);
		Image[] images = imageLoader.getImages(sConfigID + sSuffix);
		if (images.length == 1 && ImageLoader.isRealImage(images[0])) {
			imageBG = images[0];
			imageBGLeft = imageLoader.getImage(sConfigID + sSuffix + "-left");
			imageBGRight = imageLoader.getImage(sConfigID + sSuffix + "-right");
		} else if (images.length == 3 && ImageLoader.isRealImage(images[2])) {
			imageBGLeft = images[0];
			imageBG = images[1];
			imageBGRight = images[2];
		} else {
			return;
		}

		if (painter == null) {
			//control.setBackgroundImage doesn't handle transparency!
			//control.setBackgroundImage(image);

			// Workaround: create our own image with shell's background
			// for "transparent" area.  Doesn't allow control's image to show
			// through.  To do that, we'd have to walk up the tree until we 
			// found a composite with an image
			//control.setBackgroundMode(SWT.INHERIT_NONE);
			//control.setBackgroundImage(imageBG);

			String sTileMode = properties.getStringValue(sConfigID + ".drawmode");
			int tileMode = SWTSkinUtils.getTileMode(sTileMode);
			painter = new SWTBGImagePainter(control, imageBGLeft, imageBGRight,
					imageBG, tileMode);
		} else {
			//System.out.println("setImage " + sConfigID + "  " + sSuffix);
			painter.setImage(imageBGLeft, imageBGRight, imageBG);
		}

		control.update();
	}

	// @see java.lang.Object#toString()
	public String toString() {
		String s = "SWTSkinObjectBasic {" + sID;

		if (!sID.equals(sConfigID)) {
			s += "/" + sConfigID;
		}

		s += ", " + type + "; parent="
				+ ((parent == null) ? null : parent.getSkinObjectID() + "}");

		return s;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#getSkin()
	public SWTSkin getSkin() {
		return skin;
	}

	// @see java.lang.Object#equals(java.lang.Object)
	public boolean equals(Object obj) {
		if (obj instanceof SWTSkinObject) {
			SWTSkinObject skinObject = (SWTSkinObject) obj;
			boolean bEquals = skinObject.getSkinObjectID().equals(sID);
			if (parent != null) {
				return bEquals && parent.equals(skinObject.getParent());
			}
			return bEquals;
		}

		return super.equals(obj);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setVisible(boolean)
	public void setVisible(boolean visible) {
		control.setVisible(visible);
		triggerListeners(visible ? SWTSkinObjectListener.EVENT_SHOW
				: SWTSkinObjectListener.EVENT_HIDE);
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setDefaultVisibility()
	public void setDefaultVisibility() {
		if (sConfigID == null) {
			return;
		}
		
		setVisible(properties.getStringValue(sConfigID + ".visible", "true").equalsIgnoreCase(
				"true"));
	}
	
	public boolean isVisible() {
		return control != null && !control.isDisposed() && control.isVisible();
	}

	public String switchSuffix(String suffix, int level, boolean walkUp) {
		if (walkUp) {
			SWTSkinObject parentSkinObject = parent;
			SWTSkinObject skinObject = this;

			// Move up the tree until propogation stops
			while ((parentSkinObject instanceof SWTSkinObjectContainer)
					&& ((SWTSkinObjectContainer) parentSkinObject).getPropogation()) {
				skinObject = parentSkinObject;
				parentSkinObject = parentSkinObject.getParent();
			}

			if (skinObject != this) {
				//System.out.println(sConfigID + suffix + "; walkup");

				skinObject.switchSuffix(suffix, level, false);
				return null;
			}
		}
		
		//System.out.println(SystemTime.getCurrentTime() + ": " + sConfigID + suffix + "; switchy");
		if (suffixes == null) {
			suffixes = new String[level];
		} else if (suffixes.length < level) {
			String[] newSuffixes = new String[level];
			System.arraycopy(suffixes, 0, newSuffixes, 0, suffixes.length);
			suffixes = newSuffixes;
		}
		suffixes[level - 1] = suffix;

		suffix = getSuffix();

		if (sConfigID == null || control == null || control.isDisposed()) {
			return suffix;
		}

		if (properties.getStringValue(sConfigID + ".visible", "true").equalsIgnoreCase(
				"false")) {
			setVisible(false);
		}
		
		Color color = properties.getColor(sConfigID + ".color" + suffix);
		if (color != null) {
			control.setBackground(color);
		}

		Color fg = properties.getColor(sConfigID + ".fgcolor" + suffix);
		if (fg != null) {
			control.setForeground(fg);
		}

		setBackground(sConfigID + ".background", suffix);

		String sCursor = properties.getStringValue(sConfigID + ".cursor");
		if (sCursor != null && sCursor.length() > 0) {
			if (sCursor.equalsIgnoreCase("hand")) {
				control.addListener(SWT.MouseEnter,
						skin.getHandCursorListener(control.getDisplay()));
				control.addListener(SWT.MouseExit,
						skin.getHandCursorListener(control.getDisplay()));
			}
		}

		String sTooltip = properties.getStringValue(sConfigID + ".tooltip" + suffix);
		if (sTooltip != null) {
			setTooltipAndChildren(control, sTooltip);
		}

		return suffix;
	}

	public String getSuffix() {
		String suffix = "";
		for (int i = 0; i < suffixes.length; i++) {
			suffix += suffixes[i];
		}
		return suffix;
	}

	public void setTooltipAndChildren(Control c, String sToolTip) {
		c.setToolTipText(sToolTip);
		if (c instanceof Composite) {
			Control[] children = ((Composite) c).getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				setTooltipAndChildren(control, sToolTip);
			}
		}
	}

	/**
	 * @return the properties
	 */
	public SWTSkinProperties getProperties() {
		return properties;
	}

	public void setProperties(SWTSkinProperties skinProperties) {
		this.properties = skinProperties;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#addListener(com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener)
	 */
	public void addListener(SWTSkinObjectListener listener) {
		listeners_mon.enter();
		try {
			listeners.add(listener);
		} finally {
			listeners_mon.exit();
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#removeListener(com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener)
	 */
	public void removeListener(SWTSkinObjectListener listener) {
		listeners_mon.enter();
		try {
			listeners.remove(listener);
		} finally {
			listeners_mon.exit();
		}
	}

	public SWTSkinObjectListener[] getListeners() {
		return (SWTSkinObjectListener[]) listeners.toArray(new SWTSkinObjectListener[0]);
	}

	public void triggerListeners(int eventType) {
		triggerListeners(eventType, null);
	}

	public void triggerListeners(int eventType, Object params) {
		// process listeners added locally
		listeners_mon.enter();
		try {
			for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
				try {
					SWTSkinObjectListener l = (SWTSkinObjectListener) iterator.next();
					l.eventOccured(this, eventType, params);
				} catch (Exception e) {
					Debug.out("Skin Event " + SWTSkinObjectListener.NAMES[eventType]
							+ " caused an error for listener added locally", e);
				}
			}
		} finally {
			listeners_mon.exit();
		}

		// process listeners added to skin
		SWTSkinObjectListener[] listeners = skin.getSkinObjectListeners(sViewID);
		for (int i = 0; i < listeners.length; i++) {
			try {
				SWTSkinObjectListener l = listeners[i];
				l.eventOccured(this, eventType, params);
			} catch (Exception e) {
				Debug.out("Skin Event " + SWTSkinObjectListener.NAMES[eventType]
						+ " caused an error for listener added to skin", e);
			}
		}
	}

	protected void setViewID(String viewID) {
		sViewID = viewID;
	}

	public String getViewID() {
		return sViewID;
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#dispose()
	public void dispose() {
		if (control != null && !control.isDisposed()) {
			control.dispose();
		}
	}
}
