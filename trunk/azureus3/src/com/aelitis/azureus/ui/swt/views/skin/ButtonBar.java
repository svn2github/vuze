package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.InputShell;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

public class ButtonBar
	extends SkinView
{
	private SWTSkin skin;

	private SWTSkinButtonUtility editButton = null;

	private SWTSkinButtonUtility addBuddyButton = null;

	private SWTSkinObject cancelEditBuddies = null;

	private SWTSkinObject shareAllBuddiesObject;

	boolean isShareWithAllSelected = false;

	private int currentMode = BuddiesViewer.none_active_mode;

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		
		
		hookBuddyCountLabel();
		hookEditButton();
		hookAddBuddyButon();
		hookShareAllBuddiesButton();
		hookTuxGoodies();
		hookShowHideButon();

		return null;
	}

	private void hookBuddyCountLabel() {
		SWTSkinObject showImageObject = skin.getSkinObject("text-buddy-count");
		if (showImageObject instanceof SWTSkinObjectText) {
			/*
			 * Initial Friends count
			 */
			final SWTSkinObjectText buddyCountObject = (SWTSkinObjectText) showImageObject;
			buddyCountObject.setTextID("v3.buddies.count", new String[] {
				VuzeBuddyManager.getAllVuzeBuddies().size() + ""
			});
			buddyCountObject.setVisible(true);
			Utils.relayout(buddyCountObject.getControl());

			/*
			 * Update when the number of Friends changes
			 */
			VuzeBuddyManager.addListener(new VuzeBuddyListener() {

				public void buddyRemoved(VuzeBuddy buddy) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							buddyCountObject.setTextID("v3.buddies.count", new String[] {
								VuzeBuddyManager.getAllVuzeBuddies().size() + ""
							});
							Utils.relayout(buddyCountObject.getControl());
						}
					});

				}

				public void buddyAdded(VuzeBuddy buddy, int position) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							buddyCountObject.setTextID("v3.buddies.count", new String[] {
								VuzeBuddyManager.getAllVuzeBuddies().size() + ""
							});

							Utils.relayout(buddyCountObject.getControl());
						}
					});
				}

				public void buddyOrderChanged() {
				}

				public void buddyChanged(VuzeBuddy buddy) {
				}

			}, false);
		}

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
		SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
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

	public void enableShareButton(boolean value) {
		if (shareAllBuddiesObject.isVisible()) {
			if (false == value) {
				shareAllBuddiesObject.switchSuffix("-disabled");
				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null != viewer) {
					viewer.setMode(BuddiesViewer.disabled_mode);
				}
			} else {
				shareAllBuddiesObject.switchSuffix("");
				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null != viewer) {
					viewer.setMode(BuddiesViewer.share_mode);
				}
			}
		}
	}

	private void showShareWithAllSelected(boolean value) {
		isShareWithAllSelected = value;

		SWTSkinObject normalButton = skin.getSkinObject("button-buddy-share-all-normal");
		if (null != normalButton) {
			normalButton.setVisible(!isShareWithAllSelected);
		}
		SWTSkinObject selectedButton = skin.getSkinObject("button-buddy-share-all-selected");
		if (null != selectedButton) {
			selectedButton.setVisible(isShareWithAllSelected);
		}

	}

	public void setActiveMode(int mode) {
		if (currentMode != mode) {
			currentMode = mode;

			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
			if (null == viewer) {
				return;
			}

			shareAllBuddiesObject.setVisible(false);
			cancelEditBuddies.setVisible(false);

			if (mode == BuddiesViewer.none_active_mode) {
				disabledForEdit(false);
				showShareWithAllSelected(false);
			} else if (mode == BuddiesViewer.edit_mode) {
				disabledForEdit(true);
				cancelEditBuddies.setVisible(true);
				editButton.setDisabled(true);

				/*
				 * Force the Friend footer to be visible so the Friends can be edited
				 */
				SWTSkinUtils.setVisibility(skin, "Friends.visible",
						SkinConstants.VIEWID_BUDDIES_VIEWER, true, true, true);

			} else if (mode == BuddiesViewer.share_mode) {
				disabledForShare(true);
				if (VuzeBuddyManager.getAllVuzeBuddies().size() > 0) {
					shareAllBuddiesObject.setVisible(true);
				}
				/*
				 * Force the Friend footer to be visible so the Friends can be added to Share
				 */
				SWTSkinUtils.setVisibility(skin, "Friends.visible",
						SkinConstants.VIEWID_BUDDIES_VIEWER, true, true, true);
			} else {
				disabledForEdit(true);
			}
			viewer.setMode(mode);
		}
	}

	private void disabledForShare(boolean value) {
		disabledButtonBar(value);
		/*
		 * Reset shareAllBuddiesObject to normal since it has a dynamic 'disabled' state based on whether
		 * the user is in Add Friend mode or not
		 */
		shareAllBuddiesObject.switchSuffix("");
	}

	private void disabledForEdit(boolean value) {
		disabledButtonBar(value);

		/*
		 * Reset cancelEditBuddies to normal since it has no 'disabled' state
		 */
		cancelEditBuddies.switchSuffix("");
	}

	private void disabledButtonBar(boolean value) {
		SWTSkinObject buttonBarObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
		buttonBarObject.switchSuffix(value ? "-disabled" : "");
	}

	private void hookShowHideButon() {

		SWTSkinObject showImageObject = skin.getSkinObject("button-show-footer");
		SWTSkinObject hideImageObject = skin.getSkinObject("button-hide-footer");

		if (null != showImageObject && null != hideImageObject) {

			/*
			 * Change button display if the property changes
			 */
			COConfigurationManager.addParameterListener("Friends.visible",
					new ParameterListener() {

						public void parameterChanged(String parameterName) {
							showFooterToggleButton(COConfigurationManager.getBooleanParameter("Friends.visible"));
						}
					});

			/*
			 * Initial visibility state of the footer
			 */
			showFooterToggleButton(COConfigurationManager.getBooleanParameter("Friends.visible"));

			/*
			 * Hook 'show' button
			 */
			final SWTSkinButtonUtility btnShow = new SWTSkinButtonUtility(
					showImageObject);
			btnShow.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					SWTSkinUtils.setVisibility(skin, "Friends.visible",
							"footer-buddies", true, true, true);
				}
			});

			/*
			 * Hook 'hide' button
			 */
			final SWTSkinButtonUtility btnHide = new SWTSkinButtonUtility(
					hideImageObject);
			btnHide.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					SWTSkinUtils.setVisibility(skin, "Friends.visible",
							"footer-buddies", false, true, true);
				}
			});

		}

	}

	/**
	 * Toggles between showing the 'Hide' or 'Show' buttons
	 * @param value
	 */
	private void showFooterToggleButton(boolean value) {
		SWTSkinObject showImageObject = skin.getSkinObject("button-show-footer");
		SWTSkinObject hideImageObject = skin.getSkinObject("button-hide-footer");
		if (null != showImageObject) {
			showImageObject.setVisible(!value);
		}
		if (null != hideImageObject) {
			hideImageObject.setVisible(value);
		}
	}

	private void hookEditButton() {
		final SWTSkinObject editBuddies = skin.getSkinObject("button-buddy-edit");
		cancelEditBuddies = skin.getSkinObject("button-buddy-edit-cancel");

		if (null == editBuddies || null == cancelEditBuddies) {
			Debug.out("Edit button is not found... skin may not be initialized properly");
			return;
		}

		/*
		 * If the user logs out then turn off the edit mode
		 */
		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				if (null == info.userName) {
					setActiveMode(BuddiesViewer.none_active_mode);
				}
			}
		});

		if (null != editBuddies) {
			editButton = new SWTSkinButtonUtility(editBuddies);
			editButton.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					/*
					 * If it was not in edit mode then attempt a login before setting the viewer to edit mode
					 */
					if (false == viewer.isEditMode()) {
						SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
							public void loginComplete() {
								setActiveMode(BuddiesViewer.edit_mode);
							}
						});
					} else {
						setActiveMode(BuddiesViewer.none_active_mode);
					}
				}
			});
		}

		if (null != cancelEditBuddies) {
			SWTSkinButtonUtility cancelEditButton = new SWTSkinButtonUtility(
					cancelEditBuddies);
			cancelEditButton.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
					if (null != viewer) {
						setActiveMode(BuddiesViewer.none_active_mode);
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
					addBuddy();
				}
			});
		}
	}

	private void hookShareAllBuddiesButton() {
		shareAllBuddiesObject = skin.getSkinObject("button-buddy-share-all");
		if (null != shareAllBuddiesObject) {
			SWTSkinButtonUtility shareAllBuddiesButton = new SWTSkinButtonUtility(
					shareAllBuddiesObject);
			shareAllBuddiesButton.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {
					showShareWithAllSelected(!isShareWithAllSelected);
					if (true == isShareWithAllSelected) {
						shareAllBuddies();
					} else {
						unShareAllBuddies();
					}
				}
			});
		}
	}

	protected void unShareAllBuddies() {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null != viewer) {
			viewer.removeAllFromShare();
		}
	}

	protected void shareAllBuddies() {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null != viewer) {
			viewer.addAllToShare();
		}
	}

	protected void addBuddy() {
		if (com.aelitis.azureus.util.Constants.DISABLE_BUDDIES_BAR) {
			return;
		}
		if (!VuzeBuddyManager.isEnabled()) {
			VuzeBuddyManager.showDisabledDialog();
			return;
		}

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
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}

		setActiveMode(BuddiesViewer.add_buddy_mode);

		SkinView detailPanelView = SkinViewManager.getByClass(DetailPanel.class);
		if (detailPanelView instanceof DetailPanel) {
			DetailPanel detailPanel = ((DetailPanel) detailPanelView);
			detailPanel.show(true, InvitePage.PAGE_ID);
		}
	}

	public SWTSkinButtonUtility getEditButton() {
		return editButton;
	}

	public SWTSkinButtonUtility getAddBuddyButton() {
		return addBuddyButton;
	}

}
