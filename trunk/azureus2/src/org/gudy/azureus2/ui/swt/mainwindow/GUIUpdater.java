/*
 * Created on 4 mai 2004
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

import java.text.NumberFormat;
import java.util.Iterator;

import org.eclipse.swt.widgets.Display;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.views.IView;

/**
 * @author Olivier Chalouhi
 *
 */
public class GUIUpdater extends AEThread implements ParameterListener {
  
  private AzureusCore		azureus_core;
  private ConnectionManager	connection_manager;
  
  private DHTPlugin     	dhtPlugin;
  private NumberFormat  	numberFormat;
  private MainWindow 		mainWindow;
  private Display 			display;
  
  boolean finished = false;
  boolean refreshed = true;
  
  int waitTime = COConfigurationManager.getIntParameter("GUI Refresh");   
  
  public 
  GUIUpdater(
  	AzureusCore		_azureus_core,
	MainWindow 		mainWindow) 
  {       
    super("GUI updater");
    azureus_core		= _azureus_core;
    this.mainWindow = mainWindow;
    this.display = mainWindow.getDisplay();
    this.numberFormat = NumberFormat.getInstance();
    
    PluginManager	pm = AzureusCoreFactory.getSingleton().getPluginManager();
    
    connection_manager = pm.getDefaultPluginInterface().getConnectionManager();
    
    PluginInterface dht_pi = pm.getPluginInterfaceByClass( DHTPlugin.class );
    if ( dht_pi != null ){
    	dhtPlugin = (DHTPlugin)dht_pi.getPlugin();
    }
    setPriority(Thread.MAX_PRIORITY -2);
    COConfigurationManager.addParameterListener("GUI Refresh", this);
  }

  public void runSupport() {
    while (!finished) {
      if(refreshed)
        update();
      try {
        Thread.sleep(waitTime);
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
  }

  private void update() {
    refreshed = false;
    if (display != null && !display.isDisposed())
      display.asyncExec(new AERunnable(){
      public void runSupport() {
        try {
          IView view = null;
          if (!mainWindow.getShell().isDisposed() && mainWindow.isVisible() && !mainWindow.getShell().getMinimized()) {

            view = mainWindow.getCurrentView();
            
            if (view != null) {
              view.refresh();
              Tab.refresh();
            }

            IpFilter ip_filter = azureus_core.getIpFilterManager().getIPFilter();
            
            mainWindow.ipBlocked.setText( 
            		"{"+
					DisplayFormatters.formatDateShort(ip_filter.getLastUpdateTime()) + 
					"} IPs: " + 
          numberFormat.format(ip_filter.getNbRanges()) + 
					" - " + 
          numberFormat.format(ip_filter.getNbIpsBlockedAndLoggable()) + 
					"/" +
          numberFormat.format(ip_filter.getNbBannedIps()) +
					"/" + 
          numberFormat.format(azureus_core.getIpFilterManager().getBadIps().getNbBadIps()));
					
        int	nat_status = connection_manager.getNATStatus();
        
        if ( nat_status == ConnectionManager.NAT_UNKNOWN ){
            mainWindow.natStatus.setImage(null);
  			mainWindow.natStatus.setToolTipText(null);
        }else if ( nat_status == ConnectionManager.NAT_OK ){
            mainWindow.natStatus.setImage(ImageRepository.getImage("greenled"));
    		mainWindow.natStatus.setToolTipText(MessageText.getString("MainWindow.nat.status.tooltip.ok"));
        }else if ( nat_status == ConnectionManager.NAT_PROBABLY_OK ){
            mainWindow.natStatus.setImage(ImageRepository.getImage("yellowled"));
    		mainWindow.natStatus.setToolTipText(MessageText.getString("MainWindow.nat.status.tooltip.probok"));
        }else{
        	mainWindow.natStatus.setImage(ImageRepository.getImage("redled"));
    		mainWindow.natStatus.setToolTipText(MessageText.getString("MainWindow.nat.status.tooltip.bad"));

        }
               
        if( dhtPlugin == null || dhtPlugin.getStatus() == DHTPlugin.STATUS_DISABLED) {
          mainWindow.dhtStatus.setImage(ImageRepository.getImage("redled"));
          mainWindow.dhtStatus.setText(MessageText.getString("MainWindow.dht.status.disabled"));
        } else
        if(dhtPlugin.getStatus() == DHTPlugin.STATUS_INITALISING) {
          mainWindow.dhtStatus.setImage(ImageRepository.getImage("yellowled"));
          mainWindow.dhtStatus.setText(MessageText.getString("MainWindow.dht.status.initializing"));
        } else
        if(dhtPlugin.getStatus() == DHTPlugin.STATUS_FAILED) {
          mainWindow.dhtStatus.setImage(ImageRepository.getImage("redled"));
          mainWindow.dhtStatus.setText(MessageText.getString("MainWindow.dht.status.failed"));
        } else
        if(dhtPlugin.getStatus() == DHTPlugin.STATUS_RUNNING) {
        	
  		  DHT[]	dhts = dhtPlugin.getDHTs();

  		  if ( dhts.length == 0 || dhts[0].getTransport().isReachable()){
  			  
  			mainWindow.dhtStatus.setImage(ImageRepository.getImage("greenled"));
  			mainWindow.dhtStatus.setToolTipText(MessageText.getString("MainWindow.dht.status.tooltip"));

  		  }else{
			mainWindow.dhtStatus.setImage(ImageRepository.getImage("yellowled"));		
    		mainWindow.dhtStatus.setToolTipText(MessageText.getString("MainWindow.dht.status.unreachabletooltip"));
  		  }
		  
          if(dhts.length == 0 || dhts[0].getControl().getStats().getEstimatedDHTSize() == 0 ) {
            mainWindow.dhtStatus.setText(MessageText.getString("MainWindow.dht.status.running"));
          } else {
            mainWindow.dhtStatus.setText(numberFormat.format(dhts[0].getControl().getStats().getEstimatedDHTSize()) + " " + MessageText.getString("MainWindow.dht.status.users"));
          }            
        }
        
        int ul_limit_norm = NetworkManager.getMaxUploadRateBPSNormal() / 1024;
        int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;
        
        String seeding_only;
        if( NetworkManager.isSeedingOnlyUploadRate() ) {
          int ul_limit_seed = NetworkManager.getMaxUploadRateBPSSeedingOnly() / 1024;
          if( ul_limit_seed == 0 ) {
            seeding_only = "+" +Constants.INFINITY_STRING+ "K";
          }
          else {
            int diff = ul_limit_seed - ul_limit_norm;
            seeding_only = (diff >= 0 ? "+" : "") +diff+ "K";
          }
        }
        else {
          seeding_only = "";
        }
        
        mainWindow.statusDown.setText(
            MessageText.getString("ConfigView.download.abbreviated") + " " + 
            (dl_limit==0?"":"[" + dl_limit + "K] " ) +
            DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getDataReceiveRate() + mainWindow.globalManager.getStats().getProtocolReceiveRate() ));
        
        mainWindow.statusUp.setText(
            MessageText.getString("ConfigView.upload.abbreviated") + " " + 
            (ul_limit_norm==0?"":"[" + ul_limit_norm + "K" +seeding_only+ "] " ) +
            DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getDataSendRate() + mainWindow.globalManager.getStats().getProtocolSendRate() ));
          }
          
          if(mainWindow.systemTraySWT != null)
            mainWindow.systemTraySWT.update();
          
          try{
          	mainWindow.downloadBars_mon.enter();
          
            Iterator iter = mainWindow.downloadBars.values().iterator();
            while (iter.hasNext()) {
              MinimizedWindow mw = (MinimizedWindow) iter.next();
              mw.refresh();
            }
          }finally{
          	
          	mainWindow.downloadBars_mon.exit();
          }
        } catch (Exception e) {
          LGLogger.log(LGLogger.ERROR, "Error while trying to update GUI");
          Debug.printStackTrace( e );
        } finally {
          refreshed = true;
        }
      }        
    });
  }

  public void stopIt() {
    finished = true;
    COConfigurationManager.removeParameterListener("GUI Refresh", this);
    COConfigurationManager.removeParameterListener("config.style.refreshMT", this);
  }
}
