/*
 * Created on 4 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;

/**
 * @author Olivier Chalouhi
 *
 */
public class GUIUpdater extends AEThread implements ParameterListener {
	private static final LogIDs LOGID = LogIDs.GUI;

	/** Calculate timer statistics for GUI update */
	private static final boolean DEBUG_TIMER = Constants.isCVSVersion();

  private AzureusCore		azureus_core;
  private ConnectionManager	connection_manager;
  private OverallStats		overall_stats;
  private DHTPlugin     	dhtPlugin;
  private NumberFormat  	numberFormat;
  private MainWindow 		mainWindow;
  private Display 			display;
  
  private long last_sr_ratio	= -1;
  private int  last_sr_status	= -1;
  private int lastNATstatus = -1;
  private int lastDHTstatus = -1;
  private long lastDHTcount = -1;
  
  boolean finished = false;
  boolean refreshed = true;
  
  int waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
  
  Map averageTimes = DEBUG_TIMER ? new HashMap() : null;
  
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
    
	overall_stats = StatsFactory.getStats();
		

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
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
			    long lTimeStart = System.currentTimeMillis();
			    Map timeMap = DEBUG_TIMER ? new LinkedHashMap() : null;

					if (display == null || display.isDisposed())
						return;

					IView view = null;
					if (!mainWindow.getShell().isDisposed() && mainWindow.isVisible()
							&& !mainWindow.getShell().getMinimized()) {

						view = mainWindow.getCurrentView();

						if (DEBUG_TIMER)
							timeMap.put("Init", new Long(System.currentTimeMillis()));
						if (view != null) {
							view.refresh();
							if (DEBUG_TIMER) {
								String s = view.getFullTitle().replaceAll("[0-9.]++\\% : ", "");
								timeMap.put("'" + s + "' Refresh", new Long(System.currentTimeMillis()));
							}
							Tab.refresh();
							if (DEBUG_TIMER)
								timeMap.put("Tab Refresh", new Long(System.currentTimeMillis()));
						}

						// IP Filter Status Section
						IpFilter ip_filter = azureus_core.getIpFilterManager()
								.getIPFilter();

						mainWindow.ipBlocked.setText("{"
								+ DisplayFormatters.formatDateShort(ip_filter
										.getLastUpdateTime())
								+ "} IPs: "
								+ numberFormat.format(ip_filter.getNbRanges())
								+ " - "
								+ numberFormat.format(ip_filter.getNbIpsBlockedAndLoggable())
								+ "/"
								+ numberFormat.format(ip_filter.getNbBannedIps())
								+ "/"
								+ numberFormat.format(azureus_core.getIpFilterManager()
										.getBadIps().getNbBadIps()));

						// SR status section
						
			    	    long ratio = (1000* overall_stats.getUploadedBytes() / (overall_stats.getDownloadedBytes()+1) );

			    	    int	sr_status;
			    	    
			    	    if ( ratio < 500 ){
			    	    	
			    	    	sr_status = 0;
			    	    	
			    	    }else if ( ratio < 900 ){
			    	    	
			    	    	sr_status = 1;
			    	    	
			    	    }else{
			    	    	
			    	    	sr_status = 2;
			    	    }
						
			    	    if ( sr_status != last_sr_status ){
			    	    	
							String imgID;
							
							switch (sr_status) {
									case 2:
									imgID = "greenled";
									break;

								case 1:
									imgID = "yellowled";
									break;

								default:
									imgID = "redled";
									break;
							}

							mainWindow.srStatus.setImage( ImageRepository.getImage(imgID) );
							
			    	    	last_sr_status	= sr_status;
			    	    }
			    	    
			    	    if ( ratio != last_sr_ratio ){
			    	    	
							String tooltipID;
							
							switch (sr_status) {
									case 2:
									tooltipID = "MainWindow.sr.status.tooltip.ok";
									break;

								case 1:
									tooltipID = "MainWindow.sr.status.tooltip.poor";
									break;

								default:
								  	tooltipID = "MainWindow.sr.status.tooltip.bad";
									break;
							}

							String ratio_str = "";
							
						    String partial = "" + ratio%1000;
						    
						    while(partial.length() < 3){
						    	
						    	partial = "0" + partial;
						    }
						    
						    ratio_str = (ratio/1000) + "." + partial;

							mainWindow.srStatus.setToolTipText( MessageText.getString(tooltipID,new String[]{ratio_str}));
				    	   
							last_sr_ratio	= ratio;
			    	    }
		    	    
						// NAT status Section
						
						int nat_status = connection_manager.getNATStatus();

						if (lastNATstatus != nat_status) {
							String imgID;
							String tooltipID;
							String statusID;
							
							switch (nat_status) {
								case ConnectionManager.NAT_UNKNOWN:
									imgID = "grayled";
									tooltipID = "MainWindow.nat.status.tooltip.unknown";
									statusID = "MainWindow.nat.status.unknown";
									break;

								case ConnectionManager.NAT_OK:
									imgID = "greenled";
									tooltipID = "MainWindow.nat.status.tooltip.ok";
									statusID = "MainWindow.nat.status.ok";
									break;

								case ConnectionManager.NAT_PROBABLY_OK:
									imgID = "yellowled";
									tooltipID = "MainWindow.nat.status.tooltip.probok";
									statusID = "MainWindow.nat.status.probok";
									break;

								default:
									imgID = "redled";
								  tooltipID = "MainWindow.nat.status.tooltip.bad";
								  statusID = "MainWindow.nat.status.bad";
									break;
							}

							mainWindow.natStatus.setImage( ImageRepository.getImage(imgID) );
							mainWindow.natStatus.setToolTipText( MessageText.getString(tooltipID) );
							mainWindow.natStatus.setText( MessageText.getString(statusID) );
							lastNATstatus = nat_status;
						}

						// DHT Status Section
						int dht_status = (dhtPlugin == null) ? DHTPlugin.STATUS_DISABLED	: dhtPlugin.getStatus();
						long dht_count = -1;
						if (dht_status == DHTPlugin.STATUS_RUNNING) {
							DHT[] dhts = dhtPlugin.getDHTs();
							
							if( dhts.length > 0	&& dhts[0].getTransport().isReachable() ) {
								dht_count = dhts[0].getControl().getStats().getEstimatedDHTSize();
							}
						}

						if (lastDHTstatus != dht_status || lastDHTcount != dht_count) {
							switch (dht_status) {
								case DHTPlugin.STATUS_RUNNING:
									if (dht_count > 100*1000 ) {  //release dht has at least a half million users
										mainWindow.dhtStatus.setImage(ImageRepository.getImage( "greenled" ));
										mainWindow.dhtStatus.setToolTipText(MessageText.getString( "MainWindow.dht.status.tooltip" ));
										mainWindow.dhtStatus.setText(numberFormat.format(dht_count)+ " " +MessageText.getString("MainWindow.dht.status.users"));
									}
									else {
										mainWindow.dhtStatus.setImage(ImageRepository.getImage( "yellowled" ));
										mainWindow.dhtStatus.setToolTipText(MessageText.getString( "MainWindow.dht.status.unreachabletooltip" ));
										mainWindow.dhtStatus.setText( MessageText.getString( "MainWindow.dht.status.unreachable" ));
									}
									break;

								case DHTPlugin.STATUS_DISABLED:
									mainWindow.dhtStatus.setImage(ImageRepository.getImage( "grayled" ));
									mainWindow.dhtStatus.setText( MessageText.getString( "MainWindow.dht.status.disabled" ));
									break;

								case DHTPlugin.STATUS_INITALISING:
									mainWindow.dhtStatus.setImage(ImageRepository.getImage( "yellowled" ));
									mainWindow.dhtStatus.setText( MessageText.getString( "MainWindow.dht.status.initializing" ));
									break;

								case DHTPlugin.STATUS_FAILED:
									mainWindow.dhtStatus.setImage(ImageRepository.getImage( "redled" ));
									mainWindow.dhtStatus.setText( MessageText.getString( "MainWindow.dht.status.failed" ));
									break;
									
								default:
									mainWindow.dhtStatus.setImage(null);
								 break;
							}

							lastDHTstatus = dht_status;
							lastDHTcount = dht_count;
						}

						// UL/DL Status Sections
						int ul_limit_norm = NetworkManager.getMaxUploadRateBPSNormal() / 1024;
						int dl_limit = NetworkManager.getMaxDownloadRateBPS() / 1024;

						String seeding_only;
						if (NetworkManager.isSeedingOnlyUploadRate()) {
							int ul_limit_seed = NetworkManager
									.getMaxUploadRateBPSSeedingOnly() / 1024;
							if (ul_limit_seed == 0) {
								seeding_only = "+" + Constants.INFINITY_STRING + "K";
							} else {
								int diff = ul_limit_seed - ul_limit_norm;
								seeding_only = (diff >= 0 ? "+" : "") + diff + "K";
							}
						} else {
							seeding_only = "";
						}

						mainWindow.statusDown.setText(
								(dl_limit == 0 ? "" : "[" + dl_limit + "K] ")
								+ DisplayFormatters
										.formatByteCountToKiBEtcPerSec(mainWindow.globalManager
												.getStats().getDataReceiveRate()
												+ mainWindow.globalManager.getStats()
														.getProtocolReceiveRate()));

						mainWindow.statusUp.setText(
								(ul_limit_norm == 0 ? "" : "[" + ul_limit_norm + "K"
										+ seeding_only + "] ")
								+ DisplayFormatters
										.formatByteCountToKiBEtcPerSec(mainWindow.globalManager
												.getStats().getDataSendRate()
												+ mainWindow.globalManager.getStats()
														.getProtocolSendRate()));

						// End of Status Sections
						mainWindow.statusBar.layout();
					}

					if (DEBUG_TIMER)
						timeMap.put("Status Bar", new Long(System.currentTimeMillis()));
					
					if (mainWindow.systemTraySWT != null)
						mainWindow.systemTraySWT.update();

					if (DEBUG_TIMER)
						timeMap.put("SysTray", new Long(System.currentTimeMillis()));

					try {
						mainWindow.downloadBars_mon.enter();

						Iterator iter = mainWindow.downloadBars.values().iterator();
						while (iter.hasNext()) {
							MinimizedWindow mw = (MinimizedWindow) iter.next();
							mw.refresh();
						}
					} finally {

						mainWindow.downloadBars_mon.exit();
					}

					if (DEBUG_TIMER) {
						timeMap.put("DLBars", new Long(System.currentTimeMillis()));

						makeDebugToolTip(lTimeStart, timeMap);
					}

				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Error while trying to update GUI", e));
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
  
  private void makeDebugToolTip(long lTimeStart, Map timeMap) {
		final int IDX_AVG = 0;
		final int IDX_SIZE = 1;
		final int IDX_MAX = 2;
		final int IDX_LAST = 3;
		final int IDX_TIME = 4;

		long lastTime = lTimeStart;
		for (Iterator iter = timeMap.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();

			if (!averageTimes.containsKey(key))
				averageTimes.put(key, new Object[] { new Long(0), new Long(0),
						new Long(0), new Long(0), new Long(System.currentTimeMillis()) });

			Object[] average = (Object[]) averageTimes.get(key);

			long l = ((Long) timeMap.get(key)).longValue();
			long diff = l - lastTime;
			if (diff > 0) {
				long count = ((Long) average[IDX_SIZE]).longValue();
				// Limit to 20.  Gives slightly scewed averages, but doesn't
				// require storing all 20 values and averaging them each time
				if (count >= 20)
					count = 19;
				long lNewAverage = ((((Long) average[IDX_AVG]).longValue() * count) + diff)
						/ (count + 1);
				average[IDX_AVG] = new Long(lNewAverage);
				average[IDX_SIZE] = new Long(count + 1);
				if (diff > ((Long) average[IDX_MAX]).longValue())
					average[IDX_MAX] = new Long(diff);
				average[IDX_LAST] = new Long(diff);
				average[IDX_TIME] = new Long(System.currentTimeMillis());
			} else {
				average[IDX_LAST] = new Long(diff);
			}
			averageTimes.put(key, average);
			lastTime = l;
		}

		StringBuffer sb = new StringBuffer();
		for (Iterator iter = averageTimes.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			Object[] average = (Object[]) averageTimes.get(key);

			long lLastUpdated = ((Long) average[IDX_TIME]).longValue();
			if (System.currentTimeMillis() - lLastUpdated > 10000) {
				iter.remove();
				continue;
			}

			long lTime = ((Long) average[IDX_AVG]).longValue();
			if (lTime > 0) {
				if (sb.length() > 0)
					sb.append("\n");
				sb.append(average[IDX_AVG] + "ms avg: ");
				sb.append("[" + key + "]");
				sb.append(average[IDX_SIZE] + " samples");
				sb.append("; max:" + average[IDX_MAX]);
				sb.append("; last:" + average[IDX_LAST]);
			}
		}
		if (!mainWindow.getShell().isDisposed())
			mainWindow.statusText.setToolTipText(sb.toString());
	}
}
