package com.aelitis.azureus.core.metasearch.impl.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.*;

public abstract class WebEngine extends EngineImpl {

	public static final int FIELD_NAME = 1;
	public static final int FIELD_DATE = 2;
	public static final int FIELD_SIZE = 3;
	public static final int FIELD_PEERS = 4;
	public static final int FIELD_SEEDS = 5;
	public static final int FIELD_CATEGORY = 6;
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
	

	public WebEngine(int type, long id,String name,String searchURLFormat,String timeZone,boolean automaticDateParser,String userDateFormat, FieldMapping[] mappings ) {
		
		super( type, id, name );

		this.searchURLFormat 		= searchURLFormat;
		this.timeZone 				= timeZone;
		this.automaticDateParser 	= automaticDateParser;
		this.userDateFormat 		= userDateFormat;
		this.mappings				= mappings;
		
		init();
	}
	
	protected 
	WebEngine(
		Map		map )
	
		throws IOException
	{
		super( map );
		
		searchURLFormat 	= new String((byte[])map.get( "web.search_url_format" ), "UTF-8" );
		timeZone			= new String((byte[])map.get( "web.time_zone" ), "UTF-8" );
		automaticDateParser	= ((Long)map.get( "web.auto_date" )).longValue()==1;
		userDateFormat		= new String((byte[])map.get( "web.date_format" ), "UTF-8" );

		List	maps = (List)map.get( "web.maps" );
		
		mappings = new FieldMapping[maps.size()];
		
		for (int i=0;i<mappings.length;i++){
			
			Map	m = (Map)maps.get(i);
			
			mappings[i] = 
				new FieldMapping(
					new String((byte[])m.get( "name" ), "UTF-8" ),
					((Long)m.get( "field")).intValue());
		}
		
		init();
	}
	
	protected void
	exportToBencodedMap(
		Map		map )
	
		throws IOException
	{
		super.exportToBencodedMap( map );
		
		map.put( "web.search_url_format", 	searchURLFormat.getBytes( "UTF-8" ));
		map.put( "web.time_zone", 			timeZone.getBytes( "UTF-8" ));
		map.put( "web.auto_date", 			new Long( automaticDateParser?1:0));
		map.put( "web.date_format", 		userDateFormat.getBytes( "UTF-8" ));

		List	maps = new ArrayList();
		
		map.put( "web.maps", maps );
		
		for (int i=0;i<mappings.length;i++){
			
			FieldMapping fm = mappings[i];
			
			Map m = new HashMap();
			
			map.put( "name", fm.getName().getBytes( "UTF-8" ));
			map.put( "field", new Long( fm.getField()));
			
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
			
			URL url = new URL(searchURL);
			
			ResourceDownloaderFactory rdf = StaticUtilities.getResourceDownloaderFactory();
			
			ResourceDownloader url_rd = rdf.create( url );
									
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

			// List 	cookie = (List)url_rd.getProperty( "URL_Set-Cookie" );
			
			try {
				Matcher m = baseTagPattern.matcher(page);
				if(m.find()) {
					basePage = m.group(1);
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
