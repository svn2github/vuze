/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views.stats;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IView;

/**
 * 
 */
public class StatsView extends AbstractIView {
  
  GlobalManager manager;
  
  TabFolder folder;
  
  TabItem itemActivity;
  TabItem itemStats;
  TabItem itemCache;
  TabItem itemDHT;
  TabItem itemDHTcvs;
  TabItem itemVivaldi;
  
  IView viewActivity;
  IView viewStats;
  IView viewCache;
  IView viewDHT;
  IView viewDHTcvs;
  IView viewVivaldi;
  UpdateThread updateThread;
  
  public StatsView(GlobalManager manager) {
   this.manager = manager;
  }
  
  private class UpdateThread extends Thread {
    boolean bContinue;
    
    public void run() {
      try {
        bContinue = true;
        while(bContinue) {   
          ((ActivityView)viewActivity).periodicUpdate();
          ((CacheView)viewCache).periodicUpdate(); 
          ((DHTView)viewDHT).periodicUpdate();
          
          if( viewDHTcvs != null ) {
            ((DHTView)viewDHTcvs).periodicUpdate();
          }
          
          Thread.sleep(1000);
        }
      } catch(Exception e) {
      	Debug.printStackTrace( e );  
      }
    }
    
    public void stopIt() {
      bContinue = false;
    }
  }
  
  public void initialize(Composite composite) {
    folder = new TabFolder(composite, SWT.LEFT);
    folder.setBackground(Colors.background);
    
    itemActivity = new TabItem(folder, SWT.NULL);
    itemStats = new TabItem(folder, SWT.NULL);
    itemCache  = new TabItem(folder, SWT.NULL);
    itemDHT  = new TabItem(folder, SWT.NULL);
    if( Constants.isCVSVersion() )  itemDHTcvs  = new TabItem(folder, SWT.NULL);
    itemVivaldi = new TabItem(folder,SWT.NULL);

    viewActivity = new ActivityView(manager);
    viewStats = new TransferStatsView(manager);
    viewCache = new CacheView();
    viewDHT = new DHTView( DHTView.DHT_TYPE_MAIN );  
    if( Constants.isCVSVersion() )  viewDHTcvs = new DHTView( DHTView.DHT_TYPE_CVS );
    viewVivaldi = new VivaldiView();
    
    Messages.setLanguageText(itemActivity, viewActivity.getData());
    Messages.setLanguageText(itemStats, viewStats.getData());
    Messages.setLanguageText(itemCache, viewCache.getData());
    Messages.setLanguageText(itemDHT, viewDHT.getData());
    
    if( viewDHTcvs != null ) {
      Messages.setLanguageText(itemDHTcvs, viewDHTcvs.getData());
    }
    
    Messages.setLanguageText(itemVivaldi, viewVivaldi.getData());
    
    TabItem items[] = {itemActivity};
    folder.setSelection(items);
    
    viewActivity.initialize(folder);
    itemActivity.setControl(viewActivity.getComposite());
    
    viewStats.initialize(folder);
    itemStats.setControl(viewStats.getComposite());
    
    viewCache.initialize(folder);
    itemCache.setControl(viewCache.getComposite());
    
    viewDHT.initialize(folder);
    itemDHT.setControl(viewDHT.getComposite());

    if( viewDHTcvs != null ) {
      viewDHTcvs.initialize(folder);
      itemDHTcvs.setControl(viewDHTcvs.getComposite());
    }
    
    viewVivaldi.initialize(folder);
    itemVivaldi.setControl(viewVivaldi.getComposite());
    
    folder.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        refresh();
      }
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    
    
    refresh();
    viewActivity.getComposite().layout(true);
    viewVivaldi.getComposite().layout(true);
    
    updateThread = new UpdateThread(); 
    updateThread.setDaemon(true);
    updateThread.start();
  }
  
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    try {
      int index = folder.getSelectionIndex();
      
      if( index == 0 ) {
        if (viewActivity != null && !itemActivity.isDisposed())   viewActivity.refresh();
        return;
      }
      
      if( index == 1 ) {
        if (viewStats != null && !itemStats.isDisposed())  viewStats.refresh();
        return;
      }
      
      if( index == 2 ) {
        if (viewCache != null && !itemCache.isDisposed())  viewCache.refresh();
        return;
      }
      
      if( index == 3 ) {
        if (viewDHT != null && !itemDHT.isDisposed())  viewDHT.refresh();
        return;
      }
      
      if( Constants.isCVSVersion() ) {
        if( index == 4 ) {
          if (viewDHTcvs != null && !itemDHTcvs.isDisposed())  viewDHTcvs.refresh();
          return;
        }
        
        if( index == 5 ) {
          if (viewVivaldi != null && !itemVivaldi.isDisposed())  viewVivaldi.refresh();
          return;
        }
      }
      
      if( index == 4 ) {
        if (viewVivaldi != null && !itemVivaldi.isDisposed())  viewVivaldi.refresh();
        return;
      }

    } catch (Exception e) {
    	Debug.printStackTrace( e );
    }
  }
  
  public Composite getComposite() {
    return folder;
  }

  public String getFullTitle() {
    return MessageText.getString("Stats.title.full"); //$NON-NLS-1$
  }
  
  public void delete() {
    updateThread.stopIt();
    MainWindow.getWindow().clearStats();    
    
    //Don't ask me why, but without this an exception is thrown further
    // (in folder.dispose() )
    //TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
    if(Constants.isOSX) {
      if(folder != null && !folder.isDisposed()) {
        TabItem[] items = folder.getItems();
        for(int i=0 ; i < items.length ; i++) {
          if (!items[i].isDisposed())
            items[i].dispose();
        }
      }
    }
    
    viewActivity.delete();
    viewStats.delete();
    viewCache.delete();
    viewDHT.delete();
    if( viewDHTcvs != null )  viewDHTcvs.delete();
    if(! folder.isDisposed()) {
      Utils.disposeComposite(folder);
    }
  }
}
