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
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 *  
 */
public class ModePanel extends AbstractWizardPanel {

  private Button bSingle;
  private Button bDirectory;
  private Combo tracker;

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
    layout.numColumns = 4;
    panel.setLayout(layout);

    //Line :
    // O use embedded tracker []Use SSL
    
    final Button btnLocalTracker = new Button(panel, SWT.RADIO);   
    Messages.setLanguageText(btnLocalTracker, "wizard.tracker.local");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    btnLocalTracker.setLayoutData(gridData);    

    final Button btnSSL = new Button(panel, SWT.CHECK);
    Messages.setLanguageText(btnSSL, "wizard.tracker.ssl"); 
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.horizontalSpan = 2;
    btnSSL.setLayoutData( gridData );
        
    //Line :
    //Announce URL : <local announce>
    
    final Label labelLocalAnnounce = new Label(panel, SWT.NULL);
    Messages.setLanguageText(labelLocalAnnounce, "wizard.announceUrl");
    
    final Label localTrackerValue = new Label(panel, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    localTrackerValue.setLayoutData(gridData);
    
    //Line:
    //<blank>
    Label label = new Label(panel,SWT.NULL);
    label.setText(" ");
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);

    //Line:
    // O use external Tracker
    
    final Button btnExternalTracker = new Button(panel, SWT.RADIO);
    Messages.setLanguageText(btnExternalTracker, "wizard.tracker.external");
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    btnExternalTracker.setLayoutData(gridData);

    //Line:
    // [External Tracker Url ]V
    
    final String localTrackerHost = COConfigurationManager.getStringParameter("Tracker IP", "");
    final int localTrackerPort 	= COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
    final int localTrackerPortSSL = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
    final boolean SSLEnabled = COConfigurationManager.getBooleanParameter("Tracker Port SSL Enable", false );
    
    final String[] localTrackerUrl = new String[1];    
    
    if (localTrackerHost != null && !localTrackerHost.equals("")) {
      localTrackerUrl[0] = "http://" + localTrackerHost + ":" + localTrackerPort + "/announce";
      localTrackerValue.setText(localTrackerUrl[0]);
	  btnSSL.setEnabled( SSLEnabled );
    } else {
      localTrackerUrl[0] = "";
      Messages.setLanguageText(localTrackerValue, "wizard.tracker.howToLocal");
      btnLocalTracker.setSelection(false);
	  btnSSL.setEnabled(false);
      btnLocalTracker.setEnabled(false);
      localTrackerValue.setEnabled(false);
      ((NewTorrentWizard) wizard).localTracker = false;
    }

    if (((NewTorrentWizard) wizard).localTracker) {
      setTrackerUrl(localTrackerUrl[0]);
    }

    final Label labelExternalAnnounce = new Label(panel, SWT.NULL);
    Messages.setLanguageText(labelExternalAnnounce, "wizard.announceUrl");
    
    btnLocalTracker.setSelection(((NewTorrentWizard) wizard).localTracker);
    localTrackerValue.setEnabled(((NewTorrentWizard) wizard).localTracker);
    labelLocalAnnounce.setEnabled(((NewTorrentWizard) wizard).localTracker);
    btnSSL.setEnabled(((NewTorrentWizard) wizard).localTracker);
    
    btnExternalTracker.setSelection(!((NewTorrentWizard) wizard).localTracker);
    labelExternalAnnounce.setEnabled(!((NewTorrentWizard) wizard).localTracker);

    tracker = new Combo(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    tracker.setLayoutData(gridData);
    List trackers = TrackersUtil.getInstance().getTrackersList();
    Iterator iter = trackers.iterator();
    while (iter.hasNext()) {
      tracker.add((String) iter.next());
    }
    
    tracker.addModifyListener(new ModifyListener() {
      /*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
      public void modifyText(ModifyEvent arg0) {
        String text = tracker.getText();        
        setTrackerUrl(text);
        
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
    
    tracker.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        String text = tracker.getText();        
        setTrackerUrl(text);
        
        boolean valid = true;
        String errorMessage = "";
        try {
          new URL(text);
        } catch (MalformedURLException ex) {
          valid = false;
          errorMessage = MessageText.getString("wizard.invalidurl");
        }
        wizard.setErrorMessage(errorMessage);
        wizard.setNextEnabled(valid);
      }
    });
    updateTrackerURL();
    tracker.setEnabled(!((NewTorrentWizard) wizard).localTracker);
    
    new Label(panel,SWT.NULL);
    
    //Line:
    // [] add Multi-tracker information
    
    final Button btnMultiTracker = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(btnMultiTracker, "wizard.multitracker");
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    btnMultiTracker.setLayoutData(gridData);
    btnMultiTracker.addListener(SWT.Selection, new Listener() {

	    public void handleEvent(Event arg0) {
	      ((NewTorrentWizard) wizard).useMultiTracker = btnMultiTracker.getSelection();
	    }
    });
    btnMultiTracker.setSelection(((NewTorrentWizard) wizard).useMultiTracker);
    //Line:
    // include hashes for other networks (
    
    final Button btnExtraHashes = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(btnExtraHashes, "wizard.createtorrent.extrahashes");
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    btnExtraHashes.setLayoutData(gridData);
    btnExtraHashes.addListener(SWT.Selection, new Listener() {

    	public void handleEvent(Event arg0) {
    		((NewTorrentWizard) wizard).addOtherHashes = btnExtraHashes.getSelection();
    	}
    });
    btnExtraHashes.setSelection(((NewTorrentWizard) wizard).addOtherHashes);
    
    //Line:
    // ------------------------------
    
    label = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);

    //Line:
    // O single file
    bSingle = new Button(panel, SWT.RADIO);
    bSingle.setSelection(!((NewTorrentWizard) wizard).create_from_dir);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    bSingle.setLayoutData(gridData);
    Messages.setLanguageText(bSingle, "wizard.singlefile");
    
    bSingle.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        activateMode(true);
      }
    });
    
    //Line:
    // O Directory mode
    
    bDirectory = new Button(panel, SWT.RADIO);
    bDirectory.setSelection(((NewTorrentWizard) wizard).create_from_dir);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    bDirectory.setLayoutData(gridData);
    Messages.setLanguageText(bDirectory, "wizard.directory");
    
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
			
			localTrackerValue.setText(url);
			
			localTrackerUrl[0] = url;
			
			setTrackerUrl(url);
	
		  }
		});
	
    btnLocalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        ((NewTorrentWizard) wizard).localTracker = true;
        setTrackerUrl(localTrackerUrl[0]);
        btnExternalTracker.setSelection(false);
        btnLocalTracker.setSelection(true);
        tracker.setEnabled(false);
        btnSSL.setEnabled(SSLEnabled);
        labelLocalAnnounce.setEnabled(true);
        localTrackerValue.setEnabled(true);
        labelExternalAnnounce.setEnabled(false);
      }
    });

    btnExternalTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        ((NewTorrentWizard) wizard).localTracker = false;
        setTrackerUrl(tracker.getText());
        btnLocalTracker.setSelection(false);
        btnExternalTracker.setSelection(true);
        tracker.setEnabled(true);
        btnSSL.setEnabled(false);
        labelLocalAnnounce.setEnabled(false);
        localTrackerValue.setEnabled(false);
        labelExternalAnnounce.setEnabled(true);
      }
    });
    
    //Line:
    // ------------------------------
    
    label = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);

    //Line:
    //Comment: [               ]
    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "wizard.comment");

    final Text comment = new Text(panel, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    comment.setLayoutData(gridData);
    comment.setText("");

    comment.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        ((NewTorrentWizard) wizard).setComment(comment.getText());
      }
    });

    label = new Label(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
    label.setText("\n");

    label = new Label(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
    label.setForeground(Colors.blue);
    Messages.setLanguageText(label, "wizard.hint.mode");
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#getNextPanel()
	 */
  public IWizardPanel getNextPanel() {
    
    //OSX work-arround to Fix SWT BUG #43396 :
    //Combo doesn't fire Selection Event
    if(Constants.isOSX) {
      //In case we're not using the localTracker, refresh the
      //Tracker URL from the Combo text
      if( ! ((NewTorrentWizard) wizard).localTracker) {
        setTrackerUrl(tracker.getText());
      }
    }
    
    
    if(((NewTorrentWizard) wizard).useMultiTracker)
      return new MultiTrackerPanel((NewTorrentWizard) wizard, this);

    if (((NewTorrentWizard) wizard).create_from_dir) {
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
    ((NewTorrentWizard) wizard).create_from_dir = !singleFile;
    bDirectory.setSelection(!singleFile);
    bSingle.setSelection(singleFile);
  }

  void updateTrackerURL() {
    tracker.setText(((NewTorrentWizard) wizard).trackerURL);
  }
  
  void setTrackerUrl(String url) {
    ((NewTorrentWizard) wizard).trackerURL = url;
    String config = ((NewTorrentWizard) wizard).multiTrackerConfig;
    if(config.equals("")) {
	    List list = (List) ((NewTorrentWizard) wizard).trackers.get(0);
	    if(list.size() > 0)
	      list.remove(0);
	    list.add(url);
    }
  }
}
