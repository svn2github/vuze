package org.gudy.azureus2.core3.internat;

import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class
LocaleUtil 
{
  
  private static final String systemEncoding = System.getProperty("file.encoding");
  
  private static final String[] manual_charset = {
	systemEncoding,	// must be first entry due to code below that gets the system decoder
	"Big5","EUC-JP","EUC-KR","GB18030","GB2312","GBK","ISO-2022-JP","ISO-2022-KR",
	"Shift_JIS","KOI8-R",Constants.DEFAULT_ENCODING,"windows-1251",Constants.BYTE_ENCODING 
  };
  
	// the general ones *must* also be members of the above manual ones
  	
  protected static final String[] generalCharsets = {
	Constants.BYTE_ENCODING, Constants.DEFAULT_ENCODING, systemEncoding
  };
  
   private static LocaleUtil singleton = new LocaleUtil();
  
   public static LocaleUtil
   getSingleton()
   {
   	return( singleton );
   }
   
   private LocaleUtilDecoder[] 	all_decoders;
   private LocaleUtilDecoder[]	general_decoders;
   private LocaleUtilDecoder	system_decoder;
     
   private List				listeners	= new ArrayList();
  
  
  private 
  LocaleUtil() 
  {
	List	decoders 		= new ArrayList();
  	List	decoder_names	= new ArrayList();
  	
	for (int i = 0; i < manual_charset.length; i++) {
	   try {
		 String	name = manual_charset[i];
		 
		 CharsetDecoder decoder = Charset.forName(name).newDecoder();
		 
		 decoder_names.add( name );
		 
		 LocaleUtilDecoder	lu_decoder =  new LocaleUtilDecoderReal(decoder, name);
		 
		 if ( i == 0 ){
		 	
		 	system_decoder = lu_decoder;
		 }
		 
		 decoders.add( lu_decoder );
		 		 
	   }catch (Exception ignore) {
	   }
	 }

	general_decoders = new LocaleUtilDecoder[generalCharsets.length];
	
	for (int i=0;i<general_decoders.length;i++){
		
		int	gi = decoder_names.indexOf( generalCharsets[i]);
		
		if ( gi != -1 ){
		
			general_decoders[i] = (LocaleUtilDecoder)decoders.get(gi);
		}
	}

	boolean show_all = COConfigurationManager.getBooleanParameter("File.Decoder.ShowAll" );

	if ( show_all ){
		
		Map m = Charset.availableCharsets();
	  	
		Iterator it = m.keySet().iterator();
	
		while(it.hasNext()){
	  		
			String	charset_name = (String)it.next();
	  		
			if ( !decoder_names.contains( charset_name)){
	  		
				try {
				  CharsetDecoder decoder = Charset.forName(charset_name).newDecoder();
				 
				  decoders.add( new LocaleUtilDecoderReal(decoder, charset_name));
				  
				  decoder_names.add( charset_name );
				 
				} catch (Exception ignore) {
				}
			}
		}
	}
    
	decoders.add( new LocaleUtilDecoderFallback());

	all_decoders	= new LocaleUtilDecoder[ decoders.size()];
	
	decoders.toArray( all_decoders); 
  }
  
  public String
  getSystemEncoding()
  {
  	return( systemEncoding );
  }
  
  public LocaleUtilDecoder[]
  getDecoders()
  {
  	return( all_decoders );
  }
 
    public LocaleUtilDecoder[]
	getGeneralDecoders()
	{
	   	return( general_decoders );
	}
  
  public LocaleUtilDecoder
  getSystemDecoder()
  {
  	return( system_decoder );
  }
  
  protected LocaleUtilDecoderCandidate[] 
  getCandidates(
	byte[] array ) 
  {
	LocaleUtilDecoderCandidate[] candidates = new LocaleUtilDecoderCandidate[all_decoders.length];
    
	boolean show_less_likely_conversions = COConfigurationManager.getBooleanParameter("File.Decoder.ShowLax" );

	for (int i = 0; i < all_decoders.length; i++){
    	
	  candidates[i] = new LocaleUtilDecoderCandidate(i);
      
	  try{
			LocaleUtilDecoder decoder = all_decoders[i];
      	      	
			String str = decoder.tryDecode( array, show_less_likely_conversions );

			if ( str != null ){
				
				candidates[i].setDetails( decoder, str );
			}
	  } catch (Exception ignore) {
      	
	  }
	}
    
    /*
	System.out.println( "getCandidates: = " + candidates.length );
	
	for (int i=0;i<candidates.length;i++){
		
		Candidate	cand = candidates[i];
		
		if ( cand != null ){
		
			String	value = cand.getValue();
			
			if ( value != null ){
			
				System.out.println( cand.getDecoder().getName() + "/" + (value==null?-1:value.length()) + "/" + value );
			}
		}  
	}
	*/
	
	return candidates;
  }
    
  protected LocaleUtilDecoderCandidate 
  getChoosableCharsetString(
  	byte[] 		array,
	Object		decision_owner)
  
  	throws UnsupportedEncodingException
  {
    LocaleUtilDecoderCandidate[] candidates = getCandidates(array);
    
    for (int i=0;i<listeners.size();i++){
    	
    	LocaleUtilDecoderCandidate selected = ((LocaleUtilListener)listeners.get(i)).selectDecoder( this, decision_owner, candidates );
    	
    	if ( selected != null ){
    	    		
    		return( selected );
    	}
    }
    	    
	LocaleUtilDecoderCandidate cand = new LocaleUtilDecoderCandidate(0);
	
	cand.setDetails( getSystemDecoder(), new String( array ));
	
	return( cand );
  }
  
  public void
  addListener(
  	LocaleUtilListener	l )
  {
  	listeners.add(l);
  }
  
  public void
  removeListener(
  	LocaleUtilListener	l )
  {
  	listeners.remove(l);
  }
  
  public LocaleUtilDecoder
  getTorrentEncodingIfAvailable(
  		TOTorrent		torrent )
  
  	throws TOTorrentException, UnsupportedEncodingException
  {
  	String	encoding = torrent.getAdditionalStringProperty( "encoding" );
  	
  	if ( encoding != null ){
  		
  		for (int i=0;i<all_decoders.length;i++){
  			
  			if ( all_decoders[i].getName().equals( encoding )){
  				
  				return( all_decoders[i] );
  			}
  		}
  	}
  	
  	return( null );
  }
  	
	public LocaleUtilDecoder
	getTorrentEncoding(
  		TOTorrent		torrent )
  		
  		throws TOTorrentException, UnsupportedEncodingException
  	{
		String	encoding = torrent.getAdditionalStringProperty( "encoding" );
		
		if ( encoding != null ){
			
			for (int i=0;i<all_decoders.length;i++){
				
				if ( all_decoders[i].getName().equals( encoding )){
					
					return( all_decoders[i] );
				}
			}
		}
				
		LocaleUtilDecoderCandidate	candidate = getChoosableCharsetString( torrent.getName(), torrent );
		
		if ( candidate.getDecoder() == getSystemDecoder() ){
			
			TOTorrentFile[]	files = torrent.getFiles();
			
			for (int i=0;i<files.length;i++){
				
				TOTorrentFile	file = files[i];
				
				byte[][] comps = file.getPathComponents();
				
				for (int j=0;j<comps.length;j++){
					
					candidate = getChoosableCharsetString( comps[j], torrent );
					
					if ( candidate.getDecoder() != getSystemDecoder() ){
						
						break;
					}
				}
				
				if ( candidate.getDecoder() != getSystemDecoder() ){
					
					break;
				}
			}
		}
		
		if ( candidate.getDecoder() == getSystemDecoder() ){

			byte[]	comment = torrent.getComment();
			
			if ( comment != null ){
				
				candidate = getChoosableCharsetString(comment, torrent);
			}
		}
		if ( candidate.getDecoder() == getSystemDecoder() ){

			byte[]	created = torrent.getCreatedBy();
			
			if ( created != null ){
				
				candidate = getChoosableCharsetString(created, torrent);
			}
		}
		        	
		torrent.setAdditionalStringProperty("encoding", candidate.getDecoder().getName());
            
		TorrentUtils.writeToFile( torrent );
			
		return( candidate.getDecoder());
  	}
	
	public void
	setTorrentEncoding(
		TOTorrent		torrent,
		String			encoding )
	{
		torrent.setAdditionalStringProperty("encoding", encoding );
	}
	
	public void
	setDefaultTorrentEncoding(
		TOTorrent		torrent )
	{
		setTorrentEncoding( torrent, Constants.DEFAULT_ENCODING );
	}
}