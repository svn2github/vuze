/*
 * Created on 17 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;

import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.tableitems.files.*;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/23: extends TableView instead of IAbstractView
 */
public class FilesView 
       extends TableView
{
  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new SizeItem(),
    new DoneItem(),
    new PercentItem(),
    new FirstPieceItem(),
    new PieceCountItem(),
    new ProgressGraphItem(),
    new ModeItem(),
    new PriorityItem()
  };
  private DownloadManager manager;

  public FilesView(DownloadManager manager) {
    super(TableManager.TABLE_TORRENT_FILES, "FilesView", 
          basicItems, "firstpiece", SWT.MULTI | SWT.FULL_SELECTION);
    this.manager = manager;
  }

  public void initialize(Composite composite) {
    super.initialize(composite);

    
    getTable().addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent mEvent) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)getFirstSelectedDataSource();
        if (fileInfo != null && fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
          Program.launch(fileInfo.getPath() + fileInfo.getName());
      }
    });

    if(COConfigurationManager.getBooleanParameter("Always Show Torrent Files", true))
      manager.initializeDiskManager();
  }

  public void fillMenu(final Menu menu) {
    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open"); //$NON-NLS-1$
    itemOpen.setImage(ImageRepository.getImage("run"));
    
    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority"); //$NON-NLS-1$
    
    final Menu menuPriority = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); //$NON-NLS-1$
    
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemLow, "FilesView.menu.setpriority.normal"); //$NON-NLS-1$
    
    final MenuItem itemSkipped = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemSkipped, "FilesView.menu.setpriority.skipped"); //$NON-NLS-1$

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        Object[] infos = getSelectedDataSources();
        if (infos.length == 0) {
          itemOpen.setEnabled(false);
          itemPriority.setEnabled(false);
          return;
        }
        itemOpen.setEnabled(false);
        itemPriority.setEnabled(true);
        boolean open = true;
        for (int i = 0; i < infos.length; i++) {
          DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)infos[i];
          if (fileInfo.getAccessMode() != DiskManagerFileInfo.READ)
            open = false;
        }
        itemOpen.setEnabled(open);
      }
    });       

    itemOpen.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        if (fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
          Program.launch(fileInfo.getPath() + fileInfo.getName());
      }
    });
    
    itemHigh.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        fileInfo.setPriority(true);
        fileInfo.setSkipped(false);
      }
    });
        
    itemLow.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        fileInfo.setPriority(false);
        fileInfo.setSkipped(false);
      }
    });
        
    itemSkipped.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        fileInfo.setSkipped(true);
      }
    });

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    removeInvalidFileItems();

    DiskManager diskManager = manager.getDiskManager();
    if (diskManager == null)
      return;      
    DiskManagerFileInfo files[] = diskManager.getFiles();
    if (files != null && getTable().getItemCount() != files.length) {
      for (int i = 0; i < files.length; i++) {
        if (files[i] != null) {
          addDataSource(files[i]);
        }
      }
    }
    
    super.refresh();
  }
  
  private void removeInvalidFileItems() {
    DiskManager diskManager = manager.getDiskManager();
    final DiskManagerFileInfo files[] = (diskManager != null) ? diskManager.getFiles() : null;

    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        if (fileInfo != null && !containsFileInfo(files, fileInfo)) {
          removeDataSource(fileInfo);
        }
      }
    });
  }
  
  private boolean containsFileInfo(DiskManagerFileInfo[] files,
                                   DiskManagerFileInfo file) {
    //This method works with reference comparision
    if(files == null || file == null) {
      return false;
    }
    for(int i = 0 ; i < files.length ; i++) {
      if(files[i] == file)
        return true;
    }
    return false;
  }
}
