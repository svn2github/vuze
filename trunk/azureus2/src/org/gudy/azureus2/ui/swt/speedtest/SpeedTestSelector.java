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

package org.gudy.azureus2.ui.swt.speedtest;



import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 * 
 */
public class 
SpeedTestSelector
	extends AbstractWizardPanel<SpeedTestWizard> 
{
	private boolean	mlab_test = true;
	
  public SpeedTestSelector(SpeedTestWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.transfer.title"));
    wizard.setCurrentInfo(MessageText.getString("configureWizard.transfer2.hint"));
    final Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.transfer2.message");

    final Group gRadio = new Group(panel, SWT.NULL);
    Messages.setLanguageText(gRadio, "configureWizard.transfer2.group");
    gRadio.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gRadio.setLayout( layout );
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gRadio.setLayoutData(gridData);


    	// auto button
    
    Button auto_button = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(auto_button, "auto.mode");
    auto_button.setSelection( true );
    
    new Label( gRadio, SWT.NULL );
    
    
    label = new Label( gRadio, SWT.NULL );
    Messages.setLanguageText(label, "configureWizard.transfer2.test.info");

 
    
    	// manual
    
    final Button manual_button = new Button( gRadio, SWT.RADIO );
    Messages.setLanguageText(manual_button, "manual.mode");

    new Label( gRadio, SWT.NULL );
    
    
   manual_button.addListener(
		SWT.Selection,
    	new Listener()
		{
			public void 
			handleEvent(
				Event arg0 ) 
			{
				mlab_test = !manual_button.getSelection();
			}
		});
  }

 
  
  public boolean 
  isNextEnabled() 
  {
    return( true );
  }
  
  public boolean 
  isPreviousEnabled() 
  {
    return( false );
  }
  
  public IWizardPanel 
  getNextPanel() 
  {
    if ( mlab_test ){
    	
    	wizard.close();
    	
    	return( null );
    	
    }else{

    	return( new SpeedTestPanel( wizard, null ));
    }
  }

}
