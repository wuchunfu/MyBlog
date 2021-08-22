package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.OperationLog;
import org.springframework.stereotype.Repository;

/**
 * 操作日志
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface OperationLogDao extends BaseMapper<OperationLog> {
}
