/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.ui.swt.utils.ImageLoader;

/**
 * This class does not work.  Do not use it
 * 
 * 
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class SWTSkinObjectImageContainer extends SWTSkinObjectContainer
{
	Composite composite;

	private static PaintListener tilePaintListener;

	static {
		tilePaintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				System.out.println("moo");
				Composite c = (Composite) e.widget;
				Image imgSrc = (Image) c.getData("image");
				if (imgSrc == null) {
					return;
				}
				Rectangle imgSrcBounds = imgSrc.getBounds();
				Point size = c.getSize();

				int x0 = 0;
				int y0 = 0;
				int x1 = size.x;
				int y1 = size.y;

				Image imgRight = (Image) c.getData("image-right");
				if (imgRight != null) {
					int width = imgRight.getBounds().width;

					x1 -= width;
				}

				Image imgLeft = (Image) c.getData("image-left");
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
	public SWTSkinObjectImageContainer(SWTSkin skin,
			SWTSkinProperties skinProperties, String sID, String sConfigID,
			SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, parent);

		composite = (Composite) getControl();
		setLabelImage(sConfigID, sConfigID + ".image");
	}

	protected Image setLabelImage(String sConfigID) {
		return setLabelImage(sConfigID, sConfigID);
	}

	private Image setLabelImage(String sConfigID, String sImageID) {
		if (composite == null || composite.isDisposed()) {
			return null;
		}

		ImageLoader imageLoader = skin.getImageLoader(properties);
		Image image = imageLoader.getImage(sImageID);

		Image imageLeft = imageLoader.getImage(sImageID + ".left");
		if (ImageLoader.isRealImage(imageLeft)) {
			composite.setData("image-left", imageLeft);
		}
		Image imageRight = imageLoader.getImage(sImageID + ".right");
		if (ImageLoader.isRealImage(imageRight)) {
			composite.setData("image-right", imageRight);
		}

		String sDrawMode = properties.getStringValue(sImageID + ".drawmode");
		if (sDrawMode == null) {
			sDrawMode = "";
		}

		System.out.println("Hello");
		if (sDrawMode.equalsIgnoreCase("tile")) {
			Rectangle imgBounds = image.getBounds();
			composite.setSize(imgBounds.width, imgBounds.height);
			composite.setData("image", image);

			// XXX Huh? A tile of one? :)
			composite.setLayoutData(new FormData(imgBounds.width, imgBounds.height));

			// remove in case already added
			composite.removePaintListener(tilePaintListener);

			composite.addPaintListener(tilePaintListener);
		} else {
			//composite.setImage(image);
			composite.setData("ImageID", sImageID);
		}

		SWTSkinUtils.addMouseImageChangeListeners(composite);
		return image;
	}

	//	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setBackground(java.lang.String, java.lang.String)
	//	public void setBackground(String sConfigID, String sSuffix) {
	//		// No background for images?
	//	}
	//
	//	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#switchSuffix(java.lang.String)
	//	public String switchSuffix(String suffix, int level, boolean walkUp) {
	//		suffix = super.switchSuffix(suffix, level, walkUp);
	//		if (suffix == null) {
	//			return null;
	//		}
	//
	//		String sImageID = sConfigID + ".image" + suffix;
	//
	//		ImageLoader imageLoader = skin.getImageLoader(properties);
	//		Image image = imageLoader.getImage(sImageID);
	//		if (image != ImageLoader.noImage) {
	//			setLabelImage(sImageID);
	//		}
	//
	//		return suffix;
	//	}
}
