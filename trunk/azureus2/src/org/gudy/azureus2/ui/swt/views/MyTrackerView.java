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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.tableitems.TrackerTableItem;


public class 
MyTrackerView 
	extends AbstractIView
	implements TRHostListener
{
	private Composite composite; 
	private Composite panel;
	private Table table;
	
	private Map 	host_torrent_items 	= new HashMap();
	private Map 	host_torrents		= new HashMap();

	public 
	MyTrackerView(
		GlobalManager globalManager) 
	{
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
	  
	  String[] columnsHeader = 	{ 	"name", 	"tracker", 		"status", 	"seeds", 	"peers",	"announces",
	  								"uploaded", "downloaded", 	"left" };
	  
	  int[] columnsSize = 		{ 	250, 		250,			60,			60,			60,			70,
	  								70,			70,				50 };
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		columnsSize[i] = COConfigurationManager.getIntParameter("MyTrackerView." + columnsHeader[i], columnsSize[i]);
	  }

	  ControlListener resizeListener = new ControlAdapter() {
		public void controlResized(ControlEvent e) {
		  saveTableColumns((TableColumn) e.widget);
		}
	  };
	  
	  for (int i = 0; i < columnsHeader.length; i++){
	  	
		TableColumn column = new TableColumn(table, SWT.NULL);
		
		Messages.setLanguageText(column, "MyTrackerView." + columnsHeader[i]);
		
		column.setWidth(columnsSize[i]);
		
		column.addControlListener(resizeListener);
	  }
	  
	  /*
	  table.getColumn(0).addListener(SWT.Selection, new IntColumnListener("#")); //$NON-NLS-1$
	  table.getColumn(1).addListener(SWT.Selection, new StringColumnListener("name")); //$NON-NLS-1$
	  table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("size")); //$NON-NLS-1$
	  table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("done")); //$NON-NLS-1$
	  table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("status")); //$NON-NLS-1$
	  table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("seeds")); //$NON-NLS-1$
	  table.getColumn(6).addListener(SWT.Selection, new IntColumnListener("peers")); //$NON-NLS-1$
	  table.getColumn(7).addListener(SWT.Selection, new StringColumnListener("ds")); //$NON-NLS-1$
	  table.getColumn(8).addListener(SWT.Selection, new StringColumnListener("us")); //$NON-NLS-1$
	  table.getColumn(9).addListener(SWT.Selection, new StringColumnListener("eta")); //$NON-NLS-1$
	  table.getColumn(10).addListener(SWT.Selection, new StringColumnListener("tracker")); //$NON-NLS-1$
	  table.getColumn(11).addListener(SWT.Selection, new IntColumnListener("priority")); //$NON-NLS-1$
	*/
	
	  table.setHeaderVisible(true);
	  //table.addKeyListener(createKeyListener());


		Menu menu = new Menu(composite.getShell(), SWT.POP_UP);

    
	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MyTorrentsView.menu.start"); //$NON-NLS-1$

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$

	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$


	   menu.addListener(SWT.Show, new Listener() {
		 public void handleEvent(Event e) {
		   TableItem[] tis = table.getSelection();

		   itemStart.setEnabled(false);
		   itemStop.setEnabled(false);
		   itemRemove.setEnabled(false);

		   if (tis.length > 0) {
		   	
				boolean	start_ok 	= true;
				boolean	stop_ok		= true;
						
				for (int i=0;i<tis.length;i++){
					
					TableItem	ti = tis[i];
					
					TRHostTorrent	host_torrent = (TRHostTorrent)host_torrents.get( ti );
					
					int	status = host_torrent.getStatus();
					
					if ( status == TRHostTorrent.TS_STARTED ){
						
						start_ok	= false;
						
					}else if ( status != TRHostTorrent.TS_STARTED ){
						
						stop_ok = false;
					}
				}
		   		itemStart.setEnabled(start_ok);
			 	itemStop.setEnabled(stop_ok);
			 	itemRemove.setEnabled(true);
		   }
		 }
	   });

	   itemStart.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   TableItem[] tis = table.getSelection();
		   final boolean initStoppedDownloads = true;
		   for (int i = 0; i < tis.length; i++) {
			 TableItem ti = tis[i];
			 
			TRHostTorrent	torrent = (TRHostTorrent)host_torrents.get(ti);
			 if (torrent != null){
			 	
				torrent.start();
			 }
		   }
		 }
	   });
	   
	   itemStop.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   TableItem[] tis = table.getSelection();
		   final boolean initStoppedDownloads = true;
		   for (int i = 0; i < tis.length; i++) {
			 TableItem ti = tis[i];
			 
			TRHostTorrent	torrent = (TRHostTorrent)host_torrents.get(ti);
			 if (torrent != null){
			 	
				torrent.stop();
			 }
		   }
		 }
	   });
	   
	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   TableItem[] tis = table.getSelection();
		   final boolean initStoppedDownloads = true;
		   for (int i = 0; i < tis.length; i++) {
			 TableItem ti = tis[i];
			 
			TRHostTorrent	torrent = (TRHostTorrent)host_torrents.get(ti);
			 if (torrent != null){
			 	
				torrent.remove();
			 }
		   }
		 }
	   });
	   
		table.setMenu( menu );
	   
		TRHostFactory.create().addListener( this );
	}
	
	public void
	torrentAdded(
		TRHostTorrent		host_torrent )
	{	
		synchronized ( host_torrents ){
			
			TrackerTableItem item = (TrackerTableItem)host_torrents.get(host_torrent);
		  
		  	if (item == null){
		  	
				item = new TrackerTableItem(table, host_torrent);
				
		  		host_torrent_items.put(host_torrent, item);
		  		
				host_torrents.put(item.getTableItem(), host_torrent);
			}	
		}
	}
	
	public void
	torrentRemoved(
		TRHostTorrent		host_torrent )
	{
		TrackerTableItem item = (TrackerTableItem) host_torrent_items.remove(host_torrent);
		
		if (item != null) {
			
			host_torrents.remove( item.getTableItem());
			
			item.delete();
		}		
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
		
		Iterator iter = host_torrent_items.values().iterator();
		
		while (iter.hasNext()){
			
			if (panel.isDisposed()){
		  
				return;
		  	}
		  
		  	TrackerTableItem item = (TrackerTableItem) iter.next();
		  
		  	item.refresh();
		}
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
}
