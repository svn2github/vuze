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
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
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
import org.gudy.azureus2.core.MessageText;

/**
 * @author Olivier
 * 
 */
public class FilesView extends AbstractIView {

  DownloadManager manager;
  Table table;
  HashMap items;
  HashMap itemsToFile;

  public FilesView(DownloadManager manager) {
    this.manager = manager;
    items = new HashMap();
    itemsToFile = new HashMap();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {

    table = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] columnsHeader = { "name", "size", "done", "%", "firstpiece", "numberofpieces", "pieces", "mode", "priority" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
    int[] align = { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.LEFT, SWT.LEFT };
    for (int i = 0; i < columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, align[i]);
      Messages.setLanguageText(column, "FilesView." + columnsHeader[i]); //$NON-NLS-1$
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

    table.getColumn(0).addListener(SWT.Selection, new StringColumnListener("name")); //$NON-NLS-1$
    table.getColumn(1).addListener(SWT.Selection, new IntColumnListener("size")); //$NON-NLS-1$
    table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("done")); //$NON-NLS-1$
    table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("percent")); //$NON-NLS-1$
    table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("fp")); //$NON-NLS-1$
    table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("nbp")); //$NON-NLS-1$
    table.getColumn(6).addListener(SWT.Selection, new IntColumnListener("percent")); //$NON-NLS-1$
    table.getColumn(7).addListener(SWT.Selection, new IntColumnListener("mode")); //$NON-NLS-1$

    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open"); //$NON-NLS-1$
    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority"); //$NON-NLS-1$
    final Menu menuPriority = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); //$NON-NLS-1$
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemLow, "FilesView.menu.setpriority.normal"); //$NON-NLS-1$
    final MenuItem itemSkipped = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemSkipped, "FilesView.menu.setpriority.skipped"); //$NON-NLS-1$

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          itemOpen.setEnabled(false);
          return;
        }
        itemOpen.setEnabled(false);
        TableItem ti = tis[0];
        FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
        if (fileInfo.getAccessmode() == FileInfo.READ)
        itemOpen.setEnabled(true);

      }
    });       

    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
        if (fileInfo != null && fileInfo.getAccessmode() == FileInfo.READ)
          Program.launch(fileInfo.getPath() + fileInfo.getName());
      }
    });
    
    itemHigh.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            TableItem ti = tis[0];
            FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
            if (fileInfo != null)
              fileInfo.setPriority(true);
              fileInfo.setSkipped(false);
          }
        });
        
    itemLow.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            TableItem ti = tis[0];
            FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
            if (fileInfo != null)
              fileInfo.setPriority(false);
              fileInfo.setSkipped(false);
          }
        });
        
    itemSkipped.addListener(SWT.Selection, new Listener() {
              public void handleEvent(Event e) {
                TableItem[] tis = table.getSelection();
                if (tis.length == 0) {
                  return;
                }
                TableItem ti = tis[0];
                FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
                if (fileInfo != null)
                  fileInfo.setSkipped(true);
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
        FileInfo fileInfo = (FileInfo) itemsToFile.get(ti);
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
    if(getComposite() == null || getComposite().isDisposed())
      return;

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
          fileItem = new FileItem(table, manager, files[i], MainWindow.blues);
          items.put(files[i], fileItem);
          itemsToFile.put(fileItem.getItem(), files[i]);
        }
        fileItem.refresh();
      }
    }    

  }
  
  private void removeFileItems() {
    if(items == null)
      return;
    Iterator iter = items.values().iterator();
    while(iter.hasNext()) {        
      FileItem fileItem = (FileItem) iter.next();
      fileItem.delete();
    }    
    items.clear();    
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    Iterator iter = items.values().iterator();
    while (iter.hasNext()) {
      FileItem fileItem = (FileItem) iter.next();
      fileItem.delete();
    }
    if(table != null && ! table.isDisposed())
      table.dispose();
  }

  public String getData() {
    return "FilesView.title.short"; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("FilesView.title.full"); //$NON-NLS-1$
  }

  //Sorting

  private String getStringField(FileInfo fileInfo, String field) {
    if (field.equals("name")) //$NON-NLS-1$
      return fileInfo.getName();

    return ""; //$NON-NLS-1$
  }

  private long getIntField(FileInfo fileInfo, String field) {

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

    return 0;
  }

  private boolean ascending = false;
  private String lastField = ""; //$NON-NLS-1$

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
        itemsToFile.put(fileItems[i].getItem(), fileInfo);
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
        itemsToFile.put(fileItems[i].getItem(), fileInfo);
      }
    }
  }

}
