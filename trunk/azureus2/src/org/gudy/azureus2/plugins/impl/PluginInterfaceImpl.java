/*
 * File    : PluginInterfaceImpl.java
 * Created : 12 nov. 2003
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
 
package org.gudy.azureus2.plugins.impl;

import java.util.Properties;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.config.impl.ParameterRepository;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.impl.PeersTableExtensions;
import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 *
 */
public class PluginInterfaceImpl implements PluginInterface {
  
  Properties props;
  String pluginDir;
  PluginConfig config;

  public PluginInterfaceImpl(
      String key,
      Properties props,
      String pluginDir) {
    this.props = props;
    this.pluginDir = pluginDir;
    this.config = new PluginConfigImpl(key);
  }

  public void addView(PluginView view)
  {
    MainWindow window = MainWindow.getWindow();
    if(window != null) {
      window.addPluginView(view);
    }
  } 

  public void openTorrentFile(String fileName) {
    MainWindow.getWindow().openTorrent(fileName);
  }

  public void openTorrentURL(String url) {
    new FileDownloadWindow(MainWindow.getWindow().getDisplay(),url);
  }
      
  public Properties getPluginProperties() {
    return props;
  }
  
  public String getPluginDirectoryName() {
    return pluginDir;
  }

  public void addConfigUIParameters(Parameter[] parameters, String displayName) {
  	ParameterRepository.getInstance().addPlugin(parameters, displayName);
  }


  public PluginConfig getPluginconfig() {
    return config;
  }


  public PluginConfigUIFactory getPluginConfigUIFactory() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void addColumnToPeersTable(String columnName, PluginPeerItemFactory item) {
    PeersTableExtensions.getInstance().addExtension(columnName,item);
  }
  
  public Tracker getTracker() {
    // TODO Auto-generated method stub
    return null;
  }
}
