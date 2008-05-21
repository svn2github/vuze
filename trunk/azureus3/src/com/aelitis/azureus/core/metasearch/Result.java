package com.aelitis.azureus.core.metasearch;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.Entities;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;

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
	
	public abstract String getSearchQuery();
	
	public String toString() {
		return getName() + " : " + getNbSeeds() + " s, " + getNbPeers() + "p, "  ;
	}
	
	
	public String getNameHTML() {
		if(getName() != null) {
			return Entities.HTML40.escape(getName());
		}
		return null;
	}
	
	public String getCategoryHTML() {
		if(getCategory() != null) {
			return Entities.HTML40.escape(getCategory());
		}
		return null;
	}
	
	/**
	 * 
	 * @return a value between 0 and 1 representing the rank of the result
	 */
	public float getRank() {

		int seeds = getNbSeeds();
		int peers = getNbPeers();
		
		int totalPeers = seeds + peers;
		int expectedDLSpeedKBS = 20 * (peers+seeds+1) / (peers+1);
		
		//We consider 150KB/s as a good dl speed
		float speed = (float)expectedDLSpeedKBS / 150f;
		
		
		if(speed > 3f) speed = 3f;
		
		//In case there are less than 20 peers, we apply a ratio
		//This is because with such a small amount of seeds we can't make sure that the client will connect to everyone
		float totalPeersFactor = (float)totalPeers / 500f;
		if(totalPeersFactor > 1f) totalPeersFactor = 1f;
		speed = speed * totalPeersFactor;
		
		//If we're going to download faster than 150kB/s let's simply consider it as a good speed (1)
		//if(speed > 1f) speed = 1f;
		
		Date publishedDate = getPublishedDate();
		double ageInWeeks = 520.0;
		if(publishedDate != null) {
			ageInWeeks  = 1 + ((SystemTime.getCurrentTime() - publishedDate.getTime()) / 604800000.0);
		}
		if(ageInWeeks <= 0) {
			ageInWeeks = 1;
		}
		

		
		float rank = (float) (speed / (1 + Math.log(ageInWeeks)/Math.log( 10 )));
		
		if(rank > 1f) rank = 1f;
		
		String queryString = getSearchQuery();
		String name = getName();
		if(queryString != null && name != null) {
			name = name.toLowerCase();
			//TODO :  METASEARCH Change this as soon as Gouss sends a non escaped string
			StringTokenizer st = new StringTokenizer(queryString, "%20");
			while(st.hasMoreElements()) {
				String match = st.nextToken().toLowerCase();
				if(name.indexOf(match) == -1) {
					rank /= 2;
				}
			}
		}
		
		return rank;
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
		
		object.put("c", this.getCategoryHTML());
		object.put("n",this.getNameHTML());
		object.put("s","" +this.getNbSeeds());
		object.put("p","" + this.getNbPeers());
		
		int	comments = getComments();
		
		if ( comments >= 0 ){
		
			object.put( "co", "" + comments );
		}
		
		long size = this.getSize();


		object.put("l", DisplayFormatters.formatByteCountToKiBEtc( size ));
		object.put("lb", "" + size  );
		
		object.put("r", "" + this.getRank());
		
		
		object.put("cdp", this.getCDPLink());
		return object;
	}
	
}
