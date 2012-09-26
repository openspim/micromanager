package progacq;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import org.json.JSONObject;

public class OutputAsStackHandler implements AcqOutputHandler {
	private ImageStack stack;
	private ImagePlus img;

	public OutputAsStackHandler() {
		stack = null;
		img = null;
	}

	@Override
	public void processSlice(ImageProcessor ip, JSONObject meta)
			throws Exception {
		if(stack == null)
			stack = new ImageStack(ip.getWidth(), ip.getHeight());

		stack.addSlice("t=" + meta.getString("t"), ip);
	}

	@Override
	public void finalize() throws Exception {
		img = new ImagePlus("SimpleOutput", stack);
	}

	@Override
	public ImagePlus getImagePlus() throws Exception {
		return (img != null ? img : new ImagePlus("SimpleOutput", stack));
	}
}
