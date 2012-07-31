package progacq;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.PanelUI;

import org.micromanager.utils.ReportingUtils;

public class RangeSlider extends JPanel implements ChangeListener, KeyListener {
	private static final long serialVersionUID = -4704266057756694946L;

	private JTextField min, step, max;
	private JSlider sliderMin, sliderStep, sliderMax;
	private boolean triggering;

	public RangeSlider(Double minv, Double maxv) {
		triggering = true;

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));

		final JLabel minLbl = new JLabel("Min:");
		final JLabel maxLbl = new JLabel("Max:");
		final JLabel stpLbl = new JLabel("Step:");

		min = new JTextField(8);
		min.setMaximumSize(min.getPreferredSize());
		min.setAlignmentX(LEFT_ALIGNMENT);
		min.addKeyListener(this);

		step = new JTextField(8);
		step.setMaximumSize(step.getPreferredSize());
		step.setAlignmentX(CENTER_ALIGNMENT);
		step.addKeyListener(this);

		max = new JTextField(8);
		max.setMaximumSize(max.getPreferredSize());
		max.setAlignmentX(RIGHT_ALIGNMENT);
		max.addKeyListener(this);

		top.add(minLbl);
		top.add(min);
		top.add(Box.createRigidArea(new Dimension(4, 10)));
		top.add(stpLbl);
		top.add(step);
		top.add(Box.createRigidArea(new Dimension(4, 10)));
		top.add(maxLbl);
		top.add(max);

		add(top);

		JPanel bot = new JPanel();
		bot.setLayout(new BoxLayout(bot, BoxLayout.LINE_AXIS));

		sliderMin = new JSlider(minv.intValue(), maxv.intValue(),
				minv.intValue());
		sliderMin.setPaintTicks(true);
		sliderMin.setPaintLabels(true);
		sliderMin.addChangeListener(this);

		sliderMax = new JSlider(minv.intValue(), maxv.intValue(),
				maxv.intValue());
		sliderMax.setPaintTicks(true);
		sliderMax.setPaintLabels(true);
		sliderMax.addChangeListener(this);

		bot.add(sliderMin);
		bot.add(sliderMax);

		add(bot);

		JPanel stepBox = new JPanel();
		stepBox.setLayout(new BoxLayout(stepBox, BoxLayout.LINE_AXIS));

		sliderStep = new JSlider();
		sliderStep.setPaintLabels(true);
		sliderStep.setPaintTicks(true);
		sliderStep.addChangeListener(this);

		setMinMax(minv, maxv);

		stepBox.add(new JLabel("Step size:"));
		stepBox.add(sliderStep);

		add(stepBox);

		this.setUI(new PanelUI() {
			@Override
			public Dimension getPreferredSize(JComponent c) {
				int width = minLbl.getPreferredSize().width
						+ min.getPreferredSize().width
						+ stpLbl.getPreferredSize().width
						+ step.getPreferredSize().width
						+ maxLbl.getPreferredSize().width
						+ max.getPreferredSize().width;

				int height = min.getPreferredSize().height
						+ sliderMin.getPreferredSize().height
						+ sliderStep.getPreferredSize().height;

				return new Dimension(width, height);
			}
		});

		triggering = false;
	}

	public double[] getRange() {
		return new double[] { Double.parseDouble(min.getText()),
				Double.parseDouble(step.getText()),
				Double.parseDouble(max.getText()) };
	}

	private static Dictionary<Integer, JLabel> makeLabelTable(int min, int max,
			int step, int round, int align) {
		if (Math.abs(step) < 1)
			step = 1;

		int count = (max - min) / step;

		Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();

		table.put(min, new JLabel("" + min));
		table.put(max, new JLabel("" + max));

		float start = min;

		if (align == 0) {
			float offset = ((max - min) % step) / 2;

			start = min + (int) offset;
		} else if (align > 0) {
			start = max;
			step = -step;
		}

		for (int lbl = 1; lbl < count; ++lbl) {
			float nearPos = start + step * lbl;

			if (round > 0)
				nearPos = Math.round(nearPos / round) * round;

			ReportingUtils.logMessage("" + lbl + ": Putting " + (int) nearPos
					+ " (" + nearPos + ")");
			table.put((int) nearPos, new JLabel("" + (int) nearPos));
		}

		return table;
	}

	public void setMinMax(Double imin, Double imax) {
		int estStep = (int) Math.round((imax - imin) / 2);
		int estStepFlr = (int) ((imax - imin) / 2);

		sliderMin.setMinimum(imin.intValue());
		sliderMin.setLabelTable(makeLabelTable(imin.intValue(),
				imax.intValue(), estStepFlr / 2, estStep / 2, 0));
		sliderMin.setMinorTickSpacing(estStepFlr / 2);
		sliderMin.setMajorTickSpacing(estStepFlr);

		sliderMax.setMaximum(imax.intValue());
		sliderMax.setLabelTable(makeLabelTable(imin.intValue(),
				imax.intValue(), estStepFlr / 2, estStep / 2, 0));
		sliderMax.setMinorTickSpacing(estStepFlr / 2);
		sliderMax.setMajorTickSpacing(estStepFlr);

		sliderStep.setMaximum(estStep * 2);
		sliderStep.setValue(estStep);
		sliderStep.setLabelTable(makeLabelTable(0, estStep * 2, estStep / 2,
				estStep / 2, 0));
		sliderStep.setMinorTickSpacing(estStepFlr / 2);
		sliderStep.setMajorTickSpacing(estStepFlr);

		min.setText("" + imin.intValue());
		step.setText("" + estStep);
		max.setText("" + imax.intValue());
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// A slider moved. Figure out which and update everything else.
		if (triggering)
			return;
		else
			triggering = true;

		if (e.getSource().equals(sliderMin)) {
			sliderMax.setMinimum(sliderMin.getValue());
			min.setText("" + sliderMin.getValue());
		} else if (e.getSource().equals(sliderMax)) {
			sliderMin.setMaximum(sliderMax.getValue());
			max.setText("" + sliderMax.getValue());
		} else if (e.getSource().equals(sliderStep)) {
			int val = sliderStep.getValue();
			step.setText("" + val);

			// Need to clamp this for label generation.
			// Note that the value of the text box should never be clamped.
			if (val <= 0)
				val = 1;

			sliderMin.setLabelTable(sliderMin.createStandardLabels(val * 2));
			sliderMax.setLabelTable(sliderMax.createStandardLabels(val * 2));
			sliderMin.setMajorTickSpacing(val * 2);
			sliderMax.setMajorTickSpacing(val * 2);
			sliderMin.setMinorTickSpacing(val);
			sliderMax.setMinorTickSpacing(val);
		}

		triggering = false;
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);

		min.setEnabled(b);
		step.setEnabled(b);
		max.setEnabled(b);
		sliderMin.setEnabled(b);
		sliderStep.setEnabled(b);
		sliderMax.setEnabled(b);
	};

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (triggering)
			return;
		else
			triggering = true;

		int value = 1;

		try {
			value = Integer.parseInt(((JTextField) e.getComponent()).getText());
		} catch (NumberFormatException nfe) {
			triggering = false;
			return;
		}

		if (e.getComponent().equals(min)) {
			sliderMin.setValue(value);
			sliderMax.setMinimum(value);
		} else if (e.getComponent().equals(max)) {
			sliderMin.setMaximum(value);
			sliderMax.setValue(value);
		} else if (e.getComponent().equals(step)) {
			sliderStep.setValue(value);
			if (value <= 0)
				value = 1;

			sliderMin.setMajorTickSpacing(value * 2);
			sliderMax.setMajorTickSpacing(value * 2);
			sliderMin.setMinorTickSpacing(value);
			sliderMax.setMinorTickSpacing(value);
			sliderMin.setLabelTable(sliderMin.createStandardLabels(value * 2));
			sliderMax.setLabelTable(sliderMax.createStandardLabels(value * 2));
		}

		triggering = false;
	}
}
