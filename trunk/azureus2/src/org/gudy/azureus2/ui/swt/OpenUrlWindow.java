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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.MessageText;

/**
 * @author Olivier
 * 
 */
public class 
OpenUrlWindow 
{
  protected static String	CONFIG_REFERRER_DEFAULT = "openUrl.referrer.default";
	
  protected static String	last_referrer = null;
  
  static{
  	last_referrer = COConfigurationManager.getStringParameter( CONFIG_REFERRER_DEFAULT, "" );
  }
  
  public 
  OpenUrlWindow(
  	final AzureusCore	azureus_core,
	final Display 		display, 
	String 				linkURL,
	final String		referrer ) 
  {
    final Shell shell = new Shell(display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    shell.setText(MessageText.getString("openUrl.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    
    GridData gridData;
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);
    
    	// URL field
    
    Label label = new Label(shell, SWT.NULL);
    label.setText(MessageText.getString("openUrl.url"));
    gridData = new GridData();
    label.setLayoutData(gridData);
    
    final Text url = new Text(shell, SWT.BORDER);

    gridData = new GridData();//GridData.FILL_HORIZONTAL
    gridData.widthHint=400;
    gridData.horizontalSpan	= 2;
    url.setLayoutData(gridData);
    if(linkURL == null)
      Utils.setTextLinkFromClipboard(shell, gridData, url);
    else
      Utils.setTextLink(shell, gridData, url, linkURL);
    url.setSelection(url.getText().length());
    
    	// referrer field
    
    Label referrer_label = new Label(shell, SWT.NULL);
    referrer_label.setText(MessageText.getString("openUrl.referrer"));
    gridData = new GridData();
    referrer_label.setLayoutData(gridData);
    
    final Text referrer_text = new Text(shell, SWT.BORDER);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint=150;
	gridData.grabExcessHorizontalSpace = true;
    referrer_text.setLayoutData(gridData);
    
    if ( referrer != null && referrer.length() > 0 ){
    	
    	referrer_text.setText( referrer );
    	
    }else if ( last_referrer != null ){
    	
    	referrer_text.setText( last_referrer );
    }
    
    Label referrer_info = new Label(shell, SWT.NULL);
    referrer_info.setText(MessageText.getString("openUrl.referrer.info"));
    
	// line
	
	Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
	gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = 3;
	labelSeparator.setLayoutData(gridData);

    	// buttons
    
    Composite panel = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);        
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
	gridData.grabExcessHorizontalSpace = true;
    panel.setLayoutData(gridData);
 	
    new Label(panel, SWT.NULL);
    
    Button ok = new Button(panel,SWT.PUSH);
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.widthHint = 70;    
	gridData.grabExcessHorizontalSpace = true;
    ok.setLayoutData(gridData);
    ok.setText(MessageText.getString("Button.ok"));
    ok.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {     
      	last_referrer	= referrer_text.getText().trim();
      	
      	COConfigurationManager.setParameter( CONFIG_REFERRER_DEFAULT, last_referrer );
      	COConfigurationManager.save();
      	
        new FileDownloadWindow(azureus_core,display,url.getText(), last_referrer );
        shell.dispose();
      }
    }); 
    
    shell.setDefaultButton (ok);
    
    Button cancel = new Button(panel,SWT.PUSH);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
	gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.setText(MessageText.getString("Button.cancel"));
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
