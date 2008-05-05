package com.aelitis.azureus.core.metasearch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.core.metasearch.utils.MomentsAgoDateFormatter;

public abstract class Result {


	public abstract Date getPublishedDate();
	
	public abstract String getCategory();
	public abstract String getName();
	public abstract long getSize();
	public abstract int getNbPeers();
	public abstract int getNbSeeds();
	
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
		int size = (int) (this.getSize() / (1024 * 1024 / 100));
		int sizeI = size / 100;
		int sizeD = size % 100;
		String sizeS = sizeI + ".";
		if(sizeD < 10) {
			sizeS += "0";
		}
		sizeS += sizeD + " MB";
		object.put("l", sizeS);
		object.put("cdp", this.getCDPLink());
		return object;
	}
	
}
