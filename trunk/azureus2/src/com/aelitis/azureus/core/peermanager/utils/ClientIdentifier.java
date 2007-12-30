package com.aelitis.azureus.core.peermanager.utils;

import org.gudy.azureus2.core3.internat.MessageText;

public class ClientIdentifier {
	
	public static String identifyBTOnly(String peer_id_client, byte[] handshake_bytes) {
		
		// BitThief check.
		if (peer_id_client.equals("Mainline 4.4.0") && (handshake_bytes[7] & (byte)1) == 0) {
			return asDiscrepancy("BitThief*", peer_id_client, "fake_client");
		}
		
		  // We do care if something is claiming to be Azureus when it isn't. If
		  // it's a recent version of Azureus, but doesn't support advanced messaging, we
		  // know it's a fake.
		  if (!peer_id_client.startsWith("Azureus ")) {return peer_id_client;}

		  // Older versions of Azureus won't have support, so discount these first.
		  String version = peer_id_client.substring(8);
		  if (version.startsWith("1") || version.startsWith("2.0") || 
				  version.startsWith("2.1") || version.startsWith("2.2")) {
			  return peer_id_client;
		  } 
		
		  // Must be a fake.
		  return asDiscrepancy(null, peer_id_client, "fake_client");
	}
	
	public static String identifyAZMP(String peer_id_client_name, String az_msg_client_name, String az_msg_client_version, byte[] peer_id) {
		  
		/**
		 * Hack for BitTyrant - the handshake resembles this:
		 *   Client: AzureusBitTyrant
		 *   ClientVersion: 2.5.0.0BitTyrant
		 *   
		 * Yuck - let's format it so it resembles something pleasant.
 	     */ 
		if (az_msg_client_name.endsWith("BitTyrant")) {
			  return "BitTyrant " + az_msg_client_version.replaceAll("BitTyrant", "") + " (Azureus Mod)";
		  }
		  
		  String msg_client_name = az_msg_client_name + " " + az_msg_client_version;
		  
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
		  String client_displayed_name = null;
		  boolean is_peer_id_azureus = peer_id_client_name.startsWith("Azureus ");
		  boolean is_msg_client_azureus = az_msg_client_name.equals("Azureus");
		  boolean is_fake = false;
		  boolean is_mismatch = true;
		  boolean is_peer_id_unknown = peer_id_client_name.startsWith(MessageText.getString("PeerSocket.unknown"));
		  
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
				  client_displayed_name = msg_client_name;
				  is_mismatch = false;
				  
				  // Log it though.
				  BTPeerIDByteDecoder.logClientDiscrepancy(peer_id_client_name, msg_client_name, "unknown_client", "AZMP", peer_id);
			  }
			  else {
				  // We've got a general mismatch, we don't know what client it is - in most cases.
				  
				  // Ares Galaxy sometimes uses the same peer ID as Arctic Torrent, so allow it to be
				  // overridden.
				  if (msg_client_name.startsWith("Ares") && peer_id_client.equals("ArcticTorrent")) {
					  return msg_client_name;
				  }
			  }
		  }
		  
		  String discrepancy_type;
		  if (is_fake) {discrepancy_type = "fake_client";}
		  else if (is_mismatch) {discrepancy_type = "mismatch_id";}
		  else {discrepancy_type = null;}
		  
		  if (discrepancy_type != null) {
			  return asDiscrepancy(client_displayed_name, peer_id_client, msg_client_name, discrepancy_type, "AZMP", peer_id);
		  }
		  
		  return client_displayed_name;
	  }
	  
	public static String identifyLTEP(String peer_id_name, String handshake_name, byte[] peer_id) {
		if (handshake_name == null) {return peer_id_name;}
		
		/**
		 * Official BitTorrent clients should still be shown as Mainline.
		 * This is to be consistent with previous Azureus behaviour.
		 */
		String handshake_name_to_process = handshake_name;
		if (handshake_name.startsWith("BitTorrent ")) {
			handshake_name_to_process = handshake_name.replaceFirst("BitTorrent", "Mainline");
		}
		
		if (peer_id_name.startsWith("\u00B5Torrent")) {

			// 1.6.0 misidentifies itself as 1.5 in the handshake.
			if (peer_id_name.equals("\u00B5Torrent 1.6.0")) {
				return peer_id_name;
			}

			// Older µTorrent versions will not always use the appropriate character for the
			// first letter, so compensate here.
			if (!handshake_name.startsWith("\u00B5Torrent") && handshake_name.startsWith("Torrent", 1)) {
				handshake_name_to_process = "\u00B5" + handshake_name.substring(1);
			}
			
			// Some versions indicate they are the beta version in the peer ID, but not in the
			// handshake - we prefer to keep the beta identifier.
			if (peer_id_name.endsWith("Beta") && peer_id_name.startsWith(handshake_name_to_process)) {
				return peer_id_name;
			}
		}

		// Some Mainline 4.x versions identify themselves as µTorrent - according to alus,
		// this was a bug, so just identify as Mainline.
		if (peer_id_name.startsWith("Mainline 4.") && handshake_name.startsWith("Torrent", 1)) {
			return peer_id_name;
		}
		
		// Transmission 0.96 still uses 0.95 in the LT handshake, so cope with that and just display
		// 0.96.
		if (peer_id_name.equals("Transmission 0.96") && handshake_name.equals("Transmission 0.95")) {
			return peer_id_name;
		}
			
		// We allow a client to have a different version number than the one decoded from
		// the peer ID. Some clients separate version and client name using a forward slash,
		// so we split on that as well.
		String client_type_peer = peer_id_name.split(" ", 2)[0];
		String client_type_handshake = handshake_name_to_process.split(" ", 2)[0].split("/", 2)[0];
		
		if (client_type_peer.toLowerCase().equals(client_type_handshake.toLowerCase())) {return handshake_name_to_process;}
		
		// Bloody XTorrent.
		if (handshake_name_to_process.equals("Transmission 0.7-svn") && client_type_peer.equals("Azureus")) {
			return asDiscrepancy("XTorrent", peer_id_name, handshake_name, "fake_client", "LTEP", peer_id);
		}
		
		// Bloody Xtorrent.
		if (handshake_name_to_process.startsWith("Transmission") && client_type_peer.startsWith("XTorrent")) {
			return asDiscrepancy(client_type_peer, handshake_name_to_process, "fake_client");
		}
		
		// Like we do with AZMP peers, allow the handshake to define the client even if we can't extract the
		// name from the peer ID, but log it so we can possibly identify it in future.
		if (peer_id_name.startsWith(MessageText.getString("PeerSocket.unknown"))) {
			BTPeerIDByteDecoder.logClientDiscrepancy(peer_id_name, handshake_name, "unknown_client", "LTEP", peer_id);
			return handshake_name_to_process;
		}
		
		/**
		 * libtorrent is... unsurprisingly... a torrent library. Many clients use it, so cope with clients
		 * which don't identify themselves through the peer ID, but *do* identify themselves through the
		 * handshake.
		 */
		if (peer_id_name.startsWith("libtorrent (Rasterbar)")) {
			if (handshake_name_to_process.toLowerCase().indexOf("libtorrent") == -1) {
				handshake_name_to_process += " (" + peer_id_name + ")";
			}
			return handshake_name_to_process;
		}
		
		// Can't determine what the client is.
		return asDiscrepancy(null, peer_id_name, handshake_name, "mismatch_id", "LTEP", peer_id);
	}
	  
	  private static String asDiscrepancy(String client_name, String peer_id_name, String handshake_name, String discrepancy_type, String protocol_type, byte[] peer_id) {
		  if (client_name == null) {
			  BTPeerIDByteDecoder.logClientDiscrepancy(peer_id_name, handshake_name, discrepancy_type, protocol_type, peer_id);
		  }
		  return asDiscrepancy(client_name, peer_id_name + "\" / \"" + handshake_name, discrepancy_type);
	  }
	  
	  private static String asDiscrepancy(String real_client, String dodgy_client, String discrepancy_type) {
		  if (real_client == null) {
			  real_client = MessageText.getString("PeerSocket.unknown");
		  }
		  return real_client + " [" +
		  	MessageText.getString("PeerSocket." + discrepancy_type) + ": \"" + dodgy_client + "\"]"; 
	  }
}
