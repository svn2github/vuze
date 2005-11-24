/**
 * 
 */
package org.gudy.azureus2.core3.logging;

/**
 * @author TuxPaper
 */
public class LogAlert {
	// log types
	public static final int AT_INFORMATION = LogEvent.LT_INFORMATION;

	public static final int AT_WARNING = LogEvent.LT_WARNING;

	public static final int AT_ERROR = LogEvent.LT_ERROR;

	public static final boolean REPEATABLE = true;

	public static final boolean UNREPEATABLE = false;

	public int entryType;

	public Throwable err = null;

	public boolean repeatable;

	public String text;

	/**
	 * @param type
	 * @param text
	 * @param repeatable
	 */
	public LogAlert(boolean repeatable, int type, String text) {
		entryType = type;
		this.text = text;
		this.repeatable = repeatable;
	}

	public LogAlert(boolean repeatable, String text, Throwable err) {
		this(repeatable, AT_ERROR, text);
		this.err = err;
	}
}
