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
 * $Id: SHA1.java,v 1.1 2004-02-22 21:42:14 nolar Exp $
 *
 ********************************************************************************/
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use this class for getting a SHA1 message digest.
 * Create a SHA1 and reuse it after a message digest calculation. There can be as
 * many SHA1 objects as you want to have multiple calculations same time.
 * The message can be passed in one or a sequenze of parts wrapped in a
 * ByteBuffer to the update of the same SHA1 instance. To finish the calculation
 * use final, it will reset the SHA1 instance for a new calculation.
 *
 * @author emarant
 * @version $Revision: 1.1 $
 * <br>Last changed by $Author: nolar $ on $Date: 2004-02-22 21:42:14 $
 */
public final class SHA1{
    
    private ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);
    private int stateA = 0x67452301;
    private int stateB = 0xefcdab89;
    private int stateC = 0x98badcfe;
    private int stateD = 0x10325476;
    private int stateE = 0xc3d2e1f0;
    private long count = 0;
    
    /**
    * Constructor returns a SHA1 ready for use.
    */
    public SHA1() {
    }
    
    /**
    * Resets the SHA1 to initial state for a new message digest calculation.
    */
    public void reset() {
        stateA = 0x67452301;
        stateB = 0xefcdab89;
        stateC = 0x98badcfe;
        stateD = 0x10325476;
        stateE = 0xc3d2e1f0;
        count = 0;
        buffer.rewind();
        for(int i=0;i<64;i++){
            buffer.put((byte)0);
        }
        buffer.rewind();
    }
    
    /** 
    * Starts or continues a SHA1 message digest calculation.
    * input.remaining() should be a multiple of 64 to be most efficant, but
    * other amounts work too. Only remaining bytes of the ByteBuffer are used
    * and input.position() will be input.limit() after return.
    * @param input hold a part of the message. input.order() have to be ByteOrder.BIG_ENDIAN
    */
    public void update(ByteBuffer input){
        int index, partLen, i, inputLen;
        inputLen = input.remaining();
        index = ((int)count) & 63;
        count += inputLen;
        partLen = 64 - index;
        i = 0;
        if (inputLen >= partLen){
            if (index>0){
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
        if (i<inputLen){
            buffer.put(input);
        }
    }
    
    /**
    * Finishs a SHA1 message digest calculation.
    * The result is stored in digest and the SHA1-object is <b>reset</b> and so
    * ready for a new message digest calculation.
    *
    * @param digest should be a ByteBuffer with digest.remaining() &gt;= 20 and it's order have to be <code>ByteOrder.BIG_ENDIAN</code>.
    *
    */
    public void finalDigest(ByteBuffer digest){
        int index;
        
        buffer.put((byte)0x80);
        index = ((int)count) & 63;
        if (index < 56){
            for(int i = index ; i < 55 ;i++)
                buffer.put((byte)0);
        }else{
            for(int i = index ; i < 63 ; i++)
                buffer.put((byte)0);
            buffer.rewind();
            transform(buffer);
            buffer.rewind();
            for(int i=0;i<56;i++)
                buffer.put((byte)0);
        }
        buffer.putLong(count << 3);
        buffer.rewind();
        transform(buffer);
        buffer.rewind();
        // save the result in digest
        digest.putInt(stateA);
        digest.putInt(stateB);
        digest.putInt(stateC);
        digest.putInt(stateD);
        digest.putInt(stateE);
        
        reset();
    }
    
    private void transform(ByteBuffer block) {
        int a, b, c, d, e;
        int g1, g2, g3, g4, h1, h2, h3, h4, i1, i2, i3, i4, j1, j2, j3, j4;
        g1 = block.getInt();
        g2 = block.getInt();
        g3 = block.getInt();
        g4 = block.getInt();
        h1 = block.getInt();
        h2 = block.getInt();
        h3 = block.getInt();
        h4 = block.getInt();
        i1 = block.getInt();
        i2 = block.getInt();
        i3 = block.getInt();
        i4 = block.getInt();
        j1 = block.getInt();
        j2 = block.getInt();
        j3 = block.getInt();
        j4 = block.getInt();
        
        a = stateA;
        b = stateB;
        c = stateC;
        d = stateD;
        e = stateE;
        
        e += ((a << 5) | (a >>> 27)) + f1(b, c, d) + g1;  b = ((b << 30) | (b >>> 2));
        d += ((e << 5) | (e >>> 27)) + f1(a, b, c) + g2;  a = ((a << 30) | (a >>> 2));
        c += ((d << 5) | (d >>> 27)) + f1(e, a, b) + g3;  e = ((e << 30) | (e >>> 2));
        b += ((c << 5) | (c >>> 27)) + f1(d, e, a) + g4;  d = ((d << 30) | (d >>> 2));
        a += ((b << 5) | (b >>> 27)) + f1(c, d, e) + h1;  c = ((c << 30) | (c >>> 2));
        e += ((a << 5) | (a >>> 27)) + f1(b, c, d) + h2;  b = ((b << 30) | (b >>> 2));
        d += ((e << 5) | (e >>> 27)) + f1(a, b, c) + h3;  a = ((a << 30) | (a >>> 2));
        c += ((d << 5) | (d >>> 27)) + f1(e, a, b) + h4;  e = ((e << 30) | (e >>> 2));
        b += ((c << 5) | (c >>> 27)) + f1(d, e, a) + i1;  d = ((d << 30) | (d >>> 2));
        a += ((b << 5) | (b >>> 27)) + f1(c, d, e) + i2;  c = ((c << 30) | (c >>> 2));
        e += ((a << 5) | (a >>> 27)) + f1(b, c, d) + i3;  b = ((b << 30) | (b >>> 2));
        d += ((e << 5) | (e >>> 27)) + f1(a, b, c) + i4;  a = ((a << 30) | (a >>> 2));
        c += ((d << 5) | (d >>> 27)) + f1(e, a, b) + j1;  e = ((e << 30) | (e >>> 2));
        b += ((c << 5) | (c >>> 27)) + f1(d, e, a) + j2;  d = ((d << 30) | (d >>> 2));
        a += ((b << 5) | (b >>> 27)) + f1(c, d, e) + j3;  c = ((c << 30) | (c >>> 2));
        e += ((a << 5) | (a >>> 27)) + f1(b, c, d) + j4;  b = ((b << 30) | (b >>> 2));
        
        g1 = g1 ^ g3 ^ i1 ^ j2;  g1 = ((g1 << 1) | (g1 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f1(a, b, c) + g1; a = ((a << 30) | (a >>> 2));
        g2 = g2 ^ g4 ^ i2 ^ j3;  g2 = ((g2 << 1) | (g2 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f1(e, a, b) + g2; e = ((e << 30) | (e >>> 2));
        g3 = g3 ^ h1 ^ i3 ^ j4;  g3 = ((g3 << 1) | (g3 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f1(d, e, a) + g3; d = ((d << 30) | (d >>> 2));
        g4 = g4 ^ h2 ^ i4 ^ g1;  g4 = ((g4 << 1) | (g4 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f1(c, d, e) + g4; c = ((c << 30) | (c >>> 2));
        
        h1 = h1 ^ h3 ^ j1 ^ g2;  h1 = ((h1 << 1) | (h1 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f2(b, c, d) + h1; b = ((b << 30) | (b >>> 2));
        h2 = h2 ^ h4 ^ j2 ^ g3;  h2 = ((h2 << 1) | (h2 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f2(a, b, c) + h2; a = ((a << 30) | (a >>> 2));
        h3 = h3 ^ i1 ^ j3 ^ g4;  h3 = ((h3 << 1) | (h3 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f2(e, a, b) + h3; e = ((e << 30) | (e >>> 2));
        h4 = h4 ^ i2 ^ j4 ^ h1;  h4 = ((h4 << 1) | (h4 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f2(d, e, a) + h4; d = ((d << 30) | (d >>> 2));
        
        i1 = i1 ^ i3 ^ g1 ^ h2;  i1 = ((i1 << 1) | (i1 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f2(c, d, e) + i1; c = ((c << 30) | (c >>> 2));
        i2 = i2 ^ i4 ^ g2 ^ h3;  i2 = ((i2 << 1) | (i2 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f2(b, c, d) + i2; b = ((b << 30) | (b >>> 2));
        i3 = i3 ^ j1 ^ g3 ^ h4;  i3 = ((i3 << 1) | (i3 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f2(a, b, c) + i3; a = ((a << 30) | (a >>> 2));
        i4 = i4 ^ j2 ^ g4 ^ i1;  i4 = ((i4 << 1) | (i4 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f2(e, a, b) + i4; e = ((e << 30) | (e >>> 2));
        
        j1 = j1 ^ j3 ^ h1 ^ i2;  j1 = ((j1 << 1) | (j1 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f2(d, e, a) + j1; d = ((d << 30) | (d >>> 2));
        j2 = j2 ^ j4 ^ h2 ^ i3;  j2 = ((j2 << 1) | (j2 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f2(c, d, e) + j2; c = ((c << 30) | (c >>> 2));
        j3 = j3 ^ g1 ^ h3 ^ i4;  j3 = ((j3 << 1) | (j3 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f2(b, c, d) + j3; b = ((b << 30) | (b >>> 2));
        j4 = j4 ^ g2 ^ h4 ^ j1;  j4 = ((j4 << 1) | (j4 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f2(a, b, c) + j4; a = ((a << 30) | (a >>> 2));
        
        g1 = g1 ^ g3 ^ i1 ^ j2;  g1 = ((g1 << 1) | (g1 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f2(e, a, b) + g1; e = ((e << 30) | (e >>> 2));
        g2 = g2 ^ g4 ^ i2 ^ j3;  g2 = ((g2 << 1) | (g2 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f2(d, e, a) + g2; d = ((d << 30) | (d >>> 2));
        g3 = g3 ^ h1 ^ i3 ^ j4;  g3 = ((g3 << 1) | (g3 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f2(c, d, e) + g3; c = ((c << 30) | (c >>> 2));
        g4 = g4 ^ h2 ^ i4 ^ g1;  g4 = ((g4 << 1) | (g4 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f2(b, c, d) + g4; b = ((b << 30) | (b >>> 2));
        
        h1 = h1 ^ h3 ^ j1 ^ g2;  h1 = ((h1 << 1) | (h1 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f2(a, b, c) + h1; a = ((a << 30) | (a >>> 2));
        h2 = h2 ^ h4 ^ j2 ^ g3;  h2 = ((h2 << 1) | (h2 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f2(e, a, b) + h2; e = ((e << 30) | (e >>> 2));
        h3 = h3 ^ i1 ^ j3 ^ g4;  h3 = ((h3 << 1) | (h3 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f2(d, e, a) + h3; d = ((d << 30) | (d >>> 2));
        h4 = h4 ^ i2 ^ j4 ^ h1;  h4 = ((h4 << 1) | (h4 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f2(c, d, e) + h4; c = ((c << 30) | (c >>> 2));
        
        i1 = i1 ^ i3 ^ g1 ^ h2;  i1 = ((i1 << 1) | (i1 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f3(b, c, d) + i1; b = ((b << 30) | (b >>> 2));
        i2 = i2 ^ i4 ^ g2 ^ h3;  i2 = ((i2 << 1) | (i2 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f3(a, b, c) + i2; a = ((a << 30) | (a >>> 2));
        i3 = i3 ^ j1 ^ g3 ^ h4;  i3 = ((i3 << 1) | (i3 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f3(e, a, b) + i3; e = ((e << 30) | (e >>> 2));
        i4 = i4 ^ j2 ^ g4 ^ i1;  i4 = ((i4 << 1) | (i4 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f3(d, e, a) + i4; d = ((d << 30) | (d >>> 2));
        
        j1 = j1 ^ j3 ^ h1 ^ i2;  j1 = ((j1 << 1) | (j1 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f3(c, d, e) + j1; c = ((c << 30) | (c >>> 2));
        j2 = j2 ^ j4 ^ h2 ^ i3;  j2 = ((j2 << 1) | (j2 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f3(b, c, d) + j2; b = ((b << 30) | (b >>> 2));
        j3 = j3 ^ g1 ^ h3 ^ i4;  j3 = ((j3 << 1) | (j3 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f3(a, b, c) + j3; a = ((a << 30) | (a >>> 2));
        j4 = j4 ^ g2 ^ h4 ^ j1;  j4 = ((j4 << 1) | (j4 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f3(e, a, b) + j4; e = ((e << 30) | (e >>> 2));
        
        g1 = g1 ^ g3 ^ i1 ^ j2;  g1 = ((g1 << 1) | (g1 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f3(d, e, a) + g1; d = ((d << 30) | (d >>> 2));
        g2 = g2 ^ g4 ^ i2 ^ j3;  g2 = ((g2 << 1) | (g2 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f3(c, d, e) + g2; c = ((c << 30) | (c >>> 2));
        g3 = g3 ^ h1 ^ i3 ^ j4;  g3 = ((g3 << 1) | (g3 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f3(b, c, d) + g3; b = ((b << 30) | (b >>> 2));
        g4 = g4 ^ h2 ^ i4 ^ g1;  g4 = ((g4 << 1) | (g4 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f3(a, b, c) + g4; a = ((a << 30) | (a >>> 2));
        
        h1 = h1 ^ h3 ^ j1 ^ g2;  h1 = ((h1 << 1) | (h1 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f3(e, a, b) + h1; e = ((e << 30) | (e >>> 2));
        h2 = h2 ^ h4 ^ j2 ^ g3;  h2 = ((h2 << 1) | (h2 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f3(d, e, a) + h2; d = ((d << 30) | (d >>> 2));
        h3 = h3 ^ i1 ^ j3 ^ g4;  h3 = ((h3 << 1) | (h3 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f3(c, d, e) + h3; c = ((c << 30) | (c >>> 2));
        h4 = h4 ^ i2 ^ j4 ^ h1;  h4 = ((h4 << 1) | (h4 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f3(b, c, d) + h4; b = ((b << 30) | (b >>> 2));
        
        i1 = i1 ^ i3 ^ g1 ^ h2;  i1 = ((i1 << 1) | (i1 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f3(a, b, c) + i1; a = ((a << 30) | (a >>> 2));
        i2 = i2 ^ i4 ^ g2 ^ h3;  i2 = ((i2 << 1) | (i2 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f3(e, a, b) + i2; e = ((e << 30) | (e >>> 2));
        i3 = i3 ^ j1 ^ g3 ^ h4;  i3 = ((i3 << 1) | (i3 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f3(d, e, a) + i3; d = ((d << 30) | (d >>> 2));
        i4 = i4 ^ j2 ^ g4 ^ i1;  i4 = ((i4 << 1) | (i4 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f3(c, d, e) + i4; c = ((c << 30) | (c >>> 2));
        
        j1 = j1 ^ j3 ^ h1 ^ i2;  j1 = ((j1 << 1) | (j1 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f4(b, c, d) + j1; b = ((b << 30) | (b >>> 2));
        j2 = j2 ^ j4 ^ h2 ^ i3;  j2 = ((j2 << 1) | (j2 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f4(a, b, c) + j2; a = ((a << 30) | (a >>> 2));
        j3 = j3 ^ g1 ^ h3 ^ i4;  j3 = ((j3 << 1) | (j3 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f4(e, a, b) + j3; e = ((e << 30) | (e >>> 2));
        j4 = j4 ^ g2 ^ h4 ^ j1;  j4 = ((j4 << 1) | (j4 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f4(d, e, a) + j4; d = ((d << 30) | (d >>> 2));
        
        g1 = g1 ^ g3 ^ i1 ^ j2;  g1 = ((g1 << 1) | (g1 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f4(c, d, e) + g1; c = ((c << 30) | (c >>> 2));
        g2 = g2 ^ g4 ^ i2 ^ j3;  g2 = ((g2 << 1) | (g2 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f4(b, c, d) + g2; b = ((b << 30) | (b >>> 2));
        g3 = g3 ^ h1 ^ i3 ^ j4;  g3 = ((g3 << 1) | (g3 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f4(a, b, c) + g3; a = ((a << 30) | (a >>> 2));
        g4 = g4 ^ h2 ^ i4 ^ g1;  g4 = ((g4 << 1) | (g4 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f4(e, a, b) + g4; e = ((e << 30) | (e >>> 2));
        
        h1 = h1 ^ h3 ^ j1 ^ g2;  h1 = ((h1 << 1) | (h1 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f4(d, e, a) + h1; d = ((d << 30) | (d >>> 2));
        h2 = h2 ^ h4 ^ j2 ^ g3;  h2 = ((h2 << 1) | (h2 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f4(c, d, e) + h2; c = ((c << 30) | (c >>> 2));
        h3 = h3 ^ i1 ^ j3 ^ g4;  h3 = ((h3 << 1) | (h3 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f4(b, c, d) + h3; b = ((b << 30) | (b >>> 2));
        h4 = h4 ^ i2 ^ j4 ^ h1;  h4 = ((h4 << 1) | (h4 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f4(a, b, c) + h4; a = ((a << 30) | (a >>> 2));
        
        i1 = i1 ^ i3 ^ g1 ^ h2;  i1 = ((i1 << 1) | (i1 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f4(e, a, b) + i1; e = ((e << 30) | (e >>> 2));
        i2 = i2 ^ i4 ^ g2 ^ h3;  i2 = ((i2 << 1) | (i2 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f4(d, e, a) + i2; d = ((d << 30) | (d >>> 2));
        i3 = i3 ^ j1 ^ g3 ^ h4;  i3 = ((i3 << 1) | (i3 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f4(c, d, e) + i3; c = ((c << 30) | (c >>> 2));
        i4 = i4 ^ j2 ^ g4 ^ i1;  i4 = ((i4 << 1) | (i4 >>> 31));
        e += ((a << 5) | (a >>> 27)) + f4(b, c, d) + i4; b = ((b << 30) | (b >>> 2));
        
        j1 = j1 ^ j3 ^ h1 ^ i2;  j1 = ((j1 << 1) | (j1 >>> 31));
        d += ((e << 5) | (e >>> 27)) + f4(a, b, c) + j1; a = ((a << 30) | (a >>> 2));
        j2 = j2 ^ j4 ^ h2 ^ i3;  j2 = ((j2 << 1) | (j2 >>> 31));
        c += ((d << 5) | (d >>> 27)) + f4(e, a, b) + j2; e = ((e << 30) | (e >>> 2));
        j3 = j3 ^ g1 ^ h3 ^ i4;  j3 = ((j3 << 1) | (j3 >>> 31));
        b += ((c << 5) | (c >>> 27)) + f4(d, e, a) + j3; d = ((d << 30) | (d >>> 2));
        j4 = j4 ^ g2 ^ h4 ^ j1;  j4 = ((j4 << 1) | (j4 >>> 31));
        a += ((b << 5) | (b >>> 27)) + f4(c, d, e) + j4; c = ((c << 30) | (c >>> 2));
        
        stateA += a;
        stateB += b;
        stateC += c;
        stateD += d;
        stateE += e;
    }
    
    private static int f1(int a, int b, int c) {
        return (c ^ (a & (b ^ c))) + 0x5A827999;
    }
    
    private static int f2(int a, int b, int c) {
        return (a ^ b ^ c) + 0x6ED9EBA1;
    }
    
    private static int f3(int a, int b, int c) {
        return ((a & b) | (c & (a | b))) + 0x8F1BBCDC;
    }
    
    private static int f4(int a, int b, int c) {
        return (a ^ b ^ c) + 0xCA62C1D6;
    }

}
