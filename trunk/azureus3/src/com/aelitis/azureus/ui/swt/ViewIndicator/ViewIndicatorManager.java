/**
 * Created on Jul 8, 2008
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
 
package com.aelitis.azureus.ui.swt.ViewIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TuxPaper
 * @created Jul 8, 2008
 *
 */
public class ViewIndicatorManager
{
	public static List listeners = new ArrayList();
	
	public static void addListener(ViewIndicatorListener l) {
		listeners.add(l);
	}
	
	public static void refreshViewIndicator(ViewIndicator indicator) {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			ViewIndicatorListener l = (ViewIndicatorListener) array[i];
			l.viewIndicatorRefresh(indicator);
		}
	}

}
