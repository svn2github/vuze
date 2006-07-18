/*
 * Created on 16 July 2006
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.components.ControlUtils;

/**
 * @author amc1
 * Based on CategoryAdderWindow.
 */
public class SimpleTextEntryWindow {
	
	public static interface TextEntrySubmissionListener {
		public Object dataEntered(String value);
	}
	
	private Object parsed_data = null;
	private boolean data_entered = false;
	
	public SimpleTextEntryWindow(final Display display, final String title_key, final String message_key, final String default_value, final TextEntrySubmissionListener handler) {
	    final Shell shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	    if (title_key != null) {
	    	shell.setText(MessageText.getString(title_key));
	    }
	    if(! Constants.isOSX) {
	      shell.setImage(ImageRepository.getImage("azureus"));
	    }
	    GridLayout layout = new GridLayout();
	    shell.setLayout(layout);

	    Label label = new Label(shell, SWT.NONE);
	    if (message_key != null) {
	    	Messages.setLanguageText(label, message_key);
	    }
	    GridData gridData = new GridData();
	    gridData.widthHint = 300;
	    label.setLayoutData(gridData);

	    if ((message_key != null) && MessageText.keyExists(message_key + ".2")) {
	    	label = new Label(shell, SWT.NONE);
    		Messages.setLanguageText(label, message_key + ".2");
	    	gridData = new GridData();
	    	gridData.widthHint = 300;
	    	label.setLayoutData(gridData);
	    }
	    
	    final Text text_entry = new Text(shell, SWT.BORDER);
	    if (default_value != null) {
	    	text_entry.setText(default_value);
	    	text_entry.selectAll();
	    }
	    gridData = new GridData();
	    gridData.widthHint = 300;
	    text_entry.setLayoutData(gridData);

	    Composite panel = new Composite(shell, SWT.NULL);
	    final RowLayout rLayout = new RowLayout();
	    rLayout.marginTop = 0;
	    rLayout.marginLeft = 0;
	    rLayout.marginBottom = 0;
	    rLayout.marginRight = 0;
	    try {
	    	rLayout.fill = true;
	    } catch (NoSuchFieldError e) {
	    	// SWT 2.x
	    }
	    rLayout.spacing = ControlUtils.getButtonMargin();
	    panel.setLayout(rLayout);
	    gridData = new GridData();
	    gridData.horizontalAlignment = (Constants.isOSX) ? SWT.END : SWT.CENTER;
	    panel.setLayoutData(gridData);

	    Button ok = createAlertButton(panel, "Button.ok");
	    Button cancel = createAlertButton(panel, "Button.cancel");

	    ok.addListener(SWT.Selection, new Listener() {
	      /* (non-Javadoc)
	       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	       */
	      public void handleEvent(Event event) {
	    	  try {
	    		  String entered_data = text_entry.getText().trim();
	    		  Object result = entered_data;
	    		  if (handler != null) {result = handler.dataEntered(entered_data);}
	    		  SimpleTextEntryWindow.this.parsed_data = result;
	    		  SimpleTextEntryWindow.this.data_entered = true;
	    	  }
	    	  catch (Exception e) {
	    		  Debug.printStackTrace(e);
	    	  }
	    	  shell.dispose();
	      }
	    });
	
	    cancel.addListener(SWT.Selection, new Listener() {
	        /* (non-Javadoc)
	         * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	         */
	        public void handleEvent(Event event) {
	          shell.dispose();
	        }
	      });

	    shell.setDefaultButton(ok);
	    shell.pack();
	    Utils.createURLDropTarget(shell, text_entry);
	    Utils.centreWindow(shell);
	    shell.open();
	    while (!shell.isDisposed())
	      if (!display.readAndDispatch()) display.sleep();
	  }

  private static Button createAlertButton(final Composite panel, String localizationKey)
  {
      final Button button = new Button(panel, SWT.PUSH);
      button.setText(MessageText.getString(localizationKey));
      final RowData rData = new RowData();
      rData.width = Math.max(
              ControlUtils.getDialogButtonMinWidth(),
              button.computeSize(SWT.DEFAULT,  SWT.DEFAULT).x
        );
      button.setLayoutData(rData);
      return button;
  }

  public Object getStoredValue() {return this.parsed_data;}
  public String getStoredString() {return (String)this.parsed_data;}
  public boolean wasDataSubmitted() {return this.data_entered;}
}
