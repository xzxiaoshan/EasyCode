package com.sjhy.plugin.actions;

import com.intellij.database.psi.DbTable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.CacheDataUtils;
import com.sjhy.plugin.tool.ProjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作按钮分组
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class MainActionGroup extends ActionGroup {

    /**
     * MainActionGroup
     */
    public MainActionGroup() {
        // 如果子项为空，则不显示Group
        this.getTemplatePresentation().setHideGroupIfEmpty(true);
    }

    /**
     * 根据右键在不同的选项上展示不同的子菜单
     *
     * @param event 事件对象
     * @return 动作组
     */
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent event) {
        // 获取当前项目
        Project project = getEventProject(event);
        if (event == null) {
            return AnAction.EMPTY_ARRAY;
        }
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        return getMenuList();
    }

    /**
     * 初始化注册子菜单项目
     *
     * @return 子菜单数组
     */
    private AnAction[] getMenuList() {
        String mainActionId = "com.sjhy.plugin.actions.GenerateMainAction";
        String configActionId = "com.sjhy.plugin.actions.ConfigTableAction";
        String clearConfigActionId = "com.sjhy.plugin.actions.ClearTableConfigAction";
        ActionManager actionManager = ActionManager.getInstance();
        // 返回所有菜单
        return new AnAction[]{actionManager.getAction(mainActionId),
                actionManager.getAction(configActionId),
                actionManager.getAction(clearConfigActionId)};
    }

}
