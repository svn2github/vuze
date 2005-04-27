/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views.stats;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

/**
 * 
 */
public class TransferStatsView extends AbstractIView {

  GlobalManager manager;
  GlobalManagerStats stats;
  
  OverallStats totalStats;
  
  Composite panel;
  
  BufferedLabel sessionDown, sessionUp, session_ratio, sessionTime, totalDown, totalUp, total_ratio, totalTime;
  
  public TransferStatsView(GlobalManager manager) {
    this.manager = manager;
    this.stats = manager.getStats();
    this.totalStats = StatsFactory.getStats();
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    
    GridLayout panelLayout = new GridLayout();
    panelLayout.numColumns = 5;
    panelLayout.makeColumnsEqualWidth = true;
    panel.setLayout(panelLayout);
    
    GridData gridData;
    
    Label lbl = new Label(panel,SWT.NULL);
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.downloaded");
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uploaded");
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.ratio");
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uptime");
    
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.session");
    sessionDown = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionDown.setLayoutData(gridData);
    
    sessionUp = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionUp.setLayoutData(gridData);
    
    session_ratio = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    session_ratio.setLayoutData(gridData);    
    
    sessionTime = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionTime.setLayoutData(gridData);
    
    lbl = new Label(panel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.total");
    
    totalDown = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalDown.setLayoutData(gridData);
    
    totalUp = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalUp.setLayoutData(gridData);
    
    total_ratio = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    total_ratio.setLayoutData(gridData);
    
    totalTime = new BufferedLabel(panel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalTime.setLayoutData(gridData);
    
  }
  
  public void delete() {
    Utils.disposeComposite(panel);
  }

  public String getFullTitle() {
    return MessageText.getString("SpeedView.title.full"); //$NON-NLS-1$
  }
  
  public Composite getComposite() {
    return panel;
  }
  
  public void refresh() {
    long session_total_received = stats.getTotalDataBytesReceived() + stats.getTotalProtocolBytesReceived();
    long session_total_sent = stats.getTotalDataBytesSent() + stats.getTotalProtocolBytesSent();
   
    long session_total_received_protocol = stats.getTotalProtocolBytesReceived();
    long session_total_sent_protocol =  stats.getTotalProtocolBytesSent();
    
    sessionDown.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_received ) + " (" + DisplayFormatters.formatByteCountToKiBEtc( session_total_received_protocol) + ")");
    sessionUp.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_sent ) + " (" + DisplayFormatters.formatByteCountToKiBEtc( session_total_sent_protocol) + ")");   
    
    totalDown.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getDownloadedBytes() ));
    totalUp.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getUploadedBytes() ));

    sessionTime.setText( DisplayFormatters.formatETA( totalStats.getSessionUpTime() ) );
    totalTime.setText( DisplayFormatters.formatETA( totalStats.getTotalUpTime() ) );
    
    long t_ratio_raw = (1000* totalStats.getUploadedBytes() / (totalStats.getDownloadedBytes()+1) );
    long s_ratio_raw = (1000* session_total_sent / (session_total_received+1) );
    
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
  
  public String getData() {
    return "TransferStatsView.title.full";
  }
    
}

