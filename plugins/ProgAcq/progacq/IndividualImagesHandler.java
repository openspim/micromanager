package progacq;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import org.json.JSONException;
import org.json.JSONObject;

public class IndividualImagesHandler implements AcqOutputHandler {
	private File outputDirectory;
	private String namingScheme;
	
	/**
	 * Creates a nameScheme string from a list of 'short' names. Only applies to
	 * saveIndividually.
	 * 
	 * @param header Prefixed onto the string.
	 * @param t	whether or not to include time in filename
	 * @param nameMap map of short names for devices to be in the filename.
	 * @return the generated scheme (is also saved to this object!)
	 */
	public static String shortNamesToScheme(String header, boolean t, String[] devices, Map<String, String> nameMap) {
		String nameScheme = header + (t ? "-t=$(t)" : "-");

		for(String dev : devices)
			nameScheme += "-" + (nameMap == null ? dev : nameMap.get(dev)) + "=$(" + dev + ")";

		nameScheme += ".tif";

		return nameScheme;
	}


	public IndividualImagesHandler(File directory, String scheme) {
		outputDirectory = directory;
		if(!outputDirectory.exists() || !outputDirectory.isDirectory())
			throw new IllegalArgumentException("Invalid path or not a directory: " + directory.getAbsolutePath());
	
		namingScheme = scheme;
	}

	@Override
	public void processSlice(ImageProcessor ip, double X, double Y, double Z, double theta, double deltaT)
			throws Exception {
		String name = String.format("spim_TL%04f_Angle%f", deltaT, theta); //nameImage(meta);
		ImagePlus imp = new ImagePlus(name, ip);
		
		imp.setProperty("Info", X + "/" + Y + "/" + Z + ", " + theta + " @ " + deltaT + "s");
		
		IJ.save(imp, new File(outputDirectory, name).getAbsolutePath());
	}
/*
	private String nameImage(JSONObject metaData) throws JSONException {
		String result = new String(namingScheme);

		Iterator<String> iter = metaData.keys();
		while(iter.hasNext()) {
			String mde = iter.next();
			result = result.replace("$(" + mde + ")", metaData.getString(mde));
		}

		return result;
	}
*/
	@Override
	public void finalizeAcquisition() throws Exception {
		// Nothing to do.
	}

	@Override
	public ImagePlus getImagePlus() throws Exception {
		IJ.run("QuickPALM.Run_MyMacro", "Fast_VirtualStack_Opener.txt"); // TODO: Invoke the Open Virtual Stack process.

		return IJ.getImage();
	}


	@Override
	public void finalizeStack(int depth) throws Exception {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void beginStack(int axis) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
