package com.aelitis.azureus.core.metasearch.impl.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.*;

public abstract class 
WebEngine 
	extends EngineImpl 
{
	public static final int FIELD_NAME = 1;
	public static final int FIELD_DATE = 2;
	public static final int FIELD_SIZE = 3;
	public static final int FIELD_PEERS = 4;
	public static final int FIELD_SEEDS = 5;
	public static final int FIELD_CATEGORY = 6;
	public static final int FIELD_COMMENTS = 7;
	public static final int FIELD_TORRENTLINK = 102;
	public static final int FIELD_CDPLINK = 103;
	
	static private final Pattern baseTagPattern = Pattern.compile("(?i)<base.*?href=\"([^\"]+)\".*?>");
	static private final Pattern rootURLPattern = Pattern.compile("(https?://[^/]+)");
	static private final Pattern baseURLPattern = Pattern.compile("(https?://.*/)");
	
	
	private String 			searchURLFormat;
	private String 			timeZone;
	private boolean			automaticDateParser;
	private String 			userDateFormat;
	private FieldMapping[]	mappings;

	
	private String rootPage;
	private String basePage;

	private DateParser dateParser;
	

		// manual test constructor
	
	public 
	WebEngine(
		MetaSearchImpl	meta_search,
		int 			type, 
		long 			id, 
		long 			last_updated, 
		String 			name,
		String 			searchURLFormat,
		String 			timeZone,
		boolean 		automaticDateParser,
		String 			userDateFormat, 
		FieldMapping[] 	mappings ) {
		
		super( meta_search, type, id, last_updated, name );

		this.searchURLFormat 		= searchURLFormat;
		this.timeZone 				= timeZone;
		this.automaticDateParser 	= automaticDateParser;
		this.userDateFormat 		= userDateFormat;
		this.mappings				= mappings;
		
		init();
	}
	
		// bencoded constructor
	
	protected 
	WebEngine(
		MetaSearchImpl	meta_search,
		Map				map )
	
		throws IOException
	{
		super( meta_search, map );
		
		searchURLFormat 	= importString( map, "web.search_url_format" );
		timeZone			= importString( map, "web.time_zone" );
		userDateFormat		= importString( map, "web.date_format" );

		automaticDateParser	= importBoolean( map, "web.auto_date", true );

		List	maps = (List)map.get( "web.maps" );
		
		mappings = new FieldMapping[maps.size()];
		
		for (int i=0;i<mappings.length;i++){
			
			Map	m = (Map)maps.get(i);
			
			mappings[i] = 
				new FieldMapping(
					importString( m, "name" ),
					((Long)m.get( "field")).intValue());
		}
		
		init();
	}
	
		// json encoded constructor
	
	protected 
	WebEngine(
		MetaSearchImpl	meta_search,
		int				type,
		long			id,
		long			last_updated,
		String			name,
		Map				map )
	
		throws IOException
	{
		super( meta_search, type, id, last_updated, name );
		
		searchURLFormat 	= importString( map, "searchURL" );
		timeZone			= importString( map, "timezone" );
		userDateFormat		= importString( map, "aaa" );

		searchURLFormat = URLDecoder.decode( searchURLFormat, "UTF-8" );
		
		automaticDateParser	= importBoolean( map, "xxxx", true );

		List	maps = (List)map.get( "column_map" );
		
		mappings = new FieldMapping[maps.size()];
		
		for (int i=0;i<mappings.length;i++){
			
			Map	m = (Map)maps.get(i);
			
			m = (Map)m.get( "mapping" );
			
			String	field_name 	= importString( m, "vuze_field" ).toUpperCase();
			String	field_group	= importString( m, "group_nb" );
			
			int	field_id;
			
			if ( field_name.equals( "TITLE")){
				
				field_id	= FIELD_NAME;
				
			}else if ( field_name.equals( "DATE")){
				
				field_id	= FIELD_DATE;
				
			}else if ( field_name.equals( "SIZE")){
				
				field_id	= FIELD_SIZE;
				
			}else if ( field_name.equals( "PEERS")){
				
				field_id	= FIELD_PEERS;
				
			}else if ( field_name.equals( "SEEDS")){
				
				field_id	= FIELD_SEEDS;
				
			}else if ( field_name.equals( "CAT")){
				
				field_id	= FIELD_CATEGORY;
				
			}else if ( field_name.equals( "COMMENTS")){
				
				field_id	= FIELD_COMMENTS;
				
			}else if ( field_name.equals( "TORRENT")){
				
				field_id	= FIELD_TORRENTLINK;
				
			}else if ( field_name.equals( "CDP")){
				
				field_id	= FIELD_CDPLINK;
				
			}else{
				
				log( "Unrecognised field mapping '" + field_name + "'" );
				
				continue;
			}
			
			mappings[i] = new FieldMapping( field_group, field_id );
		}
		
		init();
	}
	
	protected void
	exportToBencodedMap(
		Map		map )
	
		throws IOException
	{
		super.exportToBencodedMap( map );
		
		exportString( map, "web.search_url_format", searchURLFormat );
		exportString( map, "web.time_zone", 		timeZone );		
		exportString( map, "web.date_format", 		userDateFormat );
		
		map.put( "web.auto_date", new Long( automaticDateParser?1:0));

		List	maps = new ArrayList();
		
		map.put( "web.maps", maps );
		
		for (int i=0;i<mappings.length;i++){
			
			FieldMapping fm = mappings[i];
			
			Map m = new HashMap();
			
			exportString( m, "name", fm.getName());
			m.put( "field", new Long( fm.getField()));
			
			maps.add( m );
		}
	}
	
	protected void
	init()
	{
		try {
			Matcher m = rootURLPattern.matcher(searchURLFormat);
			if(m.find()) {
				this.rootPage = m.group(1);
			}
		} catch(Exception e) {
			//Didn't find the root url within the URL
			this.rootPage = null;
		}
		
		try {
			Matcher m = baseURLPattern.matcher(searchURLFormat);
			if(m.find()) {
				this.basePage = m.group(1);
			}
		} catch(Exception e) {
			//Didn't find the root url within the URL
			this.basePage = null;
		}
		
		this.dateParser = new DateParser(timeZone,automaticDateParser,userDateFormat);
	}
	
	protected String getWebPageContent(SearchParameter[] searchParameters) {
		
		try {
			String searchURL = searchURLFormat;
			
			for(int i = 0 ; i < searchParameters.length ; i++){
				SearchParameter parameter = searchParameters[i];
				//String escapedKeyword = URLEncoder.encode(parameter.getValue(),"UTF-8");
				String escapedKeyword = parameter.getValue();
				searchURL = searchURL.replaceAll("%" + parameter.getMatchPattern(), escapedKeyword);
			}
			
			debugLog( "search_url: " + searchURL );
			
			URL url = new URL(searchURL);
			
			ResourceDownloaderFactory rdf = StaticUtilities.getResourceDownloaderFactory();
			
			ResourceDownloader url_rd = rdf.create( url );
						
			setHeaders( url_rd );
			
			/*if(cookieParameters!= null && cookieParameters.length > 0) {
				String 	cookieString = "";
				String separator = "";
				for(CookieParameter parameter : cookieParameters) {
					cookieString += separator + parameter.getName() + "=" + parameter.getValue();
					separator = "; ";
				}				
				url_rd.setProperty( "URL_Cookie", cookieString );
			}*/
			
			ResourceDownloader mr_rd = rdf.getMetaRefreshDownloader( url_rd );

			StringBuffer sb = new StringBuffer();
			
			byte[] data = new byte[8192];

			InputStream is = mr_rd.download();

			int nbRead = 0;
			while((nbRead = is.read(data)) != -1) {
				sb.append(new String(data,0,nbRead));
			}

			String page = sb.toString();

			debugLog( "page:" );
			debugLog( page );

			// List 	cookie = (List)url_rd.getProperty( "URL_Set-Cookie" );
			
			try {
				Matcher m = baseTagPattern.matcher(page);
				if(m.find()) {
					basePage = m.group(1);
					
					debugLog( "base_page: " + basePage );
				}
			} catch(Exception e) {
				//No BASE tag in the page
			}
			
			
			return page;
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void
	setHeaders(
		ResourceDownloader	rd )
	{
			// test headers
		
		String s = "SG9zdDogbG9jYWxob3N0OjQ1MTAwClVzZXItQWdlbnQ6IE1vemlsbGEvNS4wIChXaW5kb3dzOyBVOyBXaW5kb3dzIE5UIDUuMTsgZW4tVVM7IHJ2OjEuOC4xLjE0KSBHZWNrby8yMDA4MDQwNCBGaXJlZm94LzIuMC4wLjE0CkFjY2VwdDogdGV4dC94bWwsYXBwbGljYXRpb24veG1sLGFwcGxpY2F0aW9uL3hodG1sK3htbCx0ZXh0L2h0bWw7cT0wLjksdGV4dC9wbGFpbjtxPTAuOCxpbWFnZS9wbmcsKi8qO3E9MC41CkFjY2VwdC1MYW5ndWFnZTogZW4tdXMsZW47cT0wLjUKQWNjZXB0LUVuY29kaW5nOiBnemlwLGRlZmxhdGUKQWNjZXB0LUNoYXJzZXQ6IElTTy04ODU5LTEsdXRmLTg7cT0wLjcsKjtxPTAuNwpLZWVwLUFsaXZlOiAzMDAKQ29ubmVjdGlvbjoga2VlcC1hbGl2ZQ==";
		
		try{
		
			String header_string = new String( Base64.decode( s ), "UTF-8" );
		
			String[]	headers = header_string.split( "\n" );
			
			for (int i=0;i<headers.length;i++ ){
			
				String	header = headers[i];
				
				int	pos = header.indexOf( ':' );
				
				if ( pos != -1 ){
					
					String	lhs = header.substring(0,pos).trim();
					String	rhs	= header.substring(pos+1).trim();
					
					if ( !lhs.toLowerCase().equals("host")){
						
						rd.setProperty( "URL_" + lhs, rhs );
					}
				}
			}
		}catch( Throwable e ){
			
		}
	}
	
	public String getIcon() {
		if(rootPage != null) {
			return rootPage + "/favicon.ico";
		}
		return null;
	}
	
	protected FieldMapping[]
	getMappings()
	{
		return( mappings );
	}
	
	protected String
	getRootPage()
	{
		return( rootPage );
	}
	
	protected String
	getBasePage()
	{
		return( basePage );
	}
	
	protected DateParser
	getDateParser()
	{
		return( dateParser );
	}
}
