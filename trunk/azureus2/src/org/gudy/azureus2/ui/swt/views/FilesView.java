/*
 * Created on 17 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.tableitems.FileItem;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

/**
 * @author Olivier
 * 
 */
public class FilesView extends AbstractIView implements SortableTable {

  DownloadManager manager;
  Table table;
  HashMap objectToSortableItem;
  HashMap tableItemToObject;
  TableSorter sorter;

  public FilesView(DownloadManager manager) {
    this.manager = manager;
    objectToSortableItem = new HashMap();
    tableItemToObject = new HashMap();
  }

  public void initialize(Composite composite) {

    table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
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

    sorter = new TableSorter(this,"fp",true);
    sorter.addStringColumnListener(table.getColumn(0),"name");
    sorter.addIntColumnListener(table.getColumn(1),"size");
    sorter.addIntColumnListener(table.getColumn(2),"done");
    sorter.addIntColumnListener(table.getColumn(3),"percent");
    sorter.addIntColumnListener(table.getColumn(4),"fp");
    sorter.addIntColumnListener(table.getColumn(5),"nbp");
    sorter.addIntColumnListener(table.getColumn(6),"percent");
    sorter.addIntColumnListener(table.getColumn(7),"mode");
    sorter.addIntColumnListener(table.getColumn(8),"priority");

    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open"); //$NON-NLS-1$
    itemOpen.setImage(ImageRepository.getImage("run"));
    
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
          itemPriority.setEnabled(false);
          return;
        }
        itemOpen.setEnabled(false);
        itemPriority.setEnabled(true);
        boolean open = true;
        for(int i = 0 ; i < tis.length ; i++) {
          TableItem ti = tis[0];
          DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
          if (fileInfo.getAccessmode() != DiskManagerFileInfo.READ)
            open = false;
        }
        itemOpen.setEnabled(open);
      }
    });       

    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        for(int i = 0 ; i < tis.length ; i++) {
          TableItem ti = tis[i];
          DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
          if (fileInfo != null && fileInfo.getAccessmode() == DiskManagerFileInfo.READ)
            Program.launch(fileInfo.getPath() + fileInfo.getName());
        }
      }
    });
    
    itemHigh.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            for(int i = 0 ; i < tis.length ; i++) {
              TableItem ti = tis[i];
              DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
              if (fileInfo != null) {
                fileInfo.setPriority(true);
                fileInfo.setSkipped(false);
              }
            }
          }
        });
        
    itemLow.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            TableItem[] tis = table.getSelection();
            if (tis.length == 0) {
              return;
            }
            for(int i = 0 ; i < tis.length ; i++) {
              TableItem ti = tis[i];
              DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
              if (fileInfo != null) {
                fileInfo.setPriority(false);
                fileInfo.setSkipped(false);
              }
            }
          }
        });
        
    itemSkipped.addListener(SWT.Selection, new Listener() {
              public void handleEvent(Event e) {
                TableItem[] tis = table.getSelection();
                if (tis.length == 0) {
                  return;
                }
                for(int i = 0 ; i < tis.length ; i++) {
                  TableItem ti = tis[i];
                  DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
                  if (fileInfo != null)
                    fileInfo.setSkipped(true);
                }
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
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(ti);
        if (fileInfo != null && fileInfo.getAccessmode() == DiskManagerFileInfo.READ)
          Program.launch(fileInfo.getPath() + fileInfo.getName());
      }
    });

    if(COConfigurationManager.getBooleanParameter("Always Show Torrent Files", true))
      manager.initializeDiskManager();
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

    DiskManager diskManager = manager.getDiskManager();
    if (diskManager == null) {      
      return;      
    }
    removeInvalidFileItems();
    DiskManagerFileInfo files[] = diskManager.getFiles();
    if (files == null)
      return;
    for (int i = 0; i < files.length; i++) {
      if (files[i] != null) {
        FileItem fileItem = (FileItem) objectToSortableItem.get(files[i]);
        if (fileItem == null) {
          fileItem = new FileItem(table, manager, files[i], MainWindow.blues);
          objectToSortableItem.put(files[i], fileItem);
          tableItemToObject.put(fileItem.getItem(), files[i]);
        }
        fileItem.refresh();
      }
    }
  }
  
  private void removeInvalidFileItems() {
    DiskManager diskManager = manager.getDiskManager();
    if(objectToSortableItem == null || diskManager == null)
      return;
    DiskManagerFileInfo files[] = diskManager.getFiles();
    Iterator iter = objectToSortableItem.values().iterator();
    while(iter.hasNext()) {        
      FileItem fileItem = (FileItem) iter.next();
      DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tableItemToObject.get(fileItem.getItem());
      if(! containsFileInfo(files,fileInfo)) {        
        tableItemToObject.remove(fileItem.getItem());
        fileItem.delete();
        iter.remove();
      }
    }    
  }
  
  private boolean containsFileInfo(DiskManagerFileInfo[] files,DiskManagerFileInfo file) {
    //This method works with reference comparision
    if(files == null || file == null) {
      return true;
    }
    for(int i = 0 ; i < files.length ; i++) {
      if(files[i] == file)
        return true;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    Iterator iter = objectToSortableItem.values().iterator();
    while (iter.hasNext()) {
      FileItem fileItem = (FileItem) iter.next();
      fileItem.delete();
    }
    if(table != null && ! table.isDisposed())
      table.dispose();
    ConfigurationManager.getInstance().removeParameterListener("ReOrder Delay", sorter);
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

  public Map getObjectToSortableItemMap() {
    return objectToSortableItem;
  }

  public Table getTable() {
    return table;
  }

  public Map getTableItemToObjectMap() {
    return tableItemToObject;
  }

}
