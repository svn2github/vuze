package org.gudy.azureus2.platform.unix;

import org.gudy.azureus2.core3.config.COConfigurationManager;

public class ScriptAfterShutdown
{
	public static void main(String[] args) {
		String extraCmds = COConfigurationManager.getStringParameter(
				"scriptaftershutdown", null);
		if (extraCmds != null) {
			boolean exit = COConfigurationManager.getBooleanParameter(
					"scriptaftershutdown.exit", false);
			if (exit) {
				COConfigurationManager.removeParameter("scriptaftershutdown.exit");
			}
			COConfigurationManager.removeParameter("scriptaftershutdown");
			COConfigurationManager.save();
			System.out.println(extraCmds);
			if (exit) {
				System.out.println("exit");
			}
		}
	}

	public static void addExtraCommand(String s) {
		String extraCmds = COConfigurationManager.getStringParameter(
				"scriptaftershutdown", null);
		if (extraCmds == null) {
			extraCmds = s + "\n";
		} else {
			extraCmds += s + "\n";
		}
		COConfigurationManager.setParameter("scriptaftershutdown", extraCmds);
	}

	public static void setRequiresExit(boolean requiresExit) {
		if (requiresExit) {
			COConfigurationManager.setParameter("scriptaftershutdown.exit", true);
		}
	}
}
