/*
 * File    : PiecesItem.java
 * Created : 24 nov. 2003
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

package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.ui.SWT.SWTManagerImpl;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/19: modified to TableCellAdapter)
 */
public class PiecesItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  private final static int INDEX_COLOR_FADEDSTARTS = Colors.BLUES_DARKEST + 1;
  // only supports 0 or 1 border width
  private final static int borderHorizontalSize = 1;
  private final static int borderVerticalSize = 1;
  private final static int borderSplit = 1;
  private final static int completionHeight = 2;

  /** Default Constructor */
  public PiecesItem() {
    super("pieces", TableManager.TABLE_TORRENT_PEERS);
    initializeAsGraphic(POSITION_INVISIBLE, 100);
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
      cell.setGraphic(null);
      // Named infoObj so code can be copied easily to the other PiecesItem
      PEPeer infoObj = (PEPeer)cell.getDataSource();
      if (infoObj == null)
        return;
  
      Image img = (Image)infoObj.getData("PiecesImage");
      if (img != null && !img.isDisposed())
        img.dispose();
  
      infoObj.setData("PiecesImageBuffer", null);
      infoObj.setData("PiecesImage", null);
    }

    public void refresh(TableCell cell) {
      /* Notes:
       * We store our image and imageBufer in PEPeer using
       * setData & getData.
       */

      // Named infoObj so code can be copied easily to the other PiecesItem
      PEPeer infoObj = (PEPeer)cell.getDataSource();
      long lCompleted = (infoObj == null) ? 0 : infoObj.getPercentDone();
      cell.setSortValue(lCompleted);
      if (infoObj == null)
        return;
  
      //Compute bounds ...
      int newWidth = cell.getWidth();
      if (newWidth <= 0)
        return;
      int newHeight = cell.getHeight();
  
      DownloadManager dm = infoObj.getManager().getDownloadManager();
  
      int x0 = borderVerticalSize;
      int x1 = newWidth - 1 - borderVerticalSize;
      int y0 = completionHeight + borderHorizontalSize + borderSplit;
      int y1 = newHeight - 1 - borderHorizontalSize;
      int drawWidth = x1 - x0 + 1;
      if (drawWidth < 10 || y1 < 3)
        return;
      boolean bImageBufferValid = true;
      int[] imageBuffer = (int [])infoObj.getData("PiecesImageBuffer");
      if (imageBuffer == null || imageBuffer.length != drawWidth) {
        imageBuffer = new int[drawWidth];
        bImageBufferValid = false;
      }
  
      Image image = (Image)infoObj.getData("PiecesImage");
      GC gcImage;
      boolean bImageChanged;
      Rectangle imageBounds;
      if (image == null || image.isDisposed()) {
        bImageChanged = true;
      } else {
        imageBounds = image.getBounds();
        bImageChanged = imageBounds.width != newWidth ||
                        imageBounds.height != newHeight;
      }
      if (bImageChanged) {
        if (image != null && !image.isDisposed()) {
          image.dispose();
        }
        image = new Image(SWTManagerImpl.getSingleton().getDisplay(),
                          newWidth, newHeight);
        imageBounds = image.getBounds();
        bImageBufferValid = false;
  
        // draw border
        gcImage = new GC(image);
        gcImage.setForeground(Colors.grey);
        if (borderHorizontalSize > 0) {
          if (borderVerticalSize > 0) {
            gcImage.drawRectangle(0, 0, newWidth - 1, newHeight - 1);
          } else {
            gcImage.drawLine(0, 0, newWidth - 1, 0);
            gcImage.drawLine(0, newHeight -1, newWidth - 1, newHeight - 1);
          }
        } else if (borderVerticalSize > 0) {
          gcImage.drawLine(0, 0, 0, newHeight - 1);
          gcImage.drawLine(newWidth - 1, 0, newWidth - 1, newHeight - 1);
        }
  
        if (borderSplit > 0) {
          gcImage.setForeground(Colors.white);
          gcImage.drawLine(x0, completionHeight + borderHorizontalSize,
                           x1, completionHeight + borderHorizontalSize);
        }
      } else {
        gcImage = new GC(image);
      }
  
      boolean available[] = infoObj.getAvailable();
      
      if (available != null && available.length > 0) {
      try {
        boolean pieces[] = dm.getPiecesStatus();
        int nbComplete = 0;
        int nbPieces = available.length;
        int a0;
        int a1 = 0;
        for (int i = 0; i < drawWidth; i++) {
          if (i == 0) {
            // always start out with one piece
            a0 = 0;
            a1 = nbPieces / drawWidth;
            if (a1 == 0)
              a1 = 1;
          } else {
            // the last iteration, a1 will be nbPieces
            a0 = a1;
            a1 = ((i + 1) * nbPieces) / (drawWidth);
          }
  
          int index;
          int nbNeeded = 0;
  
          if (a1 <= a0) {
            index = imageBuffer[i - 1];
          } else {
            int nbAvailable = 0;
            for (int j = a0; j < a1; j++) {
              if (available[j]) {
              	if (!pieces[j]) {
              	  nbNeeded++;
              	}
                nbAvailable++;
              }
            }
            nbComplete += nbAvailable;
            index = (nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0);
            if (nbNeeded <= nbAvailable / 2)
              index += INDEX_COLOR_FADEDSTARTS;
          }
  
          if (imageBuffer[i] != index) {
            imageBuffer[i] = index;
            if (bImageBufferValid) {
              bImageChanged = true;
              if (imageBuffer[i] >= INDEX_COLOR_FADEDSTARTS)
                gcImage.setForeground(Colors.faded[index - INDEX_COLOR_FADEDSTARTS]);
              else
                gcImage.setForeground(Colors.blues[index]);
              gcImage.drawLine(i + x0, y0, i + x0, y1);
            }
          }
        }
        if (!bImageBufferValid) {
          int iLastIndex = imageBuffer[0];
          int iWidth = 1;
          for (int i = 1; i < drawWidth; i++) {
            if (iLastIndex == imageBuffer[i]) {
              iWidth++;
            } else {
              if (iLastIndex >= INDEX_COLOR_FADEDSTARTS)
                gcImage.setBackground(Colors.faded[iLastIndex - INDEX_COLOR_FADEDSTARTS]);
              else
                gcImage.setBackground(Colors.blues[iLastIndex]);
              gcImage.fillRectangle(i - iWidth + x0, y0, iWidth, y1 - y0 + 1);
              iWidth = 1;
              iLastIndex = imageBuffer[i];
            }
          }
          if (iLastIndex >= INDEX_COLOR_FADEDSTARTS)
            gcImage.setBackground(Colors.faded[iLastIndex - INDEX_COLOR_FADEDSTARTS]);
          else
            gcImage.setBackground(Colors.blues[iLastIndex]);
          gcImage.fillRectangle(x1 - iWidth + 1, y0, iWidth, y1 - y0 + 1);
          bImageChanged = true;
        }
  
        int limit = (drawWidth * nbComplete) / nbPieces;
        if (limit < drawWidth) {
          gcImage.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
          gcImage.fillRectangle(limit+x0, borderHorizontalSize,
                                x1-limit, completionHeight);
        }
        gcImage.setBackground(Colors.colorProgressBar);
        gcImage.fillRectangle(x0, borderHorizontalSize,
                              limit, completionHeight);
      } catch (Exception e) {
        System.out.println("Error Drawing PiecesItem");
        e.printStackTrace();
      } }
      gcImage.dispose();
  
      Image oldImage = ((TableCellCore)cell).getGraphicSWT();
      if (bImageChanged || image != oldImage || !cell.isValid()) {
        ((TableCellCore)cell).setGraphic(image);
        infoObj.setData("PiecesImage", image);
        infoObj.setData("PiecesImageBuffer", imageBuffer);
      }
    }
  }
}
