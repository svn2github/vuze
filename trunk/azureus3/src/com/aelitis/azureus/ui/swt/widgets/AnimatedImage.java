package com.aelitis.azureus.ui.swt.widgets;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.ImageRepository;

public class AnimatedImage {
	
	//wait time in ms
	private static final int SPEED = 100;
	
	Canvas canvas;
	boolean running;
	
	private AEThread2 runner;
	
	private Image[] images;
	private int currentImage = 0;
	
	public AnimatedImage(Composite parent) {
		canvas = new Canvas(parent,SWT.NONE);
		Color background = null;
		Composite p = parent;
		while(p != null && background == null) {
			background = p.getBackground();
			if(background != null) {
				System.out.println("background : " + background + ", composite : " + p);
			}
			p = p.getParent();
		}
		
		canvas.setBackground(background);
		canvas.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				stop();
				disposeImages();
			}
		});
	}
	
	
	private void renderNextImage() {
		if(!canvas.isDisposed()) {
			Display display = canvas.getDisplay();
			if(!display.isDisposed()) {
				display.asyncExec( new Runnable() {
					public void run() {
						if(!canvas.isDisposed() && images != null) {
							currentImage++;
							if(currentImage >= images.length) {
								currentImage = 0;
							}
							if(currentImage < images.length) {
								Image image = images[currentImage];
								if(image != null && !image.isDisposed()) {
									GC gc = new GC(canvas);
									Point canvasSize = canvas.getSize();
									Rectangle imageBounds = image.getBounds();
									
									gc.drawImage(image, (canvasSize.x-imageBounds.width)/2, (canvasSize.y-imageBounds.height)/2);
									gc.dispose();
								}
							}
						}
					}
				});
			}
		}
	}
	
	public void setLayoutData(Object data) {
		canvas.setLayoutData(data);
	}
	
	public void start() {
		running = true;
		runner = new AEThread2("image runner",true) {
			public void run() {
				while(running) {
					try {
						renderNextImage();
						Thread.sleep(SPEED);
					} catch (Exception e) {
						running = false;
					}
				}
			}
		};
		runner.start();
	}
	
	public void stop() {
		running = false;
	}
	
	public Control getControl() {
		return canvas;
	}
	
	public void dispose() {
		if(canvas != null && !canvas.isDisposed()) {
			canvas.dispose();
		}
	}
	
	public void setImageFromName(String imageName) {
		InputStream is = ImageRepository.getImageAsStream(imageName);

		if (null == is) {
			return;
		}
		ImageLoader loader = new ImageLoader();
		ImageData[] imageDataArray = loader.load(is);

		images = new Image[imageDataArray.length];
		for (int i = 0; i < imageDataArray.length; i++) {
			ImageData imageData = imageDataArray[i];
			/*
			 * Setting the transparent pixel to be black
			 */
			imageData.transparentPixel = 0;

			images[i] = new Image(canvas.getDisplay(), imageData.width,
					imageData.height);
			GC offScreenImageGC = new GC(images[i]);
			offScreenImageGC.setBackground(canvas.getBackground());
			offScreenImageGC.fillRectangle(0, 0, imageData.width,
					imageData.height);

			Image tempImage = new Image(canvas.getDisplay(), imageData);
			offScreenImageGC.drawImage(tempImage, 0, 0);

			tempImage.dispose();
			offScreenImageGC.dispose();
		}
	}
	
	private void setImages(Image[] images) {
		disposeImages();
		this.images = images;
	}

	private void disposeImages() {
		if(images != null) {
			for(int i = 0 ; i < images.length ; i++) {
				if(images[i] != null && !images[i].isDisposed()) {
					images[i].dispose();
				}
			}
		}
	}

}
