/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.*;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ParameterRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.config.plugins.PluginParameter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.core3.logging.LGLogger;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.configsections.*;

import com.aelitis.azureus.core.*;

/**
 * @author Olivier
 *
 */
public class ConfigView extends AbstractIView {
  private static final String sSectionPrefix = "ConfigView.section.";
  
  /*
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
      550,600,650,700,750,
      800,900,1000,1100,1200,1300,1400,1500,
      1750,2000,2250,2500,2750,3000,
      3500,4000,4500,5000 };
  */

  AzureusCore		azureus_core;
  
  Composite cConfig;
  Composite cConfigSection;
  StackLayout layoutConfigSection;
  Label lHeader;
  Font headerFont;
  Tree tree;
  TreeItem treePlugins;
  ArrayList pluginSections;

  public 
  ConfigView(
  	AzureusCore		_azureus_core ) 
  {
  	azureus_core	= _azureus_core;
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
    try {
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
      headerFont = new Font(d, fontData);
      lHeader.setFont(headerFont);
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
          //Check that at least an item is selected
          //OSX lets you select nothing in the tree for example when a child is selected
          //and you close its parent.
          if(tree.getSelection().length > 0)
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
    } catch (Exception e) {
      LGLogger.log(LGLogger.ERROR, "Error initializing ConfigView");
      e.printStackTrace();
    }



    // Add sections
    /** How to add a new section
     * 1) Create a new implementation of ConfigSectionSWT in a new file
     *    (Use the ConfigSectionTMP.java as a template if it's still around)
     * 2) import it into here
     * 3) add it to the internal sections list
     */
    pluginSections = ConfigSectionRepository.getInstance().getList();

    ConfigSection[] internalSections = { new ConfigSectionFile(), 
                                         new ConfigSectionFileTorrents(),
                                         new ConfigSectionFilePerformance(),
                                         new ConfigSectionServer(),
                                         new ConfigSectionTransfer(),
                                         new ConfigSectionInterface(),
                                         new ConfigSectionInterfaceStart(),
                                         new ConfigSectionInterfaceDisplay(),
                                         new ConfigSectionIPFilter(azureus_core),
                                         new ConfigSectionStats(),
                                         new ConfigSectionTracker(azureus_core),
                                         new ConfigSectionTrackerExt(),
                                         new ConfigSectionSecurity(),
                                         new ConfigSectionSharing(),
                                         new ConfigSectionLogging()
                                        };
    
    pluginSections.addAll(0, Arrays.asList(internalSections));

    for (int i = 0; i < pluginSections.size(); i++) {
   
    	// slip the non-standard "plugins" initialisation inbetween the internal ones
    	// and the plugin ones so plugin ones can be children of it
    	
      boolean	plugin_section = i >= internalSections.length;
      
      if ( i == internalSections.length ){
        // for now, init plugins seperately
        try {
          initGroupPlugins();
        } catch (Exception e) {
          LGLogger.log(LGLogger.ERROR, "Error initializing ConfigView.Plugins");
          e.printStackTrace();
        }   	
      }
      
      ConfigSection section = (ConfigSection)pluginSections.get(i);
      if (section instanceof ConfigSectionSWT) {
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
  
          ScrolledComposite sc = new ScrolledComposite(cConfigSection, SWT.H_SCROLL | SWT.V_SCROLL);
          sc.setExpandHorizontal(true);
          sc.setExpandVertical(true);
          sc.setLayoutData(new GridData(GridData.FILL_BOTH));
  
          //Composite c = ((ConfigSectionSWT)section).configSectionCreate(sc);
  
          String	section_key = name;
          
          if ( plugin_section ){
          		// if resource exists without prefix then use it as plugins don't
          		// need to start with the prefix
          	
          	if ( !MessageText.keyExists(section_key)){
          		
          		section_key = sSectionPrefix + name;
          	}
          	
          }else{
          	
          	section_key = sSectionPrefix + name;
          }
          
          Messages.setLanguageText(treeItem, section_key);
          treeItem.setData("Panel", sc);
          treeItem.setData("ID", name);
          treeItem.setData("ConfigSectionSWT", section);
          
          //sc.setContent(c);
        } catch (Exception e) {
          LGLogger.log(LGLogger.ERROR, "ConfigSection plugin '" + name + "' caused an error");
          e.printStackTrace();
        }
      }
    }
    
 


    initSaveButton();

    TreeItem[] items = { tree.getItems()[0] };
    tree.setSelection(items);
    // setSelection doesn't trigger a SelectionListener, so..
    showSection(items[0]);
  }

  private void showSection(TreeItem section) {
    ScrolledComposite item = (ScrolledComposite)section.getData("Panel");

    if (item != null) {
      ConfigSectionSWT configSection = (ConfigSectionSWT)section.getData("ConfigSectionSWT");
      if (configSection != null) {
        Composite c = ((ConfigSectionSWT)configSection).configSectionCreate(item);
        item.setContent(c);
        c.layout();
        section.setData("ConfigSectionSWT", null);
      }
      layoutConfigSection.topControl = item;
      
      ScrolledComposite sc = (ScrolledComposite)item;
      Composite c = (Composite)sc.getContent();
      
      sc.setMinSize(c.computeSize(SWT.DEFAULT, SWT.DEFAULT));
      cConfigSection.layout();
      
      updateHeader(section);
    }
  }

  private void updateHeader(TreeItem section) {
    if (section == null)
      return;

    String sHeader = section.getText();
    section = section.getParentItem();
    while (section != null) {
      sHeader = section.getText() + " : " + sHeader;
      section = section.getParentItem();
    }
    lHeader.setText(" " + sHeader);
  }


  private Composite createConfigSection(String sNameID) {
    return createConfigSection(null, sNameID, -1, true);
  }

  private Composite createConfigSection(String sNameID, int position) {
    return createConfigSection(null, sNameID, position, true);
  }

  private Composite createConfigSection(TreeItem treeItemParent, 
                                        String sNameID, 
                                        int position, 
                                        boolean bPrefix) {
    ScrolledComposite sc = new ScrolledComposite(cConfigSection, SWT.H_SCROLL | SWT.V_SCROLL);
    sc.setExpandHorizontal(true);
    sc.setExpandVertical(true);
    sc.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite cConfigSection = new Composite(sc, SWT.NULL);

    TreeItem treeItem;
    if (treeItemParent == null) {
      if (position >= 0)
        treeItem = new TreeItem(tree, SWT.NULL, position);
      else
        treeItem = new TreeItem(tree, SWT.NULL);
    } else {
      if (position >= 0)
        treeItem = new TreeItem(treeItemParent, SWT.NULL, position);
      else
        treeItem = new TreeItem(treeItemParent, SWT.NULL);
    }
    Messages.setLanguageText(treeItem, ((bPrefix) ? sSectionPrefix : "") + sNameID);
    treeItem.setData("Panel", sc);
    treeItem.setData("ID", sNameID);

    sc.setContent(cConfigSection);
    return cConfigSection;
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

        for (int i = 0; i < pluginSections.size(); i++)
          ((ConfigSection)pluginSections.get(i)).configSectionSave();
      }
    });
  }

  private void initGroupPlugins()
  {
    Label label;

    Composite infoGroup = createConfigSection(ConfigSection.SECTION_PLUGINS, 6);
    TreeItem treePlugins = findTreeItem(tree, ConfigSection.SECTION_PLUGINS);
    infoGroup.setLayout(new GridLayout());
    infoGroup.addControlListener(new Utils.LabelWrapControlListener());  

    String	sep = System.getProperty("file.separator");
    
    String sUserPluginDir 	= FileUtil.getUserFile( "plugins" ).toString(); 
    
    if ( !sUserPluginDir.endsWith(sep)){
    	sUserPluginDir += sep;
    }
    
    String sAppPluginDir 	= FileUtil.getApplicationFile( "plugins" ).toString();
    
    if ( !sAppPluginDir.endsWith(sep)){
    	sAppPluginDir += sep;
    }
    
    label = new Label(infoGroup, SWT.WRAP);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.pluginlist.whereToPut");

    
    
    label = new Label(infoGroup, SWT.WRAP);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    label.setLayoutData(gridData);
    label.setText(sUserPluginDir);
    label.setForeground(Colors.blue);
    label.setCursor(Cursors.handCursor);
    
    final String _sUserPluginDir = sUserPluginDir;
    
    //TODO : Fix it for windows
    label.addMouseListener(new MouseAdapter() {
      public void mouseUp(MouseEvent arg0) {
        if(_sUserPluginDir.endsWith("/plugins/")) {
          File f = new File(_sUserPluginDir);
          if(f.exists() && f.isDirectory()) {
            Program.launch(_sUserPluginDir);
          } else {
            String azureusDir = _sUserPluginDir.substring(0,_sUserPluginDir.length() - 9);
            System.out.println(azureusDir);
            Program.launch(azureusDir);
          }
        }
      }
    });

    label = new Label(infoGroup, SWT.WRAP);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.pluginlist.whereToPutOr");

    label = new Label(infoGroup, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    label.setLayoutData(gridData);
    label.setText(sAppPluginDir);
    label.setForeground(Colors.blue);
    label.setCursor(Cursors.handCursor);
    
 
    final String _sAppPluginDir = sAppPluginDir;
    
    //TODO : Fix it for windows
    label.addMouseListener(new MouseAdapter() {
      public void mouseUp(MouseEvent arg0) {
        if(_sAppPluginDir.endsWith("/plugins/")) {
          File f = new File(_sAppPluginDir);
          if(f.exists() && f.isDirectory()) {
            Program.launch(_sAppPluginDir);
          } else {
            String azureusDir = _sAppPluginDir.substring(0,_sAppPluginDir.length() - 9);
            System.out.println(azureusDir);
            Program.launch(azureusDir);
          }
        }
      }
    });
    
    List pluginIFs = Arrays.asList( azureus_core.getPluginManager().getPlugins());
    
    Collections.sort( 
    		pluginIFs,
			new Comparator()
			{
    			public int
				compare(
					Object	o1,
					Object	o2 )
    			{
    				return( ((PluginInterface)o1).getPluginName().compareToIgnoreCase(((PluginInterface)o2).getPluginName()));
    			}
			});
    
    Label labelInfo = new Label(infoGroup, SWT.WRAP);
    labelInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    int numPlugins = 0;
    for (int i = 0; i < pluginIFs.size(); i++) {
      PluginInterface pluginIF = (PluginInterface)pluginIFs.get(i);
 
      Properties p = pluginIF.getPluginProperties();
      
      String plugin_name = pluginIF.getPluginName();
      
      String plugin_version = pluginIF.getPluginVersion();
      
      String sDirName = pluginIF.getPluginDirectoryName();
      
      boolean	user_plugin = false;
      
      if (sDirName.length() > sUserPluginDir.length() && 
          sDirName.substring(0, sUserPluginDir.length()).equals(sUserPluginDir)){
      	
      	sDirName = sDirName.substring(sUserPluginDir.length());
      
      	user_plugin = true;
      	
      }else if (sDirName.length() > sAppPluginDir.length() && 
            sDirName.substring(0, sAppPluginDir.length()).equals(sAppPluginDir)){
      	
      	sDirName = sDirName.substring(sAppPluginDir.length());
      }
      
      // Blank means it's internal
      
      if (!sDirName.equals("")){
      	
        label = new Label(infoGroup, SWT.NULL);

      	String	broken_str = pluginIF.isOperational()?"":(" - " + MessageText.getString("ConfigView.pluginlist.broken"));
      	
      	String shared_str = user_plugin?"":" [" + MessageText.getString("ConfigView.pluginlist.shared") + "]";
      	
        label.setText( " - " + plugin_name + (plugin_version==null?"":(" " + plugin_version ))+ " (" + sDirName + ")" + broken_str + shared_str );
        numPlugins++;
      }
    }
    Messages.setLanguageText(labelInfo, (numPlugins == 0) ? "ConfigView.pluginlist.noplugins"
                                                          : "ConfigView.pluginlist.info");

    	// lastly the built-in plugins
    
    label = new Label(infoGroup, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.pluginlist.coreplugins");

    for (int i = 0; i < pluginIFs.size(); i++) {
        PluginInterface pluginIF = (PluginInterface)pluginIFs.get(i);
   
        Properties p = pluginIF.getPluginProperties();
        
        String plugin_name = pluginIF.getPluginName();

        String sDirName = pluginIF.getPluginDirectoryName();
        
        if ( sDirName.equals( "" )){
        	
            label = new Label(infoGroup, SWT.NULL);

            label.setText(" - " + plugin_name );
        }
    }
    
    ParameterRepository repository = ParameterRepository.getInstance();

    String[] names = repository.getNames();


    Arrays.sort( names );
    
    for(int i = 0; i < names.length; i++)
    {
      String pluginName = names[i];
      Parameter[] parameters = repository.getParameterBlock(pluginName);

      // Note: 2070's plugin documentation for PluginInterface.addConfigUIParameters
      //       said to pass <"ConfigView.plugins." + displayName>.  This was
      //       never implemented in 2070.  2070 read the key <displayName> without
      //       the prefix.
      //
      //       2071+ uses <sSectionPrefix ("ConfigView.section.plugins.") + pluginName>
      //       and falls back to <displayName>.  Since 
      //       <"ConfigView.plugins." + displayName> was never implemented in the
      //       first place, a check for it has not been created
      boolean bUsePrefix = MessageText.keyExists(sSectionPrefix + "plugins." + pluginName);
      Composite pluginGroup = createConfigSection(treePlugins, pluginName, -1, bUsePrefix);
      GridLayout pluginLayout = new GridLayout();
      pluginLayout.numColumns = 3;
      pluginGroup.setLayout(pluginLayout);

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
        if(parameter instanceof org.gudy.azureus2.pluginsimpl.local.ui.config.BooleanParameterImpl) {
          List parametersToEnable =
            ((org.gudy.azureus2.pluginsimpl.local.ui.config.BooleanParameterImpl)parameter).getEnabledOnSelectionParameters();
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
          ((org.gudy.azureus2.pluginsimpl.local.ui.config.BooleanParameterImpl)parameter).getDisabledOnSelectionParameters();
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
  }

  public void updateLanguage() {
    super.updateLanguage();
    updateHeader(tree.getSelection()[0]);
//    cConfig.setSize(cConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  public void delete() {
    MainWindow.getWindow().setConfig(null);
    for (int i = 0; i < pluginSections.size(); i++)
      ((ConfigSection)pluginSections.get(i)).configSectionDelete();
    pluginSections.clear();
    if(! tree.isDisposed()) {
	    TreeItem[] items = tree.getItems();
	    for (int i = 0; i < items.length; i++) {
	      Composite c = (Composite)items[i].getData("Panel");
	      Utils.disposeComposite(c);
	      items[i].setData("Panel", null);
	
	      items[i].setData("ConfigSectionSWT", null);
	    }
    }
    Utils.disposeComposite(cConfig);

  	if (headerFont != null && !headerFont.isDisposed()) {
  		headerFont.dispose();
  		headerFont = null;
  	}
  }

  public String getFullTitle() {
    return MessageText.getString("ConfigView.title.full"); //$NON-NLS-1$
  }

}
