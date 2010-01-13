/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigSectionStartShutdown implements UISWTConfigSection {

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return "startstop";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {

	}
	
	public int maxUserMode() {
		return 1;
	}


	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cDisplay = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cDisplay.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cDisplay.setLayout(layout);

		final PlatformManager platform = PlatformManagerFactory.getPlatformManager();
		
		int userMode = COConfigurationManager.getIntParameter("User Mode");
		
			// ***** start group
		
		boolean can_ral = platform.hasCapability(PlatformManagerCapabilities.RunAtLogin );
				
		if ( can_ral ){
			
			Group gStartStop = new Group(cDisplay, SWT.NULL);
			Messages.setLanguageText(gStartStop, LBLKEY_PREFIX + "start");
			layout = new GridLayout(2, false);
			gStartStop.setLayout(layout);
			gStartStop.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
	
			if ( can_ral ){
	
				gridData = new GridData();
				gridData.horizontalSpan = 2;
				BooleanParameter start_on_login = new BooleanParameter(gStartStop, "Start On Login", LBLKEY_PREFIX + "start.onlogin");
				
				try{
					start_on_login.setSelected( platform.getRunAtLogin());
					
					start_on_login.addChangeListener(
						new ParameterChangeAdapter()
						{
							public void 
							booleanParameterChanging(
								Parameter p,
								boolean toValue) 
							{
								try{
									platform.setRunAtLogin( toValue );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						});
					
				}catch( Throwable e ){
					
					start_on_login.setEnabled( false );
					
					Debug.out( e );
				}
				
				start_on_login.setLayoutData(gridData);
			}
			
		}
				
		if ( userMode > 0 ){
				
			Group gPR = new Group(cDisplay, SWT.NULL);
			Messages.setLanguageText(gPR, LBLKEY_PREFIX + "pauseresume");
			layout = new GridLayout(2, false);
			gPR.setLayout(layout);
			gPR.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));

			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter pauseOnExit = new BooleanParameter(gPR,
					"Pause Downloads On Exit", "ConfigView.label.pause.downloads.on.exit");
			pauseOnExit.setLayoutData(gridData);
	
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter resumeOnStart = new BooleanParameter(gPR,
					"Resume Downloads On Start", "ConfigView.label.resume.downloads.on.start");
			resumeOnStart.setLayoutData(gridData);
		}
		
		if ( userMode >= 0 ){
			
			Group gStop = new Group(cDisplay, SWT.NULL);
			Messages.setLanguageText(gStop, LBLKEY_PREFIX + "stop");
			layout = new GridLayout(2, false);
			gStop.setLayout(layout);
			gStop.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));

				// done downloading
			
			gridData = new GridData();
			label = new Label(gStop, SWT.NULL);
		    Messages.setLanguageText(label, "ConfigView.label.stop.downcomp");
		    label.setLayoutData( gridData );
			
		    int	shutdown_types = platform.getShutdownTypes();
		    
			List<String>	l_action_values = new ArrayList<String>();
			List<String>	l_action_descs 	= new ArrayList<String>();

			l_action_values.add( "Nothing" ); 
			l_action_values.add( "QuitVuze" );
			
			if (( shutdown_types & PlatformManager.SD_SLEEP ) != 0 ){
				
				l_action_values.add( "Sleep" );
			}
			if (( shutdown_types & PlatformManager.SD_HIBERNATE ) != 0 ){
				
				l_action_values.add( "Hibernate" );
			}
			if (( shutdown_types & PlatformManager.SD_SHUTDOWN ) != 0 ){
				
				l_action_values.add( "Shutdown" );
			}
			
			String[] action_values = l_action_values.toArray( new String[ l_action_values.size()]);
					
			for ( String s: action_values ){
				
				l_action_descs.add( MessageText.getString( "ConfigView.label.stop." + s ));
			}
			
			String[] action_descs = l_action_descs.toArray( new String[ l_action_descs.size()]);

			new StringListParameter(gStop, "On Downloading Complete Do", "Nothing", action_descs, action_values );

				// done seeding
			
			gridData = new GridData();
		    label = new Label(gStop, SWT.NULL);
		    Messages.setLanguageText(label, "ConfigView.label.stop.seedcomp");
		    label.setLayoutData( gridData );
					    
			new StringListParameter(gStop, "On Seeding Complete Do", "Nothing", action_descs, action_values );
		}
		
		if ( userMode > 0 && platform.hasCapability( PlatformManagerCapabilities.AccessExplicitVMOptions )){
			
			Group gJVM = new Group(cDisplay, SWT.NULL);
			Messages.setLanguageText(gJVM, LBLKEY_PREFIX + "jvm");
			layout = new GridLayout(2, false);
			gJVM.setLayout(layout);
			gJVM.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
			
				// info
			
			label = new Label(gJVM, SWT.NULL);
			Messages.setLanguageText(label, "jvm.info");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			label.setLayoutData( gridData );
			
			try{
				final File option_file = platform.getVMOptionFile();
			 	
				String[] options = platform.getExplicitVMOptions();
			
				for ( String option: options ){
					
					label = new Label(gJVM, SWT.NULL);
					label.setText( option );
					gridData = new GridData();
					gridData.horizontalSpan = 2;
					label.setLayoutData( gridData );
				}
				
					// show option file
				
				label = new Label(gJVM, SWT.NULL);
				Messages.setLanguageText(label, "jvm.show.file", new String[]{ option_file.getAbsolutePath() });

				Button show_folder_button = new Button( gJVM, SWT.PUSH );
				
			 	Messages.setLanguageText( show_folder_button, "MyTorrentsView.menu.explore");
			 	
			 	show_folder_button.addSelectionListener(
			 		new SelectionAdapter()
			 		{
			 			public void
			 			widgetSelected(
			 				SelectionEvent e )
			 			{
			 				
			 				ManagerUtils.open( option_file );
			 			}
			 		});

			 	label = new Label(gJVM, SWT.NULL);			
				Messages.setLanguageText(label, "jvm.reset");

				Button reset_button = new Button( gJVM, SWT.PUSH );
				
			 	Messages.setLanguageText( reset_button, "Button.reset");
			 	
			 	reset_button.addSelectionListener(
			 		new SelectionAdapter()
			 		{
			 			public void
			 			widgetSelected(
			 				SelectionEvent event )
			 			{
			 				try{
			 					platform.setExplicitVMOptions( new String[0] );
			 					
			 				}catch( Throwable e ){
			 					
			 					Debug.out( e );
			 				}
			 			}
			 		});
			 	
			}catch( Throwable e ){
				
				Debug.out( e );
				
				label = new Label(gJVM, SWT.NULL);
				Messages.setLanguageText(label, "jvm.error", new String[]{ Debug.getNestedExceptionMessage(e) });
				gridData = new GridData();
				gridData.horizontalSpan = 2;
				label.setLayoutData( gridData );
			}
		}

		return cDisplay;
	}

}
