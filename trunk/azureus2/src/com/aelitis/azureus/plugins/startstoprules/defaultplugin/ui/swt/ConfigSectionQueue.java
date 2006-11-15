/*
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

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

/** General Queueing options
 * @author TuxPaper
 * @created Jan 12, 2004
 */
public class ConfigSectionQueue implements UISWTConfigSection
{
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	/**
	 * Create the "Queue" Tab in the Configuration view
	 */
	public Composite configSectionCreate(Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		// main tab set up

		Composite cSection = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		cSection.setLayout(layout);

		// row

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.maxdownloads");
		gridData = new GridData();
		gridData.widthHint = 40;
		final IntParameter maxDLs = new IntParameter(cSection, "max downloads");
		maxDLs.setLayoutData(gridData);

		// row

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.maxactivetorrents");
		gridData = new GridData();
		gridData.widthHint = 40;
		final IntParameter maxActiv = new IntParameter(cSection,
				"max active torrents");
		maxActiv.setLayoutData(gridData);

		final Composite cMaxActiveOptionsArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cMaxActiveOptionsArea.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalIndent = 15;
		gridData.horizontalSpan = 2;
		cMaxActiveOptionsArea.setLayoutData(gridData);

		label = new Label(cMaxActiveOptionsArea, SWT.NULL);
		Image img = ImageRepository.getImage("subitem");
		img.setBackground(label.getBackground());
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gridData);
		label.setImage(img);

		gridData = new GridData();
		BooleanParameter maxActiveWhenSeedingEnabled = new BooleanParameter(
				cMaxActiveOptionsArea,
				"StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled",
				"ConfigView.label.queue.maxactivetorrentswhenseeding");
		maxActiveWhenSeedingEnabled.setLayoutData(gridData);

		gridData = new GridData();
		gridData.widthHint = 40;

		final IntParameter maxActivWhenSeeding = new IntParameter(
				cMaxActiveOptionsArea, "StartStopManager_iMaxActiveTorrentsWhenSeeding");
		maxActivWhenSeeding.setLayoutData(gridData);

		// row

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.mindownloads");
		gridData = new GridData();
		gridData.widthHint = 40;
		final IntParameter minDLs = new IntParameter(cSection, "min downloads");
		minDLs.setLayoutData(gridData);
		minDLs.setMaximumValue(maxDLs.getValue() / 2);
		
		// change controllers for above items

		maxActiveWhenSeedingEnabled.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				maxActivWhenSeeding));

		maxDLs.addChangeListener(new ParameterChangeAdapter() {
			public void parameterChanged(Parameter p, boolean caused_internally) {
				int iMaxDLs = maxDLs.getValue();
				minDLs.setMaximumValue(iMaxDLs / 2);

				int iMinDLs = minDLs.getValue();
				int iMaxActive = maxActiv.getValue();
				
				if ((iMaxDLs == 0 || iMaxDLs > iMaxActive) && iMaxActive != 0) {
					maxActiv.setValue(iMaxDLs);
				}
			}
		});

		maxActiv.addChangeListener(new ParameterChangeAdapter() {
			public void parameterChanged(Parameter p, boolean caused_internally) {
				int iMaxDLs = maxDLs.getValue();
				int iMaxActive = maxActiv.getValue();

				if ((iMaxDLs == 0 || iMaxDLs > iMaxActive) && iMaxActive != 0) {
					maxDLs.setValue(iMaxActive);
				}
			}
		});

		
		// row

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.minSpeedForActiveDL");
		final String activeDLLabels[] = new String[57];
		final int activeDLValues[] = new int[57];
		int pos = 0;
		for (int i = 0; i < 256; i += 64) {
			activeDLValues[pos] = i;
			activeDLLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeDLValues[pos], true);
			pos++;
		}
		for (int i = 256; i < 1024; i += 256) {
			activeDLValues[pos] = i;
			activeDLLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeDLValues[pos], true);
			pos++;
		}
		for (int i = 1; pos < activeDLLabels.length; i++) {
			activeDLValues[pos] = i * 1024;
			activeDLLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeDLValues[pos], true);
			pos++;
		}
		new IntListParameter(cSection, "StartStopManager_iMinSpeedForActiveDL",
				activeDLLabels, activeDLValues);

		// row

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.label.minSpeedForActiveSeeding");
		final String activeSeedingLabels[] = new String[27];
		final int activeSeedingValues[] = new int[27];
		pos = 0;

		for (int i = 0; i < 256; i += 64) {
			activeSeedingValues[pos] = i;
			activeSeedingLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeSeedingValues[pos], true);
			pos++;
		}
		for (int i = 256; i < 1024; i += 256) {
			activeSeedingValues[pos] = i;
			activeSeedingLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeSeedingValues[pos], true);
			pos++;
		}
		for (int i = 1; pos < activeSeedingLabels.length; i++) {
			activeSeedingValues[pos] = i * 1024;
			activeSeedingLabels[pos] = DisplayFormatters.formatByteCountToKiBEtcPerSec(
					activeSeedingValues[pos], true);
			pos++;
		}
		new IntListParameter(cSection,
				"StartStopManager_iMinSpeedForActiveSeeding", activeSeedingLabels,
				activeSeedingValues);

		// row

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "StartStopManager_bNewSeedsMoveTop",
				"ConfigView.label.queue.newseedsmovetop").setLayoutData(gridData);

		// row

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "Alert on close",
				"ConfigView.label.showpopuponclose").setLayoutData(gridData);

		//row 

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "StartStopManager_bDebugLog",
				"ConfigView.label.queue.debuglog").setLayoutData(gridData);

		return cSection;
	}

	public String configSectionGetName() {
		return "queue";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
}
