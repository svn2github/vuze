package com.aelitis.azureus.core.metasearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.core.metasearch.utils.MomentsAgoDateFormatter;

public abstract class Result {

	final static DateFormat outputDateFormat = new SimpleDateFormat("d-MM-y hh:mm");

	public abstract Date getPublishedDate();
	
	public abstract String getCategory();
	public abstract String getName();
	public abstract long getSize();
	public abstract int getNbPeers();
	public abstract int getNbSeeds();
	
	//Links
	public abstract String getCategoryLink();
	public abstract String getDownloadLink();
	public abstract String getCDPLink();
	
	public abstract String getEngineName();
	
	public String toString() {
		return outputDateFormat.format(getPublishedDate()) + " : " + getName() + " (" + getDownloadLink() + ") " + getNbSeeds() + " s, " + getNbPeers() + "p, "  ;
	}
	
	public Map toMap() {
		Map object = new HashMap();
		object.put("d", MomentsAgoDateFormatter.getMomentsAgoString(this.getPublishedDate()));
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
