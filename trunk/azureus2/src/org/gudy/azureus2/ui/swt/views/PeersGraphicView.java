/*
 * Created on 19 nov. 2004
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
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.ui.swt.components.graphics.PieUtils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Olivier Chalouhi
 *
 */
public class PeersGraphicView extends AbstractIView implements DownloadManagerPeerListener {

  
  private DownloadManager manager;
  
  private List peers;
  private AEMonitor peers_mon = new AEMonitor( "PeersGraphicView:peers" );;
  private PeerComparator peerComparator;
  
  
  //UI Stuff
  private Display display;
  private Composite panel;
  private static final int PEER_SIZE = 15;
  private static final int PACKET_SIZE = 10;
  private static final int OWN_SIZE = 75;
  
  //Comparator Class
  //Note: this comparator imposes orderings that are inconsistent with equals.
  class PeerComparator implements Comparator {
    public int compare(Object arg0, Object arg1) {
      int result;
      PEPeer peer0 = (PEPeer) arg0;
      PEPeer peer1 = (PEPeer) arg1;

      //Then we sort on %, but depending on interested ...
      int percent0 = peer0.getPercentDoneInThousandNotation();
      int percent1 = peer1.getPercentDoneInThousandNotation();
      
      return percent0 - percent1;
    }
  }
  
  
  public PeersGraphicView(DownloadManager manager) {    
    this.manager = manager;
    this.peers = new ArrayList();
    this.peerComparator = new PeerComparator();
    this.manager.addPeerListener(this);
  } 
  
  public void delete() {
    manager.removePeerListener(this);
    super.delete();
  }

  public Composite getComposite() {    
    return panel;
  }
  
  public String getData() {
    return "PeersGraphicView.title";
  }

  public void initialize(Composite composite) {
    display = composite.getDisplay();
    panel = new Canvas(composite,SWT.NULL);    
  }

  public void refresh() {
    doRefresh();
  }
  
  private void doRefresh() {
    //Comment the following line to enable the view
    //if(true) return;
    
    PEPeer[] sortedPeers;
    try {      
      peers_mon.enter();      
      List connectedPeers = new ArrayList();
      Iterator iter = peers.iterator();
      while(iter.hasNext()) {
        PEPeerTransport peer = (PEPeerTransport) iter.next();
        if(peer.getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED)
          connectedPeers.add(peer);
      }
      
      sortedPeers = (PEPeer[]) connectedPeers.toArray(new PEPeer[connectedPeers.size()]);      
    } finally {
      peers_mon.exit();
    }
    
    if(sortedPeers == null) return;
    Arrays.sort(sortedPeers,peerComparator);
    
    render(sortedPeers);
  }
  
  private void render(PEPeer[] sortedPeers) {
    if(panel == null || panel.isDisposed())
      return;
    Point panelSize = panel.getSize();
    Image buffer = new Image(display,panelSize.x,panelSize.y);
    GC gcBuffer = new GC(buffer);    
    gcBuffer.setBackground(Colors.white);   
    gcBuffer.setForeground(Colors.blue);
    gcBuffer.fillRectangle(0,0,panelSize.x,panelSize.y);

    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);  
    
    int nbPeers = sortedPeers.length;
    
    double angle;
    int x0 = panelSize.x / 2;
    int y0 = panelSize.y / 2;    
    int r = x0 > y0 ? y0 - 20 : x0 - 20 ;
    if( r < 10) return;
    

    
    for(int i = 0 ; i < nbPeers ; i++) {
      PEPeer peer = sortedPeers[i];
      angle = (4 * i - nbPeers) * Math.PI  / (2 * nbPeers) - Math.PI / 2;
      double deltaXX = Math.cos(angle);
      double deltaXY = Math.sin(angle);
      double deltaYX = Math.cos(angle+Math.PI / 2);
      double deltaYY = Math.sin(angle+Math.PI / 2);
      
      int[] rectangle = new int[8];
      
      
      
      if(! peer.isChokedByMe() || ! peer.isChokingMe()) {
        gcBuffer.setForeground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        rectangle[0] = x0 + (int) (deltaXX * 3 + 0.5);
        rectangle[1] = y0 + (int) (deltaXY * 3 + 0.5);
        rectangle[2] = x0 - (int) (deltaXX * 3 + 0.5);
        rectangle[3] = y0 - (int) (deltaXY * 3 + 0.5);
        
        
        rectangle[4] = x0 - (int) (deltaXX * 3 - r * deltaYX + 0.5);
        rectangle[5] = y0 - (int) (deltaXY * 3 - r * deltaYY + 0.5);
        rectangle[6] = x0 + (int) (deltaXX * 3 + r * deltaYX + 0.5);
        rectangle[7] = y0 + (int) (deltaXY * 3 + r * deltaYY + 0.5);
        gcBuffer.drawPolygon(rectangle);        
      }    
      
      
      int percentSent = peer.getConnection().getIncomingMessageQueue().getPercentDoneOfCurrentMessage();
      if(percentSent >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
        int r1 = r - r * percentSent / 100;
        rectangle[0] = x0 + (int) (deltaXX * 3 + r1 * deltaYX + 0.5);
        rectangle[1] = y0 + (int) (deltaXY * 3 + r1 * deltaYY + 0.5);
        rectangle[2] = x0 - (int) (deltaXX * 3 - r1 * deltaYX + 0.5);
        rectangle[3] = y0 - (int) (deltaXY * 3 - r1 * deltaYY + 0.5);
        
        
        rectangle[4] = x0 - (int) (deltaXX * 3 - (r1-10) * deltaYX + 0.5);
        rectangle[5] = y0 - (int) (deltaXY * 3 - (r1-10) * deltaYY + 0.5);
        rectangle[6] = x0 + (int) (deltaXX * 3 + (r1-10) * deltaYX + 0.5);
        rectangle[7] = y0 + (int) (deltaXY * 3 + (r1-10) * deltaYY + 0.5);
        gcBuffer.fillPolygon(rectangle); 
      }
      
      
      
      percentSent = peer.getConnection().getOutgoingMessageQueue().getPercentDoneOfCurrentMessage();
      if(percentSent >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        int r1 = r * percentSent / 100;
        rectangle[0] = x0 + (int) (deltaXX * 3 + r1 * deltaYX + 0.5);
        rectangle[1] = y0 + (int) (deltaXY * 3 + r1 * deltaYY + 0.5);
        rectangle[2] = x0 - (int) (deltaXX * 3 - r1 * deltaYX + 0.5);
        rectangle[3] = y0 - (int) (deltaXY * 3 - r1 * deltaYY + 0.5);
        
        
        rectangle[4] = x0 - (int) (deltaXX * 3 - (r1-10) * deltaYX + 0.5);
        rectangle[5] = y0 - (int) (deltaXY * 3 - (r1-10) * deltaYY + 0.5);
        rectangle[6] = x0 + (int) (deltaXX * 3 + (r1-10) * deltaYX + 0.5);
        rectangle[7] = y0 + (int) (deltaXY * 3 + (r1-10) * deltaYY + 0.5);
        gcBuffer.fillPolygon(rectangle); 
      }
      
      
      
      int x1 = x0 + (int) (r * deltaYX);
      int y1 = y0 + (int) (r * deltaYY);
      gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
      if(peer.isSnubbed()) {
        gcBuffer.setBackground(Colors.grey);
      }
      PieUtils.drawPie(gcBuffer,x1 - PEER_SIZE / 2,y1 - PEER_SIZE / 2,PEER_SIZE,PEER_SIZE,peer.getPercentDoneInThousandNotation() / 10);
      
      //gcBuffer.drawText(peer.getIp() , x1 + 8 , y1 , true);
    }
    
    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
    PieUtils.drawPie(gcBuffer,x0 - OWN_SIZE / 2 ,y0 - OWN_SIZE / 2,OWN_SIZE,OWN_SIZE,manager.getStats().getCompleted() / 10);
    
    gcBuffer.dispose();
    GC gcPanel = new GC(panel);
    gcPanel.drawImage(buffer,0,0);
    gcPanel.dispose();
    buffer.dispose();
    
  }
  
  public void peerManagerAdded(PEPeerManager manager) {}
  public void peerManagerRemoved(PEPeerManager manager) {}
  public void pieceAdded(PEPiece piece) {}
  public void pieceRemoved(PEPiece piece) {}
  
  public void peerAdded(PEPeer peer) {
    try {
      peers_mon.enter();
      peers.add(peer);
    } finally {
      peers_mon.exit();
    }
  }
  
  public void peerRemoved(PEPeer peer) {
    try {
      peers_mon.enter();
      peers.remove(peer);
    } finally {
      peers_mon.exit();
    }
  }
}
