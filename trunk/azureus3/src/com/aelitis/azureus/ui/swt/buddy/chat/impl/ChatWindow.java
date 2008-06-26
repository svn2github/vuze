package com.aelitis.azureus.ui.swt.buddy.chat.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.aelitis.azureus.buddy.chat.Chat;
import com.aelitis.azureus.buddy.chat.ChatDiscussion;
import com.aelitis.azureus.buddy.chat.ChatMessage;
import com.aelitis.azureus.buddy.chat.DiscussionListener;
import com.aelitis.azureus.ui.swt.views.skin.AvatarWidget;

public class ChatWindow implements DiscussionListener {
	
	private static List chatWindows =  new ArrayList();
	
	AvatarWidget avatar;
	Chat chat;
	ChatDiscussion discussion;
	
	Display display;

	Shell shell;
	Color white;
	
	Composite messages;
	Text input;
	
	Listener moveListener;
	
	static DateFormat dateFormater = new SimpleDateFormat("hh:mm");
	
	public ChatWindow(AvatarWidget _avatar,Chat _chat,ChatDiscussion _discussion) {
		this.avatar = _avatar;
		this.chat = _chat;
		this.discussion = _discussion;
		
		synchronized(chatWindows) {
			chatWindows.add(this);
		}
		
		Control avatarControl = avatar.getControl();
		display = avatarControl.getDisplay();
		
		white = display.getSystemColor(SWT.COLOR_WHITE);
		
		shell = new Shell(avatar.getControl().getShell(),SWT.NONE);
		FormLayout formLayout = new FormLayout();
		formLayout.marginBottom = 0;
		formLayout.marginTop = 0;
		formLayout.marginLeft = 0;
		formLayout.marginRight = 0;
		
		shell.setLayout(formLayout);
		
		FormData data;
		
		Button close = new Button(shell,SWT.PUSH);
		close.setText("x");
		close.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				close();
				
			}
		});
		
		data = new FormData();
		data.right = new FormAttachment(100,-5);
		close.setLayoutData(data);
		
		Button hide = new Button(shell,SWT.PUSH);
		hide.setText("-");
		hide.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				hide();
			}
		});
		
		data = new FormData();
		data.right = new FormAttachment(close,-5);
		hide.setLayoutData(data);
		
		Canvas avatarPicture = new Canvas(shell,SWT.NONE);
		avatarPicture.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Rectangle size = avatar.getAvatarImage().getBounds();
				e.gc.drawImage(avatar.getAvatarImage(), 0, 0, size.width,size.height,0,0,64,64);
			}
		});
		
		data = new FormData();
		data.width = 64;
		data.height = 64;
		data.left = new FormAttachment(0,5);
		data.top = new FormAttachment(0,5);
		
		avatarPicture.setLayoutData(data);
		
		Label avatarName = new Label(shell,SWT.NONE);
		avatarName.setText(avatar.getVuzeBuddy().getDisplayName());
		data = new FormData();
		data.left = new FormAttachment(avatarPicture,5);
		data.bottom = new FormAttachment(avatarPicture,-5,SWT.BOTTOM);
		
		avatarName.setLayoutData(data);
		
		final ScrolledComposite messagesHolder = new ScrolledComposite(shell,SWT.BORDER | SWT.V_SCROLL);
		messagesHolder.setBackground(white);
		//messagesHolder.setAlwaysShowScrollBars(true);
		messagesHolder.setExpandHorizontal(true);
		messagesHolder.setExpandVertical(true);
		
		
		messages = new Composite(messagesHolder,SWT.NONE);
		messages.setBackground(white);
		RowLayout messagesLayout = new RowLayout(SWT.VERTICAL);
		messagesLayout.fill = true;
		//messagesLayout.pack = true;
		messagesLayout.type = SWT.VERTICAL;
		messages.setLayout(messagesLayout);
		messages.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event arg0) {
			Rectangle r = messagesHolder.getClientArea();
			messagesHolder.setMinSize(messages.computeSize(r.width, SWT.DEFAULT));
			}
		});
		
		messagesHolder.setContent(messages);
		
		List chatMessages = discussion.getAllMessages();
		for(int i = 0 ; i < chatMessages.size() ; i++) {
			renderMessage((ChatMessage)chatMessages.get(i));
		}
		
		discussion.setListener(new DiscussionListener() {
			public void newMessage(final ChatMessage message) {
				if(!display.isDisposed()) {
					display.asyncExec(new Runnable() {
						public void run() {
							if(!messages.isDisposed()) {
								renderMessage(message);
							}
						}
					});
				}
			}
		});
		
		input = new Text(shell,SWT.BORDER);
		input.addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event e) {
				if(e.keyCode == 13 || e.keyCode == 3) {
					String text = input.getText();
					if(text.length() > 0) {
						chat.sendMessage(avatar.getVuzeBuddy(), text);
						input.setText("");
					}
				}
			}	
		});
		
		data = new FormData();
		data.left = new FormAttachment(0,2);
		data.right = new FormAttachment(100,-2);
		data.bottom = new FormAttachment(100,-2);
		input.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,2);
		data.right = new FormAttachment(100,-2);
		data.top = new FormAttachment(avatarPicture,5);
		data.bottom = new FormAttachment(input,-2);
		messagesHolder.setLayoutData(data);
		
		shell.setSize(250,400);
		
		setPosition();
		
		moveListener =  new Listener() {
			public void handleEvent(Event arg0) {
				setPosition();
			}
		};
		avatarControl.getShell().addListener(SWT.Move,moveListener);
		
		
		hideAllOthers();
		shell.open();
	}
	
	private void renderMessage(ChatMessage message) {
		Composite messageHolder = new Composite(messages,SWT.NONE);
		messageHolder.setBackground(white);
		messageHolder.setLayout(new FormLayout());
		FormData data;
		
		Label name = new Label(messageHolder,SWT.NONE);
		name.setBackground(white);
		name.setText(message.getSender());
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(50,0);
		name.setLayoutData(data);
		
		Label time = new Label(messageHolder,SWT.NONE);
		time.setBackground(white);
		time.setText(dateFormater.format(new Date(message.getTimestamp())));
		data = new FormData();
		data.right = new FormAttachment(100,0);
		time.setLayoutData(data);
		
		Label text = new Label(messageHolder,SWT.WRAP);
		text.setBackground(white);
		text.setText(message.getMessage());
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(name,0);
		text.setLayoutData(data);
		
		RowData rowData = new RowData();
		rowData.width = 240;
		messageHolder.setLayoutData(rowData);
		
		messageHolder.pack();
		messages.layout();
		
		if(shell.isVisible()) {
			discussion.clearNewMessages();
		}
		
	}
	
	private void close() {
		avatar.getControl().getShell().removeListener(SWT.Move, moveListener);
		shell.dispose();
		synchronized(chatWindows) {
			chatWindows.remove(ChatWindow.this);
		}
	}
	
	private void setPosition() {
		Control avatarControl = avatar.getControl();
		Point shellPosition = avatarControl.toDisplay(0,0);
		shellPosition.y -= 400;
		shell.setLocation(shellPosition);
	}
	
	public void newMessage(final ChatMessage message) {
		if(!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					renderMessage(message);
				}
			});
		}
		
	}
	
	public boolean isDisposed() {
		return shell.isDisposed();
	}
	
	public void show() {
		if(!shell.isDisposed()) {
			hideAllOthers();
			shell.setVisible(true);
			discussion.clearNewMessages();
		}
	}
	
	public void hide() {
		shell.setVisible(false);
	}
	
	public void hideAllOthers() {
		synchronized (chatWindows) {
			for(int i = 0 ; i < chatWindows.size() ; i++) {
				ChatWindow chatWindow = (ChatWindow) chatWindows.get(i);
				if(chatWindow != this && !chatWindow.isDisposed()) {
					chatWindow.hide();
				}
			}
		}
	}

}
