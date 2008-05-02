package com.aelitis.azureus.core.metasearch.impl.regex;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aelitis.azureus.core.metasearch.CookieParameter;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.DateParser;

public class RegexEngine implements Engine {

	public static final int GROUP_DISCARD = 0;
	public static final int GROUP_NAME = 1;
	public static final int GROUP_DATE = 2;
	public static final int GROUP_SIZE = 3;
	public static final int GROUP_PEERS = 4;
	public static final int GROUP_SEEDS = 5;
	public static final int GROUP_CAT = 6;
	public static final int GROUP_CATLINK = 101;
	public static final int GROUP_TORRENTLINK = 102;
	public static final int GROUP_CDPLINK = 103;
	
	
	long id;
	String name;
	String icon;
	
	String mainPage;
	String searchURLFormat;
	
	Pattern pattern;
	
	int[]	matchOrder;
	
	DateParser dateParser;
	
	public RegexEngine(long id,String name,String icon,String mainPage,String searchURLFormat,String resultPattern,String timeZone,int[] matchOrder) {
		this.id = id;
		this.icon = icon;
		this.name = name;
		this.mainPage = mainPage;
		this.searchURLFormat = searchURLFormat;
		this.dateParser = new DateParser(timeZone);
		this.pattern = Pattern.compile(resultPattern);
		this.matchOrder = matchOrder;
	}
	
	
	public Result[] search(SearchParameter[] searchParameters) {
		
		try {
			
			
			List results = new ArrayList();
			
			/*int nbPages = 1;
			if(searchURLFormat.contains("%p")) {
				nbPages = 2;
			}*/
			
			//for(int pageNb = 0 ; pageNb < nbPages ; pageNb++) {
			String searchURL = searchURLFormat;
			for(int i = 0 ; i < searchParameters.length ; i++){
				SearchParameter parameter = searchParameters[i];
				//String escapedKeyword = URLEncoder.encode(parameter.getValue(),"UTF-8");
				String escapedKeyword = parameter.getValue();
				searchURL = searchURL.replaceAll("%" + parameter.getMatchPattern(), escapedKeyword);
			}
			
			URLConnection conn = new URL(searchURL).openConnection();
			
			//HttpURLConnection httpConn = (HttpURLConnection) conn;
			
			/*if(cookieParameters!= null && cookieParameters.length > 0) {
				String 	cookieString = "";
				String separator = "";
				for(CookieParameter parameter : cookieParameters) {
					cookieString += separator + parameter.getName() + "=" + parameter.getValue();
					separator = "; ";
				}
				conn.setRequestProperty("Cookie", cookieString);
			}*/
			
			conn.connect();
				
				
				StringBuffer sb = new StringBuffer();
				byte[] data = new byte[8192];
				
				InputStream is = conn.getInputStream();
				int nbRead = 0;
				while((nbRead = is.read(data)) != -1) {
					sb.append(new String(data,0,nbRead));
				}
				
				String page = sb.toString();
				
				Matcher m = pattern.matcher(page);
				
				
				
				
				while(m.find()) {
					WebResult result = new WebResult(name,mainPage,dateParser);
					for(int i = 0 ; i < matchOrder.length ; i++) {
						int group = matchOrder[i];
						String groupContent = m.group(i+1);
						switch(group) {
						case GROUP_DISCARD :
							break;
						case GROUP_NAME :
							result.setNameFromHTML(groupContent);
							break;
						case GROUP_SIZE :
							result.setSizeFromHTML(groupContent);
							break;
						case GROUP_PEERS :
							result.setNbPeersFromHTML(groupContent);
							break;
						case GROUP_SEEDS :
							result.setNbSeedsFromHTML(groupContent);
							break;
						case GROUP_CAT :
							result.setCategoryFromHTML(groupContent);
							break;
						case GROUP_DATE :
							result.setPublishedDateFromHTML(groupContent);
							break;
						case GROUP_CATLINK :
							result.setCategoryLink(groupContent);
							break;
						case GROUP_CDPLINK :
							result.setCDPLink(groupContent);
							break;
						case GROUP_TORRENTLINK :
							result.setTorrentLink(groupContent);
							break;
						default:
							break;
						}
					}
					results.add(result);
				}
			//}
			return (Result[]) results.toArray(new Result[results.size()]);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Result[0];
	}

	public String getName() {
		return name;
	}
	
	public String getIcon() {
		return icon;
	}
	
	public long getId() {
		return id;
	}
	
}
