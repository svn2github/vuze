/*
 * File    : SavePathPanel.java
 * Created : 30 sept. 2003 17:06:45
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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class SavePathPanel extends AbstractWizardPanel {


  public SavePathPanel(Wizard wizard,AbstractWizardPanel previousPanel) {
    super(wizard,previousPanel);
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("wizard.torrentFile"));
    wizard.setCurrentInfo(MessageText.getString("wizard.choosetorrent"));
    Composite panel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);
    Label label = new Label(panel,SWT.NULL);    
    Messages.setLanguageText(label,"wizard.file");
    final Text file = new Text(panel,SWT.BORDER);
    
    file.addModifyListener(new ModifyListener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
       */
      public void modifyText(ModifyEvent arg0) {       
        String fName = file.getText();
        wizard.savePath = fName;
        String error = "";
        if(! fName.equals("")) {          
          File f = new File(file.getText());
          if(f.exists() || f.isDirectory()) {
            error = MessageText.getString("wizard.invalidfile");
          }
        }
        wizard.setErrorMessage(error);
        wizard.finish.setEnabled(!wizard.savePath.equals("") && error.equals(""));
      }
    });
    if(wizard.mode) {
      wizard.savePath = wizard.directoryPath + ".torrent";
    } else {      
      wizard.savePath = wizard.singlePath + ".torrent";
    }
    file.setText(wizard.savePath);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    file.setLayoutData(gridData);
    Button browse = new Button(panel,SWT.PUSH);
    browse.addListener(SWT.Selection,new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        FileDialog fd = new FileDialog(wizard.wizardWindow,SWT.SAVE);
        if(wizard.errorMessage.getText().equals("") && !wizard.savePath.equals("")) {
          fd.setFileName(wizard.savePath);
        }
        String f = fd.open();
        if(f != null)
          file.setText(f);      

      }
    });   
    Messages.setLanguageText(browse,"wizard.browse");

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#getNextPanel()
   */
  public AbstractWizardPanel getNextPanel() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#isNextEnabled()
   */
  public boolean isNextEnabled() {
    // TODO Auto-generated method stub
    return false;
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
