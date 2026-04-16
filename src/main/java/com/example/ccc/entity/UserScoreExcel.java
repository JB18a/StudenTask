package com.example.ccc.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.Date;

//@Data
public class UserScoreExcel {

    @ExcelProperty("任务标题")
    @ColumnWidth(20)
    private String taskTitle;

    @ExcelProperty("提交文件")
    @ColumnWidth(25)
    private String fileName;

    @ExcelProperty("分数")
    @ColumnWidth(10)
    private Integer score;

    @ExcelProperty("评语")
    @ColumnWidth(30)
    private String feedback;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private Date submitTime; // EasyExcel 会自动格式化 Date

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }
}
