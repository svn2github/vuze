/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.PeerStats;
import org.gudy.azureus2.core.Piece;

/**
 * @author Olivier
 * 
 */
public class PieceTableItem {

  Display display;
  Table table;
  Piece piece;
  TableItem item;

  private String[] oldTexts;

  public PieceTableItem(Table table, Piece piece) {
    this.table = table;
    this.piece = piece;
    initialize();
  }

  private void initialize() {
    if (table.isDisposed())
      return;
    this.display = table.getDisplay();
    if (display.isDisposed())
      return;
    display.syncExec(new Runnable() {
      public void run() {
        if (table.isDisposed())
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
    if (display.isDisposed())
      return;
    if (item == null)
      return;
    if (item.isDisposed())
      return;

    String tmp;

    tmp = "" + piece.pieceNumber;
    if (oldTexts[0].equals("")) {
      item.setText(0, tmp);
      oldTexts[0] = tmp;
    }

    tmp = PeerStats.format(piece.length);
    if (oldTexts[1].equals("")) {
      item.setText(1, tmp);
      oldTexts[1] = tmp;
    }

    tmp = "" + piece.nbBlocs;
    if (oldTexts[2].equals("")) {
      item.setText(2, tmp);
      oldTexts[2] = tmp;
    }
    tmp = "" + piece.completed;
    if (!(oldTexts[4].equals(tmp))) {
      item.setText(4, tmp);
      oldTexts[4] = tmp;
    }

    tmp = "" + piece.manager.getAvailability(piece.pieceNumber);
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
    Color blue = new Color(display, new RGB(0, 128, 255));
    Color green = new Color(display, new RGB(192, 224, 255));
    Color white = new Color(display, new RGB(255, 255, 255));
    Color color;
    GC gc = new GC(table);
    GC gcImage = new GC(image);
    for (int i = 0; i < piece.nbBlocs; i++) {
      int a0 = (i * width) / piece.nbBlocs;
      int a1 = ((i + 1) * width) / piece.nbBlocs;
      color = white;
      if (piece.requested[i])
        color = green;
      if (piece.written[i]) {
        color = blue;
      }
      gcImage.setBackground(color);
      Rectangle rect = new Rectangle(a0, 1, a1, height);
      gcImage.fillRectangle(rect);
    }
    gcImage.dispose();
    blue.dispose();
    green.dispose();
    white.dispose();
    Color colorGrey = new Color(display, new RGB(170, 170, 170));
    gc.setForeground(colorGrey);
    gc.drawImage(image, x0, y0);
    gc.drawRectangle(new Rectangle(x0, y0, width, height));
    gc.dispose();
    colorGrey.dispose();
    image.dispose();
  }

  public void remove() {
    if (display.isDisposed())
      return;
    display.syncExec(new Runnable() {
      public void run() {
        if (table.isDisposed())
          return;
        table.remove(table.indexOf(item));
      }
    });
  }

}
