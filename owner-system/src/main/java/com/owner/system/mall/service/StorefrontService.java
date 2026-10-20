package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.entity.Category;
import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
import com.owner.system.mall.domain.entity.Review;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.enums.ProductStatus;
import com.owner.system.mall.domain.vo.CategoryVO;
import com.owner.system.mall.domain.vo.ProductCardVO;
import com.owner.system.mall.domain.vo.ProductDetailVO;
import com.owner.system.mall.domain.vo.ReviewVO;
import com.owner.system.mall.domain.vo.ShopBriefVO;
import com.owner.system.mall.domain.vo.StoreHomeVO;
import com.owner.system.mall.mapper.CategoryMapper;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.ReviewMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.support.ProductAssembler;
import com.owner.system.mall.support.ShopGuard;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店面前台：首页、类目列表、搜索、详情、评价列表。
 * <p>这些接口不需要登录，只展示「在售且至少一个规格可售」的商品。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontService {

    /** 首页第一批商品数量 */
    private static final long HOME_PRODUCT_LIMIT = 12L;

    /** 首页一级类目入口数量 */
    private static final int HOME_CATEGORY_LIMIT = 5;

    /** 搜索关键字最少字数 */
    private static final int SEARCH_MIN_LENGTH = 2;

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ReviewMapper reviewMapper;
    private final ShopMapper shopMapper;
    private final SysUserMapper sysUserMapper;
    private final ShopGuard shopGuard;
    private final CategoryService categoryService;
    private final ProductAssembler assembler;

    /**
     * 营业中的店铺列表（公开）。
     * <p>前端未登录时也要能逛店面，用它确定默认进哪家店。</p>
     */
    public List<ShopBriefVO> enabledShops() {
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getStatus, 0)
                .orderByAsc(Shop::getId));
        List<ShopBriefVO> list = new ArrayList<>(shops.size());
        for (Shop shop : shops) {
            list.add(new ShopBriefVO(shop.getId(), shop.getName(), null, null));
        }
        return list;
    }

    // ------------------------------------------------------------------
    // 首页
    // ------------------------------------------------------------------

    /** 店面首页 */
    public StoreHomeVO home(Long shopId) {
        Shop shop = shopGuard.loadActiveShop(shopId);
        List<CategoryVO> categories = categoryService.activeTree(shopId);
        List<CategoryVO> entries = categories.size() > HOME_CATEGORY_LIMIT
                ? categories.subList(0, HOME_CATEGORY_LIMIT)
                : categories;

        PageResult<ProductCardVO> first = cards(shopId, null, null, null, 1L, HOME_PRODUCT_LIMIT);
        return new StoreHomeVO(shop.getId(), shop.getName(), shop.getIntro(),
                StringUtils.trimToNull(shop.getNotice()), shop.getBannerUrl(),
                shop.getBusinessHours(), entries, first.getRows(), first.getTotal());
    }

    // ------------------------------------------------------------------
    // 类目商品 / 搜索
    // ------------------------------------------------------------------

    /** 类目商品列表（类目可传一级，会展开到其二级类目） */
    public PageResult<ProductCardVO> productPage(Long shopId, Long categoryId, String sort, PageQuery query) {
        shopGuard.loadActiveShop(shopId);
        List<Long> categoryIds = expandCategory(shopId, categoryId);
        if (categoryId != null && categoryIds.isEmpty()) {
            // 一级类目下还没有二级类目，自然没有商品
            return PageResult.empty(query.getPageNumOrDefault(), query.getPageSizeOrDefault());
        }
        return cards(shopId, categoryIds, null, sort, query.getPageNumOrDefault(), query.getPageSizeOrDefault());
    }

    /** 搜索本店在售商品的名与卖点，两字起搜 */
    public PageResult<ProductCardVO> search(Long shopId, String keyword, String sort, PageQuery query) {
        shopGuard.loadActiveShop(shopId);
        String trimmed = keyword == null ? null : keyword.trim();
        if (StringUtils.isBlank(trimmed) || trimmed.length() < SEARCH_MIN_LENGTH) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "搜索至少输入 " + SEARCH_MIN_LENGTH + " 个字");
        }
        return cards(shopId, null, trimmed, sort, query.getPageNumOrDefault(), query.getPageSizeOrDefault());
    }

    // ------------------------------------------------------------------
    // 详情
    // ------------------------------------------------------------------

    /** 商品详情。已下架的商品对店面不可见。 */
    public ProductDetailVO detail(Long shopId, Long productId) {
        shopGuard.loadActiveShop(shopId);
        Product product = productMapper.selectById(productId);
        if (product == null || !shopId.equals(product.getShopId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "商品已下架");
        }

        List<Long> ids = List.of(productId);
        List<String> images = assembler.imagesOf(ids).getOrDefault(productId, List.of());
        List<ProductSku> skus = assembler.skusOf(ids).getOrDefault(productId, List.of());

        long reviewCount = reviewMapper.countByProduct(shopId, productId);
        Double average = reviewCount == 0 ? null : reviewMapper.averageScoreByProduct(shopId, productId);

        return new ProductDetailVO(product.getId(), product.getShopId(), product.getName(),
                product.getSellingPoint(), product.getDetail(), images,
                assembler.toStoreSkuVoList(skus),
                ProductAssembler.minPrice(skus), ProductAssembler.maxPrice(skus),
                ProductAssembler.totalAvailable(skus), reviewCount,
                average == null ? 0d : Math.round(average * 10d) / 10d);
    }

    /** 商品评价，每页 10 条，默认最新 */
    public PageResult<ReviewVO> reviews(Long shopId, Long productId, PageQuery query) {
        shopGuard.loadActiveShop(shopId);
        long pageNum = query.getPageNumOrDefault();
        long pageSize = query.getPageSizeOrDefault();
        long total = reviewMapper.countByProduct(shopId, productId);
        if (total == 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Review> reviews = reviewMapper.selectPageByProduct(shopId, productId,
                (pageNum - 1) * pageSize, pageSize);
        List<ReviewVO> rows = toReviewVos(reviews);
        return PageResult.of(rows, total, pageNum, pageSize);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 组装评价出参，批量取昵称与头像 */
    public List<ReviewVO> toReviewVos(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, SysUser> userMap = new HashMap<>(userIds.size() * 2);
        if (!userIds.isEmpty()) {
            for (SysUser user : sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                    .in(SysUser::getId, userIds))) {
                userMap.put(user.getId(), user);
            }
        }
        List<ReviewVO> rows = new ArrayList<>(reviews.size());
        for (Review review : reviews) {
            SysUser user = userMap.get(review.getUserId());
            rows.add(new ReviewVO(review.getId(), review.getOrderId(), review.getUserId(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getAvatarUrl(),
                    review.getScore(), review.getContent(), splitImages(review.getImageUrls()),
                    review.getCreateTime()));
        }
        return rows;
    }

    public static List<String> splitImages(String imageUrls) {
        if (StringUtils.isBlank(imageUrls)) {
            return List.of();
        }
        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 组装商品卡片分页 */
    private PageResult<ProductCardVO> cards(Long shopId, List<Long> categoryIds, String keyword,
                                           String sort, long pageNum, long pageSize) {
        long total = productMapper.countStorefrontList(shopId, categoryIds, keyword);
        if (total == 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Product> products = productMapper.selectStorefrontList(shopId, categoryIds, keyword,
                sort, (pageNum - 1) * pageSize, pageSize);
        if (products.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, List<String>> imageMap = assembler.imagesOf(ids);
        Map<Long, List<ProductSku>> skuMap = assembler.skusOf(ids);

        List<ProductCardVO> rows = new ArrayList<>(products.size());
        for (Product product : products) {
            rows.add(assembler.toCard(product,
                    imageMap.getOrDefault(product.getId(), List.of()),
                    skuMap.getOrDefault(product.getId(), List.of())));
        }
        return PageResult.of(rows, total, pageNum, pageSize);
    }

    /** 一级类目展开为其二级类目；二级类目原样返回 */
    private List<Long> expandCategory(Long shopId, Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !shopId.equals(category.getShopId())
                || (category.getStatus() != null && category.getStatus() != 0)) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (category.getParentId() == null || category.getParentId() == 0L) {
            List<Long> children = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                            .eq(Category::getShopId, shopId)
                            .eq(Category::getParentId, categoryId)
                            .eq(Category::getStatus, 0)
                            .orderByAsc(Category::getSort))
                    .stream().map(Category::getId).toList();
            return children;
        }
        return List.of(categoryId);
    }
}
