package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Base class for the page generation logic. Provides some useful utility functions.   
 */

import java.util.Vector;

import viewer.libs.MemWrapper;

public class PFilePage {
	public MemWrapper pFile = null;

	public PFilePage(MemWrapper pFil) {
		pFile = pFil;
	}

	/**
	 * return a 2 digit hex number with additional space.
	 * 
	 * @param i
	 * @return
	 */
	char hexstr[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public String toHexStringSpace(int i) {
		int msb = (i & 0xf0) / 16;
		int lsb = (i & 0x0f);
		char result[] = { hexstr[msb], hexstr[lsb], ' ' };

		return (new String(result));
	}

	/**
	 * Provide a hex dump without HTML formatting. This was mainly used for
	 * debugging. Its left here because i may want to use it again. Note: This is
	 * the old (slow) non-StringBuilder version.
	 * 
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */
	public String hexdump(int start, int end) throws Exception {
		String result = "<h2>" + IntAndHex(start) + " to " + IntAndHex(end)
				+ "</h2>\r\n               00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F";

		int ptr = start & 0xFFF0;

		result = result + "\r\n" + IntAndHex(ptr) + ": ";
		while (ptr <= end) {
			if (ptr < start) {
				result = result + "-- ";
			} else {
				result = result + toHexStringSpace(pFile.GetByteAtMem(ptr));
			}
			ptr++;

			if (((ptr & 0x0f) == 0) && (ptr != (end + 1))) {
				result = result + "\r\n" + IntAndHex(ptr) + ": ";
			}
		}
		while ((ptr & 0x0f) != 0) {
			result = result + "-- ";
			ptr++;
		}

		result = result + "\r\n";
		return (result);
	}

	/**
	 * Provide a hex dump with HTML formatting, colouring and keys. 
	 * GDS March 2018 - converted to use StringBuilder to speed it up.
	 * 
	 * @param start
	 *            - Start of values. Note displays from the lowest $10
	 * @param end
	 *            - End of values. Note, Displays entire final line with "--"
	 * @param Boundaries
	 *            - Vector of colour boundary addresses. In order of address.
	 * @param Keys
	 *            - Text to be put in the key at the bottom
	 * @return - String containing the hex dump
	 * @throws Exception
	 */
	public String hexdumpHTML(int start, int end, Vector<Integer> Boundaries, Vector<String> Keys, int displacement)
			throws Exception {
		String[] colours = { "#00ffff", "#ffff00", "#ff6666", "#cc66ff", "#0099ff", "#cccc00", "#99ff99", "#00ccff",
				"#9999ff", "#cccc00", "#6699ff", "#ff8c66", "#b366ff", "#ffb366", "#ff668c", "#66ffff" };

		start = start - displacement;
		end = end - displacement;
		int numcols = colours.length;
		int boundaryptr = 1;
		int nextboundary = -1;
		if (Boundaries.size() > 0) {
			nextboundary = Boundaries.get(0).intValue() - displacement;
		}
		StringBuilder sb = new StringBuilder();

		try {
			sb.append("<h2>" + IntAndHex(start) + " to " + IntAndHex(end) + "</h2>\r\n<table>\r\n<tr><td></td><td>");
			sb.append("<pre>00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F</pre></td></tr>\r\n");

			int ptr = start & 0xFFF0;
			int cptr = -1;

			sb.append("<tr><td>" + IntAndHex(ptr) + "</td><td><pre>");
			while (ptr <= end) {
				if (ptr < start) {
					sb.append("-- ");
				} else {
					/*
					 * check to see if we are on a boundary and change colours appropriately.
					 */
					if (nextboundary == ptr) {
						// Get the next boundary. (Or set to after the last character if at the end.)
						if (boundaryptr == Boundaries.size()) {
							nextboundary = end + 16;
						} else {
							nextboundary = Boundaries.get(boundaryptr++).intValue() - displacement;
						}
						// If not the first colour change, end the current colour.
						if (cptr != -1) {
							sb.append("</span>");
						}
						// Now, set the colour from the list.
						cptr++;
						sb.append("<span style=\"background-color:" + colours[cptr % numcols] + "\">");
					}
					// append the byte
					sb.append(toHexStringSpace(pFile.GetByteAtMem(ptr + displacement)));
				}
				ptr++;
				// go onto a newline if we have reached the end of the line.
				if (((ptr & 0x0f) == 0) && (ptr != (end + 1))) {
					// end the current colour.
					if (cptr != -1) {
						sb.append("</span>");
					}
					// output the address.
					sb.append("</pre></td></tr>\r\n<tr><td>" + IntAndHex(ptr) + "</td><td><pre>");
					// now start the colour again.
					if (cptr != -1) {
						sb.append("<span style=\"background-color:" + colours[cptr % numcols] + "\">");
					}
				}
			}
			if (cptr != -1) {
				sb.append("</span>");
			}
			while ((ptr & 0x0f) != 0) {
				sb.append("-- ");
				ptr++;
			}

			sb.append("</pre></td></tr>\r\n</table>\r\n<br>\r\n");
			for (int i = 0; i < Keys.size(); i++) {
				String key = Keys.get(i);
				String color = colours[i & 15];
				sb.append("<span style=\"background-color:" + color + "\">" + key + "</span><br>\r\n");
			}
			sb.append("<br>");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return (sb.toString());
	}

	/**
	 * Returns a string in the format "xxxx ($yyyy)" where xxxx is the decimal
	 * number and yyyy is the hex.
	 * 
	 * @param i
	 * @return
	 */
	public String IntAndHex(int i) {
		Integer ii = i;

		String result = ii.toString() + " ($" + Integer.toHexString(ii) + ")";

		return (result);
	}

	/**
	 * Character -> Token (HTML ASCII) table. for ($00 -> $3F) (0-63)
	 * note 0x09 and 0x0a require special handling as there isnt a straight UNICODE 
	 * replacement that i have been able to find. This is handled in HTMLForChar
	 */
	public int[] tokens00_3F = { 0x20, 0x2598, 0x259D, 0x2580, 0x2596, 0x258C, 0x259E, 0x259B, 0x2592, 0x00, 0x00, 0x22,
			0x23, 0x24, 0x3a, 0x3f, 0x28, 0x29, 0x3e, 0x3c, 0x3d, 0x2b, 0x2d, 0x2a, 0x2f, 0x3b, 0x2c, 0x2e, 0x30, 0x31,
			0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
			0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, };

	/**
	 * Character -> Token (HTML ASCII) table. for ($40 -> $7F) (64-127)
	 */
	private String tokens40_7F[] = { "RND", "INKEY$", "PI", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
			"", "", "", "!&lt;cursor up&gt;", "!&lt;cursor down&gt;", "!&lt;cursor left&gt;", "!&lt;cursor right&gt;",
			"!&lt;graphics&gt;", "!&lt;edit&gt;", "!&lt;newline&gt;", "!&lt;rubout&gt;", "!&lt;K/L mode&gt;",
			"!&lt;function&gt;", "", "", "", "", "!&lt;number&gt;", "!&lt;cursor&gt;" };
	
	/**
	 * No character tokens for $80 -> $BF as these are just inverted versions of $00 -> $3F
	 */

	/**
	 * Character -> Token (HTML ASCII) table. for ($C0 -> $FF) (192-255)
	 */
	private String[] tokensC0_FF = { "\"\"", "AT", "TAB ", "", "CODE ", "VAL", "LEN", "SIN", "COS ", "TAN", "ASN",
			"ACS", "ATN", "LN", "EXP", "INT", "SQR", "SGN", "ABS", "PEEK", "USR", "STR$", "CHR$", "NOT", "**", "OR",
			"AND", "<=", ">=", "<>", "THEN", "TO", "STEP", "LPRINT", "LLIST", "STOP", "SLOW", "FAST", "NEW", "SCROLL",
			"CONT", "DIM", "REM", "FOR", "GOTO", "GOSUB", "INPUT", "LOAD", "LIST", "LET", "PAUSE", "NEXT", "POKE",
			"PRINT", "PLOT", "RUN", "SAVE", "RAND", "IF", "CLS", "UNPLOT", "CLEAR", "RETURN", "COPY" };


	/**
	 * Translate a given byte to a displayable HTML string.  This expands tokens where appropriate. 
	 * 
	 * @param i
	 * @return
	 */
	public String GetToken(int i) {

		String result = "";
		if (i == 0x76) {
			result = "<nl>\r\n";
		} else {
			//displayable single character
			if ((i < 64) || ((i > 127) && (i < 192))) {
				result = result + HTMLForChar(i, false);
			} else {
				String token="";
				if ((i > 63) && (i < 128)) {
					//expand character in the 64-127 range. 
					token = tokens40_7F[i - 0x40];
				} else {
					//expand character in the 192-255 range.
					token = tokensC0_FF[i - 0xC0];
				}
				// blank means no displayable representation
				if (!token.isEmpty()) {
					// if prefixed by "!" just output string with no further conversion.
					if (token.charAt(0) == '!') {
						result = token.substring(1);
					} else {
						result = token + " ";
					}
				}
			}
		}
		return (result);
	}

	/**
	 * This function adds the tags to the given string. If its a table, it will
	 * surround with
	 * <td>tags. If its not a table, will surround with <span> tags, but only if
	 * required for a given style.
	 * 
	 * @param s
	 * @param IsTable
	 * @param Style
	 * @return
	 */
	private String MkTag(String s, boolean IsTable, String Style) {
		String result = "";
		if (IsTable) {
			if (Style.isEmpty()) {
				result = "<td>" + s + "</td>";
			} else {
				result = "<td style=\"" + Style + "\">" + s + "</td>";
			}
		} else {
			if (Style.isEmpty()) {
				result = s;
			} else {
				result = "<span style=\"" + Style + "\">" + s + "</span>";
			}
		}
		return (result);
	}

	/**
	 * Get the HTML equivalent for a given ZX81 character.
	 * Displayable characters only.
	 * 
	 * @param chr
	 *            Character
	 * @param IsTable
	 *            If TRUE, all items are returned surrounded with
	 *            <td></td>tags. if FALSE, surrounded with <span></span> tags, but only if needed
	 *            for style.
	 * @return
	 */
	public String HTMLForChar(int chr, boolean IsTable) {
		String result = "";
		if (chr == 0x76) { // end of line.
			result = MkTag("&lt;nl&gt;", IsTable, "");
			/*
			 * Special cases for the half-crosshatch characters. These are not present in
			 * UNICODE. (Or at least i couldn't find them). So just take the normal
			 * character and make them grey instead. a bit of hack, but it works.
			 */
		} else if (chr == 0x09) {
			result = MkTag("" + (char) 0x2584, IsTable, "color:grey");
		} else if (chr == 0x0A) {
			result = MkTag("" + (char) 0x2580, IsTable, "color:grey");
		} else if (chr == 0x89) {
			result = MkTag("" + (char) 0x2584, IsTable, "background-color:black;color:grey");
		} else if (chr == 0x8A) {
			result = MkTag("" + (char) 0x2580, IsTable, "background-color:black;color:grey");
		} else if ((chr < 64) || ((chr > 127) && (chr < 192))) {
			//invert style?
			boolean invert = (chr > 127) && (chr < 192);
			String style="";
			if (invert) {
				style = "background-color:black;color:white";
			}

			char ascii = (char) tokens00_3F[chr & 0x3F];
			if (ascii == 0) {
				ascii = '~';
			} 
			//htmlify some of the most used characters.
			if (ascii=='<') {
				result = MkTag("&lt;", IsTable, style);
			} else if (ascii=='>') { 
				result = MkTag("&gt;", IsTable, style);
			} else if (ascii=='&') { 
				result = MkTag("&amp;", IsTable, style);
			} else {				
				result = MkTag("" + ascii, IsTable, style);
			}
			/*
			 * Inverted normal characters
			 */
		} else {
			/*
			 * Characters from 0x40-0x7F and 0xc1-0xff are unprintable.
			 */
			result = MkTag("~", IsTable, "");
		}
		return (result);
	}

}
