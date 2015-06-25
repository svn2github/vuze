/**
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

package org.gudy.azureus2.ui.swt.views;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.config.generic.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.FontUtils;

/**
 * @author TuxPaper
 * @created Mar 11, 2015
 *
 */
public class TagSettingsView
	implements UISWTViewCoreEventListener, TagTypeListener
{

	private UISWTView swtView;

	private Composite cMainComposite;

	private ScrolledComposite sc;

	private Tag tag;

	public static class Params
	{
		private Control cName;

		private ColorParameter tagColor;

		private GenericIntParameter maxDownloadSpeed;

		private GenericIntParameter maxUploadSpeed;

		private GenericBooleanParameter viewInSideBar;

		private GenericBooleanParameter isPublic;

		public GenericBooleanParameter uploadPriority;

		public GenericFloatParameter min_sr;

		public GenericFloatParameter max_sr;

		public folderOption initalSaveFolder;

		public folderOption moveOnCompleteFolder;

		public folderOption copyOnCompleteFolder;

		public Text constraints;
	}

	private Params params = null;

	public TagSettingsView() {
	}

	// @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener#eventOccurred(org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent)
	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				Messages.updateLanguageForControl(cMainComposite);
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				Object ds = event.getData();
				dataSourceChanged(ds);
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	private void delete() {
		dataSourceChanged(null);
		params = null;
	}

	private void refresh() {
	}

	private void dataSourceChanged(Object ds) {

		if (tag != null) {
			TagType tagType = tag.getTagType();
			tagType.removeTagTypeListener(this);
		}

		if (ds instanceof Tag) {
			tag = (Tag) ds;
		} else if (ds instanceof Object[]) {
			Object[] objects = (Object[]) ds;
			if (objects.length == 1 && objects[0] instanceof Tag) {
				tag = (Tag) objects[0];
			} else {
				tag = null;
			}
		} else {
			tag = null;
		}

		if (tag != null) {
			TagType tagType = tag.getTagType();
			tagType.addTagTypeListener(this, true);
		}
		initialize(null);

	}
	
	private void initialize(final Composite parent) {
		Utils.execSWTThread(
			new Runnable()
			{
				public void 
				run()
				{
					swt_initialize( parent );
				}
			});
	}
	
	private void swt_initialize(Composite parent) {
		if (cMainComposite == null || cMainComposite.isDisposed()) {
			if (parent == null || parent.isDisposed()) {
				return;
			}
			sc = new ScrolledComposite(parent, SWT.V_SCROLL);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.getVerticalBar().setIncrement(16);
			Layout parentLayout = parent.getLayout();
			if (parentLayout instanceof GridLayout) {
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				sc.setLayoutData(gd);
			} else if (parentLayout instanceof FormLayout) {
				sc.setLayoutData(Utils.getFilledFormData());
			}

			cMainComposite = new Composite(sc, SWT.NONE);

			sc.setContent(cMainComposite);
		} else {
			Utils.disposeComposite(cMainComposite, false);
		}

		if (tag == null) {
			params = null;
			cMainComposite.setLayout(new FillLayout());
			Label label = new Label(cMainComposite, SWT.NONE);
			label.setText("Select one tag to see the tag's settings");
		} else {
			params = new Params();

			GridData gd;
			GridLayout gridLayout;
			gridLayout = new GridLayout(1, false);
			gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
			gridLayout.marginHeight = gridLayout.marginWidth = 0;
			cMainComposite.setLayout(gridLayout);

			Composite cSection1 = new Composite(cMainComposite, SWT.NONE);
			gridLayout = new GridLayout(4, false);
			gridLayout.marginHeight = 0;
			cSection1.setLayout(gridLayout);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			cSection1.setLayoutData(gd);

			Composite cSection2 = new Composite(cMainComposite, SWT.NONE);
			gridLayout = new GridLayout(4, false);
			cSection2.setLayout(gridLayout);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			cSection2.setLayoutData(gd);

			Label label;

			// Field: Tag Type
			label = new Label(cSection1, SWT.NONE);
			label.setText(tag.getTagType().getTagTypeName(true));
			FontUtils.setFontHeight(label, 12, SWT.BOLD);
			gd = new GridData();
			gd.horizontalSpan = 4;
			label.setLayoutData(gd);

			// Field: Name
			label = new Label(cSection1, SWT.NONE);
			Messages.setLanguageText(label, "MinimizedWindow.name");
			gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			label.setLayoutData(gd);

			if (!tag.getTagType().isTagTypeAuto()) {
				Text txtName = new Text(cSection1, SWT.BORDER);
				params.cName = txtName;
				gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
				txtName.setLayoutData(gd);

				txtName.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						try {
							String newName = ((Text) e.widget).getText();
							if (!tag.getTagName(true).equals(newName)) {
								tag.setTagName(newName);
							}
						} catch (TagException e1) {
							Debug.out(e1);
						}
					}
				});
			} else {
				label = new Label(cSection1, SWT.NONE);
				gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
				label.setLayoutData(gd);
				params.cName = label;
			}

			// Field: Color
			label = new Label(cSection1, SWT.NONE);
			Messages.setLanguageText(label, "label.color");
			int[] color = tag.getColor();
			params.tagColor = new ColorParameter(cSection1, null, color[0], color[1],
					color[2]) {
				// @see org.gudy.azureus2.ui.swt.config.ColorParameter#newColorChosen(org.eclipse.swt.graphics.RGB)
				public void newColorChosen(RGB newColor) {
					tag.setColor(new int[] {
						newColor.red,
						newColor.green,
						newColor.blue
					});
				}
			};

			// Field: Visible

			params.viewInSideBar = new GenericBooleanParameter(
					new GenericParameterAdapter() {
						public boolean getBooleanValue(String key) {
							return tag.isVisible();
						}

						public boolean getBooleanValue(String key, boolean def) {
							return tag.isVisible();
						}

						public void setBooleanValue(String key, boolean value) {
							tag.setVisible(value);
						}
					}, cSection2, null, "TagSettings.viewInSideBar");
			gd = new GridData();
			gd.horizontalSpan = 4;
			params.viewInSideBar.setLayoutData(gd);

			// Field: Public
			if (tag.canBePublic()) {
				params.isPublic = new GenericBooleanParameter(
						new GenericParameterAdapter() {
							public boolean getBooleanValue(String key) {
								return tag.isPublic();
							}

							public boolean getBooleanValue(String key, boolean def) {
								return tag.isPublic();
							}

							public void setBooleanValue(String key, boolean value) {
								tag.setPublic(value);
							}
						}, cSection2, null, "TagAddWindow.public.checkbox");
				gd = new GridData();
				gd.horizontalSpan = 4;
				params.isPublic.setLayoutData(gd);
			}

			////////////////////

			Group gTransfer = new Group(cMainComposite, SWT.NONE);
			gTransfer.setText("Transfer Settings");
			gridLayout = new GridLayout(4, false);
			gTransfer.setLayout(gridLayout);

			gd = new GridData(SWT.NONE, SWT.NONE, false, false, 4, 1);
			gTransfer.setLayoutData(gd);

			if (tag instanceof TagFeatureRateLimit) {
				final TagFeatureRateLimit rl = (TagFeatureRateLimit) tag;
				String k_unit = DisplayFormatters.getRateUnitBase10(
						DisplayFormatters.UNIT_KB).trim();

				if (rl.supportsTagDownloadLimit()) {
					// Field: Download Limit
					if (rl.supportsTagDownloadLimit()) {

						gd = new GridData();
						label = new Label(gTransfer, SWT.NULL);
						label.setLayoutData(gd);
						label.setText(k_unit + " " + MessageText.getString(
								"GeneralView.label.maxdownloadspeed.tooltip"));

						gd = new GridData();
						//gd.horizontalSpan = 3;
						params.maxDownloadSpeed = new GenericIntParameter(
								new GenericParameterAdapter() {
									public int getIntValue(String key) {
										int limit = rl.getTagDownloadLimit();
										return limit < 0 ? limit : limit / 1024;
									}

									public int getIntValue(String key, int def) {
										return getIntValue(key);
									}

									public void setIntValue(String key, int value) {
										if (value == -1) {
											rl.setTagDownloadLimit(-1);
										} else {
											rl.setTagDownloadLimit(value * 1024);
										}
									}

									public boolean resetIntDefault(String key) {
										return false;
									}
								}, gTransfer, null, -1, Integer.MAX_VALUE);
						params.maxDownloadSpeed.setLayoutData(gd);
					}
				}

				// Upload Limit
				if (rl.supportsTagUploadLimit()) {
					gd = new GridData();
					label = new Label(gTransfer, SWT.NULL);
					label.setLayoutData(gd);
					label.setText(k_unit + " " + MessageText.getString(
							"GeneralView.label.maxuploadspeed.tooltip"));

					gd = new GridData();
					//gd.horizontalSpan = 3;
					params.maxUploadSpeed = new GenericIntParameter(
							new GenericParameterAdapter() {
								public int getIntValue(String key) {
									int limit = rl.getTagUploadLimit();
									return limit < 0 ? limit : limit / 1024;
								}

								public int getIntValue(String key, int def) {
									return getIntValue(key);
								}

								public void setIntValue(String key, int value) {
									if (value == -1) {
										rl.setTagUploadLimit(value);
									} else {
										rl.setTagUploadLimit(value * 1024);
									}
								}

								public boolean resetIntDefault(String key) {
									return false;
								}
							}, gTransfer, null, -1, Integer.MAX_VALUE);
					params.maxUploadSpeed.setLayoutData(gd);
				}

				// Field: Upload Priority
				if (rl.getTagUploadPriority() >= 0) {
					params.uploadPriority = new GenericBooleanParameter(
							new GenericParameterAdapter() {
								public boolean getBooleanValue(String key) {
									return rl.getTagUploadPriority() > 0;
								}

								public boolean getBooleanValue(String key, boolean def) {
									return getBooleanValue(key);
								}

								public void setBooleanValue(String key, boolean value) {
									rl.setTagUploadPriority(value ? 1 : 0);
								}
							}, gTransfer, null, "cat.upload.priority");
					gd = new GridData();
					gd.horizontalSpan = 4;
					params.uploadPriority.setLayoutData(gd);
				}

				// Field: Min Share
				if (rl.getTagMinShareRatio() >= 0) {
					label = new Label(gTransfer, SWT.NONE);
					Messages.setLanguageText(label, "TableColumn.header.min_sr");
					gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
					label.setLayoutData(gd);

					params.min_sr = new GenericFloatParameter(
							new GenericParameterAdapter() {
								public float getFloatValue(String key) {
									return rl.getTagMinShareRatio() / 1000f;
								}

								public void setFloatValue(String key, float value) {
									rl.setTagMinShareRatio((int) (value * 1000));
								}
							}, gTransfer, null, 0, Float.MAX_VALUE, true, 3);
					gd = new GridData();
					//gd.horizontalSpan = 3;
					gd.widthHint = 50;
					params.min_sr.setLayoutData(gd);
				}

				// Field: Max Share
				if (rl.getTagMaxShareRatio() >= 0) {
					label = new Label(gTransfer, SWT.NONE);
					Messages.setLanguageText(label, "TableColumn.header.max_sr");
					gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
					label.setLayoutData(gd);

					params.max_sr = new GenericFloatParameter(
							new GenericParameterAdapter() {
								public float getFloatValue(String key) {
									return rl.getTagMaxShareRatio() / 1000f;
								}

								public void setFloatValue(String key, float value) {
									rl.setTagMaxShareRatio((int) (value * 1000));
								}
							}, gTransfer, null, 0, Float.MAX_VALUE, true, 3);
					gd = new GridData();
					//gd.horizontalSpan = 3;
					gd.widthHint = 50;
					params.max_sr.setLayoutData(gd);
				}
			}

			/////////////////////////////////

			if (tag instanceof TagFeatureFileLocation) {
				final TagFeatureFileLocation fl = (TagFeatureFileLocation) tag;

				if (fl.supportsTagCopyOnComplete() || fl.supportsTagInitialSaveFolder()
						|| fl.supportsTagMoveOnComplete()) {

					Group gFiles = new Group(cMainComposite, SWT.NONE);
					gFiles.setText("File Settings");
					gridLayout = new GridLayout(4, false);
					gFiles.setLayout(gridLayout);

					gd = new GridData(SWT.FILL, SWT.NONE, true, false, 4, 1);
					gFiles.setLayoutData(gd);

					if (fl.supportsTagInitialSaveFolder()) {
						params.initalSaveFolder = new folderOption(gFiles,
								"label.init.save.loc") {
							public void setFolder(File folder) {
								fl.setTagInitialSaveFolder(folder);
							}

							public File getFolder() {
								return fl.getTagInitialSaveFolder();
							}
						};
					}

					if (fl.supportsTagMoveOnComplete()) {
						params.moveOnCompleteFolder = new folderOption(gFiles,
								"label.move.on.comp") {
							public void setFolder(File folder) {
								fl.setTagMoveOnCompleteFolder(folder);
							}

							public File getFolder() {
								return fl.getTagMoveOnCompleteFolder();
							}
						};
					}

					if (fl.supportsTagCopyOnComplete()) {
						params.copyOnCompleteFolder = new folderOption(gFiles,
								"label.copy.on.comp") {
							public void setFolder(File folder) {
								fl.setTagCopyOnCompleteFolder(folder);
							}

							public File getFolder() {
								return fl.getTagCopyOnCompleteFolder();
							}
						};
					}
				}
			}

			///////////////////////////////

			if (tag.getTagType().hasTagTypeFeature(TagFeature.TF_PROPERTIES)
					&& (tag instanceof TagFeatureProperties)) {
				TagFeatureProperties tfp = (TagFeatureProperties) tag;

				final TagProperty propConstraint = tfp.getProperty(
						TagFeatureProperties.PR_CONSTRAINT);
				if (propConstraint != null) {
					Group gConstraint = new Group(cMainComposite, SWT.NONE);
					Messages.setLanguageText(gConstraint, "tag.property.constraint");
					gridLayout = new GridLayout(3, false);
					gConstraint.setLayout(gridLayout);

					gd = new GridData(SWT.FILL, SWT.NONE, true, false, 4, 1);
					gConstraint.setLayoutData(gd);

					params.constraints = new Text(gConstraint,
							SWT.WRAP | SWT.BORDER | SWT.MULTI);
					gd = new GridData(SWT.FILL, SWT.NONE, true, false, 3, 1);
					gd.heightHint = 40;
					params.constraints.setLayoutData(gd);
					params.constraints.addKeyListener(new KeyListener() {
						public void keyReleased(KeyEvent e) {
						}

						public void keyPressed(KeyEvent e) {
							params.constraints.setData("skipset", 1);
						}
					});

					Button btnSave = new Button(gConstraint, SWT.PUSH);
					btnSave.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							propConstraint.setStringList(new String[] {
								params.constraints.getText()
							});
						}
					});
					Messages.setLanguageText(btnSave, "Button.save");

					Button btnReset = new Button(gConstraint, SWT.PUSH);
					btnReset.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							params.constraints.setData("skipset", null);
							swt_updateFields();
						}
					});
					Messages.setLanguageText(btnReset, "Button.reset");

					Link lblAboutConstraint = new Link(gConstraint, SWT.WRAP);
					lblAboutConstraint.setLayoutData(
							Utils.getWrappableLabelGridData(1, GridData.GRAB_HORIZONTAL));
					lblAboutConstraint.setText(
							MessageText.getString("tag.constraints.info"));
					lblAboutConstraint.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							if (event.text != null && (event.text.startsWith("http://")
									|| event.text.startsWith("https://"))) {
								Utils.launch(event.text);
							}
						}
					});

				}
			}

			swt_updateFields();
		}

		cMainComposite.layout();
		sc.setMinSize(cMainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private static abstract class folderOption
	{
		private Button btnClear;

		private Label lblValue;

		public folderOption(final Composite parent, String labelTextID) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			Image imgOpenFolder = imageLoader.getImage("openFolderButton");

			GridData gd = new GridData();
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(gd);
			Messages.setLanguageText(label, labelTextID);

			Button browse = new Button(parent, SWT.PUSH);
			browse.setImage(imgOpenFolder);
			imgOpenFolder.setBackground(browse.getBackground());
			browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

			browse.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),
							SWT.APPLICATION_MODAL);
					String filterPath;
					File tagInitialSaveFolder = getFolder();
					filterPath = tagInitialSaveFolder != null
							? tagInitialSaveFolder.toString()
							: COConfigurationManager.getStringParameter("Default save path");
					dialog.setFilterPath(filterPath);
					dialog.setMessage(MessageText.getString("label.init.save.loc"));
					dialog.setText(MessageText.getString("label.init.save.loc"));
					final String path = dialog.open();
					if (path != null) {
						Utils.getOffOfSWTThread(new AERunnable() {
							public void runSupport() {
								setFolder(new File(path));
							}
						});
					}
				}
			});

			lblValue = new Label(parent, SWT.WRAP);
			gd = Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL);
			gd.verticalAlignment = SWT.CENTER;
			lblValue.setLayoutData(gd);

			btnClear = new Button(parent, SWT.PUSH);
			Messages.setLanguageText(btnClear, "Button.clear");
			btnClear.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					setFolder(null);
				}
			});
		}

		public void update() {
			File folder = getFolder();
			if (folder == null) {
				Messages.setLanguageText(lblValue, "label.none.assigned");
			} else {
				lblValue.setText(folder.toString());
			}
			btnClear.setVisible(folder != null);
		}

		public abstract File getFolder();

		public abstract void setFolder(File folder);
	}

	private String getFullTitle() {
		return MessageText.getString("TagSettingsView.title");
	}

	private void swt_updateFields() {
		if (tag == null || params == null) {
			initialize(null);
			return;
		}

		if (params.cName != null && !params.cName.isDisposed()) {
			String name = tag.getTagName(true);
			if (params.cName instanceof Text) {
				Text txt = (Text) params.cName;
				if (!txt.getText().equals(name)) {
					txt.setText(name);
				}
			} else if (params.cName instanceof Label) {
				Label lbl = (Label) params.cName;
				lbl.setText(name);
			}
		}

		if (params.tagColor != null) {
			int[] color = tag.getColor();
			params.tagColor.setColor(color[0], color[1], color[2]);
		}

		if (params.viewInSideBar != null) {
			params.viewInSideBar.refresh();
		}

		if (params.isPublic != null) {
			params.isPublic.refresh();
		}

		if (params.maxDownloadSpeed != null) {
			params.maxDownloadSpeed.resetToDefault();
		}
		if (params.maxUploadSpeed != null) {
			params.maxUploadSpeed.resetToDefault();
		}

		if (params.uploadPriority != null) {
			params.uploadPriority.refresh();
		}
		if (params.min_sr != null) {
			params.min_sr.refresh();
		}
		if (params.max_sr != null) {
			params.max_sr.refresh();
		}

		if (params.initalSaveFolder != null) {
			params.initalSaveFolder.update();
		}
		if (params.moveOnCompleteFolder != null) {
			params.moveOnCompleteFolder.update();
		}
		if (params.copyOnCompleteFolder != null) {
			params.copyOnCompleteFolder.update();
		}
		if (params.constraints != null
				&& params.constraints.getData("skipset") == null) {
			String text = "";
			if (tag.getTagType().hasTagTypeFeature(TagFeature.TF_PROPERTIES)
					&& (tag instanceof TagFeatureProperties)) {
				TagFeatureProperties tfp = (TagFeatureProperties) tag;

				TagProperty propConstraint = tfp.getProperty(
						TagFeatureProperties.PR_CONSTRAINT);
				if (propConstraint != null) {
					String[] stringList = propConstraint.getStringList();
					// constraint only has one entry
					if (stringList.length == 1 && stringList.length > 0) {
						text = stringList[0];
					}
				}
			}
			params.constraints.setText(text);
		}
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagTypeChanged(com.aelitis.azureus.core.tag.TagType)
	public void tagTypeChanged(TagType tag_type) {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagAdded(com.aelitis.azureus.core.tag.Tag)
	public void tagAdded(Tag tag) {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagChanged(com.aelitis.azureus.core.tag.Tag)
	public void tagChanged(final Tag changedTag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (changedTag.equals(tag)) {
					swt_updateFields();
				}
			}
		});
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagRemoved(com.aelitis.azureus.core.tag.Tag)
	public void tagRemoved(Tag tag) {
		// TODO Auto-generated method stub

	}

}
