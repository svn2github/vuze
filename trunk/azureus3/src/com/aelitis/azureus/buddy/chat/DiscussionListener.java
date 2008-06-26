package com.aelitis.azureus.buddy.chat;

import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2.chatMessage;

public interface DiscussionListener {

	public void newMessage(ChatMessage message);
}
