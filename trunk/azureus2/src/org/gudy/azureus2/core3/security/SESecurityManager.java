/*
 * File    : SESecurityManager.java
 * Created : 29-Dec-2003
 * By      : parg
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

package org.gudy.azureus2.core3.security;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.net.PasswordAuthentication;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.security.impl.*;

public class 
SESecurityManager
{
	// SSL client defaults
	
	public static final String SSL_CERTS 		= ".certs";
	public static final String SSL_KEYS			= ".keystore";
	public static final String SSL_PASSWORD 	= "changeit";
	
	public static final String DEFAULT_ALIAS	= "Azureus";
	
	public static void
	initialise()
	{
		SESecurityManagerImpl.getSingleton().initialise();
	}
	
	public static void
	exitVM(
		int		status )
	{
		SESecurityManagerImpl.getSingleton().exitVM(status);
	}
	
	public static void
	installAuthenticator()
	{
		SESecurityManagerImpl.getSingleton().installAuthenticator();
	}
	
	public static String
	getKeystoreName()
	{
		return(	SESecurityManagerImpl.getSingleton().getKeystoreName());
	}
	
	public static String
	getKeystorePassword()
	{
		return(	SESecurityManagerImpl.getSingleton().getKeystorePassword());
	}
	
	public static SSLServerSocketFactory
	getSSLServerSocketFactory()
	
		throws Exception
	{
		return( SESecurityManagerImpl.getSingleton().getSSLServerSocketFactory());
	}
	
	public static boolean
	installServerCertificates(
		URL		https_url )
	{
		return( SESecurityManagerImpl.getSingleton().installServerCertificates(https_url));
	}
	
	public static void
	createSelfSignedCertificate(
		String		alias,
		String		cert_dn,
		int			strength )
	
		throws Exception
	{
		SESecurityManagerImpl.getSingleton().createSelfSignedCertificate( alias, cert_dn, strength );
	}
	
	public static SEKeyDetails
	getKeyDetails(
		String	alias )
	
		throws Exception
	{
		return( SESecurityManagerImpl.getSingleton().getKeyDetails( alias ));
	}
	
	public static PasswordAuthentication
	getPasswordAuthentication(
		String		realm,
		URL			tracker )
	{
		return( SESecurityManagerImpl.getSingleton().getPasswordAuthentication(realm, tracker));	
	}
	
	public static void
	setPasswordAuthenticationOutcome(
		String		realm,
		URL			tracker,
		boolean		success )
	{
		SESecurityManagerImpl.getSingleton().setPasswordAuthenticationOutcome(realm, tracker, success);	
	}
		
	public static void
	addPasswordListener(
		SEPasswordListener	l )
	{
		SESecurityManagerImpl.getSingleton().addPasswordListener(l);	
	}	
	
	public static void
	removePasswordListener(
		SEPasswordListener	l )
	{
		SESecurityManagerImpl.getSingleton().removePasswordListener(l);	
	}
	
	public static void
	addPasswordHandler(
		URL						url,
		SEPasswordListener		l )
	{
		SESecurityManagerImpl.getSingleton().addPasswordHandler( url, l );
	}
	
	public static void
	removePasswordHandler(
		URL						url,
		SEPasswordListener		l )
	{
		SESecurityManagerImpl.getSingleton().removePasswordHandler( url, l );
	}
	
	public static void
	addCertificateListener(
			SECertificateListener	l )
	{
		SESecurityManagerImpl.getSingleton().addCertificateListener(l);
	}	
	
	public static void
	removeCertificateListener(
		SECertificateListener	l )
	{
		SESecurityManagerImpl.getSingleton().removeCertificateListener(l);
	}
}