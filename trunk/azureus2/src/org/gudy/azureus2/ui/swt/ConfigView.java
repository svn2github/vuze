/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core.BooleanParameter;
import org.gudy.azureus2.core.ConfigurationManager;

/**
 * @author Olivier
 * 
 */
public class ConfigView implements IView {

  private static final int upRates[] =
    {
      0,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      20,
      25,
      30,
      35,
      40,
      45,
      50,
      60,
      70,
      80,
      90,
      100,
      150,
      200,
      250,
      300,
      350,
      400,
      450,
      500,
      600,
      700,
      800,
      900,
      1000 };

  Composite gConfig;
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    final ConfigurationManager config = ConfigurationManager.getInstance();
    gConfig = new Composite(composite, SWT.NULL);
    GridLayout configLayout = new GridLayout();
    configLayout.numColumns = 2;
    gConfig.setLayout(configLayout);
    Group gFile = new Group(gConfig, SWT.NULL);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gFile.setLayoutData(gridData);

    gFile.setText("Files");
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    gFile.setLayout(layout);
    new Label(gFile, SWT.NULL).setText("Use Fast Resume Mode");
    new BooleanParameter(gFile, "Use Resume", false);
    new Label(gFile, SWT.NULL);

    new Label(gFile, SWT.NULL).setText("Allocate new files");
    new BooleanParameter(gFile, "Allocate New", true);
    new Label(gFile, SWT.NULL);

    new Label(gFile, SWT.NULL).setText("Default save path");
    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter pathParameter = new StringParameter(gFile, "Default save path", "");
    pathParameter.setLayoutData(gridData);
    Button browse = new Button(gFile, SWT.PUSH);
    browse.setText("Browse...");
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(gConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText("Please Choose the default save directory");
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });

    Group gServer = new Group(gConfig, SWT.NULL);
    gServer.setText("Server");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gServer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gServer.setLayout(layout);

    new Label(gServer, SWT.NULL).setText("Override IP address sent to tracker");
    gridData = new GridData();
    gridData.widthHint = 100;
    new StringParameter(gServer, "Override Ip", "").setLayoutData(gridData);

    new Label(gServer, SWT.NULL).setText("Servers lowest port");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "Low Port", 6881).setLayoutData(gridData);

    new Label(gServer, SWT.NULL).setText("Servers highest port");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "High Port", 6889).setLayoutData(gridData);

    Group gGlobal = new Group(gConfig, SWT.NULL);
    gGlobal.setText("Global");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gGlobal.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gGlobal.setLayout(layout);

    new Label(gGlobal, SWT.NULL).setText("Disconnect seeds when seed");
    new BooleanParameter(gGlobal, "Disconnect Seed", false);

    new Label(gGlobal, SWT.NULL).setText("Auto-Switch to low-priority when seeding");
    new BooleanParameter(gGlobal, "Switch Priority", true);

    new Label(gGlobal, SWT.NULL).setText(
      "Max active torrents (0 : unlimited)\nNew torrents won't start if you are downloading/seeding more");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gGlobal, "max active torrents", 4).setLayoutData(gridData);

    Group gTransfer = new Group(gConfig, SWT.NULL);
    gTransfer.setText("Transfer");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gTransfer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gTransfer.setLayout(layout);

    new Label(gTransfer, SWT.NULL).setText("Maximum number of connections per torrent (0 : unlimited) ");
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(gTransfer, "Max Clients", 0).setLayoutData(gridData);

    new Label(gTransfer, SWT.NULL).setText("Default max uploads per torrent");
    final String upLabels[] = new String[99];
    final int upValues[] = new int[99];
    for (int i = 0; i < 99; i++) {
      upLabels[i] = " " + (i + 2);
      upValues[i] = i + 2;
    }
    new IntListParameter(gTransfer, "Max Uploads", 4, upLabels, upValues);

    new Label(gTransfer, SWT.NULL).setText("Max Upload Speed (globally)");
    final String upsLabels[] = new String[upRates.length];
    final int upsValues[] = new int[upRates.length];
    upsLabels[0] = "Unlimited";
    upsValues[0] = 0;
    for (int i = 1; i < upRates.length; i++) {
      upsLabels[i] = " " + upRates[i] + "kB/s";
      upsValues[i] = 1024 * upRates[i];
    }
    new IntListParameter(gTransfer, "Max Upload Speed", 0, upsLabels, upsValues);

    Group gDisplay = new Group(gConfig, SWT.NULL);
    gDisplay.setText("Display");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gDisplay.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gDisplay.setLayout(layout);
    new Label(gDisplay, SWT.NULL).setText("Auto open details tab");
    new BooleanParameter(gDisplay, "Open Details", true);
    
    new Label(gDisplay, SWT.NULL).setText("Auto open download bar");
    new BooleanParameter(gDisplay, "Open Bar", false);
    

    Button enter = new Button(gConfig, SWT.PUSH);
    enter.setText("Save");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
    gridData.horizontalSpan = 2;
    enter.setLayoutData(gridData);

    enter.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        config.setParameter("updated", 1);
        config.save();
      }
    });

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return gConfig;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().setConfig(null);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return "Configuration";
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return "Configuration";
  }

}
