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
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerFactory;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerStats;

/**
 * 
 */
public class CacheView extends AbstractIView {
  
  CacheFileManagerStats stats;
  
  Composite panel;
  
  Label lblInUse,lblSize,lblPercentUsed;
  ProgressBar pbInUse;
  
  Label lblReadsInCache,lblReadsFromFile,lblPercentReads;
  ProgressBar pbReads;
  
  Label lblWritesToCache,lblWritesToFile,lblPercentWrites;
  ProgressBar pbWrites;
  
  Canvas  readsFromFile,readsFromCache,writesToCache,writesToFile;
  
  SpeedGraphic rffGraph,rfcGraph,wtcGraph,wtfGraph;
  
  public CacheView() {
    try {
      this.stats = CacheFileManagerFactory.getSingleton().getStats();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new GridLayout());
    
    generateGeneralGroup();
    generateReadsGroup();
    generateWritesGroup();
  }
  
  /**
   * 
   */
  private void generateGeneralGroup() {
    GridData gridData;
    
    Group gCacheGeneral = new Group(panel,SWT.BORDER);
    Messages.setLanguageText(gCacheGeneral,"CacheView.general.title");
    gCacheGeneral.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    GridLayout layoutGeneral = new GridLayout();
    layoutGeneral.numColumns = 4;    
    gCacheGeneral.setLayout(layoutGeneral);
    Label lbl;
    
    lbl = new Label(gCacheGeneral,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.general.inUse");
    
    lblInUse = new Label(gCacheGeneral,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblInUse.setLayoutData(gridData);
    
    pbInUse =  new ProgressBar(gCacheGeneral,SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.verticalSpan = 2;
    pbInUse.setLayoutData(gridData);
    pbInUse.setMinimum(0);
    pbInUse.setMaximum(1000);
    
    lblPercentUsed = new Label(gCacheGeneral,SWT.NULL);
    gridData = new GridData();
    gridData.verticalSpan = 2;
    gridData.widthHint = 100;
    lblPercentUsed.setLayoutData(gridData);
    
    lbl = new Label(gCacheGeneral,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.general.size");
    
    lblSize = new Label(gCacheGeneral,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblSize.setLayoutData(gridData);
  }
  
  private void generateReadsGroup() {
    GridData gridData;
    
    Group gCacheReads = new Group(panel,SWT.BORDER);
    Messages.setLanguageText(gCacheReads,"CacheView.reads.title");
    gCacheReads.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    GridLayout layoutGeneral = new GridLayout();
    layoutGeneral.numColumns = 4;    
    gCacheReads.setLayout(layoutGeneral);
    Label lbl;
    
    lbl = new Label(gCacheReads,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.reads.fromCache");
    
    lblReadsInCache = new Label(gCacheReads,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblReadsInCache.setLayoutData(gridData);
    
    pbReads =  new ProgressBar(gCacheReads,SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.verticalSpan = 2;
    pbReads.setLayoutData(gridData);
    pbReads.setMinimum(0);
    pbReads.setMaximum(1000);
    
    lblPercentReads = new Label(gCacheReads,SWT.NULL);
    gridData = new GridData();
    gridData.verticalSpan = 2;
    gridData.widthHint = 100;
    lblPercentReads.setLayoutData(gridData);
    
    lbl = new Label(gCacheReads,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.reads.fromFile");
    
    lblReadsFromFile = new Label(gCacheReads,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblReadsFromFile.setLayoutData(gridData);        
  }
  
  private void generateSpeedGroup() {
    GridData gridData;
    
    Group gCacheSpeeds = new Group(panel,SWT.BORDER);
    Messages.setLanguageText(gCacheSpeeds,"CacheView.reads.title");
    gCacheSpeeds.setLayoutData(new GridData(GridData.FILL_BOTH));
    
    GridLayout layoutGeneral = new GridLayout();
    layoutGeneral.numColumns = 3;    
    gCacheSpeeds.setLayout(layoutGeneral);
    Label lbl;
    
    lbl = new Label(gCacheSpeeds,SWT.NULL);
    lbl = new Label(gCacheSpeeds,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.speeds.reads");
    lbl = new Label(gCacheSpeeds,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.speeds.writes");
    
    lbl = new Label(gCacheSpeeds,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.speeds.fromCache");
    
    readsFromCache = new Canvas(gCacheSpeeds,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    readsFromCache.setLayoutData(gridData);
    rfcGraph = SpeedGraphic.getInstance();
    rfcGraph.initialize(readsFromCache);
    
    
    writesToCache = new Canvas(gCacheSpeeds,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    writesToCache.setLayoutData(gridData);
    wtcGraph = SpeedGraphic.getInstance();
    wtcGraph.initialize(writesToCache);
    
    lbl = new Label(gCacheSpeeds,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.speeds.fromFile");
    
    readsFromFile = new Canvas(gCacheSpeeds,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    readsFromFile.setLayoutData(gridData);
    rffGraph = SpeedGraphic.getInstance();
    rffGraph.initialize(readsFromFile);
    
    writesToFile = new Canvas(gCacheSpeeds,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    writesToFile.setLayoutData(gridData);
    wtfGraph = SpeedGraphic.getInstance();
    wtfGraph.initialize(writesToFile);
  }
  
  public void periodicUpdate() {
    rfcGraph.addIntValue((int)stats.getAverageBytesReadFromCache());
    rffGraph.addIntValue((int)stats.getAverageBytesReadFromFile());
    wtcGraph.addIntValue((int)stats.getAverageBytesWrittenToCache());
    wtfGraph.addIntValue((int)stats.getAverageBytesWrittenToFile());
  }
  
  private void generateWritesGroup() {
    GridData gridData;
    
    Group gCacheWrites = new Group(panel,SWT.BORDER);
    Messages.setLanguageText(gCacheWrites,"CacheView.writes.title");
    gCacheWrites.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    GridLayout layoutGeneral = new GridLayout();
    layoutGeneral.numColumns = 4;    
    gCacheWrites.setLayout(layoutGeneral);
    Label lbl;
    
    lbl = new Label(gCacheWrites,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.writes.toCache");
    
    lblWritesToCache = new Label(gCacheWrites,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblWritesToCache.setLayoutData(gridData);
    
    pbWrites =  new ProgressBar(gCacheWrites,SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.verticalSpan = 2;
    pbWrites.setLayoutData(gridData);
    pbWrites.setMinimum(0);
    pbWrites.setMaximum(1000);
    
    lblPercentWrites = new Label(gCacheWrites,SWT.NULL);
    gridData = new GridData();
    gridData.verticalSpan = 2;
    gridData.widthHint = 100;
    lblPercentWrites.setLayoutData(gridData);
    
    lbl = new Label(gCacheWrites,SWT.NULL);
    Messages.setLanguageText(lbl,"CacheView.writes.toFile");
    
    lblWritesToFile = new Label(gCacheWrites,SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    lblPercentWrites.setLayoutData(gridData);
  }

  public void delete() {
    Utils.disposeComposite(panel);
  }

  public String getFullTitle() {
    return MessageText.getString("CacheView.title.full"); //$NON-NLS-1$
  }
  
  public Composite getComposite() {
    return panel;
  }
  
  public void refresh() {
    //General Part    
    lblSize.setText(DisplayFormatters.formatByteCountToKiBEtc(stats.getSize()));
    lblInUse.setText(DisplayFormatters.formatByteCountToKiBEtc(stats.getUsedSize()));
    
    int perThousands = (int) ((1000 * stats.getUsedSize()) / stats.getSize());
    lblPercentUsed.setText(DisplayFormatters.formatPercentFromThousands(perThousands));
    pbInUse.setSelection(perThousands);
    
    //Reads
    lblReadsInCache.setText((DisplayFormatters.formatByteCountToKiBEtc(stats.getBytesReadFromCache())));
    lblReadsFromFile.setText((DisplayFormatters.formatByteCountToKiBEtc(stats.getBytesReadFromFile())));
    
    long totalRead = stats.getBytesReadFromCache() + stats.getBytesReadFromFile();
    if(totalRead > 0) {
      perThousands = (int) ((1000l * stats.getBytesReadFromCache()) / totalRead);
      lblPercentReads.setText(DisplayFormatters.formatPercentFromThousands(perThousands) + " " + MessageText.getString("CacheView.reads.hits"));
      pbReads.setSelection(perThousands);      
    }
    
    //Writes
    lblWritesToCache.setText((DisplayFormatters.formatByteCountToKiBEtc(stats.getBytesWrittenToCache())));
    lblWritesToFile.setText((DisplayFormatters.formatByteCountToKiBEtc(stats.getBytesWrittenToFile())));
    
    long totalWrites = stats.getBytesWrittenToCache() + stats.getBytesWrittenToFile();
    if(totalWrites > 0) {
      perThousands = (int) ((1000* stats.getBytesWrittenToCache()) / totalWrites);
      lblPercentWrites.setText(DisplayFormatters.formatPercentFromThousands(perThousands) + " " + MessageText.getString("CacheView.reads.hits"));
      pbWrites.setSelection(perThousands);      
    }
  }  
  
  public String getData() {
    return "CacheView.title.full";
  }
    
}


