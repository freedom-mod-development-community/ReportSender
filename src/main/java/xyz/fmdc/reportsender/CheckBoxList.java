package xyz.fmdc.reportsender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CheckBoxList implements MouseListener {
    private final JList<JCheckBox> list;
    private final DefaultListModel<JCheckBox> destinationModel;

    public CheckBoxList(JList<JCheckBox> list, DefaultListModel<JCheckBox> destinationModel) {
        this.list = list;
        this.destinationModel = destinationModel;
    }

    static class CheckBoxRenderer extends JCheckBox implements ListCellRenderer<JCheckBox> {
        @Override
        public Component getListCellRendererComponent(JList<? extends JCheckBox> list, JCheckBox value, int index, boolean isSelected, boolean cellHasFocus) {
            /* 項目の値を読み出して改めて表示する */
            setText(value.getText());
            setSelected(value.isSelected());
            setBackground(Color.WHITE);
            return this;
        }
    }

    public void mouseClicked(MouseEvent e) {
        /* クリックされた座標からIndex番号を取り出す */
        Point p = e.getPoint();
        int index = list.locationToIndex(p);

        JCheckBox checkBox = destinationModel.getElementAt(index);
        checkBox.setSelected(!checkBox.isSelected());

        /* 再描画してみる */
        list.repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
