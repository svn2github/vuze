/*
 * File    : MyTrackerView.java
 * Created : 30-Oct-2003
 * By      : parg
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

import com.aelitis.azureus.core.AzureusCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.CategoryManagerListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostListener;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrentRemovalVetoException;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.CategoryAdderWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.tableitems.mytracker.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;


/**
 * @author parg
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */

public class 
MyTrackerView 
	extends TableView
	implements TRHostListener, CategoryManagerListener
{
  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new TrackerItem(),
    new StatusItem(),
    new CategoryItem(),
    new PassiveItem(),
    new SeedCountItem(),
    new PeerCountItem(),
    new BadNATCountItem(),
	new AnnounceCountItem(),
    new ScrapeCountItem(),
    new CompletedCountItem(),
    new UploadedItem(),
    new DownloadedItem(),
    new LeftItem(),
    new TotalBytesInItem(),
    new AverageBytesInItem(),
    new TotalBytesOutItem(),
    new AverageBytesOutItem(),
    new DateAddedItem(),
  };

	protected static final TorrentAttribute	category_attribute = 
		TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY );

	private AzureusCore	azureus_core;

	private Menu			menuCategory;

	public 
	MyTrackerView(
		AzureusCore		_azureus_core ) 
	{
    super(TableManager.TABLE_MYTRACKER, "MyTrackerView", basicItems, "name", 
          SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    azureus_core = _azureus_core;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void 
	initialize(
		Composite composite0 ) 
	{
    super.initialize(composite0);

		azureus_core.getTrackerHost().addListener( this );
		
		final Table table = getTable();
		table.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent mEvent) {
        TRHostTorrent torrent = (TRHostTorrent)getFirstSelectedDataSource();
        if (torrent == null)
          return;
			  DownloadManager	dm = azureus_core.getGlobalManager().getDownloadManager(torrent.getTorrent());
			  if (dm != null)
				 	MainWindow.getWindow().openManagerView(dm);
		   }
		 });	   
  }
    
  public void tableStructureChanged() {
    //1. Unregister for item creation
  	azureus_core.getTrackerHost().removeListener( this );
    
    super.tableStructureChanged();

    //5. Re-add as a listener
    azureus_core.getTrackerHost().addListener( this );
  }

  public void fillMenu(final Menu menu) {	  
	    menuCategory = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
	    final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
	    Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
	    //itemCategory.setImage(ImageRepository.getImage("speed"));
	    itemCategory.setMenu(menuCategory);

	    addCategorySubMenu();
	    
	    new MenuItem(menu, SWT.SEPARATOR);

	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MyTorrentsView.menu.start"); //$NON-NLS-1$
	   Utils.setMenuItemImage(itemStart, "start");

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
	   Utils.setMenuItemImage(itemStop, "stop");

	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
	   Utils.setMenuItemImage(itemRemove, "delete");

     menu.addListener(SWT.Show, new Listener() {
		 public void handleEvent(Event e) {
			 MyTrackerView.this.showMenu();
		   Object[] hostTorrents = getSelectedDataSources();

		   itemStart.setEnabled(false);
		   itemStop.setEnabled(false);
		   itemRemove.setEnabled(false);

		   if (hostTorrents.length > 0) {
		   	
				boolean	start_ok 	= true;
				boolean	stop_ok		= true;
				boolean	remove_ok	= true;
				
				for (int i = 0; i < hostTorrents.length; i++) {
					
					TRHostTorrent	host_torrent = (TRHostTorrent)hostTorrents[i];
					
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

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
  }
	
	public void
	torrentAdded(
		TRHostTorrent		host_torrent )
	{	
	  addDataSource(host_torrent);
	}
	
	public void torrentChanged(TRHostTorrent t) { }

	public void
	torrentRemoved(
		TRHostTorrent		host_torrent )
	{
	  removeDataSource(host_torrent);
	}

	public boolean
	handleExternalRequest(
		String			client,
		String			url,
		URL				absolute_url,
		String			header,
		InputStream		is,
		OutputStream	os )
	
		throws IOException
	{
		return( false );
	}
 
	public void 
	refresh() 
	{
		if (getComposite() == null || getComposite().isDisposed()){
	   
			return;
	   	}
		
		computePossibleActions();
		MainWindow.getWindow().refreshIconBar();
		
		// Store values for columns that are calculate from peer information, so 
		// that we only have to do one loop.  (As opposed to each cell doing a loop)
		// Calculate code copied from TrackerTableItem
		TableRowCore[] rows = getRowsUnordered();
		for (int x = 0; x < rows.length; x++) {
		  if (rows[x] == null)
		    continue;
      TRHostTorrent	host_torrent = (TRHostTorrent)rows[x].getDataSource(true);
      if (host_torrent == null)
        continue;
 		 		
  		long	uploaded	= host_torrent.getTotalUploaded();
  		long	downloaded	= host_torrent.getTotalDownloaded();
  		long	left		= host_torrent.getTotalLeft();
  		
  		int		seed_count	= host_torrent.getSeedCount();
  		
  		host_torrent.setData("GUI_PeerCount", new Long(host_torrent.getLeecherCount()));
  		host_torrent.setData("GUI_SeedCount", new Long(seed_count));
  		host_torrent.setData("GUI_BadNATCount", new Long(host_torrent.getBadNATCount()));
  		host_torrent.setData("GUI_Uploaded", new Long(uploaded));
  		host_torrent.setData("GUI_Downloaded", new Long(downloaded));
  		host_torrent.setData("GUI_Left", new Long(left));

      if ( seed_count != 0 ){
        if (!rows[x].getForeground().equals(Colors.blues[Colors.BLUES_MIDDARK])) {
          rows[x].setForeground(Colors.blues[Colors.BLUES_MIDDARK]);
        }
      }
    }
    super.refresh();
	}	 

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#delete()
	  */
	 public void 
	 delete() 
	 {
	 	super.delete();
	 	azureus_core.getTrackerHost().removeListener( this );
	 }

  
  private boolean start,stop,remove;
  
  private void computePossibleActions() {
    start = stop = remove = false;
    Object[] hostTorrents = getSelectedDataSources();
    if (hostTorrents.length > 0) {
      remove = true;
      for (int i = 0; i < hostTorrents.length; i++) {
        TRHostTorrent	host_torrent = (TRHostTorrent)hostTorrents[i];
        
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
  }
  
  private void stopSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        TRHostTorrent	torrent = (TRHostTorrent)row.getDataSource(true);
        if (torrent.getStatus() == TRHostTorrent.TS_STARTED)
          torrent.stop();
      }
    });
  }
  
  private void startSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        TRHostTorrent	torrent = (TRHostTorrent)row.getDataSource(true);
        if (torrent.getStatus() == TRHostTorrent.TS_STOPPED)
          torrent.start();
      }
    });
  }
  
  private void removeSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        TRHostTorrent	torrent = (TRHostTorrent)row.getDataSource(true);
      	try{
      		torrent.remove();
      		
      	}catch( TRHostTorrentRemovalVetoException f ){
      		
      		Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", f );
      	}
      }
    });
  }

  private void addCategorySubMenu() {
    MenuItem[] items = menuCategory.getItems();
    int i;
    for (i = 0; i < items.length; i++) {
      items[i].dispose();
    }

    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
      if (catUncat != null) {
        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
        Messages.setLanguageText(itemCategory, catUncat.getName());
        itemCategory.setData("Category", catUncat);
        itemCategory.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event event) {
            MenuItem item = (MenuItem)event.widget;
            assignSelectedToCategory((Category)item.getData("Category"));
          }
        });

        new MenuItem(menuCategory, SWT.SEPARATOR);
      }

      for (i = 0; i < categories.length; i++) {
        if (categories[i].getType() == Category.TYPE_USER) {
          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
          itemCategory.setText(categories[i].getName());
          itemCategory.setData("Category", categories[i]);

          itemCategory.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              MenuItem item = (MenuItem)event.widget;
              assignSelectedToCategory((Category)item.getData("Category"));
            }
          });
        }
      }

      new MenuItem(menuCategory, SWT.SEPARATOR);
    }

    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
    Messages.setLanguageText(itemAddCategory,
                             "MyTorrentsView.menu.setCategory.add");

    itemAddCategory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        addCategory();
      }
    });

  }

  public void 
  categoryAdded(Category category) 
  {
  	MainWindow.getWindow().getDisplay().asyncExec(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				addCategorySubMenu();
	  			}
			});
  }

  public void 
  categoryRemoved(
  	Category category) 
  {
  	MainWindow.getWindow().getDisplay().asyncExec(
  		new AERunnable() 
		{
  			public void 
			runSupport() 
  			{
  				addCategorySubMenu();
  			}
		});
  }

  
  private void addCategory() {
    CategoryAdderWindow adderWindow = new CategoryAdderWindow(MainWindow.getWindow().getDisplay());
    Category newCategory = adderWindow.getNewCategory();
    if (newCategory != null)
      assignSelectedToCategory(newCategory);
  }
  
  private void assignSelectedToCategory(final Category category) {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
      	
	    TRHostTorrent	tr_torrent = (TRHostTorrent)row.getDataSource(true);
		
		TOTorrent	torrent = tr_torrent.getTorrent();
		
		DownloadManager dm = azureus_core.getGlobalManager().getDownloadManager( torrent );

		if ( dm != null ){
			
			dm.getDownloadState().setCategory( category );
			
		}else{
			
	     	String cat_str;
	      	
	      	if ( category == null ){
	      		
				cat_str = null;
	      		
	      	}else if ( category == CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED)){
	      		
				cat_str = null;
	      		
	      	}else{
	      		
				cat_str = category.getName();
	      	}
				// bit of a hack-alert here
			
			TorrentUtils.setPluginStringProperty( torrent, "azcoreplugins.category", cat_str );
			
			try{
				TorrentUtils.writeToFile( torrent );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
      }
    });
  }
}
