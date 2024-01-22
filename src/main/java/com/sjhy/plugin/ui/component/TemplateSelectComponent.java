package com.sjhy.plugin.ui.component;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.sjhy.plugin.entity.Template;
import com.sjhy.plugin.entity.TemplateGroup;
import com.sjhy.plugin.service.SettingsStorageService;
import com.sjhy.plugin.tool.StringUtils;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模板选择组件
 *
 * @author makejava
 * @version 1.0.0
 * @date 2021/08/16 16:18
 */
public class TemplateSelectComponent {

    /**
     * mainPanel
     */
    @Getter
    private JPanel mainPanel;

    /**
     * 选中所有复选框
     */
    private JBCheckBox allCheckbox;

    /**
     * 所有复选框
     */
    private List<JBCheckBox> checkBoxList;

    /**
     * 模板面板
     */
    private JPanel templatePanel;

    /**
     * 模板组下拉选择框
     */
    private final ComboBox<String> groupComboBox;

    public TemplateSelectComponent(ComboBox<String> groupComboBox) {
        this.groupComboBox = groupComboBox;
        this.init();
    }

    private void init() {
        this.mainPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        this.allCheckbox = new JBCheckBox("All");
        this.allCheckbox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSelectAllCheckBox();
            }
        });
        this.mainPanel.add(topPanel, BorderLayout.NORTH);
        this.templatePanel = new JPanel(new GridLayout(-1, 3));
        this.mainPanel.add(templatePanel, BorderLayout.CENTER);
        // 设置 "全选" 复选框默认选中
        allCheckbox.setSelected(true);
        handleSelectAllCheckBox();
    }

    /**
     * handleSelectAllCheckBox
     */
    private void handleSelectAllCheckBox() {
        if (checkBoxList == null) {
            return;
        }
        for (JBCheckBox checkBox : checkBoxList) {
            checkBox.setSelected(allCheckbox.isSelected());
        }
    }

    public void refreshTemplatePanel(String groupName) {
        this.templatePanel.removeAll();
        this.checkBoxList = new ArrayList<>();
        TemplateGroup templateGroup = SettingsStorageService.getSettingsStorage().getTemplateGroupMap().get(groupName);
        // 添加“全选”
        this.checkBoxList.add(allCheckbox);
        this.templatePanel.add(allCheckbox);
        for (Template template : templateGroup.getElementList()) {
            JBCheckBox checkBox = new JBCheckBox(template.getName());
            this.checkBoxList.add(checkBox);
            this.templatePanel.add(checkBox);
            this.setTemplateCheckBoxItemListener(checkBox);
        }
        this.allCheckbox.setSelected(true);
        handleSelectAllCheckBox();
        this.mainPanel.updateUI();
    }

    /**
     * setTemplateCheckBoxItemListener
     *
     * @param checkBox checkBox
     */
    private void setTemplateCheckBoxItemListener(JBCheckBox checkBox) {
        checkBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {// 当所有模板都逐个勾选后，全选自动勾选，当所有项都取消勾选后全选自动取消勾选
                JBCheckBox eventCheckBox = (JBCheckBox)e.getSource();
                int selectedCount = 0;
                int notSelectedCount = 0;
                int templateCount = checkBoxList.size() - 1;
                for (int i = 1; i < checkBoxList.size(); i++) {// i==1是第一个All选项
                    JBCheckBox item = checkBoxList.get(i);
                    if(item.isSelected()){
                        selectedCount++;
                    } else {
                        notSelectedCount++;
                    }
                }
                if(eventCheckBox.isSelected() && selectedCount == templateCount) {
                    checkBoxList.get(0).setSelected(true);
                }
                if(!eventCheckBox.isSelected() && notSelectedCount == templateCount) {
                    checkBoxList.get(0).setSelected(false);
                }
            }
        });
    }

    public String getSelectedGroupName() {
        return (String) this.groupComboBox.getSelectedItem();
    }

    public void setSelectedGroupName(String groupName) {
        this.groupComboBox.setSelectedItem(groupName);
    }

    public List<Template> getAllSelectedTemplate() {
        String groupName = (String) this.groupComboBox.getSelectedItem();
        if (StringUtils.isEmpty(groupName)) {
            return Collections.emptyList();
        }
        TemplateGroup templateGroup = SettingsStorageService.getSettingsStorage().getTemplateGroupMap().get(groupName);
        Map<String, Template> map = templateGroup.getElementList().stream().collect(Collectors.toMap(Template::getName, val -> val));
        List<Template> result = new ArrayList<>();
        for (JBCheckBox checkBox : this.checkBoxList) {
            if (checkBox.isSelected()) {
                Template template = map.get(checkBox.getText());
                if (template != null) {
                    result.add(template);
                }
            }
        }
        return result;
    }

    /**
     * 选中指定的模板项
     *
     * @param selectedTemplateList selectedTemplateList
     */
    public void setSelected(List<String> selectedTemplateList) {
        if(CollectionUtils.isEmpty(selectedTemplateList)) {
            return;
        }
        for (JBCheckBox checkBox : checkBoxList) {
            checkBox.setSelected(selectedTemplateList.contains(checkBox.getText()));
        }
        // 判断是否被全部勾选（-1去掉All选项）
        checkBoxList.get(0).setSelected(checkBoxList.size() - 1 == selectedTemplateList.size());
    }
}
