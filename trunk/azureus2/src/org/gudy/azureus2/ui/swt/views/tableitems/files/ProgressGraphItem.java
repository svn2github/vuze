/*
 * File    : CompletionItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.ui.SWT.SWTManagerImpl;

/** Torrent Completion Level Graphic Cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ProgressGraphItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  private static final int borderWidth = 1;

  /** Default Constructor */
  public ProgressGraphItem() {
    super("pieces", TableManager.TABLE_TORRENT_FILES);
    initializeAsGraphic(POSITION_LAST, 200);
  }

  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, TableCellDisposeListener
  {
    int lastPercentDone = 0;
    int lastWidth = 0;
    private long last_draw_time;

    public Cell(TableCell cell) {
      cell.addRefreshListener(this);
      cell.addDisposeListener(this);
    }

    public void dispose(TableCell cell) {
      Image img = cell.getGraphic();
      if (img != null && !img.isDisposed())
        img.dispose();
      cell.setGraphic(null);
    }



    public void refresh(TableCell cell) {
      DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
      int percentDone = 0;
      if (fileInfo != null && fileInfo.getLength() != 0)
        percentDone = (int)((1000 * fileInfo.getDownloaded()) / fileInfo.getLength());
      cell.setSortValue(percentDone);

      //Compute bounds ...
      Point ptNewSize = cell.getSize();

      if (ptNewSize.x == 0)
        return;

      int x1 = ptNewSize.x - borderWidth - 1;
      int y1 = ptNewSize.y - borderWidth - 1;
      if (x1 < 10 || y1 < 3) {
        return;
      }

      boolean bImageBufferValid = (lastPercentDone == percentDone) &&
                                  (lastWidth == ptNewSize.x) &&
                                  cell.isValid();
      if (bImageBufferValid) {
        return;
      }

      lastPercentDone = percentDone;
      lastWidth = ptNewSize.x;

      Image piecesImage = cell.getGraphic();

      if (piecesImage != null && !piecesImage.isDisposed())
        piecesImage.dispose();
      piecesImage = new Image(SWTManagerImpl.getSingleton().getDisplay(),
                              ptNewSize.x, ptNewSize.y);

      GC gcImage = new GC(piecesImage);

      if (fileInfo != null) {
        int firstPiece = fileInfo.getFirstPieceNumber();
        int nbPieces = fileInfo.getNbPieces();
  
        DiskManager manager = fileInfo.getDiskManager();
  
        boolean available[] = manager.getPiecesStatus();
  
        PEPeerManager pm = manager.getPeerManager();
  
        PEPiece[] pieces = pm==null?null:pm.getPieces();
  
        for (int i = 0; i < ptNewSize.x; i++) {
          int a0 = (i * nbPieces) / ptNewSize.x;
          int a1 = ((i + 1) * nbPieces) / ptNewSize.x;
          if (a1 == a0)
            a1++;
          if (a1 > nbPieces && nbPieces != 0)
            a1 = nbPieces;
          int nbAvailable = 0;
          boolean written   = false;
          boolean requested = false;
          if (firstPiece >= 0) {
            for (int j = a0; j < a1; j++){
              int this_index = j+firstPiece;
              if (available[this_index]) {
                nbAvailable++;
              }
              
              if (written || pieces == null)
                continue;
  
              PEPiece  piece = pieces[this_index];
              if (piece == null)
                continue;
  
              written = written || (piece.getLastWriteTime() + 500) > last_draw_time;
  
              if ((!written) && (!requested)) {
                boolean[] reqs = piece.getRequested();
  
                if ( reqs != null ) {
                  for (int k = 0; k < reqs.length; k++){
                    if (reqs[k]){
                      requested = true;
                      break;
                    }
                  }
                }
              }
  
            } // for j
          } else {
            nbAvailable = 1;
          }
  
          int index = (nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0);
          //System.out.print(index);
          gcImage.setBackground(written ? Colors.red
                                        : requested ? Colors.grey : Colors.blues[index]);
          gcImage.fillRectangle(i,1,1,ptNewSize.y-2);
        }
        gcImage.setForeground(
          (fileInfo.getDownloaded() == fileInfo.getLength()) ? Colors.blues[Colors.BLUES_DARKEST] 
                                                             : Colors.grey);
      } else {
        gcImage.setForeground(Colors.grey);
      }
      gcImage.drawRectangle(0, 0, ptNewSize.x - 1, ptNewSize.y - 1);

      gcImage.dispose();
      last_draw_time = SystemTime.getCurrentTime();

      cell.setGraphic(piecesImage);
    }
  }
}
