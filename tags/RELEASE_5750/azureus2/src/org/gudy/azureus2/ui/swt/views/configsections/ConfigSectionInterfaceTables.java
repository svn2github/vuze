/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionInterfaceTables
	implements UISWTConfigSection
{
	private final static String MSG_PREFIX = "ConfigView.section.style.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public String configSectionGetName() {
		return "tables";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public int maxUserMode() {
		return 2;
	}

	public Composite configSectionCreate(final Composite parent) {
		int userMode = COConfigurationManager.getIntParameter("User Mode");
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals(
				"az3");

		// "Display" Sub-Section:
		// ----------------------
		// Any Look & Feel settings that don't really change the way the user 
		// normally interacts
		Label label;
		GridLayout layout;
		GridData gridData;
		Composite cSection = new Composite(parent, SWT.NULL);
		Utils.setLayoutData(cSection, new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 1;
		cSection.setLayout(layout);

		{
			Group cGeneral = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cGeneral, "ConfigView.section.global" );
			layout = new GridLayout();
			layout.numColumns = 2;
			cGeneral.setLayout(layout);
			Utils.setLayoutData(cGeneral, new GridData( GridData.FILL_HORIZONTAL ));
		
			label = new Label(cGeneral, SWT.NULL);
			Messages.setLanguageText(label, MSG_PREFIX + "defaultSortOrder");
			int[] sortOrderValues = {
				0,
				1,
				2
			};
			String[] sortOrderLabels = {
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.asc"),
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.desc"),
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.flip")
			};
			new IntListParameter(cGeneral, "config.style.table.defaultSortOrder",
					sortOrderLabels, sortOrderValues);
	
			if (userMode > 0) {
				label = new Label(cGeneral, SWT.NULL);
				Messages.setLanguageText(label, MSG_PREFIX + "guiUpdate");
				int[] values = {
					10,
					25,
					50,
					100,
					250,
					500,
					1000,
					2000,
					5000,
					10000,
					15000
				};
				String[] labels = {
					"10 ms",
					"25 ms",
					"50 ms",
					"100 ms",
					"250 ms",
					"500 ms",
					"1 s",
					"2 s",
					"5 s",
					"10 s",
					"15 s"
				};
				new IntListParameter(cGeneral, "GUI Refresh", 1000, labels, values);
	
				label = new Label(cGeneral, SWT.NULL);
				Messages.setLanguageText(label, MSG_PREFIX + "graphicsUpdate");
				gridData = new GridData();
				IntParameter graphicUpdate = new IntParameter(cGeneral, "Graphics Update",
						1, Integer.MAX_VALUE );
				graphicUpdate.setLayoutData(gridData);
	
				label = new Label(cGeneral, SWT.NULL);
				Messages.setLanguageText(label, MSG_PREFIX + "reOrderDelay");
				gridData = new GridData();
				IntParameter reorderDelay = new IntParameter(cGeneral, "ReOrder Delay");
				reorderDelay.setLayoutData(gridData);
	
				new BooleanParameter(cGeneral, "NameColumn.showProgramIcon", MSG_PREFIX
						+ "showProgramIcon").setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
	
				////
	
				new BooleanParameter(cGeneral, "Table.extendedErase", MSG_PREFIX
						+ "extendedErase").setLayoutData(new GridData(SWT.FILL, SWT.LEFT,
						true, false, 2, 1));
	
				////
				
				boolean hhEnabled = COConfigurationManager.getIntParameter("Table.headerHeight") > 0;
	
				Button chkHeaderHeight = new Button(cGeneral, SWT.CHECK);
				Messages.setLanguageText(chkHeaderHeight, MSG_PREFIX + "enableHeaderHeight");
				chkHeaderHeight.setSelection(hhEnabled);
				
				final IntParameter paramHH = new IntParameter(cGeneral, "Table.headerHeight", 0, 100);
				paramHH.setEnabled(hhEnabled);
				
				chkHeaderHeight.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						if (((Button) e.widget).getSelection()) {
							COConfigurationManager.setParameter("Table.headerHeight", 16);
							paramHH.setEnabled(true);
						} else {
							COConfigurationManager.setParameter("Table.headerHeight", 0);
							paramHH.setEnabled(false);
						}
					}
					
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
				
				/////
	
				boolean cdEnabled = COConfigurationManager.getStringParameter("Table.column.dateformat", "").length() > 0;
	
				Button chkCustomDate = new Button(cGeneral, SWT.CHECK);
				Messages.setLanguageText(chkCustomDate, MSG_PREFIX + "customDateFormat");
				chkCustomDate.setSelection(cdEnabled);
				
				final StringParameter paramCustomDate = new StringParameter(cGeneral, "Table.column.dateformat", "");
				paramCustomDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				paramCustomDate.setEnabled(cdEnabled);
				paramCustomDate.addChangeListener(new ParameterChangeAdapter() {
					
					public void parameterChanged(Parameter p, boolean caused_internally) {
						String s = (String) p.getValueObject();
						boolean ok = false;
						try {
							SimpleDateFormat temp = new SimpleDateFormat(s);
							temp.format(new Date());
							ok = true;
						} catch (Exception e) {
							// probably illegalargumentexception
						}
						p.getControl().setBackground(ok ? null : Colors.colorErrorBG);
					}
					
				});
				
				chkCustomDate.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						if (((Button) e.widget).getSelection()) {
							COConfigurationManager.setParameter("Table.column.dateformat", "yyyy/MM/dd");
							paramCustomDate.setEnabled(true);
						} else {
							COConfigurationManager.setParameter("Table.column.dateformat", "");
							paramCustomDate.setEnabled(false);
						}
					}
					
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}
		}
		
		{
			Group cLibrary = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cLibrary, MSG_PREFIX + "library");
			layout = new GridLayout();
			layout.numColumns = 2;
			cLibrary.setLayout(layout);
			Utils.setLayoutData(cLibrary, new GridData( GridData.FILL_HORIZONTAL ));
						
				// User tree
			
			new BooleanParameter(cLibrary, "Table.useTree", MSG_PREFIX
					+ "useTree").setLayoutData(new GridData(SWT.FILL,
							SWT.LEFT, true, false, 2, 1));

			if (userMode > 1) {
				new BooleanParameter(cLibrary, "DND Always In Incomplete", MSG_PREFIX
						+ "DNDalwaysInIncomplete").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}

			if (isAZ3) {
				
				new BooleanParameter(cLibrary, "Library.EnableSimpleView", MSG_PREFIX
						+ "EnableSimpleView").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));

				
				new BooleanParameter(cLibrary, "Library.CatInSideBar", MSG_PREFIX
						+ "CatInSidebar").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}
			
			new BooleanParameter(cLibrary, "Library.ShowCatButtons", MSG_PREFIX
					+ "ShowCatButtons").setLayoutData(new GridData(SWT.FILL,
							SWT.LEFT, true, false, 2, 1));

			if (isAZ3) {

				new BooleanParameter(cLibrary, "Library.TagInSideBar", MSG_PREFIX
						+ "TagInSidebar").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}
			
			BooleanParameter show_tag = new BooleanParameter(cLibrary, "Library.ShowTagButtons", MSG_PREFIX
					+ "ShowTagButtons");
			
			show_tag.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));

			BooleanParameter show_tag_comp_only =new BooleanParameter(cLibrary, "Library.ShowTagButtons.CompOnly", MSG_PREFIX
					+ "ShowTagButtons.CompOnly");
			
			gridData = new GridData(SWT.FILL,SWT.LEFT, true, false, 2, 1);
			gridData.horizontalIndent = 25;
			show_tag_comp_only.setLayoutData( gridData );
			
			show_tag.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( show_tag_comp_only ));
			
			if (isAZ3) {

				new BooleanParameter(cLibrary, "Library.ShowTabsInTorrentView", MSG_PREFIX
						+ "ShowTabsInTorrentView").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}
			
			new BooleanParameter(cLibrary, "Library.showFancyMenu", true, MSG_PREFIX
					+ "ShowFancyMenu").setLayoutData(new GridData(SWT.FILL, SWT.LEFT,
					true, false, 2, 1));


		
			// double-click

			label = new Label(cLibrary, SWT.NULL);
			Messages.setLanguageText(label, "ConfigView.label.dm.dblclick");

			String[] dblclickOptions = {
				"ConfigView.option.dm.dblclick.play",
				"ConfigView.option.dm.dblclick.details",
				"ConfigView.option.dm.dblclick.show",
				"ConfigView.option.dm.dblclick.launch",
				"ConfigView.option.dm.dblclick.launch.qv",
				"ConfigView.option.dm.dblclick.open.browser",
			};

			String dblclickLabels[] = new String[dblclickOptions.length];
			String dblclickValues[] = new String[dblclickOptions.length];

			for (int i = 0; i < dblclickOptions.length; i++) {

				dblclickLabels[i] = MessageText.getString(dblclickOptions[i]);
				dblclickValues[i] = "" + i;
			}
			new StringListParameter(cLibrary, "list.dm.dblclick", dblclickLabels, dblclickValues);
			
				// always open websites in browser
			
			Composite cLaunchWeb = new Composite(cLibrary, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 4;
			cLaunchWeb.setLayout(layout);
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			gridData.horizontalSpan = 2;
			gridData.horizontalIndent = 25;
			Utils.setLayoutData(cLaunchWeb, gridData);
			
			BooleanParameter web_in_browser =
					new BooleanParameter(cLaunchWeb, "Library.LaunchWebsiteInBrowser", "library.launch.web.in.browser");

			BooleanParameter web_in_browser_anon =
					new BooleanParameter(cLaunchWeb, "Library.LaunchWebsiteInBrowserAnon", "library.launch.web.in.browser.anon");

			web_in_browser.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( web_in_browser_anon ));
			
				// Launch helpers
				
			Group cLaunch = new Group(cLibrary, SWT.NULL);
			Messages.setLanguageText(cLaunch, MSG_PREFIX + "launch");
			layout = new GridLayout();
			layout.numColumns = 5;
			cLaunch.setLayout(layout);
			gridData = new GridData( GridData.FILL_HORIZONTAL );
			gridData.horizontalSpan = 2;
			Utils.setLayoutData(cLaunch, gridData);
	
		    Label	info_label = new Label( cLaunch, SWT.WRAP );
		    Messages.setLanguageText( info_label, "ConfigView.label.lh.info" );
		    gridData = Utils.getWrappableLabelGridData(5, GridData.HORIZONTAL_ALIGN_FILL );
		    Utils.setLayoutData(info_label,  gridData );
		    
			for ( int i=0;i<4;i++){
				
				label = new Label(cLaunch, SWT.NULL);
				Messages.setLanguageText(label, "ConfigView.label.lh.ext");
	
				StringParameter exts = new StringParameter(cLaunch, "Table.lh" + i + ".exts", "");
				gridData = new GridData();
				gridData.widthHint = 200;
				exts.setLayoutData( gridData );
				
				label = new Label(cLaunch, SWT.NULL);
				Messages.setLanguageText(label, "ConfigView.label.lh.prog");
	
				final FileParameter prog = new FileParameter(cLaunch, "Table.lh" + i + ".prog", "", new String[0]);
	
				gridData = new GridData();
				gridData.widthHint = 400;
				prog.getControls()[0].setLayoutData( gridData );
				
				if ( Constants.isOSX ){
					COConfigurationManager.addParameterListener(
							"Table.lh" + i + ".prog",
							new ParameterListener()
							{		
								private boolean changing 		= false;
								private String 	last_changed	= "";
								
								public void 
								parameterChanged(
									String parameter_name)
								{
									if ( prog.isDisposed()){
										
										COConfigurationManager.removeParameterListener(	parameter_name, this );
										
									}else if ( changing ){
										
										return;
										
									}else{
										
										final String value = COConfigurationManager.getStringParameter( parameter_name );
										
										if ( value.equals( last_changed )){
											
											return;
										}
										
										if ( value.endsWith( ".app" )){
											
											Utils.execSWTThreadLater( 
												1,
												new Runnable()
												{
													public void 
													run()
													{
														last_changed = value;
														
														try{
															changing = true;
															
															File file = new File( value );
																
															String app_name = file.getName();
				
															int pos = app_name.lastIndexOf( "." );
															
															app_name = app_name.substring( 0,pos );
															
															String new_value = value + "/Contents/MacOS/" + app_name;
															
															if ( new File( new_value ).exists()){
															
																prog.setValue( new_value );
															}
														}finally{
															
															changing = false;
														}
													}
												});
										}
									}
								}
							});
				}
			}
		}
		
		{
			Group cSeachSubs = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cSeachSubs, MSG_PREFIX + "searchsubs");
			layout = new GridLayout();
			layout.numColumns = 2;
			cSeachSubs.setLayout(layout);
			Utils.setLayoutData(cSeachSubs, new GridData( GridData.FILL_HORIZONTAL ));
			
			
			new BooleanParameter(
				cSeachSubs, 
				"Search View Is Web View", 
				MSG_PREFIX + "search.is.web.view").setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));

			new BooleanParameter(
					cSeachSubs, 
					"Search View Switch Hidden", 
					MSG_PREFIX + "search.hide.view.switch").setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));

			label = new Label(cSeachSubs, SWT.NULL);
			Messages.setLanguageText(label, MSG_PREFIX + "searchsubs.row.height");
			gridData = new GridData();
			IntParameter graphicUpdate = new IntParameter(cSeachSubs, "Search Subs Row Height",
					16, 64 );
			graphicUpdate.setLayoutData(gridData);

		}
		
		return cSection;
	}
}
