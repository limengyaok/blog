package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.queryWithMutex(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop==null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

//    public Shop queryWithMutex(Long id) {
//        String json = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
//        if(StrUtil.isNotEmpty(json)){
//            Shop shop = JSONUtil.toBean(json, Shop.class);
//            return shop;
//        }
//        if(json != null){
//            return null;
//        }
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        try {
//            if(!isLock){
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            Shop shop = getById(id);
//            if(shop == null){
//                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            String jsonStr = JSONUtil.toJsonStr(shop);
//            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,jsonStr,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//            return shop;
//        }catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            unlock(lockKey);
//        }
//    }

    private boolean tryLock(String key){
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return isLock;
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("商铺Id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryByType(Integer typeId, Integer current, Double x, Double y) {
        if(x == null || y == null){
            Page<Shop> page = query()
                    .eq("type_id",typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        try {
            SearchRequest request = new SearchRequest("shop");

            // 创建 SearchSourceBuilder
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (typeId != null) {
                boolQuery.filter(QueryBuilders.termQuery("type_id", typeId));
            }

            // 距离排序
            GeoDistanceSortBuilder distanceSort = SortBuilders
                    .geoDistanceSort("location", y, x)
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.KILOMETERS);

            // 使用 sourceBuilder 设置所有参数
            sourceBuilder.from((current - 1) * SystemConstants.DEFAULT_PAGE_SIZE)
                    .size(SystemConstants.DEFAULT_PAGE_SIZE)
                    .query(boolQuery)
                    .sort(distanceSort);

            // 将 sourceBuilder 设置到 request 中
            request.source(sourceBuilder);

            SearchResponse search = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHits searchHits = search.getHits();
            SearchHit[] hits = searchHits.getHits();
            List<Shop> shops = new ArrayList<>();
            for (SearchHit hit : hits) {
                String json = hit.getSourceAsString();
                Shop shop = JSONUtil.toBean(json, Shop.class);
                Object[] sortValues = hit.getSortValues();
                if(sortValues.length > 0){
                    Object sortValue = sortValues[0];
                    Double distance = Double.parseDouble(String.valueOf(sortValue));
                    shop.setDistance(distance);
                }
                shops.add(shop);
            }


            return Result.ok(shops);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result searchShop(String keyword) {
        return null;
    }
}
