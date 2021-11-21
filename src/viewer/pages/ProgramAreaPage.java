package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Generate the HTML representation for the zx81 Program (Mostly as html images in a table).   
 */


import java.util.Vector;

import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;

public class ProgramAreaPage extends PFilePage {
	public ProgramAreaPage(MemWrapper pFil) {
		super(pFil);
	}
	
	/**
	 * Get the HTML for the Program area page. 
	 * @param AddressIsFile
	 * @return
	 */
	public String Get(boolean AddressIsFile) {
		Vector<Integer> Boundaries = new Vector<Integer>();
		Vector<String> keys = new Vector<String>();
		StringBuilder sb = new StringBuilder();
		if (pFile.IsValid()) {
			try {
				sb.append("<h2>Program</h2>\r\n<table>");
				//program start address. 
				int PROG=16424;  //zx80
				if (pFile.GetByteAtMem(0x4009)==0) {
					PROG = 16509; //zx81	
				} 
				
				//Program area ends either at VARS (zx80) or D_FILE (zx81). Whichever is lowest. 
				int D_FILE = pFile.GetWordAtMem(SystemVariables.VAR_D_FILE) + 1;
				int VARS = pFile.GetWordAtMem(SystemVariables.VAR_VARS);
				if (VARS < D_FILE) {
					D_FILE = VARS;
				}

				//there are 4 bytes at the start of each line. If were in that range, something has gone wrong. 
				//(POssibly a partially deleted line)
				while (PROG < (D_FILE - 4)) {
					Boundaries.add(PROG);

					// linenos are backwards, so get it in bytes.
					int linenum = pFile.GetByteAtMem(PROG++) * 256 + pFile.GetByteAtMem(PROG++);
					//get the line length. 
					int linelen = pFile.GetWordAtMem(PROG);
					if ((linelen + PROG) > D_FILE ) {
						//If its screwed up, correct it. 
						linelen  = D_FILE - PROG;
					}

					keys.add("Line: "+String.valueOf(linenum)+" ("+String.valueOf(linelen)+" bytes)");			

					//skip the line length
					PROG = PROG + 2;
					
					//start of line: 
					sb.append("<tr><td style=\"vertical-align:top\">" + String.valueOf(linenum) + "</td><td>");
					//Is our command a REM? because the normal number marker doesnt apply if so.  
					boolean InRem = (pFile.GetByteAtMem(PROG) == 0xea);
					for (int i = 0; i < linelen; i++) {
						int chr = pFile.GetByteAtMem(PROG + i);
						//if we have found a number marker and we are not in a rem... 
						if ((chr == 0x7e) && !InRem) {
							double fpv = pFile.GetFloatAtMem(PROG + i + 1);
							sb.append("<span style=\"font-size:70%\">(" + String.valueOf(fpv) + ")</span>");
							i = i + 5; // skip the number
						} else {
							sb.append(GetToken(chr));
						}
					}
					//point to next line. 
					PROG = PROG + linelen;
					sb.append("</td></tr>\r\n");
					//if the start of the next line is 0x76, there are no extra valid lines. 
					//Sometimes however you can find the remains of a deleted program in here. 
					if (pFile.GetWordAtMem(PROG) == 0x76) {
						sb.append("<tr><td colspan=\"2\">(End of program. everything after here is deleted.)</td></tr>\r\n");
						PROG++;
					}
				}
				sb.append("</table>\r\n");
				//if our addresses are by file address rather than by memory address, calculate a displacement. 
				int displacement = 0;
				if (AddressIsFile)  {
					displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
				}
				sb.append(hexdumpHTML(16509,pFile.GetWordAtMem(SystemVariables.VAR_D_FILE), Boundaries, keys,displacement));
				
				
			} catch (Exception E) {
				sb.append("\r\n\r\nError encountered: " + E.getMessage() + "<br>" + E.getStackTrace());
			}
		} else {
			sb.append("<h2>No file loaded</h2>");
		}
		
		return sb.toString();
		

	}

	
}
