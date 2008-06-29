package com.aelitis.azureus.buddy.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.buddy.impl.VuzeBuddyMessageListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerFactory;
import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyListener;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.LoginInfoManager;

public class Chat implements VuzeBuddyMessageListener {
	
	Map discussions;
	List listeners;
	LoginInfoManager loginInfoManager;
	
	public Chat() {
		discussions = new HashMap();
		listeners = new ArrayList();
		loginInfoManager = LoginInfoManager.getInstance();
		VuzeBuddyManager.addMessageListener(this);
		
		CryptoManagerFactory.getSingleton().addKeyListener(
			new CryptoManagerKeyListener()
			{
				public void
				keyChanged(
					CryptoHandler		handler )
				{
				}
				
				public void
				keyLockStatusChanged(
					CryptoHandler		handler )
				{
					if ( handler.isUnlocked()){
						
						new AEThread2( "Chat:check", true )
						{
							public void
							run()
							{
								checkPersistentMessages();
							}
						}.start();
					}
				}
			});
	}
	
	protected void
	checkPersistentMessages()
	{
		List to_check;	 
	
		synchronized (discussions) {
			
			to_check = new ArrayList( discussions.values());
		}
		
		for (int i=0;i<to_check.size();i++){
			
			ChatDiscussion d = (ChatDiscussion)to_check.get(i);
			
			if ( d.checkPersistentMessages()){
			
				notifyListenersOfChat( d.getBuddy());
			}
		}
	}
	
	public void
	checkBuddy(
		VuzeBuddy		buddy )
	{
		boolean	new_chat = false;
		
		ChatDiscussion result;
		
		boolean	check_persistent = CryptoManagerFactory.getSingleton().getECCHandler().isUnlocked();
		
		synchronized (discussions) {
			result = (ChatDiscussion) discussions.get(buddy);
			if(result == null) {
				result = new ChatDiscussion( buddy, check_persistent );
				discussions.put(buddy,result);
				new_chat = true;
			}
		}
		
		if ( new_chat ){
			
			notifyListenersOfChat( buddy);
		}
	}
	
	public void messageRecieved(VuzeBuddy buddy, String senderPK, String namespace, long sentAt, Map message) {
		if(namespace.equals("chat")) {
			messageReceived(buddy, senderPK, sentAt, message);
		}
	}
	
	public ChatDiscussion getChatDiscussionFor(VuzeBuddy buddy) {
		boolean	check_persistent = CryptoManagerFactory.getSingleton().getECCHandler().isUnlocked();

		synchronized (discussions) {
			ChatDiscussion result = (ChatDiscussion) discussions.get(buddy);
			if(result == null) {
				result = new ChatDiscussion( buddy, check_persistent );
				discussions.put(buddy,result);
			}
			
			return result;
		}
	}
	
	public void messageReceived(VuzeBuddy from,String fromPK, long sentAt, Map message) {
		String text = new String((byte[])message.get("text"));
		long originalTimeStamp = sentAt; // ((Long)message.get("timestamp")).longValue();
		// System.out.println( "chat msg: recv=" + sentAt + ",sent=" + ((Long)message.get("timestamp")).longValue());
		if(text != null) {
			ChatDiscussion discussion = getChatDiscussionFor(from);
			ChatMessage localMessage = new ChatMessage(fromPK,originalTimeStamp,SystemTime.getCurrentTime(),from.getDisplayName(),text);
			discussion.addMessage(localMessage);
			notifyListenersOfNewMessage(from,localMessage);
		}
	}
	
	public void sendMessage(final VuzeBuddy to,final String text) {
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				internalSendMessage(to, text);
			}
		});
	}
	
	private void internalSendMessage(final VuzeBuddy to,final String text) {
		AEThread2 sender = new AEThread2("message sender",true) {
			public void run() {
				try {
					Map message = new HashMap();
					long timeStamp = SystemTime.getCurrentTime();
					
						// try and use a reasonable originating timestamp
					
					timeStamp -= AzureusCoreFactory.getSingleton().getInstanceManager().getClockSkew();
					
					message.put("timestamp", new Long(timeStamp));
					message.put("text", text);
					to.sendBuddyMessage("chat", message);
					ChatDiscussion discussion = getChatDiscussionFor(to);
					
					ChatMessage localMessage = new ChatMessage(null,timeStamp,timeStamp,loginInfoManager.getUserInfo().displayName,text);
					discussion.addMessage(localMessage);
					notifyListenersOfNewMessage(to,localMessage);
				} catch(NotLoggedInException e) {
					//User is not logged in...
				}
			}
		};
		sender.start();
	}
	
	public void notifyListenersOfNewMessage(VuzeBuddy from,ChatMessage message) {
		synchronized (listeners) {
			for(int i = 0 ; i < listeners.size() ; i++) {
				ChatListener listener = (ChatListener) listeners.get(i);
				listener.newMessage(from,message);
			}
		}
	}
	
	public void notifyListenersOfChat(VuzeBuddy buddy ) {
		synchronized (listeners) {
			for(int i = 0 ; i < listeners.size() ; i++) {
				ChatListener listener = (ChatListener) listeners.get(i);
				listener.updatedChat(buddy);
			}
		}
	}
	public void addChatListener(ChatListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	public void removeChatListener(ChatListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	

}
