package progacq;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import javax.swing.event.ChangeListener;

import mmcorej.CMMCore;

public class AcqParams {
	private CMMCore			core;
	private String[]		stepDevices;
	private List<String[]>	steps;

	private double			timeStepSeconds;
	private int				timeSeqCount;

	private boolean			continuous;
	private boolean			saveIndividual;

	private File			outputDirectory;

	private ChangeListener	progressListener;

	private String[]		metaDevices;

	private String			nameScheme;

	public AcqParams() {
		this(null, null, null, 0D, 0, false, null, null, false, null);
	}

	public AcqParams(CMMCore icore, String[] idevices, List<String[]> isteps) {
		this(icore, idevices, isteps, 0D, 1, false, null, idevices, false, null);
	}

	public AcqParams(CMMCore core, String[] devs, List<String[]> steps, double deltat, int count)
	{
		this(core, devs, steps, deltat, count, false, null, devs, false, null);
	}

	public AcqParams(CMMCore iCore, String[] iDevices, List<String[]> iSteps,
			double iTimeStep, int iTimeSeqCnt, boolean iContinuous,
			ChangeListener iListener, String[] iMetaDevices, boolean saveIndv,
			File rootDir) {

		core = iCore;
		stepDevices = iDevices;
		steps = iSteps;
		timeStepSeconds = iTimeStep;
		timeSeqCount = iTimeSeqCnt;
		continuous = iContinuous;
		progressListener = iListener;
		metaDevices = iMetaDevices;
		saveIndividual = saveIndv;

		if(saveIndividual && rootDir == null)
			throw new IllegalArgumentException("Must specify a directory.");

		if(saveIndividual && !rootDir.isDirectory())
			throw new IllegalArgumentException(rootDir.getPath() + " is not a directory.");

		outputDirectory = rootDir;

		generateDefaultNameScheme();
	}

	private void generateDefaultNameScheme() {
		nameScheme = "pa-t=%t-";

		for(int i=0; i < stepDevices.length; ++i)
			nameScheme += stepDevices[i] + "=%" + i + "-";
	}

	/**
	 * Creates a nameScheme string from a list of 'short' names. Only applies to
	 * saveIndividually.
	 * 
	 * @param header Prefixed onto the string.
	 * @param t	whether or not to include time in filename
	 * @param nameMap map of short names for devices to be in the filename.
	 * @return the generated scheme (is also saved to this object!)
	 */
	public String shortNamesToScheme(String header, boolean t, Map<String, String> nameMap) {
		nameScheme = header + (t ? "-t=%t" : "-");

		for(int i=0; i < stepDevices.length; ++i)
			nameScheme += "-" + nameMap.get(stepDevices[i]) + "=%" + i;

		nameScheme += ".tif";

		return nameScheme;
	}

	/**
	 * @return the nameScheme
	 */
	public String getNameScheme() {
		return nameScheme;
	}

	/**
	 * @param nameScheme the nameScheme to set
	 */
	public void setNameScheme(String nameScheme) {
		this.nameScheme = nameScheme;
	}

	/**
	 * @return the core
	 */
	public CMMCore getCore() {
		return core;
	}

	/**
	 * @param core the core to set
	 */
	public void setCore(CMMCore core) {
		this.core = core;
	}

	/**
	 * @return the stepDevices
	 */
	public String[] getStepDevices() {
		return stepDevices;
	}

	/**
	 * @param stepDevices the stepDevices to set
	 */
	public void setStepDevices(String[] stepDevices) {
		this.stepDevices = stepDevices;
	}

	/**
	 * @return the steps
	 */
	public List<String[]> getSteps() {
		return steps;
	}

	/**
	 * @param steps the steps to set
	 */
	public void setSteps(List<String[]> steps) {
		this.steps = steps;
	}

	/**
	 * @return the timeStepSeconds
	 */
	public double getTimeStepSeconds() {
		return timeStepSeconds;
	}

	/**
	 * @param timeStepSeconds the timeStepSeconds to set
	 */
	public void setTimeStepSeconds(double timeStepSeconds) {
		this.timeStepSeconds = timeStepSeconds;
	}

	/**
	 * @return the timeSeqCount
	 */
	public int getTimeSeqCount() {
		return timeSeqCount;
	}

	/**
	 * @param timeSeqCount the timeSeqCount to set
	 */
	public void setTimeSeqCount(int timeSeqCount) {
		this.timeSeqCount = timeSeqCount;
	}

	/**
	 * @return the continuous
	 */
	public boolean isContinuous() {
		return continuous;
	}

	/**
	 * @param continuous the continuous to set
	 */
	public void setContinuous(boolean continuous) {
		this.continuous = continuous;
	}

	/**
	 * @return the progressListener
	 */
	public ChangeListener getProgressListener() {
		return progressListener;
	}

	/**
	 * @param progressListener the progressListener to set
	 */
	public void setProgressListener(ChangeListener progressListener) {
		this.progressListener = progressListener;
	}

	/**
	 * @return the metaDevices
	 */
	public String[] getMetaDevices() {
		return metaDevices;
	}

	/**
	 * @param metaDevices the metaDevices to set
	 */
	public void setMetaDevices(String[] metaDevices) {
		this.metaDevices = metaDevices;
	}

	/**
	 * @return the saveIndividual
	 */
	public boolean isSaveIndividual() {
		return saveIndividual;
	}

	/**
	 * @param saveIndividual the saveIndividual to set
	 */
	public void setSaveIndividual(boolean saveIndividual) {
		this.saveIndividual = saveIndividual;
	}

	/**
	 * @return the outputDirectory
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @param outputDirectory the outputDirectory to set
	 */
	public void setOutputDirectory(File outputDirectory) {
		if(outputDirectory != null && !outputDirectory.isDirectory())
			throw new IllegalArgumentException("Not a directory.");

		this.outputDirectory = outputDirectory;
	}
}
