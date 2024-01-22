package com.sjhy.plugin.ui.component;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.entity.ColumnConfig;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.enums.ColumnConfigType;
import com.sjhy.plugin.factory.CellEditorFactory;
import com.sjhy.plugin.tool.CurrGroupUtils;
import com.sjhy.plugin.tool.StringUtils;
import com.sjhy.plugin.ui.TableConfigHeaderMouseAdapter;
import com.sjhy.plugin.ui.base.ConfigTableModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * TableConfigJBTabs
 *
 * @author 单红宇
 * @date 2024/1/18 11:08
 */
public class TableConfigJBTabs extends JBTabsImpl {

    /**
     * tableInfoList
     */
    private final List<TableInfo> tableInfoList;

    /**
     * totalWidth
     */
    @Getter
    private int totalMinWidth = 0;

    /**
     * TableConfigJBTabs
     *
     * @param project          project
     * @param parentDisposable parentDisposable
     * @param tableInfoList    tableInfoList
     */
    public TableConfigJBTabs(@Nullable Project project, @NotNull Disposable parentDisposable, @NotNull List<TableInfo> tableInfoList) {
        super(project, parentDisposable);
        this.tableInfoList = tableInfoList;
        this.init();
    }

    /**
     * TableConfigJBTabs
     *
     * @param project       project
     * @param tableInfoList tableInfoList
     */
    public TableConfigJBTabs(@NotNull Project project, @NotNull List<TableInfo> tableInfoList) {
        super(project);
        this.tableInfoList = tableInfoList;
        this.init();
    }

    /**
     * init
     */
    public void init() {
        // 自定义一个 CloseButtonAction extends AnAction
        DefaultActionGroup tabActions = new DefaultActionGroup(new TabCloseButtonAction(this));
        for (int i = 0; i < tableInfoList.size(); i++) {
            TableInfo tableInfo = tableInfoList.get(i);
            JBTable jbTable = this.createJbTable(tableInfo);
            // 计算表宽totalWidth
            if (i == 0) {
                Enumeration<TableColumn> tableColumnEnumeration = jbTable.getColumnModel().getColumns();
                while (tableColumnEnumeration.hasMoreElements()) {
                    totalMinWidth += tableColumnEnumeration.nextElement().getMinWidth();
                }
            }
            ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(jbTable);
            String tabName = tableInfo.getObj().getName();
            TabInfo tabInfo = new TabInfo(toolbarDecorator.createPanel()).setText(tabName);
            tabInfo.setTabLabelActions(tabActions, tabName);
            tabInfo.setTabColor(new JBColor(new Color( 43, 45, 46), new Color(78, 82, 84)));
            this.addTab(tabInfo);
        }
    }

    /**
     * 基于tableInfo创建JBTable
     *
     * @param tableInfo tableInfo
     * @return JBTable
     */
    @NotNull
    private JBTable createJbTable(TableInfo tableInfo) {
        ConfigTableModel model = new ConfigTableModel(tableInfo);
        JBTable jbTable = new JBTable(model);
        jbTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.rendererTableHeader(jbTable);

        // 配置列编辑器
        TableColumn nameColumn = jbTable.getColumn(ConfigTableModel.TITLE_NAME);
        nameColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        nameColumn.setMinWidth(100);

        TableColumn typeColumn = jbTable.getColumn(ConfigTableModel.TITLE_TYPE);
        typeColumn.setCellRenderer(new ComboBoxTableRenderer<>(GlobalDict.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setCellEditor(CellEditorFactory.createComboBoxEditor(true, GlobalDict.DEFAULT_JAVA_TYPE_LIST));
        typeColumn.setMinWidth(163);

        TableColumn commentColumn = jbTable.getColumn(ConfigTableModel.TITLE_COMMENT);
        commentColumn.setCellEditor(CellEditorFactory.createTextFieldEditor());
        commentColumn.setMinWidth(140);

        TableColumn pkColumn = jbTable.getColumn(ConfigTableModel.TITLE_PK);
        pkColumn.setCellRenderer(new BooleanTableCellRenderer());
        pkColumn.setCellEditor(new BooleanTableCellEditor());
        pkColumn.setMinWidth(50);
        pkColumn.setMaxWidth(50);
        pkColumn.setResizable(false);

        // 其他附加列
        for (ColumnConfig columnConfig : CurrGroupUtils.getCurrColumnConfigGroup().getElementList()) {
            TableColumn column = jbTable.getColumn(columnConfig.getTitle());
            switch (columnConfig.getType()) {
                case TEXT:
                    column.setCellEditor(CellEditorFactory.createTextFieldEditor());
                    column.setMinWidth(120);
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
                    break;
                case BOOLEAN:
                    column.setCellRenderer(new BooleanTableCellRenderer());
                    column.setCellEditor(new BooleanTableCellEditor());
                    column.setMinWidth(60);
                    break;
                default:
                    break;
            }
        }
        return jbTable;
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
                ColumnConfig columnConfig = ((ConfigTableModel) table.getModel()).getColumnConfig(column);
                if (columnConfig != null && ColumnConfigType.BOOLEAN.equals(columnConfig.getType())) {
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

}
