package com.wzz.vo;

import com.wzz.entity.QuestionBank;
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Date 2020/10/28 20:01
 * @created by wzz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankHaveQuestionSum {
    private QuestionBank questionBank;
    //单选数量
    private Integer singleChoice;
    //多选数量
    private Integer multipleChoice;
    //判断数量
    private Integer judge;
    //简答数量
    private Integer shortAnswer;
}
