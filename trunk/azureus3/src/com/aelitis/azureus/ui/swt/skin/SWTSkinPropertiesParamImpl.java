/*
 * Created on Jun 27, 2006 1:48:36 AM
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

/**
 * SWTSkinProperties delegator that always passes a set of parameters
 * to string requests.
 * 
 * @author TuxPaper
 * @created Jun 27, 2006
 *
 */
public class SWTSkinPropertiesParamImpl
	implements SWTSkinPropertiesParam
{
	private final SWTSkinProperties properties;

	private final String[] sCloneParams;

	/**
	 * @param properties
	 */
	public SWTSkinPropertiesParamImpl(SWTSkinProperties properties,
			String[] sCloneParams) {
		this.properties = properties;
		this.sCloneParams = sCloneParams;
	}

	public void addProperty(String name, String value) {
		properties.addProperty(name, value);
	}

	public Color getColor(String name) {
		return properties.getColor(name);
	}

	public int[] getColorValue(String name) {
		return properties.getColorValue(name);
	}

	public int getIntValue(String name, int def) {
		return properties.getIntValue(name, def);
	}

	public Properties getProperties() {
		return properties.getProperties();
	}

	public String[] getStringArray(String name, String[] params) {
		return properties.getStringArray(name, params);
	}

	public String[] getStringArray(String name) {
		return properties.getStringArray(name, sCloneParams);
	}

	public String getStringValue(String name, String def) {
		return properties.getStringValue(name, sCloneParams, def);
	}

	public String getStringValue(String name, String[] params, String def) {
		return properties.getStringValue(name, params, def);
	}

	public String getStringValue(String name, String[] params) {
		return properties.getStringValue(name, params);
	}

	public String getStringValue(String name) {
		return properties.getStringValue(name, sCloneParams);
	}

	public String[] getParamValues() {
		return sCloneParams;
	}
}
