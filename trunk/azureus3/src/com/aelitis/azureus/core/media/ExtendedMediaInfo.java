package com.aelitis.azureus.core.media;

public class ExtendedMediaInfo {
	
	String videoFormat,audioFormat,demuxer;
	int width,height;
	float duration;
	
	public ExtendedMediaInfo() {
		
	}

	public String getVideoFormat() {
		return videoFormat;
	}

	public void setVideoFormat(String videoFormat) {
		this.videoFormat = videoFormat;
	}

	public String getAudioFormat() {
		return audioFormat;
	}

	public void setAudioFormat(String audioFormat) {
		this.audioFormat = audioFormat;
	}

	public String getDemuxer() {
		return demuxer;
	}

	public void setDemuxer(String demuxer) {
		this.demuxer = demuxer;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}
	
	public String toString() {
		return demuxer + "," + videoFormat + "," + audioFormat + " " + width + "x" + height + " " + duration + " secs";
	}

}
