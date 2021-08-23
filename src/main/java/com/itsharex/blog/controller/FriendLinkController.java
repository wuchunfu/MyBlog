package com.itsharex.blog.controller;

import com.itsharex.blog.annotation.OptLog;
import com.itsharex.blog.dto.FriendLinkBackDTO;
import com.itsharex.blog.dto.FriendLinkDTO;
import com.itsharex.blog.service.FriendLinkService;
import com.itsharex.blog.vo.ConditionVO;
import com.itsharex.blog.vo.FriendLinkVO;
import com.itsharex.blog.vo.PageResult;
import com.itsharex.blog.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

import static com.itsharex.blog.constant.OptTypeConst.REMOVE;
import static com.itsharex.blog.constant.OptTypeConst.SAVE_OR_UPDATE;

/**
 * 友链控制器
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Api(tags = "友链模块")
@RestController
public class FriendLinkController {
    @Autowired
    private FriendLinkService friendLinkService;

    /**
     * 查看友链列表
     *
     * @return {@link Result < FriendLinkDTO >} 友链列表
     */
    @ApiOperation(value = "查看友链列表")
    @GetMapping("/links")
    public Result<List<FriendLinkDTO>> listFriendLinks() {
        return Result.ok(friendLinkService.listFriendLinks());
    }

    /**
     * 查看后台友链列表
     *
     * @param condition 条件
     * @return {@link Result< FriendLinkBackDTO >} 后台友链列表
     */
    @ApiOperation(value = "查看后台友链列表")
    @GetMapping("/admin/links")
    public Result<PageResult<FriendLinkBackDTO>> listFriendLinkDTO(ConditionVO condition) {
        return Result.ok(friendLinkService.listFriendLinkDTO(condition));
    }

    /**
     * 保存或修改友链
     *
     * @param friendLinkVO 友链信息
     * @return {@link Result<>}
     */
    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "保存或修改友链")
    @PostMapping("/admin/links")
    public Result<?> saveOrUpdateFriendLink(@Valid @RequestBody FriendLinkVO friendLinkVO) {
        friendLinkService.saveOrUpdateFriendLink(friendLinkVO);
        return Result.ok();
    }

    /**
     * 删除友链
     *
     * @param linkIdList 友链id列表
     * @return {@link Result<>}
     */
    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除友链")
    @DeleteMapping("/admin/links")
    public Result<?> deleteFriendLink(@RequestBody List<Integer> linkIdList) {
        friendLinkService.removeByIds(linkIdList);
        return Result.ok();
    }

}

