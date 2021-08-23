package com.itsharex.blog.controller;

import com.itsharex.blog.annotation.OptLog;
import com.itsharex.blog.dto.CommentBackDTO;
import com.itsharex.blog.dto.CommentDTO;
import com.itsharex.blog.dto.ReplyDTO;
import com.itsharex.blog.service.CommentService;
import com.itsharex.blog.vo.CommentVO;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.PageResult;
import com.itsharex.blog.vo.Result;
import com.itsharex.blog.vo.ReviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

import static com.itsharex.blog.constant.OptTypeConst.REMOVE;
import static com.itsharex.blog.constant.OptTypeConst.UPDATE;

/**
 * 评论控制器
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Api(tags = "评论模块")
@RestController
public class CommentController {
    @Autowired
    private CommentService commentService;

    /**
     * 查询评论
     *
     * @param articleId 文章id
     * @return {@link Result < CommentDTO >} 评论列表
     */
    @ApiOperation(value = "查询评论")
    @ApiImplicitParam(name = "articleId", value = "文章id", required = true, dataType = "Integer")
    @GetMapping("/comments")
    public Result<PageResult<CommentDTO>> listComments(Integer articleId) {
        return Result.ok(commentService.listComments(articleId));
    }

    /**
     * 添加评论
     *
     * @param commentVO 评论信息
     * @return {@link Result<>}
     */
    @ApiOperation(value = "添加评论")
    @PostMapping("/comments")
    public Result<?> saveComment(@Valid @RequestBody CommentVO commentVO) {
        commentService.saveComment(commentVO);
        return Result.ok();
    }

    /**
     * 查询评论下的回复
     *
     * @param commentId 评论id
     * @return {@link Result< ReplyDTO >} 回复列表
     */
    @ApiOperation(value = "查询评论下的回复")
    @ApiImplicitParam(name = "commentId", value = "评论id", required = true, dataType = "Integer")
    @GetMapping("/comments/{commentId}/replies")
    public Result<List<ReplyDTO>> listRepliesByCommentId(@PathVariable("commentId") Integer commentId) {
        return Result.ok(commentService.listRepliesByCommentId(commentId));
    }

    /**
     * 评论点赞
     *
     * @param commentId 评论id
     * @return {@link Result<>}
     */
    @ApiOperation(value = "评论点赞")
    @PostMapping("/comments/{commentId}/like")
    public Result<?> saveCommentLike(@PathVariable("commentId") Integer commentId) {
        commentService.saveCommentLike(commentId);
        return Result.ok();
    }

    /**
     * 审核评论
     *
     * @param reviewVO 审核信息
     * @return {@link Result<>}
     */
    @OptLog(optType = UPDATE)
    @ApiOperation(value = "审核评论")
    @PutMapping("/admin/comments/review")
    public Result<?> updateCommentsReview(@Valid @RequestBody ReviewVO reviewVO) {
        commentService.updateCommentsReview(reviewVO);
        return Result.ok();
    }

    /**
     * 删除评论
     *
     * @param commentIdList 评论id列表
     * @return {@link Result<>}
     */
    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除评论")
    @DeleteMapping("/admin/comments")
    public Result<?> deleteComments(@RequestBody List<Integer> commentIdList) {
        commentService.removeByIds(commentIdList);
        return Result.ok();
    }

    /**
     * 查询后台评论
     *
     * @param condition 条件
     * @return {@link Result< CommentBackDTO >} 后台评论
     */
    @ApiOperation(value = "查询后台评论")
    @GetMapping("/admin/comments")
    public Result<PageResult<CommentBackDTO>> listCommentBackDTO(ConditionVO condition) {
        return Result.ok(commentService.listCommentBackDTO(condition));
    }

}

