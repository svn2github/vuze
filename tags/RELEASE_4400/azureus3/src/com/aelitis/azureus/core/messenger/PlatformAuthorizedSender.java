/**
 * Created on Apr 23, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 
package com.aelitis.azureus.core.messenger;

import java.net.URL;

import org.gudy.azureus2.core3.util.AESemaphore;

/**
 * @author TuxPaper
 * @created Apr 23, 2008
 *
 */
public interface PlatformAuthorizedSender
{

	/**
	 * @param url
	 * @param data
	 * @param sem_waitDL 
	 * @param loginAndRetry 
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	void startDownload(URL url, String data, AESemaphore sem_waitDL,
			boolean loginAndRetry);

	String getResults();

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	void clearResults();
}
