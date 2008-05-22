package com.aelitis.azureus.core.media;

public class MediaInfo {
	
	String fileName;
	boolean isPlayable;
	ExtendedMediaInfo extendedMediaInfo;
	
	public MediaInfo(String fileName) {
		super();
		this.fileName = fileName;
		this.isPlayable = false;
		this.extendedMediaInfo = null;
	}

	public boolean isPlayable() {
		return isPlayable;
	}

	public void setPlayable(boolean isPlayable) {
		this.isPlayable = isPlayable;
	}

	public ExtendedMediaInfo getExtendedMediaInfo() {
		return extendedMediaInfo;
	}

	public void setExtendedMediaInfo(ExtendedMediaInfo extendedMediaInfo) {
		this.extendedMediaInfo = extendedMediaInfo;
	}

	public String getFileName() {
		return fileName;
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(fileName);
		if(isPlayable) {
			sb.append(" is playable");
		} else {
			sb.append(" is not playable");
		}
		if(extendedMediaInfo != null) {
			sb.append("\n-------\n");
			sb.append(extendedMediaInfo.toString());
			sb.append("\n-------");
		}
		
		return sb.toString();
	}
	

}
