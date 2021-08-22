package com.itsharex.blog.handler;

import com.itsharex.blog.constant.RedisPrefixConst;
import com.itsharex.blog.service.RedisService;
import com.itsharex.blog.util.IpUtils;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * request过滤器
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Component
public class ServletRequestListenerImpl implements ServletRequestListener {
    @Autowired
    private RedisService redisService;

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        // 获取ip
        String ipAddress = IpUtils.getIpAddress(request);
        // 获取访问设备
        UserAgent userAgent = IpUtils.getUserAgent(request);
        Browser browser = userAgent.getBrowser();
        OperatingSystem operatingSystem = userAgent.getOperatingSystem();
        // 生成唯一用户标识
        String uuid = ipAddress + browser.getName() + operatingSystem.getName();
        String md5 = DigestUtils.md5DigestAsHex(uuid.getBytes());
        // 判断是否访问
        if (!redisService.sIsMember(RedisPrefixConst.UNIQUE_VISITOR, md5)) {
            redisService.incr(RedisPrefixConst.BLOG_VIEWS_COUNT, 1);
        }
        redisService.sAdd(RedisPrefixConst.UNIQUE_VISITOR, md5);
    }

}
