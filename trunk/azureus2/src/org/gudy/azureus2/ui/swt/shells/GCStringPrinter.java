/*
 * File    : GCStringPrinter.java
 * Created : 16 mars 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.swt.shells;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author Olivier Chalouhi
 * @author TuxPaper (rewrite)
 */
public class GCStringPrinter
{
	private static final boolean DEBUG = false;

	private static final int FLAG_SKIPCLIP = 1;

	private static final int FLAG_FULLLINESONLY = 2;

	private static final int FLAG_NODRAW = 4;

	private static final int FLAG_KEEP_URL_INFO = 8;

	private static final Pattern patHREF = Pattern.compile(
			"<\\s*?a\\s.*?href\\s*?=\\s*?\"(.+?)\".*?>(.*?)<\\s*?/a\\s*?>",
			Pattern.CASE_INSENSITIVE);

	// Limit word length as OSX crashes on stringExtent on very very long words
	private static final int MAX_WORD_LEN = 1023;

	private boolean cutoff;

	private GC gc;

	private String string;

	private Rectangle printArea;

	private int swtFlags;

	private int printFlags;

	private Point size;

	private Color urlColor;

	private List listUrlInfo;

	public static class URLInfo
	{
		public String url;

		public String title;

		public Color urlColor;

		int relStartPos;

		// We could use a region, but that uses a resource that requires disposal
		List hitAreas = null;

		int titleLength;

		// @see java.lang.Object#toString()
		public String toString() {
			return super.toString() + ": relStart=" + relStartPos + ";url=" + url
					+ ";title=" + title + ";hit="
					+ (hitAreas == null ? 0 : hitAreas.size());
		}
	}

	public static boolean printString(GC gc, String string, Rectangle printArea) {
		return printString(gc, string, printArea, false, false);
	}

	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly) {
		return printString(gc, string, printArea, skipClip, fullLinesOnly, SWT.WRAP
				| SWT.TOP);
	}

	/**
	 * 
	 * @param gc GC to print on
	 * @param string Text to print
	 * @param printArea Area of GC to print text to
	 * @param skipClip Don't set any clipping on the GC.  Text may overhang 
	 *                 printArea when this is true
	 * @param fullLinesOnly If bottom of a line will be chopped off, do not display it
	 * @param swtFlags SWT flags.  SWT.CENTER, SWT.BOTTOM, SWT.TOP, SWT.WRAP
	 * @return whether it fit
	 */
	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly, int swtFlags) {
		try {
			GCStringPrinter sp = new GCStringPrinter(gc, string, printArea, skipClip,
					fullLinesOnly, swtFlags);
			return sp.printString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * @param gc
	 * @param string
	 * @param printArea
	 * @param printFlags
	 * @param swtFlags
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	private boolean _printString() {
		size = new Point(0, 0);

		if (string == null) {
			return false;
		}

		if (printArea == null || printArea.isEmpty()) {
			return false;
		}

		ArrayList lines = new ArrayList();

		if (string.indexOf('\r') > 0) {
			string = string.replace('\r', ' ');
		}
		//We need to add some cariage return ...
		// replaceall is slow
		if (string.indexOf('\t') > 0) {
			string = string.replace('\t', ' ');
		}

		string = string.replaceAll("\n\n", "\n \n").replaceAll("  +", " ");

		boolean fullLinesOnly = (printFlags & FLAG_FULLLINESONLY) > 0;
		boolean skipClip = (printFlags & FLAG_SKIPCLIP) > 0;
		boolean noDraw = (printFlags & FLAG_NODRAW) > 0;
		boolean wrap = (swtFlags & SWT.WRAP) > 0;

		if ((printFlags & FLAG_KEEP_URL_INFO) == 0) {
			Matcher htmlMatcher = patHREF.matcher(string);
			boolean hasURL = htmlMatcher.find();
			if (hasURL) {
				listUrlInfo = new ArrayList(1);

				while (hasURL) {
					URLInfo urlInfo = new URLInfo();
					urlInfo.url = htmlMatcher.group(1);
					// For now, replace spaces with dashes so url title is always on 1 line
					String s = htmlMatcher.group(2); //.replaceAll(" ", "`");

					urlInfo.title = s;
					urlInfo.relStartPos = htmlMatcher.start(0);
					urlInfo.titleLength = s.length();

					//System.out.println("URLINFO! " + s + ";" + s.length() + ";" + urlInfo.relStartPos);

					string = htmlMatcher.replaceFirst(s.replaceAll("\\$", "\\\\\\$"));

					listUrlInfo.add(urlInfo);
					htmlMatcher = patHREF.matcher(string);
					hasURL = htmlMatcher.find(urlInfo.relStartPos);
				}
			}
		} else {
			Matcher htmlMatcher = patHREF.matcher(string);
			string = htmlMatcher.replaceAll("$2");
		}

		Rectangle rectDraw = new Rectangle(printArea.x, printArea.y,
				printArea.width, printArea.height);

		Rectangle oldClipping = null;
		try {
			if (!skipClip && !noDraw) {
				oldClipping = gc.getClipping();

				// Protect the GC from drawing outside the drawing area
				gc.setClipping(printArea);
			}

			// Process string line by line
			int iCurrentHeight = 0;
			int currentCharPos = 0;
			// stringtokenizer is faster than split
			StringTokenizer stLine = new StringTokenizer(string, "\n");
			while (stLine.hasMoreElements()) {
				String sLine = stLine.nextToken();

				do {
					LineInfo lineInfo = new LineInfo(sLine, currentCharPos);
					lineInfo = processLine(gc, lineInfo, printArea, wrap, fullLinesOnly,
							stLine.hasMoreElements());
					String sProcessedLine = (String) lineInfo.lineOutputed;

					if (sProcessedLine != null && sProcessedLine.length() > 0) {
						Point extent = gc.stringExtent(sProcessedLine);
						iCurrentHeight += extent.y;
						boolean isOverY = iCurrentHeight > printArea.height;

						if (DEBUG) {
							System.out.println("Adding Line: [" + sProcessedLine + "]"
									+ sProcessedLine.length() + "; h=" + iCurrentHeight + "("
									+ printArea.height + "). fullOnly?" + fullLinesOnly
									+ ". Excess: " + lineInfo.excessPos);
						}

						if (isOverY && !fullLinesOnly) {
							//fullLinesOnly = true; // <-- don't know why we needed this
							lines.add(lineInfo);
						} else if (isOverY && fullLinesOnly) {
							String excess = lineInfo.excessPos >= 0
									? sLine.substring(lineInfo.excessPos) : null;
							if (excess != null) {
								if (fullLinesOnly) {
									if (lines.size() > 0) {
										lineInfo = (LineInfo) lines.remove(lines.size() - 1);
										sProcessedLine = lineInfo.originalLine;
										//sProcessedLine = ((LineInfo) lines.remove(lines.size() - 1)).originalLine;
										extent = gc.stringExtent(sProcessedLine);
									} else {
										if (DEBUG) {
											System.out.println("No PREV!?");
										}
										return false;
									}
								}

								StringBuffer outputLine = new StringBuffer(sProcessedLine);
								int[] iLineLength = {
									extent.x
								};
								int newExcessPos = processWord(gc, sProcessedLine,
										" " + excess, printArea, false, iLineLength, outputLine,
										new StringBuffer());
								if (DEBUG) {
									System.out.println("  with word [" + excess + "] len is "
											+ iLineLength[0] + "(" + printArea.width + ") w/excess "
											+ newExcessPos);
								}

								lineInfo.lineOutputed = outputLine.toString();
								lines.add(lineInfo);
								if (DEBUG) {
									System.out.println("replace prev line with: "
											+ outputLine.toString());
								}
							} else {
								if (DEBUG) {
									System.out.println("No Excess");
								}
							}
							cutoff = true;
							return false;
						} else {
							lines.add(lineInfo);
						}
						sLine = lineInfo.excessPos >= 0 && wrap
								? sLine.substring(lineInfo.excessPos) : null;
					} else {
						if (DEBUG) {
							System.out.println("Line process resulted in no text: " + sLine);
						}
						return false;
					}

					currentCharPos += lineInfo.excessPos >= 0 ? lineInfo.excessPos
							: lineInfo.lineOutputed.length();
					//System.out.println("output: " + lineInfo.lineOutputed.length() + ";" 
					//		+ lineInfo.lineOutputed + ";xc=" + lineInfo.excessPos + ";ccp=" + currentCharPos);
					//System.out.println("lineo=" + lineInfo.lineOutputed.length() + ";" + sLine.length() );
				} while (sLine != null);
				currentCharPos += 1;
			}
		} finally {
			if (!skipClip && !noDraw) {
				gc.setClipping(oldClipping);
			}

			if (lines.size() > 0) {
				// rebuild full text to get the exact y-extent of the output
				// this may be different (but shouldn't be!) than the height of each
				// line
				StringBuffer fullText = new StringBuffer(string.length() + 10);
				for (Iterator iter = lines.iterator(); iter.hasNext();) {
					LineInfo lineInfo = (LineInfo) iter.next();
					if (fullText.length() > 0) {
						fullText.append('\n');
					}
					fullText.append(lineInfo.lineOutputed);
				}

				size = gc.textExtent(fullText.toString());

				if ((swtFlags & (SWT.BOTTOM)) != 0) {
					rectDraw.y = rectDraw.y + rectDraw.height - size.y;
				} else if ((swtFlags & SWT.TOP) == 0) {
					// center vert
					rectDraw.y = rectDraw.y + (rectDraw.height - size.y) / 2;
				}

				if (!noDraw || listUrlInfo != null) {
					for (Iterator iter = lines.iterator(); iter.hasNext();) {
						LineInfo lineInfo = (LineInfo) iter.next();
						try {
							drawLine(gc, lineInfo, swtFlags, rectDraw, noDraw);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
			}
		}

		return size.y <= printArea.height;
	}

	/**
	 * @param hasMoreElements 
	 * @param line
	 *
	 * @since 3.0.0.7
	 */
	private LineInfo processLine(final GC gc, final LineInfo lineInfo,
			final Rectangle printArea, final boolean wrap,
			final boolean fullLinesOnly, boolean hasMoreElements) {
		StringBuffer outputLine = new StringBuffer();
		int excessPos = -1;

		if (gc.stringExtent(lineInfo.originalLine).x > printArea.width) {
			if (DEBUG) {
				System.out.println("Line to process: " + lineInfo.originalLine);
			}
			StringBuffer space = new StringBuffer(1);
			int[] iLineLength = {
				0
			};

			if (!wrap) {
				if (DEBUG) {
					System.out.println("No Wrap.. doing all in one line");
				}

				excessPos = processWord(gc, lineInfo.originalLine,
						lineInfo.originalLine, printArea, wrap, iLineLength, outputLine,
						space);
			} else {
				StringTokenizer stWord = new StringTokenizer(lineInfo.originalLine, " ");
				// Process line word by word
				int curPos = 0;
				while (stWord.hasMoreElements()) {
					String word = stWord.nextToken();
					excessPos = processWord(gc, lineInfo.originalLine, word, printArea,
							wrap, iLineLength, outputLine, space);
					if (DEBUG) {
						System.out.println("  with word [" + word + "] len is "
								+ iLineLength[0] + "(" + printArea.width + ") w/excess "
								+ excessPos);
					}
					if (excessPos >= 0) {
						excessPos += curPos;
						break;
					}
					curPos += word.length() + 1;
				}
			}
		} else {
			outputLine.append(lineInfo.originalLine);
		}

		if (!wrap && hasMoreElements && excessPos >= 0) {
			outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
			cutoff = true;
		}
		//drawLine(gc, outputLine, swtFlags, rectDraw);
		//		if (!wrap) {
		//			return hasMoreElements;
		//		}
		lineInfo.excessPos = excessPos;
		lineInfo.lineOutputed = outputLine.toString();
		return lineInfo;
	}

	private class LineInfo
	{
		String originalLine;

		String lineOutputed;

		int excessPos;

		public int relStartPos;

		public LineInfo(String originalLine, int relStartPos) {
			this.originalLine = originalLine;
			this.relStartPos = relStartPos;
		}

		// @see java.lang.Object#toString()
		public String toString() {
			return super.toString() + ": relStart=" + relStartPos + ";xcess="
					+ excessPos + ";orig=" + originalLine + ";output=" + lineOutputed;
		}
	}

	/**
	 * @param int Position of part of word that didn't fit
	 *
	 * @since 3.0.0.7
	 */
	private int processWord(final GC gc, final String sLine, String word,
			final Rectangle printArea, final boolean wrap, final int[] iLineLength,
			StringBuffer outputLine, final StringBuffer space) {

		boolean bWordChopped = word.length() > MAX_WORD_LEN;
		if (word.length() > MAX_WORD_LEN) {
			word = word.substring(0, MAX_WORD_LEN);
		}
		
		Point ptWordSize = gc.stringExtent(word + " ");
		boolean bWordLargerThanWidth = ptWordSize.x > printArea.width || bWordChopped;
		int targetWidth = iLineLength[0] + ptWordSize.x; 
		if (targetWidth > printArea.width || bWordChopped) {
			//if (ptWordSize.x > printArea.width && word.length() > 1) {
			// word is longer than space avail, split
			int endIndex = word.length();
			do {
				if (targetWidth / 4 > printArea.width) {
					endIndex /= 2;
				} else {
					endIndex--;
				}
				ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
				targetWidth = iLineLength[0] + ptWordSize.x;
			} while (endIndex > 0 && targetWidth > printArea.width);

			if (DEBUG) {
				System.out.println("excess starts at " + endIndex + "(" + ptWordSize.x
						+ "px) of " + word.length() + ". "
						+ (ptWordSize.x + iLineLength[0]) + "/" + printArea.width
						+ "; wrap?" + wrap);
			}

			if (endIndex > 0 && outputLine.length() > 0) {
				outputLine.append(space);
			}

			if (endIndex == 0 && outputLine.length() == 0) {
				endIndex = 1;
			}

			if (wrap && ptWordSize.x < printArea.width && !bWordLargerThanWidth) {
				// whole word is excess
				return 0;
			}

			outputLine.append(word.substring(0, endIndex));
			if (!wrap) {
				int len = outputLine.length();
				if (len == 0) {
					if (word.length() > 0) {
						outputLine.append(word.charAt(0));
					} else if (sLine.length() > 0) {
						outputLine.append(sLine.charAt(0));
					}
				} else if (len > 1) {
					outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
					cutoff = true;
				}
			}
			//drawLine(gc, outputLine, swtFlags, rectDraw);
			if (DEBUG) {
				System.out.println("excess " + word.substring(endIndex));
			}
			return endIndex;
		}

		iLineLength[0] += ptWordSize.x;
		if (iLineLength[0] > printArea.width) {
			if (space.length() > 0) {
				space.delete(0, space.length());
			}

			if (!wrap) {
				int len = outputLine.length();
				if (len == 0) {
					if (word.length() > 0) {
						outputLine.append(word.charAt(0));
					} else if (sLine.length() > 0) {
						outputLine.append(sLine.charAt(0));
					}
				} else if (len > 1) {
					outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
					cutoff = true;
				}
				return -1;
			} else {
				return 0;
			}
			//drawLine(gc, outputLine, swtFlags, rectDraw);
		}

		if (outputLine.length() > 0) {
			outputLine.append(space);
		}
		outputLine.append(word);
		if (space.length() > 0) {
			space.delete(0, space.length());
		}
		space.append(' ');

		return -1;
	}

	/**
	 * printArea is updated to the position of the next row
	 * 
	 * @param gc
	 * @param outputLine
	 * @param swtFlags
	 * @param printArea
	 * @param noDraw 
	 */
	private void drawLine(GC gc, LineInfo lineInfo, int swtFlags,
			Rectangle printArea, boolean noDraw) {
		String text = lineInfo.lineOutputed;
		Point drawSize = gc.textExtent(text);
		int x0;
		if ((swtFlags & SWT.RIGHT) > 0) {
			x0 = printArea.x + printArea.width - drawSize.x;
		} else if ((swtFlags & SWT.CENTER) > 0) {
			x0 = printArea.x + (printArea.width - drawSize.x) / 2;
		} else {
			x0 = printArea.x;
		}

		int y0 = printArea.y;

		int lineInfoRelEndPos = lineInfo.relStartPos
				+ lineInfo.lineOutputed.length();
		int relStartPos = lineInfo.relStartPos;
		int lineStartPos = 0;

		URLInfo urlInfo = null;
		boolean drawURL = hasHitUrl();
		
		if (drawURL) {
  		URLInfo[] hitUrlInfo = getHitUrlInfo();
  		int nextHitUrlInfoPos = 0;
  
  		while (drawURL) {
  			drawURL = false;
  			for (int i = nextHitUrlInfoPos; i < hitUrlInfo.length; i++) {
					urlInfo = hitUrlInfo[i];
					
  				drawURL = (urlInfo.relStartPos < lineInfoRelEndPos)
  						&& (urlInfo.relStartPos + urlInfo.titleLength > relStartPos)
  						&& (relStartPos >= lineInfo.relStartPos)
  						&& (relStartPos < lineInfoRelEndPos);
  				if (drawURL) {
  					nextHitUrlInfoPos = i + 1;
  					break;
  				}
  			}
  
  			if (!drawURL) {
  				break;
  			}
  
  			//int numHitUrlsAlready = urlInfo.hitAreas == null ? 0 : urlInfo.hitAreas.size();
  
  			// draw text before url
  			int i = lineStartPos + urlInfo.relStartPos - relStartPos;
  			//System.out.println("numHitUrlsAlready = " + numHitUrlsAlready + ";i=" + i);
  			if (i > 0 && i > lineStartPos && i <= text.length()) {
  				String s = text.substring(lineStartPos, i);
  				if (!noDraw) {
  					//gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
  					gc.drawText(s, x0, y0, false);
  				}
  
  				Point textExtent = gc.textExtent(s, SWT.DRAW_TRANSPARENT
  						| SWT.DRAW_DELIMITER | SWT.DRAW_TAB);
  				x0 += textExtent.x;
  				relStartPos += (i - lineStartPos);
  				lineStartPos += (i - lineStartPos);
  				//System.out.println("|" + s + "|" + textExtent.x);
  			}
  
  			// draw url text
  			int end = i + urlInfo.titleLength;
  			if (i < 0) {
  				i = 0;
  			}
  			//System.out.println("end=" + end + ";" + text.length() + ";titlelen=" + urlInfo.titleLength);
  			if (end > text.length()) {
  				end = text.length();
  			}
  			String s = text.substring(i, end);
  			relStartPos += (end - i);
  			lineStartPos += (end - i);
  			//System.out.println("|" + s + "|");
  			if (!noDraw) {
  				Color fgColor = gc.getForeground();
  				if (urlInfo.urlColor != null) {
  					gc.setForeground(urlInfo.urlColor);
  				} else if (urlColor != null) {
  					gc.setForeground(urlColor);
  				}
  				gc.drawText(s, x0, y0, true);
  				gc.setForeground(fgColor);
  			}
  			Point textExtent = gc.textExtent(s);
  
  			if (urlInfo.hitAreas == null) {
  				urlInfo.hitAreas = new ArrayList(1);
  			}
  			urlInfo.hitAreas.add(new Rectangle(x0, y0, textExtent.x, textExtent.y));
  
  			x0 += textExtent.x;
  		}
		}

		// draw text after url
		if (lineStartPos < text.length()) {
			String s = text.substring(lineStartPos);
			if (!noDraw) {
				gc.drawText(s, x0, y0, true);
			}
		}
		printArea.y += drawSize.y;
	}

	public static void main(String[] args) {

		//String s = "this is $1.00";
		//String s2 = "$1";
		//String s3 = s2.replaceAll("\\$", "\\\\\\$");
		//System.out.println(s3);
		//s.replaceAll("h", s3);
		//System.out.println(s);
		//if (true) {
		//	return;
		//}

		final Display display = Display.getDefault();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);

		//final String text = "Opil Wrir, Na Poys Iysk, Yann Only. test of the string printer averlongwordthisisyesindeed";
		final String text = "Apple <A HREF=\"aa\">Banana</a>, Cow <A HREF=\"ss\">Dug Ergo</a>, Flip Only. test of the string printer averlongwordthisisyesindeed";

		shell.setSize(500, 500);

		GridLayout gridLayout = new GridLayout(2, false);
		shell.setLayout(gridLayout);

		Composite cButtons = new Composite(shell, SWT.NONE);
		GridData gridData = new GridData(SWT.NONE, SWT.FILL, false, true);
		cButtons.setLayoutData(gridData);
		final Composite cPaint = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		cPaint.setLayoutData(gridData);

		cButtons.setLayout(new RowLayout(SWT.VERTICAL));

		Listener l = new Listener() {
			public void handleEvent(Event event) {
				cPaint.redraw();
			}
		};

		final Text txtText = new Text(cButtons, SWT.WRAP | SWT.MULTI | SWT.BORDER);
		txtText.setText(text);
		txtText.addListener(SWT.Modify, l);
		txtText.setLayoutData(new RowData(100, 100));

		final Button btnSkipClip = new Button(cButtons, SWT.CHECK);
		btnSkipClip.setText("Skip Clip");
		btnSkipClip.setSelection(true);
		btnSkipClip.addListener(SWT.Selection, l);

		final Button btnFullOnly = new Button(cButtons, SWT.CHECK);
		btnFullOnly.setText("Full Lines Only");
		btnFullOnly.setSelection(true);
		btnFullOnly.addListener(SWT.Selection, l);

		final Combo cboVAlign = new Combo(cButtons, SWT.READ_ONLY);
		cboVAlign.add("Top");
		cboVAlign.add("Bottom");
		cboVAlign.add("None");
		cboVAlign.addListener(SWT.Selection, l);
		cboVAlign.select(0);

		final Combo cboHAlign = new Combo(cButtons, SWT.READ_ONLY);
		cboHAlign.add("Left");
		cboHAlign.add("Center");
		cboHAlign.add("Right");
		cboHAlign.add("None");
		cboHAlign.addListener(SWT.Selection, l);
		cboHAlign.select(0);

		final Button btnWrap = new Button(cButtons, SWT.CHECK);
		btnWrap.setText("Wrap");
		btnWrap.setSelection(true);
		btnWrap.addListener(SWT.Selection, l);

		Listener l2 = new Listener() {
			URLInfo lastHitInfo = null;

			public void handleEvent(Event event) {
				GC gc = event.gc;
				System.out.println("HE" + event.type);
				boolean ourGC = gc == null;
				if (ourGC) {
					gc = new GC(cPaint);
				}
				try {
					GCStringPrinter sp = buildSP(gc);
					Color colorURL = gc.getDevice().getSystemColor(SWT.COLOR_RED);
					Color colorURL2 = gc.getDevice().getSystemColor(
							SWT.COLOR_DARK_MAGENTA);

					if (event.type == SWT.MouseMove) {
						Point pt = cPaint.toControl(display.getCursorLocation());
						URLInfo hitUrl = sp.getHitUrl(pt.x, pt.y);
						String url1 = hitUrl == null || hitUrl.url == null ? ""
								: hitUrl.url;
						String url2 = lastHitInfo == null || lastHitInfo.url == null ? ""
								: lastHitInfo.url;

						if (url1.equals(url2)) {
							return;
						}
						lastHitInfo = hitUrl;
					}

					Rectangle bounds = cPaint.getClientArea();

					Color colorBox = gc.getDevice().getSystemColor(SWT.COLOR_YELLOW);
					Color colorText = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);

					gc.setForeground(colorText);

					Point pt = cPaint.toControl(display.getCursorLocation());
					sp.setUrlColor(colorURL);
					URLInfo hitUrl = sp.getHitUrl(pt.x, pt.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorURL2;
					}
					sp.printString();

					bounds.width--;
					bounds.height--;

					gc.setForeground(colorBox);
					gc.drawRectangle(bounds);

					//System.out.println("-         " + System.currentTimeMillis());

				} finally {
					if (ourGC) {
						gc.dispose();
					}
				}
			}

			private GCStringPrinter buildSP(GC gc) {
				Rectangle bounds = cPaint.getClientArea();

				int style = btnWrap.getSelection() ? SWT.WRAP : 0;
				if (cboVAlign.getSelectionIndex() == 0) {
					style |= SWT.TOP;
				} else if (cboVAlign.getSelectionIndex() == 1) {
					style |= SWT.BOTTOM;
				}

				if (cboHAlign.getSelectionIndex() == 0) {
					style |= SWT.LEFT;
				} else if (cboHAlign.getSelectionIndex() == 1) {
					style |= SWT.CENTER;
				} else if (cboHAlign.getSelectionIndex() == 2) {
					style |= SWT.RIGHT;
				}

				GCStringPrinter sp = new GCStringPrinter(gc, txtText.getText(), bounds,
						btnSkipClip.getSelection(), btnFullOnly.getSelection(), style);
				sp.calculateMetrics();

				return sp;
			}
		};
		cPaint.addListener(SWT.Paint, l2);
		cPaint.addListener(SWT.MouseMove, l2);

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * 
	 */
	public GCStringPrinter(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly, int swtFlags) {
		this.gc = gc;
		this.string = string;
		this.printArea = printArea;
		this.swtFlags = swtFlags;

		printFlags = 0;
		if (skipClip) {
			printFlags |= FLAG_SKIPCLIP;
		}
		if (fullLinesOnly) {
			printFlags |= FLAG_FULLLINESONLY;
		}
	}

	public GCStringPrinter(GC gc, String string, Rectangle printArea,
			int printFlags, int swtFlags) {
		this.gc = gc;
		this.string = string;
		this.printArea = printArea;
		this.swtFlags = swtFlags;
		this.printFlags = printFlags;
	}

	public boolean printString() {
		return _printString();
	}

	public boolean printString(int printFlags) {
		int oldPrintFlags = this.printFlags;
		printFlags |= printFlags;
		boolean b = _printString();
		this.printFlags = oldPrintFlags;
		return b;
	}

	public void calculateMetrics() {
		int oldPrintFlags = printFlags;
		printFlags |= FLAG_NODRAW;
		_printString();
		printFlags = oldPrintFlags;
	}

	/**
	 * @param rectangle
	 *
	 * @since 3.0.4.3
	 */
	public void printString(GC gc, Rectangle rectangle, int swtFlags) {
		this.gc = gc;
		int printFlags = this.printFlags;
		if (printArea.width == rectangle.width) {
			printFlags |= FLAG_KEEP_URL_INFO;
		}
		printArea = rectangle;
		this.swtFlags = swtFlags;
		printString(printFlags);
	}

	public Point getCalculatedSize() {
		return size;
	}

	public Color getUrlColor() {
		return urlColor;
	}

	public void setUrlColor(Color urlColor) {
		this.urlColor = urlColor;
	}

	public URLInfo getHitUrl(int x, int y) {
		if (listUrlInfo == null || listUrlInfo.size() == 0) {
			return null;
		}
		for (Iterator iter = listUrlInfo.iterator(); iter.hasNext();) {
			URLInfo urlInfo = (URLInfo) iter.next();
			if (urlInfo.hitAreas != null) {
				for (Iterator iter2 = urlInfo.hitAreas.iterator(); iter2.hasNext();) {
					Rectangle r = (Rectangle) iter2.next();
					if (r.contains(x, y)) {
						return urlInfo;
					}
				}
			}
		}
		return null;
	}

	public URLInfo[] getHitUrlInfo() {
		if (listUrlInfo == null) {
			return new URLInfo[0];
		}
		return (URLInfo[]) listUrlInfo.toArray(new URLInfo[0]);
	}

	public boolean hasHitUrl() {
		return listUrlInfo != null && listUrlInfo.size() > 0;
	}

	public boolean isCutoff() {
		return cutoff;
	}
}
