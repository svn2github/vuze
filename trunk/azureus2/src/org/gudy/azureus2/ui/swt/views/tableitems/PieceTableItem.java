/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.PiecesView;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;

/**
 * @author Olivier
 * 
 */
public class PieceTableItem implements SortableItem{

  Display display;
  PiecesView view;
  Table table;
  PEPiece piece;
  TableItem item;
  Canvas cBlockView = null;

  private String[] oldTexts;

  public PieceTableItem(PiecesView view,Table table, PEPiece piece) {
    this.view = view;
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
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        item = new TableItem(table, SWT.NULL);
        view.setItem(item,piece);

        cBlockView = new Canvas(table, SWT.NULL);
        cBlockView.addPaintListener(new PaintListener() {
        	public void paintControl(PaintEvent event) {
            if (event.count > 0 || event.width == 0 || event.height == 0)
              return;
        		updateBlockView();
        	}
        });
        
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

    tmp = DisplayFormatters.formatByteCountToKiBEtc(piece.getLength());
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
    
    updateBlockView();
  }
  
  void updateBlockView() {
    if (cBlockView == null || cBlockView.isDisposed())
      return;
    Rectangle bounds = item.getBounds(3);
    cBlockView.setBounds(bounds);

    int x1 = bounds.width - 2;
    int y1 = bounds.height - 3;
    if (x1 < 10 || y1 < 3)
      return;
    Image image = new Image(display, bounds.width, bounds.height);
    Color blue = MainWindow.blues[MainWindow.BLUES_DARKEST];
    Color green = MainWindow.blues[MainWindow.BLUES_MIDLIGHT];
    Color downloaded = MainWindow.red;
    Color color;
    GC gcImage = new GC(image);
    gcImage.setForeground(MainWindow.grey);
    gcImage.drawRectangle(0, 0, x1 + 1, y1 + 1);
    int iPixelsPerBlock = (x1 + 1) / piece.getNbBlocs();
    for (int i = 0; i < piece.getNbBlocs(); i++) {
      int nextWidth = iPixelsPerBlock;
      if (i == piece.getNbBlocs() - 1) {
        nextWidth = x1 - (iPixelsPerBlock * (piece.getNbBlocs() - 1));
      }
      color = MainWindow.white;

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

    GC gc = new GC(cBlockView);
    gc.drawImage(image, 0, 0);
    
    gc.dispose();
    image.dispose();
  }

  public void remove() {
    if (display == null || display.isDisposed())
      return;
    try {
    display.asyncExec(new Runnable() {
      public void run() {
        if (cBlockView != null) {
          if (!cBlockView.isDisposed())
            cBlockView.dispose();
          cBlockView = null;
        }

        if (table == null || table.isDisposed())
          return;
        table.remove(table.indexOf(item));
      }
    });
    }
    catch (Exception ignore) {
    }
  }
         
  /*
   * SortableItem implementation
   */
  
  public boolean setDataSource(Object dataSource) {
    if (piece != (PEPiece) dataSource) {
      piece = (PEPiece) dataSource;
      invalidate();
      return true;
    }
    return false;
  }

  public int getIndex() {
    if(table == null || table.isDisposed() || item == null || item.isDisposed())
      return -1;
    return table.indexOf(item);
  }

  public long getIntField(String field) {    
    if (field.equals("#")) //$NON-NLS-1$
      return this.piece.getPieceNumber();

    if (field.equals("size")) //$NON-NLS-1$
      return this.piece.getLength();

    if (field.equals("nbBlocs")) //$NON-NLS-1$
      return this.piece.getNbBlocs();

    if (field.equals("done")) //$NON-NLS-1$
      return this.piece.getCompleted();

    if (field.equals("availability")) //$NON-NLS-1$
      return this.piece.getAvailability();

    return 0;
  }

  public String getStringField(String field) {
    return "";
  }
  
  public TableItem getTableItem() {
    return item;
  }
  
  public void invalidate() {};

}
