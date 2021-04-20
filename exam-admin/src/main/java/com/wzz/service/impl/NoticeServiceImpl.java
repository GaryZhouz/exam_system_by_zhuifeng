package com.wzz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.entity.Notice;
import com.wzz.entity.User;
import com.wzz.mapper.NoticeMapper;
import com.wzz.mapper.UserMapper;
import com.wzz.service.NoticeService;
import com.wzz.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Date 2020/10/20 9:05
 * @created by wzz
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {
    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public boolean setAllNoticeIsHistoryNotice() {
        return noticeMapper.setAllNoticeIsHistoryNotice();
    }
}
