/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.util.FileUtil;

public class ConfigSectionInterfaceDisplay implements ConfigSectionSWT {
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
    
    new BooleanParameter(cLook, "useCustomTab",true, "ConfigView.section.style.useCustomTabs");
    new BooleanParameter(cLook, "Show Download Basket",false, "ConfigView.section.style.showdownloadbasket");
    new BooleanParameter(cLook, "Add URL Silently",false, "ConfigView.section.style.addurlsilently");
    
    String osName = System.getProperty("os.name");
    if (osName.equals("Windows XP")) {
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
        e.printStackTrace();
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
              e.printStackTrace();
            }
          } else {
            try {
              File fDest =
                new File(
                  System.getProperty("java.home")
                    + "\\bin\\javaw.exe.manifest");
              fDest.delete();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
    }

    if (osName.equals("Linux") && SWT.getPlatform().equals("gtk")) {
      // See Eclipse Bug #42416 ([Platform Inconsistency] GC(Table) has wrong origin)
      new BooleanParameter(cLook, "SWT_bGTKTableBug", "ConfigView.section.style.verticaloffset");
    }

    
    /**
     * Disabled for the moment because of some side effects
     */
    /*
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysShowTorrentFiles");
    new BooleanParameter(cLook, "Always Show Torrent Files", true);
    */

    new BooleanParameter(cLook, "config.style.useSIUnits",false, "ConfigView.section.style.useSIUnits");
    new BooleanParameter(cLook, "config.style.refreshMT",false, "ConfigView.section.style.alwaysRefreshMyTorrents");
    
    Composite cArea = new Composite(cLook, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData());
    
    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.colorScheme");
    ColorParameter colorScheme = new ColorParameter(cArea, "Color Scheme",0,128,255,true);
    gridData = new GridData();
    gridData.widthHint = 50;
    colorScheme.setLayoutData(gridData);

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.guiUpdate");
    int[] values = { 100 , 250 , 500 , 1000 , 2000 , 5000 };
    String[] labels = { "100 ms" , "250 ms" , "500 ms" , "1 s" , "2 s" , "5 s" };
    new IntListParameter(cArea, "GUI Refresh", 250, labels, values);

    
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
    
    return cLook;
  }
}
