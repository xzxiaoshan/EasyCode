package com.sjhy.plugin.ui.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 关闭按钮
 *
 * @author 单红宇
 * @date 2024/1/17 10:24
 */
public class TabCloseButtonAction extends AnAction {

    /**
     * consumer
     */
    private final Consumer<AnActionEvent> anActionEventConsumer;

    /**
     * tabs
     */
    private final JBTabsImpl tabs;

    /**
     * TabCloseButtonAction
     */
    public TabCloseButtonAction(JBTabsImpl tabs) {
        this(tabs, null);
    }

    /**
     * CloseButtonAction
     *
     * @param anActionEventConsumer anActionEventConsumer
     */
    public TabCloseButtonAction(JBTabsImpl tabs, Consumer<AnActionEvent> anActionEventConsumer) {
        this(tabs, null, null, anActionEventConsumer);
    }

    /**
     * CloseButtonAction
     *
     * @param text                  text
     * @param description           description
     * @param anActionEventConsumer anActionEventConsumer
     */
    public TabCloseButtonAction(JBTabsImpl tabs, String text, String description, Consumer<AnActionEvent> anActionEventConsumer) {
        super(text, description, AllIcons.Actions.Close);
        if (anActionEventConsumer == null) {
            anActionEventConsumer = e -> {
                // 在这里实现删除逻辑
                for (TabInfo item : tabs.getTabs()) {
                    if (item.getText().equals(e.getPlace())) {
                        tabs.removeTab(item);
                        break;
                    }
                }
            };
        }
        this.anActionEventConsumer = anActionEventConsumer;
        this.tabs = tabs;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        this.anActionEventConsumer.accept(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (tabs != null && tabs.getTabCount() == 1) {
            e.getPresentation().setIcon(null);
            e.getPresentation().setHoveredIcon(null);
        } else {
            e.getPresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
            e.getPresentation().setIcon(AllIcons.Actions.Close);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}