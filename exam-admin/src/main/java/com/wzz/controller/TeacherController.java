package com.wzz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.Util.OSSUtil;
import com.wzz.Util.RedisUtil;
import com.wzz.Util.SaltEncryption;
import com.wzz.Util.TokenUtils;
import com.wzz.entity.*;
import com.wzz.service.impl.*;
import com.wzz.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @Date 2020/10/24 15:42
 * @created by wzz
 */
@RestController
@RequestMapping(value = "/teacher")
@Slf4j
@Api(tags = "老师权限相关的接口")
public class TeacherController {

    @Autowired
    private ExamServiceImpl examService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private QuestionServiceImpl questionService;

    @Autowired
    private ExamQuestionServiceImpl examQuestionService;

    @Autowired
    private ExamRecordServiceImpl examRecordService;

    @Autowired
    private QuestionBankServiceImpl questionBankService;

    @Autowired
    private AnswerServiceImpl answerService;


    //注入自己的redis工具类
    @Autowired
    private RedisUtil redisUtil;

    //jackson
    ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/getQuestionBank")
    @ApiOperation("获取所有题库信息")
    public CommonResult<Object> getQuestionBank() {
        log.info("执行了===>TeacherController中的getQuestionBank方法");
        if (redisUtil.get("questionBanks") != null) {//redis中有缓存

            return new CommonResult<>(200, "success", redisUtil.get("questionBanks"));
        } else {//redis无缓存
            List<QuestionBank> questionBanks = questionBankService.list(new QueryWrapper<>());
            //设置默认缓存时间(10分钟) + 随机缓存时间(0-5分钟 )  来防止缓存雪崩和击穿
            redisUtil.set("questionBanks", questionBanks, 60 * 10 + new Random().nextInt(5) * 60);
            return new CommonResult<>(200, "success", questionBanks);
        }
    }

    /**
     * @param questionType    问题类型
     * @param questionBank    问题所属题库
     * @param questionContent 问题内容
     * @param pageNo          页面数
     * @param pageSize        页面大小
     * @return
     */
    @GetMapping("/getQuestion")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionType", value = "问题类型", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "questionBank", value = "问题所属题库", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "questionContent", value = "问题内容", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "页面数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "页面大小", required = true, dataType = "int", paramType = "query")
    })
    @ApiOperation("获取题目信息,可分页 ----> 查询条件(可无)(questionType,questionBank,questionContent),必须有的(pageNo,pageSize)")
    public CommonResult<Object> getQuestion(@RequestParam(required = false) String questionType,
                                            @RequestParam(required = false) String questionBank,
                                            @RequestParam(required = false) String questionContent,
                                            Integer pageNo, Integer pageSize) {
        log.info("执行了===>TeacherController中的getQuestion方法");
        //参数一是当前页，参数二是每页个数
        IPage<Question> questionPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        if (!Objects.equals(questionType, "")) wrapper.eq("qu_type", questionType);
        if (!Objects.equals(questionBank, "")) wrapper.like("qu_bank_name", questionBank);
        if (!Objects.equals(questionContent, "")) wrapper.like("qu_content", questionContent);

        questionPage = questionService.page(questionPage, wrapper);
        List<Question> questions = questionPage.getRecords();
        // 创建分页结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("questions", questions);
        result.put("total", questionPage.getTotal());
        return new CommonResult<>(200, "success", result);
    }

    /**
     * @param questionIds 需要删除的问题id的字符串,逗号分隔
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/deleteQuestion")
    @ApiOperation("根据id批量删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionIds", value = "问题id的字符串以逗号分隔", required = true, dataType = "string", paramType = "query")
    })
    @Transactional
    public CommonResult<String> deleteQuestion(String questionIds) throws InterruptedException {
        log.info("执行了===>TeacherController中的deleteQuestion方法");
        String[] ids = questionIds.split(",");
        Map<String, Object> map = new HashMap<>();
        for (String id : ids) {
            map.clear();
            map.put("question_id", id);
            // 1. 删除数据库的题目信息
            questionService.removeById(Integer.parseInt(id));
            //2. 删除答案表对应当前题目id的答案
            answerService.removeByMap(map);
            // 2. 移除redis缓存
            redisUtil.del("questionVo:" + id);
        }
        // 清楚题库的缓存
        redisUtil.del("questionBanks");
        return new CommonResult<>(200, "删除成功");
    }

    /**
     * @param questionIds 问题id字符串,逗号分隔
     * @param banks       题库id字符串,逗号分隔
     * @return
     */
    @GetMapping("/addBankQuestion")
    @ApiOperation("将问题加入题库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionIds", value = "问题id的字符串以逗号分隔", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "banks", value = "题库id的字符串以逗号分隔", required = true, dataType = "string", paramType = "query"),
    })
    public CommonResult<String> addBankQuestion(String questionIds, String banks) {
        log.info("执行了===>TeacherController中的addBankQuestion方法");
        boolean flag = false;
        //需要操作的问题
        String[] quIds = questionIds.split(",");
        //需要放入的题库id
        String[] bankIds = banks.split(",");

        //将每一个题目放入每一个题库中
        for (String quId : quIds) {
            //当前的问题对象
            Question question = questionService.getById(Integer.parseInt(quId));
            String quBankId = question.getQuBankId();
            //当前已经有的题库id
            String[] qid = quBankId.split(",");
            System.out.println(quBankId);
            //存在去重后的题库id
            Set<Integer> allId = new HashSet<>();
            if (!quBankId.equals("")) {//防止题目没有题库
                for (String s : qid) {
                    allId.add(Integer.parseInt(s));
                }
            }
            //将新增的仓库id放入
            for (String bankId : bankIds) {
                allId.add(Integer.parseInt(bankId));
            }
            //处理后的id字符串 例如(1,2,3)
            String handleHaveBankIds = allId.toString().replaceAll(" ", "");
            handleHaveBankIds = handleHaveBankIds.substring(1, handleHaveBankIds.length() - 1);
            //更新当前用户的题库id值
            question.setQuBankId(handleHaveBankIds);

            //将存放处理后的set集合遍历,然后替换数据库的题库名
            StringBuilder bankNames = new StringBuilder();
            for (Integer id : allId) {
                bankNames.append(questionBankService.getById(id).getBankName()).append(",");
            }
            //替换原来的仓库名称
            question.setQuBankName(bankNames.toString().substring(0, bankNames.toString().length() - 1));
            //更新问题对象
            flag = questionService.update(question, new UpdateWrapper<Question>().eq("id", question.getId()));
        }
        return flag ? new CommonResult<>(200, "添加题库成功") : new CommonResult<>(233, "添加题库失败");
    }

    /**
     * @param questionIds 问题id字符串,逗号分隔
     * @param banks       题库id字符串,逗号分隔
     * @return
     */
    @GetMapping("/removeBankQuestion")
    @ApiOperation("将问题从题库移除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionIds", value = "问题id的字符串以逗号分隔", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "banks", value = "题库id的字符串以逗号分隔", required = true, dataType = "string", paramType = "query"),
    })
    @Transactional
    public CommonResult<String> removeBankQuestion(String questionIds, String banks) {
        log.info("执行了===>TeacherController中的removeBankQuestion方法");
        boolean flag = false;
        //需要操作的问题
        String[] quIds = questionIds.split(",");
        //需要移除的题库id
        String[] bankIds = banks.split(",");
        //操作需要移除仓库的问题
        for (String quId : quIds) {
            Question question = questionService.getById(Integer.parseInt(quId));
            String quBankId = question.getQuBankId();
            //当前问题拥有的仓库id
            String[] curHaveId = quBankId.split(",");
            //存储处理后的id
            Set<Integer> handleId = new HashSet<>();
            if (!quBankId.equals("")) {
                for (String s : curHaveId) {
                    handleId.add(Integer.parseInt(s));
                }
            }
            //遍历查询set中是否含有需要删除的仓库id
            for (String bankId : bankIds) {
                handleId.remove(Integer.parseInt(bankId));
            }
            //处理后的id字符串 例如(1,2,3)
            String handleHaveBankIds = handleId.toString().replaceAll(" ", "");
            handleHaveBankIds = handleHaveBankIds.substring(1, handleHaveBankIds.length() - 1);
            //更新当前用户的题库id值
            question.setQuBankId(handleHaveBankIds);

            if (!handleHaveBankIds.equals("")) {//删除后还存在剩余的题库
                //将存放处理后的set集合遍历,然后替换数据库的题库名
                StringBuilder bankNames = new StringBuilder();
                for (Integer id : handleId) {
                    bankNames.append(questionBankService.getById(id).getBankName()).append(",");
                }
                //替换原来的仓库名称
                question.setQuBankName(bankNames.toString().substring(0, bankNames.toString().length() - 1));
            } else {//不剩题库了
                question.setQuBankName("");
            }
            //更新问题对象
            flag = questionService.update(question, new UpdateWrapper<Question>().eq("id", question.getId()));
        }
        return flag ? new CommonResult<>(200, "移除题库成功") : new CommonResult<>(233, "移除题库失败");
    }

    /**
     * @param file 图片文件
     * @return
     * @throws Exception
     */
    @PostMapping("/uploadQuestionImage")
    @ApiOperation("接受前端上传的图片,返回上传图片地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "图片文件", required = true, dataType = "file", paramType = "body"),
    })
    public CommonResult<String> uploadQuestionImage(MultipartFile file) throws Exception {
        log.info("执行了===>TeacherController中的uploadQuestionImage方法");
        System.out.println(file.getOriginalFilename());
        String url = OSSUtil.picOSS(file);
        return new CommonResult<>(200, "上传成功", url);
    }

    /**
     * @param questionVo 试题Vo对象
     * @return
     */
    @PostMapping("/addQuestion")
    @ApiOperation("添加试题")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionVo", value = "问题的vo视图对象", required = true, dataType = "questionVo", paramType = "body"),
    })
    public CommonResult<String> addQuestion(@RequestBody QuestionVo questionVo) {
        log.info("执行了===>TeacherController中的addQuestion方法");
        //查询所有的问题,然后就可以设置当前问题的id了
        List<Question> qus = questionService.list(new QueryWrapper<>());
        Integer currentQuId = qus.get(qus.size() - 1).getId() + 1;
        Question question = new Question();
        //设置基础字段
        question.setQuType(questionVo.getQuestionType());
        question.setId(currentQuId);
        question.setCreateTime(new Date());
        question.setLevel(questionVo.getQuestionLevel());
        question.setAnalysis(questionVo.getAnalysis());
        question.setQuContent(questionVo.getQuestionContent());
        question.setCreatePerson(questionVo.getCreatePerson());
        //设置所属题库
        String bankIds = Arrays.toString(questionVo.getBankId());
        question.setQuBankId(bankIds.substring(1, bankIds.length() - 1).replaceAll(" ", ""));
        //设置题目插图
        if (questionVo.getImages().length != 0) {
            String QuImages = Arrays.toString(questionVo.getImages());
            question.setImage(QuImages.substring(1, QuImages.length() - 1).replaceAll(" ", ""));
        }
        StringBuilder bankNames = new StringBuilder();
        for (Integer integer : questionVo.getBankId()) {
            bankNames.append(questionBankService.getById(integer).getBankName()).append(",");
        }
        String names = bankNames.toString();
        names = names.substring(0, names.length() - 1);
        question.setQuBankName(names);

        questionService.save(question);
        //设置答案对象
        StringBuffer multipleChoice = new StringBuffer();
        if (questionVo.getQuestionType() != 4) {//不为简答题
            Answer answer = new Answer();
            answer.setQuestionId(currentQuId);
            StringBuffer imgs = new StringBuffer();
            StringBuffer answers = new StringBuffer();
            for (int i = 0; i < questionVo.getAnswer().length; i++) {
                if (questionVo.getAnswer()[i].getImages().length > 0) {//如果该选项有一张图片信息
                    imgs.append(questionVo.getAnswer()[i].getImages()[0]).append(",");
                }
                answers.append(questionVo.getAnswer()[i].getAnswer()).append(",");
                //设置对的选项的下标值
                if (questionVo.getQuestionType() == 2) {//多选
                    if (questionVo.getAnswer()[i].getIsTrue().equals("true")) multipleChoice.append(i).append(",");
                } else {//单选和判断 都是仅有一个答案
                    if (questionVo.getAnswer()[i].getIsTrue().equals("true")) {
                        answer.setTrueOption(i + "");
                        answer.setAnalysis(questionVo.getAnswer()[i].getAnalysis());
                    }
                }
            }
            if (questionVo.getQuestionType() == 2)
                answer.setTrueOption(multipleChoice.toString().substring(0, multipleChoice.toString().length() - 1));
            String handleImgs = imgs.toString();
            String handleAnswers = answers.toString();
            if (handleImgs.length() != 0) handleImgs = handleImgs.substring(0, handleImgs.length() - 1);
            if (handleAnswers.length() != 0) handleAnswers = handleAnswers.substring(0, handleAnswers.length() - 1);

            //设置答案的图片
            answer.setImages(handleImgs);
            //设置所有的选项
            answer.setAllOption(handleAnswers);
            answerService.save(answer);
            //清楚题库的缓存
            redisUtil.del("questionBanks");
        }
        return new CommonResult<>(200, "新增题目成功");
    }

    /**
     * @param id 题目id
     * @return
     */
    @GetMapping("/getQuestionById/{id}")
    @ApiOperation("根据id获取题目信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "问题id", required = true, dataType = "int", paramType = "path"),
    })
    public CommonResult<Object> getQuestionById(@PathVariable("id") Integer id) {
        log.info("执行了===>TeacherController中的getQuestionById方法");
        if (redisUtil.get("questionVo:" + id) != null) {
            return new CommonResult<>(200, "查询题目信息成功", redisUtil.get("questionVo:" + id));
        } else {
            Question question = questionService.getById(id);
            Answer answer = answerService.getOne(new QueryWrapper<Answer>().eq("question_id", id));
            QuestionVo questionVo = new QuestionVo();
            //设置字段
            questionVo.setQuestionContent(question.getQuContent());
            questionVo.setAnalysis(question.getAnalysis());
            questionVo.setQuestionType(question.getQuType());
            questionVo.setQuestionLevel(question.getLevel());
            questionVo.setQuestionId(question.getId());
            if (question.getImage() != null && !Objects.equals(question.getImage(), ""))
                questionVo.setImages(question.getImage().split(","));
            questionVo.setCreatePerson(question.getCreatePerson());
            //设置所属题库
            if (!Objects.equals(question.getQuBankId(), "")) {
                String[] bids = question.getQuBankId().split(",");
                Integer[] bankIds = new Integer[bids.length];
                for (int i = 0; i < bids.length; i++) {
                    bankIds[i] = Integer.parseInt(bids[i]);
                }
                questionVo.setBankId(bankIds);
            }
            if (answer != null) {
                if (question.getQuType() != 2) {
                    String[] allOption = answer.getAllOption().split(",");
                    String[] imgs = answer.getImages().split(",");
                    QuestionVo.Answer[] qa = new QuestionVo.Answer[allOption.length];
                    for (int i = 0; i < allOption.length; i++) {
                        QuestionVo.Answer answer1 = new QuestionVo.Answer();
                        answer1.setId(i);
                        answer1.setAnswer(allOption[i]);
                        if (i <= imgs.length - 1 && !Objects.equals(imgs[i], ""))
                            answer1.setImages(new String[]{imgs[i]});
                        if (i == Integer.parseInt(answer.getTrueOption())) {
                            answer1.setIsTrue("true");
                            answer1.setAnalysis(answer.getAnalysis());
                        }
                        qa[i] = answer1;
                    }
                    questionVo.setAnswer(qa);
                } else {//多选
                    String[] allOption = answer.getAllOption().split(",");
                    String[] imgs = answer.getImages().split(",");
                    QuestionVo.Answer[] qa = new QuestionVo.Answer[allOption.length];
                    for (int i = 0; i < allOption.length; i++) {
                        QuestionVo.Answer answer1 = new QuestionVo.Answer();
                        answer1.setId(i);
                        answer1.setAnswer(allOption[i]);
                        answer1.setImages(imgs);
                        if (i < answer.getTrueOption().split(",").length && i == Integer.parseInt(answer.getTrueOption().split(",")[i])) {
                            answer1.setIsTrue("true");
                            answer1.setAnalysis(answer.getAnalysis());
                        }
                        qa[i] = answer1;
                    }
                    questionVo.setAnswer(qa);
                }
            }
            redisUtil.set("questionVo:" + id, questionVo, 60 * 5 * new Random().nextInt(2));
            return new CommonResult<>(200, "查询成功", questionVo);
        }
    }

    /**
     * @param questionVo 问题vo对象
     * @return
     */
    @PostMapping("/updateQuestion")
    @ApiOperation("更新试题")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionVo", value = "问题的vo视图对象", required = true, dataType = "questionVo", paramType = "body"),
    })
    public CommonResult<String> updateQuestion(@RequestBody QuestionVo questionVo) {
        log.info("执行了===>TeacherController中的updateQuestion方法");
        Question question = new Question();
        //设置基础字段
        question.setQuType(questionVo.getQuestionType());
        question.setId(questionVo.getQuestionId());
        question.setCreateTime(new Date());
        question.setLevel(questionVo.getQuestionLevel());
        question.setAnalysis(questionVo.getAnalysis());
        question.setQuContent(questionVo.getQuestionContent());
        question.setCreatePerson(questionVo.getCreatePerson());
        //设置所属题库
        String bankIds = Arrays.toString(questionVo.getBankId());
        question.setQuBankId(bankIds.substring(1, bankIds.length() - 1).replaceAll(" ", ""));
        //设置题目插图
        if (questionVo.getImages() != null && questionVo.getImages().length != 0) {
            String QuImages = Arrays.toString(questionVo.getImages());
            question.setImage(QuImages.substring(1, QuImages.length() - 1).replaceAll(" ", ""));
        }
        StringBuilder bankNames = new StringBuilder();
        for (Integer integer : questionVo.getBankId()) {
            bankNames.append(questionBankService.getById(integer).getBankName()).append(",");
        }
        String names = bankNames.toString();
        names = names.substring(0, names.length() - 1);
        question.setQuBankName(names);
        //更新
        questionService.update(question, new UpdateWrapper<Question>().eq("id", questionVo.getQuestionId()));
        //设置答案对象
        StringBuffer multipleChoice = new StringBuffer();
        if (questionVo.getQuestionType() != 4) {//不为简答题
            Answer answer = new Answer();
            answer.setQuestionId(questionVo.getQuestionId());
            StringBuffer imgs = new StringBuffer();
            StringBuffer answers = new StringBuffer();
            for (int i = 0; i < questionVo.getAnswer().length; i++) {
                if (questionVo.getAnswer()[i].getImages() != null && questionVo.getAnswer()[i].getImages().length > 0) {//如果该选项有一张图片信息
                    imgs.append(questionVo.getAnswer()[i].getImages()[0]).append(",");
                }
                answers.append(questionVo.getAnswer()[i].getAnswer()).append(",");
                //设置对的选项的下标值
                if (questionVo.getQuestionType() == 2) {//多选
                    if (questionVo.getAnswer()[i].getIsTrue().equals("true")) multipleChoice.append(i).append(",");
                } else {//单选和判断 都是仅有一个答案
                    if (questionVo.getAnswer()[i].getIsTrue().equals("true")) {
                        answer.setTrueOption(i + "");
                        answer.setAnalysis(questionVo.getAnswer()[i].getAnalysis());
                    }
                }
            }
            if (questionVo.getQuestionType() == 2)
                answer.setTrueOption(multipleChoice.toString().substring(0, multipleChoice.toString().length() - 1));
            String handleImgs = imgs.toString();
            String handleAnswers = answers.toString();
            if (handleImgs.length() != 0) handleImgs = handleImgs.substring(0, handleImgs.length() - 1);
            if (handleAnswers.length() != 0) handleAnswers = handleAnswers.substring(0, handleAnswers.length() - 1);

            //设置答案的图片
            answer.setImages(handleImgs);
            //设置所有的选项
            answer.setAllOption(handleAnswers);
            answerService.update(answer, new UpdateWrapper<Answer>().eq("question_id", questionVo.getQuestionId()));
            redisUtil.del("questionVo:" + questionVo.getQuestionId());
        }
        return new CommonResult<>(200, "更新题目成功");
    }

    /**
     * @param bankName 题目名称
     * @param pageNo   页面数
     * @param pageSize 页面大小
     * @return
     */
    @GetMapping("/getBankHaveQuestionSumByType")
    @ApiOperation("获取题库中所有题目类型的数量")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bankName", value = "题库名称", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "页面数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "页面大小", required = true, dataType = "int", paramType = "query"),
    })
    public CommonResult<Object> getBankHaveQuestionSumByType(@RequestParam(required = false) String bankName,
                                                             Integer pageNo, Integer pageSize) {
        log.info("执行了===>TeacherController中的getBankHaveQuestionSumByType方法");

        //参数一是当前页，参数二是每页个数
        IPage<QuestionBank> questionBankIPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<QuestionBank> wrapper = new QueryWrapper<>();
        if (!Objects.equals(bankName, "") && bankName != null) wrapper.like("bank_name", bankName);
        IPage<QuestionBank> iPage = questionBankService.page(questionBankIPage, wrapper);
        List<QuestionBank> questionBanks = iPage.getRecords();

        //封装成传给前端的数据类型
        List<BankHaveQuestionSum> bankHaveQuestionSums = new ArrayList<>();
        for (QuestionBank questionBank : questionBanks) {
            //创建vo对象
            BankHaveQuestionSum bankHaveQuestionSum = new BankHaveQuestionSum();
            //设置属性
            bankHaveQuestionSum.setQuestionBank(questionBank);
            //设置单选题的数量
            List<Question> singleQuestions = questionService.list(new QueryWrapper<Question>().eq("qu_type", 1).like("qu_bank_name", questionBank.getBankName()));
            bankHaveQuestionSum.setSingleChoice(singleQuestions.size());
            //设置多选题的数量
            List<Question> multipleQuestions = questionService.list(new QueryWrapper<Question>().eq("qu_type", 2).like("qu_bank_name", questionBank.getBankName()));
            bankHaveQuestionSum.setMultipleChoice(multipleQuestions.size());
            //设置判断题的数量
            List<Question> judgeQuestions = questionService.list(new QueryWrapper<Question>().eq("qu_type", 3).like("qu_bank_name", questionBank.getBankName()));
            bankHaveQuestionSum.setJudge(judgeQuestions.size());
            //设置简答题的数量
            List<Question> shortAnswerQuestions = questionService.list(new QueryWrapper<Question>().eq("qu_type", 4).like("qu_bank_name", questionBank.getBankName()));
            bankHaveQuestionSum.setShortAnswer(shortAnswerQuestions.size());
            //加入list中
            bankHaveQuestionSums.add(bankHaveQuestionSum);
        }
        // 创建分页结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("bankHaveQuestionSums", bankHaveQuestionSums);
        result.put("total", iPage.getTotal());
        return new CommonResult<>(200, "查询题库和所属题目信息成功", result);
    }

    /**
     * @param ids
     * @return
     */
    @GetMapping("/deleteQuestionBank")
    @ApiOperation("删除题库并去除所有题目中的包含此题库的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "删除题库的id字符串逗号分隔", required = true, dataType = "string", paramType = "query")
    })
    public CommonResult<String> deleteQuestionBank(String ids) {
        log.info("执行了===>TeacherController中的deleteQuestionBank方法");
        String[] bankId = ids.split(",");
        for (String s : bankId) {
            //找到题库
            QuestionBank questionBank = questionBankService.getById(s);
            //找到与此题库相关的所有问题信息
            List<Question> questions = questionService.list(new QueryWrapper<Question>().like("qu_bank_name", questionBank.getBankName()));
            //移除与此题库相关的信息
            for (Question question : questions) {
                String quBankName = question.getQuBankName();
                String quBankId = question.getQuBankId();
                String[] name = quBankName.split(",");
                String[] id = quBankId.split(",");
                //新的题库名
                String[] newName = new String[name.length - 1];
                //新的题库id数据
                String[] newId = new String[id.length - 1];

                for (int i = 0, j = 0; i < name.length; i++) {
                    if (!name[i].equals(questionBank.getBankName())) {
                        newName[j] = name[i];
                        j++;
                    }
                }
                for (int i = 0, j = 0; i < id.length; i++) {
                    if (!id[i].equals(String.valueOf(questionBank.getBankId()))) {
                        newId[j] = id[i];
                        j++;
                    }
                }
                String handleName = Arrays.toString(newName)
                        .replaceAll(" ", "")
                        .replaceAll("]", "")
                        .replace("[", "");
                String handleId = Arrays.toString(newId).replaceAll(" ", "")
                        .replaceAll("]", "")
                        .replace("[", "");
                //设置删除题库后的新字段
                question.setQuBankName(handleName);
                question.setQuBankId(handleId);
                //更新题目
                questionService.update(question, new UpdateWrapper<Question>().eq("id", question.getId()));
            }
            //删除题库
            questionBankService.removeById(Integer.parseInt(s));
            //清楚题库的缓存
            redisUtil.del("questionBanks");
        }
        return new CommonResult<>(200, "删除题库成功");
    }

    /**
     * @param questionBank
     * @return
     */
    @PostMapping("/addQuestionBank")
    @ApiOperation("添加题库信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "questionBank", value = "题库的实体对象", required = true, dataType = "questionBank", paramType = "body")
    })
    public CommonResult<String> addQuestionBank(@RequestBody QuestionBank questionBank) {
        log.info("执行了===>TeacherController中的addQuestionBank方法");
        boolean flag = questionBankService.save(questionBank);
        return flag ? new CommonResult<>(200, "添加题库成功") : new CommonResult<>(200, "添加题库失败");
    }

    /**
     * @param id 题库id
     * @return
     */
    @GetMapping("/getBankById")
    @ApiOperation("通过题库id获取题库信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "题库id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<QuestionBank> getBankById(Integer id) {
        log.info("执行了===>TeacherController中的getBankById方法");
        return new CommonResult<>(200, "查询题库信息成功", questionBankService.getById(id));
    }

    /**
     * @param bankId 题库id
     * @return
     */
    @GetMapping("/getQuestionByBank")
    @ApiOperation("根据题库获取所有的题目信息(单选,多选,判断题)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bankId", value = "题库id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getQuestionByBank(Integer bankId) {
        log.info("执行了===>TeacherController中的getQuestionByBank方法");
        if (redisUtil.get("questionBankQuestion:" + bankId) != null) {//查询缓存
            return new CommonResult<>(200, "当前题库题目查询成功", redisUtil.get("questionBankQuestion:" + bankId));
        } else {
            QuestionBank bank = questionBankService.getById(bankId);
            //在题库中的(单选,多选,判断题)题目
            List<Question> questions = questionService.list(new QueryWrapper<Question>().like("qu_bank_name", bank.getBankName()).in("qu_type", 1, 2, 3));
            //构造前端需要的vo对象
            List<QuestionVo> questionVos = new ArrayList<>();
            for (Question question : questions) {
                QuestionVo questionVo = new QuestionVo();

                questionVo.setQuestionId(question.getId());
                questionVo.setQuestionLevel(question.getLevel());
                if (question.getImage() != null && !question.getImage().equals("")) //防止没有图片对象
                    questionVo.setImages(question.getImage().split(","));
                questionVo.setCreatePerson(question.getCreatePerson());
                questionVo.setAnalysis(question.getAnalysis());
                questionVo.setQuestionContent(question.getQuContent());
                questionVo.setQuestionType(question.getQuType());

                Answer answer = answerService.getOne(new QueryWrapper<Answer>().eq("question_id", question.getId()));
                //选项个数
                String[] options = answer.getAllOption().split(",");
                String[] images = answer.getImages().split(",");
                //构造答案对象
                QuestionVo.Answer[] handleAnswer = new QuestionVo.Answer[options.length];
                //字段处理
                for (int i = 0; i < options.length; i++) {
                    QuestionVo.Answer answer1 = new QuestionVo.Answer();
                    if (images.length - 1 >= i && images[i] != null && !images[i].equals(""))
                        answer1.setImages(new String[]{images[i]});
                    answer1.setAnswer(options[i]);
                    answer1.setId(i);
                    answer1.setIsTrue("false");
                    handleAnswer[i] = answer1;
                }
                if (question.getQuType() != 2) {//单选和判断
                    int trueOption = Integer.parseInt(answer.getTrueOption());
                    handleAnswer[trueOption].setIsTrue("true");
                    handleAnswer[trueOption].setAnalysis(answer.getAnalysis());
                } else {//多选
                    String[] trueOptions = answer.getTrueOption().split(",");
                    for (String trueOption : trueOptions) {
                        handleAnswer[Integer.parseInt(trueOption)].setIsTrue("true");
                        handleAnswer[Integer.parseInt(trueOption)].setAnalysis(answer.getAnalysis());
                    }
                }
                questionVo.setAnswer(handleAnswer);
                questionVos.add(questionVo);
            }
            redisUtil.set("questionBankQuestion:" + bankId, questionVos, 60 * 5 + new Random().nextInt(2));
            return new CommonResult<>(200, "当前题库题目查询成功", questionVos);
        }
    }

    /**
     * @param bankId 题库id
     * @param type   题目类型
     * @return
     */
    @GetMapping("/getQuestionByBankIdAndType")
    @ApiOperation("根据题库id和题目类型获取题目信息 type(1单选 2多选 3判断)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bankId", value = "题库id", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "题目类型", required = true, dataType = "int", paramType = "query"),
    })
    public CommonResult<List<QuestionVo>> getQuestionByBankIdAndType(Integer bankId, Integer type) {
        log.info("执行了===>TeacherController中的getQuestionByBankIdAndType方法");
        //调用根据题库查询所有题目信息的方法
        CommonResult<Object> questionByBank = getQuestionByBank(bankId);
        List<QuestionVo> questionVos = (List<QuestionVo>) questionByBank.getData();
        //根据题目类型筛选题目
        questionVos.removeIf(questionVo -> !Objects.equals(questionVo.getQuestionType(), type));
        return new CommonResult<>(200, "根据题目类型查询成功", questionVos);
    }

    /**
     * @param examQueryVo 考试信息查询vo对象
     * @return
     */
    @PostMapping("/getExamInfo")
    @ApiOperation("根据信息查询考试的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examQueryVo", value = "考试信息查询vo对象", required = true, dataType = "examQueryVo", paramType = "body")
    })
    public CommonResult<List<Exam>> getExamInfo(@RequestBody ExamQueryVo examQueryVo) {
        log.info("执行了===>TeacherController中的getExamInfo方法");
        System.out.println(examQueryVo);
        //参数一是当前页，参数二是每页个数
        IPage<Exam> examIPage = new Page<>(examQueryVo.getPageNo(), examQueryVo.getPageSize());
        //查询条件(可选)
        QueryWrapper<Exam> wrapper = new QueryWrapper<>();

        if (examQueryVo.getExamType() != null) wrapper.eq("type", examQueryVo.getExamType());
        if (examQueryVo.getExamName() != null) wrapper.like("exam_name", examQueryVo.getExamName());
        if (examQueryVo.getStartTime() != null) {
            wrapper.gt("start_time", examQueryVo.getStartTime().substring(0, examQueryVo.getStartTime().indexOf("T")));
        }
        if (examQueryVo.getEndTime() != null) {
            wrapper.lt("end_time", examQueryVo.getEndTime().substring(0, examQueryVo.getEndTime().indexOf("T")));
        }
        IPage<Exam> page = examService.page(examIPage, wrapper);

        List<Exam> exams = page.getRecords();

        return new CommonResult<>(200, "查询考试信息成功", exams);
    }

    /**
     * @param type 操作类型
     * @param ids  操作的考试id集合
     * @return
     */
    @GetMapping("/operationExam/{type}")
    @ApiOperation("操作考试的信息表(type 1启用 2禁用 3删除)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "操作类型", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "ids", value = "操作的考试id集合", required = true, dataType = "string", paramType = "query"),
    })
    public CommonResult<String> operationExam(@PathVariable("type") Integer type, String ids) {
        log.info("执行了===>TeacherController中的operationExam方法");
        String[] id = ids.split(",");
        if (type == 1) {
            for (String s : id) {
                Exam exam = examService.getOne(new QueryWrapper<Exam>().eq("exam_id", Integer.parseInt(s)));
                exam.setStatus(1);
                examService.update(exam, new UpdateWrapper<Exam>().eq("exam_id", s));
            }
        } else if (type == 2) {
            for (String s : id) {
                Exam exam = examService.getOne(new QueryWrapper<Exam>().eq("exam_id", Integer.parseInt(s)));
                exam.setStatus(2);
                examService.update(exam, new UpdateWrapper<Exam>().eq("exam_id", s));
            }
        } else if (type == 3) {
            Map<String, Object> map = new HashMap<>();
            for (String s : id) {
                map.clear();
                map.put("exam_id", Integer.parseInt(s));
                examService.removeByMap(map);
                examQuestionService.removeByMap(map);
            }
        }
        return new CommonResult<>(200, "操作成功");
    }

    /**
     * @param addExamByBankVo 根据题库添加考试vo对象
     * @return
     */
    @PostMapping("/addExamByBank")
    @ApiOperation("根据题库添加考试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addExamByBankVo", value = "根据题库添加考试vo对象", required = true, dataType = "addExamByBankVo", paramType = "body")
    })
    public CommonResult<String> addExamByBank(@RequestBody AddExamByBankVo addExamByBankVo) {
        log.info("执行了===>TeacherController中的addExamByBank方法");

        Exam exam = new Exam();
        exam.setStatus(addExamByBankVo.getStatus());
        exam.setDuration(addExamByBankVo.getExamDuration());
        if (addExamByBankVo.getEndTime() != null)
            exam.setEndTime(addExamByBankVo.getEndTime());
        if (addExamByBankVo.getStartTime() != null)
            exam.setStartTime(addExamByBankVo.getStartTime());
        exam.setExamDesc(addExamByBankVo.getExamDesc());
        exam.setExamName(addExamByBankVo.getExamName());
        exam.setPassScore(addExamByBankVo.getPassScore());
        exam.setType(addExamByBankVo.getType());
        //设置密码如果有
        if (addExamByBankVo.getPassword() != null) {
            exam.setPassword(addExamByBankVo.getPassword());
        }
        //设置id
        List<Exam> examList = examService.list(new QueryWrapper<>());
        int id = 0;
        if (examList.size() != 0) {
            id = examList.get(examList.size() - 1).getExamId() + 1;
        }
        exam.setExamId(id);

        //构造考试中含有的题目信息
        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(id);
        //设置题目id字符串
        HashSet<Integer> set = new HashSet<>();
        String[] bankNames = addExamByBankVo.getBankNames().split(",");

        for (String bankName : bankNames) {
            List<Question> questions = questionService.list(new QueryWrapper<Question>().like("qu_bank_name", bankName));
            for (Question question : questions) {
                set.add(question.getId());
            }
        }
        String quIds = set.toString().substring(1, set.toString().length() - 1)
                .replaceAll(" ", "");
        System.out.println(quIds);
        examQuestion.setQuestionIds(quIds);
        //设置每一题的分数
        String[] s = quIds.split(",");
        //总分
        int totalScore = 0;
        StringBuilder sf = new StringBuilder();
        for (String s1 : s) {
            Question question = questionService.getById(Integer.parseInt(s1));
            if (question.getQuType() == 1) {
                sf.append(addExamByBankVo.getSingleScore()).append(",");
                totalScore += addExamByBankVo.getSingleScore();
            } else if (question.getQuType() == 2) {
                sf.append(addExamByBankVo.getMultipleScore()).append(",");
                totalScore += addExamByBankVo.getMultipleScore();
            } else if (question.getQuType() == 3) {
                sf.append(addExamByBankVo.getJudgeScore()).append(",");
                totalScore += addExamByBankVo.getJudgeScore();
            } else if (question.getQuType() == 4) {
                sf.append(addExamByBankVo.getShortScore()).append(",");
                totalScore += addExamByBankVo.getShortScore();
            }
        }
        examQuestion.setScores(sf.toString().substring(0, sf.toString().length() - 1));
        //设置总成绩
        exam.setTotalScore(totalScore);

        examService.save(exam);
        examQuestionService.save(examQuestion);
        return new CommonResult<>(200, "考试创建成功");
    }

    /**
     * @param addExamByQuestionVo 通过题目列表添加考试的vo对象
     * @return
     */
    @PostMapping("/addExamByQuestionList")
    @ApiOperation("根据题目列表添加考试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addExamByQuestionVo", value = "通过题目列表添加考试的vo对象", required = true, dataType = "addExamByQuestionVo",
                    paramType = "body")
    })
    public CommonResult<String> addExamByQuestionList(@RequestBody AddExamByQuestionVo addExamByQuestionVo) {
        log.info("执行了===>TeacherController中的addExamByQuestionList方法");
        Exam exam = new Exam();
        exam.setTotalScore(addExamByQuestionVo.getTotalScore());
        exam.setType(addExamByQuestionVo.getType());
        exam.setPassScore(addExamByQuestionVo.getPassScore());
        if (addExamByQuestionVo.getEndTime() != null)
            exam.setEndTime(addExamByQuestionVo.getEndTime());
        if (addExamByQuestionVo.getStartTime() != null)
            exam.setStartTime(addExamByQuestionVo.getStartTime());
        exam.setExamDesc(addExamByQuestionVo.getExamDesc());
        exam.setExamName(addExamByQuestionVo.getExamName());
        exam.setDuration(addExamByQuestionVo.getExamDuration());
        //设置密码如果有
        if (addExamByQuestionVo.getPassword() != null) {
            exam.setPassword(addExamByQuestionVo.getPassword());
        }
        exam.setStatus(addExamByQuestionVo.getStatus());
        //设置id
        List<Exam> examList = examService.list(new QueryWrapper<>());
        int id = 0;
        if (examList.size() != 0) {
            id = examList.get(examList.size() - 1).getExamId() + 1;
        }
        exam.setExamId(id);
        //设置考试的题目和分值信息
        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(id);
        examQuestion.setScores(addExamByQuestionVo.getScores());
        examQuestion.setQuestionIds(addExamByQuestionVo.getQuestionIds());

        examService.save(exam);
        examQuestionService.save(examQuestion);
        return new CommonResult<>(200, "考试创建成功");
    }

    /**
     * @param examId 考试id
     * @return
     */
    @GetMapping("/getExamInfoById")
    @ApiOperation("根据考试id查询考试的信息和题目列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examId", value = "考试id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getExamInfoById(@RequestParam Integer examId) {
        log.info("执行了===>TeacherController中的getExamInfoById方法");
        if (redisUtil.get("examInfo:" + examId) != null) {
            return new CommonResult<>(200, "查询成功", redisUtil.get("examInfo:" + examId));
        } else {
            //构造传递给前端的考试组合对象
            AddExamByQuestionVo addExamByQuestionVo = new AddExamByQuestionVo();
            Exam exam = examService.getOne(new QueryWrapper<Exam>().eq("exam_id", examId));
            addExamByQuestionVo.setExamDesc(exam.getExamDesc());
            addExamByQuestionVo.setExamDuration(exam.getDuration());
            addExamByQuestionVo.setExamId(examId);
            addExamByQuestionVo.setExamName(exam.getExamName());
            addExamByQuestionVo.setPassScore(exam.getPassScore());
            addExamByQuestionVo.setTotalScore(exam.getTotalScore());
            addExamByQuestionVo.setEndTime(exam.getEndTime());
            addExamByQuestionVo.setStartTime(exam.getStartTime());
            addExamByQuestionVo.setType(exam.getType());
            addExamByQuestionVo.setPassword(exam.getPassword());
            addExamByQuestionVo.setStatus(exam.getStatus());

            //考试中题目的对象
            ExamQuestion examQuestion = examQuestionService.getOne(new QueryWrapper<ExamQuestion>().eq("exam_id", examId));
            addExamByQuestionVo.setQuestionIds(examQuestion.getQuestionIds());
            addExamByQuestionVo.setScores(examQuestion.getScores());
            redisUtil.set("examInfo:" + examId, addExamByQuestionVo, 60 * 5 * new Random().nextInt(2));
            return new CommonResult<>(200, "查询成功", addExamByQuestionVo);
        }
    }

    /**
     * @param addExamByQuestionVo
     * @return
     */
    @PostMapping("/updateExamInfo")
    @ApiOperation("更新考试的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addExamByQuestionVo", value = "通过题目列表添加考试的vo对象", required = true, dataType = "addExamByQuestionVo",
                    paramType = "body")
    })
    public CommonResult<String> updateExamInfo(@RequestBody AddExamByQuestionVo addExamByQuestionVo) {
        log.info("执行了===>TeacherController中的updateExamInfo方法");
        Exam exam = new Exam();
        exam.setTotalScore(addExamByQuestionVo.getTotalScore());
        exam.setType(addExamByQuestionVo.getType());
        exam.setPassScore(addExamByQuestionVo.getPassScore());
        exam.setEndTime(addExamByQuestionVo.getEndTime());
        exam.setStartTime(addExamByQuestionVo.getStartTime());
        exam.setExamDesc(addExamByQuestionVo.getExamDesc());
        exam.setExamName(addExamByQuestionVo.getExamName());
        exam.setDuration(addExamByQuestionVo.getExamDuration());
        //设置密码如果有
        if (addExamByQuestionVo.getPassword() != null) {
            exam.setPassword(addExamByQuestionVo.getPassword());
        } else {
            exam.setPassword(null);
        }
        exam.setStatus(addExamByQuestionVo.getStatus());
        exam.setExamId(addExamByQuestionVo.getExamId());
        //设置考试的题目和分值信息
        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(addExamByQuestionVo.getExamId());
        examQuestion.setScores(addExamByQuestionVo.getScores());
        examQuestion.setQuestionIds(addExamByQuestionVo.getQuestionIds());

        examService.update(exam, new UpdateWrapper<Exam>().eq("exam_id", exam.getExamId()));
        examQuestionService.update(examQuestion, new UpdateWrapper<ExamQuestion>().eq("exam_id", exam.getExamId()));
        //移除缓存
        redisUtil.del("examInfo:" + exam.getExamId());
        return new CommonResult<>(200, "更新成功");
    }

    /**
     * @param examRecord
     * @param request
     * @return
     */
    @PostMapping("/addExamRecord")
    @ApiOperation("保存考试记录信息,返回保存记录的id")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examRecord", value = "考试记录实体对象", required = true, dataType = "examRecord", paramType = "body")
    })
    public CommonResult<Integer> addExamRecord(@RequestBody ExamRecord examRecord, HttpServletRequest request) {
        log.info("执行了===>TeacherController中的addExamRecord方法");
        String token = request.getHeader("authorization");
        //当前用户对象的信息
        TokenVo tokenVo = TokenUtils.verifyToken(token);
        User user = userService.getOne(new QueryWrapper<User>().eq("username", tokenVo.getUsername()));
        //设置考试信息的字段
        examRecord.setUserId(user.getId());
        //设置id
        List<ExamRecord> examRecords = examRecordService.list(new QueryWrapper<>());
        int id = 1;
        if (examRecords.size() > 0) {
            id = examRecords.get(examRecords.size() - 1).getRecordId() + 1;
        }
        examRecord.setRecordId(id);

        //设置逻辑题目的分数
        //查询所有的题目答案信息
        List<Answer> answers = answerService.list(new QueryWrapper<Answer>().in("question_id", Arrays.asList(examRecord.getQuestionIds().split(","))));
        //查询考试的题目的分数
        HashMap<String, String> map = new HashMap<>();//key是题目的id  value是题目分值
        ExamQuestion examQuestion = examQuestionService.getOne(new QueryWrapper<ExamQuestion>().eq("exam_id", examRecord.getExamId()));
        //题目的id
        String[] ids = examQuestion.getQuestionIds().split(",");
        //题目在考试中对应的分数
        String[] scores = examQuestion.getScores().split(",");
        for (int i = 0; i < ids.length; i++) {
            map.put(ids[i], scores[i]);
        }
        //逻辑分数
        int logicScore = 0;
        //错题的id
        StringBuffer sf = new StringBuffer();
        //用户的答案
        String[] userAnswers = examRecord.getUserAnswers().split("-");
        for (int i = 0; i < examRecord.getQuestionIds().split(",").length; i++) {
            int index = SaltEncryption.getIndex(answers, Integer.parseInt(examRecord.getQuestionIds().split(",")[i]));
            if (index != -1) {
                if (Objects.equals(userAnswers[i], answers.get(index).getTrueOption())) {
                    logicScore += Integer.parseInt(map.get(examRecord.getQuestionIds().split(",")[i]));
                } else {
                    sf.append(examRecord.getQuestionIds().split(",")[i]).append(",");
                }
            }
        }
        examRecord.setLogicScore(logicScore);
        if (sf.length() > 0) {//存在错的逻辑题
            examRecord.setErrorQuestionIds(sf.toString().substring(0, sf.toString().length() - 1));
        }

        System.out.println(examRecord);
        examRecord.setExamTime(new Date());
        examRecordService.save(examRecord);
        return new CommonResult<>(200, "考试记录保存成功", id);
    }

    /**
     * @param recordId 考试记录id
     * @return
     */
    @GetMapping("/getExamRecordById/{recordId}")
    @ApiOperation("根据考试的记录id查询用户考试的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "recordId", value = "考试记录id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getExamRecordById(@PathVariable Integer recordId) {
        log.info("执行了===>TeacherController中的getExamRecordById方法");
        if (redisUtil.get("examRecord:" + recordId) != null) {
            return new CommonResult<>(200, "考试信息查询成功", redisUtil.get("examRecord:" + recordId));
        } else {
            ExamRecord examRecord = examRecordService.getOne(new QueryWrapper<ExamRecord>().eq("record_id", recordId));
            redisUtil.set("examRecord:" + recordId, examRecord, 60 * 5 + new Random().nextInt(2) * 60);
            return new CommonResult<>(200, "考试信息查询成功", examRecord);
        }
    }

    /**
     * @param examId 考试id
     * @return
     */
    @GetMapping("/getExamQuestionByExamId/{examId}")
    @ApiOperation("根据考试id查询考试中的每一道题目id和分值")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examId", value = "考试id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getExamQuestionByExamId(@PathVariable Integer examId) {
        log.info("执行了===>TeacherController中的getExamQuestionByExamId方法");
        if (redisUtil.get("examQuestion:" + examId) != null) {
            return new CommonResult<>(200, "查询考试中题目和分值成功", redisUtil.get("examQuestion:" + examId));
        } else {
            ExamQuestion examQuestion = examQuestionService.getOne(new QueryWrapper<ExamQuestion>().eq("exam_id", examId));
            return new CommonResult<>(200, "查询考试中题目和分值成功", examQuestion);
        }
    }

    /**
     * @param examId   考试id
     * @param pageNo   页数
     * @param pageSize 页面大小
     * @return
     */
    @GetMapping("/getExamRecord")
    @ApiOperation("获取考试记录信息,(pageNo,pageSize)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examId", value = "考试id", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "页面数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "页面大小", required = true, dataType = "int", paramType = "query"),
    })
    public CommonResult<Object> getExamRecord(@RequestParam(required = false) Integer examId,
                                              Integer pageNo, Integer pageSize) {
        log.info("执行了===>TeacherController中的getExamRecords方法");
        //参数一是当前页，参数二是每页个数
        IPage<ExamRecord> examRecordPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        if (examId != null) wrapper.eq("exam_id", examId);

        IPage<ExamRecord> page = examRecordService.page(examRecordPage, wrapper);

        List<ExamRecord> examRecords = page.getRecords();
        // 构造结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("examRecords", examRecords);
        result.put("total", examRecordPage.getTotal());
        return new CommonResult<>(200, "success", result);
    }

    /**
     * @param userId 用户id
     * @return
     */
    @GetMapping("/getUserById/{userId}")
    @ApiOperation("根据用户id查询用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getUserById(@PathVariable Integer userId) {
        log.info("执行了===>TeacherController中的getUserById方法");
        if (redisUtil.get("user:" + userId) != null) {
            return new CommonResult<>(200, "用户信息查询成功", redisUtil.get("user:" + userId));
        } else {
            User user = userService.getOne(new QueryWrapper<User>().eq("id", userId));
            redisUtil.set("user:" + userId, user, 5 * 60 + new Random().nextInt(2) * 60);
            return new CommonResult<>(200, "用户信息查询成功", user);
        }
    }

    @GetMapping("/allExamInfo")
    @ApiOperation("查询考试所有信息")
    public CommonResult<List<Exam>> allExamInfo() {
        log.info("执行了===>TeacherController中的allExamInfo方法");
        List<Exam> exams = examService.list(new QueryWrapper<>());
        return new CommonResult<>(200, "所有考试信息获取成功", exams);
    }

    /**
     * @param totalScore   总成绩
     * @param examRecordId 考试记录id
     * @return
     */
    @GetMapping("/setObjectQuestionScore")
    @ApiOperation("设置考试记录的客观题得分,设置总分为逻辑得分+客观题")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "totalScore", value = "总成绩", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "examRecordId", value = "考试记录id", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<String> setObjectQuestionScore(Integer totalScore, Integer examRecordId) {
        ExamRecord examRecord = examRecordService.getOne(new QueryWrapper<ExamRecord>().eq("record_id", examRecordId));
        examRecord.setTotalScore(totalScore);
        boolean flag = examRecordService.update(examRecord, new UpdateWrapper<ExamRecord>().eq("record_id", examRecordId));
        return flag ? new CommonResult<>(200, "批阅成功") : new CommonResult<>(233, "批阅失败");
    }

    @GetMapping("/getExamPassRate")
    @ApiOperation("提供每一门考试的通过率数据(echarts绘图)")
    public CommonResult<List<String>> getExamPassRate() {
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
        List<String> list = new ArrayList<>();
        String res1 = Arrays.toString(examNames);
        String res2 = Arrays.toString(passRates);
        list.add(res1.substring(1, res1.length() - 1).replaceAll(" ", ""));
        list.add(res2.substring(1, res2.length() - 1).replaceAll(" ", ""));
        return new CommonResult<>(200, "考试通过率获取成功", list);
    }

    @GetMapping("/getExamNumbers")
    @ApiOperation("提供每一门考试的考试次数(echarts绘图)")
    public CommonResult<List<String>> getExamNumbers() {
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
        List<String> list = new ArrayList<>();
        String res1 = Arrays.toString(examNames);
        String res2 = Arrays.toString(examNumbers);
        list.add(res1.substring(1, res1.length() - 1).replaceAll(" ", ""));
        list.add(res2.substring(1, res2.length() - 1).replaceAll(" ", ""));
        return new CommonResult<>(200, "考试次数获取成功", list);
    }
}
