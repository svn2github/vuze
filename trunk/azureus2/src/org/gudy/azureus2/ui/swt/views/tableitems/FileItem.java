/*
 * Created on 17 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;

/**
 * @author Olivier
 * 
 */
public class FileItem implements SortableItem{

  private Display display;
  private Table table;
  private TableItem item;
  private Color blues[];

  private Image piecesImage;
  private boolean valid;
  private String oldTexts[];

  private DownloadManager manager;
  private DiskManagerFileInfo fileInfo;

  private int loopFactor;

  public FileItem(Table table, DownloadManager manager, DiskManagerFileInfo fileInfo, Color blues[]) {
    this.display = table.getDisplay();
    this.blues = blues;
    this.table = table;
    this.manager = manager;
    this.fileInfo = fileInfo;
    initialize();
  }

  private void initialize() {
    loopFactor = 0;
    valid = false;
    oldTexts = new String[table.getColumnCount()];
    for (int i = 0; i < oldTexts.length; i++)
      oldTexts[i] = ""; //$NON-NLS-1$
    item = new TableItem(table, SWT.NULL);
  }

  public void refresh() {
    if (item == null || item.isDisposed())
      return;

    String tmp;

    tmp = fileInfo.getName();
    if (!oldTexts[0].equals(tmp)) {
      oldTexts[0] = tmp;
      item.setText(0, tmp);
      Program program = Program.findProgram(fileInfo.getExtension());
      Image icon = ImageRepository.getIconFromProgram(program);
      item.setImage(0, icon);
    }

    tmp = DisplayFormatters.formatByteCountToKiBEtc(fileInfo.getLength());
    if (!oldTexts[1].equals(tmp)) {
      oldTexts[1] = tmp;
      item.setText(1, tmp);
    }

    tmp = DisplayFormatters.formatByteCountToKiBEtc(fileInfo.getDownloaded());
    if (!oldTexts[2].equals(tmp)) {
      oldTexts[2] = tmp;
      item.setText(2, tmp);
    }

    int percent = 1000;
    if (fileInfo.getLength() != 0)
      percent = (int) ((fileInfo.getDownloaded() * 1000) / fileInfo.getLength());
    tmp = (percent / 10) + "." + (percent % 10) + " %"; //$NON-NLS-1$ //$NON-NLS-2$
    if (!oldTexts[3].equals(tmp)) {
      oldTexts[3] = tmp;
      item.setText(3, tmp);
    }

    tmp = "" + fileInfo.getFirstPieceNumber(); //$NON-NLS-1$
    if (!oldTexts[4].equals(tmp)) {
      oldTexts[4] = tmp;
      item.setText(4, tmp);
    }

    tmp = "" + fileInfo.getNbPieces(); //$NON-NLS-1$
    if (!oldTexts[5].equals(tmp)) {
      oldTexts[5] = tmp;
      item.setText(5, tmp);
    }

    tmp = MessageText.getString("FileItem.read"); //$NON-NLS-1$
    if (fileInfo.getAccessmode() == DiskManagerFileInfo.WRITE)
      tmp = MessageText.getString("FileItem.write"); //$NON-NLS-1$
    if (!oldTexts[7].equals(tmp)) {
      oldTexts[7] = tmp;
      item.setText(7, tmp);
      if(tmp.equals(MessageText.getString("FileItem.read"))) { //$NON-NLS-1$
        item.setForeground(blues[4]);
      } else {
        item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
      }
    }
    
    tmp = MessageText.getString("FileItem.normal"); //$NON-NLS-1$
    if (fileInfo.isPriority())
      tmp = MessageText.getString("FileItem.high"); //$NON-NLS-1$
    if(fileInfo.isSkipped())
      tmp = MessageText.getString("FileItem.donotdownload"); //$NON-NLS-1$
    if (!oldTexts[8].equals(tmp)) {
          oldTexts[8] = tmp;
          item.setText(8, tmp);
    }

    Rectangle bounds = item.getBounds(6);
    int width = bounds.width - 1;
    int x0 = bounds.x;
    int y0 = bounds.y + 1;
    int height = bounds.height - 3;
    if (width < 10 || height < 3)
      return;
    //Get the table GC
    GC gc = new GC(table);
    gc.setClipping(table.getClientArea());

    int firstPiece = fileInfo.getFirstPieceNumber();
    int nbPieces = fileInfo.getNbPieces();

    //Only recompute the image once every 10 iterations.
    if ((loopFactor % 10) == 0 || !valid) {
      valid = true;
      if (piecesImage != null && !piecesImage.isDisposed())
        piecesImage.dispose();
      piecesImage = new Image(display, width, height);

      //System.out.println(table.getHeaderHeight());
      boolean available[] = manager.getPiecesStatus();
      GC gcImage = new GC(piecesImage);
      for (int i = 0; i < width; i++) {
        int a0 = (i * nbPieces) / width;
        int a1 = ((i + 1) * nbPieces) / width;
        if (a1 == a0)
          a1++;
        if (a1 > nbPieces && nbPieces != 0)
          a1 = nbPieces;
        int nbAvailable = 0;
        if(firstPiece >= 0) {               
          for (int j = a0; j < a1; j++)
           if (available[j+firstPiece])
              nbAvailable++;        
        } else {
           nbAvailable = 1;    
        }
        int index = (nbAvailable * 4) / (a1 - a0);
        //System.out.print(index);
        gcImage.setBackground(blues[index]);
        gcImage.fillRectangle(i,1,1,height);
      }
      gcImage.dispose();
    }
    gc.setForeground(blues[4]);
    gc.drawImage(piecesImage, x0, y0);
    gc.drawRectangle(x0, y0, width, height);
    gc.dispose();

    loopFactor++;
  }

  public void delete() {
    if (piecesImage != null && !piecesImage.isDisposed())
      piecesImage.dispose();

    if (table == null || table.isDisposed())
      return;

    if (item != null)
      table.remove(table.indexOf(item));
  }
  /**
   * @return
   */
  public TableItem getItem() {
    return item;
  }

  /**
   * @param item
   */
  public void setItem(TableItem item) {
    this.item = item;
  }
  
  public int getIndex() {    
    return table.indexOf(item);
  }

  /**
   * @return
   */
  public DiskManagerFileInfo getFileInfo() {
    return fileInfo;
  }
  
  public void invalidate() {
    valid = false;
  }
  
  /*
   * SortableItem implementation
   */
  

  public String getStringField(String field) {
    if (field.equals("name")) //$NON-NLS-1$
      return fileInfo.getName();
  
    return ""; //$NON-NLS-1$
  }

  public long getIntField(String field) {
  
    if (field.equals("size")) //$NON-NLS-1$
      return fileInfo.getLength();
  
    if (field.equals("done")) //$NON-NLS-1$
      return fileInfo.getDownloaded();
  
    if (field.equals("percent")) { //$NON-NLS-1$
      long percent = 0;
      if (fileInfo.getLength() != 0) {
        percent = (1000 * fileInfo.getDownloaded()) / fileInfo.getLength();
      }
      return percent;
    }
  
    if (field.equals("fp")) //$NON-NLS-1$
      return fileInfo.getFirstPieceNumber();
  
    if (field.equals("nbp")) //$NON-NLS-1$
      return fileInfo.getNbPieces();
  
    if (field.equals("mode")) //$NON-NLS-1$
      return fileInfo.getAccessmode();
  
    if(field.equals("priority")) {
      int prio = fileInfo.isPriority()?1:0 + 2 * (fileInfo.isSkipped()?1:0);
      return prio;
    }
    return 0;
  }
  
  public TableItem getTableItem() {
   return item;
  }

  public void setDataSource(Object dataSource) {
    fileInfo = (DiskManagerFileInfo) dataSource;
  }

}
