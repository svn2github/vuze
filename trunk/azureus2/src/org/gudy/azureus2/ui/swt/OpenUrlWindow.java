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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;

import sun.security.krb5.internal.crypto.d;

/**
 * @author Olivier
 * 
 */
public class OpenUrlWindow {

  Display display;
  Shell shell;
  
  public OpenUrlWindow(final Display display) {
    this.display = display;
    shell = new Shell(display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    shell.setText(MessageText.getString("openUrl.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    
    GridData gridData;
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);        
    
    Label label = new Label(shell,SWT.NULL);
    label.setText(MessageText.getString("openUrl.url"));
    
    final Text url = new Text(shell,SWT.BORDER);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint=300;
    url.setText("http://");
    Utils.setTextLinkFromClipboard(shell, gridData, url);
    url.setSelection(url.getText().length());
    gridData.horizontalSpan = 2;
    url.setLayoutData(gridData);
    
    new Label(shell,SWT.NULL);
    Button ok = new Button(shell,SWT.PUSH);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 100;    
    gridData.horizontalAlignment = GridData.END;
    ok.setLayoutData(gridData);
    ok.setText(MessageText.getString("openUrl.ok"));
    ok.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {        
        new FileDownloadWindow(display,url.getText());
        shell.dispose();
      }
    }); 
    shell.setDefaultButton (ok);
    
    Button cancel = new Button(shell,SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalAlignment = GridData.END;
    cancel.setLayoutData(gridData);
    cancel.setText(MessageText.getString("openUrl.cancel"));
    cancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        shell.dispose();
      }
    });        
    
    shell.pack();
    shell.open();
  }
}
