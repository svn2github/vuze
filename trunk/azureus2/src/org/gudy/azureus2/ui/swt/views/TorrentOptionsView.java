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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateEvent;
import org.gudy.azureus2.core3.download.DownloadManagerStateListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.config.generic.GenericBooleanParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;

public class 
TorrentOptionsView
	extends AbstractIView
	implements DownloadManagerStateListener
{
	private static final String	TEXT_PREFIX	= "TorrentOptionsView.param.";
	
		// adhoc parameters need explicit code to reset default values below
	
	private static final String	MAX_UPLOAD		= "max.upload";
	private static final String	MAX_DOWNLOAD	= "max.download";
	
	private DownloadManager			manager;
	
	private GenericParameterAdapter	ds_param_adapter	= new downloadStateParameterAdapter();
	private GenericParameterAdapter	adhoc_param_adapter	= new adhocParameterAdapter();
	
	private Map adhoc_parameters	= new HashMap();
	private Map	ds_parameters 		= new HashMap();
	
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

		int userMode = COConfigurationManager.getIntParameter("User Mode");

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
		
		Group gTorrentOptions = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gTorrentOptions, "ConfigView.section.transfer");
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gTorrentOptions.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gTorrentOptions.setLayout(layout);

		//Disabled for release. Need to convert from user-specified units to
	    //KB/s before restoring the following line
	    //String k_unit = DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB).trim()
	    String k_unit = DisplayFormatters.getRateUnitBase10(DisplayFormatters.UNIT_KB).trim();

			// max upload speed
		
		Label label = new Label(gTorrentOptions, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxuploadspeed.tooltip" ));

		GenericIntParameter	max_upload = new GenericIntParameter( adhoc_param_adapter, gTorrentOptions, MAX_UPLOAD, false );
		adhoc_parameters.put( MAX_UPLOAD, max_upload );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_upload.setLayoutData(gridData);
		
		if ( userMode > 0) {

				// max upload when busy
			
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.uploads.when.busy");
			
			GenericIntParameter	max_upload_when_busy = 
				new GenericIntParameter( ds_param_adapter, gTorrentOptions, DownloadManagerState.PARAM_MAX_UPLOAD_WHEN_BUSY, false );
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOAD_WHEN_BUSY, max_upload_when_busy );
			gridData = new GridData();
			gridData.widthHint = 40;
			max_upload_when_busy.setLayoutData(gridData);
		}
		
			// max download speed
		
		label = new Label(gTorrentOptions, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxdownloadspeed.tooltip" ));
	     
		GenericIntParameter	max_download = new GenericIntParameter( adhoc_param_adapter, gTorrentOptions, MAX_DOWNLOAD, false );
		adhoc_parameters.put( MAX_DOWNLOAD, max_download );
		gridData = new GridData();
		gridData.widthHint = 40;
		max_download.setLayoutData(gridData);
		
			// max uploads
		
		if (userMode > 0) {
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.uploads" );
			
			GenericIntParameter	max_uploads = 
				new GenericIntParameter( ds_param_adapter, gTorrentOptions, DownloadManagerState.PARAM_MAX_UPLOADS, false );
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS, max_uploads );
			max_uploads.setMinimumValue(2);
			gridData = new GridData();
			gridData.widthHint = 40;
			max_uploads.setLayoutData(gridData);
			
				//	max uploads when seeding enabled
			
			final Composite cMaxUploadsOptionsArea = new Composite(gTorrentOptions, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cMaxUploadsOptionsArea.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			cMaxUploadsOptionsArea.setLayoutData(gridData);
			
			label = new Label(cMaxUploadsOptionsArea, SWT.NULL);
			Image img = ImageRepository.getImage("subitem");
			img.setBackground(label.getBackground());
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
			label.setImage(img);
	
			gridData = new GridData();
			GenericBooleanParameter	max_uploads_when_seeding_enabled = 
				new GenericBooleanParameter( 
						ds_param_adapter, 
						cMaxUploadsOptionsArea, 
						DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED,
						false,
						TEXT_PREFIX + "max.uploads.when.seeding.enable");
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED, max_uploads_when_seeding_enabled );
			max_uploads_when_seeding_enabled.setLayoutData( gridData );
			
	
			GenericIntParameter	max_uploads_when_seeding = 
				new GenericIntParameter(
						ds_param_adapter, cMaxUploadsOptionsArea, 
						DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING, false );
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING, max_uploads_when_seeding );
			gridData = new GridData();
			gridData.widthHint = 40;
			max_uploads_when_seeding.setMinimumValue(2);
			max_uploads_when_seeding.setLayoutData(gridData);
			
			max_uploads_when_seeding_enabled.setAdditionalActionPerformer(
					new ChangeSelectionActionPerformer( max_uploads_when_seeding.getControl()));
					
				// max peers
			
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.peers");
			
			GenericIntParameter	max_peers = 
				new GenericIntParameter( ds_param_adapter, gTorrentOptions, DownloadManagerState.PARAM_MAX_PEERS, false );
			ds_parameters.put( DownloadManagerState.PARAM_MAX_PEERS, max_peers );
			gridData = new GridData();
			gridData.widthHint = 40;
			max_peers.setLayoutData(gridData);
		}
		
	    Label cert_label = new Label(gTorrentOptions, SWT.NULL );
	    Messages.setLanguageText(cert_label, TEXT_PREFIX + "reset.to.default");

	    Button cert_button = new Button(gTorrentOptions, SWT.PUSH);

	    Messages.setLanguageText(cert_button, TEXT_PREFIX + "reset.button" );

	    cert_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	setDefaults();
			        }
			    });
		
		manager.getDownloadState().addListener( this );
	}
	
	protected void
	setDefaults()
	{
		Iterator	it = ds_parameters.keySet().iterator();
		
		while( it.hasNext()){
						
			String	key 	= (String)it.next();

			manager.getDownloadState().setParameterDefault( key );
		}
		
		it = adhoc_parameters.values().iterator();
		
		while ( it.hasNext()){
			
			Object	param 	= it.next();
			
			if ( param instanceof GenericIntParameter ){
				
				GenericIntParameter	int_param = (GenericIntParameter)param;
				
				int_param.setValue( 0, true );
				
			}else{
				Debug.out( "Unknown parameter type: " + param.getClass());
			}
		}
	}
	
	public void
	stateChanged(
		DownloadManagerState			state,
		DownloadManagerStateEvent		event )
	{
		if ( event.getType() == DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN ){
			
			String	attribute_name = (String)event.getData();
			
			if ( attribute_name.equals( DownloadManagerState.AT_PARAMETERS )){
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							Iterator	it = ds_parameters.entrySet().iterator();
							
							while( it.hasNext()){
								
								Map.Entry	entry = (Map.Entry)it.next();
								
								String	key 	= (String)entry.getKey();
								Object	param 	= entry.getValue();
								
								if ( param instanceof GenericIntParameter ){
								
									GenericIntParameter	int_param = (GenericIntParameter)param;
									
									int	value = manager.getDownloadState().getIntParameter( key );
									
									int_param.setValue( value );
									
								}else if ( param instanceof GenericBooleanParameter ){
									
									GenericBooleanParameter	bool_param = (GenericBooleanParameter)param;
									
									boolean	value = manager.getDownloadState().getBooleanParameter( key );
									
									bool_param.setValue( value );
									
								}else{
									
									Debug.out( "Unknown parameter type: " + param.getClass());
								}
							}
						}
					},
					true );
			}
		}
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
	
	public void 
	delete()
	{
		super.delete();
		
		manager.getDownloadState().removeListener( this );
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
			if ( key == MAX_UPLOAD ){
				return( manager.getStats().getUploadRateLimitBytesPerSecond()/1024);
			}else if ( key == MAX_DOWNLOAD ){
				return( manager.getStats().getDownloadRateLimitBytesPerSecond()/1024);
			}else{
				Debug.out( "Unknown key '" + key + "'" );
				return(0);
			}
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			if ( value != getIntValue( key )){
				
				if ( key == MAX_UPLOAD ){
					manager.getStats().setUploadRateLimitBytesPerSecond(value*1024);
				}else if ( key == MAX_DOWNLOAD ){
					manager.getStats().setDownloadRateLimitBytesPerSecond(value*1024);
				}else{
					Debug.out( "Unknown key '" + key + "'" );
				}
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
			if ( value != getIntValue(key)){
				
				manager.getDownloadState().setIntParameter( key, value );
			}
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
			if ( value != getBooleanValue(key)){
				
				manager.getDownloadState().setBooleanParameter( key, value );
			}
		}
	}
}
