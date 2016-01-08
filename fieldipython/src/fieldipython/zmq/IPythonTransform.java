package fieldipython.zmq;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

public class IPythonTransform {

//	static public final String PROFILE_DIRECTORY = System.getProperty("user.home") + "/.ipython/profile_default/security/";
	static public final String PROFILE_DIRECTORY = System.getProperty("user.home") + "/Library/Jupyter/runtime/";

	public IPythonTransform() {
	}

	public IPythonInterface get(String filename) {
		String f = filename;
		if (new File(f).exists())
			return _getFromFile(f);

		f = System.getProperty("user.home") + "/.ipython/profile_default/security/" + filename;
		if (new File(f).exists())
			return _getFromFile(f);

		f = System.getProperty("user.home") + "/.ipython/profile_default/security/kernel-" + filename + ".json";
		if (new File(f).exists())
			return _getFromFile(f);

		return null;
	}

	public IPythonInterface get() {
		File[] ff = new File(PROFILE_DIRECTORY).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".json");
			}
		});
		if (ff == null)
			return null;

		Arrays.sort(ff, new Comparator<File>() {

			@Override
			public int compare(File arg0, File arg1) {
				return -Long.compare(arg0.lastModified(), arg1.lastModified());
			}
		});

		System.out.println(" sorted files to be :" + Arrays.asList(ff));

		if (ff.length == 0)
			return null;
		
		return _getFromFile(ff[0].getAbsolutePath());

		
	}

	private IPythonInterface _getFromFile(String absolutePath) {

		try (BufferedReader b = new BufferedReader(new FileReader(absolutePath)))
		{
			
			String all = "";
			while (b.ready())
				all += b.readLine() + "\n";

			JSONObject o = new JSONObject(all);

			String ip = (String) o.get("ip");
			int shellPort = ((Number) o.get("shell_port")).intValue();
			int iopubPort = ((Number) o.get("iopub_port")).intValue();

			System.out.println(" info :"+ip+" "+shellPort+" "+iopubPort);
			
			IPythonInterface ii = new IPythonInterface();
			ii.initInput("tcp://" + ip + ":" + iopubPort);
			ii.initOutput("tcp://" + ip + ":" + shellPort);
			
			return makeShim(ii);
		} 
		catch (Exception e) {
			System.out.println(" trouble parsing json file <" + absolutePath + ">");
			System.out.println(" exception is :"+e);
			e.printStackTrace();
			return null;
		}
	}

	private IPythonInterface makeShim(IPythonInterface ii) {
		return ii;
	}

}
