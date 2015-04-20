/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.IntParameter;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.configsections.ConfigSectionStartShutdown;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author TuxPaper
 * @created Apr 7, 2007
 *
 */
public class ViewQuickConfig
	implements UISWTViewCoreEventListener
{
	private UISWTView swtView;
	
	Composite	composite;
	
	public ViewQuickConfig() {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
			}
		});
	}


	private void 
	initialize(
		Composite parent) 
	{
		parent.setLayout( new GridLayout());
		
		composite = new Composite( parent, SWT.BORDER );
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		
		composite.setLayoutData(gridData);
		
		GridLayout layout = new GridLayout(4, false);
		
		composite.setLayout(layout);
		
			// done downloading - 2
		
		ConfigSectionStartShutdown.addDoneDownloadingOption( composite, false );
		
			// max simul down - 2 
		
		Label label = new Label(composite, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalIndent = 8;
		label.setLayoutData( gridData );
		Messages.setLanguageText(label, "ConfigView.label.maxdownloads.short");
		
		IntParameter maxDLs = new IntParameter( composite, "max downloads" );
				
		Utils.execSWTThreadLater(
			100,
			new Runnable() {
				
				public void run() {
					composite.traverse( SWT.TRAVERSE_TAB_NEXT);
				}
			});
	}

	private void delete() {
		Utils.disposeComposite(composite);
	}

	private String getFullTitle() {
		return( MessageText.getString( "label.quick.config" ));
	}
	
	

	private Composite getComposite() {
		return composite;
	}

	private void refresh() {

	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = event.getView();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
      case UISWTViewEvent.TYPE_FOCUSGAINED:{
    	  composite.traverse( SWT.TRAVERSE_TAB_NEXT);
    	  break;
      }
    }

    return true;
  }
}
