/*
 * Created on Jun 26, 2006 7:25:11 PM
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

import java.util.Properties;

import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.util.Debug;

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

	public Color getColor(String name) {
		if (DEBUG) {
			checkName(name);
		}

		Color val = properties.getColor(sCloneConfigID + name);
		if (val != null) {
			return val;
		}

		return properties.getColor(sTemplateConfigID + name);
	}

	public int[] getColorValue(String name) {
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
		}
		if (!name.equals(IGNORE_NAME)) {
			if (properties.getStringValue(sCloneConfigID + name) != null) {
				return properties.getIntValue(sCloneConfigID + name, def);
			}
		}
		return properties.getIntValue(sTemplateConfigID + name, def);
	}

	public Properties getProperties() {
		return properties.getProperties();
	}

	public String[] getStringArray(String name) {
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
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
		if (DEBUG) {
			checkName(name);
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
}
