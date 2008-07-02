package com.aelitis.azureus.ui.swt.views.skin;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.sharing.ShareResourceEvent;
import org.gudy.azureus2.plugins.sharing.ShareResourceFile;
import org.gudy.azureus2.plugins.sharing.ShareResourceListener;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.buddy.chat.ChatDiscussion;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.VuzeBuddySyncListener;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.buddy.chat.impl.ChatWindow;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.util.LoginInfoManager;

public class AvatarWidget
{
	private static final boolean SHOW_ONLINE_BORDER = System.getProperty(
			"az.buddy.show_online", "1").equals("1");

	private Canvas canvas = null;

	private BuddiesViewer viewer = null;

	private Composite parent = null;

	private int highlightBorder = 0;

	private int imageBorder = 1;

	private Point imageSize = null;

	private Point size = null;

	private Point nameAreaSize = null;

	private Rectangle imageBounds = null;

	private Rectangle nameAreaBounds = null;
	
	private Rectangle chatAreaBounds = null;

	private VuzeBuddySWT vuzeBuddy = null;

	private boolean isActivated = false;

	private boolean isSelected = false;

	private boolean isEnabled = true;

	private boolean isDisposing = false;

	private boolean nameLinkActive = false;

	private Color textColor = null;

	private Color textLinkColor = null;

	private Color imageBorderColor = null;

	private Color selectedColor = null;

	private Color highlightedColor = null;

	private Rectangle decorator_remove_friend = null;

	private Rectangle decorator_add_to_share = null;

	private int alpha = 255;

	private boolean sharedAlready = false;

	private Image image = null;

	//private Image imageDefaultAvatar = null;

	private Rectangle sourceImageBounds = null;

	private Menu menu;

	private static Font fontDisplayName;

	private String tooltip_remove_friend;

	private String tooltip_add_to_share;

	private String tooltip;

	private Image removeImage = null;

	private Image add_to_share_Image = null;

	private Image removeImage_normal = null;

	private Image add_to_share_Image_normal = null;

	private Image removeImage_over = null;

	private Image add_to_share_Image_over = null;
	
	private boolean isDragging = false;

	private Image add_to_share_Image_selected = null;


	private boolean isCreatingFile = false;
	
	private int creationPercent = 0;
	
	private ChatWindow chatWindow;
	private ChatDiscussion discussion;
	
	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/friend_online_icon.png", "friend_online_icon");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/grey_bubble.png", "grey_bubble");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/red_bubble.png", "red_bubble");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/large_red_bubble.png", "large_red_bubble");
	}
	
	public AvatarWidget(BuddiesViewer viewer, Point avatarSize,
			Point avatarImageSize, Point avatarNameSize, VuzeBuddySWT vuzeBuddy) {

		if (null == viewer || null == vuzeBuddy) {
			throw new NullPointerException(
					"The variable 'viewer' and 'vuzeBuddy' can not be null");
		}

		this.viewer = viewer;

		if (null == viewer.getControl() || true == viewer.getControl().isDisposed()) {
			throw new NullPointerException(
					"The given 'viewer' is not properly initialized");
		}

		this.parent = viewer.getControl();
		this.size = avatarSize;
		this.imageSize = avatarImageSize;
		this.nameAreaSize = avatarNameSize;
		this.vuzeBuddy = vuzeBuddy;
		canvas = new Canvas(parent, SWT.NONE | SWT.DOUBLE_BUFFERED);
		canvas.setData("AvatarWidget", this);

		init();
	}

	private void init() {

		ImageLoader imageLoader = ImageLoaderFactory.getInstance();
		removeImage_normal = imageLoader.getImage("image.buddy.remove");
		add_to_share_Image_normal = imageLoader.getImage("image.buddy.add.to.share");
		removeImage_over = imageLoader.getImage("image.buddy.remove-over");
		add_to_share_Image_selected = imageLoader.getImage("image.buddy.add.to.share-selected");

		removeImage = removeImage_normal;
		add_to_share_Image = add_to_share_Image_normal;

		tooltip_remove_friend = MessageText.getString("v3.buddies.remove");
		tooltip_add_to_share = MessageText.getString("v3.buddies.add.to.share");
		tooltip = vuzeBuddy.getDisplayName() + " (" + vuzeBuddy.getLoginID() + ")";

		/*
		 * Centers the image and name horizontally
		 */
		imageBounds = new Rectangle((size.x / 2) - (imageSize.x / 2), 8,
				imageSize.x, imageSize.y);

		nameAreaBounds = new Rectangle((size.x / 2) - ((nameAreaSize.x - 6) / 2),
				imageBounds.y + imageBounds.height + 2, nameAreaSize.x - 6,
				nameAreaSize.y);

		/*
		 * Position the decorator icons
		 */
		decorator_remove_friend = new Rectangle(size.x
				- (highlightBorder + imageBorder) - 12 - 1, highlightBorder
				+ imageBorder + 1, 12, 12);

		decorator_add_to_share = new Rectangle(highlightBorder + imageBorder + 1,
				highlightBorder + imageBorder + 1, 12, 12);
		/*
		 * Get the avatar image and create a default image if none was found
		 */
		image = vuzeBuddy.getAvatarImage();

		sourceImageBounds = null == image ? null : image.getBounds();

		int operations = DND.DROP_COPY;
		Transfer[] types = new Transfer[] {FileTransfer.getInstance()};
		DropTarget target = new DropTarget(canvas, operations);
		target.setTransfer(types);
		
		target.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				if(isCreatingFile) {
					event.detail = DND.DROP_NONE;
				} else {
					event.detail = DND.DROP_COPY;
					isDragging = true;
				}
			};
			public void dragOver(DropTargetEvent event) {};
			public void dragLeave(DropTargetEvent event) {
				isDragging = false;
			};
			public void dragOperationChanged(DropTargetEvent event) {};
			public void dropAccept(DropTargetEvent event) {}
			public void drop(DropTargetEvent event) {
				isDragging = false;
				// A drop has occurred, copy over the data
				if (event.data == null) { // no data to copy, indicate failure in event.detail
					event.detail = DND.DROP_NONE;
					return;
				}
				String[] files = null;
				if(event.data instanceof String[]) {
					files = (String[]) event.data;
				} else if(event.data instanceof String) {
					files = new String[] {(String)event.data};
				}
				if(files != null) {
					if(files.length == 1) {
						try {
							if(!isCreatingFile) {
								
								MessageBoxShell mb = new MessageBoxShell(canvas.getShell(),
								MessageText.getString("v3.buddies.dnd.info.dialog.title"),
								MessageText.getString("v3.buddies.dnd.info.dialog.text"),
								new String[] {
									MessageText.getString("v3.buddies.dnd.info.dialog.ok"),
								},
								0,
								"v3.buddies.dnd.info",
								MessageText.getString("v3.buddies.dnd.info.dialog.remember"),
								false,
								0);
								
								mb.open(true);
								
								creationPercent = 0;
								isCreatingFile = true;
								final File file = new File(files[0]);
								final TOTorrentCreator creator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(file,new URL("dht:"),false);
								creator.addListener(new TOTorrentProgressListener() {
									public void reportCurrentTask(
											String task_description) {
										
									}
									
									public void reportProgress(int percent_complete) {
										creationPercent = percent_complete;
										
										if(!canvas.isDisposed()) {
											canvas.getDisplay().asyncExec(new Runnable() {
												public void run() {
													canvas.redraw();
												};
											});
										}
										
									}
									
								});
								
								new AEThread2("DNDBuddy::Share",true) 
									{
							            
										public void
										run() 
							            {
											try {
												TOTorrent torrent = creator.create();
												TorrentUtils.setDecentralised( torrent );
												TorrentUtils.setDHTBackupEnabled( torrent,true );
												LocaleTorrentUtil.setDefaultTorrentEncoding( torrent );
												
												File v3Shares = new File(SystemProperties.getUserPath(),"v3shares");
												if(!v3Shares.exists()) {
													v3Shares.mkdirs();
												}
												
												TorrentUtils.setFlag(torrent, TorrentUtils.TORRENT_FLAG_LOW_NOISE, true);
												
												final File torrent_file = new File(v3Shares,file.getName() + ".torrent");
											    torrent.serialiseToBEncodedFile(torrent_file);
												
											    byte[] hash = null;
								             	try {
								             		hash = torrent.getHash();
								             	} catch (TOTorrentException e1) { }
												
												final DownloadManager dm = AzureusCoreFactory.getSingleton().getGlobalManager().addDownloadManager(
														torrent_file.getAbsolutePath(),
														hash,
														file.getAbsolutePath(),
														DownloadManager.STATE_QUEUED,
														true,	// persistent 
														true,	// for seeding
														null );	// no adapter required
												
												dm.getDownloadState().setFlag(Download.FLAG_DO_NOT_DELETE_DATA_ON_REMOVE, true);
												
												isSelected = true;
												if(!canvas.isDisposed()) {
													canvas.getDisplay().asyncExec(new Runnable() {
														public void run() {
															try {
																SelectedContentV3 sc = new SelectedContentV3(dm);
																VuzeShareUtils.getInstance().shareTorrent(sc, "buddy-dnd");
															} catch (Exception e) {
																e.printStackTrace();
															}
														}
													});
												}
												
								            } catch (Exception e) {
												e.printStackTrace();
											} finally {
												isCreatingFile = false;
											}
							            }
									}.start();
							}
							
						} catch (Exception e) {
							e.printStackTrace();
							isCreatingFile = false;
						}
						
						canvas.redraw();
					} else {
						
						MessageBoxShell mb = new MessageBoxShell(canvas.getShell(),
						MessageText.getString("v3.buddies.dnd.multifile.dialog.title"),
						MessageText.getString("v3.buddies.dnd.multifile.dialog.text"),
						new String[] {
							MessageText.getString("v3.buddies.dnd.multifile.dialog.ok"),
						}, 0);
						mb.open();
					}
				}
			}
		});
		
		canvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				if (false == isFullyVisible()) {
					return;
				}

				if (fontDisplayName == null || fontDisplayName.isDisposed()) {
					fontDisplayName = Utils.getFontWithHeight(canvas.getFont(), e.gc, 10);
				}

				try {
					e.gc.setAntialias(SWT.ON);
					e.gc.setTextAntialias(SWT.ON);
					e.gc.setAlpha(getAlpha());
					e.gc.setInterpolation(SWT.HIGH);
				} catch (Exception ex) {
					// ignore.. some of these may not be avail
				}

				/*
				 * Draw background if the widget is activated or selected
				 */
				if (true == isActivated || true == isSelected) {

					e.gc.setBackground(true == isActivated ? highlightedColor
							: selectedColor);
					Rectangle bounds = canvas.getBounds();
					e.gc.fillRoundRectangle(highlightBorder, highlightBorder,
							bounds.width - (2 * highlightBorder), bounds.height
									- (2 * highlightBorder), 6, 6);
					e.gc.setBackground(canvas.getBackground());
				}

				/*
				 * Draw highlight borders if the widget is activated (being hovered over)
				 */

				/*
				 * Draw the avatar image
				 */
				if (null == image || image.isDisposed()) {
					/*
					 * Paint nothing if the buddy has no avatar AND the default image is not found,
					 * OR the image has been disposed
					 */
					Debug.out("No avatar image found and no default image supplies?");
				} else {
					if (true == viewer.isEditMode()) {
						e.gc.setAlpha((int) (getAlpha() * .7));
						/*
						 * Image
						 */
						e.gc.drawImage(image, 0, 0, sourceImageBounds.width,
								sourceImageBounds.height, imageBounds.x, imageBounds.y,
								imageBounds.width, imageBounds.height);
						e.gc.setAlpha(getAlpha());
						/*
						 * Image border
						 */
						if (imageBorder > 0) {
							e.gc.setForeground(imageBorderColor);
							e.gc.setLineWidth(imageBorder);
							e.gc.drawRectangle(imageBounds.x - imageBorder, imageBounds.y
									- imageBorder, imageBounds.width + imageBorder,
									imageBounds.height + imageBorder);
							e.gc.setForeground(canvas.getForeground());
						}
					} else {
						/*
						 * Image
						 */
						e.gc.drawImage(image, 0, 0, sourceImageBounds.width,
								sourceImageBounds.height, imageBounds.x, imageBounds.y,
								imageBounds.width, imageBounds.height);
						/*
						 * Image border
						 */
						if (imageBorder > 0) {
							e.gc.setForeground(imageBorderColor);
							e.gc.setLineWidth(imageBorder);
							e.gc.drawRectangle(imageBounds.x - imageBorder, imageBounds.y
									- imageBorder, imageBounds.width + imageBorder,
									imageBounds.height + imageBorder);
							e.gc.setForeground(canvas.getForeground());
						}
					}
				}

				if (isSharedAlready()) {
					add_to_share_Image = add_to_share_Image_selected;
				} else {
					add_to_share_Image = add_to_share_Image_normal;
				}

				/*
				 * Draw decorator
				 */
				if (true == viewer.isEditMode()) {
					e.gc.drawImage(removeImage, 0, 0, removeImage.getBounds().width,
							removeImage.getBounds().height, decorator_remove_friend.x,
							decorator_remove_friend.y, decorator_remove_friend.width,
							decorator_remove_friend.height);
				} else if (true == viewer.isShareMode()) {
					e.gc.drawImage(add_to_share_Image, 0, 0,
							removeImage.getBounds().width, removeImage.getBounds().height,
							decorator_add_to_share.x, decorator_add_to_share.y,
							decorator_add_to_share.width, decorator_add_to_share.height);
				}

				/*
				 * Draw the buddy display name
				 */

				if (null != textLinkColor && null != textColor) {
					if (true == nameLinkActive && true == isActivated) {
						e.gc.setForeground(textLinkColor);
						if(false == isDragging) {
							canvas.setCursor(canvas.getDisplay().getSystemCursor(
									SWT.CURSOR_HAND));
						}
					} else {
						if(false == isDragging) {
							canvas.setCursor(null);
						}
						e.gc.setForeground(textColor);
					}

					/*
					 * The multi-line display of name is disabled for now 
					 */
					//					int flags = SWT.CENTER | SWT.WRAP;
					//					GCStringPrinter stringPrinter = new GCStringPrinter(e.gc,
					//							vuzeBuddy.getDisplayName(), avatarNameBounds, false, true, flags);
					//					stringPrinter.calculateMetrics();
					//
					//					if (stringPrinter.isCutoff()) {
					//						e.gc.setFont(fontDisplayName);
					//						avatarNameBounds.height += 9;
					//						avatarNameBounds.y -= 4;
					//					}
					//					stringPrinter.printString(e.gc, avatarNameBounds, SWT.CENTER);
					//					e.gc.setFont(null);
					
					if(!isCreatingFile) {
						
						e.gc.setFont(fontDisplayName);
						int width = 0;
						String displayName = vuzeBuddy.getDisplayName();
						StringBuffer displayed = new StringBuffer();
						
						Image icon = null;
						
						if(SHOW_ONLINE_BORDER && vuzeBuddy.isOnline(true)) {
							icon = ImageRepository.getImage("friend_online_icon");
							width += icon.getBounds().width + 3;
						}
						
						int dotWidth = e.gc.getAdvanceWidth('.');
						int maxWidth = nameAreaBounds.width;
						
						for(int i = 0 ; i < displayName.length() && width < maxWidth ; i++) {
							char nextChar = displayName.charAt(i);
							int extraWidth = e.gc.getAdvanceWidth(nextChar);
							if(width + 2 * dotWidth >= maxWidth) {
								//We only have room for 2 dot characters, let's simply check if we're processing the last one,
								// and if it fits in
								if(i == displayName.length() -1 && (width + extraWidth <= maxWidth) ){
									displayed.append(nextChar);
									width += extraWidth;
								} else {
									displayed.append("..");
									width += 2 * dotWidth;
								}
							} else {
								displayed.append(nextChar);
								width += extraWidth;
							}
						}
						
						int offset = (maxWidth - width) / 2;
						if(icon != null) {
							e.gc.drawImage(icon, offset + nameAreaBounds.x, nameAreaBounds.y+1);
							offset += icon.getBounds().width +1;
						}
						
						e.gc.drawText(displayed.toString(), offset + nameAreaBounds.x, nameAreaBounds.y+1,true);
						
						//e.gc.fillRectangle(nameAreaBounds);
						
						/*if(SHOW_ONLINE_BORDER && vuzeBuddy.isOnline(true)) {
							GCStringPrinter stringPrinter = new GCStringPrinter(e.gc,"%0 " + vuzeBuddy.getDisplayName(),
									nameAreaBounds, false, true, SWT.CENTER);
							stringPrinter.setImages(new Image[] {});
							stringPrinter.printString();
						} else {
							GCStringPrinter.printString(e.gc, vuzeBuddy.getDisplayName(),
									nameAreaBounds, false, true, SWT.CENTER);
						}*/
						
					} else {

						Rectangle progressArea = new Rectangle(nameAreaBounds.x+5,nameAreaBounds.y+5,nameAreaBounds.width-10,nameAreaBounds.height-10);
						e.gc.setForeground(ColorCache.getColor(e.gc.getDevice(), 130,130,130));
						e.gc.drawRectangle(progressArea.x, progressArea.y, progressArea.width, progressArea.height);
						progressArea.x += 1;
						progressArea.y += 1;	
						progressArea.height -=1;
						progressArea.width = creationPercent * (progressArea.width-1) / 100;
						e.gc.setBackground(ColorCache.getColor(e.gc.getDevice(), 0,73,153));
						e.gc.fillRectangle(progressArea.x, progressArea.y, progressArea.width, progressArea.height);
					}
				}
				
				if(chatWindow != null && chatWindow.isDisposed()) {
					chatWindow = null;
				}
				
				boolean showChatIcon = ! viewer.isEditMode() && discussion != null && ( (chatWindow != null && !chatWindow.isDisposed() && discussion.getNbMessages() > 0) || (discussion.getUnreadMessages() > 0));
				
				if(showChatIcon) {
					chatAreaBounds = new Rectangle(40,0,20,19);
					int nbMessages = discussion.getUnreadMessages();
					if(nbMessages > 0 && (chatWindow == null || !chatWindow.isVisible())) {
						int startPixel = 0;
						if(nbMessages >= 10) {
							e.gc.drawImage(ImageRepository.getImage("large_red_bubble"), 35, -1);
							startPixel = 49;
						} else {
							e.gc.drawImage(ImageRepository.getImage("red_bubble"), 40, 0);
							startPixel = 52;
						}
						
						e.gc.setForeground(ColorCache.getColor(e.gc.getDevice(), 255,255,255));
						Point textSize = e.gc.stringExtent("" + nbMessages);
						e.gc.drawText("" + nbMessages, startPixel - textSize.x / 2 , 3,true);
						
					} else {
						e.gc.drawImage(ImageRepository.getImage("grey_bubble"), 40, 0);
					}
				} else {
					chatAreaBounds = null;
				}
				
			}
		});

		canvas.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {

			}

			public void mouseExit(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				isActivated = false;
				canvas.redraw();
			}

			public void mouseEnter(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if (false == isActivated) {
					isActivated = true;
					canvas.redraw();
				}
			}
		});

		canvas.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if (e.button != 1) {
					return;
				}

				/*
				 * If it's in Share mode then clicking on any part will add it to Share
				 */
				if (true == viewer.isShareMode()) {
					doAddBuddyToShare();
					return;
				}

				if (true == nameAreaBounds.contains(e.x, e.y)) {
					doLinkClicked();
				} else if (decorator_remove_friend.contains(e.x, e.y)) {
					if (true == viewer.isEditMode()) {
						doRemoveBuddy();
					}
				} else if (decorator_add_to_share.contains(e.x, e.y)) {

				}  else {
					if ((e.stateMask & SWT.MOD1) == SWT.MOD1) {
						viewer.select(vuzeBuddy, !isSelected, true);
					} else {
						viewer.select(vuzeBuddy, !isSelected, false);
					}
					canvas.redraw();
				}
				
				
			}

			public void mouseDown(MouseEvent e) {
				//It's conflicting with double click otherwise ...
				if(chatAreaBounds != null && chatAreaBounds.contains(e.x,e.y)) {
					doChatClicked();
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
				if (false == viewer.isShareMode() && false == viewer.isEditMode()) {
					doChatClicked();
					return;
				}
			}
		});

		canvas.addMouseMoveListener(new MouseMoveListener() {
			private boolean lastActiveState = false;

			private String lastTooltipText = canvas.getToolTipText();

			public void mouseMove(MouseEvent e) {
				if (false == isFullyVisible()) {
					return;
				}
				if ((e.stateMask & SWT.MOD1) == SWT.MOD1) {
					return;
				}

				/*
				 * Optimization employed to minimize how often the tooltip text is updated;
				 * updating too frequently causes the tooltip to 'stick' to the cursor which
				 * can be annoying
				 */
				String tooltipText = "";

				if (true == viewer.isShareMode()) {
					if (false == isSharedAlready()) {
						tooltipText = tooltip_add_to_share;
					} else {
						tooltipText = tooltip;
					}
				} else if (decorator_remove_friend.contains(e.x, e.y)) {
					if (true == viewer.isEditMode()) {
						tooltipText = tooltip_remove_friend;
					} else {
						tooltipText = tooltip;
					}
				} else {
					tooltipText = tooltip;
				}

				if (false == tooltipText.equals(lastTooltipText)) {
					canvas.setToolTipText(tooltipText);
					lastTooltipText = tooltipText;
				}

				if (true == nameAreaBounds.contains(e.x, e.y)) {
					if (false == lastActiveState) {
						nameLinkActive = true;
						canvas.redraw();
						lastActiveState = true;
					}
				} if(chatAreaBounds != null && chatAreaBounds.contains(e.x,e.y)) {
					canvas.setCursor(canvas.getDisplay().getSystemCursor(
							SWT.CURSOR_HAND));
				} else {
					canvas.setCursor(null);
					if (true == lastActiveState) {
						nameLinkActive = false;
						canvas.redraw();
						lastActiveState = false;
					}
				}

			}
		});

		canvas.addListener(SWT.Move, new Listener() {
			public void handleEvent(Event arg0) {
				if(chatWindow != null && chatWindow.isVisible()) {
					chatWindow.setPosition();
				}
			}
		});
		
		initMenu();
	}

	public boolean isFullyVisible() {
		return viewer.isFullyVisible(AvatarWidget.this);
	}

	private void initMenu() {
		menu = new Menu(canvas);
		canvas.setMenu(menu);

		menu.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX) {
					return;
				}

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed()) {
							return;
						}
						MenuItem[] items = menu.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}

				bShown = true;

				fillMenu(menu);
			}
		});
	}

	protected void fillMenu(Menu menu) {
		MenuItem item;

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item, "v3.buddy.menu.viewprofile");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AvatarWidget aw = (AvatarWidget) canvas.getData("AvatarWidget");
				if (aw != null) {
					aw.doLinkClicked();
				}
			}
		});
		
		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item, "v3.buddy.menu.chat");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AvatarWidget aw = (AvatarWidget) canvas.getData("AvatarWidget");
				if (aw != null) {
					aw.doChatClicked();
				}
			}
		});

		if (Constants.isCVSVersion()) {
			MenuItem itemMenuDebug = new MenuItem(menu, SWT.CASCADE);
			itemMenuDebug.setText("Debug");
			Menu menuCVS = new Menu(menu);
			itemMenuDebug.setMenu(menuCVS);

			item = new MenuItem(menuCVS, SWT.PUSH);
			Messages.setLanguageText(item, "v3.buddy.menu.remove");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					AvatarWidget aw = (AvatarWidget) canvas.getData("AvatarWidget");
					if (aw != null) {
						doRemoveBuddy();
					}
				}
			});

			item = new MenuItem(menuCVS, SWT.PUSH);
			item.setText("Send Activity Message");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					InputShell is = new InputShell("Moo", "Message:");
					String txt = is.open();
					if (txt != null) {
						txt = LoginInfoManager.getInstance().getUserInfo().userName
								+ " says: \n" + txt;
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
								SystemTime.getCurrentTime(), txt, "Test");
						System.out.println("sending to " + vuzeBuddy.getDisplayName());
						try {
							vuzeBuddy.sendActivity(entry);
						} catch (NotLoggedInException e1) {
							Debug.out("Shouldn't Happen", e1);
						}
					}
				}
			});
			
			item = new MenuItem(menuCVS, SWT.PUSH);
			item.setText("Sync this buddy (via PK)");
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (!LoginInfoManager.getInstance().isLoggedIn()) {
						Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
								"not logged in. no can do");
						return;
					}
					final String pk = vuzeBuddy.getPublicKeys()[0];
					final long lastUpdate = vuzeBuddy.getLastUpdated();
					try {
						PlatformBuddyMessenger.sync(new String[] {
							pk
						}, new VuzeBuddySyncListener() {
							public void syncComplete() {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (vuzeBuddy.getLastUpdated() != lastUpdate) {
											Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Yay",
													"Updated");
										} else {
											Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Boo",
													"Not Updated");
										}
									}
								});
							}
						});
					} catch (NotLoggedInException e1) {
					}
				}
			});
		}
	}

	private void doRemoveBuddy() {

		LightBoxShell lbShell = new LightBoxShell(parent.getShell(), false);

		MessageBoxShell mBox = new MessageBoxShell(lbShell.getShell(),
				MessageText.getString("v3.buddies.remove.buddy.dialog.title"),
				MessageText.getString("v3.buddies.remove.buddy.dialog.text",
						new String[] {
							vuzeBuddy.getLoginID()
						}), new String[] {
					MessageText.getString("v3.mb.delPublished.delete"),
					MessageText.getString("v3.mb.delPublished.cancel")
				}, 1);

		mBox.setLeftImage(SWT.ICON_QUESTION);

		lbShell.open();

		if (1 == mBox.open(true)) {
			lbShell.close();
			return;
		}
		try {
			VuzeBuddyManager.removeBuddy(vuzeBuddy, true);
		} catch (NotLoggedInException e) {
			// should not happen, unless the user cancelled
			Debug.out(e);
		}
		lbShell.close();
	}

	private void doAddBuddyToShare() {
		if (false == isSharedAlready()) {
			viewer.addToShare(this);
			sharedAlready = true;
		} else {
			viewer.removeFromShare(vuzeBuddy);
			sharedAlready = false;
		}
		canvas.redraw();
		canvas.update();
	}

	public void doHover() {

	}

	public void doClick() {

	}

	public void doMouseEnter() {

	}

	public void doDoubleClick() {

	}

	public void doLinkClicked() {

		/*
		 * Open the user profile page but only if NOT in Share or Add mode
		 */
		if (false == viewer.isShareMode() && false == viewer.isAddBuddyMode()) {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null != uiFunctions) {
				String url = getVuzeBuddy().getProfileUrl("buddy-bar");
				uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
						true, true);
			}
		}
	}

	public void setChatDiscussion(ChatDiscussion discussion) {
		if(this.discussion != discussion) {
			this.discussion = discussion;
		}
		if(discussion.getUnreadMessages() > 0) {
			if(canvas != null && !canvas.isDisposed()) {
				canvas.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if(!canvas.isDisposed()) {
							canvas.redraw();	
						}
					}
				});
			}
		}
	}
	
	public boolean isChatWindowVisible() {
		return chatWindow != null && chatWindow.isVisible();
	}
	
	public void doChatClicked() {
		doChatClicked(false);
	}
	
	public void doChatClicked(final boolean noHide) {
		if (false == viewer.isShareMode() && false == viewer.isAddBuddyMode()) {
			if(chatWindow == null || chatWindow.isDisposed()) {
				if(discussion == null) {
					discussion = viewer.getChat().getChatDiscussionFor(vuzeBuddy);
				}
				Display display = canvas.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						chatWindow = new ChatWindow(AvatarWidget.this,viewer.getChat(),discussion);
					}
				});
				
			} else {
				if(chatWindow.isVisible() && !noHide) {
					chatWindow.hide();
				} else {
					chatWindow.show();
				}
			}
			
			canvas.redraw();
		}
	}

	public Control getControl() {
		return canvas;
	}

	public int getBorderWidth() {
		return highlightBorder;
	}

	public void setBorderWidth(int borderWidth) {
		//		this.highlightBorder = borderWidth;
	}

	public VuzeBuddySWT getVuzeBuddy() {
		return vuzeBuddy;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public void refreshVisual() {

		/*
		 * Resets the image and image bounds since this is the only info cached;
		 * all other info is asked for on-demand so no need to update them 
		 */
		image = vuzeBuddy.getAvatarImage();
		
		sourceImageBounds = null == image ? null : image.getBounds();
		tooltip = vuzeBuddy.getDisplayName() + " (" + vuzeBuddy.getLoginID() + ")";

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (null != canvas && false == canvas.isDisposed()) {
					canvas.redraw();
				}
			}
		});

	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getTextLinkColor() {
		return textLinkColor;
	}

	public void setTextLinkColor(Color textLinkColor) {
		this.textLinkColor = textLinkColor;
	}

	public void dispose(boolean animate, final AfterDisposeListener listener) {
		if (null != canvas && false == canvas.isDisposed()) {
			if(chatWindow != null && !chatWindow.isDisposed()) {
				chatWindow.close();
			}
			if (true == animate) {
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {

						isDisposing = true;

						/*
						 * KN: TODO: disposal check is still not complete since it could still happen
						 * between the .isDisposed() check and the .redraw() or .update() calls.
						 */
						while (alpha > 20 && false == canvas.isDisposed()) {
							alpha -= 30;
							canvas.redraw();
							canvas.update();

							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						if (false == canvas.isDisposed()) {
							canvas.dispose();
							parent.layout(true);
							if (null != listener) {
								listener.disposed();
							}
						}
					}
				});
			} else {
				if (false == canvas.isDisposed()) {
					canvas.dispose();
					parent.layout(true);
					if (null != listener) {
						listener.disposed();
					}
				}
			}

		}
	}

	public boolean isSharedAlready() {
		return sharedAlready;
	}

	public void setSharedAlready(boolean sharedAlready) {
		this.sharedAlready = sharedAlready;
		refreshVisual();
	}

	public void setVuzeBuddy(VuzeBuddySWT vuzeBuddy) {
		if (null != vuzeBuddy) {
			this.vuzeBuddy = vuzeBuddy;
			refreshVisual();
		}
	}

	public Point getAvatarImageSize() {
		return imageSize;
	}

	public void setAvatarImageSize(Point avatarImageSize) {
		this.imageSize = avatarImageSize;
	}

	public Point getAvatarNameSize() {
		return nameAreaSize;
	}

	public void setAvatarNameSize(Point avatarNameSize) {
		this.nameAreaSize = avatarNameSize;
	}

	public Image getAvatarImage() {
		return image;
	}

	public void setAvatarImage(Image avatarImage) {
		this.image = avatarImage;
	}

	public Color getImageBorderColor() {
		return imageBorderColor;
	}

	public void setImageBorderColor(Color imageBorderColor) {
		this.imageBorderColor = imageBorderColor;
	}

	public int getAvatarImageBorder() {
		return imageBorder;
	}

	public void setAvatarImageBorder(int avatarImageBorder) {
		this.imageBorder = avatarImageBorder;
	}

	public int getImageBorder() {
		return imageBorder;
	}

	public void setImageBorder(int imageBorder) {
		this.imageBorder = imageBorder;
	}

	public Color getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(Color selectedColor) {
		this.selectedColor = selectedColor;
	}

	public Color getHighlightedColor() {
		return highlightedColor;
	}

	public void setHighlightedColor(Color highlightedColor) {
		this.highlightedColor = highlightedColor;
	}

	public boolean isEnabled() {
		if (false == isEnabled) {
			return isEnabled;
		}

		return viewer.isEnabled();
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	private int getAlpha() {
		if (!isDisposing) {
			if (true == isEnabled()) {
				alpha = 255;
			} else {
				alpha = 128;
			}
		}

		return alpha;
	}

	public interface AfterDisposeListener
	{
		public void disposed();
	}
}
