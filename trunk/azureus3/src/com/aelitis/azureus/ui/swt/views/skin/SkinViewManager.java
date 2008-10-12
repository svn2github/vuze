/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

/**
 * @author TuxPaper
 * @created Oct 6, 2006
 *
 */
public class SkinViewManager
{

	private static Map skinViews = new HashMap();

	private static Map skinIDs = new HashMap();
	
	private static List listeners = new ArrayList();
	
	/**
	 * @param key
	 * @param skinView
	 */
	public static void add(SkinView skinView) {
		Object object = skinViews.get(skinView.getClass());
		if (object instanceof SkinView[]) {
			SkinView[] oldSkinViews = (SkinView[])object;
			SkinView[] newSkinViews = new SkinView[oldSkinViews.length + 1];
			System.arraycopy(oldSkinViews, 0, newSkinViews, 0, oldSkinViews.length);
			newSkinViews[oldSkinViews.length] = skinView;
			
			skinViews.put(skinView.getClass(), newSkinViews);
		} else if (object != null) {
			Object[] newObjs = new SkinView[] { (SkinView) object, skinView };
			skinViews.put(skinView.getClass(), newObjs);
		} else {
			skinViews.put(skinView.getClass(), skinView);
		}
		if (skinView.getMainSkinObject() != null) {
			skinIDs.put(skinView.getMainSkinObject().getSkinObjectID(), skinView);
		}
		
		triggerListeners(skinView);
	}

	/**
	 * @param string
	 * @return
	 */
	public static SkinView getByClass(Class cla) {
		SkinView sv = null;
		Object object = skinViews.get(cla);
		if (object == null) {
			return null;
		}

		if (object instanceof SkinView[]) {
			SkinView[] svs = (SkinView[]) object;
			for (int i = 0; i < svs.length; i++) {
				sv = svs[i];
	  		SWTSkinObject so = sv.getMainSkinObject();
	  		if (so != null && !so.isDisposed()) {
	  			break;
	  		} // else TODO remove
			}
		} else {
			sv = (SkinView) object;
		}

		if (sv != null) {
  		SWTSkinObject so = sv.getMainSkinObject();
  		if (so != null && so.isDisposed()) {
  			// TODO remove
  			return null;
  		}
		}
		return sv;
	}
	
	public static SkinView[] getMultiByClass(Class cla) {
		Object object = skinViews.get(cla);
		if (object instanceof Object[]) {
			return (SkinView[]) ((Object[]) object);
		}
		return new SkinView[] {
			(SkinView) object
		};
	}

	public static SkinView getBySkinObjectID(String id) {
		SkinView sv = (SkinView) skinIDs.get(id);
		if (sv != null) {
  		SWTSkinObject so = sv.getMainSkinObject();
  		if (so != null && so.isDisposed()) {
  			// TODO remove
  			return null;
  		}
		}
		return sv;
	}
	
	public static void addListener(SkinViewManagerListener l) {
		listeners.add(l);
	}
	
	private static void triggerListeners(SkinView skinView) {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			SkinViewManagerListener l = (SkinViewManagerListener) array[i];
			l.skinViewAdded(skinView);
		}
	}
	
	public static interface SkinViewManagerListener {
		public void skinViewAdded(SkinView skinview);
	}
}
