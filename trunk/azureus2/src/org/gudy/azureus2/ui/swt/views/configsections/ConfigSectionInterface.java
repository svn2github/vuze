/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import java.applet.Applet;
import java.io.File;
import java.util.HashMap;

public class ConfigSectionInterface implements UISWTConfigSection {
	private final static String KEY_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	Label passwordMatch;

	private ParameterListener decisions_parameter_listener;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {

		if (decisions_parameter_listener != null) {

			COConfigurationManager.removeParameterListener(
					"MessageBoxWindow.decisions", decisions_parameter_listener);
		}
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cDisplay = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cDisplay.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		cDisplay.setLayout(layout);

		new BooleanParameter(cDisplay, "Open Details", LBLKEY_PREFIX
				+ "opendetails");
		new BooleanParameter(cDisplay, "Open Bar", false, LBLKEY_PREFIX + "openbar");

		if (!Constants.isOSX || SWT.getVersion() >= 3300) {

			BooleanParameter est = new BooleanParameter(cDisplay,
					"Enable System Tray", true, KEY_PREFIX + "enabletray");

			BooleanParameter ctt = new BooleanParameter(cDisplay, "Close To Tray",
					true, LBLKEY_PREFIX + "closetotray");
			BooleanParameter mtt = new BooleanParameter(cDisplay, "Minimize To Tray",
					false, LBLKEY_PREFIX + "minimizetotray");

			est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(ctt
					.getControls()));
			est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(mtt
					.getControls()));

		}
		
        /**
         * Default download / upload limits available in the UI.
         */
        Group limit_group = new Group(cDisplay, SWT.NULL);
        Messages.setLanguageText(limit_group, LBLKEY_PREFIX + "set_ui_transfer_speeds");
        layout = new GridLayout();
        limit_group.setLayout(layout);
        
        Label limit_group_label = new Label(limit_group, SWT.WRAP);
        Messages.setLanguageText(limit_group_label, LBLKEY_PREFIX + "set_ui_transfer_speeds.description");
        
        String[] limit_types = new String[] {"download", "upload"};
        final String limit_type_prefix = "config.ui.speed.partitions.manual.";
        for (int i=0; i<limit_types.length; i++) {
        	final BooleanParameter bp = new BooleanParameter(limit_group, limit_type_prefix + limit_types[i] + ".enabled", false, LBLKEY_PREFIX + "set_ui_transfer_speeds.description." + limit_types[i]);
        	final StringParameter sp = new StringParameter(limit_group, limit_type_prefix + limit_types[i] + ".values", "");
        	IAdditionalActionPerformer iaap = new GenericActionPerformer(new Control[] {}) {
        		public void performAction() {
        			sp.getControl().setEnabled(bp.isSelected());	
        		}
        	};
        	
            gridData = new GridData();
            gridData.widthHint = 150;
            sp.setLayoutData(gridData);
        	iaap.performAction();
        	bp.setAdditionalActionPerformer(iaap);
        }

		new BooleanParameter(cDisplay, "Send Version Info", true, LBLKEY_PREFIX
				+ "allowSendVersion");

		Composite cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 4;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter d_play_sound = new BooleanParameter(cArea,
				"Play Download Finished", false, LBLKEY_PREFIX + "playdownloadfinished");

		// OS X counterpart for alerts (see below for what is disabled)
		if (Constants.isOSX) {
			// download info 

			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 0;
			gridData.heightHint = 0;
			Composite d_filler = new Composite(cArea, SWT.NONE);
			d_filler.setSize(0, 0);
			d_filler.setLayoutData(gridData);

			final BooleanParameter d_speechEnabledParameter = new BooleanParameter(
					cArea, "Play Download Finished Announcement", LBLKEY_PREFIX
							+ "playdownloadspeech");

			final StringParameter d_speechParameter = new StringParameter(cArea,
					"Play Download Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			d_speechParameter.setLayoutData(gridData);
			((Text) d_speechParameter.getControl()).setTextLimit(40);

			d_speechEnabledParameter
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							d_speechParameter.getControls()));

			final Label d_speechInfo = new Label(cArea, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalSpan = 4;
			gridData.horizontalIndent = 24;
			d_speechInfo.setLayoutData(gridData);

			Messages.setLanguageText(d_speechInfo, LBLKEY_PREFIX
					+ "playdownloadspeech.info");
		}

		//Option disabled on OS X, as impossible to make it work correctly
		if (!Constants.isOSX) {
			Image imgOpenFolder = ImageRepository.getImage("openFolderButton");

			// download info

			gridData = new GridData();

			gridData.widthHint = 150;

			final StringParameter d_pathParameter = new StringParameter(cArea,
					"Play Download Finished File", "");

			if (d_pathParameter.getValue().length() == 0) {

				d_pathParameter.setValue("<default>");
			}

			d_pathParameter.setLayoutData(gridData);

			Button d_browse = new Button(cArea, SWT.PUSH);

			d_browse.setImage(imgOpenFolder);

			imgOpenFolder.setBackground(d_browse.getBackground());

			d_browse
					.setToolTipText(MessageText.getString("ConfigView.button.browse"));

			d_browse.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					FileDialog dialog = new FileDialog(parent.getShell(),
							SWT.APPLICATION_MODAL);
					dialog.setFilterExtensions(new String[] { "*.wav" });
					dialog.setFilterNames(new String[] { "*.wav" });

					dialog.setText(MessageText.getString(KEY_PREFIX + "wavlocation"));

					final String path = dialog.open();

					if (path != null) {

						d_pathParameter.setValue(path);

						new AEThread("SoundTest") {
							public void runSupport() {
								try {
									Applet.newAudioClip(new File(path).toURL()).play();

									Thread.sleep(2500);

								} catch (Throwable e) {

								}
							}
						}.start();
					}
				}
			});

			Label d_sound_info = new Label(cArea, SWT.WRAP);
			Messages.setLanguageText(d_sound_info, KEY_PREFIX + "wavlocation.info");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 100;
			d_sound_info.setLayoutData(gridData);

			d_play_sound
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							d_pathParameter.getControls()));
			d_play_sound
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							new Control[] { d_browse, d_sound_info }));

			// 
		}

		BooleanParameter f_play_sound = new BooleanParameter(cArea,
				"Play File Finished", false, LBLKEY_PREFIX + "playfilefinished");

		// OS X counterpart for alerts (see below for what is disabled)

		if (Constants.isOSX) {

			// per-file info

			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 0;
			gridData.heightHint = 0;
			Composite f_filler = new Composite(cArea, SWT.NONE);
			f_filler.setSize(0, 0);
			f_filler.setLayoutData(gridData);

			final BooleanParameter f_speechEnabledParameter = new BooleanParameter(
					cArea, "Play File Finished Announcement", LBLKEY_PREFIX
							+ "playfilespeech");

			final StringParameter f_speechParameter = new StringParameter(cArea,
					"Play File Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			f_speechParameter.setLayoutData(gridData);
			((Text) f_speechParameter.getControl()).setTextLimit(40);

			f_speechEnabledParameter
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							f_speechParameter.getControls()));

			final Label speechInfo = new Label(cArea, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalSpan = 4;
			gridData.horizontalIndent = 24;
			speechInfo.setLayoutData(gridData);

			Messages.setLanguageText(speechInfo, LBLKEY_PREFIX
					+ "playfilespeech.info");
		}

		//Option disabled on OS X, as impossible to make it work correctly
		if (!Constants.isOSX) {
			Image imgOpenFolder = ImageRepository.getImage("openFolderButton");

			// file info

			gridData = new GridData();

			gridData.widthHint = 150;

			final StringParameter f_pathParameter = new StringParameter(cArea,
					"Play File Finished File", "");

			if (f_pathParameter.getValue().length() == 0) {

				f_pathParameter.setValue("<default>");
			}

			f_pathParameter.setLayoutData(gridData);

			Button f_browse = new Button(cArea, SWT.PUSH);

			f_browse.setImage(imgOpenFolder);

			imgOpenFolder.setBackground(f_browse.getBackground());

			f_browse
					.setToolTipText(MessageText.getString("ConfigView.button.browse"));

			f_browse.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					FileDialog dialog = new FileDialog(parent.getShell(),
							SWT.APPLICATION_MODAL);
					dialog.setFilterExtensions(new String[] { "*.wav" });
					dialog.setFilterNames(new String[] { "*.wav" });

					dialog.setText(MessageText.getString(KEY_PREFIX + "wavlocation"));

					final String path = dialog.open();

					if (path != null) {

						f_pathParameter.setValue(path);

						new AEThread("SoundTest") {
							public void runSupport() {
								try {
									Applet.newAudioClip(new File(path).toURL()).play();

									Thread.sleep(2500);

								} catch (Throwable e) {

								}
							}
						}.start();
					}
				}
			});

			Label f_sound_info = new Label(cArea, SWT.WRAP);
			Messages.setLanguageText(f_sound_info, KEY_PREFIX + "wavlocation.info");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 100;
			f_sound_info.setLayoutData(gridData);

			f_play_sound
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							f_pathParameter.getControls()));
			f_play_sound
					.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
							new Control[] { f_browse, f_sound_info }));
		}

		cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter popup_dl_completed = new BooleanParameter(cArea,
				"Popup Download Finished", false, LBLKEY_PREFIX + "popupdownloadfinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_completed.setLayoutData(gridData);
		
		BooleanParameter popup_file_completed = new BooleanParameter(cArea,
				"Popup File Finished", false, LBLKEY_PREFIX + "popupfilefinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_file_completed.setLayoutData(gridData);

		cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (!Constants.isOSX) {

			BooleanParameter confirm = new BooleanParameter(cArea,
					"confirmationOnExit", false,
					"ConfigView.section.style.confirmationOnExit");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			confirm.setLayoutData(gridData);
		}
		
		cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// config torrent removal
		
		BooleanParameter confirm_removal = new BooleanParameter(cArea,
				"confirm_torrent_removal", KEY_PREFIX + "confirm_torrent_removal");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		confirm_removal.setLayoutData(gridData);

		// clear remembered decisions

		final Label clear_label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(clear_label, KEY_PREFIX + "cleardecisions");

		final Button clear_decisions = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(clear_decisions, KEY_PREFIX
				+ "cleardecisionsbutton");

		clear_decisions.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				COConfigurationManager.setParameter("MessageBoxWindow.decisions",
						new HashMap());
			}
		});

		decisions_parameter_listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				if (clear_decisions.isDisposed()) {

					// tidy up from previous incarnations

					COConfigurationManager.removeParameterListener(
							"MessageBoxWindow.decisions", this);

				} else {

					boolean enabled = COConfigurationManager.getMapParameter(
							"MessageBoxWindow.decisions", new HashMap()).size() > 0;

					clear_label.setEnabled(enabled);
					clear_decisions.setEnabled(enabled);
				}
			}
		};

		decisions_parameter_listener.parameterChanged(null);

		COConfigurationManager.addParameterListener("MessageBoxWindow.decisions",
				decisions_parameter_listener);

		// password

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "password");

		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw1 = new PasswordParameter(cArea, "Password");
		pw1.setLayoutData(gridData);
		Text t1 = (Text) pw1.getControl();

		//password confirm

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordconfirm");
		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw2 = new PasswordParameter(cArea, "Password Confirm");
		pw2.setLayoutData(gridData);
		Text t2 = (Text) pw2.getControl();

		// password activated

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordmatch");
		passwordMatch = new Label(cArea, SWT.NULL);
		gridData = new GridData();
		gridData.widthHint = 150;
		passwordMatch.setLayoutData(gridData);
		refreshPWLabel();

		t1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});
		t2.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});

		// drag-drop

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.style.dropdiraction");

		String[] drop_options = {
				"ConfigView.section.style.dropdiraction.opentorrents",
				"ConfigView.section.style.dropdiraction.sharefolder",
				"ConfigView.section.style.dropdiraction.sharefoldercontents",
				"ConfigView.section.style.dropdiraction.sharefoldercontentsrecursive", };

		String dropLabels[] = new String[drop_options.length];
		String dropValues[] = new String[drop_options.length];
		for (int i = 0; i < drop_options.length; i++) {

			dropLabels[i] = MessageText.getString(drop_options[i]);
			dropValues[i] = "" + i;
		}
		new StringListParameter(cArea, "config.style.dropdiraction", "1",
				dropLabels, dropValues);

		// reset associations

		final PlatformManager platform = PlatformManagerFactory
				.getPlatformManager();

		if (platform
				.hasCapability(PlatformManagerCapabilities.RegisterFileAssociations)) {

			Composite cResetAssoc = new Composite(cArea, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			cResetAssoc.setLayout(layout);
			cResetAssoc.setLayoutData(new GridData());

			label = new Label(cResetAssoc, SWT.NULL);
			Messages.setLanguageText(label, KEY_PREFIX + "resetassoc");

			Button reset = new Button(cResetAssoc, SWT.PUSH);
			Messages.setLanguageText(reset, KEY_PREFIX + "resetassocbutton"); //$NON-NLS-1$

			reset.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {

					try {
						platform.registerApplication();

					} catch (PlatformManagerException e) {

						Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
								"Failed to register application", e));
					}
				}
			});

			new BooleanParameter(cArea, "config.interface.checkassoc", true,
					KEY_PREFIX + "checkassoc");

		}

		return cDisplay;
	}

	private void refreshPWLabel() {

		if (passwordMatch == null || passwordMatch.isDisposed())
			return;
		byte[] password = COConfigurationManager.getByteParameter("Password", ""
				.getBytes());
		COConfigurationManager.setParameter("Password enabled", false);
		if (password.length == 0) {
			passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
					+ "passwordmatchnone"));
		} else {
			byte[] confirm = COConfigurationManager.getByteParameter(
					"Password Confirm", "".getBytes());
			if (confirm.length == 0) {
				passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
						+ "passwordmatchno"));
			} else {
				boolean same = true;
				for (int i = 0; i < password.length; i++) {
					if (password[i] != confirm[i])
						same = false;
				}
				if (same) {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchyes"));
					COConfigurationManager.setParameter("Password enabled", true);
				} else {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchno"));
				}
			}
		}
	}

}
