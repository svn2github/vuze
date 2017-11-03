/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.IntListParameter;
import org.gudy.azureus2.ui.swt.config.IntParameter;
import org.gudy.azureus2.ui.swt.config.Parameter;
import org.gudy.azureus2.ui.swt.config.ParameterChangeAdapter;
import org.gudy.azureus2.ui.swt.config.ParameterChangeListener;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.plugins.startstoprules.defaultplugin.DefaultRankCalculator;


/** Seeding Automation Specific options
 * @author TuxPaper
 * @created Jan 12, 2004
 * 
 * TODO: StartStopManager_fAddForSeedingULCopyCount
 */
public class ConfigSectionDownloading implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return "queue";
  }

  public String configSectionGetName() {
    return "queue.downloading";
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  
	public int maxUserMode() {
		return 0;
	}

  public Composite configSectionCreate(Composite parent) {
    // Seeding Automation Setup
    GridData gridData;
    GridLayout layout;
    Label label;

    Composite cDownloading = new Composite(parent, SWT.NULL);

    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    cDownloading.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    Utils.setLayoutData(cDownloading, gridData);

    	// wiki link
    
	final Label linkLabel = new Label(cDownloading, SWT.NULL);
	linkLabel.setText(MessageText.getString("ConfigView.label.please.visit.here"));
	linkLabel.setData("http://wiki.vuze.com/w/Downloading_Rules");
	linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	linkLabel.setForeground(Colors.blue);
	gridData = new GridData();
	gridData.horizontalSpan = 2;
	Utils.setLayoutData(linkLabel, gridData);
	linkLabel.addMouseListener(new MouseAdapter() {
		public void mouseDoubleClick(MouseEvent arg0) {
			Utils.launch((String) ((Label) arg0.widget).getData());
		}

		public void mouseDown(MouseEvent arg0) {
			Utils.launch((String) ((Label) arg0.widget).getData());
		}
	});
	ClipboardCopy.addCopyToClipMenu( linkLabel );
	
		// sort type
	
	label = new Label(cDownloading, SWT.NULL);
	Messages.setLanguageText(label, "label.prioritize.downloads.based.on");
	
	String orderLabels[] = 
		{	MessageText.getString("label.order"), 
			MessageText.getString("label.seed.count"),
			MessageText.getString("label.speed"),
		};
	
	int orderValues[] = 
		{ 	DefaultRankCalculator.DOWNLOAD_ORDER_INDEX, 
			DefaultRankCalculator.DOWNLOAD_ORDER_SEED_COUNT, 
			DefaultRankCalculator.DOWNLOAD_ORDER_SPEED 
		};
	
	final IntListParameter sort_type = 
		new IntListParameter(cDownloading, "StartStopManager_Downloading_iSortType", 
			orderLabels, orderValues);
    
    Group gSpeed = new Group(cDownloading, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    
    layout = new GridLayout();
    layout.numColumns = 2;
    //layout.marginHeight = 0;
    gSpeed.setLayout(layout);
    gridData = new GridData(GridData.FILL_HORIZONTAL );
    gridData.horizontalSpan = 2;
    Utils.setLayoutData(gSpeed, gridData);
    
    gSpeed.setText( MessageText.getString( "label.speed.options" ));
  	// info
    
    label = new Label(gSpeed, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 300;
    Utils.setLayoutData(label, gridData);
    Messages.setLanguageText(label, "ConfigView.label.downloading.info");
    
   
    	// test time
    
    label = new Label(gSpeed, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.downloading.testTime");
    gridData = new GridData();
    final IntParameter testTime = new IntParameter(gSpeed, "StartStopManager_Downloading_iTestTimeSecs");
    testTime.setLayoutData(gridData);
    testTime.setMinimumValue( 60 );
        
    	// re-test
    
    label = new Label(gSpeed, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.downloading.reTest");
    gridData = new GridData();
    final IntParameter reTest = new IntParameter(gSpeed, "StartStopManager_Downloading_iRetestTimeMins");
    reTest.setLayoutData(gridData);
    reTest.setMinimumValue( 0 );
        
    ParameterChangeListener listener = 
    	new ParameterChangeAdapter()
    	{
	    	public void
	    	parameterChanged(
	    		Parameter	p,
	    		boolean		caused_internally )
	    	{
    			boolean is_speed = ((Integer)sort_type.getValueObject()) == DefaultRankCalculator.DOWNLOAD_ORDER_SPEED;
	    			
    			testTime.setEnabled( is_speed );
    			reTest.setEnabled( is_speed );
	    	}
    	};
    	
    sort_type.addChangeListener( listener );
    
    listener.parameterChanged( null, false );
    
    return cDownloading;
  }

  private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
    for(int i = 0 ; i < controls.length ; i++) {
      if (controls[i] instanceof Composite)
        controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
      controls[i].setEnabled(bEnabled);
    }
  }
}

