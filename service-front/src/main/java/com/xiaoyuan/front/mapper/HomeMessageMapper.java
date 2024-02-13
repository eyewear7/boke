package com.xiaoyuan.front.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyuan.common.pojo.HomeMessage;
import org.springframework.stereotype.Repository;

/**
 * FileName:    HomeMessageMapper
 * Author:      小袁
 * Date:        2022/4/27 20:17
 * Description:
 */
@Repository
public interface HomeMessageMapper extends BaseMapper<HomeMessage> {

    /**
     * 查询留言总数
     * @return
     */
    int findTotal();
}
