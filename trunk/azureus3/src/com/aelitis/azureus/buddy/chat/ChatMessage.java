package com.aelitis.azureus.buddy.chat;

public class ChatMessage {
	
	long originalTimeStamp;
	long timestamp;
	boolean isMe;
	String sender;
	String message;
	
	public ChatMessage(boolean isMe,long originalTimeStamp,long timestamp, String sender, String message) {
		super();
		this.isMe = isMe;
		this.originalTimeStamp = originalTimeStamp;
		this.timestamp = timestamp;
		this.sender = sender;
		this.message = message;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isMe() {
		return isMe;
	}

	public void setMe(boolean isMe) {
		this.isMe = isMe;
	}
	
	public boolean equals(Object o) {
		if(o instanceof ChatMessage) {
			ChatMessage other = (ChatMessage) o;
			return other.originalTimeStamp == this.originalTimeStamp;
		}
		return false;
	}
	
	
	
	

}
