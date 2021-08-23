package com.itsharex.blog.dao;

import com.itsharex.blog.dto.ArticleSearchDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * elasticsearch
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Repository
public interface ElasticsearchDao extends ElasticsearchRepository<ArticleSearchDTO, Integer> {
}
