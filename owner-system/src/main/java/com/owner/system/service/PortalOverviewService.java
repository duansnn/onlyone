package com.owner.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.domain.vo.PortalOverviewVO;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 门户仪表盘总览：只统计框架里的门户用户。
 */
@Service
@RequiredArgsConstructor
public class PortalOverviewService {

    private final SysUserMapper sysUserMapper;

    public PortalOverviewVO overview(int trendDays) {
        PortalOverviewVO vo = new PortalOverviewVO();
        vo.setPortalUserCount(sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDelFlag, 0)));
        int days = Math.max(trendDays, 1);
        LocalDate today = LocalDate.now();
        List<PortalOverviewVO.TrendPoint> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            PortalOverviewVO.TrendPoint point = new PortalOverviewVO.TrendPoint();
            point.setDate(today.minusDays(i).toString());
            trend.add(point);
        }
        vo.setTrend(trend);
        return vo;
    }
}
