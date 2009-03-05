/**
 * Created on Mar 1, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.common.RememberedDecisionsManager;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

/**
 * @author TuxPaper
 * @created Mar 1, 2009
 *
 */
public abstract class InfoBarUtil
{
	private final SWTSkinObject forSO;

	private final boolean top;

	private SWTSkin skin;

	private SWTSkinObject soTop;

	private final String stateConfigID;

	private final String textPrefix;

	public InfoBarUtil(SWTSkinObject forSO, boolean top,
			final String stateConfigID, String textPrefix) {
		this.forSO = forSO;
		this.stateConfigID = stateConfigID;
		this.textPrefix = textPrefix;
		this.skin = forSO.getSkin();
		this.top = top;

		// Migrate existing state config to remembered decision manager so user
		// can get them back
		if (COConfigurationManager.hasParameter(
							stateConfigID, true)) {
			RememberedDecisionsManager.setRemembered(stateConfigID,
					COConfigurationManager.getBooleanParameter(stateConfigID) ? 1 : 0);
			COConfigurationManager.removeParameter(stateConfigID);
		}
		
		forSO.addListener(new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					boolean show = RememberedDecisionsManager.getRememberedDecision(stateConfigID) != 0;
					if (show && allowShow() && soTop == null) {
						createInfoBar();
					}
				}
				return null;
			}
		});
	}

	protected void createInfoBar() {
		Object ldForSO = forSO.getControl().getLayoutData();
		if (!(ldForSO instanceof FormData)) {
			return;
		}
		FormData fdForSO = (FormData) ldForSO;
		SWTSkinObject parent = forSO.getParent();
		soTop = skin.createSkinObject("toptip", "library.top.info", parent);

		FormData fdTop = (FormData) soTop.getControl().getLayoutData();
		if (fdTop == null) {
			fdTop = Utils.getFilledFormData();
		}
		fdTop.top = new FormAttachment(fdForSO.top.control, fdForSO.top.offset,
				fdForSO.top.alignment);
		fdTop.bottom = null;
		soTop.getControl().setLayoutData(fdTop);
		fdForSO.top = new FormAttachment(soTop.getControl(), 0, SWT.BOTTOM);
		forSO.getControl().setLayoutData(fdForSO);

		((SWTSkinObjectContainer) parent).getComposite().layout(true);

		SWTSkinObject soClose = skin.getSkinObject("close", parent);
		if (soClose != null) {
			SWTSkinButtonUtility btnClose = new SWTSkinButtonUtility(soClose);
			btnClose.addSelectionListener(new ButtonListenerAdapter() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					soTop.setVisible(false);
					RememberedDecisionsManager.setRemembered(stateConfigID, 0);
				}
			});
		}

		soTop.addListener(new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					RememberedDecisionsManager.setRemembered(stateConfigID, 1);
				}
				return null;
			}
		});

		SWTSkinObject soText1 = skin.getSkinObject("infobar-title-1", parent);
		if (soText1 instanceof SWTSkinObjectText) {
			SWTSkinObjectText soText = (SWTSkinObjectText) soText1;
			String id = textPrefix + ".text1";
			if (MessageText.keyExists(id)) {
				soText.setTextID(id);
			}
		}
		SWTSkinObject soText2 = skin.getSkinObject("infobar-title-2", parent);
		if (soText2 instanceof SWTSkinObjectText) {
			SWTSkinObjectText soText = (SWTSkinObjectText) soText2;
			String id = textPrefix + ".text2";
			if (MessageText.keyExists(id)) {
				soText.setTextID(id);
			}
		}

		soTop.setVisible(true);
	}

	public void hide() {
		soTop.setVisible(false);
		RememberedDecisionsManager.setRemembered(stateConfigID, 0);
	}

	public void show() {
		RememberedDecisionsManager.setRemembered(stateConfigID, 1);
		if (soTop == null) {
			createInfoBar();
		} else {
			soTop.setVisible(true);
		}
	}

	public abstract boolean allowShow();
}