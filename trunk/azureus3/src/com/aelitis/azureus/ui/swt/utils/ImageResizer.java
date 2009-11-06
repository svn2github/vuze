package com.aelitis.azureus.ui.swt.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

public class ImageResizer
{

	static private final int RESIZE_STEPS = 100000;

	static private final int MARGIN = 20;

	private int minWidth, minHeight;

	private int displayWidth, displayHeight;

	private Display display;

	private Shell parent;

	private Shell shell;

	private Cursor cursor;

	private Canvas canvas;

	private Scale scale;

	private long lastUpdate = 0l;

	private Image original;

	private int originalWidth, originalHeight;

	private Image currentImage;

	private Image overlay;

	private Image overlayDragging;

	private Image overlayNotDragging;

	private boolean done;

	private Image result;

	private float zoomRatio;

	private float minZoomRatio;

	private float maxZoomRatio;

	private Point offset;

	private Listener moveImageListener = new Listener() {

		private boolean mouseDown = false;

		private Point pointDown;

		public void handleEvent(Event event) {
			//System.out.println(event);
			switch (event.type) {
				case SWT.MouseDown:
					mouseDown = true;
					pointDown = new Point(event.x, event.y);
					overlay = overlayDragging;
					drawCurrentImage();
					break;
				case SWT.MouseUp:
					mouseDown = false;
					overlay = overlayNotDragging;
					drawCurrentImage();
					break;
				case SWT.MouseMove:
					if (!mouseDown) {
						break;
					}
					offset.x = offset.x + event.x - pointDown.x;
					offset.y = offset.y + event.y - pointDown.y;
					insureOffsetIsCorrect();

					pointDown.x = event.x;
					pointDown.y = event.y;
					drawCurrentImage();
					break;
				case SWT.MouseEnter:

					break;
				case SWT.MouseExit:

					break;
			}

		}
	};

	private ImageResizerListener imageResizerListener;

	public ImageResizer(Display display, int width, int height, Shell parent) {
		this.parent = parent;
		this.display = display;
		this.minWidth = width;
		this.minHeight = height;
	}

	public void resize(Image original, ImageResizerListener l) throws ImageResizeException {

		this.original = original;
		this.imageResizerListener = l;

		//If the image is too small, let's just not deal with it
		if (!checkSize(original)) {
			dispose();
			throw new ImageResizeException(MessageText.getString(
					"ImageResizer.image.too.small", new String[] {
						minWidth + "",
						minHeight + ""
					}));
		}

		originalWidth = original.getBounds().width;
		originalHeight = original.getBounds().height;

		currentImage = new Image(display, internalResize(original,
				(int) (originalWidth * zoomRatio), (int) (originalHeight * zoomRatio)));
		offset = new Point(0, 0);

		if (minWidth != original.getBounds().width
				|| minHeight != original.getBounds().height) {

			displayWidth = minWidth + 2 * (MARGIN + 1);
			displayHeight = minHeight + 2 * (MARGIN + 1);

			overlay = overlayNotDragging = createOverlayImage((byte) 255, 0x00FFFFFF,
					(byte) 255, 0x00000000);
			overlayDragging = createOverlayImage((byte) 80, 0x00FFFFFF, (byte) 255,
					0x00FFFFFF);

			initUI();

			done = false;
		} else {
			result = computeResultImage();
			l.imageResized(result);
		}
	}

	private void initUI() {

		cursor = new Cursor(display, SWT.CURSOR_HAND);

		if (parent != null) {
			shell = ShellFactory.createShell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		} else {
			shell = ShellFactory.createMainShell(SWT.CLOSE | SWT.BORDER);
		}
		shell.setText("Thumbnail Assistant");

		Utils.setShellIcon(shell);

		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = false;
				result = null;
				done = true;
				shell = null;
				dispose();
				imageResizerListener.imageResized(result);
			}
		});

		FormLayout layout = new FormLayout();
		layout.marginBottom = 5;
		layout.marginTop = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;

		FormData data;
		shell.setLayout(layout);

		Label title = new Label(shell, SWT.WRAP);
		title.setText(MessageText.getString("ImageResizer.title"));

		data = new FormData();
		data.width = displayWidth;
		title.setLayoutData(data);

		canvas = new Canvas(shell, SWT.BORDER);
		canvas.setCursor(cursor);
		data = new FormData();
		data.width = displayWidth;
		data.height = displayHeight;
		data.top = new FormAttachment(title, 5);
		canvas.setLayoutData(data);

		canvas.addListener(SWT.MouseDown, moveImageListener);
		canvas.addListener(SWT.MouseUp, moveImageListener);
		canvas.addListener(SWT.MouseMove, moveImageListener);

		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				drawCurrentImage();
			}
		});

		offset.x = (minWidth - currentImage.getBounds().width) / 2;
		offset.y = (minHeight - currentImage.getBounds().height) / 2;

		Label label = new Label(shell, SWT.WRAP);
		//The label text depends on the presence of the scale,
		//Thefore we delay the size computation as well as
		//Assiging any FormData (layout) to it see (1)

		//The Control to witch the Buttons OK and Cancel are going to be attached
		//Depends on the presence of the scale
		Control attach = label;

		if (minZoomRatio < 1) {
			scale = new Scale(shell, SWT.HORIZONTAL);
			data = new FormData();
			data.width = displayWidth;
			data.top = new FormAttachment(label, 5);
			scale.setLayoutData(data);
			scale.setMaximum((int) (RESIZE_STEPS * maxZoomRatio));
			scale.setMinimum((int) (RESIZE_STEPS * minZoomRatio));
			scale.setIncrement((int) ((maxZoomRatio - minZoomRatio) * RESIZE_STEPS / 10));
			scale.setPageIncrement((int) ((maxZoomRatio - minZoomRatio)
					* RESIZE_STEPS / 10));

			scale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event arg0) {
					final long timestamp = SystemTime.getCurrentTime();
					lastUpdate = timestamp;

					final int position = scale.getSelection();

					AEThread t = new AEThread("") {
						public void runSupport() {
							try {
								Thread.sleep(150);
							} catch (Exception e) {
								e.printStackTrace();
							}

							if (timestamp == lastUpdate) {
								if (display != null && !display.isDisposed()) {
									display.asyncExec(new Runnable() {
										public void run() {
											refreshCurrentImage(position);
										}
									});
								}
							}
						}
					};
					t.setDaemon(true);
					t.start();

				}
			});
			attach = scale;
			label.setText(MessageText.getString("ImageResizer.move.image.with.slider"));
		} else {
			label.setText(MessageText.getString("ImageResizer.move.image"));
		}

		// (1) Layout of the label, depending on the text in it
		int width = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if (width > displayWidth) {
			width = displayWidth;
		}

		data = new FormData();
		data.width = width;
		data.top = new FormAttachment(canvas, 5);
		data.left = new FormAttachment(canvas, 0, SWT.CENTER);
		label.setLayoutData(data);

		Button btnCancel = new Button(shell, SWT.PUSH);
		btnCancel.setText("Cancel");
		data = new FormData();
		data.width = 70;
		data.top = new FormAttachment(attach, 10);
		data.right = new FormAttachment(100, -10);
		btnCancel.setLayoutData(data);
		btnCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				result = null;
				done = true;
				dispose();
				imageResizerListener.imageResized(result);
			}
		});

		Button btnOk = new Button(shell, SWT.PUSH);
		btnOk.setText("OK");
		data = new FormData();
		data.width = 70;
		data.top = new FormAttachment(attach, 10);
		data.right = new FormAttachment(btnCancel, -10);
		btnOk.setLayoutData(data);
		btnOk.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				result = computeResultImage();
				done = true;
				dispose();
				imageResizerListener.imageResized(result);
			}
		});

		shell.setDefaultButton(btnOk);
		btnOk.setFocus();
		shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (parent != null) {
			Utils.centerWindowRelativeTo(shell, parent);
		}

		shell.open();
		drawCurrentImage();

	}

	private boolean checkSize(Image image) {
		//If the image is smaller than the minimal size, we shouldn't accept it
		Rectangle size = image.getBounds();
		if (size.width < minWidth || size.height < minHeight) {
			return false;
		}

		float minHRatio = (float) (minHeight) / size.height;
		float minWRatio = (float) (minWidth) / size.width;

		float maxHRatio = (float) (minHeight * 4) / size.height;
		float maxWRatio = (float) (minWidth * 4) / size.width;

		//We must keep the min zoom bigger than the "biggest" ratio (ie, smallest zoom out)
		minZoomRatio = minHRatio > minWRatio ? minHRatio : minWRatio;
		maxZoomRatio = maxHRatio > maxWRatio ? maxHRatio : maxWRatio;

		if (maxZoomRatio > 1) {
			maxZoomRatio = 1;
		}

		zoomRatio = minZoomRatio;

		return true;
	}

	private ImageData internalResize(Image image, int newWidth, int newHeight) {

		ImageData srcData = image.getImageData();

		//int width = srcData.width,height = srcData.height;

		ImageData data = srcData;
		//int newWidth = (int)(width*zoomRatio);
		//int newHeight = (int)(height*zoomRatio);

		final ImageData copy = new ImageData(newWidth, newHeight, 24,
				new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

		Image src = new Image(display, srcData);
		Image dst = new Image(display, copy);

		GC gc = new GC(dst);
		gc.setAdvanced(true);
		try {
			gc.setInterpolation(SWT.HIGH);
		} catch (Exception e) {
			// may not be avail
		}
		gc.drawImage(src, 0, 0, srcData.width, srcData.height, 0, 0, copy.width,
				copy.height);
		//gc.setAlpha(50);
		//gc.drawImage(src,2,2,srcData.width-2,srcData.height-2,0,0,copy.width,copy.height);
		gc.dispose();

		data = dst.getImageData();

		src.dispose();
		dst.dispose();

		return data;
	}

	private void drawCurrentImage() {
		GC gcCanvas = new GC(canvas);

		Image temp = new Image(display, displayWidth, displayHeight);
		GC gc = new GC(temp);
		gc.drawImage(currentImage, offset.x + MARGIN + 1, offset.y + MARGIN + 1);
		//gc.setAlpha(128);
		gc.drawImage(overlay, 0, 0);
		//gc.setTextAntialias(SWT.ON);
		//gc.drawText("This is a test", 15, displayHeight-15,true);
		gc.dispose();

		gcCanvas.drawImage(temp, 0, 0);
		temp.dispose();

		gcCanvas.dispose();
	}

	private void insureOffsetIsCorrect() {
		int minX = minWidth - currentImage.getBounds().width;
		if (offset.x < minX) {
			offset.x = minX;
		}

		int minY = minHeight - currentImage.getBounds().height;
		if (offset.y < minY) {
			offset.y = minY;
		}

		if (offset.x > 0) {
			offset.x = 0;
		}

		if (offset.y > 0) {
			offset.y = 0;
		}

	}

	private void dispose() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}

		if (currentImage != null && !currentImage.isDisposed()) {
			currentImage.dispose();
		}

		if (overlayDragging != null && !overlayDragging.isDisposed()) {
			overlayDragging.dispose();
		}

		if (overlayNotDragging != null && !overlayNotDragging.isDisposed()) {
			overlayNotDragging.dispose();
		}

		if (cursor != null && !cursor.isDisposed()) {
			cursor.dispose();
		}
	}

	private Image computeResultImage() {
		Image img = new Image(display, minWidth, minHeight);

		/*ImageData srcData = original.getImageData();
		 ImageData dstData = new ImageData(
		 currentImage.getBounds().width,
		 currentImage.getBounds().height,
		 32,
		 new PaletteData(0xFF,0xFF00,0xFF0000));
		 Resample resample = new Resample();		
		 resample.setFilter(Resample.FILTER_TYPE_LANCZOS3, 7.0f);
		 resample.process(srcData, dstData);
		 Image filtered = new Image(display,dstData);
		 */
		GC gc = new GC(img);
		gc.drawImage(currentImage, offset.x, offset.y);
		gc.dispose();
		//filtered.dispose();
		return img;
	}

	private Image createOverlayImage(final byte marginAlpha,
			final int marginColor, final byte borderAlpha, final int borderColor) {

		int width = displayWidth;
		int height = displayHeight;

		ImageData data = new ImageData(width, height, 32, new PaletteData(
				0x000000FF, 0x0000FF00, 0x00FF0000));

		byte[] transparency = new byte[width * height];
		int[] pixels = new int[width * height];

		byte rowAlpha[] = new byte[width];
		int rowPixels[] = new int[width];
		//Top
		//Pattern
		for (int i = 0; i < width; i++) {
			rowAlpha[i] = marginAlpha;
			rowPixels[i] = marginColor;
		}
		//Fill
		for (int i = 0; i < MARGIN; i++) {
			System.arraycopy(rowAlpha, 0, transparency, i * width, width);
			System.arraycopy(rowPixels, 0, pixels, i * width, width);
		}

		//Main area
		//Pattern
		for (int i = 0; i < MARGIN; i++) {
			rowAlpha[i] = marginAlpha;
			rowAlpha[width - i - 1] = marginAlpha;
		}
		for (int i = MARGIN; i < width - MARGIN; i++) {
			rowAlpha[i] = 0;
		}
		//Fill
		for (int i = MARGIN; i < height - MARGIN; i++) {
			System.arraycopy(rowAlpha, 0, transparency, i * width, width);
			System.arraycopy(rowPixels, 0, pixels, i * width, width);
		}

		//Bottom
		//Pattern
		for (int i = 0; i < width; i++) {
			rowAlpha[i] = marginAlpha;
		}
		//Fill
		for (int i = height - MARGIN - 1; i < height; i++) {
			System.arraycopy(rowAlpha, 0, transparency, i * width, width);
			System.arraycopy(rowPixels, 0, pixels, i * width, width);
		}

		//Let's do the border part		
		for (int i = MARGIN; i < width - MARGIN; i++) {
			transparency[width * MARGIN + i] = borderAlpha;
			pixels[width * MARGIN + i] = borderColor;
		}
		for (int j = MARGIN; j < height - MARGIN; j++) {

			transparency[j * width + MARGIN] = borderAlpha;
			pixels[j * width + MARGIN] = borderColor;

			transparency[j * width + width - MARGIN - 1] = borderAlpha;
			pixels[j * width + width - MARGIN - 1] = borderColor;

		}
		for (int i = MARGIN; i < width - MARGIN; i++) {
			transparency[width * (height - MARGIN - 1) + i] = borderAlpha;
			pixels[width * (height - MARGIN - 1) + i] = borderColor;
		}

		data.alphaData = transparency;
		data.setPixels(0, 0, width * height, pixels, 0);

		Image overlay = new Image(display, data);

		return overlay;
	}

	private void refreshCurrentImage(int position) {
		float previousZoom = zoomRatio;
		zoomRatio = (float) position / RESIZE_STEPS;
		if (zoomRatio > 1) {
			zoomRatio = 1;
		}
		if (zoomRatio < minZoomRatio) {
			zoomRatio = minZoomRatio;
		}
		if (previousZoom != zoomRatio) {
			Image previous = currentImage;
			currentImage = new Image(display,
					internalResize(original, (int) (originalWidth * zoomRatio),
							(int) (originalHeight * zoomRatio)));

			//float ratio = zoomRatio / previousZoom;
			offset.x += (previous.getBounds().width - currentImage.getBounds().width) / 2;
			offset.y += (previous.getBounds().height - currentImage.getBounds().height) / 2;

			if (previous != null && !previous.isDisposed()) {
				previous.dispose();
			}

			insureOffsetIsCorrect();
			drawCurrentImage();
		}
	}
	
	public interface ImageResizerListener {
		void imageResized(Image image);
	}

	public static void main(final String args[]) throws Exception {
		final Display display = Display.getDefault();
		final Shell test = new Shell(display);
		ImageResizer resizer = new ImageResizer(display, 228, 128, null);
		String file = new FileDialog(test).open();
		Image img = new Image(display, file);
		resizer.resize(img, new  ImageResizerListener() {
			public void imageResized(Image thumbnail) {
				System.out.println(thumbnail);
				
				thumbnail.dispose();
				test.dispose();
				if (args.length == 0) {
					display.dispose();
				}
			}
		});

	}
}