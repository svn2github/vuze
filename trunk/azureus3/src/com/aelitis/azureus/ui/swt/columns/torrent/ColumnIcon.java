/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnIcon extends CoreTableColumn implements
		TableCellAddedListener
{
	/**
	 * 
	 */
	public ColumnIcon(String sTableID) {
		super("TorrentIcon", sTableID);
		initializeAsGraphic(POSITION_LAST, 16);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener
	{

		public Cell(TableCell cell) {
			//cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);

			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				// Don't ever dispose of PathIcon, it's cached and may be used elsewhere

				Image icon;

				DiskManagerFileInfo[] fileInfo = dm.getDiskManagerFileInfo();
				if (fileInfo.length <= 1) {
  				icon = ImageRepository.getPathIcon(dm.getSaveLocation().toString(), true);
				} else {
					int idxBiggest = 0;
					long lBiggest = fileInfo[0].getLength();
					for (int i = 1; i < fileInfo.length; i++) {
						if (fileInfo[i].getLength() > lBiggest) {
							lBiggest = fileInfo[i].getLength();
							idxBiggest = i;
						}
					}
					icon = ImageRepository.getPathIcon(fileInfo[idxBiggest].getFile(true).getPath(), true);
				}
				// cheat for core, since we really know it's a TabeCellImpl and want to
				// use those special functions not available to Plugins
				((TableCellCore) cell).setGraphic(icon);
			}
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
				Image icon = ImageRepository.getPathIcon(dm.getSaveLocation().toString());
				// cheat for core, since we really know it's a TabeCellImpl and want to
				// use those special functions not available to Plugins
				((TableCellCore) cell).setGraphic(icon);
			}
		}
	}
}