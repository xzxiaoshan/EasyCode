package com.sjhy.plugin.dto;

import com.intellij.database.model.DasNamespace;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbTable;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.sjhy.plugin.dict.GlobalDict;
import com.sjhy.plugin.entity.TableInfo;
import com.sjhy.plugin.tool.JSON;
import com.sjhy.plugin.tool.ReflectionUtils;
import com.sjhy.plugin.tool.StringUtils;
import lombok.Data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 表格信息设置传输对象
 *
 * @author makejava
 * @version 1.0.0
 * @date 2021/08/14 17:40
 */
@Data
public class TableInfoSettingsDTO {
    private Map<String, String> tableInfoMap;
    /**
     * 数据库最后一次生成的表
     */
    private Map<String, String> databaseLastGenTableMap;

    public TableInfoSettingsDTO() {
        this.tableInfoMap = new TreeMap<>();
        this.databaseLastGenTableMap = new HashMap<>();
    }

    private String generateKey(DbTable dbTable) {
        // 递归添加3层名称作为key，第一层为表名、第二层为名命空间名称、第三层为数据库名
        StringBuilder builder = new StringBuilder();
        DbElement element = dbTable;
        for (int i = 0; i < 3; i++) {
            String name = element.getName();
            if (builder.length() > 0) {
                // 添加分割符
                builder.insert(0, ".");
            }
            builder.insert(0, name);
            try {
                Method method = ReflectionUtils.getDeclaredMethod(element.getClass(), "getParent");
                if (method == null) {
                    break;
                }
                element = (DbElement) method.invoke(element);
            } catch (IllegalAccessException | InvocationTargetException e) {
                break;
            }
            // 未必所有的数据库都是存在三层，例如MySQL就只有两层。如果上次层不是Namespace，则不再继续获取
            if (!(element instanceof DasNamespace)) {
                break;
            }
        }
        return builder.toString();
    }

    private String generateKey(PsiClass psiClass) {
        return psiClass.getQualifiedName();
    }
    /**
     * 读表信息
     *
     * @param psiClass psi类
     * @return {@link TableInfo}
     */
    @SuppressWarnings("Duplicates")
    public TableInfo readTableInfo(PsiClass psiClass) {
        String key = generateKey(psiClass);
        TableInfoDTO dto = decode(this.tableInfoMap.get(key));
        dto = new TableInfoDTO(dto, psiClass);
        this.tableInfoMap.put(key, encode(dto));
        this.setDefaultTableGenConfig(key, dto);
        return dto.toTableInfo(psiClass);
    }

    /**
     * 读表信息
     *
     * @param dbTable 数据库表
     * @return {@link TableInfo}
     */
    @SuppressWarnings("Duplicates")
    public TableInfo readTableInfo(DbTable dbTable) {
        String key = generateKey(dbTable);
        TableInfoDTO dto = decode(this.tableInfoMap.get(key));
        // 表可能新增了字段，需要重新合并保存
        dto = new TableInfoDTO(dto, dbTable);
        this.tableInfoMap.put(key, encode(dto));
        this.setDefaultTableGenConfig(key, dto);
        return dto.toTableInfo(dbTable);
    }

    /**
     * setDefaultTableGenConfig
     *
     * @param tableKey tableKey
     * @param dto      dto
     */
    private void setDefaultTableGenConfig(String tableKey, TableInfoDTO dto) {
        // 如果该表没有历史生成的配置记录，则默认使用相同数据库中其他表最后一次生成的配置作为默认配置
        if(StringUtils.isEmpty(dto.getSavePath()) && StringUtils.isEmpty(dto.getTemplateGroupName())) {
            TableInfoDTO lasGenTableInfo = this.getLastGenTableInfo(tableKey);
            if(lasGenTableInfo != null) {
                dto.setSaveModelName(lasGenTableInfo.getSaveModelName());
                dto.setSavePackageName(lasGenTableInfo.getSavePackageName());
                dto.setSavePath(lasGenTableInfo.getSavePath());
                dto.setPreName(lasGenTableInfo.getPreName());
                dto.setTemplateGroupName(lasGenTableInfo.getTemplateGroupName());
                dto.setSelectTemplateList(lasGenTableInfo.getSelectTemplateList());
            }
        }
    }

    /**
     * 记录每个数据库最后一次生成过代码的表
     *
     * @param tableInfoKey tableInfoKey
     */
    private void saveDatabaseLastGenTableName(String tableInfoKey) {
        int i = tableInfoKey.lastIndexOf(".");
        String dbKey = tableInfoKey.substring(0, i);
        String tableName = tableInfoKey.substring(i + 1);
        this.databaseLastGenTableMap.put(dbKey, tableName);
    }

    /**
     * 设置当前同数据库中其他表最后一次生成的配置
     *
     * @param currentTableKey currentTableKey
     * @return TableInfoDTO
     */
    private TableInfoDTO getLastGenTableInfo(String currentTableKey) {
        int i = currentTableKey.lastIndexOf(".");
        String dbKey = currentTableKey.substring(0, i);
        String lastDBGenTableName = this.databaseLastGenTableMap.get(dbKey);
        if(!StringUtils.isEmpty(lastDBGenTableName)) {
            String encodedData = this.tableInfoMap.get(dbKey.concat(".").concat(lastDBGenTableName));
            if(!StringUtils.isEmpty(encodedData)) {
                return decode(encodedData);
            }
        }
        return null;
    }

    /**
     * 保存表信息
     *
     * @param tableInfo 表信息
     */
    public void saveTableInfo(TableInfo tableInfo) {
        if (tableInfo == null) {
            return;
        }
        DbTable dbTable = tableInfo.getObj();
        String key;
        if (dbTable != null) {
            key = generateKey(dbTable);
        } else if (tableInfo.getPsiClassObj() != null) {
            key = generateKey((PsiClass) tableInfo.getPsiClassObj());
        } else {
            Messages.showInfoMessage(tableInfo.getName() + "表配置信息保存失败", GlobalDict.TITLE_INFO);
            return;
        }
        this.tableInfoMap.put(key, encode(TableInfoDTO.valueOf(tableInfo)));
        this.saveDatabaseLastGenTableName(key);
    }

    /**
     * 重置表信息
     *
     * @param dbTable 数据库表
     */
    public void resetTableInfo(DbTable dbTable) {
        String key = generateKey(dbTable);
        this.tableInfoMap.put(key, encode(new TableInfoDTO(null, dbTable)));
    }

    /**
     * 删除表信息
     *
     * @param dbTable 数据库表
     */
    public void removeTableInfo(DbTable dbTable) {
        String key = generateKey(dbTable);
        this.tableInfoMap.remove(key);
    }

    /**
     * encode
     *
     * @param tableInfo tableInfo
     * @return String
     */
    private static String encode(TableInfoDTO tableInfo) {
        return Base64.getEncoder().encodeToString(JSON.toJson(tableInfo).getBytes());
    }

    /**
     * decode
     *
     * @param base64 base64
     * @return TableInfoDTO
     */
    private static TableInfoDTO decode(String base64) {
        if (StringUtils.isEmpty(base64)) {
            return null;
        }
        return JSON.parse(new String(Base64.getDecoder().decode(base64)), TableInfoDTO.class);
    }
}
