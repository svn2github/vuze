/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author TuxPaper
 * @created Apr 7, 2007
 *
 */
public class ViewDownSpeedGraph
	implements UISWTViewCoreEventListener
{

	GlobalManager manager = null;

	GlobalManagerStats stats = null;

	Canvas downSpeedCanvas;

	SpeedGraphic downSpeedGraphic;

	TimerEventPeriodic	timerEvent;
	
	private boolean everRefreshed = false;

	private UISWTView swtView;
	
	public ViewDownSpeedGraph() {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				manager = core.getGlobalManager();
				stats = manager.getStats();
			}
		});
	}

	private void periodicUpdate() {
		if (manager == null || stats == null) {
			return;
		}

		int swarms_peer_speed = (int) stats.getTotalSwarmsPeerRate(true, false);

		downSpeedGraphic.addIntsValue(new int[] {
			stats.getDataReceiveRate() + stats.getProtocolReceiveRate(),
			stats.getProtocolReceiveRate(),
			COConfigurationManager.getIntParameter("Max Download Speed KBs") * 1024,
			swarms_peer_speed
		});
	}

	private void initialize(Composite composite) {
		GridData gridData;
		composite.setLayout( new GridLayout());
		downSpeedCanvas = new Canvas(composite, SWT.DOUBLE_BUFFERED);
		gridData = new GridData(GridData.FILL_BOTH);
		downSpeedCanvas.setLayoutData(gridData);
		downSpeedGraphic = SpeedGraphic.getInstance();
		downSpeedGraphic.initialize(downSpeedCanvas);
		//downSpeedGraphic.setAutoAlpha(true);
		/* this was for testing right?
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
		*/
	}

	private void delete() {
		Utils.disposeComposite(downSpeedCanvas);
		downSpeedGraphic.dispose();
	}

	private String getFullTitle() {
		return( MessageText.getString("TableColumn.header.downspeed"));
	}
	
	

	private Composite getComposite() {
		return downSpeedCanvas;
	}

	private void refresh() {
		if (!everRefreshed) {
			everRefreshed = true;
			timerEvent = SimpleTimer.addPeriodicEvent("TopBarSpeedGraphicView", 1000, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					if ( downSpeedCanvas.isDisposed()){
						timerEvent.cancel();
					}else{
						periodicUpdate();
					}
				}
			});
		}
		downSpeedGraphic.refresh(false);
	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = event.getView();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
    }

    return true;
  }
}
