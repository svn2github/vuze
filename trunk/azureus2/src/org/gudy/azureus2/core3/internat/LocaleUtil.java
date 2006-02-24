/**
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

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
  
  /**
   * Determine which locales are candidates for handling the supplied type of 
   * string
   * 
   * @param array String in an byte array 
   * @return list of candidates.  Valid candidates have getDecoder() non-null
   */
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
		
		LocaleUtilDecoderCandidate	cand = candidates[i];
		
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
    
  /**
   * Determine which decoders are candidates for handling the supplied type of
   * string
   * 
   * @param array String in a byte array
   * @return list of possibly valid decoders.  LocaleUtilDecoder
   */
  protected List
  getCandidateDecoders(
  	byte[]		array )
  {
  	LocaleUtilDecoderCandidate[] 	candidates = getCandidates( array );
  	
  	List	decoders = new ArrayList();
  	
  	for (int i=0;i<candidates.length;i++){
  	
  		LocaleUtilDecoder	d = candidates[i].getDecoder();
  		
  		if (d != null)
  			decoders.add(d);
  	}
  	
  	return decoders;
  }
  
  /**
   * 
   * @param array
   * @return List of LocaleUtilDecoderCandidate
   */
  protected List getCandidatesAsList(byte[] array) {
		LocaleUtilDecoderCandidate[] candidates = getCandidates(array);

		List candidatesList = new ArrayList();

		for (int i = 0; i < candidates.length; i++) {
			if (candidates[i].getDecoder() != null)
				candidatesList.add(candidates[i]);
		}

		return candidatesList;
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
  		
		String canonical_name;
		
		try{
			canonical_name = Charset.forName(encoding).name();
			
		}catch( Throwable e ){
			
			canonical_name	= encoding;
		}

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
  		
  		throws TOTorrentException, UnsupportedEncodingException
  	{
		String	encoding = torrent.getAdditionalStringProperty( "encoding" );
    
			// we can only persist the torrent if it has a filename defined for it
		
		boolean bSaveToFile;
		
		try{
			TorrentUtils.getTorrentFileName( torrent );
			
			bSaveToFile	= true;
			
		}catch( Throwable e ){
			
			bSaveToFile	= false;
		}
		
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
		
		//LocaleUtilDecoder[]	valid_decoders = getTorrentCandidateDecoders( torrent );
		LocaleUtilDecoderCandidate[] candidates = getTorrentCandidates(torrent);
		
	    boolean	system_decoder_is_valid = false;

    for (int i=0;i<candidates.length;i++){
    	if (candidates[i].getDecoder() == system_decoder) {
    		system_decoder_is_valid = true;
    		break;
    	}
    }

	    	  
	    LocaleUtilDecoder	selected_decoder = null;
	    
	    for (int i=0;i<listeners.size();i++){
	    	
	    	LocaleUtilDecoderCandidate candidate = null;
				try {
					candidate = ((LocaleUtilListener)listeners.get(i)).selectDecoder( this, torrent, candidates );
				} catch (LocaleUtilEncodingException e) {
				}
	    	
	    	if ( candidate != null ){
	    	    	
	    		selected_decoder = candidate.getDecoder();
	    		
	    		break;
	    	} else {
	    		
	    		bSaveToFile = false;
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

		if (bSaveToFile){
			TorrentUtils.writeToFile( torrent );
		}
		
		return( selected_decoder );
  	}
	
	
	/**
	 * Checks the Torrent's text fields (path, comment, etc) against a list
	 * of locals, returning only those that can handle all the fields.
	 * 
	 * @param torrent
	 * @return
	 * @throws TOTorrentException
	 * @throws UnsupportedEncodingException
	 */
	protected LocaleUtilDecoderCandidate[]
	getTorrentCandidates(
  		TOTorrent		torrent )
  		
  		throws TOTorrentException, UnsupportedEncodingException
	{
		long lMinCandidates;
		byte[] minCandidatesArray;

		Set	cand_set = new HashSet();

		List candidateDecoders = getCandidateDecoders(torrent.getName());
		lMinCandidates = candidateDecoders.size();
		minCandidatesArray = torrent.getName();
		
		cand_set.addAll(candidateDecoders);
			

		TOTorrentFile[]	files = torrent.getFiles();
		
		for (int i=0;i<files.length;i++){
			
			TOTorrentFile	file = files[i];
			
			byte[][] comps = file.getPathComponents();
			
			for (int j=0;j<comps.length;j++){
				candidateDecoders = getCandidateDecoders(comps[j]);
				if (candidateDecoders.size() < lMinCandidates) {
					lMinCandidates = candidateDecoders.size();
					minCandidatesArray = comps[j];
				}
				cand_set.retainAll(candidateDecoders);
			}
		}
		
		byte[]	comment = torrent.getComment();
			
		if ( comment != null ){
			candidateDecoders = getCandidateDecoders(comment);
			if (candidateDecoders.size() < lMinCandidates) {
				lMinCandidates = candidateDecoders.size();
				minCandidatesArray = comment;
			}
			cand_set.retainAll(candidateDecoders);
		}
		
		byte[]	created = torrent.getCreatedBy();
			
		if ( created != null ){
			candidateDecoders = getCandidateDecoders(created);
			if (candidateDecoders.size() < lMinCandidates) {
				lMinCandidates = candidateDecoders.size();
				minCandidatesArray = created;
			}
			cand_set.retainAll(candidateDecoders);
		}
		
		List candidatesList = getCandidatesAsList(minCandidatesArray);
		LocaleUtilDecoderCandidate[] candidates;
		candidates = new LocaleUtilDecoderCandidate[candidatesList.size()];
		candidatesList.toArray(candidates);
		
		Arrays.sort(candidates, new Comparator() {
			public int compare(Object o1, Object o2) {
				LocaleUtilDecoderCandidate luc1 = (LocaleUtilDecoderCandidate) o1;
				LocaleUtilDecoderCandidate luc2 = (LocaleUtilDecoderCandidate) o2;

				return (luc1.getDecoder().getIndex() - luc2.getDecoder().getIndex());
			}
		});
		
		return candidates;
  	}
	
	public void
	setTorrentEncoding(
		TOTorrent		torrent,
		String			encoding )
	
		throws LocaleUtilEncodingException	
	{
		try{
			LocaleUtilDecoderCandidate[]	candidates = getTorrentCandidates(torrent);
			
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
			
			for (int i=0;i<candidates.length;i++){
				
				if ( candidates[i].getDecoder().getName().equals( canonical_requested_name )){
					
					ok	= true;
					
					break;
				}
			}
			
			if ( !ok ){
				
				String[]	charsets 	= new String[candidates.length];
				String[]	names		= new String[candidates.length];
			
				for (int i=0;i<candidates.length;i++){
					
					LocaleUtilDecoder	decoder = candidates[i].getDecoder();
					
					charsets[i] = decoder.getName();
					names[i]	= decoder.decodeString( torrent.getName());
				}
				
				throw( new LocaleUtilEncodingException(charsets, names));
			}
			
			torrent.setAdditionalStringProperty("encoding", canonical_requested_name );
			
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
	
	public String
	getCurrentTorrentEncoding(
		TOTorrent	torrent )
	{
		return( torrent.getAdditionalStringProperty("encoding"));
	}
}