/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.pieces;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.pluginsimpl.local.ui.SWT.SWTManagerImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class BlocksItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  /** Default Constructor */
  public BlocksItem() {
    super("blocks", TableManager.TABLE_TORRENT_PIECES);
    initializeAsGraphic(POSITION_LAST, 200);
  }

  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, TableCellDisposeListener
  {
    public Cell(TableCell cell) {
      cell.setFillCell(true);
      cell.addRefreshListener(this);
      cell.addDisposeListener(this);
    }

    public void dispose(TableCell cell) {
      Image img = ((TableCellCore)cell).getGraphicSWT();
      if (img != null && !img.isDisposed())
        img.dispose();
      cell.setGraphic(null);
      
      // listeners don't need to be removed on dispose (core removed them)
    }

    public void refresh(TableCell cell) {
      PEPiece piece = (PEPiece)cell.getDataSource();
      if (piece == null) {
        cell.setSortValue(0);
        return;
      }

      cell.setSortValue(piece.getCompleted());
      long lNumBlocks = piece.getNbBlocs();

      int newWidth = cell.getWidth();
      if (newWidth <= 0)
        return;
      int newHeight = cell.getHeight();
  
      int x1 = newWidth - 2;
      int y1 = newHeight - 3;
      if (x1 < 10 || y1 < 3)
        return;
      Image image = new Image(SWTManagerImpl.getSingleton().getDisplay(), 
                              newWidth, newHeight);
      Color blue = Colors.blues[Colors.BLUES_DARKEST];
      Color green = Colors.blues[Colors.BLUES_MIDLIGHT];
      Color downloaded = Colors.red;
      Color color;
      GC gcImage = new GC(image);
      gcImage.setForeground(Colors.grey);
      gcImage.drawRectangle(0, 0, x1 + 1, y1 + 1);
      int iPixelsPerBlock = (int) ((x1 + 1) / lNumBlocks);
      for (int i = 0; i < lNumBlocks; i++) {
        int nextWidth = iPixelsPerBlock;
        if (i == lNumBlocks - 1) {
          nextWidth = (int)( x1 - (iPixelsPerBlock * (lNumBlocks - 1)));
        }
        color = Colors.white;
  
        if (piece.getWritten()[i]) {
          color = blue;
        }
        
        else if (piece.getDownloaded()[i]) {
          color = downloaded;
        }
        
        else if (piece.getRequested()[i]) {
          color = green;
        }
  
        gcImage.setBackground(color);
        gcImage.fillRectangle(i * iPixelsPerBlock + 1,1,nextWidth,y1);
      }
      gcImage.dispose();

      Image oldImage = ((TableCellCore)cell).getGraphicSWT();

      ((TableCellCore)cell).setGraphic(image);
      if (oldImage != null && !oldImage.isDisposed())
        oldImage.dispose();
      
      gcImage.dispose();
    }
  }
}
