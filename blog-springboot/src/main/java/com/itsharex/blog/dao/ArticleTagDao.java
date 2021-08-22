package com.itsharex.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsharex.blog.entity.ArticleTag;
import org.springframework.stereotype.Repository;

/**
 * 文章标签
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface ArticleTagDao extends BaseMapper<ArticleTag> {

}
