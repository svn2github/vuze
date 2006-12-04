/*
 * Created on Jun 1, 2006 4:16:52 PM
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * @author TuxPaper
 * @created Jun 1, 2006
 *
 */
public class SWTSkinPropertiesImpl extends SkinPropertiesImpl implements
		SWTSkinProperties
{
	private static Map colorMap = new HashMap();

	private static Map mapFallBack = new HashMap();

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinProperties#getColor(java.lang.String)
	public Color getColor(String sID) {
		Color color;
		if (colorMap.containsKey(sID)) {
			return (Color) colorMap.get(sID);
		}

		try {
			int[] rgb = getColorValue(sID);
			if (rgb[0] > -1) {
				color = ColorCache.getColor(Display.getCurrent(), rgb[0], rgb[1],
						rgb[2]);
			} else {
				color = null;
			}
		} catch (Exception e) {
			//				IMP.getLogger().log(LoggerChannel.LT_ERROR,
			//						"Failed loading color : color." + colorNames[i]);
			color = null;
		}

		if (color == null) {
			String sFallBackID = (String) mapFallBack.get(sID);
			if (sFallBackID != null) {
				color = getColor(sFallBackID);
			}
		}

		colorMap.put(sID, color);

		return color;
	}
}
