/*
 * File    : PluginParameter.java
 * Created : 15 déc. 2003}
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
package org.gudy.azureus2.ui.swt.config.plugins;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.pluginsimpl.ui.config.BooleanParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.ColorParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.DirectoryParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.FileParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.IntParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.IntsParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.StringParameter;
import org.gudy.azureus2.pluginsimpl.ui.config.StringsParameter;

/**
 * @author Olivier
 *
 */
public class PluginParameter {

  public PluginParameterImpl implementation;
  
  public PluginParameter(Group pluginGroup,Parameter parameter) {
    if(parameter instanceof StringParameter) {
      implementation = new PluginStringParameter(pluginGroup,(StringParameter)parameter);
    } else if(parameter instanceof IntParameter) {
      implementation = new PluginIntParameter(pluginGroup,(IntParameter)parameter);
    } else if(parameter instanceof BooleanParameter) {
      implementation = new PluginBooleanParameter(pluginGroup,(BooleanParameter)parameter);
    } else if(parameter instanceof FileParameter) {
      implementation = new PluginFileParameter(pluginGroup,(FileParameter)parameter);
    } else if(parameter instanceof DirectoryParameter) {
      implementation = new PluginDirectoryParameter(pluginGroup,(DirectoryParameter)parameter);
    } else if(parameter instanceof IntsParameter) {
      implementation = new PluginIntsParameter(pluginGroup,(IntsParameter)parameter);
    } else if(parameter instanceof StringsParameter) {
      implementation = new PluginStringsParameter(pluginGroup,(StringsParameter)parameter);
    } else if(parameter instanceof ColorParameter) {
      implementation = new PluginColorParameter(pluginGroup,(ColorParameter)parameter);
    }
  }
  
  public Control[] getControls() {
    return implementation.getControls();
  }
}
