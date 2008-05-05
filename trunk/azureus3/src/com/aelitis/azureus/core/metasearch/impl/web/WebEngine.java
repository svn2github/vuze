package com.aelitis.azureus.core.metasearch.impl.web;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.DateParser;

public abstract class WebEngine implements Engine {

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
	
	protected long id;
	protected String name;
	
	protected String rootPage;
	protected String basePage;
	protected String searchURLFormat;
	
	protected DateParser dateParser;
	
	public WebEngine(long id,String name,String searchURLFormat,String timeZone,boolean automaticDateParser,String userDateFormat) {
		this.id = id;
		this.name = name;
		this.searchURLFormat = searchURLFormat;
		
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
			
			URLConnection conn = new URL(searchURL).openConnection();
			
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
