package com.aelitis.azureus.ui.swt.views.skin;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.ClientMessageContext;

public class BlankDetailPage
	extends AbstractDetailPage
{
	private Composite content = null;

	private Canvas spinnerCanvas = null;

	private GC spinnerGC;

	private Image[] spinnerImages;

	private Rectangle spinnerBounds;

	private boolean isBusy;

	private boolean busyAlready;

	private Display display;

	public BlankDetailPage(DetailPanel detailPanel, String pageID) {
		super(detailPanel, pageID);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.INHERIT_DEFAULT);
		spinnerCanvas = new Canvas(content, SWT.NO_BACKGROUND);
		if (null == spinnerGC) {
			spinnerGC = new GC(spinnerCanvas);
			spinnerGC.setBackground(content.getBackground());
		}

		display = content.getDisplay();

		content.addControlListener(new ControlListener() {

			public void controlResized(ControlEvent e) {
				Utils.execSWTThread(new AERunnable() {

					public void runSupport() {
						if (true == isBusy) {
							Utils.centerRelativeTo(spinnerBounds, content.getBounds());
							//							Point to_lbShell = content.toControl(spinnerBounds.x,
							//									spinnerBounds.y);
							//							spinnerBounds.x = to_lbShell.x;
							//							spinnerBounds.y = to_lbShell.y;
							spinnerCanvas.setBounds(spinnerBounds);
							System.out.println("Spinner bounds: " + spinnerBounds);//KN: sysout
						}
					}
				});
			}

			public void controlMoved(ControlEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}

	public void showBusy(boolean value, long delayInMilli) {
		isBusy = value;

		if (true == isBusy && false == busyAlready) {
			showSpinner(Math.max(0, delayInMilli));
		}
	}

	private void showSpinner(final long delayInMilli) {

		/*
		 * Create the images off-line and store them in the array if not done already;
		 * we will use these to draw onto the canvas to animate the spinner
		 */
		if (null == spinnerImages) {
			InputStream is = ImageRepository.getImageAsStream("spinner_big");

			if (null == is) {
				return;
			}
			ImageLoader loader = new ImageLoader();
			ImageData[] imageDataArray = loader.load(is);
			spinnerBounds = new Rectangle(0, 0, loader.logicalScreenWidth,
					loader.logicalScreenHeight);

			spinnerImages = new Image[imageDataArray.length];
			for (int i = 0; i < imageDataArray.length; i++) {
				ImageData imageData = imageDataArray[i];
				/*
				 * Setting the transparent pixel to be black
				 */
				imageData.transparentPixel = 0;

				spinnerImages[i] = new Image(display, spinnerBounds.width,
						spinnerBounds.height);
				GC offScreenImageGC = new GC(spinnerImages[i]);
				offScreenImageGC.setBackground(content.getBackground());
				offScreenImageGC.fillRectangle(0, 0, spinnerBounds.width,
						spinnerBounds.height);

				Image tempImage = new Image(display, imageData);
				offScreenImageGC.drawImage(tempImage, 0, 0, imageData.width,
						imageData.height, imageData.x, imageData.y, imageData.width,
						imageData.height);

				tempImage.dispose();
				offScreenImageGC.dispose();
			}
		}
		spinnerCanvas.setBounds(spinnerBounds);

		/*
		 * Adjust the spinner bounds to be centered on the lightbox shell itself
		 */
		Utils.centerRelativeTo(spinnerBounds, content.getBounds());
		//		Point to_lbShell = content.toControl(spinnerBounds.x, spinnerBounds.y);
		//		spinnerBounds.x = to_lbShell.x;
		//		spinnerBounds.y = to_lbShell.y;

		/*
		 * Create the canvas for the spinner; size the canvas to be just enough for the image
		 */
		if (null == spinnerCanvas) {
			spinnerCanvas = new Canvas(content, SWT.NONE);
		}
		spinnerCanvas.setBounds(spinnerBounds);
		if (null == spinnerGC) {
			spinnerGC = new GC(spinnerCanvas);
			spinnerGC.setBackground(content.getBackground());
		}

		/*
		 * Spinner animation 
		 */

		AEThread2 spinnerThread = new AEThread2("spinner-animator", true) {
			public void run() {
				final int[] imageDataIndex = new int[1];
				busyAlready = true;

				/* 
				 * First we sleep for the specified delay before we start painting; if during this time
				 * isBusy is set to false (by another thread) then it's not necessary to show the spinner. 
				 */
				if (delayInMilli > 0) {
					try {
						Thread.sleep(delayInMilli);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				/*
				 * Loop through and draw the images sequentially until we're no longer busy
				 */
				while (true == isBusy) {
					if (null == content || true == content.isDisposed()) {
						break;
					}

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							/* 
							 * Draw the image onto the canvas. 
							 */
							if (null != spinnerCanvas && false == spinnerCanvas.isDisposed()) {
								spinnerGC.drawImage(spinnerImages[imageDataIndex[0]], 0, 0);
							}
						}
					});

					/* 
					 * If we have just drawn the last image start over from the beginning
					 */
					if (imageDataIndex[0] == spinnerImages.length - 1) {
						imageDataIndex[0] = 0;
					} else {
						imageDataIndex[0]++;
					}

					/* 
					 * Sleep for a bit.
					 */
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Debug.out(e);
					}

				}

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						/* 
						 * Fill the image area with lbShell background color to 'erase' the last image drawn 
						 */
						if (null != spinnerCanvas && false == spinnerCanvas.isDisposed()) {
							spinnerGC.fillRectangle(spinnerCanvas.getClientArea());
						}
					}
				});

				busyAlready = false;
			}

		};
		spinnerThread.start();

	}

	public Control getControl() {
		return content;
	}

	public ClientMessageContext getMessageContext() {
		return null;
	}

	public void refresh(RefreshListener refreshListener) {
	}

}
