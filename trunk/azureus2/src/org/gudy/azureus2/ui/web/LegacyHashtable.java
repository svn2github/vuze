/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * LegacyHashtable.java
 *
 * Created on 31. August 2003, 18:16
 */

package org.gudy.azureus2.ui.web;

import java.util.Hashtable;

/**
 *
 * @author  tobi
 */
public class LegacyHashtable extends Hashtable {
  
  /** Creates a new instance of LegacyHashtable */
  public LegacyHashtable() {
    super();
  }
  
  public Object get(Object key) {
    if (containsKey(key))
      return super.get(key);
    else
      return key;
  }
  
}
