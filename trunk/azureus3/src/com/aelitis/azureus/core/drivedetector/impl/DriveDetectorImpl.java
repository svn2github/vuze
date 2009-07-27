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

	public void addListener(DriveDetectedListener l) {
		mon_listListeners.enter();
		try {
			if (!listListeners.contains(l)) {
				listListeners.add(l);
			}
		} finally {
			mon_listListeners.exit();
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
		mon_listListeners.enter();
		try {
			for (DriveDetectedListener l : listListeners) {
				l.driveDetected(new DriveDetectedInfoImpl(location));
			}
		} finally {
			mon_listListeners.exit();
		}
	}

	public void driveRemoved(File location) {
		mon_listListeners.enter();
		try {
			for (DriveDetectedListener l : listListeners) {
				l.driveRemoved(new DriveDetectedInfoImpl(location));
			}
		} finally {
			mon_listListeners.exit();
		}
	}
}
