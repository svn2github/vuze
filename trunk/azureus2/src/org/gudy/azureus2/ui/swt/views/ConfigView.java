/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.views.configsections.*;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Olivier
 *
 */
public class ConfigView extends AbstractIView {
	private static final LogIDs LOGID = LogIDs.GUI;
  public static final String sSectionPrefix = "ConfigView.section.";
  
  AzureusCore		azureus_core;
  
  Composite cConfig;
  Composite cConfigSection;
  StackLayout layoutConfigSection;
  Label lHeader;
  Font headerFont;
  Tree tree;
  TreeItem treePlugins;
  ArrayList pluginSections;

  /**
   * Main Initializer
   * 
   * @param _azureus_core
   */
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
    	Logger.log(new LogEvent(LOGID, "Error initializing ConfigView", e));
    }



    // Add sections
    /** How to add a new section
     * 1) Create a new implementation of ConfigSectionSWT in a new file
     *    (Use the ConfigSectionTMP.java as a template if it's still around)
     * 2) import it into here
     * 3) add it to the internal sections list
     */
    pluginSections = ConfigSectionRepository.getInstance().getList();

    ConfigSection[] internalSections = { new ConfigSectionConnection(),
                                         new ConfigSectionConnectionProxy(),
                                         new ConfigSectionConnectionAdvanced(),
                                         new ConfigSectionTransfer(),
                                         new ConfigSectionTransferLAN(),
                                         new ConfigSectionFile(), 
                                         new ConfigSectionFileTorrents(),
                                         new ConfigSectionFilePerformance(),
                                         new ConfigSectionInterface(),
                                         new ConfigSectionInterfaceLanguage(),
                                         new ConfigSectionInterfaceStart(),
                                         new ConfigSectionInterfaceDisplay(),
                                         new ConfigSectionMode(),
                                         new ConfigSectionIPFilter(azureus_core),
                                         new ConfigSectionPlugins(this, azureus_core),
                                         new ConfigSectionStats(),
                                         new ConfigSectionTracker(azureus_core),
                                         new ConfigSectionTrackerClient(),
                                         new ConfigSectionTrackerServer(azureus_core),
                                         new ConfigSectionSecurity(),
                                         new ConfigSectionSharing(),
                                         new ConfigSectionLogging()
                                        };
    
    pluginSections.addAll(0, Arrays.asList(internalSections));

    for (int i = 0; i < pluginSections.size(); i++) {
   
    	// slip the non-standard "plugins" initialisation inbetween the internal ones
    	// and the plugin ones so plugin ones can be children of it
    	
      boolean	plugin_section = i >= internalSections.length;
      
      ConfigSection section = (ConfigSection)pluginSections.get(i);
      
      if (section instanceof ConfigSectionSWT || section instanceof UISWTConfigSection ) {
        String name;
        try {
          name = section.configSectionGetName();
         } catch (Exception e) {
        	 Logger.log(new LogEvent(LOGID, "A ConfigSection plugin caused an "
							+ "error while trying to call its "
							+ "configSectionGetName function", e));
          name = "Bad Plugin";
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
          
          if(i == 0) {
            Composite c;
            if ( section instanceof ConfigSectionSWT ){
          	  
          	  c = ((ConfigSectionSWT)section).configSectionCreate(sc);
          	  
            }else{
   
            	  c = ((UISWTConfigSection)section).configSectionCreate(sc);
            }
            sc.setContent(c);
          }
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
          
          // ConfigSectionPlugins is special because it has to handle the
          // PluginConfigModel config pages
          if (section instanceof ConfigSectionPlugins)
          	((ConfigSectionPlugins)section).initPluginSubSections();
        } catch (Exception e) {
        	Logger.log(new LogEvent(LOGID, "ConfigSection plugin '" + name
							+ "' caused an error", e));
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
    	
      ConfigSection configSection = (ConfigSection)section.getData("ConfigSectionSWT");
      
      if (configSection != null) {
    	  
        Control previous = item.getContent();
        if (previous instanceof Composite) {
        	configSection.configSectionDelete();
          Utils.disposeComposite((Composite)previous,true);
        }
        
        Composite c;
        
        if ( configSection instanceof ConfigSectionSWT ){
      	  
      	  c = ((ConfigSectionSWT)configSection).configSectionCreate(item);
      	  
        }else{

          c = ((UISWTConfigSection)configSection).configSectionCreate(item);
        }
        
        item.setContent(c);
        
        c.layout();
      }
      layoutConfigSection.topControl = item;
      
      Composite c = (Composite)item.getContent();
      
      item.setMinSize(c.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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
    lHeader.setText(" " + sHeader.replaceAll("&", "&&"));
  }


  private Composite createConfigSection(String sNameID) {
    return createConfigSection(null, sNameID, -1, true);
  }

  private Composite createConfigSection(String sNameID, int position) {
    return createConfigSection(null, sNameID, position, true);
  }

  public Composite createConfigSection(TreeItem treeItemParent, 
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

  public TreeItem findTreeItem(String ID) {
  	return findTreeItem((Tree)null, ID);
  }

  private TreeItem findTreeItem(Tree tree, String ID) {
  	if (tree == null)
  		tree = this.tree;
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

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return cConfig;
  }

  public void updateLanguage() {
    super.updateLanguage();
    updateHeader(tree.getSelection()[0]);
//    cConfig.setSize(cConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  public void delete() {
    MainWindow.getWindow().clearConfig();
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

  public void
  selectSection(
  	Class	config_section_class )
  {
	  TreeItem[]	items = tree.getItems();
	  
	  for (int i=0;i<items.length;i++){
		  
		  TreeItem	item = items[i];
		  	    	
		  ConfigSection section = (ConfigSection)item.getData("ConfigSectionSWT");
			  
		  if ( section != null && section.getClass() == config_section_class ){
				  
			  tree.setSelection( new TreeItem[]{ item });
			  
			  showSection( item );
			  
			  break;
		  }
	  }
  }
}
