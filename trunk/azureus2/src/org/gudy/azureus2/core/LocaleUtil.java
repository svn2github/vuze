package org.gudy.azureus2.core;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocaleUtil {
    
	private static final String[] charset = {
		System.getProperty("file.encoding"),
		"Big5","EUC-JP","EUC-KR","GB18030","GBK","ISO-2022-JP","ISO-2022-KR",
		"Shift_JIS","KOI8-R","UTF-8","windows-1251","ISO-8859-1"
	};    
    
	public static String getCharset(byte[] array) {

		class Candidate implements Comparable {
			String name;
			String charset;

			public int compareTo(Object o) {
				Candidate candidate = (Candidate)o;
				if (candidate.name.hashCode()==name.hashCode() &&
					candidate.charset.hashCode()==charset.hashCode()) {
					return 0;
				}
				if (name.length() < candidate.name.length()) {
					return -1;
				} 
				return 1;
			}
		}

		List candidateList  = new ArrayList();
		for (int i=0;i<charset.length;i++) {
			try {
				Charset cset = Charset.forName(charset[i]);
				CharsetDecoder decoder = cset.newDecoder();
				CharBuffer cb=decoder.decode(ByteBuffer.wrap(array));
				Candidate candidate = new Candidate();
				candidate.name=cb.toString();
				candidate.charset=charset[i];
				candidateList.add(candidate);
			}
			catch (Exception e) {
			}
		}

		Object [] candidates = candidateList.toArray();
		Arrays.sort(candidates);

		int minlength = ((Candidate)candidates[0]).name.length();
		
		/*
		int filterCount = 1;
		for (int i=1;i<candidates.length;i++) {
			if (((Candidate)candidates[i]).name.length() > minlength) break;
			filterCount++;
		}
		*/

		// Compute default file.encoding candidate string
		String defaultString = null;
		try {
			Charset cset = Charset.forName(System.getProperty("file.encoding"));
			CharsetDecoder decoder = cset.newDecoder();
			CharBuffer cb=decoder.decode(ByteBuffer.wrap(array));
			defaultString = cb.toString();
		}
		catch (Exception e) {
		}

		/*
		for (int i=0;i<filterCount;i++) {
			System.out.println(((Candidate)candidates[i]).charset);
		}
		*/
       
		// If the default string length == minlength assumes that
		// the array encoding is from default charset 
		if (defaultString !=null && defaultString.length()==minlength) {
			return System.getProperty("file.encoding");
		}

		// Here it is assumed the shorter the string, the more likely is 
		// the correct charset
		return ((Candidate)candidates[0]).charset; 
	}    
}