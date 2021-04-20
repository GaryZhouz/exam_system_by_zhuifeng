package com.wzz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wzz.entity.Notice;
import com.wzz.entity.User;

/**
 * @Date 2020/10/20 9:04
 * @created by wzz
 */
public interface NoticeService extends IService<Notice> {
    // 将所有公告设置为历史公告
    boolean setAllNoticeIsHistoryNotice();
}
