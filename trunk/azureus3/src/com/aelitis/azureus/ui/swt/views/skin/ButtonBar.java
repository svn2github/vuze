package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.LoginInfoManager;

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
		hookAddBuddyButon();
		hookTuxGoodies();

		//		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
		//			public void currentlySectedContentChanged(SelectedContent[] currentContent) {
		//				if (shareButton != null) {
		//					boolean disable = currentContent.length == 0;
		//					if (shareButton.isDisabled() != disable) {
		//						shareButton.setDisabled(disable);
		//					}
		//				}
		//			}
		//		});
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
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					try {
						PlatformRelayMessenger.fetch(0);
						PlatformBuddyMessenger.sync(null);
						PlatformBuddyMessenger.getInvites();
					} catch (NotLoggedInException e1) {
					}
				}
			});

			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText("send msg to all buddies");
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					InputShell is = new InputShell("Moo", "Message:");
					String txt = is.open();
					if (txt != null) {
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
								SystemTime.getCurrentTime(), txt, "Test");
						List buddies = VuzeBuddyManager.getAllVuzeBuddies();
						for (Iterator iter = buddies.iterator(); iter.hasNext();) {
							VuzeBuddy buddy = (VuzeBuddy) iter.next();
							System.out.println("sending to " + buddy.getDisplayName());
							try {
								buddy.sendActivity(entry);
							} catch (NotLoggedInException e1) {
								Debug.out("Shouldn't Happen", e1);
							}
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
			addBuddyButton.setDisabled(false);
			viewer.setEditMode(false);
			viewer.setShareMode(false);
			viewer.setAddBuddyMode(false);
			return;

		}
		editButton.setDisabled(true);
		addBuddyButton.setDisabled(true);
		viewer.setEditMode(false);
		viewer.setShareMode(false);
		viewer.setAddBuddyMode(false);

		if (mode == edit_mode) {
			viewer.setEditMode(true);
			addBuddyButton.setDisabled(false);
		} else if (mode == share_mode) {
			viewer.setShareMode(true);
			editButton.setDisabled(false);
			addBuddyButton.setDisabled(false);
		} else if (mode == invite_mode) {
			viewer.setAddBuddyMode(true);
			editButton.setDisabled(false);
		}
	}

	private void hookShowHideButon() {

		final SWTSkinObject showImageObject = skin.getSkinObject("button-show-footer");
		final SWTSkinObject hideImageObject = skin.getSkinObject("button-hide-footer");
		final SWTSkinObject buttonBarObject = skin.getSkinObject("global-button-bar");
		boolean footerVisible = COConfigurationManager.getBooleanParameter("Footer.visible");

		if (null != showImageObject && null != hideImageObject) {

			/*
			 * Initial visibility state of the footer
			 */
			if (true == footerVisible) {
				showImageObject.setVisible(false);
				hideImageObject.setVisible(true);
			} else {
				showImageObject.setVisible(true);
				hideImageObject.setVisible(false);
			}

			/*
			 * Hook 'show' button; when pressed hide it and show the 'hide' button
			 */
			final SWTSkinButtonUtility btnShow = new SWTSkinButtonUtility(
					showImageObject);
			btnShow.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					hideImageObject.setVisible(true);
					showImageObject.setVisible(false);
					SWTSkinUtils.setVisibility(skin, "Footer.visible",
							SkinConstants.VIEWID_FOOTER, true);
					Utils.relayout(buttonBarObject.getControl());
				}
			});

			/*
			 * Hook 'hide' button; when pressed hide it and show the 'show' button
			 */
			final SWTSkinButtonUtility btnHide = new SWTSkinButtonUtility(
					hideImageObject);
			btnHide.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					showImageObject.setVisible(true);
					hideImageObject.setVisible(false);
					SWTSkinUtils.setVisibility(skin, "Footer.visible",
							SkinConstants.VIEWID_FOOTER, false);
					Utils.relayout(buttonBarObject.getControl());
				}
			});

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
						addBuddyButton.setDisabled(true);
						cancelEditBuddies.setVisible(true);
						editBuddies.setVisible(false);
					} else {
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
					addBuddyButton.setDisabled(false);
					cancelEditBuddies.setVisible(false);
					editBuddies.setVisible(true);
				}
			});
		}

	}

	//	private void hookShareButon() {
	//
	//		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-share");
	//		if (null != showHideBuddiesObject) {
	//			shareButton = new SWTSkinButtonUtility(showHideBuddiesObject);
	//			shareButton.addSelectionListener(new ButtonListenerAdapter() {
	//				public void pressed(final SWTSkinButtonUtility buttonUtility) {
	//
	//					SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
	//						public void loginComplete() {
	//							share();
	//						}
	//					});
	//
	//				}
	//			});
	//		}
	//	}

	private void share() {
		SelectedContent[] selectedContent = SelectedContentManager.getCurrentlySelectedContent();
		if (selectedContent.length == 0) {
			return;
		}

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
				sharePage.setShareItem(selectedContent[0]);

				editButton.setDisabled(true);
				addBuddyButton.setDisabled(true);
			} else {
				editButton.setDisabled(false);
				addBuddyButton.setDisabled(false);
			}
		}
	}

	private void hookAddBuddyButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-add");
		if (null != showHideBuddiesObject) {
			addBuddyButton = new SWTSkinButtonUtility(showHideBuddiesObject);
			addBuddyButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					addBuddy();
				}
			});
		}
	}

	protected void addBuddy() {
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				_addBuddy();
			}
		});
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	protected void _addBuddy() {
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
			} else {
				editButton.setDisabled(false);
			}
		}
	}

	public SWTSkinButtonUtility getEditButton() {
		return editButton;
	}

	public SWTSkinButtonUtility getAddBuddyButton() {
		return addBuddyButton;
	}

}
