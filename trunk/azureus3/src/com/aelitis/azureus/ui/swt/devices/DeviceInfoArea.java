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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

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
		
		initView();
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
}
