package org.gudy.azureus2.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;

public class LocaleUtil implements ILocaleUtilChooser {
  
  private static final String systemEncoding = System.getProperty("file.encoding"); //$NON-NLS-1$
  
  private static final String[] charset = {
    systemEncoding,
    "Big5","EUC-JP","EUC-KR","GB18030","GBK","ISO-2022-JP","ISO-2022-KR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    "Shift_JIS","KOI8-R","UTF-8","windows-1251","ISO-8859-1" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
  };
  
  protected static final String[] generalCharsets = {
    "ISO-8859-1", "UTF-8", systemEncoding  //$NON-NLS-1$ //$NON-NLS-2$
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
  
  protected boolean rememberEncodingDecision = true;
  protected boolean differentEncodingsChoosed = false;
  
  protected String lastChoosedEncoding = null;
  protected boolean waitForUserInput = true;
  
  protected static ILocaleUtilChooser chooser = null;
  
  public static LocaleUtil getLocaleUtil(Object lastEncoding) {
    return (chooser==null)?new LocaleUtil(lastEncoding):chooser.getProperLocaleUtil(lastEncoding);
  }
  
  public static void setLocaleUtilChooser(ILocaleUtilChooser ch) {
    chooser=ch;
  }
  
  public static String getCharsetString(byte[] array) throws UnsupportedEncodingException {
    return new String(array, getCharset(array));
  }
  
  public static String getCharset(byte[] array) {
    Candidate[] candidates = getCandidates(array);
    
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
  
  protected static Candidate[] getCandidates(byte[] array) {
    Candidate[] candidates = new Candidate[charset.length];
    int j=0;
    for (int i = 0; i < charset.length; i++) {
      candidates[i] = new Candidate();
      try {
        candidates[j].name = charsetDecoder[i].decode(ByteBuffer.wrap(array)).toString();
        candidates[j].charset = charset[i];
        j++;
      } catch (Exception ignore) {
      }
    }
    return candidates;
  }
  
  protected static class Candidate implements Comparable {
    private String name;
    private String charset;
    
    public String getName() {
      return name;
    }
    
    public String getCharset() {
      return charset;
    }
    
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
    /**
     * only used for contains()
     * Warning: this implementation covers not all special cases
     */
    public boolean equals(Object obj) {
      Candidate other = (Candidate) obj;
      return name.equals(other.name);
    }
    
	public int hashCode() {
		return 31*name.hashCode()+charset.hashCode();
	}
    
  }
  
  /**
   * @param lastEncoding the last (saved) encoding, which was used for the torrent
   */
  public LocaleUtil(Object lastEncoding) {
    super();
    if(lastEncoding != null) {
      String encoding = lastEncoding instanceof byte[] ? new String((byte[]) lastEncoding) : (String) lastEncoding;
      for (int i = 0; i < charset.length; i++) {
        if(charset[i].equals(encoding)) {
          lastChoosedEncoding = encoding;
          return;
        }
      }
    }
  }
  
  public String getChoosableCharsetString(byte[] array) throws UnsupportedEncodingException {
    throw new UnsupportedEncodingException("Hello, this is your base class speaking. You need to implement an ILocaleUtilChooser interface. This method is abstract here.");
  }

  
  public LocaleUtil() {
    this(null);
  }
  
  /**
   * @return true, if the lastChoosedEncoding should be remembered and only one encoding was always choosed
   */
  public boolean canEncodingBeSaved() {
    return !differentEncodingsChoosed && rememberEncodingDecision && lastChoosedEncoding != null;
  }
  
  /**
   * @return the last encoding choosed by the user; can be null
   */
  public String getLastChoosedEncoding() {
    return lastChoosedEncoding;
  }
  
  public LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtil(lastEncoding);
  }
  
}