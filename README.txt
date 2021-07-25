This is the PFILE viewer and disassembler (Java SWT version)
by Graham Salkin 2018-2021.

Changelog:
23 Jul 2021 - Modified to do away with the JAVAX dependancy so it will run on openJDK (To do this i used the SWT tooklit instead)
23 Jul 2021 - Got rid of the warnings that appeared when i upgraded to Java 11.

Description
This is a program to read and parse zx81 P files. (RAM dump files)
My addition to the Genere is that this one will try to disassemble REM statements for your amusement. Its reasonably successful.
This program should be runnable straight from the desktop as long as you have an up to date Java installed.
The source code has a maven script and should be easy to build and run.

Running
To run the program, you should just be able to double click on it.
If you run it from the command line, there are two possible parameters.
	Java -jar viewer.jar ["File to load"] ["File to save"]
If you run it with one parameter, this file will be loaded and shown.
If you run it with two parameters, the first file name will be loaded, then the HTML output will be written to the second file. The program will then terminate. (As it assumes you are running from a script)
Note that the form does open in the second case, it just gets closed again. I may remedy this so it can be run from non-gui environments (EG a web server)

Compiling
Simply copy the appropriate pom.xml (the default one is for Windows x86-64) install Maven and use the command: 
	mvn clean package
	

The only OS dependancy is the swt library, so you should be able to compile it for any OS and CPU that has a SWT port by just changing the entry
in the POM file. 
