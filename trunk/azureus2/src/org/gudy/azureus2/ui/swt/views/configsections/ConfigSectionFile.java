/*
 * File    : ConfigPanelFile.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionFile implements ConfigSectionSWT {
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

      // zero new files
    BooleanParameter zeroNew = new BooleanParameter(gFile, "Zero New", false,
                                                    "ConfigView.label.zeronewfiles");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    zeroNew.setLayoutData(gridData);

      // incrementaal file creation
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

      // check on complete
    BooleanParameter checkOnComp = new BooleanParameter(gFile, "Check Pieces on Completion", true,
                                                        "ConfigView.label.checkOncompletion");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    checkOnComp.setLayoutData(gridData);

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

      final BooleanParameter save_peers = new BooleanParameter(cResumeGroup, "File.save.peers.enable", true,
                                                               "ConfigView.section.file.save.peers.enable");
      gridData = new GridData();
      gridData.horizontalSpan = 3;
      save_peers.setLayoutData(gridData);

      // save peers max


      final Label lblSavePeersMax = new Label(cResumeGroup, SWT.NULL);
      Messages.setLanguageText(lblSavePeersMax, "ConfigView.section.file.save.peers.max");
      final IntParameter savePeersMax = new IntParameter(cResumeGroup, "File.save.peers.max", TRTrackerClient.DEFAULT_PEERS_TO_CACHE );
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

    // savepath
    BooleanParameter saveDefault = new BooleanParameter(gFile, "Use default data dir",
                                                        "ConfigView.label.defaultsavepath");

    Composite cSave = new Composite(gFile, SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      cSave.setLayoutData(gridData);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      cSave.setLayout(layout);

      gridData = new GridData(GridData.FILL_HORIZONTAL);
      final StringParameter pathParameter = new StringParameter(cSave, "Default save path" );
      pathParameter.setLayoutData(gridData);

      Button browse = new Button(cSave, SWT.PUSH);
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

      Control[] controls2 = new Control[2];
      controls2[0] = pathParameter.getControl();
      controls2[1] = browse;
      IAdditionalActionPerformer defaultSave = new ChangeSelectionActionPerformer(controls2);
      saveDefault.setAdditionalActionPerformer(defaultSave);

    // Move Completed
    BooleanParameter moveCompleted = new BooleanParameter(gFile, "Move Completed When Done", false,
                                                          "ConfigView.label.movecompleted");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    moveCompleted.setLayoutData(gridData);

    Composite gMoveCompleted = new Composite(gFile, SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalIndent = 25;
      gridData.horizontalSpan = 2;
      gMoveCompleted.setLayoutData(gridData);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 4;
      layout.numColumns = 3;
      gMoveCompleted.setLayout(layout);

      Label lDir = new Label(gMoveCompleted, SWT.NULL);
      Messages.setLanguageText(lDir, "ConfigView.label.directory");

      gridData = new GridData(GridData.FILL_HORIZONTAL);
      final StringParameter movePath = new StringParameter(gMoveCompleted,
                                                           "Completed Files Directory", "");
      movePath.setLayoutData(gridData);

      Button browse3 = new Button(gMoveCompleted, SWT.PUSH);
      browse3.setImage(imgOpenFolder);
      imgOpenFolder.setBackground(browse3.getBackground());
      browse3.setToolTipText(MessageText.getString("ConfigView.button.browse"));

      browse3.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
          dialog.setFilterPath(movePath.getValue());
          dialog.setText(MessageText.getString("ConfigView.dialog.choosemovepath"));
          String path = dialog.open();
          if (path != null) {
            movePath.setValue(path);
          }
        }
      });


      BooleanParameter moveTorrent = new BooleanParameter(gMoveCompleted, "Move Torrent When Done", true,
                                                          "ConfigView.label.movetorrent");
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      moveTorrent.setLayoutData(gridData);

      BooleanParameter moveOnly = new BooleanParameter(gMoveCompleted, "Move Only When In Default Save Dir", true,
                                                       "ConfigView.label.moveonlyusingdefaultsave");
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      moveOnly.setLayoutData(gridData);


      Control[] controls3 = new Control[]{ gMoveCompleted };
      IAdditionalActionPerformer grayPathAndButton2 = new ChangeSelectionActionPerformer(controls3);
      moveCompleted.setAdditionalActionPerformer(grayPathAndButton2);


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

      new BooleanParameter(cExtensions, "priorityExtensionsIgnoreCase",
                           "ConfigView.label.ignoreCase");

    // Confirm Delete
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gFile, "Confirm Data Delete", true,
                         "ConfigView.section.file.confirm_data_delete").setLayoutData(gridData);

    return gFile;
  }
}
