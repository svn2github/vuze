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
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.BlockedIp;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.util.DisplayFormatters;

/**
 * @author Olivier
 *
 */
public class BlockedIpsWindow {
  
  public static void show(Display display,String ips) {
    final Shell window = new Shell(display,SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(window,"ConfigView.section.ipfilter.list.title");
    window.setImage(ImageRepository.getImage("azureus"));
    
    FormLayout layout = new FormLayout();
    try {
      layout.spacing = 3;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    layout.marginHeight = 3;
    layout.marginWidth = 3;
    window.setLayout(layout);
    FormData formData;
    
    	// text area
    
    final StyledText text = new StyledText(window,SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    
    Button btnOk = new Button(window,SWT.PUSH);
    
    Button btnClear = new Button(window,SWT.PUSH);
            
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);   
    formData.bottom = new FormAttachment(btnOk);   
    text.setLayoutData(formData);
    text.setText(ips);
    text.setEditable(false);
    
    	// clear button
    
    
    Messages.setLanguageText(btnClear,"Button.clear");
    formData = new FormData();
    formData.top = new FormAttachment(text);    
    formData.right = new FormAttachment(btnOk);    
    formData.bottom = new FormAttachment(100,0);
    formData.width = 70;
    btnClear.setLayoutData(formData);
    btnClear.addListener(SWT.Selection,new Listener() {

    public void handleEvent(Event e) {
     
    	IpFilter.getInstance().clearBlockedIPs();
    	
    	text.setText( "" );
    }
    });
    
    	// ok button
    
    
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
        
    window.setDefaultButton( btnOk );
    
    window.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
			     window.dispose();
			 }
		}
    });
    
    window.setSize(720,320);
    window.layout();
    window.open();    
  }
  
  public static void showBlockedIps(Shell mainWindow) {
    StringBuffer sb = new StringBuffer();
    BlockedIp[] blocked = IpFilter.getInstance().getBlockedIps();
    String inRange = MessageText.getString("ConfigView.section.ipfilter.list.inrange");
    String notInRange = MessageText.getString("ConfigView.section.ipfilter.list.notinrange");    
    for(int i=0;i<blocked.length;i++){
      BlockedIp bIp = blocked[i];
      sb.append(DisplayFormatters.formatTimeStamp(bIp.getBlockedTime()));
      sb.append("\t[");
      sb.append( bIp.getTorrentName() );
      sb.append("] \t");
      sb.append(bIp.getBlockedIp());
      IpRange range = bIp.getBlockingRange();
      if(range == null) {
        sb.append(' ');
        sb.append(notInRange);
        sb.append('\n');
      } else {
        sb.append(' ');
        sb.append(inRange);
        sb.append(range.toString());
        sb.append('\n');
      }
    }   
    if(mainWindow == null || mainWindow.isDisposed())
      return;
    BlockedIpsWindow.show(mainWindow.getDisplay(),sb.toString());
  }
}
