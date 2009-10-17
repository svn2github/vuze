/**
 * Created on May 28, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.activities;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;

/**
 * @author TuxPaper
 * @created May 28, 2008
 *
 */
public class VuzeActivitiesConstants
{
	public static final String TYPEID_HEADER = "Header";

	public static final String TYPEID_VUZENEWS = "VUZE_NEWS_ITEM";

	public static final String TYPEID_CHANNEL_ANNOUNCE = "CHANNEL_ANNOUNCE";

	public static final String TYPEID_CONTENT_PROMO = "CONTENT_PROMO";

	public static final int SORT_DATE = 0;

	public static final int SORT_TYPE = 1;

	public static final Map SORT_TYPE_ORDER = new HashMap();

	public static VuzeActivitiesEntry[] HEADERS_SORTBY_TYPE;

	static {
		int pos = 0;

		//SORT_TYPE_ORDER.put(TYPEID_DL_ADDED, new Long(pos));
		//SORT_TYPE_ORDER.put(TYPEID_DL_COMPLETE, new Long(pos));
		//SORT_TYPE_ORDER.put(TYPEID_DL_REMOVE, new Long(pos));
		//pos++;

		SORT_TYPE_ORDER.put(TYPEID_VUZENEWS, new Long(pos));
		SORT_TYPE_ORDER.put(TYPEID_CHANNEL_ANNOUNCE, new Long(pos));
		SORT_TYPE_ORDER.put(TYPEID_CONTENT_PROMO, new Long(pos));
		pos++;

		HEADERS_SORTBY_TYPE = new VuzeActivitiesEntry[] {
			/*new VuzeActivitiesEntry(0,
					MessageText.getString("v3.activity.header.downloads"), null,
					TYPEID_DL_ADDED, TYPEID_HEADER, null),*/
			new VuzeActivitiesEntry(0,
					MessageText.getString("v3.activity.header.vuze.news"), null,
					TYPEID_VUZENEWS, TYPEID_HEADER, null),
		};

	}
}
