package com.wzz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.entity.UserRole;
import com.wzz.mapper.UserRoleMapper;
import com.wzz.service.UserRoleService;
import com.wzz.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @Date 2020/10/20 19:50
 * @created by wzz
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
}
