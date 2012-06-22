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

import java.io.File;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.backup.BackupManager;
import com.aelitis.azureus.core.backup.BackupManagerFactory;
import com.aelitis.azureus.ui.UserPrompterResultListener;


public class ConfigSectionBackupRestore implements UISWTConfigSection {

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return "backuprestore";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {

	}
	
	public int maxUserMode() {
		return 0;
	}


	public Composite 
	configSectionCreate(
		final Composite parent) 
	{
		GridData gridData;
		GridLayout layout;

		final Composite cBR = new Composite( parent, SWT.NULL );

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL |  GridData.HORIZONTAL_ALIGN_FILL);
		cBR.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		cBR.setLayout(layout);
				
	    Label	info_label = new Label( cBR, SWT.WRAP );
	    Messages.setLanguageText( info_label, "ConfigView.section.br.overview" );
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL );
	    gridData.horizontalSpan = 1;
	    
	    info_label.setLayoutData( gridData );
	    
	    final BackupManager	backup_manager = BackupManagerFactory.getManager();
	    
	    	// backup
	    
		Group gBackup = new Group(cBR, SWT.NULL);
		Messages.setLanguageText(gBackup, "br.backup");
		layout = new GridLayout(2, false);
		gBackup.setLayout(layout);
		gBackup.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
	    
	    Label backup_manual_label = new Label(gBackup, SWT.NULL );
	    Messages.setLanguageText(backup_manual_label, "br.backup.manual.info");

	    Button backup_button = new Button(gBackup, SWT.PUSH);
	    Messages.setLanguageText(backup_button, "br.backup");
	    
	    backup_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	String	def_dir = COConfigurationManager.getStringParameter( "br.backup.folder.default" );
			        	
						DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),	SWT.APPLICATION_MODAL);
						
						if ( def_dir != null ){
							dialog.setFilterPath( def_dir );
						}
						
						dialog.setMessage(MessageText.getString("br.backup.folder.info"));
						dialog.setText(MessageText.getString("br.backup.folder.title"));
						
						String path = dialog.open();
						
						if ( path != null ){
																
							COConfigurationManager.setParameter( "br.backup.folder.default", path );
								
							final TextViewerWindow viewer = 
								new TextViewerWindow(
										MessageText.getString( "br.backup.progress" ),
										null, "", true, true );
											
							viewer.setEditable( false );
							
							viewer.setOKEnabled( false );
							
							backup_manager.backup(
								new File( path ),
								new BackupManager.BackupListener()
								{
									public void
									reportProgress(
										String		str )
									{
										append( str, false );
									}
									
									public void
									reportComplete()
									{
										append( "Backup Complete!", true );

									}
									
									public void
									reportError(
										Throwable 	error )
									{
										append( "Backup Failed: " + Debug.getNestedExceptionMessage( error ), true );
									}
									
									private void
									append(
										final String		str,
										final boolean		complete )
									{	
										Utils.execSWTThread(
											new AERunnable() 
											{
												public void 
												runSupport() 
												{
													if ( str.endsWith( "..." )){
														
														viewer.append( str );
														
													}else{
													
														viewer.append( str + "\r\n" );
													}
													
													if ( complete ){
														
														viewer.setOKEnabled( true );
													}
												}
											});
									}
								});
							
							viewer.goModal();
						}
			        }
				});
	    
	    	// restore
	    
		Group gRestore = new Group(cBR, SWT.NULL);
		Messages.setLanguageText(gRestore, "br.restore");
		layout = new GridLayout(2, false);
		gRestore.setLayout(layout);
		gRestore.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
		
	    Label restore_label = new Label(gRestore, SWT.NULL );
	    Messages.setLanguageText(restore_label, "br.restore.info");

	    Button restore_button = new Button(gRestore, SWT.PUSH);
	    Messages.setLanguageText(restore_button, "br.restore");

	    restore_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	String	def_dir = COConfigurationManager.getStringParameter( "br.backup.folder.default" );
			        	
						DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),	SWT.APPLICATION_MODAL );
						
						if ( def_dir != null ){
							dialog.setFilterPath( def_dir );
						}
						
						dialog.setMessage(MessageText.getString("br.restore.folder.info"));
						
						dialog.setText(MessageText.getString("br.restore.folder.title"));
						
						final String path = dialog.open();
						
						if ( path != null ){

				        	MessageBoxShell mb = new MessageBoxShell(
				        			SWT.ICON_WARNING | SWT.OK | SWT.CANCEL,
				        			MessageText.getString("br.restore.warning.title"),
				        			MessageText.getString("br.restore.warning.info"));
				        	
				        	mb.setDefaultButtonUsingStyle(SWT.CANCEL);
				        	mb.setParent(parent.getShell());
	
				        	mb.open(new UserPrompterResultListener() {
										public void prompterClosed(int returnVal) {
											if (returnVal != SWT.OK) {
												return;
											}
	
											final TextViewerWindow viewer = 
												new TextViewerWindow(
														MessageText.getString( "br.backup.progress" ),
														null, "", true, true );
															
											viewer.setEditable( false );

											viewer.setOKEnabled( false );
											
											backup_manager.restore(
												new File( path ),
												new BackupManager.BackupListener()
												{
													public void
													reportProgress(
														String		str )
													{
														append( str, false );
													}
													
													public void
													reportComplete()
													{
														append( "Restore Complete!", true );														
													}
													
													public void
													reportError(
														Throwable 	error )
													{
														append( "Restore Failed: " + Debug.getNestedExceptionMessage( error ), true );
													}
													
													private void
													append(
														final String		str,
														final boolean		complete )
													{	
														Utils.execSWTThread(
															new AERunnable() 
															{
																public void 
																runSupport() 
																{
																	if ( str.endsWith( "..." )){
																		
																		viewer.append( str );
																		
																	}else{
																	
																		viewer.append( str + "\r\n" );
																	}
																	
																	if ( complete ){
																		
																		viewer.setOKEnabled( true );
																	}
																}
															});
													}													
												});
											
											viewer.goModal();
												
										}
									});
						}
			        }
			    });
	    
		return( cBR );
	}
}
