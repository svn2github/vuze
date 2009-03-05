/**
 * Created on Mar 2, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;

/**
 * @author TuxPaper
 * @created Mar 2, 2009
 *
 */
public class DeviceInfoArea
	extends SkinView
{
	private SideBarEntrySWT sidebarEntry;
	private DeviceMediaRenderer device;
	private Composite main;
	private Composite parent;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			sidebarEntry = sidebar.getCurrentEntry();
			device = (DeviceMediaRenderer) sidebarEntry.getDatasource();
		}
		
		parent = (Composite) skinObject.getControl();

		return super.skinObjectInitialShow(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		
		if (device == null) {
			build();
		} else {
			initView();
		}
		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		Utils.disposeComposite(main);
		return super.skinObjectHidden(skinObject, params);
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void initView() {
		main = new Composite( parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginTop = 4;
		layout.marginBottom = 4;
		layout.marginHeight = 4;
		layout.marginWidth = 4;
		main.setLayout(layout);
		GridData grid_data;
		main.setLayoutData(Utils.getFilledFormData());
		
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
		 	parent.getParent().layout();
	}

	protected void
	build()
	{
		main = new Group(parent, SWT.NONE);
		((Group)main).setText("Beta Debug");
		main.setLayoutData(Utils.getFilledFormData());
		main.setLayout(new FormLayout());
		
		FormData data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.right	= new FormAttachment(100,0);
		data.top	= new FormAttachment(main,0);

		Label label = new Label( main, SWT.NULL );
		
		label.setText( "Transcode Providers:" );
		
		label.setLayoutData( data );
		
		Button vuze_button = new Button( main, SWT.NULL );
		
		vuze_button.setText( "Install Vuze Transcoder" );
		
		PluginInstaller installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();
			
		StandardPlugin vuze_plugin = null;
		
		try{
			vuze_plugin = installer.getStandardPlugin( "vuzexcode" );

		}catch( Throwable e ){	
		}
		
		if ( vuze_plugin == null || vuze_plugin.isAlreadyInstalled()){
			
			vuze_button.setEnabled( false );
		}
		
		final StandardPlugin	f_vuze_plugin = vuze_plugin;
		
		vuze_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(
					Event arg0 ) 
				{
					try{
						f_vuze_plugin.install( true );

					}catch( Throwable e ){
						
					}
				}
			});
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.top	= new FormAttachment(label,4);

		vuze_button.setLayoutData( data );

		Control top = vuze_button;
		
		
		TranscodeProvider[] providers = DeviceManagerFactory.getSingleton().getTranscodeManager().getProviders();
		
		for ( TranscodeProvider provider: providers ){
			
			data = new FormData();
			data.left 	= new FormAttachment(0,10);
			data.right	= new FormAttachment(100,0);
			data.top	= new FormAttachment(top,4);

			Label prov_lab = new Label( main, SWT.NULL );
			
			prov_lab.setText( provider.getName());
			
			prov_lab.setLayoutData( data );
			
			top = prov_lab;
			
			TranscodeProfile[] profiles = provider.getProfiles();
			
			String line = null;
			for ( TranscodeProfile profile: profiles ){
				
				if (line == null) {
					line = profile.getName();
				} else {
					line += ", " + profile.getName();
				}
				
			}

			if (line != null) {
  			data = new FormData();
  			data.left 	= new FormAttachment(0,25);
  			data.right	= new FormAttachment(100,0);
  			data.top	= new FormAttachment(top,4);
  
  			Label prof_lab = new Label( main, SWT.WRAP );
  			
  			prof_lab.setText("Profiles: " + line);
  			
  			prof_lab.setLayoutData( data );
  			
  			top = prof_lab;
			}
		}
		
		
		Button itunes_button = new Button( main, SWT.NULL );
		
		itunes_button.setText( "Install iTunes Integration" );
		

		StandardPlugin itunes_plugin = null;
		
		try{
			itunes_plugin = installer.getStandardPlugin( "azitunes" );

		}catch( Throwable e ){	
		}
		
		if ( itunes_plugin == null || itunes_plugin.isAlreadyInstalled()){
			
			itunes_button.setEnabled( false );
		}
		
		final StandardPlugin	f_itunes_plugin = itunes_plugin;
		
		itunes_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(
					Event arg0 ) 
				{
					try{
						f_itunes_plugin.install( true );

					}catch( Throwable e ){
						
					}
				}
			});
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.top	= new FormAttachment(top,4);

		itunes_button.setLayoutData( data );
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.right	= new FormAttachment(100,0);
		data.top	= new FormAttachment(itunes_button,4);

		parent.getParent().layout();
	}
}
