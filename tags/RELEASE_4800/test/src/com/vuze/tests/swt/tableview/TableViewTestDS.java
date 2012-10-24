package com.vuze.tests.swt.tableview;

import java.util.HashMap;
import java.util.Map;

public class TableViewTestDS
{
	Map<String, Object> map = new HashMap<String, Object>();
	
	public TableViewTestDS() {
		map.put("text", getRandomWord());
	}


	private static String getRandomWord() {
		int len = (int) (Math.random() * 10) + 2;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i ++) {
			double f = Math.random();
			char c;
			if (f < 0.1) {
				c = (char) ((Math.random() * 12) + '0');
			} else if (f < 0.2) {
				c = (char) ((Math.random() * 26) + 'A');
			} else if (f < 0.98) {
				c = (char) ((Math.random() * 26) + 'a');
			} else {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
