/*
 * Created on 7 sept. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.SHA1Hasher;

/**
 * @author Olivier
 * 
 */
public class PasswordWindow {

  private Display display;
  private Shell shell;
  
  private static int nbInstances = 0;
  
  public static synchronized void showPasswordWindow(Display display) {
    if(nbInstances == 0)
       new PasswordWindow(display);     
  }
  protected PasswordWindow(Display display) {
    nbInstances++;
    this.display = display;
    shell = new Shell(display,SWT.APPLICATION_MODAL | SWT.TITLE | SWT.CLOSE);
    shell.setText(MessageText.getString("PasswordWindow.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    shell.setLayout(layout);
    
    Label label = new Label(shell,SWT.NONE);
    label.setText(MessageText.getString("PasswordWindow.passwordprotected"));
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    
    final Text password = new Text(shell,SWT.BORDER);
    password.setEchoChar('*');
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    password.setLayoutData(gridData);
    
    Button ok = new Button(shell,SWT.PUSH);
    ok.setText(MessageText.getString("PasswordWindow.ok"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection,new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        try{
                 SHA1Hasher hasher = new SHA1Hasher();
                 byte[] passwordText = password.getText().getBytes();
                 byte[] encoded = hasher.calculateHash(passwordText);
                 byte[] correct = COConfigurationManager.getByteParameter("Password","".getBytes());
                 boolean same=true;
                 for(int i=0; i<correct.length;i++)
                   {
                     if(correct[i] != encoded[i])
                       same = false;
                   }
                   if(same) {
                     MainWindow.getWindow().setVisible(true);
                     TrayWindow tw = MainWindow.getWindow().getTray();
                     if(tw != null) {
                       tw.setVisible(false);
                       tw.setMoving(false);
                     }
                     shell.dispose();                                   
                   } else {
                     shell.dispose();
                   }                   
               } catch(Exception e) {
                 e.printStackTrace();
               }
      }
    });    
    
    Button cancel = new Button(shell,SWT.PUSH);
    cancel.setText(MessageText.getString("PasswordWindow.cancel"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection,new Listener() {
          /* (non-Javadoc)
           * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
           */
          public void handleEvent(Event event) {
             
             shell.dispose();
          }
        });    
    
    
    shell.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent arg0) {
        nbInstances--;
      }
    });
    
    shell.pack();
    shell.open();
  }      

}
