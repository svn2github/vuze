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

package org.gudy.azureus2.core3.torrentdownloader.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;


import javax.net.ssl.*;

import org.gudy.azureus2.core3.torrentdownloader.*;
import org.gudy.azureus2.core3.util.Constants;

public class 
TorrentDownloader2Impl
	implements TorrentDownloader2
{
	protected String		url_str;
	
	protected boolean		cancel_download	= false;
	protected List			listeners		= new ArrayList();
	
	public 
	TorrentDownloader2Impl(
		String		_url_str )
	{
		url_str = _url_str.replaceAll( " ", "%20" );	   
	}
	
	public InputStream
	download()
	
		throws TorrentDownloaderException
	{
		try{
			URL	url = new URL( url_str );
		      
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
				
				throw( new TorrentDownloaderException("Error on connect for '" + url.toString() + "': " + Integer.toString(response) + " " + con.getResponseMessage()));    
			}

			InputStream in = con.getInputStream();

			byte[] buf = new byte[1024];
			
			int	total_read	= 0;
			
			int size = con.getContentLength();
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			while( !cancel_download ){
				
				int read = in.read(buf);
					
				if (read > 0){
					
					baos.write(buf, 0, read);
					
					total_read += read;
			        
					if (size != 0){
						
						informPercentDone(( 100 * total_read ) / size );
					}
				}else{
					
					break;
				}
			}
			
			in.close();

			return( new ByteArrayInputStream( baos.toByteArray()));
			
		}catch (java.net.MalformedURLException e){
			
			throw( new TorrentDownloaderException("Exception while parsing URL '" + url_str + "':" + e.getMessage()));
			
		}catch (java.net.UnknownHostException e){
			
			throw( new TorrentDownloaderException("Exception while initializing download of '" + url_str + "': Unknown Host '" + e.getMessage() + "'"));
			
		}catch (java.io.IOException ioe){
			
			throw( new TorrentDownloaderException("I/O Exception while downloading '" + url_str + "':" + ioe.toString()));
		}
	}
	
	public void
	cancel()
	{
		cancel_download	= true;
	}
	
	protected void
	informPercentDone(
		int	percentage )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TorrentDownloader2Listener)listeners.get(i)).percentComplete(percentage);
		}
	}
	
	public void
	addListener(
		TorrentDownloader2Listener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		TorrentDownloader2Listener		l )
	{
		listeners.remove(l);
	}
}
