/*
 * File    : EnablerParameter.java
 * Created : 17 nov. 2003
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
 
package org.gudy.azureus2.plugins.ui;

/**
 * represents a Parameter that can enable/disable other parameters according to its State.<br>
 * @author Olivier
 *
 */
public interface EnablerParameter extends Parameter {
  
  /**
   * adds a parameter to be enabled when the EnablerParameter is selected.<br>
   * The parameter will be disabled when the EnablerParameter isn't selected.
   * @param parameter the parameter to act on.
   */
  public void addEnabledOnSelection(Parameter parameter);
  
  /**
   * adds a parameter to be disabled when the EnablerParameter is selected.<br>
   * The parameter will be enabled when the EnablerParameter isn't selected.
   * @param parameter the parameter to act on.
   */
  public void addDisabledOnSelection(Parameter parameter);
}
