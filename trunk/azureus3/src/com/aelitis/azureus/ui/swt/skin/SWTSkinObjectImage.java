/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class SWTSkinObjectImage
	extends SWTSkinObjectBasic
{
	protected static final Long DRAW_SCALE = new Long(1);

	protected static final Long DRAW_STRETCH = new Long(2);

	protected static final Long DRAW_NORMAL = new Long(0);

	protected static final Long DRAW_TILE = new Long(3);

	protected static final Long DRAW_CENTER = new Long(4);

	protected static final Long DRAW_HCENTER = new Long(5);

	private static boolean ALWAYS_USE_PAINT = true;

	Label label;

	private boolean customImage;

	private String customImageID;

	private String currentImageID;

	private static PaintListener paintListener;

	private boolean noSetLabelImage = false;

	private int h_align;

	static {
		paintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {

				try {
					e.gc.setAdvanced(true);
					e.gc.setInterpolation(SWT.HIGH);
				} catch (Exception ex) {
				}

				Label label = (Label) e.widget;
				Image imgSrc = (Image) label.getData("image");
				Image imgRight = null;
				Image imgLeft = null;
				String idToRelease = null;
				ImageLoader imageLoader = null;

				if (imgSrc == null) {
					SWTSkinObjectImage soImage = (SWTSkinObjectImage) label.getData("SkinObject");
					imageLoader = soImage.getSkin().getImageLoader(
							soImage.getProperties());
					String imageID = (String) label.getData("ImageID");
					if (imageLoader.imageExists(imageID)) {
						idToRelease = imageID;
						Image[] images = imageLoader.getImages(imageID);
						if (images.length == 3) {
							imgLeft = images[0];
							imgSrc = images[1];
							imgRight = images[2];
						} else {
							imgSrc = images[0];
						}
					} else {
						return;
					}
				}
				Rectangle imgSrcBounds = imgSrc.getBounds();
				Point size = label.getSize();

				Long drawMode = (Long) label.getData("drawmode");

				if (drawMode == DRAW_STRETCH) {
					e.gc.drawImage(imgSrc, 0, 0, imgSrcBounds.width, imgSrcBounds.height,
							0, 0, size.x, size.y);
				} else if (drawMode == DRAW_CENTER || drawMode == DRAW_NORMAL) {
					e.gc.drawImage(imgSrc, (size.x - imgSrcBounds.width) / 2,
							(size.y - imgSrcBounds.height) / 2);
				} else if (drawMode == DRAW_HCENTER) {
					e.gc.drawImage(imgSrc, (size.x - imgSrcBounds.width) / 2, 0);
				} else if (drawMode == DRAW_SCALE) {
					// TODO: real scale..
					e.gc.drawImage(imgSrc, 0, 0, imgSrcBounds.width, imgSrcBounds.height,
							0, 0, size.x, size.y);
				} else {
					int x0 = 0;
					int y0 = 0;
					int x1 = size.x;
					int y1 = size.y;

					if (imgRight == null) {
						imgRight = (Image) label.getData("image-right");
					}
					if (imgRight != null) {
						int width = imgRight.getBounds().width;

						x1 -= width;
					}

					if (imgLeft == null) {
						imgLeft = (Image) label.getData("image-left");
					}
					if (imgLeft != null) {
						// TODO: Tile down
						e.gc.drawImage(imgLeft, 0, 0);

						x0 += imgLeft.getBounds().width;
					}

					for (int y = y0; y < y1; y += imgSrcBounds.height) {
						for (int x = x0; x < x1; x += imgSrcBounds.width) {
							e.gc.drawImage(imgSrc, x, y);
						}
					}

					if (imgRight != null) {
						// TODO: Tile down
						e.gc.drawImage(imgRight, x1, 0);
					}
				}
				if (idToRelease != null && imageLoader != null) {
					imageLoader.releaseImage(idToRelease);
				}
			}
		};
	}

	/**
	 * @param skin 
	 * 
	 */
	public SWTSkinObjectImage(SWTSkin skin, SWTSkinProperties skinProperties,
			String sID, String sConfigID, String sImageID, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "image", parent);
		setControl(createImageLabel(sConfigID, sImageID));
		customImage = false;
		customImageID = null;
	}

	private Label createImageLabel(String sConfigID, String sImageID) {
		int style = SWT.WRAP;

		String sAlign = properties.getStringValue(sConfigID + ".align");
		if (sAlign != null) {
			h_align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
			if (h_align != SWT.NONE) {
				style |= h_align;
			}
		}

		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style |= SWT.BORDER;
		}

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		label = new Label(createOn, style);
		label.setData("SkinObject", this);

		Color color = properties.getColor(sConfigID + ".color");
		if (color != null) {
			label.setBackground(color);
		}

		final String sURL = properties.getStringValue(sConfigID + ".url");
		if (sURL != null && sURL.length() > 0) {
			label.setToolTipText(sURL);
			label.addListener(SWT.MouseUp, new Listener() {
				public void handleEvent(Event arg0) {
					Utils.launch(UrlUtils.encode(sURL));
				}
			});
		}

		String sCursor = properties.getStringValue(sConfigID + ".cursor");
		if (sCursor != null && sCursor.length() > 0) {
			if (sCursor.equalsIgnoreCase("hand")) {
				label.addListener(SWT.MouseEnter,
						skin.getHandCursorListener(label.getDisplay()));
				label.addListener(SWT.MouseExit,
						skin.getHandCursorListener(label.getDisplay()));
			}
		}

		ImageLoader imageLoader = skin.getImageLoader(properties);
		boolean imageExists = imageLoader.imageExists(sImageID);
		if (!imageExists) {
			sImageID = sConfigID + ".image";
			imageExists = imageLoader.imageExists(sImageID);
		}

		if (imageExists) {
			setLabelImage(sConfigID, sImageID, null);
		}

		label.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				String oldImageID = (String) label.getData("ImageID");
				ImageLoader imageLoader = skin.getImageLoader(properties);
				if (oldImageID != null && label.getData("image") != null) {
					imageLoader.releaseImage(oldImageID);
				}
			}
		});

		//		SWTBGImagePainter painter = (SWTBGImagePainter) parent.getData("BGPainter");
		//		if (painter != null) {
		//			label.addListener(SWT.Paint, painter);
		//		}

		return label;
	}

	//protected void setLabelImage(String sConfigID, AECallback<Image> callback) {
	protected void setLabelImage(String sImageID, AECallback callback) {
		setLabelImage(sConfigID, sImageID, callback);
	}

	//private void setLabelImage(final String sConfigID, final String sImageID, AECallback<Image> callback) {
	private void setLabelImage(final String sConfigID, final String sImageID,
			AECallback callback) {
		Utils.execSWTThread(new AERunnableWithCallback(callback) {

			public Object runSupport() {
				if (label == null || label.isDisposed()) {
					return null;
				}

				String oldImageID = (String) label.getData("ImageID");
				if (sImageID != null && sImageID.equals(oldImageID)) {
					return null;
				}

				ImageLoader imageLoader = skin.getImageLoader(properties);

				if (oldImageID != null && label.getData("image") != null) {
					imageLoader.releaseImage(oldImageID);
				}

				Image[] images = sImageID == null || sImageID.length() == 0 ? null
						: imageLoader.getImages(sImageID);

				Image image = null;

				if (images.length == 3) {
					Image imageLeft = images[0];
					if (ImageLoader.isRealImage(imageLeft)) {
						label.setData("image-left", imageLeft);
					}

					image = images[1];

					Image imageRight = images[2];
					if (ImageLoader.isRealImage(imageRight)) {
						label.setData("image-right", imageRight);
					}
				} else if (images.length > 0) {
					image = images[0];
				}

				if (image == null) {
					image = ImageLoader.noImage;
				}

				String sDrawMode = properties.getStringValue(sConfigID + ".drawmode");
				if (sDrawMode == null) {
					sDrawMode = properties.getStringValue(
							SWTSkinObjectImage.this.sConfigID + ".drawmode", "");
				}

				//allowImageDimming = sDrawMode.equalsIgnoreCase("dim");

				Long drawMode;
				if (sDrawMode.equals("scale")) {
					drawMode = DRAW_SCALE;
				} else if (sDrawMode.equals("stretch")) {
					drawMode = DRAW_STRETCH;
				} else if (sDrawMode.equals("center")) {
					drawMode = DRAW_CENTER;
				} else if (sDrawMode.equals("h-center")) {
					drawMode = DRAW_HCENTER;
				} else if (sDrawMode.equalsIgnoreCase("tile")) {
					drawMode = DRAW_TILE;
				} else {
					drawMode = DRAW_NORMAL;
				}
				label.setData("drawmode", drawMode);

				if (drawMode != DRAW_NORMAL || ALWAYS_USE_PAINT) {
					noSetLabelImage = true;
					Rectangle imgBounds = image.getBounds();
					if (drawMode != DRAW_CENTER && drawMode != DRAW_HCENTER
							&& drawMode != DRAW_STRETCH) {
						label.setSize(imgBounds.width, imgBounds.height);
					}
					//label.setData("image", image);

					if (drawMode == DRAW_TILE || drawMode == DRAW_NORMAL) {
						// XXX Huh? A tile of one? :)
						FormData fd = (FormData) label.getLayoutData();
						if (fd == null) {
							fd = new FormData(imgBounds.width, imgBounds.height);
						} else {
							fd.width = imgBounds.width;
							fd.height = imgBounds.height;
						}
						label.setLayoutData(fd);
						Utils.relayout(label);
					}

					// remove in case already added
					label.removePaintListener(paintListener);

					label.addPaintListener(paintListener);

					label.setImage(null);
				} else if (sDrawMode.equals(("scale"))) {
					noSetLabelImage = true;
					Rectangle imgBounds = image.getBounds();
					label.setSize(imgBounds.width, imgBounds.height);
					label.setData("image", image);

				} else {
					Image oldImage = label.getImage();
					label.setImage(image);
					if (oldImage == null || image == null
							|| !oldImage.getBounds().equals(image.getBounds())) {
						Utils.relayout(label);
					}
				}
				label.setData("ImageID", sImageID);

				label.redraw();

				SWTSkinUtils.addMouseImageChangeListeners(label);
				imageLoader.releaseImage(sImageID);
				return null;
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setBackground(java.lang.String, java.lang.String)
	public void setBackground(String sConfigID, String sSuffix) {
		// No background for images?
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#switchSuffix(java.lang.String)
	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);
		if (customImage) {
			return suffix;
		}
		if (suffix == null) {
			return null;
		}

		String sImageID = (customImageID == null ? (sConfigID + ".image")
				: customImageID)
				+ suffix;

		ImageLoader imageLoader = skin.getImageLoader(properties);
		boolean imageExists = imageLoader.imageExists(sImageID);
		if (!imageExists) {
			for (int i = suffixes.length - 1; i >= 0; i--) {
				String suffixToRemove = suffixes[i];
				if (suffixToRemove != null) {
					sImageID = sImageID.substring(0, sImageID.length()
							- suffixToRemove.length());
					if (imageLoader.imageExists(sImageID)) {
						imageExists = true;
						break;
					}
				}
			}
		}

		if (imageExists) {
			setLabelImage(sImageID, null);
		}
		return suffix;
	}

	public Image getImage() {
		return label.getImage();
	}

	public void setImage(Image image) {
		customImage = true;
		customImageID = null;
		label.setData("image", image);
		label.setData("image-left", null);
		label.setData("image-right", null);
		if (!noSetLabelImage) {
			label.setImage(image);
		} else {
			label.redraw();
		}
		Utils.relayout(label);
	}

	protected void setImageByID(String sConfigID, AECallback callback) {
		if (customImage == false && customImageID != null
				&& customImageID.equals(sConfigID)) {
			if (callback != null) {
				callback.callbackFailure(null);
			}
			return;
		}
		customImage = false;
		customImageID = sConfigID;

		String sImageID = sConfigID + getSuffix();
		ImageLoader imageLoader = skin.getImageLoader(properties);
		Image image = imageLoader.getImage(sImageID);
		if (ImageLoader.isRealImage(image)) {
			setLabelImage(sConfigID, sImageID, callback);
		} else {
			setLabelImage(sConfigID, sConfigID, callback);
		}
		imageLoader.releaseImage(sImageID);
		return;
	}

	public void setImageUrl(final String url) {
		if (customImage == false && customImageID != null
				&& customImageID.equals(url)) {
			return;
		}
		customImage = false;
		customImageID = url;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final ImageLoader imageLoader = skin.getImageLoader(properties);
				imageLoader.getUrlImage(url, new ImageDownloaderListener() {
					public void imageDownloaded(Image image, boolean returnedImmediately) {
						setLabelImage(url, null);
						imageLoader.releaseImage(url);
					}
				});
			}
		});
	}
}
