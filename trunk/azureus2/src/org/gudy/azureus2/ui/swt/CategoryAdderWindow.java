/*
 * Created on 2 feb. 2004
 *
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
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;

/**
 * @author Olivier
 * 
 */
public class CategoryAdderWindow {
  private Category newCategory = null;
  public CategoryAdderWindow(final Display display) {
    final Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

    shell.setText(MessageText.getString("CategoryAddWindow.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    label.setText(MessageText.getString("CategoryAddWindow.message"));    
    GridData gridData = new GridData();
    gridData.widthHint = 200;
    label.setLayoutData(gridData);

    final Text category = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 300;
    category.setLayoutData(gridData);

    Composite panel = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);        
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    panel.setLayoutData(gridData);
    Button ok = new Button(panel, SWT.PUSH);
    ok.setText(MessageText.getString("Button.ok"));
    gridData = new GridData();
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        try {
          if (category.getText() != "") {
           newCategory = CategoryManager.createCategory(category.getText());
          }
        	
        	shell.dispose();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    Button cancel = new Button(panel, SWT.PUSH);
    cancel.setText(MessageText.getString("Button.cancel"));
    gridData = new GridData();
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    shell.pack();
    Utils.createURLDropTarget(shell, category);
    shell.open();
    while (!shell.isDisposed())
      if (!display.readAndDispatch()) display.sleep();
  }
  
  public Category getNewCategory() {
    return newCategory;
  }
}
