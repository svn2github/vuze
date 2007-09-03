package com.aelitis.azureus.core.peermanager.utils;

import org.gudy.azureus2.core3.internat.MessageText;

public class ClientIdentifier {
	
	  /**
	   * This method is used to determine the client name for peers which don't talk
	   * over an extension protocol.
	   */
	  public static String getSimpleClientName(String client) {

		  /** 
		   * We don't really care about overriding the client name derived by the
		   * peer decoder. However, we do care if something is claiming to be
		   * Azureus when it isn't. If it's a recent version of Azureus, but doesn't
		   * support advanced messaging, we know it's a fake.
		   */
		  if (!client.startsWith("Azureus ")) {return client;}
		  
		  // Older versions of Azureus won't have support, so discount these first.
		  String version = client.substring(8);
		  if (version.startsWith("1") || version.startsWith("2.0") || 
				  version.startsWith("2.1") || version.startsWith("2.2")) {
			  return client;
		  }

		  // Must be a fake.
		  return MessageText.getString("PeerSocket.unknown") + " [" +
		  	MessageText.getString("PeerSocket.fake_client") + ": \"" + client + "\"]"; 
		  
	  }

	  /**
	   * This method is used to determine the client name to specify for Azureus
	   * messaging protocol peers.
	   */
	  public static String getExtendedClientName(final String peer_id_client_name,
			  final String az_msg_client_name, final String az_msg_client_version) {
		  
			/**
			 * Hack for BitTyrant - the handshake resembles this:
			 *   Client: AzureusBitTyrant
			 *   ClientVersion: 2.5.0.0BitTyrant
			 *   
			 * Yuck - let's format it so it resembles something pleasant.
		   */ 
		  if (az_msg_client_name.endsWith("BitTyrant")) {
			  return "Azureus BitTyrant " + az_msg_client_version.replaceAll("BitTyrant", "");
		  }
		  
		  final String msg_client_name = az_msg_client_name + " " + az_msg_client_version;
		  
		  /**
		   * Do both names seem to match?
		   */
		  if (msg_client_name.equals(peer_id_client_name)) {return msg_client_name;}
		  
		  /**
		   * There may be some discrepancy - a different version number perhaps.
		   * If the main client name still seems to be the same, then return the one
		   * given to us in the AZ handshake.
			 */ 
		  String peer_id_client = peer_id_client_name.split(" ", 2)[0];
		  String az_client_name = az_msg_client_name.split(" ", 2)[0];
		  if (peer_id_client.equals(az_client_name)) {
			  return msg_client_name;
		  }
		  
		  // There is an inconsistency. Let's try figuring out what we can.
		  String client_displayed_name = MessageText.getString("PeerSocket.unknown");
		  boolean is_peer_id_azureus = peer_id_client_name.startsWith("Azureus ");
		  boolean is_msg_client_azureus = az_msg_client_name.equals("Azureus");
		  boolean is_fake = false;
		  boolean is_mismatch = true;
		  boolean is_peer_id_unknown = peer_id_client_name.startsWith(client_displayed_name);
		  
		  if (is_peer_id_azureus) {
			  
			  // Shouldn't happen.
			  if (is_msg_client_azureus) {
				  throw new RuntimeException("logic error in getExtendedClientName - both clients are Azureus");
			  }
			  else {
				  // We've got a peer ID that says Azureus, but it doesn't say Azureus in the handshake.
				  // It's definitely fake.
				  is_fake = true;
				  
				  // It might be XTorrent - it does use AZ2504 in the peer ID and "Transmission 0.7-svn"
				  // in the handshake.
				  if (msg_client_name.equals("Transmission 0.7-svn")) {client_displayed_name = "XTorrent";}
			  }
		  }
		  else {
			  if (is_msg_client_azureus) {is_fake = true;}
			  else if (is_peer_id_unknown) {
				  // Our peer ID decoding can't decode it, but the client identifies itself anyway.
				  // In that case, we won't say that it is a mismatch, and we'll just use the name
				  // provided to us.
				  //
				  // TODO: Log this information somewhere.
				  client_displayed_name = msg_client_name;
				  is_mismatch = false;
			  }
			  else {
				  // We've got a general mismatch, we don't know what client it is.
				  // TODO: Log this.
			  }
		  }
		  
		  String discrepancy_type;
		  if (is_fake) {discrepancy_type = "fake_client";}
		  else if (is_mismatch) {discrepancy_type = "mismatch_id";}
		  else {discrepancy_type = null;}
		  
		  if (discrepancy_type != null) {
			  client_displayed_name += " [" + MessageText.getString("PeerSocket." + discrepancy_type) + ": " +
			      "\"" + peer_id_client_name + "\" / \"" + msg_client_name + "\"]";
		  }
		  
		  return client_displayed_name;
	  }
	  
	  
	  public static String getExtendedClientName(final String peerIdClientName, final String ltepClientName)
	  {
		  //TODO sanity checks?
		  return ltepClientName != null ? ltepClientName+" ("+peerIdClientName+")" : peerIdClientName;
		  
	  }

	  
}
