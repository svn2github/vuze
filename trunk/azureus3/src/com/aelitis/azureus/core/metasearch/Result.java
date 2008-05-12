package com.aelitis.azureus.core.metasearch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.aelitis.azureus.core.metasearch.utils.MomentsAgoDateFormatter;

public abstract class Result {

	public abstract Date getPublishedDate();
	
	public abstract String getCategory();
	public abstract String getName();
	public abstract long getSize();
	public abstract int getNbPeers();
	public abstract int getNbSeeds();
	public abstract int getComments();
	
	//Links
	public abstract String getDownloadLink();
	public abstract String getCDPLink();
	
	
	public String toString() {
		return getName() + " : " + getNbSeeds() + " s, " + getNbPeers() + "p, "  ;
	}
	
	public Map toMap() {
		Map object = new HashMap();
		try {
			object.put("d", MomentsAgoDateFormatter.getMomentsAgoString(this.getPublishedDate()));
			object.put("ts", "" + this.getPublishedDate().getTime());
		} catch(Exception e) {
			object.put("d", "unknown");
			object.put("ts", "0");
		}
		
		object.put("c", this.getCategory());
		object.put("n",this.getName());
		object.put("s","" +this.getNbSeeds());
		object.put("p","" + this.getNbPeers());
		
		int	comments = getComments();
		
		if ( comments >= 0 ){
		
			object.put( "co", "" + comments );
		}
		
		long size = this.getSize();


		object.put("l", DisplayFormatters.formatByteCountToKiBEtc( size ));
		object.put("lb", new Long( size ));
		object.put("cdp", this.getCDPLink());
		return object;
	}
	
}
