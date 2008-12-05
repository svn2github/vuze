/**
 * Created on Dec 2, 2008
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
 
package com.aelitis.azureus.ui.swt.utils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.util.ConstantsV3;

/**
 * @author TuxPaper
 * @created Dec 2, 2008
 *
 */
public class ContentNetworkUI
{
	public static ContentNetwork getContentNetworkFromTarget(String target) {
		ContentNetwork cn = null;
		if (target.startsWith("ContentNetwork.")) {
			long networkID = Long.parseLong(target.substring(15));
			cn = ContentNetworkManagerFactory.getSingleton().getContentNetwork(networkID);
		}
		
		if (cn == null) {
			cn = ConstantsV3.DEFAULT_CONTENT_NETWORK;
		}
		return cn;
	}
	
	public static String getTarget(ContentNetwork cn) {
		return "ContentNetwork." + cn.getID();
	}
}
