package com.aelitis.azureus.core.metasearch.impl.web.regex;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class 
RegexEngine 
	extends WebEngine 
{
	public static Engine
	importFromBEncodedMap(
		Map		map )
	
		throws IOException
	{
		return( new RegexEngine( map ));
	}
	

	private String	pattern_str;
	private Pattern pattern;

	
		
	public RegexEngine(long id,String name,String searchURLFormat,String resultPattern,String timeZone,boolean automaticDateFormat,String userDateFormat,FieldMapping[] mappings) 
	{
		super(Engine.ENGINE_TYPE_REGEX, id,name,searchURLFormat,timeZone,automaticDateFormat,userDateFormat, mappings );

		init( resultPattern );
	}
	
	protected 
	RegexEngine(
		Map		map )
	
		throws IOException
	{
		super( map );
		
		String	resultPattern = new String((byte[])map.get( "regex.pattern" ), "UTF-8" );

		init( resultPattern );
	}
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException
	{
		Map	res = new HashMap();
		
		res.put( "regex.pattern", pattern_str.getBytes( "UTF-8" ));
		
		super.exportToBencodedMap( res );
		
		return( res );
	}

	protected void
	init(
		String			resultPattern )
	{
		this.pattern_str 	= resultPattern;
		this.pattern		= Pattern.compile(resultPattern);
	}
	
	public Result[] search(SearchParameter[] searchParameters) throws SearchException {
		
		String page = getWebPageContent(searchParameters);
		
		FieldMapping[] mappings = getMappings();
		
		if(page != null) {
			try {
				
				List results = new ArrayList();
					
				Matcher m = pattern.matcher(page);
					
				while(m.find()) {
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
				//}
				return (Result[]) results.toArray(new Result[results.size()]);
			
			} catch (Exception e) {
				throw new SearchException(e);
			}
		} else {
			throw new SearchException("Web Page is empty");
		}
	}
}
