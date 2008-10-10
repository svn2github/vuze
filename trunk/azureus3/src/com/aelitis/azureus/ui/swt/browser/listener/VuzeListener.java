package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.Map;

import org.gudy.azureus2.core3.util.Base32;


import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.util.MapUtils;


public class VuzeListener
	extends AbstractBrowserMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "vuze";

	public static final String OP_LOAD_VUZE_FILE = "load-vuze-file";
	
	public 
	VuzeListener() 
	{
		super( DEFAULT_LISTENER_ID );
	}

	public void 
	handleMessage(
		BrowserMessage message )
	{
		String opid = message.getOperationId();
		
		if ( OP_LOAD_VUZE_FILE.equals(opid)){
			
			Map decodedMap = message.getDecodedMap();
			
			String content = MapUtils.getMapString(decodedMap, "content", null);
	
			if ( content == null ){
			
				throw new IllegalArgumentException( "content missing" );
				
			}else{
				
				byte[] bytes = Base32.decode( content );
				
				VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
				
				VuzeFile vf = vfh.loadVuzeFile( bytes );
				
				if ( vf == null ){
					
					throw new IllegalArgumentException( "content invalid" );
					
				}else{
					
					vfh.handleFiles( new VuzeFile[]{ vf }, 0 );
				}
			}
			
		}else{
			
			throw new IllegalArgumentException("Unknown operation: " + opid);
		}
	}
}
