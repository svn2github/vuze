package com.aelitis.azureus.buddy.chat;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddyMessage;

public class ChatMessage {
	
	public static ChatMessage
	deserialise(
		BuddyPluginBuddyMessage		pm )
	{
		try{
			Map	m = pm.getRequest();
			
			long	original_ts		= ((Long)m.get( "ot" )).longValue();
			long	ts				= ((Long)m.get( "t" )).longValue();
			String	sender			= new String((byte[])m.get( "se" ), "UTF-8" );
			
			byte[]	sender_pk_b		= (byte[])m.get( "sk" );
			
			String	sender_pk;
			
			if ( sender_pk_b == null ){
			
				sender_pk 	= null;
				
			}else{
				
				sender_pk	= new String( sender_pk_b, "UTF-8" );
			}
			
			String	message			= new String((byte[])m.get( "msg" ), "UTF-8" );
			
			ChatMessage res = new ChatMessage( sender_pk, original_ts, ts, sender, message );
			
			res.setPersistentMessage( pm );
			
			return( res );
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to decode chat message '" + pm + "'", e );
			
			return( null );
		}
	}
	
	private BuddyPluginBuddyMessage		persistent_msg;
	
	final long originalTimeStamp;
	final long timestamp;
	final String senderPK;
	final String sender;
	final String message;
	
	private boolean	rendered;
	private List	listeners = new ArrayList();
	
	public ChatMessage(String senderPK,long originalTimeStamp,long timestamp, String sender, String message) {
		super();
		this.senderPK = senderPK;
		this.originalTimeStamp = originalTimeStamp;
		this.timestamp = timestamp;
		this.sender = sender;
		this.message = message;
	}
	
	public void
	setPersistentMessage(
		BuddyPluginBuddyMessage		_pm )
	{
		persistent_msg	= _pm;
	}
	
	public BuddyPluginBuddyMessage
	getPersistentMessage()
	{
		return( persistent_msg );
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public long getOriginatorTimestamp()
	{
		return originalTimeStamp;
	}
	
	public String getSender() {
		return sender;
	}

	public String getMessage() {
		return message;
	}

	public boolean isMe() {
		return senderPK == null ||  senderPK.length()==0;
	}
	
	public String
	getSenderPK()
	{
		return( senderPK );
	}
	
	public void
	setRendered()
	{
		rendered	= true;
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ChatMessageListener)listeners.get(i)).rendered( this );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public boolean
	getRendered()
	{
		return( rendered );
	}
	
	public void
	addListener(
		ChatMessageListener		l )
	{
		listeners.add( l );
	}
	
	public Map
	toMap()
	{
		Map	m = new HashMap();
		
		m.put( "ot", new Long( originalTimeStamp ));
		m.put( "t", new Long( timestamp ));
		
		try{
			m.put( "se", sender.getBytes( "UTF-8" ));
			
			if ( senderPK != null ){
			
				m.put( "sk", senderPK.getBytes( "UTF-8" ));
			}
			
			m.put( "msg", message.getBytes( "UTF-8" ));
			
		}catch( UnsupportedEncodingException e ){
			
			Debug.out(e);
		}
		
		return( m );
		
	}
	
	public boolean equals(Object o) {
		if(o instanceof ChatMessage) {
			ChatMessage other = (ChatMessage) o;
			return other.originalTimeStamp == this.originalTimeStamp;
		}
		return false;
	}
}
