/*
 * Created on 9 sept. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;

/**
 * @author Olivier
 * 
 */
public class TrackerChangerWindow {
  public TrackerChangerWindow(final Display display, final TRTrackerClient trackerConnection) {
    final Shell shell = new Shell(display);
    shell.setText(MessageText.getString("TrackerChangerWindow.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    label.setText(MessageText.getString("TrackerChangerWindow.newtracker"));    
    GridData gridData = new GridData();
    gridData.widthHint = 200;
    label.setLayoutData(gridData);

    final Text url = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 300;
    url.setLayoutData(gridData);
    Utils.setTextLinkFromClipboard(shell, gridData, url);

    Composite panel = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);        
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    panel.setLayoutData(gridData);
    Button ok = new Button(panel, SWT.PUSH);
    ok.setText(MessageText.getString("Button.ok"));
    gridData = new GridData();
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        try {
        	TOTorrent	torrent = trackerConnection.getTorrent();
        	
        	TorrentUtils.announceGroupsInsertFirst( torrent, url.getText());
        	
        	TorrentUtils.writeToFile( torrent );
        	
        	trackerConnection.resetTrackerUrl(false);
        	
        	shell.dispose();
        }
        catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    });

    Button cancel = new Button(panel, SWT.PUSH);
    cancel.setText(MessageText.getString("Button.cancel"));
    gridData = new GridData();
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    shell.pack();
    Utils.createURLDropTarget(shell, url);
    shell.open();
  }
}
