/*
 * File    : ConfigPanel*.java
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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.Constants;

public class ConfigSectionInterfaceDisplay implements ConfigSectionSWT {
  private static final String[] sColorsToOverride = { "progressBar", "error", "warning", "altRow" };
  private Color[] colorsToOverride = { Colors.colorProgressBar, 
      Colors.colorError,
      Colors.colorWarning,
      Colors.colorAltRow
                                      };
  private Button[] btnColorReset = new Button[sColorsToOverride.length];

  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_INTERFACE;
  }

	public String configSectionGetName() {
		return "display";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    // "Display" Sub-Section:
    // ----------------------
    // Any Look & Feel settings that don't really change the way the user 
    // normally interacts
    Label label;
    GridLayout layout;
    GridData gridData;
    Composite cLook = new Composite(parent,  SWT.NULL);
    cLook.setLayoutData(new GridData(GridData.FILL_BOTH));
    layout = new GridLayout();
    layout.numColumns = 1;
    cLook.setLayout(layout);
    
    BooleanParameter bpCustomTab = new BooleanParameter(cLook, "useCustomTab",
                                                        true, 
                                                        "ConfigView.section.style.useCustomTabs");
    Control cFancyTab = new BooleanParameter(cLook, "GUI_SWT_bFancyTab", true,
                                                    "ConfigView.section.style.useFancyTabs").getControl();

    Control[] controls = { cFancyTab };
    bpCustomTab.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));

    new BooleanParameter(cLook, "Show Download Basket",false, "ConfigView.section.style.showdownloadbasket");
    new BooleanParameter(cLook, "Add URL Silently",false, "ConfigView.section.style.addurlsilently");
    
    if ( Constants.isWindowsXP ) {
      final Button enableXPStyle = new Button(cLook, SWT.CHECK);
      Messages.setLanguageText(enableXPStyle, "ConfigView.section.style.enableXPStyle");
      
      boolean enabled = false;
      boolean valid = false;
      try {
        File f =
          new File(
            System.getProperty("java.home")
              + "\\bin\\javaw.exe.manifest");
        if (f.exists()) {
          enabled = true;
        }
        f= FileUtil.getApplicationFile("javaw.exe.manifest");
        if(f.exists()) {
            valid = true;
        }
      } catch (Exception e) {
      	Debug.printStackTrace( e );
        valid = false;
      }
      enableXPStyle.setEnabled(valid);
      enableXPStyle.setSelection(enabled);
      enableXPStyle.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
          //In case we enable the XP Style
          if (enableXPStyle.getSelection()) {
            try {
              File fDest =
                new File(
                  System.getProperty("java.home")
                    + "\\bin\\javaw.exe.manifest");
              File fOrigin = new File("javaw.exe.manifest");
              if (!fDest.exists() && fOrigin.exists()) {
                FileUtil.copyFile(fOrigin, fDest);
              }
            } catch (Exception e) {
            	Debug.printStackTrace( e );
            }
          } else {
            try {
              File fDest =
                new File(
                  System.getProperty("java.home")
                    + "\\bin\\javaw.exe.manifest");
              fDest.delete();
            } catch (Exception e) {
            	Debug.printStackTrace( e );
            }
          }
        }
      });
    }

    if ( Constants.isLinux && SWT.getPlatform().equals("gtk")) {
      // See Eclipse Bug #42416 ([Platform Inconsistency] GC(Table) has wrong origin)
      new BooleanParameter(cLook, "SWT_bGTKTableBug", "ConfigView.section.style.verticaloffset");
    }

    new BooleanParameter(cLook, "GUI_SWT_bAlternateTablePainting", 
                         "ConfigView.section.style.alternateTablePainting");
    
    /**
     * Disabled for the moment because of some side effects
     */
    /*
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysShowTorrentFiles");
    new BooleanParameter(cLook, "Always Show Torrent Files", true);
    */

    new BooleanParameter(cLook, "config.style.useSIUnits",false, "ConfigView.section.style.useSIUnits");
    new BooleanParameter(cLook, "config.style.useUnitsRateBits",false, "ConfigView.section.style.useUnitsRateBits");
    new BooleanParameter(cLook, "config.style.doNotUseGB",false, "ConfigView.section.style.doNotUseGB");

    
    Composite cArea = new Composite(cLook, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData());
    
    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.colorScheme");
    ColorParameter colorScheme = new ColorParameter(cArea, "Color Scheme",0,128,255);
    gridData = new GridData();
    gridData.widthHint = 50;
    colorScheme.setLayoutData(gridData);
    
    Group cColorOverride = new Group(cArea, SWT.NULL);
    Messages.setLanguageText(cColorOverride, "ConfigView.section.style.colorOverrides");
    layout = new GridLayout();
    layout.numColumns = 3;
    cColorOverride.setLayout(layout);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 2;
    cColorOverride.setLayoutData(gridData);
    
    for (int i = 0; i < sColorsToOverride.length; i++) {
      String sConfigID = "Colors."  + sColorsToOverride[i];
      label = new Label(cColorOverride, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.style.colorOverride." + sColorsToOverride[i]);
      ColorParameter colorParm = 
          new ColorParameter(cColorOverride, sConfigID,
                             colorsToOverride[i].getRed(), 
                             colorsToOverride[i].getGreen(), 
                             colorsToOverride[i].getBlue()) {
        public void newColorChosen() {
          COConfigurationManager.setParameter(sParamName + ".override", true);
          for (int i = 0; i < sColorsToOverride.length; i++) {
            if (sParamName.equals("Colors." + sColorsToOverride[i])) {
              btnColorReset[i].setEnabled(true);
              break;
            }
          }
        }
      };
      gridData = new GridData();
      gridData.widthHint = 50;
      colorParm.setLayoutData(gridData);
      btnColorReset[i] = new Button(cColorOverride, SWT.PUSH);
      Messages.setLanguageText(btnColorReset[i], "ConfigView.section.style.colorOverrides.reset");
      btnColorReset[i].setEnabled(COConfigurationManager.getBooleanParameter(sConfigID + ".override", false));
      btnColorReset[i].setData("ColorName", sConfigID);
      btnColorReset[i].addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          Button btn = (Button)event.widget;
          String sName = (String)btn.getData("ColorName");
          if (sName != null) {
            COConfigurationManager.setParameter(sName + ".override", false);
            btn.setEnabled(false);
          }
        }
      });
    }

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.guiUpdate");
    int[] values = { 100 , 250 , 500 , 1000 , 2000 , 5000 };
    String[] labels = { "100 ms" , "250 ms" , "500 ms" , "1 s" , "2 s" , "5 s" };
    new IntListParameter(cArea, "GUI Refresh", 1000, labels, values);

    
    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.graphicsUpdate");
    gridData = new GridData();
    gridData.widthHint = 15;
    IntParameter graphicUpdate = new IntParameter(cArea, "Graphics Update", 1, -1, false);
    graphicUpdate.setLayoutData(gridData);
    
    
    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.reOrderDelay");
    gridData = new GridData();
    gridData.widthHint = 15;
    IntParameter reorderDelay = new IntParameter(cArea, "ReOrder Delay");
    reorderDelay.setLayoutData(gridData);
    
    new BooleanParameter(cArea, "config.style.table.sortDefaultAscending", true, "ConfigView.section.style.sortDefaultAscending");

    
    return cLook;
  }
}
