package com.wzz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.entity.User;
import com.wzz.mapper.UserMapper;
import com.wzz.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @Date 2020/10/20 9:05
 * @created by wzz
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
