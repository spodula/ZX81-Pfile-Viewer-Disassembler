package viewer.libs;
/**
 * G Salkin Feb 2018
 * 
 * This is a quick and dirty disassembler. It relies on the XML files: z80.xml and romlabels.xml being in the 
 * root of the app. This contains the full list of all understood instructions.
 * 
 *  GDS 16 Mar 2018 - Slightly less dirty, but a lot quicker. Now creates a hashtable
 *  of the initial byte (Two bytes in the case of CB,ED,FD, DD instructions
 *  which will generally give a much more limited number of instructions items to check.
 *  (In most cases, just the one, however some of the more obscure DDCB and FDCB instructions, 
 *  it may get a load to check. However, these are sufficiently obscure that it will unlikely
 *  add too much time)   
 *
 *   
 *   
 *   The z80.xml file replacements:
 *   --------------------------
 *   HEX  -  In Mnemonic   usage
 *   --------------------------
 *   HH   - (IX+d)     IX/IY displacement
 *   NN   - n          Static 8 bit number
 *   NNNN - nn         Static 16 bit number 
 *   LLLL - nn         Address reference
 *   GG   - GG         Relative address, ASM replaces this with the calculated address
 *   ZZZZ - ZZ         Static address to jump/call to. 
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ASMLib {
	/**
	 * List of instructions
	 */
	private Hashtable<Integer, Vector<AsmInstruction>> Prefixes = new Hashtable<Integer, Vector<AsmInstruction>>();
	public Hashtable<String, String> Labels = new Hashtable<String, String>();

	// storage class for the instruction.
	public class AsmInstruction {
		public String hex;
		public String asm;

		public AsmInstruction(String hex, String asm) {
			this.hex = hex;
			this.asm = asm;
		}
	}

	/**
	 * Load the opcode XML file.
	 */
	public ASMLib() {
		try {
			InputStream document = null;
			
			if (IsInJar()) {
				document = getClass().getResourceAsStream("/z80.xml");
			} else {
			   File file =  new File("src/resources","z80.xml");
			   document = new FileInputStream(file);	
			}			

			// Open file and build an XML doctree
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(document);
			doc.getDocumentElement().normalize();

			// Iterate all the opcode elements.
			NodeList nList = doc.getElementsByTagName("opcode");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					// load the current element to an object.
					Element eElement = (Element) nNode;
					String hex = eElement.getAttribute("hex");
					String cmd = eElement.getAttribute("cmd");
					AsmInstruction ais = new AsmInstruction(hex, cmd);

					/*
					 * add to the array of sorted instructions.
					 */
					// extract the first byte
					String prefix = hex.substring(0, 2);
					// if we have a multi-byte instruction, get the hash of the first two bytes
					// instead.
					if (prefix.equals("CB") || prefix.equals("ED") || prefix.equals("FD") || prefix.equals("DD")) {
						prefix = hex.substring(0, 4);
					}
					// Convert to an integer.
					Integer numPrefix = Integer.parseInt(prefix, 16);
					// Find or create the hashtable vector for the this prefix.
					Vector<AsmInstruction> v = Prefixes.get(numPrefix);
					if (v == null) {
						v = new Vector<AsmInstruction>();
					} else {
						Prefixes.remove(numPrefix);
					}
					// Add the new instruction and put it back on the list.
					v.add(ais);
					Prefixes.put(numPrefix, v);
				}
			}
			// Load the rom labels.
			if (IsInJar()) {
				document = getClass().getResourceAsStream("/romlabels.xml");
			} else {
			   File file =  new File("src/resources","romlabels.xml");
			   document = new FileInputStream(file);	
			}			
			
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(document);
			doc.getDocumentElement().normalize();
			// Iterate all the label elements.
			nList = doc.getElementsByTagName("label");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					// add it to the list.
					String address = eElement.getAttribute("address");
					String label = eElement.getTextContent();
					Labels.put(address.toUpperCase(), label);
				}
			}

		} catch (IOException e) {
			System.out.println("Error initialising disassembly libarary. ");
			System.out.println("IOException: " + e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Error initialising disassembly libarary. ");
			System.out.println("SAXexception: " + e.getMessage());
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.out.println("Error initialising disassembly libarary. ");
			System.out.println("ParserConfigurationException: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * This class contains the returned values for the instruction.
	 *
	 */
	public class DecodedASM {
		public String instruction;
		public int length;
		public int instructions[] = null;
		public int dataref;
		public int addressref;
		public int data;
		public int disp;

		public String toString() {
			String result = instruction + " Len:" + String.valueOf(length) + " dataref:" + String.valueOf(dataref)
					+ " addressref:" + String.valueOf(addressref) + " data:" + String.valueOf(data) + " disp:"
					+ String.valueOf(disp);
			return (result);
		}
	}

	/**
	 * Decode a given array of ints. For safety, should pass up to 5. Even if the
	 * later ones are zero.
	 * 
	 * @param data
	 * @param address
	 * @return DecodedASM structure including text and decoded addresses / data
	 */
	public DecodedASM decode(int data[], int address) {
		DecodedASM result = new DecodedASM();
		// Extract the first digit (Or first two if its a prefixed instruction)
		int prefix = data[0];
		if (prefix == 0xCB || prefix == 0xED || prefix == 0xFD || prefix == 0xDD) {
			prefix = (prefix * 0x100) + data[1];
		}
		Vector<AsmInstruction> possibilities = Prefixes.get(prefix);

		// Iterate the returned possibilities. THis will be 1 possible opcode for
		// most prefixes.
		for (AsmInstruction instruction : possibilities) {
			String digits = instruction.hex;
			boolean found = true;
			int addressref = 0;
			int dataref = 0;
			int index = 0;
			int dat = 0;
			boolean firstdigit = true;

			// System.out.println(digits+" "+new Integer(digits.length()).toString());
			String lbl = null;
			if (data.length >= (digits.length() / 2)) {
				for (int i = 0; i < digits.length(); i = i + 2) {
					String digit = digits.substring(i, i + 2);
					if (digit.equals("HH")) { // IX/IY displacement
						// can just match whatever
						int rel = data[i / 2];
						if (rel > 127) {
							rel = rel - 256;
						}
						index = rel;
					} else if (digit.equals("NN")) { // Number
						if (firstdigit) {
							dat = data[i / 2];
						} else {
							dat = dat + (data[i / 2] * 256);
							//GDS 19/06/2018 - Dont apply labels for static numbers. 
							//lbl = Labels.get(Hex(dat, 4).toUpperCase().substring(0, 4));
						}
						firstdigit = false;
					} else if (digit.equals("LL")) { // Address fetch
						if (firstdigit) {
							dat = data[i / 2];
						} else {
							dat = dat + (data[i / 2] * 256);
							lbl = Labels.get(Hex(dat, 4).toUpperCase().substring(0, 4));
						}
						firstdigit = false;
					} else if (digit.equals("GG")) { // Relative jump
						int rel = data[i / 2];
						if (rel > 127) {
							rel = rel - 256;
						}
						addressref = address + (digits.length() / 2) + rel;
					} else if (digit.equals("ZZ")) { // Static jump
						if (firstdigit) {
							addressref = data[i / 2];
						} else {
							addressref = addressref + (data[i / 2] * 256);
						}
						firstdigit = false;
						lbl = Labels.get(Hex(addressref, 4).toUpperCase().substring(0, 4));
					} else {
						int value = Integer.parseInt(digit, 16);
						if (value != data[i / 2]) {
							found = false;
						}
					}
				} // for
				if (found) {
					result.length = instruction.hex.length() / 2;
					result.addressref = addressref;
					result.dataref = dataref;
					result.data = dat;
					result.disp = index;
					if (lbl != null) {
						result.instruction = instruction.asm.replace("nn", lbl).replaceAll("n", Hex(dat, 2))
								.replaceAll("ZZZZ", lbl).replaceAll("GG", lbl);
					} else {
						result.instruction = instruction.asm.replace("nn", Hex(dat, 4)).replaceAll("n", Hex(dat, 2))
								.replaceAll("ZZZZ", Hex(addressref, 4))
								.replaceAll("GG", Integer.toHexString(addressref));
					}
					if (result.instruction.contains("+d")) {
						result.instruction = result.instruction.replaceAll("d", Integer.toHexString(Math.abs(index)));
						if (index < 0) {
							result.instruction = result.instruction.replace('+', '-');
						}
					}
					return (result);
				}
			}
		}

		return result;
	}

	/**
	 * Convert a given integer to a text HEX string of the given length.
	 * 
	 * @param i
	 *            - value
	 * @param sz
	 *            - byte size
	 * @return
	 */
	public String Hex(int i, int sz) {
		String result = Integer.toHexString(i);
		while (result.length() < sz) {
			result = "0" + result;
		}
		return (result + "h");
	}

	/**
	 * Test method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ASMLib z = new ASMLib();

		System.out.println(z.decode(new int[] { 0xFF, 0x00, 0x00 }, 0x4088).toString());
		System.out.println(z.decode(new int[] { 0xDD, 0xCB, 0x33 }, 0x4088).toString());
		System.out.println(z.decode(new int[] { 0xcd, 0xf5, 0x08 }, 0x408B).toString());
		System.out.println(z.decode(new int[] { 0x11, 0xc0, 0x01 }, 0x408E).toString());
		System.out.println(z.decode(new int[] { 0x3e, 0x80 }, 0x4091).toString());
		System.out.println(z.decode(new int[] { 0xd7 }, 0x4093).toString());
		System.out.println(z.decode(new int[] { 0x1b }, 0x4094).toString());
		System.out.println(z.decode(new int[] { 0x7a }, 0x4095).toString());
		System.out.println(z.decode(new int[] { 0xb3 }, 0x4096).toString());
		System.out.println(z.decode(new int[] { 0x20, 0xf8 }, 0x4097).toString());
		System.out.println(z.decode(new int[] { 0x20, 0xf8 }, 0x4097).toString());
		System.out.println(z.decode(new int[] { 0x3A, 0x10, 0x10 }, 0x4097).toString());
		System.out.println(z.decode(new int[] { 0xDD, 0x70, 0x10 }, 0x4097).toString());
		System.out.println(z.decode(new int[] { 0xDD, 0x77, 0xFE }, 0x4097).toString());

	}

	/**
	 * returns TRUE is running from a JAR file. (This affects where to find some of the files)
	 * @return
	 */
	public boolean IsInJar() {
		@SuppressWarnings("rawtypes")
		Class me = getClass();
		return (me.getResource( me.getSimpleName()+".class").toString().startsWith("jar:"));
	}

	
}
