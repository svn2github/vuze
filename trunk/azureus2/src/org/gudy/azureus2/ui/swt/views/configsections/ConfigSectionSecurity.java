/*
 * Created on 12-Jun-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.auth.CertificateCreatorWindow;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;

/**
 * @author parg
 *
 */
public class 
ConfigSectionSecurity 
	implements ConfigSectionSWT 
{
	public String 
	configSectionGetParentSection() 
	{
	    return ConfigSection.SECTION_ROOT;
	}

	public String 
	configSectionGetName() 
	{
		return( "security" );
	}

	public void 
	configSectionSave() 
	{
	}

	public void 
	configSectionDelete() 
	{
	}
	  
	public Composite 
	configSectionCreate(
		final Composite parent) 
	{
	    GridData gridData;

	    Composite gSecurity = new Composite(parent, SWT.NULL);
	    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	    gSecurity.setLayoutData(gridData);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 2;
	    gSecurity.setLayout(layout);

	    // row
	    
	    Label cert_label = new Label(gSecurity, SWT.NULL );
	    Messages.setLanguageText(cert_label, "ConfigView.section.tracker.createcert");

	    Button cert_button = new Button(gSecurity, SWT.PUSH);

	    Messages.setLanguageText(cert_button, "ConfigView.section.tracker.createbutton");

	    cert_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	new CertificateCreatorWindow();
			        }
			    });
	    

	    return gSecurity;
	  }
	}
