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

package org.gudy.azureus2.ui.swt.maketorrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;

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

  Button previous, next, finish, cancel;

  Listener closeCatcher;

  //false : singleMode, true:  directory
  boolean mode;
  String singlePath = "";
  String directoryPath = "";
  String savePath = "";
  String trackerURL = "http://";

  IWizardPanel currentPanel;
  Composite panel;

  public Wizard(Display display) {
    this.display = display;
    wizardWindow = new Shell(display, SWT.DIALOG_TRIM);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    wizardWindow.setLayout(layout);
    wizardWindow.setImage(ImageRepository.getImage("azureus"));
    Messages.setLanguageText(wizardWindow, "wizard.title");
    Composite cTitle = new Composite(wizardWindow, SWT.NULL);
    Color white = new Color(display, 255, 255, 255);
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
    currentInfo = new Label(cTitle, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    currentInfo.setLayoutData(gridData);
    currentInfo.setBackground(white);
    errorMessage = new Label(cTitle, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    errorMessage.setLayoutData(gridData);
    errorMessage.setBackground(white);
    Color red = new Color(display, 255, 0, 0);
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

    currentPanel = new ModePanel(this, null);
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
      public void handleEvent(Event arg0) {
        /*MessageBox mb = new MessageBox(wizardWindow);
        mb.setMessage(MessageText.getString("wizard.notimplemented"));
        mb.setText(MessageText.getString("wizard.information"));
        mb.open();*/
        cancel.setEnabled(false);
        wizardWindow.addListener(SWT.Close, closeCatcher);
        clearPanel();
        currentPanel = new ProgressPanel(wizard, currentPanel);
        refresh();
        currentPanel.finish();
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

    wizardWindow.setSize(400, 300);
    wizardWindow.open();

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

  private void refresh() {
    if (currentPanel == null)
      return;
    previous.setEnabled(currentPanel.isPreviousEnabled());
    next.setEnabled(currentPanel.isNextEnabled());
    finish.setEnabled(currentPanel.isFinishEnabled());
    currentPanel.show();
    panel.layout();
    panel.redraw();
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
}
