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
import org.eclipse.swt.widgets.Item;
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

  private boolean useCustomTab;
  //private static TabFolder _folder;
  //private static CTabFolder _folder;
  private static Composite _folder;

  //private TabFolder folder;
  //private CTabFolder folder;
  private Composite folder;

  //private TabItem tabItem;
  //private CTabItem tabItem;
  private Item tabItem;

  private String lastTitle;
  private String lastTooltip;

  private Composite composite;
  //private CLabel title;
  private IView view;

  static {
    tabs = new HashMap();
  }

  //public TabItem getTabItem() {
  //public CTabItem getTabItem() {
  public Item getTabItem() {
    return tabItem;
  }

  public Tab(IView _view) {
    this.useCustomTab = MainWindow.getWindow().isUseCustomTab();
    this.view = _view;
    this.folder = _folder;
    if (_view instanceof MyTorrentsView) {
      if (useCustomTab) {
        tabItem = new CTabItem((CTabFolder) folder, SWT.NULL, 0);
      }
      else {
        tabItem = new TabItem((TabFolder) folder, SWT.NULL, 0);
      }

    }
    else {
      if (useCustomTab) {
        tabItem = new CTabItem((CTabFolder) folder, SWT.NULL);
      }
      else {
        tabItem = new TabItem((TabFolder) folder, SWT.NULL);
      }
    }
    if (!(_view instanceof MyTorrentsView || _view instanceof MyTrackerView)) {
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
        if (useCustomTab) {
          ((CTabItem) tabItem).setControl(composite);
          ((CTabFolder) folder).setSelection((CTabItem) tabItem);
        }
        else {
          ((TabItem) tabItem).setControl(composite);
          TabItem items[] = {(TabItem) tabItem };
          ((TabFolder) folder).setSelection(items);
        }
        tabItem.setText(view.getShortTitle());
        tabs.put(tabItem, view);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    else {
      try {
        _view.initialize(folder);
        tabItem.setText(view.getShortTitle());
        if (useCustomTab) {
          ((CTabItem) tabItem).setControl(_view.getComposite());
          ((CTabItem) tabItem).setToolTipText(view.getFullTitle());
          ((CTabFolder) folder).setSelection((CTabItem) tabItem);
        }
        else {
          ((TabItem) tabItem).setControl(_view.getComposite());
          ((TabItem) tabItem).setToolTipText(view.getFullTitle());
          TabItem items[] = {(TabItem) tabItem };
          ((TabFolder) folder).setSelection(items);
        }

        tabs.put(tabItem, view);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  //public static IView getView(TabItem item) {
  //public static IView getView(CTabItem item) {
  public static IView getView(Item item) {
    return (IView) tabs.get(item);
  }

  public static void refresh() {
    synchronized (tabs) {
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        //TabItem item = (TabItem) iter.next();
        //CTabItem item = (CTabItem) iter.next();
        Item item = (Item) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          if (item.isDisposed())
            continue;
          String lastTitle = item.getText();
          String newTitle = view.getShortTitle();
          if (lastTitle == null || !lastTitle.equals(newTitle)) {
            item.setText(newTitle);
          }
          if (item instanceof CTabItem) {
            String lastToolTip = ((CTabItem) item).getToolTipText();
            String newToolTip = view.getFullTitle();
            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
              ((CTabItem) item).setToolTipText(newToolTip);
            }
          }
          else if (item instanceof TabItem) {
            String lastToolTip = ((CTabItem) item).getToolTipText();
            String newToolTip = view.getFullTitle() + " " +
						 MessageText.getString("Tab.closeHint");
            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
              ((CTabItem) item).setToolTipText(newToolTip);
            }
          }
        }
        catch (Exception e) {}
      }
    }
  }

  public static void updateLanguage() {
    synchronized (tabs) {
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        //TabItem item = (TabItem) iter.next();
        //CTabItem item = (CTabItem) iter.next();
        Item item = (CTabItem) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {}
      }
    }
  }

  public static void closeAllDetails() {
    synchronized (tabs) {
      //TabItem[] tab_items = (TabItem[]) tabs.keySet().toArray(new
			// TabItem[tabs.size()]);
      //CTabItem[] tab_items = (CTabItem[]) tabs.keySet().toArray(new CTabItem[tabs.size()]);
      Item[] tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);
      for (int i = 0; i < tab_items.length; i++) {
        IView view = (IView) tabs.get(tab_items[i]);
        if (view instanceof ManagerView) {
          try {
            view.delete();
          }
          catch (Exception e) {}
          try {
            tab_items[i].dispose();
          }
          catch (Exception e) {}
          tabs.remove(tab_items[i]);
        }
      }
    }
  }

  public static void closeCurrent() {
    if (_folder == null || _folder.isDisposed())
      return;
    if(_folder instanceof TabFolder) {    
      TabItem[] items =  ((TabFolder)_folder).getSelection();
      if(items.length == 1) {
        closed(items[0]);		
      }
     } else {
       closed(((CTabFolder)_folder).getSelection());
     }
  }

  //public static void setFolder(TabFolder folder) {
  //public static void setFolder(CTabFolder folder) {
  public static void setFolder(Composite folder) {
    _folder = folder;
  }

  //public static synchronized void closed(TabItem item) {
  public static synchronized void closed(Item item) {
    IView view = null;
    synchronized (tabs) {
      view = (IView) tabs.get(item);
      if (view != null && view instanceof MyTorrentsView) {
        MainWindow.getWindow().setMytorrents(null);
        item.dispose();
        return;
      }
      if (view != null && view instanceof MyTrackerView) {
        MainWindow.getWindow().setMyTracker(null);
        item.dispose();
        return;
      }
      try {
        Control control;
        if(item instanceof CTabItem) {
          control = ((CTabItem)item).getControl();
        } else {
          control = ((TabItem)item).getControl();
        }
        if (control != null && !control.isDisposed())
          control.dispose();
        item.dispose();
      }
      catch (Exception ignore) {
        //ignore.printStackTrace();
      }
      tabs.remove(item);
    }
    if (view != null) {
      try {
        view.delete();
      }
      catch (Exception ignore) {
        //ignore.printStackTrace();
      }
    }
  }

  public void setFocus() {
    if (folder != null && !folder.isDisposed()) {
      if(useCustomTab) {
        ((CTabFolder)folder).setSelection((CTabItem)tabItem);
      } else {
        TabItem items[] = {(TabItem)tabItem};
        ((TabFolder)folder).setSelection(items);    
      }      
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
    }
    catch (Exception e) {}
    if (composite != null && !composite.isDisposed()) {
      composite.dispose();
    }
  }
}
