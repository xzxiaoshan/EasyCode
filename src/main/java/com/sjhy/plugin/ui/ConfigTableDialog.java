package com.sjhy.plugin.ui;

import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.entity.ColumnConfig;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.enums.ColumnConfigType;
import com.sjhy.plugin.factory.CellEditorFactory;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.CacheDataUtils;
import com.sjhy.plugin.tool.CurrGroupUtils;
import com.sjhy.plugin.tool.ProjectUtils;
import com.sjhy.plugin.tool.StringUtils;
import com.sjhy.plugin.ui.base.ConfigTableModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 表配置窗口
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class ConfigTableDialog extends DialogWrapper {
    /**
     * 主面板
     */
    private final JPanel mainPanel;
    /**
     * 表信息对象
     */
    private TableInfo tableInfo;

    public ConfigTableDialog() {
        super(ProjectUtils.getCurrProject());
        this.mainPanel = new JPanel(new BorderLayout());
        this.initPanel();
    }

    private void initPanel() {
        init();
        this.tableInfo = TableInfoSettingsService.getInstance().getTableInfo(CacheDataUtils.getInstance().getSelectDbTable());
        setTitle("Config Table " + this.tableInfo.getObj().getName());
        ConfigTableModel model = new ConfigTableModel(this.tableInfo);
        JBTable table = new JBTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.rendererTableHeader(table);

        int totalWidth = 0;

        // 配置列编辑器
        TableColumn nameColumn = table.getColumn(ConfigTableModel.TITLE_NAME);
        nameColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        nameColumn.setMinWidth(100);

        totalWidth+=100;
        TableColumn typeColumn = table.getColumn(ConfigTableModel.TITLE_TYPE);
        typeColumn.setCellRenderer(new ComboBoxTableRenderer<>(GlobalDict.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setCellEditor(CellEditorFactory.createComboBoxEditor(true, GlobalDict.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setMinWidth(163);
        totalWidth+=163;
        TableColumn commentColumn = table.getColumn(ConfigTableModel.TITLE_COMMENT);
        commentColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        commentColumn.setMinWidth(140);
        totalWidth+=140;
        TableColumn pkColumn = table.getColumn(ConfigTableModel.TITLE_PK);
        pkColumn.setCellRenderer(new BooleanTableCellRenderer());
        pkColumn.setCellEditor(new BooleanTableCellEditor());
        pkColumn.setMinWidth(50);
        pkColumn.setMaxWidth(50);
        pkColumn.setResizable(false);
        totalWidth+=50;
        // 其他附加列
        for (ColumnConfig columnConfig : CurrGroupUtils.getCurrColumnConfigGroup().getElementList()) {
            TableColumn column = table.getColumn(columnConfig.getTitle());
            switch (columnConfig.getType()) {
                case TEXT:
                    column.setCellEditor(CellEditorFactory.createTextFieldEditor());
                    column.setMinWidth(120);
                    totalWidth+=120;
                    break;
                case SELECT:
                    if (StringUtils.isEmpty(columnConfig.getSelectValue())) {
                        column.setCellEditor(CellEditorFactory.createTextFieldEditor());
                    } else {
                        String[] split = columnConfig.getSelectValue().split(",");
                        ArrayList<String> list = new ArrayList<>(Arrays.asList(split));
                        // 添加一个空值作为默认值
                        list.add(0, "");
                        split = list.toArray(new String[0]);
                        column.setCellRenderer(new ComboBoxTableRenderer<>(split));
                        column.setCellEditor(CellEditorFactory.createComboBoxEditor(false, split));
                    }
                    column.setMinWidth(100);
                    totalWidth+=100;
                    break;
                case BOOLEAN:
                    column.setCellRenderer(new BooleanTableCellRenderer());
                    column.setCellEditor(new BooleanTableCellEditor());
                    column.setMinWidth(60);
                    totalWidth+=60;
                    break;
                default:
                    break;
            }
        }

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table);
        this.mainPanel.add(decorator.createPanel(), BorderLayout.CENTER);
        this.mainPanel.setMinimumSize(new Dimension(totalWidth, Math.max(300, totalWidth / 3)));
    }

    /**
     * 渲染表头
     *
     * @param table table
     */
    private void rendererTableHeader(JBTable table) {
        // 获取JTable的表头组件
        JTableHeader header = table.getTableHeader();

        // 设置表头单元格渲染器
        header.setDefaultRenderer(new DefaultTableCellRenderer() {


            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                // 调用父类方法初始化渲染器组件
                JLabel renderer = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ColumnConfig columnConfig = ((ConfigTableModel)table.getModel()).getColumnConfig(column);
                if(columnConfig != null && ColumnConfigType.BOOLEAN.equals(columnConfig.getType())) {
                    // BOOLEAN类型的类，设置文本对齐方式为居中
                    renderer.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    renderer.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return renderer;
            }
        });

        TableConfigHeaderMouseAdapter tableConfigHeaderMouseAdapter = new TableConfigHeaderMouseAdapter(table);
        header.addMouseListener(tableConfigHeaderMouseAdapter);
        header.addMouseMotionListener(tableConfigHeaderMouseAdapter);

        // 确保所有列都刷新以应用新地渲染器
        table.getTableHeader().repaint();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.mainPanel;
    }

    @Override
    protected void doOKAction() {
        // 保存信息
        TableInfoSettingsService.getInstance().saveTableInfo(tableInfo);
        super.doOKAction();
    }

}
