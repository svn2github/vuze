package com.aelitis.azureus.core.metasearch.impl.web.json;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class JSONEngine extends WebEngine {
	
	String resultsEntry;
	FieldMapping[] mappings;

	public JSONEngine(long id,String name,String searchURLFormat,String resultPattern,String timeZone,String resultsEntry,FieldMapping[] mappings) {
		super(id,name,searchURLFormat,timeZone);
		
		this.mappings = mappings;
	}
	
	
	public Result[] search(SearchParameter[] searchParameters) {
		
		String page = super.getWebPageContent(searchParameters);
		if(page != null) {
			try {
					Object jsonObject = JSONValue.parse(page);
					JSONArray resultArray = null;
					if(jsonObject instanceof JSONArray) {
						resultArray = (JSONArray) resultArray;
					} else if(jsonObject instanceof JSONObject) {
						if(resultsEntry != null) {
							jsonObject = ((JSONObject)jsonObject).get(resultsEntry);
							if(jsonObject instanceof JSONArray) {
								resultArray = (JSONArray) resultArray;
							} else {
								//TODO : in debug mode, fire an exception telling the user that the object is of invalid type
							}
						} else {
							//TODO : in debug mode, fire an exception telling the user that he needs to provide a top-level object name
						}
					}
					
					if(resultArray != null) {
						
						List results = new ArrayList();
						
						for(int i = 0 ; i < resultArray.size() ; i++) {
							Object obj = resultArray.get(i);
							if(obj instanceof JSONObject) {
								JSONObject jsonEntry = (JSONObject) obj;
								WebResult result = new WebResult(rootPage,basePage,dateParser);
								for(int j = 0 ; j < mappings.length ; j++) {
									String fieldFrom = mappings[i].name;
									if(fieldFrom != null) {
										int fieldTo = mappings[i].field;
										String fieldContent = ((Object)jsonEntry.get(fieldFrom)).toString();
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
								results.add(result);
								
							}
						}
						
						return (Result[]) results.toArray(new Result[results.size()]);
						
					}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return new Result[0];
	}

	public String getName() {
		return name;
	}
	
	public String getIcon() {
		if(rootPage != null) {
			return rootPage + "/favicon.ico";
		}
		return null;
	}
	
	public long getId() {
		return id;
	}
	
}
