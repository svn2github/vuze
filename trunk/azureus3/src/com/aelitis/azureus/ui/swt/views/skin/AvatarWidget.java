package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;

import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;

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

	private boolean showInfo = false;

	private boolean nameLinkActive = false;

	private boolean isEditMode = false;

	private boolean isShareMode = false;

	private Color textColor = null;

	private Color textLinkColor = null;

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

//		avatarCanvas = new Canvas(parent, SWT.NONE);

		Utils.createTorrentDropTarget(avatarCanvas, true);

		//		Transfer[] types = new Transfer[] {
		//			TextTransfer.getInstance()
		//		};
		//
		//		DropTarget dt = new DropTarget(avatarCanvas, DND.DROP_COPY);
		//		dt.setTransfer(types);
		//		dt.addDropListener(new DropTargetListener() {
		//
		//			public void dropAccept(DropTargetEvent event) {
		//				// TODO Auto-generated method stub
		//
		//			}
		//
		//			public void drop(DropTargetEvent event) {
		//				// TODO Auto-generated method stub
		//
		//			}
		//
		//			public void dragOver(DropTargetEvent event) {
		//				if (false == isActivated) {
		//					isActivated = true;
		//					showInfo = true;
		//					region.add(new Rectangle(79, 0, 18, 16));
		//					avatarCanvas.setRegion(region);
		//					avatarCanvas.redraw();
		//				}
		//			}
		//
		//			public void dragOperationChanged(DropTargetEvent event) {
		//				// TODO Auto-generated method stub
		//
		//			}
		//
		//			public void dragLeave(DropTargetEvent event) {
		//				isActivated = false;
		//				showInfo = false;
		//				region.subtract(new Rectangle(79, 0, 18, 16));
		//				avatarCanvas.setRegion(region);
		//				avatarCanvas.redraw();
		//
		//			}
		//
		//			public void dragEnter(DropTargetEvent event) {
		//				// TODO Auto-generated method stub
		//
		//			}
		//		});

		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarCanvas.setLayoutData(rData);

		final Image avatar = vuzeBuddy.getAvatarImage();
		final Rectangle bounds = avatar.getBounds();
		avatarCanvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {

				e.gc.setAntialias(SWT.ON);

				/*
				 * Draw backgound if the widget is selected
				 */
				if (true == isSelected) {

					e.gc.setBackground(Colors.grey);
					e.gc.setAlpha(128);

					Rectangle bounds = avatarCanvas.getBounds();

					e.gc.fillRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1,
							10, 10);
					e.gc.setAlpha(255);
					e.gc.setBackground(avatarCanvas.getBackground());
				}

				/*
				 * Draw hightlight borders is the widget is activated (being hovered over)
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
				e.gc.drawImage(avatar, 0, 0, bounds.width, bounds.height, imageOffsetX
						+ borderWidth, borderWidth, avatarImageSize.x - (2 * borderWidth),
						avatarImageSize.y - (2 * borderWidth));

				if (true == isEditMode) {
					e.gc.drawImage(removeImage, 0, 0, removeImage.getBounds().width,
							removeImage.getBounds().height, 46, 0, 16, 16);
				}

				if (true == isShareMode) {
					e.gc.drawImage(add_to_share_Image, 0, 0,
							removeImage.getBounds().width, removeImage.getBounds().height,
							46, 18, 16, 16);
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
							new Rectangle(0, avatarImageSize.y, 64, 26), false, false,
							SWT.TOP | SWT.CENTER | SWT.WRAP);

				}
				else{
					System.err.println("Color is still null");//KN: sysout
				}
			}
		});

		avatarCanvas.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (null != removeImage && false == removeImage.isDisposed()) {
					removeImage.dispose();
				}
				if (null != add_to_share_Image
						&& false == add_to_share_Image.isDisposed()) {
					add_to_share_Image.dispose();
				}
			}
		});

		avatarCanvas.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {

			}

			public void mouseExit(MouseEvent e) {
				isActivated = false;
				showInfo = false;
				avatarCanvas.redraw();
			}

			public void mouseEnter(MouseEvent e) {
				if (false == isActivated) {
					isActivated = true;
					showInfo = true;
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

			public void mouseMove(MouseEvent e) {
				if (e.stateMask == SWT.MOD1) {
					return;
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

	public void doHover() {

	}

	public void doClick() {

	}

	public void doMouseEnter() {

	}

	public void doDoubleClick() {

	}

	public void doLinkClicked() {
		System.out.println("Link is clicked");
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
}
