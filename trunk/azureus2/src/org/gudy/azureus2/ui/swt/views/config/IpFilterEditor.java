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

package org.gudy.azureus2.ui.swt.views.config;

import java.util.List;

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
import org.gudy.azureus2.core.IpRange;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class IpFilterEditor {

  Display display;
  Table table;
  
  List ipRanges;
  IpRange range;

  boolean newRange;

  public IpFilterEditor(Display display,Table _table, List _ipRanges, final IpRange _range) {
    this.display = display;
    this.table = _table;
    this.ipRanges = _ipRanges;
    this.range = _range;
    if (range == null) {
      newRange = true;
      range = new IpRange("","","");
    }

    final Shell shell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(shell,"ipFilter.editFilter");
    shell.setImage(ImageRepository.getImage("ipfilter"));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ipFilter.description");

    final Text textDescription = new Text(shell, SWT.BORDER);
    GridData gridData = new GridData();
    gridData.widthHint = 300;
    textDescription.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ipFilter.start");

    final Text textStartIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textStartIp.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ipFilter.end");

    final Text textEndIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textEndIp.setLayoutData(gridData);

    final Button ok = new Button(shell, SWT.PUSH);
    Messages.setLanguageText(ok, "ipFilter.ok");

    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 100;
    ok.setLayoutData(gridData);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        range.description = textDescription.getText();
        range.startIp = textStartIp.getText();
        range.endIp = textEndIp.getText();
        range.checkValid();
        if (newRange) {
          ipRanges.add(range);
          TableItem item = new TableItem(table,SWT.NULL);
          item.setData(range);
          item.setImage(0,ImageRepository.getImage("ipfilter"));
        }        
        shell.dispose();
      }
    });   
    
    textStartIp.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent event) {
        range.startIp = textStartIp.getText();
        range.checkValid();
        if(range.isValid())
          ok.setEnabled(true);
        else
          ok.setEnabled(false);
      }
    });
    
    textEndIp.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent event) {
            range.endIp = textEndIp.getText();
            range.checkValid();
            if(range.isValid())
              ok.setEnabled(true);
            else
              ok.setEnabled(false);
          }
     });
     
    if (range != null) {
          textDescription.setText(range.description);
          textStartIp.setText(range.startIp);
          textEndIp.setText(range.endIp);
    }

    shell.pack();
    shell.open();
  }

}
