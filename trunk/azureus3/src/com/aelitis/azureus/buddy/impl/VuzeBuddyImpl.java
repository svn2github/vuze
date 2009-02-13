/**
 * Created on Apr 14, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.buddy.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryContentShare;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.chat.ChatMessage;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddyMessage;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.utils.ImageBytesDownloader;
import com.aelitis.azureus.util.*;

/**
 * BuddyPluginBuddy plus some vuze specific stuff
 * 
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddyImpl
	implements VuzeBuddy
{
	protected String displayName;

	private String loginID;

	private String code;

	private long lastUpdated;

	private long createdOn;

	private String avatarURL;

	private CopyOnWriteList<BuddyPluginBuddy> pluginBuddies = new CopyOnWriteList<BuddyPluginBuddy>();

	private AEMonitor mon_pluginBuddies = new AEMonitor("pluginBuddies");

	private ArrayList<VuzeBuddyListener> listeners = new ArrayList<VuzeBuddyListener>(0);

	protected VuzeBuddyImpl(String publicKey) {
		addPublicKey(publicKey);
	}

	protected VuzeBuddyImpl() {
	}

	public void loadFromMap(Map mapNewBuddy) {
		if (mapNewBuddy == null) {
			return;
		}
		setDisplayName(MapUtils.getMapString(mapNewBuddy, "display-name", ""
				+ mapNewBuddy.hashCode()));
		setLoginID(MapUtils.getMapString(mapNewBuddy, "login-id", ""
				+ mapNewBuddy.hashCode()));

		List<String> pksAdded = new ArrayList();
		List pkList = MapUtils.getMapList(mapNewBuddy, "pks",
				Collections.EMPTY_LIST);
		for (Iterator iter = pkList.iterator(); iter.hasNext();) {
			Object o = iter.next();
			String pk = null;
			if (o instanceof byte[]) {
				try {
					pk = new String((byte[]) o, "utf-8");
				} catch (UnsupportedEncodingException e) {
				}
			} else if (o instanceof String) {
				pk = (String) o;
			}
			if (pk != null) {
				addPublicKey(pk);
				pksAdded.add(pk);
			}
		}

		// Remove plugin buddies that have not been (re-)addded to the list
		for (BuddyPluginBuddy pluginBuddy : pluginBuddies.getList()) {
			String pk = pluginBuddy.getPublicKey();
			if (pk != null && !pksAdded.contains(pk)) {
				removePublicKey(pk);
			}
		}

		
		// first try to get the avatar via raw bytes
		byte[] newAvatar = MapUtils.getMapByteArray(mapNewBuddy, "avatar", null);
		if (newAvatar != null) {
			setAvatar(newAvatar);
		}

		// start of day, existing url none and we have one, just use it and assume corresponds
		
		String newAvatarURL = MapUtils.getMapString(mapNewBuddy, "avatar.url", null);

		if ( avatarURL == null && hasAvatar()){
			
			avatarURL = newAvatarURL;
		}else{
		
			if (!StringCompareUtils.equals(newAvatarURL, avatarURL) || !hasAvatar()) {
				avatarURL = newAvatarURL;
				if (avatarURL != null) {
					ImageBytesDownloader.loadImage(avatarURL,
							new ImageBytesDownloader.ImageDownloaderListener() {
								public void imageDownloaded(byte[] image) {
									VuzeBuddyManager.log("Got new avatar! " + toDebugString());
									setAvatar(image);
								}
							});
				}
			}
		}

		setCode(MapUtils.getMapString(mapNewBuddy, "code", null));
		setCreatedOn(MapUtils.getMapLong(mapNewBuddy, "created-on", 0));
	}

	public Map toMap() {
		Map map = new HashMap();
		map.put("display-name", displayName);
		map.put("login-id", loginID);
		map.put("code", code);
		map.put("created-on", new Long(createdOn));

		List pks = Arrays.asList(getPublicKeys());
		map.put("pks", pks);

		map.put("avatar.url", avatarURL);

		return map;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		if (displayName == null) {
			displayName = "";
		}
		if (displayName.equals(this.displayName)) {
			return;
		}
		this.displayName = displayName;
		VuzeBuddyManager.triggerOrderChangedListener();
	}

	public String getLoginID() {
		return loginID;
	}

	public void setLoginID(String loginID) {
		this.loginID = loginID;
	}

	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		boolean trigger = (this.lastUpdated > 0);
		this.lastUpdated = lastUpdated;
		if (trigger) {
			VuzeBuddyManager.triggerChangeListener(this);
		}
	}

	public byte[] getAvatar() {
		try {
			File file = getAvatarFile();
			if (file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				
				try{
					return FileUtil.readInputStreamAsByteArray(fis);
					
				}finally{
					
					fis.close();
				}
			}
		} catch (Throwable t) {
		}

		return null;
	}
	
	private File getAvatarFile() {
		// use hash of login id in case it has special chars
		return new File(SystemProperties.getUserPath(), "friends"
				+ File.separator + getLoginID().hashCode() + ".ico");
	}

	private boolean hasAvatar() {
		return getAvatarFile().exists();
	}

	public void setAvatar(byte[] avatar) {
		File file = getAvatarFile();
		FileUtil.writeBytesAsFile(file.getAbsolutePath(), avatar);

		VuzeBuddyManager.triggerChangeListener(this);
	}

	public boolean isOnline(boolean is_connected) {
		for (Iterator<BuddyPluginBuddy> iter = pluginBuddies.iterator(); iter.hasNext();) {
			BuddyPluginBuddy pluginBuddy = iter.next();
			if (pluginBuddy.isOnline(is_connected)) {

				if (pluginBuddy.getOnlineStatus() != BuddyPlugin.STATUS_APPEAR_OFFLINE) {

					return (true);
				}
			}
		}

		return false;
	}

	public int getVersion() {
		int version = VERSION_INITIAL;

		for (Iterator<BuddyPluginBuddy> iter = pluginBuddies.iterator(); iter.hasNext();) {
			BuddyPluginBuddy pluginBuddy = iter.next();

			version = Math.max(pluginBuddy.getVersion(), version);
		}

		return version;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#addPublicKey()
	public void addPublicKey(String pk) {
		// We add public keys by adding BuddyPluginBuddy

		BuddyPluginBuddy pluginBuddy = VuzeBuddyManager.getBuddyPluginBuddyForVuze(pk);

		mon_pluginBuddies.enter();
		try {

			if (pluginBuddy != null && !pluginBuddies.contains(pluginBuddy)){
				
					// check consistency of new buddy
				
				if ( pluginBuddies.size() > 0 ){
					
					BuddyPluginBuddy template_buddy = pluginBuddies.getList().get(0);
					
					Set<String> template_loc_cat = template_buddy.getLocalAuthorisedRSSCategories();
					
					pluginBuddy.setLocalAuthorisedRSSCategories( template_loc_cat );
				}
				
				pluginBuddies.add(pluginBuddy);
			}

		} finally {
			mon_pluginBuddies.exit();
		}

		VuzeBuddyManager.linkPKtoBuddy(pk, this);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#removePublicKey(java.lang.String)
	public void removePublicKey(String pk) {
		// our public key list is actually a BuddyPluginBuddy list, so find
		// it in our list and remove it
		mon_pluginBuddies.enter();
		try {
			for (Iterator<BuddyPluginBuddy> iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = iter.next();
				if (pluginBuddy.getPublicKey().equals(pk)) {
					iter.remove();
					if (pluginBuddy.getSubsystem() == BuddyPlugin.SUBSYSTEM_AZ3) {
						VuzeBuddyManager.log("Remove pk " + pk);
						pluginBuddy.remove();
					} else {
						VuzeBuddyManager.log("Can't remove pk as it's not az3: " + pk);
					}
				}
			}
		} finally {
			mon_pluginBuddies.exit();
		}
	}

	public String[] getPublicKeys() {
		mon_pluginBuddies.enter();
		try {
			String[] ret = new String[pluginBuddies.size()];
			int x = 0;

			for (Iterator<BuddyPluginBuddy> iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = iter.next();
				
				ret[x++] = pluginBuddy.getPublicKey();
			}
			return ret;
		} finally {
			mon_pluginBuddies.exit();
		}
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendActivity(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void sendActivity(VuzeActivitiesEntry entry)
			throws NotLoggedInException {
		BuddyPluginBuddy[] buddies = (BuddyPluginBuddy[]) pluginBuddies.toArray(new BuddyPluginBuddy[0]);
		VuzeBuddyManager.sendActivity(entry, buddies);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendPayloadMap(java.util.Map)
	public void sendPayloadMap(Map map)
			throws NotLoggedInException {
		BuddyPluginBuddy[] buddies = (BuddyPluginBuddy[]) pluginBuddies.toArray(new BuddyPluginBuddy[0]);
		VuzeBuddyManager.sendPayloadMap(map, buddies);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendPayloadMap(java.lang.String, java.util.Map)
	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendBuddyMessageMap(java.lang.String, java.util.Map)
	public void sendBuddyMessage(String namespace, Map map)
			throws NotLoggedInException {
		HashMap containerMap = new HashMap(3);
		containerMap.put("namespace", namespace);
		containerMap.put(VuzeBuddyManager.VUZE_MESSAGE_TYPE,
				VuzeBuddyManager.VMT_BUDDY_MESSAGE);
		containerMap.put("map", map);
		BuddyPluginBuddy[] buddies = (BuddyPluginBuddy[]) pluginBuddies.toArray(new BuddyPluginBuddy[0]);
		VuzeBuddyManager.sendPayloadMap(containerMap, buddies);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#shareDownload(com.aelitis.azureus.ui.swt.currentlyselectedcontent.CurrentContent, java.lang.String)
	public void shareDownload(SelectedContentV3 content, String message)
			throws NotLoggedInException {
		if (content == null) {
			return;
		}

		VuzeActivitiesEntryContentShare entry;
		entry = new VuzeActivitiesEntryContentShare(content, message);
		entry.setBuddyID(LoginInfoManager.getInstance().getUserInfo().userName);

		sendActivity(entry);
	}

	public void tellBuddyToSyncUp()
			throws NotLoggedInException {
		Map map = new HashMap();
		map.put(VuzeBuddyManager.VUZE_MESSAGE_TYPE, VuzeBuddyManager.VMT_BUDDYSYNC);

		sendPayloadMap(map);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getProfileAHREF(String referer) {
		return getProfileAHREF(referer, false);
	}

	public String getProfileAHREF(String referer, boolean useImage) {
		StringBuffer buf = new StringBuffer();

		buf.append("<A HREF=\"");
		buf.append(getProfileUrl(referer));
		buf.append("\" TITLE=\"");
		buf.append(displayName);
		if (!loginID.equals(displayName)) {
			buf.append(" (");
			buf.append(loginID);
			buf.append(")");
		}
		buf.append("\">");
		if (useImage) {
			buf.append("%0 ");
		}
		buf.append(displayName);
		buf.append("</A>");
		return buf.toString();
	}

	public String getProfileUrl(String referer) {
		return( ConstantsVuze.getDefaultContentNetwork().getProfileService(getLoginID(), referer));
	}

	// @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	public int compare(Object arg0, Object arg1) {
		if (!(arg0 instanceof VuzeBuddy) || !(arg1 instanceof VuzeBuddy)) {
			return 0;
		}

		String c0 = ((VuzeBuddy) arg0).getDisplayName();
		String c1 = ((VuzeBuddy) arg1).getDisplayName();

		if (c0 == null) {
			c0 = "";
		}
		if (c1 == null) {
			c1 = "";
		}
		return c0.compareToIgnoreCase(c1);
	}

	// @see java.lang.Comparable#compareTo(java.lang.Object)
	public int compareTo(Object arg0) {
		return compare(this, arg0);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#setCreatedOn(long)
	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#getCreatedOn()
	public long getCreatedOn() {
		return createdOn;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#toDebugString()
	public String toDebugString() {
		return "Buddy {" + loginID + "}";
	}

	public void addListener(VuzeBuddyListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeListener(VuzeBuddyListener l) {
		listeners.remove(l);
	}

	public VuzeBuddyListener[] getListeners() {
		return (VuzeBuddyListener[]) listeners.toArray(new VuzeBuddyListener[0]);
	}

	public int
	getStoredChatMessageCount()
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		int	res = 0;

		while (it.hasNext()) {

			BuddyPluginBuddy pluginBuddy = it.next();

			List<BuddyPluginBuddyMessage> msgs = pluginBuddy.retrieveMessages(BuddyPlugin.MT_V3_CHAT);

			res += msgs.size();
		}

		return( res );
	}
	
	public List<ChatMessage>
	getStoredChatMessages()
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		List<ChatMessage> result = new ArrayList<ChatMessage>();

		while (it.hasNext()) {

			BuddyPluginBuddy pluginBuddy = it.next();

			List<BuddyPluginBuddyMessage> msgs = pluginBuddy.retrieveMessages(BuddyPlugin.MT_V3_CHAT);

			for (int i = 0; i < msgs.size(); i++) {

				try {
					BuddyPluginBuddyMessage msg = (BuddyPluginBuddyMessage) msgs.get(i);

					ChatMessage cm = ChatMessage.deserialise(msg);

					if (cm != null) {

						result.add(cm);
					}
				} catch (Throwable e) {

				}
			}
		}

		// TODO: sort by timestamp

		return (result);
	}

	public void
	storeChatMessage(
		ChatMessage		msg )
	{
		String sender_pk = msg.getSenderPK();

		if (sender_pk != null) {

			BuddyPluginBuddy pluginBuddy = VuzeBuddyManager.getBuddyPluginBuddyForVuze(sender_pk);

			if (pluginBuddy == null) {

				VuzeBuddyManager.log( "Can't persist message for " + sender_pk + ", buddy not found" );

			} else {

				BuddyPluginBuddyMessage pm = pluginBuddy.storeMessage(
						BuddyPlugin.MT_V3_CHAT, msg.toMap());

				if (pm != null) {

					msg.setPersistentMessage(pm);
				}
			}
		}
	}

	public void
	deleteChatMessage(
		ChatMessage		msg )
	{
		BuddyPluginBuddyMessage pm = msg.getPersistentMessage();

		if (pm != null) {

			pm.delete();
		}
	}
	
	public Set<String>
	getSubscribableCategories()
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		Set<String> result = new HashSet<String>();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			Set<String> x = pluginBuddy.getRemoteAuthorisedRSSCategories();
			
			if ( x != null ){
				
				result.addAll(x);
			}
		}
		
		return( result );
	}
	
	public boolean
	isSubscribedToCategory(
		String		category )
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			if ( pluginBuddy.isRemoteRSSCategoryAuthorised( category )){
				
				Subscription	subs = lookupSubscription( pluginBuddy, category );
				
				if ( subs == null ){
					
					return( false );
				}
			}
		}
		
		return( true );
	}
	
	public void
	setSubscribedToCategory(
		String		category,
		boolean		subscribed )
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			Subscription	subs = lookupSubscription( pluginBuddy, category );

			if ( !subscribed ){
				
				if ( subs != null ){
				
					subs.remove();
				}
			}else{
				
				if ( subs == null ){
		
					if ( pluginBuddy.isRemoteRSSCategoryAuthorised( category )){
				
						try{
							pluginBuddy.subscribeToCategory( category );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			}
		}
	}
	
	protected Subscription
	lookupSubscription(
		BuddyPluginBuddy		buddy,
		String					cat )
	{
		Subscription[] subs = SubscriptionManagerFactory.getSingleton().getSubscriptions();
		
		for ( Subscription s: subs ){
			
			if ( buddy.isSubscribedToCategory( cat, s.getCreatorRef())){
				
				return( s );
			}
		}
		
		return( null );
	}
	
	public Set<String>
	getPublishedCategories()
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		Set<String> result = new HashSet<String>();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			Set<String> x = pluginBuddy.getLocalAuthorisedRSSCategories();
			
			if ( x != null ){
				
				result.addAll(x);
			}
		}
		
		return( result );
	}
	
	public boolean
	isPublishedCategory(
		String		category )
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			if ( !pluginBuddy.isLocalRSSCategoryAuthorised(category)){
				
				return( false );
			}
		}
		
		return( true );
	}
	
	public void
	setPublishedCategory(
		String		category,
		boolean		published )
	{
		Iterator<BuddyPluginBuddy> it = pluginBuddies.iterator();

		while (it.hasNext()){

			BuddyPluginBuddy pluginBuddy = it.next();

			if ( published ){
			
				pluginBuddy.addLocalAuthorisedRSSCategory( category );
				
			}else{
				
				pluginBuddy.removeLocalAuthorisedRSSCategory( category );				
			}
		}
	}
	
	public boolean
	canSetPublishedCategory(
		String		category )
	{
		return( VuzeBuddyManager.canSetPublishedCategory( category ));
	}
}
