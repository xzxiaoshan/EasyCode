package com.sjhy.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.ProjectUtils;
import com.sjhy.plugin.ui.component.TableConfigJBTabs;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 表配置窗口
 *
 * @author makejava
 * @version 1.0.0
 * @date 2024-01-18 11:20:50
 * @since 2018/07/17 13:10
 */
public class ConfigTableDialog extends DialogWrapper {
    /**
     * 主面板
     */
    private final JPanel mainPanel;

    /**
     * project
     */
    private final Project project;

    /**
     * 表格集合
     */
    @Getter
    private final List<TableInfo> tableInfoList;

    /**
     * ConfigTableDialog
     *
     * @param project       project
     * @param tableInfoList tableInfoList
     */
    public ConfigTableDialog(Project project, List<TableInfo> tableInfoList) {
        super(ProjectUtils.getCurrProject());
        this.tableInfoList = tableInfoList;
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.initPanel();
    }

    /**
     * initPanel
     */
    private void initPanel() {
        init();
        this.setTitle("Config Table");
        TableConfigJBTabs tabs = new TableConfigJBTabs(project, tableInfoList);
        int totalMinWidth = tabs.getTotalMinWidth();
        this.mainPanel.add(tabs.getComponent(), BorderLayout.CENTER);
        this.mainPanel.setMinimumSize(new Dimension(totalMinWidth, Math.max(300, totalMinWidth / 3) + 1));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.mainPanel;
    }

    @Override
    protected void doOKAction() {
        // 保存信息
        this.getTableInfoList().forEach(tableInfo -> TableInfoSettingsService.getInstance().saveTableInfo(tableInfo));
        super.doOKAction();
    }

}
