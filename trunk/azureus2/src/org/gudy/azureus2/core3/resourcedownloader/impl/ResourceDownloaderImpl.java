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

package org.gudy.azureus2.core3.resourcedownloader.impl;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;


import javax.net.ssl.*;

import org.gudy.azureus2.core3.resourcedownloader.*;
import org.gudy.azureus2.core3.util.Constants;

public class 
ResourceDownloaderImpl
	extends ResourceDownloaderBaseImpl
{
	protected String		original_url;
	
	protected InputStream 	input_stream;
	protected boolean		cancel_download	= false;
	
	protected boolean		download_initiated;
	
	public 
	ResourceDownloaderImpl(
		String		_url )
	{
		original_url	= _url;
	}
	
	public String
	getName()
	{
		return( original_url );
	}
	
	public ResourceDownloader
	getClone()
	{
		return( new ResourceDownloaderImpl( original_url ));
	}

	public void
	asyncDownload()
	{
		Thread	t = 
			new Thread( "ResourceDownloader:asyncDownload")
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
			
		t.setDaemon(false);
		
		t.start();
	}

	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		try{
			reportActivity(this, "downloading " + original_url );
			
			synchronized( this ){
				
				if ( download_initiated ){
					
					throw( new ResourceDownloaderException("Download already initiated"));
				}
				
				download_initiated	= true;
			}
			
			try{
				URL	url = new URL( original_url.replaceAll( " ", "%20" ));
			      
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
					
				synchronized( this ){
					
					input_stream = con.getInputStream();
				}
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();

				try{
					byte[] buf = new byte[8192];
					
					int	total_read	= 0;
					
						// unfortunately not all servers set content length
					
					int size = con.getContentLength();					
					
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
				}finally{
					
					input_stream.close();
				}
	
				InputStream	res = new ByteArrayInputStream( baos.toByteArray());
				
				if ( informComplete( res )){
				
					return( res );
				}
				
				throw( new ResourceDownloaderException("Contents downloaded but rejected: '" + original_url + "'" ));
				
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
		
		synchronized( this ){
			
			if ( input_stream != null ){
				
				try{
					input_stream.close();
					
				}catch( Throwable e ){
					
				}
			}
		}
		
		informFailed( new ResourceDownloaderException( "Download cancelled" ));
	}
}