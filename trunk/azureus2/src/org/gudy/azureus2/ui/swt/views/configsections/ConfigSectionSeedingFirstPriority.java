/*
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;

/** First Priority Specific options.
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionSeedingFirstPriority implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return "queue.seeding";
  }

  public String configSectionGetName() {
    return "queue.seeding.firstPriority";
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

    Composite cFirstPriorityArea = new Composite(parent, SWT.NULL);
    cFirstPriorityArea.addControlListener(new Utils.LabelWrapControlListener());

    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    cFirstPriorityArea.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cFirstPriorityArea.setLayoutData(gridData);


    label = new Label(cFirstPriorityArea, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.info");

    // ** Begin No Touch area

    cArea = new Composite(cFirstPriorityArea, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    cArea.setLayoutData(gridData);

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority");

    String fpLabels[] = { MessageText.getString("ConfigView.text.all"),
                               MessageText.getString("ConfigView.text.any") };
    int fpValues[] = { StartStopRulesDefaultPlugin.FIRSTPRIORITY_ALL, 
                       StartStopRulesDefaultPlugin.FIRSTPRIORITY_ANY };
    new IntListParameter(cArea, "StartStopManager_iFirstPriority_Type",
                         fpLabels, fpValues);

    label = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.following");

    // row
    label = new Label(cFirstPriorityArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.shareRatio");
    String minQueueLabels[] = new String[51];
    int minQueueValues[] = new int[51];
    minQueueLabels[0] = "1:2 (" + 0.5 + ")";
    minQueueValues[0] = 500;
    for (int i = 1; i < minQueueLabels.length; i++) {
      minQueueLabels[i] = i + ":1";
      minQueueValues[i] = i * 1000;
    }
    new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_ShareRatio",
                         minQueueLabels, minQueueValues);

    // row
    label = new Label(cFirstPriorityArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.seedingMinutes");

    String seedTimeLabels[] = new String[15];
    int seedTimeValues[] = new int[15];
    String sMinutes = MessageText.getString("ConfigView.text.minutes");
    String sHours = MessageText.getString("ConfigView.text.hours");
    seedTimeLabels[0] = MessageText.getString("ConfigView.text.ignore");
    seedTimeValues[0] = 0;
    seedTimeLabels[1] = "<= 90 " + sMinutes;
    seedTimeValues[1] = 90;
    for (int i = 2; i < seedTimeValues.length; i++) {
      seedTimeLabels[i] = "<= " + i + " " + sHours ;
      seedTimeValues[i] = i * 60;
    }
    new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_SeedingMinutes",
                         seedTimeLabels, seedTimeValues);

    // row
    label = new Label(cFirstPriorityArea, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.DLMinutes");

    String dlTimeLabels[] = new String[15];
    int dlTimeValues[] = new int[15];
    dlTimeLabels[0] = MessageText.getString("ConfigView.text.ignore");
    dlTimeValues[0] = 0;
    for (int i = 1; i < dlTimeValues.length; i++) {
      dlTimeLabels[i] = "<= " + (i + 2) + " " + sHours ;
      dlTimeValues[i] = (i + 2) * 60;
    }
    new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_DLMinutes",
                         dlTimeLabels, dlTimeValues);



    return cFirstPriorityArea;
  }
}
