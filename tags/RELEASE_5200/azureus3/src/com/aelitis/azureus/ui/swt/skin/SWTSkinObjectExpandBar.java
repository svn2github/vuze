package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Container that hold ExpandItems
 * 
 */
public class SWTSkinObjectExpandBar
	extends SWTSkinObjectContainer
{

	private ExpandBar expandBar;

	private List<SWTSkinObjectExpandItem> expandItems = new ArrayList<SWTSkinObjectExpandItem>();

	private List<SWTSkinObjectExpandItem> fillHeightItems = new ArrayList<SWTSkinObjectExpandItem>();

	public SWTSkinObjectExpandBar(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "expandbar", parent);
		createExpandBar();
	}

	private void createExpandBar() {
		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}

		expandBar = new ExpandBar(createOn, style); // | SWT.V_SCROLL);
		// ensure no layout for expandbar (children don't setlayoutdata because they are expanditems)
		expandBar.setLayout(null);
		expandBar.setSpacing(1);

		expandBar.addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {
				handleResize(null);
			}
		});

		triggerListeners(SWTSkinObjectListener.EVENT_CREATED);
		setControl(expandBar);
	}
	
	
	protected void handleResize(ExpandItem itemResizing) {
		SWTSkinObjectExpandItem foundItem = null;
		if (itemResizing != null) {
  		SWTSkinObjectExpandItem[] children = getChildren();
  		for (SWTSkinObjectExpandItem item : children) {
  			if (item.getExpandItem() == itemResizing) {
  				foundItem = item;
  				item.resizeComposite();
  				break;
  			}
  		}
		}
		
		for (SWTSkinObjectExpandItem autoItem : fillHeightItems) {
			if (autoItem != foundItem) {
				autoItem.resizeComposite();
			}
		}
		
	}

	public void
	relayout()
	{
		super.relayout();
		handleResize(null);
	}
	
	protected void addExpandItem(SWTSkinObjectExpandItem item) {
		expandItems.add(item);

		if (item.fillsHeight()) {
			fillHeightItems.add(item);
		}
	}

	protected void removeExpandItem(SWTSkinObjectExpandItem item) {
		expandItems.remove(item);
		fillHeightItems.remove( item );
	}

	public SWTSkinObjectExpandItem[] getChildren() {
		return expandItems.toArray(new SWTSkinObjectExpandItem[0]);
	}

	public ExpandBar getExpandbar() {
		return expandBar;
	}
}
