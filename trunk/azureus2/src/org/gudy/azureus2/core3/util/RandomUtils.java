/*
 * Created on Dec 30, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.core3.util;

import java.util.Random;

/**
 * 
 */
public class RandomUtils {
	public static final Random RANDOM = new Random( System.currentTimeMillis() );
	
	
	/**
	 * Generate a random array of bytes.
	 * @param num_to_generate number of bytes to generate
	 * @return random byte array
	 */
	public static byte[] generateRandomBytes( int num_to_generate ) {
    byte[] id = new byte[ num_to_generate ];
    RANDOM.nextBytes( id );    
    return id;
  }
	
	
	/**
	 * Generate a random string of charactors.
	 * @param num_to_generate number of chars to generate
	 * @return random char string
	 */
	public static String generateRandomAlphanumerics( int num_to_generate ) {
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		
		StringBuffer buff = new StringBuffer( num_to_generate );
		
    for( int i=0; i < num_to_generate; i++ ) {
    	int pos = (int)( RANDOM.nextDouble() * alphabet.length() );
    	buff.append( alphabet.charAt( pos ) );
    }
    
    return buff.toString();
  }
	
	
	
	/**
	 * Generate a random port number for binding a network IP listening socket to.
	 * NOTE: Will return a valid non-privileged port number >= 10000 and <= 65535.
	 * @return random port number
	 */
	public static int generateRandomNetworkListenPort() {
		int min = 10000;
		int port = min + RANDOM.nextInt( 65536 - min );
		return port;
	}

}
