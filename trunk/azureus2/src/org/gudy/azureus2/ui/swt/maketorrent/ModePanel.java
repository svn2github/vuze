/*
 * File    : ModePanel.java
 * Created : 30 sept. 2003 01:51:05
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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class ModePanel extends AbstractWizardPanel {

  public ModePanel(Wizard wizard, AbstractWizardPanel previous) {
    super(wizard, previous);
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("wizard.mode"));
    wizard.setCurrentInfo(MessageText.getString("wizard.singlefile.help"));
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);

    Label label = new Label(panel,SWT.NULL);
    Messages.setLanguageText(label,"wizard.tracker");
    final Text tracker = new Text(panel,SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    tracker.setLayoutData(gridData);
    tracker.addModifyListener(new ModifyListener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
       */
      public void modifyText(ModifyEvent arg0) {
       String text = tracker.getText();
       wizard.trackerURL = text;
       boolean valid = true;
       String errorMessage = "";
       try{
         new URL(text);
       } catch(MalformedURLException e) {
         valid = false;
         errorMessage = MessageText.getString("wizard.invalidurl");
       }
       wizard.setErrorMessage(errorMessage);
       wizard.next.setEnabled(valid);

      }
    });
    tracker.setText(wizard.trackerURL);
    

    Button bSingle = new Button(panel, SWT.RADIO);
    bSingle.setSelection(!wizard.mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bSingle.setLayoutData(gridData);
    Messages.setLanguageText(bSingle, "wizard.singlefile");

    Button bDirectory = new Button(panel, SWT.RADIO);
    bDirectory.setSelection(wizard.mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bDirectory.setLayoutData(gridData);
    Messages.setLanguageText(bDirectory, "wizard.directory");

    bSingle.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        wizard.setCurrentInfo(MessageText.getString("wizard.singlefile.help"));
        wizard.mode = false;
      }
    });

    bDirectory.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        wizard.setCurrentInfo(MessageText.getString("wizard.directory.help"));
        wizard.mode = true;
      }
    });
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#getNextPanel()
   */
  public IWizardPanel getNextPanel() {
    if(wizard.mode) {
      return new DirectoryPanel(wizard,this);
    } else {
      return new SingleFilePanel(wizard,this);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#isNextEnabled()
   */
  public boolean isNextEnabled() {
    // TODO Auto-generated method stub
    return true;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#isFinishEnabled()
   */
  public boolean isFinishEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#finish()
   */
  public void finish() {
    // TODO Auto-generated method stub

  }

}
