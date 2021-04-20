package com.wzz.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Date 2020/10/20 11:58
 * @created by wzz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenVo {
    private String roleId;
    private String username;
    private String password;
}
