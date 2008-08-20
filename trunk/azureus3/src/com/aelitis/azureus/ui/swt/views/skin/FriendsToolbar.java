package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectSash;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

/**
 * A simple toolbar for the Friends section in the left navigation
 * @author khai
 *
 */
public class FriendsToolbar
	extends SkinView
{
	private ToolBar toolbar;

	private Label friendsLabel;

	private Label showHideButton;

	private Composite parent;

	private Composite content;

	private Composite shareWithAllPanel;

	private ToolItem edit;

	private ToolItem addFriends;

	private Label image;

	private Label text;

	private double lastPercent = 0.3;

	private double minPercent = 0.05;

	private int toolbarHeight = 27;

	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/torrent_down.png",
				"button_collapse");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/torrent_up.png",
				"button_expand");
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/buddy_add_to_share.png", "add_to_share");
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/buddy_add_to_share_selected.png",
				"add_to_share_selected");
	}

	public FriendsToolbar() {
	}

	public FriendsToolbar(Composite parent) {
		this.parent = parent;
		init();
	}

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		parent = (Composite) skinObject.getControl();

		init();
		return null;
	}

	private void init() {
		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException("Parent cannot be null or disposed");
		}

		Layout parentLayout = parent.getLayout();
		if (null == parentLayout) {
			parentLayout = new FormLayout();
			parent.setLayout(parentLayout);

		} else if (false == (parentLayout instanceof FormLayout)) {
			throw new IllegalArgumentException(
					"Oops! We can not handle any layout other than FormLayout at the moment!!!");
		}

		content = new Composite(parent, SWT.NONE);

		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		content.setLayoutData(fd);

		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = 3;
		layout.marginWidth = 3;
		content.setLayout(layout);

		createControls();

		parent.getParent().layout(true);

		SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash-bottom");
		if (null != soSash) {
			lastPercent = soSash.getPercent();
			toolbarHeight = parent.getSize().y;
		}

	}

	private void createControls() {
		friendsLabel = new Label(content, SWT.NONE);
		friendsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));

		/*
		 * Initial Friends count
		 */
		updateFriendsLabel();

		/*
		 * Update when the number of Friends changes
		 */
		VuzeBuddyManager.addListener(new VuzeBuddyListener() {

			public void buddyRemoved(VuzeBuddy buddy) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});

			}

			public void buddyAdded(VuzeBuddy buddy, int position) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});
			}

			public void buddyOrderChanged() {
			}

			public void buddyChanged(VuzeBuddy buddy) {
			}

		}, false);

		createSharePanel();

		toolbar = new ToolBar(content, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		createToolItems();

		showHideButton = new Label(content, SWT.NONE);
		showHideButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		/*
		 * Initial state from configuration
		 */
		showFooterToggleButton(COConfigurationManager.getBooleanParameter("Friends.visible"));

		/*
		 * Change button display if the property changes
		 */
		COConfigurationManager.addParameterListener("Friends.visible",
				new ParameterListener() {

					public void parameterChanged(String parameterName) {
						showFooterToggleButton(COConfigurationManager.getBooleanParameter("Friends.visible"));
					}
				});

		showHideButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				boolean wasExpanded = COConfigurationManager.getBooleanParameter("Friends.visible");
				COConfigurationManager.setParameter("Friends.visible", !wasExpanded);
			}
		});

	}

	private void createToolItems() {

		edit = new ToolItem(toolbar, SWT.CHECK);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				if (true == edit.getSelection()) {
					setEditMode();
				} else {
					reset();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				if (null == info.userName) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							edit.setText(MessageText.getString("Button.bar.edit"));
							edit.setSelection(false);
							reset();
							content.layout(true);
						}
					});

				}
			}
		});

		addFriends = new ToolItem(toolbar, SWT.PUSH);
		addFriends.setText("Add");
		addFriends.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				addBuddy();

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void createSharePanel() {
		/*
		 * This panel is initially not visible; it will be made visible as needed
		 */
		shareWithAllPanel = new Composite(content, SWT.NONE);
		shareWithAllPanel.setVisible(false);

		GridData gData = new GridData(SWT.END, SWT.CENTER, false, false);
		gData.widthHint = 100;
		gData.exclude = true;
		shareWithAllPanel.setLayoutData(gData);
		shareWithAllPanel.setLayout(new GridLayout(2, false));

		image = new Label(shareWithAllPanel, SWT.NONE);
		image.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		image.setImage(ImageRepository.getImage("add_to_share"));
		text = new Label(shareWithAllPanel, SWT.NONE);
		text.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		text.setText(MessageText.getString("v3.Share.add.buddy.all"));

		hookShareListener();
	}

	private void hookShareListener() {
		MouseAdapter listener = new MouseAdapter() {
			boolean shareWithAll = false;

			public void mouseUp(MouseEvent e) {
				shareWithAll = !shareWithAll;
				shareAllBuddies(shareWithAll);
			}

		};

		shareWithAllPanel.addMouseListener(listener);
		image.addMouseListener(listener);
		text.addMouseListener(listener);
	}

	public void enableShareButton(boolean value) {
		shareWithAllPanel.setEnabled(value);
		image.setEnabled(value);
		text.setEnabled(value);

		if (false == value) {
			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
			if (null != viewer) {
				viewer.setMode(BuddiesViewer.disabled_mode);
			}
		} else {
			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
			if (null != viewer) {
				viewer.setMode(BuddiesViewer.share_mode);
			}
		}
	}

	protected void shareAllBuddies(boolean value) {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null != viewer) {
			if (true == value) {
				image.setImage(ImageRepository.getImage("add_to_share_selected"));
				viewer.addAllToShare();

			} else {
				image.setImage(ImageRepository.getImage("add_to_share"));
				viewer.removeAllFromShare();
			}
		}
	}

	private void updateFriendsLabel() {
		friendsLabel.setText(MessageText.getString("v3.buddies.count",
				new String[] {
					VuzeBuddyManager.getAllVuzeBuddies().size() + ""
				}));

		/*
		 * The text could be a different length now so do a relayout just in case
		 */
		content.layout(true);
	}

	protected void addBuddy() {
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

	protected void _addBuddy() {
		addFriends.setEnabled(false);
		friendsLabel.setEnabled(false);
		showHideButton.setEnabled(false);
		edit.setEnabled(false);

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.add_buddy_mode);

		SkinView detailPanelView = SkinViewManager.getByClass(DetailPanel.class);
		if (detailPanelView instanceof DetailPanel) {
			DetailPanel detailPanel = ((DetailPanel) detailPanelView);
			detailPanel.show(true, InvitePage.PAGE_ID);
		}
	}

	public void reset() {
		addFriends.setEnabled(true);
		friendsLabel.setEnabled(true);
		showHideButton.setEnabled(true);
		edit.setEnabled(true);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.setSelection(false);

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.none_active_mode);

		showAddWithAll(false);
		content.layout(true);

	}

	public void setShareMode() {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.share_mode);
		addFriends.setEnabled(false);
		friendsLabel.setEnabled(false);
		showHideButton.setEnabled(false);
		edit.setEnabled(false);

		showAddWithAll(true);
		content.layout(true);

		SWTSkinUtils.setVisibility(skin, "Friends.visible",
				SkinConstants.VIEWID_BUDDIES_VIEWER, true, true, true);
	}

	public void setAddFriendsMode() {

		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {

			public void loginComplete() {
				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null == viewer) {
					return;
				}
				viewer.setMode(BuddiesViewer.share_mode);
				addFriends.setEnabled(false);
				friendsLabel.setEnabled(false);
				showHideButton.setEnabled(false);
				edit.setEnabled(false);

				showAddWithAll(true);
			}

			public long getCancelDelay() {
				return 0;
			}

			public void loginCanceled() {
				reset();
			}

		});

	}

	private void showAddWithAll(boolean value) {
		GridData gData = (GridData) shareWithAllPanel.getLayoutData();
		gData.exclude = !value;
		shareWithAllPanel.setVisible(value);
	}

	public void setEditMode() {
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {

			public void loginComplete() {
				SWTSkinUtils.setVisibility(skin, "Friends.visible",
						SkinConstants.VIEWID_BUDDIES_VIEWER, true, true, true);

				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null == viewer) {
					return;
				}
				viewer.setMode(BuddiesViewer.edit_mode);

				edit.setText(MessageText.getString("Button.bar.edit.cancel"));
				edit.setEnabled(true);
				addFriends.setEnabled(false);
				friendsLabel.setEnabled(false);
				showHideButton.setEnabled(false);
				content.layout(true);
			}

			public long getCancelDelay() {
				return 0;
			}

			public void loginCanceled() {
				reset();
			}

		});
	}

	/**
	 * Toggles between showing the 'collapse' or 'expand' buttons
	 * @param value
	 */
	private void showFooterToggleButton(boolean isExpanded) {
		showHideButton.setImage(isExpanded
				? ImageRepository.getImage("button_collapse")
				: ImageRepository.getImage("button_expand"));

		showFriends(isExpanded);
	}

	/**
	 * Here we collapse or expand the bottom sash of the sidebar
	 * @param isExpanded
	 */
	public void showFriends(final boolean isExpanded) {

		/*
		 * To hide a sash we typically set it's percentage to 0 but since this bottom
		 * sash has a toolbar that we want to always be visible we use the toolbar's height
		 * then calculate that as a percentage of the total height of the side bar; we use this 
		 * as the percentage when 'collapsed'
		 * 
		 * Additionally when collapsed we make the sash invisible and turn it back to visible when
		 * it's expanded.
		 */
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash-bottom");

				if (true == isExpanded) {
					if (lastPercent != 0) {
						soSash.setPercent(lastPercent);
						soSash.setVisible(true);
					}
				} else {
					lastPercent = soSash.getPercent();

					SWTSkinObject soSidebar = skin.getSkinObject(SkinConstants.VIEWID_SIDEBAR);
					if (null != soSidebar) {
						minPercent = (double) toolbarHeight
								/ (double) soSidebar.getControl().getSize().y;
					}
					soSash.setPercent(minPercent);
					soSash.setVisible(false);
				}
			}
		});
	}
}
