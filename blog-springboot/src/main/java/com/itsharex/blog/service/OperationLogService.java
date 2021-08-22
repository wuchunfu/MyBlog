package com.itsharex.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itsharex.blog.dto.OperationLogDTO;
import com.itsharex.blog.entity.OperationLog;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.PageResult;

/**
 * 操作日志服务
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 查询日志列表
     *
     * @param conditionVO 条件
     * @return 日志列表
     */
    PageResult<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

}
