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
package org.gudy.azureus2.ui.swt.pluginsuninstaller;

import java.util.ArrayList;
import org.eclipse.swt.SWT;
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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;


/**
 * @author Olivier Chalouhi
 *
 */
public class UIPWListPanel extends AbstractWizardPanel {

  Table pluginList;
  
  public 
  UIPWListPanel(
	Wizard 					wizard, 
	IWizardPanel 			previous ) 
  {
	super(wizard, previous);
  }


  public void 
  show() 
  {
    wizard.setTitle(MessageText.getString("uninstallPluginsWizard.list.title"));
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
	Messages.setLanguageText(lblStatus,"uninstallPluginsWizard.list.loaded");
	
	pluginList = new Table(panel,SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE); 
	pluginList.setHeaderVisible(true);
	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	data.heightHint = 200;
	pluginList.setLayoutData(data);
	
	
	TableColumn tcName = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcName,"installPluginsWizard.list.name");
	tcName.setWidth(200);
	
	TableColumn tcVersion = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcVersion,"installPluginsWizard.list.version");
	tcVersion.setWidth(150);

    PluginInterface plugins[] = new PluginInterface[0];
    try {
      plugins = wizard.getAzureusCore().getPluginManager().getPluginInterfaces();
      
    } catch(final Exception e) {
    	
    	Debug.printStackTrace(e);
    }
     
    for(int i = 0 ; i < plugins.length ; i++) {
      PluginInterface plugin = plugins[i];
      if(! plugin.isMandatory()) {
        TableItem item = new TableItem(pluginList,SWT.NULL);
        item.setData(plugin);
        item.setText(0,plugin.getPluginID() + "/" + plugin.getPluginName());
        String version = plugin.getPluginVersion();
        if(version == null) version = MessageText.getString("installPluginsWizard.list.nullversion");
        item.setText(1,version);
      }
    }
	
	pluginList.addListener(SWT.Selection,new Listener() {
	  public void handleEvent(Event e) {
	    updateList();	  
	  }
	});
  }
  
	public boolean 
	isNextEnabled() 
	{
	   return false;
	}
	
	public boolean 
	isFinishEnabled() 
	{
	   return( true );
	}
	
	public IWizardPanel getFinishPanel() {
	    return new UIPWFinishPanel(wizard,this);
	}
	
  public void updateList() {
    ArrayList list = new ArrayList();
    TableItem[] items = pluginList.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(items[i].getChecked())
        list.add(items[i].getData());          
    }
    ((UnInstallPluginWizard)wizard).setPluginList(list);
  }
}
