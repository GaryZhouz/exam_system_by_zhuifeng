package com.wzz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.Util.CheckToken;
import com.wzz.Util.RedisUtil;
import com.wzz.Util.SaltEncryption;
import com.wzz.Util.TokenUtils;
import com.wzz.entity.User;
import com.wzz.entity.UserRole;
import com.wzz.service.impl.ExamRecordServiceImpl;
import com.wzz.service.impl.UserRoleServiceImpl;
import com.wzz.service.impl.UserServiceImpl;
import com.wzz.vo.CommonResult;
import com.wzz.vo.TokenVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * @Date 2020/10/20 8:50
 * @created by wzz
 * 通用接口
 */
@RestController
@RequestMapping(value = "/common")
@Slf4j
@Api(tags = "(学生,教师,管理员)通用相关接口", description = "提供相关的 Rest API")
public class CommonController {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRoleServiceImpl userRoleService;

    @Autowired
    private ExamRecordServiceImpl examRecordService;

    //注入自己的redis工具类
    @Autowired
    private RedisUtil redisUtil;

    //jackson
    ObjectMapper mapper = new ObjectMapper();

    @RequestMapping("/error")
    public CommonResult<String> error() {
        return new CommonResult<>(233, "请求错误!");
    }

    /**
     * @param user 用户实体
     * @return
     * @throws NoSuchAlgorithmException
     */
    @ApiOperation("用户注册接口")
    @PostMapping("/register")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "系统用户实体", required = true, dataType = "user", paramType = "body")
    })
    public CommonResult<String> Register(@RequestBody User user) throws NoSuchAlgorithmException {
        log.info("执行了===>CommonController中的register方法");
        //盐值
        String salt = UUID.randomUUID().toString().substring(0, 6);
        String newPwd = SaltEncryption.saltEncryption(user.getPassword(), salt);
        user.setPassword(newPwd);
        user.setSalt(salt);
        user.setCreateDate(new Date());
        //设置加密
        userService.save(user);
        //发放token令牌
        String token = TokenUtils.createToken(new TokenVo("1", user.getUsername(), newPwd));
        return new CommonResult<String>(200, "success", token);
    }

    /**
     * @param username 登录用户名
     * @return
     */
    @GetMapping("/check/{username}")
    @ApiOperation("用户名合法查询接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "系统用户唯一用户名", required = true, dataType = "string", paramType = "path")
    })
    public CommonResult<String> checkUsername(@PathVariable(value = "username") String username) {
        log.info("执行了===>CommonController中的checkUsername方法");
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userService.getOne(wrapper);
        if (user == null) return new CommonResult<>(200, "用户名可用");
        else return new CommonResult<>(233, "用户已存在");
    }

    /**
     * @param username 登录用户名
     * @param password 用户密码
     * @return
     * @throws NoSuchAlgorithmException
     */
    @PostMapping("/login")
    @ApiOperation("用户登录接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "系统用户唯一用户名", required = true, dataType = "string", paramType = "body"),
            @ApiImplicitParam(name = "password", value = "系统用户密码", required = true, dataType = "string", paramType = "body"),
    })
    public CommonResult<String> login(@RequestParam(value = "username") String username,
                                      @RequestParam(value = "password") String password) throws NoSuchAlgorithmException {
        log.info("执行了===>CommonController中的login方法");
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userService.getOne(wrapper);
        if (user != null) {
            String newPwd = SaltEncryption.saltEncryption(password, user.getSalt());
            //对用户输入的密码加密后 对比数据库的密码 并且用户的状态是正常的
            if (newPwd.equals(user.getPassword()) && user.getStatus() == 1) {
                //发放token令牌
                String token = TokenUtils.createToken(new TokenVo(user.getRoleId() + "", user.getUsername(), newPwd));
                return new CommonResult<>(200, "success", token);
            } else {//密码错误 或者 账号封禁
                return new CommonResult<>(233, "账号密码错误,或用户状态异常");
            }
        } else return new CommonResult<>(233, "用户不存在");
    }

    @GetMapping("/logout")
    @ApiOperation(value = "用户主动退出登录")
    public CommonResult<String> logout() {
        log.info("执行了===>CommonController中的logout方法");
        return new CommonResult<>(200, "退出成功");
    }

    /**
     * @param request
     * @return
     */
    @GetMapping("/getMenu")
    @ApiOperation(value = "获取不同用户的权限菜单")
    public CommonResult<Object> getMenu(HttpServletRequest request) {
        log.info("执行了===>CommonController中的getMenu方法");
        //工具类验证token是否有效 有效返回tokenVo对象,否则返回null
        TokenVo tokenVo = new CheckToken().checkToken(request, userService);
        System.out.println(tokenVo);
        if (tokenVo != null) {//有效token
            if (redisUtil.get("menu:" + tokenVo.getRoleId()) != null) {
                return new CommonResult<>(200, "success", redisUtil.get("menu:" + tokenVo.getRoleId()));
            } else {
                String roleId = tokenVo.getRoleId();
                QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
                wrapper.eq("role_id", roleId);
                UserRole userRole = userRoleService.getOne(wrapper);
                redisUtil.set("menu:" + tokenVo.getRoleId(), userRole.getMenuInfo(), 60 * 60 * 10 + new Random().nextInt(5) * 60 * 60);
                return new CommonResult<>(200, "success", userRole.getMenuInfo());
            }

        } else {
            return new CommonResult<>(233, "认证信息有误,获取数据失败");
        }
    }

    /**
     * @param request
     * @return
     */
    @GetMapping("/checkToken")
    @ApiOperation("验证用户token接口")
    public CommonResult<TokenVo> checkToken(HttpServletRequest request) {
        log.info("执行了===>CommonController中的checkToken方法");
        //工具类验证token是否有效 有效返回tokenVo对象,否则返回null
        TokenVo tokenVo = new CheckToken().checkToken(request, userService);
        if (tokenVo != null) {//有效token
            return new CommonResult<>(200, "success", tokenVo);
        } else {
            return new CommonResult<>(233, "token无效");
        }
    }

    @GetMapping("/getCurrentUser")
    @ApiOperation("供给普通用户查询个人信息使用")
    public CommonResult<User> getCurrentUser(HttpServletRequest request) {
        log.info("执行了===>CommonController中的getCurrentUser方法");
        //工具类验证token是否有效 有效返回tokenVo对象,否则返回null
        TokenVo tokenVo = new CheckToken().checkToken(request, userService);
        User user = userService.getOne(new QueryWrapper<User>().eq("username", tokenVo.getUsername()));
        return new CommonResult<>(200, "查询当前用户成功", user);
    }

    /**
     * @param request
     * @param user    用户实体
     * @return
     * @throws NoSuchAlgorithmException
     */
    @PostMapping("/updateCurrentUser")
    @ApiOperation("供给用户更改个人信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "系统用户实体", required = true, dataType = "user", paramType = "body")
    })
    public CommonResult<String> updateCurrentUser(HttpServletRequest request, @RequestBody User user) throws NoSuchAlgorithmException {
        log.info("执行了===>CommonController中的updateCurrentUser方法");
        //工具类验证token是否有效 有效返回tokenVo对象,否则返回null
        TokenVo tokenVo = new CheckToken().checkToken(request, userService);
        User curUser = userService.getOne(new QueryWrapper<User>().eq("username", tokenVo.getUsername()));
        if (!Objects.equals(user.getPassword(), "")) {
            String newPwd = SaltEncryption.saltEncryption(user.getPassword(), curUser.getSalt());
            curUser.setPassword(newPwd);
        }
        curUser.setTrueName(user.getTrueName());
        boolean flag = userService.update(curUser, new UpdateWrapper<User>().eq("username", tokenVo.getUsername()));
        return flag ? new CommonResult<>(200, "更新成功") : new CommonResult<>(233, "更新失败");
    }

}
