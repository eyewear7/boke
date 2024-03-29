package com.xiaoyuan.back.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.xiaoyuan.back.constants.QiniuProperties;
import com.xiaoyuan.back.mapper.*;
import com.xiaoyuan.back.service.ArticleService;
import com.xiaoyuan.back.service.CategoryService;
import com.xiaoyuan.back.service.helper.CommonUserServiceHelper;
import com.xiaoyuan.common.convert.TextOperation;
import com.xiaoyuan.common.util.RandomUtil;
import com.xiaoyuan.common.vo.PageVo;
import com.xiaoyuan.common.pojo.Article;
import com.xiaoyuan.common.pojo.ArticleCategory;
import com.xiaoyuan.common.pojo.ArticleContent;
import com.xiaoyuan.common.param.ArticleParam;
import com.xiaoyuan.common.param.article.ArticleQueryParam;
import com.xiaoyuan.common.vo.R;
import com.xiaoyuan.common.vo.article.ArticlePublishVo;
import com.xiaoyuan.common.vo.article.ArticleVo;
import com.xiaoyuan.common.exception.CustomerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * FileName:    ArticleServiceImpl
 * Author:      小袁
 * Date:        2022/4/16 11:24
 * Description:
 */
@Service
@Transactional
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    /**
     * 文章表DAO
     */
    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 文章表和文章分类表中间表DAO
     */
    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    /**
     * 文章表和文章内容表的中间表DAO
     */
    @Autowired
    private ArticleContentMapper articleContentMapper;

    /**
     * 分类业务层
     */
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CommonUserServiceHelper commonUserServiceHelper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public int publish(ArticleParam articleParam) {
        return createArticle(articleParam, StpUtil.getLoginIdAsLong());
    }

    @Override
    public int schedulePublish(ArticleParam articleParam, Long authorId) {
        return createArticle(articleParam, authorId);
    }

    @Override
    public R<PageVo<List<ArticleVo>>> listArticlePage(ArticleQueryParam articleQueryParam) {
        Page<ArticleVo> page = new Page<>(articleQueryParam.getPageIndex(), articleQueryParam.getPageSize());
        IPage<ArticleVo> iPage = this.baseMapper.listArticlePage(page, articleQueryParam);

        List<ArticleVo> articleVoList = iPage.getRecords();
        articleVoList.forEach(item -> {
            item.setCategoryList(categoryService.findCategoryNamesByArticleId(Long.parseLong(item.getId())));
        });

        return R.success(new PageVo<>(articleVoList, iPage.getTotal()));
    }

    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleCollectMapper articleCollectMapper;


    @Override
    public R removeArticleById(Long articleId) {
        // 先删除文章
        int deleteArticleResult = articleMapper.deleteById(articleId);

        // 再从 文章-内容中间表 删除文章内容
        QueryWrapper<ArticleContent> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        int deleteContentResult = articleContentMapper.delete(wrapper);

        // 再从 文章-分类中间表 删除文章所属分类
        QueryWrapper<ArticleCategory> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("article_id", articleId);
        articleCategoryMapper.delete(wrapper1);

        // 删除文章留言
        articleCommentMapper.removeByArticleId(articleId);

        // 删除收藏
        articleCollectMapper.removeByArticleId(articleId);

        // 删除点赞
        articleLikeMapper.removeByArticleId(articleId);

        return deleteArticleResult == 0 || deleteContentResult == 0 ? R.fail("文章删除失败！") : R.success("文章成功删除！");
    }

    @Override
    public R getArticleDetailById(Long articleId) {
        // 构造条件对象, 设置article_id = articleId
        QueryWrapper<ArticleContent> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);

        // 筛选查询结果, 查找HTML格式文本, 限制只查询一条数据, 提高效率
        wrapper.select("content");
        wrapper.last("limit 1");

        // 调用DAO
        List<ArticleContent> articleContents = articleContentMapper.selectList(wrapper);
        if (articleContents.size() <= 0) {
            return R.fail("文章不存在");
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("articleContent", articleContents.get(0).getContent());
        return R.success(map);
    }

    @Override
    public R getArticlePublishById(Long articleId) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("title", "digest", "cover", "tags");
        wrapper.eq("id", articleId);
        wrapper.last("limit 1");
        Article article = articleMapper.selectOne(wrapper);

        HashMap<String, Object> map = new HashMap<>();
        map.put("articlePublish", copy(article, articleId));
        return R.success(map);
    }

    @Override
    public R modifyArticleById(ArticleParam articleParam) {
        // 文章编号
        Long articleId = Long.parseLong(articleParam.getId());

        // 修改文章表
        Article article = new Article();
        article.setId(articleId);
        BeanUtils.copyProperties(articleParam, article);
        int modifyArticleResult = articleMapper.updateById(article);

        // 修改 文章-内容 中间表
        ArticleContent articleContent = new ArticleContent();
        articleContent.setContent(articleParam.getArticleContentParam().getContent());
        articleContent.setContentHtml(articleParam.getArticleContentParam().getContentHtml());
        // 设置条件 article_id = id
        QueryWrapper<ArticleContent> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        int modifyArticleContentResult = articleContentMapper.update(articleContent, wrapper);

        // 修改 文章-分类 中间表
        // 先删除原有的分类
        QueryWrapper<ArticleCategory> articleCategoryQueryWrapper = new QueryWrapper<>();
        // 构造删除条件 article_id = id
        articleCategoryQueryWrapper.eq("article_id", articleId);
        articleCategoryMapper.delete(articleCategoryQueryWrapper);
        // 再添加新的分类列表
        List<Integer> categoryIds = articleParam.getCategoryList();
        for (Integer categoryId : categoryIds) {
            ArticleCategory articleCategory = new ArticleCategory();
            articleCategory.setArticleId(articleId);
            articleCategory.setCategoryId(categoryId);
            articleCategoryMapper.insert(articleCategory);
        }

        return modifyArticleResult == 0 || modifyArticleContentResult == 0 ? R.fail("修改失败！") : R.success("文章修改成功！");
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.getSize() > 1024 * 1024 * 5) {
            throw new CustomerException("图片不得超过5MB！");
        }

        if (!file.getContentType().startsWith("image/")) {
            throw new CustomerException("请上传图片类型文件！");
        }

        // 上传到七牛云
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;// 指定分片上传版本
        //...其他参数参考类注释

        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        String accessKey = QiniuProperties.ACCESS_KEY;
        String secretKey = QiniuProperties.SECRET_KEY;
        String bucket = QiniuProperties.BUCKET;

        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = "article/" + RandomUtil.randomStrUUID(false) + "." + FileUtil.extName(file.getOriginalFilename());

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(file.getInputStream(), key, upToken, null, null);
            new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            throw new CustomerException("七牛云上传失败！请检查配置");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return QiniuProperties.URL_PREFIX + key;
    }

    private ArticlePublishVo copy(Article article, Long id) {
        ArticlePublishVo articlePublishVo = new ArticlePublishVo();
        // 查询文章所属分类 return 分类编号列表
        List<Integer> categoryIds = articleCategoryMapper.findCategoryIdsByArticleId(id);
        if (categoryIds.size() == 0) {
            articlePublishVo.setCategorySelected(null);
        } else {
            // 通过分类编号列表查询每个分类完整路径, 用于selected
            List<String> list = categoryMapper.getCompleteCategoryByIds(categoryIds);
            // 存储, 文章分类选中列表
            List<List<String>> categorySelected = new ArrayList<>();
            for (String s : list) {
                List<String> splitCategory = new ArrayList<>();
                // 切割 1-2-12 -->> 1 2 12
                String[] arr = s.split("-");
                // 转换 arr(数组) -->> list(集合)
                Collections.addAll(splitCategory, arr);
                categorySelected.add(splitCategory);
            }
            articlePublishVo.setCategorySelected(categorySelected);
        }
        // 查询详细内容
        articlePublishVo.setContent(articleContentMapper.getArticleMarkdownById(id));
        // 拷贝
        BeanUtils.copyProperties(article, articlePublishVo);
        return articlePublishVo;
    }

    private int createArticle(ArticleParam articleParam, Long authorId) {
        Article article = new Article();
        article.setTitle(articleParam.getTitle()); // 标题
        article.setCover(articleParam.getCover()); // 封面
        // 提取摘要
        if (articleParam.getDigest() != null) {
            article.setDigest(articleParam.getDigest());
        } else {
            String digest = TextOperation.getTextFromHtml(articleParam.getArticleContentParam().getContentHtml());
            article.setDigest(TextOperation.getArticleDigestFromText(digest)); // 摘要
        }
        article.setTags(articleParam.getTags()); // 标签
        article.setAuthorId(authorId);

        // 插入数据, 获取回传的文章编号（MyBatis-Plus内部封装）
        articleMapper.insert(article);

        /**
         * 插入分类专栏数据到中间表
         * [1,2,3] --> 循环插入
         */
        List<Integer> categoryList = articleParam.getCategoryList();
        for (Integer categoryId : categoryList) {
            ArticleCategory articleCategory = new ArticleCategory();
            articleCategory.setCategoryId(categoryId);
            articleCategory.setArticleId(article.getId());
            articleCategoryMapper.insert(articleCategory);
        }

        // 插入markdown文本和HTML格式文本数据到中间表
        ArticleContent articleContent = new ArticleContent();
        articleContent.setArticleId(article.getId());
        articleContent.setContent(articleParam.getArticleContentParam().getContent());
        articleContent.setContentHtml(articleParam.getArticleContentParam().getContentHtml());
        return articleContentMapper.insert(articleContent);
    }
}
