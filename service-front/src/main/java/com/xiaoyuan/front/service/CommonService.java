package com.xiaoyuan.front.service;

import com.xiaoyuan.front.utils.StringThreadLocal;
import com.xiaoyuan.common.constants.RedisConstantKey;
import com.xiaoyuan.common.enums.HttpStatusEnum;
import com.xiaoyuan.common.param.mail.SendMailCodeParam;
import com.xiaoyuan.common.param.SendSmsCodeParam;
import com.xiaoyuan.common.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FileName:    CommonService
 * Author:      小袁
 * Date:        2022/5/2 11:51
 * Description:
 */
@Service
public class CommonService {

    @Autowired
    private ThreadService threadService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public R sendEmailCode(String permissionCode, SendMailCodeParam sendMailCodeParam) {
        String email = sendMailCodeParam.getEmail();
        // 校验权限码
        String rightCode = redisTemplate.opsForValue().get(RedisConstantKey.EMAIL_REQUEST_VERIFY + email);
        if (StringUtils.isBlank(rightCode) || !permissionCode.equals(rightCode)) {
            return R.fail(HttpStatusEnum.ILLEGAL_OPERATION);
        }

        // 随机生成验证码
        String code = String.valueOf(new Random().nextInt(899999) + 100000);

        // 正文内容
        String content = "亲爱的用户：\n" +
                "您此次的验证码为：\n\n" +
                code + "\n\n" +
                "此验证码5分钟内有效，请立即进行下一步操作。 如非你本人操作，请忽略此邮件。\n" +
                "感谢您的使用！";
        // 发送验证码
        threadService.sendSimpleMail(email, "您此次的验证码为：" + code, content);
        // 丢入缓存，设置5分钟过期
        redisTemplate.opsForValue().set(RedisConstantKey.EMAIL + email, code, RedisConstantKey.EMAIL_EXPIRE, TimeUnit.SECONDS);
        return R.success();
    }

    /**
     * 发送短信验证码
     */
    public R sendSmsCode(String permissionCode, SendSmsCodeParam sendSmsCodeParam) {
        String phone = sendSmsCodeParam.getPhone();
        String rightCode = redisTemplate.opsForValue().get(RedisConstantKey.MOBILE_NUMBER_REQUEST_VERIFY + phone);
        if (StringUtils.isBlank(rightCode)) {
            return R.fail(HttpStatusEnum.ILLEGAL_OPERATION);
        }else if (!rightCode.equals(permissionCode)) {
            return R.fail(HttpStatusEnum.ILLEGAL_OPERATION);
        }
//        threadService.sendOneSms(sendSmsCodeParam);
        return R.success();
    }

    // 获取短信验证码权限Cookie
    public R getRequestPermissionCode(SendSmsCodeParam sendSmsCodeParam) {
        String permissionCode = UUID.randomUUID().toString();
        StringThreadLocal.put(permissionCode);
        redisTemplate.opsForValue().set(RedisConstantKey.MOBILE_NUMBER_REQUEST_VERIFY + sendSmsCodeParam.getPhone(), permissionCode , RedisConstantKey.EXPIRE_TEN_SECOND, TimeUnit.SECONDS);
        return R.success();
    }

    // 获取发送邮箱权限码
    public R getEMailCodePermission(SendMailCodeParam sendMailCodeParam) {
        // 随机生成权限码
        String permissionCode = UUID.randomUUID().toString();

        StringThreadLocal.put(permissionCode);
        redisTemplate.opsForValue().set(RedisConstantKey.EMAIL_REQUEST_VERIFY + sendMailCodeParam.getEmail(), permissionCode, RedisConstantKey.EXPIRE_TEN_SECOND, TimeUnit.SECONDS);
        return R.success();
    }
}
