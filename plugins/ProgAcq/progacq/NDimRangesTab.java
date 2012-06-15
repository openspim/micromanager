package progacq;

import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mmcorej.CMMCore;
import mmcorej.DeviceType;

public class NDimRangesTab extends JPanel {
	private static final long serialVersionUID = 7442768927469646566L;

	private RangeSlider[] controls;
	private CMMCore core;
	
	public NDimRangesTab(CMMCore icore, String[] devices) {
		core = icore;
		setDevices(devices);
	}

	public void setDevices(String[] devices) {
		removeAll();
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		Vector<RangeSlider> cVec = new Vector<RangeSlider>();
		
		for(String dev : devices) {
			JPanel p = new JPanel();
			p.setBorder(BorderFactory.createTitledBorder(dev));
			p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

			try {
				DeviceType t = core.getDeviceType(dev);

				if(t.equals(DeviceType.XYStageDevice)) {
					JPanel xP = new JPanel();
					xP.setBorder(BorderFactory.createTitledBorder("Stage X"));
					
					// TODO: Figure out how to read min/max X/Y.
					RangeSlider xRS = new RangeSlider(-100D, 100D);
					xP.add(xRS);
					cVec.add(xRS);
					
					p.add(xP);
					
					JPanel yP = new JPanel();
					yP.setBorder(BorderFactory.createTitledBorder("Stage Y"));
					
					RangeSlider yRS = new RangeSlider(-100D, 100D);
					yP.add(yRS);
					cVec.add(yRS);
					
					p.add(yP);
				} else if(t.equals(DeviceType.StageDevice)) {
					double min = -100D, max = 100D;
					
					if(core.hasProperty(dev, "Position") &&
					   core.hasPropertyLimits(dev, "Position")) {
						min = core.getPropertyLowerLimit(dev, "Position");
						max = core.getPropertyUpperLimit(dev, "Position");
					};
					
					RangeSlider rs = new RangeSlider(min, max);
					
					p.add(rs);
					cVec.add(rs);
				} else {
					p.add(new JLabel("Unrangeable Device!"));
					p.add(new JLabel(""));
					p.add(new JLabel("This device can't have"));
					p.add(new JLabel("a range applied to it."));
					p.add(new JLabel("It will be ignored."));
				}
			} catch (Exception e) {
				p.add(new JLabel("Exception!"));
				p.add(new JLabel(""));
				p.add(new JLabel("Couldn't determine the type"));
				p.add(new JLabel("of this device. It will be"));
				p.add(new JLabel("ignored."));
				
				continue;
			} finally {
				add(p);
			}
		}

		controls = new RangeSlider[cVec.size()];
		for(int i=0; i < cVec.size(); ++i)
			controls[i] = cVec.get(i);
	}
	
	public List<double[]> getRanges() {
		Vector<double[]> ranges = new Vector<double[]>(controls.length);
		
		for(RangeSlider rs : controls)
			ranges.add(rs.getRange());
		
		return ranges;
	}
}
