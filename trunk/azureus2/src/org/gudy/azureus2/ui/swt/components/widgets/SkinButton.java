package org.gudy.azureus2.ui.swt.components.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;

public abstract class SkinButton
	extends Canvas
{

	protected static String imagePath = "com/aelitis/azureus/ui/images/";

	public static final int WIDGET_STATE_NORMAL = 0;

	public static final int WIDGET_STATE_NOT_VISIBLE = 1;

	public static final int WIDGET_STATE_DISABLED = 2;

	public static final int WIDGET_STATE_HOVER = 3;

	private int currentState = WIDGET_STATE_NORMAL;

	private Image buttonImage = null;

	private String buttonText = null;

	private int alpha = 255;

	private Inset inset = new Inset(0, 0, 0, 0);

	private Color foregroundHover = null;

	private Color foregroundDisabled = null;

	private boolean enabled = true;

	public SkinButton(Composite parent) {
		this(parent, null, null);
	}

	public SkinButton(Composite parent, String buttonText) {
		this(parent, buttonText, null);
	}

	public SkinButton(Composite parent, String buttonText, Image buttonImage) {
		super(parent, SWT.DOUBLE_BUFFERED | SWT.INHERIT_DEFAULT);
		this.buttonImage = buttonImage;
		this.buttonText = buttonText;

		init();
	}

	private void init() {
		setCursor(Cursors.handCursor);
		addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				try {
					e.gc.setAntialias(SWT.ON);
					e.gc.setTextAntialias(SWT.ON);
					e.gc.setAlpha(alpha);
					e.gc.setInterpolation(SWT.HIGH);
				} catch (Exception ex) {
					// ignore.. some of these may not be avail
				}

				/*
				 * Paint background
				 */

				Image[] images = getCurrentBackgroundImages();
				if (null != images) {
					if (images.length != 3) {
						/*
						 * If all three images are not specified then just draw the first one
						 */
						if (null != images[0] && false == images[0].isDisposed()) {
							ImageData iData = images[0].getImageData();
							e.gc.drawImage(images[0], 0, 0, iData.width, iData.height, 0, 0,
									iData.width, iData.height);
						}
					}
					/*
					 * Draw the left, right, and tile the middle images to fill the space in between
					 */
					else {

						int leftOffset = 0;
						int rightOffset = 0;

						/*
						 * Draw 'left' image as-is from the left;
						 * this will be drawn on top of the tiled 'center' image above
						 */
						if (null != images[0] && false == images[0].isDisposed()) {
							ImageData iData = images[0].getImageData();
							e.gc.drawImage(images[0], 0, 0, iData.width, iData.height, 0, 0,
									iData.width, iData.height);
							leftOffset = iData.width;
						}

						/*
						 * Draw 'right' image as-is on the right;
						 * this will be drawn on top of the tiled 'center' image above
						 */
						if (null != images[2] && false == images[2].isDisposed()) {
							ImageData iData = images[2].getImageData();
							rightOffset = getSize().x - iData.width;

							e.gc.drawImage(images[2], 0, 0, iData.width, iData.height,
									rightOffset, 0, iData.width, iData.height);
						}

						/*
						 * Tile 'center' image across the entire canvas 
						 */
						if (null != images[1] && false == images[1].isDisposed()) {
							ImageData iData = images[1].getImageData();
							int iterations = (rightOffset - leftOffset) / iData.width;
							int roundingOverflow = (rightOffset - leftOffset)
									- (iterations * iData.width);

							for (int i = 0; i < iterations; i++) {
								e.gc.drawImage(images[1], 0, 0, iData.width, iData.height,
										leftOffset + (i * iData.width), 0, iData.width,
										iData.height);
							}

							if (roundingOverflow > 0) {
								e.gc.drawImage(images[1], 0, 0, roundingOverflow, iData.height,
										leftOffset + (iterations * iData.width), 0,
										roundingOverflow, iData.height);
							}
						}

					}
				}

				/*
				 * Paint the image if there is one
				 */
				int imageOffset = 0;
				if (null != getImage() && false == getImage().isDisposed()) {
					ImageData iData = getImage().getImageData();
					e.gc.drawImage(getImage(), 0, 0, iData.width, iData.height,
							inset.left, inset.top, iData.width, iData.height);
					imageOffset = inset.left + iData.width;
				}

				/*
				 * Paints the text if there is one
				 */
				if (null != getText()) {

					if (currentState == WIDGET_STATE_DISABLED) {
						if (null != getForegroundColors()[2]) {
							e.gc.setForeground(getForegroundColors()[2]);
						}
					} else if (currentState == WIDGET_STATE_HOVER) {
						if (null != getForegroundColors()[1]) {
							e.gc.setForeground(getForegroundColors()[1]);
						}
					} else if (null != getForegroundColors()[0]) {
						e.gc.setForeground(getForegroundColors()[0]);
					} else {
						e.gc.setForeground(getForeground());
					}
					if (imageOffset != 0) {
						imageOffset += 6;
						Point extent = e.gc.textExtent(getText());
						e.gc.drawText(getText(), imageOffset, (getSize().y / 2)
								- (extent.y / 2), SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC);
					} else {
						Point extent = e.gc.textExtent(getText());
						e.gc.drawText(getText(), (getSize().x / 2) - (extent.x / 2),
								(getSize().y / 2) - (extent.y / 2), SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC);
					}
				}
			}
		});

		addMouseTrackListener(new MouseTrackAdapter() {

			public void mouseEnter(MouseEvent e) {
				if (currentState != WIDGET_STATE_DISABLED) {
					if (currentState != WIDGET_STATE_HOVER) {
						currentState = WIDGET_STATE_HOVER;
						refreshVisuals();
					}
				}
			}

			public void mouseExit(MouseEvent e) {
				if (currentState != WIDGET_STATE_DISABLED) {
					if (currentState != WIDGET_STATE_NORMAL) {
						currentState = WIDGET_STATE_NORMAL;
						refreshVisuals();
					}
				}
			}

		});

	}

	/**
	 * Computes the optimal size to fit either/or the image, text, and background; whichever is larger
	 */
	public Point computeSize(int hint, int hint2, boolean changed) {
		Point backgroundExtent = new Point(0, 0);
		Point imageExtent = new Point(0, 0);
		Point textExtent = new Point(0, 0);

		if (null != getImage()) {
			imageExtent.x = getImage().getImageData().width;
			imageExtent.y = getImage().getImageData().height;
		}

		if (null != getText()) {
			GC gc = new GC(getDisplay());
			textExtent = gc.textExtent(getText());
			gc.dispose();
		}

		Image[] bgImages = getBackgroundImages();
		if (null != bgImages) {
			if (null != bgImages[0]) {
				backgroundExtent.x = bgImages[0].getImageData().width;
				backgroundExtent.y = bgImages[0].getImageData().height;
			}
			if (null != bgImages[1]) {
				backgroundExtent.x += bgImages[1].getImageData().width;
				backgroundExtent.y = bgImages[1].getImageData().height;
			}
			if (null != bgImages[2]) {
				backgroundExtent.x += bgImages[2].getImageData().width;
				backgroundExtent.y = bgImages[2].getImageData().height;
			}

		}

		int maxHeight = Math.max(backgroundExtent.y, imageExtent.y + inset.top
				+ inset.bottom);
		maxHeight = Math.max(maxHeight, textExtent.y + inset.top + inset.bottom);

		int maxWidth = Math.max(backgroundExtent.x, imageExtent.x + 6
				+ textExtent.x + inset.left + inset.right);

		maxWidth = Math.max(maxWidth, imageExtent.x + inset.left + inset.right);
		maxWidth = Math.max(maxWidth, textExtent.x + inset.left + inset.right);

		return new Point(maxWidth, maxHeight);
	}

	private Image[] getCurrentBackgroundImages() {
		if (currentState == WIDGET_STATE_DISABLED) {
			Image[] images = getBackgroundImages_disabled();
			if (null != images && images.length > 0) {
				return images;
			}
		} else if (currentState == WIDGET_STATE_HOVER) {
			Image[] images = getBackgroundImages_hover();
			if (null != images && images.length > 0) {
				return images;
			}
		}

		/*
		 * Defaults to same background
		 */
		Image[] images = getBackgroundImages();
		if (null != images && images.length > 0) {
			return images;
		}

		return null;
	}

	public Image getImage() {
		return buttonImage;
	}

	public void setImage(Image buttonImage) {
		if (this.buttonImage != buttonImage) {
			this.buttonImage = buttonImage;
			refreshVisuals();
		}
	}

	public String getText() {
		return buttonText;
	}

	public void setText(String buttonText) {
		if (this.buttonText != buttonText) {
			this.buttonText = buttonText;
			refreshVisuals();
		}
	}

	public abstract Image[] getBackgroundImages();

	public abstract Image[] getBackgroundImages_disabled();

	public abstract Image[] getBackgroundImages_hover();

	public abstract Color[] getForegroundColors();

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			super.setEnabled(enabled);
			if (false == enabled) {
				currentState = WIDGET_STATE_DISABLED;
				alpha = 128;
			} else {
				currentState = WIDGET_STATE_NORMAL;
				alpha = 255;
			}
			refreshVisuals();
			update();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Inset getInset() {
		return inset;
	}

	public void setInset(Inset inset) {
		if (this.inset != inset) {
			this.inset = inset;
			refreshVisuals();
		}

	}

	public void refreshVisuals() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (false == isDisposed()) {
					redraw();
				}
			}
		}, false);
	}

	public Color getForegroundHover() {
		return foregroundHover;
	}

	public void setForegroundHover(Color foregroundHover) {
		this.foregroundHover = foregroundHover;
	}

	public Color getForegroundDisabled() {
		return foregroundDisabled;
	}

	public void setForegroundDisabled(Color foregroundDisabled) {
		this.foregroundDisabled = foregroundDisabled;
	}

}
