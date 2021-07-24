package viewer.pages; 
/**
 * G Salkin Jan 2018
 * 
 * Basic summary page generation.    
 */

import java.io.File;
import java.util.Vector;

import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;

public class SummaryPage extends PFilePage {
	public SummaryPage(MemWrapper pFil) {
		super(pFil);
	}
	
	/**
	 * Return the HTML for the basic summary page.  
	 * @param lastFileLoaded
	 * @return
	 */
	public String Get(File lastFileLoaded,boolean AddressIsFile) {
		int displacement = 0;
		if (AddressIsFile)  {
			displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
		}
		Vector<Integer> Boundaries = new Vector<Integer>();
		Vector<String> keys = new Vector<String>();
		String result = "<h2>No file loaded</h2>";
		if (pFile.IsValid()) {
			try {
				int endloc = (int) (MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP + lastFileLoaded.length());
				
				result = "<h2>Summary for " + lastFileLoaded.getName() + "</h2>\r\n";
				result = result + "File: " + lastFileLoaded.getName() + "<br>\r\n";
				result = result + "Location: " + lastFileLoaded.getAbsolutePath() + "<br>\r\n";
				result = result + "Size: " +  String.valueOf(lastFileLoaded.length()) + "<br>\r\n";
				result = result + "Load range: " + IntAndHex(MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP)
						+ " - " + IntAndHex(endloc)
						+ "<br>\r\n";

				Boundaries.add(MemWrapper.ZX81_RAMSTART+MemWrapper.P_FILE_STARTDISP-displacement);
				keys.add("System variables");
				Boundaries.add(16509-displacement);
				keys.add("BASIC program");
				Boundaries.add(pFile.GetWordAtMem(SystemVariables.VAR_D_FILE)-displacement);
				keys.add("Display file");
				Boundaries.add(pFile.GetWordAtMem(SystemVariables.VAR_VARS)-displacement);
				keys.add("Variables");
				Boundaries.add(pFile.GetWordAtMem(SystemVariables.VAR_E_LINE)-displacement);
				keys.add("Edit line");
				Boundaries.add(pFile.GetWordAtMem(SystemVariables.VAR_STKBOT)-displacement);
				keys.add("Calculator stack");

				result = result + hexdumpHTML(MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP, pFile.GetLastRam(),
						Boundaries, keys, displacement);

			} catch (Exception E) {
				result = "Error encountered: " + E.getMessage() + "<br>" + E.getStackTrace();
			}
		}
		return (result);
	}

}
