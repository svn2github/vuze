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


import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.MessageBoxWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
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
    new StorageTypeItem()
  };
  
  private DownloadManager download_manager;
  
  public static boolean show_full_path = COConfigurationManager.getBooleanParameter( "FilesView.show.full.path", false );
  private MenuItem path_item;
  

  public FilesView(DownloadManager manager) {
    super(TableManager.TABLE_TORRENT_FILES, "FilesView", 
          basicItems, "firstpiece", SWT.MULTI | SWT.FULL_SELECTION);
    bSkipFirstColumn = true;
    ptIconSize = new Point(16, 16);
    download_manager = manager;
  }

  public void initialize(Composite composite) {
    super.initialize(composite);

    
    getTable().addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent mEvent) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)getFirstSelectedDataSource();
        if (fileInfo != null && fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
          Program.launch(fileInfo.getFile(true).toString());
      }
    });

  }

  
  
  
  public void fillMenu(final Menu menu) {
	  
    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open");
    Utils.setMenuItemImage(itemOpen, "run");
    
    final MenuItem itemRename = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRename, "FilesView.menu.rename");

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

    final MenuItem itemDelete = new MenuItem(menuPriority, SWT.CASCADE);
    Messages.setLanguageText(itemDelete, "wizard.multitracker.delete");	// lazy but we're near release

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
    	FilesView.this.showMenu();
    		
        Object[] infos = getSelectedDataSources();
        if (infos.length == 0) {
          itemOpen.setEnabled(false);
          itemPriority.setEnabled(false);
          itemRename.setEnabled(false);
          return;
        }
         
        boolean open 		= true;
        boolean	all_compact	= true;
        
        for (int i = 0; i < infos.length; i++){
        	
          DiskManagerFileInfo file_info = (DiskManagerFileInfo)infos[i];
          
          if (file_info.getAccessMode() != DiskManagerFileInfo.READ){
        	  
            open = false;
          }
          
		  if ( file_info.getStorageType() != DiskManagerFileInfo.ST_COMPACT ){

			  all_compact	= false;
		  }
        }
        
        	// we can only open files if they are read-only
        
        itemOpen.setEnabled( open );
        
        	// can't rename files for non-persistent downloads (e.g. shares) as these
        	// are managed "externally"
        
        itemRename.setEnabled( download_manager.isPersistent());
                
        	// no point in changing priority of completed downloads
        
        itemPriority.setEnabled( !download_manager.isDownloadComplete() );
        
        itemDelete.setEnabled( !( download_manager.isDownloadComplete() || all_compact ));
      }
    });       

    itemOpen.addListener(SWT.Selection, new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        if (fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
          Program.launch(fileInfo.getFile(true).toString());
      }
    });
    
    itemRename.addListener(
    	SWT.Selection, 
    	new SelectedTableRowsListener() 
    	{
    		public void 
    		run(
    			TableRowCore row) 
    		{
    			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
   
    			FileDialog fDialog = new FileDialog(getComposite().getShell(), SWT.SYSTEM_MODAL | SWT.SAVE);  
    			
    			File	existing_file = fileInfo.getFile(true);
    			
    			fDialog.setFilterPath(existing_file.getParent());
    			
    			fDialog.setFileName( existing_file.getName());
    			
    			fDialog.setText( MessageText.getString("FilesView.rename.choose.path"));
          
    			String	res = fDialog.open();
          
    			if ( res != null ){
        	  
    				boolean	paused = false;
    				
    				try{
    					paused = download_manager.pause();
    					
    						// gotta pick up the skeleton entry if we paused
    					
    					fileInfo = download_manager.getDiskManagerFileInfo()[fileInfo.getIndex()];

	    				File	target = new File( res );
	        	  
	    				boolean	ok = false;
	        	  
	    				if ( target.exists()){
	        		 
	    					if ( target.equals( existing_file )){
	        			  
	    						// nothing to do
	    						
	    					}else if ( !existing_file.exists()){
		
	    						ok	= true;
	    						
	    					}else{
	        			  
	    						if ( MessageBoxWindow.open( 
	    								"FilesView.messagebox.rename.id",
	    								SWT.OK | SWT.CANCEL,
	    								SWT.OK,
	    								getComposite().getDisplay(), 
	    								MessageBoxWindow.ICON_WARNING,
	    								MessageText.getString( "FilesView.rename.confirm.delete.title" ),
	    								MessageText.getString( "FilesView.rename.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.OK ){
		        		    			        		    		
	    							ok	= true;
	    						}
	    					}
	    				}else{
	        			  
	    					ok = true;
	    				}
	        	  
	    				if ( ok ){
	        		  
	    					if ( !fileInfo.setLink( target )){
	    						
	    						MessageBox mb = new MessageBox(getComposite().getShell(),SWT.ICON_ERROR | SWT.OK );
	    					    
	    						mb.setText(MessageText.getString("FilesView.rename.failed.title"));
	    					    
	    						mb.setMessage(MessageText.getString("FilesView.rename.failed.text"));
	    					    
	    						mb.open();	    					
	    					}
	        		  
	    					row.refresh( true );
	    				}
    				
	    			}finally{
	    				
	    				if ( paused ){
	    					
	    					download_manager.resume();
	    				}
	    			}
    			}
    		}
    	});
    
    itemHigh.addListener(SWT.Selection, 
    	new SelectedTableRowsListener() 
    	{
    		public void
    		runAll(
    			TableRowCore[]	rows )
    		{
    			changePriority( 0, rows );
    		}
    	});
        
    itemLow.addListener(SWT.Selection, 
	   	new SelectedTableRowsListener() 
    	{
    		public void
    		runAll(
    			TableRowCore[]	rows )
    		{
    			changePriority( 1, rows );
    		}
    	});
        
    itemSkipped.addListener(SWT.Selection, 
	   	new SelectedTableRowsListener() 
    	{
    		public void
    		runAll(
    			TableRowCore[]	rows )
    		{
    			changePriority( 2, rows );
    		}
    	});
    
    itemDelete.addListener(SWT.Selection, 
    	   	new SelectedTableRowsListener() 
        	{
        		public void
        		runAll(
        			TableRowCore[]	rows )
        		{
        			changePriority( 3, rows );
        		}
        	});

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
  }
  
  protected void
  changePriority(
	  int				type ,
	  TableRowCore[]	rows )
  {
		DiskManagerFileInfo[] file_infos	= new DiskManagerFileInfo[rows.length];
			
		boolean	pause = false;
		
			// if the user has defaulted the storage switch option to "don't use compact" then
			// we don't need to pause the download etc.
		
		boolean	compact_disabled = MessageBoxWindow.getRememberedDecision( 
										"FilesView.messagebox.skip.id",
										SWT.YES | SWT.NO ) == SWT.NO;
				
		for (int i=0;i<file_infos.length;i++){
					
			DiskManagerFileInfo file_info = file_infos[i] = (DiskManagerFileInfo)rows[i].getDataSource(true);
				
			int	storage_type = file_info.getStorageType();
			
				// delete + non-compact requires work
			
			if ( type == 3 && storage_type != DiskManagerFileInfo.ST_COMPACT ){
				
				pause	= true;
				
			}else if ( type != 3 && storage_type == DiskManagerFileInfo.ST_COMPACT ){
					
				pause	= true;
				
			}else if ( file_info.isSkipped() && type != 2 ){
				
					// skipped -> normal
				
				if ( compact_disabled ){
					
						// compact is disabled but we have a residual compact file 
					
					pause =  storage_type == DiskManagerFileInfo.ST_COMPACT;
					
				}else{
					
					pause	= true;
				}
					
			}else if ( type == 2 && !file_info.isSkipped()){
				
					// normal -> skipped

					// if compact is disabled this transition doesn't require pausing
				
				if ( !compact_disabled ){
				
					pause	= true;
				}
			}
		}
		
		try{
			if ( pause ){
				
				pause = download_manager.pause();
				
				if ( pause ){
						// we've got to pick up the new info as stopping causes skeleton
						// info to be returned and this is the only info that will
						// accept changes in storage strategy...
					
					DiskManagerFileInfo[] new_info = download_manager.getDiskManagerFileInfo();
					
					for (int i=0;i<file_infos.length;i++){
						
						file_infos[i] = new_info[file_infos[i].getIndex()];
					}
				}
			}
		
			for (int i=0;i<file_infos.length;i++){

				DiskManagerFileInfo	fileInfo = file_infos[i];
				
				if ( type == 0){
					
		   			fileInfo.setPriority(true);
		   			
					setSkipped( fileInfo, compact_disabled, false, false );
					
				}else if ( type == 1 ){
					
		 			fileInfo.setPriority(false);
		 			
					setSkipped( fileInfo, compact_disabled, false, false );
					
				}else if ( type == 2 ){
					
					setSkipped( fileInfo, compact_disabled, true, false );
					
				}else{
					
					setSkipped( fileInfo, false, true, true );
				}
			}
		}finally{
			
			if ( pause ){
			
				download_manager.resume();
			}
		}
  }
  
  protected void
  setSkipped(
	 DiskManagerFileInfo	info,
	 boolean				compact_disabled,
	 boolean				skipped,
	 boolean				force_compact )
  {
		// if we're not managing the download then don't do anything other than
		// change the file's priority
	
	if ( !download_manager.isPersistent()){
		
		info.setSkipped( skipped );

		return;
	}
	
	File	existing_file 			= info.getFile(true);	
	int		existing_storage_type	= info.getStorageType();
	
		// we can't ever have storage = COMPACT and !skipped
		
	int		new_storage_type;
	
	if ( existing_file.exists()){
		
		if (!skipped ){
			
			new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			
		}else{
	
			boolean	delete_file = 
				force_compact ||
				MessageBoxWindow.open( 
					"FilesView.messagebox.skip.id",
					SWT.YES | SWT.NO,
					SWT.YES | SWT.NO,
					getComposite().getDisplay(), 
					MessageBoxWindow.ICON_WARNING,
					MessageText.getString( "FilesView.rename.confirm.delete.title" ),
					MessageText.getString( "FilesView.skip.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.YES;
			
			if ( delete_file ){
				
				new_storage_type	= DiskManagerFileInfo.ST_COMPACT;

			}else{
				
				new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			}
		}
	}else{
		
		if ( skipped ){
			
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
	
	if ( existing_storage_type != new_storage_type ){
		
		ok = info.setStorageType( new_storage_type );
		
	}else{
		
		ok = true;
	}
	
	if ( ok ){
		
		info.setSkipped( skipped );
	}
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    removeInvalidFileItems();

    DiskManagerFileInfo files[] = getFileInfo();
	
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
          forceFullRefresh();
          COConfigurationManager.setParameter( "FilesView.show.full.path", show_full_path );
        }
      });
      
    }
  }
  
  
  private void forceFullRefresh() {
    super.tableInvalidate();
    super.refresh();
  }
  
  private DiskManagerFileInfo[]
  getFileInfo()
  {
	  return( download_manager.getDiskManagerFileInfo());
  }
  
  public void delete() {
    super.delete();
  }
}
