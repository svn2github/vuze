/*
 * File    : Main.java
 * Created : 1 déc. 2003}
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
package org.gudy.azureus2.ui.swing;

import java.awt.Container;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;

/**
 * @author Olivier
 *
 */
public class Main extends JFrame implements TorrentDownloaderCallBackInterface {

  String torrentUrl;
  String tempDir;
  
  JLabel lImage;
  JLabel lStatus;
  JLabel lFile;
  JProgressBar progressBar;
  JLabel etaTitle;
  JLabel etaValue;
  JLabel downloadToTitle;
  JLabel downloadToValue;
  JLabel speedTitle;
  JLabel speedValue;
  JButton bOpen;
  JButton bOpenDir;
  JButton bCancel;
  
  public Main() {
    this.torrentUrl = System.getProperty("azureus.torrenturl");
    this.tempDir = System.getProperty("user.dir") + System.getProperty("file.separator") + ".azureus" + System.getProperty("file.separator");
    GridLayout layout = new GridLayout(8,4);
    this.setLayout(layout);
    lImage = new JLabel();
    lStatus = new JLabel();
    lFile = new JLabel();
    progressBar = new JProgressBar();
    etaTitle = new JLabel("Estimated time left :");
    etaValue = new JLabel("Computing...");
    downloadToTitle = new JLabel("Download to :");
    downloadToValue = new JLabel();
    speedTitle = new JLabel("Transfer rate :");
    speedValue = new JLabel("Computing...");
    bOpen = new JButton("Open");
    bOpenDir = new JButton("Open Folder");
    bCancel = new JButton("Cancel");
    Container container = this.getContentPane();
  }
  
  
  
  public static void main(String args[]) {
    if(args.length == 1) {
      System.setProperty("azureus.torrenturl",args[0]);
      new Main();
    }    
  }
  
  public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
    
  }

}
