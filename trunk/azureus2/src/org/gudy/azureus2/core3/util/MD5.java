/*
 * Created on 16 avr. 2004
 * Created by Olivier Chalouhi
 * 
 * Azureus - a Java Bittorrent client
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
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;

/**
 * @author Olivier Chalouhi
 *
 */
public class MD5 {
  
  int h0,h1,h2,h3;
  int length;
  ByteBuffer finalBuffer;
  
  public MD5() {
    finalBuffer = ByteBuffer.allocateDirect(64);
    finalBuffer.position(0);
    finalBuffer.limit(64);
    
    reset();
  }
  
  public void transform(ByteBuffer M) {    
    int x0 , x1 , x2 , x3 ,  x4 , x5 , x6 , x7 , x8 , x9 ,
    x10, x11, x12, x13, x14, x15;
    
    int a,b,c,d;
    
    x0 = M.getInt();
    x1 = M.getInt();
    x2 = M.getInt();
    x3 = M.getInt();
    x4 = M.getInt();
    x5 = M.getInt();
    x6 = M.getInt();
    x7 = M.getInt();
    x8 = M.getInt();
    x9 = M.getInt();
    x10 = M.getInt();
    x11 = M.getInt();
    x12 = M.getInt();
    x13 = M.getInt();
    x14 = M.getInt();
    x15 = M.getInt();
    
    a = h0 ; b = h1 ; c = h2 ; d = h3 ;
    a += ((b & c) + ( ~b & d)) + x0 + -680876936;
    a += b + ((a << 7) | (a >>> 25));
    d += ((a & b) + ( ~a & c)) + x1 + -389564586;
    d += a + ((d << 12) | (d >>> 20));
    c += ((d & a) + ( ~d & b)) + x2 + 606105819;
    c += d + ((c << 17) | (c >>> 15));
    b += ((c & d) + ( ~c & a)) + x3 + -1044525330;
    b += c + ((b << 22) | (b >>> 10));
    a += ((b & c) + ( ~b & d)) + x4 + -176418897;
    a += b + ((a << 7) | (a >>> 25));
    d += ((a & b) + ( ~a & c)) + x5 + 1200080426;
    d += a + ((d << 12) | (d >>> 20));
    c += ((d & a) + ( ~d & b)) + x6 + -1473231341;
    c += d + ((c << 17) | (c >>> 15));
    b += ((c & d) + ( ~c & a)) + x7 + -45705983;
    b += c + ((b << 22) | (b >>> 10));
    a += ((b & c) + ( ~b & d)) + x8 + 1770035416;
    a += b + ((a << 7) | (a >>> 25));
    d += ((a & b) + ( ~a & c)) + x9 + -1958414417;
    d += a + ((d << 12) | (d >>> 20));
    c += ((d & a) + ( ~d & b)) + x10 + -42063;
    c += d + ((c << 17) | (c >>> 15));
    b += ((c & d) + ( ~c & a)) + x11 + -1990404162;
    b += c + ((b << 22) | (b >>> 10));
    a += ((b & c) + ( ~b & d)) + x12 + 1804603682;
    a += b + ((a << 7) | (a >>> 25));
    d += ((a & b) + ( ~a & c)) + x13 + -40341101;
    d += a + ((d << 12) | (d >>> 20));
    c += ((d & a) + ( ~d & b)) + x14 + -1502002290;
    c += d + ((c << 17) | (c >>> 15));
    b += ((c & d) + ( ~c & a)) + x15 + 1236535329;
    b += c + ((b << 22) | (b >>> 10));
    a += ((b & d) + (c & ~d)) + x1 + -165796510;
    a += b + ((a << 5) | (a >>> 27));
    d += ((a & c) + (b & ~c)) + x6 + -1069501632;
    d += a + ((d << 9) | (d >>> 23));
    c += ((d & b) + (a & ~b)) + x11 + 643717713;
    c += d + ((c << 14) | (c >>> 18));
    b += ((c & a) + (d & ~a)) + x0 + -373897302;
    b += c + ((b << 20) | (b >>> 12));
    a += ((b & d) + (c & ~d)) + x5 + -701558691;
    a += b + ((a << 5) | (a >>> 27));
    d += ((a & c) + (b & ~c)) + x10 + 38016083;
    d += a + ((d << 9) | (d >>> 23));
    c += ((d & b) + (a & ~b)) + x15 + -660478335;
    c += d + ((c << 14) | (c >>> 18));
    b += ((c & a) + (d & ~a)) + x4 + -405537848;
    b += c + ((b << 20) | (b >>> 12));
    a += ((b & d) + (c & ~d)) + x9 + 568446438;
    a += b + ((a << 5) | (a >>> 27));
    d += ((a & c) + (b & ~c)) + x14 + -1019803690;
    d += a + ((d << 9) | (d >>> 23));
    c += ((d & b) + (a & ~b)) + x3 + -187363961;
    c += d + ((c << 14) | (c >>> 18));
    b += ((c & a) + (d & ~a)) + x8 + 1163531501;
    b += c + ((b << 20) | (b >>> 12));
    a += ((b & d) + (c & ~d)) + x13 + -1444681467;
    a += b + ((a << 5) | (a >>> 27));
    d += ((a & c) + (b & ~c)) + x2 + -51403784;
    d += a + ((d << 9) | (d >>> 23));
    c += ((d & b) + (a & ~b)) + x7 + 1735328473;
    c += d + ((c << 14) | (c >>> 18));
    b += ((c & a) + (d & ~a)) + x12 + -1926607734;
    b += c + ((b << 20) | (b >>> 12));
    a += (b ^ c ^ d) + x5 + -378558;
    a += b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x8 + -2022574463;
    d += a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x11 + 1839030562;
    c += d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x14 + -35309556;
    b += c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x1 + -1530992060;
    a += b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x4 + 1272893353;
    d += a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x7 + -155497632;
    c += d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x10 + -1094730640;
    b += c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x13 + 681279174;
    a += b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x0 + -358537222;
    d += a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x3 + -722521979;
    c += d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x6 + 76029189;
    b += c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x9 + -640364487;
    a += b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x12 + -421815835;
    d += a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x15 + 530742520;
    c += d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x2 + -995338651;
    b += c + ((b << 23) | (b >>> 9));
    a += (c ^ (b  | ~d)) + x0 + -198630844;
    a += b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x7 + 1126891415;
    d += a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x14 + -1416354905;
    c += d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x5 + -57434055;
    b += c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x12 + 1700485571;
    a += b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x3 + -1894986606;
    d += a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x10 + -1051523;
    c += d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x1 + -2054922799;
    b += c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x8 + 1873313359;
    a += b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x15 + -30611744;
    d += a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x6 + -1560198380;
    c += d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x13 + 1309151649;
    b += c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x4 + -145523070;
    a += b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x11 + -1120210379;
    d += a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x2 + 718787259;
    c += d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x9 + -343485551;
    b += c + ((b << 21) | (b >>> 11));
    
    h0 += a;
    h1 += b;
    h2 += c;
    h3 += d;    
  }
  
  /**
   * Resets the MD5 to initial state for a new message digest calculation.
   * Must be called before starting a new hash calculation.
   */
  public void reset() {
    h0 = 0x01234567;
    h1 = 0x89abcdef;
    h2 = 0xfedcba98;   
    h3 = 0x76543210;    
    
    length = 0;
    
    finalBuffer.clear();
  }
  
  private void completeFinalBuffer(ByteBuffer buffer) {
    if(finalBuffer.position() == 0) 
      return;
    
    while(buffer.remaining() > 0 && finalBuffer.remaining() > 0) {
      finalBuffer.put(buffer.get());
    }
    
    if(finalBuffer.remaining() == 0) {
      finalBuffer.position(0);
      transform(finalBuffer);
      finalBuffer.position(0);
    }
  }
  
  
  /**
   * Starts or continues a MD5 message digest calculation.
   * Only the remaining bytes of the given ByteBuffer are used.
   * @param buffer input data
   */
  public void update(ByteBuffer buffer) {
    length += buffer.remaining();
    //Save current position to leave given buffer unchanged
    int position = buffer.position();
    
    //Complete the final buffer if needed
    completeFinalBuffer(buffer);
    
    while(buffer.remaining() >= 64) {
      transform(buffer);
    }
    
    if(buffer.remaining() != 0) {
      finalBuffer.put(buffer);
    }
    
    buffer.position(position);
  }
  
  
  /**
   * Finishes the SHA-1 message digest calculation.
   * @return 16-byte hash result
   */
  public byte[] digest() {
    byte[] result = new byte[16];
    
    finalBuffer.put((byte)0x80);
    if(finalBuffer.remaining() < 8) {
      while(finalBuffer.remaining() > 0) {
        finalBuffer.put((byte)0);
      }
      finalBuffer.position(0);
      transform(finalBuffer);
      finalBuffer.position(0);
    }
    
    while(finalBuffer.remaining() > 8) {
      finalBuffer.put((byte)0);
    }
    
    finalBuffer.putLong(length << 3);
    finalBuffer.position(0);
    transform(finalBuffer);
    
    finalBuffer.position(0);
    finalBuffer.putInt(h0);
    finalBuffer.putInt(h1);
    finalBuffer.putInt(h2);
    finalBuffer.putInt(h3);    
    finalBuffer.position(0);
    
    for(int i  = 0 ; i < 16 ; i++) {
     result[i] = finalBuffer.get(); 
    }
    
    return result;
  }
  
  
  /**
   * Finishes the MD5 message digest calculation, by first performing a final update
   * from the given input buffer, then completing the calculation as with digest().
   * @param buffer input data
   * @return 16-byte hash result
   */
  public byte[] digest(ByteBuffer buffer) {
    update( buffer );
    return digest();
  }
  
}
