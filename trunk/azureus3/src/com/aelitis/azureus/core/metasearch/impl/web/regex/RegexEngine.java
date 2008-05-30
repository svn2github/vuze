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

package com.aelitis.azureus.core.metasearch.impl.web.regex;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

import org.gudy.azureus2.core3.util.UrlUtils;
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
RegexEngine 
	extends WebEngine 
{	
	
	public static EngineImpl
	importFromBEncodedMap(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		return( new RegexEngine( meta_search, map ));
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
		return( new RegexEngine( meta_search, id, last_updated, name, map ));
	}

	private String	pattern_str;
	private Pattern pattern;

	
		// explicit test constructor
	
	public 
	RegexEngine(
		MetaSearchImpl		meta_search,
		long 				id,
		long 				last_updated,
		String 				name,
		String 				searchURLFormat,
		String 				resultPattern,
		String 				timeZone,
		boolean 			automaticDateFormat,
		String 				userDateFormat,
		FieldMapping[] 		mappings) 
	{
		super( meta_search, Engine.ENGINE_TYPE_REGEX, id,last_updated,name,searchURLFormat,timeZone,automaticDateFormat,userDateFormat, mappings );

		init( resultPattern );
		
		setSource( ENGINE_SOURCE_LOCAL );
		
		setSelectionState( SEL_STATE_MANUAL_SELECTED );
	}
	
		// bencoded 
	
	protected 
	RegexEngine(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		super( meta_search, map );
		
		String	resultPattern = importString( map, "regex.pattern" );

		init( resultPattern );
	}
	
		// json
	
	protected 
	RegexEngine(
		MetaSearchImpl		meta_search,
		long				id,
		long				last_updated,
		String				name,
		JSONObject			map )
	
		throws IOException
	{
		super( meta_search, Engine.ENGINE_TYPE_REGEX, id, last_updated, name, map );
		
		String	resultPattern = importString( map, "regexp" );

		resultPattern = URLDecoder.decode( resultPattern, "UTF-8" );
		
		init( resultPattern );
	}
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException
	{
		Map	res = new HashMap();
		
		exportString( res, "regex.pattern", pattern_str );
		
		super.exportToBencodedMap( res );
		
		return( res );
	}

	protected void
	exportToJSONObject(
		JSONObject		res )
	
		throws IOException
	{
		res.put( "regexp", UrlUtils.encode( pattern_str ));

		super.exportToJSONObject( res );
	}
	
	protected void
	init(
		String			resultPattern )
	{
		pattern_str 	= resultPattern.trim();
		pattern			= Pattern.compile(pattern_str);
	}
	
	protected Result[] 
	searchSupport(
		SearchParameter[] 	searchParameters,
		int					max_matches,
		String				headers,
		ResultListener		listener )
	
		throws SearchException 
	{
		debugStart();
				
		String page = getWebPageContent( searchParameters, headers );
			
		if ( listener != null ){
			
			listener.contentReceived( this, page );
		}
		
		debugLog( "pattern: " + pattern_str );
		
		/*
		if ( getId() == 3 ){
			
			writeToFile( "C:\\temp\\template.txt", page );
			writeToFile( "C:\\temp\\pattern.txt", pattern.pattern());
			
			String page2 = readFile( "C:\\temp\\template.txt" );
			
			Set s1 = new HashSet();
			Set s2 = new HashSet();
			
			for (int i=0;i<page.length();i++){
				s1.add( new Character( page.charAt(i)));
			}
			for (int i=0;i<page2.length();i++){
				s2.add( new Character( page2.charAt(i)));
			}
			
			s1.removeAll(s2);
			
			Iterator it = s1.iterator();
			
			while( it.hasNext()){
				
				Character c = (Character)it.next();
				
				System.out.println( "diff: " + c + "/" + (int)c.charValue());
			}
			
		}
		
		try{
			regexptest();
		}catch( Throwable e ){
			
		}
		 */
		
		String searchQuery = null;
		
		for(int i = 0 ; i < searchParameters.length ; i++) {
			if(searchParameters[i].getMatchPattern().equals("s")) {
				searchQuery = searchParameters[i].getValue();
			}
		}
		
		
		FieldMapping[] mappings = getMappings();
		
		if ( page != null ){
			
			try {
				
				List results = new ArrayList();
					
				Matcher m = pattern.matcher( page );
					
				while( m.find()){
					
					if ( max_matches >= 0 ){
						if ( --max_matches < 0 ){
							break;
						}
					}
					
					if ( listener != null ){
						
						String[]	groups = new String[m.groupCount()];
						
						for (int i=0;i<groups.length;i++){
							
							groups[i] = m.group(i+1);
						}
						
						listener.matchFound( this, groups );
					}
					
					debugLog( "Found match:" );
					
					WebResult result = new WebResult(getRootPage(),getBasePage(),getDateParser(),searchQuery);
					for(int i = 0 ; i < mappings.length ; i++) {
						int group = -1;
						try {
							group = Integer.parseInt(mappings[i].getName());
						} catch(Exception e) {
							//In "Debug/Test" mode, we should fire an exception / notification
						}
						if(group > 0 && group <= m.groupCount()) {
							
							int field = mappings[i].getField();
							String groupContent = m.group(group);
							
							debugLog( "    " + field + "=" + groupContent );
							
							switch(field) {
							case FIELD_NAME :
								result.setNameFromHTML(groupContent);
								break;
							case FIELD_SIZE :
								result.setSizeFromHTML(groupContent);
								break;
							case FIELD_PEERS :
								result.setNbPeersFromHTML(groupContent);
								break;
							case FIELD_SEEDS :
								result.setNbSeedsFromHTML(groupContent);
								break;
							case FIELD_CATEGORY :
								result.setCategoryFromHTML(groupContent);
								break;
							case FIELD_DATE :
								result.setPublishedDateFromHTML(groupContent);
								break;
							case FIELD_CDPLINK :
								result.setCDPLink(groupContent);
								break;
							case FIELD_TORRENTLINK :
								result.setTorrentLink(groupContent);
								break;
							default:
								break;
							}
						}
					}
					
					results.add(result);
				}
								
				return (Result[]) results.toArray(new Result[results.size()]);
			
			} catch (Exception e) {
				throw new SearchException(e);
			}
		} else {
			throw new SearchException("Web Page is empty");
		}
	}
	
	/*
	protected void
	writeToFile(
		String		file,
		String		str )
	{
		try{
			PrintWriter pw = new PrintWriter( new FileWriter( new File( file )));
			
			pw.println( str );
			
			pw.close();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private static String
	readFile(
		String	file )
	{
		try{
			StringBuffer sb = new StringBuffer();
			
			LineNumberReader lnr = new LineNumberReader( new FileReader( new File( file )));
			
			while( true ){
				
				String 	line = lnr.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				sb.append( line );
			}
			
			return( sb.toString());
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( null );
		}
	}
	
	private static void
	regexptest()
	
		throws Exception
	{
		Pattern pattern = Pattern.compile( readFile( "C:\\temp\\pattern.txt" ));
		
		String	page = readFile( "C:\\temp\\template.txt" );
		
		Matcher m = pattern.matcher( page);
		
		while(m.find()) {
			
			int groups = m.groupCount();
			
			System.out.println( "found match: groups = " + groups );
		}
	}
	*/
}
