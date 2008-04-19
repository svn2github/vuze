package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;

public class AvatarWidget
{
	private Canvas avatarCanvas = null;

	private Composite parent = null;

	private int borderWidth = 0;

	private Point avatarImageSize = null;

	private Point avatarSize = null;

	private VuzeBuddySWT vuzeBuddy = null;

	private int imageOffsetX = 0;

	private boolean isActivated = false;

	private boolean isSelected = false;

	private boolean showInfo = false;

	public AvatarWidget(Composite parent, Point avatarSize,
			Point avatarImageSize, VuzeBuddySWT vuzeBuddy) {

		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException(
					"The variable 'parent' can not be null or disposed");
		}

		this.parent = parent;
		this.avatarSize = avatarSize;
		this.avatarImageSize = avatarImageSize;
		this.vuzeBuddy = vuzeBuddy;

		init();
	}

	private void init() {

		final Image infoImage = parent.getDisplay().getSystemImage(
				SWT.ICON_INFORMATION);

		imageOffsetX = (avatarSize.x / 2) - (avatarImageSize.x / 2);

		avatarCanvas = new Canvas(parent, SWT.NONE);
		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarCanvas.setLayoutData(rData);

		final Region region = new Region();
		region.add(new Rectangle(imageOffsetX, 0, avatarImageSize.x,
				avatarImageSize.y));
		region.add(new Rectangle(0, avatarImageSize.y, avatarSize.x, avatarSize.y));
		avatarCanvas.setRegion(region);

		final Image avatar = vuzeBuddy.getAvatarImage();
		final Rectangle bounds = avatar.getBounds();
		avatarCanvas.addPaintListener(new PaintListener() {

			private boolean currentSelectedState = false;

			private boolean currentActivatedState = false;

			public void paintControl(PaintEvent e) {

				e.gc.setAntialias(SWT.ON);
				//				e.gc.c
				if (true == isActivated) {
					//					if (currentSelectedState != isSelected) {
					currentSelectedState = isSelected;
					if (true == isSelected) {
						e.gc.setBackground(Colors.green);
					} else {
						e.gc.setBackground(Colors.red);
					}

					e.gc.setAlpha(128);

					e.gc.fillRectangle(0, 0, avatarCanvas.getBounds().width,
							avatarCanvas.getBounds().height);

					e.gc.drawLine(imageOffsetX + 1, avatarImageSize.y, imageOffsetX + 1
							+ avatarImageSize.x, avatarImageSize.y);

					e.gc.setAlpha(255);
					e.gc.setBackground(avatarCanvas.getBackground());

					//					}

				} else {
					if (true == isSelected) {
						e.gc.setBackground(Colors.green);
						e.gc.setAlpha(128);

						e.gc.fillRectangle(0, 0, avatarCanvas.getBounds().width,
								avatarCanvas.getBounds().height);

						e.gc.drawLine(imageOffsetX + 1, avatarImageSize.y, imageOffsetX + 1
								+ avatarImageSize.x, avatarImageSize.y);

						e.gc.setAlpha(255);
						e.gc.setBackground(avatarCanvas.getBackground());
					}
				}

				e.gc.drawImage(avatar, 0, 0, bounds.width, bounds.height, imageOffsetX
						+ borderWidth, borderWidth, avatarImageSize.x - (2 * borderWidth),
						avatarImageSize.y - (2 * borderWidth));

				if (true == isActivated) {
					e.gc.drawImage(infoImage, 0, 0, infoImage.getBounds().width,
							infoImage.getBounds().height, 81, 0, 16, 16);
				}

				e.gc.setForeground(Colors.white);
				Point textExtent = e.gc.textExtent(vuzeBuddy.getDisplayName());

				int textOffset = (avatarSize.x / 2) - (textExtent.x / 2);
				e.gc.drawText(vuzeBuddy.getDisplayName(), textOffset,
						avatarImageSize.y + 2, true);
			}
		});

		avatarCanvas.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (null != region && false == region.isDisposed()) {
					region.dispose();
				}
				
				if (null != infoImage && false == infoImage.isDisposed()) {
					infoImage.dispose();
				}
			}
		});

		avatarCanvas.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {
			}

			public void mouseExit(MouseEvent e) {
				isActivated = false;
				showInfo = false;
				region.subtract(new Rectangle(79, 0, 18, 16));
				avatarCanvas.setRegion(region);
				avatarCanvas.redraw();
			}

			public void mouseEnter(MouseEvent e) {
				isActivated = true;
				showInfo = true;
				region.add(new Rectangle(79, 0, 18, 16));
				avatarCanvas.setRegion(region);
				avatarCanvas.redraw();
			}
		});

		avatarCanvas.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				//				isSelected=false;
				//				avatarCanvas.redraw();
			}

			public void mouseDown(MouseEvent e) {
				isSelected = !isSelected;
				avatarCanvas.redraw();
				if (e.stateMask == SWT.CONTROL) {
					System.out.println("CTRL is in effect");
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
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

	public Control getControl() {
		return avatarCanvas;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}
}
