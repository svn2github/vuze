package com.aelitis.azureus.core.metasearch.impl.web.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;

public class 
JSONEngine 
	extends WebEngine 
{
	public static Engine
	importFromBEncodedMap(
		Map		map )
	
		throws IOException
	{
		return( new JSONEngine( map ));
	}
	

	
	private String resultsEntryPath;

	

	public JSONEngine(long id,long last_updated,String name,String searchURLFormat,String timeZone,boolean automaticDateFormat,String userDateFormat,String resultsEntryPath,FieldMapping[] mappings) {
		super( Engine.ENGINE_TYPE_JSON, id,last_updated,name,searchURLFormat,timeZone,automaticDateFormat,userDateFormat,mappings);
		
		this.resultsEntryPath = resultsEntryPath;
	}
	
	protected 
	JSONEngine(
		Map		map )
	
		throws IOException
	{
		super( map );
		
		this.resultsEntryPath = importString( map, "json.path" );
	}
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException
	{
		Map	res = new HashMap();
		
		exportString( res, "json.path", resultsEntryPath );
		
		super.exportToBencodedMap( res );
		
		return( res );
	}
	
	public Result[] search(SearchParameter[] searchParameters) throws SearchException {
		
		String page = super.getWebPageContent(searchParameters);
		
		FieldMapping[] mappings = getMappings();

		if(page != null) {
			try {
				Object jsonObject = JSONValue.parse(page);
				
				JSONArray resultArray = null;
				
				if(resultsEntryPath != null) {
					StringTokenizer st = new StringTokenizer(resultsEntryPath,";");
					if(jsonObject instanceof JSONArray && st.countTokens() > 0) {
						JSONArray array = (JSONArray) jsonObject;
						if(array.size() == 1) {
							jsonObject = array.get(0);
						}
					}
					while(st.hasMoreTokens()) {
						try {
							jsonObject = ((JSONObject)jsonObject).get(st.nextToken());
						} catch(Throwable t) {
							throw new SearchException("Invalid entry path : " + resultsEntryPath,t);
						}
					}
				}
				
				try {
					resultArray = (JSONArray) jsonObject;
				} catch (Throwable t) {
					throw new SearchException("Object is not a result array. Check the JSON service and/or the entry path");
				}
					
					
				if(resultArray != null) {
					
					List results = new ArrayList();
					
					for(int i = 0 ; i < resultArray.size() ; i++) {
						Object obj = resultArray.get(i);
						if(obj instanceof JSONObject) {
							JSONObject jsonEntry = (JSONObject) obj;
							WebResult result = new WebResult(getRootPage(),getBasePage(),getDateParser());
							for(int j = 0 ; j < mappings.length ; j++) {
								String fieldFrom = mappings[j].getName();
								if(fieldFrom != null) {
									int fieldTo = mappings[j].getField();
									Object fieldContentObj = ((Object)jsonEntry.get(fieldFrom));
									if(fieldContentObj != null) {
										String fieldContent = fieldContentObj.toString();
										switch(fieldTo) {
										case FIELD_NAME :
											result.setNameFromHTML(fieldContent);
											break;
										case FIELD_SIZE :
											result.setSizeFromHTML(fieldContent);
											break;
										case FIELD_PEERS :
											result.setNbPeersFromHTML(fieldContent);
											break;
										case FIELD_SEEDS :
											result.setNbSeedsFromHTML(fieldContent);
											break;
										case FIELD_CATEGORY :
											result.setCategoryFromHTML(fieldContent);
											break;
										case FIELD_DATE :
											result.setPublishedDateFromHTML(fieldContent);
											break;
										case FIELD_COMMENTS :
											result.setCommentsFromHTML(fieldContent);
											break;
										case FIELD_CDPLINK :
											result.setCDPLink(fieldContent);
											break;
										case FIELD_TORRENTLINK :
											result.setTorrentLink(fieldContent);
											break;
										default:
											break;
										}
									}
								}
							}
							results.add(result);
							
						}
					}
					
					return (Result[]) results.toArray(new Result[results.size()]);
					
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new SearchException(e);
			}
		} else {
			throw new SearchException("Web Page is empty");
		}
		
		return new Result[0];
	}
	
	public String getIcon() {
		
		String rootPage = getRootPage();
		
		if(rootPage != null) {
			return rootPage + "/favicon.ico";
		}
		return null;
	}
	

}
