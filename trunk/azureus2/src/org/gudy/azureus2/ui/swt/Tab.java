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

/**
 * @author Olivier
 * 
 */
public class Tab {

  private static HashMap tabs;
  private static CTabFolder _folder;

  private CTabFolder folder;
  private CTabItem tabItem;
  private Composite composite;
//  private CLabel title;
  private IView view;

  static {
    tabs = new HashMap();
  }

  public CTabItem getTabItem() {
		return tabItem;
  }

  public Tab(IView _view) {
    this.view = _view;
    this.folder = _folder;
    tabItem = new CTabItem(folder, SWT.NULL);
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
      folder.setSelection(tabItem);
      tabs.put(tabItem, view);
    } catch (Exception e) {
    }
  }

  public static IView getView(CTabItem item) {
    return (IView) tabs.get(item);
  }

  public static void refresh() {
    synchronized (tabs) {
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        CTabItem item = (CTabItem) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          if (item.isDisposed())
            continue;
          item.setText(view.getShortTitle());
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
			CTabItem[] tab_items =
				(CTabItem[]) tabs.keySet().toArray(new CTabItem[tabs.size()]);
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

  public static void setFolder(CTabFolder folder) {
    _folder = folder;
  }

  public static synchronized void closed(CTabItem item) {
    IView view = null;
    synchronized (tabs) {
      view = (IView) tabs.get(item);
      tabs.remove(item);
    }
    if (view != null) {
      try {
        view.delete();
      } catch (Exception ignore) {
      }
    }
  }

  public void setFocus() {
    if (folder != null && !folder.isDisposed()) {
      folder.setSelection(tabItem);
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
  }
}
