package com.itsharex.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.itsharex.blog.constant.CommonConst;
import com.itsharex.blog.constant.RedisPrefixConst;
import com.itsharex.blog.dao.ArticleDao;
import com.itsharex.blog.dao.CategoryDao;
import com.itsharex.blog.dao.MessageDao;
import com.itsharex.blog.dao.TagDao;
import com.itsharex.blog.dao.UserInfoDao;
import com.itsharex.blog.dao.WebsiteConfigDao;
import com.itsharex.blog.dto.ArticleRankDTO;
import com.itsharex.blog.dto.ArticleStatisticsDTO;
import com.itsharex.blog.dto.BlogBackInfoDTO;
import com.itsharex.blog.dto.BlogHomeInfoDTO;
import com.itsharex.blog.dto.CategoryDTO;
import com.itsharex.blog.dto.TagDTO;
import com.itsharex.blog.dto.UniqueViewDTO;
import com.itsharex.blog.entity.Article;
import com.itsharex.blog.entity.WebsiteConfig;
import com.itsharex.blog.service.BlogInfoService;
import com.itsharex.blog.service.PageService;
import com.itsharex.blog.service.RedisService;
import com.itsharex.blog.service.UniqueViewService;
import com.itsharex.blog.util.BeanCopyUtils;
import com.itsharex.blog.util.IpUtils;
import com.itsharex.blog.vo.BlogInfoVO;
import com.itsharex.blog.vo.PageVO;
import com.itsharex.blog.vo.WebsiteConfigVO;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.itsharex.blog.constant.CommonConst.CITY;
import static com.itsharex.blog.constant.CommonConst.DEFAULT_CONFIG_ID;
import static com.itsharex.blog.constant.CommonConst.PROVINCE;
import static com.itsharex.blog.constant.CommonConst.UNKNOWN;
import static com.itsharex.blog.constant.RedisPrefixConst.BLOG_VIEWS_COUNT;
import static com.itsharex.blog.constant.RedisPrefixConst.UNIQUE_VISITOR;
import static com.itsharex.blog.constant.RedisPrefixConst.VISITOR_AREA;
import static com.itsharex.blog.enums.ArticleStatusEnum.PUBLIC;

/**
 * ??????????????????
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service
public class BlogInfoServiceImpl implements BlogInfoService {
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private MessageDao messageDao;
    @Autowired
    private UniqueViewService uniqueViewService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private WebsiteConfigDao websiteConfigDao;
    @Resource
    private HttpServletRequest request;
    @Autowired
    private PageService pageService;

    @Override
    public BlogHomeInfoDTO getBlogHomeInfo() {
        // ??????????????????
        Integer articleCount = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, PUBLIC.getStatus())
                .eq(Article::getIsDelete, CommonConst.FALSE));
        // ??????????????????
        Integer categoryCount = categoryDao.selectCount(null);
        // ??????????????????
        Integer tagCount = tagDao.selectCount(null);
        // ???????????????
        Object count = redisService.get(BLOG_VIEWS_COUNT);
        String viewsCount = Optional.ofNullable(count).orElse(0).toString();
        // ??????????????????
        WebsiteConfigVO websiteConfig = this.getWebsiteConfig();
        // ??????????????????
        List<PageVO> pageVOList = pageService.listPages();
        // ????????????
        return BlogHomeInfoDTO.builder()
                .articleCount(articleCount)
                .categoryCount(categoryCount)
                .tagCount(tagCount)
                .viewsCount(viewsCount)
                .websiteConfig(websiteConfig)
                .pageList(pageVOList)
                .build();
    }

    @Override
    public BlogBackInfoDTO getBlogBackInfo() {
        // ???????????????
        Object count = redisService.get(BLOG_VIEWS_COUNT);
        Integer viewsCount = Integer.parseInt(Optional.ofNullable(count).orElse(0).toString());
        // ???????????????
        Integer messageCount = messageDao.selectCount(null);
        // ???????????????
        Integer userCount = userInfoDao.selectCount(null);
        // ???????????????
        Integer articleCount = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, CommonConst.FALSE));
        // ?????????????????????
        List<UniqueViewDTO> uniqueViewList = uniqueViewService.listUniqueViews();
        // ??????????????????
        List<ArticleStatisticsDTO> articleStatisticsList = articleDao.listArticleStatistics();
        // ??????????????????
        List<CategoryDTO> categoryDTOList = categoryDao.listCategoryDTO();
        // ??????????????????
        List<TagDTO> tagDTOList = BeanCopyUtils.copyList(tagDao.selectList(null), TagDTO.class);
        // ??????redis????????????????????????
        Map<Object, Double> articleMap = redisService.zReverseRangeWithScore(RedisPrefixConst.ARTICLE_VIEWS_COUNT, 0, 4);
        BlogBackInfoDTO blogBackInfoDTO = BlogBackInfoDTO.builder()
                .articleStatisticsList(articleStatisticsList)
                .tagDTOList(tagDTOList)
                .viewsCount(viewsCount)
                .messageCount(messageCount)
                .userCount(userCount)
                .articleCount(articleCount)
                .categoryDTOList(categoryDTOList)
                .uniqueViewDTOList(uniqueViewList)
                .build();
        if (CollectionUtils.isNotEmpty(articleMap)) {
            // ??????????????????
            List<ArticleRankDTO> articleRankDTOList = listArticleRank(articleMap);
            blogBackInfoDTO.setArticleRankDTOList(articleRankDTOList);
        }
        return blogBackInfoDTO;
    }

    @Override
    public void updateWebsiteConfig(WebsiteConfigVO websiteConfigVO) {
        // ??????????????????
        WebsiteConfig websiteConfig = WebsiteConfig.builder()
                .id(1)
                .config(JSON.toJSONString(websiteConfigVO))
                .build();
        websiteConfigDao.updateById(websiteConfig);
        // ????????????
        redisService.del(RedisPrefixConst.WEBSITE_CONFIG);
    }

    @Override
    public WebsiteConfigVO getWebsiteConfig() {
        WebsiteConfigVO websiteConfigVO;
        // ??????????????????
        Object websiteConfig = redisService.get(RedisPrefixConst.WEBSITE_CONFIG);
        if (Objects.nonNull(websiteConfig)) {
            websiteConfigVO = JSON.parseObject(websiteConfig.toString(), WebsiteConfigVO.class);
        } else {
            // ?????????????????????
            String config = websiteConfigDao.selectById(DEFAULT_CONFIG_ID).getConfig();
            websiteConfigVO = JSON.parseObject(config, WebsiteConfigVO.class);
            redisService.set(RedisPrefixConst.WEBSITE_CONFIG, config);
        }
        return websiteConfigVO;
    }

    @Override
    public String getAbout() {
        Object value = redisService.get(RedisPrefixConst.ABOUT);
        return Objects.nonNull(value) ? value.toString() : "";
    }

    @Override
    public void updateAbout(BlogInfoVO blogInfoVO) {
        redisService.set(RedisPrefixConst.ABOUT, blogInfoVO.getAboutContent());
    }

    @Override
    public void report() {
        // ??????ip
        String ipAddress = IpUtils.getIpAddress(request);
        // ??????????????????
        UserAgent userAgent = IpUtils.getUserAgent(request);
        Browser browser = userAgent.getBrowser();
        OperatingSystem operatingSystem = userAgent.getOperatingSystem();
        // ????????????????????????
        String uuid = ipAddress + browser.getName() + operatingSystem.getName();
        String md5 = DigestUtils.md5DigestAsHex(uuid.getBytes());
        // ??????????????????
        if (!redisService.sIsMember(UNIQUE_VISITOR, md5)) {
            // ????????????????????????
            String ipSource = IpUtils.getIpSource(ipAddress);
            if (StringUtils.isNotBlank(ipSource)) {
                ipSource = ipSource.substring(0, 2)
                        .replaceAll(PROVINCE, "")
                        .replaceAll(CITY, "");
                redisService.hIncr(VISITOR_AREA, ipSource, 1L);
            } else {
                redisService.hIncr(VISITOR_AREA, UNKNOWN, 1L);
            }
            // ?????????+1
            redisService.incr(BLOG_VIEWS_COUNT, 1);
            // ??????????????????
            redisService.sAdd(UNIQUE_VISITOR, md5);
        }
    }

    /**
     * ??????????????????
     *
     * @param articleMap ????????????
     * @return {@link List<ArticleRankDTO>} ????????????
     */
    private List<ArticleRankDTO> listArticleRank(Map<Object, Double> articleMap) {
        // ????????????id
        List<Integer> articleIdList = new ArrayList<>(articleMap.size());
        articleMap.forEach((key, value) -> articleIdList.add((Integer) key));
        // ??????????????????
        return articleDao.selectList(new LambdaQueryWrapper<Article>()
                        .select(Article::getId, Article::getArticleTitle)
                        .in(Article::getId, articleIdList))
                .stream().map(article -> ArticleRankDTO.builder()
                        .articleTitle(article.getArticleTitle())
                        .viewsCount(articleMap.get(article.getId()).intValue())
                        .build())
                .sorted(Comparator.comparingInt(ArticleRankDTO::getViewsCount).reversed())
                .collect(Collectors.toList());
    }

}
