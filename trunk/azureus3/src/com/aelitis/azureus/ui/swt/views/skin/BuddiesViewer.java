package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

public class BuddiesViewer
	extends SkinView
{
	private Composite avatarsPanel = null;

	private Composite parent = null;

	private SWTSkin skin = null;

	private int borderWidth = 5;

	private Point avatarImageSize = new Point(40 + borderWidth, 40 + borderWidth);

	private Point avatarSize = new Point(avatarImageSize.x + 20,
			avatarImageSize.y + 16);

	private List avatarWidgets = new ArrayList();

	private boolean isShareMode = false;

	private boolean isEditMode = false;

	private boolean isAddBuddyMode = false;

	private Color textColor = null;

	private Color textLinkColor = null;

	private List sharedAvatars = new ArrayList();

	public BuddiesViewer() {
	}

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		SWTSkinObject viewer = skin.getSkinObject(SkinConstants.VIEWID_BUDDIES_VIEWER);

		if (null != viewer) {

			parent = (Composite) viewer.getControl();

			Composite content = new Composite(parent, SWT.NONE);
			FormData fd = new FormData();
			fd.top = new FormAttachment(0, 0);
			fd.bottom = new FormAttachment(100, 0);
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			content.setLayoutData(fd);

			avatarsPanel = new Composite(content, SWT.NONE);
			avatarsPanel.setLocation(0, 0);

			textColor = skin.getSkinProperties().getColor("color.links.normal");
			textLinkColor = skin.getSkinProperties().getColor("color.links.hover");

			RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
			rLayout.wrap = false;
			rLayout.spacing = 10;
			rLayout.marginTop = 6;
			rLayout.marginBottom = 0;
			rLayout.marginLeft = 0;
			rLayout.marginRight = 0;
			rLayout.marginWidth = 0;
			rLayout.marginHeight = 0;
			avatarsPanel.setLayout(rLayout);

			fillBuddies(avatarsPanel);

			parent.layout(true);

			hookScrollers();
		}
		return null;

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
					scrollPageRight();
					//					Point location = avatarsPanel.getLocation();
					//					location.x -= parent.getSize().x;
					//
					//					//					if (location.x * -1 < avatarsPanel.getSize().x - parent.getSize().x) {
					//					avatarsPanel.setLocation(location.x, location.y);
					//					avatarsPanel.layout();
					//					//					}
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

	private void scrollPageRight() {
		final Point location = avatarsPanel.getLocation();

		parent.getDisplay().asyncExec(new AERunnable() {

			public void runSupport() {
				int pageWidth = parent.getSize().x;
				int incrementer = (int) (pageWidth * .5);
				while (incrementer > 0) {
					location.x -= incrementer;
					avatarsPanel.setLocation(location.x, location.y);
					parent.update();
					incrementer = (int) (incrementer * .5);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		});

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

			if (true == value) {
				setShareMode(false);
				setAddBuddyMode(false);
			}
		}
	}

	private void fillBuddies(Composite composite) {

		List buddies = getBuddies();

		for (Iterator iterator = buddies.iterator(); iterator.hasNext();) {
			VuzeBuddySWT vuzeBuddy = (VuzeBuddySWT) iterator.next();
			createBuddyControls(composite, vuzeBuddy);
		}
		composite.layout();
		Point size = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		composite.setSize(size);

	}

	private AvatarWidget createBuddyControls(Composite composite,
			final VuzeBuddySWT vuzeBuddy) {
		AvatarWidget avatarWidget = new AvatarWidget(this, avatarSize,
				avatarImageSize, vuzeBuddy);
		avatarWidget.setBorderWidth(borderWidth);
		avatarWidget.setTextColor(textColor);
		avatarWidget.setTextLinkColor(textLinkColor);

		avatarWidgets.add(avatarWidget);

		return avatarWidget;
	}

	public void removeBuddy(final AvatarWidget widget) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				avatarWidgets.remove(widget);
				widget.dispose(true);
				avatarsPanel.layout(true);
			}
		});
	}

	public void removeBuddy(VuzeBuddy buddy) {
		AvatarWidget widget = findWidget(buddy);
		if (null != widget) {
			removeBuddy(widget);
		} else {
			Debug.out("Unknown VuzeBuddy; can not remove from viewer since we don't have it.");
		}
	}

	public void updateBuddy(VuzeBuddy buddy) {
		if (buddy instanceof VuzeBuddySWT) {
			AvatarWidget widget = findWidget(buddy);
			if (null != widget) {
				widget.setVuzeBuddy((VuzeBuddySWT) buddy);
				widget.getVuzeBuddy();
			} else {
				addBuddy(buddy);
			}
		}
	}

	public void addBuddy(final VuzeBuddy buddy) {
		if (buddy instanceof VuzeBuddySWT) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					createBuddyControls(avatarsPanel, (VuzeBuddySWT) buddy);
					avatarsPanel.layout();
					Point size = avatarsPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
					avatarsPanel.setSize(size);
				}
			});

		} else {
			Debug.out("Wrong type VuzeBuddy... must be of type VuzeBuddySWT");
		}
	}

	private AvatarWidget findWidget(VuzeBuddy buddy) {
		if (null != buddy) {
			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				if (true == buddy.equals(widget.getVuzeBuddy())) {
					return widget;
				}

			}
		}

		return null;
	}

	/**
	 * Return a list of <code>VuzeBuddySWT</code> that are currently selected
	 * @return
	 */
	public List getSelection() {
		List selected = new ArrayList();
		for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
			AvatarWidget widget = (AvatarWidget) iterator.next();
			if (true == widget.isSelected()) {
				selected.add(widget.getVuzeBuddy());
			}
		}
		return selected;
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

	private List getBuddies() {

		List buddiesList = VuzeBuddyManager.getAllVuzeBuddies();
		for (Iterator iterator = buddiesList.iterator(); iterator.hasNext();) {
			System.out.println("Got friend: "
					+ ((VuzeBuddy) iterator.next()).getLoginID());//KN: sysout			
		}

		VuzeBuddyManager.addListener(new VuzeBuddyListener() {

			public void buddyRemoved(VuzeBuddy buddy) {
				System.out.println("VuzeBuddyManager.buddyRemoved: "
						+ buddy.getLoginID());//KN: sysout
				removeBuddy(buddy);
			}

			public void buddyChanged(VuzeBuddy buddy) {
				System.out.println("VuzeBuddyManager.buddyChanged" + buddy.getLoginID());//KN: sysout
				updateBuddy(buddy);
			}

			public void buddyAdded(VuzeBuddy buddy) {
				System.out.println("VuzeBuddyManager.buddyAdded" + buddy.getLoginID());//KN: sysout
				addBuddy(buddy);
			}
		}, false);

		return buddiesList;
	}

	public Composite getControl() {
		return avatarsPanel;
	}

	public boolean isShareMode() {
		return isShareMode;
	}

	public void addToShare(AvatarWidget widget) {
		sharedAvatars.add(widget);

		SkinView detailPanelView = SkinViewManager.get(DetailPanel.class);
		if (detailPanelView instanceof DetailPanel) {
			DetailPanel detailPanel = ((DetailPanel) detailPanelView);
			SharePage sharePage = (SharePage) detailPanel.getPage(SharePage.PAGE_ID);
			sharePage.addBuddy(widget.getVuzeBuddy());
		}
	}

	public void removeFromShare(AvatarWidget widget) {
		if (true == sharedAvatars.contains(widget)) {
			sharedAvatars.remove(widget);
		}
	}

	public void setShareMode(boolean isShareMode) {
		if (this.isShareMode != isShareMode) {
			this.isShareMode = isShareMode;

			/*
			 * Clears the shared list
			 */
			sharedAvatars.clear();

			for (Iterator iterator = avatarWidgets.iterator(); iterator.hasNext();) {
				AvatarWidget widget = (AvatarWidget) iterator.next();
				widget.setShareMode(isShareMode);
				widget.refreshVisual();
			}

			if (true == isShareMode) {
				setEditMode(false);
				setAddBuddyMode(false);
			}
		}
	}

	public boolean isAddBuddyMode() {
		return isAddBuddyMode;
	}

	public void setAddBuddyMode(boolean isAddBuddyMode) {
		this.isAddBuddyMode = isAddBuddyMode;
		/*
		 * Turn off share mode when we enter add buddy flow
		 */
		if (true == isAddBuddyMode) {
			setShareMode(false);
			setEditMode(false);
		}
	}

}
