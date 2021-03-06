package com.itsharex.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsharex.blog.constant.CommonConst;
import com.itsharex.blog.constant.MQPrefixConst;
import com.itsharex.blog.constant.RedisPrefixConst;
import com.itsharex.blog.dao.ArticleDao;
import com.itsharex.blog.dao.CommentDao;
import com.itsharex.blog.dao.TalkDao;
import com.itsharex.blog.dao.UserInfoDao;
import com.itsharex.blog.dto.CommentBackDTO;
import com.itsharex.blog.dto.CommentDTO;
import com.itsharex.blog.dto.EmailDTO;
import com.itsharex.blog.dto.ReplyCountDTO;
import com.itsharex.blog.dto.ReplyDTO;
import com.itsharex.blog.entity.Comment;
import com.itsharex.blog.service.BlogInfoService;
import com.itsharex.blog.service.CommentService;
import com.itsharex.blog.service.RedisService;
import com.itsharex.blog.util.HTMLUtils;
import com.itsharex.blog.util.PageUtils;
import com.itsharex.blog.util.UserUtils;
import com.itsharex.blog.vo.CommentVO;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.PageResult;
import com.itsharex.blog.vo.ReviewVO;
import com.itsharex.blog.vo.WebsiteConfigVO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.itsharex.blog.constant.CommonConst.BLOGGER_ID;
import static com.itsharex.blog.constant.CommonConst.TRUE;
import static com.itsharex.blog.enums.CommentTypeEnum.getCommentEnum;
import static com.itsharex.blog.enums.CommentTypeEnum.getCommentPath;

/**
 * ????????????
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentDao, Comment> implements CommentService {
    @Autowired
    private CommentDao commentDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private TalkDao talkDao;
    @Autowired
    private RedisService redisService;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private BlogInfoService blogInfoService;

    /**
     * ????????????
     */
    @Value("${website.url}")
    private String websiteUrl;

    @Override
    public PageResult<CommentDTO> listComments(CommentVO commentVO) {
        // ???????????????
        Integer commentCount = commentDao.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Objects.nonNull(commentVO.getTopicId()), Comment::getTopicId, commentVO.getTopicId())
                .eq(Comment::getType, commentVO.getType())
                .isNull(Comment::getParentId)
                .eq(Comment::getIsReview, TRUE));
        if (commentCount == 0) {
            return new PageResult<>();
        }
        // ????????????????????????
        List<CommentDTO> commentDTOList = commentDao.listComments(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentVO);
        if (CollectionUtils.isEmpty(commentDTOList)) {
            return new PageResult<>();
        }
        // ??????redis?????????????????????
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.COMMENT_LIKE_COUNT);
        // ????????????id??????
        List<Integer> commentIdList = commentDTOList.stream()
                .map(CommentDTO::getId)
                .collect(Collectors.toList());
        // ????????????id????????????????????????
        List<ReplyDTO> replyDTOList = commentDao.listReplies(commentIdList);
        // ?????????????????????
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        // ????????????id??????????????????
        Map<Integer, List<ReplyDTO>> replyMap = replyDTOList.stream()
                .collect(Collectors.groupingBy(ReplyDTO::getParentId));
        // ????????????id???????????????
        Map<Integer, Integer> replyCountMap = commentDao.listReplyCountByCommentId(commentIdList)
                .stream().collect(Collectors.toMap(ReplyCountDTO::getCommentId, ReplyCountDTO::getReplyCount));
        // ??????????????????
        commentDTOList.forEach(item -> {
            item.setLikeCount((Integer) likeCountMap.get(item.getId().toString()));
            item.setReplyDTOList(replyMap.get(item.getId()));
            item.setReplyCount(replyCountMap.get(item.getId()));
        });
        return new PageResult<>(commentDTOList, commentCount);
    }

    @Override
    public List<ReplyDTO> listRepliesByCommentId(Integer commentId) {
        // ????????????????????????????????????
        List<ReplyDTO> replyDTOList = commentDao.listRepliesByCommentId(PageUtils.getLimitCurrent(), PageUtils.getSize(), commentId);
        // ??????redis?????????????????????
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.COMMENT_LIKE_COUNT);
        // ??????????????????
        replyDTOList.forEach(item -> item.setLikeCount((Integer) likeCountMap.get(item.getId().toString())));
        return replyDTOList;
    }

    @Override
    public void saveComment(CommentVO commentVO) {
        // ????????????????????????
        WebsiteConfigVO websiteConfig = blogInfoService.getWebsiteConfig();
        Integer isReview = websiteConfig.getIsCommentReview();
        // ????????????
        commentVO.setCommentContent(HTMLUtils.filter(commentVO.getCommentContent()));
        Comment comment = Comment.builder()
                .userId(UserUtils.getLoginUser().getUserInfoId())
                .replyUserId(commentVO.getReplyUserId())
                .topicId(commentVO.getTopicId())
                .commentContent(commentVO.getCommentContent())
                .parentId(commentVO.getParentId())
                .type(commentVO.getType())
                .isReview(isReview == TRUE ? CommonConst.FALSE : TRUE)
                .build();
        commentDao.insert(comment);
        // ??????????????????????????????,????????????
        if (websiteConfig.getIsEmailNotice().equals(TRUE)) {
            CompletableFuture.runAsync(() -> notice(comment));
        }
    }

    @Override
    public void saveCommentLike(Integer commentId) {
        // ??????????????????
        String commentLikeKey = RedisPrefixConst.COMMENT_USER_LIKE + UserUtils.getLoginUser().getUserInfoId();
        if (redisService.sIsMember(commentLikeKey, commentId)) {
            // ????????????????????????id
            redisService.sRemove(commentLikeKey, commentId);
            // ???????????????-1
            redisService.hDecr(RedisPrefixConst.COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        } else {
            // ????????????????????????id
            redisService.sAdd(commentLikeKey, commentId);
            // ???????????????+1
            redisService.hIncr(RedisPrefixConst.COMMENT_LIKE_COUNT, commentId.toString(), 1L);
        }
    }

    @Override
    public void updateCommentsReview(ReviewVO reviewVO) {
        // ????????????????????????
        List<Comment> commentList = reviewVO.getIdList().stream().map(item -> Comment.builder()
                        .id(item)
                        .isReview(reviewVO.getIsReview())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(commentList);
    }

    @Override
    public PageResult<CommentBackDTO> listCommentBackDTO(ConditionVO condition) {
        // ?????????????????????
        Integer count = commentDao.countCommentDTO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // ????????????????????????
        List<CommentBackDTO> commentBackDTOList = commentDao.listCommentBackDTO(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        return new PageResult<>(commentBackDTOList, count);
    }

    /**
     * ??????????????????
     *
     * @param comment ????????????
     */
    public void notice(Comment comment) {
        // ???????????????????????????
        Integer userId = BLOGGER_ID;
        String id = Objects.nonNull(comment.getTopicId()) ? comment.getTopicId().toString() : "";
        if (Objects.nonNull(comment.getReplyUserId())) {
            userId = comment.getReplyUserId();
        } else {
            switch (Objects.requireNonNull(getCommentEnum(comment.getType()))) {
                case ARTICLE:
                    userId = articleDao.selectById(comment.getTopicId()).getUserId();
                    break;
                case TALK:
                    userId = talkDao.selectById(comment.getTopicId()).getUserId();
                    break;
                default:
                    break;
            }
        }
        String email = userInfoDao.selectById(userId).getEmail();
        if (StringUtils.isNotBlank(email)) {
            // ????????????
            EmailDTO emailDTO = new EmailDTO();
            if (comment.getIsReview().equals(TRUE)) {
                // ????????????
                emailDTO.setEmail(email);
                emailDTO.setSubject("????????????");
                // ??????????????????
                String url = websiteUrl + getCommentPath(comment.getType()) + id;
                emailDTO.setContent("??????????????????????????????????????????" + url + "\n????????????");
            } else {
                // ?????????????????????
                String adminEmail = userInfoDao.selectById(BLOGGER_ID).getEmail();
                emailDTO.setEmail(adminEmail);
                emailDTO.setSubject("????????????");
                emailDTO.setContent("??????????????????????????????????????????????????????????????????");
            }
            rabbitTemplate.convertAndSend(MQPrefixConst.EMAIL_EXCHANGE, "*", new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
        }
    }

}
