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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.config.generic.GenericBooleanParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;

public class 
TorrentOptionsView
	extends AbstractIView
{
	private static final String	TEXT_PREFIX	= "TorrentOptionsView.param.";
	
	private static final String	MAX_UPLOAD		= "max.upload";
	private static final String	MAX_DOWNLOAD	= "max.download";
	private static final String	MAX_UPLOADS		= "max.uploads";
	
	private DownloadManager			manager;
	
	private GenericParameterAdapter	ds_param_adapter	= new downloadStateParameterAdapter();
	private GenericParameterAdapter	adhoc_param_adapter	= new adhocParameterAdapter();
	
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
		layout.numColumns = 1;
		panel.setLayout(layout);

			// header 
		
		Composite cHeader = new Composite(panel, SWT.BORDER);
		GridLayout configLayout = new GridLayout();
		configLayout.marginHeight = 3;
		configLayout.marginWidth = 0;
		cHeader.setLayout(configLayout);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		cHeader.setLayoutData(gridData);
		
		Display d = panel.getDisplay();
		cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		
		Label lHeader = new Label(cHeader, SWT.NULL);
		lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		FontData[] fontData = lHeader.getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		int fontHeight = (int)(fontData[0].getHeight() * 1.2);
		fontData[0].setHeight(fontHeight);
		Font headerFont = new Font(d, fontData);
		lHeader.setFont(headerFont);
		lHeader.setText( " " + MessageText.getString( "authenticator.torrent" ) + " : " + manager.getDisplayName().replaceAll("&", "&&"));
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		lHeader.setLayoutData(gridData);
		
		Group gTransfer = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gTransfer, "ConfigView.section.transfer");
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gTransfer.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 3;
		gTransfer.setLayout(layout);

		//Disabled for release. Need to convert from user-specified units to
	    //KB/s before restoring the following line
	    //String k_unit = DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB).trim()
	    String k_unit = DisplayFormatters.getRateUnitBase10(DisplayFormatters.UNIT_KB).trim();

			// max upload speed
		
		GenericIntParameter	max_upload = new GenericIntParameter( adhoc_param_adapter, gTransfer, MAX_UPLOAD, false );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_upload.setLayoutData(gridData);
		
		Label label = new Label(gTransfer, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxuploadspeed.tooltip" ));

			// max download speed
		
		GenericIntParameter	max_download = new GenericIntParameter( adhoc_param_adapter, gTransfer, MAX_DOWNLOAD, false );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_download.setLayoutData(gridData);
		
		label = new Label(gTransfer, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxdownloadspeed.tooltip" ));
	     
			// max uploads
		
		GenericIntParameter	max_uploads = new GenericIntParameter( adhoc_param_adapter, gTransfer, MAX_UPLOADS, false );
		max_uploads.setMinimumValue(2);
		gridData = new GridData();
		gridData.widthHint = 40;
		max_uploads.setLayoutData(gridData);
		
		label = new Label(gTransfer, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
		Messages.setLanguageText(label, TEXT_PREFIX + "max.uploads" );
		
			//	max uploads when seeding enabled
		
		label = new Label(gTransfer, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalIndent = 20;
		GenericBooleanParameter	max_uploads_when_seeding_enabled = 
			new GenericBooleanParameter( 
					ds_param_adapter, 
					gTransfer, 
					DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED,
					false,
					TEXT_PREFIX + "max.uploads.when.seeding.enable");
		max_uploads_when_seeding_enabled.setLayoutData( gridData );
		

		GenericIntParameter	max_uploads_when_seeding = 
			new GenericIntParameter( ds_param_adapter, gTransfer, DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING, false );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_uploads_when_seeding.setMinimumValue(2);
		max_uploads_when_seeding.setLayoutData(gridData);
		
		max_uploads_when_seeding_enabled.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( max_uploads_when_seeding.getControl()));
				
			// max peers
		
		GenericIntParameter	max_peers = 
			new GenericIntParameter( ds_param_adapter, gTransfer, DownloadManagerState.PARAM_MAX_PEERS, false );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_peers.setLayoutData(gridData);
		
		label = new Label(gTransfer, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData( gridData );
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
	adhocParameterAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			return( getIntValue( key, 0 ));
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			if ( key == MAX_UPLOADS ){
				return( manager.getStats().getMaxUploads());
			}else if ( key == MAX_UPLOAD ){
				return( manager.getStats().getUploadRateLimitBytesPerSecond()/1024);
			}else if ( key == MAX_DOWNLOAD ){
				return( manager.getStats().getDownloadRateLimitBytesPerSecond()/1024);
			}else{
				return(0);
			}
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			if ( key == MAX_UPLOADS ){
				manager.getStats().setMaxUploads(value);
			}else if ( key == MAX_UPLOAD ){
				manager.getStats().setUploadRateLimitBytesPerSecond(value*1024);
			}else if ( key == MAX_DOWNLOAD ){
				manager.getStats().setDownloadRateLimitBytesPerSecond(value*1024);
			}
		}		
	}
	
	protected class
	downloadStateParameterAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			return( getIntValue( key, 0 ));
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			return( manager.getDownloadState().getIntParameter( key ));
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			manager.getDownloadState().setIntParameter( key, value );
		}	
		
		public boolean
		getBooleanValue(
			String	key )
		{
			return( getBooleanValue(key,false));
		}
		
		public boolean
		getBooleanValue(
			String		key,
			boolean		def )
		{
			return( manager.getDownloadState().getBooleanParameter( key ));
		}
		
		public void
		setBooleanValue(
			String		key,
			boolean		value )
		{
			manager.getDownloadState().setBooleanParameter( key, value );
		}
	}
}
