package com.xiaoyuan.common.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * FileName:    ArticleComment
 * Author:      小袁
 * Date:        2022/5/3 19:28
 * Description:
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "xy_article_comment")
public class ArticleComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论编号, 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 文章编号
     */
    private Long articleId;

    /**
     * 父级评论
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;
}
