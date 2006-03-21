/*
 * Created on Mar 21, 2006 3:09:00 PM
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
package org.gudy.azureus2.core3.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author TuxPaper
 * @created Mar 21, 2006
 *
 */
public class UrlUtils {
	/**
	 * test string for possibility that it's an URL.  Considers 40 byte hex 
	 * strings as URLs
	 * 
	 * @param sURL
	 * @return
	 */
	public static boolean isURL(String sURL) {
		if (sURL == null || sURL.length() < 5)
			return false;

		String sLower = sURL.toLowerCase();
		return sLower.startsWith("http://") || sLower.startsWith("https://")
				|| sLower.startsWith("magnet:") || sLower.startsWith("ftp://")
				|| sLower.matches("^[a-fA-F0-9]{40}$");
	}

	public static String parseTextForURL(String text) {
		// examples:
		// <A HREF=http://abc.om/moo>test</a>
		// <A style=cow HREF="http://abc.om/moo">test</a>
		// <a href="http://www.gnu.org/licenses/fdl.html" target="_top">moo</a>

		Pattern pat = Pattern.compile("<.*a\\s++.*href=\"?([^\\'\"\\s>]++).*",
				Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(text);
		if (m.find()) {
			return m.group(1);
		}

		if (UrlUtils.isURL(text))
			return text;

		return null;
	}
}
