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

  IView viewActivity;
  IView viewStats;
  IView viewCache;
  
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

    viewActivity = new ActivityView(manager);
    viewStats = new TransferStatsView(manager);
    viewCache = new CacheView();
    
    Messages.setLanguageText(itemActivity, viewActivity.getData());
    Messages.setLanguageText(itemStats, viewStats.getData());
    Messages.setLanguageText(itemCache, viewCache.getData());
    
    TabItem items[] = {itemActivity};
    folder.setSelection(items);
    viewActivity.initialize(folder);
    itemActivity.setControl(viewActivity.getComposite());
    viewStats.initialize(folder);
    itemStats.setControl(viewStats.getComposite());
    viewCache.initialize(folder);
    itemCache.setControl(viewCache.getComposite());

    
    folder.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        refresh();
      }
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    
    
    refresh();
    viewActivity.getComposite().layout(true);
    
    updateThread = new UpdateThread(); 
    updateThread.setDaemon(true);
    updateThread.start();
  }
  
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    try {
      switch (folder.getSelectionIndex()) {
        case 0 :
          if (viewActivity != null && !itemActivity.isDisposed())
            viewActivity.refresh();
          break;
        case 1 :
        if (viewStats != null && !itemStats.isDisposed())
          viewStats.refresh();
          break;
        case 2 :
        if (viewCache != null && !itemCache.isDisposed())
          viewCache.refresh();
          break;        
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
    MainWindow.getWindow().setStats(null);    
    viewActivity.delete();
    viewStats.delete();
    viewCache.delete();
    if(! folder.isDisposed()) {
      Utils.disposeComposite(folder);
    }
  }
}
