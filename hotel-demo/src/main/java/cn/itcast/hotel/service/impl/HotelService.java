package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParam;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Resource
    RestHighLevelClient client;
    @Override
    public PageResult search(RequestParam param){
        try {

            //准备request
            SearchRequest request = new SearchRequest("hotel");
            //准备DSL
            //query
            //构建BooleanQuery
            BoolQueryBuilder boolQuery=QueryBuilders.boolQuery();
            //关键字搜素
            if (param.getKey() == null || "".equals(param.getKey())) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            } else {
                boolQuery.must(QueryBuilders.matchQuery("all", param.getKey()));
            }
           //条件过滤
            //1.城市
            if (param.getCity()!=null && !param.getCity().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("city",param.getCity()));
            }
            if (param.getBrand()!=null && !param.getBrand().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("brand",param.getBrand()));
            }
            if (param.getStarName()!=null && !param.getStarName().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("starName",param.getStarName()));
            }
            if (param.getMinPrice()!=null && !param.getMinPrice().equals("")) {
                boolQuery.filter(QueryBuilders.rangeQuery("price").gte(param.getMinPrice()).lte(param.getMaxPrice()));
            }
            request.source().query(boolQuery);
            //算分控制
            FunctionScoreQueryBuilder functionScoreQueryBuilder =
                    QueryBuilders.functionScoreQuery(
                            //原始查询，相关性算法的查询
                            boolQuery,
                            //fucntion score的数组
                            new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                    //一个fucntion score元素
                                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                            //过滤条件
                                            QueryBuilders.termQuery("isAd",true),
                                            //算分函数
                                            ScoreFunctionBuilders.weightFactorFunction(10)
                                    )
                            });
            //排序
            String location=param.getLocation();
            if(location!=null && !location.equals(""))
            {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location",new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                );
            }

            //2. 分页
            request.source().from((param.getPage()-1)*param.getSize()).size(param.getSize());
            //发送请求，得到响应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //解析响应
            return Handerhithlight(response);
        }catch (IOException e)
        {
            throw new RuntimeException();
        }
    }

    @Override
    public Map<String, List<String>> filters(RequestParam param){
        try {
        //获取request
        SearchRequest searchRequest = new SearchRequest("hotel");
            BoolQueryBuilder boolQuery=QueryBuilders.boolQuery();
            //关键字搜素
            if (param.getKey() == null || "".equals(param.getKey())) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            } else {
                boolQuery.must(QueryBuilders.matchQuery("all", param.getKey()));
            }
            //条件过滤
            //1.城市
            if (param.getCity()!=null && !param.getCity().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("city",param.getCity()));
            }
            if (param.getBrand()!=null && !param.getBrand().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("brand",param.getBrand()));
            }
            if (param.getStarName()!=null && !param.getStarName().equals("")) {
                boolQuery.filter(QueryBuilders.termQuery("starName",param.getStarName()));
            }
            if (param.getMinPrice()!=null && !param.getMinPrice().equals("")) {
                boolQuery.filter(QueryBuilders.rangeQuery("price").gte(param.getMinPrice()).lte(param.getMaxPrice()));
            }
            searchRequest.source().query(boolQuery);
        //编写DSL
        searchRequest.source().size(0);
        filterContructor(searchRequest);
        //发请求
        SearchResponse response= null;
        response = client.search(searchRequest, RequestOptions.DEFAULT);
        //解析结果
        Map<String,List<String>> result=new HashMap<>();
        Aggregations aggregations=response.getAggregations();
        List<String> List1=getAggByname(aggregations,"brandAgg");
        List<String> List2=getAggByname(aggregations,"cityAgg");
        List<String> List3=getAggByname(aggregations,"starAgg");
        result.put("品牌",List1);
        result.put("城市",List2);
        result.put("星级",List3);
            System.out.println(result);
        return result;
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuggestions(String prefix) throws IOException {
        // 1.准备request
        SearchRequest request=new SearchRequest("hotel");
        // 2. 准备DSL
        request.source().suggest(new SuggestBuilder()
                .addSuggestion(
                        "suggestions",
                        SuggestBuilders.completionSuggestion("suggestion").prefix(prefix).skipDuplicates(true).size(10)
                )
        );
        // 3. 发起请求
        SearchResponse res = client.search(request, RequestOptions.DEFAULT);
        //4. 解析结果
        Suggest suggest=res.getSuggest();
        CompletionSuggestion suggestion=suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options1 = suggestion.getOptions();
        List<String> result=new ArrayList<>(options1.size());
        for (Suggest.Suggestion.Entry.Option options : options1) {
            String text = options.getText().string();
            result.add(text);
        }
        return result;
    }

    @Override
    public void insertByid(Long id) {
        //根据ID先查询酒店数据
        Hotel hotel = getById(id);
        HotelDoc hotelDoc=new HotelDoc(hotel);
        //准备request
        IndexRequest request=new IndexRequest("hotel").id(hotel.getId().toString());
        //准备DSL
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        //发送请求
        try {
            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByid(Long id) {
        //获取request
        DeleteRequest request=new DeleteRequest("hotel", String.valueOf(id));
        //准备发送请求
        try {
            client.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getAggByname(Aggregations aggregations,String name) {
        List<String> cons=new ArrayList<>();
        Terms terms=aggregations.get(name);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String keyAsString = bucket.getKeyAsString();
            cons.add(keyAsString);
        }
        return  cons;
    }

    private void filterContructor(SearchRequest searchRequest) {
        searchRequest.source().size(0);
        searchRequest.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        searchRequest.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        searchRequest.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName")
                .size(100)
        );
    }

    PageResult Handerhithlight(SearchResponse response){
        //解析结果
        SearchHits searchHits= response.getHits();
        //1.查询总条数
        long total=searchHits.getTotalHits().value;
        System.out.println("共搜索："+total);
        //1.2 查询结果数组
        SearchHit[] hits=searchHits.getHits();
        ArrayList<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            //得到source
            String  json=hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //获得排序值
            Object[] sortValues = hit.getSortValues();
            for (Object sortValue : sortValues) {
                Object sortValue1 = sortValues[0];
                hotelDoc.setDistance(sortValue1);
            }

            hotels.add(hotelDoc);
        }
        return new PageResult(total,hotels);
    }
}
