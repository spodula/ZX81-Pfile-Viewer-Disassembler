package viewer;

/**
 * G Salkin Jan 2018
 * 
 * User interface.    
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;


class BrowserMainForm {
	private boolean AddressIsFile = false;

	Display display = null;
	Shell shell = null;
	PFileParser CurrentFile = null;
	Text PFile = null; 
	Browser browser = null;

	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	
	public void MakeForm() {
		display = new Display();
		shell = new Shell(display);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns=3;
		shell.setLayout(gridLayout);
		shell.setText("ZX81 P file parser");

		ToolBar toolbar = new ToolBar(shell, SWT.NONE);
		ToolItem itemLoadFile = new ToolItem(toolbar, SWT.PUSH);itemLoadFile.setText("Load");
		ToolItem itemSaveFile = new ToolItem(toolbar, SWT.PUSH);itemSaveFile.setText("Save");
		ToolItem itemInformation = new ToolItem(toolbar, SWT.PUSH);itemInformation.setText("Information");
		ToolItem itemSYSVARS = new ToolItem(toolbar, SWT.PUSH);itemSYSVARS.setText("SysVars");
		ToolItem itemDisplay = new ToolItem(toolbar, SWT.PUSH);itemDisplay.setText("Display");
		ToolItem itemBasic = new ToolItem(toolbar, SWT.PUSH);itemBasic.setText("BASIC");
		ToolItem itemVariables = new ToolItem(toolbar, SWT.PUSH);itemVariables.setText("Variables");
		ToolItem itemDisassRem = new ToolItem(toolbar, SWT.PUSH);itemDisassRem.setText("Disassemble REM");
		ToolItem itemDisassAll = new ToolItem(toolbar, SWT.PUSH);itemDisassAll.setText("Disassemble All");
		ToolItem itemAddressIsFile = new ToolItem(toolbar, SWT.CHECK);itemAddressIsFile.setText("Load address/file address");

		GridData data = new GridData();
		data.horizontalSpan = 3;
		toolbar.setLayoutData(data);

		// current file.
		CurrentFile = new PFileParser();
		
		Label labelAddress = new Label(shell, SWT.NONE);
		labelAddress.setText("Filename");

		PFile = new Text(shell, SWT.BORDER);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		PFile.setLayoutData(data);
		
		try {
			browser = new Browser(shell, SWT.NONE);
		} catch (SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			display.dispose();
			return;
		}
		
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);
		
		final Label status = new Label(shell, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		status.setLayoutData(data);
		
		/* event handling */		
		itemLoadFile.addListener(SWT.Selection, event -> {
			FileDialog fd = new FileDialog(shell, SWT.OPEN);
	        fd.setText("Open P File");
	        fd.setFilterPath("C:/");
	        String[] filterExt = { "*.P", "*.*" };
	        fd.setFilterExtensions(filterExt);
	        String selected = fd.open();
	        System.out.println(selected);
	        if (selected!=null && !selected.isBlank()) {
	        	LoadFile(selected);
	        }
		});
		
		itemSaveFile.addListener(SWT.Selection, event -> {
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
	        fd.setText("Save Disassembled P File as HTML");
	        fd.setFilterPath("C:/");
	        fd.setFileName(PFile.getText()+".html");
	        String selected = fd.open();
	        System.out.println(selected);
	        if (selected!=null && !selected.isBlank()) {
	        	SaveFile(selected);
	        }
		});

		itemInformation.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_SUMMARY,AddressIsFile));
		});

		itemSYSVARS.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_SYSVARS,AddressIsFile));
		});

		itemDisplay.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_DISPLAY,AddressIsFile));
		});

		itemBasic.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_PROGRAM,AddressIsFile));
		});

		itemVariables.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_VARIABLES,AddressIsFile));
		});
		
		itemDisassRem.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_ASM_DATA,AddressIsFile));
		});
		
		itemDisassAll.addListener(SWT.Selection, event -> {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_ASM_REM,AddressIsFile));
		});
		
		itemAddressIsFile.addListener(SWT.Selection, event -> {
			AddressIsFile = itemAddressIsFile.getSelection(); 
			browser.setText(CurrentFile.RedoPrevious(AddressIsFile));
		});
	}

	/**
	 * 
	 * @param filename
	 */
	public void LoadFile(String filename) {
		if (PFile != null) {
			PFile.setText(filename);
		}
		CurrentFile.Load(new File(filename));
		if (browser != null) {
			browser.setText(CurrentFile.ProcessEntry(PFileParser.PAGE_SUMMARY, AddressIsFile));
		} 
	}

	/**
	 * 
	 * @param filename
	 */
	public void SaveFile(String filename) {
		// save file.
		File file = new File(filename);
		PrintWriter writer;
		try {
			writer = new PrintWriter(file, "UTF-8");
			writer.write("<html>\r\n");
			writer.write("<head>\r\n");
			writer.write(" <title>Contents of file " + file.getName() + "</title>");
			writer.write("</head>\r\n");
			writer.write("<body>\r\n");
			writer.write("<h1>Contents:</h1>");
			writer.write("<ul><li><a href=\"#1\">Summary</a></li>\r\n");
			writer.write("<li><a href=\"#2\">System Variables</a></li>\r\n");
			writer.write("<li><a href=\"#3\">Display area</a></li>\r\n");
			writer.write("<li><a href=\"#4\">BASIC Program</a></li>\r\n");
			writer.write("<li><a href=\"#5\">BASIC Variables</a></li>\r\n");
			writer.write("<li><a href=\"#6\">Rem disassembly (Trying to identify Data)</a></li>\r\n");
			writer.write("<li><a href=\"#7\">Rem disassembly (All)</a></li></ul>\r\n");

			for (int i = 1; i < 8; i++) {
				writer.write("<a name=\"" + String.valueOf(i) + "\">");
				writer.write(CurrentFile.ProcessEntry(i, AddressIsFile));
			}
			writer.write("</body>\r\n");
			writer.write("</html>\r\n");

			writer.close();
		} catch (FileNotFoundException e1) {
			if (browser != null) {
				browser.setText("Cant save file!" + e1.getMessage());
			}
			System.out.println("Cant save file!" + e1.getMessage());
		} catch (UnsupportedEncodingException e) {
			if (browser != null) {
				browser.setText("Cant save file!" + e.getMessage());
			}
			System.out.println("Cant save file!" + e.getMessage());
			
		}
	}
}