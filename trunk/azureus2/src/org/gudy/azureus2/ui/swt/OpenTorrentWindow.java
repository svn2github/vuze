/*
 * OpenTorrentWindow.java
 *
 * Created on February 23, 2004, 4:09 PM
 */

package org.gudy.azureus2.ui.swt;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.Messages;


public class OpenTorrentWindow {

  private boolean bUseFolderBrowse = false;
  private Text lTorrentInfo;
  private Text txtTorrent;
  private Shell shell;
  
  public OpenTorrentWindow(final Display display, GlobalManager gm) {
    GridData gridData;
    Label label;
    shell = new Shell(display, SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    
    shell.setText(MessageText.getString("OpenTorrentWindow.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);
		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				shell.layout();
			}
		});

    label = new Label(shell, SWT.BORDER | SWT.WRAP);
    Messages.setLanguageText(label, "OpenTorrentWindow.message");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);

    label = new Label(shell, SWT.NONE);
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "OpenTorrentWindow.torrentLocation");
    txtTorrent = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = 50;
    gridData.widthHint = 450;
    txtTorrent.setLayoutData(gridData);
    Button browseTorrent = new Button(shell, SWT.PUSH);
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    browseTorrent.setLayoutData(gridData);
    Messages.setLanguageText(browseTorrent, "OpenTorrentWindow.addFiles");

    browseTorrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        FileDialog fDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor", "*.*" });
        fDialog.setFilterNames(new String[] { "*.torrent", "*.tor", "*.*" });
        fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
        String fileName = fDialog.open();
        if (fileName != null) {
          String[] sTorrentFilenames = fDialog.getFileNames();
          String sTorrentFilePath = fDialog.getFilterPath();
          String sTorrentFiles = sTorrentFilePath + File.separator + sTorrentFilenames[0];
          for (int i = 1; i < sTorrentFilenames.length; i++) {
            sTorrentFiles += '\n' + sTorrentFilePath + File.separator + sTorrentFilenames[i];
          }
          txtTorrent.setText(sTorrentFiles);
          checkTorrentFiles();
        }
      }
    });
    
    lTorrentInfo = new Text(shell, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
    lTorrentInfo.setVisible(false);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.heightHint = 0;
    gridData.horizontalSpan = 3;
    lTorrentInfo.setLayoutData(gridData);
    lTorrentInfo.setBackground(label.getBackground());
    lTorrentInfo.setForeground(label.getForeground());

    label = new Label(shell, SWT.NONE);
    Messages.setLanguageText(label, "OpenTorrentWindow.dataLocation");
    final Text txtData = new Text(shell, SWT.BORDER);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    txtData.setLayoutData(gridData);
    Button browseData = new Button(shell, SWT.PUSH);
    Messages.setLanguageText(browseData, "ConfigView.button.browse");

    browseData.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        String sSavePath;
        String sDefPath = COConfigurationManager.getBooleanParameter("Use default data dir") ?
                          COConfigurationManager.getStringParameter("Default save path", "") :
                          "";

        if ( sDefPath.length() > 0 ){
    	    File	f = new File(sDefPath);
    	    
    	    if ( !f.exists()){
    	    	f.mkdirs();
    	    }
        }
        
        if (bUseFolderBrowse) {
          DirectoryDialog dDialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
          dDialog.setFilterPath(sDefPath);
          dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath"));
          sSavePath = dDialog.open();
        } else {
          FileDialog fDialog = new FileDialog(shell, SWT.SYSTEM_MODAL);
          fDialog.setFilterPath(sDefPath);
          fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
          sSavePath = fDialog.open();
        }
        if (sSavePath != null) {
          txtData.setText(sSavePath);
        }
      }
    });

    Composite cArea = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 0;
    cArea.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    cArea.setLayoutData(gridData);

    Group gStartModes = new Group(cArea, SWT.NULL);
    gStartModes.setLayout(new GridLayout());
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gStartModes.setLayoutData(gridData);
    String[] startModes = { "queued", "stopped", "forceStarted" };
    Messages.setLanguageText(gStartModes, "OpenTorrentWindow.startMode");
    for (int i = 0; i < startModes.length; i++) {
      Button radioButton = new Button(gStartModes, SWT.RADIO);
      Messages.setLanguageText(radioButton, "OpenTorrentWindow.startMode." + startModes[i]);
      gridData = new GridData();
      radioButton.setLayoutData(gridData);
      if (i == 0)
        radioButton.setSelection(true);
    }

    Group gQueueLocations = new Group(cArea, SWT.NULL);
    gQueueLocations.setLayout(new GridLayout());
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gQueueLocations.setLayoutData(gridData);
    Messages.setLanguageText(gQueueLocations , "OpenTorrentWindow.addPosition");
    String[] queueLocations = { "first", "last" };
    for (int i = 0; i < queueLocations.length; i++) {
      Button radioButton = new Button(gQueueLocations, SWT.RADIO);
      Messages.setLanguageText(radioButton, "OpenTorrentWindow.addPosition." + queueLocations[i]);
      gridData = new GridData();
      radioButton.setLayoutData(gridData);
      if (i == 0)
        radioButton.setSelection(true);
    }

    cArea = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 0;
    cArea.setLayout(layout);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.horizontalSpan = 3;
    cArea.setLayoutData(gridData);
    
    Button ok = new Button(cArea, SWT.PUSH);
    ok.setEnabled(false);
    Messages.setLanguageText(ok, "Button.ok");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    Button cancel = new Button(cArea, SWT.PUSH);
    Messages.setLanguageText(cancel, "Button.cancel");
    gridData = new GridData();
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    shell.pack();
    shell.open();

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }
  
  private void checkTorrentFiles() {
    String[] sFileNames = txtTorrent.getText().split("\r\n");
    String sMessages = "";
    int numFiles = 0;
    for (int i = 0; i < sFileNames.length; i ++) {
      if (sFileNames[i] != "") {
        numFiles++;
        try {
          TOTorrent torrent = TorrentUtils.readFromFile( new File(sFileNames[i]), false);
          if (!bUseFolderBrowse && !torrent.isSimpleTorrent()) {
            bUseFolderBrowse = true;
          }
        } catch (Exception e) {
          sMessages += sFileNames[i] + ": " + e.getMessage() + "\n";
        }
      }
    }
    if (!bUseFolderBrowse && numFiles > 1)
      bUseFolderBrowse = true;
    lTorrentInfo.setText(sMessages);
    lTorrentInfo.setVisible((sMessages != ""));
    GridData gd = (GridData)lTorrentInfo.getLayoutData();
    gd.heightHint = (sMessages == "") ? 0 : 50;
    shell.layout();
  }
}
