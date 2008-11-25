/**
 * Created on Oct 16, 2008
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
 
package com.aelitis.azureus.util;


/**
 * Stupid EMP uses this class.  Who wrote such a crappy plugin.
 * oh wait, it was me :(
 * 
 * @author TuxPaper
 * @created Oct 16, 2008
 *
 * @deprecated
 */
public class Constants
{

	public static final boolean isOSX = ConstantsV3.isOSX;
	public static final boolean isWindows = ConstantsV3.isWindows;
	public static final String URL_COMMENTS = ConstantsV3.URL_COMMENTS;
	public static final String URL_PREFIX = ConstantsV3.URL_PREFIX;
	public static String URL_SUFFIX; 
	
	public static void update() {
		URL_SUFFIX = ConstantsV3.URL_SUFFIX;
	}
}
