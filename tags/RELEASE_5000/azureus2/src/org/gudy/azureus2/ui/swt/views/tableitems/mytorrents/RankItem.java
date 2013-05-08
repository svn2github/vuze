/*
 * File    : RankItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

/**
 * Torrent Position column.
 * 
 * One object for all rows to save memory
 * 
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class RankItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "#";

	private Image imgUp;

	private Image imgDown;

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT });
	}

	private boolean bInvalidByTrigger = false;
	
  /** Default Constructor */
  public RankItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 50, sTableID);
    setRefreshInterval(INTERVAL_INVALID_ONLY);
    AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				core.getGlobalManager().addListener(new GMListener());
			}
		});
    setMaxWidthAuto(true);
    setMinWidthAuto(true);

    ImageLoader imageLoader = ImageLoader.getInstance();
    imgUp = imageLoader.getImage("image.torrentspeed.up");
    imgDown = imageLoader.getImage("image.torrentspeed.down");
  }

  public void refresh(TableCell cell) {
  	bInvalidByTrigger = false;

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    long value = (dm == null) ? 0 : dm.getPosition();
    String text = "" + value;

  	boolean complete = dm == null ? false : dm.getAssumedComplete();
  	if (complete) {
  		value += 0x10000;
  	}
    
    cell.setSortValue(value);
    cell.setText(text);

    if (cell instanceof TableCellSWT) {
    	Image img = dm.getAssumedComplete() ? imgUp : imgDown;
    	((TableCellSWT)cell).setIcon(img);
    }

  }
  
  private class GMListener implements GlobalManagerListener {
    	DownloadManagerListener listener;
    	
    	public GMListener() {
    		 listener = new DownloadManagerListener() {
					public void completionChanged(DownloadManager manager, boolean bCompleted) {
					}

					public void downloadComplete(DownloadManager manager) {
					}

					public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
						/** We will be getting multiple position changes, but we only need
						 * to invalidate cells once.
						 */
						if (bInvalidByTrigger)
							return;
						RankItem.this.invalidateCells();
						bInvalidByTrigger = true;
					}

					public void stateChanged(DownloadManager manager, int state) {
					}
					public void
					filePriorityChanged( DownloadManager download, org.gudy.azureus2.core3.disk.DiskManagerFileInfo file )
					{	  
					}
    		 };
    	}
    	
			public void destroyed() {
			}

			public void destroyInitiated() {
				try {
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					gm.removeListener(this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}

			public void downloadManagerAdded(DownloadManager dm) {
				dm.addListener(listener);
			}

			public void downloadManagerRemoved(DownloadManager dm) {
				dm.removeListener(listener);
			}

			public void seedingStatusChanged(boolean seeding_only_mode, boolean b) {
			}
  }

}
