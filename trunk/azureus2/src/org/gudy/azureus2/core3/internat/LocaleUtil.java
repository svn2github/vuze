package org.gudy.azureus2.core3.internat;

import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
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
	"Shift_JIS","KOI8-R",
	"TIS-620",	// added for bug #1008848 
	Constants.DEFAULT_ENCODING,"windows-1251",Constants.BYTE_ENCODING 
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
   private LocaleUtilDecoder	fallback_decoder;
     
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
		 
		 if ( decoder != null ){
		 	
			 LocaleUtilDecoder	lu_decoder =  new LocaleUtilDecoderReal(decoders.size(),decoder);
			 
			 decoder_names.add( lu_decoder.getName());
			
			 if ( i == 0 ){
			 	
			 	system_decoder = lu_decoder;
			 }
			 
			 decoders.add( lu_decoder );
			 
		 }else if ( i == 0 ){
		 	
		 	Debug.out( "System decoder failed to be found!!!!" );
		 }
		 
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
				 
				  if ( decoder != null ){
				  	
				  	LocaleUtilDecoder	lu_decoder = new LocaleUtilDecoderReal(decoders.size(),decoder);
				  
				  	decoders.add( lu_decoder);
				  
				  	decoder_names.add( lu_decoder.getName());
				  }
				 
				} catch (Exception ignore) {
				}
			}
		}
	}
    
	fallback_decoder = new LocaleUtilDecoderFallback(decoders.size());
	
	decoders.add( fallback_decoder );

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
    
  protected LocaleUtilDecoder[]
  getCandidateDecoders(
  	byte[]		array )
  {
  	LocaleUtilDecoderCandidate[] 	candidates = getCandidates( array );
  	
  	List	decoders = new ArrayList();
  	
  	for (int i=0;i<candidates.length;i++){
  	
  		LocaleUtilDecoder	d = candidates[i].getDecoder();
  		
  		if ( d != null ){
  			
  			decoders.add(d);
  		}
  	}
  	
  	LocaleUtilDecoder[]	res = new LocaleUtilDecoder[decoders.size()];
  	
  	decoders.toArray( res );
  	
  	return( res );
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
  		
  			// get canonical name
  		
		String canonical_name = Charset.forName(encoding).name();

  		for (int i=0;i<all_decoders.length;i++){
  			
  			if ( all_decoders[i].getName().equals( canonical_name )){
  				
  				return( all_decoders[i] );
  			}
  		}
  	}
  	
  	return( null );
  }
  	
	public LocaleUtilDecoder
	getTorrentEncoding(
  		TOTorrent		torrent )
  		
  		throws TOTorrentException, UnsupportedEncodingException, LocaleUtilEncodingException
  	{
		String	encoding = torrent.getAdditionalStringProperty( "encoding" );
		
		if ( encoding != null ){
			
 			// get canonical name
	  		
			try{
				String canonical_name = encoding.equals(fallback_decoder.getName())?
												encoding:
												Charset.forName(encoding).name();
	
				for (int i=0;i<all_decoders.length;i++){
					
					if ( all_decoders[i].getName().equals( canonical_name )){
						
						return( all_decoders[i] );
					}
				}				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
				
			// get the decoders valid for various localisable parts of torrent content
			// not in any particular order
		
		LocaleUtilDecoder[]	valid_decoders = getTorrentCandidateDecoders( torrent );
		
			// now pick on the torrent name, get valid decoders for that, and then trim down
			// to overall valid ones
		
	    LocaleUtilDecoderCandidate[] candidates = getCandidates(torrent.getName());

	    boolean	system_decoder_is_valid = false;
	    
	    for (int i=0;i<candidates.length;i++){
	    	
	    	LocaleUtilDecoderCandidate	candidate  = candidates[i];
	    	
	    	if ( candidate.getDecoder() != null ){
	    		
	    		boolean	ok = false;
	    		
	    		for (int j=0;j<valid_decoders.length;j++){
	    		
	    			if ( candidate.getDecoder() == valid_decoders[j]){
	    				
	    				if ( candidate.getDecoder() == system_decoder ){
	    					
	    					system_decoder_is_valid	= true;
	    				}
	    				
	    				ok	= true;
	    				
	    				break;
	    			}
	    		}
	    		
	    		if ( !ok ){
	    			
	    			candidate.setDetails( null, null );
	    		}
	    	}
	    }
	    	  
	    LocaleUtilDecoder	selected_decoder = null;
	    
	    for (int i=0;i<listeners.size();i++){
	    	
	    	LocaleUtilDecoderCandidate candidate = ((LocaleUtilListener)listeners.get(i)).selectDecoder( this, torrent, candidates );
	    	
	    	if ( candidate != null ){
	    	    	
	    		selected_decoder = candidate.getDecoder();
	    		
	    		break;
	    	}
	    }
	    	    
	    if ( selected_decoder == null ){
	    	
	    		// go for system decoder, if valid, fallback if not
		
	    	if ( system_decoder_is_valid ){
	    		
	    		selected_decoder	= system_decoder;
	    		
	    	}else{
	    		
	    		selected_decoder	= fallback_decoder;
	    	}
	    } 
		        	
		torrent.setAdditionalStringProperty("encoding", selected_decoder.getName());
            
		TorrentUtils.writeToFile( torrent );
			
		return( selected_decoder );
  	}
	
	
	protected LocaleUtilDecoder[]
	getTorrentCandidateDecoders(
  		TOTorrent		torrent )
  		
  		throws TOTorrentException, UnsupportedEncodingException
  	{
		Set	cand_set = new HashSet();
				   
		cand_set.addAll(Arrays.asList(getCandidateDecoders(torrent.getName())));
			
		TOTorrentFile[]	files = torrent.getFiles();
		
		for (int i=0;i<files.length;i++){
			
			TOTorrentFile	file = files[i];
			
			byte[][] comps = file.getPathComponents();
			
			for (int j=0;j<comps.length;j++){
				
				cand_set.retainAll( Arrays.asList( getCandidateDecoders( comps[j] )));
			}
		}
		
		byte[]	comment = torrent.getComment();
			
		if ( comment != null ){
				
			cand_set.retainAll( Arrays.asList( getCandidateDecoders( comment )));
		}
		
		byte[]	created = torrent.getCreatedBy();
			
		if ( created != null ){
				
			cand_set.retainAll( Arrays.asList( getCandidateDecoders( created )));
		}
		        	
		LocaleUtilDecoder[]	res = new LocaleUtilDecoder[cand_set.size()];
		
		cand_set.toArray( res );
		
		Arrays.sort(res,
				new Comparator()
				{
			   		public int
					compare(
						Object o1, 
						Object o2 )
			   		{
			   			LocaleUtilDecoder	lu1 = (LocaleUtilDecoder)o1;
			   			LocaleUtilDecoder	lu2 = (LocaleUtilDecoder)o2;
			   			
			   			return( lu1.getIndex() - lu2.getIndex());
			   		}
				});
		
		return( res );
  	}
	
	public void
	setTorrentEncoding(
		TOTorrent		torrent,
		String			encoding )
	
		throws LocaleUtilEncodingException	
	{
		try{
			LocaleUtilDecoder[]	decoders = getTorrentCandidateDecoders( torrent );
			
			String	canonical_requested_name;
			
				// "System" means use the system encoding
			
			if ( encoding.equalsIgnoreCase("system" )){
				
				canonical_requested_name	= getSystemEncoding();
				
			}else if ( encoding.equalsIgnoreCase( LocaleUtilDecoderFallback.NAME )){
				
				canonical_requested_name	= LocaleUtilDecoderFallback.NAME;
				
			}else{
				
				CharsetDecoder requested_decoder = Charset.forName(encoding).newDecoder();
			
				canonical_requested_name	= requested_decoder.charset().name();
			}
			
			boolean	 ok = false;
			
			for (int i=0;i<decoders.length;i++){
				
				if ( decoders[i].getName().equals( canonical_requested_name )){
					
					ok	= true;
					
					break;
				}
			}
			
			if ( !ok ){
				
				String[]	charsets 	= new String[decoders.length];
				String[]	names		= new String[decoders.length];
			
				for (int i=0;i<decoders.length;i++){
					
					LocaleUtilDecoder	decoder = decoders[i];
					
					charsets[i] = decoder.getName();
					names[i]	= decoder.decodeString( torrent.getName());
				}
				
				throw( new LocaleUtilEncodingException(charsets, names));
			}
			
			torrent.setAdditionalStringProperty("encoding", encoding );
			
		}catch( Throwable e ){
			
			if ( e instanceof LocaleUtilEncodingException ){
				
				throw((LocaleUtilEncodingException)e);
			}
			
			throw( new LocaleUtilEncodingException(e));
		}
	}
	
	public void
	setDefaultTorrentEncoding(
		TOTorrent		torrent )
	
		throws LocaleUtilEncodingException
	{
		setTorrentEncoding( torrent, Constants.DEFAULT_ENCODING );
	}
}