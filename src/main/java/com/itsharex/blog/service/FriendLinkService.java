package com.itsharex.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itsharex.blog.dto.FriendLinkBackDTO;
import com.itsharex.blog.dto.FriendLinkDTO;
import com.itsharex.blog.entity.FriendLink;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.FriendLinkVO;
import com.itsharex.blog.vo.PageResult;

import java.util.List;

/**
 * 友链服务
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 查看友链列表
     *
     * @return 友链列表
     */
    List<FriendLinkDTO> listFriendLinks();

    /**
     * 查看后台友链列表
     *
     * @param condition 条件
     * @return 友链列表
     */
    PageResult<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition);

    /**
     * 保存或更新友链
     *
     * @param friendLinkVO 友链
     */
    void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO);

}
