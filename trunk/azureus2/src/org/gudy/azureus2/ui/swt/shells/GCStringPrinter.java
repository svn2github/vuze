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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
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

	private static final Pattern patHREF = Pattern.compile(
			"<.*a\\s++.*href=\"(.+?)\">(.+?)</a>", Pattern.CASE_INSENSITIVE);

	private GC gc;

	private final String string;

	private Rectangle printArea;

	private int swtFlags;

	private int printFlags;

	private Point size;

	private Color urlColor;

	private URLInfo urlInfo;

	private static class URLInfo
	{
		String url;

		String title;

		int originalStartPos;

		Rectangle hitArea;
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

	private boolean _printString(GC gc, String string, Rectangle printArea,
			int printFlags, int swtFlags) {
		size = new Point(0, 0);

		if (string == null) {
			return false;
		}

		ArrayList lines = new ArrayList();

		if (printArea.isEmpty()) {
			return false;
		}

		boolean fullLinesOnly = (printFlags & FLAG_FULLLINESONLY) > 0;
		boolean skipClip = (printFlags & FLAG_SKIPCLIP) > 0;
		boolean noDraw = (printFlags & FLAG_NODRAW) > 0;
		boolean wrap = (swtFlags & SWT.WRAP) > 0;
		Matcher htmlMatcher = patHREF.matcher(string);
		boolean hasHTML = htmlMatcher.find();

		urlInfo = null;
		if (hasHTML) {
			urlInfo = new URLInfo();
			urlInfo.url = htmlMatcher.group(1);
			// For now, replace spaces with dashes so url title is always on 1 line
			String s = htmlMatcher.group(2).replaceAll(" ", "`");

			// For now, chop off any title text that is too long for a full row
			// as it will break parsing later
			Point titleExtent = gc.textExtent(s);
			while (s.length() > 0 && titleExtent.x + 3 >= printArea.width) {
				s = s.substring(0, s.length() - 1); 
				titleExtent = gc.textExtent(s);
			}

			urlInfo.title = s;
			urlInfo.originalStartPos = htmlMatcher.start(1);
			
			string = htmlMatcher.replaceFirst(s.replaceAll("\\$", "\\\\\\$"));
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

			//We need to add some cariage return ...
			// replaceall is slow
			String sTabsReplaced = string.indexOf('\t') > 0 ? string.replaceAll("\t",
					"  ") : string;

			// Process string line by line
			int iCurrentHeight = 0;
			int currentCharPos = 0;
			StringTokenizer stLine = new StringTokenizer(sTabsReplaced, "\n");
			while (stLine.hasMoreElements()) {
				String sLine = stLine.nextToken().replaceAll("\r", " ");
				String sLastExcess = null;

				do {
					Object[] lineInfo = processLine(gc, sLine, printArea, wrap,
							fullLinesOnly, stLine.hasMoreElements());
					String sProcessedLine = (String) lineInfo[0];

					if (sProcessedLine != null && sProcessedLine.length() > 0) {
						int excessPos = ((Long) lineInfo[1]).intValue();

						Point extent = gc.stringExtent(sProcessedLine);
						iCurrentHeight += extent.y;
						boolean isOverY = iCurrentHeight > printArea.height;

						if (DEBUG) {
							System.out.println("Adding Line: [" + sProcessedLine + "]"
									+ sProcessedLine.length() + "; h=" + iCurrentHeight + "("
									+ printArea.height + "). fullOnly?" + fullLinesOnly
									+ ". Excess: " + lineInfo[1]);
						}

						if (isOverY && !fullLinesOnly) {
							fullLinesOnly = true;
							lines.add(sProcessedLine);
						} else if (isOverY && fullLinesOnly) {
							String excess;
							if (fullLinesOnly) {
								excess = sLastExcess;
							} else {
								excess = excessPos >= 0 ? sLine.substring(excessPos) : null;
							}
							if (excess != null) {
								if (fullLinesOnly) {
									if (lines.size() > 0) {
										sProcessedLine = (String) lines.remove(lines.size() - 1);
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

								lines.add(outputLine.toString());
								if (DEBUG) {
									System.out.println("replace prev line with: "
											+ outputLine.toString());
								}
							} else {
								if (DEBUG) {
									System.out.println("No Excess");
								}
							}
							return false;
						} else {
							lines.add(sProcessedLine);
						}
						sLine = excessPos >= 0 ? sLine.substring(excessPos) : null;
						sLastExcess = sLine;
					} else {
						if (DEBUG) {
							System.out.println("Line process resulted in no text: " + sLine);
						}
						return false;
					}
				} while (sLine != null);
			}
		} finally {
			if (!skipClip && !noDraw) {
				gc.setClipping(oldClipping);
			}

			if (lines.size() > 0) {
				String fullText = null;
				for (Iterator iter = lines.iterator(); iter.hasNext();) {
					String text = (String) iter.next();
					if (fullText == null) {
						fullText = text;
					} else {
						fullText += "\n" + text;
					}
				}

				size = gc.textExtent(fullText);

				if ((swtFlags & (SWT.BOTTOM)) != 0) {
					rectDraw.y = rectDraw.y + rectDraw.height - size.y;
				} else if ((swtFlags & SWT.TOP) == 0) {
					// center vert
					rectDraw.y = rectDraw.y + (rectDraw.height - size.y) / 2;
				}

				if (!noDraw || hasHTML) {
					for (Iterator iter = lines.iterator(); iter.hasNext();) {
						String text = (String) iter.next();
						drawLine(gc, text, swtFlags, rectDraw, urlInfo, noDraw);
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
	private static Object[] processLine(final GC gc, final String sLine,
			final Rectangle printArea, final boolean wrap,
			final boolean fullLinesOnly, boolean hasMoreElements) {
		StringBuffer outputLine = new StringBuffer();
		int excessPos = -1;

		if (gc.stringExtent(sLine).x > printArea.width) {
			if (DEBUG) {
				System.out.println("Line to process: " + sLine);
			}
			StringTokenizer stWord = new StringTokenizer(sLine, " ");
			StringBuffer space = new StringBuffer(1);
			int[] iLineLength = {
				0
			};

			if (!wrap) {
				if (DEBUG) {
					System.out.println("No Wrap.. doing all in one line");
				}

				processWord(gc, sLine, sLine, printArea, wrap, iLineLength, outputLine,
						space);
			} else {
				// Process line word by word
				int curPos = 0;
				while (stWord.hasMoreElements()) {
					String word = stWord.nextToken();
					excessPos = processWord(gc, sLine, word, printArea, wrap,
							iLineLength, outputLine, space);
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
			outputLine.append(sLine);
		}

		if (!wrap && hasMoreElements) {
			outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
		}
		//drawLine(gc, outputLine, swtFlags, rectDraw);
		//		if (!wrap) {
		//			return hasMoreElements;
		//		}
		return new Object[] {
			outputLine.toString(),
			new Long(excessPos)
		};
	}

	/**
	 * @param int Position of part of word that didn't fit
	 *
	 * @since 3.0.0.7
	 */
	private static int processWord(final GC gc, final String sLine, String word,
			final Rectangle printArea, final boolean wrap, final int[] iLineLength,
			StringBuffer outputLine, final StringBuffer space) {

		Point ptWordSize = gc.stringExtent(word + " ");
		boolean bWordLargerThanWidth = ptWordSize.x > printArea.width;
		if (iLineLength[0] + ptWordSize.x > printArea.width) {
			//if (ptWordSize.x > printArea.width && word.length() > 1) {
			// word is longer than space avail, split
			int endIndex = word.length();
			do {
				endIndex--;
				ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
			} while (endIndex > 0 && ptWordSize.x + iLineLength[0] > printArea.width);

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
	private void drawLine(GC gc, String outputLine, int swtFlags,
			Rectangle printArea, URLInfo urlInfo, boolean noDraw) {
		String sOutputLine = outputLine.toString();
		Point drawSize = gc.textExtent(sOutputLine);
		int x0;
		if ((swtFlags & SWT.RIGHT) > 0) {
			x0 = printArea.x + printArea.width - drawSize.x;
		} else if ((swtFlags & SWT.CENTER) > 0) {
			x0 = printArea.x + (printArea.width - drawSize.x) / 2;
		} else {
			x0 = printArea.x;
		}

		int y0 = printArea.y;

		if (urlInfo != null) {
			int i = sOutputLine.indexOf(urlInfo.title);
			if (i < 0) {
				if (!noDraw) {
					gc.drawText(sOutputLine, x0, y0, true);
				}
			} else {
				String s = sOutputLine.substring(0, i);
				//s+="i i";
				if (!noDraw) {
					gc.drawText(s, x0, y0, true);
				}
				Point textExtent = gc.textExtent(s);
				x0 += textExtent.x - 1;
				//System.out.println("|" + s + "|" + textExtent.x);

				int end = i + urlInfo.title.length();
				s = sOutputLine.substring(i, end).replaceAll("`", " ");
				//s = "i i ";
				if (!noDraw) {
					Color fgColor = gc.getForeground();
					if (urlColor != null) {
						gc.setForeground(urlColor);
					}
					gc.drawText(s, x0, y0, true);
					gc.setForeground(fgColor);
				}
				textExtent = gc.textExtent(s);

				urlInfo.hitArea = new Rectangle(x0, y0, textExtent.x, textExtent.y);

				if (end < sOutputLine.length() - 1) {
					x0 += textExtent.x;
					s = sOutputLine.substring(end);
					if (!noDraw) {
						gc.drawText(s, x0, y0, true);
					}
				}
			}

		} else {
			if (!noDraw) {
				gc.drawText(sOutputLine, x0, y0, true);
			}
		}
		printArea.y += drawSize.y;
	}

	private static int getAdvanceWidth(GC gc, String s) {
		int result = 0;
		for (int i = 0; i < s.length(); i++) {
			result += gc.getAdvanceWidth(s.charAt(i)) - 1;
		}
		return result;
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
		
		Display display = Display.getDefault();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);

		final String text = "Opil Wrir, Na Poys Iysk, Yann Only. test of the string printer averlongwordthisisyesindeed";

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

		cPaint.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event event) {

				GC gc = new GC(cPaint);

				Color colorBox = gc.getDevice().getSystemColor(SWT.COLOR_YELLOW);
				Color colorText = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);

				gc.setForeground(colorText);
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

				printString(gc, txtText.getText(), bounds, btnSkipClip.getSelection(),
						btnFullOnly.getSelection(), style);

				bounds.width--;
				bounds.height--;

				gc.setForeground(colorBox);
				gc.drawRectangle(bounds);

				System.out.println();

				gc.dispose();
			}
		});

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
		return _printString(gc, string, printArea, printFlags, swtFlags);
	}

	public void calculateMetrics() {
		_printString(gc, string, printArea, printFlags | FLAG_NODRAW, swtFlags);
	}

	/**
	 * @param rectangle
	 *
	 * @since 3.0.4.3
	 */
	public void printString(GC gc, Rectangle rectangle, int swtFlags) {
		this.gc = gc;
		printArea = rectangle;
		this.swtFlags = swtFlags;
		printString();
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

	public Rectangle getUrlHitArea() {
		if (urlInfo == null) {
			return null;
		}
		return urlInfo.hitArea;
	}
	
	public String getUrl() {
		if (urlInfo == null) {
			return null;
		}
		return urlInfo.url;
	}
}
