/*
 * File    : ConfigSectionInterfaceAlerts.java
 * Created : Dec 4, 2006
 * By      : TuxPaper
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import java.applet.Applet;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ConfigSectionInterfaceAlerts
	implements UISWTConfigSection
{
	private final static String INTERFACE_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private final static String STYLE_PREFIX = "ConfigView.section.style.";

	private final static int REQUIRED_MODE = 0;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	/* Name of section will be pulled from 
	 * ConfigView.section.<i>configSectionGetName()</i>
	 */
	public String configSectionGetName() {
		return "interface.alerts";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}

	public int maxUserMode() {
		return REQUIRED_MODE;
	}

	public Composite configSectionCreate(final Composite parent) {
		Image imgOpenFolder = null;
		ImageLoader imageLoader = ImageLoader.getInstance();
		imgOpenFolder = imageLoader.getImage("openFolderButton");

		GridData gridData;
		GridLayout layout;

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginWidth = 0;
		//layout.numColumns = 2;
		cSection.setLayout(layout);

		Composite cArea = new Composite(cSection, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 4;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// DOWNLOAD FINISHED
		
		playSoundWhen(
				cArea, imgOpenFolder, 
				"Play Download Finished Announcement",
				"Play Download Finished Announcement Text",
				"playdownloadspeech",
				"Play Download Finished",
				"Play Download Finished File",
				"playdownloadfinished" );
		
		// DOWNLOAD ERROR
		
		playSoundWhen(
			cArea, imgOpenFolder, 
			"Play Download Error Announcement",
			"Play Download Error Announcement Text",
			"playdownloaderrorspeech",
			"Play Download Error",
			"Play Download Error File",
			"playdownloaderror" );

		
		// FILE FINISHED
		
		playSoundWhen(
				cArea, imgOpenFolder, 
				"Play File Finished Announcement",
				"Play File Finished Announcement Text",
				"playfilespeech",
				"Play File Finished",
				"Play File Finished File",
				"playfilefinished" );
		

		// NOTIFICATION ADDED
		
		playSoundWhen(
				cArea, imgOpenFolder, 
				"Play Notification Added Announcement",
				"Play Notification Added Announcement Text",
				"playnotificationaddedspeech",
				"Play Notification Added",
				"Play Notification Added File",
				"playnotificationadded" );
		
			// xxxxxxxxxxxxxxxx
		
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals("az3");
		
		if ( isAZ3 ){
			
			BooleanParameter p = new BooleanParameter(cArea,
					"Request Attention On New Download", LBLKEY_PREFIX + "dl.add.req.attention");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			p.setLayoutData(gridData);
		}
		
		BooleanParameter activate_win = new BooleanParameter(cArea,
				"Activate Window On External Download", LBLKEY_PREFIX + "show.win.on.add");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		activate_win.setLayoutData(gridData);
		
		BooleanParameter no_auto_activate = new BooleanParameter(cArea,
				"Reduce Auto Activate Window", LBLKEY_PREFIX + "reduce.auto.activate");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		no_auto_activate.setLayoutData(gridData);
		
		
			// popups group
		
		Group gPopup = new Group(cSection, SWT.NULL);
		Messages.setLanguageText( gPopup, "label.popups" );
		layout = new GridLayout();
		layout.numColumns = 2;
		gPopup.setLayout(layout);
		gPopup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter popup_dl_added = new BooleanParameter(gPopup,
				"Popup Download Added", LBLKEY_PREFIX + "popupdownloadadded");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_added.setLayoutData(gridData);

		BooleanParameter popup_dl_completed = new BooleanParameter(gPopup,
				"Popup Download Finished", LBLKEY_PREFIX + "popupdownloadfinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_completed.setLayoutData(gridData);

		BooleanParameter popup_dl_error = new BooleanParameter(gPopup,
				"Popup Download Error", LBLKEY_PREFIX + "popupdownloaderror");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_error.setLayoutData(gridData);
		
		BooleanParameter popup_file_completed = new BooleanParameter(gPopup,
				"Popup File Finished", LBLKEY_PREFIX + "popupfilefinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_file_completed.setLayoutData(gridData);

		BooleanParameter disable_sliding = new BooleanParameter(gPopup,
				"GUI_SWT_DisableAlertSliding", STYLE_PREFIX + "disableAlertSliding");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		disable_sliding.setLayoutData(gridData);

		// Timestamps for popup alerts.
		BooleanParameter show_alert_timestamps = new BooleanParameter(gPopup,
				"Show Timestamp For Alerts", LBLKEY_PREFIX + "popup.timestamp");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		show_alert_timestamps.setLayoutData(gridData);

		// Auto-hide popup setting.
		Label label = new Label(gPopup, SWT.WRAP);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "popup.autohide");
		label.setLayoutData(new GridData());
		IntParameter auto_hide_alert = new IntParameter(gPopup,
				"Message Popup Autoclose in Seconds", 0, 86400);
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		auto_hide_alert.setLayoutData(gridData);

		return cSection;
	}
	
	private void
	playSoundWhen(
		final Composite 	cArea,
		Image				imgOpenFolder,
		String				announceEnableConfig,
		String				announceKeyConfig,
		String				announceResource,
		String				playEnableConfig,
		String				playKeyConfig,
		String				playResource )
	{
		if (Constants.isOSX) {
			// download info 

			new BooleanParameter(
					cArea, announceEnableConfig, LBLKEY_PREFIX + announceResource );

			final StringParameter d_speechParameter = new StringParameter(cArea,announceKeyConfig);
			GridData gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			d_speechParameter.setLayoutData(gridData);
			((Text) d_speechParameter.getControl()).setTextLimit(40);
		}

		new BooleanParameter(cArea,playEnableConfig, LBLKEY_PREFIX +playResource );

		// download info

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);

		final StringParameter e_pathParameter = new StringParameter(cArea,playKeyConfig, "");

		if (e_pathParameter.getValue().length() == 0) {

			e_pathParameter.setValue("<default>");
		}

		e_pathParameter.setLayoutData(gridData);

		Button d_browse = new Button(cArea, SWT.PUSH);

		d_browse.setImage(imgOpenFolder);

		imgOpenFolder.setBackground(d_browse.getBackground());

		d_browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

		d_browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(cArea.getShell(),
						SWT.APPLICATION_MODAL);
				dialog.setFilterExtensions(new String[] {
					"*.wav"
				});
				dialog.setFilterNames(new String[] {
					"*.wav"
				});

				dialog.setText(MessageText.getString(INTERFACE_PREFIX + "wavlocation"));

				final String path = dialog.open();

				if (path != null) {

					e_pathParameter.setValue(path);

					new AEThread2("SoundTest") {
						public void run() {
							try {
								Applet.newAudioClip(new File(path).toURI().toURL()).play();

								Thread.sleep(2500);

							} catch (Throwable e) {

							}
						}
					}.start();
				}
			}
		});

		Label d_sound_info = new Label(cArea, SWT.WRAP);
		Messages.setLanguageText(d_sound_info, INTERFACE_PREFIX
				+ "wavlocation.info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 100;
		Utils.setLayoutData(d_sound_info, gridData);
	}
}
