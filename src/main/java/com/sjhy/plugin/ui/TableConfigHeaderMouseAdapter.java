package com.sjhy.plugin.ui;

import com.sjhy.plugin.entity.ColumnConfig;
import com.sjhy.plugin.enums.ColumnConfigType;
import com.sjhy.plugin.ui.base.ConfigTableModel;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * 表头鼠标悬停和点击处理器
 *
 * @author 单红宇
 * @date 2024/1/10 16:15
 */
public class TableConfigHeaderMouseAdapter extends MouseInputAdapter {

    private final JTable table;

    /**
     * 构造方法
     *
     * @param table table
     */
    public TableConfigHeaderMouseAdapter(JTable table) {
        this.table = table;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        this.processBooleanColumnChecked(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.processMouseStyle(e);
    }

    /**
     * 处理Boolean列的全选
     *
     * @param e e
     */
    private void processBooleanColumnChecked(MouseEvent e) {
        boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
        if (SwingUtilities.isLeftMouseButton(e) && isCtrlPressed) {
            Object clickSource = e.getSource();
            if (clickSource instanceof JTableHeader) {
                JTableHeader header = (JTableHeader) e.getSource();
                int columnIndex = header.columnAtPoint(e.getPoint());
                if (columnIndex > -1) { // 防止点击区域不在任何列上
                    // 根据列的索引读取TableColumn，然后判断该列是否为Boolean列
                    ColumnConfig columnConfig = ((ConfigTableModel) table.getModel()).getColumnConfig(columnIndex);
                    if(columnConfig.getType().equals(ColumnConfigType.BOOLEAN)) {
                        toggleSelectAllRowsInColumn(columnIndex);
                    }
                }
            }
        }
    }

    /**
     * 全选/取消全选
     *
     * @param columnIndex columnIndex
     */
    private void toggleSelectAllRowsInColumn(int columnIndex) {
        // 该列所有行的勾选框如果都是勾选，则全部变为不勾选
        // 否则全部勾选（包含部分勾选和全部未勾选）
        boolean isAllChecked = true;
        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            if(Boolean.FALSE.equals(table.getValueAt(row, columnIndex))) {
                isAllChecked = false;
                break;
            }
        }
        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            // 如果该单元格式不可编辑的，则跳过
            if(!table.isCellEditable(row, columnIndex)) {
                continue;
            }
            table.setValueAt(!isAllChecked, row, columnIndex);
        }
    }

    /**
     * 处理鼠标样式
     *
     * @param e e
     */
    private void processMouseStyle(MouseEvent e) {
        boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
        // 鼠标移动到表头上，并且按住了Ctrl键，并且这一列是Boolean的列
        JTableHeader header = (JTableHeader) e.getSource();
        if (isCtrlPressed) {
            int columnIndex = header.columnAtPoint(e.getPoint());
            if (columnIndex > -1) { // 防止点击区域不在任何列上
                ColumnConfig columnConfig = ((ConfigTableModel) table.getModel()).getColumnConfig(columnIndex);
                if(columnConfig.getType().equals(ColumnConfigType.BOOLEAN)) {
                    header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
        }
        header.setCursor(Cursor.getDefaultCursor());
    }

}

