package com.itsharex.blog.strategy.impl;

import com.alibaba.fastjson.JSON;
import com.itsharex.blog.config.WeiboConfigProperties;
import com.itsharex.blog.dto.SocialTokenDTO;
import com.itsharex.blog.dto.SocialUserInfoDTO;
import com.itsharex.blog.dto.WeiboTokenDTO;
import com.itsharex.blog.dto.WeiboUserInfoDTO;
import com.itsharex.blog.enums.LoginTypeEnum;
import com.itsharex.blog.exception.BizException;
import com.itsharex.blog.vo.WeiboLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.itsharex.blog.constant.SocialLoginConst.ACCESS_TOKEN;
import static com.itsharex.blog.constant.SocialLoginConst.CLIENT_ID;
import static com.itsharex.blog.constant.SocialLoginConst.CLIENT_SECRET;
import static com.itsharex.blog.constant.SocialLoginConst.CODE;
import static com.itsharex.blog.constant.SocialLoginConst.GRANT_TYPE;
import static com.itsharex.blog.constant.SocialLoginConst.REDIRECT_URI;
import static com.itsharex.blog.constant.SocialLoginConst.UID;
import static com.itsharex.blog.enums.StatusCodeEnum.WEIBO_LOGIN_ERROR;

/**
 * 微博登录策略实现
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service("weiboLoginStrategyImpl")
public class WeiboLoginStrategyImpl extends AbstractSocialLoginStrategyImpl {
    @Autowired
    private WeiboConfigProperties weiboConfigProperties;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public SocialTokenDTO getSocialToken(String data) {
        WeiboLoginVO weiBoLoginVO = JSON.parseObject(data, WeiboLoginVO.class);
        // 获取微博token信息
        WeiboTokenDTO weiboToken = getWeiboToken(weiBoLoginVO);
        // 返回token信息
        return SocialTokenDTO.builder()
                .openId(weiboToken.getUid())
                .accessToken(weiboToken.getAccess_token())
                .loginType(LoginTypeEnum.WEIBO.getType())
                .build();
    }

    @Override
    public SocialUserInfoDTO getSocialUserInfo(SocialTokenDTO socialTokenDTO) {
        // 定义请求参数
        Map<String, String> data = new HashMap<>(2);
        data.put(UID, socialTokenDTO.getOpenId());
        data.put(ACCESS_TOKEN, socialTokenDTO.getAccessToken());
        // 获取微博用户信息
        WeiboUserInfoDTO weiboUserInfoDTO = restTemplate.getForObject(weiboConfigProperties.getUserInfoUrl(), WeiboUserInfoDTO.class, data);
        // 返回用户信息
        return SocialUserInfoDTO.builder()
                .nickname(Objects.requireNonNull(weiboUserInfoDTO).getScreen_name())
                .avatar(weiboUserInfoDTO.getAvatar_hd())
                .build();
    }

    /**
     * 获取微博token信息
     *
     * @param weiBoLoginVO 微博登录信息
     * @return {@link WeiboTokenDTO} 微博token
     */
    private WeiboTokenDTO getWeiboToken(WeiboLoginVO weiBoLoginVO) {
        // 根据code换取微博uid和accessToken
        MultiValueMap<String, String> weiboData = new LinkedMultiValueMap<>();
        // 定义微博token请求参数
        weiboData.add(CLIENT_ID, weiboConfigProperties.getAppId());
        weiboData.add(CLIENT_SECRET, weiboConfigProperties.getAppSecret());
        weiboData.add(GRANT_TYPE, weiboConfigProperties.getGrantType());
        weiboData.add(REDIRECT_URI, weiboConfigProperties.getRedirectUrl());
        weiboData.add(CODE, weiBoLoginVO.getCode());
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(weiboData, null);
        try {
            return restTemplate.exchange(weiboConfigProperties.getAccessTokenUrl(), HttpMethod.POST, requestEntity, WeiboTokenDTO.class).getBody();
        } catch (Exception e) {
            throw new BizException(WEIBO_LOGIN_ERROR);
        }
    }

}
