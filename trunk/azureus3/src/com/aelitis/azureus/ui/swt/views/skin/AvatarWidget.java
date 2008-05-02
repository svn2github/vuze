package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.util.Constants;

public class AvatarWidget
{
	private Canvas avatarCanvas = null;

	private BuddiesViewer viewer = null;

	private Composite parent = null;

	private int borderWidth = 0;

	private Point avatarImageSize = null;

	private Point avatarSize = null;

	private VuzeBuddySWT vuzeBuddy = null;

	private int imageOffsetX = 0;

	private boolean isActivated = false;

	private boolean isSelected = false;

	private boolean nameLinkActive = false;

	private boolean isEditMode = false;

	private boolean isShareMode = false;

	private Color textColor = null;

	private Color textLinkColor = null;

	private Rectangle decoratorBounds = null;

	private int alpha = 255;

	private boolean sharedAlready = false;

	private Image avatarImage = null;

	private Rectangle avatarBounds = null;

	public AvatarWidget(BuddiesViewer viewer, Point avatarSize,
			Point avatarImageSize, VuzeBuddySWT vuzeBuddy) {

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
		this.avatarSize = avatarSize;
		this.avatarImageSize = avatarImageSize;
		this.vuzeBuddy = vuzeBuddy;
		avatarCanvas = new Canvas(parent, SWT.NONE);

		init();
	}

	private void init() {

		final Image removeImage = ImageRepository.getImage("progress_remove");
		final Image add_to_share_Image = ImageRepository.getImage("add_to_share");

		imageOffsetX = (avatarSize.x / 2) - (avatarImageSize.x / 2);
		decoratorBounds = new Rectangle(imageOffsetX + avatarImageSize.x - 13, 0,
				16, 16);

		//		Utils.createTorrentDropTarget(avatarCanvas, true);

		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarCanvas.setLayoutData(rData);

		avatarCanvas.setToolTipText(vuzeBuddy.getLoginID());

		avatarImage = vuzeBuddy.getAvatarImage();

		if (null == avatarImage) {
			avatarImage = ImageRepository.getImage("buddy_default_avatar");
		}
		avatarBounds = null == avatarImage ? null : avatarImage.getBounds();

		avatarCanvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {

				e.gc.setAntialias(SWT.ON);
				e.gc.setAlpha(alpha);

				/*
				 * Draw backgound if the widget is selected
				 */
				if (true == isSelected) {

					e.gc.setBackground(Colors.grey);
					e.gc.setAlpha((int) (alpha * .5));

					Rectangle bounds = avatarCanvas.getBounds();

					e.gc.fillRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1,
							10, 10);
					e.gc.setAlpha(alpha);
					e.gc.setBackground(avatarCanvas.getBackground());
				}

				/*
				 * Draw hightlight borders if the widget is activated (being hovered over)
				 */
				if (true == isActivated) {

					e.gc.setForeground(Colors.grey);
					Rectangle bounds = avatarCanvas.getBounds();
					e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1,
							10, 10);
					e.gc.setForeground(avatarCanvas.getForeground());

				}

				/*
				 * Draw the avatar image
				 */
				if (null == avatarImage) {
					//Do something if no Avatar like display default
				} else {
					if (true == isEditMode) {
						e.gc.setAlpha((int) (alpha * .7));
						e.gc.drawImage(avatarImage, 0, 0, avatarBounds.width,
								avatarBounds.height, imageOffsetX + borderWidth, borderWidth,
								avatarImageSize.x - (2 * borderWidth), avatarImageSize.y
										- (2 * borderWidth));
						e.gc.setAlpha(alpha);
					} else {

						e.gc.drawImage(avatarImage, 0, 0, avatarBounds.width,
								avatarBounds.height, imageOffsetX + borderWidth, borderWidth,
								avatarImageSize.x - (2 * borderWidth), avatarImageSize.y
										- (2 * borderWidth));
					}
				}
				/*
				 * Draw decorator
				 */
				if (true == isEditMode) {
					e.gc.drawImage(removeImage, 0, 0, removeImage.getBounds().width,
							removeImage.getBounds().height, decoratorBounds.x,
							decoratorBounds.y, decoratorBounds.width, decoratorBounds.height);
				} else if (true == isShareMode && false == sharedAlready) {
					e.gc.drawImage(add_to_share_Image, 0, 0,
							removeImage.getBounds().width, removeImage.getBounds().height,
							decoratorBounds.x, decoratorBounds.y, decoratorBounds.width,
							decoratorBounds.height);
				}

				/*
				 * Draw the buddy display name
				 */

				if (null != textLinkColor && null != textColor) {
					if (true == nameLinkActive && true == isActivated) {
						e.gc.setForeground(textLinkColor);
					} else {
						e.gc.setForeground(textColor);
					}

					GCStringPrinter.printString(e.gc, vuzeBuddy.getDisplayName(),
							new Rectangle(1, avatarImageSize.y, avatarSize.x - 2,
									avatarSize.y - avatarImageSize.y), false, false, SWT.TOP
									| SWT.CENTER);

				}
			}
		});

		avatarCanvas.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {

			}

			public void mouseExit(MouseEvent e) {
				isActivated = false;
				avatarCanvas.redraw();
			}

			public void mouseEnter(MouseEvent e) {
				if (false == isActivated) {
					isActivated = true;
					avatarCanvas.redraw();
				}
			}
		});

		avatarCanvas.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
				if (e.y > avatarImageSize.y && e.stateMask != SWT.MOD1) {
					doLinkClicked();
				} else if (decoratorBounds.contains(e.x, e.y)) {
					if (true == isEditMode()) {
						doRemoveBuddy();
					} else if (true == isShareMode()) {
						doAddBuddyToShare();
					}
				} else {
					if (e.stateMask == SWT.MOD1) {
						viewer.select(vuzeBuddy, !isSelected, true);
					} else {
						viewer.select(vuzeBuddy, !isSelected, false);
					}
					avatarCanvas.redraw();
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		avatarCanvas.addMouseMoveListener(new MouseMoveListener() {
			private boolean lastActiveState = false;

			private String lastTooltipText = avatarCanvas.getToolTipText();

			public void mouseMove(MouseEvent e) {
				if (e.stateMask == SWT.MOD1) {
					return;
				}

				/*
				 * Optimization employed to minimize how often the tooltip text is updated;
				 * updating too frequently causes the tooltip to 'stick' to the cursor which
				 * can be annoying
				 */
				String tooltipText = "";
				if (decoratorBounds.contains(e.x, e.y)) {
					if (true == isEditMode()) {
						tooltipText = "Remove";
					} else if (true == isShareMode() && false == sharedAlready) {
						tooltipText = "Add to share";
					} else {
						tooltipText = vuzeBuddy.getLoginID();
					}
				} else {
					tooltipText = vuzeBuddy.getLoginID();
				}

				if (false == tooltipText.equals(lastTooltipText)) {
					avatarCanvas.setToolTipText(tooltipText);
					lastTooltipText = tooltipText;
				}

				if (e.y > avatarImageSize.y) {
					if (false == lastActiveState) {
						nameLinkActive = true;
						avatarCanvas.redraw();
						lastActiveState = true;
					}
				} else {
					if (true == lastActiveState) {
						nameLinkActive = false;
						avatarCanvas.redraw();
						lastActiveState = false;
					}
				}

			}
		});
	}

	private void doRemoveBuddy() {
		MessageBox mBox = new MessageBox(parent.getShell(), SWT.ICON_QUESTION
				| SWT.YES | SWT.NO);
		mBox.setMessage("Really delete?");
		if (SWT.NO == mBox.open()) {
			return;
		}
		VuzeBuddyManager.removeBuddy(vuzeBuddy);
	}

	private void doAddBuddyToShare() {
		viewer.addToShare(this);
		sharedAlready = true;
		avatarCanvas.redraw();
		avatarCanvas.update();
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
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null != uiFunctions) {
			String url = getVuzeBuddy().getProfileUrl("buddy-bar");
			uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0, true,
					true);
		}
	}

	public Control getControl() {
		return avatarCanvas;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
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
		if (null != avatarCanvas && false == avatarCanvas.isDisposed()) {
			avatarCanvas.redraw();
		}

	}

	public boolean isEditMode() {
		return isEditMode;
	}

	public void setEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
	}

	public boolean isShareMode() {
		return isShareMode;
	}

	public void setShareMode(boolean isShareMode) {
		this.isShareMode = isShareMode;

		if (false == isShareMode) {
			setSharedAlready(false);
		}
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

	public void dispose(boolean animate) {
		if (null != avatarCanvas && false == avatarCanvas.isDisposed()) {
			if (true == animate) {
				parent.getDisplay().asyncExec(new AERunnable() {

					public void runSupport() {

						while (alpha > 20) {
							alpha -= 30;
							avatarCanvas.redraw();
							avatarCanvas.update();

							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						avatarCanvas.dispose();
					}
				});
			} else {
				avatarCanvas.dispose();
			}

		}
	}

	public boolean isSharedAlready() {
		return sharedAlready;
	}

	public void setSharedAlready(boolean sharedAlready) {
		this.sharedAlready = sharedAlready;
	}

	public void setVuzeBuddy(VuzeBuddySWT vuzeBuddy) {
		if (this.vuzeBuddy != vuzeBuddy && null != vuzeBuddy) {
			this.vuzeBuddy = vuzeBuddy;

			/*
			 * Resets the image and image bounds since this is the only info cached;
			 * all other info is asked for on-demand so no need to update them 
			 */
			avatarImage = vuzeBuddy.getAvatarImage();
			if (null == avatarImage) {
				avatarImage = ImageRepository.getImage("buddy_default_avatar");
			}
			avatarBounds = null == avatarImage ? null : avatarImage.getBounds();
		}
	}
}
