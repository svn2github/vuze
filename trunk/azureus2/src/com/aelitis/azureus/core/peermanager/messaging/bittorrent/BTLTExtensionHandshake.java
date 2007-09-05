/*
 * Created on 10 Aug 2007
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
package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

import java.util.Collections;
import java.util.Map;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

/**
 * @author Allan Crooks
 *
 */
public class BTLTExtensionHandshake implements BTMessage {
	
	private Map data_dict;
	private byte[] bencoded_data;
	private String bencoded_string;
	private String description;
    private byte version;
    private DirectByteBuffer[] buffer_array;
    
    public BTLTExtensionHandshake(Map data_dict, byte version) {
    	this.data_dict = (data_dict == null) ? Collections.EMPTY_MAP : data_dict;
    	this.version = version;
    }

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.peermanager.messaging.Message#deserialize(org.gudy.azureus2.core3.util.DirectByteBuffer, byte)
	 */
	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException( "[" +getID() + "] decode error: data == null");
		}
		if (data.remaining(DirectByteBuffer.SS_MSG ) < 1) {
			throw new MessageException( "[" +getID() + "] decode error: less than 1 byte in payload");
		}
		byte message_type = data.get(DirectByteBuffer.SS_MSG);
		if (message_type != 0) {
			throw new MessageException( "[" +getID() + "] decode error: no support for extension message ID " + message_type);
		}
		
		// Try decoding the data now.
		Map res_data_dict = MessagingUtil.convertBencodedByteStreamToPayload(data, 1, getID());
		
		BTLTExtensionHandshake result = new BTLTExtensionHandshake(res_data_dict, this.version);
		return result;
	}

	public DirectByteBuffer[] getData() {
		if (buffer_array == null) {
			buffer_array = new DirectByteBuffer[1];
			DirectByteBuffer buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG_LT_EXT_HANDSHAKE, getBencodedData().length + 1);
			buffer_array[0] = buffer;
			
			buffer.put(DirectByteBuffer.SS_MSG, (byte)0); // Indicate it is a handshake, not any extension protocol.
			buffer.put(DirectByteBuffer.SS_MSG, getBencodedData());
			buffer.flip(DirectByteBuffer.SS_MSG);
		}
		return buffer_array;
	}
	
	public void destroy() {
		this.data_dict = null;
		this.bencoded_data = null;
		this.description = null;
		if (buffer_array != null) {
			buffer_array[0].returnToPool();
		}
		this.buffer_array = null;
	}

	public String getDescription() {
		if (description == null) {
			description = BTMessage.ID_BT_LT_EXTENSION_HANDSHAKE + ": " + this.getBencodedString();
		}
		return description;
	}
	
	public String getBencodedString() {
		if (this.bencoded_string == null) {
			try {
				this.bencoded_string = new String(this.getBencodedData(), Constants.BYTE_ENCODING);
			}
			catch (java.io.UnsupportedEncodingException uee) {
				this.bencoded_string = "";
				Debug.printStackTrace(uee);
			}
		}
		return this.bencoded_string;
	}
	
	public byte[] getBencodedData() {
		if (this.bencoded_data == null) {
			try {this.bencoded_data = BEncoder.encode(this.data_dict);}
			catch (java.io.IOException ioe) {
				this.bencoded_data = new byte[0];
				Debug.printStackTrace(ioe);
			}
		}
		return this.bencoded_data;
	}
	
	public Map getDataMap() {
		return this.data_dict;
	}
	
	public String getClientName() {
		byte[] client_name = (byte[])data_dict.get("v");
		if (client_name == null) {return null;}
		try {return new String(client_name, Constants.DEFAULT_ENCODING);}
		catch (java.io.IOException ioe) {return null;}
	}
	
	public int getTCPListeningPort()
	{
		Long port = (Long)data_dict.get("p");
		if(port == null)
			return -1;
		int val = port.intValue();
		if(val <= 65535 && val > 0)
			return val;
		return -1;
	}
	
	public Boolean isCryptoRequested()
	{
		Long crypto = (Long)data_dict.get("e");
		if(crypto == null)
			return null;
		return Boolean.valueOf(crypto.longValue() == 1);
	}

    // Identification methods.
    public String getID() {return BTMessage.ID_BT_LT_EXTENSION_HANDSHAKE;}
    public byte[] getIDBytes() {return BTMessage.ID_BT_LT_EXTENSION_HANDSHAKE_BYTES;}
    public String getFeatureID() {return BTMessage.BT_FEATURE_ID;} 
    public int getFeatureSubID() {return BTMessage.SUBID_BT_LT_EXTENSION_HANDSHAKE;}
    public int getType() {return Message.TYPE_PROTOCOL_PAYLOAD;}
	public byte getVersion() {return this.version;}


}

