/*
 * Created on 09.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.util;

import java.io.FileNotFoundException;

/**
 * @author Tobias Minich
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class FileIsADirectoryException extends FileNotFoundException {

	/**
	 * @param string
	 */
	public FileIsADirectoryException(String string) {
      super(string);
	}

}
