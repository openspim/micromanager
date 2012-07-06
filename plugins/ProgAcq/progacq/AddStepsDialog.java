package progacq;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
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

public class AddStepsDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 7649155758354822637L;

	private Vector<JPanel> controls;

	// The outer dimension here is unused for everything but XYStage devs.
	// XYStageDevs have a second min/step/max (obviously; for X/Y).
	private Vector<double[]> results;

	/**
	 * Generates an instance of this class and displays it. The window listener
	 * will be alerted when the popup is closed.
	 * 
	 * @param parent
	 *            Passed to the super constructor (required by JDialog).
	 * @param core
	 *            The Micro-Manager core the devices exist on (for info).
	 * @param devices
	 *            The list of devices to request ranges for.
	 * @param listener
	 *            A window listener to alert when the popup closes.
	 */
	public static void doInstance(JFrame parent, CMMCore core,
			List<String> devices, WindowListener listener) {
		AddStepsDialog dlg = new AddStepsDialog(parent, core, devices);

		dlg.addWindowListener(listener);
		dlg.setVisible(true);
	}

	public AddStepsDialog(JFrame parent, CMMCore core, List<String> devices) {
		super(parent, "Add Steps");

		JPanel master = new JPanel();
		master.setLayout(new BoxLayout(master, BoxLayout.PAGE_AXIS));

		JPanel horizPanel = new JPanel();
		horizPanel.setLayout(new BoxLayout(horizPanel, BoxLayout.LINE_AXIS));

		controls = new Vector<JPanel>(devices.size());

		for (String name : devices) {
			JPanel devPanel = new JPanel();
			devPanel.setBorder(BorderFactory.createTitledBorder(name));

			try {
				if (core.getDeviceType(name).equals(DeviceType.StageDevice)) {
					devPanel.setLayout(new BoxLayout(devPanel,
							BoxLayout.PAGE_AXIS));

					devPanel.add(labelledField("Min:"));
					devPanel.add(labelledField("Step:"));
					devPanel.add(labelledField("Max:"));

					controls.add(devPanel);
				} else if (core.getDeviceType(name).equals(
						DeviceType.XYStageDevice)) {
					devPanel.setLayout(new BoxLayout(devPanel,
							BoxLayout.LINE_AXIS));
					JPanel x = new JPanel();
					x.setLayout(new BoxLayout(x, BoxLayout.PAGE_AXIS));
					x.setBorder(BorderFactory.createTitledBorder("Stage X"));

					x.add(labelledField("Min:"));
					x.add(labelledField("Step:"));
					x.add(labelledField("Max:"));

					JPanel y = new JPanel();
					y.setLayout(new BoxLayout(y, BoxLayout.PAGE_AXIS));
					y.setBorder(BorderFactory.createTitledBorder("Stage Y"));

					y.add(labelledField("Min:"));
					y.add(labelledField("Step:"));
					y.add(labelledField("Max:"));

					devPanel.add(x);
					devPanel.add(y);

					controls.add(x);
					controls.add(y);
				} else {
					throw new Exception("Laziness win");
				}
			} catch (Exception e) {
				e.printStackTrace();

				JPanel vert = new JPanel();
				vert.setLayout(new BoxLayout(vert, BoxLayout.PAGE_AXIS));

				vert.add(new JLabel("Unknown device!"));
				vert.add(new JLabel(" "));
				vert.add(new JLabel("I don't know how"));
				vert.add(new JLabel("to program this"));
				vert.add(new JLabel("particular device."));

				devPanel.add(vert);

				controls.add(devPanel);
			}

			horizPanel.add(devPanel);
		}

		master.add(horizPanel);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));

		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(this);

		buttons.add(ok);
		buttons.add(cancel);

		master.add(buttons);

		this.getContentPane().add(master);
		this.pack();
	}

	private static JPanel labelledField(String lbl) {
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.LINE_AXIS));
		box.setAlignmentX(Component.RIGHT_ALIGNMENT);

		box.add(new JLabel(lbl));

		JTextField text = new JTextField(8);
		text.setMaximumSize(text.getPreferredSize());

		box.add(text);

		return box;
	}

	public List<double[]> getResults() {
		return results;
	}

	private JTextField getTextBox(Component[] comps, int trip) {
		JPanel line = (JPanel) comps[trip];

		return (JTextField) line.getComponent(1);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Find and extract all the information. Bleh.
		if ("Cancel".equals(e.getActionCommand())) {
			results = null;
			this.dispose();
			return;
		}

		results = new Vector<double[]>(controls.size());
		results.setSize(controls.size());

		for (int i = 0; i < controls.size(); ++i) {
			Component[] components = controls.get(i).getComponents();

			if (components.length == 3) {
				double[] values = new double[3];

				JTextField focus = null;
				try {
					focus = getTextBox(components, 0);
					values[0] = Double.parseDouble(focus.getText());

					focus = getTextBox(components, 1);
					values[1] = Double.parseDouble(focus.getText());

					focus = getTextBox(components, 2);
					values[2] = Double.parseDouble(focus.getText());
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(this,
							"Invalid or malformed number.");
					results = null;
					if (focus != null)
						focus.grabFocus();
					return;
				}

				results.set(i, values);
			} else {
				results.set(i, null);
			}
		}

		this.dispose();
	}
}
