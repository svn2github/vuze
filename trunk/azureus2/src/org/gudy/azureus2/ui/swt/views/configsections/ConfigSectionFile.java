/*
 * File    : ConfigPanelFile.java
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class ConfigSectionFile implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

  public String configSectionGetName() {
    return ConfigSection.SECTION_FILES;
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }

  public Composite configSectionCreate(final Composite parent) {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    Label label;

    Composite gFile = new Composite(parent, SWT.NULL);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    gFile.setLayout(layout);

    
    int userMode = COConfigurationManager.getIntParameter("User Mode");

    // Default Dir Sction
    Group gDefaultDir = new Group(gFile, SWT.NONE);
    Messages.setLanguageText(gDefaultDir, "ConfigView.section.file.defaultdir.section");
    layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginHeight = 0;
    gDefaultDir.setLayout(layout);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gDefaultDir.setLayoutData(gridData);

    // Save Path
    Label lblDefaultDir = new Label(gDefaultDir, SWT.NONE);
    Messages.setLanguageText(lblDefaultDir, "ConfigView.section.file.defaultdir.ask");
    lblDefaultDir.setLayoutData(new GridData());
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter pathParameter = new StringParameter(gDefaultDir, "Default save path" );
    pathParameter.setLayoutData(gridData);

    Button browse = new Button(gDefaultDir, SWT.PUSH);
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaultsavepath"));
        String path = dialog.open();
        if (path != null) {
          pathParameter.setValue(path);
        }
      }
    });
    
    // def dir: autoSave
    BooleanParameter autoSaveToDir = new BooleanParameter(gDefaultDir,
				"Use default data dir", "ConfigView.section.file.defaultdir.auto");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    autoSaveToDir.setLayoutData(gridData);
    
    // def dir: best guess
    BooleanParameter bestGuess = new BooleanParameter(gDefaultDir,
				"DefaultDir.BestGuess", "ConfigView.section.file.defaultdir.bestguess");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    bestGuess.setLayoutData(gridData);

    IAdditionalActionPerformer aapDefaultDirStuff = new ChangeSelectionActionPerformer(
				bestGuess.getControls(), true);
    autoSaveToDir.setAdditionalActionPerformer(aapDefaultDirStuff);

    // def dir: auto update
    BooleanParameter autoUpdateSaveDir = new BooleanParameter(gDefaultDir, 
    		"DefaultDir.AutoUpdate", "ConfigView.section.file.defaultdir.lastused");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    autoUpdateSaveDir.setLayoutData(gridData);

    IAdditionalActionPerformer aapDefaultDirStuff2 = new ChangeSelectionActionPerformer(
    		autoUpdateSaveDir.getControls(), true);
    autoSaveToDir.setAdditionalActionPerformer(aapDefaultDirStuff2);


    ////////////////////
    
    if( userMode > 0 && !Constants.isWindows ) {
    	BooleanParameter xfsAllocation = 
    		new BooleanParameter(gFile, "XFS Allocation", false,
                                    "ConfigView.label.xfs.allocation");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	xfsAllocation.setLayoutData(gridData);
    }

    BooleanParameter zeroNew = null;
    
    if( userMode > 0 ) {
    	// zero new files
    	zeroNew = new BooleanParameter(gFile, "Zero New", false,
                                                    "ConfigView.label.zeronewfiles");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	zeroNew.setLayoutData(gridData);
    }

    
    if( userMode > 0 ) {
    	// truncate too large
    	BooleanParameter truncateLarge = 
    		new BooleanParameter(gFile, "File.truncate.if.too.large", false,
                                    "ConfigView.section.file.truncate.too.large");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	truncateLarge.setLayoutData(gridData);
    }
    
    if( userMode > 0 ) {
    	// incremental file creation
    	BooleanParameter incremental = new BooleanParameter(gFile, "Enable incremental file creation", false,
                                                        "ConfigView.label.incrementalfile");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	incremental.setLayoutData(gridData);

    	//Make the incremental checkbox (button) deselect when zero new is used
    	Button[] btnIncremental = {(Button)incremental.getControl()};
    	zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnIncremental));

    	//Make the zero new checkbox(button) deselct when incremental is used
    	Button[] btnZeroNew = {(Button)zeroNew.getControl()};
    	incremental.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(btnZeroNew));
    }
    
    
    if( userMode > 0 ) {
    	// check on complete
    	BooleanParameter checkOnComp = new BooleanParameter(gFile, "Check Pieces on Completion", true,
                                                        "ConfigView.label.checkOncompletion");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	checkOnComp.setLayoutData(gridData);
    }
    

    if( userMode > 1 ) {
    	
    	BooleanParameter strictLocking = 
    		new BooleanParameter(gFile, "File.strict.locking", true,
                                    "ConfigView.label.strictfilelocking");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	strictLocking.setLayoutData(gridData);
    }
    
    if( userMode > 0 ) {
    	// resume data
      final BooleanParameter bpUseResume = new BooleanParameter(gFile, "Use Resume", true,
                                                              "ConfigView.label.usefastresume");
      bpUseResume.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite cResumeGroup = new Composite(gFile, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 4;
      layout.numColumns = 3;
      cResumeGroup.setLayout(layout);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalIndent = 25;
      gridData.horizontalSpan = 2;
      cResumeGroup.setLayoutData(gridData);

      Label lblSaveResumeInterval = new Label(cResumeGroup, SWT.NULL);
      Messages.setLanguageText(lblSaveResumeInterval, "ConfigView.label.saveresumeinterval");

      IntParameter paramSaveInterval = new IntParameter(cResumeGroup, "Save Resume Interval");
      gridData = new GridData();
      gridData.widthHint = 30;
      paramSaveInterval.setLayoutData(gridData);

      Label lblMinutes = new Label(cResumeGroup, SWT.NULL);
      Messages.setLanguageText(lblMinutes, "ConfigView.text.minutes");

      // save peers

      final BooleanParameter recheck_all = new BooleanParameter(cResumeGroup, "On Resume Recheck All", false,
                                                               "ConfigView.section.file.resume.recheck.all");
      gridData = new GridData();
      gridData.horizontalSpan = 3;
      recheck_all.setLayoutData(gridData);
      // save peers

      final BooleanParameter save_peers = new BooleanParameter(cResumeGroup, "File.save.peers.enable", true,
                                                               "ConfigView.section.file.save.peers.enable");
      gridData = new GridData();
      gridData.horizontalSpan = 3;
      save_peers.setLayoutData(gridData);

      // save peers max


      final Label lblSavePeersMax = new Label(cResumeGroup, SWT.NULL);
      Messages.setLanguageText(lblSavePeersMax, "ConfigView.section.file.save.peers.max");
      final IntParameter savePeersMax = new IntParameter(cResumeGroup, "File.save.peers.max", TRTrackerAnnouncer.DEFAULT_PEERS_TO_CACHE );
      gridData = new GridData();
      gridData.widthHint = 30;
      savePeersMax.setLayoutData(gridData);
      final Label lblPerTorrent = new Label(cResumeGroup, SWT.NULL);
      Messages.setLanguageText(lblPerTorrent, "ConfigView.section.file.save.peers.pertorrent");


      final Control[] controls = { cResumeGroup };

      /*
      IAdditionalActionPerformer performer = new ChangeSelectionActionPerformer(controls);
      bpUseResume.setAdditionalActionPerformer(performer);
      */

      IAdditionalActionPerformer f_enabler =
        new GenericActionPerformer(controls) {
          public void performAction() {
            controlsSetEnabled(controls, bpUseResume.isSelected());

            if ( bpUseResume.isSelected()){
              lblSavePeersMax.setEnabled( save_peers.isSelected());
              savePeersMax.getControl().setEnabled( save_peers.isSelected());
              lblPerTorrent.setEnabled( save_peers.isSelected());
            }
          }
        };

      bpUseResume.setAdditionalActionPerformer(f_enabler);
      save_peers.setAdditionalActionPerformer(f_enabler);

    } //end usermode>0

      if( userMode > 0 ) {   	
      	// Auto-Prioritize
      	label = new Label(gFile, SWT.WRAP);
      	gridData = new GridData();
      	gridData.widthHint = 180;
      	label.setLayoutData(gridData);
      	Messages.setLanguageText(label, "ConfigView.label.priorityExtensions");

      	Composite cExtensions = new Composite(gFile, SWT.NULL);
      	gridData = new GridData(GridData.FILL_HORIZONTAL);
      	cExtensions.setLayoutData(gridData);
      	layout = new GridLayout();
      	layout.marginHeight = 0;
      	layout.marginWidth = 0;
      	layout.numColumns = 3;
      	cExtensions.setLayout(layout);

      	gridData = new GridData(GridData.FILL_HORIZONTAL);
      	new StringParameter(cExtensions, "priorityExtensions", "").setLayoutData(gridData);

      	new BooleanParameter(cExtensions, "priorityExtensionsIgnoreCase", "ConfigView.label.ignoreCase");
      }
      

    // Confirm Delete
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gFile, "Confirm Data Delete", true,
                         "ConfigView.section.file.confirm_data_delete").setLayoutData(gridData);


    try{
	    final PlatformManager	platform  = PlatformManagerFactory.getPlatformManager();
	    
	    if (platform.hasCapability(PlatformManagerCapabilities.RecoverableFileDelete)){

		    gridData = new GridData();
		    gridData.horizontalSpan = 2;
		    new BooleanParameter(gFile, "Move Deleted Data To Recycle Bin", true,
		                         "ConfigView.section.file.nativedelete").setLayoutData(gridData);

	    }    
    }catch( Throwable e ){
    	
    }
    
    if( userMode > 0 ) {
    	// check on complete
    	BooleanParameter backupConfig = 
    		new BooleanParameter(gFile, "Use Config File Backups", true,
                                    "ConfigView.label.backupconfigfiles");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	backupConfig.setLayoutData(gridData);
    }

    return gFile;
  }
}
