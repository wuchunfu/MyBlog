package com.itsharex.blog.strategy;

import com.itsharex.blog.dto.UserInfoDTO;

/**
 * 第三方登录策略
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
public interface SocialLoginStrategy {

    /**
     * 登录
     *
     * @param data 数据
     * @return {@link UserInfoDTO} 用户信息
     */
    UserInfoDTO login(String data);

}
