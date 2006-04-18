/*
 * Created on Jul 29, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.networkmanager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.util.Debug;



import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.impl.*;
import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 *
 */
public class NetworkManager {
  public static final int UNLIMITED_RATE = 1024 * 1024 * 100; //100 mbyte/s
  
  private static final NetworkManager instance = new NetworkManager();

  protected static int tcp_mss_size;
  protected static int max_download_rate_bps;
  
  protected static int max_upload_rate_bps_normal;
  protected static int max_upload_rate_bps_seeding_only;
  protected static int max_upload_rate_bps;
  
  protected static boolean lan_rate_enabled;
  protected static int max_lan_upload_rate_bps;
  protected static int max_lan_download_rate_bps;
  
  protected static boolean seeding_only_mode_allowed;
  protected static boolean seeding_only_mode = false;  
  
  public static boolean 	REQUIRE_CRYPTO_HANDSHAKE;
  public static boolean 	INCOMING_HANDSHAKE_FALLBACK_ALLOWED;	
  public static boolean 	OUTGOING_HANDSHAKE_FALLBACK_ALLOWED;
	

  static {
  	COConfigurationManager.addAndFireParameterListeners(
  			new String[]{ "network.transport.encrypted.require",
    									"network.transport.encrypted.fallback.incoming",
    									"network.transport.encrypted.fallback.outgoing",
    									"LAN Speed Enabled",
    									"Max Upload Speed KBs",
    									"Max LAN Upload Speed KBs",
    									"Max Upload Speed Seeding KBs",
    									"enable.seedingonly.upload.rate",
    									"Max Download Speed KBs",
    									"Max LAN Download Speed KBs",
    									"network.tcp.mtu.size" },
    		new ParameterListener()	{
    			 public void  parameterChanged(	String ignore ) {
    				 REQUIRE_CRYPTO_HANDSHAKE				      = COConfigurationManager.getBooleanParameter( "network.transport.encrypted.require");
    				 INCOMING_HANDSHAKE_FALLBACK_ALLOWED	= COConfigurationManager.getBooleanParameter( "network.transport.encrypted.fallback.incoming");
    				 OUTGOING_HANDSHAKE_FALLBACK_ALLOWED	= COConfigurationManager.getBooleanParameter( "network.transport.encrypted.fallback.outgoing");
    				 
    				 max_upload_rate_bps_normal = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) * 1024;
    				 if( max_upload_rate_bps_normal < 1024 )  max_upload_rate_bps_normal = UNLIMITED_RATE;
    				 if( max_upload_rate_bps_normal > UNLIMITED_RATE )  max_upload_rate_bps_normal = UNLIMITED_RATE;
    				 
    				 max_lan_upload_rate_bps = COConfigurationManager.getIntParameter( "Max LAN Upload Speed KBs" ) * 1024;
    				 if( max_lan_upload_rate_bps < 1024 )  max_lan_upload_rate_bps = UNLIMITED_RATE;
    				 if( max_lan_upload_rate_bps > UNLIMITED_RATE )  max_lan_upload_rate_bps = UNLIMITED_RATE;
    	        
    				 max_upload_rate_bps_seeding_only = COConfigurationManager.getIntParameter( "Max Upload Speed Seeding KBs" ) * 1024;
    				 if( max_upload_rate_bps_seeding_only < 1024 )  max_upload_rate_bps_seeding_only = UNLIMITED_RATE;
    				 if( max_upload_rate_bps_seeding_only > UNLIMITED_RATE )  max_upload_rate_bps_seeding_only = UNLIMITED_RATE;
    	        
    				 seeding_only_mode_allowed = COConfigurationManager.getBooleanParameter( "enable.seedingonly.upload.rate" );
    				 
    				 max_download_rate_bps = COConfigurationManager.getIntParameter( "Max Download Speed KBs" ) * 1024;
    				 if( max_download_rate_bps < 1024 )  max_download_rate_bps = UNLIMITED_RATE;
    				 if( max_download_rate_bps > UNLIMITED_RATE )  max_download_rate_bps = UNLIMITED_RATE;
    	        
    				 lan_rate_enabled = COConfigurationManager.getBooleanParameter("LAN Speed Enabled");
    				 max_lan_download_rate_bps = COConfigurationManager.getIntParameter( "Max LAN Download Speed KBs" ) * 1024;
    				 if( max_lan_download_rate_bps < 1024 )  max_lan_download_rate_bps = UNLIMITED_RATE;
    				 if( max_lan_download_rate_bps > UNLIMITED_RATE )  max_lan_download_rate_bps = UNLIMITED_RATE;
    	        
    				 tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
    	        
    				 refreshRates();
    			 }
    		});
  }

  
  private final ConnectDisconnectManager connect_disconnect_manager = new ConnectDisconnectManager();
  private final IncomingSocketChannelManager incoming_socketchannel_manager = new IncomingSocketChannelManager();
  private final WriteController write_controller = new WriteController();
  private final ReadController read_controller = new ReadController();

  
  private final TransferProcessor upload_processor = new TransferProcessor( TransferProcessor.TYPE_UPLOAD, new LimitedRateGroup(){
    public int getRateLimitBytesPerSecond() {  return max_upload_rate_bps;  }
  });
  private final TransferProcessor download_processor = new TransferProcessor( TransferProcessor.TYPE_DOWNLOAD, new LimitedRateGroup(){
    public int getRateLimitBytesPerSecond() {  return max_download_rate_bps;  }
  });
  
  
  private final TransferProcessor lan_upload_processor = new TransferProcessor( TransferProcessor.TYPE_UPLOAD, new LimitedRateGroup(){
    public int getRateLimitBytesPerSecond() {  return max_lan_upload_rate_bps;  }
  });
  private final TransferProcessor lan_download_processor = new TransferProcessor( TransferProcessor.TYPE_DOWNLOAD, new LimitedRateGroup(){
    public int getRateLimitBytesPerSecond() {  return max_lan_download_rate_bps;  }
  });
  
  
  private final LimitedRateGroup unlimited_rate_group = new LimitedRateGroup() {
  	public int getRateLimitBytesPerSecond() {  return 0;  }
  };
  
  private NetworkManagerStats	stats = new NetworkManagerStats();
  
  
  private NetworkManager() {
    /* nothing */    
  }
  
  

  
  private static void refreshRates() {
    if( isSeedingOnlyUploadRate() ) {
      max_upload_rate_bps = max_upload_rate_bps_seeding_only;
    }
    else {
      max_upload_rate_bps = max_upload_rate_bps_normal;
    }
    
    if( max_upload_rate_bps < 1024 ) {
      Debug.out( "max_upload_rate_bps < 1024=" +max_upload_rate_bps);
    }
    
    //ensure that mss isn't greater than up/down rate limits
    if( tcp_mss_size > max_upload_rate_bps )  tcp_mss_size = max_upload_rate_bps - 1;
    if( tcp_mss_size > max_download_rate_bps )  tcp_mss_size = max_download_rate_bps - 1;
    if( tcp_mss_size > max_lan_upload_rate_bps )  tcp_mss_size = max_lan_upload_rate_bps -1;
    if( tcp_mss_size > max_lan_download_rate_bps )  tcp_mss_size = max_lan_download_rate_bps -1;
    
    if( tcp_mss_size < 512 )  tcp_mss_size = 512; 
  }
  
  
  public static boolean isSeedingOnlyUploadRate() {
    return seeding_only_mode_allowed && seeding_only_mode;
  }
  
  public static int getMaxUploadRateBPSNormal() {
    if( max_upload_rate_bps_normal == UNLIMITED_RATE )  return 0;
    return max_upload_rate_bps_normal;
  }
  
  public static int getMaxUploadRateBPSSeedingOnly() {
    if( max_upload_rate_bps_seeding_only == UNLIMITED_RATE )  return 0;
    return max_upload_rate_bps_seeding_only;
  }
  
  public static int getMaxDownloadRateBPS() {
    if( max_download_rate_bps == UNLIMITED_RATE )  return 0;
    return max_download_rate_bps; 
  }
  
  
  
  public void initialize() {
    AzureusCoreFactory.getSingleton().getGlobalManager().addListener( new GlobalManagerListener() {
      public void downloadManagerAdded( DownloadManager dm ){}
      public void downloadManagerRemoved( DownloadManager dm ){}
      public void destroyInitiated(){}
      public void destroyed(){}

      public void seedingStatusChanged( boolean seeding_only ) {
        seeding_only_mode = seeding_only;
        refreshRates();
      }
    });
  }
  
  
  
  /**
   * Get the singleton instance of the network manager.
   * @return the network manager
   */
  public static NetworkManager getSingleton() {  return instance;  }
  
  
  
  /**
   * Create a new unconnected remote network connection (for outbound-initiated connections).
   * @param remote_address to connect to
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   * @return a new connection
   */
  public NetworkConnection createConnection( InetSocketAddress remote_address, MessageStreamEncoder encoder, MessageStreamDecoder decoder, boolean connect_with_crypto, boolean allow_fallback, byte[] shared_secret ) { 
    return NetworkConnectionFactory.create( remote_address, encoder, decoder, connect_with_crypto, allow_fallback, shared_secret );
  }
  
  
  
  /**
   * Request the acceptance and routing of new incoming connections that match the given initial byte sequence.
   * @param matcher initial byte sequence used for routing
   * @param listener for handling new inbound connections
   * @param factory to use for creating default stream encoder/decoders
   */
  public void requestIncomingConnectionRouting( ByteMatcher matcher, final RoutingListener listener, final MessageStreamFactory factory ) {
    incoming_socketchannel_manager.registerMatchBytes( matcher, new IncomingSocketChannelManager.MatchListener() {
      public boolean
      autoCryptoFallback()
      {
    	return( listener.autoCryptoFallback());
      }
      public void connectionMatched( TCPTransportHelperFilter filter, ByteBuffer read_so_far ) {
        listener.connectionRouted( NetworkConnectionFactory.create( filter, read_so_far, factory.createEncoder(), factory.createDecoder() ) );
      }
    });
  }
  
  
  /**
   * Cancel a request for inbound connection routing.
   * @param matcher byte sequence originally used to register
   */
  public void cancelIncomingConnectionRouting( ByteMatcher matcher ) {
    incoming_socketchannel_manager.deregisterMatchBytes( matcher );
  }
  

  
  
  /**
   * Add an upload entity for write processing.
   * @param entity to add
   */
  public void addWriteEntity( RateControlledEntity entity ) {
    write_controller.addWriteEntity( entity );
  }
  
  
  /**
   * Remove an upload entity from write processing.
   * @param entity to remove
   */
  public void removeWriteEntity( RateControlledEntity entity ) {
    write_controller.removeWriteEntity( entity );
  }
  
  
  /**
   * Add a download entity for read processing.
   * @param entity to add
   */
  public void addReadEntity( RateControlledEntity entity ) {
    read_controller.addReadEntity( entity );
  }
  
  
  /**
   * Remove a download entity from read processing.
   * @param entity to remove
   */
  public void removeReadEntity( RateControlledEntity entity ) {
    read_controller.removeReadEntity( entity );
  }  
  
  
  

  /**
   * Get the manager for new incoming socket channel connections.
   * @return manager
   */
  public IncomingSocketChannelManager getIncomingSocketChannelManager() {  return incoming_socketchannel_manager;  }
  

  /**
   * Get the socket channel connect / disconnect manager.
   * @return connect manager
   */
  public ConnectDisconnectManager getConnectDisconnectManager() {  return connect_disconnect_manager;  }
  
  
  /**
   * Asynchronously close the given socket channel.
   * @param channel to close
   */
  public void closeSocketChannel( SocketChannel channel ) {
    connect_disconnect_manager.closeConnection( channel );
  }
  
  public void closeSocketChannel( SocketChannel channel, int delay ) {
	    connect_disconnect_manager.closeConnection( channel, delay );
  }
  
  
  /**
   * Get the virtual selector used for socket channel read readiness.
   * @return read readiness selector
   */
  public VirtualChannelSelector getReadSelector() {  return read_controller.getReadSelector();  }
  
  
  /**
   * Get the virtual selector used for socket channel write readiness.
   * @return write readiness selector
   */
  public VirtualChannelSelector getWriteSelector() {  return write_controller.getWriteSelector();  }
  
  
  /**
   * Register peer connection for network upload and download handling.
   * NOTE: The given max rate limits are ignored until the connection is upgraded.
   * NOTE: The given max rate limits are ignored for LANLocal connections.
   * @param peer_connection to register for network transfer processing
   * @param upload_group upload rate limit group
   * @param download_group download rate limit group
   */
  public void startTransferProcessing( NetworkConnection peer_connection, LimitedRateGroup upload_group, LimitedRateGroup download_group ) {
  	if( peer_connection.isLANLocal() && lan_rate_enabled ) {
  		lan_upload_processor.registerPeerConnection( peer_connection, unlimited_rate_group );
  		lan_download_processor.registerPeerConnection( peer_connection, unlimited_rate_group );
  	}
  	else {
  		upload_processor.registerPeerConnection( peer_connection, upload_group );
  		download_processor.registerPeerConnection( peer_connection, download_group );
  	}
  }
  
  
  /**
   * Cancel network upload and download handling for the given connection.
   * @param peer_connection to cancel
   */
  public void stopTransferProcessing( NetworkConnection peer_connection ) {
  	if( lan_upload_processor.isRegistered( peer_connection )) {
  		lan_upload_processor.deregisterPeerConnection( peer_connection );
  		lan_download_processor.deregisterPeerConnection( peer_connection );
  	}
  	else {
  		upload_processor.deregisterPeerConnection( peer_connection );
  		download_processor.deregisterPeerConnection( peer_connection );
  	}
  }
  
  
  /**
   * Upgrade the given connection to high-speed network transfer handling.
   * @param peer_connection to upgrade
   */
  public void upgradeTransferProcessing( NetworkConnection peer_connection ) {
	  if( lan_upload_processor.isRegistered( peer_connection )) {
  		lan_upload_processor.upgradePeerConnection( peer_connection );
  		lan_download_processor.upgradePeerConnection( peer_connection );
  	}
  	else {
  		upload_processor.upgradePeerConnection( peer_connection );
  		download_processor.upgradePeerConnection( peer_connection );
  	}
  }

  /**
   * Downgrade the given connection back to a normal-speed network transfer handling.
   * @param peer_connection to downgrade
   */
  public void downgradeTransferProcessing( NetworkConnection peer_connection ) {
	  if( lan_upload_processor.isRegistered( peer_connection )) {
  		lan_upload_processor.downgradePeerConnection( peer_connection );
  		lan_download_processor.downgradePeerConnection( peer_connection );
  	}
  	else {
  		upload_processor.downgradePeerConnection( peer_connection );
  		download_processor.downgradePeerConnection( peer_connection );
  	}
  }
  
  
  
  /**
   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
   * connections, or 1452 (1492-40) for PPPOE connections.
   * @return mss size in bytes
   */
  public static int getTcpMssSize() {  return tcp_mss_size;  }
  
  
  /**
   * Get port that the TCP server socket is listening for incoming connections on.
   * @return port number
   */
  public int getTCPListeningPortNumber() {  return incoming_socketchannel_manager.getTCPListeningPortNumber();  }
  
  
  public NetworkManagerStats
  getStats()
  {
	  return( stats );
  }
  
  
  /**
   * Byte stream match filter for routing.
   */
  public interface ByteMatcher {
    /**
     * Get the number of bytes this matcher requires.
     * @return size in bytes
     */
    public int size();
    
    /**
     * Get the minimum number of bytes required to determine if this matcher applies
     * @return
     */
    
    public int minSize();
    
    /**
     * Check byte stream for match.
     * @param to_compare
     * @return true if a match, false if not a match
     */
    public boolean matches( ByteBuffer to_compare, int port );
    
    /**
     * Check for a minimum match
     * @param to_compare
     * @return
     */
    public boolean minMatches( ByteBuffer to_compare, int port );
    
    /**
     * Returns true if this shared secret is recognised by the matcher
     * @param shared_secret
     * @return
     */
    public byte[] getSharedSecret();
  }
  
  
  
  /**
   * Listener for routing events.
   */
  public interface RoutingListener {
	  
	  /**
	   * Currently if message crypto is on and default fallback for incoming not
	   * enabled then we would bounce incoming messages from non-crypto transports
	   * For example, NAT check
	   * This method allows auto-fallback for such transports
	   * @return
	   */
	public boolean
	autoCryptoFallback();
	
    /**
     * The given incoming connection has been accepted.
     * @param connection accepted
     */
    public void connectionRouted( NetworkConnection connection );
  }
  
}
