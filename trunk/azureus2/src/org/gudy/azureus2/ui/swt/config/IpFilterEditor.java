/*
 * File    : IpFilterEditor.java
 * Created : 8 oct. 2003 13:18:42
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

package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class IpFilterEditor {

  Display display;
  Table table;
  
  IpRange range;

  boolean newRange;

  public IpFilterEditor(Display display,Table _table, final IpRange _range) {
    this.display = display;
    this.table = _table;
    this.range = _range;
    if (range == null) {
      newRange = true;
      range = IpFilter.getInstance().createRange(false);
    }

    final Shell shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(shell,"ConfigView.section.ipfilter.editFilter");
    shell.setImage(ImageRepository.getImage("ipfilter"));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.description");

    final Text textDescription = new Text(shell, SWT.BORDER);
    GridData gridData = new GridData();
    gridData.widthHint = 300;
    textDescription.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.start");

    final Text textStartIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textStartIp.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.end");

    final Text textEndIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textEndIp.setLayoutData(gridData);

    final Button ok = new Button(shell, SWT.PUSH);
    Messages.setLanguageText(ok, "Button.ok");

    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 100;
    ok.setLayoutData(gridData);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        range.setDescription( textDescription.getText());
        range.setStartIp( textStartIp.getText());
        range.setEndIp( textEndIp.getText());
        range.checkValid();
        if (newRange) {
          IpFilter.getInstance().addRange(range);
          TableItem item = new TableItem(table,SWT.NULL);
          item.setData(range);
          item.setImage(0,ImageRepository.getImage("ipfilter"));
          item.setText(0, range.getDescription());
          item.setText(1, range.getStartIp());
          item.setText(2, range.getEndIp());
        }else{
        	TableItem[] items = table.getItems();
        	
        	for (int i=0;i<items.length;i++){
        		
        		if ( items[i].getData() == range ){
        			
        	        items[i].setText(0, range.getDescription());
        	        items[i].setText(1, range.getStartIp());
        	        items[i].setText(2, range.getEndIp());
        	        
        	        break;
        	   	}
        	}
        }
         shell.dispose();
      }
    });   
    
    textStartIp.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent event) {
        range.setStartIp( textStartIp.getText());
        range.checkValid();
        if(range.isValid())
          ok.setEnabled(true);
        else
          ok.setEnabled(false);
      }
    });
    
    textEndIp.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent event) {
            range.setEndIp( textEndIp.getText());
            range.checkValid();
            if(range.isValid())
              ok.setEnabled(true);
            else
              ok.setEnabled(false);
          }
     });
     
    if (range != null) {
          textDescription.setText(range.getDescription());
          textStartIp.setText(range.getStartIp());
          textEndIp.setText(range.getEndIp());
    }

    shell.pack();
    shell.open();
  }

}
