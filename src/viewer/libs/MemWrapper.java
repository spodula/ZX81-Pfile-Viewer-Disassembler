package viewer.libs;

/**
 * G Salkin Jan 2018
 * 
 * This provides a wrapper around a loaded ".P" file. 
 * These files are just zx81 memory dump files, (Same as zx81 save files). They are loaded at $4009 onwards. 
 * 
 * This wrapper just loads the file and pretends its a bit of memory. 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MemWrapper {
	public static int ZX81_RAMSTART = 0x4000;
	public static int P_FILE_STARTDISP = 0x0009;

	private byte contents[] = null;

	public int GetLastRam() {
		return (contents.length + ZX81_RAMSTART - P_FILE_STARTDISP);
	}

	/**
	 * Load the file and do a little basic sanity checking.
	 * 
	 * @param pfile
	 * @throws IOException
	 */
	public MemWrapper(File pfile) throws FileNotFoundException, IOException {
		RandomAccessFile f = new RandomAccessFile(pfile, "r");
		try {
			int filelen = (int) f.length();
			if (filelen > 32768) {
				throw new IOException("File is too long to be a P file...");
			} else {
				contents = new byte[filelen];
				f.readFully(contents);
			}
		} finally {
			f.close();
		}
	}

	/**
	 * Returns TRUE if a file is loaded.
	 * 
	 * @return
	 */
	public boolean IsValid() {
		return (contents != null);
	}

	/**
	 * create a blank object.
	 * 
	 * @throws IOException
	 */
	public MemWrapper() {
	}

	/**
	 * Extract a byte from a given address.
	 * 
	 * @param memaddr
	 * @return
	 * @throws Exception
	 */
	public int GetByteAtMem(int memaddr) throws Exception {
		int realaddress = memaddr;
		memaddr = memaddr - ZX81_RAMSTART - P_FILE_STARTDISP;
		try {
			int i1 = (contents[memaddr] & 0xff);

			return (i1);
		} catch (Exception e) {
			int lastmem = contents.length + ZX81_RAMSTART - P_FILE_STARTDISP;
			throw new Exception("Illegal attempt to access a byte at " + String.valueOf(realaddress)
					+ " Actual last byte is " + String.valueOf(lastmem));
		}
	}

	/**
	 * Extract a DWORD from a given address.
	 * 
	 * @param memaddr
	 * @return
	 * @throws Exception
	 */
	public int GetWordAtMem(int memaddr) throws Exception {
		int addr = memaddr - ZX81_RAMSTART - P_FILE_STARTDISP;
		if (addr >= contents.length) {
			int lastmem = contents.length + ZX81_RAMSTART - P_FILE_STARTDISP;
			throw new Exception("Requested address: " + String.valueOf(memaddr)
					+ " is outside of the PFILE.  Actual last byte is " + String.valueOf(lastmem));
		}
		int i1 = (contents[addr] & 0xff);
		int i2 = (contents[addr + 1] & 0xff);
		int value = i1 + (i2 * 256);
		return (value);
	}

	/**
	 * Extract a zx81 5 digit floating point variable at the given address:
	 * 
	 * format is: |Exp|MSD|Digit|Digit|LSD|
	 * 
	 * Where EXP is the binary point location where $80 = 0 exp=0 is a special case
	 * meaning digit is 0 note the MSB of the MSD is the sign. This is also where
	 * the most significant digit goes, so we extract the sign, then we always treat
	 * that bit as 1.
	 * 
	 * @param memaddr
	 * @return
	 * @throws Exception
	 */
	public double GetFloatAtMem(int memaddr) throws Exception {
		int exponent = GetByteAtMem(memaddr);
		double fVal = 0;
		// exponent of zero is a special case. Always equals exactly zero.
		if (exponent != 0) {
			// extract the sign
			boolean sign = (GetByteAtMem(memaddr + 1) & 0x80) == 0x80;
			// Extract the 32 bit number, merging "1" in the MSB.
			long value = GetByteAtMem(memaddr + 1) | 0x80;
			value = value * 256 + GetByteAtMem(memaddr + 2);
			value = value * 256 + GetByteAtMem(memaddr + 3);
			value = value * 256 + GetByteAtMem(memaddr + 4);
			// Get the the exponent. Note 0x80=2^0
			exponent = exponent - 0x80;
			// output is a floating point value so transfer my long word there.
			fVal = value;
			// note, done like this in stages because Java treats these as 32 bit integers,
			// its too big to do on one go.
			fVal = fVal / 65536;
			fVal = fVal / 65536;
			// shift via the exponent... (2^exponent)
			fVal = fVal * Math.pow(2, exponent);
			// negate if required.
			if (sign) {
				fVal = -fVal;
			}
		}
		return (fVal);
	}

}
