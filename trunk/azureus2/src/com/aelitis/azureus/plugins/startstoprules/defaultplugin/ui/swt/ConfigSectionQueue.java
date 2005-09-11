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

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

/** General Queueing options
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionQueue implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

  /**
   * Create the "Queue" Tab in the Configuration view
   */
  public Composite configSectionCreate(Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label label;

    // main tab set up

    Composite gMainTab = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    gMainTab.setLayout(layout);

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxdownloads"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    final IntParameter maxDLs = new IntParameter(gMainTab, "max downloads");
    maxDLs.setLayoutData(gridData); //$NON-NLS-1$

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.maxactivetorrents"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 40;
    final IntParameter maxActiv = new IntParameter(gMainTab, "max active torrents");
    maxActiv.setLayoutData(gridData); //$NON-NLS-1$
    
    maxDLs.addChangeListener( new ParameterChangeListener() {
      public void parameterChanged( Parameter p, boolean caused_internally ) {
        int val1 = maxDLs.getValue();
        int val2 = maxActiv.getValue();
        
        if( (val1 == 0 || val1 > val2) && val2 != 0) {
          maxActiv.setValue( val1 );
        }
      }
    });
    
    maxActiv.addChangeListener( new ParameterChangeListener() {
      public void parameterChanged( Parameter p, boolean caused_internally ) {
        int val1 = maxDLs.getValue();
        int val2 = maxActiv.getValue();
        
        if( (val1 == 0 || val1 > val2) && val2 != 0) {
          maxDLs.setValue( val2 );
        }
      }
    });

    String	bytes_per_sec 	= DisplayFormatters.getRateUnit( DisplayFormatters.UNIT_B );
    String	k_per_sec 		= DisplayFormatters.getRateUnit( DisplayFormatters.UNIT_KB );

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minSpeedForActiveDL"); //$NON-NLS-1$
    final String activeDLLabels[] = new String[57];
    final int activeDLValues[] = new int[57];
    int pos = 0;
    for (int i = 0; i < 256; i += 64) {
        activeDLLabels[pos] = "" + i + " " + bytes_per_sec;
        activeDLValues[pos] = i;
        pos++;
    }
    for (int i = 256; i < 1024; i += 256) {
      activeDLLabels[pos] = "" + i + " " + bytes_per_sec;
      activeDLValues[pos] = i;
      pos++;
    }
    for (int i = 1; pos < activeDLLabels.length; i++) {
      activeDLLabels[pos] = "" + i + " " + k_per_sec;
      activeDLValues[pos] = i * 1024;
      pos++;
    }
    new IntListParameter(gMainTab, "StartStopManager_iMinSpeedForActiveDL", activeDLLabels, activeDLValues);

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.minSpeedForActiveSeeding");
    final String activeSeedingLabels[] = new String[27];
    final int activeSeedingValues[] = new int[27];
    pos = 0;
        
    for (int i = 0; i < 256; i += 64) {
    	activeSeedingLabels[pos] = "" + i + " " + bytes_per_sec;
    	activeSeedingValues[pos] = i;
        pos++;
    }
    for (int i = 256; i < 1024; i += 256) {
      activeSeedingLabels[pos] = "" + i + " " + bytes_per_sec;
      activeSeedingValues[pos] = i;
      pos++;
    }
    for (int i = 1; pos < activeSeedingLabels.length; i++) {
      activeSeedingLabels[pos] = "" + i + " " + k_per_sec;
      activeSeedingValues[pos] = i * 1024;
      pos++;
    }
    new IntListParameter(gMainTab, "StartStopManager_iMinSpeedForActiveSeeding", 
                         activeSeedingLabels, activeSeedingValues);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gMainTab, "StartStopManager_bNewSeedsMoveTop", true,
                         "ConfigView.label.queue.newseedsmovetop").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gMainTab, "Alert on close", true,
                         "ConfigView.label.showpopuponclose").setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gMainTab, "StartStopManager_bDebugLog",
                         "ConfigView.label.queue.debuglog").setLayoutData(gridData);

    return gMainTab;
  }

  public String configSectionGetName() {
    return "queue";
  }

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
}
