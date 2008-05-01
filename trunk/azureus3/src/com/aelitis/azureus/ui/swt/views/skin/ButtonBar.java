package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.InputShell;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

public class ButtonBar
	extends SkinView
{
	private SWTSkin skin;

	public static final int none_active_mode = 0;

	public static final int edit_mode = 1;

	public static final int share_mode = 2;

	public static final int invite_mode = 3;

	private SWTSkinButtonUtility editButton = null;

	private SWTSkinButtonUtility addBuddyButton = null;

	private SWTSkinButtonUtility shareButton = null;

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		/*
		 * Skip hooking the show/hide button unless a command line parameter is specified
		 * WARNING: TODO -- This is temporary and must be removed once the buddies features are complete
		 */
		if (true == System.getProperty("debug.buddies.bar", "0").equals("1")) {
			hookShowHideButon();
		}

		hookEditButton();
		hookShareButon();
		hookAddBuddyButon();
		hookTuxGoodies();
		return null;
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	private void hookTuxGoodies() {
		if (!Constants.isCVSVersion()) {
			return;
		}
		SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_ACTIVITY_TAB);
		if (skinObject != null) {
			Menu menu = new Menu(skinObject.getControl());
			MenuItem menuItem;
			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText("buddy sync up");
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					PlatformRelayMessenger.fetch(0);
					PlatformBuddyMessenger.sync(null);
					PlatformBuddyMessenger.getInvites();
				}
			});

			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText("send msg to all buddies");
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InputShell is = new InputShell("Moo", "Message:");
					String txt = is.open();
					if (txt != null) {
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
								SystemTime.getCurrentTime(), txt, "Test");
						List buddies = VuzeBuddyManager.getAllVuzeBuddies();
						for (Iterator iter = buddies.iterator(); iter.hasNext();) {
							VuzeBuddy buddy = (VuzeBuddy) iter.next();
							System.out.println("sending to " + buddy.getDisplayName());
							buddy.sendActivity(entry);
						}
					}
				}
			});

			skinObject.getControl().setMenu(menu);
		}

	}

	public void setActiveMode(int mode) {

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}

		if (mode == none_active_mode) {
			editButton.setDisabled(false);
			shareButton.setDisabled(false);
			addBuddyButton.setDisabled(false);
			viewer.setEditMode(false);
			viewer.setShareMode(false);
			viewer.setAddBuddyMode(false);
			return;

		}
		editButton.setDisabled(true);
		shareButton.setDisabled(true);
		addBuddyButton.setDisabled(true);
		viewer.setEditMode(false);
		viewer.setShareMode(false);
		viewer.setAddBuddyMode(false);

		if (mode == edit_mode) {
			viewer.setEditMode(true);
			shareButton.setDisabled(false);
			addBuddyButton.setDisabled(false);
		} else if (mode == share_mode) {
			viewer.setShareMode(true);
			editButton.setDisabled(false);
			addBuddyButton.setDisabled(false);
		} else if (mode == invite_mode) {
			viewer.setAddBuddyMode(true);
			shareButton.setDisabled(false);
			editButton.setDisabled(false);
		}
	}

	private void hookShowHideButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("text-buddy-show-hide");
		final SWTSkinObject showImageObject = skin.getSkinObject("button-show-footer");
		final SWTSkinObject hideImageObject = skin.getSkinObject("button-hide-footer");
		final SWTSkinObject buttonBarObject = skin.getSkinObject("global-button-bar");

		boolean footerVisible = COConfigurationManager.getBooleanParameter("Footer.visible");
		if (footerVisible) {
			showImageObject.setVisible(false);
			hideImageObject.setVisible(true);
		} else {
			showImageObject.setVisible(true);
			hideImageObject.setVisible(false);
		}

		if (null != showHideBuddiesObject) {
			final SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(
					showHideBuddiesObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {
					/*
					 * Sets the text according to the visibility of the footer when ever the footer is shown or hidden
					 */
					SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
					if (skinObject != null) {
						skinObject.addListener(new SWTSkinObjectListener() {

							public Object eventOccured(SWTSkinObject skinObject,
									final int eventType, Object params) {

								if (eventType == SWTSkinObjectListener.EVENT_HIDE) {
									btnGo.setTextID("Button.bar.show");
									showImageObject.setVisible(true);
									hideImageObject.setVisible(false);

								} else if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
									btnGo.setTextID("Button.bar.hide");
									showImageObject.setVisible(false);
									hideImageObject.setVisible(true);
								}
								return null;
							}
						});

						SWTSkinUtils.setVisibility(skin, "Footer.visible",
								SkinConstants.VIEWID_FOOTER, !skinObject.isVisible());

						Utils.relayout(buttonBarObject.getControl());
					}
				}

			});

			/*
			 * Sets the text according to the visibility of the footer on initialization
			 */
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
			if (skinObject != null) {
				if (true == skinObject.isVisible()) {
					btnGo.setTextID("Button.bar.hide");
				} else {
					btnGo.setTextID("Button.bar.show");
				}
			}
		}
	}

	private void hookEditButton() {
		final SWTSkinObject editBuddies = skin.getSkinObject("button-buddy-edit");
		final SWTSkinObject cancelEditBuddies = skin.getSkinObject("button-buddy-edit-cancel");

		if (null == editBuddies || null == cancelEditBuddies) {
			Debug.out("Edit button is not found... skin may not be initialized properly");
			return;
		}
		if (null != editBuddies) {
			editButton = new SWTSkinButtonUtility(editBuddies);
			editButton.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					viewer.setEditMode(!viewer.isEditMode());
					if (true == viewer.isEditMode()) {
						shareButton.setDisabled(true);
						addBuddyButton.setDisabled(true);
						cancelEditBuddies.setVisible(true);
						editBuddies.setVisible(false);
					} else {
						shareButton.setDisabled(false);
						addBuddyButton.setDisabled(false);
						cancelEditBuddies.setVisible(false);
					}

				}
			});
		}

		if (null != cancelEditBuddies) {
			SWTSkinButtonUtility cancelEditButton = new SWTSkinButtonUtility(
					cancelEditBuddies);
			cancelEditButton.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					viewer.setEditMode(false);
					shareButton.setDisabled(false);
					addBuddyButton.setDisabled(false);
					cancelEditBuddies.setVisible(false);
					editBuddies.setVisible(true);
				}
			});
		}

	}

	private void hookShareButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-share");
		if (null != showHideBuddiesObject) {
			shareButton = new SWTSkinButtonUtility(showHideBuddiesObject);
			shareButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					viewer.setShareMode(!viewer.isShareMode());

					SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
					if (detailPanelView instanceof DetailPanel) {

						DetailPanel detailPanel = ((DetailPanel) detailPanelView);
						detailPanel.show(viewer.isShareMode(), SharePage.PAGE_ID);

						/*
						 * Calling the browser to set the inviteFromShare flag to false
						 */
						if (true == viewer.isShareMode()) {
							SharePage sharePage = (SharePage) detailPanel.getPage(SharePage.PAGE_ID);
							if (null != sharePage.getMessageContext()) {
								sharePage.getMessageContext().executeInBrowser(
										"inviteFromShare(" + true + ")");
							}

							sharePage.setBuddies(viewer.getSelection());

							editButton.setDisabled(true);
							addBuddyButton.setDisabled(true);
						} else {
							editButton.setDisabled(false);
							addBuddyButton.setDisabled(false);
						}
					}
				}
			});
		}
	}

	private void hookAddBuddyButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-add");
		if (null != showHideBuddiesObject) {
			addBuddyButton = new SWTSkinButtonUtility(showHideBuddiesObject);
			addBuddyButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					viewer.setAddBuddyMode(!viewer.isAddBuddyMode());

					SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
					if (detailPanelView instanceof DetailPanel) {
						DetailPanel detailPanel = ((DetailPanel) detailPanelView);
						detailPanel.show(viewer.isAddBuddyMode(), InvitePage.PAGE_ID);
						/*
						 * Calling the browser to set the inviteFromShare flag to false
						 */
						if (true == viewer.isAddBuddyMode()) {
							IDetailPage invitePage = detailPanel.getPage(InvitePage.PAGE_ID);
							if (null != invitePage.getMessageContext()) {
								invitePage.getMessageContext().executeInBrowser(
										"inviteFromShare(" + false + ")");
							}
							editButton.setDisabled(true);
							shareButton.setDisabled(true);
						} else {
							editButton.setDisabled(false);
							shareButton.setDisabled(false);
						}
					}
				}
			});
		}
	}

	public SWTSkinButtonUtility getEditButton() {
		return editButton;
	}

	public SWTSkinButtonUtility getAddBuddyButton() {
		return addBuddyButton;
	}

	public SWTSkinButtonUtility getShareButton() {
		return shareButton;
	}
}
