/*
 * File    : WelcomePanel.java
 * Created : 12 oct. 2003 16:05:59
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 * 
 */
public class WelcomePanel extends AbstractWizardPanel {


  public WelcomePanel(ConfigureWizard wizard,IWizardPanel previous) {
    super(wizard,previous);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.welcome.title"));
    
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);
  
    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 1;
    panel.setLayout(layout);     

    Label label = new Label(panel,SWT.WRAP);
    gridData = new GridData();
    gridData.widthHint = 380;    
    label.setLayoutData(gridData);
    Messages.setLanguageText(label,"configureWizard.welcome.message");
  }
  
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel#isNextEnabled()
   */
  public boolean isNextEnabled() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel#getNextPanel()
   */
  public IWizardPanel getNextPanel() {
    return new TransferPanel(((ConfigureWizard)wizard),this);
  }

}
