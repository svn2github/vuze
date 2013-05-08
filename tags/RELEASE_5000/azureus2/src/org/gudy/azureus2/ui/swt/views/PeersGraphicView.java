/*
 * Created on 19 nov. 2004
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
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.graphics.PieUtils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;

/**
 * This is the "Swarm" View
 * 
 * @author Olivier Chalouhi
 *
 */
public class PeersGraphicView
	implements DownloadManagerPeerListener, UISWTViewCoreEventListener, UIPluginViewToolBarListener
{
  
  public static String MSGID_PREFIX = "PeersGraphicView";

	private DownloadManager manager = null;
  
  private static final int NB_ANGLES = 1000;  
  private double[] angles;
  //private double[] deltaPerimeters;
  private double perimeter;
  private double[] rs;
  private double[] deltaXXs;
  private double[] deltaXYs;
  private double[] deltaYXs;
  private double[] deltaYYs;
  
  private Point oldSize;
  
  private List<PEPeer> peers;
  private AEMonitor peers_mon = new AEMonitor( "PeersGraphicView:peers" );;
  private PeerComparator peerComparator;
  
  private Map<PEPeer,int[]>		peer_hit_map = new HashMap<PEPeer, int[]>();
  private int					me_hit_x;
  private int					me_hit_y;
  
  private Image					my_flag;
  
  //UI Stuff
  private Display display;
  private Composite panel;

	private UISWTView swtView;
  private static final int PEER_SIZE = 18;
  //private static final int PACKET_SIZE = 10;
  private static final int OWN_SIZE_DEFAULT = 75;
  private static final int OWN_SIZE_MIN		= 30;
  private static final int OWN_SIZE_MAX 	= 75;
  private static int OWN_SIZE = OWN_SIZE_DEFAULT;
  
  
  //Comparator Class
  //Note: this comparator imposes orderings that are inconsistent with equals.
  class PeerComparator implements Comparator<PEPeer> {
  	public int compare(PEPeer peer0, PEPeer peer1) {

      int percent0 = peer0.getPercentDoneInThousandNotation();
      int percent1 = peer1.getPercentDoneInThousandNotation();
      
      int result = percent0 - percent1;
      
      if ( result == 0 ){
    	  
    	  long l = peer0.getTimeSinceConnectionEstablished() - peer1.getTimeSinceConnectionEstablished();
    	  
    	  if ( l < 0 ){
    		  result = -1;
    	  }else if ( result > 0 ){
    		  result = 1;
    	  }
      }
      
      return( result );
    }
  }
  
  
  public PeersGraphicView() {
    angles = new double[NB_ANGLES];
    //deltaPerimeters = new double[NB_ANGLES];
    rs = new double[NB_ANGLES];
    deltaXXs = new double[NB_ANGLES];
    deltaXYs = new double[NB_ANGLES];
    deltaYXs = new double[NB_ANGLES];
    deltaYYs = new double[NB_ANGLES];
    
    for(int i = 0 ; i < NB_ANGLES ; i++) {
      angles[i] = 2 * i * Math.PI / NB_ANGLES - Math.PI;
      deltaXXs[i] = Math.cos(angles[i]);
      deltaXYs[i] = Math.sin(angles[i]);
      deltaYXs[i] = Math.cos(angles[i]+Math.PI / 2);
      deltaYYs[i] = Math.sin(angles[i]+Math.PI / 2);
    }
    
    this.peers = new ArrayList<PEPeer>();
    this.peerComparator = new PeerComparator();
    
    InetAddress ia = NetworkAdmin.getSingleton().getDefaultPublicAddress();
    
    if ( ia != null ){
    	
    	my_flag = ImageRepository.getCountryFlag( ia, false );
    }
  } 
  
  private boolean comp_focused;
  private Object focus_pending_ds;
  
  private void
  setFocused( boolean foc )
  {
	  if ( foc ){
		  
		  comp_focused = true;
		  
		  dataSourceChanged( focus_pending_ds );
		  
	  }else{
		  
		  focus_pending_ds = manager;
		  
		  dataSourceChanged( null );
		  
		  comp_focused = false;
	  }
  }
  
  private void dataSourceChanged(Object newDataSource) {
	  if ( !comp_focused ){
		  focus_pending_ds = newDataSource;
		  return;
	  }
	  DownloadManager old_manager = manager;
	  if (newDataSource == null){
		  manager = null;
	  }else if (newDataSource instanceof Object[]){
		  Object temp = ((Object[])newDataSource)[0];
		  if ( temp instanceof DownloadManager ){
			  manager = (DownloadManager)temp;
		  }else if ( temp instanceof DiskManagerFileInfo){
			  manager = ((DiskManagerFileInfo)temp).getDownloadManager();
		  }else{
			  return;
		  }
	  }else{
		  if ( newDataSource instanceof DownloadManager ){
			  manager = (DownloadManager)newDataSource;
		  }else if ( newDataSource instanceof DiskManagerFileInfo){
			  manager = ((DiskManagerFileInfo)newDataSource).getDownloadManager();
		  }else{
			  return;
		  }
	  }

	  if ( old_manager == manager ){
		  return;
	  }
	  
	  if (old_manager != null){
		  old_manager.removePeerListener(this);
	  }
	  

	  try {
		  peers_mon.enter();
		  
		  peers.clear();
	  } finally {
		  peers_mon.exit();
	  }
	  if (manager != null){
		  manager.addPeerListener(this);
	  }
  }

  private void delete() {
  	if (manager != null){
  		manager.removePeerListener(this);
  	}
  	
  	peer_hit_map.clear();
  }

  private Composite getComposite() {    
    return panel;
  }
  
  private String getData() {
    return "PeersGraphicView.title.full";
  }

  private void initialize(Composite composite) {
    display = composite.getDisplay();
    
    panel = new Canvas(composite,SWT.NO_BACKGROUND);
        
    panel.addListener(SWT.MouseHover, new Listener() {
		public void handleEvent(Event event) {
			
			if ( manager == null ){
				return;
			}
			
			int	x = event.x;
			int y = event.y;
			
			String tt;

			if ( 	x >= me_hit_x && x <= me_hit_x+OWN_SIZE &&
					y >= me_hit_y && y <= me_hit_y+OWN_SIZE ){
				
				tt = DisplayFormatters.formatDownloadStatus( manager ) + ", " + 
						DisplayFormatters.formatPercentFromThousands(manager.getStats().getCompleted());
				
			}else{
				
				PEPeer target = null;
							
				for( Map.Entry<PEPeer,int[]> entry: peer_hit_map.entrySet()){
					
					int[] loc = entry.getValue();
					
					int	loc_x = loc[0];
					int loc_y = loc[1];
					
					if ( 	x >= loc_x && x <= loc_x+PEER_SIZE &&
							y >= loc_y && y <= loc_y+PEER_SIZE ){
						
						target = entry.getKey();
						
						break;
					}
				}	
				
				if ( target == null ){
					
					tt = null;
					
				}else{
					
					PEPeerStats stats = target.getStats();
					
					String[] details = PeerUtils.getCountryDetails( target );
					
					String dstr = (details==null||details.length<2)?"":(" - " + details[0] + "/" + details[1]);
					
					tt = target.getIp() + dstr + ", " + 
							DisplayFormatters.formatPercentFromThousands(target.getPercentDoneInThousandNotation()) + "\r\n" +
							"Up=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( stats.getDataSendRate() + stats.getProtocolSendRate()) + ", " +
							"Down=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( stats.getDataReceiveRate() + stats.getProtocolReceiveRate());
				}
			}
			
			panel.setToolTipText( tt );
		}
    });
    
    panel.addMouseListener(
    	new MouseAdapter()
    	{
    		public void 
    		mouseDoubleClick(
    			MouseEvent event )
    		{
    			int	x = event.x;
    			int y = event.y;
    										
				for( Map.Entry<PEPeer,int[]> entry: peer_hit_map.entrySet()){
					
					int[] loc = entry.getValue();
					
					int	loc_x = loc[0];
					int loc_y = loc[1];
					
					if ( 	x >= loc_x && x <= loc_x+PEER_SIZE &&
							y >= loc_y && y <= loc_y+PEER_SIZE ){
						
						PEPeer target = entry.getKey();
						
							// ugly code to locate any associated 'PeersView' that we can locate the peer in
						
						try{
							String dm_id = "DMDetails_" + Base32.encode( manager.getTorrent().getHash());
							
							MdiEntry mdi_entry = UIFunctionsManager.getUIFunctions().getMDI().getEntry( dm_id );
						
							if ( mdi_entry != null ){
								
								mdi_entry.setDatasource(new Object[] { manager, target } );
							}
								
							Composite comp = panel.getParent();
							
							while( comp != null ){
								
								if ( comp instanceof CTabFolder ){
									
									CTabFolder tf = (CTabFolder)comp;
									
									CTabItem[] items = tf.getItems();
									
									for ( CTabItem item: items ){
										
										UISWTViewCore view = (UISWTViewCore)item.getData("IView");
										
										UISWTViewEventListener listener = view.getEventListener();
										
										if ( listener instanceof UISWTViewEventListenerHolder ){
											
											listener = ((UISWTViewEventListenerHolder)listener).getDelegatedEventListener( view );
										}
										
										if ( listener instanceof PeersView ){
											
											tf.setSelection( item );
											
											Event ev = new Event();
											
											ev.item = item;
											
												// manual setSelection doesn't file selection event - derp
											
											tf.notifyListeners( SWT.Selection, ev );
											
											((PeersView)listener).selectPeer( target );
											
											return;
										}
									}
								}
									
								comp = comp.getParent();
							}
						}catch( Throwable e ){
							
						}
						
						break;
					}
				}		
    		}
    	});
  }

  private void refresh() {
    doRefresh();
  }
  
  private void doRefresh() {
    //Comment the following line to enable the view
    //if(true) return;
    
    PEPeer[] sortedPeers;
    try {      
      peers_mon.enter();      
      List<PEPeerTransport> connectedPeers = new ArrayList<PEPeerTransport>();
      for (PEPeer peer : peers) {
      	if (peer instanceof PEPeerTransport) {
      		PEPeerTransport peerTransport = (PEPeerTransport) peer;
      		if(peerTransport.getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED)
      			connectedPeers.add(peerTransport);
      	}
      }
      
      sortedPeers = connectedPeers.toArray(new PEPeer[connectedPeers.size()]);      
    } finally {
      peers_mon.exit();
    }
    
    if(sortedPeers == null) return;
    Arrays.sort(sortedPeers,peerComparator);
    
    render(sortedPeers);
  }
  
  private void render(PEPeer[] sortedPeers) {
	peer_hit_map.clear();
	  
    if(panel == null || panel.isDisposed()){
    	return;
    }
    
    if ( manager == null ){
    	GC gcPanel = new GC(panel);
    	gcPanel.fillRectangle( panel.getBounds());
    	gcPanel.dispose();
    	return;
    }
    Point panelSize = panel.getSize();
    
    int	min_dim = Math.min( panelSize.x, panelSize.y );
    
    if ( min_dim <= 100 ){
    	OWN_SIZE = OWN_SIZE_MIN;
    }else if ( min_dim >= 400 ){
    	OWN_SIZE = OWN_SIZE_DEFAULT;
    }else{
    	int s_diff = OWN_SIZE_MAX - OWN_SIZE_MIN;
    	float rat = (min_dim - 100.0f)/(400-100);
    	
    	OWN_SIZE = OWN_SIZE_MIN + (int)(s_diff * rat );
    }
    
    
    int x0 = panelSize.x / 2;
    int y0 = panelSize.y / 2;  
    int a = x0 - 20;
    int b = y0 - 20;
    if(a < 10 || b < 10) return;
    
    if(oldSize == null || !oldSize.equals(panelSize)) {     
      oldSize = panelSize;      
      perimeter = 0;
      for(int i = 0 ; i < NB_ANGLES ; i++) {
        rs[i] = Math.sqrt(1/(deltaYXs[i] * deltaYXs[i] / (a*a) + deltaYYs[i] * deltaYYs[i] / (b * b)));
        perimeter += rs[i];
      }
    }
    Image buffer = new Image(display,panelSize.x,panelSize.y);
    GC gcBuffer = new GC(buffer);    
    gcBuffer.setBackground(Colors.white);   
    gcBuffer.setForeground(Colors.blue);
    gcBuffer.fillRectangle(0,0,panelSize.x,panelSize.y);

    try {
      gcBuffer.setTextAntialias(SWT.ON);
      gcBuffer.setAntialias(SWT.ON);
    } catch(Exception e) {
    }
    
    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);      
    
    int nbPeers = sortedPeers.length;
    
    int iAngle = 0;
    double currentPerimeter = 0;    
    //double angle;
    double r;   

    for(int i = 0 ; i < nbPeers ; i++) {
      PEPeer peer = sortedPeers[i];
      do {
        //angle = angles[iAngle];
        r     = rs[iAngle];
        currentPerimeter += r;
        if(iAngle + 1 < NB_ANGLES) iAngle++;
      } while( currentPerimeter < i * perimeter / nbPeers);
            
      //angle = (4 * i - nbPeers) * Math.PI  / (2 * nbPeers) - Math.PI / 2;
      
      int[] triangle = new int[6];
      
      
      int percent_received 	= peer.getPercentDoneOfCurrentIncomingRequest();
      int percent_sent 		= peer.getPercentDoneOfCurrentOutgoingRequest();

      	// set up base line state
      

      boolean	drawLine = false;
      
      	// unchoked
      
      if ( !peer.isChokingMe() || percent_received >= 0 ){
      	gcBuffer.setForeground(Colors.blues[1] );
     	drawLine = true;
      }

      	// unchoking
      
      if ( !peer.isChokedByMe() || percent_sent >= 0 ){
  		gcBuffer.setForeground(Colors.blues[3]);
  		drawLine = true;
      }
         
      	// receiving from choked peer (fast request in)
      
      if ( !peer.isChokingMe() && peer.isUnchokeOverride() && peer.isInteresting()){
  		gcBuffer.setForeground(Colors.green);
  		drawLine = true;
      }
      
      	// sending to choked peer (fast request out)
      
      if ( peer.isChokedByMe() && percent_sent >= 0 ){
    	gcBuffer.setForeground(Colors.green);
    	drawLine = true;
      }
      
      if ( drawLine ){
		int x1 = x0 + (int) ( r * deltaYXs[iAngle] );
		int y1 = y0 + (int) ( r * deltaYYs[iAngle] );        
		gcBuffer.drawLine(x0,y0,x1,y1);
      }    
      
      
      
      if(percent_received >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
        double r1 = r - r * percent_received / 100;
        triangle[0] = (int) (x0 + (r1-10) * deltaYXs[iAngle] + 0.5);
        triangle[1] = (int) (y0 + (r1-10) * deltaYYs[iAngle] + 0.5);
        
        triangle[2] =  (int) (x0 + deltaXXs[iAngle] * 4 + (r1) * deltaYXs[iAngle] + 0.5);
        triangle[3] =  (int) (y0 + deltaXYs[iAngle] * 4 + (r1) * deltaYYs[iAngle] + 0.5);
        
        
        triangle[4] =  (int) (x0 - deltaXXs[iAngle] * 4 + (r1) * deltaYXs[iAngle] + 0.5);
        triangle[5] =  (int) (y0 - deltaXYs[iAngle] * 4 + (r1) * deltaYYs[iAngle] + 0.5);
        
        gcBuffer.fillPolygon(triangle); 
      }
      
      
      
      if(percent_sent >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        double r1 = r * percent_sent / 100;
        triangle[0] = (int) (x0 + r1 * deltaYXs[iAngle] + 0.5);
        triangle[1] = (int) (y0 + r1 * deltaYYs[iAngle] + 0.5);
        
        triangle[2] =  (int) (x0 + deltaXXs[iAngle] * 4 + (r1-10) * deltaYXs[iAngle] + 0.5);
        triangle[3] =  (int) (y0 + deltaXYs[iAngle] * 4 + (r1-10) * deltaYYs[iAngle] + 0.5);
        
        
        triangle[4] =  (int) (x0 - deltaXXs[iAngle] * 4 + (r1-10) * deltaYXs[iAngle] + 0.5);
        triangle[5] =  (int) (y0 - deltaXYs[iAngle] * 4 + (r1-10) * deltaYYs[iAngle] + 0.5);
        gcBuffer.fillPolygon(triangle); 
      }
      
      
      
      int x1 = x0 + (int) (r * deltaYXs[iAngle]);
      int y1 = y0 + (int) (r * deltaYYs[iAngle]);
      gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
      if(peer.isSnubbed()) {
        gcBuffer.setBackground(Colors.grey);
      }
      
      /*int PS = (int) (PEER_SIZE);      
        if (deltaXY == 0) {
          PS = (int) (PEER_SIZE * 2);
        } else {
          if (deltaYY > 0) {
            PS = (int) (PEER_SIZE / deltaXY);
          }
        }*/
      //PieUtils.drawPie(gcBuffer,(x1 - PS / 2),y1 - PS / 2,PS,PS,peer.getPercentDoneInThousandNotation() / 10);
      
      int peer_x = x1 - PEER_SIZE / 2;
      int peer_y = y1 - PEER_SIZE / 2;
      
      peer_hit_map.put( peer, new int[]{ peer_x, peer_y });
      
      Image flag = ImageRepository.getCountryFlag( peer, false );
      if ( flag != null ){
    	  PieUtils.drawPie(gcBuffer, flag, peer_x, peer_y,PEER_SIZE,PEER_SIZE,peer.getPercentDoneInThousandNotation() / 10, true );
      }else{
      
    	  PieUtils.drawPie(gcBuffer, peer_x, peer_y,PEER_SIZE,PEER_SIZE,peer.getPercentDoneInThousandNotation() / 10);
      }
      //gcBuffer.drawText(peer.getIp() , x1 + 8 , y1 , true);
    }
    
    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
    
    me_hit_x = x0 - OWN_SIZE / 2;
    me_hit_y = y0 - OWN_SIZE / 2;
      
   	PieUtils.drawPie(gcBuffer, me_hit_x, me_hit_y,OWN_SIZE,OWN_SIZE,manager.getStats().getCompleted() / 10);

    if ( my_flag != null ){
    	PieUtils.drawPie(gcBuffer, my_flag, me_hit_x, me_hit_y,OWN_SIZE,OWN_SIZE,manager.getStats().getCompleted() / 10, false );
    }
    
    gcBuffer.dispose();
    GC gcPanel = new GC(panel);
    gcPanel.drawImage(buffer,0,0);
    gcPanel.dispose();
    buffer.dispose();   
  }
  
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
  public void peerManagerAdded(PEPeerManager manager) {}
  public void peerManagerRemoved(PEPeerManager manager) {}
  
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

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = event.getView();
      	swtView.setTitle(MessageText.getString(getData()));
      	swtView.setToolBarListener(this);
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(MessageText.getString(getData()));
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
      	dataSourceChanged(event.getData());
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
        	String id = "DMDetails_Swarm";
        	
        	setFocused( true );	// do this before next code as it can pick up the corrent 'manager' ref
  	      
	      	if (manager != null) {
	      		if (manager.getTorrent() != null) {
	  					id += "." + manager.getInternalName();
	      		} else {
	      			id += ":" + manager.getSize();
	      		}
	      	}
	  
	      	SelectedContentManager.changeCurrentlySelectedContent(id, new SelectedContent[] {
	      		new SelectedContent(manager)
	      	});
	      	
	      break;
      case UISWTViewEvent.TYPE_FOCUSLOST:
    	  setFocused( false );
    	  break;
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
    }

    return true;
  }
	
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		return( ViewUtils.toolBarItemActivated(manager, item, activationType, datasource));
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		ViewUtils.refreshToolBarItems(manager, list);
	}
}
