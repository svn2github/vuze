/*
 * Created on 15 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.security.impl;

import java.security.SecureRandom;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.security.AECryptoHandler;
import com.aelitis.azureus.core.security.AESecurityManager;
import com.aelitis.azureus.core.security.AESecurityManagerPasswordHandler;

public class 
AESecurityManagerImpl 
	implements AESecurityManager
{
	private static AESecurityManagerImpl		singleton;
	
	public static synchronized AESecurityManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new AESecurityManagerImpl();
		}
		
		return( singleton );
	}
	
	private byte[]	secure_id;
	
	public byte[]
	getSecureID()
	{
		if ( secure_id == null ){
			
			secure_id = COConfigurationManager.getByteParameter( "core.crypto.id", null );
		}
		
		if ( secure_id == null ){
			
			secure_id = new byte[20];
		
			new SecureRandom().nextBytes( secure_id );
			
			COConfigurationManager.setParameter( "core.crypto.id", secure_id );
			
			COConfigurationManager.save();
		}
		
		return( secure_id );
	}
	
	public AECryptoHandler
	getECCHandler()
	{
		return( null );
	}
	
	public void
	addPasswordHandler(
		AESecurityManagerPasswordHandler		handler )
	{
		
	}
	
	public void
	removePasswordHandler(
		AESecurityManagerPasswordHandler		handler )
	{
		
	}
}
