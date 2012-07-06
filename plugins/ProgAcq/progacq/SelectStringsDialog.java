package progacq;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

public class SelectStringsDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -8892280229379680840L;
	private JList left, right;
	private static Vector<String> outputList;
	private JButton oneLeft, oneRight, allLeft, allRight;
	private JButton moveUp, moveDown, moveTop, moveBottom;
	private JButton ok, cancel;

	public static void doInstance(JFrame owner, List<String> left,
			List<String> right, WindowListener done) {
		SelectStringsDialog ssd = new SelectStringsDialog(owner, left, right);

		ssd.addWindowListener(done);
		ssd.setVisible(true);
	};

	public SelectStringsDialog(JFrame owner, List<String> leftList,
			List<String> rightList) {
		super(owner, "Select", true);

		outputList = new Vector<String>(rightList);

		DefaultListModel leftListModel = new DefaultListModel();
		addAll(leftList, leftListModel);

		DefaultListModel rightListModel = new DefaultListModel();
		addAll(rightList, rightListModel);

		left = new JList(leftListModel);
		left.setFixedCellWidth(128);
		left.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		JScrollPane leftScroller = new JScrollPane(left);
		leftScroller.setSize(128, 128);

		right = new JList(rightListModel);
		right.setFixedCellWidth(128);
		right.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		JScrollPane rightScroller = new JScrollPane(right);
		rightScroller.setSize(128, 128);

		// TODO: Polish the UI more; replace all these with icons?
		allLeft = new JButton("<<");
		allLeft.addActionListener(this);

		oneLeft = new JButton("<");
		oneLeft.addActionListener(this);

		oneRight = new JButton(">");
		oneRight.addActionListener(this);

		allRight = new JButton(">>");
		allRight.addActionListener(this);

		moveUp = new JButton("Up");
		moveUp.addActionListener(this);

		moveDown = new JButton("Down");
		moveDown.addActionListener(this);

		moveTop = new JButton("Top");
		moveTop.addActionListener(this);

		moveBottom = new JButton("Bottom");
		moveBottom.addActionListener(this);

		ok = new JButton("OK");
		ok.addActionListener(this);

		cancel = new JButton("Cancel");
		cancel.addActionListener(this);

		getContentPane().setLayout(
				new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		JPanel lists = new JPanel();
		lists.setLayout(new BoxLayout(lists, BoxLayout.LINE_AXIS));

		lists.add(leftScroller);

		JPanel shuttlers = new JPanel();
		shuttlers.setLayout(new BoxLayout(shuttlers, BoxLayout.PAGE_AXIS));

		shuttlers.add(allLeft);
		shuttlers.add(oneLeft);
		shuttlers.add(oneRight);
		shuttlers.add(allRight);

		lists.add(shuttlers);

		lists.add(rightScroller);

		JPanel orders = new JPanel();
		orders.setLayout(new BoxLayout(orders, BoxLayout.PAGE_AXIS));

		orders.add(moveTop);
		orders.add(moveUp);
		orders.add(moveDown);
		orders.add(moveBottom);

		lists.add(orders);

		this.getContentPane().add(lists);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));

		buttons.add(ok);
		buttons.add(cancel);

		this.getContentPane().add(buttons);

		this.pack();
	};

	public static Vector<String> getFinalList() {
		return outputList;
	}

	private static Vector<String> getAll(JList right) {
		Vector<String> res = new Vector<String>(right.getModel().getSize());

		for (int i = 0; i < right.getModel().getSize(); ++i)
			res.add(right.getModel().getElementAt(i).toString());

		return res;
	};

	private static void addAll(List<String> from, DefaultListModel to) {
		for (String val : from)
			to.addElement(val);
	};

	private static void transferItems(JList from, Object[] values, JList to) {
		DefaultListModel fromLM = (DefaultListModel) (from.getModel());
		DefaultListModel toLM = (DefaultListModel) (to.getModel());

		for (int i = 0; i < values.length; ++i) {
			toLM.add(to.getMaxSelectionIndex() > 0 ? to.getMaxSelectionIndex()
					: to.getModel().getSize(), values[i]);
			fromLM.removeElement(values[i]);
		}
	};

	private static void moveItems(JList on, int delta) {
		// TODO: This might be neater if I just extend DefaultListModel to have
		// a reorder function. Then I can work with the data directly, instead
		// of turning an array into a list and splicing it all over. In
		// addition, moving several values down needs to be fixed. Basically,
		// don't trust this beyond moving single values up and down.

		DefaultListModel dlm = (DefaultListModel) on.getModel();

		Object[] values = dlm.toArray();
		int[] selection = on.getSelectedIndices();

		for (int i = 0; i < selection.length; ++i) {
			int idx = selection[i];
			if (idx + delta < 0 || idx + delta >= values.length)
				return; // Stop moving if we're at the end.
			selection[i] += delta;

			Object tmp = values[idx + delta];
			values[idx + delta] = values[idx];
			values[idx] = tmp;
		}

		dlm.clear();
		for (Object val : values)
			dlm.addElement(val);

		on.setSelectedIndices(selection);
	}

	public void actionPerformed(ActionEvent e) {
		if (">".equals(e.getActionCommand())) {
			transferItems(left, left.getSelectedValues(), right);
		} else if (">>".equals(e.getActionCommand())) {
			transferItems(left, ((DefaultListModel) left.getModel()).toArray(),
					right);
		} else if ("<".equals(e.getActionCommand())) {
			transferItems(right, right.getSelectedValues(), left);
		} else if ("<<".equals(e.getActionCommand())) {
			transferItems(right,
					((DefaultListModel) right.getModel()).toArray(), left);
		} else if ("Up".equals(e.getActionCommand())) {
			moveItems(right, -1);
		} else if ("Down".equals(e.getActionCommand())) {
			moveItems(right, 1);
		} else if ("Top".equals(e.getActionCommand())) {
			moveItems(right, -right.getMinSelectionIndex());
		} else if ("Bottom".equals(e.getActionCommand())) {
			moveItems(right,
					right.getModel().getSize() - right.getMaxSelectionIndex()
							- 1);
		} else if ("OK".equals(e.getActionCommand())) {
			outputList = getAll(right);
			this.dispose();
		} else if ("Cancel".equals(e.getActionCommand())) {
			this.dispose();
		} else {
			throw new RuntimeException("Who broke the action listener? :|");
		}
	};
};