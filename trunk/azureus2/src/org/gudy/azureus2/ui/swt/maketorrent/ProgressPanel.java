/*
 * File    : ProgressPanel.java
 * Created : 7 oct. 2003 13:01:42
 * By      : Olivier 
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

package org.gudy.azureus2.ui.swt.maketorrent;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.ui.swt.wizard.*;

/**
 * @author Olivier
 * 
 */
public class ProgressPanel extends AbstractWizardPanel implements TOTorrentProgressListener {

  Text tasks;
  ProgressBar progress;
  Display display;

  public ProgressPanel(NewTorrentWizard wizard, IWizardPanel previousPanel) {
    super(wizard, previousPanel);
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#show()
   */
  public void show() {
    display = wizard.getDisplay();
    wizard.setTitle(MessageText.getString("wizard.progresstitle"));
    wizard.setCurrentInfo("");
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 1;
    panel.setLayout(layout);

    tasks = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
    tasks.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = 120;
    tasks.setLayoutData(gridData);

    progress = new ProgressBar(panel, SWT.NULL);
    progress.setMinimum(0);
    progress.setMaximum(0);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    progress.setLayoutData(gridData);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#finish()
   */
  public void finish() {
    Thread t = new Thread("Torrent Maker") {
      public void run() {
        makeTorrent();
      }
    };
    t.setPriority(Thread.MIN_PRIORITY);
    t.setDaemon(true);
    t.start();
  }

  public void makeTorrent() {
    if(!((NewTorrentWizard)wizard).localTracker)
      TrackersUtil.getInstance().addTracker(((NewTorrentWizard)wizard).trackerURL);
    File f;
    if (((NewTorrentWizard)wizard).mode) {
      f = new File(((NewTorrentWizard)wizard).directoryPath);
    }
    else {
      f = new File(((NewTorrentWizard)wizard).singlePath);
    }

    try {
       URL url = new URL(((NewTorrentWizard)wizard).trackerURL);
      TOTorrent torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(f, url, this);
      this.reportCurrentTask(MessageText.getString("wizard.savingfile"));
      torrent.setComment(((NewTorrentWizard)wizard).getComment());
      torrent.serialiseToBEncodedFile(new File(((NewTorrentWizard)wizard).savePath));
      this.reportCurrentTask(MessageText.getString("wizard.filesaved"));
	  wizard.switchToClose();
	}
    catch (Exception e) {
      e.printStackTrace();
      reportCurrentTask(MessageText.getString("wizard.operationfailed"));
      reportCurrentTask(LGLogger.exceptionToString(e));
	  wizard.switchToClose();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core3.torrent.TOTorrentProgressListener#reportCurrentTask(java.lang.String)
   */
  public void reportCurrentTask(final String task_description) {
    if (display != null && !display.isDisposed()) {
      display.asyncExec(new Runnable() {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
          if (tasks != null && !tasks.isDisposed()) {
            tasks.append(task_description + Text.DELIMITER);
          }
        }
      });
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core3.torrent.TOTorrentProgressListener#reportProgress(int)
   */
  public void reportProgress(final int percent_complete) {
    if (display != null && !display.isDisposed()) {
      display.asyncExec(new Runnable() {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
          if (progress != null && !progress.isDisposed()) {
            progress.setSelection(percent_complete);
          }

        }
      });
    }
  }

}
