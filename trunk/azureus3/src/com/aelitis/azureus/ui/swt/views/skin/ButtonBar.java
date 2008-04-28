package com.aelitis.azureus.ui.swt.views.skin;

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
		return null;
	}

	private void hookShowHideButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-show-hide");
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
									int eventType, Object params) {

								if (eventType == SWTSkinObjectListener.EVENT_HIDE) {
									btnGo.setTextID("Button.bar.show");
								} else if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
									btnGo.setTextID("Button.bar.hide");
								}
								return null;
							}
						});

						SWTSkinUtils.setVisibility(skin, "Footer.visible",
								SkinConstants.VIEWID_FOOTER, !skinObject.isVisible());
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
		if (null != editBuddies) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(editBuddies);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
					if (null == viewer) {
						return;
					}

					viewer.setEditMode(!viewer.isEditMode());

				}
			});
		}

	}

	private void hookShareButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-share");
		if (null != showHideBuddiesObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(
					showHideBuddiesObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
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
							IDetailPage invitePage = detailPanel.getPage(SharePage.PAGE_ID);
							if (null != invitePage.getMessageContext()) {
								invitePage.getMessageContext().executeInBrowser(
										"inviteFromShare(" + true + ")");
							}
						}
					}
				}
			});
		}
	}

	private void hookAddBuddyButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-add");
		if (null != showHideBuddiesObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(
					showHideBuddiesObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
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
						}
					}
				}
			});
		}
	}
}
