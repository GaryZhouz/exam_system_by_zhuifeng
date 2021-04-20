package com.wzz.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wzz.entity.Exam;
import com.wzz.entity.ExamQuestion;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;

/**
 * @Date 2020/11/2 15:48
 * @created by wzz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddExamByQuestionVo {
    private String examName;
    private String examDesc;
    private Integer type;
    private String password;
    private Integer examDuration;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
    private Integer totalScore;
    private Integer passScore;
    private Integer status;


    private String questionIds;
    private Integer examId;
    private String scores;
}
