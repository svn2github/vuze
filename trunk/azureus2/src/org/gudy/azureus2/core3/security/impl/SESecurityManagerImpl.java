/*
 * File    : SECertificateHandlerImpl.java
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

package org.gudy.azureus2.core3.security.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;

import java.security.*;

import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.util.*;

public class 
SESecurityManagerImpl 
{
	protected static String	keystore;
	protected static String	truststore;
	
	protected static List	certificate_listeners 	= new ArrayList();
	protected static List	password_listeners 		= new ArrayList();
	
	public static void
	initialise()
	{
		// 	keytool -genkey -keystore %home%\.keystore -keypass changeit -storepass changeit -keyalg rsa -alias azureus

		// 	keytool -export -keystore %home%\.keystore -keypass changeit -storepass changeit -alias azureus -file azureus.cer

		// 	keytool -import -keystore %home%\.certs -alias azureus -file azureus.cer			
	
		// debug SSL with -Djavax.net.debug=ssl
	
		keystore 	= FileUtil.getUserFile(SESecurityManager.SSL_KEYS).getAbsolutePath();
		truststore 	= FileUtil.getUserFile(SESecurityManager.SSL_CERTS).getAbsolutePath();
		
		System.setProperty( "javax.net.ssl.trustStore", truststore );
	
		System.setProperty( "javax.net.ssl.trustStorePassword", SESecurityManager.SSL_PASSWORD );
	}
	
	public static SSLServerSocketFactory
	getSSLServerSocketFactory()
	
		throws Exception
	{
		Security.addProvider((java.security.Provider)
				Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
		
		SSLContext context = SSLContext.getInstance( "SSL" );
		
		// Create the key manager factory used to extract the server key
		
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		
		KeyStore key_store = KeyStore.getInstance("JKS");
		
		InputStream kis;
		
		kis = new FileInputStream(keystore);
		
		key_store.load(kis, SESecurityManager.SSL_PASSWORD.toCharArray());
		
		kis.close();
		
		keyManagerFactory.init(key_store, SESecurityManager.SSL_PASSWORD.toCharArray());
		
		// Initialize the context with the key managers
		
		context.init(  	keyManagerFactory.getKeyManagers(), 
				null,
				new java.security.SecureRandom());
		
		SSLServerSocketFactory factory = context.getServerSocketFactory();
		
		return( factory );
	}
	
	public static synchronized boolean
	installServerCertificates(
		URL		https_url )
	{
		String	host	= https_url.getHost();
		int		port	= https_url.getPort();
		
		if ( port == -1 ){
			port = 443;
		}
		
		SSLSocket	socket = null;
		
		try{
	
				// to get the server certs we have to use an "all trusting" trust manager
			
			TrustManager[] trustAllCerts = new TrustManager[]{
						new X509TrustManager() {
							public java.security.cert.X509Certificate[] getAcceptedIssuers() {
								return null;
							}
							public void checkClientTrusted(
									java.security.cert.X509Certificate[] certs, String authType) {
							}
							public void checkServerTrusted(
									java.security.cert.X509Certificate[] certs, String authType) {
							}
						}
					};
			
			SSLContext sc = SSLContext.getInstance("SSL");
			
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			
			SSLSocketFactory factory = sc.getSocketFactory();
					
			socket = (SSLSocket)factory.createSocket(host, port);
		
			socket.startHandshake();
			
			java.security.cert.Certificate[] serverCerts = socket.getSession().getPeerCertificates();
			
			if ( serverCerts.length == 0 ){
								
				return( false );
			}
			
			java.security.cert.Certificate	cert = serverCerts[0];
						
			java.security.cert.X509Certificate x509_cert;
			
			if ( cert instanceof java.security.cert.X509Certificate ){
				
				x509_cert = (java.security.cert.X509Certificate)cert;
				
			}else{
				
				java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
				
				x509_cert = (java.security.cert.X509Certificate)cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
			}
				
			String	resource = https_url.toString();
			
			int	param_pos = resource.indexOf("?");
			
			if ( param_pos != -1 ){
				
				resource = resource.substring(0,param_pos);
			}
			
			for (int i=0;i<certificate_listeners.size();i++){
				
				if (((SECertificateListener)certificate_listeners.get(i)).trustCertificate( resource, x509_cert )){
					
					String	alias = host.concat(":").concat(String.valueOf(port));
			
					addCertToTrustStore( alias, cert );
			
					return( true );
				}
			}
			
			return( false );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( false );
			
		}finally{
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static synchronized void
	addCertToTrustStore(
		String							alias,
		java.security.cert.Certificate	cert )
	
		throws Exception
	{
		FileInputStream		in 	= null;
		FileOutputStream	out = null;
				
		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			
			if ( !new File(truststore).exists()){
		
				keystore.load(null,null);
				
			}else{
			
				in = new FileInputStream(truststore);
			
				keystore.load(in, SESecurityManager.SSL_PASSWORD.toCharArray());				
			}
			
			if ( cert != null ){
				if ( keystore.containsAlias( alias )){
				
					keystore.deleteEntry( alias );
				}
							
				keystore.setCertificateEntry(alias, cert);
				
				out = new FileOutputStream(truststore);
			
				keystore.store(out, SESecurityManager.SSL_PASSWORD.toCharArray());
			}
			
				// pick up the changed trust store
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			
			tmf.init(keystore);
			
			SSLContext ctx = SSLContext.getInstance("SSL");
			
			ctx.init(null, tmf.getTrustManagers(), null);
						
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
			
		}finally{
			
			if ( in != null ){
				try{
					in.close();
				}catch( Throwable e ){
				}
			}
			if ( out != null ){
				try{
					out.close();
				}catch( Throwable e ){
				}
			}
			
		}
	}
	
	public static PasswordAuthentication
	getPasswordAuthentication(
		String		realm,
		URL			tracker )
	{
		for (int i=0;i<password_listeners.size();i++){
			
			PasswordAuthentication res = ((SEPasswordListener)password_listeners.get(i)).getAuthentication( realm, tracker );
			
			if ( res != null ){
				
				return( res );
			}
		}
		
		return( null );
	}
	
	public static synchronized void
	addPasswordListener(
		SEPasswordListener	l )
	{
		password_listeners.add(l);
	}	
	
	public static synchronized void
	removePasswordListener(
		SEPasswordListener	l )
	{
		password_listeners.remove(l);
	}
	
	public static synchronized void
	addCertificateListener(
		SECertificateListener	l )
	{
		certificate_listeners.add(l);
	}	
	
	public static synchronized void
	removeCertificateListener(
		SECertificateListener	l )
	{
		certificate_listeners.remove(l);
	}
}
