/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author TuxPaper
 * @created Feb 14, 2007
 *
 */
public class JSONUtils
{
	public static int getJSONInt(JSONObject json, String key, int def) {
		try {
			return json.getInt(key);
		} catch (JSONException e) {
			return def;
		}
	}

	public static String getJSONString(JSONObject json, String key, String def) {
		try {
			return json.getString(key);
		} catch (JSONException e) {
			return def;
		}
	}

	public static boolean getJSONBoolean(JSONObject json, String key, boolean def) {
		try {
			return json.getBoolean(key);
		} catch (JSONException e) {
			return def;
		}
	}

}
