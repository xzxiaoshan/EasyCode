package com.sjhy.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.CacheDataUtils;
import com.sjhy.plugin.ui.ConfigTableDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 表配置菜单
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class ConfigTableAction extends AnAction {

    /**
     * 处理方法
     *
     * @param event 事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        // 处理选中的表
        CodeGenerateService.getInstance(project).cacheSelectedTables(event);
        // 选中的表
        List<TableInfo> selectedTables = CacheDataUtils.getInstance().getDbTableList().stream()
                .map(dbTable -> TableInfoSettingsService.getInstance().getTableInfo(dbTable)).collect(Collectors.toList());
        if(!selectedTables.isEmpty()) {
            new ConfigTableDialog(project, selectedTables).show();
        }
    }
}
