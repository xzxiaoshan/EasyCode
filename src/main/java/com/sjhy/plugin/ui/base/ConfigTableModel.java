package com.sjhy.plugin.ui.base;

import com.intellij.util.ui.EditableModel;
import com.sjhy.plugin.entity.ColumnConfig;
import com.sjhy.plugin.entity.ColumnInfo;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.enums.ColumnConfigType;
import com.sjhy.plugin.tool.CollectionUtil;
import com.sjhy.plugin.tool.CurrGroupUtils;
import com.sjhy.plugin.tool.StringUtils;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author makejava
 * @version 1.0.0
 * @date 2021/08/14 13:41
 */
public class ConfigTableModel extends DefaultTableModel implements EditableModel {

    /**
     * 名称的Title
     */
    public static final String TITLE_NAME = "name";

    /**
     * 数据类型的Title
     */
    public static final String TITLE_TYPE = "type";

    /**
     * 备注的Title
     */
    public static final String TITLE_COMMENT = "comment";

    /**
     * 主键的Title
     */
    public static final String TITLE_PK = "PK";

    /**
     * 默认列
     */
    public static final String[] DEFAULT_TITLE_ARRAY = new String[]{TITLE_NAME, TITLE_TYPE, TITLE_COMMENT, TITLE_PK};

    private TableInfo tableInfo;

    public ConfigTableModel(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
        this.initColumn();
        this.initTableData();
    }

    private void initColumn() {
        addColumn(TITLE_NAME);
        addColumn(TITLE_TYPE);
        addColumn(TITLE_COMMENT);
        addColumn(TITLE_PK);
        for (ColumnConfig columnConfig : CurrGroupUtils.getCurrColumnConfigGroup().getElementList()) {
            addColumn(columnConfig.getTitle());
        }
    }

    private void initTableData() {
        // 删除所有列
        int size = getRowCount();
        for (int i = 0; i < size; i++) {
            super.removeRow(0);
        }
        // 渲染列数据
        for (ColumnInfo columnInfo : this.tableInfo.getFullColumn()) {
            List<Object> values = new ArrayList<>();
            values.add(columnInfo.getName());
            values.add(columnInfo.getType());
            values.add(columnInfo.getComment());
            values.add(columnInfo.getIsPrimaryKey());
            Map<String, Object> ext = columnInfo.getExt();
            if (ext == null) {
                ext = Collections.emptyMap();
            }
            for (ColumnConfig columnConfig : CurrGroupUtils.getCurrColumnConfigGroup().getElementList()) {
                Object obj = ext.get(columnConfig.getTitle());
                if (obj == null) {
                    if (columnConfig.getType() == ColumnConfigType.BOOLEAN) {
                        values.add(false);
                    } else {
                        values.add("");
                    }
                } else {
                    values.add(obj);
                }
            }
            addRow(values.toArray());
        }
    }

    @Override
    public void addRow() {
        Map<String, ColumnInfo> map = this.tableInfo.getFullColumn().stream().collect(Collectors.toMap(ColumnInfo::getName, val -> val));
        String newName = "demo";
        for (int i = 0; map.containsKey(newName); i++) {
            newName = "demo" + i;
        }
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setCustom(true);
        columnInfo.setName(newName);
        columnInfo.setExt(new HashMap<>(16));
        columnInfo.setComment("");
        columnInfo.setShortType("String");
        columnInfo.setType("java.lang.String");
        columnInfo.setIsPrimaryKey(false);
        this.tableInfo.getFullColumn().add(columnInfo);
        // 刷新表数据
        this.initTableData();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        ColumnInfo columnInfo = this.tableInfo.getFullColumn().get(row);
        if (columnInfo == null) {
            return;
        }
        switch (column) {
            case 0:
                String name = (String) value;
                // 列名不允许为空
                if (StringUtils.isEmpty(name)) {
                    return;
                }
                // 已存在重名不允许修改
                boolean existsName = this.tableInfo.getFullColumn().stream().anyMatch(item -> Objects.equals(item.getName(), name));
                if (existsName) {
                    return;
                }
                columnInfo.setName(name);
                break;
            case 1:
                String type = (String) value;
                // 列名不允许为空
                if (StringUtils.isEmpty(type)) {
                    return;
                }
                columnInfo.setType(type);
                columnInfo.setShortType(type.substring(type.lastIndexOf(".") + 1));
                break;
            case 2:
                columnInfo.setComment((String) value);
                break;
            case 3:
                columnInfo.setIsPrimaryKey((Boolean) value);
                break;
            default:
                ColumnConfig columnConfig = CurrGroupUtils.getCurrColumnConfigGroup().getElementList().get(column - DEFAULT_TITLE_ARRAY.length);
                if (columnInfo.getExt() == null) {
                    columnInfo.setExt(new HashMap<>(16));
                }
                columnInfo.getExt().put(columnConfig.getTitle(), value);
                break;
        }
        super.setValueAt(value, row, column);
    }

    @Override
    public void removeRow(int row) {
        ColumnInfo columnInfo = this.tableInfo.getFullColumn().get(row);
        if (columnInfo == null) {
            return;
        }
        // 非自定义列不允许删除
        if (Boolean.FALSE.equals(columnInfo.getCustom())) {
            return;
        }
        this.tableInfo.getFullColumn().remove(row);
        this.initTableData();
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
        ColumnInfo columnInfo = this.tableInfo.getFullColumn().remove(oldIndex);
        this.tableInfo.getFullColumn().add(newIndex, columnInfo);
        this.initTableData();
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
        return false;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        ColumnInfo columnInfo = this.tableInfo.getFullColumn().get(row);
        // 非自定义列，列名不允许修改，自动映射的Java类型也不允许修改
        // 考虑：尽量不要用户在这里做操作；并且对于一个模板来说，还是会规范好某种数据库类型固定到Java的类型为好，如果用户确实需要自定义，统一设置中也是可以修改的
        if(column == 0 || column == 1) {// 列名称和数据类型
            if (columnInfo != null && Boolean.FALSE.equals(columnInfo.getCustom())) {
                return false;
            }
        } else if (column == 3 && !CollectionUtil.isEmpty(this.tableInfo.getPkColumn())) { // 主键
            // 如果从数据库读取的所有列没有一个主键，则PK列可以编辑，需要用户勾选主键列，否则使用数据库的主键且不可以编辑
            return false;
        }
        return super.isCellEditable(row, column);
    }

}
