/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package com.aelitis.azureus.core.metasearch.impl.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.*;
import com.aelitis.azureus.core.util.GeneralUtils;
import com.aelitis.azureus.util.ImportExportUtils;

public abstract class 
WebEngine 
	extends EngineImpl 
{
	
	static private final Pattern baseTagPattern = Pattern.compile("(?i)<base.*?href=\"([^\"]+)\".*?>");
	static private final Pattern rootURLPattern = Pattern.compile("(https?://[^/]+)");
	static private final Pattern baseURLPattern = Pattern.compile("(https?://.*/)");
		
	
	private String 			searchURLFormat;
	private String 			timeZone;
	private boolean			automaticDateParser;
	private String 			userDateFormat;
	private String			downloadLinkCSS;
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
		FieldMapping[] 	mappings ) 
	{	
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
		
		searchURLFormat 	= ImportExportUtils.importString( map, "web.search_url_format" );
		timeZone			= ImportExportUtils.importString( map, "web.time_zone" );
		userDateFormat		= ImportExportUtils.importString( map, "web.date_format" );
		downloadLinkCSS		= ImportExportUtils.importString( map, "web.dl_link_css" );
		
		automaticDateParser	= ImportExportUtils.importBoolean( map, "web.auto_date", true );

		List	maps = (List)map.get( "web.maps" );
		
		mappings = new FieldMapping[maps.size()];
		
		for (int i=0;i<mappings.length;i++){
			
			Map	m = (Map)maps.get(i);
			
			mappings[i] = 
				new FieldMapping(
						ImportExportUtils.importString( m, "name" ),
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
		
		ImportExportUtils.exportString( map, "web.search_url_format", searchURLFormat );
		ImportExportUtils.exportString( map, "web.time_zone", 		timeZone );		
		ImportExportUtils.exportString( map, "web.date_format", 		userDateFormat );
		ImportExportUtils.exportString( map, "web.dl_link_css",		downloadLinkCSS );
		
		map.put( "web.auto_date", new Long( automaticDateParser?1:0));

		List	maps = new ArrayList();
		
		map.put( "web.maps", maps );
		
		for (int i=0;i<mappings.length;i++){
			
			FieldMapping fm = mappings[i];
			
			Map m = new HashMap();
			
			ImportExportUtils.exportString( m, "name", fm.getName());
			m.put( "field", new Long( fm.getField()));
			
			maps.add( m );
		}
	}
	
		// json encoded constructor
	
	protected 
	WebEngine(
		MetaSearchImpl	meta_search,
		int				type,
		long			id,
		long			last_updated,
		String			name,
		JSONObject		map )
	
		throws IOException
	{
		super( meta_search, type, id, last_updated, name, map );
		
		searchURLFormat 	= ImportExportUtils.importString( map, "searchURL" );
		timeZone			= ImportExportUtils.importString( map, "timezone" );
		userDateFormat		= ImportExportUtils.importString( map, "time_format" );
		downloadLinkCSS		= ImportExportUtils.importString( map, "download_link" );

		if ( downloadLinkCSS != null ){
			
			downloadLinkCSS = downloadLinkCSS.trim();
			
			if ( downloadLinkCSS.length() == 0 ){
				
				downloadLinkCSS = null;
				
			}else{
				
				downloadLinkCSS = URLDecoder.decode( downloadLinkCSS, "UTF-8" );
			}
		}
		
		searchURLFormat = URLDecoder.decode( searchURLFormat, "UTF-8" );
		
		automaticDateParser	= userDateFormat == null || userDateFormat.trim().length() == 0;

		List	maps = (List)map.get( "column_map" );
		
		List	conv_maps = new ArrayList();
		
		for (int i=0;i<maps.size();i++){
			
			Map	m = (Map)maps.get(i);
				
				// backwards compact from when there was a mapping entry
			
			Map test = (Map)m.get( "mapping" );
			
			if ( test != null ){
				
				m = test;
			}
			
			String	vuze_field 	= ImportExportUtils.importString( m, "vuze_field" ).toUpperCase();
			
			String	field_name	= ImportExportUtils.importString( m, "group_nb" );	// regexp case
			
			if ( field_name == null ){
				
				field_name = ImportExportUtils.importString( m, "field_name" );	// json case
			}
			
			if ( vuze_field == null || field_name == null ){
				
				log( "Missing field mapping name/value in '" + m + "'" );

			}
			int	field_id = vuzeFieldToID( vuze_field );
			
			if ( field_id == -1 ){
				
				log( "Unrecognised field mapping '" + vuze_field + "'" );
				
				continue;
			}
			
			conv_maps.add( new FieldMapping( field_name, field_id ));
		}
		
		mappings = (FieldMapping[])conv_maps.toArray( new FieldMapping[conv_maps.size()]);
		
		init();
	}
	
	protected void
	exportToJSONObject(
		JSONObject		res )
	
		throws IOException
	{		
		super.exportToJSONObject( res );
		
		res.put( "searchURL", 	UrlUtils.encode( searchURLFormat));
		res.put( "timezone", 	timeZone );	
		
		if ( downloadLinkCSS != null ){
			
			res.put( "download_link", UrlUtils.encode( downloadLinkCSS ));
		}
		
		if ( !automaticDateParser ){
			
			res.put( "time_format",	userDateFormat );
		}
		
		JSONArray	maps = new JSONArray();
		
		res.put( "column_map", maps );

		for (int i=0;i<mappings.length;i++){
			
			FieldMapping fm = mappings[i];
			
			int	field_id = fm.getField();
			
			String	field_value = vuzeIDToField( field_id );
			
			if ( field_value == null ){
				
				log( "JSON export: unknown field id " + field_id );
				
			}else{
							
				JSONObject entry = new JSONObject();

				maps.add( entry );
					
				entry.put( "vuze_field", field_value );
				
				if ( getType() == ENGINE_TYPE_JSON ){
					
					entry.put( "field_name", fm.getName());
					
				}else{
					
					entry.put( "group_nb", fm.getName());
				}
			}
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
		
		this.dateParser = new DateParserRegex(timeZone,automaticDateParser,userDateFormat);
	}
	
	public String 
	getReferer() 
	{
		return( getRootPage());
	}
	
	protected String 
	getWebPageContent(
		SearchParameter[] 	searchParameters,
		String				headers )
	
		throws SearchException
	{
		
		try {
			String searchURL = searchURLFormat;
			
			String[]	from_strs 	= new String[ searchParameters.length ];
			String[]	to_strs 	= new String[ searchParameters.length ];
			
			for( int i = 0 ; i < searchParameters.length ; i++ ){
				
				SearchParameter parameter = searchParameters[i];
				
				from_strs[i]	= "%" + parameter.getMatchPattern();
				to_strs[i]		= URLEncoder.encode(parameter.getValue(),"UTF-8");
			}
			
			searchURL = 
					GeneralUtils.replaceAll( searchURL, from_strs, to_strs );
				
			//System.out.println(searchURL);
			
			debugLog( "search_url: " + searchURL );
			
			URL url = new URL(searchURL);
			
			ResourceDownloaderFactory rdf = StaticUtilities.getResourceDownloaderFactory();
			
			ResourceDownloader url_rd = rdf.create( url );
						
			setHeaders( url_rd, headers );
			
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

			List cts = (List)url_rd.getProperty( "URL_Content-Type" );
			
			String content_charset = "UTF-8";
			
			if ( cts != null && cts.size() > 0 ){
				
				String	content_type = (String)cts.get(0);
				
				int	pos = content_type.toLowerCase().indexOf( "charset" );
				
				if ( pos != -1 ){
					
					content_type = content_type.substring( pos+1 );
					
					pos = content_type.indexOf('=');
					
					if ( pos != -1 ){
						
						content_type = content_type.substring( pos+1 ).trim();
						
						pos = content_type.indexOf(';');
						
						if ( pos != -1 ){
							
							content_type = content_type.substring(0,pos).trim();
						}
						
						if ( Charset.isSupported( content_type )){
							
							debugLog( "charset: " + content_type );
							
							content_charset = content_type;
						}
					}
				}
			}
			
			int nbRead = 0;
			
			while((nbRead = is.read(data)) != -1){
				
				sb.append(new String(data,0,nbRead, content_charset ));
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
				
		}catch( Throwable e) {
			
			e.printStackTrace();
			
			throw( new SearchException( "Failed to load page", e ));
		}
	}

	protected void
	setHeaders(
		ResourceDownloader		rd,
		String					encoded_headers )
	{
		UrlUtils.setBrowserHeaders( rd, encoded_headers, rootPage );
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
	
	public String 
	getDownloadLinkCSS()
	{
		if ( downloadLinkCSS == null ){
			
			return( "" );
		}
		
		return( downloadLinkCSS );
	}
}
