/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views.stats;


import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.components.graphics.PingGraphic;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;

/**
 * 
 */
public class TransferStatsView extends AbstractIView {

  GlobalManager manager;
  AzureusCore core;
  GlobalManagerStats stats;
  
  OverallStats totalStats;
  
  Composite mainPanel;  
  
  Composite generalPanel;
  BufferedLabel nowUp, nowDown, sessionDown, sessionUp, session_ratio, sessionTime, totalDown, totalUp, total_ratio, totalTime;
  
  Group autoSpeedPanel;
  StackLayout autoSpeedPanelLayout;
  Composite autoSpeedInfoPanel;
  Composite autoSpeedDisabledPanel;
  Canvas  pingCanvas;  
  PingGraphic pingGraph;
  BufferedLabel idlePing,maxPing,maxUp,currentPing;
  
  
  private final DecimalFormat formatter = new DecimalFormat( "##.#" );
  
  
  
  public TransferStatsView(GlobalManager manager,AzureusCore core) {
    this.core = core;
    this.manager = manager;
    this.stats = manager.getStats();
    this.totalStats = StatsFactory.getStats();
  }
  
  public void initialize(Composite composite) {
    
    mainPanel = new Composite(composite,SWT.NULL);
    GridLayout mainLayout = new GridLayout();
    mainPanel.setLayout(mainLayout);
    
    createGeneralPanel();
    
    createAutoSpeedPanel();
  }

  private void createGeneralPanel() {
    generalPanel = new Composite(mainPanel,SWT.BORDER);
    GridData generalPanelData = new GridData(GridData.FILL_HORIZONTAL);
    generalPanel.setLayoutData(generalPanelData);
    
    GridLayout panelLayout = new GridLayout();
    panelLayout.numColumns = 5;
    panelLayout.makeColumnsEqualWidth = true;
    generalPanel.setLayout(panelLayout);
    
    GridData gridData;
    
    Label lbl = new Label(generalPanel,SWT.NULL);
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.downloaded");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uploaded");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.ratio");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uptime");
    
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    
    /////// NOW /////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.now");
    
    nowDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    nowDown.setLayoutData(gridData);
    
    nowUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    nowUp.setLayoutData(gridData);
    
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    
    
    //////// SESSION ////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.session");
    sessionDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionDown.setLayoutData(gridData);
    
    sessionUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionUp.setLayoutData(gridData);
    
    session_ratio = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    session_ratio.setLayoutData(gridData);    
    
    sessionTime = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionTime.setLayoutData(gridData);
    
    
    ///////// TOTAL ///////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.total");
    
    totalDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalDown.setLayoutData(gridData);
    
    totalUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalUp.setLayoutData(gridData);
    
    total_ratio = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    total_ratio.setLayoutData(gridData);
    
    totalTime = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalTime.setLayoutData(gridData);
  }
  
  
  private void createAutoSpeedPanel() {
    autoSpeedPanel = new Group(mainPanel,SWT.NONE);
    GridData generalPanelData = new GridData(GridData.FILL_BOTH);
    autoSpeedPanel.setLayoutData(generalPanelData);
    Messages.setLanguageText(autoSpeedPanel,"SpeedView.stats.autospeed");
    
    
    autoSpeedPanelLayout = new StackLayout();
    autoSpeedPanel.setLayout(autoSpeedPanelLayout);
    
    autoSpeedInfoPanel = new Composite(autoSpeedPanel,SWT.NULL);
    autoSpeedInfoPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout();
    layout.numColumns = 8;
    layout.makeColumnsEqualWidth = true;
    autoSpeedInfoPanel.setLayout(layout);
    
    Label label;
    GridData gridData;
    
    label = new Label(autoSpeedInfoPanel,SWT.NONE);
    Messages.setLanguageText(label,"SpeedView.stats.idlePing");    
    idlePing = new BufferedLabel(autoSpeedInfoPanel,SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    idlePing.setLayoutData(gridData);
    
    label = new Label(autoSpeedInfoPanel,SWT.NONE);
    Messages.setLanguageText(label,"SpeedView.stats.maxPing");    
    maxPing = new BufferedLabel(autoSpeedInfoPanel,SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    maxPing.setLayoutData(gridData);
    
    label = new Label(autoSpeedInfoPanel,SWT.NONE);
    Messages.setLanguageText(label,"SpeedView.stats.maxUp");    
    maxUp = new BufferedLabel(autoSpeedInfoPanel,SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    maxUp.setLayoutData(gridData);
    
    label = new Label(autoSpeedInfoPanel,SWT.NONE);
    Messages.setLanguageText(label,"SpeedView.stats.currentPing");    
    currentPing = new BufferedLabel(autoSpeedInfoPanel,SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    currentPing.setLayoutData(gridData);
    
    pingCanvas = new Canvas(autoSpeedInfoPanel,SWT.NONE);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 8;
    pingCanvas.setLayoutData(gridData);
    
    pingGraph = PingGraphic.getInstance();
    pingGraph.initialize(pingCanvas);
    
    autoSpeedDisabledPanel = new Composite(autoSpeedPanel,SWT.NULL);
    autoSpeedDisabledPanel.setLayout(new GridLayout());
    Label disabled = new Label(autoSpeedDisabledPanel,SWT.NULL);
    disabled.setEnabled(false);
    Messages.setLanguageText(disabled,"SpeedView.stats.autospeed.disabled");
    disabled.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL));
    
    SpeedManager speedManager = core.getSpeedManager();
    autoSpeedPanelLayout.topControl = speedManager.isAvailable() ? autoSpeedInfoPanel : autoSpeedDisabledPanel;
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 8;
    
	Legend.createLegendComposite(
			autoSpeedInfoPanel,
    		PingGraphic.colors,
    		new String[]{
        			"TransferStatsView.legend.pingaverage",        			
        			"TransferStatsView.legend.ping1",        			
        			"TransferStatsView.legend.ping2",
    				"TransferStatsView.legend.ping3" },
    		gridData );
  }
  
  public void delete() {
    Utils.disposeComposite(generalPanel);
    pingGraph.dispose();
  }

  public String getFullTitle() {
    return MessageText.getString("SpeedView.title.full"); //$NON-NLS-1$
  }
  
  public Composite getComposite() {
    return mainPanel;
  }
  
  
  
  public void refresh() {

    refreshGeneral();
    
    refreshPingPanel();
    
  }

  private void refreshGeneral() {
    int now_prot_down_rate = stats.getProtocolReceiveRate();
    int now_prot_up_rate = stats.getProtocolSendRate();
    
    int now_total_down_rate = stats.getDataReceiveRate() + now_prot_down_rate;
    int now_total_up_rate = stats.getDataSendRate() + now_prot_up_rate;
    
    float now_perc_down = (float)(now_prot_down_rate *100) / (now_total_down_rate==0?1:now_total_down_rate);
    float now_perc_up = (float)(now_prot_up_rate *100) / (now_total_up_rate==0?1:now_total_up_rate);

    nowDown.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec( now_total_down_rate ) +
                    "  (" + DisplayFormatters.formatByteCountToKiBEtcPerSec( now_prot_down_rate ) +
                    ", " +formatter.format( now_perc_down )+ "%)" );
    
    nowUp.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec( now_total_up_rate ) +
                  "  (" + DisplayFormatters.formatByteCountToKiBEtcPerSec( now_prot_up_rate ) +
                  ", " +formatter.format( now_perc_up )+ "%)" );

    ///////////////////////////////////////////////////////////////////////

    long session_prot_received = stats.getTotalProtocolBytesReceived();
    long session_prot_sent = stats.getTotalProtocolBytesSent();
      
    long session_total_received = stats.getTotalDataBytesReceived() + session_prot_received;
    long session_total_sent = stats.getTotalDataBytesSent() + session_prot_sent;

    float session_perc_received = (float)(session_prot_received *100) / (session_total_received==0?1:session_total_received);
    float session_perc_sent = (float)(session_prot_sent *100) / (session_total_sent==0?1:session_total_sent);

    sessionDown.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_received ) +
                        "  (" + DisplayFormatters.formatByteCountToKiBEtc( session_prot_received ) +
                        ", " +formatter.format( session_perc_received )+ "%)" );
    
    sessionUp.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_sent ) +
                      "  (" + DisplayFormatters.formatByteCountToKiBEtc( session_prot_sent ) +
                      ", " +formatter.format( session_perc_sent )+ "%)" );
    
    ////////////////////////////////////////////////////////////////////////
    
    totalDown.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getDownloadedBytes() ));
    totalUp.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getUploadedBytes() ));

    sessionTime.setText( DisplayFormatters.formatETA( totalStats.getSessionUpTime() ) );
    totalTime.setText( DisplayFormatters.formatETA( totalStats.getTotalUpTime() ) );
    
    long dl_bytes = totalStats.getDownloadedBytes();
    
    long t_ratio_raw = (1000* totalStats.getUploadedBytes() / (dl_bytes==0?1:dl_bytes) );
    long s_ratio_raw = (1000* session_total_sent / (session_total_received==0?1:session_total_received) );
    
    String t_ratio = "";
    String s_ratio = "";

    String partial = String.valueOf(t_ratio_raw % 1000);
    while (partial.length() < 3) {
      partial = "0" + partial;
    }
    t_ratio = (t_ratio_raw / 1000) + "." + partial;
    
    partial = String.valueOf(s_ratio_raw % 1000);
    while (partial.length() < 3) {
      partial = "0" + partial;
    }
    s_ratio = (s_ratio_raw / 1000) + "." + partial;
    
    
    total_ratio.setText( t_ratio );
    session_ratio.setText( s_ratio );
  }  
  
  private void refreshPingPanel() {
    SpeedManager speedManager = core.getSpeedManager();
    if(speedManager.isAvailable() && speedManager.isEnabled()) {
      autoSpeedPanelLayout.topControl = autoSpeedInfoPanel;
      autoSpeedPanel.layout();
      SpeedManagerPingSource sources[] = speedManager.getPingSources();
      if(sources.length > 0) {
        int average = 0;
        for(int i = 0 ; i < sources.length ; i++) {
          average += sources[i].getPingTime();
        }
        average = average / sources.length;        
        pingGraph.refresh();        
        
        currentPing.setText(average + " ms");
        idlePing.setText(speedManager.getIdlePingMillis() + " ms");
        maxPing.setText(speedManager.getMaxPingMillis() + " ms");
        maxUp.setText(DisplayFormatters.formatByteCountToBase10KBEtcPerSec(speedManager.getMaxUploadSpeed()));
        
      }
    } else {
      autoSpeedPanelLayout.topControl = autoSpeedDisabledPanel;
      autoSpeedPanel.layout();
    }
    
    
  }
  
  public void periodicUpdate() {
    SpeedManager speedManager = core.getSpeedManager();
    if(speedManager.isAvailable() && speedManager.isEnabled()) {
      SpeedManagerPingSource sources[] = speedManager.getPingSources();
      if(sources.length > 0) {
        int[] pings = new int[sources.length];
        for(int i = 0 ; i < sources.length ; i++) {
          pings[i] = sources[i].getPingTime();
        }
        pingGraph.addIntsValue(pings);
      }
    }
  }
  
  public String getData() {
    return "TransferStatsView.title.full";
  }
    
}

