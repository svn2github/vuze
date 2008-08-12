package com.aelitis.azureus.core.metasearch.impl.web.rss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
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
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;
import com.aelitis.azureus.util.ImportExportUtils;

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
		String 				searchURLFormat ) 
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
				new FieldMapping[0]);		
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
	
	protected Result[] 
	searchSupport(
		SearchParameter[] 	searchParameters, 
		int 				max_matches,
		String 				headers, 
		ResultListener 		listener) 
	
		throws SearchException 
	{
		debugStart();
		
		String page = super.getWebPageContent( searchParameters, headers );
		
		if ( listener != null ){
			listener.contentReceived( this, page );
		}
		
		
		String searchQuery = null;
		
		for(int i = 0 ; i < searchParameters.length ; i++){
			if(searchParameters[i].getMatchPattern().equals("s")) {
				searchQuery = searchParameters[i].getValue();
			}
		}
		
		FieldMapping[] mappings = getMappings();

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(page.getBytes("UTF-8"));
			RSSFeed rssFeed = StaticUtilities.getRSSFeed(bais);
			RSSChannel[] channels = rssFeed.getChannels();
			
			List results = new ArrayList();
			
			for(int i = 0 ; i < channels.length ; i++) {
				RSSItem[] items = channels[i].getItems();
				for(int j = 0 ; j < items.length ; j++) {
					RSSItem item = items[j];
					WebResult result = new WebResult(this,getRootPage(),getBasePage(),getDateParser(),"");
					
					result.setPublishedDate(item.getPublicationDate());
					result.setNameFromHTML(item.getTitle());
					result.setCDPLink(item.getLink().toExternalForm());
					
					SimpleXMLParserDocumentNode node = item.getNode();
					if(node != null) {
						SimpleXMLParserDocumentNode[] children = node.getChildren();
						for(int k = 0 ; k < children.length ; k++) {
							SimpleXMLParserDocumentNode child = children[k];
							if(child.getName().equals("enclosure")) {
								SimpleXMLParserDocumentAttribute typeAtt = child.getAttribute("type");
								if(typeAtt != null && typeAtt.getValue().equals("application/x-bittorrent")) {
									
									SimpleXMLParserDocumentAttribute urlAtt = child.getAttribute("url");
									if(urlAtt != null) {
										result.setTorrentLink(urlAtt.getValue());
									}
									
									SimpleXMLParserDocumentAttribute lengthAtt = child.getAttribute("length");
									if(lengthAtt != null) {	
										result.setSizeFromHTML(lengthAtt.getValue());
									}
									
								}
							}
							else if(child.getName().equals("category")) {
								result.setCategory(child.getValue());
							}
							else if(child.getName().equals("comments")) {
								result.setCDPLink(child.getValue());
							}
							else if(child.getName().equals("link") || child.getName().equals("guid")) {
								try {
									String value = child.getValue();
									if(value.endsWith(".torrent")) {
										URL url = new URL(value);
										result.setTorrentLink(value);
									}
									
								} catch (Exception e) {
									//Do nothing, apparently, the url is not valid
								}
							}
						}
					}
					
					results.add(result);
					
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
