package viewer;
/**
 * G Salkin Jan 2018
 * 
 * Wrapper around the P File pages.   
 * Probably not the best way of doing it, but it simplifies remembering what the last page was
 * when you change the address type. 
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
//import java.util.Date;

import viewer.libs.MemWrapper;
import viewer.pages.AsmPage;
import viewer.pages.DisplayPage;
import viewer.pages.ProgramAreaPage;
import viewer.pages.SummaryPage;
import viewer.pages.SystemVariablesPage;
import viewer.pages.VariablesPage;

public class PFileParser {
	private MemWrapper pFile = new MemWrapper();
	private File lastFileLoaded = null;

	private SummaryPage summ = null;
	private ProgramAreaPage prog = null;
	private DisplayPage disp = null;
	private SystemVariablesPage sysvars = null;
	private VariablesPage progvars = null;
	private AsmPage asm = null;

	public static int PAGE_SUMMARY = 1;
	public static int PAGE_SYSVARS = 2;
	public static int PAGE_DISPLAY = 3;
	public static int PAGE_PROGRAM = 4;
	public static int PAGE_VARIABLES = 5;
	public static int PAGE_ASM_DATA = 6;
	public static int PAGE_ASM_REM = 7;

	private int lastid = -1;

	/**
	 * Load the given file and populate the page generation objects
	 * 
	 * @param file
	 */
	public void Load(File file) {
		try {
			if (file.exists()) {
				lastFileLoaded = file;
				pFile = new MemWrapper(file);
				summ = new SummaryPage(pFile);
				prog = new ProgramAreaPage(pFile);
				disp = new DisplayPage(pFile);
				sysvars = new SystemVariablesPage(pFile);
				progvars = new VariablesPage(pFile);
				asm = new AsmPage(pFile);
			}
		} catch (FileNotFoundException e) {
			System.out.println("File " + file.getAbsolutePath() + " not found!");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Return the given page.
	 * 
	 * @param PageID
	 * @param AddressIsFile
	 * @return
	 */
	public String ProcessEntry(int PageID, boolean AddressIsFile) {
		//Date start = new Date();
		String result = "";
		if (lastFileLoaded != null) {
			switch (PageID) {
			case 1:
				result = summ.Get(lastFileLoaded, AddressIsFile);
				break;
			case 2:
				result = sysvars.get(AddressIsFile);
				break;
			case 3:
				result = disp.get(AddressIsFile);
				break;
			case 4:
				result = prog.Get(AddressIsFile);
				break;
			case 5:
				result = progvars.get(AddressIsFile);
				break;
			case 6:
				result = asm.get(false, AddressIsFile);
				break;
			case 7:
				result = asm.get(true, AddressIsFile);
				break;
			}
			lastid = PageID;
		}
		//Date enddate = new Date();
		//System.out.println("Dt = " + new Long(enddate.getTime() - start.getTime()).toString() + "ms");
		return (result);
	}

	/**
	 * Return the last page displayed or the summary page if its not been set yet.
	 * (Used for when you change the address type.
	 * 
	 * @param AddressIsFile
	 * @return
	 */
	public String RedoPrevious(boolean AddressIsFile) {
		String result = "";
		if (lastid != -1) {
			result = ProcessEntry(lastid, AddressIsFile);
		} else {
			result = ProcessEntry(PAGE_SUMMARY, AddressIsFile);
		}
		return (result);
	}

}
