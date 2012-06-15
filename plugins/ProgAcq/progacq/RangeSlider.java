package progacq;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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

public class RangeSlider extends JPanel implements ChangeListener, KeyListener {
	private static final long serialVersionUID = -4704266057756694946L;

	private JTextField min, step, max;
	private JSlider sliderMin, sliderStep, sliderMax;
	private boolean triggering;

	public RangeSlider(Double minv, Double maxv) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));

		JLabel minLbl = new JLabel("Min:");
		JLabel maxLbl = new JLabel("Max:");
		JLabel stpLbl = new JLabel("Step:");

		min = new JTextField(minv.toString(), 8);
		min.setMaximumSize(min.getPreferredSize());
		min.setAlignmentX(LEFT_ALIGNMENT);
		min.addKeyListener(this);

		int estStep = (int) ((maxv - minv) / 4);

		step = new JTextField("" + estStep, 8);
		step.setMaximumSize(step.getPreferredSize());
		step.setAlignmentX(CENTER_ALIGNMENT);
		step.addKeyListener(this);

		max = new JTextField(maxv.toString(), 8);
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

		left.add(top);

		JPanel bot = new JPanel();
		bot.setLayout(new BoxLayout(bot, BoxLayout.LINE_AXIS));

		sliderMin = new JSlider(minv.intValue(), maxv.intValue(),
				minv.intValue());
		sliderMin.setPaintTicks(true);
		sliderMin.setPaintLabels(true);
		sliderMin.setMinorTickSpacing(estStep / 2);
		sliderMin.setMajorTickSpacing(estStep);
		sliderMin.addChangeListener(this);

		sliderMax = new JSlider(minv.intValue(), maxv.intValue(),
				maxv.intValue());
		sliderMax.setPaintTicks(true);
		sliderMax.setPaintLabels(true);
		sliderMax.setMinorTickSpacing(estStep / 2);
		sliderMax.setMajorTickSpacing(estStep);
		sliderMax.addChangeListener(this);

		bot.add(sliderMin);
		bot.add(sliderMax);

		left.add(bot);

		this.add(left);

		sliderStep = new JSlider(JSlider.VERTICAL, 0, 2 * estStep, estStep);
		sliderStep.setPaintLabels(true);
		sliderStep.setPaintTicks(true);
		sliderStep.setMajorTickSpacing(estStep);
		sliderStep.setMinorTickSpacing(estStep / 2);
		sliderStep.addChangeListener(this);

		this.add(sliderStep);

		this.setUI(new PanelUI() {
			@Override
			public Dimension getPreferredSize(JComponent c) {
				int width = min.getPreferredSize().width
						+ step.getPreferredSize().width
						+ max.getPreferredSize().width
						+ sliderStep.getPreferredSize().width;

				int height = min.getPreferredSize().height
						+ sliderMin.getPreferredSize().height;

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

	public void setMinMax(Double min, Double max) {
		int estStep = (int) ((max - min) / 2);

		sliderMin.setMinimum(min.intValue());
		sliderMin.setMinorTickSpacing(estStep / 2);
		sliderMin.setMajorTickSpacing(estStep);
		sliderMin.setLabelTable(sliderMin.createStandardLabels(estStep));

		sliderMax.setMaximum(max.intValue());
		sliderMax.setMinorTickSpacing(estStep / 2);
		sliderMax.setMajorTickSpacing(estStep);
		sliderMax.setLabelTable(sliderMax.createStandardLabels(estStep));

		sliderStep.setMaximum(estStep * 2);
		sliderStep.setValue(estStep);
		sliderStep.setMinorTickSpacing(estStep / 2);
		sliderStep.setMajorTickSpacing(estStep);
		sliderStep.setLabelTable(sliderStep.createStandardLabels(estStep));

		step.setText("" + estStep);
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
			sliderMin.setMajorTickSpacing(val * 2);
			sliderMax.setMajorTickSpacing(val * 2);
			sliderMin.setMinorTickSpacing(val);
			sliderMax.setMinorTickSpacing(val);
			sliderMin.setLabelTable(sliderMin.createStandardLabels(val * 2));
			sliderMax.setLabelTable(sliderMax.createStandardLabels(val * 2));
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
