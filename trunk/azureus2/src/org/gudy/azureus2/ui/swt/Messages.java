/*
 * Created on 21.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.ui.swt.components.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.config.IParameter;
import org.gudy.azureus2.ui.swt.views.TableView;


/**
 * @author Arbeiten
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Messages {

  /**
   * 
   */
  private Messages() {

    // TODO Auto-generated constructor stub
  }
  public static void updateLanguageForControl(Widget widget) {
    if (widget == null || widget.isDisposed())
      return;

    updateLanguageFromData(widget);
    updateToolTipFromData(widget);

    if (widget instanceof CTabFolder) {
      CTabFolder folder = (CTabFolder) widget;
      CTabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    } else if (widget instanceof TabFolder) {
      TabFolder folder = (TabFolder) widget;
      TabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    }
    else if(widget instanceof CoolBar) {
      CoolItem[] items = ((CoolBar)widget).getItems();
      for(int i = 0 ; i < items.length ; i++) {
        Control control = items[i].getControl();
        updateLanguageForControl(control);
      }
    }
    else if(widget instanceof ToolBar) {
      ToolItem[] items = ((ToolBar)widget).getItems();
      for(int i = 0 ; i < items.length ; i++) {
        updateLanguageForControl(items[i]);
      }
    }
    else if (widget instanceof Composite) {
      Composite group = (Composite) widget;
      Control[] controls = group.getChildren();
      for (int i = 0; i < controls.length; i++) {
        updateLanguageForControl(controls[i]);
      }
      if (widget instanceof Table) {
        Table table = (Table) widget;
        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
          updateLanguageFromData(columns[i]);
        }
        updateLanguageForControl(table.getMenu());

        TableView tv = (TableView)widget.getData("TableView");
        if (tv != null)
          tv.tableInvalidate();
      }
      else if (widget instanceof Tree) {
        Tree tree = (Tree) widget;
        TreeItem[] treeitems = tree.getItems();
        for (int i = 0; i < treeitems.length; i++) {
          updateLanguageForControl(treeitems[i]);
        }
      }
        
      group.layout();
    }
    else if (widget instanceof MenuItem) {
      MenuItem menuItem = (MenuItem) widget;
      updateLanguageForControl(menuItem.getMenu());
    }
    else if (widget instanceof Menu) {
      Menu menu = (Menu) widget;
      if (menu.getStyle() == SWT.POP_UP)
        System.out.println("POP_UP");

      MenuItem[] items = menu.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
      }
    }
    else if (widget instanceof TreeItem) {
      TreeItem treeitem = (TreeItem) widget;
      TreeItem[] treeitems = treeitem.getItems();
      for (int i = 0; i < treeitems.length; i++) {
        updateLanguageFromData(treeitems[i]);
      }
    }
    
  }

  public static void setLanguageText(IParameter paramObject, String key) {
    setLanguageText(paramObject.getControl(), key, false);
  }

  public static void setLanguageText(Widget widget, String key) {
    setLanguageText(widget, key, false);
  }

  public static void setLanguageText(Widget widget, String key, boolean setTooltipOnly) {
  	widget.setData(key);
  	if(!setTooltipOnly)
      updateLanguageFromData(widget);
  	updateToolTipFromData(widget);
  }
  
  public static void setLanguageText(BufferedWidget buffered_widget, String key) {
    setLanguageText(buffered_widget.getWidget(), key, false);
  }

  public static void setLanguageText(BufferedWidget buffered_widget, String key, boolean setTooltipOnly) {
    setLanguageText(buffered_widget.getWidget(), key, setTooltipOnly);
  }
  
  private static void updateToolTipFromData(Widget widget) {
    if(widget instanceof Control) {
      String key = (String) widget.getData();
      if(key != null) {
        if(!key.endsWith(".tooltip"))
          key += ".tooltip";
        String toolTip = MessageText.getString(key);
        if(!toolTip.equals('!' + key + '!')) {
          ((Control)widget).setToolTipText(toolTip);
        }
      }
    } else if(widget instanceof ToolItem) {
      String key = (String) widget.getData();
      if(key != null) {
        if(!key.endsWith(".tooltip"))
          key += ".tooltip";
        String toolTip = MessageText.getString(key);
        if(!toolTip.equals('!' + key + '!')) {
          ((ToolItem)widget).setToolTipText(toolTip);
        }
      }
    }
  }
  

  public static void updateLanguageFromData(Widget widget) {
      if (widget.getData() != null) {
        String key = (String) widget.getData();
        if(key.endsWith(".tooltip"))
          return;
        if (widget instanceof MenuItem)
           ((MenuItem) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof TableColumn)
           ((TableColumn) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof Label)
           ((Label) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof Group)
           ((Group) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof Button)
           ((Button) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof CTabItem)
           ((CTabItem) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof TabItem)
           ((TabItem) widget).setText(MessageText.getString((String) widget.getData()));
        else if (widget instanceof TreeItem)
          ((TreeItem) widget).setText(MessageText.getString((String) widget.getData()));
        else if(widget instanceof Shell) 
          ((Shell) widget).setText(MessageText.getString((String) widget.getData()));
        else
          System.out.println("No cast for " + widget.getClass().getName());
      } 
  }

  public static void setLanguageTooltip(Widget widget, String key) {
    widget.setData(key);
    updateTooltipLanguageFromData(widget);
  }

  public static void updateTooltipLanguageFromData(Widget widget) {
    if (widget.getData() != null) {
      if (widget instanceof CLabel)
        ((CLabel) widget).setToolTipText(MessageText.getString((String) widget.getData()));
      else
        System.out.println("No cast for " + widget.getClass().getName());
    }
  }
}
