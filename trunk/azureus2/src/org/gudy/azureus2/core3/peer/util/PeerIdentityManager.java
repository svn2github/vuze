/*
 * Created on Mar 18, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.peer.util;


import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;


/**
 * Maintains peer identity information.
 */
public class PeerIdentityManager {

  private static final AEMonitor 		class_mon	= new AEMonitor( "PeerIdentityManager:class");

  private static final Map 				dataMap = new HashMap();

  private static int totalIDs = 0;
  
  
    
  public static PeerIdentityDataID
  createDataID(
  	byte[]		data )
  {
  	PeerIdentityDataID	data_id = new PeerIdentityDataID( data );
  	
  	Map peerMap;
  	
    try{
        class_mon.enter();
      
        peerMap = (Map)dataMap.get( data_id );
        
        if( peerMap == null ){
        	
          peerMap = new HashMap();
          
          dataMap.put( data_id, peerMap );
        }
    }finally{
    	
    	class_mon.exit();
    }
	
	data_id.setPeerMap( peerMap );
	
	return( data_id );
  }
  
  //Main peer identity container.
  //Add new identity items (like pgp key, authentication user/pass, etc)
  //to this class if/when needed.
  private static class PeerIdentity {
    private final byte[] id;
    private final int hashcode;
    
    private PeerIdentity( byte[] _id ) {
      this.id = _id;
      this.hashcode = new String( id ).hashCode();
    }
    
    public boolean equals( Object obj ) {
      if (this == obj)  return true;
      if (obj != null && obj instanceof PeerIdentity) {
        PeerIdentity other = (PeerIdentity)obj;
        return Arrays.equals(this.id, other.id);
      }
      return false;
    }
    
    public int hashCode() {
      return hashcode;
    }
  }
  
  
  /**
   * Add a new peer identity to the manager.
   * @param data_id unique id for the data item associated with this connection
   * @param peer_id unique id for this peer connection
   * @param ip remote peer's ip address
   */
  public static void 
  addIdentity( PeerIdentityDataID data_id, byte[] peer_id, String ip ) {
     PeerIdentity peerID = new PeerIdentity( peer_id );
    
    try{
      class_mon.enter();
    
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap == null ) {
        peerMap = new HashMap();
        dataMap.put( data_id, peerMap );
      }
      
      Object old = peerMap.put( peerID, ip );
      if( old == null ) {
        totalIDs++;
      }
    }finally{
      class_mon.exit();
    }
  }
  
  
  /**
   * Remove a peer identity from the manager.
   * @param data_id id for the data item associated with this connection
   * @param peer_id id for this peer connection
   */
  public static void removeIdentity( PeerIdentityDataID data_id, byte[] peer_id ) {
     
    try{
    	class_mon.enter();
      
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        PeerIdentity peerID = new PeerIdentity( peer_id );
        
        Object old = peerMap.remove( peerID );
        if( old != null ) {
          totalIDs--;
        }
      }
    }finally{
    	class_mon.exit();
    }
  }
  

  /**
   * Check if the manager already has the given peer identity.
   * @param data_id id for the data item associated with this connection
   * @param peer_id id for this peer connection
   * @return true if the peer identity is found, false if not found
   */
  public static boolean containsIdentity( PeerIdentityDataID data_id, byte[] peer_id ) {
    PeerIdentity peerID = new PeerIdentity( peer_id );
    
    try{
    	class_mon.enter();
  
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        if( peerMap.containsKey( peerID ) ) {
          return true;
        }
      }
    }finally{
    	class_mon.exit();
    }
    
    return false;
  }
  
  
  /**
   * Get the total number of peer identities managed.
   * @return total number of peers over all data items
   */
  public static int getTotalIdentityCount() {
    return totalIDs;
  }
  
  
  /**
   * Get the total number of peer identities managed for the given data item.
   * @param data_id data item to count over
   * @return total number of peers for this data item
   */
  public static int 
  getIdentityCount( 
  	PeerIdentityDataID data_id )
  {
  	return( data_id.getPeerMap().size());
  }
  
  
  /**
   * Check if the given IP address is already present in the manager's
   * peer identity list for the given data item (i.e. check if there is
   * already a peer with that IP address).
   * @param data_id id for the data item associated with this connection
   * @param ip IP address to check for
   * @return true if the IP is found, false if not found
   */
  public static boolean containsIPAddress( PeerIdentityDataID data_id, String ip ) {
    
    try{
    	class_mon.enter();
   	  
      Map peerMap = (Map)dataMap.get( data_id );
      if( peerMap != null ) {
        if( peerMap.containsValue( ip ) ) {
          return true;
        }
      }
    }finally{
    	class_mon.exit();
    }
    
    return false;
  }

}
