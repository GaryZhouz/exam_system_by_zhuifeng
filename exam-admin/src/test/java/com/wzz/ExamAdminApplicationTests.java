package com.wzz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.Util.RedisUtil;
import com.wzz.entity.*;
import com.wzz.service.impl.*;
import com.wzz.vo.CommonResult;
import com.wzz.vo.QuestionVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.*;

@SpringBootTest
class ExamAdminApplicationTests {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private QuestionServiceImpl questionService;

    @Autowired
    private QuestionBankServiceImpl questionBankService;

    @Autowired
    private ExamRecordServiceImpl examRecordService;

    @Autowired
    private AnswerServiceImpl answerService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserRoleServiceImpl userRoleService;

    @Test
    void t2() {
        System.out.println(redisUtil.get("userRoles"));
        List<UserRole> userRoles = userRoleService.list(new QueryWrapper<>());
        redisUtil.set("userRoles", userRoles);
    }

    @Autowired
    private ExamServiceImpl examService;

    @Test
    void t4() {
        List<Exam> exams = examService.list(new QueryWrapper<>());
        List<ExamRecord> examRecords = examRecordService.list(new QueryWrapper<ExamRecord>().isNotNull("total_score"));
        //考试的名称
        String[] examNames = new String[exams.size()];
        //考试通过率
        double[] passRates = new double[exams.size()];

        double total = 0;
        double pass = 0;
        for (int i = 0; i < exams.size(); i++) {
            examNames[i] = exams.get(i).getExamName();
            total = 0;
            pass = 0;
            for (ExamRecord examRecord : examRecords) {
                if (Objects.equals(examRecord.getExamId(), exams.get(i).getExamId())) {
                    total++;
                    if (examRecord.getTotalScore() >= exams.get(i).getPassScore()) pass++;
                }
            }
            passRates[i] = pass / total;
        }
        for (int i = 0; i < passRates.length; i++) {
            if (Double.isNaN(passRates[i])) passRates[i] = 0;
        }
        String res1 = Arrays.toString(examNames);
        String res2 = Arrays.toString(passRates);
        System.out.println(res1.substring(1, res1.length() - 1).replaceAll(" ", ""));
        System.out.println(res2.substring(1, res2.length() - 1).replaceAll(" ", ""));
    }

    @Test
    void t5() {
        List<Exam> exams = examService.list(new QueryWrapper<>());
        List<ExamRecord> examRecords = examRecordService.list(new QueryWrapper<ExamRecord>());
        //考试的名称
        String[] examNames = new String[exams.size()];
        //考试的考试次数
        String[] examNumbers = new String[exams.size()];

        int total = 0;
        int cur = 0;
        for (int i = 0; i < exams.size(); i++) {
            examNames[i] = exams.get(i).getExamName();
            total = 0;
            cur = 0;
            for (ExamRecord examRecord : examRecords) {
                total++;
                if (Objects.equals(examRecord.getExamId(), exams.get(i).getExamId())) {
                    cur++;
                }
            }
            examNumbers[i] = cur + "";
        }
        System.out.println(Arrays.toString(examNames));
        System.out.println(Arrays.toString(examNumbers));
    }
}
