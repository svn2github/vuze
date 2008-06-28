package com.aelitis.azureus.ui.swt.buddy.chat.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.chat.Chat;
import com.aelitis.azureus.buddy.chat.ChatDiscussion;
import com.aelitis.azureus.buddy.chat.ChatMessage;
import com.aelitis.azureus.buddy.chat.DiscussionListener;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.AvatarWidget;
import com.aelitis.azureus.util.Constants;

public class ChatWindow implements DiscussionListener {
	
	private static List chatWindows =  new ArrayList();
	
	AvatarWidget avatar;
	Chat chat;
	ChatDiscussion discussion;
	
	Display display;

	Shell shell;
	Color white;
	
	ScrolledComposite messagesHolder;
	Composite messages;
	
	Text input;
	
	Listener moveListener;
	
	Font textFont;
	Font nameFont;
	Font timeFont;
	
	PaintListener myNameHighligther;
	PaintListener friendNameHighlighter;
	
	static DateFormat dateFormater = new SimpleDateFormat("hh:mm a");
	
	static final int border = 5;
	static final int spacing = 5;
	
	Listener linkListener;
	
	String lastSender;
	
	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/button_dialog_min.png", "button_dialog_min");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/button_dialog_min-over.png", "button_dialog_min-over");
		
	}
	
	public ChatWindow(AvatarWidget _avatar,Chat _chat,ChatDiscussion _discussion) {
		this.avatar = _avatar;
		this.chat = _chat;
		this.discussion = _discussion;
		
		synchronized(chatWindows) {
			chatWindows.add(this);
		}
		
		linkListener = new Listener() {
			public void handleEvent(Event e) {
				String text = e.text;
				//System.out.println(text);
				//TODO Gudy / Tux launch utily?
				Program.launch(text);
			}
		};
		
		Control avatarControl = avatar.getControl();
		display = avatarControl.getDisplay();
		
		
		shell = new Shell(avatar.getControl().getShell(),SWT.NONE);
		shell.setBackground(ColorCache.getColor(display, 42,42,42));
		
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					hide();
				}
			}
		});
		
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 3;
		fillLayout.marginWidth = 3;
		shell.setLayout(fillLayout);
		
		final Composite mainPanel = new Composite(shell,SWT.NONE);
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginBottom = 0;
		formLayout.marginTop = 0;
		formLayout.marginLeft = 0;
		formLayout.marginRight = 0;
		
		mainPanel.setLayout(formLayout);
		mainPanel.setBackground(ColorCache.getColor(display, 18,18,18));
		
		myNameHighligther = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Label label = (Label) e.widget;
				String text = (String)label.getData("text");
				if(text != null) {
					Point p = label.getSize();
					try { e.gc.setTextAntialias(SWT.ON); } catch(Exception e2) {}
					e.gc.setBackground(ColorCache.getColor(display, 192,204,220));
					e.gc.fillRoundRectangle(0, 0, p.x, p.y, 10, 10);
					e.gc.drawText(text, 3, 0);
				}
			}
		};
		
		friendNameHighlighter = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Label label = (Label) e.widget;
				String text = (String)label.getData("text");
				if(text != null) {
					Point p = label.getSize();
					try { e.gc.setTextAntialias(SWT.ON); } catch(Exception e2) {}
					e.gc.setBackground(ColorCache.getColor(display, 208,208,208));
					e.gc.fillRoundRectangle(0, 0, p.x, p.y, 10, 10);
					e.gc.drawText(text, 3, 0);
				}
			}
		};
		
		FontData[] fDatas = shell.getFont().getFontData();
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(Constants.isOSX) {
				fDatas[i].setHeight(12);
			} else {
				fDatas[i].setHeight(10);
			}
		}
		textFont = new Font(display,fDatas);
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(Constants.isOSX) {
				fDatas[i].setHeight(9);
			} else {
				fDatas[i].setHeight(7);
			}
		}
		timeFont = new Font(display,fDatas);
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(Constants.isOSX) {
				fDatas[i].setHeight(14);
			} else {
				fDatas[i].setHeight(12);
			}
			fDatas[i].setStyle(SWT.BOLD | SWT.ITALIC);
		}
		nameFont = new Font(display,fDatas);
		
		
		
		
		FormData data;
		
		final Label close = new Label(mainPanel,SWT.NONE);
		close.setBackground(mainPanel.getBackground());
		close.setImage(ImageRepository.getImage("button_skin_close"));
		close.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent arg0) {
				close.setImage(ImageRepository.getImage("button_skin_close-over"));
			}
			public void mouseExit(MouseEvent arg0) {
				close.setImage(ImageRepository.getImage("button_skin_close"));
			}
			public void mouseHover(MouseEvent arg0) {
				
			}
		});
		
		//close.setText("X");
		close.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		close.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				close();
				
			}
		});
		
		data = new FormData();
		data.right = new FormAttachment(100,-border);
		data.top = new FormAttachment(0,border);
		close.setLayoutData(data);
		
		final Label hide = new Label(mainPanel,SWT.PUSH);
		hide.setBackground(mainPanel.getBackground());
		hide.setImage(ImageRepository.getImage("button_dialog_min"));
		hide.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent arg0) {
				hide.setImage(ImageRepository.getImage("button_dialog_min-over"));
			}
			public void mouseExit(MouseEvent arg0) {
				hide.setImage(ImageRepository.getImage("button_dialog_min"));
			}
			public void mouseHover(MouseEvent arg0) {
				
			}
		});
		
		hide.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		hide.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				hide();
			}
		});
		
		
		data = new FormData();
		data.right = new FormAttachment(close,-border);
		data.top = new FormAttachment(0,border);
		hide.setLayoutData(data);
		
		
	
		Canvas avatarPicture = new Canvas(mainPanel,SWT.NONE);
		avatarPicture.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Rectangle size = avatar.getAvatarImage().getBounds();
				e.gc.drawImage(avatar.getAvatarImage(), 0, 0, size.width,size.height,0,0,30,30);
			}
		});
		
		data = new FormData();
		data.width = 30;
		data.height = 30;
		data.left = new FormAttachment(0,border);
		data.top = new FormAttachment(0,border);
		
		avatarPicture.setLayoutData(data);
		
		Label avatarName = new Label(mainPanel,SWT.NONE);
		avatarName.setBackground(mainPanel.getBackground());
		avatarName.setFont(nameFont);
		avatarName.setText(avatar.getVuzeBuddy().getDisplayName());
		avatarName.setForeground(ColorCache.getColor(display,151,151,151));
		data = new FormData();
		data.left = new FormAttachment(avatarPicture,spacing);
		data.top = new FormAttachment(avatarPicture,-6,SWT.CENTER);
		data.right = new FormAttachment(hide,-5);
		avatarName.setLayoutData(data);
		
		/*Label header = new Label(shell,SWT.NONE);
		header.setBackground(ColorCache.getColor(display, 72,72,72));
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(0,0);
		data.bottom = new FormAttachment(0,30);
		
		header.setLayoutData(data);*/
		
		messagesHolder = new ScrolledComposite(mainPanel,SWT.BORDER | SWT.V_SCROLL);
		messagesHolder.setBackground(ColorCache.getColor(display,236,236,236));
		messagesHolder.setAlwaysShowScrollBars(true);
		messagesHolder.setExpandHorizontal(true);
		messagesHolder.setExpandVertical(true);
		
		
		messages = new Composite(messagesHolder,SWT.NONE);
		messages.setBackground(messagesHolder.getBackground());
		RowLayout messagesLayout = new RowLayout(SWT.VERTICAL);
		messagesLayout.fill = true;
		//messagesLayout.pack = true;
		messagesLayout.spacing = 5;
		messagesLayout.type = SWT.VERTICAL;
		messages.setLayout(messagesLayout);
		messagesHolder.setContent(messages);
		
		List chatMessages = discussion.getAllMessages();
		for(int i = 0 ; i < chatMessages.size() ; i++) {
			ChatMessage cm = (ChatMessage)chatMessages.get(i);
			cm.setRendered();
			renderMessage( cm );
		}
		
		discussion.setListener(new DiscussionListener() {
			public void newMessage(final ChatMessage message) {
				message.setRendered();
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
		
		input = new Text(mainPanel,SWT.WRAP);
		input.setBackground(messagesHolder.getBackground());
		input.setTextLimit(256);
		input.setFont(textFont);

		input.addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event e) {
				if(e.keyCode == 13) {
					String text = input.getText().trim();
					if(text.length() > 0) {
						chat.sendMessage(avatar.getVuzeBuddy(), text);	
					}
					input.setText("");
				}
			}	
		});
		input.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				mainPanel.layout();	
			}
		});
		
		data = new FormData();
		data.left = new FormAttachment(0,border);
		data.right = new FormAttachment(100,-border);
		data.bottom = new FormAttachment(100,-border);
		input.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,border);
		data.right = new FormAttachment(100,-border);
		data.top = new FormAttachment(avatarPicture,spacing);
		data.bottom = new FormAttachment(input,-border);
		messagesHolder.setLayoutData(data);
		
		shell.setSize(250,300);
		
		setPosition();
		
		moveListener =  new Listener() {
			public void handleEvent(Event arg0) {
				setPosition();
			}
		};
		avatarControl.getShell().addListener(SWT.Move,moveListener);
		
		input.setFocus();
		
		
		
		if(avatar.getVuzeBuddy().getVersion() < VuzeBuddy.VERSION_CHAT) {
			renderSystemMessage(MessageText.getString("v3.chat.wrongversion",new String[] {avatar.getVuzeBuddy().getDisplayName()}));
			input.setEnabled(false);
		} else {
			if(!avatar.getVuzeBuddy().isOnline(true)) {
				renderSystemMessage(MessageText.getString("v3.chat.offline",new String[] {avatar.getVuzeBuddy().getDisplayName()}));
			}
		}

		hideAllOthers();
		shell.open();
		
		//Need to post to display in order to NOT catch the mouse up event which would hide this window...
		display.asyncExec(new Runnable() {
			public void run() {
				avatar.getControl().redraw();
			}
		});
		
	}
	
	private void renderSystemMessage(String message) {
		Composite messageHolder = new Composite(messages,SWT.NONE);
		messageHolder.setBackground(ColorCache.getColor(display, 244,238,188));
		FillLayout layout = new FillLayout();
		layout.marginHeight = 3;
		layout.marginWidth = 5;	
		messageHolder.setLayout(layout);
		

		Label text = new Label(messageHolder,SWT.WRAP);
		text.setBackground(ColorCache.getColor(display, 244,238,188));
		text.setText(message);
		
		RowData rowData = new RowData();
		rowData.width = 210;
		messageHolder.setLayoutData(rowData);
		
		messageHolder.pack();
		messages.layout();

	}
	
	private void renderMessage(ChatMessage message) {
		Composite messageHolder = new Composite(messages,SWT.INHERIT_FORCE);
		messageHolder.setBackground(messagesHolder.getBackground());
		messageHolder.setLayout(new FormLayout());
		FormData data;
		
		Label name = null;
		Label time = new Label(messageHolder,SWT.NONE);
		
		String sender = message.getSender();
		if(sender != null) {
			if(!sender.equals(lastSender)) {
				name = new Label(messageHolder,SWT.NONE);
				name.setData("text",message.getSender());
				//name.setForeground(ColorCache.getColor(display,80,80,80));
				data = new FormData();
				data.left = new FormAttachment(0,0);
				data.right = new FormAttachment(100,0);
				name.setLayoutData(data);
				if(message.isMe()) {
					name.addPaintListener(myNameHighligther);
				} else {
					name.addPaintListener(friendNameHighlighter);
				}
			}
			lastSender = sender;
		}
		
		
		time.setText(dateFormater.format(new Date(message.getTimestamp())));
		data = new FormData();
		if(name != null) {
			data.top = new FormAttachment(name,1);
		}
		data.right = new FormAttachment(100,0);
		time.setLayoutData(data);
		time.setFont(timeFont);
		
		Link text = new Link(messageHolder,SWT.WRAP);
		text.setBackground(messageHolder.getBackground());
		text.setFont(textFont);
		String msg = message.getMessage();
		msg = msg.replaceAll("(?i)((?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\]))", "<a href=\"mailto:$1\">$0</a>");
		msg = msg.replaceAll("(?i)\\b(https?://[^\\s]*?)\\s", "<a href=\"$1\">$0</a>");
		text.setText(msg);
		data = new FormData();
		data.left = new FormAttachment(0,3);
		data.right = new FormAttachment(time,-3);
		if(name != null) {
			data.top = new FormAttachment(name,0);
		}
		text.setLayoutData(data);
		text.addListener(SWT.Selection, linkListener);
		
		RowData rowData = new RowData();
		rowData.width = 210;
		messageHolder.setLayoutData(rowData);
		
		messageHolder.pack();
		messages.layout();
		
		Rectangle r = messagesHolder.getClientArea();
		messagesHolder.setMinSize(messages.computeSize(r.width, SWT.DEFAULT));
		
		messagesHolder.getVerticalBar().setSelection(messagesHolder.getVerticalBar().getMaximum());
		messagesHolder.layout();	
		
		if(shell.isVisible()) {
			discussion.clearNewMessages();
		}
		
	}
	
	public void close() {
		discussion.clearAllMessages();
		discussion.setListener( null );
		avatar.getControl().getShell().removeListener(SWT.Move, moveListener);
		shell.dispose();
		if(textFont != null && !textFont.isDisposed()) {
			textFont.dispose();
		}
		if(nameFont != null && !nameFont.isDisposed()) {
			nameFont.dispose();
		}
		if(timeFont != null && !timeFont.isDisposed()) {
			timeFont.dispose();
		}
		synchronized(chatWindows) {
			chatWindows.remove(ChatWindow.this);
		}
		avatar.getControl().redraw();
	}
	
	public void setPosition() {
		Control avatarControl = avatar.getControl();
		if(avatar.isFullyVisible() && !shell.isDisposed()) {
			Point shellPosition = avatarControl.toDisplay(0,0);
			shellPosition.y -= 300;
			int displayWidth = display.getBounds().width;
			if(shellPosition.x + 250 > displayWidth) {
				shellPosition.x = displayWidth - 250;
			}
			shell.setLocation(shellPosition);
		} else {
			hide();
		}
	}
	
	public void newMessage(final ChatMessage message) {
		if(!display.isDisposed()) {
			message.setRendered();
			display.asyncExec(new Runnable() {
				public void run() {
					renderMessage(message);
					avatar.getControl().redraw();
				}
			});
		}
		
	}
	
	public boolean isDisposed() {
		if(shell != null) {
			return shell.isDisposed();
		} else {
			return true;
		}
	}
	
	public boolean isVisible() {
		if(!shell.isDisposed()) {
			return shell.isVisible();
		}
		return false;
	}
	
	public void show() {
		if(!shell.isDisposed()) {
			hideAllOthers();
			setPosition();
			shell.setVisible(true);
			discussion.clearNewMessages();
			avatar.getControl().redraw();
		}
	}
	
	public void hide() {
		if(discussion.getNbMessages() == 0) {
			close();
		}
		if(!shell.isDisposed()) {
			shell.setVisible(false);
			Color gray = display.getSystemColor(SWT.COLOR_DARK_GRAY);
			Control[] controls = messages.getChildren();
			for(int i = 0 ; i < controls.length ; i++) {
				Control[] children = ((Composite)controls[i]).getChildren();
				for(int j = 0 ; j < children.length ; j++) {
					children[j].setForeground(gray);
				}
			}
		}
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
