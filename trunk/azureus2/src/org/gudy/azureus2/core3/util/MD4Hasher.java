/********************************************************************************
 *
 * jMule - a Java massive parallel file sharing client
 *
 * Copyright (C) by the jMuleGroup ( see the CREDITS file )
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id: MD4Hasher.java,v 1.2 2004-02-16 15:56:18 parg Exp $
 *
 ********************************************************************************/


package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use this class for getting a MD4 message digest.
 * Create a MD4 and reuse it after a message digest calculation. There can be as
 * many MD4 objects as you want to have multiple calculations same time.
 * The message can be passed in one or a sequenze of parts wrapped in a
 * ByteBuffer to the update of the same MD4 instance. To finish the calculation
 * use final, it will reset the MD4 instance for a new calculation.
 *
 * @author emarant
 * @version $Revision: 1.2 $
 * <br>Last changed by $Author: parg $ on $Date: 2004-02-16 15:56:18 $
 */
public final class MD4Hasher{
    
    private ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
    private int stateA = 0x67452301;
    private int stateB = 0xEFCDAB89;
    private int stateC = 0x98BADCFE;
    private int stateD = 0x10325476;
    private long count = 0;
    
    /**
    * Constructor returns a MD4 ready for use.
    */
    public MD4Hasher(){
    }
    
    /**
    * Resets the MD4 to initial state for a new message digest calculation.
    */
    public void reset(){
        stateA = 0x67452301;
        stateB = 0xEFCDAB89;
        stateC = 0x98BADCFE;
        stateD = 0x10325476;
        count = 0;
        buffer.rewind();
        for(int i=0;i<64;i++){
            buffer.put((byte)0);
        }
        buffer.rewind();
    }
    
    public void
    update(
    	byte[]		data,
		int			pos,
		int			len )
    {
    	ByteBuffer bb = ByteBuffer.wrap( data, pos, len );
    	
    	bb.order( ByteOrder.LITTLE_ENDIAN );
    	
    	update( bb );
    }    
    
    public void
    update(
    	byte[]		data )
    {
    	update( data, 0, data.length );
    }
    
    public byte[]
    getDigest()
    {
    	ByteBuffer result_buffer = ByteBuffer.allocate(16);
   
    	result_buffer.order( ByteOrder.LITTLE_ENDIAN );
    	
    	finalDigest(result_buffer);
    	
    	byte[] result = new byte[16];
    	
    	result_buffer.position(0);
    	
    	for(int i = 0 ; i < result.length ; i++) {
    		
    		result[i] = result_buffer.get();
    	}   
    	
    	return result;   
    }
    /** 
    * Starts or continues a MD4 message digest calculation.
    * input.remaining() should be a multiple of 64 to be most efficant, but
    * other amounts work too. Only remaining bytes of the ByteBuffer are used
    * and input.position() will be input.limit() after return.
    * @param input hold a part of the message. input.order() have to be ByteOrder.LITTLE_ENDIAN
    */
    public void update(ByteBuffer input){
        int index, partLen, i, inputLen;
        inputLen = input.remaining();
        index = ((int)count) & 63;
        count += inputLen;
        partLen = 64 - index;
        i = 0;
        if (inputLen >= partLen){
  //if-block  nur notwendig, wenn input.remaining() kein vielfaches von 64 
  // beim vorherigen Aufruf war und das MD4-Obj.nicht zurückgesetzt wurde
  //  (zBsp. durch finalDigest(ByteBuffer digest) )
            if (index>0){
 //               System.out.println("used special");
                int t = input.limit();
                input.limit(input.position()+partLen);
                buffer.put(input);
                buffer.rewind();
                input.limit(t);
                transform(buffer);
                buffer.rewind();
                i = partLen;
                index = partLen;
            }
            
            while(i + 63 < inputLen){
                transform(input);
                i += 64;
            }
        }
  //      System.out.println(i+" "+buffer+" "+ input+ " "+inputLen);
        // save remaining input in buffer
        if (i<inputLen){
   //     System.out.println(i+" "+buffer+" "+ input+ " "+inputLen+" "+index);
          // input.position(i+input.position()-index);
  //      System.out.println(i+" "+buffer+" "+ input+ " "+inputLen);
            buffer.put(input);
        }
    }
    
    /**
    * Finishs a MD4 message digest calculation.
    * The result is stored in digest and the MD4-object is <b>reset</b> and so
    * ready for a new message digest calculation.
    *
    * @param digest should be a ByteBuffer with digest.remaining() &gt;= 16
    *
    */
    public void finalDigest(ByteBuffer digest){
        int index;
        
        index = ((int)count) & 63;
        if (index < 56){
            buffer.put((byte)0x80);
            for(int i = index ; i < 55 ;i++)
                buffer.put((byte)0);
            buffer.putLong(count << 3);
            buffer.rewind();
            transform(buffer);
            buffer.rewind();
        }else{
            buffer.put((byte)0x80);
            for(int i = index ; i < 63 ; i++)
                buffer.put((byte)0);
            buffer.rewind();
            transform(buffer);
            buffer.rewind();
            for(int i=0;i<56;i++)
                buffer.put((byte)0);
            buffer.putLong(count << 3);
            buffer.rewind();
            transform(buffer);
            buffer.rewind();
        }
        // save the result in digest
        digest.putInt(stateA);
        digest.putInt(stateB);
        digest.putInt(stateC);
        digest.putInt(stateD);
        
        reset();
    }
    
    private void transform(ByteBuffer block){
        int a, b, c, d;
        long e,f,g,h,i,j,k,l;
        
        a = stateA;
        b = stateB;
        c = stateC;
        d = stateD;
        e = block.getLong();
        f = block.getLong();
        g = block.getLong();
        h = block.getLong();
        i = block.getLong();
        j = block.getLong();
        k = block.getLong();
        l = block.getLong();
        
        a = FF(a, b, c, d, (int)e ,  3);
        d = FF(d, a, b, c, (int)(e >>> 32),  7);
        c = FF(c, d, a, b, (int)f, 11);
        b = FF(b, c, d, a, (int)(f >>> 32), 19);
        a = FF(a, b, c, d, (int)g,  3);
        d = FF(d, a, b, c, (int)(g >>> 32),  7);
        c = FF(c, d, a, b, (int)h, 11);
        b = FF(b, c, d, a, (int)(h >>> 32), 19);
        a = FF(a, b, c, d, (int)i,  3);
        d = FF(d, a, b, c, (int)(i >>> 32),  7);
        c = FF(c, d, a, b, (int)j, 11);
        b = FF(b, c, d, a, (int)(j >>> 32), 19);
        a = FF(a, b, c, d, (int)k,  3);
        d = FF(d, a, b, c, (int)(k >>> 32),  7);
        c = FF(c, d, a, b, (int)l, 11);
        b = FF(b, c, d, a, (int)(l >>> 32), 19);
        
        a = GG(a, b, c, d, (int)e,  3);
        d = GG(d, a, b, c, (int)g,  5);
        c = GG(c, d, a, b, (int)i,  9);
        b = GG(b, c, d, a, (int)k, 13);
        a = GG(a, b, c, d, (int)(e >>> 32),  3);
        d = GG(d, a, b, c, (int)(g >>> 32),  5);
        c = GG(c, d, a, b, (int)(i >>> 32),  9);
        b = GG(b, c, d, a, (int)(k >>> 32), 13);
        a = GG(a, b, c, d, (int)f,  3);
        d = GG(d, a, b, c, (int)h,  5);
        c = GG(c, d, a, b, (int)j,  9);
        b = GG(b, c, d, a, (int)l, 13);
        a = GG(a, b, c, d, (int)(f >>> 32),  3);
        d = GG(d, a, b, c, (int)(h >>> 32),  5);
        c = GG(c, d, a, b, (int)(j >>> 32),  9);
        b = GG(b, c, d, a, (int)(l >>> 32), 13);
        
        a = HH(a, b, c, d, (int)e,  3);
        d = HH(d, a, b, c, (int)i,  9);
        c = HH(c, d, a, b, (int)g, 11);
        b = HH(b, c, d, a, (int)k, 15);
        a = HH(a, b, c, d, (int)f,  3);
        d = HH(d, a, b, c, (int)j,  9);
        c = HH(c, d, a, b, (int)h, 11);
        b = HH(b, c, d, a, (int)l, 15);
        a = HH(a, b, c, d, (int)(e >>> 32),  3);
        d = HH(d, a, b, c, (int)(i >>> 32),  9);
        c = HH(c, d, a, b, (int)(g >>> 32), 11);
        b = HH(b, c, d, a, (int)(k >>> 32), 15);
        a = HH(a, b, c, d, (int)(f >>> 32),  3);
        d = HH(d, a, b, c, (int)(j >>> 32),  9);
        c = HH(c, d, a, b, (int)(h >>> 32), 11);
        b = HH(b, c, d, a, (int)(l >>> 32), 15);
        
        stateA += a;
        stateB += b;
        stateC += c;
        stateD += d;
    }
    
    private static int FF (int a, int b, int c, int d, int x, int s){
        int t = a + ((b & c) | (~b & d)) + x;
        return t << s | t >>> (32 - s);
    }
    
    private static int GG (int a, int b, int c, int d, int x, int s){
        int t = a + ((b & (c | d)) | (c & d)) + x + 0x5A827999;
        return t << s | t >>> (32 - s);
    }
    
    private static int HH (int a, int b, int c, int d, int x, int s){
        int t = a + (b ^ c ^ d) + x + 0x6ED9EBA1;
        return t << s | t >>> (32 - s);
    }
}
