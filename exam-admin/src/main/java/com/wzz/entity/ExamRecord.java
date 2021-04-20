package com.wzz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Date 2020/11/5 11:01
 * @created by wzz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("考试记录")
@TableName(value = "exam_record")
public class ExamRecord implements Serializable {
    @ApiModelProperty(value = "主键 考试主键的id", example = "1")
    private Integer recordId;

    @ApiModelProperty(value = "考试用户id", example = "1")
    private Integer userId;

    @ApiModelProperty(value = "用户考试中的答案", example = "1")
    private String userAnswers;

    @ApiModelProperty(value = "考试过程中的信用截图", example = "imgUrl")
    private String creditImgUrl;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "考试时间", example = "2020-10-20")
    private Date examTime;

    @ApiModelProperty(value = "考试逻辑得分", example = "69")
    private Integer logicScore;

    @ApiModelProperty(value = "考试的id", example = "1")
    private Integer examId;

    @ApiModelProperty(value = "考试的题目id", example = "1")
    private String questionIds;

    @ApiModelProperty(value = "考试总得分", example = "100")
    private Integer totalScore;

    @ApiModelProperty(value = "考试错题id", example = "1,2,3")
    private String errorQuestionIds;
}
