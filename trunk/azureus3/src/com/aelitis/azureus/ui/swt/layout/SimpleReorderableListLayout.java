package com.aelitis.azureus.ui.swt.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class SimpleReorderableListLayout extends Layout {
	
	public int margin;

	public boolean wrap;
	public boolean center;
	
	public int borderW = 3;
	public int borderH = 3;
	
	private int itemsPerRow;
	private int maxHeight = 0;
	private int maxWidth = 0;
	
	private int extraSpacing;
	
	
	private boolean cached = false;
	private Point cachedSize = null;
	
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		if(flushCache || ! cached || cachedSize == null) {
			
			Control[] controls = composite.getChildren();
			
			
			for(int i = 0 ; i < controls.length ; i++) {
				Object layoutData = controls[i].getLayoutData();
				if(layoutData != null && layoutData instanceof SimpleReorderableListLayoutData) {
					SimpleReorderableListLayoutData sData = (SimpleReorderableListLayoutData) layoutData;
					if(sData.height > maxHeight) maxHeight = sData.height;
					if(sData.width > maxWidth) maxWidth = sData.width;
				}
			}
			
			
			if((wHint != SWT.DEFAULT) && wrap) {
				itemsPerRow = 1;
				int width = 2 * borderW + maxWidth;
				while(width < wHint) {
					width += margin + maxWidth;
					if(width < wHint) {
						itemsPerRow++;
					}
				}
				if(center) {
					width = 2 * borderW + (margin + maxWidth) * itemsPerRow - margin;
					extraSpacing = (wHint - width) / (itemsPerRow+1);
				} else {
					extraSpacing = 0;
				}
			} else {
				itemsPerRow = controls.length;
			}
			
			//Avoid dividing by 0 when there are no items
			int nbRows = itemsPerRow > 0 ? (controls.length+itemsPerRow-1) / itemsPerRow : 1;
			
			cached = true;
			cachedSize = new Point(2 * borderW + (maxWidth+margin) * itemsPerRow - margin, 2 * borderH + (margin + maxHeight) * nbRows - margin);
			
		} 
		
		return cachedSize;
		
	}

	protected void layout(Composite composite, boolean flushCache) {
		if(!cached || cachedSize == null) {
			computeSize(composite, 0, 0, true);
		}
		
		Control[] controls = composite.getChildren();
		List sortedControls = new ArrayList(controls.length);
		for(int i = 0 ; i < controls.length ; i++) {
			sortedControls.add(controls[i]);
		}
		
		Collections.sort(sortedControls,new Comparator() {
			public int compare(Object o1, Object o2) {
				Control c1 = (Control) o1;
				Control c2 = (Control) o2;
				Object layoutData1 = c1.getLayoutData();
				Object layoutData2 = c2.getLayoutData();
				if(layoutData1 == null || ! (layoutData1 instanceof SimpleReorderableListLayoutData) ) return 0;
				if(layoutData2 == null || ! (layoutData2 instanceof SimpleReorderableListLayoutData) ) return 0;
				SimpleReorderableListLayoutData data1 = (SimpleReorderableListLayoutData) layoutData1;
				SimpleReorderableListLayoutData data2 = (SimpleReorderableListLayoutData) layoutData2;
				return data1.position - data2.position;
			}
		});
		
		for(int i = 0 ; i < sortedControls.size() ; i++) {
			int xn = i % itemsPerRow;
			int yn = i / itemsPerRow;
			Control control = (Control) sortedControls.get(i);
			int x = borderW + (margin + maxWidth + extraSpacing) * xn + extraSpacing;
			int y = borderH + (margin + maxHeight) * yn;
			control.setLocation(x,y);
			control.setBounds(x,y,maxWidth,maxHeight);
		}
	
	}

}
