/**
 * Created on Mar 1, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;

/**
 * @author TuxPaper
 * @created Mar 1, 2009
 *
 */
public abstract class TranscodeChooser
{
	private static final String skinFile = "skin3_transcodechooser";

	private static final String shellSkinObjectID = "shell";

	private Shell shell;

	private SWTSkin skin;

	private Font fontDevice;

	protected TranscodeTarget selectedDevice;

	protected TranscodeProfile selectedProfile;

	private Listener clickListener;

	private SWTSkinObjectContainer soList;

	private Shell mainShell;

	private SWTSkinObjectContainer soBottomContainer;

	private Button btnNoPrompt;

	private int transcodeRequirement;

	private java.util.List<String> listImageIDsToRelease = new ArrayList<String>();

	public TranscodeChooser() {
		this(null);
	}

	public TranscodeChooser(TranscodeTarget device) {
		selectedDevice = device;
	}

	public void show() {
		// Check if plugin is installed
		if (!DevicesFTUX.ensureInstalled()) {
			return;
		}

		mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
		shell = ShellFactory.createShell(mainShell, SWT.DIALOG_TRIM | SWT.RESIZE);

		Utils.setShellIcon(shell);

		skin = SWTSkinFactory.getNonPersistentInstance(
				SkinnedDialog.class.getClassLoader(), "com/aelitis/azureus/ui/skin/",
				skinFile + ".properties");

		skin.initialize(shell, shellSkinObjectID);

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.close();
				}
			}
		});

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				closed();
			}
		});

		skin.layout();

		soList = (SWTSkinObjectContainer) skin.getSkinObject("list");
		if (soList != null) {
			clickListener = new Listener() {
				boolean down = false;

				public void handleEvent(Event event) {
					if (event.type == SWT.MouseDown) {
						down = true;
					} else if (event.type == SWT.MouseUp && down) {
						Widget widget = (event.widget instanceof Label)
								? ((Label) event.widget).getParent() : event.widget;
						if (selectedDevice == null) {
							selectedDevice = (TranscodeTarget) widget.getData("DeviceMediaRenderer");
							if (selectedDevice == null) {
								Debug.out("device is null!");
							}
							createProfileList(soList);
						} else {
							selectedProfile = (TranscodeProfile) widget.getData("TranscodeProfile");
							if (selectedProfile == null) {
								Debug.out("profile is null!");
							} else {
								if (btnNoPrompt != null) {
									if (btnNoPrompt.getSelection()) {
										selectedDevice.setDefaultTranscodeProfile(selectedProfile);
									}
								}
							}
							shell.dispose();
						}
						down = false;
					}
				}
			};

			if (selectedDevice == null) {
				createDeviceList(soList);
			} else {
				createProfileList(soList);
			}
		}
		
		SWTSkinObject soBottom = skin.getSkinObject("bottom");
		if (soBottom instanceof SWTSkinObjectContainer) {
			soBottomContainer = (SWTSkinObjectContainer) soBottom;
			
			soBottomContainer.addListener(new SWTSkinObjectListener(){
			
				public Object eventOccured(SWTSkinObject skinObject, int eventType,
						Object params) {
					if (eventType == EVENT_SHOW) {
						skinObject.removeListener(this);
						initBottom();
					}
					return null;
				}
			});
			soBottomContainer.setVisible(selectedDevice != null);
		}

		// we may have disposed of shell during device/profile list building
		// (ex. no devices avail)
		if (shell.isDisposed()) {
			return;
		}

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Utils.disposeSWTObjects(new Object[] {
					fontDevice
				});
				for (String id : listImageIDsToRelease) {
					ImageLoader.getInstance().releaseImage(id);
				}
			}
		});
		shell.open();
	}

	/**
	 * @param soBottomContainer2
	 *
	 * @since 4.1.0.5
	 */
	protected void initBottom() {
		Composite composite = soBottomContainer.getComposite();
		btnNoPrompt = new Button(composite, SWT.CHECK);
		Messages.setLanguageText(btnNoPrompt, "MessageBoxWindow.nomoreprompting");
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(MessageText.getString("device.xcode.whenreq"));
		
		final Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		combo.add(MessageText.getString("device.xcode.whenreq"));
		combo.add(MessageText.getString("device.xcode.always"));
		combo.add(MessageText.getString("device.xcode.never"));
		transcodeRequirement = selectedDevice.getTranscodeRequirement();
		switch (transcodeRequirement) {
			case TranscodeTarget.TRANSCODE_ALWAYS:
				combo.select(1);
				break;
			
			case TranscodeTarget.TRANSCODE_NEVER:
				combo.select(2);
				break;

			case TranscodeTarget.TRANSCODE_WHEN_REQUIRED:
			default:
				combo.select(0);
				break;
		}
		
		combo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int i = combo.getSelectionIndex();
				switch (i) {
					case 0:
						transcodeRequirement = TranscodeTarget.TRANSCODE_WHEN_REQUIRED;
						break;

					case 1:
						transcodeRequirement = TranscodeTarget.TRANSCODE_ALWAYS;
						break;

					case 2:
						transcodeRequirement = TranscodeTarget.TRANSCODE_NEVER;
						break;
				}
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		FormData fd;
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 10);
		fd.top = new FormAttachment(combo, 0, SWT.CENTER);
		btnNoPrompt.setLayoutData(fd);
		
		fd = new FormData();
		fd.right = new FormAttachment(100, -10);
		fd.top = new FormAttachment(0, 5);
		fd.bottom = new FormAttachment(100, -5);
		combo.setLayoutData(fd);
		
		fd = new FormData();
		fd.right = new FormAttachment(combo, -5);
		fd.top = new FormAttachment(combo, 0, SWT.CENTER);
		label.setLayoutData(fd);

		Point computeSize = shell.computeSize(600, SWT.DEFAULT, true);
		shell.setSize(computeSize);
	}

	/**
	 * @param soList
	 *
	 * @since 4.1.0.5
	 */
	private void createProfileList(SWTSkinObjectContainer soList) {
		if (selectedDevice == null) {
			Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "No Device",
					"No Device Selected!?");
			shell.dispose();
			return;
		}
		
		try {
			TranscodeProfile defaultProfile = selectedDevice.getDefaultTranscodeProfile();
			if (defaultProfile != null) {
				// user chose not to ask
				selectedProfile = defaultProfile;
				shell.dispose();
				return;
			}
		} catch (TranscodeException e) {
		}

		TranscodeProfile[] profiles = selectedDevice.getTranscodeProfiles();

		if (profiles.length == 0) {
			Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "No Profiles",
					"No Profiles for " + selectedDevice.getDevice().getName());
			shell.dispose();
			return;
		}

		if (profiles.length == 1) {
			selectedProfile = profiles[0];
			shell.dispose();
			return;
		}

		Composite parent = soList.getComposite();
		if (parent.getChildren().length > 0) {
			Utils.disposeComposite(parent, false);
		}

		final SWTSkinObjectText soInfoTitle = (SWTSkinObjectText) skin.getSkinObject("info-title");
		if (soInfoTitle != null) {
			soInfoTitle.setText("Additional Information");
		}
		final SWTSkinObjectText soInfoText = (SWTSkinObjectText) skin.getSkinObject("info-text");
		if (soInfoText != null) {
			soInfoText.setText("Hover over a profile to see additional information here");
		}

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.justify = true;
		layout.fill = true;
		parent.setLayout(layout);

		Listener listenerMouseInout = new Listener() {
			public void handleEvent(Event event) {
				Widget widget = (event.widget instanceof Canvas)
						? ((Canvas) event.widget).getParent() : event.widget;
				TranscodeProfile profile = (TranscodeProfile) widget.getData("TranscodeProfile");
				if (profile == null) {
					return;
				}
				if (event.type == SWT.MouseEnter) {
					if (soInfoTitle != null) {
						soInfoTitle.setText(profile.getName());
					}
					if (soInfoText != null) {
						soInfoText.setText(profile.getDescription());
						Point computeSize = shell.computeSize(shell.getClientArea().width,
								SWT.DEFAULT, true);
						shell.setSize(computeSize);
					}
				}
			}
		};

		parent.addListener(SWT.MouseEnter, new Listener() {
			public void handleEvent(Event event) {
				if (soInfoTitle != null) {
					soInfoTitle.setText("Additional Information");
				}
				if (soInfoText != null) {
					soInfoText.setText("Hover over a profile to see additional information here");
					Point computeSize = shell.computeSize(shell.getClientArea().width,
							SWT.DEFAULT, true);
					shell.setSize(computeSize);
				}
			}
		});

		GridData gridData;
		for (TranscodeProfile profile : profiles) {
			Composite c = new Composite(parent, SWT.NONE);
			c.setLayout(new GridLayout());
			c.setCursor(c.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			c.addListener(SWT.MouseUp, clickListener);
			c.addListener(SWT.MouseDown, clickListener);
			c.setData("TranscodeProfile", profile);

			c.addListener(SWT.MouseEnter, listenerMouseInout);

			final Canvas lblImage = new Canvas(c, SWT.NONE);
			lblImage.addListener(SWT.MouseEnter, listenerMouseInout);
			lblImage.addListener(SWT.MouseUp, clickListener);
			lblImage.addListener(SWT.MouseDown, clickListener);
			lblImage.addListener(SWT.Paint, new Listener() {
				public void handleEvent(Event event) {
					Image image = (Image) lblImage.getData("Image");
					if (image != null) {
						Rectangle bounds = image.getBounds();
						event.gc.drawImage(image, bounds.x, bounds.y, bounds.width,
								bounds.height, bounds.x, bounds.y, bounds.width,
								bounds.height);
					} else {
						Rectangle ca = lblImage.getClientArea();
						event.gc.drawRectangle(ca.x, ca.y, ca.width - 1, ca.height - 1);
					}
				}
			});
			gridData = new GridData();
			gridData.heightHint = 100;
			gridData.widthHint = 120;
			String url = profile.getIconURL();
			if (url != null) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				Image image = imageLoader.getUrlImage(url,
						new ImageDownloaderListener() {
							public void imageDownloaded(Image image,
									boolean returnedImmediately) {
								if (!returnedImmediately) {
									lblImage.setData("Image", image);
									Rectangle bounds = image.getBounds();
									GridData gridData = (GridData) lblImage.getLayoutData();
									gridData.heightHint = bounds.height;
									gridData.widthHint = bounds.width;
									lblImage.setLayoutData(gridData);
									lblImage.getShell().layout(new Control[] {
										lblImage
									});
								}
							}
						});
				if (image != null) {
					lblImage.setData("Image", image);
					Rectangle bounds = image.getBounds();
					gridData.heightHint = bounds.height;
					gridData.widthHint = bounds.width;
				}
			}
			lblImage.setLayoutData(gridData);

			Label label = new Label(c, SWT.WRAP | SWT.CENTER);
			label.addListener(SWT.MouseEnter, listenerMouseInout);
			label.addListener(SWT.MouseUp, clickListener);
			label.addListener(SWT.MouseDown, clickListener);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			label.setLayoutData(gridData);
			String s = profile.getName();
			//s += " (via " + profile.getProvider().getName() + ")";
			label.setText(s);
			label.setCursor(c.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		}

		SWTSkinObjectText soTitle = (SWTSkinObjectText) skin.getSkinObject("title");
		if (soTitle != null) {
			soTitle.setText("Choose the Type of Conversion");
		}
		
		if (soBottomContainer != null) {
			soBottomContainer.setVisible(true);
		}

		Point computeSize = shell.computeSize(600, SWT.DEFAULT, true);
		shell.setSize(computeSize);
		Utils.centerWindowRelativeTo(shell, mainShell);
	}

	private void createDeviceList(SWTSkinObjectContainer soDeviceList) {
		Composite parent = soDeviceList.getComposite();
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = gridLayout.horizontalSpacing = 0;
		parent.setLayout(gridLayout);

		DeviceManager device_manager = DeviceManagerFactory.getSingleton();
		Device[] devices = device_manager.getDevices();

		if (devices.length == 0) {
			Utils.openMessageBox(
					mainShell,
					SWT.OK,
					"No Devices Found",
					"We couldn't find any devices.  Maybe you didn't install the Vuze Transcoder Plugin?");
			shell.dispose();
			return;
		}

		fontDevice = Utils.getFontWithHeight(parent.getFont(), null, 21, SWT.BOLD);

		/**
		PaintListener paintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Rectangle ca = ((Composite) e.widget).getClientArea();
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
				e.gc.setAntialias(SWT.ON);
				e.gc.fillRoundRectangle(ca.x, ca.y, ca.width - 1, ca.height - 1, 10, 10);
				e.gc.drawRoundRectangle(ca.x, ca.y, ca.width - 1, ca.height - 1, 10, 10);
			}
		};
		**/

		for (Device device : devices) {
			if (device.getType() != Device.DT_MEDIA_RENDERER || device.isHidden()) {
				continue;
			}

			TranscodeTarget renderer = (TranscodeTarget) device;

			if (renderer.getTranscodeProfiles().length == 0) {
				continue;
			}

			/** can't align button with image */
			Button button = new Button(parent, SWT.LEFT | SWT.PUSH);
			StringBuffer sb = new StringBuffer(device.getName());
			button.setFont(fontDevice);
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.setData("DeviceMediaRenderer", device);
			button.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					selectedDevice = (TranscodeTarget) e.widget.getData("DeviceMediaRenderer");
					if (selectedDevice == null) {
						Debug.out("device is null!");
					}
					createProfileList(soList);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			String imageID = "image.sidebar.device." + device.getName() + ".big";
			System.out.println(imageID);
			listImageIDsToRelease.add(imageID);
			Image imgRenderer = ImageLoader.getInstance().getImage(imageID);

			if (ImageLoader.isRealImage(imgRenderer)) {
				button.setImage(imgRenderer);

				// buttons are center when they have an image..
				// fill with a bunch of spaces so it left aligns
				char[] c = new char[100];
				Arrays.fill(c, ' ');
				sb.append(c);
			} else {
				sb.insert(0, ' ');
			}

			button.setText(sb.toString());

			/***

			Composite c = new Composite(parent, SWT.NONE);
			c.setCursor(c.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			c.addPaintListener(paintListener);

			c.setData("DeviceMediaRenderer", device);

			RowLayout fillLayout = new RowLayout(SWT.HORIZONTAL);
			fillLayout.marginWidth = 10;
			fillLayout.marginHeight = 2;
			fillLayout.marginTop = fillLayout.marginBottom = 0;
			fillLayout.center = true;
			fillLayout.spacing = 10;
			c.setLayout(fillLayout);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.heightHint = 30;
			c.setLayoutData(gridData);
			c.addListener(SWT.MouseUp, clickListener);
			c.addListener(SWT.MouseDown, clickListener);

			Label label = new Label(c, SWT.WRAP);
			label.setCursor(c.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			label.setImage(imgRenderer);
			label.addListener(SWT.MouseUp, clickListener);
			label.addListener(SWT.MouseDown, clickListener);

			label = new Label(c, SWT.WRAP | SWT.LEFT);
			label.setFont(fontDevice);
			label.setText(device.getName());
			label.setCursor(c.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			label.addListener(SWT.MouseUp, clickListener);
			label.addListener(SWT.MouseDown, clickListener);
			*/
		}

		SWTSkinObjectText soTitle = (SWTSkinObjectText) skin.getSkinObject("title");
		if (soTitle != null) {
			soTitle.setText("Choose a device to playback to");
		}

		//shell.pack();
		Point computeSize = shell.computeSize(600, SWT.DEFAULT, true);
		shell.setSize(computeSize);
		Utils.centerWindowRelativeTo(shell, mainShell);
	}

	public abstract void closed();
	
	public int getTranscodeRequirement() {
		return transcodeRequirement;
	}
}
