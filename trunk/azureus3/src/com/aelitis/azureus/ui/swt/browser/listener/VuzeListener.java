package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.Map;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.util.MapUtils;


public class VuzeListener
	extends AbstractBrowserMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "vuze";

	public static final String OP_LOAD_VUZE_FILE = "load-vuze-file";

	public static final String OP_INSTALL_TRIAL = "install-trial";

	public static final String OP_GET_MODE = "get-mode";
	
	public static final String OP_GET_REMAINING = "get-plus-remaining";

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
		}else if (OP_INSTALL_TRIAL.equals(opid)) {
			FeatureManagerUI.createTrial();

		}else if (OP_GET_MODE.equals(opid)) {
			Map decodedMap = message.getDecodedMap();

			String callback = MapUtils.getMapString(decodedMap, "callback", null);
			
			if (callback != null) {
				
				context.executeInBrowser(callback + "('" + FeatureManagerUI.getMode() + "')");
				
			} else {
				
				message.debug("bad or no callback param");
			}
		}else if (OP_GET_REMAINING.equals(opid)) {
			Map decodedMap = message.getDecodedMap();

			String callback = MapUtils.getMapString(decodedMap, "callback", null);
			
			if (callback != null) {
				
				long plusRemainingInMS = FeatureManagerUI.getPlusExpiryTimeStamp() - SystemTime.getCurrentTime();
				context.executeInBrowser(callback + "(" + plusRemainingInMS + ")");
				
			} else {
				
				message.debug("bad or no callback param");
			}
		}else{
			
			throw new IllegalArgumentException("Unknown operation: " + opid);
		}
	}
}
