package com.itsharex.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsharex.blog.constant.CommonConst;
import com.itsharex.blog.constant.RedisPrefixConst;
import com.itsharex.blog.dao.ArticleDao;
import com.itsharex.blog.dao.ArticleTagDao;
import com.itsharex.blog.dao.CategoryDao;
import com.itsharex.blog.dao.TagDao;
import com.itsharex.blog.dto.ArchiveDTO;
import com.itsharex.blog.dto.ArticleBackDTO;
import com.itsharex.blog.dto.ArticleDTO;
import com.itsharex.blog.dto.ArticleHomeDTO;
import com.itsharex.blog.dto.ArticlePaginationDTO;
import com.itsharex.blog.dto.ArticlePreviewDTO;
import com.itsharex.blog.dto.ArticlePreviewListDTO;
import com.itsharex.blog.dto.ArticleRecommendDTO;
import com.itsharex.blog.dto.ArticleSearchDTO;
import com.itsharex.blog.entity.Article;
import com.itsharex.blog.entity.ArticleTag;
import com.itsharex.blog.entity.Category;
import com.itsharex.blog.entity.Tag;
import com.itsharex.blog.exception.BizException;
import com.itsharex.blog.service.ArticleService;
import com.itsharex.blog.service.ArticleTagService;
import com.itsharex.blog.service.RedisService;
import com.itsharex.blog.service.TagService;
import com.itsharex.blog.strategy.context.SearchStrategyContext;
import com.itsharex.blog.util.BeanCopyUtils;
import com.itsharex.blog.util.CommonUtils;
import com.itsharex.blog.util.PageUtils;
import com.itsharex.blog.util.UserUtils;
import com.itsharex.blog.vo.ArticleTopVO;
import com.itsharex.blog.vo.ArticleVO;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.DeleteVO;
import com.itsharex.blog.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.itsharex.blog.constant.CommonConst.ARTICLE_SET;
import static com.itsharex.blog.constant.RedisPrefixConst.ARTICLE_VIEWS_COUNT;
import static com.itsharex.blog.enums.ArticleStatusEnum.DRAFT;
import static com.itsharex.blog.enums.ArticleStatusEnum.PUBLIC;

/**
 * ????????????
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private TagService tagService;
    @Autowired
    private ArticleTagDao articleTagDao;
    @Autowired
    private SearchStrategyContext searchStrategyContext;
    @Autowired
    private HttpSession session;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ArticleTagService articleTagService;

    @Override
    public PageResult<ArchiveDTO> listArchives() {
        Page<Article> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        // ??????????????????
        Page<Article> articlePage = articleDao.selectPage(page, new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getCreateTime)
                .orderByDesc(Article::getCreateTime)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus()));
        List<ArchiveDTO> archiveDTOList = BeanCopyUtils.copyList(articlePage.getRecords(), ArchiveDTO.class);
        return new PageResult<>(archiveDTOList, (int) articlePage.getTotal());
    }

    @Override
    public PageResult<ArticleBackDTO> listArticleBacks(ConditionVO condition) {
        // ??????????????????
        Integer count = articleDao.countArticleBacks(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // ??????????????????
        List<ArticleBackDTO> articleBackDTOList = articleDao.listArticleBacks(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        // ?????????????????????????????????
        Map<Object, Double> viewsCountMap = redisService.zAllScore(ARTICLE_VIEWS_COUNT);
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.ARTICLE_LIKE_COUNT);
        // ???????????????????????????
        articleBackDTOList.forEach(item -> {
            Double viewsCount = viewsCountMap.get(item.getId());
            if (Objects.nonNull(viewsCount)) {
                item.setViewsCount(viewsCount.intValue());
            }
            item.setLikeCount((Integer) likeCountMap.get(item.getId().toString()));
        });
        return new PageResult<>(articleBackDTOList, count);
    }

    @Override
    public List<ArticleHomeDTO> listArticles() {
        return articleDao.listArticles(PageUtils.getLimitCurrent(), PageUtils.getSize());
    }

    @Override
    public ArticlePreviewListDTO listArticlesByCondition(ConditionVO condition) {
        // ????????????
        List<ArticlePreviewDTO> articlePreviewDTOList = articleDao.listArticlesByCondition(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        // ?????????????????????(??????????????????)
        String name;
        if (Objects.nonNull(condition.getCategoryId())) {
            name = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                            .select(Category::getCategoryName)
                            .eq(Category::getId, condition.getCategoryId()))
                    .getCategoryName();
        } else {
            name = tagService.getOne(new LambdaQueryWrapper<Tag>()
                            .select(Tag::getTagName)
                            .eq(Tag::getId, condition.getTagId()))
                    .getTagName();
        }
        return ArticlePreviewListDTO.builder()
                .articlePreviewDTOList(articlePreviewDTOList)
                .name(name)
                .build();
    }

    @Override
    public ArticleDTO getArticleById(Integer articleId) {
        // ??????????????????
        CompletableFuture<List<ArticleRecommendDTO>> recommendArticleList = CompletableFuture
                .supplyAsync(() -> articleDao.listRecommendArticles(articleId));
        // ??????????????????
        CompletableFuture<List<ArticleRecommendDTO>> newestArticleList = CompletableFuture
                .supplyAsync(() -> {
                    List<Article> articleList = articleDao.selectList(new LambdaQueryWrapper<Article>()
                            .select(Article::getId, Article::getArticleTitle, Article::getArticleCover, Article::getCreateTime)
                            .eq(Article::getIsDelete, CommonConst.FALSE)
                            .eq(Article::getStatus, PUBLIC.getStatus())
                            .orderByDesc(Article::getId)
                            .last("limit 5"));
                    return BeanCopyUtils.copyList(articleList, ArticleRecommendDTO.class);
                });
        // ??????id????????????
        ArticleDTO article = articleDao.getArticleById(articleId);
        if (Objects.isNull(article)) {
            throw new BizException("???????????????");
        }
        // ?????????????????????
        updateArticleViewsCount(articleId);
        // ??????????????????????????????
        Article lastArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus())
                .lt(Article::getId, articleId)
                .orderByDesc(Article::getId)
                .last("limit 1"));
        Article nextArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus())
                .gt(Article::getId, articleId)
                .orderByAsc(Article::getId)
                .last("limit 1"));
        article.setLastArticle(BeanCopyUtils.copyObject(lastArticle, ArticlePaginationDTO.class));
        article.setNextArticle(BeanCopyUtils.copyObject(nextArticle, ArticlePaginationDTO.class));
        // ???????????????????????????
        Double score = redisService.zScore(ARTICLE_VIEWS_COUNT, articleId);
        if (Objects.nonNull(score)) {
            article.setViewsCount(score.intValue());
        }
        article.setLikeCount((Integer) redisService.hGet(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString()));
        // ??????????????????
        try {
            article.setRecommendArticleList(recommendArticleList.get());
            article.setNewestArticleList(newestArticleList.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return article;
    }

    @Override
    public void saveArticleLike(Integer articleId) {
        // ??????????????????
        String articleLikeKey = RedisPrefixConst.ARTICLE_USER_LIKE + UserUtils.getLoginUser().getUserInfoId();
        if (redisService.sIsMember(articleLikeKey, articleId)) {
            // ????????????????????????id
            redisService.sRemove(articleLikeKey, articleId);
            // ???????????????-1
            redisService.hDecr(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString(), 1L);
        } else {
            // ????????????????????????id
            redisService.sAdd(articleLikeKey, articleId);
            // ???????????????+1
            redisService.hIncr(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString(), 1L);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateArticle(ArticleVO articleVO) {
        // ??????????????????
        Category category = saveArticleCategory(articleVO);
        // ?????????????????????
        Article article = BeanCopyUtils.copyObject(articleVO, Article.class);
        if (Objects.nonNull(category)) {
            article.setCategoryId(category.getId());
        }
        article.setUserId(UserUtils.getLoginUser().getUserInfoId());
        this.saveOrUpdate(article);
        // ??????????????????
        saveArticleTag(articleVO, article.getId());
    }

    /**
     * ??????????????????
     *
     * @param articleVO ????????????
     * @return {@link Category} ????????????
     */
    private Category saveArticleCategory(ArticleVO articleVO) {
        // ????????????????????????
        Category category = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, articleVO.getCategoryName()));
        if (Objects.isNull(category) && !articleVO.getStatus().equals(DRAFT.getStatus())) {
            category = Category.builder()
                    .categoryName(articleVO.getCategoryName())
                    .build();
            categoryDao.insert(category);
        }
        return category;
    }

    @Override
    public void updateArticleTop(ArticleTopVO articleTopVO) {
        // ????????????????????????
        Article article = Article.builder()
                .id(articleTopVO.getId())
                .isTop(articleTopVO.getIsTop())
                .build();
        articleDao.updateById(article);
    }

    @Override
    public void updateArticleDelete(DeleteVO deleteVO) {
        // ??????????????????????????????
        List<Article> articleList = deleteVO.getIdList().stream()
                .map(id -> Article.builder()
                        .id(id)
                        .isTop(CommonConst.FALSE)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(articleList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticles(List<Integer> articleIdList) {
        // ????????????????????????
        articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIdList));
        // ????????????
        articleDao.deleteBatchIds(articleIdList);
    }

    @Override
    public List<ArticleSearchDTO> listArticlesBySearch(ConditionVO condition) {
        return searchStrategyContext.executeSearchStrategy(condition.getKeywords());
    }

    @Override
    public ArticleVO getArticleBackById(Integer articleId) {
        // ??????????????????
        Article article = articleDao.selectById(articleId);
        // ??????????????????
        Category category = categoryDao.selectById(article.getCategoryId());
        String categoryName = null;
        if (Objects.nonNull(category)) {
            categoryName = category.getCategoryName();
        }
        // ??????????????????
        List<String> tagNameList = tagDao.listTagNameByArticleId(articleId);
        // ????????????
        ArticleVO articleVO = BeanCopyUtils.copyObject(article, ArticleVO.class);
        articleVO.setCategoryName(categoryName);
        articleVO.setTagNameList(tagNameList);
        return articleVO;
    }

    /**
     * ?????????????????????
     *
     * @param articleId ??????id
     */
    public void updateArticleViewsCount(Integer articleId) {
        // ?????????????????????????????????????????????
        Set<Integer> articleSet = CommonUtils.castSet(Optional.ofNullable(session.getAttribute(ARTICLE_SET)).orElseGet(HashSet::new), Integer.class);
        if (!articleSet.contains(articleId)) {
            articleSet.add(articleId);
            session.setAttribute(ARTICLE_SET, articleSet);
            // ?????????+1
            redisService.zIncr(ARTICLE_VIEWS_COUNT, articleId, 1D);
        }
    }

    /**
     * ??????????????????
     *
     * @param articleVO ????????????
     */
    private void saveArticleTag(ArticleVO articleVO, Integer articleId) {
        // ???????????????????????????????????????
        if (Objects.nonNull(articleVO.getId())) {
            articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, articleVO.getId()));
        }
        // ??????????????????
        List<String> tagNameList = articleVO.getTagNameList();
        if (CollectionUtils.isNotEmpty(tagNameList)) {
            // ????????????????????????
            List<Tag> existTagList = tagService.list(new LambdaQueryWrapper<Tag>()
                    .in(Tag::getTagName, tagNameList));
            List<String> existTagNameList = existTagList.stream()
                    .map(Tag::getTagName)
                    .collect(Collectors.toList());
            List<Integer> existTagIdList = existTagList.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            // ??????????????????????????????
            tagNameList.removeAll(existTagNameList);
            if (CollectionUtils.isNotEmpty(tagNameList)) {
                List<Tag> tagList = tagNameList.stream().map(item -> Tag.builder()
                                .tagName(item)
                                .build())
                        .collect(Collectors.toList());
                tagService.saveBatch(tagList);
                List<Integer> tagIdList = tagList.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                existTagIdList.addAll(tagIdList);
            }
            // ????????????id????????????
            List<ArticleTag> articleTagList = existTagIdList.stream().map(item -> ArticleTag.builder()
                            .articleId(articleId)
                            .tagId(item)
                            .build())
                    .collect(Collectors.toList());
            articleTagService.saveBatch(articleTagList);
        }
    }

}
