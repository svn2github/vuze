package org.gudy.azureus2.platform.macosx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.gudy.azureus2.core3.util.FileUtil;

public class PListEditor {
	
	String plistFile;
	
	
	public PListEditor(String plistFile) {
		this.plistFile = plistFile;
	}
	
	public void setFileTypeExtensions(String[] extensions) {
		StringBuffer value = new StringBuffer();
		StringBuffer find = new StringBuffer();
		find.append("(?s).*?<key>CFBundleDocumentTypes</key>\\s*<array>.*?<key>CFBundleTypeExtensions</key>\\s*<array>");
		for(int i = 0 ; i < extensions.length ; i++) {
			value.append("\n\t\t\t\t<string>");
			value.append(extensions[i]);
			value.append("</string>");
			
			find.append(".*?");
			find.append(extensions[i]);
		}
		value.append("\n\t\t\t");
		
		find.append(".*?</array>.*");
		String match = "(?s)(<key>CFBundleDocumentTypes</key>\\s*<array>.*?<key>CFBundleTypeExtensions</key>\\s*<array>)(.*?)(</array>)";
		
		setValue(find.toString(), match, value.toString());
	}
	
	public void setSimpleStringValue(String key,String value) {
		String find = "(?s).*?<key>" + key + "</key>\\s*" + "<string>" + value + "</string>.*";
		String match = "(?s)(<key>" + key + "</key>\\s*" + "<string>)(.*?)(</string>)";
		setValue(find, match, value);
	}
	
	private boolean isValuePresent(String match) {
		String fileContent = getFileContent();
		if(fileContent != null) {
			System.out.println("Searching for:\n" + match);
			return fileContent.matches(match);
		}
		return false;
	}
	
	/**
	 * 
	 * @param find the regex expression to find if the value is already present
	 * @param match the regex expression that will match for the replace, it needs to capture 3 groups, the 2nd one being replaced by value
	 * @param value the value that replaces the 2nd match group
	 */
	private void setValue(String find,String match,String value) {
		String fileContent = getFileContent();
		if(fileContent != null) {
			if(!isValuePresent(find)) {
				System.out.println("Changing " +plistFile);
				fileContent = fileContent.replaceFirst(match, "$1"+value + "$3");
				setFileContent(fileContent);
				touchFile();
			}
		}
	}
	
	private String getFileContent() {
		try {
			return FileUtil.readFileAsString(new File(plistFile), 10* 1024);
		} catch(IOException e) {
			return null;
		}
	}
	
	private void setFileContent(String fileContent) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(plistFile);
			fos.write(fileContent.getBytes("UTF-8"));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void touchFile() {
		String command[] = new String[] {
				"touch",
				plistFile
		};
		try {
			Runtime.getRuntime().exec(command);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		PListEditor editor = new PListEditor("/Applications/Azureus.app/Contents/Info.plist");
		editor.setFileTypeExtensions(new String[] {"torrent","tor","vuze"});
		editor.setSimpleStringValue("CFBundleName", "Vuze");
		editor.setSimpleStringValue("CFBundleTypeName", "Vuze Download");
		editor.setSimpleStringValue("CFBundleGetInfoString","Vuze");
	}

}
