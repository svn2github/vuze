/*
 * Created on Jun 26, 2006 7:25:11 PM
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt.skin;

import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Simple extension of SWTSkinProperties that first checks the original
 * cloning id before checking the keys that it's cloning.
 * <P>
 * Cloned Skin Objects will be calling this class with a Config ID of "" plus
 * whatever property name string they add on.
 * 
 * @author TuxPaper
 * @created Jun 26, 2006
 *
 */
public class SWTSkinPropertiesClone
	implements SWTSkinPropertiesParam
{
	private static final String IGNORE_NAME = ".type";

	private static final boolean DEBUG = true;

	private final SWTSkinProperties properties;

	private final String sCloneConfigID;

	private final String sTemplateConfigID;

	private final String[] sCloneParams;

	/**
	 * Initialize
	 * 
	 * @param properties Where to read properties from
	 * @param sCloneConfigID The config key that told us to clone something
	 */
	public SWTSkinPropertiesClone(SWTSkinProperties properties,
			String sCloneConfigID, String[] sCloneParams) {
		this.properties = properties;
		this.sCloneConfigID = sCloneConfigID;
		this.sCloneParams = sCloneParams;
		this.sTemplateConfigID = sCloneParams[0];
	}

	/**
	 * @param name
	 */
	private void checkName(String name) {
		if (name.startsWith(sTemplateConfigID)) {
			System.err.println(name + " shouldn't have template prefix of "
					+ sTemplateConfigID + "; " + Debug.getStackTrace(true, false));
		}

		if (name.startsWith(sCloneConfigID)) {
			System.err.println(name + " shouldn't have clone prefix of "
					+ sCloneConfigID + "; " + Debug.getStackTrace(true, false));
		}
	}

	public void addProperty(String name, String value) {
		properties.addProperty(sCloneConfigID + name, value);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinProperties#getColorWithAlpha(java.lang.String)
	public SWTColorWithAlpha getColorWithAlpha(String name) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getColorWithAlpha(name);
		}


		SWTColorWithAlpha val = properties.getColorWithAlpha(sCloneConfigID + name);
		if (val != null) {
			return val;
		}

		return properties.getColorWithAlpha(sTemplateConfigID + name);
	}

	public Color getColor(String name) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getColor(name);
		}


		Color val = properties.getColor(sCloneConfigID + name);
		if (val != null) {
			return val;
		}

		return properties.getColor(sTemplateConfigID + name);
	}

	public int[] getColorValue(String name) {
		if (name == null) {
			return new int[] { -1, -1, -1 };
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getColorValue(name);
		}

		if (!name.equals(IGNORE_NAME)) {
			int[] val = properties.getColorValue(sCloneConfigID + name);
			if (val[0] < 0) {
				return val;
			}
		}

		return properties.getColorValue(sTemplateConfigID + name);
	}

	public int getIntValue(String name, int def) {
		if (name == null) {
			return def;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getIntValue(name, def);
		}

		if (!name.equals(IGNORE_NAME)) {
			if (properties.getStringValue(sCloneConfigID + name) != null) {
				return properties.getIntValue(sCloneConfigID + name, def);
			}
		}
		return properties.getIntValue(sTemplateConfigID + name, def);
	}

	public String[] getStringArray(String name) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringArray(name);
		}

		if (!name.equals(IGNORE_NAME)) {
			String[] val = properties.getStringArray(sCloneConfigID + name,
					sCloneParams);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringArray(sTemplateConfigID + name, sCloneParams);
	}

	public String getStringValue(String name, String def) {
		if (name == null) {
			return def;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringValue(name, def);
		}
		
		if (!name.equals(IGNORE_NAME)) {
			String val = properties.getStringValue(sCloneConfigID + name,
					sCloneParams);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringValue(sTemplateConfigID + name, sCloneParams,
				def);
	}

	public String getStringValue(String name) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringValue(name);
		}

		if (!name.equals(IGNORE_NAME)) {
			String val = properties.getStringValue(sCloneConfigID + name,
					sCloneParams);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringValue(sTemplateConfigID + name, sCloneParams);
	}

	public String[] getStringArray(String name, String[] params) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringArray(name, params);
		}

		if (!name.equals(IGNORE_NAME)) {
			String[] val = properties.getStringArray(sCloneConfigID + name, params);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringArray(sTemplateConfigID + name, params);
	}

	public String getStringValue(String name, String[] params, String def) {
		if (name == null) {
			return def;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringValue(name, params, def);
		}

		if (!name.equals(IGNORE_NAME)) {
			String val = properties.getStringValue(sCloneConfigID + name, params);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringValue(sTemplateConfigID + name, params, def);
	}

	public String getStringValue(String name, String[] params) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getStringValue(name, params);
		}

		if (!name.equals(IGNORE_NAME)) {
			String val = properties.getStringValue(sCloneConfigID + name, params);
			if (val != null) {
				return val;
			}
		}

		return properties.getStringValue(sTemplateConfigID + name, params);
	}

	public SWTSkinProperties getOriginalProperties() {
		return properties;
	}

	public String[] getParamValues() {
		return sCloneParams;
	}
	
	// @see com.aelitis.azureus.ui.skin.SkinProperties#getBooleanValue(java.lang.String, boolean)
	public boolean getBooleanValue(String name, boolean def) {
		if (name == null) {
			return def;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getBooleanValue(name, def);
		}

		if (!name.equals(IGNORE_NAME)) {
			if (properties.getStringValue(sCloneConfigID + name) != null) {
				return properties.getBooleanValue(sCloneConfigID + name, def);
			}
		}
		return properties.getBooleanValue(sTemplateConfigID + name, def);
	}
	
	// @see com.aelitis.azureus.ui.skin.SkinProperties#clearCache()
	public void clearCache() {
		properties.clearCache();
	}
	
	// @see com.aelitis.azureus.ui.skin.SkinProperties#contains(java.lang.String)
	public boolean hasKey(String name) {
		return properties.hasKey(name);
	}

	public Color getColor(String name, Color def) {
		Color color = getColor(name);
		if (color == null) {
			return def;
		}
		return color;
	}
	
	// @see com.aelitis.azureus.ui.skin.SkinProperties#getEmHeightPX()
	public int getEmHeightPX() {
		return properties.getEmHeightPX();
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinProperties#getPxValue(java.lang.String, int)
	public int getPxValue(String name, int def) {
		String value = getStringValue(name, (String) null);
		if (value == null) {
			return def;
		}

		int result = def;
		try {
			if (value.endsWith("rem")) {
				float em = Float.parseFloat(value.substring(0, value.length() - 3));

				result = (int) (properties.getEmHeightPX() * em);
			} else {
				result = Integer.parseInt(value);
				result = Utils.adjustPXForDPI(result);
			}
		} catch (NumberFormatException e) {
			// ignore error.. it might be valid to store a non-numeric..
			//e.printStackTrace();
		}
		return result;
	}

	// @see com.aelitis.azureus.ui.skin.SkinProperties#getReferenceID(java.lang.String)
	public String getReferenceID(String name) {
		if (name == null) {
			return null;
		}
		if (DEBUG) {
			checkName(name);
		}
		if (name.length() > 0 && name.charAt(0) != '.') {
			return properties.getReferenceID(name);
		}

		if (!name.equals(IGNORE_NAME)) {
			String val = properties.getReferenceID(sCloneConfigID + name);
			if (val != null) {
				return val;
			}
		}

		return properties.getReferenceID(sTemplateConfigID + name);
	}
	
	// @see com.aelitis.azureus.ui.skin.SkinProperties#addResourceBundle(java.util.ResourceBundle)
	public void addResourceBundle(ResourceBundle subBundle, String skinPath) {
		properties.addResourceBundle(subBundle, skinPath);
	}
	
	public void addResourceBundle(ResourceBundle subBundle, String skinPath,
			ClassLoader loader) {
		properties.addResourceBundle(subBundle, skinPath,loader);
	}
	// @see com.aelitis.azureus.ui.skin.SkinProperties#getClassLoader()
	public ClassLoader getClassLoader() {
		return properties.getClassLoader();
	}

}
