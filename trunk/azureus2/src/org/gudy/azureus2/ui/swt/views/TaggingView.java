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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.utils.TagButtonsUI;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;
import org.gudy.azureus2.ui.swt.views.utils.TagButtonsUI.TagButtonTrigger;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.tag.TagTypeListener.TagEvent;
import com.aelitis.azureus.ui.UIFunctions.TagReturner;

/**
 * View showing tags set on selected taggable item(s).  Sometimes easier than
 * drag and dropping to buttons/sidebar
 * 
 * @author TuxPaper
 * @created Mar 23, 2015
 *
 */
public class TaggingView
	implements UISWTViewCoreEventListener, TagTypeListener
{
	public static final String MSGID_PREFIX = "TaggingView";

	private UISWTView swtView;

	private Composite cMainComposite;

	private ScrolledComposite sc;

	private List<Taggable> taggables;

	private Composite parent;

	private TagButtonsUI tagButtonsUI;

	public TaggingView() {
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
				parent = (Composite) event.getData();
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
				initialize();
				if (taggables == null) {
					dataSourceChanged(swtView.getDataSource());
				}
				break;
				
			case UISWTViewEvent.TYPE_FOCUSLOST:
				delete();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	private void delete() {
		Utils.disposeComposite(sc);
		dataSourceChanged(null);
	}

	private void refresh() {
	}

	private void dataSourceChanged(Object ds) {
		boolean wasNull = taggables == null;

		if (ds instanceof Taggable) {
			taggables = new ArrayList<Taggable>();
			taggables.add((Taggable) ds);
		} else if (ds instanceof Taggable[]) {
			taggables = new ArrayList<Taggable>();
			taggables.addAll(Arrays.asList((Taggable[]) ds));
		} else if (ds instanceof Object[]) {
			taggables = new ArrayList<Taggable>();
			Object[] objects = (Object[]) ds;
			for (Object o : objects) {
				if (o instanceof Taggable) {
					Taggable taggable = (Taggable) o;
					if ( !taggables.contains( taggable )){
						taggables.add(taggable);
					}
				} else if (o instanceof DiskManagerFileInfo) {
					DownloadManager temp = ((DiskManagerFileInfo) o).getDownloadManager();
					if (temp != null) {
						if (!taggables.contains(temp)) {
							taggables.add(temp);
						}
					}
				}
			}
			if (taggables.size() == 0) {
				taggables = null;
			}
		} else {
			taggables = null;
		}

		boolean isNull = taggables == null;
		if (isNull != wasNull) {
			TagManager tm = TagManagerFactory.getTagManager();
			TagType tagType;
			/*
			tagType = tm.getTagType(TagType.TT_DOWNLOAD_CATEGORY);
			if (isNull) {
				tagType.removeTagTypeListener(this);
			} else {
				tagType.addTagTypeListener(this, false);
			}
			*/
			tagType = tm.getTagType(TagType.TT_DOWNLOAD_MANUAL);
			if (isNull) {
				tagType.removeTagTypeListener(this);
			} else {
				tagType.addTagTypeListener(this, false);
			}
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_updateFields();
			}
		});
	}

	private void initialize() {
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
		
		cMainComposite.setLayout(new GridLayout(1, false));

		TagManager tm = TagManagerFactory.getTagManager();
		int[] tagTypesWanted = {
			TagType.TT_DOWNLOAD_MANUAL,
		//TagType.TT_DOWNLOAD_CATEGORY
		};
		
		tagButtonsUI = new TagButtonsUI();
		
		List<Tag> listAllTags = new ArrayList<Tag>();
		
		for (int tagType : tagTypesWanted) {
			
			TagType tt = tm.getTagType(tagType);
			List<Tag> tags = tt.getTags();
			listAllTags.addAll(tags);
		}
		tagButtonsUI.buildTagGroup(listAllTags, cMainComposite,
				new TagButtonTrigger() {
					public void tagButtonTriggered(Tag tag, boolean doTag) {
						for (Taggable taggable : taggables) {
							if (doTag) {
								tag.addTaggable(taggable);
							} else {
								tag.removeTaggable(taggable);
							}
							swt_updateFields();
						}
					}
				});
		
		Button buttonAdd = new Button(cMainComposite, SWT.PUSH);
		buttonAdd.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		Messages.setLanguageText(buttonAdd, "label.add.tag");
		buttonAdd.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				TagUIUtils.createManualTag(new TagReturner() {
					public void returnedTags(Tag[] tags) {
						if (taggables == null) {
							return;
						}
						for (Tag tag : tags) {
							for (Taggable taggable : taggables) {
								tag.addTaggable(taggable);
							}
						}
					}
				});
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = sc.getClientArea();
				Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
				sc.setMinSize(size);
			}
		});

		swt_updateFields();

		Rectangle r = sc.getClientArea();
		Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
		sc.setMinSize(size);
	}

	private String getFullTitle() {
		return MessageText.getString("label.tags");
	}

	private void swt_updateFields() {

		if (cMainComposite == null || cMainComposite.isDisposed()) {
			return;
		}
		
		if (tagButtonsUI.updateFields(taggables)) {
			parent.layout();
		}
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagTypeChanged(com.aelitis.azureus.core.tag.TagType)
	public void tagTypeChanged(TagType tag_type) {
		// TODO Auto-generated method stub
	}

	@Override
	public void tagEventOccurred(TagEvent event ) {
		int	type = event.getEventType();
		Tag	tag = event.getTag();
		if ( type == TagEvent.ET_TAG_ADDED ){
			tagAdded( tag );
		}else if ( type == TagEvent.ET_TAG_CHANGED ){
			tagChanged( tag );
		}else if ( type == TagEvent.ET_TAG_REMOVED ){
			tagRemoved( tag );
		}
	}
	
	public void tagAdded(Tag tag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				initialize();
			}
		});
	}

	public void tagChanged(final Tag changedTag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_updateFields();
			}
		});
	}

	public void tagRemoved(Tag tag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				initialize();
			}
		});
	}

}
