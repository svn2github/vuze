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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
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

/**
 * @author TuxPaper
 * @created Apr 7, 2007
 *
 */
public class ViewUpSpeedGraph
	extends AbstractIView
{

	GlobalManager manager;

	GlobalManagerStats stats;

	OverallStats totalStats;

	Canvas upSpeedCanvas;

	SpeedGraphic upSpeedGraphic;

	public ViewUpSpeedGraph() {
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

		upSpeedGraphic.addIntsValue(new int[] {
			stats.getDataSendRate() + stats.getProtocolSendRate(),
			COConfigurationManager.getIntParameter(TransferSpeedValidator.getActiveUploadParameter(manager)) * 1024,
			swarms_peer_speed
		});
	}

	public void initialize(Composite composite) {
		GridData gridData;

		upSpeedCanvas = new Canvas(composite, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		upSpeedCanvas.setLayoutData(gridData);
		upSpeedGraphic = SpeedGraphic.getInstance();
		upSpeedGraphic.initialize(upSpeedCanvas);
		//upSpeedGraphic.setAutoAlpha(true);
	}

	public void delete() {
		Utils.disposeComposite(upSpeedCanvas);
		upSpeedGraphic.dispose();
	}

	public String getFullTitle() {
		return "UL Speed";
	}

	public Composite getComposite() {
		return upSpeedCanvas;
	}

	public void refresh() {
		upSpeedGraphic.refresh();
	}

	public String getData() {
		return "SpeedView.title.full";
	}
}
