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
  private Composite panel;
  private static final int PEER_SIZE = 15;
  private static final int OWN_SIZE = 75;
  
  //Comparator Class
  //Note: this comparator imposes orderings that are inconsistent with equals.
  class PeerComparator implements Comparator {
    public int compare(Object arg0, Object arg1) {
      int result;
      PEPeer peer0 = (PEPeer) arg0;
      PEPeer peer1 = (PEPeer) arg1;
      boolean interesting0,interesting1,interested0,interested1;
      
      //Order is : Non Interesting < Interesting
      interesting0 = peer0.isInterestedInMe();
      interesting1 = peer1.isInterestedInMe();
      
      result =  (interesting0 ? 1 : -1) - (interesting1 ? 1 : -1);
      if(result != 0) return result;
      
      //Order is : Non Interested < Interested
      interested0 = peer0.isInterestingToMe();
      interested1 = peer1.isInterestingToMe();
      
      result =  (interested0 ? 1 : -1) - (interested1 ? 1 : -1);
      if(result != 0) return result;
      
      //Then we sort on %, but depending on interested ...
      int percent0 = peer0.getPercentDoneInThousandNotation();
      int percent1 = peer1.getPercentDoneInThousandNotation();
      
      return interested0 ? percent0 - percent1 : percent1 - percent0;
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
    panel = new Canvas(composite,SWT.NULL);
  }

  public void refresh() {
    
    //Comment the following line to enable the view
    if(true) return;
    
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
    Display display = panel.getDisplay();
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
      angle = (2 * Math.PI *i) / nbPeers - Math.PI / 2;
      int x = x0 + (int) (r * Math.cos(angle));
      int y = y0 + (int) (r * Math.sin(angle));
      int[] triangle = new int[6];
      triangle[0] = x;
      triangle[1] = y;      
      if(! peer.isChokedByMe()) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        int x1 = (int) (5 * Math.cos(angle+Math.PI / 2));
        int y1 = (int) (5 * Math.sin(angle+Math.PI / 2));
        triangle[0] = x;
        triangle[1] = y;
        triangle[2] = x0 ;
        triangle[3] = y0 ;
        triangle[4] = x0 + x1;
        triangle[5] = y0 + y1;        
        gcBuffer.fillPolygon(triangle);        
      }
      
      if(! peer.isChokingMe()) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
        int x1 = (int) (5 * Math.cos(angle+Math.PI / 2));
        int y1 = (int) (5 * Math.sin(angle+Math.PI / 2));
        triangle[0] = x - x1;
        triangle[1] = y - y1;
        triangle[2] = x ;
        triangle[3] = y ;
        triangle[4] = x0;
        triangle[5] = y0;        
        gcBuffer.fillPolygon(triangle);        
      }
      
      PieUtils.drawPie(gcBuffer,x - PEER_SIZE / 2,y - PEER_SIZE / 2,PEER_SIZE,PEER_SIZE,peer.getPercentDoneInThousandNotation() / 10);
      
      //gcBuffer.drawText(peer.getIp() , x , y , true);
    }
    
    
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
