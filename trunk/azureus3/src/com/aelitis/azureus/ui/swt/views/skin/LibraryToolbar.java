package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

public class LibraryToolbar
	extends SkinView
{

	private Composite parent;

	private ToolBar toolbar;

	private Label viewTitle;

	private ToolItem smallTabel;

	private ToolItem largeTable;

	private Composite content;

	static {
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/button_table_view_normal.png",
				"button_small_table");
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/button_table_view_large.png",
				"button_large_table");
	}

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		parent = (Composite) skinObject.getControl();

		init();
		return null;
	}

	private void init() {
		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException("Parent cannot be null or disposed");
		}

		Layout parentLayout = parent.getLayout();
		if (null == parentLayout) {
			parentLayout = new FormLayout();
			parent.setLayout(parentLayout);

		} else if (false == (parentLayout instanceof FormLayout)) {
			throw new IllegalArgumentException(
					"Oops! We can not handle any layout other than FormLayout at the moment!!!");
		}

		content = new Composite(parent, SWT.NONE);

		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		content.setLayoutData(fd);

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 3;
		layout.marginWidth = 3;
		content.setLayout(layout);

		createControls();

	}

	private void createControls() {
		toolbar = new ToolBar(content, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		createToolItems();

		viewTitle = new Label(content, SWT.NONE);
		viewTitle.setText("This is where the title/description can be displayed... or can be left out completely!!!!!");
		viewTitle.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
	}

	/**
	 * Creates all the tool items for this toolbar.
	 * Subclasses may override this method to implement a different set of tool items
	 */
	public void createToolItems() {
		smallTabel = new ToolItem(toolbar, SWT.CHECK);
		smallTabel.setImage(ImageRepository.getImage("button_small_table"));
		smallTabel.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				System.out.println("Small is selected: " + smallTabel.getSelection());//KN: sysout
				if (true == smallTabel.getSelection()) {
					largeTable.setSelection(false);
					setTableMode(false);
				} else {
					largeTable.setSelection(true);
					setTableMode(true);
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		largeTable = new ToolItem(toolbar, SWT.CHECK);
		largeTable.setImage(ImageRepository.getImage("button_large_table"));
		largeTable.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				System.out.println("Large is selected: " + largeTable.getSelection());//KN: sysout
				if (true == largeTable.getSelection()) {
					smallTabel.setSelection(false);
					setTableMode(true);
				} else {
					smallTabel.setSelection(true);
					setTableMode(false);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void setTableMode(boolean isLarge) {
		//		if (true == isLarge) {
		//			smallTabel.setSelection(false);
		//			largeTable.setSelection(true);
		//		} else {
		//			largeTable.setSelection(false);
		//			largeTable.setSelection(false);
		//		}

		System.out.println("Table mode is large: " + isLarge);//KN: sysout
	}
}
