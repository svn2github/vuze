/*
 * Created on Apr 2, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.peer.impl.transport.base;


import java.util.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.peer.impl.transport.sharedport.*;

/**
 * Selector singleton for handling new outgoing connections.
 */
public class OutgoingConnector {
  
  private static OutgoingConnector connector;
  private final ArrayList connectionsToAdd;
  private final HashMap connectionsOutstanding;
  private final SelectorGuard selectorGuard;
  public static final int TIMEOUT = 120*1000;	  //2min

  
  
  
  private OutgoingConnector() {
    connectionsToAdd = new ArrayList();
    connectionsOutstanding = new HashMap();
    selectorGuard = new SelectorGuard(50, 100);
    
    Thread loop = new Thread("Outgoing socket connector") {
      public void run() {
        mainLoop();
      }
    };
    loop.setDaemon( true );
    loop.start();
    
  }
  
  private static synchronized OutgoingConnector getInstance() {
    if ( connector == null )  connector = new OutgoingConnector();
    return connector;
  }
  
  
  private void mainLoop() {
    
    long prevCheckTime = 0;
    
    Selector selector = null;
    try {
      selector = Selector.open();
    }
    catch (Exception e) { e.printStackTrace(); }
    
    
    while (true) {
      
      //add new connections
      synchronized( connectionsToAdd ) {
        for (int i=0; i < connectionsToAdd.size(); i++) {
          Connection conn = (Connection)connectionsToAdd.get( i );
          try {
            SelectionKey key = conn.channel.register( selector, SelectionKey.OP_CONNECT );
            conn.startTime = System.currentTimeMillis();
            connectionsOutstanding.put( key, conn );
          }
          catch (Exception e) {
            e.printStackTrace();
            conn.listener.done();
          }
        }
        connectionsToAdd.clear();
      }
      
      //do the select
      try {
        int count = selector.select(1000);
        if ( !selectorGuard.isSelectorOK( count )) {
          selector = selectorGuard.repairSelector( selector );
        }
      }
      catch (Exception e) { e.printStackTrace(); }
      
      //notify of connection operation completed
      for (Iterator i = selector.selectedKeys().iterator(); i.hasNext();) {
        SelectionKey key = (SelectionKey)i.next();  i.remove();
        key.cancel();
        Connection conn = (Connection)connectionsOutstanding.remove( key );
        conn.listener.done();
      }
      
      //check for timeout'd connection attempts
      long currTime = System.currentTimeMillis();
      if ( currTime - prevCheckTime > 1000 ) {
        prevCheckTime = currTime;
        for (Iterator i = connectionsOutstanding.keySet().iterator(); i.hasNext();) {
          SelectionKey key = (SelectionKey)i.next();
          Connection conn = (Connection)connectionsOutstanding.get( key );
          if ( currTime - conn.startTime > TIMEOUT ) {
            key.cancel();
            connectionsOutstanding.remove( key );
            conn.listener.done();
          }
        }
      }
      
    }
  }
  
  
  
  private static class Connection {
    private final SocketChannel channel;
    private final OutgoingConnectorListener listener;
    private long startTime;
    
    private Connection( SocketChannel channel, OutgoingConnectorListener listener ) {
      this.channel = channel;
      this.listener = listener;
    }
  }
  
  
  public interface OutgoingConnectorListener {
    /**
     * Called when the connection operation completes (OP_CONNECT)
     * or times out.
     * NOTE:
     * Run finishConnect() on the socket to determine
     * connect failure/success/timeout.
     */
    public void done();
  }
  
  
  /**
   * Add a socket to the connection selector, which will call the listener once
   * the connection operation completes/ timeouts.
   * NOTE:
   * SocketChannel.connect() must be run on the socket before adding.
   */
  public static void addConnection( SocketChannel channel, OutgoingConnectorListener listener ) {
    ArrayList list = OutgoingConnector.getInstance().connectionsToAdd;
    synchronized( list ) {
      Connection conn =  new Connection( channel, listener );
      list.add( conn );
    }
  }
  
}
