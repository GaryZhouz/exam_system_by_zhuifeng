package com.wzz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.Util.RedisUtil;
import com.wzz.Util.SaltEncryption;
import com.wzz.entity.Notice;
import com.wzz.entity.QuestionBank;
import com.wzz.entity.User;
import com.wzz.entity.UserRole;
import com.wzz.service.impl.*;
import com.wzz.vo.CommonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Date 2020/10/20 19:07
 * @created by wzz
 */
@RestController
@RequestMapping(value = "/admin")
@Slf4j
@Api(tags = "超级管理员权限相关的接口")
public class AdminController {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRoleServiceImpl userRoleService;

    @Autowired
    private QuestionServiceImpl questionService;

    @Autowired
    private QuestionBankServiceImpl questionBankService;

    @Autowired
    private NoticeServiceImpl noticeService;

    //注入自己的redis工具类
    @Autowired
    private RedisUtil redisUtil;

    //jackson
    ObjectMapper mapper = new ObjectMapper();

    /**
     * @param loginName 登录用户名
     * @param trueName  用户真实姓名
     * @param pageNo    查询页数
     * @param pageSize  页面大小
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/getUser")
    @ApiOperation("获取用户信息,可分页 ----> 查询条件(可无)(username,trueName),必须有的(pageNo,pageSize)")
    public CommonResult<Object> getUser(@RequestParam(required = false) String loginName,
                                        @RequestParam(required = false) String trueName,
                                        Integer pageNo, Integer pageSize) throws InterruptedException {
        log.info("执行了===>AdminController中的getUser方法");
        //参数一是当前页，参数二是每页个数
        IPage<User> userPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (!Objects.equals(loginName, "") && loginName != null) wrapper.like("username", loginName);
        if (!Objects.equals(trueName, "") && trueName != null) wrapper.like("true_name", trueName);
        userPage = userService.page(userPage, wrapper);
        List<User> users = userPage.getRecords();
        // 创建分页结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", userPage.getTotal());
        return new CommonResult<>(200, "success", result);
    }

    /**
     * @param type    操作类型
     * @param userIds 操作用户的id的字符串,以逗号分隔
     * @return
     */
    @GetMapping("/handleUser/{type}")
    @ApiOperation("管理员操作用户: type=1(启用) 2(禁用) 3(删除) userIds(需要操作的用户id)")
    public CommonResult<String> handleUser(@PathVariable("type") Integer type, String userIds) {
        log.info("执行了===>AdminController中的handleUser方法");
        //转换成数组 需要操作的用户的id数组
        String[] ids = userIds.split(",");
        if (type == 1) {//启用
            for (String id : ids) {
                //当前需要修改的用户
                User user = userService.getById(Integer.parseInt(id));
                user.setStatus(1);//设置为启用的用户
                userService.update(user, new UpdateWrapper<User>().eq("id", Integer.parseInt(id)));
            }
            return new CommonResult<>(200, "操作成功");
        } else if (type == 2) {//禁用
            for (String id : ids) {
                //当前需要修改的用户
                User user = userService.getById(Integer.parseInt(id));
                user.setStatus(2);//设置为禁用的用户
                userService.update(user, new UpdateWrapper<User>().eq("id", Integer.parseInt(id)));
            }
            return new CommonResult<>(200, "操作成功");
        } else if (type == 3) {//删除
            for (String id : ids) {
                userService.removeById(Integer.parseInt(id));
            }
            return new CommonResult<>(200, "操作成功");
        } else return new CommonResult<>(233, "操作有误");
    }

    /**
     * @param user 用户实体
     * @return
     * @throws NoSuchAlgorithmException
     */
    @PostMapping("/addUser")
    @ApiOperation("管理员用户新增用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "系统用户实体", required = true, dataType = "user", paramType = "body")
    })
    public CommonResult<String> addUser(@RequestBody User user) throws NoSuchAlgorithmException {
        log.info("执行了===>AdminController中的addUser方法");
        //盐值
        String salt = UUID.randomUUID().toString().substring(0, 6);
        String newPwd = SaltEncryption.saltEncryption(user.getPassword(), salt);
        user.setPassword(newPwd);
        user.setSalt(salt);
        user.setCreateDate(new Date());
        boolean save = userService.save(user);
        return save ? new CommonResult<>(200, "操作成功") : new CommonResult<>(233, "操作失败");
    }

    @GetMapping("/getRole")
    @ApiOperation("查询系统存在的所有角色信息")
    public CommonResult<Object> getRole() {
        log.info("执行了===>AdminController中的getRole方法");
        if (redisUtil.get("userRoles") != null) {//redis中有缓存
            return new CommonResult<>(200, "success", redisUtil.get("userRoles"));
        } else {//redis无缓存
            List<UserRole> userRoles = userRoleService.list(new QueryWrapper<>());
            //设置默认缓存时间(10分钟) + 随机缓存时间(0-5分钟 )  来防止缓存雪崩和击穿
            redisUtil.set("userRoles", userRoles, 60 * 10 + new Random().nextInt(5) * 60);
            return new CommonResult<>(200, "success", userRoles);
        }
    }

    /**
     * @param content  公告内容模糊查询
     * @param pageNo   查询页码
     * @param pageSize 页面大小
     * @return
     */
    @GetMapping("/getAllNotice")
    @ApiOperation("获取系统发布的所有公告(分页 条件查询  二合一接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeContent", value = "搜索公告内容", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "查询结果分页当前页面", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "查询结果的页面条数大小", required = true, dataType = "int", paramType = "query")
    })
    public CommonResult<Object> getAllNotice(@RequestParam(required = false, name = "noticeContent") String content,
                                             Integer pageNo, Integer pageSize) {
        log.info("执行了===>AdminController中的getAllNotice方法");
        //参数一是当前页，参数二是每页个数
        IPage<Notice> noticeIPage = new Page<>(pageNo, pageSize);
        //查询条件(可选)
        QueryWrapper<Notice> wrapper = new QueryWrapper<>();
        if (!Objects.equals(content, "") && content != null) wrapper.like("content", content);
        noticeIPage = noticeService.page(noticeIPage, wrapper);
        List<Notice> notices = noticeIPage.getRecords();
        // 创建分页结果集
        Map<Object, Object> result = new HashMap<>();
        result.put("notices", notices);
        result.put("total", noticeIPage.getTotal());
        return new CommonResult<>(200, "查询所有公告信息", result);
    }

    /**
     * @param notice 公告实体
     * @return
     */
    @PostMapping("/publishNotice")
    @ApiOperation("发布新公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "notice", value = "通知实体对象", required = true, dataType = "notice", paramType = "body")
    })
    @Transactional
    public CommonResult<String> publishNotice(@RequestBody Notice notice) {
        log.info("执行了===>AdminController中的publishNotice方法");
        if (notice.getStatus() == 1) {// 当前发布的是置顶公告
            // 1. 将当前所有公告设置为历史公告
            noticeService.setAllNoticeIsHistoryNotice();
            // 2. 新增最新公告进去
            notice.setCreateTime(new Date());
            boolean save = noticeService.save(notice);
            if (redisUtil.get("currentNewNotice") != null && save)
                redisUtil.set("currentNewNotice", notice.getContent());
            return save ? new CommonResult<>(200, "发布公告成功") :
                    new CommonResult<>(233, "发布公告失败");
        } else if (notice.getStatus() == 0) {// 不发布最新公告
            notice.setCreateTime(new Date());
            boolean save = noticeService.save(notice);
            return save ? new CommonResult<>(200, "发布公告成功") :
                    new CommonResult<>(233, "发布公告失败");
        } else
            throw new RuntimeException("发布公告状态有误");
    }

    /**
     * @param noticeIds 公告唯一id
     * @return
     */
    @GetMapping("/deleteNotice")
    @ApiOperation("批量删除公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "noticeIds", value = "系统公告id", required = true, dataType = "string", paramType = "query")
    })
    @Transactional
    public CommonResult<String> deleteNotice(@RequestParam(name = "ids") String noticeIds) {
        log.info("执行了===>AdminController中的deleteNotice方法");
        //转换成数组 需要操作的用户的id数组
        String[] ids = noticeIds.split(",");
        for (String id : ids) {
            noticeService.removeById(Integer.parseInt(id));
        }
        return new CommonResult<>(200, "批量删除公告成功");
    }

    /**
     * @param notice 公告实体
     * @return
     */
    @PostMapping("/updateNotice")
    @ApiOperation("更新公告")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "notice", value = "通知实体对象", required = true, dataType = "notice", paramType = "body")
    })
    @Transactional
    public CommonResult<String> updateNotice(@RequestBody Notice notice) {
        log.info("执行了===>AdminController中的updateNotice方法");
        System.out.println(notice);
        // 查询当前公告信息
        QueryWrapper<Notice> wrapper = new QueryWrapper<Notice>().eq("n_id", notice.getNId());
        Notice targetNotice = noticeService.getOne(wrapper);

        if (notice.getStatus() == 1) {// 当前更新成为置顶公告
            // 将当前所有公告设置为历史公告
            noticeService.setAllNoticeIsHistoryNotice();
            targetNotice.setUpdateTime(new Date());
            targetNotice.setContent(notice.getContent());
            targetNotice.setStatus(notice.getStatus());

            boolean update = noticeService.update(targetNotice, wrapper);
            if (redisUtil.get("currentNewNotice") != null && update)// 清楚旧缓存
                redisUtil.set("currentNewNotice", notice.getContent());
            return update ? new CommonResult<>(200, "更新公告成功") :
                    new CommonResult<>(233, "更新公告失败");
        } else if (notice.getStatus() == 0) {// 不发布最新公告
            targetNotice.setUpdateTime(new Date());
            targetNotice.setContent(notice.getContent());
            targetNotice.setStatus(notice.getStatus());
            boolean update = noticeService.update(targetNotice, wrapper);
            return update ? new CommonResult<>(200, "更新公告成功") :
                    new CommonResult<>(233, "更新公告失败");
        } else
            throw new RuntimeException("公告状态有误");
    }
}
