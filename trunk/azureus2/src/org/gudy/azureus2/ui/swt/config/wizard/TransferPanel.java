/*
 * File    : TransferPanel.java
 * Created : 12 oct. 2003 19:41:14
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

package org.gudy.azureus2.ui.swt.config.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 * 
 */
public class TransferPanel extends AbstractWizardPanel {

  public TransferPanel(ConfigureWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.transfer.title"));
    wizard.setCurrentInfo(MessageText.getString("configureWizard.transfer.hint"));
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

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.widthHint = 380;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.transfer.message");

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.connection");

    final Combo connections = new Combo(panel, SWT.SINGLE | SWT.READ_ONLY);
    for (int i = 0; i < 5; i++) {
      connections.add(MessageText.getString("configureWizard.transfer.connection." + i));
    }
    connections.select(0);

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.maxUpSpeed");

    final Text textMaxUpSpeed = new Text(panel, SWT.BORDER);

    connections.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        int index = connections.getSelectionIndex();
        if (index == 0) {
          textMaxUpSpeed.setEnabled(true);
        }
        else {
          textMaxUpSpeed.setEnabled(false);
          int upSpeeds[] = { 0, 5, 13, 28, 43 };
          textMaxUpSpeed.setText("" + upSpeeds[index]);
        }
      }
    });

    textMaxUpSpeed.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        int maxUp = 0;
        try {
          maxUp = Integer.parseInt(textMaxUpSpeed.getText());
        } catch (Exception e) {}
        if(maxUp < 5 && maxUp > 0) {
          wizard.setErrorMessage(MessageText.getString("configureWizard.transfer.uploadTooLow"));
          wizard.setNextEnabled(false);
        } else {
          wizard.setErrorMessage("");
          wizard.setNextEnabled(true);
          computeAll(maxUp);
        }
        
      }
    });

    textMaxUpSpeed.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event event) {
        String text = event.text;
        char[] chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            event.doit = false;
            return;
          }
        }
      }
    });

  }

  public void computeAll(int maxUploadSpeed) {}

}
