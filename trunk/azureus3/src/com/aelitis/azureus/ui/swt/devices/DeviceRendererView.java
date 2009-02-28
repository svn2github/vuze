/*
 * Created on Feb 2, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;


public class 
DeviceRendererView 
	extends DeviceManagerUI.deviceView
	implements TranscodeTargetListener
{
	private DeviceManagerUI		device_manager_ui;
	private DeviceMediaRenderer	device;
	
	private Composite		main;

	private Composite 		files_area;
	
	protected
	DeviceRendererView(
		String					_parent_key,
		DeviceMediaRenderer		_device )
	{
		super( _parent_key, _device );
		
		device = _device;
		
		device.addListener( this );
	}
	
	public void 
	initialize(
		Composite parent )
	{  				
		main = new Composite( parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginTop = 4;
		layout.marginBottom = 4;
		layout.marginHeight = 4;
		layout.marginWidth = 4;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);
		
		Label info_lab = new Label( main, SWT.NONE );
		
		info_lab.setText( "Transcode details for " + getTitle());
		
			// control
		
		Composite control = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginLeft = 0;
		control.setLayout(layout);

			// browse to local dir
		
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalSpan = 1;
		control.setLayoutData(grid_data);

			Label dir_lab = new Label( control, SWT.NONE );
			dir_lab.setText( "Local directory: " + device.getWorkingDirectory().getAbsolutePath());

		
			Button show_folder_button = new Button( control, SWT.PUSH );
				
		 	Messages.setLanguageText( show_folder_button, "MyTorrentsView.menu.explore");
		 	
		 	show_folder_button.addSelectionListener(
		 		new SelectionAdapter()
		 		{
		 			public void
		 			widgetSelected(
		 				SelectionEvent e )
		 			{
		 				
		 				ManagerUtils.open( device.getWorkingDirectory());
		 			}
		 		});
		 	
		 	new Label( control, SWT.NONE );
		 	
		 	if ( device.canFilterFilesView()){
		 		
				final Button show_xcode_button = new Button( control, SWT.CHECK );
				
			 	Messages.setLanguageText( show_xcode_button, "devices.xcode.only.show");
			 	
			 	show_xcode_button.setSelection( device.getFilterFilesView());
			 	
			 	show_xcode_button.addSelectionListener(
			 		new SelectionAdapter()
			 		{
			 			public void
			 			widgetSelected(
			 				SelectionEvent e )
			 			{		 				
			 				device.setFilterFilesView( show_xcode_button.getSelection());
			 			}
			 		});
		 	}
		 	
		 	// files
		
		Label files_lab = new Label( main, SWT.NONE );
			
		files_lab.setText( "Transcoded files" );
		
	    final ScrolledComposite files_scroll = new ScrolledComposite(main, SWT.V_SCROLL | SWT.H_SCROLL );
	    grid_data = new GridData(SWT.FILL, SWT.FILL, true, true);
	    files_scroll.setLayoutData(grid_data);
	    
		files_area = new Composite(files_scroll, SWT.NONE);

		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 1;
		files_area.setLayout(layout);
		
		files_scroll.setContent(files_area);
		files_scroll.setExpandVertical(true);
		files_scroll.setExpandHorizontal(true);

		files_scroll.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = files_scroll.getClientArea();
				files_scroll.setMinSize(files_area.computeSize(r.width, SWT.DEFAULT ));
			}
		});
	    
		grid_data = new GridData(GridData.FILL_BOTH );
		files_area.setLayoutData(grid_data);
		
		buildFiles();
	}
	
	protected void
	buildFiles()
	{
		for (Control c: files_area.getChildren()){
			
			c.dispose();
		}
		
		TranscodeFile[] files = device.getFiles();
		
		for ( TranscodeFile file: files ){
			
			Label file_lab = new Label( files_area, SWT.NONE );
			
			file_lab.setText( file.getFile().getName() + ", comp=" + file.isComplete());
			
			GridData grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalIndent = 16;
			file_lab.setLayoutData(grid_data);
		}
		
		files_area.layout();
	}
	
	public void 
	fileAdded(
		TranscodeFile file ) 
	{
		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						buildFiles();
					}
				});
	}
	
	public void 
	fileChanged(
		TranscodeFile file ) 
	{
		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						buildFiles();
					}
				});
	}
	
	public void 
	fileRemoved(
		TranscodeFile file ) 
	{
		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						buildFiles();
					}
				});
	}
	
	public Composite 
	getComposite()
	{
		return( main );
	}
	
	public void
	delete()
	{
		super.delete();
	}
}
