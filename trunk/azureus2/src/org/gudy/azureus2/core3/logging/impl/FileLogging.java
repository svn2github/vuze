/**
 * 
 */
package org.gudy.azureus2.core3.logging.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.logging.ILogEventListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

/**
 * Log events to a file.
 * 
 * @author TuxPaper
 */
// TODO: Filter
public class FileLogging implements ILogEventListener {
	public static final String LOG_FILE_NAME = "az.log";

	public static final String BAK_FILE_NAME = "az.log.bak";

	public static LogIDs[] configurableLOGIDs = { LogIDs.ALERT, LogIDs.CORE,
			LogIDs.DISK, LogIDs.GUI, LogIDs.NET, LogIDs.NWMAN, LogIDs.PEER,
			LogIDs.PLUGIN, LogIDs.TRACKER, LogIDs.CACHE };

	private static final String sTimeStampFormat = "HH:mm:ss.SSS|";

	private boolean bLogToFile = false;

	private String sLogDir = "";

	private int iLogFileMaxMB = 1;

	// List of components we don't log.  
	// Array represents LogTypes (info, warning, error)
	private ArrayList[] ignoredComponents = new ArrayList[3];

	public void initialize() {
		// Shorten from COConfigurationManager To make code more readable
		final ConfigurationManager config = ConfigurationManager.getInstance();
		boolean overrideLog = System.getProperty("azureus.overridelog") != null;

		Logger.addListener(this);

		for (int i = 0; i < ignoredComponents.length; i++) {
			ignoredComponents[i] = new ArrayList();
		}

		if (!overrideLog) {
			config.addListener(new COConfigurationListener() {
				public void configurationSaved() {
					checkLoggingConfig();
				}
			});
		}

		checkLoggingConfig();
		config.addParameterListener("Logging Enable", new ParameterListener() {
			public void parameterChanged(String parameterName) {
				bLogToFile = config.getBooleanParameter("Logging Enable");
			}
		});
	}

	private void checkLoggingConfig() {
		try {
			// Shorten from COConfigurationManager To make code more readable
			final ConfigurationManager config = ConfigurationManager.getInstance();

			boolean overrideLog = System.getProperty("azureus.overridelog") != null;
			if (overrideLog) {
				bLogToFile = true;
				sLogDir = ".";
				iLogFileMaxMB = 2;

				for (int i = 0; i < ignoredComponents.length; i++) {
					ignoredComponents[i].clear();
				}
			} else {
				bLogToFile = config.getBooleanParameter("Logging Enable");

				sLogDir = config.getStringParameter("Logging Dir", "");

				iLogFileMaxMB = config.getIntParameter("Logging Max Size");

				for (int i = 0; i < ignoredComponents.length; i++) {
					ignoredComponents[i].clear();
					int logType = indexToLogType(i);
					for (int j = 0; j < configurableLOGIDs.length; j++) {
						if (!config.getBooleanParameter("bLog." + logType + "."
								+ configurableLOGIDs[j], true))
							ignoredComponents[i].add(configurableLOGIDs[j]);
					}
				}
			}
		} catch (Throwable t) {
			Debug.printStackTrace(t);
		}
	}

	private void logToFile(String str) {
		if (!bLogToFile)
			return;

		synchronized (Logger.class) {

			SimpleDateFormat format;
			format = new SimpleDateFormat(sTimeStampFormat);

			str = format.format(new Date()) + str;

			PrintWriter pw = null;

			File file_name = new File(sLogDir + File.separator + LOG_FILE_NAME);

			try {
				pw = new PrintWriter(new FileWriter(file_name, true));
				if (pw != null)
					pw.print(str);

			} catch (Throwable e) {

				// can't log this as go recursive!!!!

			} finally {

				if (pw != null) {
					try {
						pw.close();
					} catch (Throwable e) {
						// can't log as go recursive!!!!
					}

					// two files so half
					long lMaxBytes = (iLogFileMaxMB * 1024 * 1024) / 2;

					if (file_name.length() > lMaxBytes) {
						File back_name = new File(sLogDir + File.separator + BAK_FILE_NAME);

						if ((!back_name.exists()) || back_name.delete()) {
							if (!file_name.renameTo(back_name))
								file_name.delete();
						} else {
							file_name.delete();
						}
					}
				}
			} // finally
		} // sync
	}

	private int logTypeToIndex(int entryType) {
		switch (entryType) {
			case LogEvent.LT_INFORMATION:
				return 0;
			case LogEvent.LT_WARNING:
				return 1;
			case LogEvent.LT_ERROR:
				return 2;
		}
		return 0;
	}

	private int indexToLogType(int index) {
		switch (index) {
			case 0:
				return LogEvent.LT_INFORMATION;
			case 1:
				return LogEvent.LT_WARNING;
			case 2:
				return LogEvent.LT_ERROR;
		}
		return LogEvent.LT_INFORMATION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.core3.logging.ILoggerListener2#log(org.gudy.azureus2.core3.logging.LogEvent)
	 */

	public void log(LogEvent event) {
		if (ignoredComponents[logTypeToIndex(event.entryType)]
				.contains(event.logID))
			return;

		StringBuffer text = new StringBuffer(event.entryType + "|" + event.logID
				+ "| ");
		int len = text.length() + sTimeStampFormat.length();
		boolean needLF = false;

		if (event.peer != null) {
			text.append("Peer[");
			text.append(event.peer.getIp());
			text.append(";");
			text.append(event.peer.getClient());
			text.append("]");
			needLF = true;
		}

		if (event.torrent != null) {
			if (event.peer != null)
				text.append("; ");
			text.append("Torrent: ");
			text.append(TorrentUtils.getLocalisedName(event.torrent));
			needLF = true;
		}

		if (needLF) {
			text.append("\r\n");
			for (int i = 0; i < len; i++)
				text.append(" ");
		}

		text.append(event.text);

		if (event.text == null || !event.text.endsWith("\n"))
			text.append("\r\n");

		logToFile(text.toString());
	}
}
