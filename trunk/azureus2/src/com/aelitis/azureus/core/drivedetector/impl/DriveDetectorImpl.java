/*
 * Created on Jul 26, 2009 5:18:54 PM
 * Copyright (C) 2009 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.drivedetector.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AEMonitor2;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.drivedetector.*;

/**
 * @author TuxPaper
 * @created Jul 26, 2009
 *
 */
public class DriveDetectorImpl
	implements DriveDetector
{
	private List<DriveDetectedListener> listListeners = new ArrayList<DriveDetectedListener>(1);
	private AEMonitor2 mon_listListeners = new AEMonitor2("listListeners");
	
	private List<File> listDrives = new ArrayList<File>(1); 
	private AEMonitor2 mon_listDrives = new AEMonitor2("listDrives");

	public void addListener(DriveDetectedListener l) {
		mon_listListeners.enter();
		try {
			if (!listListeners.contains(l)) {
				listListeners.add(l);
			} else {
				// already added, skip trigger
				return;
			}
		} finally {
			mon_listListeners.exit();
		}
		
		File[] drives = listDrives.toArray(new File[0]);
		for (File drive : drives) {
			try {
				l.driveDetected(new DriveDetectedInfoImpl(drive));
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void removeListener(DriveDetectedListener l) {
		mon_listListeners.enter();
		try {
			listListeners.remove(l);
		} finally {
			mon_listListeners.exit();
		}
	}

	public void driveDetected(File location) {
		mon_listDrives.enter();
		try {
			if (!listDrives.contains(location)) {
				listDrives.add(location);
			} else {
				// not there, no trigger
				return;
			}
		} finally {
			mon_listDrives.exit();
		}
		
		mon_listListeners.enter();
		try {
			for (DriveDetectedListener l : listListeners) {
				try {
					l.driveDetected(new DriveDetectedInfoImpl(location));
  			} catch (Exception e) {
  				Debug.out(e);
  			}
			}
		} finally {
			mon_listListeners.exit();
		}
	}

	public void driveRemoved(File location) {
		mon_listDrives.enter();
		try {
			if (!listDrives.remove(location)) {
				// not there, no trigger
				return;
			}
		} finally {
			mon_listDrives.exit();
		}
		
		mon_listListeners.enter();
		try {
			for (DriveDetectedListener l : listListeners) {
				try {
					l.driveRemoved(new DriveDetectedInfoImpl(location));
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		} finally {
			mon_listListeners.exit();
		}
	}
}
