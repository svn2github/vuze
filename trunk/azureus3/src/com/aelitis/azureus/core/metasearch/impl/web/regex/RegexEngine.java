package com.aelitis.azureus.core.metasearch.impl.web.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class RegexEngine extends WebEngine {
	
	Pattern pattern;
	
	FieldMapping[] mappings;
	
	public RegexEngine(long id,String name,String searchURLFormat,String resultPattern,String timeZone,FieldMapping[] mappings) {
		super(id,name,searchURLFormat,timeZone);

		this.pattern = Pattern.compile(resultPattern);
		this.mappings = mappings;
	}
	
	
	public Result[] search(SearchParameter[] searchParameters) {
		
		String page = super.getWebPageContent(searchParameters);
		
		if(page != null) {
			try {
				
				List results = new ArrayList();
					
				Matcher m = pattern.matcher(page);
					
				while(m.find()) {
					WebResult result = new WebResult(rootPage,basePage,dateParser);
					for(int i = 0 ; i < mappings.length ; i++) {
						int group = -1;
						try {
							group = Integer.parseInt(mappings[i].name);
						} catch(Exception e) {
							//In "Debug/Test" mode, we should fire an exception / notification
						}
						if(group > 0 && group <= m.groupCount()) {
							int field = mappings[i].field;
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
				e.printStackTrace();
			}
		}
		
		return new Result[0];
	}
	
}
