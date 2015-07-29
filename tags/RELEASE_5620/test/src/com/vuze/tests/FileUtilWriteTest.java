/**
 * Created on Nov 22, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.vuze.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.FileUtil;

/**
 * @author TuxPaper
 * @created Nov 22, 2010
 *
 */
public class FileUtilWriteTest
{
	public static void main(String[] args) {
		int total = 15;
		while (total-- > 0) {
  		try {
  			new AEThread2("foo " + total) {
					public void run() {
						while (true) {
  						File file = new File("c:\\temp\\TMP" + (long) (Math.random() * 6) + ".config");
  						try {
  							Thread.sleep((long) (Math.random() * 10000));
  							writeOne(file);
  						} catch (Exception e) {
  							// TODO Auto-generated catch block
  							e.printStackTrace();
  						}
						}
					}
				}.start();
  		} catch (Exception e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
		}
		
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("done");
	}

	private static File writeOne(File file) throws IOException {
		byte[] b = new byte[(int) (Math.random() * 165535)];
		HashMap map = new HashMap();
		map.put("info", b);
		
		if (!file.exists()) {
  		FileOutputStream os = new FileOutputStream(file);
  		os.write(b);
  		os.close();
		}
		
		System.out.println(file);
		FileUtil.writeResilientFile(file, map);
		
		return file;
	}
}
