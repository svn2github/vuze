package org.gudy.azureus2.core;

/*
 * @(#)MessageDigest.java	1.7 95/08/15
 *
 * Copyright (c) 1994 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * Updated to JDK 1.0.2 levels by Chuck McManis
 */

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Random;


/**
 * The MessageDigest class defines a general class for computing digest
 * functions. It is defined as an abstract class that is subclassed by
 * message digest algorithms. In this way the PKCS classes can be built
 * to take a MessageDigest object without needing to know what 'kind'
 * of message digest they are computing.
 *
 * This class defines the standard functions that all message digest
 * algorithms share, and ways to put all Java fundamental types into
 * the digest. It does not define methods for digestifying either
 * arbitrary objects or arrays of objects however.
 *
 * @version 	5 Oct 1996, 1.8
 * @author 	Chuck McManis
 */
public abstract class MessageDigest {

    /** the actual digest bits. */
    public byte digestBits[];

    /** status of the digest */
    public boolean digestValid;

    /**
     * This function is used to initialize any internal digest
     * variables or parameters.
     */
    public abstract void init();

    /**
     * The basic unit of digestifying is the byte. This method is
     * defined by the particular algorithim's subclass for that
     * algorithim. Subsequent versions of this method defined here
     * decompose the basic type into bytes and call this function.
     * If special processing is needed for a particular type your
     * subclass should override the method for that type.
     */
    public abstract void update(byte aValue);

    /**
     * Add a boolean to the digest.
     */
    public synchronized void update(boolean aValue) {
        byte	b;

        if (aValue)
            b = 1;
        else
            b = 0;
        update(b);
    }

    /**
     * Add a short value to the digest.
     */
    public synchronized void update(short aValue) {
        byte	b1, b2;

        b1 = (byte)((aValue >>> 8) & 0xff);
        b2 = (byte)(aValue & 0xff);
        update(b1);
        update(b2);
    }

    /**
     * Add an integer value to the digest.
     */
    public synchronized void update(int aValue) {
        byte	b;

        for (int i = 3; i >= 0; i--) {
            b = (byte)((aValue >>> (i * 8)) & 0xff);
            update(b);
        }
    }

    /**
     * Add a long to the digest.
     */
    public synchronized void update(long aValue) {
        byte	b;

    	for (int i = 7; i >= 0; i--) {
    	    b = (byte)((aValue >>> (i * 8)) & 0xff);
    	    update(b);
    	}
    }

    /**
     * Add specific bytes to the digest.
     */
    public synchronized void update(byte input[], int offset, int len) {
        for (int i = 0; i < len; i++) {
            update(input[i+offset]);
        }
    }

    /**
     * Add an array of bytes to the digest.
     */
    public synchronized void update(byte input[]) {
        update(input, 0, input.length);
    }

    /**
     * Add an array of shorts to the digest.
     */
    public synchronized void update(short input[]) {
    	for (int i = 0; i < input.length; i++) {
    	    update(input[i]);
    	}
    }

    /**
     * Add an array of integers to the digest.
     */
    public synchronized void update(int input[]) {
    	for (int i = 0; i < input.length; i++) {
    	    update(input[i]);
    	}
    }

    /**
     * Add an array of longs to the digest.
     */
    public synchronized void update(long input[]) {
    	for (int i = 0; i < input.length; i++) {
    	    update(input[i]);
    	}
    }

    /**
     * Add the bytes in the String 'input' to the current digest.
     * Note that the string characters are treated as unicode chars
     * of 16 bits each. To digestify ISO-Latin1 strings (ASCII) use
     * the updateASCII() method.
     */
    public void update(String input) {
    	int	i, len;
    	short	x;

    	len = input.length();
    	for (i = 0; i < len; i++) {
    	    x = (short) input.charAt(i);
    	    update(x);
    	}
    }

    /**
     * Treat the string as a sequence of ISO-Latin1 (8 bit) characters.
     */
    public void updateASCII(String input) {
    	int	i, len;
    	byte	x;

    	len = input.length();
    	for (i = 0; i < len; i++) {
    	    x = (byte) (input.charAt(i) & 0xff);
    	    update(x);
    	}
    }

    /**
     * Read a stream and update a digest until the
     * stream sends an EOF.
     */
    public void update(InputStream in) {
    	byte	b;

        try {
    	    while ((b = (byte) in.read()) != -1)
    	        update(b);
    	} catch (Exception e) { }
    }

    /**
     * Perform the final computations and cleanup.
     */
    public abstract void finish();

    /**
     * Complete digest computation on an array of bytes.
     */
    public void computeDigest(byte source[]) {
    	init();
    	update(source);
    	finish();
    }

    public void computeDigest(InputStream in) {
    	init();
    	update(in);
    	finish();
    }

    /**
     * helper function that prints unsigned two character hex digits.
     */
    private void hexDigit(PrintStream p, byte x) {
    	char c;

    	c = (char) ((x >> 4) & 0xf);
    	if (c > 9)
    		c = (char) ((c - 10) + 'A');
    	else
    		c = (char) (c + '0');
    	p.write(c);
    	c = (char) (x & 0xf);
    	if (c > 9)
    		c = (char)((c-10) + 'A');
    	else
    		c = (char)(c + '0');
    	p.write(c);
    }

    /**
     * Return a string representation of this object.
     */
    public String toString() {
    	ByteArrayOutputStream ou = new ByteArrayOutputStream();
    	PrintStream p = new PrintStream(ou);

    	p.print(this.getClass().getName()+" Message Digest ");
    	if (digestValid) {
    	    p.print("<");
    	    for(int i = 0; i < digestBits.length; i++)
     	        hexDigit(p, digestBits[i]);
    	    p.print(">");
    	} else {
    	    p.print("<incomplete>");
    	}
    	p.println();
    	return (ou.toString());
    }

    /**
     * Compare two digests for equality. Simple byte compare.
     */
    public static boolean isEqual(byte digesta[], byte digestb[]) {
    	int	i;

    	if (digesta.length != digestb.length)
    	    return (false);

    	for (i = 0; i < digesta.length; i++) {
    	    if (digesta[i] != digestb[i]) {
    		return (false);
    	    }
    	}
    	return (true);
    }

    /**
     * Non static version that compares this digest to one passed.
     */
    public boolean isEqual(byte otherDigest[]) {
	    return (MessageDigest.isEqual(digestBits, otherDigest));
    }

    /**
     * Return a string that represents the algorithim of this
     * message digest.
     */
    public abstract String getAlg();

    static byte testdata[];

    public static void benchmark(MessageDigest md) {
        Random rnd = new Random(); // use random data
        testdata = new byte[16384];
        long times[] = new long[14];
        double rates[] = new double[14];

        for (int i = 0; i < testdata.length; i++) {
            testdata[i] = (byte) (rnd.nextInt() >>> 8);
        }
        System.out.println("Benchmarking "+md.getAlg());
        System.out.println("Bytes   Time (mS)   Rate (Bytes/Sec)");
        for (int i = 1; i < 10; i++) {
            long t1 = System.currentTimeMillis();
            md.init();
            for (int k = 0; k < i; k++)
                md.update(testdata);
            md.finish();
            times[i] = System.currentTimeMillis() - t1;
            rates[i] = ((i*testdata.length) * 1000.0) / times[i];
            System.out.println((i*testdata.length)+"\t"+times[i]+"\t"+rates[i]);
        }
        System.out.println("Done.");
    }

}

