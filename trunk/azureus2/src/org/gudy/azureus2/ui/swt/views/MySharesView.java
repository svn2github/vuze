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

/**
 * @author parg
 *
 */

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.tableitems.SharingTableItem;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.local.*;

public class 
MySharesView 
	extends AbstractIView
	implements ShareManagerListener , SortableTable
{
	private GlobalManager	global_manager;
	private Composite 		composite; 
	private Composite 		panel;
	private Table 			table;
	
	private Map 	objectToSortableItem 	= new HashMap();
	private Map 	tableItemToObject		= new HashMap();

	private TableSorter sorter;
  
	public 
	MySharesView(
		GlobalManager globalManager) 
	{
		global_manager = globalManager;
	}
	 
	public void 
	initialize(
		Composite composite0 ) 
	{
	  if( panel != null ){
	  	      
		return;
	  }
	  
	  composite = new Composite(composite0, SWT.NULL);
	  GridLayout layout = new GridLayout();
	  layout.numColumns = 1;
	  layout.horizontalSpacing = 0;
	  layout.verticalSpacing = 0;
	  layout.marginHeight = 0;
	  layout.marginWidth = 0;
	  composite.setLayout(layout);
	  GridData gridData = new GridData(GridData.FILL_BOTH);
          
	  panel = new Composite(composite, SWT.NULL);
	  panel.setLayoutData(gridData);
    
	  layout = new GridLayout(1, false);
	  layout.marginHeight = 0;
	  layout.marginWidth = 0;
	  layout.verticalSpacing = 0;
	  layout.horizontalSpacing = 0;
	  panel.setLayout(layout);
	  
	  table = new Table(panel, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
	  
	  gridData = new GridData(GridData.FILL_BOTH); 
	  
	  table.setLayoutData(gridData);
	  
	  String[] columnsHeader = 	{ 	"name", 	"type" 	};
	  
	  int[] columnsSize = 		{ 	400, 		100		};
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		columnsSize[i] = COConfigurationManager.getIntParameter("MySharesView.".concat(columnsHeader[i]), columnsSize[i]);
	  }

	  ControlListener resizeListener = new ControlAdapter() {
		public void controlResized(ControlEvent e) {
		  saveTableColumns((TableColumn) e.widget);
		}
	  };
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		TableColumn column = new TableColumn(table, SWT.NULL);
		
		Messages.setLanguageText(column, "MySharesView.".concat(columnsHeader[i]));
		
		column.setWidth(columnsSize[i]);
		
		column.addControlListener(resizeListener);
	  }
	
	  sorter = new TableSorter(this,"MySharesView","name",false);
    
	  sorter.addStringColumnListener(table.getColumn(0),"name");
	  sorter.addStringColumnListener(table.getColumn(1),"type");
    
	  table.setHeaderVisible(true);	 

	  Menu menu = new Menu(composite.getShell(), SWT.POP_UP);

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
		   TableItem[] tis = table.getSelection();

		   //itemStart.setEnabled(false);
		   //itemStop.setEnabled(false);
		   itemRemove.setEnabled(false);

		   if (tis.length > 0) {
		   	
				boolean	start_ok 	= true;
				boolean	stop_ok		= true;
						
				for (int i=0;i<tis.length;i++){
					
					TableItem	ti = tis[i];
					
					/*
					TRHostTorrent	host_torrent = (TRHostTorrent)tableItemToObject.get( ti );
					
					int	status = host_torrent.getStatus();
					
					if ( status != TRHostTorrent.TS_STOPPED ){
						
						start_ok	= false;
						
					}
					
					if ( status != TRHostTorrent.TS_STARTED ){
						
						stop_ok = false;
					}
					*/
				}
				
		   		//itemStart.setEnabled(start_ok);
			 	//itemStop.setEnabled(stop_ok);
			 	itemRemove.setEnabled(true);
		   }
		 }
	   });

	   /*
	   itemStart.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   //startSelectedTorrents();
		 }    
	   });
	   
	   itemStop.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   //stopSelectedTorrents();
		 }    
	   });
	   */
	   
	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   removeSelectedShares();
		 }   
	   });
	   
		table.setMenu( menu );
		
		table.addMouseListener(new MouseAdapter() {
		   public void mouseDoubleClick(MouseEvent mEvent) {
			 TableItem[] tis = table.getSelection();
			 if (tis.length == 0) {
			   return;
			 }
			 TableItem ti = tis[0];
			
			 ShareResource	share = (ShareResource)tableItemToObject.get(ti);
			 
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
	
	public void
	resourceAdded(
		ShareResource		resource )
	{		
		synchronized ( tableItemToObject ){
			
			SharingTableItem item = (SharingTableItem)tableItemToObject.get(resource);
			
			if (item == null){
				
				item = new SharingTableItem( this, table, resource );
				
				objectToSortableItem.put(resource, item);		  					
			}	
		}
	}
	
	public void
	resourceModified(
		ShareResource		resource )
	{
	}
	
	public void
	resourceDeleted(
		ShareResource		resource )
	{
		SharingTableItem item = (SharingTableItem) objectToSortableItem.remove(resource);
		
		if (item != null) {
			
			tableItemToObject.remove( item.getTableItem());
			
			if(item != null)
				item.delete();
		}	
	}
	
	public void
	reportProgress(
		final int		percent_complete )
	{
	}
	
	public void
	reportCurrentTask(
		final String	task_description )
	{
	}
 
	private void saveTableColumns(TableColumn t)
	{
	  COConfigurationManager.setParameter((String) t.getData(), t.getWidth());
	  COConfigurationManager.save();
	}
	
	public Composite getComposite() {
	   return composite;
	 }

	public void 
	refresh() 
	{
		if (getComposite() == null || getComposite().isDisposed()){
	   
			return;
	   	}
		
		computePossibleActions();
		MainWindow.getWindow().refreshIconBar();
		
		synchronized(tableItemToObject){
			
			Iterator iter = objectToSortableItem.values().iterator();
			
			while (iter.hasNext()){
				
				if (panel.isDisposed()){
			  
					return;
			  	}
			  
				SharingTableItem item = (SharingTableItem) iter.next();
			  
			  	item.refresh();
			}
		}
	}	 

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#delete()
	  */
	 public void 
	 delete() 
	 {
	 	try{
	 		PluginInitializer.getDefaultInterface().getShareManager().removeListener(this);
	 		
	 	}catch( ShareException e ){
	 		
	 		e.printStackTrace();
	 	}
	 	
	   	MainWindow.getWindow().setMyShares(null);
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
	  */
	 public String getFullTitle() {
	   return MessageText.getString("MySharesView.myshares");
	 }
   
   public void putShare(TableItem item, ShareResource share) {
     tableItemToObject.put(item, share);
   }
   
   /*
    * SortableTable implementation
    *
    */
   
  public Map getObjectToSortableItemMap() {
    return objectToSortableItem;
  }

  public Map getTableItemToObjectMap() {
    return tableItemToObject;
  }

  public Table getTable() {
    return table;
  }
  
  private boolean start,stop,remove;
  
  private void computePossibleActions() {
    start = stop = remove = false;
    TableItem[] tis = table.getSelection();
    if(tis.length > 0) {
      remove = true;
      for (int i=0;i<tis.length;i++){        
        TableItem	ti = tis[i];        
        ShareResource	share = (ShareResource)tableItemToObject.get( ti );
        if(share == null)
          return;
        
        /*
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
  	/*
    if(itemKey.equals("start")) {
      //startSelectedTorrents();
      return;
    }
    if(itemKey.equals("stop")){
      //stopSelectedTorrents();
      return;
    }
    */
    if(itemKey.equals("remove")){
      removeSelectedShares();
      return;
    }
    return;
  }
  
  /*
  private void stopSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      
      TRHostTorrent	torrent = (TRHostTorrent)tableItemToObject.get(ti);
      if (torrent != null && torrent.getStatus() == TRHostTorrent.TS_STARTED){
        
        torrent.stop();
      }
    }
  }
  
  private void startSelectedTorrents() {
    TableItem[] tis = table.getSelection();		  
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      
      TRHostTorrent	torrent = (TRHostTorrent)tableItemToObject.get(ti);
      if (torrent != null && torrent.getStatus() == TRHostTorrent.TS_STOPPED){
        
        torrent.start();
      }
    }
  }
  */
  
  private void 
  removeSelectedShares()
  {
    TableItem[] tis = table.getSelection();		   
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      
      ShareResource	share = (ShareResource)tableItemToObject.get(ti);
      
      if (share != null){
        
      	try{
      		share.delete();
      		
      	}catch( Throwable e ){
      		
      	  Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
      	}
      }
    }
  }
}
