/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class 
TOTorrentException
	extends Exception
{
	public static final int		RT_FILE_NOT_FOUND			= 1;
	public static final int		RT_ZERO_LENGTH				= 2;
	public static final int		RT_TOO_BIG					= 3;
	public static final int		RT_READ_FAILS				= 4;
	public static final int		RT_WRITE_FAILS				= 5;
	public static final int		RT_DECODE_FAILS				= 6;
	public static final int		RT_UNSUPPORTED_ENCODING		= 7;
	public static final int		RT_HASH_FAILS				= 8;
	
	protected int	reason;
	
	public
	TOTorrentException(
		String		_str,
		int			_reason )
	{
		super( _str );
		
		reason	= _reason;
	}
	
	public int
	getReason()
	{
		return( reason );
	}
}
