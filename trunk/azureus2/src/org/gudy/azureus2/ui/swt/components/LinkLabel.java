/*
 * Created on 11-Nov-2005
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;

public class 
LinkLabel 
{
	public
	LinkLabel(
		Composite	composite,
		String		resource,
		String		link )
	{
		this( composite, new GridData(), resource, link );
	}
	
	public
	LinkLabel(
		Composite	composite,
		GridData	gridData,
		String		resource,
		String		link )
	{
	    final Label linkLabel = new Label(composite, SWT.NULL);
	    Messages.setLanguageText(linkLabel,resource);
	    linkLabel.setData(link);
	    linkLabel.setCursor(Cursors.handCursor);
	    linkLabel.setForeground(Colors.blue);
	    linkLabel.setLayoutData( gridData );
	    linkLabel.addMouseListener(new MouseAdapter() {
	      public void mouseDoubleClick(MouseEvent arg0) {
	        Program.launch((String) ((Label) arg0.widget).getData());
	      }
	      public void mouseUp(MouseEvent arg0) {
	        Program.launch((String) ((Label) arg0.widget).getData());
	      }
	    });
	}
}
