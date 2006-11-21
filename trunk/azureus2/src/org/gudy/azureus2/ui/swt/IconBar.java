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
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.components.*;

/**
 * @author Olivier
 *
 */
public class IconBar {
  
  CoolBar coolBar;
  Composite parent;    
  Map itemKeyToControl;
  
  IconBarEnabler currentEnabler;
	private Composite cIconBar;
  
  public IconBar(Composite parent) {
    this.parent = parent;
    cIconBar = new Composite(parent, SWT.NONE);
    
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    cIconBar.setLayout(layout);
    
    this.itemKeyToControl = new HashMap();    
    	// 3.1 onwards the default is gradient-fill - the disabled icons' transparency no workies
    	// so disabled buttons look bad on the gradient-filled background
    this.coolBar = new CoolBar(cIconBar,Constants.isWindows?SWT.FLAT:SWT.NULL);
    initBar();       
    this.coolBar.setLocked(true);
    
    coolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    

    Label separator = new Label(cIconBar, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
	}

	public void setEnabled(String itemKey,boolean enabled) {
    BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(itemKey);
    if(BufferedToolItem != null)
      BufferedToolItem.setEnabled(enabled);
  }
  
  public void setSelection(String itemKey,boolean selection) {
    BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(itemKey);
    if(BufferedToolItem != null)
      BufferedToolItem.setSelection(selection);
  }
  
  public void setCurrentEnabler(IconBarEnabler enabler) {
    this.currentEnabler = enabler;
    refreshEnableItems();
  }
  
  private void refreshEnableItems() {
    Iterator iter = itemKeyToControl.keySet().iterator();
    while(iter.hasNext()) {
      String key = (String) iter.next();
      BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(key);
      if(BufferedToolItem == null )
        continue;
      if(currentEnabler != null) {
        BufferedToolItem.setEnabled(currentEnabler.isEnabled(key));
        BufferedToolItem.setSelection(currentEnabler.isSelected(key));
      }
      else {        
        BufferedToolItem.setEnabled(false);
        BufferedToolItem.setSelection(false);
      }
    }
  }
  
  private BufferedToolItem createBufferedToolItem(ToolBar toolBar,int style,String key,String imageName,String toolTipKey) {    
    final BufferedToolItem bufferedToolItem = new BufferedToolItem(toolBar,style);
    bufferedToolItem.setData("key",key);
    Messages.setLanguageText(bufferedToolItem.getWidget(),toolTipKey,true);   
    bufferedToolItem.setImage(ImageRepository.getImage(imageName));
   
    
    bufferedToolItem.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        if(currentEnabler != null)
          currentEnabler.itemActivated((String)bufferedToolItem.getData("key"));        	
      }
    });
    itemKeyToControl.put(key,bufferedToolItem);
    return bufferedToolItem;
  }  
  
  private void initBar() {
    //The File Menu
    CoolItem coolItem = new CoolItem(coolBar,SWT.NULL);

    ToolBar toolBar = new ToolBar(coolBar,SWT.FLAT);
    createBufferedToolItem(toolBar,SWT.PUSH,"open","cb_open_no_default","iconBar.open.tooltip");
    // XXX TuxPaper: Remove images (open, open_url, open_folder) from CVS and ImageRepository
    createBufferedToolItem(toolBar,SWT.PUSH,"new","cb_new","iconBar.new.tooltip");
    toolBar.pack(); 
    Point p = toolBar.getSize();
    coolItem.setControl(toolBar);
    coolItem.setSize(coolItem.computeSize (p.x,p.y));
    coolItem.setMinimumSize(p.x,p.y);
    
    
    coolItem = new CoolItem(coolBar,SWT.NULL); 
    toolBar = new ToolBar(coolBar,SWT.FLAT);    
    createBufferedToolItem(toolBar,SWT.PUSH,"top","cb_top","iconBar.top.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"up","cb_up","iconBar.up.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"down","cb_down","iconBar.down.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"bottom","cb_bottom","iconBar.bottom.tooltip");
    new BufferedToolItem(toolBar,SWT.SEPARATOR);
    createBufferedToolItem(toolBar,SWT.PUSH,"run","cb_run","iconBar.run.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"host","cb_host","iconBar.host.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"publish","cb_publish","iconBar.publish.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"start","cb_start","iconBar.start.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"stop","cb_stop","iconBar.stop.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"remove","cb_remove","iconBar.remove.tooltip");

    toolBar.pack();
    p = toolBar.getSize();
    coolItem.setControl(toolBar);
    coolItem.setSize(p.x,p.y);
    coolItem.setMinimumSize(p.x,p.y);    
  }
  
  public void setLayoutData(Object layoutData) {
  	cIconBar.setLayoutData(layoutData);
  }
  
  public static void main(String args[]) {
    Display display = new Display();
    Shell shell = new Shell(display);
    ImageRepository.loadImages(display);
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    shell.setLayout(layout);
    IconBar ibar = new IconBar(shell);
    FormData formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    ibar.setLayoutData(formData);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();        
  }
  
	public Composite getComposite() {
		return cIconBar;
	}

	/**
	 * 
	 */
	public void delete() {
		Utils.disposeComposite(cIconBar);
		itemKeyToControl.clear();
		currentEnabler = null;
	}

}
