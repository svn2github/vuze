/*
 * File    : ConfigPanelFileTorrents.java
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
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.torrent.TOTorrent;

public class ConfigSectionFileTorrents implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_FILES;
  }

	public String configSectionGetName() {
		return "torrents";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
    GridData gridData;
    GridLayout layout;
    Label label;

    // Sub-Section: File -> Torrent
    // ----------------------------
    Composite cTorrent = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTorrent.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cTorrent.setLayout(layout);
    
    // Save .Torrent files to..
    BooleanParameter saveTorrents = new BooleanParameter(cTorrent, "Save Torrent Files", true,
                                                         "ConfigView.label.savetorrents");

    Composite gSaveTorrents = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 25;
    gridData.horizontalSpan = 2;
    gSaveTorrents.setLayoutData(gridData);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 4;
    layout.numColumns = 3;
    gSaveTorrents.setLayout(layout);

    Label lSaveDir = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(lSaveDir, "ConfigView.label.savedirectory");

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter torrentPathParameter = new StringParameter(gSaveTorrents,
                                                                     "General_sDefaultTorrent_Directory");
    torrentPathParameter.setLayoutData(gridData);

    Button browse2 = new Button(gSaveTorrents, SWT.PUSH);
    browse2.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse2.getBackground());
    browse2.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse2.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(torrentPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaulttorrentpath"));
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gSaveTorrents, "Save Torrent Backup", false,
                        "ConfigView.label.savetorrentbackup").setLayoutData(gridData);

    Control[] controls = new Control[]{ gSaveTorrents };
    IAdditionalActionPerformer grayPathAndButton1 = new ChangeSelectionActionPerformer(controls);
    saveTorrents.setAdditionalActionPerformer(grayPathAndButton1);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(
    		cTorrent, 
			"Default Start Torrents Stopped", false,
    		"ConfigView.label.defaultstarttorrentsstopped").setLayoutData(gridData);


    // Watch Folder
    BooleanParameter watchFolder = new BooleanParameter(cTorrent, "Watch Torrent Folder", false,
                                                        "ConfigView.label.watchtorrentfolder");

    Composite gWatchFolder = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 25;
    gridData.horizontalSpan = 2;
    gWatchFolder.setLayoutData(gridData);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 4;
    layout.numColumns = 3;
    gWatchFolder.setLayout(layout);

    Label lImportDir = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lImportDir, "ConfigView.label.importdirectory");

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter watchFolderPathParameter = new StringParameter(gWatchFolder,
                                                                         "Watch Torrent Folder Path", "");
    watchFolderPathParameter.setLayoutData(gridData);

    Button browse4 = new Button(gWatchFolder, SWT.PUSH);
    browse4.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse4.getBackground());
    browse4.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse4.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(watchFolderPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosewatchtorrentfolderpath"));
        String path = dialog.open();
        if (path != null) {
          watchFolderPathParameter.setValue(path);
        }
      }
    });

    Label lWatchTorrentFolderInterval = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lWatchTorrentFolderInterval, "ConfigView.label.watchtorrentfolderinterval");
    final String watchTorrentFolderIntervalLabels[] = new String[5];
    final int watchTorrentFolderIntervalValues[] = new int[5];
    for (int i = 1; i < 6; i++) {
      watchTorrentFolderIntervalLabels[i - 1] = " " + i + " min";
      watchTorrentFolderIntervalValues[i - 1] = i;
    }
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new IntListParameter(gWatchFolder, "Watch Torrent Folder Interval", 1, 
                         watchTorrentFolderIntervalLabels, 
                         watchTorrentFolderIntervalValues).setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    new BooleanParameter(gWatchFolder, "Start Watched Torrents Stopped", true,
                         "ConfigView.label.startwatchedtorrentsstopped").setLayoutData(gridData);

    controls = new Control[]{ gWatchFolder };
    watchFolder.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));

    // locale decoder
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.decoder.label");
  
    LocaleUtilDecoder[] decoders = LocaleUtil.getSingleton().getDecoders();
  
    String decoderLabels[] = new String[decoders.length + 1];
    String decoderValues[] = new String[decoders.length + 1];
  
    decoderLabels[0] = MessageText.getString("ConfigView.section.file.decoder.nodecoder");
    decoderValues[0] = "";
  
    for (int i = 1; i <= decoders.length; i++) {
      decoderLabels[i] = decoderValues[i] = decoders[i-1].getName();
      }
    new StringListParameter(cTorrent, "File.Decoder.Default", "", 
                            decoderLabels, decoderValues);
  
      // locale always prompt
  
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cTorrent, "File.Decoder.Prompt", false,
                         "ConfigView.section.file.decoder.prompt").setLayoutData(gridData);
          
    
    	// show lax decodings
    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cTorrent, "File.Decoder.ShowLax", false,
                         "ConfigView.section.file.decoder.showlax").setLayoutData(gridData);

    	// show all decoders
    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(cTorrent, "File.Decoder.ShowAll", false,
                         "ConfigView.section.file.decoder.showall").setLayoutData(gridData);
   
    
    Label lIgnoreFiles = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(lIgnoreFiles, "ConfigView.section.file.torrent.ignorefiles");

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    new StringParameter(cTorrent, "File.Torrent.IgnoreFiles",
                        TOTorrent.DEFAULT_IGNORE_FILES).setLayoutData(gridData);

    return cTorrent;
  }
}
