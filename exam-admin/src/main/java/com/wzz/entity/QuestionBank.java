package com.wzz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Date 2020/10/24 15:18
 * @created by wzz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("题库实体")
@TableName(value = "question_bank")
public class QuestionBank {

    @ApiModelProperty(value = "主键 题库id", example = "1")
    @TableId(type = IdType.AUTO)
    private Integer bankId;

    @ApiModelProperty(value = "题库名称", example = "小学数学")
    private String bankName;
}
