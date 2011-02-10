package com.aelitis.azureus.ui.swt.feature;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureManagerListener;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence.LicenceInstallationListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.activities.VuzeActivitiesConstants;
import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.util.ConstantsVuze;

public class FeatureManagerUIListener
	implements FeatureManagerListener
{
	private final static boolean DEBUG = Constants.IS_CVS_VERSION;

	private static final String ID_ACTIVITY_EXPIRING = "ExpiringEntry";
	private static final String ID_ACTIVITY_EXPIRED = "ExpiredEntry";

	private final FeatureManager featman;

	private String pendingAuthForKey;
	
	private Map<String, Object[]> licence_map = new HashMap<String, Object[]>();

	public FeatureManagerUIListener(FeatureManager featman) {
		if (DEBUG) {
			System.out.println("FEAT:");
		}
		this.featman = featman;
	}

	public void licenceAdded(Licence licence) {
		updateUI();
		
		mapLicence( licence );
		
		if (DEBUG) {
			System.out.println("FEAT: Licence " + licence.getKey() + " Added with state " + licence.getState());
		}

		if (licence.getState() == Licence.LS_PENDING_AUTHENTICATION) {
			pendingAuthForKey = licence.getKey();
			FeatureManagerUI.openLicenceValidatingWindow();
		}

		if (licence.isFullyInstalled()) {
			return;
		}else{
			licence.retryInstallation();
		}
	}

	public void licenceChanged(Licence licence) {
		int state = licence.getState();

		boolean stateChanged = true;

		Licence lastLicence = mapLicence( licence );
			
		if (lastLicence != null) {
			stateChanged = lastLicence.getState() != licence.getState();
			
			if ( 	( !stateChanged ) && 
					licence.getState() == Licence.LS_AUTHENTICATED &&
					lastLicence.isFullyInstalled() != licence.isFullyInstalled()){
					
					stateChanged = true;
				}
		} else {
			// licenceChanged gets fired for all licences after listener is added
			// (via code in FeatureManagerUI)
			// skip case where licence is already cancelled
			if (state == Licence.LS_CANCELLED || state == Licence.LS_REVOKED
					|| state == Licence.LS_ACTIVATION_DENIED) {
				stateChanged = false;
			}
		}

		updateUI();
		if (DEBUG) {
			System.out.println("FEAT: License " + licence.getKey()
					+ " State Changed: " + state + "; changed? " + stateChanged);
		}
		
		if (!stateChanged) {
			return;
		}
	
		if (state == Licence.LS_PENDING_AUTHENTICATION) {
			pendingAuthForKey = licence.getKey();
			FeatureManagerUI.openLicenceValidatingWindow();
		} else {
			FeatureManagerUI.closeLicenceValidatingWindow();
			if (state == Licence.LS_AUTHENTICATED) {
				if (licence.getKey().equals(pendingAuthForKey)) {
					if (licence.isFullyInstalled()) {
						pendingAuthForKey = null;
						FeatureManagerUI.openLicenceSuccessWindow();
					} // else assumed install process is taking place
				}
			} else if (state == Licence.LS_INVALID_KEY) {
				FeatureManagerUI.openLicenceFailedWindow(state, licence.getKey());
				if (licence.getKey().equals(pendingAuthForKey)) {
					pendingAuthForKey = null;
				}
			} else if (state == Licence.LS_REVOKED) {
				FeatureManagerUI.openLicenceRevokedWindow(licence);
			} else if (state == Licence.LS_ACTIVATION_DENIED) {
				FeatureManagerUI.openLicenceActivationDeniedWindow(licence);
			}
		}
	}

	private Licence
	mapLicence(
		Licence		licence )
	{
		Licence existing_licence;
		
		LicenceInstallationListener	new_listener = null;
		
		synchronized ( licence_map ){
			
			String key = licence.getKey();
			
			Object[] entry = licence_map.get( key );
					
			if ( entry == null ){
				
				existing_licence = null;
				
				new_listener = 
					new LicenceInstallationListener()
					{
						FeatureManagerInstallWindow install_window = null;
	
						public void start(String licence_key) {
							if (DEBUG) {
								System.out.println("FEATINST: START! " + licence_key);
							}
							try {
								Licence licence = featman.addLicence(licence_key);
	
								install_window = new FeatureManagerInstallWindow(licence);
		
								install_window.open();
	
							} catch (PluginException e) {
								Debug.out(e);
							}
						}
	
						public void reportProgress(String licenceKey, String install, int percent) {
							if (DEBUG) {
								System.out.println("FEATINST: " + install + ": " + percent);
							}
						}
	
						public void reportActivity(String licenceKey, String install,
								String activity) {
							if (DEBUG) {
								System.out.println("FEAT: ACTIVITY: " + install + ": " + activity);
							}
						}
	
						public boolean alreadyFailing = false;
						public void failed(String licenceKey, PluginException error) {
							if (DEBUG) {
								System.out.println("FEAT: FAIL: " + licenceKey + ": " + error.toString());
							}
		
							if ( install_window != null ){
								
								install_window.close();
							}
							
							if ( licenceKey.equals(pendingAuthForKey)){
								
								pendingAuthForKey = null;
							}

							if (alreadyFailing) {
								return;
							}
							alreadyFailing = true;

							String s = Debug.getNestedExceptionMessage(error);
							
							MessageBoxShell mb = new MessageBoxShell(
									SWT.ICON_ERROR | SWT.OK,
									"License Addition Error for " + licenceKey,
									s );

							mb.open( new UserPrompterResultListener() {
								public void prompterClosed(int result) {
									alreadyFailing = false;
								}
							} );
						}
	
						public void complete(String licenceKey) {
		
							if ( licenceKey.equals(pendingAuthForKey)){
	
								pendingAuthForKey = null;
								
								FeatureManagerUI.openLicenceSuccessWindow();
							}
						}
					};
					
				
				licence_map.put( key, new Object[]{ licence, new_listener });
				
			}else{
				
				existing_licence = (Licence)entry[0];
				
				entry[0] = licence;
			}
		}

		if ( new_listener != null ){
			
			licence.addInstallationListener( new_listener );
		}	
		
		return( existing_licence );
	}
	
	/**
	 * 
	 */
	private void updateUI() {
		UIFunctionsManagerSWT.runWithUIFSWT(new UIFunctionsManagerSWT.UIFSWTRunnable() {
			public void run(final UIFunctionsSWT uif) {
				final boolean hasFullLicence = FeatureManagerUI.hasFullLicence();

				try {
					buildNotifications();
				} catch (Exception e) {
					Debug.out(e);
				}
				
				final SWTSkin skin = SWTSkinFactory.getInstance();
				if (skin != null) {
					SWTSkinObject soHeader = skin.getSkinObject("plus-header");
					if (soHeader != null) {
						soHeader.setVisible(hasFullLicence);
					}
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							uif.getMainShell().setText(hasFullLicence ? "Vuze Plus" : "Vuze");
						}
					});
				}

				MultipleDocumentInterfaceSWT mdi = uif.getMDISWT();
				if (mdi != null) {
					MdiEntrySWT entry = mdi.getEntrySWT(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
					if (entry != null) {
						entry.setTitleID(hasFullLicence ? "mdi.entry.plus.full"
								: "mdi.entry.plus.free");
						SBC_PlusFTUX view = (SBC_PlusFTUX) SkinViewManager.getByClass(SBC_PlusFTUX.class);
						if (view != null) {
							view.updateLicenceInfo();
						}
						SkinView[] views = SkinViewManager.getMultiByClass(SBC_BurnFTUX.class);
						if (views != null) {
							for (SkinView bview : views) {
								((SBC_BurnFTUX) bview).updateLicenceInfo();
							}
						}
					}
				}
			}
		});
	}

	public static void buildNotifications() {
		long plusExpiryTimeStamp = FeatureManagerUI.getPlusExpiryTimeStamp();
		long msLeft = FeatureManagerUI.getPlusExpiryTimeStamp()
				- SystemTime.getCurrentTime();
		long daysLeft = msLeft / 1000l / 60l / 60l / 24l;

		if (daysLeft > 30) {
			return;
		}

		String s;
		String id;
		String ref = "plus_note_" + (daysLeft > 0 ? "expiring_" : "expired_")
				+ Math.abs(daysLeft);
		String strA = "TARGET=\"" + MultipleDocumentInterface.SIDEBAR_SECTION_PLUS
				+ "\" HREF=\"#" + ref + "\"";

		if (daysLeft >= 0) {
			String msgID = "plus.notificaiton." + ID_ACTIVITY_EXPIRING
					+ (daysLeft == 1 ? ".s" : ".p");
			s = MessageText.getString(msgID, new String[] {
				"" + daysLeft,
				strA
			});
			id = ID_ACTIVITY_EXPIRING;
		} else {
			String msgID = "plus.notificaiton." + ID_ACTIVITY_EXPIRED
					+ (daysLeft == -1 ? ".s" : ".p");
			s = MessageText.getString(msgID, new String[] {
				"" + -daysLeft,
				strA
			});
			id = ID_ACTIVITY_EXPIRED;
		}
		VuzeActivitiesEntry entry = VuzeActivitiesManager.getEntryByID(id);
		if (entry == null) {
			boolean existed = VuzeActivitiesManager.isEntryIdRemoved(id);
			if (existed) {
				return;
			}

			entry = new VuzeActivitiesEntry(SystemTime.getCurrentTime(), s,
					VuzeActivitiesConstants.TYPEID_CONTENT_PROMO);
			entry.setID(id);

			if (daysLeft < 0) {
				UIFunctionsManager.getUIFunctions().getMDI().showEntryByID(
						MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
			}
		} else {
			entry.setText(s);
			entry.setTimestamp(SystemTime.getCurrentTime());
		}
		VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
			entry
		});
	}

	public void licenceRemoved(Licence licence) {
		Object[] entry;
		
		synchronized( licence_map ){
			
			entry = licence_map.remove(licence.getKey());
		}

		if ( entry != null ){
			
			licence.removeInstallationListener( (LicenceInstallationListener)entry[1] );
		}
		
		updateUI();
	}

}
