/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.ui.Parameter;
import org.gudy.azureus2.plugins.ui.impl.GenericParameter;
import org.gudy.azureus2.plugins.ui.impl.ParameterRepository;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.ipchecker.IpCheckerWizard;
import org.gudy.azureus2.ui.swt.ipchecker.IpSetterCallBack;

/**
 * @author Olivier
 * 
 */
public class ConfigView extends AbstractIView {

  private static final int upRates[] =
    {
      0,
      5,6,7,8,9,10,
      11,12,13,14,15,16,17,18,19,20,
      21,22,23,24,25,26,27,28,29,30,
      31,32,33,34,35,36,37,38,39,40,
      41,42,43,44,45,46,47,48,49,50,
      55,60,65,70,75,80,85,90,95,100,
      110,120,130,140,150,160,170,180,190,200,
      210,220,230,240,250,
      275,300,325,350,375,400,425,450,475,500,
      550,600,650,700,750,800,850,900,950,1000,
      1100,1200,1300,1400,1500,1600,1700,1800,1900,2000,
      2250,2500,2750,3000,
      3500,4000,4500,5000 };

  private static final int statsPeriods[] =
	  {
	  	1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 40, 50,
	  	60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 
	  	900, 1200, 1800, 2400, 3000, 3600, 
	  	7200, 10800, 14400, 21600, 43200, 86400,
	  };

  private static final int logFileSizes[] =
		 {
		   1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100,
		 };

  IpFilter filter;

  Composite cConfig;
  //CTabFolder ctfConfig;
  TabFolder tfConfig;
  Table table;
  boolean noChange;
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

    tfConfig = new TabFolder(cConfig, SWT.TOP | SWT.FLAT);
    //ctfConfig = new CTabFolder(cConfig, SWT.TOP | SWT.FLAT);
    //ctfConfig.setSelectionBackground(new Color[] { MainWindow.white }, new int[0]);
    
    TabItem itemFile = initGroupFile();
    //CTabItem itemFile = initGroupFile();
    
    initGroupServer();
    initGroupDownloads();
    initGroupTransfer();
    initGroupDisplay();
    initGroupIrc();
    initGroupFilter();
    initGroupPlugins();
	initStats();
    initStyle();
    initTracker();
    initLogging();
    
    initSaveButton(); 
    TabItem[] items = {itemFile};
    tfConfig.setSelection(items);
    Utils.changeBackgroundComposite(cConfig,MainWindow.getWindow().getBackground());
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

  private void initGroupPlugins()
  {
  	GridData gridData;
  	
	TabItem itemPlugins = new TabItem(tfConfig, SWT.NULL);
	itemPlugins.setText("Plugins");

	TabFolder tfPlugins = new TabFolder(tfConfig, SWT.TOP | SWT.FLAT);
	String[] names;
	Parameter[] tempParams;
	ParameterRepository repository = ParameterRepository.getInstance();
	TabItem tempTab;
	Group tempGroup;
	GridLayout tempLayout;
	Label tempLabel;
	GenericParameter tempParam;
	GridData gData;
	
	names = repository.getNames();
	for(int i = 0; i < names.length; i++)
	{
		tempParams = repository.getParameterBlock(names[i]);
		tempTab = new TabItem(tfPlugins, SWT.NULL);
		tempTab.setText(repository.getNames()[i]);
		
		tempGroup = new Group(tfPlugins, SWT.NULL);
		tempLayout = new GridLayout();
		tempLayout.numColumns = 2;
		tempGroup.setLayout(tempLayout);
        
		for(int j = 0; j < tempParams.length; j++)
		{
			tempParam = (GenericParameter)(tempParams[j]);
			tempLabel = new Label(tempGroup, SWT.NULL);
			tempLabel.setText(tempParam.getLabel());
			
			if(tempParam instanceof org.gudy.azureus2.plugins.ui.impl.StringParameter)
			{
				org.gudy.azureus2.plugins.ui.impl.StringParameter tpar = (org.gudy.azureus2.plugins.ui.impl.StringParameter)(tempParam);
//				String defaultVal = tpar.getDefaultValue();
				String defaultVal = "test";
				StringParameter uiParam = new StringParameter(tempGroup, tempParam.getKey(), defaultVal);
				gData = new GridData();
				gData.widthHint = 200;
				uiParam.setLayoutData(gData);
			}
		}
		tempTab.setControl(tempGroup);
	}
	itemPlugins.setControl(tfPlugins);
  }

  private void initGroupFilter() {
    GridData gridData;
    Label label;
    TabItem itemFilter = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemFilter = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemFilter, "ipFilter.shortTitle"); //$NON-NLS-1$

    Group gFilter = new Group(tfConfig, SWT.NULL);
    //Group gFilter = new Group(ctfConfig, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gFilter.setLayoutData(gridData);

    GridLayout layoutFilter = new GridLayout();
    layoutFilter.numColumns = 3;
    gFilter.setLayout(layoutFilter);

    gridData = new GridData(GridData.BEGINNING);
    BooleanParameter enabled = new BooleanParameter(gFilter, "Ip Filter Enabled",false); //$NON-NLS-1$
    enabled.setLayoutData(gridData);
        
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
    
    Control[] controls = new Control[3];
    controls[0] = add;
    controls[1] = remove;
    controls[2] = edit;
    IAdditionalActionPerformer enabler = new ChangeSelectionActionPerformer(controls);
    enabled.setAdditionalActionPerformer(enabler); 

    populateTable();

    itemFilter.setControl(gFilter);
  }

  private void initGroupIrc() {
    GridData gridData;
    GridLayout layout;
    Label label;
    TabItem itemIrc = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemIrc = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemIrc, "ConfigView.section.irc"); //$NON-NLS-1$

    Group gIrc = new Group(tfConfig, SWT.NULL);
    //Group gIrc = new Group(ctfConfig, SWT.NULL);

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
    TabItem itemDisplay = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemDisplay = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemDisplay, "ConfigView.section.display"); //$NON-NLS-1$
    
    Group gDisplay = new Group(tfConfig, SWT.NULL);
    //Group gDisplay = new Group(ctfConfig, SWT.NULL);
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

    label = new Label(gDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.allowSendVersion");
    new BooleanParameter(gDisplay, "Send Version Info",true);
    
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
    TabItem itemTransfer = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemTransfer = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemTransfer, "ConfigView.section.transfer"); //$NON-NLS-1$

    Group gTransfer = new Group(tfConfig, SWT.NULL);
    //Group gTransfer = new Group(ctfConfig, SWT.NULL);

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
      upsLabels[i] = " " + upRates[i] + " KB/s"; //$NON-NLS-1$ //$NON-NLS-2$
      upsValues[i] = 1024 * upRates[i];
    }
    new IntListParameter(gTransfer, "Max Upload Speed", 0, upsLabels, upsValues); //$NON-NLS-1$  

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.playdownloadfinished"); //$NON-NLS-1$
    new BooleanParameter(gTransfer, "Play Download Finished", true); //$NON-NLS-1$

    itemTransfer.setControl(gTransfer);
  }

  private void initGroupDownloads() {
    GridData gridData;
    GridLayout layout;
    Label label;
    TabItem itemDownloads = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemDownloads = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemDownloads, "ConfigView.section.seeding"); //$NON-NLS-1$

    Group gDownloads = new Group(tfConfig, SWT.NULL);    
    //Group gDownloads = new Group(ctfConfig, SWT.NULL);
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
    new BooleanParameter(gDownloads, "Switch Priority", false); //$NON-NLS-1$
    
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
    final String stopRatioPeersLabels[] = new String[5];
    final int stopRatioPeersValues[] = new int[5];
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
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startRatioPeers"); //$NON-NLS-1$
    final String startRatioPeersLabels[] = new String[13];
    final int startRatioPeersValues[] = new int[13];
    startRatioPeersLabels[0] = MessageText.getString("ConfigView.text.neverStart");
    startRatioPeersValues[0] = 0;
    for (int i = 1; i < 13; i++) {
      startRatioPeersLabels[i] = (i + 3) + " " + peers; //$NON-NLS-1$
      startRatioPeersValues[i] = i + 3;
    }
    new IntListParameter(gDownloads, "Start Peers Ratio", 0, startRatioPeersLabels, startRatioPeersValues);
    
    String seeds = MessageText.getString("ConfigView.label.seeds");
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.startNumSeeds"); //$NON-NLS-1$
    final String startNumSeedsLabels[] = new String[16];
    final int startNumSeedsValues[] = new int[16];
    startNumSeedsLabels[0] = MessageText.getString("ConfigView.text.neverStart");
    startNumSeedsValues[0] = 0;
    for (int i = 1; i < 16; i++) {
      startNumSeedsLabels[i] = i + " " + seeds; //$NON-NLS-1$
      startNumSeedsValues[i] = i;
    }
    gridData = new GridData();
    gridData.verticalSpan = 2;
    new IntListParameter(gDownloads, "Start Num Peers", 0, startNumSeedsLabels, startNumSeedsValues).setLayoutData(gridData);    
    label = new Label(gDownloads,SWT.NULL);
    //Messages.setLanguageText(label,"ConfigView.label.override");
    
    label = new Label(gDownloads, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.showpopuponclose"); //$NON-NLS-1$
    new BooleanParameter(gDownloads, "Alert on close", true);
    
    itemDownloads.setControl(gDownloads);
  }

  private void initGroupServer() {
    GridData gridData;
    GridLayout layout;
    Label label;
    TabItem itemServer = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemServer = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemServer, "ConfigView.section.server"); //$NON-NLS-1$

    Group gServer = new Group(tfConfig, SWT.NULL);
    //Group gServer = new Group(ctfConfig, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gServer.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gServer.setLayout(layout);

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.overrideip"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 113;
    gridData.horizontalSpan = 3;
    new StringParameter(gServer, "Override Ip", "").setLayoutData(gridData); //$NON-NLS-1$ //$NON-NLS-2$

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.bindip"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 113;
    gridData.horizontalSpan = 3;
    new StringParameter(gServer, "Bind IP", "").setLayoutData(gridData); //$NON-NLS-1$ //$NON-NLS-2$
    
    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverportrange"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "Low Port", 6881).setLayoutData(gridData); //$NON-NLS-1$

    Label lDash = new Label(gServer, SWT.NULL);
    lDash.setText(" - ");

    gridData = new GridData();
    gridData.widthHint = 40;
    IntParameter high_port = new IntParameter(gServer, "High Port", 6889);
    high_port.setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gServer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.serverportshared"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    BooleanParameter ssp = new BooleanParameter(gServer, "Server.shared.port", true);
    ssp.setLayoutData(gridData); //$NON-NLS-1$

    Control[] controls = new Control[2];
    controls[0] = lDash;
    controls[1] = high_port.getControl();
    IAdditionalActionPerformer grayHighPort = new ChangeSelectionActionPerformer(controls, true);
    ssp.setAdditionalActionPerformer(grayHighPort);

	
    itemServer.setControl(gServer);
  }

  private TabItem initGroupFile() {
  //private CTabItem initGroupFile() {
    GridData gridData;
    TabItem itemFile = new TabItem(tfConfig, SWT.NULL);
    //CTabItem itemFile = new CTabItem(ctfConfig, SWT.NULL);
    Messages.setLanguageText(itemFile, "ConfigView.section.files"); //$NON-NLS-1$

    Group gFile = new Group(tfConfig, SWT.NULL);
    //Group gFile = new Group(ctfConfig, SWT.NULL);

    GridLayout layout = new GridLayout();
    layout.numColumns = 7;
    gFile.setLayout(layout);
    Label label;
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.zeronewfiles"); //$NON-NLS-1$
    BooleanParameter zeroNew = new BooleanParameter(gFile, "Zero New", false); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.incrementalfile"); //$NON-NLS-1$
    BooleanParameter incremental = new BooleanParameter(gFile, "Enable incremental file creation", false); //$NON-NLS-1$
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);

    //Make the incremental checkbox (button) deselect when zero new is used
    Button[] btnIncremental = {(Button)incremental.getControl()};
    zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnIncremental));
    
    //Make the zero new checkbox(button) deselct when incremental is used
    Button[] btnZeroNew = {(Button)zeroNew.getControl()}; 
    incremental.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnZeroNew));
    
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.checkOncompletion"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Check Pieces on Completion", true);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
    new Label(gFile, SWT.NULL);
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
    new Label(gFile, SWT.NULL);
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.defaultsavepath"); //$NON-NLS-1$
    BooleanParameter saveDefault = new BooleanParameter(gFile, "Use default data dir", true); //$NON-NLS-1$
 
    Button browse = new Button(gFile, SWT.PUSH);
    Messages.setLanguageText(browse, "ConfigView.button.browse"); //$NON-NLS-1$
    
    gridData = new GridData();
    gridData.widthHint = 180;
    gridData.horizontalSpan = 4;
    final StringParameter pathParameter = new StringParameter(gFile, "Default save path", ""); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);
    
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(tfConfig.getShell(), SWT.APPLICATION_MODAL);
        //DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });
    
    controls = new Control[2];
    controls[0] = pathParameter.getControl();
    controls[1] = browse;
    IAdditionalActionPerformer defaultSave = new ChangeSelectionActionPerformer(controls);
    saveDefault.setAdditionalActionPerformer(defaultSave);
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.savetorrents"); //$NON-NLS-1$
    BooleanParameter saveTorrents = new BooleanParameter(gFile, "Save Torrent Files", true); //$NON-NLS-1$    

    Button browse2 = new Button(gFile, SWT.PUSH);
    Messages.setLanguageText(browse2, "ConfigView.button.browse"); //$NON-NLS-1$
   
    gridData = new GridData();
    gridData.widthHint = 180;
    gridData.horizontalSpan = 4;
    final StringParameter torrentPathParameter = new StringParameter(gFile, "General_sDefaultTorrent_Directory", ""); //$NON-NLS-1$ //$NON-NLS-2$
    torrentPathParameter.setLayoutData(gridData);
    
    browse2.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(tfConfig.getShell(), SWT.APPLICATION_MODAL);
        //DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(torrentPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaulttorrentpath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });
    
    controls = new Control[2];
    controls[0] = torrentPathParameter.getControl();
    controls[1] = browse2;
    IAdditionalActionPerformer grayPathAndButton1 = new ChangeSelectionActionPerformer(controls);
    saveTorrents.setAdditionalActionPerformer(grayPathAndButton1);
    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.movecompleted"); //$NON-NLS-1$
    BooleanParameter moveCompleted = new BooleanParameter(gFile, "Move Completed When Done", false); //$NON-NLS-1$    

    Button browse3 = new Button(gFile, SWT.PUSH);
    Messages.setLanguageText(browse3, "ConfigView.button.browse"); //$NON-NLS-1$
    
    gridData = new GridData();
    gridData.widthHint = 180;
    gridData.horizontalSpan = 2;
    final StringParameter movePathParameter = new StringParameter(gFile, "Completed Files Directory", "");
    movePathParameter.setLayoutData(gridData);
    
    browse3.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(tfConfig.getShell(), SWT.APPLICATION_MODAL);
        //DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(movePathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosemovepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          movePathParameter.setValue(path);
        }
      }
    });
        
    
    Label lMoveTorrent = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(lMoveTorrent, "ConfigView.label.movetorrent"); //$NON-NLS-1$
    BooleanParameter moveTorrent = new BooleanParameter(gFile, "Move Torrent When Done", true); //$NON-NLS-1$    
    
    controls = new Control[4];
    controls[0] = movePathParameter.getControl();
    controls[1] = browse3;
    controls[2] = lMoveTorrent;
    controls[3] = moveTorrent.getControl();
    IAdditionalActionPerformer grayPathAndButton2 = new ChangeSelectionActionPerformer(controls);
    moveCompleted.setAdditionalActionPerformer(grayPathAndButton2);

    
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.priorityExtensions"); //$NON-NLS-1$
    
    gridData = new GridData();
    gridData.widthHint = 262;
    gridData.horizontalSpan = 6;
    new StringParameter(gFile, "priorityExtensions", "").setLayoutData(gridData); //$NON-NLS-1$       


	label = new Label(gFile, SWT.NULL);
	Messages.setLanguageText(label, "ConfigView.section.file.decoder.label"); //$NON-NLS-1$
	new Label(gFile, SWT.NULL);
	new Label(gFile, SWT.NULL);
  
	LocaleUtilDecoder[]	decoders = LocaleUtil.getDecoders();
	
	String decoderLabels[] = new String[decoders.length + 1];
	String decoderValues[] = new String[decoders.length + 1];
	
	decoderLabels[0] = MessageText.getString( "ConfigView.section.file.decoder.nodecoder");
	decoderValues[0] = "";
	
	for (int i = 1; i <= decoders.length; i++) {
		
		decoderLabels[i] = decoderValues[i] = decoders[i-1].getName();
    }
	Control[] decoder_controls = new Control[2];
	decoder_controls[0] = label;
	decoder_controls[1] = new StringListParameter(gFile, "File.Decoder.Default", "", decoderLabels, decoderValues).getControl(); //$NON-NLS-1$    
	new Label(gFile, SWT.NULL);
	new Label(gFile, SWT.NULL);
 


    itemFile.setControl(gFile);
    return itemFile;
  }
  
  private void initStats() {
	 GridData gridData;
	 GridLayout layout;
	 Label label;
   TabItem itemStats = new TabItem(tfConfig, SWT.NULL);
	 //CTabItem itemStats = new CTabItem(ctfConfig, SWT.NULL);
	 Messages.setLanguageText(itemStats, "ConfigView.section.stats"); //$NON-NLS-1$

   Group gStats = new Group(tfConfig, SWT.NULL);
	 //Group gStats = new Group(ctfConfig, SWT.NULL);
	 gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	 gStats.setLayoutData(gridData);
	 layout = new GridLayout();
	 layout.numColumns = 3;
	 gStats.setLayout(layout);

		// row
		
	 label = new Label(gStats, SWT.NULL);
	 Messages.setLanguageText(label, "ConfigView.section.stats.enable"); //$NON-NLS-1$
	 BooleanParameter enableStats = new BooleanParameter(gStats, "Stats Enable", false); //$NON-NLS-1$

	 label = new Label(gStats, SWT.NULL);

   Control[] controls = new Control[7];
   
		// row
		
	 Label lStatsPath = new Label(gStats, SWT.NULL);
	 Messages.setLanguageText(lStatsPath, "ConfigView.section.stats.defaultsavepath"); //$NON-NLS-1$

	 gridData = new GridData();
	 gridData.widthHint = 150;
   final StringParameter pathParameter = new StringParameter(gStats, "Stats Dir", ""); //$NON-NLS-1$ //$NON-NLS-2$
   pathParameter.setLayoutData(gridData);
   controls[0] = lStatsPath;
   controls[1] = pathParameter.getControl();
   Button browse = new Button(gStats, SWT.PUSH);
   Messages.setLanguageText(browse, "ConfigView.button.browse"); //$NON-NLS-1$
   controls[2] = browse;
   browse.addListener(SWT.Selection, new Listener() {
	  /* (non-Javadoc)
	   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	   */
	  public void handleEvent(Event event) {
      DirectoryDialog dialog = new DirectoryDialog(tfConfig.getShell(), SWT.APPLICATION_MODAL);
		//DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
		dialog.setFilterPath(pathParameter.getValue());
		dialog.setText(MessageText.getString("ConfigView.section.stats.choosedefaultsavepath")); //$NON-NLS-1$
		String path = dialog.open();
		if (path != null) {
		  pathParameter.setValue(path);
		}
	  }
	});

	// row
		
	Label lSaveFile = new Label(gStats, SWT.NULL);
	Messages.setLanguageText(lSaveFile, "ConfigView.section.stats.savefile"); //$NON-NLS-1$
	
	gridData = new GridData();
	gridData.widthHint = 150;
	final StringParameter fileParameter = new StringParameter(gStats, "Stats File", StatsWriterPeriodic.DEFAULT_STATS_FILE_NAME ); 
  fileParameter.setLayoutData(gridData);
  controls[3] = lSaveFile;
  controls[4] = fileParameter.getControl();
	label = new Label(gStats, SWT.NULL);

		// row
		
	Label lSaveFreq = new Label(gStats, SWT.NULL);
		
	Messages.setLanguageText(lSaveFreq, "ConfigView.section.stats.savefreq"); 
	final String spLabels[] = new String[statsPeriods.length];
	final int spValues[] = new int[statsPeriods.length];
	for (int i = 0; i < statsPeriods.length; i++) {
		int	num = statsPeriods[i];
		
		if ( num%3600 ==0 ){
	
			spLabels[i] = " " + (statsPeriods[i]/3600) + " " + MessageText.getString("ConfigView.section.stats.hours" );
		
		}else if ( num%60 ==0 ){
	
			spLabels[i] = " " + (statsPeriods[i]/60) + " " + MessageText.getString("ConfigView.section.stats.minutes" );
		
		}else{
	
			spLabels[i] = " " + statsPeriods[i] + " " + MessageText.getString("ConfigView.section.stats.seconds" );
		}
		
		spValues[i] = statsPeriods[i];
	}
	
  controls[5] = lSaveFreq;
  controls[6] = new IntListParameter(gStats, "Stats Period", 0, spLabels, spValues).getControl();  
  enableStats.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));
 
	itemStats.setControl(gStats);
   }

  private void initStyle() {
    GridData gridData;
   GridLayout layout;
   Label label;
   TabItem itemStyle = new TabItem(tfConfig, SWT.NULL);
   //CTabItem itemStats = new CTabItem(ctfConfig, SWT.NULL);
   Messages.setLanguageText(itemStyle, "ConfigView.section.style"); //$NON-NLS-1$

   Group gStyle = new Group(tfConfig, SWT.NULL);
   //Group gStats = new Group(ctfConfig, SWT.NULL);
   gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
   gStyle.setLayoutData(gridData);
   layout = new GridLayout();
   layout.numColumns = 2;
   gStyle.setLayout(layout);
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.useCustomTabs"); //$NON-NLS-1$
   new BooleanParameter(gStyle, "useCustomTab",true); //$NON-NLS-1$
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.confirmationOnExit"); //$NON-NLS-1$
   new BooleanParameter(gStyle, "confirmationOnExit",false); //$NON-NLS-1$
   
   String osName = System.getProperty("os.name");
   if (osName.equals("Windows XP")) {
     label = new Label(gStyle, SWT.NULL);
     Messages.setLanguageText(label, "ConfigView.section.style.enableXPStyle"); //$NON-NLS-1$
     final Button enableXPStyle = new Button(gStyle, SWT.CHECK);
     boolean enabled = false;
     boolean valid = false;
     try {
       File f =
         new File(
           System.getProperty("java.home")
             + "\\bin\\javaw.exe.manifest");
       if (f.exists()) {
         enabled = true;
       f= new File("javaw.exe.manifest");
       if(f.exists())
           valid = true;
       }
     } catch (Exception e) {
       e.printStackTrace();
       valid = false;
     }
     enableXPStyle.setEnabled(valid);
     enableXPStyle.setSelection(enabled);
     enableXPStyle.addListener(SWT.Selection, new Listener() {
       public void handleEvent(Event arg0) {
         //In case we enable the XP Style
         if (enableXPStyle.getSelection()) {
           try {
             File fDest =
               new File(
                 System.getProperty("java.home")
                   + "\\bin\\javaw.exe.manifest");
             File fOrigin = new File("javaw.exe.manifest");
             if (!fDest.exists() && fOrigin.exists()) {
               FileUtil.copyFile(fOrigin, fDest);
             }
           } catch (Exception e) {
             e.printStackTrace();
           }
         } else {
           try {
             File fDest =
               new File(
                 System.getProperty("java.home")
                   + "\\bin\\javaw.exe.manifest");
             fDest.delete();
           } catch (Exception e) {
             e.printStackTrace();
           }
         }
       }
     });
   }
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.colorScheme"); //$NON-NLS-1$
   ColorParameter colorScheme = new ColorParameter(gStyle, "Color Scheme",0,128,255); //$NON-NLS-1$
   gridData = new GridData();
   gridData.widthHint = 50;
   colorScheme.setLayoutData(gridData);
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.guiUpdate"); //$NON-NLS-1$
   int[] values = { 100 , 250 , 500 , 1000 , 2000 , 5000 };
   String[] labels = { "100 ms" , "250 ms" , "500 ms" , "1 s" , "2 s" , "5 s" };
   new IntListParameter(gStyle, "GUI Refresh", 250, labels, values);
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.graphicsUpdate"); //$NON-NLS-1$
   int[] gValues = new int[50];
   String[] gLabels = new String[50];   
   for(int i = 1 ; i <= 50 ; i++) {
     gValues[i-1] = i;
     gLabels[i-1] = "" + i;
   }
   new IntListParameter(gStyle, "Graphics Update", 4, gLabels, gValues);
   
   label = new Label(gStyle, SWT.NULL);
   Messages.setLanguageText(label, "ConfigView.section.style.reOrderDelay"); //$NON-NLS-1$
   int[] rValues = new int[51];
   String[] rLabels = new String[51]; 
   rValues[0] = 0;
   rLabels[0] = MessageText.getString("ConfigView.section.style.reOrderDelay.never");
   for(int i = 1 ; i <= 50 ; i++) {
     rValues[i] = i;
     rLabels[i] = "" + i;
   }
   new IntListParameter(gStyle, "ReOrder Delay", 0, rLabels, rValues);
   
   itemStyle.setControl(gStyle);
  }
  
  
  
 	private void 
 	initTracker() 
  	{
		GridData gridData;
		GridLayout layout;
		Label label;
		TabItem itemStats = new TabItem(tfConfig, SWT.NULL);
		Messages.setLanguageText(itemStats, "ConfigView.section.tracker"); //$NON-NLS-1$

		Group gTracker = new Group(tfConfig, SWT.NULL);
		
	    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	    
		gTracker.setLayoutData(gridData);
		
		layout = new GridLayout();
		
		layout.numColumns = 4;
		
		gTracker.setLayout(layout);

		// row
		
		label = new Label(gTracker, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.tracker.pollinterval"); 
    
		label = new Label(gTracker, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmin"); 
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
   
		IntParameter pollIntervalMin = new IntParameter(gTracker, "Tracker Poll Interval Min", TRHost.DEFAULT_MIN_RETRY_DELAY );
	
		gridData = new GridData();
		gridData.widthHint = 30;
		pollIntervalMin.setLayoutData( gridData );
		
		// row
		
		label = new Label(gTracker, SWT.NULL);
				
		label = new Label(gTracker, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmax"); 
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
   
		IntParameter pollIntervalMax = new IntParameter(gTracker, "Tracker Poll Interval Max", TRHost.DEFAULT_MAX_RETRY_DELAY );
	
		gridData = new GridData();
		gridData.widthHint = 30;
		pollIntervalMax.setLayoutData( gridData );
		
		// row
		
		label = new Label(gTracker, SWT.NULL);
				
		label = new Label(gTracker, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincby"); 
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
  
		IntParameter pollIntervalIncBy = new IntParameter(gTracker, "Tracker Poll Inc By", TRHost.DEFAULT_INC_BY );
		
		gridData = new GridData();
		gridData.widthHint = 30;
		pollIntervalIncBy.setLayoutData( gridData );
		
		// row
		
		label = new Label(gTracker, SWT.NULL);
				
		label = new Label(gTracker, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincper"); 
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
   
		IntParameter pollIntervalIncPer = new IntParameter(gTracker, "Tracker Poll Inc Per", TRHost.DEFAULT_INC_PER );
		
		gridData = new GridData();
		gridData.widthHint = 30;
		pollIntervalIncPer.setLayoutData( gridData );
		
	   // row
		
	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.ip"); 
	  
      final StringParameter tracker_ip = new StringParameter(gTracker, "Tracker IP", "" );
	  
	  gridData = new GridData();
	  gridData.widthHint = 100;
	  gridData.horizontalSpan = 2;
	  tracker_ip.setLayoutData( gridData );
	  
	  Button check_button = new Button(gTracker, SWT.PUSH);
	  
	  Messages.setLanguageText(check_button, "ConfigView.section.tracker.checkip"); //$NON-NLS-1$

      final Display display = gTracker.getDisplay();
    
	  check_button.addListener(SWT.Selection, new Listener() {

 	  public void 
	  handleEvent(Event event) 
	  {
      IpCheckerWizard wizard = new IpCheckerWizard(cConfig.getDisplay());
      wizard.setIpSetterCallBack(new IpSetterCallBack() {
        public void setIp(final String ip) {
          if(display == null || display.isDisposed())
            return;
            display.asyncExec(new Runnable() {
            public void run() {
              if(tracker_ip != null)
                tracker_ip.setValue(ip);
            }
          });
        }
       });

		 }
	   });
		
	   // row
		
	   label = new Label(gTracker, SWT.NULL);
	   Messages.setLanguageText(label, "ConfigView.section.tracker.port"); 
    
	   IntParameter tracker_port = new IntParameter(gTracker, "Tracker Port", TRHost.DEFAULT_PORT );

	   gridData = new GridData();
	   gridData.widthHint = 50;
	   tracker_port.setLayoutData( gridData );
	  
	   final BooleanParameter nonsslEnable = new BooleanParameter(gTracker, "Tracker Port Enable", true);

	   Control[] non_ssl_controls = new Control[1];
	   non_ssl_controls[0] = tracker_port.getControl();
	  					
	   nonsslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( non_ssl_controls ));

	   label = new Label(gTracker, SWT.NULL);
		
	   // row
		
	   label = new Label(gTracker, SWT.NULL);
	   Messages.setLanguageText(label, "ConfigView.section.tracker.sslport"); 
    
	   IntParameter tracker_port_ssl = new IntParameter(gTracker, "Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );

	   gridData = new GridData();
	   gridData.widthHint = 50;
	   tracker_port_ssl.setLayoutData( gridData );

	   final BooleanParameter sslEnable = new BooleanParameter(gTracker, "Tracker Port SSL Enable", false);
	   
	   Control[] ssl_controls = new Control[1];
	   ssl_controls[0] = tracker_port_ssl.getControl();
	  					
	   sslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( ssl_controls ));
	    
	   label = new Label(gTracker, SWT.NULL);
	   Messages.setLanguageText(label, "ConfigView.section.tracker.sslport.info");
     final String linkFAQ = "http://azureus.sourceforge.net/faq.php#17";
     label.setCursor(MainWindow.handCursor);
     label.setForeground(MainWindow.blue);
     label.addMouseListener(new MouseAdapter() {
       public void mouseDoubleClick(MouseEvent arg0) {
         Program.launch(linkFAQ);
       }
       public void mouseDown(MouseEvent arg0) {
         Program.launch(linkFAQ);
       }
     });

    // row
			
	  label = new Label(gTracker, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.tracker.publishenable"); 
    
      BooleanParameter enablePublish = new BooleanParameter(gTracker, "Tracker Publish Enable", true);
	  gridData = new GridData();
	  gridData.horizontalSpan = 2;
	  enablePublish.setLayoutData( gridData );

      label = new Label(gTracker, SWT.NULL);

      // row
			
      label = new Label(gTracker, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.tracker.publicenable"); 
    
      BooleanParameter publicPublish = new BooleanParameter(gTracker, "Tracker Public Enable", false);
	  gridData = new GridData();
	  gridData.horizontalSpan = 2;
	  publicPublish.setLayoutData( gridData );

      label = new Label(gTracker, SWT.NULL);

	  // row

	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenableweb"); 
    
	  final BooleanParameter passwordEnableWeb = new BooleanParameter(gTracker, "Tracker Password Enable Web", false);
	  gridData = new GridData();
	  gridData.horizontalSpan = 2;
	  passwordEnableWeb.setLayoutData( gridData );

	  label = new Label(gTracker, SWT.NULL);
	  
	  // row

	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent"); 
    
	  final BooleanParameter passwordEnableTorrent = new BooleanParameter(gTracker, "Tracker Password Enable Torrent", false);
	  gridData = new GridData();
	  gridData.horizontalSpan = 2;
	  passwordEnableTorrent.setLayoutData( gridData );

	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent.info"); 


		// row
		
	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.username"); 
	  
	  final StringParameter tracker_username = new StringParameter(gTracker, "Tracker Username", "" );
	  
	  gridData = new GridData();
	  gridData.horizontalSpan = 2;
	  gridData.widthHint = 100;
	  tracker_username.setLayoutData( gridData );

	  label = new Label(gTracker, SWT.NULL);

	 	 // row
		
	  label = new Label(gTracker, SWT.NULL);
	  Messages.setLanguageText(label, "ConfigView.section.tracker.password"); 
	  
	  final PasswordParameter tracker_password = new PasswordParameter(gTracker, "Tracker Password" );
	  
	  gridData = new GridData();
	  gridData.widthHint = 100;
	  gridData.horizontalSpan = 2;
	  tracker_password.setLayoutData( gridData );

	  label = new Label(gTracker, SWT.NULL);


 	  Control[] x_controls = new Control[2];
	  x_controls[0] = tracker_username.getControl();
	  x_controls[1] = tracker_password.getControl();
	
	  IAdditionalActionPerformer enabler = 
	  		new GenericActionPerformer(x_controls)
	  				{
						public void performAction() 
						{
							boolean selected =  passwordEnableWeb.isSelected() ||
												passwordEnableTorrent.isSelected();
											
							for (int i=0;i<controls.length;i++){
								
								controls[i].setEnabled( selected );
							}
						}
  					};
  					
	  passwordEnableWeb.setAdditionalActionPerformer(enabler); 
	  passwordEnableTorrent.setAdditionalActionPerformer(enabler); 

	  itemStats.setControl(gTracker);
	}
	
	
	
	
	private void initLogging() {
		GridData gridData;
		GridLayout layout;
		Label label;
	  TabItem itemLogging = new TabItem(tfConfig, SWT.NULL);
		Messages.setLanguageText(itemLogging, "ConfigView.section.logging"); //$NON-NLS-1$

	  Group gLogging = new Group(tfConfig, SWT.NULL);
		//Group gStats = new Group(ctfConfig, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gLogging.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 3;
		gLogging.setLayout(layout);

		   // row
		
		label = new Label(gLogging, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.logging.enable"); //$NON-NLS-1$
		BooleanParameter enableLogging = new BooleanParameter(gLogging, "Logging Enable", false); //$NON-NLS-1$

		label = new Label(gLogging, SWT.NULL);

	    Control[] controls = new Control[4];
   
		   // row
		
		Label lStatsPath = new Label(gLogging, SWT.NULL);
		Messages.setLanguageText(lStatsPath, "ConfigView.section.logging.logdir"); //$NON-NLS-1$

		gridData = new GridData();
		gridData.widthHint = 150;
	  final StringParameter pathParameter = new StringParameter(gLogging, "Logging Dir", ""); //$NON-NLS-1$ //$NON-NLS-2$
	  pathParameter.setLayoutData(gridData);
	  controls[0] = lStatsPath;
	  controls[1] = pathParameter.getControl();
	  Button browse = new Button(gLogging, SWT.PUSH);
	  Messages.setLanguageText(browse, "ConfigView.button.browse"); //$NON-NLS-1$
	  controls[2] = browse;
	  browse.addListener(SWT.Selection, new Listener() {
		 /* (non-Javadoc)
		  * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		  */
		 public void handleEvent(Event event) {
		 DirectoryDialog dialog = new DirectoryDialog(tfConfig.getShell(), SWT.APPLICATION_MODAL);
		   //DirectoryDialog dialog = new DirectoryDialog(ctfConfig.getShell(), SWT.APPLICATION_MODAL);
		   dialog.setFilterPath(pathParameter.getValue());
		   dialog.setText(MessageText.getString("ConfigView.section.logging.choosedefaultsavepath")); //$NON-NLS-1$
		   String path = dialog.open();
		   if (path != null) {
			 pathParameter.setValue(path);
		   }
		 }
	   });
 
	   Label lMaxLog = new Label(gLogging, SWT.NULL);
		
	   Messages.setLanguageText(lMaxLog, "ConfigView.section.logging.maxsize"); 
	   final String lmLabels[] = new String[logFileSizes.length];
	   final int lmValues[] = new int[logFileSizes.length];
	   for (int i = 0; i < logFileSizes.length; i++) {
		   int	num = logFileSizes[i];
			
		   lmLabels[i] = " " + num + " MB";
				
		   lmValues[i] = num;
	   }
	
	   controls[3] = new IntListParameter(gLogging, "Logging Max Size", 0, lmLabels, lmValues).getControl();  
	   enableLogging.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));
 
	   itemLogging.setControl(gLogging);
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
    if(passwordMatch == null || passwordMatch.isDisposed())
      return;
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

    if (table == null || table.isDisposed() || noChange)
      return;
    noChange = true;
    TableItem[] items = table.getItems();
    for (int i = 0; i < items.length; i++) {
      IpRange range = (IpRange) items[i].getData();
      if (items[i] == null || items[i].isDisposed())
        continue;
      String tmp = items[i].getText(0);
      if (range.getDescription() != null && !range.getDescription().equals(tmp))
        items[i].setText(0, range.getDescription());

      tmp = items[i].getText(1);
      if (range.getStartIp() != null && !range.getStartIp().equals(tmp))
        items[i].setText(1, range.getStartIp());

      tmp = items[i].getText(2);
      if (range.getEndIp() != null && !range.getEndIp().equals(tmp))
        items[i].setText(2, range.getEndIp());

    }
  }

  public void updateLanguage() {
    super.updateLanguage();
    tfConfig.setSize(tfConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    //ctfConfig.setSize(ctfConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().setConfig(null);
    Utils.disposeComposite(tfConfig);
    //Utils.disposeComposite(ctfConfig);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("ConfigView.title.full"); //$NON-NLS-1$
  }

  private void populateTable() {
    List ipRanges = filter.getIpRanges();
    Display display = cConfig.getDisplay();
    if(display == null || display.isDisposed()) {
      return;
    }
    synchronized (ipRanges) {
      Iterator iter = ipRanges.iterator();
      while (iter.hasNext()) {
        final IpRange range = (IpRange) iter.next();
        display.asyncExec(new Runnable() {
          public void run() {
            if(table == null || table.isDisposed())
              return;
            TableItem item = new TableItem(table, SWT.NULL);
            item.setImage(0, ImageRepository.getImage("ipfilter"));
            item.setText(0, range.getDescription());
            item.setText(1, range.getStartIp());
            item.setText(2, range.getEndIp());
            item.setData(range);
          }
        });        
      }
    }
  }

  public void removeRange(IpRange range) {
    List ranges = filter.getIpRanges();
    synchronized (ranges) {
      ranges.remove(range);
    }
    noChange = false;
  }

  public void editRange(IpRange range) {
    new IpFilterEditor(tfConfig.getDisplay(), table, filter.getIpRanges(), range);
    noChange = false;
    //new IpFilterEditor(ctfConfig.getDisplay(), table, filter.getIpRanges(), range);
  }

  public void addRange() {
    new IpFilterEditor(tfConfig.getDisplay(), table, filter.getIpRanges(), null);
    noChange = false;
    //new IpFilterEditor(ctfConfig.getDisplay(), table, filter.getIpRanges(), null);
  }

}
