package progacq;

import java.io.File;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import org.json.JSONObject;

import loci.common.DataTools;
import loci.common.services.ServiceFactory;

import loci.formats.FormatTools;
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

	private CMMCore core;
	private int depth, thetas, timesteps;
	private double deltat;

	public OMETIFFHandler(CMMCore icore, File outFile, String xyDev,
			String cDev, String zDev, String tDev, int idepth, int ithetas,
			int itimesteps, double ideltat) {
		if(outFile == null)
			throw new IllegalArgumentException("Null path specified.");

		xytzDevices = new String[] {xyDev, cDev, zDev, tDev};

		imageCounter = sliceCounter = 0;

		core = icore;
		depth = idepth;
		thetas = ithetas;
		timesteps = itimesteps;
		deltat = ideltat;
		outputFile = outFile;

		try {
			resetWriter();
		} catch(Throwable t) {
			throw new IllegalArgumentException(t);
		}
	}

	private void resetWriter() throws Exception {
		meta = new ServiceFactory().getInstance(OMEXMLService.class).createOMEXMLMetadata(null);

		meta.createRoot();
		meta.setImageID(MetadataTools.createLSID("Image", 0), imageCounter);
		meta.setPixelsID(MetadataTools.createLSID("Pixels", 0), imageCounter);
		meta.setPixelsDimensionOrder(DimensionOrder.XYCZT, imageCounter);
		meta.setChannelID(MetadataTools.createLSID("Channel", 0), imageCounter, 0);
		meta.setChannelSamplesPerPixel(new PositiveInteger(1), imageCounter, 0);
		meta.setPixelsBinDataBigEndian(Boolean.FALSE, imageCounter, 0);
		meta.setPixelsType(PixelType.UINT16, imageCounter);

		meta.setPixelsSizeX(new PositiveInteger((int)core.getImageWidth()), imageCounter);
		meta.setPixelsSizeY(new PositiveInteger((int)core.getImageHeight()), imageCounter);
		meta.setPixelsSizeZ(new PositiveInteger(depth), imageCounter);
		meta.setPixelsSizeC(new PositiveInteger(thetas), imageCounter);
		meta.setPixelsSizeT(new PositiveInteger(timesteps), imageCounter);

		meta.setPixelsPhysicalSizeX(new PositiveFloat(core.getPixelSizeUm()), imageCounter);
		meta.setPixelsPhysicalSizeY(new PositiveFloat(core.getPixelSizeUm()), imageCounter);
		meta.setPixelsPhysicalSizeZ(new PositiveFloat(1d), imageCounter);
		meta.setPixelsTimeIncrement(new Double(deltat), imageCounter);

		writer = new ImageWriter().getWriter(outputFile.getAbsolutePath());
		writer.setWriteSequentially(true);
		writer.setMetadataRetrieve(meta);
		writer.setInterleaved(false);
		writer.setValidBitsPerPixel((int) core.getImageBitDepth());
		writer.setCompression("Uncompressed");
		writer.setId(outputFile.getAbsolutePath());

		sliceCounter = 0;
	}

	@Override
	public ImagePlus getImagePlus() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processSlice(ImageProcessor ip, JSONObject metaobj)
			throws Exception {
		byte[] data = DataTools.shortsToBytes((short[])ip.getPixels(), true);

		String xystr = metaobj.getString(xytzDevices[0]);
		double x = Double.parseDouble(xystr.substring(0, xystr.indexOf("x")));
		double y = Double.parseDouble(xystr.substring(xystr.indexOf("x") + 1));

		meta.setPixelsID(MetadataTools.createLSID("Pixels", sliceCounter), imageCounter);
		meta.setPlanePositionX(x, imageCounter, sliceCounter);
		meta.setPlanePositionY(y, imageCounter, sliceCounter);
		meta.setPlanePositionZ(metaobj.getDouble(xytzDevices[2]), imageCounter, sliceCounter);
		meta.setPlaneDeltaT(metaobj.getDouble(xytzDevices[3]), imageCounter, sliceCounter);

		writer.savePlane(sliceCounter, data);

		++sliceCounter;
	}

	@Override
	public void finalize() throws Exception {
		writer.close();

		++imageCounter;

		resetWriter();
	}
}
