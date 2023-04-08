package cn.itcast.hotel.web;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParam;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    private IHotelService iHotelService;
    @PostMapping("list")
    public PageResult search(@RequestBody RequestParam param) throws IOException {
        return iHotelService.search(param);

    }
    @PostMapping("filters")
    public Map<String, List<String>> getFilters(@RequestBody RequestParam param) throws IOException {
        return iHotelService.filters(param);
    }
    @GetMapping("suggestion")
    public List<String> getSuggestions(@org.springframework.web.bind.annotation.RequestParam("key") String prefix) throws IOException {
        return iHotelService.getSuggestions(prefix);
    }


}
