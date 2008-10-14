/*
 * Created on Oct 13, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;

public class 
BrowserShell 
{
	public
	BrowserShell(
		String		title_resource,
		String		url,
		int			width,
		int			height )
	{
		Shell parent_shell = Utils.findAnyShell();
		
		MessageBoxShell boxShell = 
			new MessageBoxShell(
				parent_shell,
				MessageText.getString( title_resource ),
				"",
				new String[] {
					//MessageText.getString("Button.ok")
				},
				0 );

		boxShell.setUrl( url );

		boxShell.setSquish( true );
		
		boxShell.setSize( width, height );
		
		boxShell.open();
	}
}
