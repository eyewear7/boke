package com.xiaoyuan.back.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyuan.common.pojo.FriendLink;
import com.xiaoyuan.common.vo.R;

/**
 * FileName:    FriendLinkService
 * Author:      小袁
 * Date:        2022/4/30 15:10
 * Description:
 */
public interface FriendLinkService extends IService<FriendLink> {

    /**
     * 添加一条友链
     * @param friendLink
     * @return
     */
    R insert(FriendLink friendLink);

    /**
     * 删除一条友链
     * @param id
     * @return
     */
    R deleteById(Integer id);

    /**
     * 编辑
     * @param friendLink
     * @return
     */
    R modifyById(FriendLink friendLink);

    /**
     * 分页查询友链
     * @return
     */
    R listFriendLinkPage(Integer pageIndex, Integer pageSize);
}
