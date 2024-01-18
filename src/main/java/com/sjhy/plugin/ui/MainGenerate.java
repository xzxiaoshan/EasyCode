package com.sjhy.plugin.ui;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ExceptionUtil;
import com.sjhy.plugin.constants.StrState;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.dto.GenerateOptions;
import com.sjhy.plugin.dto.SettingsStorageDTO;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.entity.Template;
import com.sjhy.plugin.service.CodeGenerateService;
import com.sjhy.plugin.service.SettingsStorageService;
import com.sjhy.plugin.service.TableInfoSettingsService;
import com.sjhy.plugin.tool.CacheDataUtils;
import com.sjhy.plugin.tool.ModuleUtils;
import com.sjhy.plugin.tool.ProjectUtils;
import com.sjhy.plugin.tool.StringUtils;
import com.sjhy.plugin.ui.component.TableConfigJBTabs;
import com.sjhy.plugin.ui.component.TemplateSelectComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选择保存路径
 *
 * @author makejava
 * @version 1.0.0
 * @date 2024-01-18 13:11:37
 * @since 2018/07/17 13:10
 */
public class MainGenerate extends DialogWrapper {
    /**
     * 主面板
     */
    private JPanel contentPane;
    /**
     * 模型下拉框
     */
    private JComboBox<String> moduleComboBox;
    /**
     * 包字段
     */
    private JTextField packageField;
    /**
     * 路径字段
     */
    private JTextField pathField;
    /**
     * 前缀字段
     */
    private JTextField preField;
    /**
     * 包选择按钮
     */
    private JButton packageChooseButton;
    /**
     * 路径选择按钮
     */
    private JButton pathChooseButton;
    /**
     * 模板选择下拉框
     */
    private ComboBox<String> templateGroupComboBox;
    /**
     * 模板面板
     */
    private JPanel templatePanel;
    /**
     * 弹框选是复选框
     */
    private JCheckBox titleSureCheckBox;
    /**
     * 格式化代码复选框
     */
    private JCheckBox reFormatCheckBox;
    /**
     * 弹框全否复选框
     */
    private JCheckBox titleRefuseCheckBox;
    /**
     * 表配置
     */
    private JPanel tableConfigPane;
    /**
     * 数据缓存工具类
     */
    private final CacheDataUtils cacheDataUtils = CacheDataUtils.getInstance();
    /**
     * 表信息服务
     */
    private final TableInfoSettingsService tableInfoService;
    /**
     * 项目对象
     */
    private final Project project;
    /**
     * 代码生成服务
     */
    private final CodeGenerateService codeGenerateService;
    /**
     * 当前项目中的module
     */
    private final List<Module> moduleList;

    /**
     * 实体模式生成代码
     */
    private final boolean entityMode;

    /**
     * 模板选择组件
     */
    private TemplateSelectComponent templateSelectComponent;

    /**
     * 构造方法
     *
     * @param project project
     */
    public MainGenerate(Project project) {
        this(project, false);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    /**
     * 构造方法
     *
     * @param project    project
     * @param entityMode entityMode
     */
    public MainGenerate(Project project, boolean entityMode) {
        super(project);
        this.entityMode = entityMode;
        this.project = project;
        this.tableInfoService = TableInfoSettingsService.getInstance();
        this.codeGenerateService = CodeGenerateService.getInstance(project);
        // 初始化module，存在资源路径的排前面
        this.moduleList = new LinkedList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // 存在源代码文件夹放前面，否则放后面
            if (ModuleUtils.existsSourcePath(module)) {
                this.moduleList.add(0, module);
            } else {
                this.moduleList.add(module);
            }
        }
        this.initPanel();
        this.refreshData();
        this.initEvent();
        init();
        setTitle(GlobalDict.TITLE_INFO);
        //初始化路径
        refreshPath();
    }

    /**
     * initEvent
     */
    private void initEvent() {
        this.initModuleSelectedEvent();
        this.initPackageChooseEvent();
        this.initPathChooseEvent();
        this.initSureCheckBoxEvent();
    }

    /**
     * initSureCheckBoxEvent
     */
    private void initSureCheckBoxEvent() {
        // 覆盖代码复选框互斥
        ItemListener itemListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItemSelectable() == titleSureCheckBox) {
                titleRefuseCheckBox.setSelected(false);
            } else if (e.getStateChange() == ItemEvent.SELECTED && e.getItemSelectable() == titleRefuseCheckBox) {
                titleSureCheckBox.setSelected(false);
            }
        };
        titleSureCheckBox.addItemListener(itemListener);
        titleRefuseCheckBox.addItemListener(itemListener);
    }

    /**
     * initPathChooseEvent
     */
    private void initPathChooseEvent() {
        //选择路径
        pathChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectUtils.getBaseDir(project);
            Module module = getSelectModule();
            if (module != null) {
                path = ModuleUtils.getSourcePath(module);
            }
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                pathField.setText(virtualFile.getPath());
            }
        });
    }

    /**
     * initPackageChooseEvent
     */
    private void initPackageChooseEvent() {
        try {
            Class<?> cls = Class.forName("com.intellij.ide.util.PackageChooserDialog");
            //添加包选择事件
            packageChooseButton.addActionListener(e -> {
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class, Project.class);
                    PackageChooserDialog dialog = (PackageChooserDialog) constructor.newInstance("Package Chooser", project);
                    // 默认展开所有
                    Tree tree = (Tree) dialog.getPreferredFocusedComponent();
                    if (tree != null) {
                        TreeModel treeModel = tree.getModel();
                        if (treeModel != null) {
                            TreeNode basePackageTreeNode = this.getProjectBasePackageTreeNode((TreeNode) treeModel.getRoot());
                            TreePath basePackageTreePath = this.treeNodeToTreePath(basePackageTreeNode);
                            String basePackage = this.treePathToPackageName(basePackageTreePath);
                            tree.expandPath(basePackageTreePath);
                            dialog.selectPackage(basePackage);
                        }
                    }
                    // 显示窗口
                    Method showMethod = cls.getMethod("show");
                    showMethod.invoke(dialog);
                    // 获取选中的包名
                    Method getSelectedPackageMethod = cls.getMethod("getSelectedPackage");
                    Object psiPackage = getSelectedPackageMethod.invoke(dialog);
                    if (psiPackage != null) {
                        Method getQualifiedNameMethod = psiPackage.getClass().getMethod("getQualifiedName");
                        String packageName = (String) getQualifiedNameMethod.invoke(psiPackage);
                        packageField.setText(packageName);
                        // 刷新路径
                        refreshPath();
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
                         InvocationTargetException e1) {
                    ExceptionUtil.rethrow(e1);
                }
            });

            // 添加包编辑框失去焦点事件
            packageField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    // 刷新路径
                    refreshPath();
                }
            });
        } catch (ClassNotFoundException e) {
            // 没有PackageChooserDialog，并非支持Java的IDE，禁用相关UI组件
            packageField.setEnabled(false);
            packageChooseButton.setEnabled(false);
        }
    }

    /**
     * initModuleSelectedEvent
     */
    private void initModuleSelectedEvent() {
        //监听module选择事件
        moduleComboBox.addActionListener(e -> refreshPath()); // 刷新路径
    }

    /**
     * treePathToPackageName
     *
     * @param path path
     * @return String
     */
    private String treePathToPackageName(TreePath path) {
        return ((PsiPackage) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()).getQualifiedName();
    }

    /**
     * treeNodeToTreePath
     *
     * @param node node
     * @return TreePath
     */
    public TreePath treeNodeToTreePath(TreeNode node) {
        List<TreeNode> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node); // 将当前节点添加到路径列表的开头
            node = node.getParent();
        }
        return new TreePath(path.toArray(new TreeNode[0]));
    }

    /**
     * getProjectBasePackageTreeNode
     *
     * @param node node
     * @return TreeNode
     */
    private TreeNode getProjectBasePackageTreeNode(TreeNode node) {
        // 检查当前节点是否为目标节点
        int childCount = node.getChildCount();
        if (childCount > 1) { // 找到第一个有子节点的package视为basePackage
            return node;
        } else if (node.getParent() == null && childCount == 0) { // 纯基础java工程，还没有package的情况
            return node;
        } else if (childCount == 0 && node.getParent().getChildCount() == 1) { // 整个project都没有二级package
            return node;
        } else {
            // 遍历子节点
            for (int i = 0; i < childCount; i++) {
                TreeNode foundNode = getProjectBasePackageTreeNode(node.getChildAt(i));
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }
        // 如果没有找到目标节点，则返回null
        return null;
    }

    /**
     * refreshData
     */
    private void refreshData() {
        // 获取选中的表信息（鼠标右键的那张表），并提示未知类型
        TableInfo tableInfo;
        if (entityMode) {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectPsiClass());
        } else {
            tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectDbTable());
        }

        // 设置默认配置信息
        if (!StringUtils.isEmpty(tableInfo.getSaveModelName())) {
            moduleComboBox.setSelectedItem(tableInfo.getSaveModelName());
        }
        if (!StringUtils.isEmpty(tableInfo.getSavePackageName())) {
            packageField.setText(tableInfo.getSavePackageName());
        }
        if (!StringUtils.isEmpty(tableInfo.getPreName())) {
            preField.setText(tableInfo.getPreName());
        }
        SettingsStorageDTO settings = SettingsStorageService.getSettingsStorage();
        String groupName = settings.getCurrTemplateGroupName();
        if (!StringUtils.isEmpty(tableInfo.getTemplateGroupName())) {
            if (settings.getTemplateGroupMap().containsKey(tableInfo.getTemplateGroupName())) {
                groupName = tableInfo.getTemplateGroupName();
            }
        }
        templateSelectComponent.setSelectedGroupName(groupName);
        String savePath = tableInfo.getSavePath();
        if (!StringUtils.isEmpty(savePath)) {
            // 判断是否需要拼接项目路径
            if (savePath.startsWith(StrState.RELATIVE_PATH)) {
                String projectPath = project.getBasePath();
                savePath = projectPath + savePath.substring(1);
            }
            pathField.setText(savePath);
        }
    }

    @Override
    protected void doOKAction() {
        onOK();
        super.doOKAction();
    }

    /**
     * 确认按钮回调事件
     */
    private void onOK() {
        List<Template> selectTemplateList = templateSelectComponent.getAllSelectedTemplate();
        // 如果选择的模板是空的
        if (selectTemplateList.isEmpty()) {
            Messages.showWarningDialog("Can't Select Template!", GlobalDict.TITLE_INFO);
            return;
        }
        String savePath = pathField.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog("Can't Select Save Path!", GlobalDict.TITLE_INFO);
            return;
        }
        // 针对Linux系统路径做处理
        savePath = savePath.replace("\\", "/");
        // 保存路径使用相对路径
        String basePath = project.getBasePath();
        if (!StringUtils.isEmpty(basePath) && savePath.startsWith(basePath)) {
            if (savePath.length() > basePath.length()) {
                if ("/".equals(savePath.substring(basePath.length(), basePath.length() + 1))) {
                    savePath = savePath.replace(basePath, ".");
                }
            } else {
                savePath = savePath.replace(basePath, ".");
            }
        }
        // 保存配置
        List<TableInfo> selectedTableInfoList;
        if (!entityMode) {
            selectedTableInfoList = cacheDataUtils.getDbTableList().stream()
                    .map(dbTable -> TableInfoSettingsService.getInstance()
                            .getTableInfo(dbTable)).collect(Collectors.toList());
        } else {
            TableInfo tableInfo = tableInfoService.getTableInfo(cacheDataUtils.getSelectPsiClass());
            selectedTableInfoList = new ArrayList<>();
            selectedTableInfoList.add(tableInfo);
        }
        String finalSavePath = savePath;
        selectedTableInfoList.forEach(tableInfo -> {
            tableInfo.setSavePath(finalSavePath);
            tableInfo.setSavePackageName(packageField.getText());
            tableInfo.setPreName(preField.getText());
            tableInfo.setTemplateGroupName(templateSelectComponent.getselectedGroupName());
            Module module = getSelectModule();
            if (module != null) {
                tableInfo.setSaveModelName(module.getName());
            }
            // 保存配置
            tableInfoService.saveTableInfo(tableInfo);
        });

        // 生成代码
        codeGenerateService.generate(selectTemplateList, getGenerateOptions());
    }

    /**
     * 初始化方法
     */
    private void initPanel() {
        // 初始化模板组
        this.templateSelectComponent = new TemplateSelectComponent(this.templateGroupComboBox);
        templatePanel.add(this.templateSelectComponent.getMainPanel(), BorderLayout.CENTER);

        //初始化Module选择
        for (Module module : this.moduleList) {
            moduleComboBox.addItem(module.getName());
        }

        // 初始化模板组
        this.initTemplateGroup();
        // 初始化表配置
        this.initTableConfigPanel();
    }

    /**
     * 初始化表配置Panel
     */
    private void initTableConfigPanel() {
        // 选中的表
        List<TableInfo> selectedTables = CacheDataUtils.getInstance().getDbTableList().stream()
                .map(dbTable -> TableInfoSettingsService.getInstance().getTableInfo(dbTable)).collect(Collectors.toList());

        TableConfigJBTabs tabs = new TableConfigJBTabs(project, selectedTables);
        int totalMinWidth = tabs.getTotalMinWidth();
        this.tableConfigPane.add(tabs.getComponent(), BorderLayout.CENTER);
        this.tableConfigPane.setMinimumSize(new Dimension(totalMinWidth, Math.max(300, totalMinWidth / 3)));
    }

    /**
     * 初始化模板组
     */
    private void initTemplateGroup() {
        this.templateGroupComboBox.removeAllItems();
        for (String groupName : SettingsStorageService.getSettingsStorage().getTemplateGroupMap().keySet()) {
            this.templateGroupComboBox.addItem(groupName);
        }
        this.templateGroupComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupName = (String) ((ComboBox<?>)e.getSource()).getSelectedItem();
                if (StringUtils.isEmpty(groupName)) {
                    return;
                }
                templateSelectComponent.refreshTemplatePanel(groupName);
            }
        });
    }

    /**
     * 获取生成选项
     *
     * @return {@link GenerateOptions}
     */
    private GenerateOptions getGenerateOptions() {
        return GenerateOptions.builder()
                .entityModel(this.entityMode)
                .reFormat(reFormatCheckBox.isSelected())
                .titleSure(titleSureCheckBox.isSelected())
                .titleRefuse(titleRefuseCheckBox.isSelected())
                .unifiedConfig(true)
                .build();
    }

    /**
     * 获取选中的Module
     *
     * @return 选中的Module
     */
    private Module getSelectModule() {
        String name = (String) moduleComboBox.getSelectedItem();
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    /**
     * 获取基本路径
     *
     * @return 基本路径
     */
    private String getBasePath() {
        Module module = getSelectModule();
        VirtualFile baseVirtualFile = ProjectUtils.getBaseDir(project);
        if (baseVirtualFile == null) {
            Messages.showWarningDialog("无法获取到项目基本路径！", GlobalDict.TITLE_INFO);
            return "";
        }
        String baseDir = baseVirtualFile.getPath();
        if (module != null) {
            VirtualFile virtualFile = ModuleUtils.getSourcePath(module);
            if (virtualFile != null) {
                baseDir = virtualFile.getPath();
            }
        }
        return baseDir;
    }

    /**
     * 刷新目录
     */
    private void refreshPath() {
        String packageName = packageField.getText();
        // 获取基本路径
        String path = getBasePath();
        // 兼容Linux路径
        path = path.replace("\\", "/");
        // 如果存在包路径，添加包路径
        if (!StringUtils.isEmpty(packageName)) {
            path += "/" + packageName.replace(".", "/");
        }
        pathField.setText(path);
    }
}
