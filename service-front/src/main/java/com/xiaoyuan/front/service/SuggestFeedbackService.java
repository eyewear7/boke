package com.xiaoyuan.front.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyuan.common.pojo.SuggestFeedback;
import com.xiaoyuan.common.vo.R;

/**
 * FileName:    SuggestFeedbackService
 * Author:      小袁
 * Date:        2022/4/28 19:11
 * Description:
 */
public interface SuggestFeedbackService extends IService<SuggestFeedback> {

    /**
     * 提交建议
     * @param suggestFeedback
     * @return
     */
    R insert(SuggestFeedback suggestFeedback);
}
