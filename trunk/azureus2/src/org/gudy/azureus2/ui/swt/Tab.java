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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.plugins.PluginView;

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
  private static boolean eventCloseAllowed = true;
  private static Item selectedItem = null;

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
    if (_view instanceof MyTorrentsSuperView) {
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
    
    if(useCustomTab) {
	    folder.addMouseListener(new MouseAdapter() {
	      public void mouseDown(MouseEvent arg0) {
	        if(arg0.button == 2) {
	          if(eventCloseAllowed) { 
	            Rectangle rectangle =((CTabItem)tabItem).getBounds(); 
	            if(rectangle.contains(arg0.x, arg0.y)) {
	              eventCloseAllowed = false;
	              folder.removeMouseListener(this);
	              closed(tabItem);
	            }
	          }
	        } else {          
	          selectedItem = (Item) ((CTabFolder) folder).getSelection();
	          //System.out.println("selected: "+selectedItem.getText());
	        }
	      }
	      
	      public void mouseUp(MouseEvent arg0) {
	        eventCloseAllowed = true;
	        if(selectedItem != null) {
	          if(_folder instanceof CTabFolder)
	            ((CTabFolder) _folder).setSelection((CTabItem)selectedItem);	          
	        }
	      }
	    });
    }

    if (!(_view instanceof MyTorrentsSuperView || _view instanceof MyTrackerView || _view instanceof MySharesView )) {
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
        _view.setTabListener();
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
        _view.setTabListener();
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
    MainWindow.getWindow().refreshIconBar();
    selectedItem = tabItem;
//    System.out.println("selected: "+selectedItem.getText());
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
            String lastToolTip = ((TabItem) item).getToolTipText();
            String newToolTip = view.getFullTitle() + " " +
						 MessageText.getString("Tab.closeHint");
            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
              ((TabItem) item).setToolTipText(newToolTip);
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
        Item item = (Item) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {}
      }
    }
  }

  public static void closeAllTabs() {
    synchronized (tabs) {
      Item[] tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);
      for (int i = 0; i < tab_items.length; i++) {
        closed(tab_items[i]);
      }
    }
  }

  public static void closeAllDetails() {
    synchronized (tabs) {
      Item[] tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);
      for (int i = 0; i < tab_items.length; i++) {
        IView view = (IView) tabs.get(tab_items[i]);
        if (view instanceof ManagerView) {
          closed(tab_items[i]);
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

  /**
   * @param selectNext if true, the next tab is selected, else the previous
   *
   * @author Rene Leonhardt
   */
  public static void selectNextTab(boolean selectNext) {
    if (_folder == null || _folder.isDisposed())
      return;
    final int nextOrPrevious = selectNext ? 1 : -1;
    if(_folder instanceof TabFolder) {
      TabFolder tabFolder = (TabFolder)_folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
    } else {
      CTabFolder tabFolder = (CTabFolder)_folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
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
      if (view != null) {
        try {
          if(view instanceof PluginView) {
            MainWindow.getWindow().removeActivePluginView((PluginView)view);
          }
          view.delete();
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (view instanceof MyTorrentsSuperView) {
          MainWindow.getWindow().setMytorrents(null);
          item.dispose();
          return;
        }
        if (view instanceof MyTrackerView) {
          MainWindow.getWindow().setMyTracker(null);
          item.dispose();
          return;
        }
        if (view instanceof MySharesView) {
        	MainWindow.getWindow().setMyShares(null);
        	item.dispose();
        	return;
        }
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
      if (view != null) {
        if(view instanceof PluginView) {
          MainWindow.getWindow().removeActivePluginView((PluginView)view);
        }
        view.delete();
      }
      tabItem.dispose();
    }
    catch (Exception e) {}
    if (composite != null && !composite.isDisposed()) {
      composite.dispose();
    }
  }

  /**
   * ESC or CTRL+F4 closes current tab, F6 selects next, CTRL+F6 selects previous
   * @return a KeyListener for Tabs
   *
   * @author Rene Leonhardt
   */
  public static KeyAdapter createTabKeyListener() {
    return new KeyAdapter() {
      public void keyReleased(KeyEvent keyEvent) {
        // ESC or CTRL+F4 closes current Tab
        if(keyEvent.character == SWT.ESC || (keyEvent.keyCode == 0x100000d && keyEvent.stateMask == SWT.CTRL)) {
          eventCloseAllowed = false;
          closeCurrent();
        }
        if(keyEvent.keyCode == 0x100000f) {
          // F6 selects next Tab
          if(keyEvent.stateMask == 0)
            selectNextTab(true);
          // Shift+F6 selects previous Tab
          else if(keyEvent.stateMask == SWT.SHIFT)
            selectNextTab(false);
        }
      }

      public void keyPressed(KeyEvent keyEvent) {
        eventCloseAllowed = true;
        // F6 selects next Tab
//        if(keyEvent.keyCode == 0x100000f && keyEvent.stateMask == 0) {
//          selectNextTab();
//        }
      }
    };
  }
  
  public static KeyAdapter defaultListener;

  public static synchronized void addTabKeyListenerToComposite(Composite folder) {
    if(folder == null)
      return;

    if(defaultListener == null)      
      defaultListener = createTabKeyListener();

    addTabKeyListenerToComposite(defaultListener,folder);    
  }
  
  public static void addTabKeyListenerToComposite(KeyListener listener,Composite folder) {    
    folder.removeKeyListener(listener);
    folder.addKeyListener(listener);
    Control[] children = folder.getChildren();
    for (int i = 0; i < children.length; i++) {
      if(children[i] instanceof Composite)
        addTabKeyListenerToComposite(listener,(Composite) children[i]);
      else {
				children[i].removeKeyListener(listener);
        children[i].addKeyListener(listener);
			}
    }
  }
  
}
