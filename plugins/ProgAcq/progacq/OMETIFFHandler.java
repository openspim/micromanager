package progacq;

import java.io.File;
import java.util.UUID;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.json.JSONObject;
import org.micromanager.utils.ReportingUtils;

import loci.common.DataTools;
import loci.common.services.ServiceFactory;

import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import mmcorej.CMMCore;

import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;

public class OMETIFFHandler implements AcqOutputHandler {
	private File outputDirectory;
	private String[] xytzDevices;

	private IMetadata meta;
	private int imageCounter, sliceCounter;
	private IFormatWriter writer;

	private CMMCore core;
	private int stacks, timesteps;
	private AcqRow[] acqRows;
	private double deltat;

	public OMETIFFHandler(CMMCore iCore, File outDir, String xyDev,
			String cDev, String zDev, String tDev, AcqRow[] acqRows,
			int iTimeSteps, double iDeltaT) {

		if(outDir == null || !outDir.exists() || !outDir.isDirectory())
			throw new IllegalArgumentException("Null path specified: " + outDir.toString());

		xytzDevices = new String[] {xyDev, cDev, zDev, tDev};

		imageCounter = -1;
		sliceCounter = 0;

		stacks = acqRows.length;
		core = iCore;
		timesteps = iTimeSteps;
		deltat = iDeltaT;
		outputDirectory = outDir;
		this.acqRows = acqRows;

		try {
			meta = new ServiceFactory().getInstance(OMEXMLService.class).createOMEXMLMetadata(null);

			meta.createRoot();

			meta.setDatasetID(MetadataTools.createLSID("Dataset", 0), 0);

			//for(int image = 0; image < stacks*timesteps; ++image) {
			for (int image = 0; image < stacks; ++image) {
				meta.setImageID(MetadataTools.createLSID("Image", image), image);

				AcqRow row = acqRows[image];
				int depth = row.getDepth();

				//meta.setUUID((String) fileNamesAndUUIDs[image][1]);

				meta.setPixelsID(MetadataTools.createLSID("Pixels", 0), image);
				meta.setPixelsDimensionOrder(DimensionOrder.XYCZT, image);
				meta.setPixelsBinDataBigEndian(Boolean.FALSE, image, 0);
				meta.setPixelsType(core.getImageBitDepth() == 8 ? PixelType.UINT8 : PixelType.UINT16, image);
				meta.setChannelID(MetadataTools.createLSID("Channel", 0), image, 0);
				meta.setChannelSamplesPerPixel(new PositiveInteger(1), image, 0);

				for (int t = 0; t < timesteps; ++t) {
					String fileName = makeFilename(image, t);
					meta.setUUIDFileName(fileName, image, t);
					meta.setUUIDValue("urn:uuid:" + (String)UUID.nameUUIDFromBytes(fileName.getBytes()).toString(), image, t);

					meta.setTiffDataPlaneCount(new NonNegativeInteger(depth), image, t);
					meta.setTiffDataFirstC(new NonNegativeInteger(0), image, t);
					meta.setTiffDataFirstZ(new NonNegativeInteger(0), image, t);
					meta.setTiffDataFirstT(new NonNegativeInteger(t), image, t);
				};

				meta.setPixelsSizeX(new PositiveInteger((int)core.getImageWidth()), image);
				meta.setPixelsSizeY(new PositiveInteger((int)core.getImageHeight()), image);
				meta.setPixelsSizeZ(new PositiveInteger(depth), image);
				meta.setPixelsSizeC(new PositiveInteger(1), image);
				meta.setPixelsSizeT(new PositiveInteger(timesteps), image);

				meta.setPixelsPhysicalSizeX(new PositiveFloat(core.getPixelSizeUm()), image);
				meta.setPixelsPhysicalSizeY(new PositiveFloat(core.getPixelSizeUm()), image);
				meta.setPixelsPhysicalSizeZ(new PositiveFloat(1d), image);
				meta.setPixelsTimeIncrement(new Double(deltat), image);

			}

			writer = new ImageWriter().getWriter(makeFilename(0, 0));

			writer.setWriteSequentially(true);
			writer.setMetadataRetrieve(meta);
			writer.setInterleaved(false);
			writer.setValidBitsPerPixel((int) core.getImageBitDepth());
			writer.setCompression("Uncompressed");
			openWriter(0, 0);

			IJ.log(((OMEXMLMetadata)meta).dumpXML());
		} catch(Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	private static String makeFilename(int angleIndex, int timepoint) {
		return "spim_TL" + (timepoint + 1) + "_Angle" + angleIndex + ".ome.tiff";
	}

	private String makePath(int angleIndex, int timepoint) {
		return new File(outputDirectory, makeFilename(angleIndex, timepoint)).getAbsolutePath();

	}
	private void openWriter(int angleIndex, int timepoint) throws Exception {
		meta.setUUID(meta.getUUIDValue(angleIndex, timepoint));
		writer.changeOutputFile(makePath(angleIndex, timepoint));
		writer.setSeries(angleIndex);

		sliceCounter = 0;
	}

	@Override
	public ImagePlus getImagePlus() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void beginStack(int axis) throws Exception {
		ReportingUtils.logMessage("Beginning stack along dimension " + axis);

		if(++imageCounter < stacks * timesteps)
			openWriter(imageCounter % stacks, imageCounter / stacks);
	}

	@Override
	public void processSlice(ImageProcessor ip, double X, double Y, double Z, double theta, double deltaT)
			throws Exception {
		long bitDepth = core.getImageBitDepth();
		byte[] data = bitDepth == 8 ?
			(byte[])ip.getPixels() :
			DataTools.shortsToBytes((short[])ip.getPixels(), true);

		int image = imageCounter % stacks;
		int plane = (imageCounter / stacks)*acqRows[image].getDepth() + sliceCounter;

		meta.setPlanePositionX(X, image, plane);
		meta.setPlanePositionY(Y, image, plane);
		meta.setPlanePositionZ(Z, image, plane);
		meta.setPlaneTheZ(new NonNegativeInteger(sliceCounter+1), image, plane);
		meta.setPlaneTheT(new NonNegativeInteger(imageCounter / stacks + 1), image, plane);
		meta.setPlaneDeltaT(deltaT, image, plane);
		meta.setPlaneAnnotationRef(X + "/" + Y + "/" + Z, image, plane, 0);
		writer.saveBytes(image, data);

		++sliceCounter;
	}

	@Override
	public void finalizeStack(int depth) throws Exception {
		ReportingUtils.logMessage("Finished stack along dimension " + depth);
	}

	@Override
	public void finalizeAcquisition() throws Exception {
		if(writer != null)
			writer.close();

		ReportingUtils.logMessage("" + imageCounter + " vs " + stacks);
		imageCounter = 0;

		writer = null;

	}
}
