/*
 * File    : IconBar.java
 * Created : 7 déc. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.swt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Olivier
 *
 */
public class IconBar {
  
  CoolBar coolBar;
  Composite parent;    
  Map itemKeyToControl;
  
  IconBarEnabler currentEnabler;
  
  public IconBar(Composite parent) {
    this.parent = parent;
    this.itemKeyToControl = new HashMap();
    new Label(parent,SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    this.coolBar = new CoolBar(parent,SWT.NONE);
    initBar();
    new Label(parent,SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));    
    this.coolBar.setLocked(true);
  }
  
  public void setEnabled(String itemKey,boolean enabled) {
    ToolItem toolItem = (ToolItem) itemKeyToControl.get(itemKey);
    if(toolItem != null)
      toolItem.setEnabled(enabled);
  }
  
  public void setSelection(String itemKey,boolean selection) {
    ToolItem toolItem = (ToolItem) itemKeyToControl.get(itemKey);
    if(toolItem != null)
      toolItem.setSelection(selection);
  }
  
  public void setCurrentEnabler(IconBarEnabler enabler) {
    this.currentEnabler = enabler;
    refreshEnableItems();
  }
  
  private void refreshEnableItems() {
    Iterator iter = itemKeyToControl.keySet().iterator();
    while(iter.hasNext()) {
      String key = (String) iter.next();
      ToolItem toolItem = (ToolItem) itemKeyToControl.get(key);
      if(toolItem == null || toolItem.isDisposed())
        continue;
      if(currentEnabler != null) {
        toolItem.setEnabled(currentEnabler.isEnabled(key));
        toolItem.setSelection(currentEnabler.isSelected(key));
      }
      else {        
        toolItem.setEnabled(false);
        toolItem.setSelection(false);
      }
    }
  }
  
  private ToolItem createToolItem(ToolBar toolBar,int style,String key,String imageName,String toolTipKey) {    
    final ToolItem toolItem = new ToolItem(toolBar,style);
    toolItem.setData("key",key);
    Messages.setLanguageText(toolItem,toolTipKey);   
    toolItem.setImage(ImageRepository.getImage(imageName));
    toolItem.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        if(currentEnabler != null)
          currentEnabler.itemActivated((String)toolItem.getData("key"));        	
      }
    });
    itemKeyToControl.put(key,toolItem);
    return toolItem;
  }  
  
  private void initBar() {
    //The File Menu
    CoolItem coolItem = new CoolItem(coolBar,SWT.NULL); 
    ToolBar toolBar = new ToolBar(coolBar,SWT.FLAT);
    createToolItem(toolBar,SWT.PUSH,"open","cb_open","iconBar.open.tooltip");    
    createToolItem(toolBar,SWT.PUSH,"open_no_default","cb_open_no_default","iconBar.openNoDefault.tooltip");    
    createToolItem(toolBar,SWT.PUSH,"open_url","cb_open_url","iconBar.openURL.tooltip");    
    createToolItem(toolBar,SWT.PUSH,"open_folder","cb_open_folder","iconBar.openFolder.tooltip");            
    createToolItem(toolBar,SWT.PUSH,"new","cb_new","iconBar.new.tooltip");
    toolBar.pack();
    Point p = toolBar.getSize();
    coolItem.setControl(toolBar);
    coolItem.setSize(p.x,p.y);
    coolItem.setMinimumSize(p.x,p.y);
    
    
    coolItem = new CoolItem(coolBar,SWT.NULL); 
    toolBar = new ToolBar(coolBar,SWT.FLAT);    
    createToolItem(toolBar,SWT.PUSH,"up","cb_up","iconBar.up.tooltip");
    createToolItem(toolBar,SWT.PUSH,"down","cb_down","iconBar.down.tooltip");
    new ToolItem(toolBar,SWT.SEPARATOR);
    createToolItem(toolBar,SWT.PUSH,"run","cb_run","iconBar.run.tooltip");
    createToolItem(toolBar,SWT.PUSH,"host","cb_host","iconBar.host.tooltip");
    createToolItem(toolBar,SWT.PUSH,"start","cb_start","iconBar.start.tooltip");
    createToolItem(toolBar,SWT.PUSH,"stop","cb_stop","iconBar.stop.tooltip");
    createToolItem(toolBar,SWT.PUSH,"remove","cb_remove","iconBar.remove.tooltip");
    toolBar.pack();
    p = toolBar.getSize();
    coolItem.setControl(toolBar);
    coolItem.setSize(p.x,p.y);
    coolItem.setMinimumSize(p.x,p.y);
  }
  
  void setLayoutData(Object layoutData) {
    coolBar.setLayoutData(layoutData);
  }
  
  public static void main(String args[]) {
    Display display = new Display();
    Shell shell = new Shell(display);
    ImageRepository.loadImages(display);
    GridLayout layout = new GridLayout();
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    shell.setLayout(layout);
    IconBar ibar = new IconBar(shell);
    ibar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();        
  }

}
