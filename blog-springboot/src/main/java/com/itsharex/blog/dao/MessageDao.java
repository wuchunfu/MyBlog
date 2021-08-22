package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.Message;
import org.springframework.stereotype.Repository;

/**
 * 留言
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface MessageDao extends BaseMapper<Message> {

}
