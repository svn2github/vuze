/**
 * InputBuffer.java
 *
 * Copyright (c) 2001 Regents of the University of California.
 * All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 */

package org.gudy.azureus2.ui.web2.util;
import java.math.BigInteger;

/**
 * Efficient input buffer interface.
 *
 * @author	Sean C. Rhea
 * @version	$Id: InputBuffer.java,v 1.1 2003-12-01 00:58:01 belgabor Exp $
 */
public interface InputBuffer extends Cloneable {

    /**
     * Buffers must support clone such that the read pointer on a cloned
     * buffer is independent of the original.  This functionality is used
     * in Tapestry and will be used in SignedQS as well, I think.  It
     * relates to the need to deserialize a buffer twice.  Whether or not
     * we keep using it is subject to change.
     */
    public Object clone () throws CloneNotSupportedException;

    /**
     * Convert to to_bytes compatible form.  THIS FUNCTION SHOULD ONLY BE
     * CALLED FROM THE TYPETABLE.  Returns a new byte array with
     * the remaining (unread) buffer data in <code>out_buffer [0]</code>
     * and an offset into that buffer in <code>out_offset [0][0]</code>.
     * This function is intended to be used during porting from the old
     * interface to the new; it will go away once that port is complete.
     * This function MUST NOT increment the read pointer.
     * Use it like this:
     *
     * <pre>
     	byte data [][] = {{0}};
	int offset [][] = {{0}};
	buffer.convertToBytes (data, offset);
	...
	int foo = ByteUtils.bytesToInt (data [0], offset [0]);
	etc.
	</pre>
     */
    //public void convertToBytes (byte out_buffer [][], int out_offset [][]);

    /**
     * Skip the read pointer forward by count bytes.  THIS FUNCTION SHOULD
     * ONLY BE CALLED FROM THE TYPETABLE.  
     */
    //public void skipBytes (int count);

    /** 
     * Read the next byte out of the buffer.
     */
    public byte nextByte ();

    /**
     * Read the next <code>length</code> bytes out the of the buffer, and
     * place them in the array <code>output</code>, starting at index
     * <code>offset</code>.
     *
     * @param output the array into which the bytes are written
     * @param offset the index in <code>output</code> at which we start
     *               writing bytes
     * @param length the number of bytes to read
     */
    public void nextBytes (byte [] output, int offset, int length);

    /**
     * Write all of the remaining bytes in this input buffer into the given
     * output buffer.  We need something like this so that (for example)
     * the payload of an ostore.tapestry.impl.RouteMsg doesn't have to be
     * deserialized on an intermediate node in its path.  I'm not entirely
     * happy with the idea, though, so please don't use it for now without
     * talking to me first about what you're using it for.
     *
     * @param output the output buffer into which the bytes are written
     * @param length the number of bytes to read
     */
    public void nextBytes (OutputBuffer output);

    /**
     * Read the next short out of the buffer.
     */
    public short nextShort ();

    /**
     * Read the next integer out of the buffer.
     */
    public int nextInt ();

    /** 
     * Read the next long out of the buffer.
     */
    public long nextLong ();

    /** 
     * Read the next long out of the buffer.
     */
    public double nextDouble ();

    /**
     * Read the next boolean out of the buffer.
     */
    public boolean nextBoolean ();

    /**
     * Read the next String out of the buffer.
     */
    public String nextString ();

    /**
     * Read the next BigInteger out of the buffer.
     */
    public BigInteger nextBigInteger () throws QSException;

    /**
     * Read the next object out of the buffer.
     */
    //public QuickSerializable nextObject () throws QSException;

    /**
     * Create a new input buffer from the read point at this one,
     * containing the next length bytes.  Subject to change in the future.
     */
    public InputBuffer subBuffer (int length);

}


