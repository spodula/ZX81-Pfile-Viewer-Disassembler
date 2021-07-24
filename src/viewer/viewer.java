package viewer;
/**
 * G Salkin Jan 2018
 * 
 * zx81 P file viewer.    
 */


public class viewer {
	private static String loadfile = "";
	private static String savefile = "";

	public static void start(String[] args) {
		// create the scene
		BrowserMainForm bmf = new BrowserMainForm();
		bmf.MakeForm();

		if (!loadfile.isEmpty()) {
			System.out.println("Loading: " + loadfile);
			bmf.LoadFile(loadfile);
		}
		if (!savefile.isEmpty()) {
			System.out.println("Saving: " + savefile);
			bmf.SaveFile(savefile);
		} else {
			bmf.loop();
		}

	}

	public static void main(String[] args) {
		if (args.length < 3) {
			if (args.length > 0) {
				loadfile = args[0];
				if (args.length > 1) {
					savefile = args[1];
				}
			}
		} else {
			System.out.println("Too many args. Expecting [load] [save]");
		}
		start(args);
	}
}
