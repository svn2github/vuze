package org.gudy.azureus2.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

public class LocaleUtil {

  private static final String systemEncoding = System.getProperty("file.encoding");

	private static final String[] charset = {
    systemEncoding,
		"Big5","EUC-JP","EUC-KR","GB18030","GBK","ISO-2022-JP","ISO-2022-KR",
		"Shift_JIS","KOI8-R","UTF-8","windows-1251","ISO-8859-1"
	};    

  private static final CharsetDecoder[] charsetDecoder = new CharsetDecoder[charset.length];

  static {
    for (int i = 0; i < charset.length; i++) {
      try {
        charsetDecoder[i] = Charset.forName(charset[i]).newDecoder();
      } catch (Exception ignore) {
      }
    }
  }

  public static String getCharsetString(byte[] array) throws UnsupportedEncodingException {
    return new String(array, getCharset(array));
  }

	public static String getCharset(byte[] array) {
    Candidate[] candidates = new Candidate[charset.length];
    for (int i = 0; i < charset.length; i++) {
      candidates[i] = new Candidate();
      try {
        candidates[i].name = charsetDecoder[i].decode(ByteBuffer.wrap(array)).toString();
        candidates[i].charset = charset[i];
      } catch (Exception ignore) {
      }
    }

    String defaultString = candidates[0].name;

		Arrays.sort(candidates);

		int minlength = candidates[0].name.length();
		
		/*
		int filterCount = 1;
		for (int i=1;i<candidates.length;i++) {
			if (((Candidate)candidates[i]).name.length() > minlength) break;
			filterCount++;
		}
		*/

		/*
		for (int i=0;i<filterCount;i++) {
			System.out.println(((Candidate)candidates[i]).charset);
		}
		*/
       
		// If the default string length == minlength assumes that
		// the array encoding is from default charset 
		if (defaultString != null && defaultString.length() == minlength) {
			return systemEncoding;
		}

		// Here it is assumed the shorter the string, the more likely is 
		// the correct charset
		return candidates[0].charset; 
	}    

  private static class Candidate implements Comparable {
    String name;
    String charset;

    public int compareTo(Object o) {
      Candidate candidate = (Candidate)o;
      if(null == name || null == candidate.name)
        return 0;
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
}