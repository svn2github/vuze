/*
 * Created on 21.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.core3.internat.MessageText;

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
  public static void updateLanguageForControl(Widget composite) {
    if (composite == null || composite.isDisposed())
      return;

    updateLanguageFromData(composite);

    if (composite instanceof CTabFolder) {
      CTabFolder folder = (CTabFolder) composite;
      CTabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    } else if (composite instanceof TabFolder) {
      TabFolder folder = (TabFolder) composite;
      TabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    }
    else if (composite instanceof Composite) {
      Composite group = (Composite) composite;
      Control[] controls = group.getChildren();
      for (int i = 0; i < controls.length; i++) {
        updateLanguageForControl(controls[i]);
      }
      if (composite instanceof Table) {
        Table table = (Table) composite;
        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
          updateLanguageFromData(columns[i]);
        }
        updateLanguageForControl(table.getMenu());
      }
      group.layout();
    }
    else if (composite instanceof MenuItem) {
      MenuItem menuItem = (MenuItem) composite;
      updateLanguageForControl(menuItem.getMenu());
    }
    else if (composite instanceof Menu) {
      Menu menu = (Menu) composite;
      if (menu.getStyle() == SWT.POP_UP)
        System.out.println("POP_UP");

      MenuItem[] items = menu.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
      }
    }
  }

  public static void setLanguageText(Widget widget, String key) {
    widget.setData(key);
    updateLanguageFromData(widget);
  }

  public static void updateLanguageFromData(Widget widget) {
    if (widget.getData() != null) {
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
      else if(widget instanceof Shell) 
        ((Shell) widget).setText(MessageText.getString((String) widget.getData()));
      else
        System.out.println("No cast for " + widget.getClass().getName());
    }
  }

}
