package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Generate the system variables page   
 */

import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;
import viewer.libs.SystemVariables.SystemVariable;

public class SystemVariablesPage extends PFilePage {

	public SystemVariablesPage(MemWrapper pFil) {
		super(pFil);
	}

	/**
	 * Return the HTML for the system variables page.
	 * 
	 * @return
	 */
	public String get(boolean AddressIsFile) {
		int displacement = 0;
		if (AddressIsFile) {
			displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
		}
		String result = "<h2>No file loaded</h2>";
		if (pFile.IsValid()) {
			try {

				result = "<h2>System variables</h2>\r\n" + "<table>\r\n"
						+ "<tr><th style=\"width:120px\">Address</th><th style=\"width:70px\">Name</th><th style=\"width:120px\">Value</th><th>Notes</th></tr>\r\n";
				SystemVariables sysvars = new SystemVariables();
				for (SystemVariable variable : sysvars.vars) {
					if (variable != null) {
						result = result + "<tr><td>" + IntAndHex(variable.address - displacement) + "</td>";
						result = result + "<td>" + variable.name + "</td>";

						if (variable.sz == SystemVariables.SYSVAR_FLAGS) {
							int flags = pFile.GetByteAtMem(variable.address);
							String[][] flaglist = sysvars.FlagList.get(variable.abbrev);
							if (flaglist != null) {
								result = result + "<td>" + IntAndHex(flags) + " (" + decode_flags(flags, flaglist)
										+ ")</td>";
							} else {
								result = result + "<td>" + IntAndHex(flags) + "(Cannot find flag defs for "
										+ variable.abbrev + ")</td>";
							}
						} else if (variable.sz == 1) {
							result = result + "<td>" + IntAndHex(pFile.GetByteAtMem(variable.address)) + "</td>";
						} else if (variable.sz == 2) {
							result = result + "<td>" + IntAndHex(pFile.GetWordAtMem(variable.address)) + "</td>";
						} else if (variable.sz == SystemVariables.SYSVAR_COORDS) {
							result = result + "<td>" + IntAndHex(pFile.GetWordAtMem(variable.address)) + " ("
									+ String.valueOf(pFile.GetByteAtMem(variable.address + 1)) + ","
									+ String.valueOf(pFile.GetByteAtMem(variable.address))+ ")</td>";
						} else if (variable.sz == SystemVariables.SYSVAR_PRBUFF) {
							// output the printer buffer in 8 character lines.
							result = result + "<td style=\"border: 1px solid black\">";
							for (int i = 0; i < 32; i++) {
								int chr = pFile.GetByteAtMem(variable.address + i);

								result = result + GetToken(chr);
								if ((i % 8) == 7) {
									result = result + "<br>";
								}
							}
							result = result + "</td>";
						}
						result = result + "<td>" + variable.desc + "</td></tr>\r\n";
					}
				}
				result = result + "</table>\r\n";
			} catch (Exception E) {
				result = "Error encountered: " + E.getMessage() + "<br>" + E.getStackTrace();
			}
		}
		return result;
	}

	/**
	 * Generate a string of the flags referenced in the given array
	 * 
	 * @param val
	 * @param flags
	 * @return
	 */
	public String decode_flags(int val, String flags[][]) {
		String result = "";
		for (int i = 0; i < 8; i++) {
			int bit = val & 0x01;
			val = val / 2;
			String disp = flags[i][bit];
			if (!disp.isEmpty()) {
				result = result + " " + disp;
			}
		}

		return (result.trim());
	}

}
