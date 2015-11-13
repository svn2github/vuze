/*
 * Created on Mar 7, 2012
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */


package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class 
UTUploadOnly 
	implements LTMessage
{
	private final byte version;
	private DirectByteBuffer buffer = null;
	  
	private boolean	upload_only;
	
	public
	UTUploadOnly(
		boolean		_upload_only,
		byte		_version )
	{
		upload_only		= _upload_only;
		version			= _version;
	}
	
	public String 
	getID()
	{
		return( ID_UT_UPLOAD_ONLY );
	}

	public byte[] 
	getIDBytes()
	{
		return( ID_UT_UPLOAD_ONLY_BYTES );
	}

	public String 
	getFeatureID() 
	{  
		return LTMessage.LT_FEATURE_ID;  
	}  
	
	public int 
	getFeatureSubID() 
	{ 
		return SUBID_UT_UPLOAD_ONLY;  
	}
	
	public int 
	getType() 
	{  
		return Message.TYPE_PROTOCOL_PAYLOAD;  
	}
	
	public byte 
	getVersion() 
	{ 
		return version; 
	};

	public String 
	getDescription()
	{
		return( ID_UT_UPLOAD_ONLY);
	}
	
	public boolean
	isUploadOnly()
	{
		return( upload_only );
	}
	
	public DirectByteBuffer[] 
	getData()
	{
		if ( buffer == null) {
		
			buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG_LT_HANDSHAKE, 1 );
			
			buffer.put(DirectByteBuffer.SS_MSG, (byte)(upload_only?1:0));
			buffer.flip(DirectByteBuffer.SS_MSG);
		}
		
		return( new DirectByteBuffer[]{ buffer });
	}

	public Message 
	deserialize( 
		DirectByteBuffer 	data, 
		byte 				version ) 

		throws MessageException
	{
		byte[] dict_bytes = new byte[ Math.min( 128, data.remaining( DirectByteBuffer.SS_MSG )) ];
					
		data.get( DirectByteBuffer.SS_MSG, dict_bytes );
		
		if ( dict_bytes.length != 1 ){
			
			throw( new MessageException( "decode failed: incorrect length" ));
		}
		
		boolean ulo = dict_bytes[0] != 0;
		
		return( new UTUploadOnly( ulo, version ));
	}

	public void 
	destroy()
	{
		if ( buffer != null ){
			
			buffer.returnToPool();
		}
	}
}