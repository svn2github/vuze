/*
 * File    : ConfigureWizard.java
 * Created : 12 oct. 2003 16:06:44
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.config.wizard;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.ui.UserPrompterResultListener;

/**
 * @author Olivier
 * 
 */
public class ConfigureWizard extends Wizard {

  //Transfer settings
  int upSpeed = 4;
  int maxUpSpeed = 40;
  int maxActiveTorrents = 7;
  int maxDownloads = 5;
  
  //Server / NAT Settings
  int serverTCPListenPort = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
  //Files / Torrents
  private String 	_dataPath;
  private boolean 	_dataPathChanged;
  String torrentPath;
  
  boolean completed = false;
 

  public 
  ConfigureWizard(
		boolean modal) 
  {
    super("configureWizard.title",modal);
    IWizardPanel panel = new LanguagePanel(this,null);
    try  {
      torrentPath = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
    } catch(Exception e) {
      torrentPath = ""; 
    }
    
    if ( COConfigurationManager.getBooleanParameter( "Use default data dir" )){
    
    	_dataPath = COConfigurationManager.getStringParameter( "Default save path" );
    	
    }else{
    	_dataPath = "";
    }
    
    this.setFirstPanel(panel);
  }
  
  public void onClose() {
		try {
			if (!completed
					&& !COConfigurationManager.getBooleanParameter("Wizard Completed")) {
				MessageBoxShell mb = new MessageBoxShell(
						MessageText.getString("wizard.close.confirmation"),
						MessageText.getString("wizard.close.message"), new String[] {
							MessageText.getString("Button.yes"),
							MessageText.getString("Button.no")
						}, 0);

				mb.open(new UserPrompterResultListener() {
					public void prompterClosed(int result) {
						if (result == 1) {
							COConfigurationManager.setParameter("Wizard Completed", true);
							COConfigurationManager.save();
						}
					}
				});

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.onClose();
	}
  
  protected String
  getDataPath()
  {
	  return( _dataPath );
  }
  
  protected void
  setDataPath(
	String	s )
  {
	  _dataPath 		= s;
	  _dataPathChanged 	= true;
  }
  
  protected boolean
  hasDataPathChanged()
  {
	  return( _dataPathChanged );
  }
}
