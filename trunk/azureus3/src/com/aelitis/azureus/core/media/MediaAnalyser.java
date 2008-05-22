package com.aelitis.azureus.core.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class MediaAnalyser {
	
	String analyserFilePath;
	boolean screenshotSupported;
	boolean validExecutable;
	
	public MediaAnalyser(String analyserFilePath) {
		this.analyserFilePath = analyserFilePath;
		this.screenshotSupported = false;
		this.validExecutable = false;
		String[] command = {
				analyserFilePath,
				"-vo", "help"
		};
		try {
			Process p = Runtime.getRuntime().exec(command);
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while( (line = br.readLine()) != null) {
				if(line.startsWith("MPlayer")) {
					this.validExecutable = true;
				}
				if(line.indexOf("jpeg") != -1) {
					this.screenshotSupported = true;
				}
			}
		} catch(Exception e) {
			
		}
	}
	
	public boolean isScreenshotSupported() {
		return screenshotSupported;
	}
	
	public boolean isValidExecutable() {
		return validExecutable;
	}
	
	public MediaInfo analyse(String file,String screenshotsPath) {
		MediaInfo result = new MediaInfo(file);
		if(!validExecutable) {
			return result;
		}
		try {
			String[] command;
			if(screenshotSupported) {
				command = new String[] {
						analyserFilePath,
						"-identify",
						"-frames", "10",
						"-vf", "scale=336:-2",
						"-vo", "jpeg:outdir=\"" + screenshotsPath + "\"",
						"-ao", "null",
						"-sstep", "5",
						file
				};
			} else {
				command = new String[] {
						analyserFilePath,
						"-identify",
						"-frames", "0",
						"-ao", "null",
						file
				};
			}
			final Process p = Runtime.getRuntime().exec(command);
			Thread tWatcher = new Thread("MediaAnalyser Process Watcher") {
				public void run() {
					try {
						Thread.sleep(10000);
					} catch(Exception e) {
						return;
					}
					try {
						if(p.exitValue() == 0) {
							return;
						}
					} catch(IllegalThreadStateException e) {
						//p.destroy();
					} finally {
						p.destroy();
					}
				}
			};
			tWatcher.start();
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			int width = 0, height = 0;
			String demuxer = null, videoFormat = null, audioFormat = null;
			float duration = 0f;
			boolean videoCodecFound = false,audioCodecFound = false;
			
			while( (line = br.readLine()) != null) {
				if(line.indexOf("=") != -1) {
					StringTokenizer st = new StringTokenizer(line,"=");
					if(st.countTokens() == 2) {
						String id = st.nextToken();
						String value = st.nextToken();
						if("ID_DEMUXER".equals(id)) {
							demuxer = value;
						} else if("ID_VIDEO_FORMAT".equals(id)) {
							videoFormat = value;
						} else if("ID_AUDIO_FORMAT".equals(id)) {
							audioFormat = value;
						} else if("ID_LENGTH".equals(id)) {
							try {
								duration = Float.parseFloat(value);
							} catch(Exception e) {e.printStackTrace();}
						} else if("ID_VIDEO_WIDTH".equals(id)) {
							try {
								width = Integer.parseInt(value);
							} catch(Exception e) {e.printStackTrace();}
						} else if("ID_VIDEO_HEIGHT".equals(id)) {
							try {
								height = Integer.parseInt(value);
							} catch(Exception e) {e.printStackTrace();}
						}
					}
				}
				if(line.indexOf("Selected video codec:") != -1) {
					videoCodecFound = true;
				}
				if(line.indexOf("Selected audio codec:") != -1) {
					audioCodecFound = true;
				}
			}
			
			if(videoCodecFound && audioCodecFound) {
				result.setPlayable(true);
				ExtendedMediaInfo emi = new ExtendedMediaInfo();
				emi.setAudioFormat(audioFormat);
				emi.setVideoFormat(videoFormat);
				emi.setDuration(duration);
				emi.setWidth(width);
				emi.setHeight(height);
				emi.setDemuxer(demuxer);
				result.setExtendedMediaInfo(emi);
			}
			
			tWatcher.interrupt();
			
		} catch(IOException e) {
			//Likely because we destroyed the process
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static void main(String args[]) {
		if(args.length < 2) return;
		MediaAnalyser analyser = new MediaAnalyser(args[0]);
		System.out.println(analyser.screenshotSupported);
		analyseStatic(analyser, args[1]);
	}
	
	
	public static void analyseStatic(MediaAnalyser analyser,String file) {
		File f = new File(file);
		if(f.isDirectory()) {
			File[] contents = f.listFiles();
			for(int i = 0 ; i < contents.length ; i++) {
				analyseStatic(analyser, contents[i].getAbsolutePath());
			}
		} else if(f.isFile()) {
			String screenshotPath = "/tmp_screenshots/screens" + file.hashCode();
			System.out.println("Analysing : " + file + ", screenshotPath : " + screenshotPath);
			System.out.println(analyser.analyse(file,screenshotPath));
		}
		
	}

}
