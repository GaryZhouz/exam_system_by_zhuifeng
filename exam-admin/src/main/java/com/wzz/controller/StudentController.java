package com.wzz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itextpdf.text.DocumentException;
import com.wzz.Util.CertificateUtil.ContentStyle;
import com.wzz.Util.CertificateUtil.DateTimeUtil;
import com.wzz.Util.CertificateUtil.PDFUtil;
import com.wzz.Util.RedisUtil;
import com.wzz.entity.ExamRecord;
import com.wzz.entity.Notice;
import com.wzz.entity.User;
import com.wzz.service.impl.*;
import com.wzz.vo.CommonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @Date 2020/11/7 19:44
 * @created by wzz
 */
@RestController
@RequestMapping(value = "/student")
@Slf4j
@Api(tags = "学生权限相关的接口")
public class StudentController {

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

    @Autowired
    private NoticeServiceImpl noticeService;

    //注入自己的redis工具类
    @Autowired
    private RedisUtil redisUtil;

    /**
     * @param username 系统登录用户名
     * @param pageNo   页面大小
     * @param pageSize 页面大小
     * @param examId   考试id
     * @return
     */
    @GetMapping("/getMyGrade")
    @ApiOperation("获取个人成绩(分页 根据考试名查询)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "系统唯一用户名", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "当前页面数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "当前页面大小", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "examId", value = "考试唯一id", required = false, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getMyGrade(String username, Integer pageNo, Integer pageSize,
                                                     @RequestParam(required = false) Integer examId) {
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        //参数一是当前页，参数二是每页个数
        IPage<ExamRecord> examRecordPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        if (examId != null) wrapper.eq("exam_id", examId);

        IPage<ExamRecord> page = examRecordService.page(examRecordPage, wrapper);
        List<ExamRecord> examRecords = page.getRecords();
        // 创建分页结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("examRecords", examRecords);
        result.put("total", examRecordPage.getTotal());
        return new CommonResult<>(200, "查询成绩成功", result);
    }

    @GetMapping("/getCurrentNewNotice")
    @ApiOperation("获取当前系统最新的公告")
    public CommonResult<Object> getCurrentNewNotice() {
        log.info("执行了===>StudentController中的getCurrentNewNotice方法");
        if (redisUtil.get("currentNewNotice") != null) {//redis中有缓存
            return new CommonResult<>(200, "获取最新公告成功", redisUtil.get("currentNewNotice"));
        } else {//redis无缓存
            Notice notice = noticeService.getOne(new QueryWrapper<Notice>().eq("status", "1"));
            //设置默认缓存时间(24小时)
            redisUtil.set("currentNewNotice", notice.getContent(), 60 * 1440);
            return new CommonResult<>(200, "获取最新公告成功", notice.getContent());
        }
    }

    /**
     * @param response
     * @param examName     考试名称用于模糊查询
     * @param examRecordId 考试记录的id
     * @throws IOException
     * @throws DocumentException
     */
    @GetMapping("/getCertificate")
    @ApiOperation("生成证书接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "examName", value = "考试名称", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "examRecordId", value = "考试记录id", required = true, dataType = "int", paramType = "query")
    })
    public void getCertificate(HttpServletResponse response, @RequestParam(name = "examName") String examName,
                               @RequestParam(name = "examRecordId") Integer examRecordId) throws IOException, DocumentException, URISyntaxException {
        log.info("执行了===>StudentController中getCertificate的方法");
        System.out.println(examRecordId);
        // 1. 查询考试记录信息
        ExamRecord examRecord = examRecordService.getOne(new QueryWrapper<ExamRecord>().eq("record_id", examRecordId));
        // 2. 获取用户id
        Integer userId = examRecord.getUserId();
        // 3. 查询用户的真实姓名生成证书
        User user = userService.getOne(new QueryWrapper<User>().eq("id", userId));


        // ****windows下用如下路径****
        // 获取证书背景图片路径
        String backgroundImage = Objects.requireNonNull(PDFUtil.class.getClassLoader().getResource("static/images/certificateBg.png")).getPath();
        // 获取发放证书的项目Logo
        String logo = Objects.requireNonNull(PDFUtil.class.getClassLoader().getResource("static/images/logo.png")).getPath();
        // 生成的pdf的文件位置(一个模板多次生成)
        String pdfFilePath = Objects.requireNonNull(PDFUtil.class.getClassLoader().getResource("static/templateCertificate.pdf")).getPath();


        // ****linux服务器下用如下(地址对应服务器上文件路径)
//        String backgroundImage = "/wzz/nginx_static_resources/certificateBg.png";
//        String logo = "/wzz/nginx_static_resources/logo.png";
//        String pdfFilePath = "/wzz/nginx_static_resources/templateCertificate.pdf";

        // 生成工具类
        PDFUtil pdfUtil = new PDFUtil();

        // 证书字体样式
        ContentStyle style1 = new ContentStyle();
        style1.setFontSize(15);
        ContentStyle style2 = new ContentStyle();
        style2.setFontSize(10);

        // 准备证书所需要的数据
        String trueName = user.getTrueName();
        Date examTime = examRecord.getExamTime();
        // 生成XXX同学信息
        String userInfo = trueName + "同学：";
        // 生成证书内容
        String content = "您于" + DateTimeUtil.DateToString(examTime) +
                "在" + examName + "测评中取得优异成绩!";
        // 创建证书
        pdfUtil.openDocument(pdfFilePath)
                .addImage(backgroundImage, 0, 400)
                .addLogo(logo, 270, 480)
                .addContent(userInfo, 85, 630, style1)
                .addContent("特发此证,以资鼓励!", 125, 495, style2)
                .addContent("Power By WangZhouzhou", 360, 495, style2);
        // 结束截取字符串的索引
        int end = 0;
        // 证书内容分行,防止超出证书边缘
        for (int i = 0, y = 590; i < content.length(); y -= 30) {
            end = Math.min(i + 30, content.length());
            pdfUtil.addContent(content.substring(i, end), 125, y, style1);
            i = end;
        }
        // 关闭创建pdf的工具
        pdfUtil.close();
        // 文件转码
        if (pdfFilePath.contains("%")) {
            try {
                pdfFilePath = URLDecoder.decode(pdfFilePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.debug("responseFileStream decode error:" + e.toString());
            }
        }
        // 输出流
        ServletOutputStream out = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(new File(pdfFilePath));
            String[] dir = pdfFilePath.split("/");
            // 获取文件名
            String fileName = dir[dir.length - 1];
            String[] array = fileName.split("[.]");
            // 文件类型
            String fileType = array[array.length - 1].toLowerCase();
            //设置文件ContentType类型
            if ("jpg,jepg,gif,png".contains(fileType)) {//图片类型
                response.setContentType("image/" + fileType);
            } else if ("pdf".contains(fileType)) {//pdf类型
                response.setContentType("application/pdf");
            } else {//自动判断下载文件类型
                response.setContentType("multipart/form-data");
            }
            //设置文件头：最后一个参数是设置下载文件名
            //response.setHeader("Content-Disposition", "attachment;fileName="+fileName);
            out = response.getOutputStream();
            // 读取文件流
            int len = 0;
            byte[] buffer = new byte[1024 * 10];
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            log.error("responseFileStream error:FileNotFoundException" + e.toString());
        } catch (Exception e) {
            log.error("responseFileStream error:" + e.toString());
        } finally {
            try {
                out.close();
                in.close();
            } catch (NullPointerException e) {
                log.error("responseFileStream stream close() error:NullPointerException" + e.toString());
            } catch (Exception e) {
                log.error("responseFileStream stream close() error:" + e.toString());
            }
        }
    }

}
