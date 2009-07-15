/**
 * Created on Feb 24, 2009
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

package com.aelitis.azureus.ui.swt.content;


import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;

import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.content.columns.ColumnRC_Title;
import com.aelitis.azureus.ui.swt.devices.columns.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

/**
 * @author TuxPaper
 * @created Feb 24, 2009
 *
 */
public class SBC_RCMView
	extends SkinView
	implements UIUpdatable
{
	public static final String TABLE_RCM = "RCM";

	private static boolean columnsAdded = false;



	
	private TableViewSWTImpl<RelatedContent> tv_related_content;

	private SideBarEntrySWT 	sidebar_entry;
	private Composite			table_parent;
	

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object 
	skinObjectInitialShow(
		SWTSkinObject skinObject, Object params) 
	{
		super.skinObjectInitialShow(skinObject, params);

		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initColumns(core);
				}
			});


		SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
		
		if ( sidebar != null ){
			
			sidebar_entry = sidebar.getCurrentEntry();
			
			//sidebarEntry.setIconBarEnabler(this);
			// device = (Device) sidebarEntry.getDatasource();
		}




		return null;
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void initColumns(AzureusCore core) {
		if (columnsAdded) {
			return;
		}
		columnsAdded = true;
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		TableManager tableManager = uiManager.getTableManager();
		tableManager.registerColumn(RelatedContent.class, ColumnRC_Title.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnRC_Title(column);
					}
				});
	}

	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		SWTSkinObject so_list = getSkinObject("rcm-list");
		
		if ( so_list != null ){
			
			initTable((Composite) so_list.getControl());
		}
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return super.skinObjectHidden(skinObject, params);
	}


	private void 
	initTable(
		Composite control ) 
	{

		tv_related_content = 
			new TableViewSWTImpl<RelatedContent>(
					RelatedContent.class, 
					TABLE_RCM,
					TABLE_RCM, 
					new TableColumnCore[0], 
					ColumnRC_Title.COLUMN_ID, 
					SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		
		tv_related_content.setRowDefaultHeight(50);
		tv_related_content.setHeaderVisible(true);
		
		// tvFiles.setParentDataSource(device);

		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_related_content.addSelectionListener(new TableSelectionListener() {

			public void selected(TableRowCore[] row) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void mouseExit(TableRowCore row) {
			}

			public void mouseEnter(TableRowCore row) {
			}

			public void focusChanged(TableRowCore focus) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void deselected(TableRowCore[] rows) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);

		tv_related_content.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				public void 
				tableViewInitialized() 
				{
					Object data_source = sidebar_entry.getDatasource();
					
					if ( data_source instanceof RelatedContentEnumerator ){
						
						final TableViewSWTImpl<RelatedContent> f_table = tv_related_content;
						
						((RelatedContentEnumerator)data_source).enumerate(
							new RelatedContentManagerListener()
							{
								public void
								lookupStarted(
									Download		for_download )
								{
									
								}
								
								public void
								foundContent(
									Download				for_download,
									final RelatedContent	content )
								{
									Utils.execSWTThread(
										new Runnable()
										{
											public void
											run()
											{
												if ( tv_related_content == f_table && !tv_related_content.isDisposed()){
												
													f_table.addDataSource( content );
												}
											}
										});
								}
								
								public void
								lookupCompleted(
									Download		for_download )
								{
									
								}
							});
					}
				/*
				if (transTarget == null) {
					// just add all jobs' files
					TranscodeJob[] jobs = transcode_queue.getJobs();
					for (TranscodeJob job : jobs) {
						TranscodeFile file = job.getTranscodeFile();
						if (file != null) {
							tvFiles.addDataSource(file);
						}
					}
				} else {
					tvFiles.addDataSources(transTarget.getFiles());
				}
				*/
				}

				public void 
				tableViewDestroyed() 
				{
				}
			});

		tv_related_content.addMenuFillListener(new TableViewSWTMenuFillListener() {
			public void fillMenu(Menu menu) {
				
			}

			public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
			}
		});

		tv_related_content.initialize( table_parent );

		control.layout(true);
	}



	



	public String getUpdateUIName() {
		return "RCMView";
	}

	public void updateUI() {
		if ( tv_related_content != null) {
			tv_related_content.refreshTable(false);
		}
	}

}
