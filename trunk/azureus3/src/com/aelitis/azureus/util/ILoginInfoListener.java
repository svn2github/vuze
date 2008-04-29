/**
 * 
 */
package com.aelitis.azureus.util;

import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

public interface ILoginInfoListener
{
	public void loginUpdate(LoginInfo info, boolean isNewLoginID);
}