/*
 * File    : BlockedIpsWindow.java
 * Created : 17 déc. 2003}
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Olivier
 *
 */
public class BlockedIpsWindow {
  
  public static void show(Display display,String ips) {
    final Shell window = new Shell(display,SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(window,"ipFilter.list.title");
    window.setImage(ImageRepository.getImage("azureus"));
    
    FormLayout layout = new FormLayout();
    layout.spacing = 3;
    layout.marginHeight = 3;
    layout.marginWidth = 3;
    window.setLayout(layout);
    FormData formData;
    
    StyledText text = new StyledText(window,SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    Button btnOk = new Button(window,SWT.PUSH);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);   
    formData.bottom = new FormAttachment(btnOk);   
    text.setLayoutData(formData);
    text.setText(ips);
    
    
    Messages.setLanguageText(btnOk,"Button.ok");
    formData = new FormData();
    formData.right = new FormAttachment(100,0);    
    formData.bottom = new FormAttachment(100,0);
    formData.width = 70;
    btnOk.setLayoutData(formData);
    btnOk.addListener(SWT.Selection,new Listener() {
    public void handleEvent(Event e) {
      window.dispose();
    }
    });
        
    window.setSize(420,320);
    window.layout();
    window.open();    
  }
}
