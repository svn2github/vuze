package com.aelitis.azureus.core.drm.msdrm;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.login.NotLoggedInException;

public class LicenseAquirer {
	
	//private static final String PROGID = "DRM.GetLicense";
	
	private static final String STATUS_REQUESTED = "requested";
	private static final String STATUS_INSTALLED = "installed";
	private static final String STATUS_SUCCESS = "success";
	private static final String STATUS_UPDATE = "update";
	private static final String STATUS_FAILURE = "failure";
	private static final String STATUS_CLIENT_FAILURE = "client-failure";

	private static final String ERROR_INCOMPATIBLE_CLIENT = "Incompatible Client";
	private static final String ERROR_SERVER_COMMUNICATION= "Server did not send a valid response";
	private static final String ERROR_EXCEEDED_DRM_DELIVERY_LIMIT = "Key delivery limit exceeded.";
	
	private Browser browser;
	
	public LicenseAquirer(Composite composite) {
		browser = new Browser(composite,SWT.NONE);
		browser.setText("<html><body><object id=\"netobj\" classid=\"clsid:A9FC132B-096D-460B-B7D5-1DB0FAE0C062\" width=\"0\" height=\"0\"></object></body></html>");
		browser.setVisible(false);
	}
	
	private String getSystemInfo() throws LicenseAquisitionException {
		final AESemaphore sem = new AESemaphore("waitForBrowserResponse");
		final String[] result  = {null};
		
		browser.getDisplay().asyncExec(new Runnable() {
			public void run() {
				browser.addStatusTextListener(new StatusTextListener() {
					public void changed(StatusTextEvent event) {
						if( ! ("".equals(event.text) ) ) {
							browser.removeStatusTextListener(this);
							result[0] = event.text;
							sem.release();
						}
					}
				});
				
				browser.execute("try {window.status = netobj.GetSystemInfo(); } catch(e) { window.status= 'ERROR : ' + e ; }");
			}
		});
		
		sem.reserve(10000);
		
		if(result[0] == null) {
			throw new LicenseAquisitionException("JS Call did not complete");
		}
		
		if(result[0].startsWith("ERROR : ")) {
			throw new LicenseAquisitionException("JS Call returned : " + result[0]);
		}

		return result[0];
		
	}
	
	private String getDRMVersion() throws LicenseAquisitionException {
		
		final AESemaphore sem = new AESemaphore("waitForBrowserResponse");
		final String[] result  = {null};
		
		browser.getDisplay().asyncExec(new Runnable() {
			public void run() {
				browser.addStatusTextListener(new StatusTextListener() {
					public void changed(StatusTextEvent event) {
						browser.removeStatusTextListener(this);
						result[0] = event.text;
						sem.release();
					}
				});
				
				browser.execute("try { window.status = netobj.GetDRMVersion(); } catch(e) { window.status= 'ERROR : ' + e ; }");
			}
		});
		
		sem.reserve(10000);
		
		if(result[0] == null) {
			throw new LicenseAquisitionException("JS Call did not complete");
		}
		
		if(result[0].startsWith("ERROR : ")) {
			throw new LicenseAquisitionException("JS Call returned : " + result[0]);
		}
		
		return result[0];
		
	}
	
	private void storeLicense(final String license) throws LicenseAquisitionException {
		browser.getDisplay().asyncExec(new Runnable() {
			public void run() {
					browser.execute("netobj.StoreLicense(" + license + ");");	
			}
		});
	}
	
	private void makeRPCRequest(String rpcCommand, JSONObject rpcParameters, PlatformMessengerListener listener) throws LicenseAquisitionException {
		PlatformMessage message = new PlatformMessage("drm", "drm", rpcCommand,rpcParameters,60*1000);
		PlatformMessenger.queueMessage(message, listener);
	}
	
	private boolean checkDRMVersion(String systemInfo,String drmVersion) throws LicenseAquisitionException,DRMUpdateRequiredException {
		JSONObject parameters = new JSONObject();
		parameters.put("drmVersion", drmVersion);
		parameters.put("clientInfo", systemInfo);
		
		final AESemaphore sem = new AESemaphore( "waitForPlatformResponse" );
		
		final Boolean[] result = {null};
		final Exception[] error = {null};
		
		makeRPCRequest("check", parameters,new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				
			}
			
			public void replyReceived(PlatformMessage message,
					String replyType, Map reply) {
				try {
					if("response".equals(replyType)) {
	
						if(reply.get("action") != null) {
							Map action = (Map) reply.get("action");
							if(action.get("name") != null) {
								String name = (String) action.get("name");
								if("update".equals(name)) {
									error[0] = new DRMUpdateRequiredException((String) action.get("url"));
								}
								if("success".equals(name)) {
									result[0] = new Boolean(true);
								}
							}
							
							error[0] = new LicenseAquisitionException("no action, or invalid action in response");
							return;
						}
						
					}
					if("exception".equals(replyType)) {
						error[0] = new LicenseAquisitionException("Platform Call returned : " + reply.toString() ); 
					}
				} finally {
					sem.release();
				}
				
				
			}
		});
		
		sem.reserve();
		
		if(result[0] == null && error[0] == null) {
			throw new LicenseAquisitionException("Platform Call did not complete");
		}
		
		if(error[0] != null) {
			if(error[0] instanceof LicenseAquisitionException) {
				throw (LicenseAquisitionException) error[0];
			}
			
			if(error[0] instanceof DRMUpdateRequiredException) {
				throw (DRMUpdateRequiredException) error[0];
			}
			
			throw new LicenseAquisitionException(error[0].getMessage());
		}
		
		Boolean res = result[0];
		
		if(res != null) {
			return res.booleanValue();
		}
		
		return false;

	}
	
	private String getLicenseFromPlatform(String systemInfo,String contentHash) throws LicenseAquisitionException,CheckVersionRequiredException,ExceededDRMDeliveryLimitException {
		JSONObject parameters = new JSONObject();
		parameters.put("hash", contentHash);
		parameters.put("clientInfo", systemInfo);
		
		final AESemaphore sem = new AESemaphore( "waitForPlatformResponse" );
		
		final String[] result = {null};
		final Exception[] error = {null};
		
		makeRPCRequest("deliver", parameters,new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				System.out.println("sent");
				
			}
			
			public void replyReceived(PlatformMessage message,
					String replyType, Map reply) {
				try {
					if("response".equals(replyType)) {
						
						
						if(reply.get("license") != null) {
							result[0] = (String) reply.get("license");
							return;
						}
						
						if(reply.get("rejected") != null && ERROR_EXCEEDED_DRM_DELIVERY_LIMIT.equals(reply.get("rejected"))) {
							error[0] = new ExceededDRMDeliveryLimitException(ERROR_EXCEEDED_DRM_DELIVERY_LIMIT);
							return;
						}
						
						if(reply.get("action") != null) {
							Map action = (Map) reply.get("action");
							if(action.get("name") != null) {
								String name = (String) action.get("name");
								if("check".equals(name)) {
									error[0] = new CheckVersionRequiredException(ERROR_EXCEEDED_DRM_DELIVERY_LIMIT);
								}
							}
							
							error[0] = new LicenseAquisitionException("no action, or invalid action in response");
							return;
						}
						
					}
					if("exception".equals(replyType)) {
						error[0] = new LicenseAquisitionException("Platform Call returned : " + reply.toString() ); 
					}
				} finally {
					sem.release();
				}
				
				
			}
		});
		
		sem.reserve();
		
		if(result[0] == null && error[0] == null) {
			throw new LicenseAquisitionException("Platform Call did not complete");
		}
		
		if(error[0] != null) {
			if(error[0] instanceof LicenseAquisitionException) {
				throw (LicenseAquisitionException) error[0];
			}
			
			if(error[0] instanceof CheckVersionRequiredException) {
				throw (CheckVersionRequiredException) error[0];
			}
			
			if(error[0] instanceof ExceededDRMDeliveryLimitException) {
				throw (ExceededDRMDeliveryLimitException) error[0];
			}

			throw new LicenseAquisitionException(error[0].getMessage());
		}
		
		String res = result[0];
		
		return res;
		
	}
	
	public void aquireLicenseFor(String contentHash) throws LicenseAquisitionException,ExceededDRMDeliveryLimitException,DRMUpdateRequiredException {
		String systemInfo = getSystemInfo();
		String license = null;
		try {
			license = getLicenseFromPlatform(systemInfo, contentHash);
		} catch (CheckVersionRequiredException e) {
			checkDRMSystem();
		}
		if(license != null) {
			storeLicense(license);
		} else {
			throw new LicenseAquisitionException("failed to aquire a license");
		}
		System.out.println("license aquired");
	}
	
	public boolean checkDRMSystem() throws LicenseAquisitionException,DRMUpdateRequiredException {
		String systemInfo = getSystemInfo();
		String drmVersion = getDRMVersion();
		
		return checkDRMVersion(systemInfo, drmVersion);
		
	}
	
	public static void main(String args[]) throws Exception {
		Display display = new Display();
		final Shell shell = new Shell(display);
		//shell.setLayout(new FillLayout());
		shell.open();
		final LicenseAquirer la = new LicenseAquirer(shell);
		Thread t = new Thread() {
			public void run() {
				try {
					la.aquireLicenseFor("SNWEAY7K6RJPAJF2HD52BEX27ERKJXAO");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
		
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
		
		display.dispose();
	}

}
