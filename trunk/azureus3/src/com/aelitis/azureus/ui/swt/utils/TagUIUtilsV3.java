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
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.StandardButtonsArea;

/**
 * @author TuxPaper
 * @created Feb 26, 2015
 *
 */
public class TagUIUtilsV3
{

	public static void showCreateTagDialog(final UIFunctions.TagReturner tagReturner) {
		final SkinnedDialog dialog = new SkinnedDialog("skin3_dlg_addtag", "shell",
				SWT.DIALOG_TRIM);
		SWTSkin skin = dialog.getSkin();

		final SWTSkinObjectTextbox tb = (SWTSkinObjectTextbox) skin.getSkinObject(
				"tag-name");
		final SWTSkinObjectCheckbox cb = (SWTSkinObjectCheckbox) skin.getSkinObject(
				"tag-share");

		if (tb == null || cb == null) {
			return;
		}

		cb.setChecked(COConfigurationManager.getBooleanParameter(
				"tag.sharing.default.checked"));

		SWTSkinObject soButtonArea = skin.getSkinObject("bottom-area");
		if (soButtonArea instanceof SWTSkinObjectContainer) {
			StandardButtonsArea buttonsArea = new StandardButtonsArea() {
				// @see com.aelitis.azureus.ui.swt.views.skin.StandardButtonsArea#clicked(int)
				protected void clicked(int buttonValue) {
					if (buttonValue == SWT.OK) {

						String tag_name = tb.getText().trim();
						TagType tt = TagManagerFactory.getTagManager().getTagType(
								TagType.TT_DOWNLOAD_MANUAL);

						Tag tag = tt.getTag(tag_name, true);

						if (tag == null) {

							try {

								tag = tt.createTag(tag_name, true);

								tag.setPublic(cb.isChecked());


							} catch (TagException e) {

								Debug.out(e);
							}
						}
						
						// return tag even if it already existed.  
						// Case: assigning tag to DL, user enters same tag name because 
						// they forgot they already had one
						if (tagReturner != null && tag != null) {
							tagReturner.returnedTags(new Tag[] {
								tag
							});
						}

					}
					
					dialog.close();
				}
			};
			buttonsArea.setButtonIDs(new String[] {
				MessageText.getString("Button.add"),
				MessageText.getString("Button.cancel")
			});
			buttonsArea.setButtonVals(new Integer[] {
				SWT.OK,
				SWT.CANCEL
			});
			buttonsArea.swt_createButtons(
					((SWTSkinObjectContainer) soButtonArea).getComposite());
		}

		dialog.open();
	}

}
