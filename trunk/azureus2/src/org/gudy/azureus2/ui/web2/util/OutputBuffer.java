/**
 * OutputBuffer.java
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
 * A buffer into which <code>QuickSerializable</code> objects can be
 * written.  
 *
 * @see QuickSerializable
 *
 * @author  Sean C. Rhea
 * @version $Id: OutputBuffer.java,v 1.1 2003-12-01 00:58:01 belgabor Exp $
 */

public interface OutputBuffer
{
    /**
     * Adds the specified byte to the digest.
     */
    void add (byte input);

    /**
     * Adds the specified byte array to the digest.
     */
    void add (byte[] value);

    /**
     * Adds <code>length</code> bytes of the specified array to the digest,
     * starting at <code>offset</code>.
     */
    void add (byte[] value, int offset, int length);

    /**
     * Adds the specified object to the digest.
     * This method invokes the <code>QuickSerializable.serialize</code>
     * method on <code>value</code>; be careful to avoid infinite loops.
     */
    //void add (QuickSerializable value);

    /**
     * Adds the remaining bytes in the given input buffer to this output
     * buffer.
     */
    void add (InputBuffer buffer);

    void add (boolean b);

    void add (short i);

    void add (int i);

    void add (long i);

    void add (double x);

    void add (String s);

    void add (BigInteger value);
}

