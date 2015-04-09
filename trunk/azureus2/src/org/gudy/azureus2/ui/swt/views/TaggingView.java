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
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.MenuBuildUtils.MenuBuilder;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.UIFunctions.TagReturner;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

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

	private List<Button> buttons;

	private Composite parent;

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
					taggables.add(taggable);
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

		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
				Tag tag = (Tag) button.getData("Tag");
				if (button.getGrayed()) {
					button.setGrayed(false);
					button.setSelection(!button.getSelection());
					button.getParent().redraw();
				}
				boolean doTag = button.getSelection();
				for (Taggable taggable : taggables) {
					if (doTag) {
						tag.addTaggable(taggable);
					} else {
						tag.removeTaggable(taggable);
					}
				}
				button.getParent().redraw();
				button.getParent().update();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		Listener menuDetectListener = new Listener() {
			public void handleEvent(Event event) {

				final Button button = (Button) event.widget;
				Menu menu = new Menu(button);
				button.setMenu(menu);
				
				MenuBuildUtils.addMaintenanceListenerForMenu(menu, new MenuBuilder() {
					public void buildMenu(final Menu menu, MenuEvent menuEvent) {
						Tag tag = (Tag) button.getData("Tag");
						TagUIUtils.createSideBarMenuItems(menu, tag);
					}
				});
			}
		};
		
		PaintListener paintListener = new PaintListener() {
			
			public void paintControl(PaintEvent e) {
				Button button;
				Composite c = null;
				if (e.widget instanceof Composite) {
					c = (Composite) e.widget;
					button = (Button) c.getChildren()[0];
				} else {
					button = (Button) e.widget;
				}
				Tag tag = (Tag) button.getData("Tag");
				if (tag == null) {
					return;
				}

				
				
				//ImageLoader.getInstance().getImage(? "check_yes" : "check_no");

				if (c != null) {
					boolean checked = button.getSelection();
  				Point size = c.getSize();
  				Point sizeButton = button.getSize();
  				e.gc.setAntialias(SWT.ON);
  				e.gc.setForeground(ColorCache.getColor(e.display, tag.getColor()));
  				int lineWidth = button.getSelection() ? 2 : 1;
  				e.gc.setLineWidth(lineWidth);

  				int curve = 20;
  				int width = sizeButton.x + lineWidth + 1;
  				width += Constants.isOSX ? 5 : curve / 2;
  				if (checked) {
    				e.gc.setAlpha(0x20);
    				e.gc.setBackground(ColorCache.getColor(e.display, tag.getColor()));
    				e.gc.fillRoundRectangle(-curve, lineWidth - 1, width + curve, size.y - lineWidth, curve, curve);
    				e.gc.setAlpha(0xff);
  				}
  				if (!checked) {
    				e.gc.setAlpha(0x80);
  				}
  				e.gc.drawRoundRectangle(-curve, lineWidth - 1, width + curve, size.y - lineWidth, curve, curve);
  				e.gc.drawLine(lineWidth - 1, lineWidth, lineWidth - 1, size.y - lineWidth);
				} else {
  				if (!Constants.isOSX && button.getSelection()) {
    				Point size = button.getSize();
    				e.gc.setBackground(ColorCache.getColor(e.display, tag.getColor()));
    				e.gc.setAlpha(20);
    				e.gc.fillRectangle(0, 0, size.x, size.y);
  				}
				}
			}
		};

		buttons = new ArrayList<Button>();
		for (int tagType : tagTypesWanted) {
			Composite c = new Composite(cMainComposite, SWT.DOUBLE_BUFFERED);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			RowLayout rowLayout = new RowLayout();
			rowLayout.pack = false;
			rowLayout.spacing = 5;
			c.setLayout(rowLayout);
			
			TagType tt = tm.getTagType(tagType);
			List<Tag> tags = tt.getTags();
			tags = TagUIUtils.sortTags(tags);
			for (Tag tag : tags) {
				Composite p = new Composite(c, SWT.DOUBLE_BUFFERED);
				GridLayout layout = new GridLayout(1, false);
				layout.marginHeight = 3;
				if (Constants.isWindows) {
					layout.marginWidth = 6;
					layout.marginLeft = 2;
					layout.marginTop = 1;
				} else {
					layout.marginWidth = 0;
					layout.marginLeft = 3;
					layout.marginRight = 10;
				}
				p.setLayout(layout);
				p.addPaintListener(paintListener);
				
				Button button = new Button(p, SWT.CHECK);
				buttons.add(button);
				if ( tag.isTagAuto()){
					button.setEnabled( false );
				}else{
					button.addSelectionListener(selectionListener);
				}
				button.setData("Tag", tag);

				button.addListener(SWT.MenuDetect, menuDetectListener);
				button.addPaintListener(paintListener);
			}
		}
		
		Button buttonAdd = new Button(cMainComposite, SWT.PUSH);
		buttonAdd.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		Messages.setLanguageText(buttonAdd, "label.add.tag");
		buttonAdd.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				TagUIUtils.createManualTag(new TagReturner() {
					public void returnedTags(Tag[] tags) {
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
		

		List<Control> layoutChanges = new ArrayList<Control>();
		for (Button button : buttons) {
			boolean hasTag = false;
			boolean hasNoTag = false;

			Tag tag = (Tag) button.getData("Tag");
			if (tag == null) {
				continue;
			}
			String name = tag.getTagName(true);
			if (!button.getText().equals(name)) {
				button.setText(name);
				layoutChanges.add(button);
			}

			if (taggables == null) {
				button.setSelection(false);
				button.setEnabled(false);
				button.getParent().redraw();
				continue;
			}
			if ( !tag.isTagAuto()){
				button.setEnabled(true);
			}
			
			for (Taggable taggable : taggables) {
				boolean curHasTag = tag.hasTaggable(taggable);
				if (!hasTag && curHasTag) {
					hasTag = true;
					if (hasNoTag) {
						break;
					}
				} else if (!hasNoTag && !curHasTag) {
					hasNoTag = true;
					if (hasTag) {
						break;
					}
				}
			}
			if (hasTag && hasNoTag) {
				button.setGrayed(true);
				button.setSelection(true);
			} else {
				button.setGrayed(false);
				button.setSelection(hasTag);
			}
			button.getParent().redraw();
		}

		if (layoutChanges.size() > 0) {
			cMainComposite.layout(layoutChanges.toArray(new Control[0]));
			parent.layout();
		}
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagTypeChanged(com.aelitis.azureus.core.tag.TagType)
	public void tagTypeChanged(TagType tag_type) {
		// TODO Auto-generated method stub
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagAdded(com.aelitis.azureus.core.tag.Tag)
	public void tagAdded(Tag tag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				initialize();
			}
		});
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagChanged(com.aelitis.azureus.core.tag.Tag)
	public void tagChanged(final Tag changedTag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_updateFields();
			}
		});
	}

	// @see com.aelitis.azureus.core.tag.TagTypeListener#tagRemoved(com.aelitis.azureus.core.tag.Tag)
	public void tagRemoved(Tag tag) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				initialize();
			}
		});
	}

}
