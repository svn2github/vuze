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
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.*;

/**
 * @author Olivier
 * 
 */
public class ModePanel extends AbstractWizardPanel {

  public ModePanel(NewTorrentWizard wizard, AbstractWizardPanel previous) {
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

    Composite panel = new Composite(rootPanel, SWT.NO_RADIO_GROUP);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);

    final Button btnLocalTracker = new Button(panel,SWT.RADIO);
    Label labelLocalTracker = new Label(panel,SWT.NULL);
    Messages.setLanguageText(labelLocalTracker,"wizard.tracker.local");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    labelLocalTracker.setLayoutData(gridData);
    
    String localTrackerHost = COConfigurationManager.getStringParameter("Tracker IP","");
    int localTrackePort = COConfigurationManager.getIntParameter("Tracker Port",6969);
    final String localTrackerUrl;
    
    Label localTrackerValue = new Label(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    localTrackerValue.setLayoutData(gridData);    
    
    final Button btnExternalTracker = new Button(panel,SWT.RADIO);
    Label label = new Label(panel,SWT.NULL);
    Messages.setLanguageText(label,"wizard.tracker.external");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
          
    if(localTrackerHost != null && !localTrackerHost.equals("")) {
      localTrackerUrl = "http://" + localTrackerHost + ":" + localTrackePort + "/announce";
      localTrackerValue.setText("\t" + localTrackerUrl);        
    } else {    
      localTrackerUrl = "";
      Messages.setLanguageText(localTrackerValue,"wizard.tracker.howToLocal");
      btnLocalTracker.setSelection(false);
      btnLocalTracker.setEnabled(false);
      localTrackerValue.setEnabled(false);
      labelLocalTracker.setEnabled(false);      
      ((NewTorrentWizard)wizard).localTracker = false;
    }
    
    btnLocalTracker.setSelection(((NewTorrentWizard)wizard).localTracker);
    btnExternalTracker.setSelection(!((NewTorrentWizard)wizard).localTracker);
    
    label = new Label(panel,SWT.NULL);
    Messages.setLanguageText(label,"wizard.announceUrl");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
        
    final Combo tracker = new Combo(panel,SWT.NULL);
    List trackers = TrackersUtil.getInstance().getTrackersList();
    Iterator iter = trackers.iterator();
    while(iter.hasNext()) {
      tracker.add((String)iter.next());
    }
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    tracker.setLayoutData(gridData);
    tracker.addModifyListener(new ModifyListener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
       */
      public void modifyText(ModifyEvent arg0) {
       String text = tracker.getText();
       ((NewTorrentWizard)wizard).trackerURL = text;
       boolean valid = true;
       String errorMessage = "";
       try{
         new URL(text);
       } catch(MalformedURLException e) {
         valid = false;
         errorMessage = MessageText.getString("wizard.invalidurl");
       }
       wizard.setErrorMessage(errorMessage);
       wizard.setNextEnabled(valid);

      }
    });
    tracker.setText(((NewTorrentWizard)wizard).trackerURL);
    tracker.setEnabled(!((NewTorrentWizard)wizard).localTracker);
    label = new Label(panel,SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);

    final Button bSingle = new Button(panel, SWT.RADIO);
    bSingle.setSelection(!((NewTorrentWizard)wizard).mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bSingle.setLayoutData(gridData);
    Messages.setLanguageText(bSingle, "wizard.singlefile");

    final Button bDirectory = new Button(panel, SWT.RADIO);
    bDirectory.setSelection(((NewTorrentWizard)wizard).mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bDirectory.setLayoutData(gridData);
    Messages.setLanguageText(bDirectory, "wizard.directory");

    bSingle.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        wizard.setCurrentInfo(MessageText.getString("wizard.singlefile.help"));
        ((NewTorrentWizard)wizard).mode = false;
        bDirectory.setSelection(false);
        bSingle.setSelection(true);
      }
    });

    bDirectory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        wizard.setCurrentInfo(MessageText.getString("wizard.directory.help"));
        ((NewTorrentWizard)wizard).mode = true;
        bSingle.setSelection(false);
        bDirectory.setSelection(true);
      }
    });
    
    btnLocalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {              
        ((NewTorrentWizard)wizard).localTracker = true;
        ((NewTorrentWizard)wizard).trackerURL = localTrackerUrl;
        btnExternalTracker.setSelection(false);
        btnLocalTracker.setSelection(true);
        tracker.setEnabled(false);
      }
    });
    
    btnExternalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {              
        ((NewTorrentWizard)wizard).localTracker = false;
        btnLocalTracker.setSelection(false);
        btnExternalTracker.setSelection(true);
        tracker.setEnabled(true);
      }
    });
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#getNextPanel()
   */
  public IWizardPanel getNextPanel() {
    if(((NewTorrentWizard)wizard).mode) {
      return new DirectoryPanel(((NewTorrentWizard)wizard),this);
    } else {
      return new SingleFilePanel(((NewTorrentWizard)wizard),this);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#isNextEnabled()
   */
  public boolean isNextEnabled() {
    // TODO Auto-generated method stub
    return true;
  }

}
