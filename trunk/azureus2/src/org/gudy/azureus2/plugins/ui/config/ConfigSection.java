/*
 * File    : ConfigSection.java
 * Created : 23 jan. 2004
 * By      : Paper
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

package org.gudy.azureus2.plugins.ui.config;

import org.eclipse.swt.widgets.Composite;

/**
 * Extend this class to add a new configuration panel to the Config view.
 */
public interface ConfigSection {
  /**
   * Configuration panel will be added to main configuration view area
   */
  public static final String SECTION_ROOT = "root";
  /**
   * Configuration panel will be added to the plugins view area.
   */
  public static final String SECTION_PLUGINS = "plugins";

  /**
   * Returns section you want your configuration panel to be under.
   * See SECTION_* constants.  To add a subsection to your own ConfigSection,
   * return the configSectionGetName result of your parent.<br>
   */
  public String configSectionGetParentSection();


  /**
   * Create your own configuration panel here.  It can be anything that inherits
   * from SWT's Composite class.
   * Please be mindfull of small screen resolutions
   *
   * @param parent The parent of your configuration panel
   * @return your configuration panel
   */
  public Composite configSectionCreate(Composite parent);

  /**
   * In order for the plugin to display its section correctly, a key in the
   * Plugin language file will need to contain 
   * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
   *
   * @return The name of the configuration section
   */
  public String configSectionGetName();

  /**
   * User selected Save.
   * All saving of non-plugin tabs have been completed, as well as
   * saving of plugins that implement org.gudy.azureus2.plugins.ui.config
   * parameters.
   */
  public void configSectionSave();

  /**
   * Config view is closing
   */
  public void configSectionDelete();
}
