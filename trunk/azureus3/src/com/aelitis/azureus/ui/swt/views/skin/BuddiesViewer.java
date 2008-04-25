package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddyUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

public class BuddiesViewer
{
	private Composite avatarsPanel = null;

	private Composite parent = null;

	private SWTSkin skin = null;

	private int borderWidth = 3;

	private Point avatarImageSize = new Point(32 + borderWidth, 32 + borderWidth);

	private Point avatarSize = new Point(avatarImageSize.x * 2,
			avatarImageSize.y + 26);

	private List avatarWidgets = new ArrayList();

	private boolean isShareMode = false;

	private boolean isEditMode = false;

	private Color textColor = null;

	private Color textLinkColor = null;

	public BuddiesViewer(Composite parent, SWTSkin skin) {
		this.parent = parent;
		this.skin = skin;

		init();
		hookScrollers();
		hookEditButton();
		hookShareButon();
		hookAddBuddyButon();
		hookShowHideButon();
	}

	private void init() {
		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException(
					"The variable 'parent' can not be null or disposed");
		}

		avatarsPanel = new Composite(parent, SWT.NONE);

		textColor = skin.getSkinProperties().getColor("color.links.normal");
		textLinkColor = skin.getSkinProperties().getColor("color.links.hover");

		RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
		rLayout.wrap = false;
		rLayout.spacing = 10;
		rLayout.marginTop = 0;
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		avatarsPanel.setLayout(rLayout);

		fillBuddies(avatarsPanel);

		parent.layout(true);

		Listener l = new Listener() {
			int startX, startY;

			public void handleEvent(Event e) {

				if (e.type == SWT.MouseDown && e.button == 1
						&& (e.stateMask & SWT.CONTROL) != 0) {
					startX = e.x;
					startY = e.y;
					avatarsPanel.setCursor(parent.getDisplay().getSystemCursor(
							SWT.CURSOR_HAND));
					System.out.println("Mouse down");
				}
				if (e.type == SWT.MouseMove && (e.stateMask & SWT.BUTTON1) != 0
						&& (e.stateMask & SWT.CONTROL) != 0) {
					Point p = avatarsPanel.toDisplay(e.x, e.y);
					p.x -= startX;
					p.y -= startY;
					//					content.setLocation(p);
					System.err.println("X:" + p.x);
					System.out.println("Mouse moving...");
				}
				if (e.type == SWT.MouseUp) {
					avatarsPanel.setCursor(null);
					System.out.println("Mouse up");
				}

				if (e.type == SWT.KeyDown && (e.stateMask & SWT.BUTTON1) != 0
						&& (e.stateMask & SWT.CONTROL) != 0) {
					avatarsPanel.setCursor(parent.getDisplay().getSystemCursor(
							SWT.CURSOR_HAND));
				}
				if (e.type == SWT.Resize) {
					System.out.println("panel: " + avatarsPanel.getSize().x);
					System.out.println("parent: " + parent.getSize().x);
				}

			}
		};
		avatarsPanel.addListener(SWT.MouseDown, l);
		avatarsPanel.addListener(SWT.MouseMove, l);
		avatarsPanel.addListener(SWT.MouseUp, l);
		avatarsPanel.addListener(SWT.KeyDown, l);
		avatarsPanel.addListener(SWT.Resize, l);
		//		avatarsPanel.addListener(SWT.Dispose, l);

	}

	private void hookScrollers() {
		final SWTSkinObject leftScroll = skin.getSkinObject("buddies-left-scroller");
		if (null != leftScroll) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(leftScroll);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					Point location = avatarsPanel.getLocation();
					if (location.x < 0) {
						avatarsPanel.setLocation(location.x + parent.getSize().x,
								location.y);
						avatarsPanel.layout();
					}
				}
			});
		}

		final SWTSkinObject rightScroll = skin.getSkinObject("buddies-right-scroller");
		if (null != rightScroll) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(rightScroll);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					Point location = avatarsPanel.getLocation();
					location.x -= parent.getSize().x;

					//					if (location.x * -1 < avatarsPanel.getSize().x - parent.getSize().x) {
					avatarsPanel.setLocation(location.x, location.y);
					avatarsPanel.layout();
					//					}
				}
			});
		}

		enableScroll(leftScroll, rightScroll);
		parent.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				enableScroll(leftScroll, rightScroll);
			}
		});
	}

	private void hookEditButton() {

		final SWTSkinObject editBuddies = skin.getSkinObject("button-buddy-edit");
		if (null != editBuddies) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(editBuddies);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {

				private LightBoxShell lbShell = null;

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					setEditMode(!isEditMode());

					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();

					if (null != uiFunctions) {
						IMainWindow mainWindow = uiFunctions.getMainWindow();

						if (true == isEditMode()) {
							lbShell = new LightBoxShell(uiFunctions.getMainShell(), false);
							SWTSkinObject footerObject = skin.getSkinObject("footer");

							int insetHeight = footerObject.getControl().getSize().y;
							insetHeight += mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
							lbShell.setInsets(0, insetHeight, 0, 0);
							lbShell.open();
						} else {
							if (null != lbShell) {
								lbShell.close();
								lbShell = null;
							}
						}

					}

				}
			});

			//			btnGo.setDisabled(true);
		}

	}

	private void enableScroll(SWTSkinObject leftScroll, SWTSkinObject rightScroll) {
		if (null == leftScroll || null == rightScroll) {
			return;
		}
		if (avatarsPanel.getSize().x > parent.getSize().x) {
			leftScroll.setVisible(true);
			rightScroll.setVisible(true);
		} else {
			leftScroll.setVisible(false);
			rightScroll.setVisible(false);
		}
	}

	public boolean isEditMode() {
		return isEditMode;
	}

	public void setEditMode(boolean value) {
		if (isEditMode != value) {
			isEditMode = value;
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				widget.setEditMode(value);
				widget.refreshVisual();
			}
		}
	}

	private void fillBuddies(Composite composite) {

		VuzeBuddySWT[] buddies = getBuddies();

		for (int i = 0; i < buddies.length; i++) {
			VuzeBuddySWT vuzeBuddy = buddies[i];
			createBuddyControls(composite, vuzeBuddy);
		}

	}

	private AvatarWidget createBuddyControls(Composite composite,
			final VuzeBuddySWT vuzeBuddy) {
		AvatarWidget avatarWidget = new AvatarWidget(this, avatarSize,
				avatarImageSize, vuzeBuddy);
		avatarWidget.setBorderWidth(borderWidth);
		avatarWidget.setTextColor(textColor);
		avatarWidget.setTextLinkColor(textLinkColor);

		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarWidget.getControl().setLayoutData(rData);

		avatarWidgets.add(avatarWidget);

		return avatarWidget;
	}

	public void select(VuzeBuddySWT buddy, boolean value, boolean appendSelection) {

		if (null != buddy) {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (true == buddy.equals(widget.getVuzeBuddy())) {
					widget.setSelected(value);
					if (true == appendSelection) {
						break;
					}
				} else if (false == appendSelection) {
					if (true == value) {
						if (widget.isSelected() != false) {
							widget.setSelected(false);
							widget.refreshVisual();
						}
					} else {
						widget.setSelected(false);
						widget.refreshVisual();
					}
				}
			}
		}
	}

	private VuzeBuddySWT[] getBuddies() {
		VuzeBuddySWT[] buddies = new VuzeBuddySWT[100];
		for (int i = 0; i < buddies.length; i++) {
			buddies[i] = (VuzeBuddySWT) VuzeBuddyUtils.createRandomBuddy();
		}

		return buddies;
	}

	public Composite getControl() {
		return avatarsPanel;
	}

	public boolean isShareMode() {
		return isShareMode;
	}

	public void setShareMode(boolean isShareMode) {
		if (this.isShareMode != isShareMode) {
			this.isShareMode = isShareMode;
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				widget.setShareMode(isShareMode);
				widget.refreshVisual();
			}
		}
	}

	private void hookShareButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-share");
		if (null != showHideBuddiesObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(
					showHideBuddiesObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					setShareMode(!isShareMode());

					SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
					if (detailPanelView instanceof DetailPanel) {
						((DetailPanel) detailPanelView).show(isShareMode(),
								SharePage.PAGE_ID);
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
				boolean dummy = false;

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					dummy = !dummy;

					/*
					 * Turn off share mode when we enter add buddy flow
					 */
					if(true == isShareMode()){
						setShareMode(false);
					}
					
					SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
					if (detailPanelView instanceof DetailPanel) {
						((DetailPanel) detailPanelView).show(dummy, InvitePage.PAGE_ID);
					}
				}
			});
		}
	}

	private void hookShowHideButon() {

		final SWTSkinObject showHideBuddiesObject = skin.getSkinObject("button-buddy-show-hide");
		if (null != showHideBuddiesObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(
					showHideBuddiesObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {

				public void pressed(SWTSkinButtonUtility buttonUtility) {
					SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
					if (skinObject != null) {
						SWTSkinUtils.setVisibility(skin, "Footer.visible",
								SkinConstants.VIEWID_FOOTER, !skinObject.isVisible());
					}
				}
			});
		}
	}
}
