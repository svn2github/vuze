/*
 * Created on 9 sept. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    label.setText(MessageText.getString("TrackerChangerWindow.newtracker"));    
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 200;
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);

    final Text url = new Text(shell, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    url.setLayoutData(gridData);
    url.setText("http://");

    Button ok = new Button(shell, SWT.PUSH);
    ok.setText(MessageText.getString("TrackerChangerWindow.ok"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        try {
          trackerConnection.setTrackerUrl(url.getText());
          shell.dispose();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    Button cancel = new Button(shell, SWT.PUSH);
    cancel.setText(MessageText.getString("TrackerChangerWindow.cancel"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
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
    shell.open();
  }
}
