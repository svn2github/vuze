/*
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;

import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/** Auto Starting specific options
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionSeedingAutoStarting implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
		return "queue.seeding";
  }

	public String configSectionGetName() {
		return "queue.seeding.autoStarting";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }

  public Composite configSectionCreate(Composite parent) {
    // Seeding Automation Setup
    GridData gridData;
    GridLayout layout;
    Label label;
    Composite cArea;

    Composite gQR = new Composite(parent, SWT.NULL);
    gQR.addControlListener(new Utils.LabelWrapControlListener());

    layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    gQR.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gQR.setLayoutData(gridData);


    // ** Begin Rank Type area
    // Rank Type area.  Encompases the 4 (or more) options groups

    Composite cRankType = new Group(gQR, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 2;
    cRankType.setLayout(layout);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    cRankType.setLayoutData(gridData);
    Messages.setLanguageText(cRankType, "ConfigView.label.seeding.rankType");

    // Seeds:Peer options
    RadioParameter rparamPeerSeed =
        new RadioParameter(cRankType, "StartStopManager_iRankType", 
                           StartStopRulesDefaultPlugin.RANK_SPRATIO);
    Messages.setLanguageText(rparamPeerSeed, "ConfigView.label.seeding.rankType.peerSeed");
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    rparamPeerSeed.setLayoutData(gridData);

    new Label(cRankType, SWT.NULL);


    // Seed Count options
    RadioParameter rparamSeedCount =
        new RadioParameter(cRankType, "StartStopManager_iRankType", 
                           StartStopRulesDefaultPlugin.RANK_SEEDCOUNT);
    Messages.setLanguageText(rparamSeedCount, "ConfigView.label.seeding.rankType.seed");
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    rparamSeedCount.setLayoutData(gridData);

    Group gSeedCount = new Group(cRankType, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 2;
    layout.marginWidth = 2;
    layout.numColumns = 3;
    gSeedCount.setLayout(layout);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.verticalSpan = 1;
    gSeedCount.setLayoutData(gridData);
    Messages.setLanguageText(gSeedCount, "ConfigView.label.seeding.rankType.seed.options");

    label = new Label(gSeedCount, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.rankType.seed.fallback");

    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gridData.widthHint = 20;
    IntParameter intParamFallBack = new IntParameter(gSeedCount, "StartStopManager_iRankTypeSeedFallback");
    intParamFallBack.setLayoutData(gridData);

    Label labelFallBackSeeds = new Label(gSeedCount, SWT.NULL);
    label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    Messages.setLanguageText(labelFallBackSeeds, "ConfigView.label.seeds");

    Control[] controlsSeedCount = { gSeedCount };
    rparamSeedCount.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsSeedCount));


    // timed rotation ranking type
    RadioParameter rparamTimed =
        new RadioParameter(cRankType, "StartStopManager_iRankType", 
                           StartStopRulesDefaultPlugin.RANK_TIMED);
    Messages.setLanguageText(rparamTimed, "ConfigView.label.seeding.rankType.timedRotation");
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    rparamTimed.setLayoutData(gridData);

    new Label(cRankType, SWT.NULL);


    // No Ranking
    RadioParameter rparamNone =
        new RadioParameter(cRankType, "StartStopManager_iRankType", 
                           StartStopRulesDefaultPlugin.RANK_NONE);
    Messages.setLanguageText(rparamNone, "ConfigView.label.seeding.rankType.none");
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    rparamNone.setLayoutData(gridData);
    
    new Label(cRankType, SWT.NULL);
    
    // ** End Rank Type area


    Composite cNoTimeNone = new Composite(gQR, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 2;
    cNoTimeNone.setLayout(layout);
    gridData = new GridData();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cNoTimeNone.setLayoutData(gridData);
    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cNoTimeNone, 
                         "StartStopManager_bPreferLargerSwarms", 
                         "ConfigView.label.seeding.preferLargerSwarms").setLayoutData(gridData);



    label = new Label(cNoTimeNone, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minPeersToBoostNoSeeds"); //$NON-NLS-1$
    final String boostQRPeersLabels[] = new String[9];
    final int boostQRPeersValues[] = new int[9];
    String peers = MessageText.getString("ConfigView.text.peers");
    for (int i = 0; i < boostQRPeersValues.length; i++) {
      boostQRPeersLabels[i] = (i+1) + " " + peers; //$NON-NLS-1$
      boostQRPeersValues[i] = (i+1);
    }
    gridData = new GridData();
    new IntListParameter(cNoTimeNone, "StartStopManager_iMinPeersToBoostNoSeeds", boostQRPeersLabels, boostQRPeersValues);


    label = new Label(cNoTimeNone, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.numPeersAsFullCopy");

    cArea = new Composite(cNoTimeNone, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    cArea.setLayout(layout);
    gridData = new GridData();
    cArea.setLayoutData(gridData);

    gridData = new GridData();
    gridData.widthHint = 20;
    IntParameter paramFakeFullCopy = new IntParameter(cArea, "StartStopManager_iNumPeersAsFullCopy");
    paramFakeFullCopy.setLayoutData(gridData);
    final Text txtFakeFullCopy = (Text)paramFakeFullCopy.getControl();

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.peers");


    final Composite cFullCopyOptionsArea = new Composite(cNoTimeNone, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    cFullCopyOptionsArea.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalIndent = 15;
    gridData.horizontalSpan = 2;
    cFullCopyOptionsArea.setLayoutData(gridData);
    
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Image img = ImageRepository.getImage("subitem");
    img.setBackground(label.getBackground());
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gridData);
    label.setImage(img);
    
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.fakeFullCopySeedStart");

    gridData = new GridData();
    gridData.widthHint = 20;
    new IntParameter(cFullCopyOptionsArea, "StartStopManager_iFakeFullCopySeedStart").setLayoutData(gridData);
    label = new Label(cFullCopyOptionsArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeds");
    

    final int iNumPeersAsFullCopy = COConfigurationManager.getIntParameter("StartStopManager_iNumPeersAsFullCopy");
    controlsSetEnabled(cFullCopyOptionsArea.getChildren(), iNumPeersAsFullCopy != 0);

    paramFakeFullCopy.getControl().addListener(SWT.Modify, new Listener() {
        public void handleEvent(Event event) {
          try {
            Text control = (Text)event.widget;
            if (control.getEnabled()) {
              int value = Integer.parseInt(control.getText());
              boolean enabled = (value != 0);
              if (cFullCopyOptionsArea.getEnabled() != enabled) {
                cFullCopyOptionsArea.setEnabled(enabled);
                controlsSetEnabled(cFullCopyOptionsArea.getChildren(), enabled);
              }
            }
          }
          catch (Exception e) {}
        }
    });

    Control[] controlsNoTimeNone = { cNoTimeNone };
    rparamPeerSeed.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsNoTimeNone) {
      public void performAction() {
        super.performAction();
        Event e = new Event();
        e.widget = txtFakeFullCopy;
        txtFakeFullCopy.notifyListeners(SWT.Modify, e);
      }
    });
    rparamSeedCount.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsNoTimeNone) {
      public void performAction() {
        super.performAction();
        Event e = new Event();
        e.widget = txtFakeFullCopy;
        txtFakeFullCopy.notifyListeners(SWT.Modify, e);
      }
    });
    
    
    int iRankType = COConfigurationManager.getIntParameter("StartStopManager_iRankType");
    boolean enable = (iRankType == StartStopRulesDefaultPlugin.RANK_SPRATIO || 
                      iRankType == StartStopRulesDefaultPlugin.RANK_SEEDCOUNT);
    controlsSetEnabled(controlsNoTimeNone, enable);
      

    new BooleanParameter(gQR, "StartStopManager_bAutoStart0Peers", 
                         "ConfigView.label.seeding.autoStart0Peers");

    return gQR;
  }
  private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
    for(int i = 0 ; i < controls.length ; i++) {
      if (controls[i] instanceof Composite)
        controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
      controls[i].setEnabled(bEnabled);
    }
  }
}

