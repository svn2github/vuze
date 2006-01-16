/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;

public class 
TorrentOptionsView
	extends AbstractIView
{
	private static final String	TEXT_PREFIX	= "TorrentOptionsView.param.";
	
	private DownloadManager		manager;
	private parameterAdapter	param_adapter	= new parameterAdapter();
	
	private Composite 			panel;
	
	protected
	TorrentOptionsView(
		DownloadManager		_manager )
	{
		manager	= _manager;
	}
	
	public void 
	initialize(
		Composite composite) 
	{
		panel = new Composite(composite, SWT.NULL);
		
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 2;
		layout.numColumns = 2;
		panel.setLayout(layout);

		GenericIntParameter	max_peers = new GenericIntParameter( param_adapter, panel, "max.peers", false );
		GridData gd = new GridData();
		gd.widthHint = 40;
		max_peers.setLayoutData(gd);
		
		Label label = new Label(panel, SWT.NULL);
		Messages.setLanguageText(label, TEXT_PREFIX + "max.peers");
	}
	
	public Composite 
	getComposite() 
	{
		return panel;
	}
	
	public String 
	getFullTitle() 
	{
		return MessageText.getString("TorrentOptionsView.title.full");
	}

	public String 
	getData() 
	{
		return( "TorrentOptionsView.title.short" );
	}
	
	protected class
	parameterAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			Integer	i = manager.getDownloadState().getIntParameter( key );
			
			if ( i == null ){
				
				return( 0 );
				
			}else{
				
				return( i.intValue());
			}
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			Integer	i = manager.getDownloadState().getIntParameter( key );
			
			if ( i == null ){
				
				return( def );
				
			}else{
				
				return( i.intValue());
			}	
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			manager.getDownloadState().setIntParameter( key, new Integer( value ));
		}		
	}
}
