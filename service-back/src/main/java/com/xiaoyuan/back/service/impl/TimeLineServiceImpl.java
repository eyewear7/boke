package com.xiaoyuan.back.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.back.mapper.TimeLineMapper;
import com.xiaoyuan.back.service.TimeLineService;
import com.xiaoyuan.common.util.DateConverterUtil;
import com.xiaoyuan.common.pojo.TimeLine;
import com.xiaoyuan.common.vo.TimeLineVo;
import com.xiaoyuan.common.vo.PageUtils;
import com.xiaoyuan.common.vo.R;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * FileName:    TimeLineServiceImpl
 * Author:      小袁
 * Date:        2022/4/20 12:27
 * Description:
 */
@Service
@Transactional
public class TimeLineServiceImpl extends ServiceImpl<TimeLineMapper, TimeLine> implements TimeLineService {

    @Autowired
    private TimeLineMapper timeLineMapper;

    @Override
    public R insert(TimeLineVo timeLineVo) {
        TimeLine timeLine = new TimeLine();
        timeLine.setTitle(timeLineVo.getTitle());
        timeLine.setDescribe(timeLineVo.getDescribe());
        timeLine.setContent(timeLineVo.getContent());
        timeLine.setStartTime(DateConverterUtil.toDateFromString(timeLineVo.getStartTime()));
        timeLine.setEndTime(DateConverterUtil.toDateFromString(timeLineVo.getEndTime()));
        return timeLineMapper.insert(timeLine) == 0 ? R.fail("添加失败") : R.success("添加成功");
    }

    @Override
    public R modify(TimeLineVo timeLineVo) {
        TimeLine timeLine = new TimeLine();
        timeLine.setId(Long.parseLong(timeLineVo.getId()));
        timeLine.setTitle(timeLineVo.getTitle());
        timeLine.setDescribe(timeLineVo.getDescribe());
        timeLine.setContent(timeLineVo.getContent());
        timeLine.setStartTime(DateConverterUtil.toDateFromString(timeLineVo.getStartTime()));
        timeLine.setEndTime(DateConverterUtil.toDateFromString(timeLineVo.getEndTime()));
        return timeLineMapper.updateById(timeLine) == 0 ? R.fail("编辑失败") : R.success("编辑成功");
    }

    @Override
    public R delete(Long id) {
        return timeLineMapper.deleteById(id) == 0 ? R.fail("删除失败") : R.success("删除成功");
    }

    @Override
    public R listTimeLinePage(Integer pageIndex, Integer pageSize) {
        // 创建分页对象
        Page<TimeLine> page = new Page<>(pageIndex, pageSize);
        // 查询
        IPage<TimeLine> timeLineIPage = timeLineMapper.selectPage(page, null);
        // 回显对象
        List<TimeLineVo> lineVos = copyList(timeLineIPage.getRecords());

        HashMap<String, Object> map = new HashMap<>();
        map.put("timeLineList", new PageUtils(lineVos, timeLineIPage.getTotal(), pageIndex, pageSize));
        return R.success(map);
    }

    @Override
    public R getTimeLineInfoById(Long id) {
        TimeLine timeLine = timeLineMapper.selectById(id);

        HashMap<String, Object> map = new HashMap<>();
        map.put("timeLine", copy(timeLine));
        return timeLine == null ? R.fail("该条时间线已不存在...") : R.success(map);
    }

    private TimeLineVo copy(TimeLine timeLine) {
        if (timeLine == null) return null;

        TimeLineVo timeLineVo = new TimeLineVo();
        // Long -->> String
        timeLineVo.setId(String.valueOf(timeLine.getId()));
        // 开始时间 和 结束时间
        timeLineVo.setStartTime(DateConverterUtil.toStringFromDate(timeLine.getStartTime()));
        timeLineVo.setEndTime(DateConverterUtil.toStringFromDate(timeLine.getEndTime()));
        // 拷贝剩下的
        BeanUtils.copyProperties(timeLine, timeLineVo);
        return timeLineVo;
    }

    private List<TimeLineVo> copyList(List<TimeLine> list) {
        List<TimeLineVo> lineVos = new ArrayList<>();
        for (TimeLine timeLine : list) {
            lineVos.add(copy(timeLine));
        }
        return lineVos;
    }
}
