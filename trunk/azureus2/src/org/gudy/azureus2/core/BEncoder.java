/*
 * BEncoder.java
 *
 * Created on June 4, 2003, 10:17 PM
 */

package org.gudy.azureus2.core;

import java.io.*;
import java.util.*;

/**
 * A set of utility methods to encode a Map into a bencoded array of byte.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 *
 * @author  TdC_VgA
 */
public class BEncoder {          
    /** Creates a new instance of BEncoder */
    public BEncoder() {
    }
    
    public static byte[] encode(Map object){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BEncoder.encode(baos, object);
        return baos.toByteArray();
    }    
    
    private static void encode(ByteArrayOutputStream baos, Object object){
        if(object instanceof String){
            String tempString = (String)object;
            
            try{
              baos.write((String.valueOf(tempString.length())).getBytes());
              baos.write(':');
              baos.write(tempString.getBytes());
            }catch(IOException e){
                e.printStackTrace();
            }
            
        }else if(object instanceof Map){
            Map tempMap = (Map)object;
            SortedMap tempTree = null;
            
            //write the d            
            baos.write('d');
            
            //are we sorted?
            if(tempMap instanceof TreeMap){
                tempTree = (TreeMap)tempMap;
            }else{
                //do map sorting here
                tempTree = new TreeMap(tempMap);                
            }            
                        
            //create a list to hold the alpha ordered keys
            ArrayList keyList = new ArrayList();            
            
            //BUILD THE KEY LIST
            //I KNOW THIS IS NASTY, BUT SUN DIDN'T SEE FIT TO RETURN A NULL
            do{
                try{
                    //get the key
                    String tempKey = (String)tempTree.firstKey();          
                    //stuff it into the list
                    keyList.add(tempKey);                    
                    //get the rest of the tree
                    tempTree = tempTree.tailMap(tempKey+"\0");                    
                }catch(NoSuchElementException e){
                    break;
                }
            }while(true);
            
            //encode all of the keys
            for(int i = 0; i<keyList.size(); i++){
                Object key = keyList.get(i);
                //encode the key
                BEncoder.encode(baos, key);
                //encode the value
                BEncoder.encode(baos, tempMap.get(key));
            }           
            
            baos.write('e');
            
            
        }else if(object instanceof List){
            List tempList = (List)object;         
            //write out the l                   
            baos.write('l');                                   
            
            for(int i = 0; i<tempList.size(); i++){
                //encode the first element
                BEncoder.encode(baos, tempList.get(i));                            
            }                        
            baos.write('e');                          
            
        }else if(object instanceof Long){
            Long tempLong = (Long)object;         
            //write out the l       
            try{
                baos.write('i');
                baos.write(tempLong.toString().getBytes());
                baos.write('e');
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(object instanceof Integer){
			Integer tempInteger = (Integer)object;         
			//write out the l       
			try{
        baos.write('i');
        baos.write(tempInteger.toString().getBytes());
        baos.write('e');
			}catch(IOException e){
				e.printStackTrace();
			}
        }else if(object instanceof byte[]){
            byte[] tempByteArray = (byte[])object;
            try{
                baos.write((String.valueOf(tempByteArray.length)).getBytes());
                baos.write(':');
                baos.write(tempByteArray);
            }catch(IOException e){
                e.printStackTrace();
            }
        }      
    }
}
