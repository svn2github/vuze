/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * AddFind.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.ui.console.ConsoleInput;
import org.pf.file.FileFinder;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AddFind implements IConsoleCommand {
	
	public static void commandAdd(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())) {
			String subcommand = "";
			for (int i=0; i<args.size(); i++)
				subcommand += (String) args.get(0) + " ";
			subcommand = subcommand.trim();
			boolean scansubdir = false;
			CommandLine commands = null;
			CommandLineParser parser = new PosixParser();
			Options options = new Options();
			OptionGroup addy = new OptionGroup();
			options.addOption("o", "output", true, "Output Directory.");
			addy.addOption(new Option("r", "recurse", false, "Recurse Subdirs."));

			OptionBuilder.hasArgs();
			OptionBuilder.withDescription("Add found file nr x.");
			addy.addOption(OptionBuilder.create('n'));

			options.addOption(new Option("f", "find", false, "Only find files, don't add."));
			options.addOptionGroup(addy);
			try {
				commands = parser.parse(options, subcommand.split(" "), true);
			} catch (ParseException exp) {
				ci.out.println("> Parsing add commandline failed. Reason: " + exp.getMessage());
			}
			if (commands.hasOption('r'))
				scansubdir = true;
			String outputDir = "";
			if (commands.hasOption('o'))
				outputDir = commands.getOptionValue('o');
			else
				try {
					outputDir = COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory");
				} catch (Exception e) {
					e.printStackTrace();
				}

			boolean Finding = false;

			if (commands.hasOption('n')) {
				String[] numbers = commands.getOptionValues('n');
				ci.out.println("> Numbers:");
				for (int i = 0; i < numbers.length; i++)
					ci.out.println(numbers[i]);
				String[] whatelse = commands.getArgs();
				ci.out.println("> Else:");
				for (int i = 0; i < whatelse.length; i++)
					ci.out.println(whatelse[i]);
			} else {
				String[] whatelse = commands.getArgs();
				for (int j = 0; j < whatelse.length; j++) {
					if (whatelse[j].toUpperCase().startsWith("HTTP://")) {
						/*
						 * try { out.println("> Starting Download of
						 * "+whatelse[j]+" ..."); HTTPDownloader dl = new
						 * HTTPDownloader(whatelse[j],
						 * COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
						 * String file = dl.download();
						 * gm.addDownloadManager(file, outputDir);
						 * out.println("> Download of "+whatelse[j]+"
						 * succeeded"); } catch (Exception e) { out.println(">
						 * Download of "+whatelse[j]+" failed");
						 */
						ci.out.println("> Starting Download of " + whatelse[j] + " ...");
						try {
							TorrentDownloaderFactory.downloadManaged(whatelse[j]);
						} catch (Exception e) {
							e.printStackTrace(ci.out);
						}
					} else {
						File test = new File(whatelse[j]);
						if (test.exists()) {
							if (test.isDirectory()) {
								File[] toadd = FileFinder.findFiles(whatelse[j], "*.torrent;*.tor", false);
								if ((toadd != null) && (toadd.length > 0)) {
									for (int i = 0; i < toadd.length; i++) {
										ci.gm.addDownloadManager(toadd[i].getAbsolutePath(), outputDir);
										ci.out.println("> '" + toadd[i].getAbsolutePath() + "' added.");
										ci.torrents = null;
									}
								} else {
									ci.out.println("> Directory '" + whatelse[j] + "' seems to contain no torrent files.");
								}
							} else {
								ci.gm.addDownloadManager(whatelse[j], outputDir);
								ci.out.println("> '" + whatelse[j] + "' added.");
								ci.torrents = null;
							}
						} else {
							ci.adds = FileFinder.findFiles(whatelse[j].substring(0, whatelse[j].lastIndexOf(System.getProperty("file.separator"))), whatelse[j].substring(whatelse[j].lastIndexOf(System.getProperty("file.separator")) + 1), false);
							if ((ci.adds != null) && (ci.adds.length > 0)) {
								ci.out.println("> Found " + Integer.toString(ci.adds.length) + " files:");
								for (int i = 0; i < ci.adds.length; i++) {
									try {
										ci.out.println(ci.adds[i].getCanonicalPath());
									} catch (Exception e) {
										ci.out.println(ci.adds[i].getAbsolutePath());
									}
								}
							} else {
								ci.out.println("> No files found. Searched for '" + subcommand.substring(subcommand.lastIndexOf(System.getProperty("file.separator")) + 1) + "' in '" + subcommand.substring(0, subcommand.lastIndexOf(System.getProperty("file.separator"))));
							}
						}
					}
				}
			}
		} else {
			ci.out.println("> Missing subcommand for 'add'\r\n> add syntax: see 'help add'");
		}
	}
	
	public static void RegisterCommands() {
		try {
			ConsoleInput.RegisterCommand("add", AddFind.class.getMethod("commandAdd", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("a", AddFind.class.getMethod("commandAdd", ConsoleCommandParameters));
      ConsoleInput.RegisterHelp("add [.torrent path|url]\t\t+\tAdd a download from the given .torrent file path or url. Example: 'add /path/to/the.torrent' or 'add http://www.url.com/to/the.torrent'");
		} catch (Exception e) {e.printStackTrace();}
	}
}
