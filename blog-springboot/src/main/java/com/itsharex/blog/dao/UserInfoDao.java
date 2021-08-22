package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.UserInfo;
import org.springframework.stereotype.Repository;

/**
 * 用户信息
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface UserInfoDao extends BaseMapper<UserInfo> {

}
