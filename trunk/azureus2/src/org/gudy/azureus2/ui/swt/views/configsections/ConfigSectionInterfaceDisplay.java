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
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

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
    layout.numColumns = 2;
    cLook.setLayout(layout);
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.useCustomTabs"); //$NON-NLS-1$
    new BooleanParameter(cLook, "useCustomTab",true); //$NON-NLS-1$
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.showdownloadbasket"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Show Download Basket",false); //$NON-NLS-1$
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.addurlsilently"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Add URL Silently",false); //$NON-NLS-1$
    
    String osName = System.getProperty("os.name");
    if (osName.equals("Windows XP")) {
      label = new Label(cLook, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.style.enableXPStyle"); //$NON-NLS-1$
      final Button enableXPStyle = new Button(cLook, SWT.CHECK);
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

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.colorScheme"); //$NON-NLS-1$
    ColorParameter colorScheme = new ColorParameter(cLook, "Color Scheme",0,128,255,true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 50;
    colorScheme.setLayoutData(gridData);

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.guiUpdate"); //$NON-NLS-1$
    int[] values = { 100 , 250 , 500 , 1000 , 2000 , 5000 };
    String[] labels = { "100 ms" , "250 ms" , "500 ms" , "1 s" , "2 s" , "5 s" };
    new IntListParameter(cLook, "GUI Refresh", 250, labels, values);

    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.graphicsUpdate"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 15;
    IntParameter graphicUpdate = new IntParameter(cLook, "Graphics Update", 1, -1, false);
    graphicUpdate.setLayoutData(gridData);
    
    
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.reOrderDelay"); //$NON-NLS-1$
    gridData = new GridData();
    gridData.widthHint = 15;
    IntParameter reorderDelay = new IntParameter(cLook, "ReOrder Delay");
    reorderDelay.setLayoutData(gridData);
    
    if (osName.equals("Linux") && SWT.getPlatform().equals("gtk")) {
     label = new Label(cLook, SWT.NULL);
     Messages.setLanguageText(label, "ConfigView.section.style.verticaloffset"); //$NON-NLS-1$
     new IntParameter(cLook, VerticalAligner.parameterName,28); //$NON-NLS-1$
    }

    
    /**
     * Disabled for the moment because of some side effects
     */
    /*
    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysShowTorrentFiles"); //$NON-NLS-1$
    new BooleanParameter(cLook, "Always Show Torrent Files", true); //$NON-NLS-1$
    */

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.useSIUnits"); //$NON-NLS-1$
    new BooleanParameter(cLook, "config.style.useSIUnits",false); //$NON-NLS-1$

    label = new Label(cLook, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.style.alwaysRefreshMyTorrents"); //$NON-NLS-1$
    new BooleanParameter(cLook, "config.style.refreshMT",false); //$NON-NLS-1$
    
    return cLook;
  }
}
