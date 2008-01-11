/*
 * Created on 18 Sep 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.azureus.AZStylePeerExchange;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;
import com.aelitis.azureus.core.peermanager.peerdb.PeerExchangerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;

/**
 * @author Allan Crooks
 *
 * Largely copied from AZPeerExchange.
 */
public class UTPeerExchange implements AZStylePeerExchange, LTMessage {
	
	// Debug flag for testing purposes - currently disabled by default.
    public static final boolean ENABLED = true; 
	
	  private static final LogIDs LOGID = LogIDs.NET;

	  private static final byte bss = DirectByteBuffer.SS_MSG;

	  private DirectByteBuffer buffer = null;
	  private String description = null;
	  
	  private final byte version;
	  private final PeerItem[] peers_added;
	  private final PeerItem[] peers_dropped;
	  
	  public UTPeerExchange(PeerItem[] _peers_added, PeerItem[] _peers_dropped, byte version ) {
	    this.peers_added = _peers_added;
	    this.peers_dropped = _peers_dropped;
	    this.version = version;
	  }
	  
	  private void insertPeers(String key_name, Map root_map, boolean include_flags, PeerItem[] peers) {
	    if( peers != null && peers.length > 0 ) {
	      byte[] raw_peers = new byte[peers.length * 6];
	      byte[] peer_flags = (include_flags) ? new byte[peers.length] : null;
	      
	      for( int i=0; i < peers.length; i++ ) {
	    	  
	    	// This will break with IPv6 peers.
	        byte[] serialised_peer = peers[i].getSerialization();
	        System.arraycopy(serialised_peer, 0, raw_peers, i * 6, 6);
	        if (peer_flags != null && NetworkManager.getCryptoRequired(peers[i].getCryptoLevel())) {
	        	peer_flags[i] |= 0x01; // Encrypted connection. 
	        }
	        // 0x02 indicates if the peer is a seed, but that's difficult to determine
	        // so we'll leave it.
	      }
	      root_map.put(key_name, raw_peers);
	      if (peer_flags != null) {
	    	  root_map.put(key_name + ".f", peer_flags);
	      }
	    }
	  }
	  
	  private PeerItem[] extractPeers(String key_name, Map root_map) {
	    PeerItem[] return_peers = null;
	    ArrayList peers = new ArrayList();

	    byte[] raw_peer_data = (byte[])root_map.get(key_name);
	    if( raw_peer_data != null ) {
	    	if (raw_peer_data.length % 6 != 0) {
	    		if (Logger.isEnabled())
	    			Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, "PEX (UT): peer data size not multiple of 6: " + raw_peer_data.length));
	    	}
	      int peer_num = raw_peer_data.length / 6;
	      byte[] flags = (root_map == null) ? null : (byte[])root_map.get(key_name + ".f");
	      if (flags != null && flags.length != peer_num) {
	    	  if (flags.length > 0) {
	    		  if (Logger.isEnabled()) {
	    			  Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, "PEX (UT): invalid peer flags: peers=" + peer_num + ", flags=" + flags.length ));
	    		  }
	    	  }
	    	  flags = null;
	      }
	      
	      for (int i=0; i<peer_num; i++) {
	    	  byte[] full_address = new byte[6];
	    	  System.arraycopy(raw_peer_data, i * 6, full_address, 0, 6);
	    	  byte type = PeerItemFactory.HANDSHAKE_TYPE_PLAIN;        
	    	  if (flags != null && (flags[i] & 0x01) == 0x01) {
	    		  type = PeerItemFactory.HANDSHAKE_TYPE_CRYPTO;
	    	  }
	    	  try {
	    		  PeerItem peer = PeerItemFactory.createPeerItem(full_address, PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE, type, 0);
	    		  peers.add(peer);
	    	  }
	    	  catch (Exception e) {
	    		  if (Logger.isEnabled())
	    		  	Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, "PEX (UT): invalid peer received"));	 
		      }
	      }
	      
	      if(!peers.isEmpty()) {
	    	  return_peers = new PeerItem[peers.size()];
	    	  peers.toArray(return_peers);
	      }
	    }
	    return return_peers;
	  }
	  
	  public PeerItem[] getAddedPeers() {  return peers_added;  }
	  public PeerItem[] getDroppedPeers() {  return peers_dropped;  }
	  public String getID() {  return LTMessage.ID_UT_PEX;  }
	  public byte[] getIDBytes() {  return LTMessage.ID_UT_PEX_BYTES;  }
	  public String getFeatureID() {  return LTMessage.LT_FEATURE_ID;  }  
	  public int getFeatureSubID() { return LTMessage.SUBID_UT_PEX;  }
	  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
	  public byte getVersion() { return version; };
	  
	  public String getDescription() {
	    if( description == null ) {
	      int add_count = peers_added == null ? 0 : peers_added.length;
	      int drop_count = peers_dropped == null ? 0 : peers_dropped.length;
	      
	      description = getID().toUpperCase() + " with " +add_count+ " added and " +drop_count+ " dropped peers";
	    }
	    
	    return description;
	  }
	  
	  
	  public DirectByteBuffer[] getData() {
	    if( buffer == null ) {
	      Map payload_map = new HashMap();
	      insertPeers("added", payload_map, true, peers_added );
	      insertPeers("dropped", payload_map, false, peers_dropped );
	      buffer = MessagingUtil.convertPayloadToBencodedByteStream(payload_map, DirectByteBuffer.AL_MSG_UT_PEX);
	      if( buffer.remaining( bss ) > 2000 )  System.out.println( "Generated UTPeerExchange size = " +buffer.remaining( bss )+ " bytes" );
	    }
	    
	    return new DirectByteBuffer[]{ buffer };
	  }
	  
	  
	  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {
	    if( data.remaining( bss ) > 2000 )  System.out.println( "Received UT-PEX msg byte size = " +data.remaining( bss ) );
	    Map root = MessagingUtil.convertBencodedByteStreamToPayload(data, 2, getID());
	    PeerItem[] added = extractPeers("added", root);
	    PeerItem[] dropped = extractPeers("dropped", root);
	      
	    //if( added == null && dropped == null )  throw new MessageException( "[" +getID()+ "] received exchange message without any adds or drops" );
	    return new UTPeerExchange(added, dropped, version);
	  }
	  
	  
	  public void destroy() {
	    if( buffer != null )  buffer.returnToPool();
	  }

	  /**
	   * Arbitrary value - most clients are configured to about 100 or so...
	   * We'll allow ourselves to be informed about 200 connected peers from
	   * the initial handshake, and then cap either list to about 100.
	   * 
	   * These values are plucked from the air really - although I've seen PEX
	   * sizes where the added list is about 300 (sometimes), most contain a
	   * sensible number (not normally over 100).
	   * 
	   * Subsequent PEX messages are relatively small too, so we'll stick to
	   * smaller limits - 50 would be probably fine, but watching some big
	   * swarms over a short period, the biggest "added" list I saw was one
	   * containing 38 peers, so it's quite possible list sizes above 50 get
	   * sent out. So 100 is a safe-ish figure. 
	   */
	  public int getMaxAllowedPeersPerVolley(boolean initial, boolean added) {
		  return (initial && added) ? 200 : 100;
	  }
	  
}
