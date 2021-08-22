package com.itsharex.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微博token
 *
 * @author wuchunfu
 * @date 2021-08-21
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WeiboTokenDTO {

    /**
     * 微博uid
     */
    private String uid;

    /**
     * 访问令牌
     */
    private String access_token;

}
