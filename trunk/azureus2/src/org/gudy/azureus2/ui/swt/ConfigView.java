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
import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.MessageText;

/**
 * @author Olivier
 * 
 */
public class ConfigView extends AbstractIView {

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
    Messages.setLanguageText(gFile, "ConfigView.section.files"); //$NON-NLS-1$
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gFile.setLayoutData(gridData);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    gFile.setLayout(layout);
    Label label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.usefastresume"); //$NON-NLS-1$

    new BooleanParameter(gFile, "Use Resume", false); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.allocatenewfiles"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Allocate New", true); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.defaultsavepath"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter pathParameter = new StringParameter(gFile, "Default save path", ""); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);
    Button browse = new Button(gFile, SWT.PUSH);
    Messages.setLanguageText(browse, "ConfigView.button.browse"); //$NON-NLS-1$
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(gConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });

    Group gServer = new Group(gConfig, SWT.NULL);
    Messages.setLanguageText(gServer, "ConfigView.section.server"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gServer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gServer.setLayout(layout);

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.overrideip"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 100;
    new StringParameter(gServer, "Override Ip", "").setLayoutData(gridData); //$NON-NLS-1$ //$NON-NLS-2$

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverportlow"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "Low Port", 6881).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverporthigh"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "High Port", 6889).setLayoutData(gridData); //$NON-NLS-1$

    Group gGlobal = new Group(gConfig, SWT.NULL);
    Messages.setLanguageText(gGlobal, "ConfigView.section.global"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gGlobal.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gGlobal.setLayout(layout);

    label = new Label(gGlobal, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.disconnetseed"); //$NON-NLS-1$
    new BooleanParameter(gGlobal, "Disconnect Seed", false); //$NON-NLS-1$

    label = new Label(gGlobal, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.switchpriority"); //$NON-NLS-1$
    new BooleanParameter(gGlobal, "Switch Priority", true); //$NON-NLS-1$

    label = new Label(gGlobal, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxactivetorrents"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gGlobal, "max active torrents", 4).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gGlobal, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxdownloads"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gGlobal, "max downloads", 4).setLayoutData(gridData); //$NON-NLS-1$

    Group gTransfer = new Group(gConfig, SWT.NULL);
    Messages.setLanguageText(gTransfer, "ConfigView.section.transfer"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gTransfer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gTransfer.setLayout(layout);

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxclients"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(gTransfer, "Max Clients", 0).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads"); //$NON-NLS-1$
    final String upLabels[] = new String[99];
    final int upValues[] = new int[99];
    for (int i = 0; i < 99; i++) {
      upLabels[i] = " " + (i + 2); //$NON-NLS-1$
      upValues[i] = i + 2;
    }
    new IntListParameter(gTransfer, "Max Uploads", 4, upLabels, upValues); //$NON-NLS-1$

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed"); //$NON-NLS-1$
    final String upsLabels[] = new String[upRates.length];
    final int upsValues[] = new int[upRates.length];
    upsLabels[0] = MessageText.getString("ConfigView.unlimited"); //$NON-NLS-1$
    upsValues[0] = 0;
    for (int i = 1; i < upRates.length; i++) {
      upsLabels[i] = " " + upRates[i] + "kB/s"; //$NON-NLS-1$ //$NON-NLS-2$
      upsValues[i] = 1024 * upRates[i];
    }
    new IntListParameter(gTransfer, "Max Upload Speed", 0, upsLabels, upsValues); //$NON-NLS-1$

    Group gDisplay = new Group(gConfig, SWT.NULL);
    Messages.setLanguageText(gDisplay, "ConfigView.section.display"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gDisplay.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gDisplay.setLayout(layout);

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.opendetails"); //$NON-NLS-1$
    new BooleanParameter(gDisplay, "Open Details", true); //$NON-NLS-1$

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openbar"); //$NON-NLS-1$
    new BooleanParameter(gDisplay, "Open Bar", false); //$NON-NLS-1$

    Group gStart = new Group(gConfig, SWT.NULL);
    Messages.setLanguageText(gStart, "ConfigView.section.start"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gStart.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gStart.setLayout(layout);

    label = new Label(gStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.showsplash"); //$NON-NLS-1$
    new BooleanParameter(gStart, "Show Splash", true); //$NON-NLS-1$

    label = new Label(gStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.autoupdate"); //$NON-NLS-1$
    new BooleanParameter(gStart, "Auto Update", true); //$NON-NLS-1$

    Button enter = new Button(gConfig, SWT.PUSH);
    Messages.setLanguageText(enter, "ConfigView.button.save"); //$NON-NLS-1$
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
    gridData.horizontalSpan = 2;
    enter.setLayoutData(gridData);

    enter.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        config.setParameter("updated", 1); //$NON-NLS-1$
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
    return MessageText.getString("ConfigView.title.short"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("ConfigView.title.full"); //$NON-NLS-1$
  }

}
