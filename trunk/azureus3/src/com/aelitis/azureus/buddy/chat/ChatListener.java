package com.aelitis.azureus.buddy.chat;

import com.aelitis.azureus.buddy.VuzeBuddy;

public interface ChatListener {
	
	public void newMessage(VuzeBuddy from,ChatMessage message);

}
