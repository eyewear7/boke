package com.xiaoyuan.front.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.common.util.EncryptionAlgorithmUtil;
import com.xiaoyuan.common.util.DateConverterUtil;
import com.xiaoyuan.common.util.StringMatch;
import com.xiaoyuan.front.mapper.HomeMessageMapper;
import com.xiaoyuan.front.service.HomeMessageService;
import com.xiaoyuan.front.utils.UserThreadLocal;
import com.xiaoyuan.front.vo.CommonUserVo;
import com.xiaoyuan.front.vo.HomeMessageVo;
import com.xiaoyuan.front.vo.param.HomeMessageParam;
import com.xiaoyuan.common.pojo.HomeMessage;
import com.xiaoyuan.common.enums.HttpStatusEnum;
import com.xiaoyuan.common.vo.PageUtils;
import com.xiaoyuan.common.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileName:    HomeMessageServiceImpl
 * Author:      小袁
 * Date:        2022/4/27 20:16
 * Description:
 */
@Service
@Transactional
public class HomeMessageServiceImpl extends ServiceImpl<HomeMessageMapper, HomeMessage> implements HomeMessageService {

    @Override
    public R insert(HomeMessageParam homeMessageParam) {
        /**
         * 统一非法参数校验
         */
        if (homeMessageParam == null) {
            return R.fail(HttpStatusEnum.PARAM_ERROR);
        }else if (StringUtils.isBlank(homeMessageParam.getContent())) {
            return R.fail(HttpStatusEnum.PARAM_ERROR);
        }else if (StringMatch.isContainsEmoji(homeMessageParam.getContent())) {
            return R.fail(HttpStatusEnum.NO_SUPPORT_EMOJI);
        }

        // 获取被拦截的用户数据（此接口被权限拦截）
        CommonUserVo commonUserVo = UserThreadLocal.get();

        // 创建留言对象, 赋值
        HomeMessage homeMessage = new HomeMessage();
        // 对用户编号解码, 设置留言人
        homeMessage.setAuthorId(Long.parseLong(EncryptionAlgorithmUtil.decode(commonUserVo.getNumber())));
        homeMessage.setContent(homeMessageParam.getContent());
        homeMessage.setDetailContent(homeMessageParam.getDetailContent());

        // 插入
        return this.baseMapper.insert(homeMessage) == 0 ? R.fail() : R.success();
    }

    @Override
    public R listHomeMessagePage(PageUtils pageUtils) {
        if (pageUtils == null) {
            return R.fail();
        }

        int pageIndex = pageUtils.getPageIndex();
        int pageSize = pageUtils.getPageSize();

        if (pageIndex <= 0 || pageSize <= 0) {
            return R.fail().message("参数错误");
        }

        if (pageSize > 32) {
            return R.fail().message("超出限制");
        }

        // 分页对象
        Page<HomeMessage> page = new Page<>(pageIndex, pageSize);
        // 构建条件
        QueryWrapper<HomeMessage> wrapper = new QueryWrapper<>();
        wrapper.select("content", "detail_content", "background", "gmt_create");
        wrapper.orderByDesc("gmt_create");

        // 查询, 回传对象
        IPage<HomeMessage> homeMessageIPage = this.baseMapper.selectPage(page, wrapper);

        // 前端显示对象
        List<HomeMessageVo> homeMessageVoList = new ArrayList<>();

        // 查询结果对象
        List<HomeMessage> homeMessageList = homeMessageIPage.getRecords();

        // 转换 为前端Vo
        homeMessageList.forEach(item -> {
            homeMessageVoList.add(copy(item));
        });

        // 重新赋值到分页工具对象
        pageUtils = new PageUtils(homeMessageVoList, homeMessageIPage.getTotal(), pageIndex, pageSize);
        // 回传
        Map<String, Object> map = new HashMap<>();
        map.put("messageList", pageUtils);
        return R.success(map);
    }

    @Override
    public Integer findMessageTotal() {
        return this.baseMapper.findTotal();
    }

    private HomeMessageVo copy(HomeMessage homeMessage) {
        HomeMessageVo homeMessageVo = new HomeMessageVo();
        BeanUtils.copyProperties(homeMessage, homeMessageVo);
        // 格式化日期
        homeMessageVo.setGmtCreate(DateConverterUtil.toStringFromDate(homeMessage.getGmtCreate()));

        return homeMessageVo;
    }
}
