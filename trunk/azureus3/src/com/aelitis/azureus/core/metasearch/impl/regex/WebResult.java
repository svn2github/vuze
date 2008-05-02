package com.aelitis.azureus.core.metasearch.impl.regex;

import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.lang.*;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.impl.DateParser;

public class WebResult extends Result {
	
	
	String mainPageURL;
	DateParser dateParser;
	
	String name;
	String category = "";
	
	Date publishedDate;
	
	long size;
	int nbPeers;
	int nbSeeds;
	
	String cdpLink;
	String torrentLink;
	String categoryLink;
	
	String source;
	
	public WebResult(String source,String mainPageURL,DateParser dateParser) {
		this.source = source;
		this.mainPageURL = mainPageURL;
		this.dateParser = dateParser;
	}
	
	
	
	public void setNameFromHTML(String name) {
		if(name != null) {
			this.name = Entities.HTML40.unescape(name);
		}
	}
	
	public void setCategoryFromHTML(String category) {
		if(category != null) {
			this.category = Entities.HTML40.unescape(category).trim();
			int separator = this.category.indexOf(">");
			
			if(separator != -1) {
				this.category = this.category.substring(separator+1).trim();
			}
		}
	}
	
	public void setNbPeersFromHTML(String nbPeers) {
		if(nbPeers != null) {
			String nbPeersS = Entities.HTML40.unescape(nbPeers);
			try {
				this.nbPeers = Integer.parseInt(nbPeersS);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setNbSeedsFromHTML(String nbSeeds) {
		if(nbSeeds != null) {
			String nbSeedsS = Entities.HTML40.unescape(nbSeeds);
			try {
				this.nbSeeds = Integer.parseInt(nbSeedsS);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setPublishedDateFromHTML(String publishedDate) {
		if(publishedDate != null) {
			String publishedDateS = Entities.HTML40.unescape(publishedDate).replace((char)160,(char)32);
			this.publishedDate = dateParser.parseDate(publishedDateS);
		}
	}
	

	public void setSizeFromHTML(String size) {
		if(size != null) {
			String sizeS = Entities.HTML40.unescape(size).replace((char)160,(char)32);
			sizeS = sizeS.replaceAll("<[^>]+>", " ");
			try {
				StringTokenizer st = new StringTokenizer(sizeS," ");
				double base = Double.parseDouble(st.nextToken());
				String unit = st.nextToken().toLowerCase();
				long multiplier = 1;
				if("mb".equals(unit)) {
					multiplier = 1000*1000;
				} else if("mib".equals(unit)) {
					multiplier = 1024*1024;
				} else if("gb".equals(unit)) {
					multiplier = 1000*1000*1000;
				} else if("gib".equals(unit)) {
					multiplier = 1024*1024*1024;
				} else if("kb".equals(unit)) {
					multiplier = 1000;
				} else if("kib".equals(unit)) {
					multiplier = 1024;
				}
				
				this.size = (long) (base * multiplier);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setCDPLink(String cdpLink) {
		this.cdpLink = cdpLink;
	}
	
	public void setTorrentLink(String torrentLink) {
		this.torrentLink = torrentLink;
	}
	
	public void setCategoryLink(String categoryLink) {
		this.categoryLink = categoryLink;
	}
	

	public String getCDPLink() {
		if(cdpLink.startsWith("http")) {
			return cdpLink;
		}
		if(cdpLink.startsWith("/")) {
			return mainPageURL + cdpLink;
		}
		
		/*
		 * TODO : this is obviously wrong, and we should use the relative url scheme here.
		 */
		return mainPageURL + "/" + cdpLink;
	}

	public String getCategory() {
		return category;
	}

	public String getCategoryLink() {
		if(categoryLink.startsWith("http")) {
			return categoryLink;
		}
		return mainPageURL + categoryLink;
	}

	public String getDownloadLink() {
		if(torrentLink.startsWith("http")) {
			return torrentLink;
		}
		return mainPageURL + torrentLink;
	}

	public String getName() {
		return name;
	}

	public int getNbPeers() {
		return nbPeers;
	}

	public int getNbSeeds() {
		return nbSeeds;
	}

	public Date getPublishedDate() {
		return publishedDate;
	}

	public long getSize() {
		return size;
	}
	
	public String getEngineName() {
		return source;
	}
	

}
