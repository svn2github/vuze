/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 * 
 */
public class PieceTableItem {

  Display display;
  Table table;
  PEPiece piece;
  TableItem item;

  private String[] oldTexts;

  public PieceTableItem(Table table, PEPiece piece) {
    this.table = table;
    this.piece = piece;
    initialize();
  }

  private void initialize() {
    if (table == null || table.isDisposed())
      return;
    this.display = table.getDisplay();
    if (display == null || display.isDisposed())
      return;
    display.syncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        item = new TableItem(table, SWT.NULL);
      }
    });

    oldTexts = new String[6];
    for (int i = 0; i < oldTexts.length; i++) {
      oldTexts[i] = "";
    }
  }

  public void updateDisplay() {
    if (display == null || display.isDisposed())
      return;
    if (item == null || item.isDisposed())
      return;

    String tmp;

    tmp = "" + piece.getPieceNumber();
    if (!oldTexts[0].equals(tmp)) {
      item.setText(0, tmp);
      oldTexts[0] = tmp;
    }

    tmp = DisplayFormatters.formatByteCountToKBEtc(piece.getLength());
    if (!oldTexts[1].equals(tmp)) {
      item.setText(1, tmp);
      oldTexts[1] = tmp;
    }

    tmp = "" + piece.getNbBlocs();
    if (!oldTexts[2].equals(tmp)) {
      item.setText(2, tmp);
      oldTexts[2] = tmp;
    }
    tmp = "" + piece.getCompleted();
    if (!(oldTexts[4].equals(tmp))) {
      item.setText(4, tmp);
      oldTexts[4] = tmp;
    }

    tmp = "" + piece.getAvailability();
    if (!(oldTexts[5].equals(tmp))) {
      item.setText(5, tmp);
      oldTexts[5] = tmp;
    }

    Rectangle bounds = item.getBounds(3);
    int width = bounds.width - 1;
    int x0 = bounds.x;
    int y0 = bounds.y + 1;
    int height = bounds.height - 3;
    if (width < 10 || height < 3)
      return;
    Image image = new Image(display, width, height);
    Color blue = MainWindow.blues[4];
    Color green = MainWindow.blues[1];
    Color color;
    GC gc = new GC(table);
    GC gcImage = new GC(image);
    for (int i = 0; i < piece.getNbBlocs(); i++) {
      int a0 = (i * width) / piece.getNbBlocs();
      int a1 = ((i + 1) * width) / piece.getNbBlocs();
      color = MainWindow.white;
      if (piece.getRequested()[i])
        color = green;
      if (piece.getWritten()[i]) {
        color = blue;
      }
      gcImage.setBackground(color);
      gcImage.fillRectangle(a0,1,a1,height);
    }
    gcImage.dispose();
    gc.setForeground(MainWindow.grey);
    gc.drawImage(image, x0, y0);
    gc.drawRectangle(x0, y0, width, height);
    gc.dispose();
    image.dispose();
  }

  public void remove() {
    if (display == null || display.isDisposed())
      return;
    try {
    display.syncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        table.remove(table.indexOf(item));
      }
    });
    }
    catch (Exception ignore) {
    }
  }

  /**
   * @param piece
   */
  public void setPiece(PEPiece piece) {
    this.piece = piece;
  }
  
  public int getIndex() {
    if(table != null && !table.isDisposed())
      return table.indexOf(item);
    return 0;
  }

}
