/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.pluginsimpl.ui.config.ParameterRepository;
import org.gudy.azureus2.pluginsimpl.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.config.plugins.PluginParameter;
import org.gudy.azureus2.ui.swt.ipchecker.IpCheckerWizard;
import org.gudy.azureus2.ui.swt.ipchecker.IpSetterCallBack;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.pluginsimpl.PluginInitializer;
import org.gudy.azureus2.core3.logging.LGLogger;

/**
 * @author Olivier
 *
 */
public class ConfigView extends AbstractIView {

  public static final int upRates[] =
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
  Composite cConfigSection;
  StackLayout layoutConfigSection;
  Label lHeader;
  Tree tree;
  TreeItem treePlugins;
  Table table;
  ArrayList pluginSections;
  boolean noChange;
  Label passwordMatch;

  public ConfigView() {
    filter = IpFilter.getInstance();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    GridData gridData;
    /*
    /--cConfig-------------------------------------------------------\
    | ###SashForm#form############################################## |
    | # /--tree--\ /--cRightSide---------------------------------\ # |
    | # |        | | ***cHeader********************************* | # |
    | # |        | | * lHeader                                 * | # |
    | # |        | | ******************************************* | # |
    | # |        | | ###Composite cConfigSection################ | # |
    | # |        | | #                                         # | # |
    | # |        | | #                                         # | # |
    | # |        | | #                                         # | # |
    | # |        | | #                                         # | # |
    | # |        | | ########################################### | # |
    | # \--------/ \---------------------------------------------/ # |
    | ############################################################## |
    |  [Button]                                                      |
    \----------------------------------------------------------------/
    */
    cConfig = new Composite(composite, SWT.NONE);
    GridLayout configLayout = new GridLayout();
    configLayout.marginHeight = 0;
    configLayout.marginWidth = 0;
    cConfig.setLayout(configLayout);
    gridData = new GridData(GridData.FILL_BOTH);
    cConfig.setLayoutData(gridData);

    SashForm form = new SashForm(cConfig,SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_BOTH);
    form.setLayoutData(gridData);

    tree = new Tree(form, SWT.BORDER);
    tree.setLayout(new FillLayout());

    Composite cRightSide = new Composite(form, SWT.NULL);
    configLayout = new GridLayout();
    configLayout.marginHeight = 3;
    configLayout.marginWidth = 0;
    cRightSide.setLayout(configLayout);

    // Header
    Composite cHeader = new Composite(cRightSide, SWT.BORDER);
    configLayout = new GridLayout();
    configLayout.marginHeight = 3;
    configLayout.marginWidth = 0;
    cHeader.setLayout(configLayout);
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    cHeader.setLayoutData(gridData);

    Display d = cRightSide.getDisplay();
    cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
    cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

    lHeader = new Label(cHeader, SWT.NULL);
    lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
    lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
    FontData[] fontData = lHeader.getFont().getFontData();
    fontData[0].setStyle(SWT.BOLD);
    int fontHeight = (int)(fontData[0].getHeight() * 1.2);
    fontData[0].setHeight(fontHeight);
    lHeader.setFont(new Font(d, fontData));
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    lHeader.setLayoutData(gridData);

    // Config Section
    cConfigSection = new Composite(cRightSide, SWT.NULL);
    layoutConfigSection = new StackLayout();
    cConfigSection.setLayout(layoutConfigSection);
    gridData = new GridData(GridData.FILL_BOTH);
    cConfigSection.setLayoutData(gridData);


    form.setWeights(new int[] {20,80});

    tree.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Tree tree = (Tree)e.getSource();
        showSection(tree.getSelection()[0]);
      }
    });
    // Double click = expand/contract branch
    tree.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
          TreeItem item = (TreeItem)e.item;
          if (item != null)
            item.setExpanded(!item.getExpanded());
      }
    });


    initGroupFile();
    initGroupServer();
    initGroupTransfer();
    initGroupDisplay();
    initGroupIrc();
    initGroupFilter();
    initGroupPlugins();
    initStats();
    initTracker();
    initSharing();
    initLogging();

    // Add plugin sections
    pluginSections = ConfigSectionRepository.getInstance().getList();
    for (int i = 0; i < pluginSections.size(); i++) {
      ConfigSection section = (ConfigSection)pluginSections.get(i);
      String name;
      try {
        name = section.configSectionGetName();
      } catch (Exception e) {
        LGLogger.log(LGLogger.ERROR, "A ConfigSection plugin caused an error while trying to call its configSectionGetName function");
        name = "Bad Plugin";
        e.printStackTrace();
      }
      try {
        TreeItem treeItem = null;
        String location = section.configSectionGetParentSection();

        if (location.equalsIgnoreCase(ConfigSection.SECTION_ROOT))
          treeItem = new TreeItem(tree, SWT.NULL);
        else if (location != "") {
          TreeItem treeItemFound = findTreeItem(tree, location);
          if (treeItemFound != null)
            treeItem = new TreeItem(treeItemFound, SWT.NULL);
        }

        if (treeItem == null)
          treeItem = new TreeItem(treePlugins, SWT.NULL);

        Composite c = section.configSectionCreate(cConfigSection);

        treeItem.setText(name);
        treeItem.setData(c);
        treeItem.setData("ID", section.configSectionGetID());
      } catch (Exception e) {
        LGLogger.log(LGLogger.ERROR, "ConfigSection plugin '" + name + "' caused an error");
        e.printStackTrace();
      }
    }
    initSaveButton();

    TreeItem[] items = { tree.getItems()[0] };
    tree.setSelection(items);
    // setSelection doesn't trigger a SelectionListener, so..
    showSection(items[0]);
  }

  private void showSection(TreeItem section) {
    Composite item = (Composite)section.getData();
    if (item != null) {
      layoutConfigSection.topControl = item;
      cConfigSection.layout();
      String sHeader = section.getText();
      section = section.getParentItem();
      while (section != null) {
        sHeader = section.getText() + " : " + sHeader;
        section = section.getParentItem();
      }
      lHeader.setText(" " + sHeader);
    }
  }

  private TreeItem findTreeItem(Tree tree, String ID) {
    TreeItem[] items = tree.getItems();
    for (int i = 0; i < items.length; i++) {
      String itemID = (String)items[i].getData("ID");
      if (itemID != null && itemID.equalsIgnoreCase(ID)) {
        return items[i];
      }
      TreeItem itemFound = findTreeItem(items[i], ID);
      if (itemFound != null)
        return itemFound;
    }
	 return null;
  }

  private TreeItem findTreeItem(TreeItem item, String ID) {
    TreeItem[] subItems = item.getItems();
    for (int i = 0; i < subItems.length; i++) {
      String itemID = (String)subItems[i].getData("ID");
      if (itemID != null && itemID.equalsIgnoreCase(ID)) {
        return subItems[i];
      }

      TreeItem itemFound = findTreeItem(subItems[i], ID);
      if (itemFound != null)
        return itemFound;
    }
    return null;
  }

  private void initSaveButton() {
    GridData gridData;
    Button save = new Button(cConfig, SWT.PUSH);
    Messages.setLanguageText(save, "ConfigView.button.save"); //$NON-NLS-1$
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 80;
    save.setLayoutData(gridData);

    save.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        COConfigurationManager.setParameter("updated", 1); //$NON-NLS-1$
        COConfigurationManager.save();
        filter.save();
        for (int i = 0; i < pluginSections.size(); i++)
          ((ConfigSection)pluginSections.get(i)).configSectionSave();
      }
    });
  }

  private void initGroupPlugins()
  {
    GridData gridData;

    Composite infoGroup = new Composite(cConfigSection, SWT.NULL);
    infoGroup.setLayout(new GridLayout());
    infoGroup.addControlListener(new Utils.LabelWrapControlListener());  

    treePlugins = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treePlugins, ConfigSection.SECTION_PLUGINS);
    treePlugins.setData(infoGroup);
    treePlugins.setData("ID", ConfigSection.SECTION_PLUGINS);


    List pluginIFs = PluginInitializer.getPluginInterfaces();
    Label labelInfo = new Label(infoGroup, SWT.WRAP);
    labelInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    int numPlugins = 0;
    for (int i = 0; i < pluginIFs.size(); i++) {
      PluginInterface pluginIF = (PluginInterface)pluginIFs.get(i);
      Label label = new Label(infoGroup, SWT.NULL);

      Properties p = pluginIF.getPluginProperties();
      String s = p.getProperty("plugin.name", pluginIF.getPluginDirectoryName());
      // Blank means it's internal
      if (s != "") {
        label.setText(" - " + s);
        numPlugins++;
      }
    }
    Messages.setLanguageText(labelInfo, (numPlugins == 0) ? "ConfigView.pluginlist.noplugins"
                                                          : "ConfigView.pluginlist.info");


    ParameterRepository repository = ParameterRepository.getInstance();

    String[] names = repository.getNames();


    for(int i = 0; i < names.length; i++)
    {
      String pluginName = names[i];
      Parameter[] parameters = repository.getParameterBlock(pluginName);

      Composite pluginGroup = new Composite(cConfigSection, SWT.NULL);
      GridLayout pluginLayout = new GridLayout();
      pluginLayout.numColumns = 3;
      pluginGroup.setLayout(pluginLayout);

      TreeItem subItem = new TreeItem(treePlugins, SWT.NULL);
      Messages.setLanguageText(subItem, pluginName);
      subItem.setData(pluginGroup);

      Map parameterToPluginParameter = new HashMap();
      //Add all parameters
      for(int j = 0; j < parameters.length; j++)
      {
        Parameter parameter = parameters[j];
        parameterToPluginParameter.put(parameter,new PluginParameter(pluginGroup,parameter));
      }
      //Check for dependencies
      for(int j = 0; j < parameters.length; j++) {
        Parameter parameter = parameters[j];
        if(parameter instanceof org.gudy.azureus2.pluginsimpl.ui.config.BooleanParameter) {
          List parametersToEnable =
            ((org.gudy.azureus2.pluginsimpl.ui.config.BooleanParameter)parameter).getEnabledOnSelectionParameters();
          List controlsToEnable = new ArrayList();
          Iterator iter = parametersToEnable.iterator();
          while(iter.hasNext()) {
            Parameter parameterToEnable = (Parameter) iter.next();
            PluginParameter pp = (PluginParameter) parameterToPluginParameter.get(parameterToEnable);
            Control[] controls = pp.getControls();
            for(int k = 0 ; k < controls.length ; k++) {
              controlsToEnable.add(controls[k]);
            }
          }

          List parametersToDisable =
          ((org.gudy.azureus2.pluginsimpl.ui.config.BooleanParameter)parameter).getDisabledOnSelectionParameters();
          List controlsToDisable = new ArrayList();
          iter = parametersToDisable.iterator();
          while(iter.hasNext()) {
            Parameter parameterToDisable = (Parameter) iter.next();
            PluginParameter pp = (PluginParameter) parameterToPluginParameter.get(parameterToDisable);
            Control[] controls = pp.getControls();
            for(int k = 0 ; k < controls.length ; k++) {
              controlsToDisable.add(controls[k]);
            }
          }

          Control[] ce = new Control[controlsToEnable.size()];
          Control[] cd = new Control[controlsToDisable.size()];

          if(ce.length + cd.length > 0) {
            IAdditionalActionPerformer ap = new DualChangeSelectionActionPerformer(
                (Control[]) controlsToEnable.toArray(ce),
                (Control[]) controlsToDisable.toArray(cd));
            PluginParameter pp = (PluginParameter) parameterToPluginParameter.get(parameter);
            pp.setAdditionalActionPerfomer(ap);
          }

        }
      }
    }
  }

  private void initGroupFilter() {
    GridData gridData;
    Label label;

    Composite gFilter = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gFilter.setLayoutData(gridData);
    GridLayout layoutFilter = new GridLayout();
    layoutFilter.numColumns = 3;
    gFilter.setLayout(layoutFilter);

    TreeItem itemFilter = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(itemFilter, "ipFilter.shortTitle"); //$NON-NLS-1$
    itemFilter.setData(gFilter);

    // start controls
    gridData = new GridData(GridData.BEGINNING);
    BooleanParameter enabled = new BooleanParameter(gFilter, "Ip Filter Enabled",true); //$NON-NLS-1$
    enabled.setLayoutData(gridData);

    label = new Label(gFilter, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ipFilter.enable"); //$NON-NLS-1$


    gridData = new GridData(GridData.BEGINNING);
    BooleanParameter deny = new BooleanParameter(gFilter, "Ip Filter Allow",false); //$NON-NLS-1$
    enabled.setLayoutData(gridData);

    label = new Label(gFilter, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ipFilter.allow");

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
  }

  private void initGroupIrc() {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gIrc = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeIrc = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeIrc, "ConfigView.section.irc"); //$NON-NLS-1$
    treeIrc.setData(gIrc);

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
  }

  private void initGroupDisplay() {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite cDisplay = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeDisplay = new TreeItem(tree, SWT.NULL);
    // "ConfigView.section.style" says "Interface" in english, which is better
    // than "Display" in "ConfigView.section.display" because Interface is a
    // much broader term.
    Messages.setLanguageText(treeDisplay, "ConfigView.section.style"); //$NON-NLS-1$
    treeDisplay.setData(cDisplay);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cDisplay.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cDisplay.setLayout(layout);

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.opendetails"); //$NON-NLS-1$
    new BooleanParameter(cDisplay, "Open Details"); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.openbar"); //$NON-NLS-1$
    new BooleanParameter(cDisplay, "Open Bar", false); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.closetotray"); //$NON-NLS-1$
    new BooleanParameter(cDisplay, "Close To Tray", true); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minimizetotray"); //$NON-NLS-1$
    new BooleanParameter(cDisplay, "Minimize To Tray", false); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.password"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    new PasswordParameter(cDisplay, "Password").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.passwordconfirm"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 150;
    new PasswordParameter(cDisplay, "Password Confirm").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.passwordmatch"); //$NON-NLS-1$
    passwordMatch = new Label(cDisplay, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 150;
    passwordMatch.setLayoutData(gridData);


    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.allowSendVersion");
    new BooleanParameter(cDisplay, "Send Version Info",true);

    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.confirmationOnExit"); //$NON-NLS-1$
    new BooleanParameter(cDisplay, "confirmationOnExit",false); //$NON-NLS-1$
    
    label = new Label(cDisplay, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.dropdiraction");

    String[] drop_options = {
         "ConfigView.section.style.dropdiraction.opentorrents",
         "ConfigView.section.style.dropdiraction.sharefolder",
         "ConfigView.section.style.dropdiraction.sharefoldercontents",
         "ConfigView.section.style.dropdiraction.sharefoldercontentsrecursive",
     };

    String dropLabels[] = new String[drop_options.length];
    String dropValues[] = new String[drop_options.length];
    for (int i = 0; i < drop_options.length; i++) {

       dropLabels[i] = MessageText.getString( drop_options[i]);
       dropValues[i] = "" + i;
    }
    new StringListParameter(cDisplay, "config.style.dropdiraction", "", dropLabels, dropValues);


    // "Start" Sub-Section
    // -------------------
    Composite gStart = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeStart = new TreeItem(treeDisplay, SWT.NULL);
    Messages.setLanguageText(treeStart, "ConfigView.section.start"); //$NON-NLS-1$
    treeStart.setData(gStart);

    gStart.setLayoutData(new GridData(GridData.FILL_BOTH));
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
    
    // "Display" Sub-Section:
    // ----------------------
    // Any Look & Feel settings that don't really change the way the user 
    // normally interacts
    Composite cLook = new Composite(cConfigSection, SWT.NULL);
    cLook.setLayoutData(new GridData(GridData.FILL_BOTH));
    layout = new GridLayout();
    layout.numColumns = 2;
    cLook.setLayout(layout);
    
    TreeItem treeLook = new TreeItem(treeDisplay, SWT.NULL);
    Messages.setLanguageText(treeLook, "ConfigView.section.display"); //$NON-NLS-1$
    treeLook.setData(cLook);

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.useCustomTabs"); //$NON-NLS-1$
    new BooleanParameter(cLook, "useCustomTab",true); //$NON-NLS-1$
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.showdownloadbasket"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Show Download Basket",false); //$NON-NLS-1$
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.addurlsilently"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Add URL Silently",false); //$NON-NLS-1$
    
    String osName = System.getProperty("os.name");
    if (osName.equals("Windows XP")) {
      label = new Label(cLook, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.style.enableXPStyle"); //$NON-NLS-1$
      final Button enableXPStyle = new Button(cLook, SWT.CHECK);
      boolean enabled = false;
      boolean valid = false;
      try {
        File f =
          new File(
            System.getProperty("java.home")
              + "\\bin\\javaw.exe.manifest");
        if (f.exists()) {
          enabled = true;
        }
        f= FileUtil.getApplicationFile("javaw.exe.manifest");
        if(f.exists()) {
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

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.colorScheme"); //$NON-NLS-1$
    ColorParameter colorScheme = new ColorParameter(cLook, "Color Scheme",0,128,255,true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 50;
    colorScheme.setLayoutData(gridData);

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.guiUpdate"); //$NON-NLS-1$
    int[] values = { 100 , 250 , 500 , 1000 , 2000 , 5000 };
    String[] labels = { "100 ms" , "250 ms" , "500 ms" , "1 s" , "2 s" , "5 s" };
    new IntListParameter(cLook, "GUI Refresh", 250, labels, values);

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.graphicsUpdate"); //$NON-NLS-1$
    int[] gValues = new int[50];
    String[] gLabels = new String[50];
    for(int i = 1 ; i <= 50 ; i++) {
      gValues[i-1] = i;
      gLabels[i-1] = "" + i;
    }
    new IntListParameter(cLook, "Graphics Update", 4, gLabels, gValues);

    if (osName.equals("Linux") && SWT.getPlatform().equals("gtk")) {
     label = new Label(cLook, SWT.NULL);
     Messages.setLanguageText(label, "ConfigView.section.style.verticaloffset"); //$NON-NLS-1$
     new IntParameter(cLook, VerticalAligner.parameterName,28); //$NON-NLS-1$
    }

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.reOrderDelay"); //$NON-NLS-1$
    int[] rValues = new int[51];
    String[] rLabels = new String[51];
    rValues[0] = 0;
    rLabels[0] = MessageText.getString("ConfigView.section.style.reOrderDelay.never");
    for(int i = 1 ; i <= 50 ; i++) {
      rValues[i] = i;
      rLabels[i] = "" + i;
    }
    new IntListParameter(cLook, "ReOrder Delay", 0, rLabels, rValues);


    /**
     * Disabled for the moment because of some side effects
     */
    /*
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysShowTorrentFiles"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Always Show Torrent Files", true); //$NON-NLS-1$
    */

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.useSIUnits"); //$NON-NLS-1$
    new BooleanParameter(cLook, "config.style.useSIUnits",false); //$NON-NLS-1$

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysRefreshMyTorrents"); //$NON-NLS-1$
    new BooleanParameter(cLook, "config.style.refreshMT",false); //$NON-NLS-1$
  }

  private void initGroupTransfer() {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gTransfer = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeTransfer = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeTransfer, "ConfigView.section.transfer"); //$NON-NLS-1$
    treeTransfer.setData(gTransfer);

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
    Messages.setLanguageText(label, "ConfigView.label.slowconnect"); //$NON-NLS-1$
    new BooleanParameter(gTransfer, "Slow Connect", false); //$NON-NLS-1$

    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxuploads"); //$NON-NLS-1$
    final String upLabels[] = new String[299];
    final int upValues[] = new int[299];
    for (int i = 0; i < 299; i++) {
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
    Messages.setLanguageText(label, "ConfigView.label.allowsameip"); //$NON-NLS-1$
    new BooleanParameter(gTransfer, "Allow Same IP Peers", false); //$NON-NLS-1$


    label = new Label(gTransfer, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.prioritizefirstpiece"); //$NON-NLS-1$
    new BooleanParameter(gTransfer, "Prioritize First Piece", false); //$NON-NLS-1$


    if(!System.getProperty("os.name").equals("Mac OS X")) {
      label = new Label(gTransfer, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.playdownloadfinished"); //$NON-NLS-1$
      new BooleanParameter(gTransfer, "Play Download Finished", false); //$NON-NLS-1$
   }
  }

  private void initGroupServer() {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gServer = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeServer = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeServer, "ConfigView.section.server"); //$NON-NLS-1$
    treeServer.setData(gServer);

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
    Messages.setLanguageText(label, "ConfigView.label.serverport"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gServer, "TCP.Listen.Port", 6881).setLayoutData(gridData); //$NON-NLS-1$
  }

  private void initGroupFile() {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    Composite cArea;

    Composite gFile = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeFile = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeFile, "ConfigView.section.files"); //$NON-NLS-1$
    treeFile.setData(gFile);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    gFile.setLayout(layout);
    Label label;

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.zeronewfiles"); //$NON-NLS-1$
    BooleanParameter zeroNew = new BooleanParameter(gFile, "Zero New", false); //$NON-NLS-1$

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.incrementalfile"); //$NON-NLS-1$
    BooleanParameter incremental = new BooleanParameter(gFile, "Enable incremental file creation", false); //$NON-NLS-1$

    //Make the incremental checkbox (button) deselect when zero new is used
    Button[] btnIncremental = {(Button)incremental.getControl()};
    zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnIncremental));

    //Make the zero new checkbox(button) deselct when incremental is used
    Button[] btnZeroNew = {(Button)zeroNew.getControl()};
    incremental.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnZeroNew));

    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.checkOncompletion"); //$NON-NLS-1$
    new BooleanParameter(gFile, "Check Pieces on Completion", true);


    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.usefastresume"); //$NON-NLS-1$
    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    BooleanParameter bpUseResume = new BooleanParameter(cArea, "Use Resume", true); //$NON-NLS-1$

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.saveresumeinterval"); //$NON-NLS-1$

    final String saveResumeLabels[] = new String[19];
    final int saveResumeValues[] = new int[19];
    for (int i = 2; i < 21; i++) {
      saveResumeLabels[i - 2] = " " + i + " min"; //$NON-NLS-1$ //$NON-NLS-2$
      saveResumeValues[i - 2] = i;
    }

    IntListParameter listSave = new IntListParameter(cArea, "Save Resume Interval", 5, saveResumeLabels, saveResumeValues); //$NON-NLS-1$

    Control[] controls = new Control[2];
    controls[0] = label;
    controls[1] = listSave.getControl();
    IAdditionalActionPerformer performer = new ChangeSelectionActionPerformer(controls);
    bpUseResume.setAdditionalActionPerformer(performer);

    // savepath
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.defaultsavepath"); //$NON-NLS-1$

    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    BooleanParameter saveDefault = new BooleanParameter(cArea, "Use default data dir", true); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter pathParameter = new StringParameter(cArea, "Default save path", ""); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);

    Button browse = new Button(cArea, SWT.PUSH);
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
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

    // Move Completed
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.movecompleted"); //$NON-NLS-1$
    BooleanParameter moveCompleted = new BooleanParameter(gFile, "Move Completed When Done", false); //$NON-NLS-1$

    Composite gMoveCompleted = new Composite(gFile, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 15;
    gridData.horizontalSpan = 2;
    gMoveCompleted.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gMoveCompleted.setLayout(layout);

    label = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.directory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter movePathParameter = new StringParameter(gMoveCompleted, "Completed Files Directory", "");
    movePathParameter.setLayoutData(gridData);

    Button browse3 = new Button(gMoveCompleted, SWT.PUSH);
    browse3.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse3.getBackground());
    browse3.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse3.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(movePathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosemovepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          movePathParameter.setValue(path);
        }
      }
    });


    Label lMoveTorrent = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(lMoveTorrent, "ConfigView.label.movetorrent"); //$NON-NLS-1$
    BooleanParameter moveTorrent = new BooleanParameter(gMoveCompleted, "Move Torrent When Done", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    moveTorrent.setLayoutData(gridData);

    Label lMoveOnly = new Label(gMoveCompleted, SWT.NULL);
    Messages.setLanguageText(lMoveOnly, "ConfigView.label.moveonlyusingdefaultsave"); //$NON-NLS-1$
    BooleanParameter moveOnly = new BooleanParameter(gMoveCompleted, "Move Only When In Default Save Dir", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    moveOnly.setLayoutData(gridData);


    controls = new Control[6];
    controls[0] = movePathParameter.getControl();
    controls[1] = browse3;
    controls[2] = lMoveTorrent;
    controls[3] = moveTorrent.getControl();
    controls[4] = lMoveOnly;
    controls[5] = moveOnly.getControl();
    IAdditionalActionPerformer grayPathAndButton2 = new ChangeSelectionActionPerformer(controls);
    moveCompleted.setAdditionalActionPerformer(grayPathAndButton2);


    // Auto-Prioritize
    label = new Label(gFile, SWT.WRAP);
    gridData = new GridData();
    gridData.widthHint = 180;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ConfigView.label.priorityExtensions"); //$NON-NLS-1$

    cArea = new Composite(gFile, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new StringParameter(cArea, "priorityExtensions", "").setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.ignoreCase");
    new BooleanParameter(cArea, "priorityExtensionsIgnoreCase");

    // Confirm Delete
    label = new Label(gFile, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.confirm_data_delete");
    new BooleanParameter(gFile, "Confirm Data Delete", true);


    // Sub-Section: File -> Torrent
    // ----------------------------
    Composite cTorrent = new Composite(cConfigSection, SWT.NULL);

    TreeItem treeTorrent = new TreeItem(treeFile, SWT.NULL);
    Messages.setLanguageText(treeTorrent, "ConfigView.section.torrents");
    treeTorrent.setData(cTorrent);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTorrent.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cTorrent.setLayout(layout);
    
    // Save .Torrent files to..
    
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.savetorrents"); //$NON-NLS-1$
    BooleanParameter saveTorrents = new BooleanParameter(cTorrent, "Save Torrent Files", true); //$NON-NLS-1$

    Composite gSaveTorrents = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    gridData.horizontalSpan = 2;
    gSaveTorrents.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gSaveTorrents.setLayout(layout);

    label = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.savedirectory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter torrentPathParameter = new StringParameter(gSaveTorrents, "General_sDefaultTorrent_Directory", ""); //$NON-NLS-1$ //$NON-NLS-2$
    torrentPathParameter.setLayoutData(gridData);

    Button browse2 = new Button(gSaveTorrents, SWT.PUSH);
    browse2.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse2.getBackground());
    browse2.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse2.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(torrentPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaulttorrentpath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });

    Label lSaveTorrentBackup = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(lSaveTorrentBackup, "ConfigView.label.savetorrentbackup"); //$NON-NLS-1$
    BooleanParameter saveTorrentBackup = new BooleanParameter(gSaveTorrents, "Save Torrent Backup", false); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    saveTorrentBackup.setLayoutData(gridData);

    controls = new Control[4];
    controls[0] = torrentPathParameter.getControl();
    controls[1] = browse2;
    controls[2] = lSaveTorrentBackup;
    controls[3] = saveTorrentBackup.getControl();
    IAdditionalActionPerformer grayPathAndButton1 = new ChangeSelectionActionPerformer(controls);
    saveTorrents.setAdditionalActionPerformer(grayPathAndButton1);


    // Watch Folder
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.watchtorrentfolder"); //$NON-NLS-1$
    BooleanParameter watchFolder = new BooleanParameter(cTorrent, "Watch Torrent Folder", false); //$NON-NLS-1$

    Composite gWatchFolder = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    gridData.horizontalSpan = 2;
    gWatchFolder.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gWatchFolder.setLayout(layout);

    label = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.importdirectory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
//    gridData = new GridData();
//    gridData.widthHint = 220;
    final StringParameter watchFolderPathParameter = new StringParameter(gWatchFolder, "Watch Torrent Folder Path", "");
    watchFolderPathParameter.setLayoutData(gridData);

    Button browse4 = new Button(gWatchFolder, SWT.PUSH);
    browse4.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse4.getBackground());
    browse4.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse4.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(watchFolderPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosewatchtorrentfolderpath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          watchFolderPathParameter.setValue(path);
        }
      }
    });

    Label lWatchTorrentFolderInterval = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lWatchTorrentFolderInterval, "ConfigView.label.watchtorrentfolderinterval"); //$NON-NLS-1$
    final String watchTorrentFolderIntervalLabels[] = new String[5];
    final int watchTorrentFolderIntervalValues[] = new int[5];
    for (int i = 1; i < 6; i++) {
      watchTorrentFolderIntervalLabels[i - 1] = " " + i + " min"; //$NON-NLS-1$ //$NON-NLS-2$
      watchTorrentFolderIntervalValues[i - 1] = i;
    }
    IntListParameter iWatchTorrentFolderIntervalParameter = new IntListParameter(gWatchFolder, "Watch Torrent Folder Interval", 1, watchTorrentFolderIntervalLabels, watchTorrentFolderIntervalValues);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    iWatchTorrentFolderIntervalParameter.setLayoutData(gridData);

    Label lStartWatchedTorrentsStopped = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lStartWatchedTorrentsStopped, "ConfigView.label.startwatchedtorrentsstopped"); //$NON-NLS-1$
    BooleanParameter startWatchedTorrentsStopped = new BooleanParameter(gWatchFolder, "Start Watched Torrents Stopped", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    startWatchedTorrentsStopped.setLayoutData(gridData);
    controls = new Control[6];
    controls[0] = watchFolderPathParameter.getControl();
    controls[1] = browse4;
    controls[2] = lWatchTorrentFolderInterval;
    controls[3] = iWatchTorrentFolderIntervalParameter.getControl();
    controls[4] = lStartWatchedTorrentsStopped;
    controls[5] = startWatchedTorrentsStopped.getControl();
    IAdditionalActionPerformer grayPathAndButton3 = new ChangeSelectionActionPerformer(controls);
    watchFolder.setAdditionalActionPerformer(grayPathAndButton3);

    // locale decoder
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.decoder.label"); //$NON-NLS-1$
  
    LocaleUtilDecoder[] decoders = LocaleUtil.getDecoders();
  
    String decoderLabels[] = new String[decoders.length + 1];
    String decoderValues[] = new String[decoders.length + 1];
  
    decoderLabels[0] = MessageText.getString( "ConfigView.section.file.decoder.nodecoder");
    decoderValues[0] = "";
  
    for (int i = 1; i <= decoders.length; i++) {
      decoderLabels[i] = decoderValues[i] = decoders[i-1].getName();
      }
    Control[] decoder_controls = new Control[2];
    decoder_controls[0] = label;
    decoder_controls[1] = new StringListParameter(cTorrent, "File.Decoder.Default", "", decoderLabels, decoderValues).getControl(); //$NON-NLS-1$
  
      // locale always prompt
  
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.decoder.prompt");
    new BooleanParameter(cTorrent, "File.Decoder.Prompt", false);
  }

  private void initStats() {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gStats = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gStats.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gStats.setLayout(layout);

    TreeItem treeStats = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeStats, "ConfigView.section.stats"); //$NON-NLS-1$
    treeStats.setData(gStats);

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
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
    controls[2] = browse;
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
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
    controls[3] = fileParameter.getControl();
    label = new Label(gStats, SWT.NULL);

    // row

    Label lxslFile = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lxslFile, "ConfigView.section.stats.xslfile"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter xslParameter = new StringParameter(gStats, "Stats XSL File", "" );
    xslParameter.setLayoutData(gridData);
    controls[4] = xslParameter.getControl();
    Label lxslDetails = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lxslDetails, "ConfigView.section.stats.xslfiledetails"); //$NON-NLS-1$
    final String linkFAQ = "http://azureus.sourceforge.net/faq.php#20";
    lxslDetails.setCursor(MainWindow.handCursor);
    lxslDetails.setForeground(MainWindow.blue);
    lxslDetails.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
        Program.launch(linkFAQ);
      }
      public void mouseDown(MouseEvent arg0) {
        Program.launch(linkFAQ);
      }
    });
    // row

    Label lSaveFreq = new Label(gStats, SWT.NULL);

    Messages.setLanguageText(lSaveFreq, "ConfigView.section.stats.savefreq");
    final String spLabels[] = new String[statsPeriods.length];
    final int spValues[] = new int[statsPeriods.length];
    for (int i = 0; i < statsPeriods.length; i++) {
      int num = statsPeriods[i];

      if ( num%3600 == 0 )
        spLabels[i] = " " + (statsPeriods[i]/3600) + " " + 
                             MessageText.getString("ConfigView.section.stats.hours");

      else if ( num%60 == 0 )
        spLabels[i] = " " + (statsPeriods[i]/60) + " " + 
                             MessageText.getString("ConfigView.section.stats.minutes");

      else
        spLabels[i] = " " + statsPeriods[i] + " " + 
                            MessageText.getString("ConfigView.section.stats.seconds");

      spValues[i] = statsPeriods[i];
    }

    controls[5] = lSaveFreq;
    controls[6] = new IntListParameter(gStats, "Stats Period", 0, spLabels, spValues).getControl();
    enableStats.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));
  }  // initStats


  private void initTracker() {
    GridData gridData;
    GridLayout layout;
    Label label;

    // main tab set up
    Composite gMainTab = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 6;
    gMainTab.setLayout(layout);

    TreeItem treeTracker = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeTracker, "ConfigView.section.tracker"); //$NON-NLS-1$
    treeTracker.setData(gMainTab);


    // web tab set up
    Composite gWebTab = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gWebTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 6;
    gWebTab.setLayout(layout);

    TreeItem treeWeb = new TreeItem(treeTracker, SWT.NULL);
    Messages.setLanguageText(treeWeb, "ConfigView.section.tracker.web");
    treeWeb.setData(gWebTab);


    // extensions tab set up
    Composite gExtTab = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gExtTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 6;
    gExtTab.setLayout(layout);

    TreeItem treeExt = new TreeItem(treeTracker, SWT.NULL);
    Messages.setLanguageText(treeExt, "ConfigView.section.tracker.extensions");
    treeExt.setData(gExtTab);


      // MAIN TAB DATA
    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollinterval");
    gridData = new GridData();
    label.setLayoutData( gridData );

    	// Poll Group
    
    Group gPollStuff = new Group(gMainTab, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 5;
    gPollStuff.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gPollStuff.setLayout(layout);

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmin");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMin = new IntParameter(gPollStuff, "Tracker Poll Interval Min", TRHost.DEFAULT_MIN_RETRY_DELAY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalMin.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmax");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMax = new IntParameter(gPollStuff, "Tracker Poll Interval Max", TRHost.DEFAULT_MAX_RETRY_DELAY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalMax.setLayoutData( gridData );

    // row

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincby");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncBy = new IntParameter(gPollStuff, "Tracker Poll Inc By", TRHost.DEFAULT_INC_BY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalIncBy.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincper");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncPer = new IntParameter(gPollStuff, "Tracker Poll Inc Per", TRHost.DEFAULT_INC_PER );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalIncPer.setLayoutData( gridData );

    
    // scrape + cache group
 
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.scrapeandcache");
    gridData = new GridData();
    label.setLayoutData( gridData );
    
    Group gScrapeCache = new Group(gMainTab, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 5;
    gScrapeCache.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gScrapeCache.setLayout(layout);
    
    // row
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcescrapepercentage");
    gridData = new GridData();
    gridData.horizontalSpan	= 1;
    label.setLayoutData( gridData );

    IntParameter scrapeannouncepercentage = new IntParameter(gScrapeCache, "Tracker Scrape Retry Percentage", TRHost.DEFAULT_SCRAPE_RETRY_PERCENTAGE );

    gridData = new GridData();
    gridData.widthHint = 30;
    scrapeannouncepercentage.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.scrapecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter scrapeCachePeriod = new IntParameter(gScrapeCache, "Tracker Scrape Cache", TRHost.DEFAULT_SCRAPE_CACHE_PERIOD );

    gridData = new GridData();
    gridData.widthHint = 30;
    scrapeCachePeriod.setLayoutData( gridData );
    
 
    // row

    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcecacheminpeers");
    gridData = new GridData();
    gridData.horizontalSpan	= 1;
    label.setLayoutData( gridData );

    IntParameter announceCacheMinPeers = new IntParameter(gScrapeCache, "Tracker Announce Cache Min Peers", TRHost.DEFAULT_ANNOUNCE_CACHE_PEER_THRESHOLD );

    gridData = new GridData();
    gridData.widthHint = 30;
    announceCacheMinPeers.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter announceCachePeriod = new IntParameter(gScrapeCache, "Tracker Announce Cache", TRHost.DEFAULT_ANNOUNCE_CACHE_PERIOD );

    gridData = new GridData();
    gridData.widthHint = 30;
    announceCachePeriod.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.maxpeersreturned");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxPeersReturned = new IntParameter(gMainTab, "Tracker Max Peers Returned", 100 );

    gridData = new GridData();
    gridData.widthHint = 50;
    maxPeersReturned.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.ip");

    final StringParameter tracker_ip = new StringParameter(gMainTab, "Tracker IP", "" );

    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalSpan = 2;
    tracker_ip.setLayoutData( gridData );

    Button check_button = new Button(gMainTab, SWT.PUSH);

    Messages.setLanguageText(check_button, "ConfigView.section.tracker.checkip"); //$NON-NLS-1$

    final Display display = gMainTab.getDisplay();

    check_button.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
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
           }); // setIPSetterCallback
         }
    });

    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.port");

    IntParameter tracker_port = new IntParameter(gMainTab, "Tracker Port", TRHost.DEFAULT_PORT );

    gridData = new GridData();
    gridData.widthHint = 50;
    tracker_port.setLayoutData( gridData );

    final BooleanParameter nonsslEnable = new BooleanParameter(gMainTab, "Tracker Port Enable", true);

    Control[] non_ssl_controls = new Control[1];
    non_ssl_controls[0] = tracker_port.getControl();

    nonsslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( non_ssl_controls ));

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.forceport");

    BooleanParameter forcePortDetails = new BooleanParameter(gMainTab,  "Tracker Port Force External", false);
    label = new Label(gMainTab, SWT.NULL);


    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.sslport");

    IntParameter tracker_port_ssl = new IntParameter(gMainTab, "Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );

    gridData = new GridData();
    gridData.widthHint = 50;
    tracker_port_ssl.setLayoutData( gridData );

    final BooleanParameter sslEnable = new BooleanParameter(gMainTab, "Tracker Port SSL Enable", false);

    Control[] ssl_controls = new Control[1];
    ssl_controls[0] = tracker_port_ssl.getControl();

    sslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( ssl_controls ));

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.sslport.info");
    final String linkFAQ = "http://azureus.sourceforge.net/faq.php#19";
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
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    Control[] f_controls = new Control[1];
    f_controls[0] = forcePortDetails.getControl();

    IAdditionalActionPerformer f_enabler =
      new GenericActionPerformer(f_controls) {
        public void performAction()
        {
          boolean selected =  nonsslEnable.isSelected() ||
          sslEnable.isSelected();
    
          controls[0].setEnabled( selected );
        }
      };

    nonsslEnable.setAdditionalActionPerformer(f_enabler);
    sslEnable.setAdditionalActionPerformer(f_enabler);


    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.publicenable");

    BooleanParameter publicPublish = new BooleanParameter(gMainTab, "Tracker Public Enable", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    publicPublish.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenableweb");

    final BooleanParameter passwordEnableWeb = new BooleanParameter(gMainTab, "Tracker Password Enable Web", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    passwordEnableWeb.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent");

    final BooleanParameter passwordEnableTorrent = new BooleanParameter(gMainTab, "Tracker Password Enable Torrent", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    passwordEnableTorrent.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent.info");

    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.username");

    final StringParameter tracker_username = new StringParameter(gMainTab, "Tracker Username", "" );

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.widthHint = 100;
    tracker_username.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.password");

    final PasswordParameter tracker_password = new PasswordParameter(gMainTab, "Tracker Password" );

    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalSpan = 2;
    tracker_password.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);


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


    // **** web tab ****


    // row

    label = new Label(gWebTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.publishenable");

    BooleanParameter enablePublish = new BooleanParameter(gWebTab, "Tracker Publish Enable", true);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enablePublish.setLayoutData( gridData );

    label = new Label(gWebTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.publishenabledetails");

    BooleanParameter enablePublishDetails = new BooleanParameter(gWebTab, "Tracker Publish Enable Details", true);
    label = new Label(gWebTab, SWT.NULL);

    Control[] publish_controls = new Control[1];
    publish_controls[0] = enablePublishDetails.getControl();

    enablePublish.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( publish_controls ));

    // row

    label = new Label(gWebTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.torrentsperpage");

    final IntParameter tracker_skip = new IntParameter(gWebTab, "Tracker Skip", 0 );

    gridData = new GridData();
    gridData.horizontalSpan = 1;
    gridData.widthHint = 100;
    tracker_skip.setLayoutData( gridData );

    label = new Label(gWebTab, SWT.NULL);
    label = new Label(gWebTab, SWT.NULL);
    label = new Label(gWebTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );


    // **** extensions tab ****

    // row

    label = new Label(gExtTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.sendpeerids");

    BooleanParameter sendPeerIDs = new BooleanParameter(gExtTab, "Tracker Send Peer IDs", true);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enablePublish.setLayoutData( gridData );

    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);

    // row

    label = new Label(gExtTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.enableudp");

    BooleanParameter enableUDP = new BooleanParameter(gExtTab, "Tracker Port UDP Enable", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableUDP.setLayoutData( gridData );

    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);
    label = new Label(gExtTab, SWT.NULL);


  } // initTracker

  private void initSharing() {
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gSharing = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gSharing.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gSharing.setLayout(layout);

    TreeItem treeSharing = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(treeSharing, "ConfigView.section.sharing");
    treeSharing.setData(gSharing);

    // row

    label = new Label(gSharing, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.sharing.usessl");
    BooleanParameter sharingSSL = new BooleanParameter(gSharing, "Sharing Use SSL", false);

    label = new Label(gSharing, SWT.NULL);
  }


  private void initLogging() {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite gLogging = new Composite(cConfigSection, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gLogging.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gLogging.setLayout(layout);

    TreeItem itemLogging = new TreeItem(tree, SWT.NULL);
    Messages.setLanguageText(itemLogging, "ConfigView.section.logging"); //$NON-NLS-1$
    itemLogging.setData(gLogging);

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
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
    controls[2] = browse;
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
      DirectoryDialog dialog = new DirectoryDialog(cConfig.getShell(), SWT.APPLICATION_MODAL);
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
      int  num = logFileSizes[i];
      lmLabels[i] = " " + num + " MB";
      lmValues[i] = num;
    }

    controls[3] = new IntListParameter(gLogging, "Logging Max Size", 0, lmLabels, lmValues).getControl();
    enableLogging.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));
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
    cConfig.setSize(cConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  public void delete() {
    MainWindow.getWindow().setConfig(null);
    for (int i = 0; i < pluginSections.size(); i++)
      ((ConfigSection)pluginSections.get(i)).configSectionDelete();
    TreeItem[] items = tree.getItems();
    for (int i = 0; i < items.length; i++)
      items[i].setData(null);
    Utils.disposeComposite(cConfig);
  }

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
    new IpFilterEditor(cConfig.getDisplay(), table, filter.getIpRanges(), range);
    noChange = false;
  }

  public void addRange() {
    new IpFilterEditor(cConfig.getDisplay(), table, filter.getIpRanges(), null);
    noChange = false;
  }

}
