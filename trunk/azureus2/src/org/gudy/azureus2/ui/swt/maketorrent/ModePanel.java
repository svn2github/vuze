/*
 * File : ModePanel.java Created : 30 sept. 2003 01:51:05 By : Olivier
 * 
 * Azureus - a Java Bittorrent client
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details ( see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
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
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 *  
 */
public class ModePanel extends AbstractWizardPanel {

  private Button bSingle;
  private Button bDirectory;

  public ModePanel(NewTorrentWizard wizard, AbstractWizardPanel previous) {
    super(wizard, previous);
  }

  /*
	 * (non-Javadoc)
	 * 
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

    final Button btnLocalTracker = new Button(panel, SWT.RADIO);
    Label labelLocalTracker = new Label(panel, SWT.NULL);
    Messages.setLanguageText(labelLocalTracker, "wizard.tracker.local");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    labelLocalTracker.setLayoutData(gridData);

	Label label = new Label(panel, SWT.NULL);
	label = new Label(panel, SWT.NULL);
	Messages.setLanguageText(label, "wizard.tracker.ssl"); 
    
	final Button btnSSL = new Button(panel, SWT.CHECK);
	gridData = new GridData();
	gridData.horizontalSpan = 1;
	btnSSL.setLayoutData( gridData );

    final String localTrackerHost = COConfigurationManager.getStringParameter("Tracker IP", "");
	final int localTrackerPort 	= COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
	final int localTrackerPortSSL = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
	
    final String[] localTrackerUrl = new String[1];

    final Label localTrackerValue = new Label(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    localTrackerValue.setLayoutData(gridData);

    final Button btnExternalTracker = new Button(panel, SWT.RADIO);
    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "wizard.tracker.external");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);


    if (localTrackerHost != null && !localTrackerHost.equals("")) {
      localTrackerUrl[0] = "http://" + localTrackerHost + ":" + localTrackerPort + "/announce";
      localTrackerValue.setText("\t" + localTrackerUrl[0]);
	  btnSSL.setEnabled( true );
    } else {
      localTrackerUrl[0] = "";
      Messages.setLanguageText(localTrackerValue, "wizard.tracker.howToLocal");
      btnLocalTracker.setSelection(false);
	  btnSSL.setEnabled(false);
      btnLocalTracker.setEnabled(false);
      localTrackerValue.setEnabled(false);
      labelLocalTracker.setEnabled(false);
      ((NewTorrentWizard) wizard).localTracker = false;
    }

    if (((NewTorrentWizard) wizard).localTracker) {
      ((NewTorrentWizard) wizard).trackerURL = localTrackerUrl[0];
    }

    btnLocalTracker.setSelection(((NewTorrentWizard) wizard).localTracker);
    btnExternalTracker.setSelection(!((NewTorrentWizard) wizard).localTracker);

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "wizard.announceUrl");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);

    final Combo tracker = new Combo(panel, SWT.NULL);
    List trackers = TrackersUtil.getInstance().getTrackersList();
    Iterator iter = trackers.iterator();
    while (iter.hasNext()) {
      tracker.add((String) iter.next());
    }
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    tracker.setLayoutData(gridData);
    tracker.addModifyListener(new ModifyListener() {
      /*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
      public void modifyText(ModifyEvent arg0) {
        String text = tracker.getText();
        ((NewTorrentWizard) wizard).trackerURL = text;
        boolean valid = true;
        String errorMessage = "";
        try {
          new URL(text);
        } catch (MalformedURLException e) {
          valid = false;
          errorMessage = MessageText.getString("wizard.invalidurl");
        }
        wizard.setErrorMessage(errorMessage);
        wizard.setNextEnabled(valid);

      }
    });
    tracker.setText(((NewTorrentWizard) wizard).trackerURL);
    tracker.setEnabled(!((NewTorrentWizard) wizard).localTracker);
    label = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);

    bSingle = new Button(panel, SWT.RADIO);
    bSingle.setSelection(!((NewTorrentWizard) wizard).mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bSingle.setLayoutData(gridData);
    Messages.setLanguageText(bSingle, "wizard.singlefile");

    bDirectory = new Button(panel, SWT.RADIO);
    bDirectory.setSelection(((NewTorrentWizard) wizard).mode);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    bDirectory.setLayoutData(gridData);
    Messages.setLanguageText(bDirectory, "wizard.directory");

    bSingle.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        activateMode(true);
      }
    });

    bDirectory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        activateMode(false);
      }
    });

	btnSSL.addListener(SWT.Selection, new Listener() {
	  public void handleEvent(Event arg0) {
	  	String	url;
	  	
		if ( btnSSL.getSelection()){
			url = "https://" + localTrackerHost + ":" + localTrackerPortSSL + "/announce";
		}else{
			url = "http://" + localTrackerHost + ":" + localTrackerPort + "/announce";
		}
		
		localTrackerValue.setText("\t" + url );
		
		localTrackerUrl[0] = url;
		
		((NewTorrentWizard) wizard).trackerURL = url;

	  }
	});
	
    btnLocalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        ((NewTorrentWizard) wizard).localTracker = true;
        ((NewTorrentWizard) wizard).trackerURL = localTrackerUrl[0];
        btnExternalTracker.setSelection(false);
        btnLocalTracker.setSelection(true);
        tracker.setEnabled(false);
        btnSSL.setEnabled(true);
      }
    });

    btnExternalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        ((NewTorrentWizard) wizard).localTracker = false;
		((NewTorrentWizard) wizard).trackerURL = tracker.getText();
        btnLocalTracker.setSelection(false);
        btnExternalTracker.setSelection(true);
        tracker.setEnabled(true);
        btnSSL.setEnabled(false);
      }
    });

    label = new Label(panel, SWT.NULL);
    label = new Label(panel, SWT.NULL);

    Messages.setLanguageText(label, "wizard.comment");

    final Text comment = new Text(panel, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    comment.setLayoutData(gridData);
    comment.setText("");

    comment.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        ((NewTorrentWizard) wizard).setComment(comment.getText());
      }
    });

    label = new Label(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    label.setText("\n");

    label = new Label(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    label.setForeground(MainWindow.blue);
    Messages.setLanguageText(label, "wizard.hint.mode");
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#getNextPanel()
	 */
  public IWizardPanel getNextPanel() {
    if (((NewTorrentWizard) wizard).mode) {
      return new DirectoryPanel(((NewTorrentWizard) wizard), this);
    } else {
      return new SingleFilePanel(((NewTorrentWizard) wizard), this);
    }
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#isNextEnabled()
	 */
  public boolean isNextEnabled() {
    // TODO Auto-generated method stub
    return true;
  }

  void activateMode(boolean singleFile) {
    wizard.setCurrentInfo(MessageText.getString(singleFile ? "wizard.singlefile.help" : "wizard.directory.help"));
    ((NewTorrentWizard) wizard).mode = !singleFile;
    bDirectory.setSelection(!singleFile);
    bSingle.setSelection(singleFile);
  }

}
