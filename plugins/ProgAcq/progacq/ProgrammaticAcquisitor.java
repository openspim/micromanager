package progacq;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mmcorej.CMMCore;
import mmcorej.DeviceType;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class ProgrammaticAcquisitor implements MMPlugin, ActionListener {
	private static final String BTN_START = "Start";
	private static final String BTN_REMOVE_STEPS = "Remove Steps";
	private static final String BTN_ADD_RANGES = "Add Ranges...";
	private static final String BTN_SELECT_DEVICES = "Select Devices...";

	public static String menuName = "Programmatic Acquisitor";
	public static String tooltipDescription = "Allows the acquiring of complex series of images.";

	private ScriptInterface app;
	private CMMCore core;
	private MMStudioMainFrame gui;

	private JFrame frame;
	private JTable stepsTbl;
	private JTextField stepBox;
	private JTextField countBox;
	private JCheckBox timeCB;

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

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.setBorder(BorderFactory.createTitledBorder("Steps"));

		stepsTbl = new JTable();
		stepsTbl.setFillsViewportHeight(true);
		stepsTbl.setAutoCreateColumnsFromModel(true);
		stepsTbl.setModel(new StepTableModel());

		JScrollPane tblScroller = new JScrollPane(stepsTbl);
		top.add(tblScroller);

		JPanel stepsBtns = new JPanel();
		stepsBtns.setLayout(new BoxLayout(stepsBtns, BoxLayout.PAGE_AXIS));
		stepsBtns.setAlignmentY(Component.TOP_ALIGNMENT);

		JButton selDevs = new JButton(BTN_SELECT_DEVICES);
		selDevs.addActionListener(this);

		JButton addRanges = new JButton(BTN_ADD_RANGES);
		addRanges.addActionListener(this);

		// TODO: Add discrete row support.
		JButton addDisc = new JButton("Add Discretes...");

		JButton remStep = new JButton(BTN_REMOVE_STEPS);
		remStep.addActionListener(this);

		stepsBtns.add(selDevs);
		stepsBtns.add(addRanges);
		stepsBtns.add(addDisc);
		stepsBtns.add(remStep);

		top.add(stepsBtns);

		frame.getContentPane().add(top);

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

		timeCB.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				stepBox.setEnabled(timeCB.isSelected());
				countBox.setEnabled(timeCB.isSelected());
			}
		});

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
								insertRowsByRanges(AddStepsDialog.getResults());
							}
						});
		} else if (BTN_REMOVE_STEPS.equals(e.getActionCommand())) {
			((StepTableModel) stepsTbl.getModel()).removeRows(stepsTbl
					.getSelectedRows());
		} else if (BTN_START.equals(e.getActionCommand())) {
			if (timeCB.isSelected()) {
				if (countBox.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Please enter a count or disable timing.");
					countBox.requestFocusInWindow();
				} else if (stepBox.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Please enter a time step or disable timing.");
					stepBox.requestFocusInWindow();
				}
			} else {
				try {
					performAcquisition(
							core,
							((StepTableModel) stepsTbl.getModel())
									.getColumnNames(),
							((StepTableModel) stepsTbl.getModel()).getRows(),
							false,
							timeCB.isSelected() ? Integer.parseInt(countBox
									.getText()) : 1,
							timeCB.isSelected() ? Double.parseDouble(stepBox
									.getText()) : 0);
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(frame,
							"Error during acquisition: " + e1.getMessage());
				}
			}
		} else {
			throw new Error("Who broke the action listener? :(");
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

	private void insertRowsByRanges(Vector<double[]> ranges) {
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

		// Build a quick list of indices of X/Y stage devices.
		// Below, we condense the X and Y coordinates into an ordered pair so
		// they can be inserted into the table. This list is used to determine
		// which sets of indices need to be squished into a single value.
		Vector<Integer> xyStages = new Vector<Integer>(stepsTbl.getModel()
				.getColumnCount());
		for (int i = 0; i < stepsTbl.getModel().getColumnCount(); ++i) {
			try {
				if (core.getDeviceType(stepsTbl.getModel().getColumnName(i))
						.equals(DeviceType.XYStageDevice))
					xyStages.add(i);
			} catch (Exception e) {
				// I can't think of a more graceless way to resolve this issue.
				// But then, nor can I think of a more graceful one.
				throw new Error("Couldn't resolve type of device \""
						+ stepsTbl.getModel().getColumnName(i) + "\"", e);
			}
		}

		Vector<Vector<Double>> rows = getRows(values);

		for (Vector<Double> row : rows) {
			if (xyStages.size() > 0) {
				Vector<String> finalRow = new Vector<String>();

				for (int i = 0; i < row.size(); ++i)
					if (xyStages.contains(i))
						finalRow.add(row.get(i) + ", " + row.get(++i));
					else
						finalRow.add("" + row.get(i));

				((StepTableModel) stepsTbl.getModel()).insertRow(finalRow
						.toArray());
			} else {
				((StepTableModel) stepsTbl.getModel()).insertRow(row.toArray());
			}
		}
	};

	// TODO related to this function:
	// 1. Threading!
	// 2. Fix application of timestep!
	// 3. Rephrase to use ImagePlus (ala SPIMAcquisition.java)!
	// 3.1. Add metadata and the like!
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
	 * @param steps
	 *            A list of states for all devices. Each 'row' should have the
	 *            same length, the length of the above. For X/Y stages, elements
	 *            should be ordered pairs. For stage devices, elements should be
	 *            doubles as strings. No other devices are yet supported (state
	 *            based devices will hopefully be added soon!).
	 * @param waitEach
	 *            If true, waits for each device in turn rather than moving them
	 *            simultaneously; if false, it still waits at the end of issuing
	 *            all movements for each device to be finished before acquiring.
	 * @param steps
	 *            Number of acquisition sequences to run.
	 * @param timestep
	 *            Delay in milliseconds between the beginning of each
	 *            acquisition. Arbitrary if only a single acquisition. Also
	 *            lying right now; for the moment, it's a delay.
	 * @throws Exception
	 *             on encountering malformed data or bad device names, or an
	 *             exception while stepping (i.e. motor malfunction).
	 */
	public static void performAcquisition(CMMCore core, String[] devices,
			Vector<String[]> steps, boolean waitEach, int timesteps,
			double timestep) throws Exception {

		core.removeImageSynchroAll();
		for (String dev : devices)
			core.assignImageSynchro(dev);

		for (int seq = 0; seq < timesteps; ++seq) {
			int step = 0;
			for (String[] positions : steps) {
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

				core.snapImage();

				++step;
			}

			core.sleep(timestep);
		}
	};

	private static double parseX(String pair) {
		return Double.parseDouble(pair.substring(0, pair.indexOf(',')));
	};

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
	};

	private Vector<String> getUsedDevs() {
		Vector<String> res = new Vector<String>();

		for (int i = 0; i < stepsTbl.getModel().getColumnCount(); ++i)
			res.add(stepsTbl.getModel().getColumnName(i));

		return res;
	};
};