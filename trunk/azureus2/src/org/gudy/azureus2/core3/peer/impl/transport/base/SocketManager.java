/*
 * Created on Apr 6, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.peer.impl.transport.base;


import java.util.*;
import java.net.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.peer.impl.transport.sharedport.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.config.*;


/**
 * Handles socket connections.
 */
public class SocketManager {
  
  private static final SocketManager manager = new SocketManager();
  private final SelectorGuard guard = new SelectorGuard( 50, 100 );
  private final HashMap newConnectionRequests = new HashMap();
  private final HashMap pendingOutboundConnections = new HashMap();
  private final ArrayList connectionsToClose = new ArrayList();
  private final ArrayList listenersToClose = new ArrayList();
  private Selector selector = null;
  
  
  private SocketManager() {
    Thread loop = new Thread("SocketManager") {
      public void run() {
        mainLoop();
      }
    };
    loop.setDaemon( true );
    loop.start();
  }
  
  
  private void mainLoop() {
    try {
      selector = Selector.open();
      while( true ) {
        addNewOutboundRequests();
        runSelect();
        notificationOfConnects();
        doClosings();
      }
    }
    catch (Exception e) {
      System.out.println("SOCKETMANAGER: selector open b0rked");
      e.printStackTrace(); }
  }
  
  
  private void addNewOutboundRequests() {
    synchronized( newConnectionRequests ) {
      Set keys = newConnectionRequests.keySet();
      for (Iterator i = keys.iterator(); i.hasNext();) {
        InetSocketAddress address = (InetSocketAddress)i.next();
        OutboundConnectionListener listener = (OutboundConnectionListener)newConnectionRequests.get( address );
        SocketChannel channel = null;
        try {
          channel = SocketChannel.open();
          
          String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
          if ( bindIP.length() > 6 ) {
            channel.socket().bind(new InetSocketAddress(InetAddress.getByName(bindIP), 0));
          }
          
          channel.socket().setReceiveBufferSize( PEPeerTransport.RECEIVE_BUFF_SIZE );
          //channel.socket().setSendBufferSize( COConfigurationManager.getIntParameter("MTU.Size") );
          
          channel.configureBlocking( false );
          channel.connect( address );
          SelectionKey key = channel.register( selector, SelectionKey.OP_CONNECT );
          pendingOutboundConnections.put( key, listener );
        }
        catch (Throwable t) {
          //t.printStackTrace();
          listener.connectionDone( null, t.getMessage() );
          synchronized( connectionsToClose ) {
            connectionsToClose.add( channel );
          }
        }
      }
      newConnectionRequests.clear();
    }
  }
  
  
  private void runSelect() {
    try {
      int count = selector.select( 1000 );
      if ( !guard.isSelectorOK( count )) {
        selector = guard.repairSelector( selector );
      }
    }
    catch (Exception e) { e.printStackTrace(); }
  }
  
  
  private void notificationOfConnects() {
    for (Iterator i = selector.selectedKeys().iterator(); i.hasNext();) {
      SelectionKey key = (SelectionKey)i.next();  i.remove();
      SocketChannel channel = (SocketChannel)key.channel();
      OutboundConnectionListener listener = (OutboundConnectionListener)pendingOutboundConnections.remove( key );
      key.cancel();
            
      boolean canceled = false;
      synchronized( listenersToClose ) {
        if ( listenersToClose.contains( listener )) {
        	listenersToClose.remove( listener );
          canceled = true;
        }
      }
      
      if ( canceled ) {
      	synchronized( connectionsToClose ) {
      		connectionsToClose.add( channel );
      	} 
      }
      else {
      	try {
      		if ( channel.finishConnect() ) {
      			listener.connectionDone( channel, null );
      		}
      		else { //should never happen
      			listener.connectionDone( null, "finishConnect() failed" );
      			synchronized( connectionsToClose ) {
      				connectionsToClose.add( channel );
      			}
      		}
      	}
      	catch (Throwable t) {
          try {
          	listener.connectionDone( null, t.getMessage() );
          	synchronized( connectionsToClose ) {
          		connectionsToClose.add( channel );
          	}
          }
          catch (Throwable x) { x.printStackTrace(); }
      	}
      }
    }
  }
  
  
  private void doClosings() {
    synchronized( connectionsToClose ) {
      for (int i=0; i < connectionsToClose.size(); i++) {
        SocketChannel channel = (SocketChannel)connectionsToClose.get( i );
        if ( channel != null ) {
          try {
            channel.close();
          } catch (Exception e) { e.printStackTrace(); }
        }
      }
      connectionsToClose.clear();
    }
  }
  
  
  /**
   * If the connection attempt was successful, the listener will be called
   * with a valid socket channel, and a null error_msg.
   * If the connection attempt failed, the listener will be called with
   * a null socket channel, and error_msg will detail the specific error.
   */
  public interface OutboundConnectionListener {
    public void connectionDone( SocketChannel channel, String error_msg );
  }
  
  
  /**
   * Request that a new socket channel connection attempt be made. 
   * @param address the address you want to connect to
   * @param listener will be called when the connection attempt completes or fails
   */
  public static void requestOutboundConnection( InetSocketAddress address, OutboundConnectionListener listener ) {
    HashMap map = manager.newConnectionRequests;
    synchronized( map ) {
      map.put( address, listener );
    }
  }
  
  
  /**
   * Tell the manager to close the given connection.
   * @param channel socket channel to close
   */
  public static void closeConnection( SocketChannel channel ) {
    ArrayList list = manager.connectionsToClose;
    synchronized( list ) {
      list.add( channel );
    }
  }
  
  
  /**
   * Tell the manager to cancel the given connection request.
   * @param listener listener used in the initial request
   */
  public static void cancelOutboundRequest( OutboundConnectionListener listener ) {
  	ArrayList list = manager.listenersToClose;
    synchronized( list ) {
    	list.add( listener );
    }
  }

}
