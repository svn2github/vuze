/*
 * File    : Wizard.java
 * Created : 30 sept. 2003 00:06:56
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

package org.gudy.azureus2.ui.swt.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier
 * 
 */
public class Wizard {

  Display display;
  Shell wizardWindow;
  Label title;
  Label currentInfo;
  Label errorMessage;
  IWizardPanel currentPanel;
  Composite panel;
  Font titleFont;
  Button previous, next, finish, cancel;

  Listener closeCatcher;
  
  public Wizard(Display display,String keyTitle) {
    this(display);
    setTitleKey(keyTitle);
  }

  public Wizard(Display display) {
    this.display = display;
    wizardWindow = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    wizardWindow.setLayout(layout);
    wizardWindow.setImage(ImageRepository.getImage("azureus"));
    Composite cTitle = new Composite(wizardWindow, SWT.NULL);
    Color white = display.getSystemColor(SWT.COLOR_WHITE);
    cTitle.setBackground(white);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    cTitle.setLayoutData(gridData);
    GridLayout titleLayout = new GridLayout();
    titleLayout.numColumns = 1;
    cTitle.setLayout(titleLayout);
    title = new Label(cTitle, SWT.NULL);
    title.setBackground(white);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    title.setLayoutData(gridData);
    Font font = title.getFont();
    FontData[] data = font.getFontData();
    for(int i = 0 ; i < data.length ; i++) {
      data[i].setStyle(SWT.BOLD);
    }
    titleFont=new Font(display,data);
    title.setFont(titleFont);
    currentInfo = new Label(cTitle, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    currentInfo.setLayoutData(gridData);
    currentInfo.setBackground(white);
    errorMessage = new Label(cTitle, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    errorMessage.setLayoutData(gridData);
    errorMessage.setBackground(white);
    Color red = display.getSystemColor(SWT.COLOR_RED);
    errorMessage.setForeground(red);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new Label(wizardWindow, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(gridData);

    panel = new Composite(wizardWindow, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new Label(wizardWindow, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(gridData);

    Composite cButtons = new Composite(wizardWindow, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    cButtons.setLayoutData(gridData);
    GridLayout layoutButtons = new GridLayout();
    layoutButtons.numColumns = 5;
    cButtons.setLayout(layoutButtons);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new Label(cButtons, SWT.NULL).setLayoutData(gridData);

    previous = new Button(cButtons, SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 70;
    gridData.horizontalAlignment = GridData.END;
    previous.setLayoutData(gridData);
    Messages.setLanguageText(previous, "wizard.previous");

    next = new Button(cButtons, SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 70;
    gridData.horizontalAlignment = GridData.BEGINNING;
    next.setLayoutData(gridData);
    Messages.setLanguageText(next, "wizard.next");

    finish = new Button(cButtons, SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 70;
    gridData.horizontalAlignment = GridData.CENTER;
    finish.setLayoutData(gridData);
    Messages.setLanguageText(finish, "wizard.finish");

    cancel = new Button(cButtons, SWT.PUSH);
    gridData = new GridData();
    gridData.widthHint = 70;
    gridData.horizontalAlignment = GridData.CENTER;
    cancel.setLayoutData(gridData);
    Messages.setLanguageText(cancel, "wizard.cancel");

    refresh();

    previous.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        clearPanel();
        currentPanel = currentPanel.getPreviousPanel();
        refresh();
      }
    });

    next.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        clearPanel();
        currentPanel = currentPanel.getNextPanel();
        refresh();
      }
    });

    final Wizard wizard = this;

    closeCatcher = new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        event.doit = false;
      }
    };

    finish.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
		public void handleEvent(Event arg0){
    		finishSelected();
		}
    });
    
    cancel.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        wizardWindow.dispose();
      }
    });

    wizardWindow.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent de) {
        onClose();
      }
    });
    
 	wizardWindow.addListener(SWT.Traverse, new Listener() {
 		
		public void handleEvent(Event e) {
			
			if ( e.character == SWT.ESC){
							
				if ( cancel.isEnabled()){
					
					wizardWindow.dispose();
				}
			}
		}
	});

		
    wizardWindow.setSize(400, 400);
    
	Utils.centreWindow( wizardWindow );

    wizardWindow.open();

  }
  
   private void
   finishSelected()
   {
	   if ( currentPanel.isFinishSelectionOK()){
      	
		   cancel.setEnabled(false);
		   wizardWindow.addListener(SWT.Close, closeCatcher);
		   clearPanel();
		   currentPanel = currentPanel.getFinishPanel();
		   refresh();
		   currentPanel.finish();
	   }
  }

  private void clearPanel() {
    Control[] controls = panel.getChildren();
    for (int i = 0; i < controls.length; i++) {
      if (controls[i] != null && !controls[i].isDisposed())
        controls[i].dispose();
    }
    setTitle("");
    setCurrentInfo("");
  }

  private void refresh(){
    if (currentPanel == null){
    	
    	setDefaultButton();
    	
      	return;
    }
    
    previous.setEnabled(currentPanel.isPreviousEnabled());
    
    next.setEnabled(currentPanel.isNextEnabled());
    
    finish.setEnabled(currentPanel.isFinishEnabled());
    
	setDefaultButton();
	
    currentPanel.show();
    panel.layout();
    panel.redraw();
  }

	private void
	setDefaultButton()
	{
		if (display != null && !display.isDisposed()){
		
		 	display.asyncExec(new Runnable() {
				public void run() {
		 	
			  	Button	default_button = null;
			  	
				if ( next.isEnabled()){
			    	
					default_button = next;
					
				}else if ( finish.isEnabled()){
				
					default_button = finish;
					
				}else if ( previous.isEnabled()){
					
					default_button = previous;
					
				}else if ( cancel.isEnabled()){
					
					default_button	= cancel;
				}
				
				if ( default_button != null ){
				
					wizardWindow.setDefaultButton( default_button );
				}
		 	}
		});
	 }
  }
  
  public Composite getPanel() {
    return panel;
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public void setCurrentInfo(String currentInfo) {
    this.currentInfo.setText("\t" + currentInfo);
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage.setText(errorMessage);
  }

  public void setTitleKey(String key) {
    Messages.setLanguageText(wizardWindow, key);
  }

  public void setNextEnabled(boolean enabled) {
    this.next.setEnabled(enabled);
	setDefaultButton();
  }

  public void setPreviousEnabled(boolean enabled) {
    this.previous.setEnabled(enabled);
	setDefaultButton();
  }

  public void setFinishEnabled(boolean enabled) {
    this.finish.setEnabled(enabled);
	setDefaultButton();
  }

  public void setFirstPanel(IWizardPanel panel) {
    this.currentPanel = panel;
    refresh();
  }

  public Shell getWizardWindow() {
    return wizardWindow;
  }

  public String getErrorMessage() {
    return errorMessage.getText();
  }

  public Display getDisplay() {
    return display;
  }

  public void switchToClose() {
    if (display != null && !display.isDisposed()) {}
    display.asyncExec(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        if (closeCatcher != null && wizardWindow != null && !wizardWindow.isDisposed()) {
          wizardWindow.removeListener(SWT.Close, closeCatcher);
          cancel.setText(MessageText.getString("wizard.close"));
          cancel.setEnabled(true);
		  setDefaultButton();
        }
      }
    });
  }
  
  public void onClose() {
  	if (titleFont != null && !titleFont.isDisposed()) {
  		titleFont.dispose();
  		titleFont=null;
  	}
  }  
  /**
   * @return Returns the currentPanel.
   */
  public IWizardPanel getCurrentPanel() {
    return currentPanel;
  }

}
