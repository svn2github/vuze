/*
 * Created on Mar 15, 2010 02:29 PM
 * Copyright (C) 2010 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.aelitis.azureus.ui.swt.plugininstall;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;

import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Gudy
 * @created Mar 15, 2010
 *
 */
public class SimplePluginInstallWindow
	implements SimplePluginInstallerListener
{
	private final static boolean FAKE_DELAY = Constants.IS_CVS_VERSION;

	private VuzeMessageBox box;

	private ProgressBar progressBar;

	private SWTSkinObjectText soProgressText;

	private String progressText;

	private SWTSkinObjectText soInstallPct;
	
	private SimplePluginInstaller 	installer;
	private String					resource_prefix;

	public 
	SimplePluginInstallWindow(
		SimplePluginInstaller 	_installer, 
		String 					_resource_prefix ) 
	{
		
		installer 			= _installer;
		resource_prefix		= _resource_prefix;
		
		installer.setListener(this);
	}

	public void open() {
		
		box = new VuzeMessageBox("","", null, 0);
		box.setSubTitle(MessageText.getString( resource_prefix + ".subtitle" ));
		box.addResourceBundle(SimplePluginInstallWindow.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource( resource_prefix + ".image" );

		this.progressText = MessageText.getString( resource_prefix + ".description" );
		
		box.setListener(new VuzeMessageBoxListener() {

			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.register.install", "dlg.register.install",
						soExtra);

				SWTSkinObjectContainer soProgressBar = (SWTSkinObjectContainer) skin.getSkinObject("progress-bar");
				if (soProgressBar != null) {
					progressBar = new ProgressBar(soProgressBar.getComposite(),
							SWT.HORIZONTAL);
					progressBar.setMinimum(0);
					progressBar.setMaximum(100);
					progressBar.setLayoutData(Utils.getFilledFormData());
				}
				
				soInstallPct = (SWTSkinObjectText) skin.getSkinObject("install-pct");

				soProgressText = (SWTSkinObjectText) skin.getSkinObject("progress-text");
				if (soProgressText != null && progressText != null) {
					soProgressText.setText(progressText);
				}
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				installer.setListener(null);
				try {
					installer.cancel();
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		});
	}

	
	public void failed( Throwable e) {
		if (box != null) {
			box.close(0);
		}
	}
	
	public void finished() {
		if (box != null) {
			box.close(0);
		}
	}
	
	public void progress(final int percent) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int pct = percent == 100 ? 99 : percent;
				if (soInstallPct != null) {
					soInstallPct.setText(MessageText.getString("dlg.auth.install.pct",
							new String[] {
								"" + pct
							}));
				}
				if (progressBar != null && !progressBar.isDisposed()) {
					// never reach 100%!
					progressBar.setSelection(pct);
				}
			}
		});
		
	}
}
