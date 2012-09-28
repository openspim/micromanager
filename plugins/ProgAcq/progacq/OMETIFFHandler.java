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
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;

public class OMETIFFHandler implements AcqOutputHandler {
	private File outputFile;
	private String[] xytzDevices;

	private IMetadata meta;
	private int imageCounter, sliceCounter;
	private IFormatWriter writer;

	private Vector3D lastPosition;
	private double lastTheta;

	private CMMCore core;
	private int stacks, timesteps;
	private int[] stackDepths;
	private double deltat;

	public OMETIFFHandler(CMMCore iCore, File outFile, String xyDev,
			String cDev, String zDev, String tDev, int[] iStackDepths,
			int iTimeSteps, double iDeltaT) {

		if(outFile == null)
			throw new IllegalArgumentException("Null path specified.");

		xytzDevices = new String[] {xyDev, cDev, zDev, tDev};

		imageCounter = sliceCounter = 0;

		stacks = iStackDepths.length;
		stackDepths = iStackDepths;
		core = iCore;
		timesteps = iTimeSteps;
		deltat = iDeltaT;
		outputFile = outFile;

		try {
			meta = new ServiceFactory().getInstance(OMEXMLService.class).createOMEXMLMetadata(null);

			meta.createRoot();

			resetWriter();
		} catch(Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	private void resetWriter() throws Exception {
		defaultMetaData();

		writer = new ImageWriter().getWriter(outputFile.getAbsolutePath());
		writer.setWriteSequentially(true);
		writer.setMetadataRetrieve(meta);
		writer.setInterleaved(false);
		writer.setValidBitsPerPixel((int) core.getImageBitDepth());
		writer.setCompression("Uncompressed");
		writer.setId(outputFile.getAbsolutePath());
//		writer.setSeries(imageCounter);

		sliceCounter = 0;
	}

	private void defaultMetaData() throws Exception {
		meta.setImageID(MetadataTools.createLSID("Image", imageCounter), imageCounter);
		meta.setPixelsID(MetadataTools.createLSID("Pixels", 0), imageCounter);
		meta.setPixelsDimensionOrder(DimensionOrder.XYCZT, imageCounter);
		meta.setChannelID(MetadataTools.createLSID("Channel", 0), imageCounter, 0);
		meta.setChannelSamplesPerPixel(new PositiveInteger(1), imageCounter, 0);
		meta.setPixelsBinDataBigEndian(Boolean.FALSE, imageCounter, 0);
		meta.setPixelsType(PixelType.UINT16, imageCounter);

		meta.setPixelsSizeX(new PositiveInteger((int)core.getImageWidth()), imageCounter);
		meta.setPixelsSizeY(new PositiveInteger((int)core.getImageHeight()), imageCounter);
		meta.setPixelsSizeZ(new PositiveInteger(stackDepths[imageCounter]), imageCounter);
		meta.setPixelsSizeC(new PositiveInteger(1), imageCounter);
		meta.setPixelsSizeT(new PositiveInteger(timesteps), imageCounter);

		meta.setPixelsPhysicalSizeX(new PositiveFloat(core.getPixelSizeUm()), imageCounter);
		meta.setPixelsPhysicalSizeY(new PositiveFloat(core.getPixelSizeUm()), imageCounter);
		meta.setPixelsPhysicalSizeZ(new PositiveFloat(1d), imageCounter);
		meta.setPixelsTimeIncrement(new Double(deltat), imageCounter);
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
		byte[] data = DataTools.shortsToBytes((short[])ip.getPixels(), true);

		Vector3D pos = getPos(metaobj);

		// Determine differences from the last position.
		if(lastPosition != null) {
			Vector3D diff = pos.subtract(lastPosition);
			if(diff.getX() == 0 && diff.getY() == 0 && diff.getZ() != 0 &&
				metaobj.getDouble(xytzDevices[1]) == lastTheta) {
				// Should this case get removed?
			} else {
				++imageCounter;
				sliceCounter = 0;

				ReportingUtils.logMessage("X/Y changed! " + pos.toString() + ", " + lastPosition.toString());

				writer.close();
				resetWriter();
			}
		}

		lastPosition = pos;
		lastTheta = metaobj.getDouble(xytzDevices[1]);

		meta.setPlanePositionX(pos.getX(), imageCounter, sliceCounter);
		meta.setPlanePositionY(pos.getY(), imageCounter, sliceCounter);
		meta.setPlanePositionZ(pos.getZ(), imageCounter, sliceCounter);
		meta.setPlaneDeltaT(metaobj.getDouble(xytzDevices[3]), imageCounter, sliceCounter);

		writer.setSeries(imageCounter);
		writer.savePlane(sliceCounter, data);

		++sliceCounter;
	}

	@Override
	public void finalize() throws Exception {
		writer.close();

		ReportingUtils.logMessage("" + imageCounter + " vs " + stacks);
		imageCounter = 0;

		resetWriter();
	}
}
