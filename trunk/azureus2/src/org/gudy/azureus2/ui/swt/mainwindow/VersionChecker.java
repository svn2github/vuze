/*
 * Created on 3 mai 2004
 * Created by Olivier Chalouhi
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
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;


class 
VersionChecker 
	extends Thread 
{	
  
  private static final int RECOMMENDED_SWT_VERSION = 3044; // (M8 = 3044) (M7 = 3038)
  public static final long AUTO_UPDATE_CHECK_PERIOD = 23*60*60*1000;  // 23 hours
  
  private static Timer version_check_timer;
  
  public static synchronized void
  checkForNewVersion()
  {
    if ( version_check_timer == null ){
      
      version_check_timer = new Timer("Auto-update timer",1);

      version_check_timer.addPeriodicEvent( 
        AUTO_UPDATE_CHECK_PERIOD,
        new TimerEventPerformer()
        {
          public void
          perform(
            TimerEvent  ev )
          {
            checkForNewVersion();
          }
        }
       );
    }
  
    new VersionChecker().start();
  }
  
  private String latestVersion;
  private String latestVersionFileName;
  
  public VersionChecker() {
		super("Version Checker");
		setDaemon(true);
	}
  
  
	public void run() {
    MainWindow mainWindow = MainWindow.getWindow();
    mainWindow.setStatusText("MainWindow.status.checking ...");
		ByteArrayOutputStream message = new ByteArrayOutputStream(); //$NON-NLS-1$

		int nbRead = 0;
		HttpURLConnection con = null;
		InputStream is = null;
		try {
			String id = COConfigurationManager.getStringParameter("ID",null);        
			String url = "http://azureus.sourceforge.net/version.php";
			if(id != null && COConfigurationManager.getBooleanParameter("Send Version Info")) {
				url += "?id=" + id + "&version=" + Constants.AZUREUS_VERSION;
			}
			URL reqUrl = new URL(url); //$NON-NLS-1$
			con = (HttpURLConnection) reqUrl.openConnection();
			con.connect();
			is = con.getInputStream();
			//        int length = con.getContentLength();
			//        System.out.println(length);
			byte[] data = new byte[1024];
			while (nbRead >= 0) {
				nbRead = is.read(data);
				if (nbRead >= 0) {
					message.write(data, 0, nbRead);
				}
			}
			Map decoded = BDecoder.decode(message.toByteArray());
			latestVersion = new String((byte[]) decoded.get("version")); //$NON-NLS-1$
			byte[] bFileName = (byte[]) decoded.get("filename"); //$NON-NLS-1$
			if (bFileName != null)
				latestVersionFileName = new String(bFileName);
      
      String sText = " . ";
      int iSWTVer = SWT.getVersion();
      if (iSWTVer < RECOMMENDED_SWT_VERSION) {
        sText += "SWT v"+ iSWTVer + " MainWindow.status.tooOld";
      }
      mainWindow.setStatusText(Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + " / MainWindow.status.latestversion " + latestVersion + sText);
/*
      final Display display = SWTThread.getInstance().getDisplay();
      
			if (display == null || display.isDisposed())
				return;
			display.asyncExec(new Runnable() {
				public void run() {
					if (mainWindow.this.mainWindow.statusText.isDisposed())
						return;
					if (!SystemProperties.isJavaWebStartInstance() &&  MainWindow.VERSION.compareTo(mainWindow.this.mainWindow.latestVersion) < 0) {
						mainWindow.this.mainWindow.latestVersion += " (" + MessageText.getString("MainWindow.status.latestversion.clickupdate") + ")";
						mainWindow.this.mainWindow.setStatusVersion();
						mainWindow.this.mainWindow.statusText.setForeground(Colors.red);
						mainWindow.this.mainWindow.statusText.setCursor(Cursors.handCursor);
						mainWindow.this.mainWindow.statusText.addMouseListener(new MouseAdapter() {
							public void mouseDoubleClick(MouseEvent arg0) {
								UpdateWindow.show(mainWindow.this.mainWindow.latestVersion);
							}
							public void mouseUp(MouseEvent arg0) {
                UpdateWindow.show(mainWindow.this.mainWindow.latestVersion);
							}
						});
						if (COConfigurationManager.getBooleanParameter("Auto Update", true)) {
              UpdateWindow.show(mainWindow.this.mainWindow.latestVersion);
						}
					}
					else {
						mainWindow.this.mainWindow.setStatusVersion();
					}
				}
			});*/
		}
		catch (Exception e) {
      mainWindow.setStatusText("MainWindow.status.unknown");			
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e1) {}
				is = null;
			}
			if (con != null) {
				con.disconnect();
				con = null;
			}
		}
	}
}