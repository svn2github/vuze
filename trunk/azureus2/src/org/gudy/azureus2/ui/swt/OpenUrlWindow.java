/*
 * File    : OpenUrlWindow.java
 * Created : 3 nov. 2003 15:30:46
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;

/**
 * @author Olivier
 * 
 */
public class OpenUrlWindow {

  public OpenUrlWindow(final Display display, String linkURL) {
    final Shell shell = new Shell(display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    shell.setText(MessageText.getString("openUrl.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    
    GridData gridData;
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);        
    Label label = new Label(shell, SWT.NULL);
    label.setText(MessageText.getString("openUrl.url"));
    gridData = new GridData();
    label.setLayoutData(gridData);
    
    final Text url = new Text(shell, SWT.BORDER);

    gridData = new GridData();//GridData.FILL_HORIZONTAL
    gridData.widthHint=300;
    url.setLayoutData(gridData);
    if(linkURL == null)
      Utils.setTextLinkFromClipboard(shell, gridData, url);
    else
      Utils.setTextLink(shell, gridData, url, linkURL);
    url.setSelection(url.getText().length());
    
    Composite panel = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);        
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    panel.setLayoutData(gridData);
    Button ok = new Button(panel,SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 100;    
    gridData.horizontalSpan = 2;
    ok.setLayoutData(gridData);
    ok.setText(MessageText.getString("openUrl.ok"));
    ok.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {        
        new FileDownloadWindow(display,url.getText());
        shell.dispose();
      }
    }); 
    shell.setDefaultButton (ok);
    
    Button cancel = new Button(panel,SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 100;
    cancel.setLayoutData(gridData);
    cancel.setText(MessageText.getString("openUrl.cancel"));
    cancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        shell.dispose();
      }
    });        
    
    shell.pack();
    Utils.createURLDropTarget(shell, url);
    shell.open();
  }
}
