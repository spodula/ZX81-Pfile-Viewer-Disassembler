package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Generate the HTML representation for the variables area.   
 */ 

import java.util.Vector;

import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;

public class VariablesPage extends PFilePage {
	public VariablesPage(MemWrapper pFil) {
		super(pFil);
	}
	
   /**
	 * return the HTML for the Program variables area. 
	 * @return
	 */
	public String get(boolean AddressIsFile) {
		int displacement = 0;
		if (AddressIsFile)  {
			displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
		}
		StringBuilder sb = new StringBuilder();
		Vector<Integer> Boundaries = new Vector<Integer>();
		Vector<String> keys = new Vector<String>();
		if (pFile.IsValid()) {
			try {
				int VARS = pFile.GetWordAtMem(SystemVariables.VAR_VARS);
				int ELINE = pFile.GetWordAtMem(SystemVariables.VAR_E_LINE);
				sb.append("<h2>Variables</h2>"
						+ "Loaded location: " + IntAndHex(VARS) + " - " + IntAndHex(ELINE - 1) + "<br>\r\n"
						+ "File location: " + IntAndHex(VARS-0x4009) + " - " + IntAndHex(ELINE - 1-0x4009) + "<br>\r\n<br>\r\n<br>\r\n");

				if (ELINE == (VARS + 1)) {
					sb.append("<h2>No variables</h2>");
				} else {
					while (VARS < ELINE) {
						sb.append(String.valueOf(VARS-displacement) + ": ");
						Boundaries.add(VARS-displacement);
						int chr = pFile.GetByteAtMem(VARS++);
						int ctype = chr / 0x20;
						if (chr == 0x80) {
							//anything after this marker is junk so just skip it. 
							sb.append("End of variables<br>\r\n");
							VARS = ELINE;
						} else if (ctype == 1) {
							sb.append("unknown type");
						} else if (ctype == 2) { // string
							VARS = VariableType2(sb, keys, VARS, chr );
						} else if (ctype == 3) { // number (1 letter)
							VARS = VariableType3(sb, keys, VARS, chr);
						} else if (ctype == 4) { // Array of numbers
							VARS = VariableType4(sb, keys, VARS, chr);
						} else if (ctype == 5) { // Number who's name is longer than 1 letter
							VARS = VariableType5(sb, keys, VARS, chr);
						} else if (ctype == 6) { // array of characters
							VARS = VariableType6(sb, keys, VARS, chr);
						} else if (ctype == 7) { // for/next control variable
							VARS = VariableType7(sb, keys, VARS, chr);
						} else {
							System.out.print("UNKNOWN! $" + Integer.toHexString(chr));
						}
					}
				}
				sb.append(hexdumpHTML(pFile.GetWordAtMem(SystemVariables.VAR_VARS), ELINE - 1, Boundaries, keys,displacement));
			} catch (Exception E) {
				sb.append("<br><br>\r\n\r\nError encountered: " + E.getMessage() + "<br>" + E.getStackTrace());
			}
		} else {
			sb.append("<h2>No file loaded</h2>");
		}
		return sb.toString();
	}
	/**
	 * Handler for type 2 variables (Strings)
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType2(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception {
		String key = IntAndHex(Address - 1) + ": ";
		int varname = (chr & 0x3f) + 0x20;
		varname = varname - 0x26 + 'A';
		int length = pFile.GetWordAtMem(Address);
		Address = Address + 2;
		String s = "";
		sb.append("String " + String.valueOf((char) varname) + "$ ("
				+ String.valueOf(length) + " bytes)= \"");
		while (length > 0) {
			int c = pFile.GetByteAtMem(Address++);
			s = s + GetToken(c);
			length--;
		}
		key = key + String.valueOf((char) varname) + "$=\"" + s + "\"";
		keys.add(key);
		sb.append(s + "\"<br>\r\n");
		return(Address);
	}
	
	/**
	 * Handler for type 3 variables (Numbers with a one character name)
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType3(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception {
		String key = IntAndHex(Address - 1) + ": ";
		int varname = chr & 0x3f;
		varname = varname - 0x26 + 'A';
		double value = pFile.GetFloatAtMem(Address);
		String txt = "Number " + String.valueOf((char) varname) + "=" + String.valueOf(value);
		sb.append(txt + "<br>\r\n");
		Address = Address + 5;
		keys.add(key + "Number " + String.valueOf((char) varname) + "="
				+ String.valueOf(value));
		return(Address);
	}

	/**
	 * Handler for type 4 variables (Numeric arrays)
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType4(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception { 
		String key = IntAndHex(Address - 1) + ": ";
		int varname = (chr & 0x3f) + 0x20;
		varname = varname - 0x26 + 'A';
		String txt = "Number array " + String.valueOf((char) varname) + "(";
		// int length = pFile.GetWordAtMem(VARS);
		Address = Address + 2;
		int dimensions = pFile.GetByteAtMem(Address++);
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			dims[i] = pFile.GetWordAtMem(Address++);
			Address++;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		key = key + txt + ")";
		txt = txt + ") = {\r\n";
		keys.add(key);
		boolean first = false;
		boolean done = false;
		while (!done) {
			double val = pFile.GetFloatAtMem(Address);
			Address = Address + 5;
			txt = txt + "(";
			for (int i = 0; i < dimensions; i++) {
				if (i != 0) {
					txt = txt + ",";
				}
				txt = txt + String.valueOf(Math.round(dimcounts[i]));
			}
			txt = txt + ")=" + String.valueOf(val);
			boolean decdone = false;
			int dimid = dimensions - 1;
			while (!decdone) {
				int num = dimcounts[dimid];
				num++;
				if (num > dims[dimid]) {
					dimcounts[dimid] = 1;
					if (dimid == dimensions - 1) {
						txt = txt + "\r\n";
					} else {
						txt = txt + "\r\n\r\n";
					}
					dimid--;
					if (dimid == -1) {
						decdone = true;
						done = true;
					}
					first = true;
				} else {
					if (!first) {
						txt = txt + ", ";
					}
					first = false;
					dimcounts[dimid] = num;
					decdone = true;
				}
			}
		}
		txt = txt.trim() + "\r\n}\r\n";

		sb.append(txt.replace("\r\n", "<br>\r\n") + "<br>");

		return(Address);
	}

	/**
	 * Handler for type 5 arrays (Number with a name > 1)
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType5(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception { 
		String key = IntAndHex(Address - 1) + ": Number ";
		sb.append("Number ");
		boolean done = false;
		while (!done) {
			int varname = (chr & 0x3f) - 0x26 + 'A';
			sb.append(String.valueOf((char) varname));
			key = key + String.valueOf((char) varname);
			chr = pFile.GetByteAtMem(Address++);
			done = (chr & 0x80) == 0x80;
		}
		int varname = (chr & 0x3f) - 0x26 + 'A';
		sb.append(String.valueOf((char) varname));
		key = key + String.valueOf((char) varname);

		sb.append(" = ");
		key = key + " = ";

		double value = pFile.GetFloatAtMem(Address);
		sb.append(String.valueOf(value).toString() + "<br>");
		key = key + String.valueOf(value).toString();
		Address = Address + 5;
		keys.add(key);
		return(Address);
	}
	
	/**
	 * Handler for type 6 variables (Character arrays)
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType6(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception { 
		String key = IntAndHex(Address - 1) + ": ";
		int varname = (chr & 0x3f) + 0x20;
		varname = varname - 0x26 + 'A';
		String txt = "Character array " + String.valueOf((char) varname) + "$(";
		// int length = pFile.GetWordAtMem(VARS);
		Address = Address + 2;
		int dimensions = pFile.GetByteAtMem(Address++);
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			dims[i] = pFile.GetWordAtMem(Address++);
			Address++;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		key = key + txt + ")";
		keys.add(key);
		txt = txt + ") = {\r\n";
		boolean first = false;
		boolean done = false;
		while (!done) {
			int chracter = pFile.GetByteAtMem(Address++);
			txt = txt + "(";
			for (int i = 0; i < dimensions; i++) {
				if (i != 0) {
					txt = txt + ",";
				}
				txt = txt + String.valueOf(Math.round(dimcounts[i])).toString();
			}
			txt = txt + ")=" + GetToken(chracter);
			boolean decdone = false;
			int dimid = dimensions - 1;
			while (!decdone) {
				int num = dimcounts[dimid];
				num++;
				if (num > dims[dimid]) {
					dimcounts[dimid] = 1;
					if (dimid == dimensions - 1) {
						txt = txt + "\r\n";
					} else {
						txt = txt + "\r\n\r\n";
					}
					dimid--;
					if (dimid == -1) {
						decdone = true;
						done = true;
					}
					first = true;
				} else {
					if (!first) {
						txt = txt + ", ";
					}
					first = false;
					dimcounts[dimid] = num;
					decdone = true;
				}
			}
		}
		txt = txt.trim() + "\r\n}\r\n";

		sb.append(txt.replace("\r\n", "<br>\r\n") + "<br>");
		return(Address);
		
	}
	
	/**
	 * Handler for type 7 variables (FOR/NEXT variables)
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private int VariableType7(StringBuilder sb, Vector<String> keys, int Address, int chr ) throws Exception {
		String key = IntAndHex(Address - 1) + ": ";
		int varname = (chr & 0x3f);// + 0x20;
		varname = varname - 0x26 + 'A';

		String txt = "For/Next  " + String.valueOf((char) varname);

		txt = txt + " Value=" + String.valueOf(pFile.GetFloatAtMem(Address)).toString();
		Address = Address + 5;
		txt = txt + " Limit=" + String.valueOf(pFile.GetFloatAtMem(Address)).toString();
		Address = Address + 5;
		txt = txt + " Step=" + String.valueOf(pFile.GetFloatAtMem(Address)).toString();
		Address = Address + 5;
		int line = pFile.GetByteAtMem(Address++) + (pFile.GetByteAtMem(Address++) * 256);
		txt = txt + " Loop line=" + String.valueOf(line) + "\r\n";

		keys.add(key + txt);
		sb.append(txt.replace("\r\n", "<br>\r\n"));
		return(Address);
	}
	

}
