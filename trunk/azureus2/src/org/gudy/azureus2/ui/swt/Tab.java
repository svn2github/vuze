/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.views.*;

/**
 * @author Olivier
 * 
 */
public class Tab {

  private static HashMap tabs;
  private static TabFolder _folder;
  //private static CTabFolder _folder;

  private TabFolder folder;
  private TabItem tabItem;
  
  private String lastTitle;
  private String lastTooltip;
  //private CTabFolder folder;
  //private CTabItem tabItem;
  private Composite composite;
  //private CLabel title;
  private IView view;

  static {
    tabs = new HashMap();
  }

  public TabItem getTabItem() {
  //public CTabItem getTabItem() {
		return tabItem;
  }

  public Tab(IView _view) {
    this.view = _view;
    this.folder = _folder;
    if(_view instanceof MyTorrentsView) {
      tabItem = new TabItem(folder, SWT.NULL, 0);
      //tabItem = new CTabItem(folder, SWT.NULL, 0);
    } else {
      tabItem = new TabItem(folder, SWT.NULL);
      //tabItem = new CTabItem(folder, SWT.NULL);
    }
    if(!( _view instanceof MyTorrentsView)) {
      composite = new Composite(folder, SWT.NULL);
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.horizontalSpacing = 0;
      layout.verticalSpacing = 0;
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      composite.setLayout(layout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      try {
        _view.initialize(composite);
        _view.getComposite().setLayoutData(gridData);
        tabItem.setControl(composite);
        tabItem.setText(view.getShortTitle());
        TabItem items[] = {tabItem};
        folder.setSelection(items);
        //folder.setSelection(tabItem);
        tabs.put(tabItem, view);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      try {
        _view.initialize(folder);              
        tabItem.setControl(_view.getComposite());
        tabItem.setText(view.getShortTitle());
        tabItem.setToolTipText(view.getFullTitle());
        TabItem items[] = {tabItem};
        folder.setSelection(items);
        //folder.setSelection(tabItem);
        tabs.put(tabItem, view);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static IView getView(TabItem item) {
  //public static IView getView(CTabItem item) {
    return (IView) tabs.get(item);
  }

  public static void refresh() {
    synchronized (tabs) {
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        TabItem item = (TabItem) iter.next();
        //CTabItem item = (CTabItem) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          if (item.isDisposed())
            continue;
          String lastTitle = item.getText();
          String newTitle = view.getShortTitle();
          if(lastTitle == null || !lastTitle.equals(newTitle)) {
            item.setText(newTitle);            
          }
          String lastToolTip = item.getToolTipText();
          String newToolTip = view.getFullTitle() + " " + MessageText.getString("Tab.closeHint");
          if(lastToolTip == null || !lastToolTip.equals(newToolTip)) {
            item.setToolTipText(newToolTip);          
          }
        } catch (Exception e) {
        }
      }
    }
  }

  public static void updateLanguage() {
    synchronized (tabs) {
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        CTabItem item = (CTabItem) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          view.updateLanguage();
          view.refresh();
        } catch (Exception e) {
        }
      }
    }
  }

  public static void closeAllDetails() {
		synchronized (tabs) {
		  TabItem[] tab_items = (TabItem[]) tabs.keySet().toArray(new TabItem[tabs.size()]); 
			//CTabItem[] tab_items = (CTabItem[]) tabs.keySet().toArray(new CTabItem[tabs.size()]);
			for (int i = 0; i < tab_items.length; i++) {
        IView view = (IView) tabs.get(tab_items[i]);
        if(view instanceof ManagerView) {
          try {
            view.delete();
          } catch (Exception e) {
          }
          try {
            tab_items[i].dispose();
          } catch (Exception e) {
          }
          tabs.remove(tab_items[i]);
        }
			}
		}
	}
  
  public static void closeCurrent() {
    if(_folder == null || _folder.isDisposed())
      return;
    TabItem[] items = _folder.getSelection();
    if(items.length == 1) {
      closed(items[0]);
    }
  }

  public static void setFolder(TabFolder folder) {
  //public static void setFolder(CTabFolder folder) {
    _folder = folder;
  }

  public static synchronized void closed(TabItem item) {
  //public static synchronized void closed(CTabItem item) {
    IView view = null;
    synchronized (tabs) {
      view = (IView) tabs.get(item);
      if(view != null && view instanceof MyTorrentsView) {
        MainWindow.getWindow().setMytorrents(null);
        item.dispose();
        return;
      }
	  if(view != null && view instanceof MyTrackerView) {
		MainWindow.getWindow().setMyTracker(null);
		item.dispose();
		return;
	  }
	  try {		  
      Control control = item.getControl();
      if(control != null && ! control.isDisposed())
        control.dispose();
      item.dispose();
	  } catch (Exception ignore) {
      //ignore.printStackTrace();
	  }
      tabs.remove(item);
    }
    if (view != null) {
      try {
        view.delete();
      } catch (Exception ignore) {
        //ignore.printStackTrace();
      }
    }
  }

  public void setFocus() {
    if (folder != null && !folder.isDisposed()) {
      TabItem items[] = {tabItem};
      folder.setSelection(items);
      //folder.setSelection(tabItem);
    }
  }

  public void dispose() {
    IView view = null;
    synchronized (tabs) {
      view = (IView) tabs.get(tabItem);
      tabs.remove(tabItem);
    }
    try {
      if (view != null)
        view.delete();
      tabItem.dispose();
    } catch (Exception e) {
    }
    if(composite != null && ! composite.isDisposed()) {
      composite.dispose();
    }
  }
}
