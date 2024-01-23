package com.sjhy.plugin.tool;

import com.intellij.util.ui.UIUtil;
import com.sjhy.plugin.dict.GlobalDict;

import javax.swing.*;

/**
 * 消息弹框工具类
 * 对message dialog弹框进行兼容处理
 *
 * @author makejava
 * @version 1.0.0
 * @since 2021/02/01 15:36
 */
public class MessageDialogUtils {

    /**
     * MessageDialogUtils
     */
    private MessageDialogUtils() {
        // nothing
    }

    /**
     * yes no确认框
     *
     * @param msg 消息
     * @return 是否确认
     */
    public static boolean yesNo(String msg) {
        Object[] options = new Object[]{"Yes", "No"};
        return JOptionPane.showOptionDialog(null,
                msg, GlobalDict.TITLE_INFO,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                UIUtil.getQuestionIcon(),
                options, options[0]) == 0;
    }

    /**
     * 显示确认框
     *
     * @param msg        确认框消息
     * @param yesText    yesText
     * @param noText     noText
     * @param cancelText cancelText
     * @return 点击按钮
     */
    public static int yesNoCancel(String msg, String yesText, String noText, String cancelText) {
        Object[] options = new Object[]{yesText, noText, cancelText};
        return JOptionPane.showOptionDialog(null,
                msg, GlobalDict.TITLE_INFO,
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                UIUtil.getQuestionIcon(),
                options, options[0]);
    }

}
