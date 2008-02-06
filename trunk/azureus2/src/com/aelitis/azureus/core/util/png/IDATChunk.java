/*
 * Created on Feb 5, 2008
 * Created by Olivier Chalouhi
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package com.aelitis.azureus.core.util.png;

import java.nio.ByteBuffer;

public class IDATChunk extends CRCedChunk {
	private static final byte[] type = {(byte) 73, (byte) 68, (byte) 65, (byte) 84 };
	
	private int width;
	
	public IDATChunk(int width) {
		super(type);
		this.width = width;
	}
	
	
	public byte[] getContentPayload() {
		byte[] payload = new byte[width];
		ByteBuffer buffer = ByteBuffer.allocate(width + 11);
		
		buffer.put((byte)0x87);
		buffer.put((byte)0);
		
		//Final Block, uncompressed data
		buffer.put((byte)128);
		
		
		short len = (short) width;
		short nlen = (short) (1- len); 
		
		buffer.putShort(len);
		buffer.putShort(nlen);
		
		buffer.put(payload);
		
		//zlib crc
		buffer.putShort(len);
		buffer.putShort((short)1);
		
		
		return buffer.array();
	}

}
