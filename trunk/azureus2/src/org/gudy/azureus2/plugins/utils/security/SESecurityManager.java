/*
 * Created on 17-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.plugins.utils.security;

/**
 * @author parg
 *
 */

import java.net.Authenticator;

public interface 
SESecurityManager 
{
		// runs the given task with the supplied Authenticator. Note that the 
		// scope of the authenticator is "vm-wide" so that if by chance another
		// thread attempts to perform an operation that requires authentication
		// which the supplied one is in force, the request will be directed to the
		// authenticator
	
	public void
	runWithAuthenticator(
		Authenticator	authenticator,
		Runnable		task );
	
		/**
		 * returns the SHA1 hash of the input data
		 * @param data_in
		 * @return
		 */
	
	public byte[]
	calculateSHA1(
		byte[]		data_in );
}
