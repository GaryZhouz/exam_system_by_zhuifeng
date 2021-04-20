package com.wzz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.entity.Exam;
import com.wzz.mapper.ExamMapper;
import com.wzz.service.ExamService;
import org.springframework.stereotype.Service;

/**
 * @Date 2020/10/20 9:05
 * @created by wzz
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {
}
