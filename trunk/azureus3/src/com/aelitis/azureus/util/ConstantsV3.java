/*
 * Created on Aug 30, 2006
 * Created by Alon Rohter
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.util;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;

/**
 * @deprecated Only for EMP
 */
public class ConstantsV3
{
	/** @deprecated Used by UMP only.. */
	public static boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;

	/** @deprecated Used by UMP only.. */
	public static boolean isWindows = org.gudy.azureus2.core3.util.Constants.isWindows;

	/** @deprecated Use {@link ConstantsVuze#DEFAULT_CONTENT_NETWORK_ID} **/
	public static final ContentNetwork DEFAULT_CONTENT_NETWORK = ConstantsVuze.getDefaultContentNetwork();

	/** @deprecated Used by UMP only.. */
	public static final String URL_PREFIX = DEFAULT_CONTENT_NETWORK.getServiceURL(ContentNetwork.SERVICE_SITE);
}
