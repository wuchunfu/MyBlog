package com.itsharex.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsharex.blog.dao.ArticleTagDao;
import com.itsharex.blog.entity.ArticleTag;
import com.itsharex.blog.service.ArticleTagService;
import org.springframework.stereotype.Service;

/**
 * 文章标签服务
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagDao, ArticleTag> implements ArticleTagService {

}
