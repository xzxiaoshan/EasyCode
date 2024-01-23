package com.sjhy.plugin.actions;

import com.intellij.database.psi.DbTable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.CacheDataUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 表配置菜单
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class ClearTableConfigAction extends AnAction {

    /**
     * 处理方法
     *
     * @param event 事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if(event.getProject() == null) {
            return;
        }
        // 处理选中的表
        CodeGenerateService.getInstance(event.getProject()).cacheSelectedTables(event);

        DbTable dbTable = CacheDataUtils.getInstance().getSelectDbTable();
        if (dbTable == null) {
            return;
        }
        TableInfoSettingsService.getInstance().removeTableInfo(dbTable);
        Messages.showInfoMessage(dbTable.getName() + "表配置信息已重置成功", GlobalDict.TITLE_INFO);
    }
}
