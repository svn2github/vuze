package com.aelitis.azureus.ui.swt.buddy.chat.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.buddy.chat.ChatMessage;
import com.aelitis.azureus.ui.swt.views.skin.AvatarWidget;
import com.aelitis.azureus.util.Constants;

public class MessageNotificationWindow {
	
	public static int nbOpen = 0;
	public static int currentOffset = 0;
	
	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/chatNotification.png", "chatNotification");
	}
	
	public MessageNotificationWindow(final AvatarWidget avatar,final ChatMessage message) {
		final org.eclipse.swt.widgets.Display display = avatar.getControl().getDisplay();
		
		final Shell shell = new Shell(display,SWT.NO_TRIM |  SWT.ON_TOP);
		
		Image background = ImageRepository.getImage("chatNotification");
		final Region region = new Region();
		final ImageData imageData = background.getImageData();
		if (imageData.alphaData != null) {
			Rectangle pixel = new Rectangle(0, 0, 1, 1);
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					if (imageData.getAlpha(x, y) == 255) {
						pixel.x = imageData.x + x;
						pixel.y = imageData.y + y;
						region.add(pixel);
					} 
				}
			}
		} else {
			ImageData mask = imageData.getTransparencyMask();
			Rectangle pixel = new Rectangle(0, 0, 1, 1);
			for (int y = 0; y < mask.height; y++) {
				for (int x = 0; x < mask.width; x++) {
					if (mask.getPixel(x, y) != 0) {
						pixel.x = imageData.x + x;
						pixel.y = imageData.y + y;
						region.add(pixel);
					}
				}
			}
		}
		shell.setRegion(region);
		shell.setBackgroundImage(background);
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		
		shell.setLayout(new FormLayout());
		FormData data;
		
		Label image = new Label(shell,SWT.NONE);
		image.setImage(avatar.getAvatarImage());
		
		
		
		Label name = new Label(shell,SWT.WRAP);
		name.setText(message.getSender());
		
		FontData[] fDatas = name.getFont().getFontData();
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(Constants.isOSX) {
				fDatas[i].setHeight(12);
			} else {
				fDatas[i].setHeight(10);
			}
			fDatas[i].setStyle(SWT.BOLD);
		}
		final Font nameFont = new Font(display,fDatas);
		name.setFont(nameFont);
		
		Label text = new Label(shell,SWT.NONE);
		text.setText(message.getMessage());
		
		data = new FormData();
		data.left = new FormAttachment(0,10);
		data.top = new FormAttachment(0,10);
		image.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(image,5);
		data.top = new FormAttachment(image,0,SWT.TOP);
		name.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(image,5);
		data.top = new FormAttachment(name,5);
		data.right = new FormAttachment(100,-5);
		data.bottom = new FormAttachment(100,-5);
		text.setLayoutData(data);
		
		Listener mouseUpListener = new Listener() {
			public void handleEvent(Event arg0) {
				avatar.doChatClicked();
				avatar.getControl().getShell().setVisible(true);
				avatar.getControl().getShell().setActive();
				shell.dispose();
				region.dispose();
				nameFont.dispose();
				nbOpen--;
				if(nbOpen == 0) {
					currentOffset = 0;
				}
			}
		};
		
		shell.addListener(SWT.MouseUp, mouseUpListener);
		name.addListener(SWT.MouseUp, mouseUpListener);
		image.addListener(SWT.MouseUp, mouseUpListener);
		text.addListener(SWT.MouseUp, mouseUpListener);
		
		AEThread2 closer = new AEThread2("notification closer",true) {
			public void run() {
				try {
					Thread.sleep(5000);
					
					for(int alpha = 255 ; alpha > 0 ; alpha-=10) {
						
						final int _alpha = alpha;
						
						if(!display.isDisposed()) {
							display.asyncExec(new Runnable() {
								public void run() {
									if(!shell.isDisposed()) {
										try {
											shell.setAlpha(_alpha);
										} catch(Throwable t) {
											//Ignore
										}
									}
								};
							});
						}
						
						Thread.sleep(50);
					}
					
					if(!display.isDisposed()) {
						display.asyncExec(new Runnable() {
							public void run() {
								if(!shell.isDisposed()) {
									shell.dispose();
									region.dispose();
									nameFont.dispose();
									nbOpen--;
									if(nbOpen == 0) {
										currentOffset = 0;
									}
								}
							};
						});
					}
				} catch (Exception e) {
					//Do nothing
				}
			}
		};
		
		closer.start();
		
		
		Rectangle displayArea = display.getBounds();
		shell.setLocation(displayArea.width - imageData.width - 50 ,100 + (imageData.height+20) * currentOffset);
		shell.setSize(imageData.x + imageData.width, imageData.y + imageData.height);
		
		shell.open();
		shell.setActive();
		//shell.forceActive();
		nbOpen++;
		currentOffset++;
	}
	

}
