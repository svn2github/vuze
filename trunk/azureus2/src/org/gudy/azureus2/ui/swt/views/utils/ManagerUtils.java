/*
 * File    : ManagerUtils.java
 * Created : 7 déc. 2003}
 * By      : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;

/**
 * @author Olivier
 *
 */
public class ManagerUtils {
  
  public static void run(DownloadManager dm) {
    if(dm != null) {
      Program.launch(dm.getFullName());
    }
  }
  
  public static boolean isStartable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state != DownloadManager.STATE_STOPPED) {
      return false;
    }
    return true;
  }
  
  public static boolean isStopable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state == DownloadManager.STATE_STOPPED) {
      return false;
    }
    return true;
  }
  
  public static boolean isRemoveable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state != DownloadManager.STATE_STOPPED
        && state != DownloadManager.STATE_QUEUED
        && state != DownloadManager.STATE_ERROR ){
       
      return false;
    }
    return true;
  }
  
  public static void 
  host(
  	AzureusCore		azureus_core,
	DownloadManager dm,
	Composite 		panel) 
  {
    if(dm == null)
      return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().hostTorrent(torrent);
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  public static void 
  publish(
  		AzureusCore		azureus_core,
		DownloadManager dm,
		Composite		 panel) 
  {
    if(dm == null)
     return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().publishTorrent(torrent);
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  public static void remove(DownloadManager dm)
  	throws GlobalManagerDownloadRemovalVetoException{
    if (isRemoveable(dm)) {
      GlobalManager globalManager = dm.getGlobalManager();
      globalManager.removeDownloadManager(dm);
    }
  }
  
  public static void start(DownloadManager dm) {
    if (dm != null && dm.getState() == DownloadManager.STATE_STOPPED) {
      dm.setState(DownloadManager.STATE_WAITING);
    }
  }

  public static void queue(DownloadManager dm,Composite panel) {
    if (dm != null) {
    	if (dm.getState() == DownloadManager.STATE_STOPPED)
      	dm.setState(DownloadManager.STATE_QUEUED);
      else if (dm.getState() == DownloadManager.STATE_DOWNLOADING || dm.getState() == DownloadManager.STATE_SEEDING) {
      	stop(dm,panel,DownloadManager.STATE_QUEUED);
      }
    }
  }
  
  public static void stop(DownloadManager dm,Composite panel) {
  	stop(dm, panel, DownloadManager.STATE_STOPPED);
  }
  
  public static void stop(DownloadManager dm,Composite panel,int stateAfterStopped) {
    if (dm != null && dm.getState() != DownloadManager.STATE_STOPPED && dm.getState() != stateAfterStopped) {
      if (dm.getState() == DownloadManager.STATE_SEEDING
          && dm.getStats().getShareRatio() >= 0
          && dm.getStats().getShareRatio() < 1000
          && COConfigurationManager.getBooleanParameter("Alert on close", true)) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
        mb.setText(MessageText.getString("seedmore.title"));
        mb.setMessage(
            MessageText.getString("seedmore.shareratio")
            + (dm.getStats().getShareRatio() / 10)
            + "%.\n"
            + MessageText.getString("seedmore.uploadmore"));
        int action = mb.open();
        if (action == SWT.YES)
          dm.stopIt(stateAfterStopped);
      }
      else {
        dm.stopIt(stateAfterStopped);
      }
    }
  }
  
}
