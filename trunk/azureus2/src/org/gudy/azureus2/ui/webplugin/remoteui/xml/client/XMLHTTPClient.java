/*
 * File    : XMLHTTPClient.java
 * Created : 13-Mar-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.xml.client;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;

public class 
XMLHTTPClient 
{
	protected
	XMLHTTPClient()
	{
		boolean	quick_test = false;
			
		try{
			long	req_id = SystemTime.getCurrentTime();
			
			if ( quick_test ){
				SimpleXMLParserDocument	res = 
					sendRequest( 	"<REQUEST>"+
										"<METHOD>getDownloads</METHOD>"+
										"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
									"</REQUEST>");
				
				res.print();
				
			}else{
				
				SimpleXMLParserDocument	res = 
					sendRequest( 	"<REQUEST>"+
										"<METHOD>getSingleton</METHOD>"+
										"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
									"</REQUEST>");
				
				res.print();
	
				String connection_id = res.getChild( "_connection_id" ).getValue().trim();
				
				String plugin_if_oid	= res.getChild( "_object_id" ).getValue().trim();
				
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + plugin_if_oid + "</_object_id></OBJECT>" +
							"<METHOD>getShortCuts</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
	
				String sc_oid	= res.getChild( "_object_id" ).getValue().trim();
			
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + sc_oid + "</_object_id></OBJECT>" +
							"<METHOD>getDownloadStats[byte[]]</METHOD>"+
							"<PARAMS>"+
								"<ENTRY>6495034E54A2B374560389FAE3812A6191C614BA</ENTRY>"+
							"</PARAMS>" + 
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
				
				res.print();
				
				/*
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + plugin_if_oid + "</_object_id></OBJECT>" +
							"<METHOD>getDownloadManager</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
	
				String dl_man_oid	= res.getChild( "_object_id" ).getValue().trim();
				
					// IP Filter
				
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + plugin_if_oid + "</_object_id></OBJECT>" +
							"<METHOD>getIPFilter</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
	
				
				String ip_filter_oid	= res.getChild( "_object_id" ).getValue().trim();

				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + ip_filter_oid + "</_object_id></OBJECT>" +
							"<METHOD>setInRangeAddressesAreAllowed[boolean]</METHOD>"+
							"<PARAMS>"+
								"<ENTRY>false</ENTRY>"+
							"</PARAMS>" + 
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
				
				res.print();

				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + ip_filter_oid + "</_object_id></OBJECT>" +
							"<METHOD>getInRangeAddressesAreAllowed</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
				
				res.print();
				*/
				
				/*
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + ip_filter_oid + "</_object_id></OBJECT>" +
							"<METHOD>getRanges</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
				
				res.print();


				SimpleXMLParserDocumentNode[]	kids = res.getChildren();
				
				for (int i=0;i<kids.length;i++){
				
					String range_oid	= kids[i].getChild( "_object_id" ).getValue().trim();

					res = sendRequest( 
							"<REQUEST>" +
								"<OBJECT><_object_id>" + range_oid + "</_object_id></OBJECT>" +
								"<METHOD>delete</METHOD>"+
								"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
								"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
							"</REQUEST>");
				}
				*/
				
				/*
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + ip_filter_oid + "</_object_id></OBJECT>" +
							"<METHOD>createAndAddRange[String,String,String,boolean]</METHOD>"+
							"<PARAMS>"+
								"<ENTRY>XML Test</ENTRY>"+
								"<ENTRY>1.1.1.1</ENTRY>"+
								"<ENTRY>1.1.1.2</ENTRY>"+
								"<ENTRY>false</ENTRY>"+
							"</PARAMS>" +
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");	
				
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + ip_filter_oid + "</_object_id></OBJECT>" +
							"<METHOD>save</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");				
				*/
				
				/*
				
					// config stuff
				
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + plugin_if_oid + "</_object_id></OBJECT>" +
							"<METHOD>getPluginconfig</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
	
				
				String config_oid	= res.getChild( "_object_id" ).getValue().trim();

				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + config_oid + "</_object_id></OBJECT>" +
							"<PARAMS>"+
								"<ENTRY index=\"1\">12</ENTRY>"+
								"<ENTRY index=\"0\">Max Upload Speed KBs</ENTRY>"+
							//	"<ENTRY>Max Upload Speed KBs</ENTRY>"+
							//	"<ENTRY>12</ENTRY>"+
							"</PARAMS>" +
							"<METHOD>setParameter[String,int]</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
				*/
				
				/* stuff for adding a torrent
				 
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + plugin_if_oid + "</_object_id></OBJECT>" +
							"<METHOD>getTorrentManager</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
	
				String torrent_man_oid	= res.getChild( "_object_id" ).getValue().trim();
	
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + torrent_man_oid + "</_object_id></OBJECT>" +
							"<METHOD>getURLDownloader[URL]</METHOD>"+
							"<PARAMS><ENTRY>http://69.50.170.99/sn//torrents/1669/RAR_Password_Cracker_4.12[www.elitetopdown.com]-rar.torrent</ENTRY></PARAMS>" +
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
			
				res.print();
				
				String torrent_downloader_oid	= res.getChild( "_object_id" ).getValue().trim();

				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + torrent_downloader_oid + "</_object_id></OBJECT>" +
							"<METHOD>download</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
			
				res.print();

				String torrent_oid	= res.getChild( "_object_id" ).getValue().trim();
				
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + dl_man_oid + "</_object_id></OBJECT>" +
							"<METHOD>addDownload[Torrent]</METHOD>"+
							"<PARAMS><ENTRY><OBJECT><_object_id>" + torrent_oid + "</_object_id></OBJECT></ENTRY></PARAMS>" +
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
	
				res.print();
				*/
				
				/*
				res = sendRequest( 
						"<REQUEST>" +
							"<OBJECT><_object_id>" + dl_man_oid + "</_object_id></OBJECT>" +
							"<METHOD>getDownloads</METHOD>"+
							"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
							"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
						"</REQUEST>");
			
				res.print();
				*/
				
				/*
				SimpleXMLParserDocumentNode[]	kids = res.getChildren();
				
				for (int i=0;i<kids.length;i++){
				
					String dl_oid	= kids[i].getChild( "_object_id" ).getValue().trim();
				
					System.out.println( "kid: oid = " + dl_oid );
					
					res = sendRequest( 
							"<REQUEST>" +
								"<OBJECT><_object_id>" + dl_oid + "</_object_id></OBJECT>" +
								"<METHOD>stop</METHOD>"+
								"<CONNECTION_ID>" + connection_id + "</CONNECTION_ID>"+
								"<REQUEST_ID>" + (req_id++) + "</REQUEST_ID>"+
							"</REQUEST>");
					
					res.print();
						
				}
				*/
			}

		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}

	protected SimpleXMLParserDocument
	sendRequest(
		String	request )
	
		throws SimpleXMLParserDocumentException
	{
		String	resp = sendRequestSupport( request );
		
		System.out.println( "got:" + resp );
		
		return( SimpleXMLParserDocumentFactory.create( resp ));
	}
	
	protected String
	sendRequestSupport(
		String		request )
	{
		try{
	
		    URL url = new URL( "http://127.0.0.1:6884/process.cgi" );
			
			// System.out.println( "doc base = " + url );
			
			HttpURLConnection con;
			
			if ( url.getProtocol().equalsIgnoreCase("https")){
				
				// see ConfigurationChecker for SSL client defaults
				
				URLConnection url_con = url.openConnection();
				
					// Opera doesn't return a javax class
				
				if ( url_con.getClass().getName().startsWith( "javax")){
									
					HttpsURLConnection ssl_con = (HttpsURLConnection)url_con;
					
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
					
					con = (HttpURLConnection)url_con;
				}
			}else{
				
				con = (HttpURLConnection) url.openConnection();
			}
	
			con.setRequestProperty("Connection", "close" );
			
			con.setRequestMethod( "POST" );
			
			con.setAllowUserInteraction( true );
			
			con.setDoInput( true );
			
			con.setDoOutput( true );
						
			con.connect();
		
			PrintWriter os = null;
			
			try{
				os	= new PrintWriter( new OutputStreamWriter( con.getOutputStream() , Constants.DEFAULT_ENCODING ));
			
				os.print( request );
				
				os.flush();
				
			}finally{
			
				if ( os != null ){
					
					os.close();
				}
			}
			
			InputStream is = null;
			
			try{
				
				is = con.getInputStream();
				
				int content_length = con.getContentLength();
				
				byte[] data = new byte[1024];
				
				int	num_read = 0;
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				while ( num_read < content_length ){
					
					try{
						int	len = is.read(data);
						
						if ( len > 0 ){
							
							baos.write(data, 0, len);
															
							num_read += len;
							
						}else if ( len == 0 ){
							
							Thread.sleep(20);
							
						}else{
							
							break;
						}
						
					}catch (Exception e){
						
						e.printStackTrace();
						
						break;
					}
				}
												
				return( new String( baos.toByteArray(), Constants.DEFAULT_ENCODING ));
				
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
		}catch( Throwable e ){		
	
			throw( new RuntimeException( "whoops", e ));
		}	
	}
	
	public static void
	main(
		String[]		args )
	{
		new XMLHTTPClient();
	}
}
