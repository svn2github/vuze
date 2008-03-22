/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

/**
 * @author TuxPaper
 * @created Apr 7, 2007
 *
 */
public class ViewDownSpeedGraph
	extends AbstractIView
{

	GlobalManager manager;

	GlobalManagerStats stats;

	OverallStats totalStats;

	Canvas downSpeedCanvas;

	SpeedGraphic downSpeedGraphic;

	public ViewDownSpeedGraph() {
		this.manager = AzureusCoreFactory.getSingleton().getGlobalManager();
		this.stats = manager.getStats();
		this.totalStats = StatsFactory.getStats();
		
		SimpleTimer.addPeriodicEvent("TopBarSpeedGraphicView", 1000, new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				periodicUpdate();
			}
		});
	}

	public void periodicUpdate() {

		int swarms_peer_speed = (int) stats.getTotalSwarmsPeerRate(true, false);

		downSpeedGraphic.addIntsValue(new int[] {
			stats.getDataReceiveRate() + stats.getProtocolReceiveRate(),
			stats.getProtocolReceiveRate(),
			COConfigurationManager.getIntParameter("Max Download Speed KBs") * 1024,
			swarms_peer_speed
		});
	}

	public void initialize(Composite composite) {
		GridData gridData;

		downSpeedCanvas = new Canvas(composite, SWT.NONE);
		gridData = new GridData(GridData.FILL_BOTH);
		downSpeedCanvas.setLayoutData(gridData);
		downSpeedGraphic = SpeedGraphic.getInstance();
		downSpeedGraphic.initialize(downSpeedCanvas);
		//downSpeedGraphic.setAutoAlpha(true);
		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		Color cBG1 = skinProperties.getColor("color.topbar.speed.bg1");
		Color cBG2 = skinProperties.getColor("color.topbar.speed.bg2");
		Color cBG3 = skinProperties.getColor("color.topbar.speed.bg3");
		downSpeedGraphic.setColors(cBG1, cBG2, cBG3);
		downSpeedGraphic.setLineColors(skinProperties.getColor("color.topbar.speed.average"),
				skinProperties.getColor("color.topbar.speed.value0"),
				skinProperties.getColor("color.topbar.speed.overhead"),
				skinProperties.getColor("color.topbar.speed.value1"),
				skinProperties.getColor("color.topbar.speed.value2plus"),
				skinProperties.getColor("color.topbar.speed.trimmed"));
	}

	public void delete() {
		Utils.disposeComposite(downSpeedCanvas);
		downSpeedGraphic.dispose();
	}

	public String getFullTitle() {
		return "DL Speed";
	}
	
	

	public Composite getComposite() {
		return downSpeedCanvas;
	}

	public void refresh() {
		downSpeedGraphic.refresh();
	}

	public String getData() {
		return "SpeedView.title.full";
	}
}
