/*
 * File    : SHA1Az.java
 * Created : 12 mars 2004
 * By      : Olivier
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
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;

/**
 * @author Olivier
 * 
 */
public final class SHA1Az {

  private int h0,h1,h2,h3,h4;
  private ByteBuffer finalBuffer;
  
  private ByteBuffer saveBuffer;
  private int s0,s1,s2,s3,s4;
  
  int length;
  int saveLength;
  
  public SHA1Az() {
    finalBuffer = ByteBuffer.allocateDirect(64);
    finalBuffer.position(0);
    finalBuffer.limit(64);
    
    saveBuffer = ByteBuffer.allocateDirect(64);
    saveBuffer.position(0);
    saveBuffer.limit(64);
  }
  
  private void transform(ByteBuffer M) {
    int w0 , w1 , w2 , w3 ,  w4 , w5 , w6 , w7 , w8 , w9 ,
    w10, w11, w12, w13, w14, w15;
    
    int a,b,c,d,e;
    
    w0 = M.getInt();
    w1 = M.getInt();
    w2 = M.getInt();
    w3 = M.getInt();
    w4 = M.getInt();
    w5 = M.getInt();
    w6 = M.getInt();
    w7 = M.getInt();
    w8 = M.getInt();
    w9 = M.getInt();
    w10 = M.getInt();
    w11 = M.getInt();
    w12 = M.getInt();
    w13 = M.getInt();
    w14 = M.getInt();
    w15 = M.getInt();
    
    a = h0 ; b = h1 ; c = h2 ; d = h3 ; e = h4;
    e += ((a << 5) | ( a >>> 27)) + w0 + ((b & c) | ((~b ) & d)) + 0x5A827999 ;
    b = (b << 30) | (b >>> 2) ;
    d += ((e << 5) | ( e >>> 27)) + w1 + ((a & b) | ((~a ) & c)) + 0x5A827999 ;
    a = (a << 30) | (a >>> 2) ;
    c += ((d << 5) | ( d >>> 27)) + w2 + ((e & a) | ((~e ) & b)) + 0x5A827999 ;
    e = (e << 30) | (e >>> 2) ;
    b += ((c << 5) | ( c >>> 27)) + w3 + ((d & e) | ((~d ) & a)) + 0x5A827999 ;
    d = (d << 30) | (d >>> 2) ;
    a += ((b << 5) | ( b >>> 27)) + w4 + ((c & d) | ((~c ) & e)) + 0x5A827999 ;
    c = (c << 30) | (c >>> 2) ;
    e += ((a << 5) | ( a >>> 27)) + w5 + ((b & c) | ((~b ) & d)) + 0x5A827999 ;
    b = (b << 30) | (b >>> 2) ;
    d += ((e << 5) | ( e >>> 27)) + w6 + ((a & b) | ((~a ) & c)) + 0x5A827999 ;
    a = (a << 30) | (a >>> 2) ;
    c += ((d << 5) | ( d >>> 27)) + w7 + ((e & a) | ((~e ) & b)) + 0x5A827999 ;
    e = (e << 30) | (e >>> 2) ;
    b += ((c << 5) | ( c >>> 27)) + w8 + ((d & e) | ((~d ) & a)) + 0x5A827999 ;
    d = (d << 30) | (d >>> 2) ;
    a += ((b << 5) | ( b >>> 27)) + w9 + ((c & d) | ((~c ) & e)) + 0x5A827999 ;
    c = (c << 30) | (c >>> 2) ;
    e += ((a << 5) | ( a >>> 27)) + w10 + ((b & c) | ((~b ) & d)) + 0x5A827999 ;
    b = (b << 30) | (b >>> 2) ;
    d += ((e << 5) | ( e >>> 27)) + w11 + ((a & b) | ((~a ) & c)) + 0x5A827999 ;
    a = (a << 30) | (a >>> 2) ;
    c += ((d << 5) | ( d >>> 27)) + w12 + ((e & a) | ((~e ) & b)) + 0x5A827999 ;
    e = (e << 30) | (e >>> 2) ;
    b += ((c << 5) | ( c >>> 27)) + w13 + ((d & e) | ((~d ) & a)) + 0x5A827999 ;
    d = (d << 30) | (d >>> 2) ;
    a += ((b << 5) | ( b >>> 27)) + w14 + ((c & d) | ((~c ) & e)) + 0x5A827999 ;
    c = (c << 30) | (c >>> 2) ;
    e += ((a << 5) | ( a >>> 27)) + w15 + ((b & c) | ((~b ) & d)) + 0x5A827999 ;
    b = (b << 30) | (b >>> 2) ;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w0 + ((a & b) | ((~a ) & c)) + 0x5A827999 ;
    a = (a << 30) | (a >>> 2) ;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w1 + ((e & a) | ((~e ) & b)) + 0x5A827999 ;
    e = (e << 30) | (e >>> 2) ;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w2 + ((d & e) | ((~d ) & a)) + 0x5A827999 ;
    d = (d << 30) | (d >>> 2) ;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w3 + ((c & d) | ((~c ) & e)) + 0x5A827999 ;
    c = (c << 30) | (c >>> 2) ;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w4 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    b = (b << 30) | (b >>> 2) ;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w5 + (a ^ b ^ c) + 0x6ED9EBA1 ;
    a = (a << 30) | (a >>> 2) ;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w6 + (e ^ a ^ b) + 0x6ED9EBA1 ;
    e = (e << 30) | (e >>> 2) ;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w7 + (d ^ e ^ a) + 0x6ED9EBA1 ;
    d = (d << 30) | (d >>> 2) ;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w8 + (c ^ d ^ e) + 0x6ED9EBA1 ;
    c = (c << 30) | (c >>> 2) ;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w9 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    b = (b << 30) | (b >>> 2) ;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w10 + (a ^ b ^ c) + 0x6ED9EBA1 ;
    a = (a << 30) | (a >>> 2) ;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w11 + (e ^ a ^ b) + 0x6ED9EBA1 ;
    e = (e << 30) | (e >>> 2) ;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w12 + (d ^ e ^ a) + 0x6ED9EBA1 ;
    d = (d << 30) | (d >>> 2) ;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w13 + (c ^ d ^ e) + 0x6ED9EBA1 ;
    c = (c << 30) | (c >>> 2) ;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w14 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    b = (b << 30) | (b >>> 2) ;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w15 + (a ^ b ^ c) + 0x6ED9EBA1 ;
    a = (a << 30) | (a >>> 2) ;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w0 + (e ^ a ^ b) + 0x6ED9EBA1 ;
    e = (e << 30) | (e >>> 2) ;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w1 + (d ^ e ^ a) + 0x6ED9EBA1 ;
    d = (d << 30) | (d >>> 2) ;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w2 + (c ^ d ^ e) + 0x6ED9EBA1 ;
    c = (c << 30) | (c >>> 2) ;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w3 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    b = (b << 30) | (b >>> 2) ;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w4 + (a ^ b ^ c) + 0x6ED9EBA1 ;
    a = (a << 30) | (a >>> 2) ;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w5 + (e ^ a ^ b) + 0x6ED9EBA1 ;
    e = (e << 30) | (e >>> 2) ;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w6 + (d ^ e ^ a) + 0x6ED9EBA1 ;
    d = (d << 30) | (d >>> 2) ;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w7 + (c ^ d ^ e) + 0x6ED9EBA1 ;
    c = (c << 30) | (c >>> 2) ;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w8 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    b = (b << 30) | (b >>> 2) ;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w9 + ((a & b) | (a & c) | (b & c)) + 0x8F1BBCDC ;
    a = (a << 30) | (a >>> 2) ;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w10 + ((e & a) | (e & b) | (a & b)) + 0x8F1BBCDC ;
    e = (e << 30) | (e >>> 2) ;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w11 + ((d & e) | (d & a) | (e & a)) + 0x8F1BBCDC ;
    d = (d << 30) | (d >>> 2) ;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w12 + ((c & d) | (c & e) | (d & e)) + 0x8F1BBCDC ;
    c = (c << 30) | (c >>> 2) ;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w13 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    b = (b << 30) | (b >>> 2) ;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w14 + ((a & b) | (a & c) | (b & c)) + 0x8F1BBCDC ;
    a = (a << 30) | (a >>> 2) ;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w15 + ((e & a) | (e & b) | (a & b)) + 0x8F1BBCDC ;
    e = (e << 30) | (e >>> 2) ;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w0 + ((d & e) | (d & a) | (e & a)) + 0x8F1BBCDC ;
    d = (d << 30) | (d >>> 2) ;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w1 + ((c & d) | (c & e) | (d & e)) + 0x8F1BBCDC ;
    c = (c << 30) | (c >>> 2) ;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w2 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    b = (b << 30) | (b >>> 2) ;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w3 + ((a & b) | (a & c) | (b & c)) + 0x8F1BBCDC ;
    a = (a << 30) | (a >>> 2) ;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w4 + ((e & a) | (e & b) | (a & b)) + 0x8F1BBCDC ;
    e = (e << 30) | (e >>> 2) ;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w5 + ((d & e) | (d & a) | (e & a)) + 0x8F1BBCDC ;
    d = (d << 30) | (d >>> 2) ;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w6 + ((c & d) | (c & e) | (d & e)) + 0x8F1BBCDC ;
    c = (c << 30) | (c >>> 2) ;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w7 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    b = (b << 30) | (b >>> 2) ;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w8 + ((a & b) | (a & c) | (b & c)) + 0x8F1BBCDC ;
    a = (a << 30) | (a >>> 2) ;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w9 + ((e & a) | (e & b) | (a & b)) + 0x8F1BBCDC ;
    e = (e << 30) | (e >>> 2) ;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w10 + ((d & e) | (d & a) | (e & a)) + 0x8F1BBCDC ;
    d = (d << 30) | (d >>> 2) ;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w11 + ((c & d) | (c & e) | (d & e)) + 0x8F1BBCDC ;
    c = (c << 30) | (c >>> 2) ;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w12 + (b ^ c ^ d) + 0xCA62C1D6 ;
    b = (b << 30) | (b >>> 2) ;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w13 + (a ^ b ^ c) + 0xCA62C1D6 ;
    a = (a << 30) | (a >>> 2) ;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w14 + (e ^ a ^ b) + 0xCA62C1D6 ;
    e = (e << 30) | (e >>> 2) ;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w15 + (d ^ e ^ a) + 0xCA62C1D6 ;
    d = (d << 30) | (d >>> 2) ;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w0 + (c ^ d ^ e) + 0xCA62C1D6 ;
    c = (c << 30) | (c >>> 2) ;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w1 + (b ^ c ^ d) + 0xCA62C1D6 ;
    b = (b << 30) | (b >>> 2) ;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w2 + (a ^ b ^ c) + 0xCA62C1D6 ;
    a = (a << 30) | (a >>> 2) ;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w3 + (e ^ a ^ b) + 0xCA62C1D6 ;
    e = (e << 30) | (e >>> 2) ;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w4 + (d ^ e ^ a) + 0xCA62C1D6 ;
    d = (d << 30) | (d >>> 2) ;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w5 + (c ^ d ^ e) + 0xCA62C1D6 ;
    c = (c << 30) | (c >>> 2) ;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w6 + (b ^ c ^ d) + 0xCA62C1D6 ;
    b = (b << 30) | (b >>> 2) ;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w7 + (a ^ b ^ c) + 0xCA62C1D6 ;
    a = (a << 30) | (a >>> 2) ;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w8 + (e ^ a ^ b) + 0xCA62C1D6 ;
    e = (e << 30) | (e >>> 2) ;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w9 + (d ^ e ^ a) + 0xCA62C1D6 ;
    d = (d << 30) | (d >>> 2) ;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w10 + (c ^ d ^ e) + 0xCA62C1D6 ;
    c = (c << 30) | (c >>> 2) ;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    e += ((a << 5) | ( a >>> 27)) + w11 + (b ^ c ^ d) + 0xCA62C1D6 ;
    b = (b << 30) | (b >>> 2) ;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    d += ((e << 5) | ( e >>> 27)) + w12 + (a ^ b ^ c) + 0xCA62C1D6 ;
    a = (a << 30) | (a >>> 2) ;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    c += ((d << 5) | ( d >>> 27)) + w13 + (e ^ a ^ b) + 0xCA62C1D6 ;
    e = (e << 30) | (e >>> 2) ;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    b += ((c << 5) | ( c >>> 27)) + w14 + (d ^ e ^ a) + 0xCA62C1D6 ;
    d = (d << 30) | (d >>> 2) ;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    a += ((b << 5) | ( b >>> 27)) + w15 + (c ^ d ^ e) + 0xCA62C1D6 ;
    c = (c << 30) | (c >>> 2) ;

    h0 += a;
    h1 += b;
    h2 += c;
    h3 += d;
    h4 += e;
  }
  
  /*
  private void transform(ByteBuffer M) {
  	int w0 , w1 , w2 , w3 ,  w4 , w5 , w6 , w7 , w8 , w9 ,
    w10, w11, w12, w13, w14, w15;
  	
    int a,b,c,d,e,temp;
    
    w0 = M.getInt();
    w1 = M.getInt();
    w2 = M.getInt();
    w3 = M.getInt();
    w4 = M.getInt();
    w5 = M.getInt();
    w6 = M.getInt();
    w7 = M.getInt();
    w8 = M.getInt();
    w9 = M.getInt();
    w10 = M.getInt();
    w11 = M.getInt();
    w12 = M.getInt();
    w13 = M.getInt();
    w14 = M.getInt();
    w15 = M.getInt();
    
    a = h0 ; b = h1 ; c = h2 ; d = h3 ; e = h4;
    
    temp = ((a << 5) | (a >>> 27)) + e + w0 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    
    temp = ((a << 5) | (a >>> 27)) + e + w1 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w0 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w1 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w0 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w1 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w0 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w1 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w0 = w13 ^ w8 ^ w2 ^ w0; w0 = (w0 << 1) | (w0 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w0 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w1 = w14 ^ w9 ^ w3 ^ w1; w1 = (w1 << 1) | (w1 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w1 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w2 = w15 ^ w10 ^ w4 ^ w2; w2 = (w2 << 1) | (w2 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w3 = w0 ^ w11 ^ w5 ^ w3; w3 = (w3 << 1) | (w3 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w4 = w1 ^ w12 ^ w6 ^ w4; w4 = (w4 << 1) | (w4 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w5 = w2 ^ w13 ^ w7 ^ w5; w5 = (w5 << 1) | (w5 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w6 = w3 ^ w14 ^ w8 ^ w6; w6 = (w6 << 1) | (w6 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w7 = w4 ^ w15 ^ w9 ^ w7; w7 = (w7 << 1) | (w7 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w8 = w5 ^ w0 ^ w10 ^ w8; w8 = (w8 << 1) | (w8 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w9 = w6 ^ w1 ^ w11 ^ w9; w9 = (w9 << 1) | (w9 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w10 = w7 ^ w2 ^ w12 ^ w10; w10 = (w10 << 1) | (w10 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w11 = w8 ^ w3 ^ w13 ^ w11; w11 = (w11 << 1) | (w11 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w12 = w9 ^ w4 ^ w14 ^ w12; w12 = (w12 << 1) | (w12 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w13 = w10 ^ w5 ^ w15 ^ w13; w13 = (w13 << 1) | (w13 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w14 = w11 ^ w6 ^ w0 ^ w14; w14 = (w14 << 1) | (w14 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;
    w15 = w12 ^ w7 ^ w1 ^ w15; w15 = (w15 << 1) | (w15 >>> 31) ;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;

    h0 += a;
    h1 += b;
    h2 += c;
    h3 += d;
    h4 += e; 

  	
  }
  
  private void transform(ByteBuffer M) {
    int w0 , w1 , w2 , w3 ,  w4 , w5 , w6 , w7 , w8 , w9 ,
    w10, w11, w12, w13, w14, w15, w16, w17, w18, w19 ,
    w20, w21, w22, w23, w24, w25, w26, w27, w28, w29 ,
    w30, w31, w32, w33, w34, w35, w36, w37, w38, w39 ,
    w40, w41, w42, w43, w44, w45, w46, w47, w48, w49 ,
    w50, w51, w52, w53, w54, w55, w56, w57, w58, w59 ,
    w60, w61, w62, w63, w64, w65, w66, w67, w68, w69 ,
    w70, w71, w72, w73, w74, w75, w76, w77, w78, w79;
    
    int a,b,c,d,e;
    
    int temp;
    
    w0 = M.getInt();
    w1 = M.getInt();
    w2 = M.getInt();
    w3 = M.getInt();
    w4 = M.getInt();
    w5 = M.getInt();
    w6 = M.getInt();
    w7 = M.getInt();
    w8 = M.getInt();
    w9 = M.getInt();
    w10 = M.getInt();
    w11 = M.getInt();
    w12 = M.getInt();
    w13 = M.getInt();
    w14 = M.getInt();
    w15 = M.getInt();
    
    w16 = w13 ^ w8 ^ w2 ^ w0;
    w16 = (w16 << 1) | (w16 >>> 31);
    w17 = w14 ^ w9 ^ w3 ^ w1;
    w17 = (w17 << 1) | (w17 >>> 31);
    w18 = w15 ^ w10 ^ w4 ^ w2;
    w18 = (w18 << 1) | (w18 >>> 31);
    w19 = w16 ^ w11 ^ w5 ^ w3;
    w19 = (w19 << 1) | (w19 >>> 31);
    w20 = w17 ^ w12 ^ w6 ^ w4;
    w20 = (w20 << 1) | (w20 >>> 31);
    w21 = w18 ^ w13 ^ w7 ^ w5;
    w21 = (w21 << 1) | (w21 >>> 31);
    w22 = w19 ^ w14 ^ w8 ^ w6;
    w22 = (w22 << 1) | (w22 >>> 31);
    w23 = w20 ^ w15 ^ w9 ^ w7;
    w23 = (w23 << 1) | (w23 >>> 31);
    w24 = w21 ^ w16 ^ w10 ^ w8;
    w24 = (w24 << 1) | (w24 >>> 31);
    w25 = w22 ^ w17 ^ w11 ^ w9;
    w25 = (w25 << 1) | (w25 >>> 31);
    w26 = w23 ^ w18 ^ w12 ^ w10;
    w26 = (w26 << 1) | (w26 >>> 31);
    w27 = w24 ^ w19 ^ w13 ^ w11;
    w27 = (w27 << 1) | (w27 >>> 31);
    w28 = w25 ^ w20 ^ w14 ^ w12;
    w28 = (w28 << 1) | (w28 >>> 31);
    w29 = w26 ^ w21 ^ w15 ^ w13;
    w29 = (w29 << 1) | (w29 >>> 31);
    w30 = w27 ^ w22 ^ w16 ^ w14;
    w30 = (w30 << 1) | (w30 >>> 31);
    w31 = w28 ^ w23 ^ w17 ^ w15;
    w31 = (w31 << 1) | (w31 >>> 31);
    w32 = w29 ^ w24 ^ w18 ^ w16;
    w32 = (w32 << 1) | (w32 >>> 31);
    w33 = w30 ^ w25 ^ w19 ^ w17;
    w33 = (w33 << 1) | (w33 >>> 31);
    w34 = w31 ^ w26 ^ w20 ^ w18;
    w34 = (w34 << 1) | (w34 >>> 31);
    w35 = w32 ^ w27 ^ w21 ^ w19;
    w35 = (w35 << 1) | (w35 >>> 31);
    w36 = w33 ^ w28 ^ w22 ^ w20;
    w36 = (w36 << 1) | (w36 >>> 31);
    w37 = w34 ^ w29 ^ w23 ^ w21;
    w37 = (w37 << 1) | (w37 >>> 31);
    w38 = w35 ^ w30 ^ w24 ^ w22;
    w38 = (w38 << 1) | (w38 >>> 31);
    w39 = w36 ^ w31 ^ w25 ^ w23;
    w39 = (w39 << 1) | (w39 >>> 31);
    w40 = w37 ^ w32 ^ w26 ^ w24;
    w40 = (w40 << 1) | (w40 >>> 31);
    w41 = w38 ^ w33 ^ w27 ^ w25;
    w41 = (w41 << 1) | (w41 >>> 31);
    w42 = w39 ^ w34 ^ w28 ^ w26;
    w42 = (w42 << 1) | (w42 >>> 31);
    w43 = w40 ^ w35 ^ w29 ^ w27;
    w43 = (w43 << 1) | (w43 >>> 31);
    w44 = w41 ^ w36 ^ w30 ^ w28;
    w44 = (w44 << 1) | (w44 >>> 31);
    w45 = w42 ^ w37 ^ w31 ^ w29;
    w45 = (w45 << 1) | (w45 >>> 31);
    w46 = w43 ^ w38 ^ w32 ^ w30;
    w46 = (w46 << 1) | (w46 >>> 31);
    w47 = w44 ^ w39 ^ w33 ^ w31;
    w47 = (w47 << 1) | (w47 >>> 31);
    w48 = w45 ^ w40 ^ w34 ^ w32;
    w48 = (w48 << 1) | (w48 >>> 31);
    w49 = w46 ^ w41 ^ w35 ^ w33;
    w49 = (w49 << 1) | (w49 >>> 31);
    w50 = w47 ^ w42 ^ w36 ^ w34;
    w50 = (w50 << 1) | (w50 >>> 31);
    w51 = w48 ^ w43 ^ w37 ^ w35;
    w51 = (w51 << 1) | (w51 >>> 31);
    w52 = w49 ^ w44 ^ w38 ^ w36;
    w52 = (w52 << 1) | (w52 >>> 31);
    w53 = w50 ^ w45 ^ w39 ^ w37;
    w53 = (w53 << 1) | (w53 >>> 31);
    w54 = w51 ^ w46 ^ w40 ^ w38;
    w54 = (w54 << 1) | (w54 >>> 31);
    w55 = w52 ^ w47 ^ w41 ^ w39;
    w55 = (w55 << 1) | (w55 >>> 31);
    w56 = w53 ^ w48 ^ w42 ^ w40;
    w56 = (w56 << 1) | (w56 >>> 31);
    w57 = w54 ^ w49 ^ w43 ^ w41;
    w57 = (w57 << 1) | (w57 >>> 31);
    w58 = w55 ^ w50 ^ w44 ^ w42;
    w58 = (w58 << 1) | (w58 >>> 31);
    w59 = w56 ^ w51 ^ w45 ^ w43;
    w59 = (w59 << 1) | (w59 >>> 31);
    w60 = w57 ^ w52 ^ w46 ^ w44;
    w60 = (w60 << 1) | (w60 >>> 31);
    w61 = w58 ^ w53 ^ w47 ^ w45;
    w61 = (w61 << 1) | (w61 >>> 31);
    w62 = w59 ^ w54 ^ w48 ^ w46;
    w62 = (w62 << 1) | (w62 >>> 31);
    w63 = w60 ^ w55 ^ w49 ^ w47;
    w63 = (w63 << 1) | (w63 >>> 31);
    w64 = w61 ^ w56 ^ w50 ^ w48;
    w64 = (w64 << 1) | (w64 >>> 31);
    w65 = w62 ^ w57 ^ w51 ^ w49;
    w65 = (w65 << 1) | (w65 >>> 31);
    w66 = w63 ^ w58 ^ w52 ^ w50;
    w66 = (w66 << 1) | (w66 >>> 31);
    w67 = w64 ^ w59 ^ w53 ^ w51;
    w67 = (w67 << 1) | (w67 >>> 31);
    w68 = w65 ^ w60 ^ w54 ^ w52;
    w68 = (w68 << 1) | (w68 >>> 31);
    w69 = w66 ^ w61 ^ w55 ^ w53;
    w69 = (w69 << 1) | (w69 >>> 31);
    w70 = w67 ^ w62 ^ w56 ^ w54;
    w70 = (w70 << 1) | (w70 >>> 31);
    w71 = w68 ^ w63 ^ w57 ^ w55;
    w71 = (w71 << 1) | (w71 >>> 31);
    w72 = w69 ^ w64 ^ w58 ^ w56;
    w72 = (w72 << 1) | (w72 >>> 31);
    w73 = w70 ^ w65 ^ w59 ^ w57;
    w73 = (w73 << 1) | (w73 >>> 31);
    w74 = w71 ^ w66 ^ w60 ^ w58;
    w74 = (w74 << 1) | (w74 >>> 31);
    w75 = w72 ^ w67 ^ w61 ^ w59;
    w75 = (w75 << 1) | (w75 >>> 31);
    w76 = w73 ^ w68 ^ w62 ^ w60;
    w76 = (w76 << 1) | (w76 >>> 31);
    w77 = w74 ^ w69 ^ w63 ^ w61;
    w77 = (w77 << 1) | (w77 >>> 31);
    w78 = w75 ^ w70 ^ w64 ^ w62;
    w78 = (w78 << 1) | (w78 >>> 31);
    w79 = w76 ^ w71 ^ w65 ^ w63;
    w79 = (w79 << 1) | (w79 >>> 31);
    
    a = h0;
    b = h1;
    c = h2;
    d = h3;
    e = h4;
    
    temp = ((a << 5) | (a >>> 27)) + e + w0 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w1 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w2 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w3 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w4 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w5 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w6 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w7 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w8 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w9 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w10 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w11 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w12 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w13 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w14 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w15 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w16 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w17 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w18 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w19 + ((b & c) | ((~b) & d)) + 0x5A827999 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w20 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w21 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w22 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w23 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w24 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w25 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w26 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w27 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w28 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w29 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w30 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w31 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w32 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w33 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w34 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w35 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w36 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w37 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w38 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w39 + (b ^ c ^ d) + 0x6ED9EBA1 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w40 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w41 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w42 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w43 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w44 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w45 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w46 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w47 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w48 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w49 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w50 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w51 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w52 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w53 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w54 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w55 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w56 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w57 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w58 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w59 + ((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w60 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w61 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w62 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w63 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w64 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w65 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w66 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w67 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w68 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w69 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w70 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w71 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w72 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w73 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w74 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w75 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w76 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w77 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w78 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;
    temp = ((a << 5) | (a >>> 27)) + e + w79 + (b ^ c ^ d) + 0xCA62C1D6 ;
    e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp; 
    
    h0 += a;
    h1 += b;
    h2 += c;
    h3 += d;
    h4 += e;
  }
  
  */
  
  public void reset() {
    h0 = 0x67452301;
    h1 = 0xEFCDAB89;
    h2 = 0x98BADCFE;
    h3 = 0x10325476;    
    h4 = 0xC3D2E1F0;    
    
    length = 0;
    
    finalBuffer.position(0);
  }
  
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
  
  public byte[] digest() {
    byte[] result = new byte[20];
    
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
    finalBuffer.putInt(h4);    
    finalBuffer.position(0);
    
    for(int i  = 0 ; i < 20 ; i++) {
     result[i] = finalBuffer.get(); 
    }
    
    return result;
  }
  
  public  void saveState() {
    s0=h0;
    s1=h1;
    s2=h2;
    s3=h3;
    s4=h4;
    
    saveLength = length;
    
    saveBuffer.position(0);
    int position = finalBuffer.position();
    
    finalBuffer.position(0);
    finalBuffer.limit(position);
    
    saveBuffer.put(finalBuffer);
    saveBuffer.position(0);
    saveBuffer.limit(position);
    
    finalBuffer.limit(64);
  }
  
  public void restoreState() {
    h0=s0;
    h1=s1;
    h2=s2;
    h3=s3;
    h4=s4;
    
    length = saveLength;
    
    finalBuffer.position(0);
    finalBuffer.limit(64);
    finalBuffer.put(saveBuffer);
  }
 
  private void completeFinalBuffer(ByteBuffer buffer) {
    if(finalBuffer.position() == 0) 
      return;
    
    int remaining = buffer.remaining();
    
    while(buffer.remaining() > 0 && finalBuffer.remaining() > 0) {
      finalBuffer.put(buffer.get());
    }
    
    if(finalBuffer.remaining() == 0) {
      finalBuffer.position(0);
      transform(finalBuffer);
      finalBuffer.position(0);
    }
  }

  public byte[] digest(ByteBuffer buffer) {
    byte[] result = new byte[20];
    int length = buffer.remaining();
    int totalLength = 64 * ((length + 63) / 64);
    if(totalLength > buffer.capacity() + buffer.position()) {
     System.err.println("BUFFER IS TOO SMALL TO BE HASHED. SHA-1 ERROR. RETURN INVALID");
     return result;
    }
    buffer.mark();
    int position = buffer.position();    
    buffer.limit(position + totalLength);
    buffer.position(length);
    int needsFill = totalLength - 9 - length;
    if(needsFill >= 0) {
      buffer.put((byte)0x80);
      for(int i = 0 ; i < needsFill ; i++) {
       buffer.put((byte)0);       
      }
      buffer.putLong((length << 3));     
    } else {
      finalBuffer.position(0);
       if(totalLength != length) {
        buffer.put((byte)0x80);
        while(buffer.remaining() > 0) {
         buffer.put((byte)0); 
        }
       } else {
        finalBuffer.put((byte) 0x80);
       }
      while(finalBuffer.remaining() > 8) {
       finalBuffer.put((byte)0); 
      }
      finalBuffer.putLong(length << 3);
      finalBuffer.rewind();
    }
    
    reset();
    buffer.position(position);
    while(buffer.remaining() > 0) {
      transform(buffer);
    }
    if(needsFill < 0) {
      transform(finalBuffer);
    }
    finalBuffer.rewind();
    finalBuffer.putInt(h0);
    finalBuffer.putInt(h1);
    finalBuffer.putInt(h2);
    finalBuffer.putInt(h3);
    finalBuffer.putInt(h4);
    finalBuffer.rewind();
    
    for(int i  = 0 ; i < 20 ; i++) {
     result[i] = finalBuffer.get(); 
    }
    
    buffer.position(position);
    buffer.limit(position + length);
    
    return result;
  }
  
  public static void main(String args[]) {
   SHA1 sha1Ref = new SHA1();
   SHA1Az sha1Test = new SHA1Az();
   
   ByteBuffer buffer = ByteBuffer.allocateDirect(16384);
   
   int lTest = 8178 ;//+ (int)(8192 * Math.random());
   for(int i = 0 ; i < lTest ; i++) {
    byte b = (byte) (Math.random() * 255);
    buffer.put(b);
   }
   buffer.position(0);
   buffer.limit(lTest);
   
   ByteBuffer result = ByteBuffer.allocate(20);
   System.out.println(buffer.position() + " - " + buffer.remaining());
   sha1Ref.update(buffer);
   sha1Ref.finalDigest(result);
   byte[] bResult = new byte[20];
   bResult = result.array();
   buffer.position(0);
   System.out.println(ByteFormatter.nicePrint(bResult));
   System.out.println(buffer.position() + " - " + buffer.remaining());
   System.out.println(ByteFormatter.nicePrint(sha1Test.digest(buffer)));
   System.out.println(buffer.position() + " - " + buffer.remaining());   
   sha1Test.reset();
   sha1Test.update(buffer);
   System.out.println(ByteFormatter.nicePrint(sha1Test.digest()));
   System.out.println(buffer.position() + " - " + buffer.remaining());
   
  }
    
  
  
  
  
}
