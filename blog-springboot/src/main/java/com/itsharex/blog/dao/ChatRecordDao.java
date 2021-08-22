package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.ChatRecord;
import org.springframework.stereotype.Repository;

/**
 * 聊天记录
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface ChatRecordDao extends BaseMapper<ChatRecord> {
}
