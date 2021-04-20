package com.wzz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wzz.entity.UserRole;
import org.springframework.stereotype.Repository;

/**
 * @Date 2020/10/20 19:49
 * @created by wzz
 */
//在对应的mapper上面实现基本的接口
@Repository//代表持久层
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
