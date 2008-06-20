/*
 * Created on Jun 20, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.plugins.utils.search;

public interface 
SearchResult 
{
	public static final int	PR_NAME				= 1;
	public static final int	PR_PUB_DATE			= 2;
	public static final int	PR_SIZE				= 3;
	public static final int	PR_LEECHER_COUNT	= 4;
	public static final int	PR_SEED_COUNT		= 5;
	public static final int	PR_SUPER_SEED_COUNT	= 6;
	public static final int	PR_CATEGORY			= 7;
	public static final int	PR_COMMENTS			= 8;
	public static final int	PR_VOTES			= 9;
	public static final int	PR_CONTENT_TYPE		= 10;
	public static final int	PR_DETAILS_LINK		= 11;
	public static final int	PR_DOWNLOAD_LINK	= 12;
	public static final int	PR_PLAY_LINK		= 13;
	public static final int	PR_PRIVATE			= 14;
	public static final int	PR_DRM_KEY			= 15;
	
	public Object
	getProperty(
		int		property_name );
}
