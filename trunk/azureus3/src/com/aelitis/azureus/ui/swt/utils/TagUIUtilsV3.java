/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;

/**
 * @author TuxPaper
 * @created Feb 26, 2015
 *
 */
public class TagUIUtilsV3
{

	public static Tag showCreateTagDialog() {
		SkinnedDialog dialog = new SkinnedDialog("skin3_dlg_addtag", "dlg.addtag");
		final VuzeMessageBox mb = new VuzeMessageBox(
				MessageText.getString("TagAddWindow.title"), null, new String[] {
					MessageText.getString("Button.add"),
					MessageText.getString("Button.cancel")
		}, 0);
		mb.setSkinnedDialagTemplate("skin3_dlg_generic_notop");
		mb.setButtonVals(new Integer[] {
			SWT.OK,
			SWT.CANCEL,
		});
		mb.setSubTitle(MessageText.getString("TagAddWindow.subtitle"));
		mb.addResourceBundle(TagUIUtilsV3.class, SkinPropertiesImpl.PATH_SKIN_DEFS,
				"skin3_dlg_addtag");

		final SWTSkinObjectTextbox[] tb = {
			null
		};
		final SWTSkinObjectCheckbox[] cb = {
			null
		};
		mb.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.addtag", "dlg.addtag", soExtra);

				tb[0] = (SWTSkinObjectTextbox) skin.getSkinObject("tag-name", soExtra);
				cb[0] = (SWTSkinObjectCheckbox) skin.getSkinObject("tag-share",
						soExtra);
				
				Control control = tb[0].getControl();
				control.getParent().setBackgroundMode(SWT.INHERIT_DEFAULT);

				cb[0].setChecked(COConfigurationManager.getBooleanParameter(
						"tag.sharing.default.checked"));
			}
		});

		final Tag[] tag = {
			null
		};
		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {

				if (result != SWT.OK || tb[0] == null) {
					return;
				}

				String tag_name = tb[0].getText().trim();
				TagType tt = TagManagerFactory.getTagManager().getTagType(
						TagType.TT_DOWNLOAD_MANUAL);

				Tag existing = tt.getTag(tag_name, true);

				if (existing == null) {

					try {

						tag[0] = tt.createTag(tag_name, true);

						tag[0].setPublic(cb[0].isChecked());

					} catch (TagException e) {

						Debug.out(e);
					}
				}
			}
		});
		mb.waitUntilClosed();
		
		return tag[0];
	}

}
