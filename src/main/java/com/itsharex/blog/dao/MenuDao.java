package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.Menu;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜单
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface MenuDao extends BaseMapper<Menu> {

    /**
     * 根据用户id查询菜单
     *
     * @param userInfoId 用户信息id
     * @return 菜单列表
     */
    List<Menu> listMenusByUserInfoId(Integer userInfoId);

}
