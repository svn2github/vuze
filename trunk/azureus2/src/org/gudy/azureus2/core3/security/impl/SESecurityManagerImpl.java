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
import java.security.cert.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.util.*;

public class 
SESecurityManagerImpl 
{
	protected static SESecurityManagerImpl	singleton = new SESecurityManagerImpl();
	
	protected String	keystore_name;
	protected String	truststore_name;
	
	protected List	certificate_listeners 	= new ArrayList();
	protected List	password_listeners 		= new ArrayList();
	
	protected Map	password_handlers		= new HashMap();
	
	protected boolean	 exit_vm_permitted	= false;
	
	
	protected AEMonitor	this_mon	= new AEMonitor( "SESecurityManager" );
	
	public static SESecurityManagerImpl
	getSingleton()
	{
		return( singleton );
	}
	
	public void
	initialise()
	{
		// 	keytool -genkey -keystore %home%\.keystore -keypass changeit -storepass changeit -keyalg rsa -alias azureus

		// 	keytool -export -keystore %home%\.keystore -keypass changeit -storepass changeit -alias azureus -file azureus.cer

		// 	keytool -import -keystore %home%\.certs -alias azureus -file azureus.cer			
	
		// debug SSL with -Djavax.net.debug=ssl
	
		keystore_name 	= FileUtil.getUserFile(SESecurityManager.SSL_KEYS).getAbsolutePath();
		truststore_name 	= FileUtil.getUserFile(SESecurityManager.SSL_CERTS).getAbsolutePath();
		
		System.setProperty( "javax.net.ssl.trustStore", truststore_name );
	
		System.setProperty( "javax.net.ssl.trustStorePassword", SESecurityManager.SSL_PASSWORD );
		
		
		installAuthenticator();
		
		try{
			Security.addProvider((java.security.Provider)
				Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		try{
			SESecurityManagerBC.initialise();
			
		}catch( Throwable e ){
			
			LGLogger.log( LGLogger.ERROR, "Bouncy Castle not available" );
		}
		
		installSecurityManager();
	}
	
	public String
	getKeystoreName()
	{
		return( keystore_name );
	}
	
	public String
	getKeystorePassword()
	{
		return(	SESecurityManager.SSL_PASSWORD );	
	}
	
	protected void
	installSecurityManager()
	{
		try{
			final SecurityManager	old_sec_man	= System.getSecurityManager();
			
			System.setSecurityManager(
				new SecurityManager()
				{
					public void 
					checkExit(int status) 
					{
						if ( old_sec_man != null ){
						
							old_sec_man.checkExit( status );
						}
						
						if ( !exit_vm_permitted ){
							
							throw( new SecurityException( "VM exit operation prohibited"));
						}
					}
					
					public void 
					checkPermission(Permission perm)
					{
						if ( old_sec_man != null ){
							
							old_sec_man.checkPermission( perm );
						}
					}
					
					public void 
					checkPermission(
						Permission 	perm, 
						Object 		context) 
					{
						if ( old_sec_man != null ){
							
							old_sec_man.checkPermission( perm, context );
						}
					}
	
				});
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public void
	exitVM(
		int		status )
	{
		try{
			exit_vm_permitted	= true;
			
			System.exit( status );
			
		}finally{
			
			exit_vm_permitted	= false;
		}
	}
	
	public void
	installAuthenticator()
	{
		Authenticator.setDefault(
				new Authenticator()
				{
					protected AEMonitor	auth_mon = new AEMonitor( "SESecurityManager:auth");
					
					protected PasswordAuthentication
					getPasswordAuthentication()
					{			
						try{
							auth_mon.enter();
						
							PasswordAuthentication	res =  
								getAuthentication( 
										getRequestingPrompt(),
										getRequestingProtocol(),
										getRequestingHost(),
										getRequestingPort());
							
							/*
							System.out.println( "Authenticator:getPasswordAuth: res = " + res );
							
							if ( res != null ){
								
								System.out.println( "    user = '" + res.getUserName() + "', pw = '" + new String(res.getPassword()) + "'" );
							}
							*/
							
							return( res );
							
						}finally{
							
							auth_mon.exit();
						}
					}
				});
	}
	
	public PasswordAuthentication
	getAuthentication(
		String		realm,
		String		protocol,
		String		host,
		int			port )
	{
		try{
			this_mon.enter();
			
			URL	tracker_url = new URL( protocol + "://" + host + ":" + port + "/" );
		
			return( getPasswordAuthentication( realm, tracker_url ));
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace( e );
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	checkKeyStoreHasEntry()
	{
		File	f  = new File(keystore_name);
		
		if ( !f.exists()){
			
			LGLogger.logAlertUsingResource( 
					LGLogger.AT_ERROR,
					"Security.keystore.empty",
					new String[]{ keystore_name });
			
			return( false );
		}
		
		try{
			KeyStore key_store = loadKeyStore();
			
			Enumeration enumx = key_store.aliases();
			
			if ( !enumx.hasMoreElements()){
				
				LGLogger.logAlertUsingResource( 
						LGLogger.AT_ERROR,
						"Security.keystore.empty",
						new String[]{ keystore_name });
				
				return( false );			
			}
			
		}catch( Throwable e ){
		
			LGLogger.logAlertUsingResource( 
					LGLogger.AT_ERROR,
					"Security.keystore.corrupt",
					new String[]{ keystore_name });
			
			return( false );			
		}
		
		return( true );
	}
	
	protected KeyStore
	loadKeyStore()
	
		throws Exception
	{
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		
		return( loadKeyStore( keyManagerFactory ));
	}
	
	protected KeyStore
	loadKeyStore(
		KeyManagerFactory	keyManagerFactory )
		
		throws Exception
	{
		KeyStore key_store = KeyStore.getInstance("JKS");
		
		if ( !new File(keystore_name).exists()){
			
			key_store.load(null,null);
			
		}else{
			
			InputStream kis = null;
			
			try{
				kis = new FileInputStream(keystore_name);
			
				key_store.load(kis, SESecurityManager.SSL_PASSWORD.toCharArray());
				
			}finally{
				
				if ( kis != null ){
					
					kis.close();
				}
			}
		}
		
		keyManagerFactory.init(key_store, SESecurityManager.SSL_PASSWORD.toCharArray());
		
		return( key_store );
	}
	
	public SSLServerSocketFactory
	getSSLServerSocketFactory()
	
		throws Exception
	{
		if ( !checkKeyStoreHasEntry()){
			
			return( null );
		}
		
		SSLContext context = SSLContext.getInstance( "SSL" );
		
		// Create the key manager factory used to extract the server key
		
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		
		loadKeyStore(keyManagerFactory);
		
		// Initialize the context with the key managers
		
		context.init(  	
				keyManagerFactory.getKeyManagers(), 
				null,
				new java.security.SecureRandom());
		
		SSLServerSocketFactory factory = context.getServerSocketFactory();
		
		return( factory );
	}
	
	public SEKeyDetails
	getKeyDetails(
		String		alias )
	
		throws Exception
	{
		// Create the key manager factory used to extract the server key
				
		KeyStore key_store = loadKeyStore();
		
		final Key key = key_store.getKey( alias, SESecurityManager.SSL_PASSWORD.toCharArray());
		
		if ( key == null ){
			
			return( null );
		}
		
		java.security.cert.Certificate[]	chain = key_store.getCertificateChain( alias );

		final X509Certificate[]	res = new X509Certificate[chain.length];
		
		for (int i=0;i<chain.length;i++){
			
			if ( !( chain[i] instanceof X509Certificate )){
				
				throw( new Exception( "Certificate chain must be comprised of X509Certificate entries"));
			}
			
			res[i] = (X509Certificate)chain[i];
		}
		
		return( new SEKeyDetails()
				{
					public Key
					getKey()
					{
						return( key );
					}
					
					public X509Certificate[]
					getCertificateChain()
					{
						return( res );
					}
				});
	}
	
	public void
	createSelfSignedCertificate(
		String		alias,
		String		cert_dn,
		int			strength )
	
		throws Exception
	{
		SESecurityManagerBC.createSelfSignedCertificate( this, alias, cert_dn, strength );
	}
	
	public boolean
	installServerCertificates(
		URL		https_url )
	{
		try{
			this_mon.enter();
		
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
				
				Debug.printStackTrace( e );
				
				return( false );
				
			}finally{
				
				if ( socket != null ){
					
					try{
						socket.close();
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addCertToKeyStore(
		String								alias,
		Key									public_key,
		java.security.cert.Certificate[] 	certChain )
	
		throws Exception
	{
		try{
			this_mon.enter();
		
			KeyStore key_store = loadKeyStore();
			
			if( key_store.containsAlias( alias )){
				
				key_store.deleteEntry( alias );
			}
			
			key_store.setKeyEntry( alias, public_key, SESecurityManager.SSL_PASSWORD.toCharArray(), certChain );
			
			FileOutputStream	out = null;
			
			try{
				out = new FileOutputStream(keystore_name);
			
				key_store.store(out, SESecurityManager.SSL_PASSWORD.toCharArray());
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
				
			}finally{
				
				if ( out != null ){
					
					out.close();
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addCertToTrustStore(
		String							alias,
		java.security.cert.Certificate	cert )
	
		throws Exception
	{
		try{
			this_mon.enter();
		
			KeyStore keystore = KeyStore.getInstance("JKS");
			
			if ( !new File(truststore_name).exists()){
		
				keystore.load(null,null);
				
			}else{
			
				FileInputStream		in 	= null;
	
				try{
					in = new FileInputStream(truststore_name);
			
					keystore.load(in, SESecurityManager.SSL_PASSWORD.toCharArray());
					
				}finally{
					
					if ( in != null ){
						
						in.close();
					}
				}
			}
			
			if ( cert != null ){
				
				if ( keystore.containsAlias( alias )){
				
					keystore.deleteEntry( alias );
				}
							
				keystore.setCertificateEntry(alias, cert);
	
				FileOutputStream	out = null;
				
				try{
					out = new FileOutputStream(truststore_name);
			
					keystore.store(out, SESecurityManager.SSL_PASSWORD.toCharArray());
			
				}finally{
					
					if ( out != null ){
						
						out.close();
					}						
				}
			}
			
				// pick up the changed trust store
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			
			tmf.init(keystore);
			
			SSLContext ctx = SSLContext.getInstance("SSL");
			
			ctx.init(null, tmf.getTrustManagers(), null);
						
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public PasswordAuthentication
	getPasswordAuthentication(
		String		realm,
		URL			tracker )
	{
		Object[]	handler = (Object[])password_handlers.get(tracker.toString());
		
		if ( handler != null ){
			
			return(((SEPasswordListener)handler[0]).getAuthentication( realm, (URL)handler[1] ));
		}
		
		for (int i=0;i<password_listeners.size();i++){
			
			PasswordAuthentication res = ((SEPasswordListener)password_listeners.get(i)).getAuthentication( realm, tracker );
			
			if ( res != null ){
				
				return( res );
			}
		}
		
		return( null );
	}
	
	public void
	setPasswordAuthenticationOutcome(
		String		realm,
		URL			tracker,
		boolean		success )
	{
		for (int i=0;i<password_listeners.size();i++){
			
			((SEPasswordListener)password_listeners.get(i)).setAuthenticationOutcome( realm, tracker, success );
		}
	}
		
	public void
	addPasswordListener(
		SEPasswordListener	l )
	{
		try{
			this_mon.enter();
		
			password_listeners.add(l);
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	removePasswordListener(
		SEPasswordListener	l )
	{
		try{
			this_mon.enter();
		
			password_listeners.remove(l);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addPasswordHandler(
		URL						url,
		SEPasswordListener		l )
	{
		String url_s	= url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
		
		password_handlers.put( url_s, new Object[]{ l, url });
	}
	
	public void
	removePasswordHandler(
		URL						url,
		SEPasswordListener		l )
	{
		Ignore.ignore( l );
		
		String url_s	= url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
		
		password_handlers.remove( url_s );
	}
	
	public void
	addCertificateListener(
		SECertificateListener	l )
	{
		try{
			this_mon.enter();
		
			certificate_listeners.add(l);
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	removeCertificateListener(
		SECertificateListener	l )
	{
		try{
			this_mon.enter();
			
			certificate_listeners.remove(l);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		SESecurityManagerImpl man = SESecurityManagerImpl.getSingleton();
		
		man.initialise();
		
		try{
			man.createSelfSignedCertificate( "SomeAlias", "CN=fred,OU=wap,O=wip,L=here,ST=there,C=GB", 1000 );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
}
