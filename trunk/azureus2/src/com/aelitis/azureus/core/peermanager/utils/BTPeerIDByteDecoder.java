/*
 * Created on Nov 12, 2003
 * Created by Alon Rohter
 * Copyright (C) 2003-2004 Alon Rohter, All Rights Reserved.
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.peermanager.utils;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import java.io.*;


/**
 * Used for identifying clients by their peerID.
 */
public class BTPeerIDByteDecoder {

	final static boolean LOG_UNKNOWN;

	static {
		String	prop = System.getProperty("log.unknown.peerids");
		LOG_UNKNOWN = prop != null && prop.equals("1");
	}

	private static void logUnknownClient0(byte[] peer_id_bytes, Writer log) throws IOException {
		String text = new String(peer_id_bytes, 0, 20, Constants.BYTE_ENCODING);
		text = text.replace((char)12, (char)32);
		text = text.replace((char)10, (char)32);

		log.write("[" + text + "] "); // Readable
		log.write(ByteFormatter.encodeString(peer_id_bytes) + " "); // Usable for assertion tests.
		for (int i=0; i < 20; i++) {
			log.write(i+"=" + peer_id_bytes[i] + " "); // In case we want the old format...
		}
		log.write("\n");
	}

	static void logUnknownClient(byte[] peer_id_bytes) {
		
		// Enable this block for now - just until we get more feedback about
		// unknown clients.
		if (Constants.isCVSVersion()) {
			Debug.out("Unable to decode peer correctly - peer ID bytes: " + ByteFormatter.encodeString(peer_id_bytes));
		}
		
		if (!LOG_UNKNOWN) {return;}
		FileWriter log = null;
		File log_file = FileUtil.getUserFile("identification.log");
		try {
			log = new FileWriter(log_file, true);
			logUnknownClient0(peer_id_bytes, log);
		}
		catch (Throwable e) {
			Debug.printStackTrace(e);
		}
		finally {
			try {if (log != null) log.close();}
			catch (IOException ignore) {/*ignore*/}
		}
	}

	static void logUnknownClient(String peer_id) {
		try {logUnknownClient(peer_id.getBytes(Constants.BYTE_ENCODING));}
		catch (UnsupportedEncodingException uee) {}
	}

	public static String decode0(byte[] peer_id_bytes) {
		String peer_id = null;
		try {peer_id = new String(peer_id_bytes, Constants.BYTE_ENCODING);}
		catch (UnsupportedEncodingException uee) {return "";}

		// We store the result here.
		String client = null;

		/**
		 * If the client reuses parts of the peer ID of other peers, then try to determine this
		 * first (before we misidentify the client).
		 */
		if (BTPeerIDByteDecoderUtils.isPossibleSpoofClient(peer_id)) {
			client = decodeBitSpiritClient(peer_id, peer_id_bytes);
			if (client != null) {return client;}
			client = decodeBitCometClient(peer_id, peer_id_bytes);
			if (client != null) {return client;}
			return "BitSpirit (bad peer ID)";
		}

		/**
		 * See if the client uses Az style identification.
		 */
		if (BTPeerIDByteDecoderUtils.isAzStyle(peer_id)) {
			client = BTPeerIDByteDecoderDefinitions.getAzStyleClientName(peer_id);
			if (client != null) {
				String client_with_version = BTPeerIDByteDecoderDefinitions.getAzStyleClientVersion(client, peer_id);
				if (client_with_version != null) {return client_with_version;}
				return client;
			}
		}

		/**
		 * See if the client uses Shadow style identification.
		 */
		if (BTPeerIDByteDecoderUtils.isShadowStyle(peer_id)) {
			client = BTPeerIDByteDecoderDefinitions.getShadowStyleClientName(peer_id);
			if (client != null) {
				String client_ver = BTPeerIDByteDecoderUtils.getShadowStyleVersionNumber(peer_id);
				if (client_ver != null) {return client + " " + client_ver;}
				return client;
			}
		}

		/**
		 * See if the client uses Mainline style identification.
		 */
		client = BTPeerIDByteDecoderDefinitions.getMainlineStyleClientName(peer_id);
		if (client != null) {
			/**
			 * We haven't got a good way of detecting whether this is a Mainline style
			 * version of peer ID until we start decoding peer ID information. So for
			 * that reason, we wait until we get client version information here - if
			 * we don't manage to determine a version number, then we assume that it
			 * has been misidentified and carry on with it.
			 */
			String client_ver = BTPeerIDByteDecoderUtils.getMainlineStyleVersionNumber(peer_id);
			if (client_ver != null) {return client + " " + client_ver;}
		}

		/**
		 * Check for BitSpirit / BitComet (non possible spoof client mode).
		 */
		client = decodeBitSpiritClient(peer_id, peer_id_bytes);
		if (client != null) {return client;}
		client = decodeBitCometClient(peer_id, peer_id_bytes);
		if (client != null) {return client;}


		/**
		 * See if the client identifies itself using a particular substring.
		 */
		BTPeerIDByteDecoderDefinitions.ClientData client_data = BTPeerIDByteDecoderDefinitions.getSubstringStyleClient(peer_id); 
		if (client_data != null) {
			client = client_data.client_name;
			String client_with_version = BTPeerIDByteDecoderDefinitions.getSubstringStyleClientVersion(client_data, peer_id, peer_id_bytes);
			if (client_with_version != null) {return client_with_version;}
			return client;
		}

		client = identifyAwkwardClient(peer_id_bytes);
		if (client != null) {return client;}
		return null;
	}

	/**
	 * Decodes the given peerID, returning an identification string.
	 */  
	public static String decode(byte[] peer_id) {
		String client = null;
		try {client = decode0(peer_id);}
		catch (Throwable e) {Debug.printStackTrace(e);}

		if (client != null) {return client;}
		logUnknownClient(peer_id);
		String sPeerID = getPrintablePeerID(peer_id);
		return MessageText.getString("PeerSocket.unknown") + "[" + sPeerID + "]";
	}

	public static String identifyAwkwardClient(byte[] peer_id) { 

		int iFirstNonZeroPos = 0;

		iFirstNonZeroPos = 20;
		for( int i=0; i < 20; i++ ) {
			if( peer_id[i] != (byte)0 ) {
				iFirstNonZeroPos = i;
				break;
			}
		}      

		//Shareaza check
		if( iFirstNonZeroPos == 0 ) {
			boolean bShareaza = true;
			for( int i=0; i < 16; i++ ) {
				if( peer_id[i] == (byte)0 ) {
					bShareaza = false;
					break;
				}
			}
			if( bShareaza ) {
				for( int i=16; i < 20; i++ ) {
					if( peer_id[i] != ( peer_id[i % 16] ^ peer_id[15 - (i % 16)] ) ) {
						bShareaza = false;
						break;
					}
				}
				if( bShareaza )  return "Shareaza";
			}
		}

		byte three = (byte)3;
		if ((iFirstNonZeroPos == 9)
				&& (peer_id[9] == three)
				&& (peer_id[10] == three)
				&& (peer_id[11] == three)) {
			return "Snark";
		}

		if ((iFirstNonZeroPos == 12) && (peer_id[12] == (byte)97) && (peer_id[13] == (byte)97)) {
			return "Experimental 3.2.1b2";
		}
		if ((iFirstNonZeroPos == 12) && (peer_id[12] == (byte)0) && (peer_id[13] == (byte)0)) {
			return "Experimental 3.1";
		}
		if (iFirstNonZeroPos == 12) return "Mainline";

		return null;

	}

	private static String decodeBitSpiritClient(String peer_id, byte[] peer_id_bytes) {
		if (!peer_id.substring(2, 4).equals("BS")) {return null;}
		String version = BTPeerIDByteDecoderUtils.decodeNumericValueOfByte(peer_id_bytes[1]);
		if ("0".equals(version)) {version = "1";}
		return "BitSpirit v" + version;
	}

	private static String decodeBitCometClient(String peer_id, byte[] peer_id_bytes) {
		String mod_name = null;
		if (peer_id.startsWith("exbc")) {mod_name = "";}
		else if (peer_id.startsWith("FUTB")) {mod_name  = "(Mod 1) ";}
		else if (peer_id.startsWith("xUTB")) {mod_name  = "(Mod 2) ";}
		else {return null;}

		boolean is_bitlord = (peer_id.substring(6, 10).equals("LORD"));

		/**
		 * Older versions of BitLord are of the form x.yy, whereas new versions (1 and onwards),
		 * are of the form x.y. BitComet is of the form x.yy.
		 */
		String client_name = (is_bitlord) ? "BitLord " : "BitComet ";
		String maj_version = BTPeerIDByteDecoderUtils.decodeNumericValueOfByte(peer_id_bytes[4]);
		int min_version_length = (is_bitlord && !maj_version.equals("0")) ? 1 : 2;

		return client_name + mod_name + maj_version + "." +
		BTPeerIDByteDecoderUtils.decodeNumericValueOfByte(peer_id_bytes[5], min_version_length);	  
	}


	protected static String getPrintablePeerID(  	byte[]		peer_id)
	{
		String	sPeerID = "";
		byte[] peerID = new byte[ peer_id.length ];
		System.arraycopy( peer_id, 0, peerID, 0, peer_id.length );

		try {
			for (int i = 0; i < peerID.length; i++) {
				int b = (0xFF & peerID[i]);
				if (b < 32 || b > 127)
					peerID[i] = '-';
			}
			sPeerID = new String(peerID, Constants.BYTE_ENCODING);
		}
		catch (UnsupportedEncodingException ignore) {}
		catch (Throwable e) {}

		return( sPeerID );
	}

	private static void assertDecode(String client_result, String peer_id) throws Exception {
		if (peer_id.length() > 40) {
			peer_id = peer_id.replaceAll("[ ]", "");
		}

		byte[] byte_peer_id = null;
		if (peer_id.length() == 40) {
			byte_peer_id = ByteFormatter.decodeString(peer_id);
		}
		else if (peer_id.length() == 20) {
			byte_peer_id = peer_id.getBytes(Constants.BYTE_ENCODING);
		}
		else {
			throw new IllegalArgumentException(peer_id);
		}
		assertDecode(client_result, byte_peer_id);
	}

	private static void assertDecode(String client_result, byte[] peer_id) throws Exception {
		String peer_id_as_string = new String(peer_id, Constants.BYTE_ENCODING).replace('\n', ' ').replace('\r', ' ');
		System.out.println("Testing for " + client_result + ", peer ID: " + peer_id_as_string);
		String decoded_result = decode0(peer_id);
		if (client_result.equals(decoded_result)) {return;}
		throw new RuntimeException("assertion failure - expected \"" + client_result + "\", got \"" + decoded_result + "\": " + peer_id_as_string);
	}

	public static void main(String[] args) throws Exception {
		assertDecode("BitTornado 0.3.9", "T0390----5uL5NvjBe2z");
		assertDecode("Mainline", "0000000000000000000000004C53441933104277");
		assertDecode("Shareaza 2.1.3.2", "2D535A323133322D000000000000000000000000");
		assertDecode("ABC 2.6.9", "413236392D2D2D2D345077199FAEC4A673BECA01");
		assertDecode("BitComet 0.56", "6578626300387A4463102D6E9AD6723B339F35A9");
		assertDecode("Azureus 2.2.0.0", "2D415A323230302D3677664732776B3677574C63");
		assertDecode("BitSpirit v2", "000242539B7ED3E058A8384AA748485454504254");
		assertDecode("BitSpirit (bad peer ID)", "4D342D302D322D2D6898D9D0CAF25E4555445030");
		assertDecode("BitLord 0.56", "6578626300384C4F52443200048ECED57BD71028");
		assertDecode("BitTornado 0.3.10", "543033412D2D2D2D2D6351374B5848424E733264");
		assertDecode("Azureus 2.0.3.2", "2D2D2D2D2D417A757265757354694E7A2A6454A7");
		assertDecode("Opera (Build 7685)", "OP7685f2c1495b1680bf");
		assertDecode("KTorrent 1.1 RC1", "-KT11R1-693649213030");
		assertDecode("BitSpirit v3", "00034253 07248896 44C59530 8A5FF2CA 55445030");
		assertDecode("TuoTu 2.1.0", "2D545432 3130772D 6471216E 57667E51 63657874");
		assertDecode("CyberArtemis 2.5.2.0", "2D415432 3532302D 76454574 30774F36 76306372");
		assertDecode("Ares 2.0.5", "-AG2053-Em6o1EmvwLtD");
		assertDecode("FlashGet 1.80", "2D464730 31383075 F8005782 1359D64B B3DFD265");
		assertDecode("Mainline 5.0.7", "4D352D30 2D372D2D 39616137 35376566 64356265");
		assertDecode("BitTornado 0.3.12", "54303343 2D2D2D2D 2D367459 6F6C7868 56554653");
		assertDecode("Rufus 0.6.9", "00455253 416E6F6E 796D6F75 7382BE42 75024AE3");
		assertDecode("Azureus 1", "417A7572 65757300 00000000 000000A0 76F0AEF7");
		assertDecode("Halite 0.2.9", "-HL0290-xUO*9ugvENUE");
		assertDecode("Transmission 0.72", "2D545230 3037322D 38766436 68726D70 3034616E");
		assertDecode("\u00B5Torrent 1.7.0 Beta", "2D555431 3730422D 92844644 1DB0A094 A01C01E5");
		//assertDecode("", "2D4E50303230312DCA5D53B5485C6AA1C52B4960"); // Unknown client "-NP0201-"...
		assertDecode("libTorrent (Rakshasa) 0.11.2", "2D6C74304232302D0D739B93E6BE21FEBB557B20");
		assertDecode("ABC 3.1", "413331302D2D303031763547797372344E784E4B");
		assertDecode("\u00B5Torrent 1.7.0 RC", "2D55543137302D00AF8BC5ACCC4631481EB3EB60");
		assertDecode("libtorrent (Rasterbar) 0.13.0", "2D4C54304430302D655A305077614444722D7E76"); // The latest version at time of writing is v0.12, but I'll assume this is valid.
		assertDecode("Tribler 3.7", "5233372D2D2D30303375417048793835312D5071");
		//assertDecode("", "2D4244303330302D31534769525A387557705748"); // Unknown client "-BD0300-"....
		assertDecode("BitTornado 0.3.18", "543033492D2D3030386759366942364171323743");

		System.out.println("Done.");
	}
}
