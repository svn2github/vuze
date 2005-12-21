/*
 * Created on 13-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.components;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * 
 * TODO: have a callback when color changes
 */
public class Legend {
	/**
	 * Create a legend containing a modifyable color box and description
	 * 
	 * @param panel Where to add legend to
	 * @param blockColors array of colors for each legend entry.  This
	 *                     array WILL BE modified if the user changes the color
	 * @param keys array of keys for each legend entry
	 * @return The composite containing the legend
	 */
	public static Composite createLegendComposite(Composite panel,
			Color[] blockColors, String[] keys) {
		Object layout = panel.getLayout();
		Object layoutData = null;
		if (layout instanceof GridLayout)
			layoutData = new GridData(GridData.FILL_HORIZONTAL);

		return createLegendComposite(panel, blockColors, keys, layoutData);
	}


	/**
	 * Create a legend containing a modifyable color box and description
	 * 
	 * @param panel Where to add legend to
	 * @param blockColors array of colors for each legend entry.  This
	 *                     array WILL BE modified if the user changes the color
	 * @param keys array of keys for each legend entry
	 * @param layoutData How to layout the legend (ie. GridData, LayoutData, etc)
	 * @return The composite containing the legend
	 */
	public static Composite createLegendComposite(final Composite panel,
			final Color[] blockColors, final String[] keys, Object layoutData) {
		
		final ConfigurationManager config = ConfigurationManager.getInstance();

		if (blockColors.length != keys.length)
			return null;

		final ArrayList disposeList = new ArrayList();
		
		final Color[] defaultColors = new Color[blockColors.length];
		System.arraycopy(blockColors, 0, defaultColors, 0, blockColors.length);

		Composite legend = new Composite(panel, SWT.NONE);
		if (layoutData != null)
			legend.setLayoutData(layoutData);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.spacing = 0;
		legend.setLayout(layout);

		RowData data;
		for (int i = 0; i < blockColors.length; i++) {
			int r = config.getIntParameter(keys[i] + ".red", -1);
			if (r >= 0) {
				int g = config.getIntParameter(keys[i] + ".green");
				int b = config.getIntParameter(keys[i] + ".blue");
				
				Color color = new Color(panel.getDisplay(), r, g, b);
				disposeList.add(color);
				blockColors[i] = color;
			}

			Composite colorSet = new Composite(legend, SWT.NONE);

			colorSet.setLayout(new RowLayout(SWT.HORIZONTAL));

			final Label lblColor = new Label(colorSet, SWT.BORDER);
			lblColor.setData("Index", new Integer(i));
			lblColor.setBackground(blockColors[i]);
			data = new RowData();
			data.width = 20;
			data.height = 10;
			lblColor.setLayoutData(data);
			lblColor.addMouseListener(new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					Integer iIndex = (Integer)lblColor.getData("Index");
					if (iIndex == null)
						return;
					int index = iIndex.intValue();

					if (e.button == 1) {
						ColorDialog cd = new ColorDialog(panel.getShell());
						cd.setRGB(blockColors[index].getRGB());
						
						RGB rgb = cd.open();
						if (rgb != null) {
							Color color = new Color(panel.getDisplay(), rgb);
							disposeList.add(color);
							blockColors[index] = color;
							lblColor.setBackground(blockColors[index]);
							config.setRGBParameter(keys[index], rgb.red, rgb.green, rgb.blue);
						}
					} else {
						blockColors[index] = defaultColors[index];
						lblColor.setBackground(defaultColors[index]);
						config.removeParameter(keys[index] + ".red");
						config.removeParameter(keys[index] + ".green");
						config.removeParameter(keys[index] + ".blue");
					}
				}
			});

			BufferedLabel lblDesc = new BufferedLabel(colorSet, SWT.NULL);
			Messages.setLanguageText(lblDesc, keys[i]);
		}
		
		legend.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				// We don't want to give them disposed colors
				// Restore defaults in case blockColors is a static or is used
				// afterwards, or if the view wants to dispose of the old colors.
				for (int i = 0; i < blockColors.length; i++) {
					blockColors[i] = defaultColors[i];
				}

				Utils.disposeSWTObjects(disposeList);
			}
		});

		return legend;
	}
}
