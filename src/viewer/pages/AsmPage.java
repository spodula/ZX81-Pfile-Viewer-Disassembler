package viewer.pages;
/**
 * G Salkin Jan 2018
 * 
 * Generate the HTML page for the REM disassembly. 
 * Note, there are two modes, ALLREM will disassemble all the REM Statements.
 *  
 * The other mode will try to figure out which parts are machine code and which are Data
 * by roughly following program flow. This may not always be accurate, especially if
 * the program uses memory saving items like using  USR VAL "xxxx", which cannot
 * be easily parsed.  It does seem successful for quite a lot of P files though.
 * 
 * GDS 23-07-2021 - Changed to take into account code at the end of basic area that is just
 * 					corrupt basic lines, EG adventure-C
 */
//TODO: sometimes entry point can be skipped of previous bytes identified as ASM.

import java.util.Hashtable;
import java.util.Vector;

import viewer.libs.ASMLib;
import viewer.libs.MemWrapper;
import viewer.libs.SystemVariables;

public class AsmPage extends PFilePage {
	private static byte MEMTYPE_NONE = 0;   //Program code thats not important
	private static byte MEMTYPE_REM = 1;  	//Program code that is part of REM statement
	private static byte MEMTYPE_NONREM = 2;	//Program code that is not part of a REM, but contains M/C anyway. 
	private static byte MEMTYPE_MC = 3;		//Program code that is unequivocally Machine code.

	public AsmPage(MemWrapper pFil) {
		super(pFil);
	}

	/**
	 * This attempts to provide an assembly listing of all the REM statements in a file. 
	 * @param AllRem
	 * 		If TRUE, treats all REM statements as something to be disassembled. This is useful
	 *      in some files where the program doesn't use straight USR xxxxx statements to call the code. 
	 *      If FALSE, the program tries to follow the program flow to identify data and MC.   
	 * @param AddressIsFile
	 * 		if TRUE, output addresses in the file rather than addresses where loaded. 
	 * @return
	 */
	public String get(boolean AllRem,boolean AddressIsFile) {
		int displacement = 0;
		if (AddressIsFile)  {
			displacement = MemWrapper.ZX81_RAMSTART + MemWrapper.P_FILE_STARTDISP;
		}
		StringBuilder sb = new StringBuilder();
		if (pFile.IsValid()) {
			try {
				Hashtable<Integer,Integer> UsrLineno = new Hashtable<Integer,Integer>();
				/*
				 * create an mirror of the program area with the rem statements as possibilities
				 * and record the USR locations.
				 */

				// Prepare the ram and get the start and end of the basic area.
				Vector<Integer> USRCalls = new Vector<Integer>();
				byte ramtypes[] = new byte[16384];
				int basicptr = 16509;
				int basicend = pFile.GetWordAtMem(SystemVariables.VAR_D_FILE) + 1;
				int VARS = pFile.GetWordAtMem(SystemVariables.VAR_VARS);
				if (VARS < basicend) {
					basicend = VARS;
				}
				//preset all the ram types. 
				for (int i = 0; i < 16384; i++) {
					ramtypes[i] = MEMTYPE_NONE;
				}
				// Now, parse the basic area for REM statements and USR statements.
				boolean lookingfornum = false;
				boolean eop = false;
				while ((basicptr < (basicend - 4)) && !eop) {
					if (pFile.GetWordAtMem(basicptr) != 0x76) { // end of program marker.
						int linenum = pFile.GetByteAtMem(basicptr++) * 256 + pFile.GetByteAtMem(basicptr++);
						
						//get linelen, but trim to basicend if needed. (EG, partial P file)
						int linelen = pFile.GetWordAtMem(basicptr);  
						if ((linelen + basicptr) > basicend) {
							linelen = basicend - basicptr;
						}
						basicptr = basicptr + 2;
						//now check what is being processed. 
						boolean InRem = false;
						for (int i = 0; i < linelen; i++) {
							int chr = pFile.GetByteAtMem(basicptr + i);
							if (InRem) {
								//if we are in a REM, stay there until the end and just mark it as REM. 
								ramtypes[basicptr + i - 0x4000] = MEMTYPE_REM;
							} else {
								//See if we swap to REM. 
								InRem = (pFile.GetByteAtMem(basicptr) == 0xea);
								if (!InRem) {
									//if were not in a REM statement, look for the USR token. 
									if (!lookingfornum) {
										if (chr == 0xd4) { // found a "USR"
											lookingfornum = true;
										}
									} else { // found USR, looking for number or a char between 0x1c and 0x25.
										if (chr == 0x7e) { // number
											double fpv = pFile.GetFloatAtMem(basicptr + i + 1);
											
											USRCalls.add((int) fpv);
											UsrLineno.put((int) fpv, linenum);
											i = i + 5; // skip the number
											lookingfornum = false;
										} else { // If we are not a literal number, we cant process it.
											// some programs use VAL "xxxx" to save memory. (May be parsable, but not at
											// the moment)
											// or store the addresses in a variable. (Probably never going to happen)
											if ((chr < 0x1c) || (chr > 0x25)) {
												lookingfornum = false;
											}
										} //if 0x7e
									} //if lookingfornum  
								} // if !inrem 
							} //if inrem 
						} //for
						basicptr = basicptr + linelen;  //next line
					} else {
						eop = true;
					}
				} //Program search loop
				
				//output USR locations and REM locations. 
				sb.append("<h2>USR locations found:</h2>");
				if (USRCalls.size() == 0) {
					sb.append("none.<br>");
				} else {
					sb.append("<ul>\r\n");
					for (Integer i : USRCalls) {
						Integer lineno = UsrLineno.get(i);
						sb.append("<li>" + i.toString() +" - (Line no. "+lineno.toString()+") </li>\r\n");
					}
					sb.append("</ul>\r\n");
				}
				sb.append("<h2>REM locations:</h2>\r\n<ul>");
				boolean started = false;
				for (int i = 0; i < ramtypes.length; i++) {
					if (!started) {
						if (ramtypes[i] == MEMTYPE_REM) {
							started = true;
							sb.append("<li>" + String.valueOf(i + 0x4000 - displacement) + " - ");
						}
					} else {
						if (ramtypes[i] != MEMTYPE_REM) {
							started = false;
							sb.append(String.valueOf(i + 0x4000 - displacement) + "\r\n");
						}
					}
				}
				
				//if we have no end marker, add in the end of memory marker. 
				if (started) {
					sb.append(String.valueOf(ramtypes.length + 0x4000) + "\r\n");
				}
				sb.append("</ul>\r\n");
				
				//There are some ZX81 programs which put their code in the final parts of the program area. 
				//This appears as gibberish in the program listing. 
				//What we are doing here is trying to identify this gibberish by seeing if any USR calls
				//code in an area which is not down as a REM statement. 
				//If we do find any code here, we just mark everything here as ASM code to be disassembled.
				//an example is the Adventure C tape.
				//This code is a bit of a hack,
				basicptr = 16509;
				eop = false;
				while ((basicptr < (basicend - 4)) && !eop) {
					//skip linenumber
					basicptr++;
					basicptr++;
					//get linelen, but trim to basicend if needed. (EG, partial P file)
					int linelen = pFile.GetWordAtMem(basicptr);  
					if ((linelen + basicptr) > basicend) {
						linelen = basicend - basicptr;
						eop = true;
					} 
					//See if any of the USR commands call this location.
					for (Integer i : USRCalls) {
						if (i>=(basicptr-4) && i <=basicptr+linelen) {
							for(int j = basicptr-4;j<basicptr+linelen;j++) {
								ramtypes[j-0x4000] = MEMTYPE_NONREM;
							}
						}
					}
					
					basicptr = basicptr + 2;
					basicptr = basicptr + linelen;  //next line

				} //Program search loop
				
				sb.append("<h2>Non-REM ASM locations:</h2>\r\n<ul>");
				started = false;
				for (int i = 0; i < ramtypes.length; i++) {
					if (!started) {
						if (ramtypes[i] == MEMTYPE_NONREM) {
							started = true;
							sb.append("<li>" + String.valueOf(i + 0x4000 - displacement) + " - ");
						}
					} else {
						if (ramtypes[i] != MEMTYPE_NONREM) {
							started = false;
							sb.append(String.valueOf(i + 0x4000 - displacement) + "\r\n");
						}
					}
				}
				
				
				if ((USRCalls.size() == 0) && !AllRem) {
					sb.append("<h2>No defined entry points</h2>");
				} else {
					/*
					 * First, we are going to try to identify which bits of the rem statements 
					 * are code and which are data. To do this, we will process a list of all the
					 * USR calls found in the basic program and follow the data flow. When we find jumps
					 * and calls, we will add these to another list and then process that. We will do this
					 * until all calls are marked as Machine code.
					 * 
					 *  If we are in ALLREM mode, we will ignore the above and just mark all REMs
					 *  as being Machine code to be outputted. 
					 */
					
					ASMLib z80 = new ASMLib();
					// generate a list of start addresses filtering out ROM adddresses. 
					Vector<Integer> Addresses = new Vector<Integer>();
					for (Integer addr : USRCalls) {
						if ((addr > 16384) && (addr < 32768)) {
							Addresses.add(addr);
						}
					}
					if (AllRem) {
						// if we are in allrem, just mark all rem locations as MC to be disassembled. 
						for (int i = 0; i < ramtypes.length; i++) {
							if (ramtypes[i] == MEMTYPE_REM || ramtypes[i] == MEMTYPE_NONREM ) {
								ramtypes[i] = MEMTYPE_MC;
							}
						}
					} else {
						if (Addresses.size() == 0) {
							
							sb.append("<h2>No Non-rom entry points</h2>");
						} else {
							// Start at each USR location and try a first pass at disassembly to try to
							// identify data and program areas.
							boolean done = false;
							while (!done) { // keep iterating the list until no more left.
								Vector<Integer> nextpass = new Vector<Integer>();
								done = true;
								for (Integer addr : Addresses) {
									if (ramtypes[addr - 16384] != MEMTYPE_MC) { // already processed this address?
										done = false;
										boolean blockfinished = false;
										// process until we reach an end command or we run out of REM.
										while (!blockfinished && (ramtypes[addr - 16384] == MEMTYPE_REM || ramtypes[addr - 16384] == MEMTYPE_NONREM )) {
											int data[] = new int[5];
											for (int i = 0; i < 5; i++) {
												data[i] = pFile.GetByteAtMem(addr.intValue() + i);
											}
											// mark instruction as being MC.
											ASMLib.DecodedASM result = z80.decode(data, addr);
											for (int i = 0; i < result.length; i++) {
												ramtypes[addr - 16384 + i] = MEMTYPE_MC;
											}
											addr = addr + result.length;
											// check for jumps or calls to add to the list.
											if (result.instruction.startsWith("CALL")
													|| result.instruction.startsWith("JR")
													|| result.instruction.startsWith("JP")
													|| result.instruction.startsWith("DJNZ")) {
												// only add jumps likely to be in the RAM area.
												if ((result.addressref > 16383) && (result.addressref < 32768)) {
													nextpass.add(result.addressref);
												}
											}
											// check for end of code.
											if (result.instruction.equals("RET")
													|| ((result.instruction.startsWith("JR")  //JR and JP without a comma in it EG, JP XXXX
													||   result.instruction.startsWith("JP"))
															&& !result.instruction.contains(","))) {
												blockfinished = true;
											}
										}
									}

								} // for
								Addresses = nextpass;
							} // while
						}
					}
					/*
					 * Next, we trawl the entire memory area and when we come across memtype REM, we now assume data, and
					 * when we find MC, we disassemble and output it. 
					 */
					
					int lasttype = MEMTYPE_NONE;
					sb.append("<pre>");

					int numhex = 0;
					String tokens = "";
					for (int i = 0; i < ramtypes.length; i++) {
						int thistype = ramtypes[i];
						//Check for a type change (EG, REM->MC, MC->REM, xxx->None, ect
						if (thistype != lasttype) {
							// lasttype was REM?
							if (lasttype == MEMTYPE_REM || lasttype == MEMTYPE_NONREM ) {
								while (numhex++ < 8) {
									sb.append("    ");
								}
								sb.append("   ;"+tokens);
								tokens = "";
								sb.append("\r\n");
								// lasttype was MC?
							} else if (lasttype == MEMTYPE_MC) {
								sb.append("\r\n");
							}
							lasttype = thistype;
							sb.append("\r\n");
							if (thistype == MEMTYPE_REM || thistype == MEMTYPE_NONREM) {
								sb.append(IntAndHex(i + 16384 - displacement) + "\tDEFB ");
								numhex = -1;
							}
						}
						//Process the current byte as the current type. 
						if (thistype == MEMTYPE_REM || thistype == MEMTYPE_NONREM) {
							//output 8 bytes of data per line.
							if (numhex++ == 8) {
								sb.append("   ;"+tokens);
								tokens = "";
								sb.append("\r\n" + IntAndHex(i + 16384 - displacement) + "\tDEFB ");
								numhex = 0;
							}
							int chr = pFile.GetByteAtMem(i + 16384);
							sb.append("$" + toHexStringSpace(chr));
							tokens = tokens + GetToken(chr);
						} else if (lasttype == MEMTYPE_MC) {
							//disassemble the next few bytes of data.
							int data[] = new int[5];
							for (int j = 0; j < 5; j++) {
								data[j] = pFile.GetByteAtMem(16384 + i + j);
							}
							// decode instruction
							ASMLib.DecodedASM result = z80.decode(data, 16384 + i);
							// output it. - First, assemble a list of hex bytes, but pad out to 12 chars (4x3)
							String hex = "";
							for (int j = 0; j < result.length; j++) {
								hex = hex + toHexStringSpace(data[j]);
							}
							while (hex.length() < 12) {
								hex = hex + "   ";
							}
							//If the current address is found in the USR list, output that. 
							Integer Address =  i + 16384;
							if (USRCalls.contains(Address)) {
								Integer lineno = UsrLineno.get(Address);
								sb.append("<b>" + IntAndHex(i + 16384) + " - Basic entry point (Line "+lineno.toString()+").</b>\r\n");
							}

							//output assembly line. 
							sb.append(IntAndHex(i + 16384 - displacement) + "\t" + hex + "\t" + result.instruction + "\r\n");
							//next instruction.
							i = i + result.length - 1;
						}
					} // for
					sb.append("</pre>");
				}
			} catch (Exception E) {
				sb.append("<br><br>\r\n\r\nError encountered: " + E.getMessage() + "<br>");
				E.printStackTrace();
			}
		} else {
			sb.append("<h2>No file loaded</h2>");
		}
		return sb.toString();
	}

}
