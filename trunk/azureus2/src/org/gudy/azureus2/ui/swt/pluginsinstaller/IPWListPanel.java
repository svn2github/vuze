/*
 * Created on 29 nov. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;


/**
 * @author Olivier Chalouhi
 *
 */
public class IPWListPanel extends AbstractWizardPanel {

  Table pluginList;
  
  public 
  IPWListPanel(
	Wizard 					wizard, 
	IWizardPanel 			previous ) 
  {
	super(wizard, previous);
  }


  public void 
  show() 
  {
    wizard.setTitle(MessageText.getString("installPluginsWizard.list.title"));
    wizard.setErrorMessage("");
    
	Composite rootPanel = wizard.getPanel();
	GridLayout layout = new GridLayout();
	layout.numColumns = 1;
	rootPanel.setLayout(layout);

	Composite panel = new Composite(rootPanel, SWT.NULL);
	GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
	panel.setLayoutData(gridData);
	layout = new GridLayout();
	layout.numColumns = 1;
	panel.setLayout(layout);
	
	final Label lblStatus = new Label(panel,SWT.NULL);
	Messages.setLanguageText(lblStatus,"installPluginsWizard.list.loading");
	
	pluginList = new Table(panel,SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE); 
	pluginList.setHeaderVisible(true);
	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	data.heightHint = 120;
	pluginList.setLayoutData(data);
	
	
	TableColumn tcName = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcName,"installPluginsWizard.list.name");
	tcName.setWidth(200);
	
	TableColumn tcVersion = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcVersion,"installPluginsWizard.list.version");
	tcVersion.setWidth(150);
		
	
	Label lblDescription = new Label(panel,SWT.NULL);
	Messages.setLanguageText(lblDescription,"installPluginsWizard.list.description");
	
	final StyledText txtDescription = new StyledText(panel,SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
	txtDescription.setEditable(false);
	
	data = new GridData(GridData.FILL_HORIZONTAL);
	data.heightHint = 100;
	txtDescription.setLayoutData(data);

	AEThread listLoader = new AEThread("Plugin List Loader") {
	  public void runSupport() {
	    final StandardPlugin plugins[];
	    try {
	      plugins = wizard.getAzureusCore().getPluginManager().getPluginInstaller().getStandardPlugins();
	      
	      Arrays.sort( 
	      	plugins,
		  	new Comparator()
			{
	      		public int 
				compare(
					Object o1, 
					Object o2)
	      		{
	      			return(((StandardPlugin)o1).getName().compareTo(((StandardPlugin)o2).getName()));
	      		}
			});
			
	    } catch(final Exception e) {
	    	
	    	Debug.printStackTrace(e);
		    wizard.getDisplay().asyncExec(new AERunnable() {
			      public void runSupport() {
			      	txtDescription.setText( Debug.getNestedExceptionMessage(e));
			      }
		    });
		    
	    	return;
	    }
	    
	    wizard.getDisplay().asyncExec(new AERunnable() {
	      public void runSupport() {
	        Messages.setLanguageText(lblStatus,"installPluginsWizard.list.loaded");
	        for(int i = 0 ; i < plugins.length ; i++) {
	          StandardPlugin plugin = plugins[i];
	          if(plugin.getAlreadyInstalledPlugin() == null) {
	            TableItem item = new TableItem(pluginList,SWT.NULL);
	            item.setData(plugin);
	            item.setText(0,plugin.getName());
	            item.setText(1,plugin.getVersion());
	          }
	        }
	      }
	    });
	  }
	};
	
	listLoader.setDaemon(true);
	
	listLoader.start();
	
	
	pluginList.addListener(SWT.Selection,new Listener() {
	  public void handleEvent(Event e) {
	    if(pluginList.getSelectionCount() > 0) {
	      txtDescription.setText( MessageText.getString( "installPluginsWizard.details.loading"));

	      wizard.getDisplay().asyncExec(new AERunnable() {
		      public void runSupport() {

		      	TableItem itemSelected = pluginList.getSelection()[0];
		      	txtDescription.setText( ((StandardPlugin)itemSelected.getData()).getDescription());
		      }
	      	});
	    }
	    updateList();
	  }
	});
  }
  
	public boolean 
	isNextEnabled() 
	{
	   return true;
	}
	
	public boolean 
	isFinishEnabled() 
	{
	   return false ;
	}
	
  public IWizardPanel getNextPanel() {
    return new IPWInstallModePanel(wizard,this);
  }
	
  public void updateList() {
    ArrayList list = new ArrayList();
    TableItem[] items = pluginList.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(items[i].getChecked())
        list.add(items[i].getData());          
    }
    ((InstallPluginWizard)wizard).setPluginList(list);
  }
}
