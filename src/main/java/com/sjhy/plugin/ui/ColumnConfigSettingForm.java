package com.sjhy.plugin.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.util.ui.JBUI;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.dto.SettingsStorageDTO;
import com.sjhy.plugin.entity.ColumnConfig;
import com.sjhy.plugin.entity.ColumnConfigGroup;
import com.sjhy.plugin.enums.ColumnConfigType;
import com.sjhy.plugin.factory.CellEditorFactory;
import com.sjhy.plugin.tool.CloneUtils;
import com.sjhy.plugin.ui.base.ConfigTableModel;
import com.sjhy.plugin.ui.component.GroupNameComponent;
import com.sjhy.plugin.ui.component.TableComponent;
import org.jetbrains.annotations.Nullable;
import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author makejava
 * @version 1.0.0
 * @date 2021/08/10 13:27
 */
public class ColumnConfigSettingForm implements Configurable, BaseSettings {
    private final JPanel mainPanel;
    /**
     * 列配置
     */
    private Map<String, ColumnConfigGroup> columnConfigGroupMap;
    /**
     * 当前分组名
     */
    private ColumnConfigGroup currColumnConfigGroup;
    /**
     * 表格组件
     */
    private TableComponent<ColumnConfig> tableComponent;
    /**
     * 分组操作组件
     */
    private GroupNameComponent<ColumnConfig, ColumnConfigGroup> groupNameComponent;

    public ColumnConfigSettingForm() {
        this.mainPanel = new JPanel(new BorderLayout());
    }

    private void initTable() {
        // 第一列，类型
        String[] columnConfigTypeNames = Stream.of(ColumnConfigType.values()).map(ColumnConfigType::name).toArray(String[]::new);
        TableCellEditor typeEditor = CellEditorFactory.createComboBoxEditor(false, columnConfigTypeNames);
        TableCellRenderer typeRenderer = new ComboBoxTableRenderer<>(columnConfigTypeNames);
        TableComponent.Column<ColumnConfig> typeColumn = new TableComponent.Column<>("type", item -> item.getType().name(), (entity, val) -> entity.setType(ColumnConfigType.valueOf(val)), typeEditor, typeRenderer);
        // 第二列标题
        TableCellEditor titleEditor = CellEditorFactory.createTextFieldEditor();
        this.restrictedInputContent((DefaultCellEditor)titleEditor);
        TableComponent.Column<ColumnConfig> titleColumn = new TableComponent.Column<>("title", ColumnConfig::getTitle, ColumnConfig::setTitle, titleEditor, null);

        // 第三列选项
        String column3Name = "defaultValue";
        TableCellEditor defaultValueEditor = CellEditorFactory.createTextFieldEditor();
        TableComponent.Column<ColumnConfig> defaultValueColumn = new TableComponent.Column<>(column3Name, ColumnConfig::getDefaultValue,
                ColumnConfig::setDefaultValue, defaultValueEditor, null);

        List<TableComponent.Column<ColumnConfig>> columns = Arrays.asList(typeColumn, titleColumn, defaultValueColumn);

        // 表格初始化
        this.tableComponent = new TableComponent<>(columns, this.currColumnConfigGroup.getElementList(), ColumnConfig.class);
        // 自定义表头渲染处理
        DefaultTableCellRenderer defaultTableCellHeaderRenderer = new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = DefaultLookup.getColor(this, ui, "TableHeader.focusCellForeground");
                    bgColor = DefaultLookup.getColor(this, ui, "TableHeader.focusCellBackground");
                }
                JTableHeader header = table.getTableHeader();
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);
                setBorder(JBUI.Borders.emptyLeft(5));

                if(column3Name.equals(value)) {
                    label.setToolTipText("1.SELECT类型使用英文逗号分隔多个下拉选项<br>2.TEXT类型为文本默认值<br>3.BOOLEAN类型可以填写true设定默认勾选");
                    label.setIcon(AllIcons.General.ContextHelp);
                } else {
                    label.setIcon(null);
                    label.setToolTipText(null);
                }
                return label;
            }
        };
        defaultTableCellHeaderRenderer.setHorizontalTextPosition(SwingConstants.LEFT);
        this.tableComponent.getTable().getTableHeader().setDefaultRenderer(defaultTableCellHeaderRenderer);
        this.mainPanel.add(this.tableComponent.createPanel(), BorderLayout.CENTER);
    }

    /**
     * 限制输入内容
     */
    private void restrictedInputContent(DefaultCellEditor cellEditor){
        // 通过监听器限制不能输入内置的默认列
        JTextField textField = (JTextField) cellEditor.getComponent();
        ((PlainDocument)textField.getDocument()).setDocumentFilter(new DocumentFilter(){
            private final String[] forbiddenValues = ConfigTableModel.DEFAULT_TITLE_ARRAY;
            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
                if (containsForbiddenWords(text) || containsForbiddenWords(fb.getDocument().getText(0, fb.getDocument().getLength()))) {
                    Toolkit.getDefaultToolkit().beep(); // 发出错误提示声音
                } else {
                    super.insertString(fb, offset, text, attrs);
                }
            }
            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
                if (containsForbiddenWords(newText)) {
                    Toolkit.getDefaultToolkit().beep(); // 发出错误提示声音
                } else {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            private boolean containsForbiddenWords(String input) {
                for (String word : forbiddenValues) {
                    if (input.equals(word)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }



    private void initGroupName() {

        // 切换分组操作
        Consumer<ColumnConfigGroup> switchGroupOperator = typeColumnConfigGroupMap -> {
            this.currColumnConfigGroup = typeColumnConfigGroupMap;
            refreshUiVal();
        };

        this.groupNameComponent = new GroupNameComponent<>(switchGroupOperator, this.columnConfigGroupMap);
        this.mainPanel.add(groupNameComponent.getPanel(), BorderLayout.NORTH);
    }

    private void initPanel() {
        this.loadSettingsStore(getSettingsStorage());
        // 初始化表格
        this.initTable();
        this.initGroupName();
    }

    @Override
    public String getDisplayName() {
        return "Column Config";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public @Nullable JComponent createComponent() {
        this.initPanel();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !this.columnConfigGroupMap.equals(getSettingsStorage().getColumnConfigGroupMap())
                || !getSettingsStorage().getCurrColumnConfigGroupName().equals(this.currColumnConfigGroup.getName());
    }

    @Override
    public void apply() {
        getSettingsStorage().setColumnConfigGroupMap(this.columnConfigGroupMap);
        getSettingsStorage().setCurrColumnConfigGroupName(this.currColumnConfigGroup.getName());
        // 保存包后重新加载配置
        this.loadSettingsStore(getSettingsStorage());
    }

    /**
     * 加载配置信息
     *
     * @param settingsStorage 配置信息
     */
    @Override
    public void loadSettingsStore(SettingsStorageDTO settingsStorage) {
        // 复制配置，防止篡改
        this.columnConfigGroupMap = CloneUtils.cloneByJson(settingsStorage.getColumnConfigGroupMap(), new TypeReference<Map<String, ColumnConfigGroup>>() {
        });
        this.currColumnConfigGroup = this.columnConfigGroupMap.get(settingsStorage.getCurrColumnConfigGroupName());
        if (this.currColumnConfigGroup == null) {
            this.currColumnConfigGroup = this.columnConfigGroupMap.get(GlobalDict.DEFAULT_GROUP_NAME);
        }
        this.refreshUiVal();
    }

    private void refreshUiVal() {
        if (this.tableComponent != null) {
            this.tableComponent.setDataList(this.currColumnConfigGroup.getElementList());
        }
        if (this.groupNameComponent != null) {
            this.groupNameComponent.setGroupMap(this.columnConfigGroupMap);
            this.groupNameComponent.setCurrGroupName(this.currColumnConfigGroup.getName());
        }
    }
}
