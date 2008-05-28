/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

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
	public abstract void setCategory(String category);
	
	public abstract String getContentType();
	public abstract void setContentType(String contentType);
	
	public abstract String getName();
	public abstract long getSize();
	public abstract int getNbPeers();
	public abstract int getNbSeeds();
	public abstract int getComments();
	
	//Links
	public abstract String getDownloadLink();
	public abstract String getCDPLink();
	public abstract String getPlayLink();
	
	
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
		
		if ( seeds < 0 ){
			seeds = 0;
		}
		
		if ( peers < 0 ){
			peers = 0;
		}
		
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
		
		if(this.getNbSeeds() >= 0) {
			object.put("s","" + this.getNbSeeds());
		} else {
			object.put("s","--");
		}
			
		if(this.getNbPeers() >= 0) {
			object.put("p","" + this.getNbPeers());
		} else {
			object.put("p","--");
		}
		
		int	comments = getComments();
		
		if ( comments >= 0 ){
		
			object.put( "co", "" + comments );
		}
		
		long size = this.getSize();
		if(size >= 0) {
			object.put("l", DisplayFormatters.formatByteCountToKiBEtc( size ));
			object.put("lb", "" + size  );
		} else {
			object.put("l", "--");
			object.put("lb", "0");
		}
		
		object.put("r", "" + this.getRank());
		
		object.put("ct", this.getContentType());
		
		object.put("cdp", this.getCDPLink());
		return object;
	}
	
}
