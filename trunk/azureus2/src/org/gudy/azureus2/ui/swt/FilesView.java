/*
 * Created on 17 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.DiskManager;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.FileInfo;

/**
 * @author Olivier
 * 
 */
public class FilesView implements IView {

  DownloadManager manager;
  Table table;
  HashMap items;
  HashMap itemsToFile;

  public Color blues[];

  public FilesView(DownloadManager manager) {
    this.manager = manager;
    items = new HashMap();
    itemsToFile = new HashMap();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    blues = new Color[5];
    Display display = composite.getDisplay();
    
    blues[4] = new Color(display, new RGB(0, 128, 255));
    blues[3] = new Color(display, new RGB(64, 160, 255));
    blues[2] = new Color(display, new RGB(128, 192, 255));
    blues[1] = new Color(display, new RGB(192, 224, 255));
    blues[0] = new Color(display, new RGB(255, 255, 255));
    
    table = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] titles = { "Name", "Size", "Done", "%", "First piece #", "# of pieces" ,"Pieces", "Mode", "Priority" };
    int[] align = { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.LEFT, SWT.LEFT };
    for (int i = 0; i < titles.length; i++) {
      TableColumn column = new TableColumn(table, align[i]);
      column.setText(titles[i]);
    }
    table.getColumn(0).setWidth(300);
    table.getColumn(1).setWidth(70);
    table.getColumn(2).setWidth(70);
    table.getColumn(3).setWidth(60);
    table.getColumn(4).setWidth(75);
    table.getColumn(5).setWidth(75);
    table.getColumn(6).setWidth(150);
    table.getColumn(7).setWidth(60);
    table.getColumn(8).setWidth(70);
    
    table.getColumn(0).addListener(SWT.Selection, new StringColumnListener("name"));
    table.getColumn(1).addListener(SWT.Selection, new IntColumnListener("size"));
    table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("done"));
    table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("percent"));
    table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("fp"));
    table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("nbp"));
    table.getColumn(6).addListener(SWT.Selection, new IntColumnListener("percent"));
    table.getColumn(7).addListener(SWT.Selection, new IntColumnListener("mode"));
    
    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
    final MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Open");

        menu.addListener(SWT.Show, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              item.setEnabled(false);
              return;
            }
            item.setEnabled(false);
            TableItem ti = tis[0];
            FileInfo fileInfo= (FileInfo) itemsToFile.get(ti);
            if(fileInfo.getAccessmode() == FileInfo.READ)
              item.setEnabled(true);
            
          }
        });

        item.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            TableItem ti = tis[0];
            FileInfo fileInfo= (FileInfo) itemsToFile.get(ti);
            if (fileInfo != null && fileInfo.getAccessmode() == FileInfo.READ)
              Program.launch(fileInfo.getPath() + fileInfo.getName());
          }
        });
        table.setMenu(menu);
        
    table.addMouseListener(new MouseAdapter() {
          /* (non-Javadoc)
           * @see org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
           */
          public void mouseDoubleClick(MouseEvent mEvent) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            TableItem ti = tis[0];
            FileInfo fileInfo= (FileInfo) itemsToFile.get(ti);
            if (fileInfo != null && fileInfo.getAccessmode() == FileInfo.READ)
              Program.launch(fileInfo.getPath() + fileInfo.getName());
          }
        });

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return table;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    DiskManager diskManager = manager.diskManager;
    if (diskManager == null)
      return;
    FileInfo files[] = diskManager.getFiles();
    if (files == null)
      return;
    for (int i = 0; i < files.length; i++) {
      if (files[i] != null) {
        FileItem fileItem = (FileItem) items.get(files[i]);
        if (fileItem == null) {
          fileItem = new FileItem(table,manager, files[i], blues);
          items.put(files[i], fileItem);
          itemsToFile.put(fileItem.getItem(),files[i]);
        }
        fileItem.refresh();
      }
    }

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    Iterator iter = items.values().iterator();
    while(iter.hasNext()) {
      FileItem fileItem = (FileItem) iter.next();
      fileItem.delete();
    }
    if(blues != null) {
      for(int i = 0 ; i < blues.length ; i++) {
        if(blues[i] != null && ! blues[i].isDisposed())
          blues[i].dispose();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return "Files";
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return "Files";
  }
  
  
  //Sorting
  
  private String getStringField(FileInfo fileInfo, String field) {
    if (field.equals("name"))
      return fileInfo.getName();

    return "";
  }

  private long getIntField(FileInfo fileInfo, String field) {

    if (field.equals("size"))
      return fileInfo.getLength();

    if (field.equals("done"))
      return fileInfo.getDownloaded();
 
    if (field.equals("percent"))
    {
      long percent = 0;
      if(fileInfo.getLength() > 0) {
        percent = (1000 * fileInfo.getDownloaded()) / fileInfo.getLength();
      }
      return percent;
    }

    if (field.equals("fp"))
      return fileInfo.getFirstPieceNumber();

    if (field.equals("nbp"))
      return fileInfo.getNbPieces();

    if (field.equals("mode"))
      return fileInfo.getAccessmode();   
      
    return 0;
  }
  
  private boolean ascending = false;
    private String lastField = "";

    private void orderInt(String field) {
      if (lastField.equals(field))
        ascending = !ascending;
      else {
        lastField = field;
        ascending = true;
      }
      synchronized (items) {
        List ordered = new ArrayList(items.size());
        FileItem fileItems[] = new FileItem[items.size()];
        Iterator iter = items.keySet().iterator();
        while (iter.hasNext()) {
          FileInfo fileInfo = (FileInfo) iter.next();
          FileItem item = (FileItem) items.get(fileInfo);
          fileItems[item.getIndex()] = item;
          long value = getIntField(fileInfo, field);
          int i;
          for (i = 0; i < ordered.size(); i++) {
            FileInfo fileInfoi = (FileInfo) ordered.get(i);
            long valuei = getIntField(fileInfoi, field);
            if (ascending) {
              if (valuei >= value)
                break;
            }
            else {
              if (valuei <= value)
                break;
            }
          }
          ordered.add(i, fileInfo);
        }

        for (int i = 0; i < ordered.size(); i++) {
          FileInfo fileInfo = (FileInfo) ordered.get(i);
          fileItems[i].setFileInfo(fileInfo);
          fileItems[i].invalidate();
          items.put(fileInfo, fileItems[i]);          
          itemsToFile.put(fileItems[i].getItem(),fileInfo);   
        }
      }
    }

    private class IntColumnListener implements Listener {

      private String field;

      public IntColumnListener(String field) {
        this.field = field;
      }

      public void handleEvent(Event e) {
        orderInt(field);
      }
    }

    private class StringColumnListener implements Listener {

      private String field;

      public StringColumnListener(String field) {
        this.field = field;
      }

      public void handleEvent(Event e) {
        orderString(field);
      }
    }

    private void orderString(String field) {
      if (lastField.equals(field))
        ascending = !ascending;
      else {
        lastField = field;
        ascending = true;
      }
      synchronized (items) {
        Collator collator = Collator.getInstance(Locale.getDefault());
        List ordered = new ArrayList(items.size());
        FileItem fileItems[] = new FileItem[items.size()];
        Iterator iter = items.keySet().iterator();
        while (iter.hasNext()) {
          FileInfo fileInfo = (FileInfo) iter.next();
          FileItem item = (FileItem) items.get(fileInfo);
          fileItems[item.getIndex()] = item;
          String value = getStringField(fileInfo, field);
          int i;
          for (i = 0; i < ordered.size(); i++) {
            FileInfo fileInfoi = (FileInfo) ordered.get(i);
            String valuei = getStringField(fileInfoi, field);
            if (ascending) {
              if (collator.compare(valuei, value) <= 0)
                break;
            }
            else {
              if (collator.compare(valuei, value) >= 0)
                break;
            }
          }
          ordered.add(i, fileInfo);
        }

        for (int i = 0; i < ordered.size(); i++) {
          FileInfo fileInfo = (FileInfo) ordered.get(i);
          fileItems[i].setFileInfo(fileInfo);
          fileItems[i].invalidate();
          items.put(fileInfo, fileItems[i]);  
          itemsToFile.put(fileItems[i].getItem(),fileInfo);   
        }
      }
    }

}
