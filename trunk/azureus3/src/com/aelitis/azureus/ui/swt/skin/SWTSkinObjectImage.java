/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AECallback;
import org.gudy.azureus2.core3.util.AERunnableWithCallback;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ImageLoader;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class SWTSkinObjectImage
	extends SWTSkinObjectBasic
{
	private static boolean ALWAYS_USE_PAINT = false;

	Label label;

	private boolean customImage;

	private String customImageID;

	private static PaintListener tilePaintListener;

	private boolean noSetLabelImage = false;

	static {
		tilePaintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.setAdvanced(true);

				Label label = (Label) e.widget;
				Image imgSrc = (Image) label.getData("image");
				if (imgSrc == null) {
					return;
				}
				Rectangle imgSrcBounds = imgSrc.getBounds();
				Point size = label.getSize();

				int x0 = 0;
				int y0 = 0;
				int x1 = size.x;
				int y1 = size.y;

				Image imgRight = (Image) label.getData("image-right");
				if (imgRight != null) {
					int width = imgRight.getBounds().width;

					x1 -= width;
				}

				Image imgLeft = (Image) label.getData("image-left");
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
			int align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
			if (align != SWT.NONE) {
				style |= align;
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
		Image image = imageLoader.getImage(sImageID);
		if (!ImageLoader.isRealImage(image)) {
			sImageID = sConfigID + ".image";
			image = imageLoader.getImage(sImageID);
		}

		if (ImageLoader.isRealImage(image)) {
			setLabelImage(sConfigID, sImageID, null);
		}

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

				if (sImageID.equals(label.getData("ImageID"))) {
					return label.getImage();
				}

				ImageLoader imageLoader = skin.getImageLoader(properties);
				Image image = imageLoader.getImage(sImageID);

				Image imageLeft = imageLoader.getImage(sImageID + ".left");
				if (ImageLoader.isRealImage(imageLeft)) {
					label.setData("image-left", imageLeft);
				}
				Image imageRight = imageLoader.getImage(sImageID + ".right");
				if (ImageLoader.isRealImage(imageRight)) {
					label.setData("image-right", imageRight);
				}

				String sDrawMode = properties.getStringValue(sConfigID + ".drawmode");
				if (sDrawMode == null) {
					sDrawMode = "";
				}

				//allowImageDimming = sDrawMode.equalsIgnoreCase("dim");

				if (sDrawMode.equalsIgnoreCase("tile") || ALWAYS_USE_PAINT) {
					noSetLabelImage = true;
					Rectangle imgBounds = image.getBounds();
					label.setSize(imgBounds.width, imgBounds.height);
					label.setData("image", image);

					// XXX Huh? A tile of one? :)
					FormData fd = (FormData) label.getLayoutData();
					if (fd == null) {
						fd = new FormData(imgBounds.width, imgBounds.height);
					} else {
						fd.width = imgBounds.width;
						fd.height = imgBounds.height;
					}
					label.setLayoutData(fd);

					// remove in case already added
					label.removePaintListener(tilePaintListener);

					label.addPaintListener(tilePaintListener);

					label.setImage(null);
				} else {
					Image oldImage = label.getImage();
					label.setImage(image);
					label.setData("ImageID", sImageID);
					if (oldImage != null && image != null
							&& !oldImage.getBounds().equals(image.getBounds())) {
						Utils.relayout(label);
					}
				}

				label.redraw();

				SWTSkinUtils.addMouseImageChangeListeners(label);
				return image;
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setBackground(java.lang.String, java.lang.String)
	public void setBackground(String sConfigID, String sSuffix) {
		// No background for images?
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#switchSuffix(java.lang.String)
	public String switchSuffix(String suffix, int level, boolean walkUp) {
		suffix = super.switchSuffix(suffix, level, walkUp);
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
		Image image = imageLoader.getImage(sImageID);
		if (image != ImageLoader.noImage) {
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
		label.setData("Image", image);
		label.setData("image-left", null);
		label.setData("image-right", null);
		if (!noSetLabelImage) {
			label.setImage(image);
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
		return;
	}
}
