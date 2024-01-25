package com.sjhy.plugin.entity;

import com.sjhy.plugin.enums.ColumnConfigType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 列配置信息
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
@Data
@NoArgsConstructor
public class ColumnConfig implements AbstractItem<ColumnConfig>, Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

    /**
     * 标题
     */
    private String title;
    /**
     * 类型
     */
    private ColumnConfigType type;
    /**
     * 默认值（如果是select类型，逗号分隔）
     */
    private String defaultValue;

    public ColumnConfig(String title, ColumnConfigType type) {
        this.title = title;
        this.type = type;
    }

    public ColumnConfig(String title, ColumnConfigType type, String defaultValue) {
        this.title = title;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public ColumnConfig defaultVal() {
        return new ColumnConfig("demo", ColumnConfigType.TEXT);
    }
}
