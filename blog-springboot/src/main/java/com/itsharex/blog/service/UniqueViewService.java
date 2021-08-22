package com.itsharex.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itsharex.blog.dto.UniqueViewDTO;
import com.itsharex.blog.entity.UniqueView;

import java.util.List;

/**
 * 用户量统计
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
public interface UniqueViewService extends IService<UniqueView> {

    /**
     * 获取7天用户量统计
     *
     * @return 用户量
     */
    List<UniqueViewDTO> listUniqueViews();

}
