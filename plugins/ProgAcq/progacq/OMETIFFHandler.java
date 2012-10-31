package progacq;

import java.io.File;

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
	private int[] stackDepths;
	private double deltat;

	public OMETIFFHandler(CMMCore iCore, File outDir, String xyDev,
			String cDev, String zDev, String tDev, int[] iStackDepths,
			int iTimeSteps, double iDeltaT) {

		if(outDir == null || !outDir.exists() || !outDir.isDirectory())
			throw new IllegalArgumentException("Null path specified: " + outDir.toString());

		xytzDevices = new String[] {xyDev, cDev, zDev, tDev};

		imageCounter = sliceCounter = 0;

		stacks = iStackDepths.length;
		stackDepths = iStackDepths;
		core = iCore;
		timesteps = iTimeSteps;
		deltat = iDeltaT;
		outputDirectory = outDir;

		try {
			meta = new ServiceFactory().getInstance(OMEXMLService.class).createOMEXMLMetadata(null);

			meta.createRoot();

			meta.setDatasetID(MetadataTools.createLSID("Dataset", 0), 0);

			for(int image = 0; image < stacks; ++image) {
				meta.setImageID(MetadataTools.createLSID("Image", image), image);
				meta.setPixelsID(MetadataTools.createLSID("Pixels", 0), image);
				meta.setPixelsDimensionOrder(DimensionOrder.XYCZT, image);
				meta.setPixelsBinDataBigEndian(Boolean.FALSE, 0, image);
				meta.setPixelsType(core.getImageBitDepth() == 8 ? PixelType.UINT8 : PixelType.UINT16, image);
				meta.setChannelID(MetadataTools.createLSID("Channel", 0), 0, image);
				meta.setChannelSamplesPerPixel(new PositiveInteger(1), 0, image);

				meta.setPixelsSizeX(new PositiveInteger((int)core.getImageWidth()), image);
				meta.setPixelsSizeY(new PositiveInteger((int)core.getImageHeight()), image);
				meta.setPixelsSizeZ(new PositiveInteger(stackDepths[image % stacks]), image);
				meta.setPixelsSizeC(new PositiveInteger(1), image);
				meta.setPixelsSizeT(new PositiveInteger(timesteps), image);

				meta.setPixelsPhysicalSizeX(new PositiveFloat(core.getPixelSizeUm()), image);
				meta.setPixelsPhysicalSizeY(new PositiveFloat(core.getPixelSizeUm()), image);
				meta.setPixelsPhysicalSizeZ(new PositiveFloat(1d), image);
				meta.setPixelsTimeIncrement(new Double(deltat), image);
			}
		} catch(Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	private void openWriter(int timepoint, double theta) throws Exception {
		File path = new File(outputDirectory,
				"spim_TL" + timepoint + "_Angle" + theta + ".ome.tiff"
		);

		writer = new ImageWriter().getWriter(path.getAbsolutePath());

		writer.setWriteSequentially(true);
		writer.setMetadataRetrieve(meta);
		writer.setInterleaved(false);
		writer.setValidBitsPerPixel((int) core.getImageBitDepth());
		writer.setCompression("Uncompressed");
		writer.setId(path.getAbsolutePath());
		writer.setSeries(++imageCounter % stacks);

		sliceCounter = 0;
	}

	@Override
	public ImagePlus getImagePlus() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	private Vector3D getPos(JSONObject metaobj) throws Exception {
		String xystr = metaobj.getString(xytzDevices[0]);
		double x = Double.parseDouble(xystr.substring(0, xystr.indexOf("x")));
		double y = Double.parseDouble(xystr.substring(xystr.indexOf("x") + 1));
		double z = metaobj.getDouble(xytzDevices[2]);

		return new Vector3D(x, y, z);
	}

	@Override
	public void processSlice(ImageProcessor ip, JSONObject metaobj)
			throws Exception {
		long bitDepth = core.getImageBitDepth();
		byte[] data = bitDepth == 8 ?
			(byte[])ip.getPixels() :
			DataTools.shortsToBytes((short[])ip.getPixels(), true);

		Vector3D pos = getPos(metaobj);

		if(/*lastPosition == null ||*/ writer == null) {
			openWriter((imageCounter/stacks)+1, metaobj.getDouble(xytzDevices[1]));
		}

//		lastPosition = pos;
//		lastTheta = metaobj.getDouble(xytzDevices[1]);

		meta.setPlanePositionX(pos.getX(), imageCounter % stacks, sliceCounter);
		meta.setPlanePositionY(pos.getY(), imageCounter % stacks, sliceCounter);
		meta.setPlanePositionZ(pos.getZ(), imageCounter % stacks, sliceCounter);
//		meta.setPlaneTheC(new NonNegativeInteger((int)metaobj.getDouble(xytzDevices[1])), 0, sliceCounter);
		meta.setPlaneTheZ(new NonNegativeInteger(sliceCounter), imageCounter % stacks, sliceCounter);
		meta.setPlaneTheT(new NonNegativeInteger((imageCounter/stacks)+1), imageCounter % stacks, sliceCounter);
		meta.setPlaneDeltaT(metaobj.getDouble(xytzDevices[3]), imageCounter % stacks, sliceCounter);
		meta.setPlaneAnnotationRef(pos.getX() + "/" + pos.getY() + "/" + pos.getZ(), imageCounter % stacks, sliceCounter, 0);

		writer.savePlane(sliceCounter, data);

		++sliceCounter;
	}

	@Override
	public void finalizeStack(int depth) throws Exception {
		ReportingUtils.logMessage("Finished stack along dimension " + depth);

		writer.close();
		writer = null;
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
