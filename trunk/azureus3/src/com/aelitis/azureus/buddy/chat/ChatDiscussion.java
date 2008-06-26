package com.aelitis.azureus.buddy.chat;

import java.util.ArrayList;
import java.util.List;

public class ChatDiscussion {
	
	List messages;
	int unreadMessages;
	DiscussionListener listener;
	
	public ChatDiscussion() {
		messages = new ArrayList();
	}
	
	public void addMessage(ChatMessage message) {
		synchronized(messages) {
			if(!messages.contains(message)) {
				unreadMessages++;
				messages.add(message);
				if(listener != null) {
					listener.newMessage(message);
				}
			}
		}
	}
	
	public List getAllMessages() {
		unreadMessages = 0;
		return messages;
	}
	
	public List getNewMessages() {
		int nbMessages = messages.size();
		List result = messages.subList(nbMessages - unreadMessages, nbMessages);
		unreadMessages = 0;
		return result;
	}
	
	public void clearAllMessages() {
		synchronized (messages) {
			messages.clear();
		}
	}
	
	public void clearNewMessages() {
		unreadMessages = 0;
	}

	public DiscussionListener getListener() {
		return listener;
	}

	public void setListener(DiscussionListener listener) {
		this.listener = listener;
	}

	public int getUnreadMessages() {
		return unreadMessages;
	}

}
