/*
 * Created on 27-Apr-2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.pluginsimpl.local.ui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeEvent;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;


/**
 * 
 */
public class BasicPluginViewImpl extends PluginView implements UIPropertyChangeListener {
  
  BasicPluginViewModel model;
  String pluginName;
  
  //GUI elements
  Display display;
  Composite panel;
  ProgressBar progress;
  BufferedLabel status;
  BufferedLabel task;
  StyledText log;
  
  public BasicPluginViewImpl(BasicPluginViewModel model) {
    this.model = model;
    this.pluginName = model.getName();
  }
  
  public String getPluginViewName() {
    return pluginName;
  }

  public Composite getComposite() {
    return panel;
  }

  public void initialize(Composite composite) {
    this.display = composite.getDisplay();
    panel = new Composite(composite,SWT.NULL);
    GridLayout panelLayout = new GridLayout();
    GridData gridData;
    panelLayout.numColumns = 2;
    panel.setLayout(panelLayout);
    
    /*
     * Status       : [Status Text]
     * Current Task : [Task Text]
     * Progress     : [||||||||||----------]
     * Log :
     * [
     * 
     * 
     * ]
     */
    
    if(model.getStatus().getVisible()) {
      Label statusTitle = new Label(panel,SWT.NULL);
      Messages.setLanguageText(statusTitle,"plugins.basicview.status");
    
      status = new BufferedLabel(panel,SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      status.setLayoutData(gridData);
    }
    
    if(model.getActivity().getVisible()) {
      Label activityTitle = new Label(panel,SWT.NULL);
      Messages.setLanguageText(activityTitle,"plugins.basicview.activity");
    
      task = new BufferedLabel(panel,SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      task.setLayoutData(gridData);
    }
    
    if(model.getProgress().getVisible()) {
      Label progressTitle = new Label(panel,SWT.NULL);
      Messages.setLanguageText(progressTitle,"plugins.basicview.progress");
    
      progress = new ProgressBar(panel,SWT.NULL);
      progress.setMaximum(100);
      progress.setMinimum(0);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      progress.setLayoutData(gridData);
    }
    
    if(model.getLogArea().getVisible()) {
      Label logTitle = new Label(panel,SWT.NULL);
      Messages.setLanguageText(logTitle,"plugins.basicview.log");
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      logTitle.setLayoutData(gridData);
      
      log = new StyledText(panel,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 2;
      log.setLayoutData(gridData);
      log.setText( model.getLogArea().getText());
      model.getLogArea().addPropertyChangeListener(this);
    }
  }
  
  public void refresh() {
    if(status != null) {
      status.setText(model.getStatus().getText());
    }
    if(task != null) {
      task.setText(model.getActivity().getText());
    }
    if(progress != null) {
      progress.setSelection(model.getProgress().getPercentageComplete());
    }
  }
  
  public void propertyChanged(UIPropertyChangeEvent ev) {
    if(ev.getSource() != model.getLogArea())
      return;
    final String old_value = (String)ev.getOldPropertyValue();
    final String new_value = (String) ev.getNewPropertyValue();
    if(display == null || display.isDisposed())
      return;
    if(log == null)
      return;
    display.asyncExec(new Runnable() {
      public void run() {
        if(log.isDisposed())
          return;
        if ( new_value.startsWith( old_value )){
        	log.append( new_value.substring(old_value.length()));
        }else{
        	log.setText(new_value);
        }
      }
    });
  }
  
  public String getFullTitle() {
    return pluginName;
  }
}
