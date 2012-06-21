package progacq;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mmcorej.CMMCore;
import mmcorej.DeviceType;

public class DiscreteValuesDialog extends JDialog implements ActionListener,
		KeyListener {
	private static final long serialVersionUID = 6426560743323818047L;

	// Floating-point number.
	private static String FP = "[-+]?\\d*\\.?\\d+(?:[eE][-+]?[0-9]+)?";
	// Flanked (by spaces) colon.
	private static String FC = "\\s*:\\s*";

	// MATLAB style ranges -- (min : step : max)
	private static String ML = "\\(\\s*" + FP + FC + FP + FC + FP + "\\s*\\)";
	// Discrete array elements -- [a, b, c, d]
	private static String AE = "\\[((?:" + FP + "\\s*,?\\s*)+)\\]";
	// Repetition braces -- {value, count}
	private static String RB = "\\{" + FP + "\\s*,\\s*" + FP + "\\}";

	private static Pattern FPP = Pattern.compile(FP);
	private static Pattern discChunk = Pattern.compile("(?:" + ML + "|" + AE
			+ "|" + RB + ")");

	private CMMCore core;
	private List<JTextField> fields;
	private List<JLabel> labels;
	private List<Double[]> values;
	private String[] devs;

	public static void doInstance(JFrame f, CMMCore c, List<String> de,
			WindowListener w) {
		DiscreteValuesDialog d = new DiscreteValuesDialog(f, c, de);

		d.addWindowListener(w);

		d.setVisible(true);
	}

	public DiscreteValuesDialog(JFrame owner, CMMCore icore,
			List<String> devices) {
		super(owner, "Add Discrete Values");

		core = icore;

		devs = devices.toArray(new String[devices.size()]);

		fields = new Vector<JTextField>(devices.size());
		labels = new Vector<JLabel>(devices.size());

		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		JPanel top = new JPanel();

		JLabel instr = new JLabel(read("resources/disc_instr.txt"));
		top.add(instr);

		add(top);

		for (String dev : devices) {
			JPanel devCont = new JPanel();
			devCont.setBorder(BorderFactory.createTitledBorder(dev));

			try {
				if (core.getDeviceType(dev).equals(DeviceType.XYStageDevice)) {
					devCont.setLayout(new BoxLayout(devCont,
							BoxLayout.PAGE_AXIS));
					JPanel x = new JPanel();
					x.setBorder(BorderFactory.createTitledBorder("Stage X"));
					x.setLayout(new BoxLayout(x, BoxLayout.LINE_AXIS));

					JTextField xF = new JTextField(32);
					xF.setMaximumSize(xF.getPreferredSize());
					xF.addKeyListener(this);
					x.add(xF);
					fields.add(xF);

					x.add(Box.createHorizontalStrut(4));

					JLabel xFL = new JLabel("Values: 0");
					x.add(xFL);
					labels.add(xFL);

					x.setMaximumSize(x.getPreferredSize());
					devCont.add(x);

					JPanel y = new JPanel();
					y.setBorder(BorderFactory.createTitledBorder("Stage Y"));
					y.setLayout(new BoxLayout(y, BoxLayout.LINE_AXIS));

					JTextField yF = new JTextField(32);
					yF.setMaximumSize(yF.getPreferredSize());
					yF.addKeyListener(this);
					y.add(yF);
					fields.add(yF);

					y.add(Box.createHorizontalStrut(4));

					JLabel yFL = new JLabel("Values: 0");
					y.add(yFL);
					labels.add(yFL);

					y.setMaximumSize(y.getPreferredSize());
					devCont.add(y);
				} else if (core.getDeviceType(dev).equals(
						DeviceType.StageDevice)) {
					devCont.setLayout(new BoxLayout(devCont,
							BoxLayout.LINE_AXIS));

					JTextField f = new JTextField(32);
					f.addKeyListener(this);
					f.setMaximumSize(f.getPreferredSize());
					devCont.add(f);
					fields.add(f);

					devCont.add(Box.createHorizontalStrut(4));

					JLabel fL = new JLabel("Values: 0");
					devCont.add(fL);
					labels.add(fL);
				} else {
					throw new Exception("eh");
				}
			} catch (Exception e) {
				devCont.add(new JLabel(
						"Unknown device type? for this device! It will be ignored."));
				e.printStackTrace();
			} finally {
				devCont.setMaximumSize(devCont.getPreferredSize());
				add(devCont);
			}
		}

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));

		bottom.add(Box.createHorizontalGlue());

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		JButton ok = new JButton("OK");
		ok.addActionListener(this);

		bottom.add(cancel);
		bottom.add(ok);

		add(bottom);

		pack();
		setResizable(false);

		values = new Vector<Double[]>(fields.size());
		for (int i = 0; i < fields.size(); ++i)
			values.add(null);
	}

	/**
	 * Java is wonderful, but sometimes its oversights drive me insane.
	 * 
	 * @param filename
	 *            Path of a file to read.
	 * @return Contents of the file.
	 */
	private String read(String name) {
		try {
			InputStream str = getClass().getResourceAsStream(name);

			byte[] buf = new byte[(int) str.available()];

			str.read(buf);

			return new String(buf);
		} catch (Exception e) {
			e.printStackTrace();

			return "";
		}
	}

	private Double[] getAllValues(String in) throws Exception {
		Vector<Double> allVals = new Vector<Double>();

		Matcher m = discChunk.matcher(in);

		while (m.find()) {
			switch (m.group().charAt(0)) {
			case '[': {
				Matcher numbers = FPP.matcher(m.group());
				while (numbers.find())
					allVals.add(Double.parseDouble(numbers.group()));
				break;
			}
			case '(': {
				Matcher numbers = FPP.matcher(m.group());
				if (!numbers.find())
					throw new Exception("Invalid MATLAB range \"" + m.group()
							+ "\"");
				double min = Double.parseDouble(numbers.group());

				if (!numbers.find())
					throw new Exception("Invalid MATLAB range \"" + m.group()
							+ "\"");
				double step = Double.parseDouble(numbers.group());
				if (step == 0)
					throw new Exception("Step size 0!");

				if (!numbers.find())
					throw new Exception("Invalid MATLAB range \"" + m.group()
							+ "\"");
				double max = Double.parseDouble(numbers.group());

				Double[] discs = new Double[(int) ((max - min) / step) + 1];
				for (int i = 0; i < discs.length; ++i)
					discs[i] = min + step * i;

				allVals.addAll(Arrays.asList(discs));
				break;
			}
			case '{': {
				Matcher numbers = FPP.matcher(m.group());

				if (!numbers.find())
					throw new Exception("Invalid reptition brace \""
							+ m.group() + "\"");
				double val = Double.parseDouble(numbers.group());

				if (!numbers.find())
					throw new Exception("Invalid repetition brace \""
							+ m.group() + "\"");
				int count = Integer.parseInt(numbers.group());

				for (int i = 0; i < count; ++i)
					allVals.add(val);

				break;
			}
			default:
				throw new Exception("Unknown chunk type for \"" + m.group()
						+ "\"");
			}
		}

		return allVals.toArray(new Double[allVals.size()]);
	}

	public List<String[]> getRows() {
		if (values == null)
			return null;

		Vector<String[]> rows = new Vector<String[]>(values.get(0).length);

		for (int val = 0; val < values.get(0).length; ++val) {
			Vector<String> row = new Vector<String>();

			int skip = 0;
			for (int dev = 0; dev < devs.length; ++dev) {
				try {
					if (core.getDeviceType(devs[dev]).equals(
							DeviceType.XYStageDevice)) {
						row.add(values.get(dev + skip)[val].toString() + ", "
								+ values.get(dev + (++skip))[val].toString());
					} else if (core.getDeviceType(devs[dev]).equals(
							DeviceType.StageDevice)) {
						row.add(values.get(dev + skip)[val].toString());
					} else {
						row.add("");
					}
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this,
							"Error determining device type for \"" + devs[dev]
									+ "\"!");
					return null;
				}
			}

			rows.add(row.toArray(new String[row.size()]));
		}

		return rows;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		JTextField source = (JTextField) e.getComponent();

		int devIdx = fields.indexOf(source);

		JLabel sourceL = labels.get(devIdx);
		try {
			Double[] vals = getAllValues(source.getText());

			values.set(devIdx, vals);

			sourceL.setText("Values: " + vals.length);
		} catch (Exception e1) {
			// For now, don't care. Only show messages if they try to add the
			// rows with an invalid string.
			sourceL.setText(e1.getMessage());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("Cancel".equals(e.getActionCommand())) {
			values = null;
		} else if ("OK".equals(e.getActionCommand())) {
			String errors = "";

			int first = -1;
			for (int i = 0; i < fields.size(); ++i) {
				try {
					Double[] vals = getAllValues(fields.get(i).getText());

					values.set(i, vals);

					labels.get(i).setText("Values: " + vals.length);

					if (first == -1) {
						first = vals.length;
					} else if (first >= 0 && vals.length != first) {
						errors += "All devices must have the same number of values!\n";
						first = -2;
					}
				} catch (Exception e1) {
					errors += e1.getMessage() + "\n";
				}
			}

			if (!errors.isEmpty()) {
				JOptionPane.showMessageDialog(this, errors);
				return;
			}
		}

		this.dispose();
	}
}
