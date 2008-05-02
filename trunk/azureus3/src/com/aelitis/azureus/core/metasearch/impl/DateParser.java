package com.aelitis.azureus.core.metasearch.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class DateParser {
	
	static boolean DEBUG = false;
	
	TimeZone timeZone;
	DateFormat ddMMMyyyyFormat;
	DateFormat ddMMMyyFormat;
	DateFormat MMddyyyyFormat;
	
	public DateParser() {
		this("GMT");
	}
	
	public DateParser(String timeZone) {
		
		this.timeZone = TimeZone.getTimeZone(timeZone);
		
		ddMMMyyyyFormat = new SimpleDateFormat("dd MMM yyyy");
		ddMMMyyyyFormat.setTimeZone(this.timeZone);
		
		ddMMMyyFormat = new SimpleDateFormat("dd MMM yy");
		ddMMMyyFormat.setTimeZone(this.timeZone);
		
		MMddyyyyFormat = new SimpleDateFormat("MM-dd yyyy");
		MMddyyyyFormat.setTimeZone(this.timeZone);
		
	}
	
	public Date parseDate(String date) {
		Date result = parseDateInternal(date);
		if(DEBUG) {
			System.out.println(date + " > " + result.toString());
		}
		
		return result;
	}
	
	private Date parseDateInternal(String s) {
		if(s == null) {
			return null;
		}
		s = s.toLowerCase().trim();
		
		
		//"Today hh:mm" and "Y-day hh:mm" cases
		if(s.startsWith("today ") || s.startsWith("y-day ")) {
			try {
				Calendar calendar = new GregorianCalendar();
				calendar.setTimeZone(timeZone);
				String time = s.substring(6);
				StringTokenizer st = new StringTokenizer(time,":");
				int hours = Integer.parseInt(st.nextToken());
				int minutes = Integer.parseInt(st.nextToken());
				calendar.set(Calendar.HOUR_OF_DAY, hours);
				calendar.set(Calendar.MINUTE,minutes);
				if(s.startsWith("y-day ")) {
					calendar.add(Calendar.DATE, -1);
				}
				return calendar.getTime();
			} catch(Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		//"07-25 2006", "02-01 02:53" and "03 Mar 2006" cases
		if(s.length() > 3) { 
			String thirdCharacter = s.substring(2,3);
			
			//"07-25 2006" and "02-01 02:53" cases
			if(thirdCharacter.equals("-")) {
				if(s.length() > 9) {
					String ninthCharacter = s.substring(8,9);
					//"02-01 02:53" case
					if(ninthCharacter.equals(":")) {
						try {
							int month = Integer.parseInt(s.substring(0,2));
							int day = Integer.parseInt(s.substring(3,5));
							int hours = Integer.parseInt(s.substring(6,8));
							int minutes = Integer.parseInt(s.substring(9,11));
							Calendar calendar = new GregorianCalendar();
							calendar.setTimeZone(timeZone);
							calendar.set(Calendar.MONTH, month-1);
							calendar.set(Calendar.DAY_OF_MONTH,day);
							calendar.set(Calendar.HOUR_OF_DAY, hours);
							calendar.set(Calendar.MINUTE,minutes);
							return calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} 
					//"07-25 2006"
					else {
						try {
							return MMddyyyyFormat.parse(s);
						} catch (Exception e) {
							e.printStackTrace();
						}	
					}
				} else {
					//"03-25" case
					try {
						int month = Integer.parseInt(s.substring(0,2));
						int day = Integer.parseInt(s.substring(3,5));
						Calendar calendar = new GregorianCalendar();
						calendar.setTimeZone(timeZone);
						calendar.set(Calendar.MONTH, month);
						calendar.set(Calendar.DAY_OF_MONTH,day);
						return calendar.getTime();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			} else if(s.length() == 9 && s.indexOf(" ") != -1){
				//"21 Mar 08" case
				try {
					return ddMMMyyFormat.parse(s);
				} catch (Exception e) {
					if(DEBUG) {e.printStackTrace();}
				}	
			}
			
			//"03 Mar 2006" case
			if(thirdCharacter.equals(" ")) {
				try {
					return ddMMMyyyyFormat.parse(s);
				} catch (Exception e) {
					if(DEBUG) {e.printStackTrace();}
				}
			}
		}
		
		//Age based stuff
		if(		s.endsWith(" ago") ||
				s.indexOf("month")!= -1 || 
				s.indexOf("hour") != -1 || 
				s.indexOf("day") != -1 || 
				s.indexOf("year") != -1) {
			
			s= s.replaceAll(" ago", "");
			StringTokenizer st = new StringTokenizer(s," ");
			if(st.countTokens() >= 2) {
				try {
					Calendar calendar = new GregorianCalendar();
					while(st.hasMoreTokens()) {
						float value = Float.parseFloat(st.nextToken());
						String unit = st.nextToken();
						
						calendar.setTimeZone(timeZone);
						if(unit.startsWith("min")) {
							calendar.add(Calendar.MINUTE, -(int)value);
						}
						if(unit.startsWith("hour")) {
							calendar.add(Calendar.HOUR_OF_DAY, -(int)value);
						}
						if(unit.startsWith("day")) {
							calendar.add(Calendar.DATE, -(int)value);
						}
						if(unit.startsWith("month")) {
							calendar.add(Calendar.MONTH, -(int)value);
						}
						if(unit.startsWith("year")) {
							calendar.add(Calendar.YEAR, -(int)value);
						}
					}
					return calendar.getTime();
				} catch (Exception e) {
					if(DEBUG) {e.printStackTrace();}
				}
			}
		}
		
		if(s.equals("today")) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTimeZone(timeZone);
			//calendar.set(Calendar.HOUR_OF_DAY,12);
			//calendar.set(Calendar.MINUTE,0);
			return calendar.getTime();
		}
		
		if(s.equals("yesterday")) {
			Calendar calendar = new GregorianCalendar();
			calendar.setTimeZone(timeZone);
			calendar.add(Calendar.DATE, -1);
			//calendar.set(Calendar.HOUR_OF_DAY,12);
			//calendar.set(Calendar.MINUTE,0);
			return calendar.getTime();
		}
		
		try {
			StringTokenizer st = new StringTokenizer(s," ");
			Calendar calendar = new GregorianCalendar();
			calendar.setTimeZone(timeZone);
			while(st.hasMoreTokens()) {
					String element = st.nextToken();
					int field = -1;
					int end_offset = -1;
					if(element.endsWith("h")) {
						field = Calendar.HOUR_OF_DAY;
						end_offset = 1;
					}
					if(element.endsWith("d")) {
						field = Calendar.DAY_OF_MONTH;
						end_offset = 1;
					}
					if(element.endsWith("w")) {
						field = Calendar.WEEK_OF_YEAR;
						end_offset = 1;
					}
					if(element.endsWith("m")) {
						field = Calendar.MONTH;
						end_offset = 1;
					}
					if(element.endsWith("mon")) {
						field = Calendar.MONTH;
						end_offset = 3;
					}
					if(element.endsWith("y")) {
						field = Calendar.YEAR;
						end_offset = 1;
					}
					if(field != -1 && end_offset != -1) {
						int value = (int) (Float.parseFloat(element.substring(0,element.length() - end_offset)));
						calendar.add(field,-value);
					}
			}
			
			return calendar.getTime();
			
		} catch (Exception e) {
			if(DEBUG) {e.printStackTrace();}
		}
		
		return null;
	}
	
	
	public static void main(String args[]) {
		DEBUG = true;
		DateParser dateParser = new DateParser();
		
		dateParser.parseDate("Today 05:34");
		dateParser.parseDate("Y-Day 21:55");
		dateParser.parseDate("07-25 2006");
		dateParser.parseDate("02-01 02:53");
		dateParser.parseDate("03 Mar 2006");
		dateParser.parseDate("0 minute ago");
		dateParser.parseDate("3 hours ago");
		dateParser.parseDate("2 days ago");
		dateParser.parseDate("10 months ago");
		dateParser.parseDate("45 mins ago");
		dateParser.parseDate("Today");
		dateParser.parseDate("Yesterday");
		dateParser.parseDate("16.9w");
		dateParser.parseDate("22.6h");
		dateParser.parseDate("1.7d");
		dateParser.parseDate("2d 7h");
		dateParser.parseDate("1w");
		dateParser.parseDate("1w 4d");
		dateParser.parseDate("1mon 1w");
		
	}
	

}
