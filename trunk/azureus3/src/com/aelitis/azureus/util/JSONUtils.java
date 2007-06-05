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

import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


/**
 * @author TuxPaper
 * @created Feb 14, 2007
 *
 */
public class JSONUtils
{
	/**
	 * decodes JSON formatted text into a map.
	 * 
	 *  If the json text is not a map, a map with the key "value" will be returned.
	 *  the value of "value" will either be an List, String, Number, Boolean, or null
	 */
	public static Map decodeJSON(String json) {
		Object object = JSONValue.parse(json);
		if (object instanceof Map) {
			return (Map) object;
		}
		// could be : ArrayList, String, Number, Boolean
		Map map = new HashMap();
		map.put("value", object);
		return map;
	}
	
	public static Map encodeToJSONObject(Map map) {
		Map newMap = new JSONObject();
		
		for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
			Object key = (Object) iter.next();
			Object value = map.get(key);
			
			if ((value instanceof Map) && !(value instanceof JSONObject)) {
				value = encodeToJSONObject((Map)value);
			} else if ((value instanceof List) && !(value instanceof JSONArray)) {
				value = encodeToJSONArray((List)value);
			}
			
			newMap.put(key, value);
		}
		return newMap;
	}

	/**
	 * @param value
	 * @return
	 *
	 * @since 3.0.1.5
	 */
	private static List encodeToJSONArray(List list) {
		List newList = new JSONArray(list);

		for (int i = 0; i < newList.size(); i++) {
			Object value = newList.get(i);
			
			if ((value instanceof Map) && !(value instanceof JSONObject)) {
				newList.set(i, encodeToJSONObject((Map)value));
				
			} else if ((value instanceof List) && !(value instanceof JSONArray)) {
				newList.set(i, encodeToJSONArray((List)value));
			}
		}
		
		return newList;
	}
}
