package viewer.libs;

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

/**
 * G Salkin Feb 2018
 * 
 * This is basically a storage object for the system variable definitions.
 */

public class SystemVariables {
	public static int SYSVAR_BYTE   = 1;
	public static int SYSVAR_WORD   = 2;
	public static int SYSVAR_COORDS = 3;
	public static int SYSVAR_FLAGS  = 4;
	public static int SYSVAR_PRBUFF = 5;

	public static int VAR_D_FILE = 16396;
	public static int VAR_VARS = 16400;
	public static int VAR_E_LINE = 16404;
	public static int VAR_STKBOT = 16410;
	public static int VAR_STKEND = 16412;

	/**
	 * Array of flag meanings, idx1 = bit number (0-7), idx2 = 0=unset 1=set
	 */
	public Hashtable<String, String[][]> FlagList = new Hashtable<String, String[][]>();

	/**
	 * Array of system variables. 
	 */
	public SystemVariable[] vars = null;

	/**
	 * Load the system variables from the sysvars.xml file.
	 */
	public SystemVariables() {
		try {
			/*
			 * Open the sysvars.xml file. 
			 */
			
			InputStream document = null;
			
			if (IsInJar()) {
				document = getClass().getResourceAsStream("/sysvars.xml");
			} else {
			File file =  new File("src/resources","sysvars.xml");
			   document = new FileInputStream(file);	
			}
			
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(document);
			doc.getDocumentElement().normalize();

			//temporary object for the system variables while loading them. 
			Vector<SystemVariable> store = new Vector<SystemVariable>();

			/*
			 * Load the sysvar definitions into the vars array.
			 */
			NodeList nList = doc.getElementsByTagName("sysvar");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					// load the current element to an object.
					Element eElement = (Element) nNode;
					String address = eElement.getAttribute("address");
					String clss = eElement.getAttribute("class");
					String abbrev = eElement.getAttribute("abbrev");
					String desc = eElement.getTextContent();
					int iAddress = Integer.parseInt(address, 16);
					//Convert the Class into an integer representation.  
					int iClass = SYSVAR_BYTE;
					if (clss.equals("WORD")) {
						iClass = SYSVAR_WORD;
					} else if (clss.equals("COORDS")) {
						iClass = SYSVAR_COORDS;
					} else if (clss.equals("FLAGS")) {
						iClass = SYSVAR_FLAGS;
					} else if (clss.equals("PRBUFF")) {
						iClass = SYSVAR_PRBUFF;
					}
					//set the variable. 
					SystemVariable sysvar = new SystemVariable(iAddress, iClass, abbrev, desc, abbrev);
					store.add(sysvar);
				}
			}
			//Convert the store object into a flat array.  
			vars = store.toArray(new SystemVariable[1]);
			
			/*
			 * Load the flag definitions into the FlagList array.
			 */
			nList = doc.getElementsByTagName("flag");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node FlagGroup = nList.item(temp);
				if (FlagGroup.getNodeType() == Node.ELEMENT_NODE) {
					// load the current element to an object.
					Element eElement = (Element) FlagGroup;
					String flagname = eElement.getAttribute("abbrev");
					NodeList Bits = FlagGroup.getChildNodes();

					String tmparray[][] = { { "", "" }, { "", "" }, { "", "" }, { "", "" }, { "", "" }, { "", "" },
							{ "", "" }, { "", "" } };
					//iterate the child nodes of type "bit"
					for (int elem = 0; elem < Bits.getLength(); elem++) {
						Node nBit = Bits.item(elem);
						if ((nBit.getNodeType() == Node.ELEMENT_NODE) && (nBit.getNodeName().equals("bit"))) {
							Element bit = (Element) nBit;
							String bitnum = bit.getAttribute("num");
							String falseval = bit.getAttribute("false");
							String trueval = bit.getAttribute("true");
							//Some basic sanity checking. 
							if (bitnum == null) {
								System.out.println(
										"Attribute 'num' on element 'bit' (" + flagname + ") must be present.");
							} else {
								int iBit = Integer.parseInt(bitnum);
								if (iBit > 7) {
									System.out.println("Only bit.num values between 0 and 7 allowed!");
								} else {
									//Can occur if the bit doesn't contain the true or false attributes. 
									if (falseval==null) {
										falseval = "";
									}
									if (trueval==null) {
										trueval = "";
									}
									//set them. 
									tmparray[iBit][0] = falseval;
									tmparray[iBit][1] = trueval;
								}
							}

						}
					}
					FlagList.put(flagname, tmparray);
				}
			}

		} catch (IOException e) {
			System.out.println("Error loading System variables: ");
			System.out.println("IOException: " + e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Error loading System variables: ");
			System.out.println("SAXexception: " + e.getMessage());
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.out.println("Error loading System variables: ");
			System.out.println("ParserConfigurationException: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error loading System variables: ");
			System.out.println(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Class for the returned value
	 *
	 */
	public class SystemVariable {
		public int address;
		public String name;
		public String desc;
		public String abbrev;
		public int sz;

		public SystemVariable(int addr, int size, String name, String desc, String abbrev) {
			this.address = addr;
			this.name = name;
			this.desc = desc;
			this.sz = size;
			this.abbrev = abbrev;
		}
	}

	public boolean IsInJar() {
		@SuppressWarnings("rawtypes")
		Class me = getClass();
		return (me.getResource( me.getSimpleName()+".class").toString().startsWith("jar:"));
	}
	
}
