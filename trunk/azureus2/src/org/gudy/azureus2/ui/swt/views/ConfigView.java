/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.IpFilter;
import org.gudy.azureus2.core.IpRange;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;

/**
 * @author Olivier
 * 
 */
public class ConfigView extends AbstractIView {

  private static final int upRates[] =
    {
      0,
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

  IpFilter filter;

  Composite cConfig;
  CTabFolder ctfConfig;
  Table table;
  Label passwordMatch;

  public ConfigView() {
    filter = IpFilter.getInstance();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    cConfig = new Composite(composite, SWT.NONE);
    GridLayout configLayout = new GridLayout();
    configLayout.marginHeight = 0;
    configLayout.marginWidth = 0;
    configLayout.numColumns = 2;
    cConfig.setLayout(configLayout);

    ctfConfig = new CTabFolder(cConfig, SWT.TOP | SWT.FLAT);
    ctfConfig.setSelectionBackground(new Color[] { MainWindow.white }, new int[0]);
    
    CTabItem itemFile = initGroupFile();
    initGroupServer();
    initGroupDownloads();
    initGroupTransfer();
    initGroupDisplay();
    initGroupIrc();
    initGroupFilter();
    
    initSaveButton(); 
       
    ctfConfig.setSelection(itemFile);
  }

  private void initSaveButton() {
    GridData gridData;
    Button save = new Button(cConfig, SWT.PUSH);
    Messages.setLanguageText(save, "ConfigView.button.save"); //$NON-NLS-1$
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
    gridData.widthHint = 80;
    save.setLayoutData(gridData);

    save.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        COConfigurationManager.setParameter("updated", 1); //$NON-NLS-1$
        COConfigurationManager.save();
        filter.save();
      }
    });
  }

  private void initGroupFilter() {
    GridData gridData;
    Label label;
    CTabItem itemFilter = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemFilter, "ipFilter.shortTitle"); //$NON-NLS-1$

    Group gFilter = new Group(ctfConfig, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gFilter.setLayoutData(gridData);

    GridLayout layoutFilter = new GridLayout();
    layoutFilter.numColumns = 3;
    gFilter.setLayout(layoutFilter);

    gridData = new GridData(GridData.BEGINNING);
    new BooleanParameter(gFilter, "Ip Filter Enabled",false).setLayoutData(gridData); //$NON-NLS-1$
        
    label = new Label(gFilter, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ipFilter.enable"); //$NON-NLS-1$
    
    
    table = new Table(gFilter, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
    String[] headers = { "ipFilter.description", "ipFilter.start", "ipFilter.end" };
    int[] sizes = { 200, 110, 110 };
    int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };
    for (int i = 0; i < headers.length; i++) {
      TableColumn tc = new TableColumn(table, aligns[i]);
      tc.setText(headers[i]);
      tc.setWidth(sizes[i]);
      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
    }

    table.setHeaderVisible(true);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.heightHint = 200;
    gridData.verticalSpan = 4;
    gridData.horizontalSpan = 2;
    table.setLayoutData(gridData);

    Button add = new Button(gFilter, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    add.setLayoutData(gridData);
    Messages.setLanguageText(add, "ipFilter.add");
    add.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        addRange();
      }
    });

    Button remove = new Button(gFilter, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    remove.setLayoutData(gridData);
    Messages.setLanguageText(remove, "ipFilter.remove");
    remove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        removeRange((IpRange) selection[0].getData());
        table.remove(table.indexOf(selection[0]));
        selection[0].dispose();
      }
    });

    Button edit = new Button(gFilter, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    edit.setLayoutData(gridData);
    Messages.setLanguageText(edit, "ipFilter.edit");
    edit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        editRange((IpRange) selection[0].getData());
      }
    });

    table.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        editRange((IpRange) selection[0].getData());
      }
    });

    /*Button save = new Button(gFilter, SWT.PUSH);
    gridData = new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_END);
    gridData.widthHint = 100;
    save.setLayoutData(gridData);
    Messages.setLanguageText(save, "ipFilter.save");
    save.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        filter.save();
      }
    });*/

    populateTable();

    itemFilter.setControl(gFilter);
  }

  private void initGroupIrc() {
    GridData gridData;
    GridLayout layout;
    Label label;
    CTabItem itemIrc = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemIrc, "ConfigView.section.irc"); //$NON-NLS-1$

    Group gIrc = new Group(ctfConfig, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gIrc.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gIrc.setLayout(layout);

    label = new Label(gIrc, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.ircserver"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new StringParameter(gIrc, "Irc Server", "irc.freenode.net").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gIrc, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.ircchannel"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new StringParameter(gIrc, "Irc Channel", "#azureus-users").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gIrc, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.irclogin"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new StringParameter(gIrc, "Irc Login", "").setLayoutData(gridData); //$NON-NLS-1$

    itemIrc.setControl(gIrc);
  }

  private void initGroupDisplay() {
    GridData gridData;
    GridLayout layout;
    Label label;
    CTabItem itemDisplay = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemDisplay, "ConfigView.section.display"); //$NON-NLS-1$

    Group gDisplay = new Group(ctfConfig, SWT.NULL);
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

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.closetotray"); //$NON-NLS-1$
    new BooleanParameter(gDisplay, "Close To Tray", true); //$NON-NLS-1$

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minimizetotray"); //$NON-NLS-1$
    new BooleanParameter(gDisplay, "Minimize To Tray", false); //$NON-NLS-1$

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.password"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new PasswordParameter(gDisplay, "Password").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.passwordconfirm"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new PasswordParameter(gDisplay, "Password Confirm").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.passwordmatch"); //$NON-NLS-1$
    passwordMatch = new Label(gDisplay, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 150;
    passwordMatch.setLayoutData(gridData);

    itemDisplay.setControl(gDisplay);

    //CTabItem itemStart = new CTabItem(ctfConfig, SWT.NULL);
    //Messages.setLanguageText(itemStart, "ConfigView.section.start"); //$NON-NLS-1$ //general

    Group gStart = new Group(gDisplay, SWT.NULL);
    Messages.setLanguageText(gStart, "ConfigView.section.start"); //$NON-NLS-1$ //general
    
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 2;
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

    label = new Label(gStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openconsole"); //$NON-NLS-1$
    new BooleanParameter(gStart, "Open Console", false); //$NON-NLS-1$

    label = new Label(gStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openconfig"); //$NON-NLS-1$
    new BooleanParameter(gStart, "Open Config", false); //$NON-NLS-1$

    label = new Label(gStart, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startminimized"); //$NON-NLS-1$
    new BooleanParameter(gStart, "Start Minimized", false); //$NON-NLS-1$

  }

  private void initGroupTransfer() {
    GridData gridData;
    GridLayout layout;
    Label label;
    CTabItem itemTransfer = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemTransfer, "ConfigView.section.transfer"); //$NON-NLS-1$

    Group gTransfer = new Group(ctfConfig, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gTransfer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gTransfer.setLayout(layout);

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxactivetorrents"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gTransfer, "max active torrents", 4).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxdownloads"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gTransfer, "max downloads", 4).setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxclients"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(gTransfer, "Max Clients", 0).setLayoutData(gridData); //$NON-NLS-1$
    
    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.slowconnect"); //$NON-NLS-1$
    new BooleanParameter(gTransfer, "Slow Connect", false); //$NON-NLS-1$
    
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

    itemTransfer.setControl(gTransfer);
  }

  private void initGroupDownloads() {
    GridData gridData;
    GridLayout layout;
    Label label;
    CTabItem itemDownloads = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemDownloads, "ConfigView.section.seeding"); //$NON-NLS-1$

    Group gDownloads = new Group(ctfConfig, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gDownloads.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gDownloads.setLayout(layout);

    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.disconnetseed"); //$NON-NLS-1$
    new BooleanParameter(gDownloads, "Disconnect Seed", false); //$NON-NLS-1$

    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.switchpriority"); //$NON-NLS-1$
    new BooleanParameter(gDownloads, "Switch Priority", true); //$NON-NLS-1$
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.stopRatio"); //$NON-NLS-1$
    final String stopRatioLabels[] = new String[11];
    final int stopRatioValues[] = new int[11];
    stopRatioLabels[0] = MessageText.getString("ConfigView.text.neverStop");
    stopRatioValues[0] = 0;
    for (int i = 1; i < 11; i++) {
      stopRatioLabels[i] = i + ":" + 1; //$NON-NLS-1$
      stopRatioValues[i] = i;
    }
    new IntListParameter(gDownloads, "Stop Ratio", 0, stopRatioLabels, stopRatioValues);
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.stopRatioPeers"); //$NON-NLS-1$    
    final String stopRatioPeersLabels[] = new String[4];
    final int stopRatioPeersValues[] = new int[4];
    stopRatioPeersLabels[0] = MessageText.getString("ConfigView.text.neverStop");
    stopRatioPeersValues[0] = 0;
    String peers = MessageText.getString("ConfigView.text.peers");
    for (int i = 1; i < stopRatioPeersValues.length; i++) {
      stopRatioPeersLabels[i] = i + " " + peers; //$NON-NLS-1$
      stopRatioPeersValues[i] = i;
    }
    gridData = new GridData();
    gridData.verticalSpan = 2;
    new IntListParameter(gDownloads, "Stop Peers Ratio", 0, stopRatioPeersLabels, stopRatioPeersValues).setLayoutData(gridData);
    label = new Label(gDownloads,SWT.NULL);
    Messages.setLanguageText(label,"ConfigView.label.onlyafter50");
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startRatioPeers"); //$NON-NLS-1$
    final String startRatioPeersLabels[] = new String[11];
    final int startRatioPeersValues[] = new int[11];
    startRatioPeersLabels[0] = MessageText.getString("ConfigView.text.neverStart");
    startRatioPeersValues[0] = 0;
    for (int i = 1; i < 11; i++) {
      startRatioPeersLabels[i] = (i + 3) + " " + peers; //$NON-NLS-1$
      startRatioPeersValues[i] = i + 3;
    }
    new IntListParameter(gDownloads, "Start Peers Ratio", 0, startRatioPeersLabels, startRatioPeersValues);
    
    String seeds = MessageText.getString("ConfigView.label.seeds");
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startNumSeeds"); //$NON-NLS-1$
    final String startNumSeedsLabels[] = new String[11];
    final int startNumSeedsValues[] = new int[11];
    startNumSeedsLabels[0] = MessageText.getString("ConfigView.text.neverStart");
    startNumSeedsValues[0] = 0;
    for (int i = 1; i < 11; i++) {
      startNumSeedsLabels[i] = i + " " + seeds; //$NON-NLS-1$
      startNumSeedsValues[i] = i;
    }
    gridData = new GridData();
    gridData.verticalSpan = 2;
    new IntListParameter(gDownloads, "Start Num Peers", 0, startNumSeedsLabels, startNumSeedsValues).setLayoutData(gridData);    
    label = new Label(gDownloads,SWT.NULL);
    Messages.setLanguageText(label,"ConfigView.label.override");
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.showpopuponclose"); //$NON-NLS-1$
    new BooleanParameter(gDownloads, "Alert on close", true);
    
    itemDownloads.setControl(gDownloads);
  }

  private void initGroupServer() {
    GridData gridData;
    GridLayout layout;
    Label label;
    CTabItem itemServer = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemServer, "ConfigView.section.server"); //$NON-NLS-1$

    Group gServer = new Group(ctfConfig, SWT.NULL);
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

    itemServer.setControl(gServer);
  }

  private CTabItem initGroupFile() {
    GridData gridData;
    CTabItem itemFile = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemFile, "ConfigView.section.files"); //$NON-NLS-1$

    Group gFile = new Group(ctfConfig, SWT.NULL);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    gFile.setLayout(layout);
    

    Label label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.allocatenewfiles"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Allocate New", true); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.incrementalfile"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Enable incremental file creation", false); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.usefastresume"); //$NON-NLS-1$
    BooleanParameter bpUseResume = new BooleanParameter(gFile, "Use Resume", false); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.saveresumeinterval"); //$NON-NLS-1$
    final String saveResumeLabels[] = new String[19];
    final int saveResumeValues[] = new int[19];
    for (int i = 2; i < 21; i++) {
      saveResumeLabels[i - 2] = " " + i + " min"; //$NON-NLS-1$ //$NON-NLS-2$
      saveResumeValues[i - 2] = i;
    }
    Control[] controls = new Control[2];
    controls[0] = label;
    controls[1] = new IntListParameter(gFile, "Save Resume Interval", 5, saveResumeLabels, saveResumeValues).getControl(); //$NON-NLS-1$    
    IAdditionalActionPerformer performer = new ChangeSelectionActionPerformer(controls);
    bpUseResume.setAdditionalActionPerformer(performer);    
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
        DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.defaultTorrentPath"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter torrentPathParameter = new StringParameter(gFile, "General_sDefaultTorrent_Directory", ""); //$NON-NLS-1$ //$NON-NLS-2$
    torrentPathParameter.setLayoutData(gridData);
    Button browse2 = new Button(gFile, SWT.PUSH);
    Messages.setLanguageText(browse2, "ConfigView.button.browse"); //$NON-NLS-1$
    browse2.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(torrentPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });
    
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.priorityExtensions"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 100;
    new StringParameter(gFile, "priorityExtensions", "").setLayoutData(gridData); //$NON-NLS-1$       
    new Label(gFile, SWT.NULL);
    
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.checkOncompletion"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Check Pieces on Completion", false);

    itemFile.setControl(gFile);
    return itemFile;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return cConfig;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    byte[] password = COConfigurationManager.getByteParameter("Password", "".getBytes());
    COConfigurationManager.setParameter("Password enabled", false);
    if (password.length == 0) {
      passwordMatch.setText(MessageText.getString("ConfigView.label.passwordmatchnone"));
    }
    else {
      byte[] confirm = COConfigurationManager.getByteParameter("Password Confirm", "".getBytes());
      if (confirm.length == 0) {
        passwordMatch.setText(MessageText.getString("ConfigView.label.passwordmatchno"));
      }
      else {
        boolean same = true;
        for (int i = 0; i < password.length; i++) {
          if (password[i] != confirm[i])
            same = false;
        }
        if (same) {
          passwordMatch.setText(MessageText.getString("ConfigView.label.passwordmatchyes"));
          COConfigurationManager.setParameter("Password enabled", true);
        }
        else {
          passwordMatch.setText(MessageText.getString("ConfigView.label.passwordmatchno"));
        }
      }
    }

    if (table == null || table.isDisposed())
      return;
    TableItem[] items = table.getItems();
    for (int i = 0; i < items.length; i++) {
      IpRange range = (IpRange) items[i].getData();
      if (items[i] == null || items[i].isDisposed())
        continue;
      String tmp = items[i].getText(0);
      if (range.description != null && !range.description.equals(tmp))
        items[i].setText(0, range.description);

      tmp = items[i].getText(1);
      if (range.description != null && !range.startIp.equals(tmp))
        items[i].setText(1, range.startIp);

      tmp = items[i].getText(2);
      if (range.description != null && !range.endIp.equals(tmp))
        items[i].setText(2, range.endIp);

    }
  }

  public void updateLanguage() {
    super.updateLanguage();
    ctfConfig.setSize(ctfConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().setConfig(null);
    Utils.disposeComposite(ctfConfig);
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

  private void populateTable() {
    List ipRanges = filter.getIpRanges();
    synchronized (ipRanges) {
      Iterator iter = ipRanges.iterator();
      while (iter.hasNext()) {
        IpRange range = (IpRange) iter.next();
        TableItem item = new TableItem(table, SWT.NULL);
        item.setImage(0, ImageRepository.getImage("ipfilter"));
        item.setText(0, range.description);
        item.setText(1, range.startIp);
        item.setText(2, range.endIp);
        item.setData(range);
      }
    }
  }

  public void removeRange(IpRange range) {
    List ranges = filter.getIpRanges();
    synchronized (ranges) {
      ranges.remove(range);
    }
  }

  public void editRange(IpRange range) {
    new IpFilterEditor(ctfConfig.getDisplay(), table, filter.getIpRanges(), range);
  }

  public void addRange() {
    new IpFilterEditor(ctfConfig.getDisplay(), table, filter.getIpRanges(), null);
  }

}
