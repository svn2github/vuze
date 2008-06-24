package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.VuzeBuddySyncListener;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.util.LoginInfoManager;

public class AvatarWidget
{
	private static final boolean SHOW_ONLINE_BORDER = System.getProperty(
			"az.buddy.show_online", "1").equals("1");

	private Canvas canvas = null;

	private BuddiesViewer viewer = null;

	private Composite parent = null;

	private int highlightBorder = 0;

	private int imageBorder = 1;

	private Point imageSize = null;

	private Point size = null;

	private Point nameAreaSize = null;

	private Rectangle imageBounds = null;

	private Rectangle nameAreaBounds = null;

	private VuzeBuddySWT vuzeBuddy = null;

	private boolean isActivated = false;

	private boolean isSelected = false;

	private boolean isEnabled = true;

	private boolean isDisposing = false;

	private boolean nameLinkActive = false;

	private Color textColor = null;

	private Color textLinkColor = null;

	private Color imageBorderColor = null;

	private Color selectedColor = null;

	private Color highlightedColor = null;

	private Rectangle decorator_remove_friend = null;

	private Rectangle decorator_add_to_share = null;

	private int alpha = 255;

	private boolean sharedAlready = false;

	private Image image = null;

	private Image imageDefaultAvatar = null;

	private Rectangle sourceImageBounds = null;

	private Menu menu;

	private static Font fontDisplayName;

	private String tooltip_remove_friend;

	private String tooltip_add_to_share;

	private String tooltip;

	private Image removeImage = null;

	private Image add_to_share_Image = null;

	private Image removeImage_normal = null;

	private Image add_to_share_Image_normal = null;

	private Image removeImage_over = null;

	private Image add_to_share_Image_selected = null;

	public AvatarWidget(BuddiesViewer viewer, Point avatarSize,
			Point avatarImageSize, Point avatarNameSize, VuzeBuddySWT vuzeBuddy) {

		if (null == viewer || null == vuzeBuddy) {
			throw new NullPointerException(
					"The variable 'viewer' and 'vuzeBuddy' can not be null");
		}

		this.viewer = viewer;

		if (null == viewer.getControl() || true == viewer.getControl().isDisposed()) {
			throw new NullPointerException(
					"The given 'viewer' is not properly initialized");
		}

		this.parent = viewer.getControl();
		this.size = avatarSize;
		this.imageSize = avatarImageSize;
		this.nameAreaSize = avatarNameSize;
		this.vuzeBuddy = vuzeBuddy;
		canvas = new Canvas(parent, SWT.NONE | SWT.DOUBLE_BUFFERED);
		canvas.setData("AvatarWidget", this);

		init();
	}

	private void init() {

		ImageLoader imageLoader = ImageLoaderFactory.getInstance();
		removeImage_normal = imageLoader.getImage("image.buddy.remove");
		add_to_share_Image_normal = imageLoader.getImage("image.buddy.add.to.share");
		removeImage_over = imageLoader.getImage("image.buddy.remove-over");
		add_to_share_Image_selected = imageLoader.getImage("image.buddy.add.to.share-selected");
		imageDefaultAvatar = imageLoader.getImage("image.buddy.default.avatar");

		removeImage = removeImage_normal;
		add_to_share_Image = add_to_share_Image_normal;

		tooltip_remove_friend = MessageText.getString("v3.buddies.remove");
		tooltip_add_to_share = MessageText.getString("v3.buddies.add.to.share");
		tooltip = vuzeBuddy.getDisplayName() + " (" + vuzeBuddy.getLoginID() + ")";

		/*
		 * Centers the image and name horizontally
		 */
		imageBounds = new Rectangle((size.x / 2) - (imageSize.x / 2), 8,
				imageSize.x, imageSize.y);

		nameAreaBounds = new Rectangle((size.x / 2) - ((nameAreaSize.x - 6) / 2),
				imageBounds.y + imageBounds.height + 2, nameAreaSize.x - 6,
				nameAreaSize.y);

		/*
		 * Position the decorator icons
		 */
		decorator_remove_friend = new Rectangle(size.x
				- (highlightBorder + imageBorder) - 12 - 1, highlightBorder
				+ imageBorder + 1, 12, 12);

		decorator_add_to_share = new Rectangle(highlightBorder + imageBorder + 1,
				highlightBorder + imageBorder + 1, 12, 12);
		/*
		 * Get the avatar image and create a default image if none was found
		 */
		image = vuzeBuddy.getAvatarImage();
		if (null == image) {
			image = imageDefaultAvatar;
		}

		sourceImageBounds = null == image ? null : image.getBounds();

		canvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				if (false == isFullyVisible()) {
					return;
				}

				if (fontDisplayName == null || fontDisplayName.isDisposed()) {
					fontDisplayName = Utils.getFontWithHeight(canvas.getFont(), e.gc, 10);
				}

				try {
					e.gc.setAntialias(SWT.ON);
					e.gc.setTextAntialias(SWT.ON);
					e.gc.setAlpha(getAlpha());
					e.gc.setInterpolation(SWT.HIGH);
				} catch (Exception ex) {
					// ignore.. some of these may not be avail
				}

				/*
				 * Draw background if the widget is activated or selected
				 */
				if (true == isActivated || true == isSelected) {

					e.gc.setBackground(true == isActivated ? highlightedColor
							: selectedColor);
					Rectangle bounds = canvas.getBounds();
					e.gc.fillRoundRectangle(highlightBorder, highlightBorder,
							bounds.width - (2 * highlightBorder), bounds.height
									- (2 * highlightBorder), 6, 6);
					e.gc.setBackground(canvas.getBackground());
				}

				/*
				 * Draw highlight borders if the widget is activated (being hovered over)
				 */

				if (SHOW_ONLINE_BORDER) {

					if (true == vuzeBuddy.isOnline( true )) {

						e.gc.setForeground(ColorCache.getColor(canvas.getDisplay(), 33,
								107, 57));
						e.gc.setBackground(ColorCache.getColor(canvas.getDisplay(), 40,
								130, 70));
						Rectangle bounds = canvas.getBounds();

						e.gc.fillRectangle(8, 5, bounds.width - 16, bounds.height - 18);
						e.gc.drawRectangle(8, 5, bounds.width - 17, bounds.height - 19);
						e.gc.setForeground(ColorCache.getColor(canvas.getDisplay(), 50,
								159, 86));
						e.gc.drawLine(8, 5, bounds.width - 9, 5);
						e.gc.drawLine(8, 5, 8, bounds.height - 15);

						e.gc.setForeground(canvas.getForeground());

					}
				}

				/*
				 * Draw the avatar image
				 */
				if (null == image || image.isDisposed()) {
					/*
					 * Paint nothing if the buddy has no avatar AND the default image is not found,
					 * OR the image has been disposed
					 */
					Debug.out("No avatar image found and no default image supplies?");
				} else {
					if (true == viewer.isEditMode()) {
						e.gc.setAlpha((int) (getAlpha() * .7));
						/*
						 * Image
						 */
						e.gc.drawImage(image, 0, 0, sourceImageBounds.width,
								sourceImageBounds.height, imageBounds.x, imageBounds.y,
								imageBounds.width, imageBounds.height);
						e.gc.setAlpha(getAlpha());
						/*
						 * Image border
						 */
						if (imageBorder > 0) {
							e.gc.setForeground(imageBorderColor);
							e.gc.setLineWidth(imageBorder);
							e.gc.drawRectangle(imageBounds.x - imageBorder, imageBounds.y
									- imageBorder, imageBounds.width + imageBorder,
									imageBounds.height + imageBorder);
							e.gc.setForeground(canvas.getForeground());
						}
					} else {
						/*
						 * Image
						 */
						e.gc.drawImage(image, 0, 0, sourceImageBounds.width,
								sourceImageBounds.height, imageBounds.x, imageBounds.y,
								imageBounds.width, imageBounds.height);
						/*
						 * Image border
						 */
						if (imageBorder > 0) {
							e.gc.setForeground(imageBorderColor);
							e.gc.setLineWidth(imageBorder);
							e.gc.drawRectangle(imageBounds.x - imageBorder, imageBounds.y
									- imageBorder, imageBounds.width + imageBorder,
									imageBounds.height + imageBorder);
							e.gc.setForeground(canvas.getForeground());
						}
					}
				}

				if (isSharedAlready()) {
					add_to_share_Image = add_to_share_Image_selected;
				} else {
					add_to_share_Image = add_to_share_Image_normal;
				}

				/*
				 * Draw decorator
				 */
				if (true == viewer.isEditMode()) {
					e.gc.drawImage(removeImage, 0, 0, removeImage.getBounds().width,
							removeImage.getBounds().height, decorator_remove_friend.x,
							decorator_remove_friend.y, decorator_remove_friend.width,
							decorator_remove_friend.height);
				} else if (true == viewer.isShareMode()) {
					e.gc.drawImage(add_to_share_Image, 0, 0,
							removeImage.getBounds().width, removeImage.getBounds().height,
							decorator_add_to_share.x, decorator_add_to_share.y,
							decorator_add_to_share.width, decorator_add_to_share.height);
				}

				/*
				 * Draw the buddy display name
				 */

				if (null != textLinkColor && null != textColor) {
					if (true == nameLinkActive && true == isActivated) {
						e.gc.setForeground(textLinkColor);
						canvas.setCursor(canvas.getDisplay().getSystemCursor(
								SWT.CURSOR_HAND));
					} else {
						canvas.setCursor(null);
						e.gc.setForeground(textColor);
					}

					/*
					 * The multi-line display of name is disabled for now 
					 */
					//					int flags = SWT.CENTER | SWT.WRAP;
					//					GCStringPrinter stringPrinter = new GCStringPrinter(e.gc,
					//							vuzeBuddy.getDisplayName(), avatarNameBounds, false, true, flags);
					//					stringPrinter.calculateMetrics();
					//
					//					if (stringPrinter.isCutoff()) {
					//						e.gc.setFont(fontDisplayName);
					//						avatarNameBounds.height += 9;
					//						avatarNameBounds.y -= 4;
					//					}
					//					stringPrinter.printString(e.gc, avatarNameBounds, SWT.CENTER);
					//					e.gc.setFont(null);
					e.gc.setFont(fontDisplayName);
					GCStringPrinter.printString(e.gc, vuzeBuddy.getDisplayName(),
							nameAreaBounds, false, true, SWT.CENTER);

				}
			}
		});

		canvas.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {

			}

			public void mouseExit(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				isActivated = false;
				canvas.redraw();
			}

			public void mouseEnter(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if (false == isActivated) {
					isActivated = true;
					canvas.redraw();
				}
			}
		});

		canvas.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if (e.button != 1) {
					return;
				}

				/*
				 * If it's in Share mode then clicking on any part will add it to Share
				 */
				if (true == viewer.isShareMode()) {
					doAddBuddyToShare();
					return;
				}

				if (true == nameAreaBounds.contains(e.x, e.y)) {
					doLinkClicked();
				} else if (decorator_remove_friend.contains(e.x, e.y)) {
					if (true == viewer.isEditMode()) {
						doRemoveBuddy();
					}
				} else if (decorator_add_to_share.contains(e.x, e.y)) {

				} else {
					if ((e.stateMask & SWT.MOD1) == SWT.MOD1) {
						viewer.select(vuzeBuddy, !isSelected, true);
					} else {
						viewer.select(vuzeBuddy, !isSelected, false);
					}
					canvas.redraw();
				}
			}

			public void mouseDown(MouseEvent e) {

			}

			public void mouseDoubleClick(MouseEvent e) {
				if (false == viewer.isShareMode()) {
					doLinkClicked();
					return;
				}
			}
		});

		canvas.addMouseMoveListener(new MouseMoveListener() {
			private boolean lastActiveState = false;

			private String lastTooltipText = canvas.getToolTipText();

			public void mouseMove(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if ((e.stateMask & SWT.MOD1) == SWT.MOD1) {
					return;
				}

				/*
				 * Optimization employed to minimize how often the tooltip text is updated;
				 * updating too frequently causes the tooltip to 'stick' to the cursor which
				 * can be annoying
				 */
				String tooltipText = "";

				if (true == viewer.isShareMode()) {
					if (false == isSharedAlready()) {
						tooltipText = tooltip_add_to_share;
					} else {
						tooltipText = tooltip;
					}
				} else if (decorator_remove_friend.contains(e.x, e.y)) {
					if (true == viewer.isEditMode()) {
						tooltipText = tooltip_remove_friend;
					} else {
						tooltipText = tooltip;
					}
				} else {
					tooltipText = tooltip;
				}

				if (false == tooltipText.equals(lastTooltipText)) {
					canvas.setToolTipText(tooltipText);
					lastTooltipText = tooltipText;
				}

				if (true == nameAreaBounds.contains(e.x, e.y)) {
					if (false == lastActiveState) {
						nameLinkActive = true;
						canvas.redraw();
						lastActiveState = true;
					}
				} else {
					if (true == lastActiveState) {
						nameLinkActive = false;
						canvas.redraw();
						lastActiveState = false;
					}
				}

			}
		});

		initMenu();
	}

	private boolean isFullyVisible() {
		return viewer.isFullyVisible(AvatarWidget.this);
	}

	private void initMenu() {
		menu = new Menu(canvas);
		canvas.setMenu(menu);

		menu.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX) {
					return;
				}

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed()) {
							return;
						}
						MenuItem[] items = menu.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}

				bShown = true;

				fillMenu(menu);
			}
		});
	}

	protected void fillMenu(Menu menu) {
		MenuItem item;

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item, "v3.buddy.menu.viewprofile");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AvatarWidget aw = (AvatarWidget) canvas.getData("AvatarWidget");
				if (aw != null) {
					aw.doLinkClicked();
				}
			}
		});

		if (Constants.isCVSVersion()) {
			MenuItem itemMenuDebug = new MenuItem(menu, SWT.CASCADE);
			itemMenuDebug.setText("Debug");
			Menu menuCVS = new Menu(menu);
			itemMenuDebug.setMenu(menuCVS);

			item = new MenuItem(menuCVS, SWT.PUSH);
			Messages.setLanguageText(item, "v3.buddy.menu.remove");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					AvatarWidget aw = (AvatarWidget) canvas.getData("AvatarWidget");
					if (aw != null) {
						doRemoveBuddy();
					}
				}
			});

			item = new MenuItem(menuCVS, SWT.PUSH);
			item.setText("Send Activity Message");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					InputShell is = new InputShell("Moo", "Message:");
					String txt = is.open();
					if (txt != null) {
						txt = LoginInfoManager.getInstance().getUserInfo().userName
								+ " says: \n" + txt;
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
								SystemTime.getCurrentTime(), txt, "Test");
						System.out.println("sending to " + vuzeBuddy.getDisplayName());
						try {
							vuzeBuddy.sendActivity(entry);
						} catch (NotLoggedInException e1) {
							Debug.out("Shouldn't Happen", e1);
						}
					}
				}
			});
			
			item = new MenuItem(menuCVS, SWT.PUSH);
			item.setText("Sync this buddy (via PK)");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					final String pk = vuzeBuddy.getPublicKeys()[0];
					final long lastUpdate = vuzeBuddy.getLastUpdated();
					try {
						PlatformBuddyMessenger.sync(new String[] {
							pk
						}, new VuzeBuddySyncListener() {
							public void syncComplete() {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (vuzeBuddy.getLastUpdated() != lastUpdate) {
											Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Yay",
													"Updated");
										} else {
											Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Boo",
													"Not Updated");
										}
									}
								});
							}
						});
					} catch (NotLoggedInException e1) {
					}
				}
			});
		}
	}

	private void doRemoveBuddy() {

		LightBoxShell lbShell = new LightBoxShell(parent.getShell(), false);

		MessageBoxShell mBox = new MessageBoxShell(lbShell.getShell(),
				MessageText.getString("v3.buddies.remove.buddy.dialog.title"),
				MessageText.getString("v3.buddies.remove.buddy.dialog.text",
						new String[] {
							vuzeBuddy.getLoginID()
						}), new String[] {
					MessageText.getString("v3.mb.delPublished.delete"),
					MessageText.getString("v3.mb.delPublished.cancel")
				}, 1);

		mBox.setLeftImage(SWT.ICON_QUESTION);

		lbShell.open();

		if (1 == mBox.open(true)) {
			lbShell.close();
			return;
		}
		try {
			VuzeBuddyManager.removeBuddy(vuzeBuddy, true);
		} catch (NotLoggedInException e) {
			// should not happen, unless the user cancelled
			Debug.out(e);
		}
		lbShell.close();
	}

	private void doAddBuddyToShare() {
		if (false == isSharedAlready()) {
			viewer.addToShare(this);
			sharedAlready = true;
		} else {
			viewer.removeFromShare(vuzeBuddy);
			sharedAlready = false;
		}
		canvas.redraw();
		canvas.update();
	}

	public void doHover() {

	}

	public void doClick() {

	}

	public void doMouseEnter() {

	}

	public void doDoubleClick() {

	}

	public void doLinkClicked() {

		/*
		 * Open the user profile page but only if NOT in Share or Add mode
		 */
		if (false == viewer.isShareMode() && false == viewer.isAddBuddyMode()) {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null != uiFunctions) {
				String url = getVuzeBuddy().getProfileUrl("buddy-bar");
				uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
						true, true);
			}
		}
	}

	public Control getControl() {
		return canvas;
	}

	public int getBorderWidth() {
		return highlightBorder;
	}

	public void setBorderWidth(int borderWidth) {
		//		this.highlightBorder = borderWidth;
	}

	public VuzeBuddySWT getVuzeBuddy() {
		return vuzeBuddy;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public void refreshVisual() {

		/*
		 * Resets the image and image bounds since this is the only info cached;
		 * all other info is asked for on-demand so no need to update them 
		 */
		image = vuzeBuddy.getAvatarImage();
		if (null == image) {
			image = imageDefaultAvatar;
		}
		sourceImageBounds = null == image ? null : image.getBounds();
		tooltip = vuzeBuddy.getDisplayName() + " (" + vuzeBuddy.getLoginID() + ")";

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (null != canvas && false == canvas.isDisposed()) {
					canvas.redraw();
				}
			}
		});

	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getTextLinkColor() {
		return textLinkColor;
	}

	public void setTextLinkColor(Color textLinkColor) {
		this.textLinkColor = textLinkColor;
	}

	public void dispose(boolean animate, final AfterDisposeListener listener) {
		if (null != canvas && false == canvas.isDisposed()) {
			if (true == animate) {
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {

						isDisposing = true;

						/*
						 * KN: TODO: disposal check is still not complete since it could still happen
						 * between the .isDisposed() check and the .redraw() or .update() calls.
						 */
						while (alpha > 20 && false == canvas.isDisposed()) {
							alpha -= 30;
							canvas.redraw();
							canvas.update();

							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						if (false == canvas.isDisposed()) {
							canvas.dispose();
							parent.layout(true);
							if (null != listener) {
								listener.disposed();
							}
						}
					}
				});
			} else {
				if (false == canvas.isDisposed()) {
					canvas.dispose();
					parent.layout(true);
					if (null != listener) {
						listener.disposed();
					}
				}
			}

		}
	}

	public boolean isSharedAlready() {
		return sharedAlready;
	}

	public void setSharedAlready(boolean sharedAlready) {
		this.sharedAlready = sharedAlready;
		refreshVisual();
	}

	public void setVuzeBuddy(VuzeBuddySWT vuzeBuddy) {
		if (null != vuzeBuddy) {
			this.vuzeBuddy = vuzeBuddy;
			refreshVisual();
		}
	}

	public Point getAvatarImageSize() {
		return imageSize;
	}

	public void setAvatarImageSize(Point avatarImageSize) {
		this.imageSize = avatarImageSize;
	}

	public Point getAvatarNameSize() {
		return nameAreaSize;
	}

	public void setAvatarNameSize(Point avatarNameSize) {
		this.nameAreaSize = avatarNameSize;
	}

	public Image getAvatarImage() {
		return image;
	}

	public void setAvatarImage(Image avatarImage) {
		this.image = avatarImage;
	}

	public Color getImageBorderColor() {
		return imageBorderColor;
	}

	public void setImageBorderColor(Color imageBorderColor) {
		this.imageBorderColor = imageBorderColor;
	}

	public int getAvatarImageBorder() {
		return imageBorder;
	}

	public void setAvatarImageBorder(int avatarImageBorder) {
		this.imageBorder = avatarImageBorder;
	}

	public int getImageBorder() {
		return imageBorder;
	}

	public void setImageBorder(int imageBorder) {
		this.imageBorder = imageBorder;
	}

	public Color getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(Color selectedColor) {
		this.selectedColor = selectedColor;
	}

	public Color getHighlightedColor() {
		return highlightedColor;
	}

	public void setHighlightedColor(Color highlightedColor) {
		this.highlightedColor = highlightedColor;
	}

	public boolean isEnabled() {
		if (false == isEnabled) {
			return isEnabled;
		}

		return viewer.isEnabled();
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	private int getAlpha() {
		if (!isDisposing) {
			if (true == isEnabled()) {
				alpha = 255;
			} else {
				alpha = 128;
			}
		}

		return alpha;
	}

	public interface AfterDisposeListener
	{
		public void disposed();
	}
}
