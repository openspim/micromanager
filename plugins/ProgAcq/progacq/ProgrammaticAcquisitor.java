package progacq;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mmcorej.CMMCore;
import mmcorej.DeviceType;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class ProgrammaticAcquisitor implements MMPlugin, ActionListener,
		ChangeListener {
	private static final String BTN_ADD_DISCRETES = "Add Discretes...";
	private static final String BTN_START = "Start";
	private static final String BTN_REMOVE_STEPS = "Remove Steps";
	private static final String BTN_ADD_RANGES = "Add Ranges...";
	private static final String BTN_SELECT_DEVICES = "Select Devices...";

	public static String menuName = "Programmatic Acquisitor";
	public static String tooltipDescription = "Allows the acquiring of complex series of images.";

	private ScriptInterface app;
	private CMMCore core;
	private static MMStudioMainFrame gui;

	private JFrame frame;
	private JTable stepsTbl;
	private JTextField stepBox;
	private JTextField countBox;
	private JCheckBox timeCB;
	private JCheckBox tDevCB;
	private JCheckBox zDevCB;
	private RangeSlider rangeX;
	private RangeSlider rangeY;
	private RangeSlider rangeZ;
	private RangeSlider rangeTheta;
	private JComboBox tDevCmbo;
	private JComboBox zDevCmbo;
	private JComboBox xyDevCmbo;
	private JTabbedPane tabs;

	@Override
	public void dispose() {
		if (frame == null)
			return;

		frame.dispose();
		frame = null;
	};

	@Override
	public void setApp(ScriptInterface app) {
		this.app = app;
		this.core = app.getMMCore();
		this.gui = MMStudioMainFrame.getInstance(); // Removeme if unneeded!
	};

	@Override
	public void show() {
		if (frame == null)
			buildFrame();

		frame.setVisible(true);
	};

	@Override
	public void configurationChanged() {
		// TODO:
		// Our table is broken! We'll have to inform the user and drop any
		// columns of the table that aren't valid any more.

		// Part 1: The Sliders tab.
		xyDevCmbo.setModel(new DefaultComboBoxModel(core
				.getLoadedDevicesOfType(DeviceType.XYStageDevice).toArray()));
		xyDevCmbo.setMaximumSize(xyDevCmbo.getPreferredSize());
		xyDevCmbo.setSelectedItem(core.getXYStageDevice());

		zDevCmbo.setModel(new DefaultComboBoxModel(core.getLoadedDevicesOfType(
				DeviceType.StageDevice).toArray()));
		zDevCmbo.setMaximumSize(zDevCmbo.getPreferredSize());
		zDevCmbo.setSelectedItem(core.getFocusDevice());

		zDevCB.setEnabled(zDevCmbo.getItemCount() > 0);
		zDevCB.setSelected(zDevCmbo.getItemCount() > 0);

		tDevCmbo.setModel(new DefaultComboBoxModel(core.getLoadedDevicesOfType(
				DeviceType.StageDevice).toArray()));
		tDevCmbo.setMaximumSize(tDevCmbo.getPreferredSize());
		tDevCmbo.setSelectedIndex(tDevCmbo.getItemCount() - 1);

		tDevCB.setEnabled(tDevCmbo.getItemCount() > 1);
		tDevCB.setSelected(tDevCmbo.getItemCount() > 1);
	};

	@Override
	public String getDescription() {
		return "Users can specify a sequence or range of settings on any number of devices, order them, and acquire all described images.";
	};

	@Override
	public String getVersion() {
		return "0.01";
	};

	@Override
	public String getInfo() {
		return "None yet!";
	};

	@Override
	public String getCopyright() {
		return "I have rights?";
	};

	private void buildFrame() {
		if (frame != null)
			return;

		frame = new JFrame(ProgrammaticAcquisitor.menuName);
		frame.getContentPane().setLayout(
				new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));

		tabs = new JTabbedPane();

		JPanel sliders = new JPanel();
		sliders.setName("SPIM");
		sliders.setAlignmentX(Component.LEFT_ALIGNMENT);
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.PAGE_AXIS));

		tabs.add("SPIM", sliders);

		JPanel xy = new JPanel();
		xy.setLayout(new BoxLayout(xy, BoxLayout.PAGE_AXIS));
		xy.setBorder(BorderFactory.createTitledBorder("X/Y Stage"));

		JPanel xyDev = new JPanel();
		xyDev.setLayout(new BoxLayout(xyDev, BoxLayout.LINE_AXIS));

		JLabel xyDevLbl = new JLabel("X/Y Stage Device:");
		xyDevCmbo = new JComboBox(core.getLoadedDevicesOfType(
				DeviceType.XYStageDevice).toArray());
		xyDevCmbo.setMaximumSize(xyDevCmbo.getPreferredSize());

		xyDev.add(xyDevLbl);
		xyDev.add(xyDevCmbo);

		xy.add(xyDev);

		// These names keep getting more and more convoluted.
		JPanel xyXY = new JPanel();
		xyXY.setLayout(new BoxLayout(xyXY, BoxLayout.LINE_AXIS));

		JPanel xy_x = new JPanel();
		xy_x.setBorder(BorderFactory.createTitledBorder("Stage X"));

		rangeX = new RangeSlider(-100D, 100D);

		xy_x.add(rangeX);

		xyXY.add(xy_x);

		JPanel xy_y = new JPanel();
		xy_y.setBorder(BorderFactory.createTitledBorder("Stage Y"));

		rangeY = new RangeSlider(-100D, 100D);

		xy_y.add(rangeY);

		xyXY.add(xy_y);

		xy.add(xyXY);

		sliders.add(xy);

		JPanel z = new JPanel();
		z.setBorder(BorderFactory.createTitledBorder("Stage Z"));
		z.setLayout(new BoxLayout(z, BoxLayout.PAGE_AXIS));

		JPanel zDev = new JPanel();
		zDev.setLayout(new BoxLayout(zDev, BoxLayout.LINE_AXIS));

		zDevCB = new JCheckBox("");
		zDevCB.addChangeListener(this);
		JLabel zDevLbl = new JLabel("Z Stage Device:");
		zDevCmbo = new JComboBox(core.getLoadedDevicesOfType(
				DeviceType.StageDevice).toArray());
		zDevCmbo.setMaximumSize(zDevCmbo.getPreferredSize());

		zDev.add(zDevCB);
		zDev.add(zDevLbl);
		zDev.add(zDevCmbo);

		z.add(zDev);

		z.add(Box.createRigidArea(new Dimension(10, 4)));

		rangeZ = new RangeSlider(-100D, 100D);

		z.add(rangeZ);

		sliders.add(z);

		JPanel t = new JPanel();
		t.setBorder(BorderFactory.createTitledBorder("Theta"));
		t.setLayout(new BoxLayout(t, BoxLayout.PAGE_AXIS));

		JPanel tDev = new JPanel();
		tDev.setLayout(new BoxLayout(tDev, BoxLayout.LINE_AXIS));

		tDevCB = new JCheckBox("");
		tDevCB.addChangeListener(this);
		JLabel tDevLbl = new JLabel("Theta Device:");
		tDevLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		tDevCmbo = new JComboBox(core.getLoadedDevicesOfType(
				DeviceType.StageDevice).toArray());
		tDevCmbo.setMaximumSize(tDevCmbo.getPreferredSize());
		tDevCmbo.setSelectedIndex(tDevCmbo.getItemCount() - 1);
		tDevCmbo.setAlignmentX(Component.LEFT_ALIGNMENT);

		tDev.add(tDevCB);
		tDev.add(tDevLbl);
		tDev.add(tDevCmbo);

		t.add(tDev);

		t.add(Box.createRigidArea(new Dimension(10, 4)));

		rangeTheta = new RangeSlider(-180D, 180D);

		t.add(rangeTheta);

		sliders.add(t);

		JPanel steps = new JPanel();
		steps.setLayout(new BoxLayout(steps, BoxLayout.LINE_AXIS));

		stepsTbl = new JTable();
		stepsTbl.setFillsViewportHeight(true);
		stepsTbl.setAutoCreateColumnsFromModel(true);
		stepsTbl.setModel(new StepTableModel());

		JScrollPane tblScroller = new JScrollPane(stepsTbl);
		steps.add(tblScroller);

		JPanel stepsBtns = new JPanel();
		stepsBtns.setLayout(new BoxLayout(stepsBtns, BoxLayout.PAGE_AXIS));

		JButton selDevs = new JButton(BTN_SELECT_DEVICES);
		selDevs.addActionListener(this);
		selDevs.setAlignmentY(Component.TOP_ALIGNMENT);

		JButton addRanges = new JButton(BTN_ADD_RANGES);
		addRanges.addActionListener(this);

		// TODO: Add discrete row support.
		JButton addDisc = new JButton(BTN_ADD_DISCRETES);

		JButton remStep = new JButton(BTN_REMOVE_STEPS);
		remStep.addActionListener(this);

		stepsBtns.add(selDevs);
		stepsBtns.add(addRanges);
		stepsBtns.add(addDisc);
		stepsBtns.add(remStep);

		steps.add(stepsBtns);

		tabs.add("Advanced", steps);

		frame.getContentPane().add(tabs);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));

		JPanel timeBox = new JPanel();
		timeBox.setLayout(new BoxLayout(timeBox, BoxLayout.LINE_AXIS));
		timeBox.setBorder(BorderFactory.createTitledBorder("Time"));

		timeCB = new JCheckBox("");
		timeCB.setSelected(true);

		JLabel step = new JLabel("Interval:");
		// TODO: Enforce a minimum? The stage needs time to move.
		step.setToolTipText("Delay between acquisition sequences in milliseconds.");
		stepBox = new JTextField(8);
		stepBox.setMaximumSize(stepBox.getPreferredSize());

		JLabel count = new JLabel("Count:");
		count.setToolTipText("Number of acquisition sequences to perform. Each is described by the table above.");
		countBox = new JTextField(8);
		countBox.setMaximumSize(countBox.getPreferredSize());

		timeCB.addChangeListener(this);

		timeBox.add(timeCB);
		timeBox.add(step);
		timeBox.add(stepBox);
		timeBox.add(count);
		timeBox.add(countBox);

		bottom.add(timeBox);

		JButton go = new JButton(BTN_START);
		go.addActionListener(this);

		bottom.add(go);

		frame.getContentPane().add(bottom);

		frame.pack();

		// Simulate a configuration change; this finishes off a few little UI
		// bits that couldn't be taken care of above (component creation order).
		configurationChanged();
	};

	@Override
	public void actionPerformed(ActionEvent e) {
		if (BTN_SELECT_DEVICES.equals(e.getActionCommand())) {
			SelectStringsDialog.doInstance(frame, getUnusedDevs(),
					getUsedDevs(), new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent e) {
							((StepTableModel) stepsTbl.getModel())
									.setColumns(SelectStringsDialog
											.getFinalList());
						}
					});
		} else if (BTN_ADD_RANGES.equals(e.getActionCommand())) {
			if (getUsedDevs().size() <= 0)
				JOptionPane.showMessageDialog(frame,
						"You must select at least one device!");
			else
				AddStepsDialog.doInstance(frame, core, getUsedDevs(),
						new WindowAdapter() {
							@Override
							public void windowClosed(WindowEvent e) {
								StepTableModel mdl = (StepTableModel) stepsTbl
										.getModel();

								Vector<String[]> data = generateRowsFromRanges(
										AddStepsDialog.getResults(),
										mdl.getColumnNames());

								for (String[] row : data)
									mdl.insertRow(row);
							}
						});
		} else if (BTN_ADD_DISCRETES.equals(e.getActionCommand())) {
			JOptionPane.showMessageDialog(frame,
					"Not supported yet! (Add a single-elements range?)");
		} else if (BTN_REMOVE_STEPS.equals(e.getActionCommand())) {
			((StepTableModel) stepsTbl.getModel()).removeRows(stepsTbl
					.getSelectedRows());
		} else if (BTN_START.equals(e.getActionCommand())) {
			if (timeCB.isSelected()) {
				if (countBox.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Please enter a count or disable timing.");
					countBox.requestFocusInWindow();
					return;
				} else if (stepBox.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Please enter a time step or disable timing.");
					stepBox.requestFocusInWindow();
					return;
				}
			}

			try {
				if ("SPIM".equals(tabs.getSelectedComponent().getName())) {
					List<double[]> ranges = new Vector<double[]>(Arrays.asList(
							rangeX.getRange(), rangeY.getRange()));

					List<String> devs = new Vector<String>(
							Arrays.asList(xyDevCmbo.getSelectedItem()
									.toString()));

					if (zDevCB.isSelected()) {
						ranges.add(rangeZ.getRange());
						devs.add(zDevCmbo.getSelectedItem().toString());
					}

					if (tDevCB.isSelected()) {
						ranges.add(rangeTheta.getRange());
						devs.add(tDevCmbo.getSelectedItem().toString());
					}

					String[] devsa = toArray(devs);

					List<String[]> rows = generateRowsFromRanges(ranges, devsa);

					// performAcquisition(
					performAndShowAcq(
							core,
							devsa,
							rows,
							false,
							timeCB.isSelected() ? Integer.parseInt(countBox
									.getText()) : 1,
							timeCB.isSelected() ? Double.parseDouble(stepBox
									.getText()) : 0);
				} else {
					// performAcquisition(
					performAndShowAcq(
							core,
							((StepTableModel) stepsTbl.getModel())
									.getColumnNames(),
							((StepTableModel) stepsTbl.getModel()).getRows(),
							false,
							timeCB.isSelected() ? Integer.parseInt(countBox
									.getText()) : 1,
							timeCB.isSelected() ? Double.parseDouble(stepBox
									.getText()) : 0);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(frame,
						"Error during acquisition: " + e1.getMessage());
			}
		} else {
			throw new Error("Who broke the action listener? :(");
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(zDevCB)) {
			rangeZ.setEnabled(zDevCB.isSelected());
			zDevCmbo.setEnabled(zDevCB.isSelected());
		} else if (e.getSource().equals(tDevCB)) {
			rangeTheta.setEnabled(tDevCB.isSelected());
			tDevCmbo.setEnabled(tDevCB.isSelected());
		} else if (e.getSource().equals(timeCB)) {
			countBox.setEnabled(timeCB.isSelected());
			stepBox.setEnabled(timeCB.isSelected());
		}
	}

	private Vector<Vector<Double>> getRows(List<double[]> subranges) {
		double[] first = (double[]) subranges.get(0);
		Vector<Vector<Double>> rows = new Vector<Vector<Double>>();

		if (subranges.size() == 1) {
			for (double val : first) {
				Vector<Double> row = new Vector<Double>();
				row.add(val);
				rows.add(row);
			}
		} else {
			for (double val : first) {
				Vector<Vector<Double>> subrows = getRows(subranges.subList(1,
						subranges.size()));

				for (Vector<Double> row : subrows) {
					Vector<Double> newRow = new Vector<Double>(row);
					newRow.add(0, val);
					rows.add(newRow);
				}
			}
		}

		return rows;
	}

	public Vector<String[]> generateRowsFromRanges(List<double[]> ranges,
			String[] devs) {
		// Each element of range is a triplet of min/step/max.
		// This function determines the discrete values of each range, then
		// works out all possible values and adds them as rows to the table.
		Vector<double[]> values = new Vector<double[]>(ranges.size());

		for (double[] triplet : ranges) {
			double[] discretes = new double[(int) ((triplet[2] - triplet[0]) / triplet[1]) + 1];

			for (int i = 0; i < discretes.length; ++i)
				discretes[i] = triplet[0] + triplet[1] * i;

			values.add(discretes);
		}

		return condenseXY(getRows(values), devs);
	}

	private Vector<String[]> condenseXY(List<? extends List<Double>> rows,
			String[] devs) {
		// Build a quick list of indices of X/Y stage devices.
		// Below, we condense the X and Y coordinates into an ordered pair so
		// they can be inserted into the table. This list is used to determine
		// which sets of indices need to be squished into a single value.
		Vector<Integer> xyStages = new Vector<Integer>(devs.length);
		for (int i = 0; i < devs.length; ++i) {
			try {
				if (core.getDeviceType(devs[i])
						.equals(DeviceType.XYStageDevice))
					xyStages.add(i);
			} catch (Exception e) {
				// I can't think of a more graceless way to resolve this issue.
				// But then, nor can I think of a more graceful one.
				throw new Error("Couldn't resolve type of device \"" + devs[i]
						+ "\"", e);
			}
		}

		Vector<String[]> finalRows = new Vector<String[]>();

		for (List<Double> row : rows) {
			if (xyStages.size() > 0) {
				Vector<String> finalRow = new Vector<String>();

				for (int i = 0; i < row.size(); ++i)
					if (xyStages.contains(i))
						finalRow.add(row.get(i) + ", " + row.get(++i));
					else
						finalRow.add("" + row.get(i));

				finalRows.add(toArray(finalRow));
			} else {
				finalRows.add(toArray(row));
			}
		}

		return finalRows;
	}

	private String[] toArray(List<? extends Object> anything) {
		String[] ret = new String[anything.size()];

		for (int i = 0; i < anything.size(); ++i)
			ret[i] = anything.get(i).toString();

		return ret;
	}

	// TODO related to this function:
	// 1. Threading!
	// 2. Fix application of timestep!
	/**
	 * This function runs a generalized acquisition sequence. It's a mixture of
	 * Micro-Manager's built in sequencing support (confusing) and waiting for
	 * motors.
	 * 
	 * @param core
	 *            The Micro-Manager core reference to work with. This class has
	 *            a reference, but it's possible (in the future) that this class
	 *            may be referenced without being instantiated.
	 * @param devices
	 *            A list of devices to work with. Should be as long as each row
	 *            in 'steps'. Should only contain stage and X/Y stage device
	 *            labels (nothing else is supported just yet!).
	 * @param rows
	 *            A list of states for all devices. Each 'row' should have the
	 *            same length, the length of the above. For X/Y stages, elements
	 *            should be ordered pairs. For stage devices, elements should be
	 *            doubles as strings. No other devices are yet supported (state
	 *            based devices will hopefully be added soon!).
	 * @param waitEach
	 *            If true, waits for each device in turn rather than moving them
	 *            simultaneously; if false, it still waits at the end of issuing
	 *            all movements for each device to be finished before acquiring.
	 * @param timeseqs
	 *            Number of acquisition sequences to run.
	 * @param timestep
	 *            Delay in milliseconds between the beginning of each
	 *            acquisition. Arbitrary if only a single acquisition. Also
	 *            lying right now; for the moment, it's a delay.
	 * @throws Exception
	 *             on encountering malformed data or bad device names, or an
	 *             exception while stepping (i.e. motor malfunction).
	 */
	public static ImagePlus performAcquisition(CMMCore core, String[] devices,
			List<String[]> rows, boolean waitEach, int timeseqs, double timestep)
			throws Exception {

		core.removeImageSynchroAll();
		for (String dev : devices)
			core.assignImageSynchro(dev);

		ImageStack img = new ImageStack((int) core.getImageWidth(),
				(int) core.getImageHeight());

		for (int seq = 0; seq < timeseqs; ++seq) {
			int step = 0;
			for (String[] positions : rows) {
				for (int i = 0; i < devices.length; ++i) {
					String dev = devices[i];
					String pos = positions[i];
					try {
						if (core.getDeviceType(dev).equals(
								DeviceType.StageDevice))
							core.setPosition(dev, Double.parseDouble(pos));
						else if (core.getDeviceType(dev).equals(
								DeviceType.XYStageDevice))
							core.setXYPosition(dev, parseX(pos), parseY(pos));
						else
							throw new Exception("Unknown device type for \""
									+ dev + "\"");
					} catch (NumberFormatException e) {
						throw new Exception("Malformed number for device \""
								+ dev + "\", row " + step, e);
					}

					if (waitEach)
						core.waitForDevice(dev);
				}

				if (!waitEach)
					for (String dev : devices)
						core.waitForDevice(dev);

				// TODO: This is probably wrong.
				synchronized (core) {
					core.snapImage();
				}

				img.addSlice(generateMeta(seq * timestep, core, devices),
						newImageProcessor(core));

				++step;
			}

			core.sleep(timestep);
		}

		return new ImagePlus("SPIM!", img);
	}

	public static void performAndShowAcq(final CMMCore core,
			final String[] devs, final List<String[]> rows, final boolean wait,
			final int timeseqs, final double timestep) {
		if (SwingUtilities.isEventDispatchThread()) {
			new Thread() {
				@Override
				public void run() {
					performAndShowAcq(core, devs, rows, wait, timeseqs,
							timestep);
				}
			}.start();
			return;
		}

		try {
			performAcquisition(core, devs, rows, wait, timeseqs, timestep)
					.show();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ImageProcessor newImageProcessor(CMMCore core) {
		try {
			if (core.getBytesPerPixel() == 1) {
				return new ByteProcessor((int) core.getImageWidth(),
						(int) core.getImageHeight(), (byte[]) core.getImage());
			} else if (core.getBytesPerPixel() == 2) {
				return new ShortProcessor((int) core.getImageWidth(),
						(int) core.getImageHeight(), (short[]) core.getImage(),
						null);
			} else {
				throw new Exception("Bwuh");
			}
		} catch (Exception e) {
			return null;
		}
	}

	private static String generateMeta(Double t, CMMCore core, String[] devs) {
		String out = "t=" + t + "?; ";

		for (String dev : devs)
			try {
				out += dev + "=";
				if (core.getDeviceType(dev).equals(DeviceType.XYStageDevice)) {
					out += core.getXPosition(dev) + ", "
							+ core.getYPosition(dev);
				} else if (core.getDeviceType(dev).equals(
						DeviceType.StageDevice)) {
					out += core.getPosition(dev);
				} else {
					out += "<unknown>";
				}
				out += "; ";
			} catch (Exception e) {
				e.printStackTrace();
				return "<<<EXCEPTION: " + e.getMessage() + ">>>";
			}

		return out;
	};

	private static double parseX(String pair) {
		return Double.parseDouble(pair.substring(0, pair.indexOf(',')));
	}

	private static double parseY(String pair) {
		return Double.parseDouble(pair.substring(pair.indexOf(' ') + 1));
	}

	private Vector<String> getUnusedDevs() {
		Vector<String> all = new Vector<String>((int) core.getLoadedDevices()
				.size());

		for (String entry : core.getLoadedDevices())
			all.add(entry);

		for (int i = 0; i < stepsTbl.getModel().getColumnCount(); ++i)
			all.remove(stepsTbl.getModel().getColumnName(i));

		return all;
	}

	private Vector<String> getUsedDevs() {
		Vector<String> res = new Vector<String>();

		for (int i = 0; i < stepsTbl.getModel().getColumnCount(); ++i)
			res.add(stepsTbl.getModel().getColumnName(i));

		return res;
	}
};