package com.aelitis.azureus.ui.swt.layout;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class SimpleReorderableListLayout extends Layout {
	
	public int margin;

	public int borderW = 3;
	public int borderH = 3;
	
	private boolean cached = false;
	private Point cachedSize = null;
	
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if(true || flushCache || ! cached || cachedSize == null) {
			int totalWidth = 0;
			int maxHeight = 0;
			Control[] controls = composite.getChildren();
			int currentMargin = 0;
			for(int i = 0 ; i < controls.length ; i++) {
				Object layoutData = controls[i].getLayoutData();
				if(layoutData != null && layoutData instanceof SimpleReorderableListLayoutData) {
					SimpleReorderableListLayoutData sData = (SimpleReorderableListLayoutData) layoutData;
					if(sData.height > maxHeight) maxHeight = sData.height;
					totalWidth += currentMargin + sData.width;
					currentMargin = margin;
				}
			}
			
			cached = true;
			cachedSize = new Point(totalWidth + 2 * borderW, maxHeight + 2 * borderH);
			
		} 
		
		return cachedSize;
		
	}

	protected void layout(Composite composite, boolean flushCache) {
		if(!cached || cachedSize == null) {
			computeSize(composite, 0, 0, true);
		}
		
		Control[] controls = composite.getChildren();
		
		int[] positions = new int[controls.length];
			
		//Compute the positions of each based on the index of the controls
		for(int i = 0 ; i < controls.length ; i++) {
			Object layoutData = controls[i].getLayoutData();
			if(layoutData != null && layoutData instanceof SimpleReorderableListLayoutData) {
				SimpleReorderableListLayoutData sData = (SimpleReorderableListLayoutData) layoutData;
				int index = sData.position;
				for(int j = index+1 ; j < positions.length ; j++) {
					positions[j] += margin + sData.width;
				}
			}
		}
		
		int[] extraShift = new int[controls.length];
		//Set the positions
		for(int i = 0 ; i < controls.length ; i++) {
			Object layoutData = controls[i].getLayoutData();
			if(layoutData != null && layoutData instanceof SimpleReorderableListLayoutData) {
				SimpleReorderableListLayoutData sData = (SimpleReorderableListLayoutData) layoutData;
				int index = sData.position;
				if(index >= 0 && index < positions.length) {
					controls[i].setLocation(extraShift[index] + borderW + positions[index], borderH);
					controls[i].setBounds(extraShift[index] + borderW + positions[index], borderH,sData.width,sData.height);
				}
				if(index >= 0 && index < extraShift.length) {
					extraShift[index] += margin + sData.width;
				}
			}
		}
	}

}
