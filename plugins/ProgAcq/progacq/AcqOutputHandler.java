package progacq;

import org.json.JSONObject;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public interface AcqOutputHandler {
	/**
	 * Gets the current state of the acquisition sequence as represented by an
	 * ImagePlus. This can be called in the middle of acquisition, prior to
	 * finalize (specifically if acquisition is interrupted).
	 *
	 * @return an ImagePlus representing the acquisition
	 * @throws Exception
	 */
	public abstract ImagePlus getImagePlus() throws Exception;

	/**
	 * Handle the next slice as output by the acquisition code. What this means
	 * obviously depends largely on implementation.
	 *
	 * @param ip an ImageProcessor holding the pixels
	 * @param meta a JSONObject containing metadata (including the state of all
	 * 	relevant devices).
	 * @throws Exception
	 */
	public abstract void processSlice(ImageProcessor ip, JSONObject meta) throws Exception;

	/**
	 * A stack has finished being acquired; react accordingly.
	 *
	 * @param depth The dimension along which the stack has been finished.
	 * @throws Exception
	 */
	public abstract void finalizeStack(int depth) throws Exception;

	/**
	 * The acquisition has ended; do any clean-up and finishing steps (such as
	 * saving the collected data to a file). IMPORTANT: After a call to finalize
	 * the handler should be in a state where it can accept new slices as an
	 * entirely different acquisition.
	 *
	 * @throws Exception
	 */
	public abstract void finalizeAcquisition() throws Exception;
}
