/*
 * File    : MySharesView.java
 * Created : 18-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
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
 */


package org.gudy.azureus2.ui.swt.views;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.local.*;

/**
 * @author parg
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */
public class MySharesView 
       extends TableView
       implements ShareManagerListener
{
  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new TypeItem()
  };
	private GlobalManager	global_manager;
	
	public MySharesView(GlobalManager globalManager) {
    super(TableManager.TABLE_MYSHARES, "MySharesView", basicItems, "name", 
          SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		global_manager = globalManager;
	}
	 
  public void initialize(Composite composite) {
    super.initialize(composite);

		getTable().addMouseListener(new MouseAdapter() {
		   public void mouseDoubleClick(MouseEvent mEvent) {
			 TableItem[] tis = getTable().getSelection();
			 if (tis.length == 0) {
			   return;
			 }
			 ShareResource share = (ShareResource)getFirstSelectedDataSource();
			 
			 if (share != null){
			 	
			 	List dms = global_manager.getDownloadManagers();
			 	
			 	for (int i=0;i<dms.size();i++){
			 		
			 		DownloadManager	dm = (DownloadManager)dms.get(i);
			 		
			 		try{
				 		byte[]	share_hash = null;
				 		
				 		if ( share.getType() == ShareResource.ST_DIR ){
				 			
				 			share_hash = ((ShareResourceDir)share).getItem().getTorrent().getHash();
				 			
				 		}else if ( share.getType() == ShareResource.ST_FILE ){
				 			
				 			share_hash = ((ShareResourceFile)share).getItem().getTorrent().getHash();
				 		}
				 		
				 		if ( Arrays.equals( share_hash, dm.getTorrent().getHash())){
				 		
						 	MainWindow.getWindow().openManagerView(dm);
						 	
						 	break;
				 		}
			 		}catch( Throwable e ){
			 			
			 			e.printStackTrace();
			 		}
			 	}
			 }
		   }
		 });	
		 createRows();
	}

  private void createRows() {
		try{

			ShareManager	sm = PluginInitializer.getDefaultInterface().getShareManager();
			
			ShareResource[]	shares = sm.getShares();
			
			for (int i=0;i<shares.length;i++){
				
				resourceAdded(shares[i]);
			}
			
			sm.addListener(this);
			
		}catch( ShareException e ){
			
			e.printStackTrace();
		}
	}

  public void tableStructureChanged() {
    super.tableStructureChanged();
    createRows();
  }

  public void fillMenu(final Menu menu) {
		/*
	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MySharesView.menu.start"); //$NON-NLS-1$
	   itemStart.setImage(ImageRepository.getImage("start"));

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MySharesView.menu.stop"); //$NON-NLS-1$
	   itemStop.setImage(ImageRepository.getImage("stop"));
	   */
		
	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MySharesView.menu.remove"); //$NON-NLS-1$
	   itemRemove.setImage(ImageRepository.getImage("delete"));


	   menu.addListener(SWT.Show, new Listener() {
		 public void handleEvent(Event e) {
		   Object[] shares = getSelectedDataSources();

		   itemRemove.setEnabled(shares.length > 0);
		 }
	   });

	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   removeSelectedShares();
		 }   
	   });

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
	}
	
	public void resourceAdded(ShareResource resource) {		
	  addDataSource(resource);
	}
	
	public void resourceModified(ShareResource resource) { }
	
	public void resourceDeleted(ShareResource resource) {
	  removeDataSource(resource);
	}
	
	public void reportProgress(final int percent_complete) {	}
	
	public void	reportCurrentTask(final String task_description) { }
 
	public void refresh() {
		if (getComposite() == null || getComposite().isDisposed()) {
      return;
	  }
		
		computePossibleActions();
		MainWindow.getWindow().refreshIconBar();
		
    super.refresh();
	}	 

  public void delete() {
    super.delete();

	 	try {
	 		PluginInitializer.getDefaultInterface().getShareManager().removeListener(this);
	 	}catch( ShareException e ){
	 		e.printStackTrace();
	 	}
	 	
    MainWindow.getWindow().setMyShares(null);
  }

  private boolean start,stop,remove;
  
  private void computePossibleActions() {
    start = stop = remove = false;
    Object[] shares = getSelectedDataSources();
    if (shares.length > 0) {
      remove = true;
      for (int i=0; i < shares.length; i++){        
        /*
        ShareResource	share = (ShareResource)shares[i];
        
        int	status = host_torrent.getStatus();
        
        if ( status == TRHostTorrent.TS_STOPPED ){          
          start	= true;          
        }
        
        if ( status == TRHostTorrent.TS_STARTED ){          
          stop = true;
        }
        */
      }
    }
  }
  
  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    return false;
  }
  

  public void itemActivated(String itemKey) {
    if(itemKey.equals("remove")){
      removeSelectedShares();
      return;
    }
  }
  
  private void 
  removeSelectedShares()
  {
    Object[] shares = getSelectedDataSources();
    for (int i = 0; i < shares.length; i++) {
    	try{
    		((ShareResource)shares[i]).delete();
    		
    	}catch( Throwable e ){
    		
    	  Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
    	}
    }
  }
}
