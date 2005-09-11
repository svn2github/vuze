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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;

public class ConfigSectionLogging implements UISWTConfigSection {
  private static final int logFileSizes[] =
     {
       1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100, 200, 300, 500
     };

  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "logging";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
  	int[] components = { 0, 1, 2, 4 };
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    GridLayout layout;

    Composite gLogging = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gLogging.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gLogging.setLayout(layout);

    
    BooleanParameter enable_logger = new BooleanParameter(gLogging, "Logger.Enabled", "ConfigView.section.logging.loggerenable");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enable_logger.setLayoutData(gridData);

    // row

    final BooleanParameter enableLogging = 
      new BooleanParameter(gLogging, 
                           "Logging Enable", 
                           "ConfigView.section.logging.enable");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableLogging.setLayoutData(gridData);

    Composite cArea = new Composite(gLogging, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    cArea.setLayoutData(gridData);


    // row

    Label lStatsPath = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(lStatsPath, "ConfigView.section.logging.logdir"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter pathParameter = new StringParameter(cArea, "Logging Dir"); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);
    Button browse = new Button(cArea, SWT.PUSH);
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
        dialog.setText(MessageText.getString("ConfigView.section.logging.choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
        pathParameter.setValue(path);
        }
      }
    });

    Label lMaxLog = new Label(cArea, SWT.NULL);

    Messages.setLanguageText(lMaxLog, "ConfigView.section.logging.maxsize");
    final String lmLabels[] = new String[logFileSizes.length];
    final int lmValues[] = new int[logFileSizes.length];
    for (int i = 0; i < logFileSizes.length; i++) {
      int  num = logFileSizes[i];
      lmLabels[i] = " " + num + " MB";
      lmValues[i] = num;
    }

    IntListParameter paramMaxSize = new IntListParameter(cArea, "Logging Max Size", lmLabels, lmValues);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    paramMaxSize.setLayoutData(gridData);

    
    Composite cLogTypes = new Composite(gLogging, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    layout.makeColumnsEqualWidth = true;
    cLogTypes.setLayout(layout);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    cLogTypes.setLayoutData(gridData);

		for (int i = 0; i < components.length; i++) {
      Group gLogType = new Group(cLogTypes, SWT.NULL);
      layout = new GridLayout();
      layout.numColumns = 1;
      gLogType.setLayout(layout);
      gLogType.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
      Messages.setLanguageText(gLogType, "ConfigView.section.logging.log" + components[i] + "component");
      
      for (int j = 0; j <= 3; j++) {
        new BooleanParameter(gLogType, "bLog" + components[i] + "-" + j,
                             "ConfigView.section.logging.log" + j + "type");
      }
    }
    
    
    final Control[] controls_main = { cArea, cLogTypes };
    final ChangeSelectionActionPerformer perf2 = new ChangeSelectionActionPerformer( controls_main );
    
    enableLogging.setAdditionalActionPerformer( perf2 );
    
    enable_logger.setAdditionalActionPerformer(
        new IAdditionalActionPerformer() {
          ChangeSelectionActionPerformer p1 = new ChangeSelectionActionPerformer(new Control[] {enableLogging.getControl() } );

          public void performAction() {
            p1.performAction();
          }
          public void setSelected(boolean selected) {
            p1.setSelected( selected );
            if( !selected && enableLogging.isSelected() )  enableLogging.setSelected( false );
          }
          public void setIntValue(int value) { /*nothing*/ }
          public void setStringValue(String value) { /*nothing*/ }
        }
    );

		// diagnostics
	
	Label generate_info = new Label(gLogging, SWT.NULL);

	Messages.setLanguageText(generate_info, "ConfigView.section.logging.generatediagnostics.info");

	Button generate_button = new Button(gLogging, SWT.PUSH);

	Messages.setLanguageText(generate_button, "ConfigView.section.logging.generatediagnostics");

	generate_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(Event event) 
				{
					StringWriter sw = new StringWriter();
					
					PrintWriter	pw = new PrintWriter( sw );
					
					AEDiagnostics.generateEvidence( pw );
					
					pw.close();
					
					String	evidence = sw.toString();
					
					ClipboardCopy.copyToClipBoard( evidence );
					
					StringTokenizer	tok = new StringTokenizer(evidence, "\n" );
					
					while( tok.hasMoreTokens()){
						
						LGLogger.log( LGLogger.AT_COMMENT, tok.nextToken().trim());
					}
				}
			});
	
    return gLogging;
  }
}
