package com.itsharex.blog.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsharex.blog.constant.RedisPrefixConst;
import com.itsharex.blog.dao.UniqueViewDao;
import com.itsharex.blog.dto.UniqueViewDTO;
import com.itsharex.blog.entity.UniqueView;
import com.itsharex.blog.service.RedisService;
import com.itsharex.blog.service.UniqueViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.itsharex.blog.constant.RedisPrefixConst.VISITOR_AREA;
import static com.itsharex.blog.enums.ZoneEnum.SHANGHAI;

/**
 * 访问量统计服务
 *
 * @author wuchunfu
 * @date 2021-08-21
 */
@Service
public class UniqueViewServiceImpl extends ServiceImpl<UniqueViewDao, UniqueView> implements UniqueViewService {
    @Autowired
    private RedisService redisService;
    @Autowired
    private UniqueViewDao uniqueViewDao;

    @Override
    public List<UniqueViewDTO> listUniqueViews() {
        DateTime startTime = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -7));
        DateTime endTime = DateUtil.endOfDay(new Date());
        return uniqueViewDao.listUniqueViews(startTime, endTime);
    }

    @Scheduled(cron = " 0 0 0 * * ?")
    public void saveUniqueView() {
        // 获取每天用户量
        Long count = redisService.sSize(RedisPrefixConst.UNIQUE_VISITOR);
        // 获取昨天日期插入数据
        UniqueView uniqueView = UniqueView.builder()
                .createTime(LocalDateTimeUtil.offset(LocalDateTime.now(ZoneId.of(SHANGHAI.getZone())), -1, ChronoUnit.DAYS))
                .viewsCount(Optional.of(count.intValue()).orElse(0))
                .build();
        uniqueViewDao.insert(uniqueView);
    }

    @Scheduled(cron = " 0 1 0 * * ?")
    public void clear() {
        // 清空redis访客记录
        redisService.del(RedisPrefixConst.UNIQUE_VISITOR);
        // 清空redis游客区域统计
        redisService.del(VISITOR_AREA);
    }

}