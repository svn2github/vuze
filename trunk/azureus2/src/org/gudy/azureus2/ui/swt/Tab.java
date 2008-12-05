/*
 * Created on 29 juin 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *  
 */
package org.gudy.azureus2.ui.swt;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;

import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.PluginView;

/**
 * @author Olivier
 * @author James Yeh Added Add/Remove event listeners
 */
public class Tab implements ParameterListener, UIUpdatable {
	private static final LogIDs LOGID = LogIDs.GUI;
	
	private static final String ID = "TabSet";

  private HashMap 	tabs;
  private AEMonitor  class_mon 	= new AEMonitor(ID);

  private boolean useCustomTab;

  private Composite folder;


  private boolean eventCloseAllowed = true;
  private Item selectedItem = null;


	private MainWindow mainwindow;

	private Listener activateListener;

  /**
	 * 
	 */
	public Tab(MainWindow _mainWindow) {
		mainwindow = _mainWindow;

		tabs = new HashMap();
		
		COConfigurationManager.addParameterListener("GUI_SWT_bFancyTab", this);

    activateListener = new Listener() {
			public void handleEvent(Event event) {
				IView view = null;
				Composite parent = (Composite) event.widget;
				IView oldView = getView(selectedItem);
				if (oldView instanceof IViewExtension) {
					((IViewExtension) oldView).viewDeactivated();
				}

				while (parent != null && !parent.isDisposed() && view == null) {
					if (parent instanceof CTabFolder) {
						CTabFolder folder = (CTabFolder) parent;
						selectedItem = folder.getSelection();
						view = getView(selectedItem);
					} else if (parent instanceof TabFolder) {
						TabFolder folder = (TabFolder) parent;
						TabItem[] selection = folder.getSelection();
						if (selection.length > 0) {
							selectedItem = selection[0];
							view = getView(selectedItem);
						}
					}

					if (view == null)
						parent = parent.getParent();
				}

				if (view != null) {
					if (view instanceof IViewExtension) {
						((IViewExtension) view).viewActivated();
					}
					view.refresh();
				}
			}
		};
		
		mainwindow.getUIFunctions().getUIUpdater().addUpdater(this);
	}

	public Composite createFolderWidget(Composite parent) {
		Display display = parent.getDisplay();

		if (tabs == null) {
			tabs = new HashMap();
		}
		if (folder != null && !folder.isDisposed()) {
			return folder;
		}

		useCustomTab = COConfigurationManager.getBooleanParameter("useCustomTab");

		if (!useCustomTab) {
			folder = new TabFolder(parent, SWT.V_SCROLL);
		} else {
			folder = new CTabFolder(parent, SWT.CLOSE | SWT.BORDER);

			float[] hsb = folder.getBackground().getRGB().getHSB();
			hsb[2] *= (Constants.isOSX) ? 0.9 : 0.97;
			folder.setBackground(ColorCache.getColor(parent.getDisplay(), hsb));

			hsb = folder.getForeground().getRGB().getHSB();
			hsb[2] *= (Constants.isOSX) ? 1.1 : 0.03;
			folder.setForeground(ColorCache.getColor(parent.getDisplay(), hsb));

			//((CTabFolder)folder).setBorderVisible(false);

			((CTabFolder) folder).addCTabFolder2Listener(new CTabFolder2Adapter() {
				public void close(CTabFolderEvent event) {
					if (!closed((Item) event.item)) {
						event.doit = false;
					}
				}
			});

			// I think this closes the tab on middle click
			folder.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent arg0) {
					CTabItem tabItem = ((CTabFolder) folder).getItem(new Point(arg0.x,
							arg0.y));
					if (arg0.button == 2) {
						if (eventCloseAllowed) {
							Rectangle rectangle = ((CTabItem) tabItem).getBounds();
							if (rectangle.contains(arg0.x, arg0.y)) {
								eventCloseAllowed = false;
								selectedItem = null;
								//folder.removeMouseListener(this);
								closed(tabItem);
							}
						}
					} else {
						selectedItem = ((CTabFolder) folder).getSelection();
					}
				}

				public void mouseUp(MouseEvent arg0) {
					eventCloseAllowed = true;
					if (selectedItem != null) {
						((CTabFolder) folder).setSelection((CTabItem) selectedItem);
						ensureVisibilities();
					}
				}
			});
		}

		folder.getDisplay().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				// Another window has control, skip filter
				Control focus_control = folder.getDisplay().getFocusControl();
				if (focus_control != null
						&& focus_control.getShell() != folder.getShell()) {
					return;
				}

				int key = event.character;
				if ((event.stateMask & SWT.MOD1) != 0 && event.character <= 26
						&& event.character > 0)
					key += 'a' - 1;

				// ESC or CTRL+F4 closes current Tab
				if (key == SWT.ESC
						|| (event.keyCode == SWT.F4 && event.stateMask == SWT.CTRL)) {
					closeCurrent();
					event.doit = false;
				} else if (event.keyCode == SWT.F6
						|| (event.character == SWT.TAB && (event.stateMask & SWT.CTRL) != 0)) {
					// F6 or Ctrl-Tab selects next Tab
					// On Windows the tab key will not reach this filter, as it is
					// processed by the traversal TRAVERSE_TAB_NEXT.  It's unknown
					// what other OSes do, so the code is here in case we get TAB
					if ((event.stateMask & SWT.SHIFT) == 0) {
						event.doit = false;
						selectNextTab(true);
						// Shift+F6 or Ctrl+Shift+Tab selects previous Tab
					} else if (event.stateMask == SWT.SHIFT) {
						selectNextTab(false);
						event.doit = false;
					}
				}
			}
		});

		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (folder == null || folder.isDisposed()) {
					return;
				}

				if (useCustomTab && !folder.isDisposed()) {
					ensureVisibilities();
				}

				mainwindow.getUIFunctions().refreshIconBar();
				mainwindow.getUIFunctions().refreshTorrentMenu();
			}
		};

		if (!useCustomTab) {
			((TabFolder) folder).addSelectionListener(selectionAdapter);
		} else {
			try {
				((CTabFolder) folder).setMinimumCharacters(75);
			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Can't set MIN_TAB_WIDTH", e));
			}
			//try {
			///  TabFolder2ListenerAdder.add((CTabFolder)folder);
			//} catch (NoClassDefFoundError e) {
			((CTabFolder) folder).addCTabFolderListener(new CTabFolderAdapter() {
				public void itemClosed(CTabFolderEvent event) {
					if (!event.doit) {
						return;
					}
					closed((CTabItem) event.item);
					event.doit = true;
					((CTabItem) event.item).dispose();
				}
			});
			//}

			((CTabFolder) folder).addSelectionListener(selectionAdapter);

			try {
				((CTabFolder) folder).setSelectionBackground(new Color[] {
					display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
					display.getSystemColor(SWT.COLOR_LIST_BACKGROUND),
					display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
				}, new int[] {
					10,
					90
				}, true);
			} catch (NoSuchMethodError e) {
				/** < SWT 3.0M8 **/
				((CTabFolder) folder).setSelectionBackground(new Color[] {
					display.getSystemColor(SWT.COLOR_LIST_BACKGROUND)
				}, new int[0]);
			}
			((CTabFolder) folder).setSelectionForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

			try {
				/* Pre 3.0M8 doesn't have Simple-mode (it's always simple mode)
				   in 3.0M9, it was called setSimpleTab(boolean)
				   in 3.0RC1, it's called setSimple(boolean)
				   Prepare for the future, and use setSimple()
				 */
				((CTabFolder) folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
			} catch (NoSuchMethodError e) {
				/** < SWT 3.0RC1 **/
			}
		}
		

		return folder;
	}

  

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	protected void ensureVisibilities() {
		if (!(folder instanceof CTabFolder)) {
			return;
		}
		CTabItem[] items = ((CTabFolder) folder).getItems();
		CTabItem item = ((CTabFolder) folder).getSelection();
		for (int i = 0; i < items.length; i++) {
			CTabItem tabItem = items[i];
			if (tabItem == null || tabItem.isDisposed()) {
				continue;
			}
			if (tabItem == item) {
				try {
					((CTabFolder) folder).setSelection(tabItem);
					Control control = getView(tabItem).getComposite();
					if (control != null) {
						control.setVisible(true);
						control.setFocus();
					}
					
				} catch (Throwable e) {
					Debug.printStackTrace(e);
					//Do nothing
				}
			} else {
				try {
					Control control = getView(tabItem).getComposite();
					if (control != null) {
						control.setVisible(false);
					}
				} catch (Throwable e) {
					Debug.printStackTrace(e);
					//Do nothing
				}
			}
		}
	}

	public Item createTabItem(final IView _view, boolean bFocus) {
		if (folder.isDisposed()) {
			return null;
		}

		Item tabItem;

		if (folder instanceof CTabFolder) {
			CTabFolder tabFolder = (CTabFolder) folder;
			tabItem = new CTabItem(tabFolder, SWT.NULL,
					(_view instanceof MyTorrentsSuperView) ? 0 : tabFolder.getItemCount());

		} else {
			TabFolder tabFolder = (TabFolder) folder;
			tabItem = new TabItem(tabFolder, SWT.NULL,
					(_view instanceof MyTorrentsSuperView) ? 0 : tabFolder.getItemCount());
		}


		tabs.put(tabItem, _view);

		try {
			// Always create a composite around the IView, because a lot of them
			// assume that their parent is of GridLayout layout.
			final Composite tabArea = new Composite(folder, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			tabArea.setLayout(layout);

			_view.initialize(tabArea);
			tabItem.setText(escapeAccelerators(_view.getShortTitle()));

			Composite viewComposite = _view.getComposite();
			if (viewComposite != null && !viewComposite.isDisposed()) {
				viewComposite.addListener(SWT.Activate, activateListener);

				// make sure the view's layout data is of GridLayoutData
				if ((tabArea.getLayout() instanceof GridLayout)
						&& !(viewComposite.getLayoutData() instanceof GridData)) {
					viewComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
				}

				if (viewComposite != tabArea) {
					viewComposite.addDisposeListener(new DisposeListener() {
						boolean alreadyHere = false;

						public void widgetDisposed(DisposeEvent e) {
							if (alreadyHere) {
								return;
							}
							alreadyHere = true;
							Item tab = getTab(_view);
							if (tab != null) {
								closed(tab);
							}
						}
					});
				}
			}

			if (folder instanceof CTabFolder) {
				((CTabItem) tabItem).setControl(tabArea);
				// Disabled for SWT 3.2RC5.. CTabItem tooltip doesn't always disappear
				//				((CTabItem) tabItem).setToolTipText(view.getFullTitle());
				if (bFocus) {
					((CTabFolder) folder).setSelection((CTabItem) tabItem);
					ensureVisibilities();
				}
			} else {
				((TabItem) tabItem).setControl(tabArea);
				((TabItem) tabItem).setToolTipText(_view.getFullTitle());
				TabItem items[] = {
					(TabItem) tabItem
				};
				if (bFocus) {
					((TabFolder) folder).setSelection(items);
				}
			}
		} catch (Exception e) {
			tabs.remove(tabItem);
			Debug.printStackTrace(e);
		}

		if (bFocus) {
			UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uif != null) {
				uif.refreshIconBar();
				uif.refreshTorrentMenu();
			}
			selectedItem = tabItem;
		}
		
		return tabItem;
	}


  public IView getView(Item item) {
    return (IView) tabs.get(item);
  }
  
  public Item
  getTab(
		IView	view )
  {
	   try{
		   class_mon.enter();
	    	
		   Iterator iter = tabs.keySet().iterator();
		   
		   while( iter.hasNext()){
			 
			   Item item = (Item) iter.next();
			   
			   IView this_view = (IView) tabs.get(item); 
			   
			   if ( this_view == view ){
				   
				   return( item );
			   }
		   }
		   
		   return( null );
		   
	   }finally{
		   
		   class_mon.exit();
	   }
  }
  
  public Item[] getAllTabs() {
		try {
			class_mon.enter();

			Item[] tabItems = new Item[tabs.size()];
			if (tabItems.length > 0) {
				tabItems = (Item[]) tabs.keySet().toArray(tabItems);
			}

			return tabItems;
		} finally {

			class_mon.exit();
		}
	}
  
  public IView[] getAllViews() {
		try {
			class_mon.enter();

			IView[] views = new IView[tabs.size()];
			if (views.length > 0) {
				views = (IView[])tabs.values().toArray(views);
			}
			
			return views;
		} finally {

			class_mon.exit();
		}
	}

  
  public void refresh() {
    try{
    	class_mon.enter();
    	
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
            item.setText(escapeAccelerators(newTitle));
          }
          if (item instanceof CTabItem) {
// Disabled for SWT 3.2RC5.. CTabItem tooltip doesn't always disappear
//            String lastToolTip = ((CTabItem) item).getToolTipText();
//            String newToolTip = view.getFullTitle();
//            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
//              ((CTabItem) item).setToolTipText(newToolTip);
//            }
          }
          else if (item instanceof TabItem) {
            String lastToolTip = ((TabItem) item).getToolTipText();
            String newToolTip = view.getFullTitle();
            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
              ((TabItem) item).setToolTipText(newToolTip);
            }
          }
        }
        catch (Exception e){
        	
        	Debug.printStackTrace(e);
        }
      }
    }finally{
    	
    	class_mon.exit();
    }
  }

  public void 
  updateLanguage() 
  {
  	IView[] views;
  	
    try{
    	class_mon.enter();
      
    	views = (IView[]) tabs.values().toArray(new IView[tabs.size()]);
    	   	
    }finally{
    	
    	class_mon.exit();
    }   
    
    for (int i = 0; i < views.length; i++) {

    	IView view = views[i];
    	
        try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {
        	Debug.printStackTrace(e);
        }
    }
  }


  public void 
  closeAllTabs() 
  {
  	Item[] tab_items;
  	
    try{
    	class_mon.enter();
      
    	tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);
    	
    }finally{
    	
    	class_mon.exit();
    }
    
    for (int i = 0; i < tab_items.length; i++) {
    	
        closed(tab_items[i], true);
      }
  }

  public boolean hasDetails()
  {
      boolean hasDetails = false;
      try
      {
          class_mon.enter();

          Iterator iter = tabs.values().iterator();
          while (iter.hasNext())
          {
              IView view = (IView) iter.next();
              if(view instanceof ManagerView)
              {
                  hasDetails = true;
                  break;
              }
          }
      }
      finally
      {
          class_mon.exit();
      }

      return hasDetails;
  }

  public void
  closeAllDetails() 
  {
  	Item[] tab_items;
  	
    try{
    	class_mon.enter();
    	
    	tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);

    }finally{
    	
    	class_mon.exit();
    }
    
    for (int i = 0; i < tab_items.length; i++) {
        IView view = (IView) tabs.get(tab_items[i]);
        if (view instanceof ManagerView) {
          closed(tab_items[i]);
        }
      }
  }

  public void closeCurrent() {
    if (folder == null || folder.isDisposed())
      return;
    if(folder instanceof TabFolder) {    
      TabItem[] items =  ((TabFolder)folder).getSelection();
      if(items.length == 1) {
        closed(items[0]);		
      }
     } else {
       closed(((CTabFolder)folder).getSelection());
     }
  }

  /**
   * @param selectNext if true, the next tab is selected, else the previous
   *
   * @author Rene Leonhardt
   */
  public void selectNextTab(boolean selectNext) {
    if (folder == null || folder.isDisposed())
      return;
    final int nextOrPrevious = selectNext ? 1 : -1;
    if(folder instanceof TabFolder) {
      TabFolder tabFolder = (TabFolder)folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
    } else {
      CTabFolder tabFolder = (CTabFolder)folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
			ensureVisibilities();
    }
  }

  public boolean 
  closed(Item item) 
  {
  	return closed(item, false);
  }
  
  public boolean 
  closed(Item item, boolean bForceClose) 
  {
  	if (item == null) {
  		return true;
  	}

    IView view = (IView) tabs.get(item);
    if (!bForceClose && view instanceof UISWTViewImpl) {
    	if (!((UISWTViewImpl)view).requestClose()) {
    		return false;
    	}
    }

    try{
    	class_mon.enter();
    	
    	view = (IView) tabs.remove(item);
    }finally{
    	
    	class_mon.exit();
    }
    
    if (view != null) {
        try {
          if(view instanceof PluginView) {
          	mainwindow.removeActivePluginView(((PluginView)view).getPluginViewName());
          }
          if(view instanceof UISWTPluginView) {
          	mainwindow.removeActivePluginView(((UISWTPluginView)view).getPluginViewName());
          }
          if(view instanceof UISWTView)
          	mainwindow.removeActivePluginView(((UISWTView)view).getViewID());
   
          view.delete();
        } catch (Exception e) {
        	Debug.printStackTrace( e );
        }

        if (view instanceof MyTorrentsSuperView) {
          //TODO : There is a problem here on OSX when using Normal TABS
          /*  org.eclipse.swt.SWTException: Widget is disposed
                at org.eclipse.swt.SWT.error(SWT.java:2691)
                at org.eclipse.swt.SWT.error(SWT.java:2616)
                at org.eclipse.swt.SWT.error(SWT.java:2587)
                at org.eclipse.swt.widgets.Widget.error(Widget.java:546)
                at org.eclipse.swt.widgets.Widget.checkWidget(Widget.java:296)
                at org.eclipse.swt.widgets.Control.setVisible(Control.java:2573)
                at org.eclipse.swt.widgets.TabItem.releaseChild(TabItem.java:180)
                at org.eclipse.swt.widgets.Widget.dispose(Widget.java:480)
                at org.gudy.azureus2.ui.swt.Tab.closed(Tab.java:322)
           */
          //Tried to add a if(! item.isDisposed()) but it's not fixing it
          //Need to investigate...
          item.dispose();
          return true;
        }
        if (view instanceof MyTrackerView) {
          item.dispose();
          return true;
        }
        if (view instanceof MySharesView) {
        	item.dispose();
          return true;
        }
      }
      try {
        /*Control control;
        if(item instanceof CTabItem) {
          control = ((CTabItem)item).getControl();
        } else {
          control = ((TabItem)item).getControl();
        }
        if (control != null && !control.isDisposed())
          control.dispose();
        */
        item.dispose();
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
      return true;
  }

  public void setFocus(Item item) {
  	if (item == null || item.isDisposed()) {
  		return;
  	}

		if (folder != null && !folder.isDisposed()) {
			if (useCustomTab) {
				((CTabFolder) folder).setSelection((CTabItem) item);
				ensureVisibilities();
			} else {
				TabItem items[] = {
					(TabItem) item
				};
				((TabFolder) folder).setSelection(items);
			}
		}
	}

  public void dispose(Item tabItem) {
    IView localView = null;
    try{
    	class_mon.enter();
      
      localView = (IView) tabs.get(tabItem);

      if (localView instanceof UISWTViewImpl) {
				if (!((UISWTViewImpl) localView).requestClose())
					return;
			}

      tabs.remove(tabItem);
    }finally{
    
    	class_mon.exit();
    }
    try {
      if (localView != null) {
        if(localView instanceof PluginView) {
        	mainwindow.removeActivePluginView(((PluginView)localView).getPluginViewName());
        }
        if(localView instanceof UISWTPluginView) {
        	mainwindow.removeActivePluginView(((UISWTPluginView)localView).getPluginViewName());
        }

        localView.delete();
      }
      tabItem.dispose();
    }
    catch (Exception e) {}
  }


  protected String
  escapeAccelerators(
	 String	str )
  {
	  if ( str == null ){
		  
		  return( str );
	  }
	  
	  return( str.replaceAll( "&", "&&" ));
  }
  
  public void generateDiagnostics(IndentWriter writer) {
		Object[] views = tabs.values().toArray();
		for (int i = 0; i < views.length; i++) {
			IView view = (IView) views[i];

			if (view != null) {
				writer.println(view.getFullTitle());

				try {
					writer.indent();

					view.generateDiagnostics(writer);
				} catch (Exception e) {

				} finally {

					writer.exdent();
				}
			}
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void update() {
		if (folder != null) {
			if (useCustomTab) {
				((CTabFolder) folder).update();
			} else {
				((TabFolder) folder).update();
			}
		}
	}

	/**
	 * @param viewID
	 *
	 * @since 3.1.1.1
	 */
	public void closePluginViews(String viewID) {
		Item[] items;

		if (folder instanceof CTabFolder)
			items = ((CTabFolder) folder).getItems();
		else if (folder instanceof TabFolder)
			items = ((TabFolder) folder).getItems();
		else
			return;

		for (int i = 0; i < items.length; i++) {
			IView view = getView(items[i]);
			if (view instanceof UISWTViewImpl) {
				String sID = ((UISWTViewImpl) view).getViewID();
				if (sID != null && sID.equals(viewID)) {
					try {
						Item tab = getTab(view);
						if (tab != null) {
							closed(tab);
						}
					} catch (Exception e) {
						Debug.printStackTrace(e);
					}
				}
			}
		} // for
	}

	// @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	public void parameterChanged(String parameterName) {
		if (parameterName.equals("GUI_SWT_bFancyTab")
				&& folder instanceof CTabFolder && folder != null
				&& !folder.isDisposed()) {
			try {
				((CTabFolder) folder).setSimple(!COConfigurationManager.getBooleanParameter("GUI_SWT_bFancyTab"));
			} catch (NoSuchMethodError e) {
				/** < SWT 3.0RC1 **/
			}
		}
	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public IView getCurrentView() {
		try {
			if (!useCustomTab) {
				TabItem[] selection = ((TabFolder) folder).getSelection();
				if (selection.length > 0) {
					return getView(selection[0]);
				}
				return null;
			}
			return getView(((CTabFolder) folder).getSelection());
		} catch (Exception e) {
			return null;
		}
	}
	
	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		IView currentView = getCurrentView();
		if (currentView != null) {
			return ID + "-" + currentView.getFullTitle();
		}
		return ID;
	}
	
	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (folder == null || folder.isDisposed()) {
			return;
		}

		IView currentView = getCurrentView();
		if (currentView != null) {
			try {
				currentView.refresh();
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		
		refresh();
	}
}
