package progacq;

import org.json.JSONObject;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public interface AcqOutputHandler {
	public abstract ImagePlus getImagePlus() throws Exception;
	public abstract void ProcessSlice(ImageProcessor ip, JSONObject meta) throws Exception;
	public abstract void Finalize() throws Exception;
}
