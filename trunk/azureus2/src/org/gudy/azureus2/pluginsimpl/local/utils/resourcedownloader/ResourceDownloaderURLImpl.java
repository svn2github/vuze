/*
 * File    : TorrentDownloader2Impl.java
 * Created : 27-Feb-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;


import javax.net.ssl.*;
import java.net.PasswordAuthentication;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderURLImpl
	extends 	ResourceDownloaderBaseImpl
	implements 	SEPasswordListener
{
	private static final int BUFFER_SIZE = 32768;
  
	protected URL			original_url;
	protected boolean		auth_supplied;
	protected String		user_name;
	protected String		password;
	
	protected InputStream 	input_stream;
	protected boolean		cancel_download	= false;
	
	protected boolean		download_initiated;
	protected long			size		 	= -2;	// -1 -> unknown
	
	public 
	ResourceDownloaderURLImpl(
		ResourceDownloaderBaseImpl	_parent,
		URL							_url )
	{
		this( _parent, _url, false, null, null );
	}
	
	public 
	ResourceDownloaderURLImpl(
		ResourceDownloaderBaseImpl	_parent,
		URL							_url,
		String						_user_name,
		String						_password )
	{
		this( _parent, _url, true, _user_name, _password );
	}
	
	public 
	ResourceDownloaderURLImpl(
		ResourceDownloaderBaseImpl	_parent,
		URL							_url,
		boolean						_auth_supplied,
		String						_user_name,
		String						_password )
	{
		super( _parent );
		
		/*
		if ( _url.getHost().equals( "212.159.18.92")){
			try{
				_url = new URL(_url.getProtocol() + "://192.168.0.2:" + _url.getPort() + "/" + _url.getPath());
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		*/
		
		original_url	= _url;
		auth_supplied	= _auth_supplied;
		user_name		= _user_name;
		password		= _password;
	}
	
	protected URL
	getURL()
	{
		return( original_url );
	}
	
	public String
	getName()
	{
		return( original_url.toString());
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{
			// only every try getting the size once
		
		if ( size == -2 ){
			
			try{
				ResourceDownloaderURLImpl c = (ResourceDownloaderURLImpl)getClone( this );
				
				addReportListener( c );
				
				size = c.getSizeSupport();
				
			}finally{
				
				if ( size == -2 ){
					
					size = -1;
				}
			}
		}
		
		return( size );
	}
	
	protected void
	setSize(
		long	l )
	{
		size	= l;
	}
	
	protected long
	getSizeSupport()
	
		throws ResourceDownloaderException
	{
		// System.out.println("ResourceDownloader:getSize - " + getName());
		
		try{
			reportActivity(this, "getting size of " + original_url );

			try{
				URL	url = new URL( original_url.toString().replaceAll( " ", "%20" ));
			      
				try{
					if ( auth_supplied ){
	
						SESecurityManager.addPasswordHandler( url, this );
					}
	
					HttpURLConnection	con;
					
					if ( url.getProtocol().equalsIgnoreCase("https")){
				      	
							// see ConfigurationChecker for SSL client defaults
		
						HttpsURLConnection ssl_con = (HttpsURLConnection)url.openConnection();
		
							// allow for certs that contain IP addresses rather than dns names
		  	
						ssl_con.setHostnameVerifier(
								new HostnameVerifier()
								{
									public boolean
									verify(
											String		host,
											SSLSession	session )
									{
										return( true );
									}
								});
		  	
						con = ssl_con;
		  	
					}else{
		  	
						con = (HttpURLConnection) url.openConnection();
		  	
					}
		  
					con.setRequestMethod( "HEAD" );
					
					con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);     
		  
					con.connect();
		
					int response = con.getResponseCode();
					
					if ((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
						
						throw( new ResourceDownloaderException("Error on connect for '" + url.toString() + "': " + Integer.toString(response) + " " + con.getResponseMessage()));    
					}
						
					return( con.getContentLength());					
									
				}finally{
					
					if ( auth_supplied ){
					
						SESecurityManager.removePasswordHandler( url, this );
					}
				}
			}catch (java.net.MalformedURLException e){
				
				throw( new ResourceDownloaderException("Exception while parsing URL '" + original_url + "':" + e.getMessage(), e));
				
			}catch (java.net.UnknownHostException e){
				
				throw( new ResourceDownloaderException("Exception while initializing download of '" + original_url + "': Unknown Host '" + e.getMessage() + "'", e));
				
			}catch (java.io.IOException e ){
				
				throw( new ResourceDownloaderException("I/O Exception while downloading '" + original_url + "':" + e.toString(), e ));
			}
		}catch( Throwable e ){
			
			ResourceDownloaderException	rde;
			
			if ( e instanceof ResourceDownloaderException ){
				
				rde = (ResourceDownloaderException)e;
				
			}else{
				
				rde = new ResourceDownloaderException( "Unexpected error", e );
			}
						
			throw( rde );
		}		
	}
	
	public ResourceDownloader
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		ResourceDownloaderURLImpl c = new ResourceDownloaderURLImpl( parent, original_url, auth_supplied, user_name, password );
		
		c.setSize( size );
		
		return( c );
	}

	public void
	asyncDownload()
	{
		Thread	t = 
			new AEThread( "ResourceDownloader:asyncDownload")
			{
				public void
				run()
				{
					try{
						download();
						
					}catch ( ResourceDownloaderException e ){
					}
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}

	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		// System.out.println("ResourceDownloader:download - " + getName());
		
		try{
			reportActivity(this, getLogIndent() + "Downloading: " + original_url );
			
			try{
				this_mon.enter();
				
				if ( download_initiated ){
					
					throw( new ResourceDownloaderException("Download already initiated"));
				}
				
				download_initiated	= true;
				
			}finally{
				
				this_mon.exit();
			}
			
			try{
				URL	url = new URL( original_url.toString().replaceAll( " ", "%20" ));
			      
					// some authentications screw up without an explicit port number here
				
				if( url.getPort() == -1 ){
					
					int	target_port;
					
					if ( url.getProtocol().toLowerCase().equals( "http" )){
						
						target_port = 80;
						
					}else{
						
						target_port = 443;
					}
					
					try{
						String str = original_url.toString().replaceAll( " ", "%20" );
					
						int	pos = str.indexOf( "://" );
						
						pos = str.indexOf( "/", pos+4 );
						
						url = new URL( str.substring(0,pos) + ":" + target_port + str.substring(pos));
												
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
				
				try{
					if ( auth_supplied ){
						
						SESecurityManager.addPasswordHandler( url, this );
					}

					HttpURLConnection	con;
					
					if ( url.getProtocol().equalsIgnoreCase("https")){
				      	
							// see ConfigurationChecker for SSL client defaults
		
						HttpsURLConnection ssl_con = (HttpsURLConnection)url.openConnection();
		
							// allow for certs that contain IP addresses rather than dns names
		  	
						ssl_con.setHostnameVerifier(
								new HostnameVerifier()
								{
									public boolean
									verify(
											String		host,
											SSLSession	session )
									{
										return( true );
									}
								});
		  	
						con = ssl_con;
		  	
					}else{
		  	
						con = (HttpURLConnection) url.openConnection();
		  	
					}
		  
					con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);     
		  
					con.connect();
		
					int response = con.getResponseCode();
										
					if ((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
						
						throw( new ResourceDownloaderException("Error on connect for '" + url.toString() + "': " + Integer.toString(response) + " " + con.getResponseMessage()));    
					}
						
					try{
						this_mon.enter();
						
						input_stream = con.getInputStream();
						
					}finally{
						
						this_mon.exit();
					}
					
					ByteArrayOutputStream	baos;
	
					try{
						byte[] buf = new byte[BUFFER_SIZE];
						
						int	total_read	= 0;
						
							// unfortunately not all servers set content length
						
						int size = con.getContentLength();					
						
						baos = size>0?new ByteArrayOutputStream(size):new ByteArrayOutputStream();
						
						while( !cancel_download ){
							
							int read = input_stream.read(buf);
								
							if ( read > 0 ){
								
								baos.write(buf, 0, read);
								
								total_read += read;
						        
								if ( size > 0){
									
									informPercentDone(( 100 * total_read ) / size );
								}
							}else{
								
								break;
							}
						}
						
							// if we've got a size, make sure we've read all of it
						
						if ( size > 0 && total_read != size ){
							
							throw( new IOException( "Premature end of stream" ));
						}
					}finally{
						
						input_stream.close();
					}
		
					InputStream	res = new ByteArrayInputStream( baos.toByteArray());
					
					if ( informComplete( res )){
					
						return( res );
					}
					
					throw( new ResourceDownloaderException("Contents downloaded but rejected: '" + original_url + "'" ));
					
				}finally{
					
					if ( auth_supplied ){
						
						SESecurityManager.removePasswordHandler( url, this );
					}
				}
			}catch (java.net.MalformedURLException e){
				
				throw( new ResourceDownloaderException("Exception while parsing URL '" + original_url + "':" + e.getMessage(), e));
				
			}catch (java.net.UnknownHostException e){
				
				throw( new ResourceDownloaderException("Exception while initializing download of '" + original_url + "': Unknown Host '" + e.getMessage() + "'", e));
				
			}catch (java.io.IOException e ){
				
				throw( new ResourceDownloaderException("I/O Exception while downloading '" + original_url + "':" + e.toString(), e ));
			}
		}catch( Throwable e ){
			
			ResourceDownloaderException	rde;
			
			if ( e instanceof ResourceDownloaderException ){
				
				rde = (ResourceDownloaderException)e;
				
			}else{
				
				rde = new ResourceDownloaderException( "Unexpected error", e );
			}
			
			informFailed(rde);
			
			throw( rde );
		}
	}
	
	public void
	cancel()
	{
		cancel_download	= true;
		
		try{
			this_mon.enter();
			
			if ( input_stream != null ){
				
				try{
					input_stream.close();
					
				}catch( Throwable e ){
					
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		informFailed( new ResourceDownloaderException( "Download cancelled" ));
	}
	
	public PasswordAuthentication
	getAuthentication(
		String		realm,
		URL			tracker )
	{
		if ( user_name == null || password == null ){
			
			String user_info = tracker.getUserInfo();
			
			if ( user_info == null ){
				
				return( null );
			}
			
			String	user_bit	= user_info;
			String	pw_bit		= "";
			
			int	pos = user_info.indexOf(':');
			
			if ( pos != -1 ){
				
				user_bit	= user_info.substring(0,pos);
				pw_bit		= user_info.substring(pos+1);
			}
			
			return( new PasswordAuthentication( user_bit, pw_bit.toCharArray()));
		}
		
		return( new PasswordAuthentication( user_name, password.toCharArray()));
	}
	
	public void
	setAuthenticationOutcome(
		String		realm,
		URL			tracker,
		boolean		success )
	{
		
	}
}