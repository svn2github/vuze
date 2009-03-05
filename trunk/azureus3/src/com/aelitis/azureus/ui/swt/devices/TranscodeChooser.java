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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Debug;
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

	private Image imgRenderer;

	protected TranscodeTarget selectedDevice;

	protected TranscodeProfile selectedProfile;

	private Listener clickListener;

	private SWTSkinObjectContainer soList;

	private Shell mainShell;

	public TranscodeChooser() {
	}

	public TranscodeChooser(TranscodeTarget device) {
		selectedDevice = device;
	}

	public void show() {
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

		// temp
		imgRenderer = ImageLoader.getInstance().getImage(
				"image.sidebar.device.renderer");

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
							if ( selectedDevice == null ){
								Debug.out( "device is null!" );
							}
							createProfileList(soList);
						} else {
							selectedProfile = (TranscodeProfile) widget.getData("TranscodeProfile");
							if ( selectedProfile == null ){
								Debug.out( "profile is null!" );
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
				ImageLoader.getInstance().releaseImage("image.sidebar.device.renderer");
			}
		});

		shell.open();
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
				Widget widget = (event.widget instanceof Label)
						? ((Label) event.widget).getParent() : event.widget;
				TranscodeProfile profile = (TranscodeProfile) widget.getData("TranscodeProfile");
				if (event.type == SWT.MouseEnter) {
					if (soInfoTitle != null) {
						soInfoTitle.setText(profile.getName());
					}
					if (soInfoText != null) {
						soInfoText.setText(profile.getDescription() + "\n"
								+ profile.getFileExtension());
						Point computeSize = shell.computeSize(shell.getClientArea().width, SWT.DEFAULT, true);
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
					Point computeSize = shell.computeSize(shell.getClientArea().width, SWT.DEFAULT, true);
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

			final Label lblImage = new Label(c, SWT.BORDER);
			lblImage.addListener(SWT.MouseEnter, listenerMouseInout);
			lblImage.addListener(SWT.MouseUp, clickListener);
			lblImage.addListener(SWT.MouseDown, clickListener);
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
									lblImage.setImage(image);
									GridData gridData = (GridData) lblImage.getLayoutData();
									gridData.heightHint = -1;
									gridData.widthHint = -1;
									lblImage.setLayoutData(gridData);
									lblImage.getShell().layout(new Control[] {
										lblImage
									});
								}
							}
						});
				if (image != null) {
					lblImage.setImage(image);
					gridData.heightHint = -1;
					gridData.widthHint = -1;
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
			soTitle.setText("Choose a transcoding profile");
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

		fontDevice = Utils.getFontWithHeight(parent.getFont(), null, 22, SWT.BOLD);

		PaintListener paintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Rectangle ca = ((Composite) e.widget).getClientArea();
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				e.gc.setAntialias(SWT.ON);
				e.gc.drawRoundRectangle(ca.x, ca.y, ca.width - 1, ca.height - 1, 10, 10);
			}
		};

		for (Device device : devices) {
			if (device.getType() != Device.DT_MEDIA_RENDERER || device.isHidden()) {
				continue;
			}

			TranscodeTarget renderer = (TranscodeTarget) device;

			if (renderer.getTranscodeProfiles().length == 0) {
				continue;
			}

			/** can't align button with image
			Button button = new Button(parent, SWT.RIGHT | SWT.PUSH);
			button.setText(device.getName());
			button.setFont(fontDevice);
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			// temp:
			button.setImage(imgRenderer);
			
			**/

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

		}

		SWTSkinObjectText soTitle = (SWTSkinObjectText) skin.getSkinObject("title");
		if (soTitle != null) {
			soTitle.setText("Choose a device to playback to");
		}

		shell.pack();
		Utils.centerWindowRelativeTo(shell, mainShell);
	}

	public abstract void closed();
}
