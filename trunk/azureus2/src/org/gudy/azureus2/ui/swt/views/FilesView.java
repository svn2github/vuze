/*
 * Created on 17 juil. 2003
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;


import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.MessageBoxWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.file.FileInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.tableitems.files.*;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/23: extends TableView instead of IAbstractView
 */
public class FilesView 
       extends TableView
{
	boolean refreshing = false;

  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new PathItem(),
    new SizeItem(),
    new DoneItem(),
    new PercentItem(),
    new FirstPieceItem(),
    new PieceCountItem(),
    new RemainingPiecesItem(),
    new ProgressGraphItem(),
    new ModeItem(),
    new PriorityItem(),
    new StorageTypeItem(),
    new FileExtensionItem(), 
  };
  
  private DownloadManager manager = null;
  
  public static boolean show_full_path = COConfigurationManager.getBooleanParameter( "FilesView.show.full.path", false );
  private MenuItem path_item;
  

  /**
   * Initialize 
   */
  public FilesView() {
    super(TableManager.TABLE_TORRENT_FILES, "FilesView", basicItems,
				"firstpiece", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
    setRowDefaultIconSize(new Point(16, 16));
    bEnableTabViews = true;
    coreTabViews = new IView[] { new FileInfoView() };
  }

	public void dataSourceChanged(Object newDataSource) {
		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;

		removeAllTableRows();
	}
	
	public void runDefaultAction() {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) getFirstSelectedDataSource();
		if (fileInfo != null
				&& fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
			Utils.launch(fileInfo.getFile(true).toString());
	}

	public void fillMenu(final Menu menu) {
		Object[] infos = getSelectedDataSources();
		boolean hasSelection = (infos.length > 0);

    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open");
    Utils.setMenuItemImage(itemOpen, "run");
    // Invoke open on enter, double click
    menu.setDefaultItem(itemOpen);

		// Explore  (Copied from MyTorrentsView)
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu.explore");
		itemExplore.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
		    Object[] dataSources = getSelectedDataSources();
		    for (int i = dataSources.length - 1; i >= 0; i--) {
		    	DiskManagerFileInfo info = (DiskManagerFileInfo)dataSources[i];
		      if (info != null)
		        ManagerUtils.open(info.getFile(true));
		    }
			}
		});
		itemExplore.setEnabled(hasSelection);

	MenuItem itemRenameOrRetarget = null, itemRename = null, itemRetarget = null;
	if (!COConfigurationManager.getBooleanParameter("FilesView.separate_rename_and_retarget")) {
	    itemRenameOrRetarget = new MenuItem(menu, SWT.PUSH);
	    Messages.setLanguageText(itemRenameOrRetarget, "FilesView.menu.rename");
		itemRenameOrRetarget.setData("rename", Boolean.valueOf(true));
		itemRenameOrRetarget.setData("retarget", Boolean.valueOf(true));
	}
	else {
		itemRename = new MenuItem(menu, SWT.PUSH);
		itemRetarget = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRename, "FilesView.menu.rename_only");
		Messages.setLanguageText(itemRetarget, "FilesView.menu.retarget");
		itemRename.setData("rename", Boolean.valueOf(true));
		itemRename.setData("retarget", Boolean.valueOf(false));
		itemRetarget.setData("rename", Boolean.valueOf(false));
		itemRetarget.setData("retarget", Boolean.valueOf(true));

	}
		
    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority"); //$NON-NLS-1$
    
    final Menu menuPriority = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
    itemHigh.setData("Priority", new Integer(0));
    Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); //$NON-NLS-1$
    
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
    itemLow.setData("Priority", new Integer(1));
    Messages.setLanguageText(itemLow, "FilesView.menu.setpriority.normal"); //$NON-NLS-1$
    
    final MenuItem itemSkipped = new MenuItem(menuPriority, SWT.CASCADE);
    itemSkipped.setData("Priority", new Integer(2));
    Messages.setLanguageText(itemSkipped, "FilesView.menu.setpriority.skipped"); //$NON-NLS-1$

    final MenuItem itemDelete = new MenuItem(menuPriority, SWT.CASCADE);
    itemDelete.setData("Priority", new Integer(3));
    Messages.setLanguageText(itemDelete, "wizard.multitracker.delete");	// lazy but we're near release

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);

		if (!hasSelection) {
			itemOpen.setEnabled(false);
			itemPriority.setEnabled(false);
			if (itemRenameOrRetarget != null) {
				itemRenameOrRetarget.setEnabled(false);
			}
			else {
				itemRename.setEnabled(false);
				itemRetarget.setEnabled(false);
			}
			return;
		}

		boolean open 				= true;
		boolean all_compact 		= true;
		boolean	all_skipped			= true;
		boolean	all_priority		= true;
		boolean	all_not_priority	= true;
		
		for (int i = 0; i < infos.length; i++) {

			DiskManagerFileInfo file_info = (DiskManagerFileInfo) infos[i];

			if (file_info.getAccessMode() != DiskManagerFileInfo.READ) {

				open = false;
			}

			if (file_info.getStorageType() != DiskManagerFileInfo.ST_COMPACT) {

				all_compact = false;
			}
			
			if ( file_info.isSkipped()){
				
				all_priority		= false;
				all_not_priority	= false;
				
			}else{
				all_skipped = false;
			
				if ( !file_info.isPriority()){
					
					all_priority = false;
				}else{
					
					all_not_priority = false;
				}
			}
		}

		// we can only open files if they are read-only

		itemOpen.setEnabled(open);

		// can't rename files for non-persistent downloads (e.g. shares) as these
		// are managed "externally"

		if (itemRenameOrRetarget != null) {
			itemRenameOrRetarget.setEnabled(manager.isPersistent());
		}
		else {
			itemRename.setEnabled(manager.isPersistent());
			itemRetarget.setEnabled(manager.isPersistent());
		}

		itemSkipped.setEnabled( !all_skipped );
	
		itemHigh.setEnabled( !all_priority );
		
		itemLow.setEnabled( !all_not_priority );
		
		itemDelete.setEnabled( !all_compact );

    itemOpen.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        if (fileInfo.getAccessMode() == DiskManagerFileInfo.READ) {
        	Utils.launch(fileInfo.getFile(true).toString());
        }
      }
    });
    
    Listener rename_listener = new Listener() {
    	public void handleEvent(Event event) {
    		boolean rename_it = ((Boolean)event.widget.getData("rename")).booleanValue();
    		boolean retarget_it = ((Boolean)event.widget.getData("retarget")).booleanValue();
    		rename(getSelectedRows(), rename_it, retarget_it);
    	}
    };
    
    if (itemRenameOrRetarget != null) {
    	itemRenameOrRetarget.addListener(SWT.Selection, rename_listener);
    }
    else {
    	itemRename.addListener(SWT.Selection, rename_listener);
    	itemRetarget.addListener(SWT.Selection, rename_listener);
    }
    
    Listener priorityListener = new Listener() {
			public void handleEvent(Event event) {
				changePriority(((Integer) event.widget.getData("Priority")).intValue(),
						getSelectedRows());
			}
    };

    itemHigh.addListener(SWT.Selection, priorityListener); 
    itemLow.addListener(SWT.Selection, priorityListener);
    itemSkipped.addListener(SWT.Selection, priorityListener); 
    itemDelete.addListener(SWT.Selection, priorityListener);
	}

	private String askForRenameFilename(DiskManagerFileInfo fileInfo) {
		SimpleTextEntryWindow dialog = new SimpleTextEntryWindow(getComposite().getDisplay());
		dialog.setTitle("FilesView.rename.filename.title");
		dialog.setMessage("FilesView.rename.filename.text");
		dialog.setPreenteredText(fileInfo.getFile(true).getName(), false); // false -> it's not "suggested", it's a previous value
		dialog.allowEmptyInput(false);
		dialog.prompt();
		if (!dialog.hasSubmittedInput()) {return null;}
		return dialog.getSubmittedInput();
	}
	
	private String askForRetargetedFilename(DiskManagerFileInfo fileInfo) {
		FileDialog fDialog = new FileDialog(getComposite().getShell(), SWT.SYSTEM_MODAL | SWT.SAVE);  
		File existing_file = fileInfo.getFile(true);
		fDialog.setFilterPath(existing_file.getParent());
		fDialog.setFileName(existing_file.getName());
		fDialog.setText(MessageText.getString("FilesView.rename.choose.path"));
		return fDialog.open();
	}
	
	private String askForSaveDirectory(DiskManagerFileInfo fileInfo) {
		DirectoryDialog dDialog = new DirectoryDialog(getComposite().getShell(), SWT.SYSTEM_MODAL | SWT.SAVE);
		File current_dir = fileInfo.getFile(true).getParentFile();
		dDialog.setFilterPath(current_dir.getPath());
		dDialog.setText(MessageText.getString("FilesView.rename.choose.path.dir"));
		return dDialog.open();
	}
	
	private boolean askCanOverwrite(File file) {
		return MessageBoxWindow.open( 
				"FilesView.messagebox.rename.id",
				SWT.OK | SWT.CANCEL,
				SWT.OK, true,
				getComposite().getDisplay(), 
				MessageBoxWindow.ICON_WARNING,
				MessageText.getString( "FilesView.rename.confirm.delete.title" ),
				MessageText.getString( "FilesView.rename.confirm.delete.text", new String[]{ file.toString()})) == SWT.OK;
	}
	
	private void moveFile(final DiskManagerFileInfo fileInfo, final File target) {

		// this behaviour should be put further down in the core but I'd rather not
		// do so close to release :(
		final boolean[] result = { false };
		
		FileUtil.runAsTask(new AzureusCoreOperationTask() {
			public void run(AzureusCoreOperation operation) {
					result[0] = fileInfo.setLink(target);
				}
			}
		);

		if (!result[0]){
			MessageBox mb = new MessageBox(getComposite().getShell(),SWT.ICON_ERROR | SWT.OK);
			mb.setText(MessageText.getString("FilesView.rename.failed.title"));
			mb.setMessage(MessageText.getString("FilesView.rename.failed.text"));
			mb.open();	    					
		}

	}
  
	protected void rename(TableRowCore[] rows, boolean rename_it, boolean retarget_it) {
	 	if (manager == null) {return;}
	 	if (rows.length == 0) {return;}
	 	
	 	String save_dir = null;
	 	if (!rename_it && retarget_it) {
	 		save_dir = askForSaveDirectory((DiskManagerFileInfo)rows[0].getDataSource(true));
	 		if (save_dir == null) {return;}
	 	}
	 	
		boolean	paused = false;
		try {
			for (int i=0; i<rows.length; i++) {
				TableRowCore row = rows[i];
				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)rows[i].getDataSource(true);
				File existing_file = fileInfo.getFile(true);
				File f_target = null;
				if (rename_it && retarget_it) {
					String s_target = askForRetargetedFilename(fileInfo);
					if (s_target != null)
						f_target = new File(s_target);
				}
				else if (rename_it) {
					String s_target = askForRenameFilename(fileInfo);
					if (s_target != null)
						f_target = new File(existing_file.getParentFile(), s_target);
				}
				else {
					// Parent directory has changed.
					f_target = new File(save_dir, existing_file.getName());
				}
				
				// So are we doing a rename?
				if (f_target == null) {continue;}
			
			    if (!paused) {paused = manager.pause();}
			    
    			if (f_target.exists()){
    				
    				// Nothing to do.
    				if (f_target.equals(existing_file))
    					continue;
    					
    				// A rewrite will occur, so we need to ask the user's permission.
    				else if (existing_file.exists() && !askCanOverwrite(existing_file))
    					continue;
    				
    				// If we reach here, then it means we are doing a real move, but there is
    				// no existing file.
    			}
    					
    			moveFile(fileInfo, f_target);
    			row.invalidate();
			}
		}
		finally {
			if (paused){manager.resume();}
		}
	}
	
	
  protected void
  changePriority(
	  int				type ,
	  TableRowCore[]	rows )
  {
	  	if ( manager == null){
	  		
	  		return;
	  	}
  				
		boolean	paused = false;		

		try{
	
			for (int i=0;i<rows.length;i++){

				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)rows[i].getDataSource(true);

				boolean	this_paused;
				
				if ( type == 0){
					
		   			fileInfo.setPriority(true);
		   			
					this_paused = setSkipped( fileInfo, false, false );
					
				}else if ( type == 1 ){
					
		 			fileInfo.setPriority(false);
		 			
		 			this_paused = setSkipped( fileInfo, false, false );
					
				}else if ( type == 2 ){
					
					this_paused = setSkipped( fileInfo, true, false );
					
				}else{
					
					this_paused = setSkipped( fileInfo, true, true );
				}
				
				paused = paused || this_paused;
			}
		}finally{
			
			if ( paused ){
			
				manager.resume();
			}
		}
  }
  
  protected boolean
  setSkipped(
	 DiskManagerFileInfo	info,
	 boolean				skipped,
	 boolean				delete_action )
  {
		// if we're not managing the download then don't do anything other than
		// change the file's priority
	
	if ( !manager.isPersistent()){
		
		info.setSkipped( skipped );

		return( false );
	}
	
	File	existing_file 			= info.getFile(true);	
	int		existing_storage_type	= info.getStorageType();
	
		// we can't ever have storage = COMPACT and !skipped
		
	int		new_storage_type;
	
	if ( existing_file.exists()){
		
		if (!skipped ){
			
			new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			
		}else{
	
			boolean	delete_file;
			
			if ( delete_action ){
				
				delete_file =
					MessageBoxWindow.open( 
						"FilesView.messagebox.delete.id",
						SWT.OK | SWT.CANCEL,
						SWT.OK, true,
						getComposite().getDisplay(), 
						MessageBoxWindow.ICON_WARNING,
						MessageText.getString( "FilesView.rename.confirm.delete.title" ),
						MessageText.getString( "FilesView.rename.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.OK;
				
			}else{
				
					// OK, too many users have got confused over the option to truncate files when selecting
					// do-not-download so I'm removing it
				
				delete_file	= false;
				
				/*
				delete_file =
					MessageBoxWindow.open( 
						"FilesView.messagebox.skip.id",
						SWT.YES | SWT.NO,
						SWT.YES | SWT.NO,
						getComposite().getDisplay(), 
						MessageBoxWindow.ICON_WARNING,
						MessageText.getString( "FilesView.rename.confirm.delete.title" ),
						MessageText.getString( "FilesView.skip.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.YES;
				*/
			}

			if ( delete_file ){
				
				new_storage_type	= DiskManagerFileInfo.ST_COMPACT;

			}else{
				
				new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			}
		}
	}else{
		
		if ( skipped ){
			
			boolean	compact_disabled = MessageBoxWindow.getRememberedDecision( 
											"FilesView.messagebox.skip.id",
											SWT.YES | SWT.NO ) == SWT.NO;

			if ( compact_disabled ){
				
				new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
				
			}else{
				
				new_storage_type	= DiskManagerFileInfo.ST_COMPACT;
			}
		}else{
			
			new_storage_type	= DiskManagerFileInfo.ST_LINEAR;

		}
	}
	
	boolean	ok;
	
	boolean	paused	= false;
	
	if ( existing_storage_type != new_storage_type ){
		
		if ( new_storage_type == DiskManagerFileInfo.ST_COMPACT ){
			
			paused = manager.pause();
		}
		
		ok = info.setStorageType( new_storage_type );
		
	}else{
		
		ok = true;
	}
	
	if ( ok ){
		
		info.setSkipped( skipped );
	}
	
	return( paused );
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public synchronized void refresh(boolean bForceSort) {
  	if (refreshing)
  		return;

  	try {
	  	refreshing = true;
	    if(getComposite() == null || getComposite().isDisposed())
	      return;
	
	    removeInvalidFileItems();
	
	    DiskManagerFileInfo files[] = getFileInfo();
		
	    if (files != null && getTable().getItemCount() != files.length && files.length > 0) {
		    Object filesCopy[] = new Object[files.length]; 
		    System.arraycopy(files, 0, filesCopy, 0, files.length);

		    addDataSources(filesCopy);
		    processDataSourceQueue();
	    }
	    
	    super.refresh(bForceSort);
  	} finally {
  		refreshing = false;
  	}
  }
  
  private void removeInvalidFileItems() {
    
    final DiskManagerFileInfo files[] = getFileInfo();

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
  

  
  /* SubMenu for column specific tasks.
   */
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {

    if (sColumnName.equals("path")) {
      path_item = new MenuItem( menuThisColumn, SWT.CHECK );
      
      menuThisColumn.addListener( SWT.Show, new Listener() {
        public void handleEvent(Event e) {
          path_item.setSelection( show_full_path );
        }
      });
      
      Messages.setLanguageText(path_item, "FilesView.fullpath");
      
      path_item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          show_full_path = path_item.getSelection();
          columnInvalidate("path");
          refreshTable(false);
          COConfigurationManager.setParameter( "FilesView.show.full.path", show_full_path );
        }
      });
      
    }
  }
  
  
  private DiskManagerFileInfo[]
  getFileInfo()
  {
  	if (manager == null)
  		return null;
	  return( manager.getDiskManagerFileInfo());
  }
}
