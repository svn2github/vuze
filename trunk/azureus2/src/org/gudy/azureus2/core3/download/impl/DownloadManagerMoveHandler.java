/*
 * Created on 23 May 2008
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.core3.download.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

/**
 * @author Allan Crooks
 *
 */
public class DownloadManagerMoveHandler {
	
	public static SaveLocationManager CURRENT_HANDLER = DownloadManagerDefaultPaths.DEFAULT_HANDLER;
    
    // Helper log functions.
	static void logInfo(String message, DownloadManager dm) {
		LogRelation lr = (dm instanceof LogRelation) ? (LogRelation)dm : null;
		if (lr == null) {return;}
		if (!Logger.isEnabled()) {return;}
		Logger.log(new LogEvent(lr, LogIDs.CORE, LogEvent.LT_INFORMATION, message));
	}

	static void logWarn(String message, DownloadManager dm) {
		LogRelation lr = (dm instanceof LogRelation) ? (LogRelation)dm : null;
		if (lr == null) {return;}
		if (!Logger.isEnabled()) {return;}
		Logger.log(new LogEvent(lr, LogIDs.CORE, LogEvent.LT_WARNING, message));
	}
	
	static void logError(String message, DownloadManager dm, Throwable e) {
		LogRelation lr = (dm instanceof LogRelation) ? (LogRelation)dm : null;
		if (lr == null) {return;}
		if (!Logger.isEnabled()) {return;}
		Logger.log(new LogEvent(lr, LogIDs.CORE, message, e));
	}
	
	static String describe(DownloadManager dm) {
		if (dm == null) {return "";}
		return "\"" + dm.getDisplayName() + "\"";
	}

	private static boolean isApplicableDownload(DownloadManager dm) {
		if (!dm.isPersistent()) {
			logInfo(describe(dm) + " is not persistent.", dm);
			return false;
		}
		
		if (dm.getDownloadState().getFlag(DownloadManagerState.FLAG_DISABLE_AUTO_FILE_MOVE)) {
			logInfo(describe(dm) + " has exclusion flag set.", dm);
			return false;
		}
		
		return true;
	} 

	public static SaveLocationChange onInitialisation(DownloadManager dm) {
		if (!isApplicableDownload(dm)) {return null;}
		try {return CURRENT_HANDLER.onInitialization(PluginCoreUtils.wrap(dm), true);}
		catch (Exception e) {
			logError("Error trying to determine initial download location.", dm, e);
			return null;
		}
	}
	
	public static SaveLocationChange onRemoval(DownloadManager dm) {
		if (!isApplicableDownload(dm)) {return null;}
		try {return CURRENT_HANDLER.onRemoval(PluginCoreUtils.wrap(dm), true);}
		catch (Exception e) {
			logError("Error trying to determine on-removal location.", dm, e);
			return null;
		}
	}
	
	public static SaveLocationChange onCompletion(DownloadManager dm) {
		if (!isApplicableDownload(dm)) {return null;}
		
		if (dm.getDownloadState().getFlag(DownloadManagerState.FLAG_MOVE_ON_COMPLETION_DONE)) {
			logInfo("Completion flag already set on " + describe(dm) + ", skip move-on-completion behaviour.", dm);
			return null;
		}
		
		SaveLocationChange sc;
		try {sc = CURRENT_HANDLER.onCompletion(PluginCoreUtils.wrap(dm), true);}
		catch (Exception e) {
			logError("Error trying to determine on-completion location.", dm, e);
			return null;
		}
		
		logInfo("Setting completion flag on " + describe(dm) + ", may have been set before.", dm);
		dm.getDownloadState().setFlag(DownloadManagerState.FLAG_MOVE_ON_COMPLETION_DONE, true);
		return sc;
	}
	
	public static boolean canGoToCompleteDir(DownloadManager dm) {
		if (!dm.isDownloadComplete(false)) {return false;}
		return COConfigurationManager.getBooleanParameter("Move Completed When Done");
	}
	
	public static SaveLocationChange recalculatePath(DownloadManager dm) {
		Download download = PluginCoreUtils.wrap(dm);
		SaveLocationChange result = null;
		if (canGoToCompleteDir(dm)) {
			result = CURRENT_HANDLER.onCompletion(download, false);
		}
		if (result == null) {
			result = CURRENT_HANDLER.onInitialization(download, false);
		}
		return result;
	}

	
}
