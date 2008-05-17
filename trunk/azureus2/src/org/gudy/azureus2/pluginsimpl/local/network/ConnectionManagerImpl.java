/*
 * Created on Feb 9, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.network;

import java.net.InetSocketAddress;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.plugins.messaging.MessageStreamDecoder;
import org.gudy.azureus2.plugins.messaging.MessageStreamEncoder;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.network.Transport;
import org.gudy.azureus2.plugins.network.TransportCipher;
import org.gudy.azureus2.plugins.network.TransportException;
import org.gudy.azureus2.plugins.network.TransportFilter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamDecoderAdapter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamEncoderAdapter;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilterStreamCipher;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelper;

import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;


/**
 *
 */
public class ConnectionManagerImpl implements ConnectionManager {
  
  private static ConnectionManagerImpl instance;
  
  
  public static synchronized ConnectionManagerImpl 
  getSingleton(
	AzureusCore		core )
  {
	  if ( instance == null ){
		  
		  instance = new ConnectionManagerImpl( core );
	  }
	  
	  return( instance );
  }
  
  private AzureusCore		azureus_core;
  
  private ConnectionManagerImpl(AzureusCore _core) {
  
	  azureus_core	= _core;
  }
  

  public Connection 
  createConnection( 
	InetSocketAddress remote_address, 
	MessageStreamEncoder encoder, 
	MessageStreamDecoder decoder ) 
  {
	  ConnectionEndpoint connection_endpoint	= new ConnectionEndpoint( remote_address );
	  
	  connection_endpoint.addProtocol( new ProtocolEndpointTCP( remote_address ));
	 
	  com.aelitis.azureus.core.networkmanager.NetworkConnection core_conn =
		  NetworkManager.getSingleton().createConnection( connection_endpoint, new MessageStreamEncoderAdapter( encoder ), new MessageStreamDecoderAdapter( decoder ), false, false, null );
    
	  return new ConnectionImpl( core_conn );
  }
  
  public int
  getNATStatus()
  {
	  return( azureus_core.getGlobalManager().getNATStatus());
  }
  
  public TransportCipher createTransportCipher(String algorithm, int mode, SecretKeySpec key_spec, AlgorithmParameterSpec params) throws TransportException {
	  try {
		  com.aelitis.azureus.core.networkmanager.impl.TransportCipher cipher = new com.aelitis.azureus.core.networkmanager.impl.TransportCipher(algorithm, mode, key_spec, params);
		  return new TransportCipherImpl(cipher);
	  }
	  catch (Exception e) {
		  throw new TransportException(e);
	  }
  }
  
  public TransportFilter createTransportFilter(Transport transport, TransportCipher read_cipher, TransportCipher write_cipher) throws TransportException {
	  if (!(((TransportImpl)transport).core_transport instanceof TCPTransportImpl)) {
		  throw new TransportException("transport type not supported - " + ((TransportImpl)transport).core_transport);
	  }
	  
	  TCPTransportImpl core_transport = (TCPTransportImpl)((TransportImpl)transport).core_transport;
	  TCPTransportHelper helper = new TCPTransportHelper(core_transport.getSocketChannel());
	  TransportHelperFilterStreamCipher core_filter = new TransportHelperFilterStreamCipher(helper, ((TransportCipherImpl)read_cipher).cipher, ((TransportCipherImpl)write_cipher).cipher);
	  return new TransportFilterImpl(core_filter);
  }
  
}
