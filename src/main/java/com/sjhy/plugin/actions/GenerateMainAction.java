package com.sjhy.plugin.actions;

import com.intellij.database.model.DasColumn;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.entity.TypeMapper;
import com.sjhy.plugin.enums.MatchType;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.tool.CacheDataUtils;
import com.sjhy.plugin.tool.CurrGroupUtils;
import com.sjhy.plugin.tool.StringUtils;
import com.sjhy.plugin.ui.MainGenerate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 代码生成菜单
 *
 * @author makejava
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
public class GenerateMainAction extends AnAction {

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
        // 校验类型映射
        if (!typeValidator(project, CacheDataUtils.getInstance().getSelectDbTable())) {
            // 没通过不打开窗口
            return;
        }
        //开始处理
        new MainGenerate(event.getProject()).show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        //获取选中的PSI元素
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        e.getPresentation().setEnabled(psiElement instanceof DbTable);
    }

    /**
     * 类型校验，如果存在未知类型则引导用于去条件类型
     *
     * @param dbTable 原始表对象
     * @return 是否验证通过
     */
    private boolean typeValidator(Project project, DbTable dbTable) {
        // 处理所有列
        JBIterable<? extends DasColumn> columns = DasUtil.getColumns(dbTable);
        List<TypeMapper> typeMapperList = CurrGroupUtils.getCurrTypeMapperGroup().getElementList();

        // 简单的记录报错弹窗次数，避免重复报错
        Set<String> errorCount = new HashSet<>();

        FLAG:
        for (DasColumn column : columns) {
            String typeName = column.getDasType().getSpecification();
            for (TypeMapper typeMapper : typeMapperList) {
                try {
                    if (typeMapper.getMatchType() == MatchType.ORDINARY) {
                        if (typeName.equalsIgnoreCase(typeMapper.getColumnType())) {
                            continue FLAG;
                        }
                    } else {
                        // 不区分大小写的正则匹配模式
                        if (Pattern.compile(typeMapper.getColumnType(), Pattern.CASE_INSENSITIVE).matcher(typeName).matches()) {
                            continue FLAG;
                        }
                    }
                } catch (PatternSyntaxException e) {
                    if (!errorCount.contains(typeMapper.getColumnType())) {
                        Messages.showWarningDialog(
                                "类型映射《" + typeMapper.getColumnType() + "》存在语法错误，请及时修正。报错信息:" + e.getMessage(),
                                GlobalDict.TITLE_INFO);
                        errorCount.add(typeMapper.getColumnType());
                    }
                }
            }
            // 没找到类型，提示用户选择输入类型
            if(!new Dialog(project, typeName).showAndGet()) {
                return false;
            }
        }
        return true;
    }

    public static class Dialog  extends DialogWrapper {

        private String typeName;

        private JPanel mainPanel;

        private ComboBox<String> comboBox;

        protected Dialog(@Nullable Project project, String typeName) {
            super(project);
            this.typeName = typeName;
            this.initPanel();
        }

        private void initPanel() {
            setTitle(GlobalDict.TITLE_INFO);
            String msg = String.format("数据库类型%s，没有找到映射关系，请输入想转换的类型？", typeName);
            JLabel label = new JLabel(msg);
            this.mainPanel = new JPanel(new BorderLayout());
            this.mainPanel.setBorder(JBUI.Borders.empty(5, 10, 7, 10));
            mainPanel.add(label, BorderLayout.NORTH);
            this.comboBox = new ComboBox<>(GlobalDict.DEFAULT_JAVA_TYPE_LIST);
            this.comboBox.setEditable(true);
            this.mainPanel.add(this.comboBox, BorderLayout.CENTER);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return this.mainPanel;
        }

        @Override
        protected void doOKAction() {
            String selectedItem = (String) this.comboBox.getSelectedItem();
            if (StringUtils.isEmpty(selectedItem)) {
                super.doCancelAction();
                return;
            }
            super.doOKAction();
            typeName = typeName.toLowerCase();
            // matcher1 对应 abc(1)/abc(1) unsigned 这种格式
            Matcher matcher1 = Pattern.compile("(\\w+)\\(\\d+\\)(.*)").matcher(typeName);
            // matcher2 对应 abc(3,2)/abc(3,2) unsigned 这种格式
            Matcher matcher2 = Pattern.compile("(\\w+)\\(\\d+,\\d+\\)(.*)").matcher(typeName);
            MatchType matchType = MatchType.ORDINARY;
            if(matcher1.matches()) {
                matchType = MatchType.REGEX;
                typeName = matcher1.group(1).concat("(\\(\\d+\\)(.*))?");
            } else if(matcher2.matches()) {
                matchType = MatchType.REGEX;
                typeName = matcher2.group(1).concat("(\\(\\d+,\\d+\\)(.*))?");
            }
            TypeMapper typeMapper = new TypeMapper();
            typeMapper.setMatchType(matchType);
            typeMapper.setJavaType(selectedItem);
            typeMapper.setColumnType(typeName);
            CurrGroupUtils.getCurrTypeMapperGroup().getElementList().add(typeMapper);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}