/*
 * File    : MyTrackerView.java
 * Created : 30-Oct-2003
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
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

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
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.tableitems.TrackerTableItem;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

import org.gudy.azureus2.pluginsimpl.local.*;
import org.gudy.azureus2.pluginsimpl.local.ui.mytracker.*;

public class 
MyTrackerView 
	extends AbstractIView
	implements TRHostListener , SortableTable
{
	private GlobalManager	global_manager;
	private Composite 		composite; 
	private Composite 		panel;
	private Table 			table;
	
	private Map 	objectToSortableItem 	= new HashMap();
	private Map 	tableItemToObject		= new HashMap();

  private TableSorter sorter;
  
	public 
	MyTrackerView(
		GlobalManager globalManager) 
	{
		global_manager = globalManager;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	 
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
	  
	  String[] columnsHeader = 	{ 	"name", 	"tracker", 		"status", 	"seeds", 		"peers",	"announces",
	  								"scrapes",	"completed",	"uploaded", "downloaded", 	"left",		"bytesin",
	  								"bytesinave",	"bytesout",	"bytesoutave"};
	  
	  int[] columnsSize = 		{ 	250, 		250,			60,			60,				60,			70,
	  								60,			70,				70,			70,				50,			50,
	  								50,			50,				50 };
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		columnsSize[i] = COConfigurationManager.getIntParameter("MyTrackerView.".concat(columnsHeader[i]), columnsSize[i]);
	  }

	  ControlListener resizeListener = new ControlAdapter() {
		public void controlResized(ControlEvent e) {
		  saveTableColumns((TableColumn) e.widget);
		}
	  };
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		TableColumn column = new TableColumn(table, SWT.NULL);
		
		Messages.setLanguageText(column, "MyTrackerView.".concat(columnsHeader[i]));
		
		column.setWidth(columnsSize[i]);
		
		column.addControlListener(resizeListener);
	  }
	
    sorter = new TableSorter(this, "MyTrackerView", "name",false);
    sorter.addStringColumnListener(table.getColumn(0),"name");
    sorter.addStringColumnListener(table.getColumn(1),"tracker");
    
    sorter.addIntColumnListener(table.getColumn(2),"status");    
    sorter.addIntColumnListener(table.getColumn(3),"seeds");
    sorter.addIntColumnListener(table.getColumn(4),"peers");
    sorter.addIntColumnListener(table.getColumn(5),"announces");
    sorter.addIntColumnListener(table.getColumn(6),"scrapes");
    sorter.addIntColumnListener(table.getColumn(7),"completed");
    sorter.addIntColumnListener(table.getColumn(8),"uploaded");
    sorter.addIntColumnListener(table.getColumn(9),"downloaded");
    sorter.addIntColumnListener(table.getColumn(10),"left");        
    sorter.addIntColumnListener(table.getColumn(11),"bytesin");
    sorter.addIntColumnListener(table.getColumn(12),"bytesinave");
    sorter.addIntColumnListener(table.getColumn(13),"bytesout");
    sorter.addIntColumnListener(table.getColumn(14),"bytesoutave");
    
	  table.setHeaderVisible(true);	 

		Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
   
	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MyTorrentsView.menu.start"); //$NON-NLS-1$
	   itemStart.setImage(ImageRepository.getImage("start"));

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
	   itemStop.setImage(ImageRepository.getImage("stop"));

	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
	   itemRemove.setImage(ImageRepository.getImage("delete"));

	   MyTrackerImpl pi_my_tracker = (MyTrackerImpl)PluginInitializer.getDefaultInterface().getUIManager().getMyTracker();
	   
	   MyTrackerContextMenuItemImpl[]	pi_menus = (MyTrackerContextMenuItemImpl[])pi_my_tracker.getContextMenus();
	   
	   for (int i=0;i<pi_menus.length;i++){
	   	
	   	   final MyTrackerContextMenuItemImpl	pi_menu = pi_menus[i];
	   	   
		   final MenuItem pi_item = new MenuItem(menu, SWT.PUSH);
		   
		   Messages.setLanguageText(pi_item, pi_menus[i].getResourceKey());
		   
		   pi_item.addListener(
		   		SWT.Selection, 
				new Listener() 
				{
		   			public void 
					handleEvent(Event e)
					{
		   				TableItem[] tis = table.getSelection();
		   				
			   		    for (int i = 0; i < tis.length; i++) {
			   		    	TableItem ti = tis[i];
			   		      
			   		    	TRHostTorrent	torrent = (TRHostTorrent)tableItemToObject.get(ti);
			   		    	
			   		    	if (torrent != null ){
			   		        
			   		    		pi_menu.fire( torrent );
			   		    	}
			   		    }			 
					}
				});
	   }
	   
	   menu.addListener(SWT.Show, new Listener() {
		 public void handleEvent(Event e) {
		   TableItem[] tis = table.getSelection();

		   itemStart.setEnabled(false);
		   itemStop.setEnabled(false);
		   itemRemove.setEnabled(false);

		   if (tis.length > 0) {
		   	
				boolean	start_ok 	= true;
				boolean	stop_ok		= true;
				boolean	remove_ok	= true;
				
				for (int i=0;i<tis.length;i++){
					
					TableItem	ti = tis[i];
					
					TRHostTorrent	host_torrent = (TRHostTorrent)tableItemToObject.get( ti );
					
					int	status = host_torrent.getStatus();
					
					if ( status != TRHostTorrent.TS_STOPPED ){
						
						start_ok	= false;
						
					}
					
					if ( status != TRHostTorrent.TS_STARTED ){
						
						stop_ok = false;
					}
					
					/*
					try{
						
						host_torrent.canBeRemoved();
						
					}catch( TRHostTorrentRemovalVetoException f ){
						
						remove_ok = false;
					}
					*/
				}
		   		itemStart.setEnabled(start_ok);
			 	itemStop.setEnabled(stop_ok);
			 	itemRemove.setEnabled(remove_ok);
		   }
		 }
	   });

	   itemStart.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   startSelectedTorrents();
		 }    
	   });
	   
	   itemStop.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   stopSelectedTorrents();
		 }    
	   });
	   
	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   removeSelectedTorrents();
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
			
			 TRHostTorrent	torrent = (TRHostTorrent)tableItemToObject.get(ti);
			 
			 if (torrent != null){
			 	
			 	List dms = global_manager.getDownloadManagers();
			 	
			 	for (int i=0;i<dms.size();i++){
			 		
			 		DownloadManager	dm = (DownloadManager)dms.get(i);
			 		
			 		if ( dm.getTorrent() == torrent.getTorrent()){
			 		
					 	MainWindow.getWindow().openManagerView(dm);
					 	
					 	break;
			 		}
			 	}
			 }
		   }
		 });	   
		TRHostFactory.create().addListener( this );
  }
	
	public void
	torrentAdded(
		TRHostTorrent		host_torrent )
	{	
		synchronized ( tableItemToObject ){
			
			TrackerTableItem item = (TrackerTableItem)tableItemToObject.get(host_torrent);
		  
		  	if (item == null){
		  	
				item = new TrackerTableItem(this,table, host_torrent);
				
		  	objectToSortableItem.put(host_torrent, item);		  					
			}	
		}
	}
	
	public void
	torrentChanged(
		TRHostTorrent		t )
	{
	}

	public void
	torrentRemoved(
		TRHostTorrent		host_torrent )
	{
		TrackerTableItem item = (TrackerTableItem) objectToSortableItem.remove(host_torrent);
		
		if (item != null) {
			
			tableItemToObject.remove( item.getTableItem());
			if(item != null)
			  item.delete();
		}		
	}

	public boolean
	handleExternalRequest(
		String			client,
		String			url,
		String			header,
		InputStream		is,
		OutputStream	os )
	
		throws IOException
	{
		return( false );
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
			  
			  	TrackerTableItem item = (TrackerTableItem) iter.next();
			  
			  	item.refresh();
			}
		}
    Utils.alternateTableBackground(table);
	}	 

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#delete()
	  */
	 public void 
	 delete() 
	 {
		TRHostFactory.create().removeListener( this );
		
	   	MainWindow.getWindow().setMyTracker(null);
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
	  */
	 public String getFullTitle() {
	   return MessageText.getString("MyTrackerView.mytracker");
	 }
   
   public void putHost(TableItem item, TRHostTorrent host_torrent) {
     tableItemToObject.put(item, host_torrent);
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
        TRHostTorrent	host_torrent = (TRHostTorrent)tableItemToObject.get( ti );
        if(host_torrent == null)
          return;
        
        int	status = host_torrent.getStatus();
        
        if ( status == TRHostTorrent.TS_STOPPED ){          
          start	= true;          
        }
        
        if ( status == TRHostTorrent.TS_STARTED ){          
          stop = true;
        }
        
        /*
        try{     	
        	host_torrent.canBeRemoved();
        	
        }catch( TRHostTorrentRemovalVetoException f ){
        	
        	remove = false;
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
    if(itemKey.equals("start")) {
      startSelectedTorrents();
      return;
    }
    if(itemKey.equals("stop")){
      stopSelectedTorrents();
      return;
    }
    if(itemKey.equals("remove")){
      removeSelectedTorrents();
      return;
    }
    return;
  }
  
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
  
  private void removeSelectedTorrents() {
    TableItem[] tis = table.getSelection();		   
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      
      TRHostTorrent	torrent = (TRHostTorrent)tableItemToObject.get(ti);
      if (torrent != null){
        
      	try{
      		torrent.remove();
      		
      	}catch( TRHostTorrentRemovalVetoException f ){
      		
      		String	message = f.getMessage();
      	      	
      		Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", f );
      	}
      }
    }
  }
}
