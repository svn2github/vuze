package com.aelitis.azureus.buddy.chat;

import java.util.ArrayList;
import java.util.List;

import com.aelitis.azureus.buddy.VuzeBuddy;

public class ChatDiscussion implements ChatMessageListener {
	
	VuzeBuddy	buddy;
	List messages;
	int unreadMessages;
	DiscussionListener listener;
	
	public ChatDiscussion(
			VuzeBuddy	_buddy ) {
		buddy		= _buddy;
		messages 	= new ArrayList();
		
		List stored = buddy.getStoredChatMessages();
		
		for (int i=0;i<stored.size();i++){
			
			ChatMessage msg = (ChatMessage)stored.get(i);
										
			messages.add( msg );
				
			msg.addListener( this );
			
			unreadMessages++;
		}
	}
	
	public void addMessage(ChatMessage message) {
		synchronized(messages) {
			if(!messages.contains(message)) {
				unreadMessages++;
				messages.add(message);
				message.addListener( this );
				if(listener != null) {
					listener.newMessage(message);
				}
				
				if ( !message.getRendered()){
				
					buddy.storeChatMessage( message );
				}
			}
		}
	}
	
	public void
	rendered(
		ChatMessage		message )
	{
		buddy.deleteChatMessage( message );
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
	
	public int getNbMessages() {
		return messages.size();
	}

}
