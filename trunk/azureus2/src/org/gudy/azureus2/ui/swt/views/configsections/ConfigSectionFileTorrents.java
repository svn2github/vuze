/*
 * File    : ConfigPanelFileTorrents.java
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
    
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.savetorrents"); //$NON-NLS-1$
    BooleanParameter saveTorrents = new BooleanParameter(cTorrent, "Save Torrent Files", true); //$NON-NLS-1$

    Composite gSaveTorrents = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    gridData.horizontalSpan = 2;
    gSaveTorrents.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gSaveTorrents.setLayout(layout);

    label = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.savedirectory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter torrentPathParameter = new StringParameter(gSaveTorrents, "General_sDefaultTorrent_Directory", ""); //$NON-NLS-1$ //$NON-NLS-2$
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
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaulttorrentpath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });

    Label lSaveTorrentBackup = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(lSaveTorrentBackup, "ConfigView.label.savetorrentbackup"); //$NON-NLS-1$
    BooleanParameter saveTorrentBackup = new BooleanParameter(gSaveTorrents, "Save Torrent Backup", false); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    saveTorrentBackup.setLayoutData(gridData);

    Control[] controls = new Control[4];
    controls[0] = torrentPathParameter.getControl();
    controls[1] = browse2;
    controls[2] = lSaveTorrentBackup;
    controls[3] = saveTorrentBackup.getControl();
    IAdditionalActionPerformer grayPathAndButton1 = new ChangeSelectionActionPerformer(controls);
    saveTorrents.setAdditionalActionPerformer(grayPathAndButton1);


    // Watch Folder
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.watchtorrentfolder"); //$NON-NLS-1$
    BooleanParameter watchFolder = new BooleanParameter(cTorrent, "Watch Torrent Folder", false); //$NON-NLS-1$

    Composite gWatchFolder = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 10;
    gridData.horizontalSpan = 2;
    gWatchFolder.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gWatchFolder.setLayout(layout);

    label = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.label.importdirectory"); //$NON-NLS-1$

    gridData = new GridData(GridData.FILL_HORIZONTAL);
//    gridData = new GridData();
//    gridData.widthHint = 220;
    final StringParameter watchFolderPathParameter = new StringParameter(gWatchFolder, "Watch Torrent Folder Path", "");
    watchFolderPathParameter.setLayoutData(gridData);

    Button browse4 = new Button(gWatchFolder, SWT.PUSH);
    browse4.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse4.getBackground());
    browse4.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse4.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(watchFolderPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosewatchtorrentfolderpath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
          watchFolderPathParameter.setValue(path);
        }
      }
    });

    Label lWatchTorrentFolderInterval = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lWatchTorrentFolderInterval, "ConfigView.label.watchtorrentfolderinterval"); //$NON-NLS-1$
    final String watchTorrentFolderIntervalLabels[] = new String[5];
    final int watchTorrentFolderIntervalValues[] = new int[5];
    for (int i = 1; i < 6; i++) {
      watchTorrentFolderIntervalLabels[i - 1] = " " + i + " min"; //$NON-NLS-1$ //$NON-NLS-2$
      watchTorrentFolderIntervalValues[i - 1] = i;
    }
    IntListParameter iWatchTorrentFolderIntervalParameter = new IntListParameter(gWatchFolder, "Watch Torrent Folder Interval", 1, watchTorrentFolderIntervalLabels, watchTorrentFolderIntervalValues);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    iWatchTorrentFolderIntervalParameter.setLayoutData(gridData);

    Label lStartWatchedTorrentsStopped = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lStartWatchedTorrentsStopped, "ConfigView.label.startwatchedtorrentsstopped"); //$NON-NLS-1$
    BooleanParameter startWatchedTorrentsStopped = new BooleanParameter(gWatchFolder, "Start Watched Torrents Stopped", true); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    startWatchedTorrentsStopped.setLayoutData(gridData);
    controls = new Control[6];
    controls[0] = watchFolderPathParameter.getControl();
    controls[1] = browse4;
    controls[2] = lWatchTorrentFolderInterval;
    controls[3] = iWatchTorrentFolderIntervalParameter.getControl();
    controls[4] = lStartWatchedTorrentsStopped;
    controls[5] = startWatchedTorrentsStopped.getControl();
    IAdditionalActionPerformer grayPathAndButton3 = new ChangeSelectionActionPerformer(controls);
    watchFolder.setAdditionalActionPerformer(grayPathAndButton3);

    // locale decoder
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.decoder.label"); //$NON-NLS-1$
  
    LocaleUtilDecoder[] decoders = LocaleUtil.getDecoders();
  
    String decoderLabels[] = new String[decoders.length + 1];
    String decoderValues[] = new String[decoders.length + 1];
  
    decoderLabels[0] = MessageText.getString( "ConfigView.section.file.decoder.nodecoder");
    decoderValues[0] = "";
  
    for (int i = 1; i <= decoders.length; i++) {
      decoderLabels[i] = decoderValues[i] = decoders[i-1].getName();
      }
    Control[] decoder_controls = new Control[2];
    decoder_controls[0] = label;
    decoder_controls[1] = new StringListParameter(cTorrent, "File.Decoder.Default", "", decoderLabels, decoderValues).getControl(); //$NON-NLS-1$
  
      // locale always prompt
  
    label = new Label(cTorrent, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.file.decoder.prompt");
    new BooleanParameter(cTorrent, "File.Decoder.Prompt", false);
          
    return cTorrent;
  }
}
