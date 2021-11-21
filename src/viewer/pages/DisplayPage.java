package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Generate the HTML page for the Display file stored in the PFile.    
 */

import java.util.Vector;

import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;

public class DisplayPage extends PFilePage {
	boolean IsInJar;

	public DisplayPage(MemWrapper pFil) {
		super(pFil);
		@SuppressWarnings("rawtypes")
		Class me = getClass();
		IsInJar = me.getResource(me.getSimpleName() + ".class").toString().startsWith("jar:");
	}

	/**
	 * Return the HTML representation for the display file.
	 * 
	 * @return
	 */
	public String get(boolean AddressIsFile) {
		int displacement = 0;
		if (AddressIsFile) {
			displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
		}
		StringBuilder sb = new StringBuilder();
		if (pFile.IsValid()) {
			try {
				//Screen always starts at D_FILE.  
				int DISPLAY_START = pFile.GetWordAtMem(SystemVariables.VAR_D_FILE) + 1;
				int DPointer = DISPLAY_START;
				sb.append("<h2>Display</h2>\r\n");
				sb.append("<b>Address when loaded: </b>" + String.valueOf(DISPLAY_START) + "<br>\r\n");
				sb.append("<b>Address in file: </b>" + String.valueOf(DISPLAY_START - 0x4009) + "<br>\r\n");
				sb.append(
						"<table border=\"1\" style=\"border:1px solid black; border-collapse: collapse;text-align:center\">\r\n");
				sb.append("<tr style=\"background-color:yellow\"><td></td>");
				
				//header line
				for (int col = 0; col < 33; col++) {
					sb.append("<td>" + String.valueOf(col) + "</td>");
				}
				sb.append("</tr>\r\n");

				//22 lines.
				for (int row = 0; row < 23; row++) {
					boolean endofrow = false;
					//line no.
					sb.append("<tr><td style=\"background-color:yellow\">" + String.valueOf(row) + "</td>");
					for (int col = 0; col < 33; col++) {
						if (endofrow) {
							sb.append("<td style=\"background-color:grey\"></td>");
						} else {
							int chr = pFile.GetByteAtMem(DPointer++);
							if (chr == 0x76) { // end of line.
								endofrow = true;
							}
							sb.append(HTMLForChar(chr, true));
						} // not end of row.
					} // for
					sb.append("</tr>\r\n");
				}
				sb.append("</table>\r\n");

				//output the Hex dump at the end. 
				Vector<Integer> keys = new Vector<Integer>();
				keys.add(DISPLAY_START);
				sb.append(hexdumpHTML(DISPLAY_START, DPointer - 1, keys, new Vector<String>(), displacement));

			} catch (Exception E) {
				sb.append("<br><br>\r\n\r\nError encountered: " + E.getMessage() + "<br>" + E.getStackTrace());
			}
		} else {
			sb.append("<h2>No file loaded</h2>");
		}
		// System.out.println(sb.toString());

		return sb.toString();
	}
}
