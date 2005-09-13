/*
 * Created on 13-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.ui.swt.Messages;

public class 
Legend 
{
	  /** Creates the legend Composite
	   * 
	   * @return The created Legend Composite
	   */
	  public static Composite 
	  createLegendComposite(
		Composite	panel,
		Color[] 	colors,
		String[]	keys) 
	  {
	  	if(colors.length != keys.length) return null;
	  	
	  	Composite legend = new Composite(panel,SWT.NULL);
	  	legend.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	  	
	  	
	    GridLayout layout = new GridLayout();
	    int numColumns = colors.length * 2;
	    if(numColumns > 10) numColumns = 10;
	    layout.numColumns = numColumns;
	    legend.setLayout(layout);
	    GridData data;
	    
	    for(int i = 0 ; i < colors.length ; i++) {
	    	Label lblColor = new Label(legend,SWT.BORDER);
	    	lblColor.setBackground(colors[i]);
	    	data = new GridData();
	    	data.widthHint = 20;
	    	data.heightHint = 10;
	    	lblColor.setLayoutData(data);
	    	
	    	Label lblDesc = new Label(legend,SWT.NULL);
	    	Messages.setLanguageText(lblDesc,keys[i]);
	    	data = new GridData();
	    	data.widthHint = 150;
	    	lblDesc.setLayoutData(data);
	    }
	    
	    return legend;
	  	
	  }
}
