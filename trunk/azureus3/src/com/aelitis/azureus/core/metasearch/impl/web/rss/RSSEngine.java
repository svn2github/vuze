package com.aelitis.azureus.core.metasearch.impl.web.rss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSChannel;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSItem;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.EngineImpl;
import com.aelitis.azureus.core.metasearch.impl.MetaSearchImpl;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class 
RSSEngine 
	extends WebEngine 
{
	public static EngineImpl
	importFromBEncodedMap(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		return( new RSSEngine( meta_search, map ));
	}
	
	public static Engine
	importFromJSONString(
		MetaSearchImpl		meta_search,
		long				id,
		long				last_updated,
		String				name,
		JSONObject			map )
	
		throws IOException
	{
		return( new RSSEngine( meta_search, id, last_updated, name, map ));
	}
	
		// explicit constructor
	
	public 
	RSSEngine(
		MetaSearchImpl		meta_search,
		long 				id,
		long 				last_updated,
		String 				name,
		String 				searchURLFormat,
		boolean				needs_auth,
		String				auth_method,
		String				login_url,
		String[]			required_cookies )
	{
		super( 	meta_search, 
				Engine.ENGINE_TYPE_RSS, 
				id,
				last_updated,
				name,
				searchURLFormat,
				"GMT",
				false,
				"EEE, d MMM yyyy HH:mm:ss Z",
				new FieldMapping[0],
				needs_auth,
				auth_method,
				login_url,
				required_cookies );		
	}
	
	protected 
	RSSEngine(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		super( meta_search, map );
	}
	
		// json 
	
	protected 
	RSSEngine(
		MetaSearchImpl		meta_search,
		long				id,
		long				last_updated,
		String				name,
		JSONObject			map )
	
		throws IOException
	{
		super( meta_search, Engine.ENGINE_TYPE_REGEX, id, last_updated, name, map );
	}
	
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException
	{
		Map	res = new HashMap();
				
		super.exportToBencodedMap( res );
		
		return( res );
	}
	
	public boolean
	supportsField(
		int		field_id )
	{
			// don't know about optional fields (such as direct download - be optimistic)
		
		switch( field_id ){
			case FIELD_NAME:
			case FIELD_DATE:
			case FIELD_CATEGORY:
			case FIELD_COMMENTS:
			case FIELD_CDPLINK:
			case FIELD_TORRENTLINK:
			case FIELD_DOWNLOADBTNLINK:
			{
				return( true );
			}
		}
	
		return( false );
	}
	
	protected Result[] 
	searchSupport(
		SearchParameter[] 	searchParameters, 
		Map					searchContext,
		int 				desired_max_matches,
		int					absolute_max_matches,
		String 				headers, 
		ResultListener 		listener) 
	
		throws SearchException 
	{
		debugStart();
		
		pageDetails page_details = super.getWebPageContent( searchParameters, searchContext, headers, true );
		
		String	page = page_details.getContent();
		
		if ( listener != null ){
			listener.contentReceived( this, page );
		}
			
		if ( page == null || page.length() == 0 ){
			
			return( new Result[0]);
		}
		
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(page.getBytes("UTF-8"));
			RSSFeed rssFeed = StaticUtilities.getRSSFeed(bais);
			RSSChannel[] channels = rssFeed.getChannels();
			
			List results = new ArrayList();
			
			for(int i = 0 ; i < channels.length ; i++) {
				RSSChannel channel = channels[i];
				RSSItem[] items = channel.getItems();
				for(int j = 0 ; j < items.length ; j++) {
					RSSItem item = items[j];
					WebResult result = new WebResult(this,getRootPage(),getBasePage(),getDateParser(),"");
					
					result.setPublishedDate(item.getPublicationDate());
					result.setNameFromHTML(item.getTitle());
					
					URL cdp_link = item.getLink();
					
					if ( cdp_link != null ){
					
						result.setCDPLink(cdp_link.toExternalForm());
					}
					
					String uid = item.getUID();
					
					if ( uid != null ){
						
						result.setUID( uid );
					}
					
					SimpleXMLParserDocumentNode node = item.getNode();
					if(node != null) {
						SimpleXMLParserDocumentNode[] children = node.getChildren();
						for(int k = 0 ; k < children.length ; k++) {
							SimpleXMLParserDocumentNode child = children[k];
							
							String	lc_child_name 		= child.getName().toLowerCase();
							String	lc_full_child_name 	= child.getFullName().toLowerCase();
							
							String	value = child.getValue();
							
							if (lc_child_name.equals( "enclosure" )){
								
								SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute("type");
								
								if( typeAtt != null && typeAtt.getValue().equalsIgnoreCase( "application/x-bittorrent")) {
									
									SimpleXMLParserDocumentAttribute urlAtt = child.getAttribute("url");
									
									if( urlAtt != null ){
										
										result.setTorrentLink(urlAtt.getValue());
									}
									
									SimpleXMLParserDocumentAttribute lengthAtt = child.getAttribute("length");
									
									if (lengthAtt != null){
										
										result.setSizeFromHTML(lengthAtt.getValue());
									}
								}
							}else if(lc_child_name.equals( "category" )) {
								
								result.setCategory( value );
								
							}else if(lc_child_name.equals( "comments" )){
								
								result.setCommentsFromHTML( value );
								
							}else if(lc_child_name.equals( "link" ) || lc_child_name.equals( "guid" )) {
								
								String lc_value = value.toLowerCase();
								
								try{									
									if ( 	lc_value.endsWith( ".torrent" ) ||
											lc_value.startsWith( "magnet:" ) ||
											lc_value.startsWith( "dht:" )){
										
										new URL(value);
										
										result.setTorrentLink(value);
									}
								} catch (Throwable e){
								}
							}else if ( lc_child_name.equals( "content" ) && rssFeed.isAtomFeed()){
								
								SimpleXMLParserDocumentAttribute srcAtt = child.getAttribute( "src" );
								
								String	src = srcAtt==null?null:srcAtt.getValue();
											
								if ( src != null ){
									
									boolean	is_dl_link = false;
									
									SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute( "type" );
									
									if ( typeAtt != null && typeAtt.getValue().equalsIgnoreCase("application/x-bittorrent")) {

										is_dl_link = true;
									}
									
									if ( !is_dl_link ){
									
										is_dl_link = src.toLowerCase().indexOf( ".torrent" ) != -1;
									}
										
									if ( is_dl_link ){
										
										try{
											new URL( src );
										
											result.setTorrentLink( src );
											
										}catch( Throwable e ){
										}
									}
								}
							}else if ( lc_full_child_name.equals( "vuze:size" )){
								
								result.setSizeFromHTML( value );
								
							}else if ( lc_full_child_name.equals( "vuze:seeds" )){
								
								result.setNbSeedsFromHTML( value );
								
							}else if ( lc_full_child_name.equals( "vuze:superseeds" )){
								
								result.setNbSuperSeedsFromHTML( value );
								
							}else if ( lc_full_child_name.equals( "vuze:peers" )){
								
								result.setNbPeersFromHTML( value );
								
							}else if ( lc_full_child_name.equals( "vuze:contenttype" )){
								
								String	type = value.toLowerCase();
								
								if ( type.startsWith( "video" )){
									
									type = Engine.CT_VIDEO;
									
								}else if ( type.startsWith( "audio" )){
									
									type = Engine.CT_AUDIO;
									
								}else if ( type.startsWith( "games" )){
									
									type = Engine.CT_GAME;
								}
								
								result.setContentType( type );
								
							}else if ( lc_full_child_name.equals( "vuze:downloadurl" )){

								result.setTorrentLink( value);
								
							}else if ( lc_full_child_name.equals( "vuze:playurl" )){

								result.setPlayLink( value);
								
							}else if ( lc_full_child_name.equals( "vuze:drmkey" )){

								result.setDrmKey( value);
								
							}else if ( lc_full_child_name.equals( "vuze:assethash" )){

								result.setHash( value);
							}
						}
					}
					
					results.add(result);
					
					if ( absolute_max_matches >= 0 && results.size() == absolute_max_matches ){
						
						break;
					}
				}
			}
			
			Result[] res = (Result[]) results.toArray(new Result[results.size()]);

			debugLog( "success: found " + res.length + " results" );
			
			return( res );
			
			
		}catch ( Throwable e ){
			
			debugLog( "failed: " + Debug.getNestedExceptionMessageAndStack( e ));
			
			if ( e instanceof SearchException ){
				
				throw((SearchException)e );
			}
			
			throw( new SearchException( "RSS matching failed", e ));
		}
	}
}
