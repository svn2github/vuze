package com.aelitis.azureus.core.metasearch.impl.web.regex;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.MetaSearchImpl;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class 
RegexEngine 
	extends WebEngine 
{	
	public static Engine
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
		Map					map )
	
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
		
		setSource( Engine.ENGINE_SOURCE_MANUAL );
		
		setSelected( true );
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
		Map					map )
	
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
	init(
		String			resultPattern )
	{
		pattern_str 	= resultPattern.trim();
		pattern			= Pattern.compile(pattern_str);
	}
	
	public Result[] 
	search(
		SearchParameter[] searchParameters ) 
	
		throws SearchException 
	{
		debugStart();
				
		String page = getWebPageContent(searchParameters);
				
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
		
		FieldMapping[] mappings = getMappings();
		
		if ( page != null ){
			
			try {
				
				List results = new ArrayList();
					
				Matcher m = pattern.matcher( page );
					
				while( m.find()){
					
					debugLog( "Found match:" );
					
					WebResult result = new WebResult(getRootPage(),getBasePage(),getDateParser());
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

}
