/* Written and copyright 2001 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */
package org.gudy.azureus2.ui.web;

import java.io.IOException;

public interface Jhttpp2InputStream
{
  /** reads the data */
  public int read_f(byte[] b) throws IOException;
}
