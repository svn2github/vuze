/*
 * File    : UpdateSWTWindow.java
 * Created : 2 avr. 2004
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
package org.gudy.azureus2.ui.swt.updater;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateSWTWindow implements GeneralListener{
  
  public boolean bIgnored = false;
  
  Shell shell;
  Label status;
  String statusBase;
  ProgressBar progress;
  Button btnOk;
  Button btnCancel;
  Button btnIgnore;
  Display display;
  
  int lastPercent;
  
  MainUpdater mainUpdater;
  
  public UpdateSWTWindow() {
    display = new Display();
    shell = new Shell(display);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    shell.setLayout(layout);
    
    shell.setText(MessageText.getString("window.updateswt.title"));
    
    Label label = new Label(shell,SWT.WRAP);
    label.setText(MessageText.getString("window.updateswt.text"));
    GridData gridData = new GridData(GridData.FILL_BOTH);    
    label.setLayoutData(gridData);
    
    status = new Label(shell,SWT.WRAP);
    statusBase = MessageText.getString("window.updateswt.status") + " : ";
    status.setText(statusBase + "\n -"); 
    gridData = new GridData(GridData.FILL_HORIZONTAL);    
    status.setLayoutData(gridData);
        
    progress = new ProgressBar(shell,SWT.HORIZONTAL);
    progress.setMinimum(0);
    progress.setMaximum(100);
    progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    Composite composite = new Composite(shell,SWT.NULL);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    layout = new GridLayout();   
    layout.numColumns = 3;
    composite.setLayout(layout);
    
    btnOk = new Button(composite,SWT.PUSH);
    btnOk.setText(MessageText.getString("window.updateswt.ok"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    gridData.widthHint = 100;
    btnOk.setLayoutData(gridData);
    btnOk.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        btnOk.setEnabled(false);        
        mainUpdater = new MainUpdater(UpdateSWTWindow.this);
        btnIgnore.setEnabled(false);
      }
    });
    
    btnIgnore = new Button(composite,SWT.PUSH);
    btnIgnore.setText(MessageText.getString("window.updateswt.ignore"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 70;
    btnIgnore.setLayoutData(gridData);
    btnIgnore.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        bIgnored = true;
        shell.dispose();
      }
    });
    
    btnCancel = new Button(composite,SWT.PUSH);
    btnCancel.setText(MessageText.getString("window.updateswt.cancel"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 100;
    btnCancel.setLayoutData(gridData);
    btnCancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        if(mainUpdater != null)
          mainUpdater.cancel();
        shell.dispose();
      }
    });
        
    shell.setSize(500,300);
    shell.layout();
    Utils.centreWindow(shell);
    
    shell.open ();
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }    
    display.dispose ();
    
  }
  
  
  public void percentDone(final int percent) {
    if(lastPercent == percent) return;
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new Runnable() {
        public void run() {
          if(!progress.isDisposed())
            progress.setSelection(percent); 
        }
      });
    }
  }
  
  public void processFailed() {
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new Runnable() {
        public void run() {
         status.setText(statusBase + MessageText.getString("window.updateswt.failed")); 
         btnOk.setEnabled(true);
         btnIgnore.setEnabled(true);
        }
      });
    }
  }
  

  public void processName(final String name) {    
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new Runnable() {
        public void run() {
         status.setText(statusBase + name);
         //shell.pack();
        }
      });
    }
  }
  
  public void processSucceeded() {
    mainUpdater.launchSWTUpdate();
    if(display != null && ! display.isDisposed()) {
      display.syncExec(new Runnable() {
        public void run() {
          shell.dispose();
        }
      });
    }
  }
}
