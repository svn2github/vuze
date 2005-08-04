package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

public class StringListChooser {

  private Display display;  
  private Shell shell;
  private Label label;
  private Combo combo;
  
  private String result;  
  
  public StringListChooser(final Shell parentShell) {
    result = null;
    
    display = parentShell.getDisplay();
    if(display == null || display.isDisposed()) return;
    display.syncExec(new Runnable() {
      public void run() {
       createShell(parentShell); 
      }
    });
  }
  
  private void createShell(Shell parentShell) {
      
    shell = ShellFactory.createShell(display,SWT.APPLICATION_MODAL | SWT.BORDER | SWT.TITLE | SWT.CLOSE);
    
    GridLayout layout = new GridLayout();    
    layout.numColumns = 2;
    shell.setLayout(layout);
    GridData data;
    
    label = new Label(shell,SWT.WRAP);
    
    combo = new Combo(shell,SWT.READ_ONLY);
    
    Button ok = new Button(shell,SWT.PUSH);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
       result = combo.getText();
       shell.dispose();       
       //Unlock waiting "open()" if any
       unlockWaitingOpen();
      }
    });
    ok.setText(MessageText.getString("Button.ok"));
    
    Button cancel = new Button(shell,SWT.PUSH);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
       result = null;
       shell.dispose();       
       //Unlock waiting "open()" if any
       unlockWaitingOpen();
      }
    });
    cancel.setText(MessageText.getString("Button.cancel"));
    
    
    shell.addListener(SWT.Dispose,new Listener() {
      public void handleEvent(Event arg0) {
       result = null;
       //Unlock waiting "open()" if any
       unlockWaitingOpen();
      }
    });
    
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    label.setLayoutData(data);
    
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    combo.setLayoutData(data);
        
    data = new GridData();
    data.grabExcessHorizontalSpace = true;
    data.horizontalAlignment = SWT.END;
    ok.setLayoutData(data);
    
    shell.layout();
    
  }
  
  public void setTitle(final String title) {
    if(display == null || display.isDisposed()) return;
    display.asyncExec(new Runnable() {    
      public void run() {
        shell.setText(title);
      }    
    });
  }
  
  public void setText(final String text) {
    if(display == null || display.isDisposed()) return;
    display.asyncExec(new Runnable() {    
      public void run() {
        label.setText(text);
      }    
    });
  }
  
  public void addOption(final String option) {
    if(display == null || display.isDisposed()) return;
    display.asyncExec(new Runnable() {    
      public void run() {
        combo.add(option);
      }    
    });
  }
  
  public String open() {
    if(display == null || display.isDisposed()) return null;    
    display.asyncExec(new Runnable() {    
      public void run() {
       shell.open(); 
      }    
    });
    
    //Add some code here to wait for the shell to close
    
    lockWaitingOpen();
    
    return result;
  }
  
  private void lockWaitingOpen() {
    
  }
  
  private void unlockWaitingOpen() {
    
  }
}
