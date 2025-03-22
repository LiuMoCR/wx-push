package com.asleepyfish.strategy.template;

import cn.hutool.core.date.DateTime;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.asleepyfish.common.WxConstants;
import com.asleepyfish.common.WxTemplateConstants;
import com.asleepyfish.dto.IdentityInfo;
import com.asleepyfish.strategy.WxTemplateStrategy;
import com.asleepyfish.util.WxOpUtils;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @Author: asleepyfish
 * @Date: 2022/9/13 19:31
 * @Description: 特殊早安推送策略
 */
@Service(WxTemplateConstants.SPECIAL_MORNING)
public class SpecialMorningStrategy implements WxTemplateStrategy {

    @Override
    public void execute(WxMpTemplateMessage wxMpTemplateMessage, IdentityInfo identityInfo) {
        Integer districtCode = WxOpUtils.getDistrictCode(identityInfo);
        // 获取天气的url
        String weatherUrl = "https://api.map.baidu.com/weather/v1/?district_id=" + districtCode + "&data_type=all&ak=" + WxConstants.BAI_DU_AK;
        // 天气信息json格式
        String weatherStr = HttpUtil.get(weatherUrl);
        JSONObject result = JSONObject.parseObject(JSONObject.parseObject(weatherStr).get("result").toString());
        // 实时天气
        JSONObject now = JSONObject.parseObject(result.get("now").toString());
        // 今日天气
        JSONObject today = JSONArray.parseArray(result.get("forecasts").toString()).getJSONObject(0);
        // 明日天气
        JSONObject tomorrow = JSONArray.parseArray(result.get("forecasts").toString()).getJSONObject(1);
        // 每日英语
        String dailyEnglishUrl = "http://api.tianapi.com/everyday/index?key=" + WxConstants.TX_AK;
        String dailyEnglishStr = HttpUtil.get(dailyEnglishUrl);
        JSONObject dailyEnglishObject = JSONArray.parseArray(JSONObject.parseObject(dailyEnglishStr).get("newslist").toString()).getJSONObject(0);
        // 英文句子
        String english = dailyEnglishObject.get("content").toString();
        // 20230505更新，wx平台最新规范[https://developers.weixin.qq.com/community/develop/doc/000a2ae286cdc0f41a8face4c51801]
        // 每个模板块最多只能填充20个字符，需要对超长内容切割
        String english1 = english.substring(0, Math.min(english.length(), 20));
        String english2 = null;
        if (english.length() > 20) {
            english2 = english.substring(20);
        }
        // 中文翻译
        String chinese = dailyEnglishObject.get("note").toString();
        String chinese1 = chinese.substring(0, Math.min(chinese.length(), 20));
        String chinese2 = null;
        if (chinese.length() > 20) {
            chinese2 = chinese.substring(20);
        }
        wxMpTemplateMessage.addData(new WxMpTemplateData("location", identityInfo.getAddress(), "#9370DB"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("now_temp", now.get("temp").toString(), "#87CEFA"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("now_weather", now.get("text").toString(), "#87CEEB"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("now_wind_dir", now.get("wind_dir").toString(), "#708090"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("now_wind_class", now.get("wind_class").toString(), "#708090"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("now_rh", now.get("rh").toString(), "#778899"));
        String todayWeatherDay = today.get("text_day").toString();
        String todayWeatherNight = today.get("text_night").toString();
        if (todayWeatherDay.equals(todayWeatherNight)) {
            wxMpTemplateMessage.addData(new WxMpTemplateData("today_weather", todayWeatherDay, "#FFC1C1"));
        } else {
            wxMpTemplateMessage.addData(new WxMpTemplateData("today_weather", todayWeatherDay + "转" + todayWeatherNight, "#FFC1C1"));
        }
        wxMpTemplateMessage.addData(new WxMpTemplateData("today_high", today.get("high").toString(), "#CD9B9B"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("today_low", today.get("low").toString(), "#CD9B9B"));
        String tomorrowWeatherDay = tomorrow.get("text_day").toString();
        String tomorrowWeatherNight = tomorrow.get("text_night").toString();
        if (tomorrowWeatherDay.equals(tomorrowWeatherNight)) {
            wxMpTemplateMessage.addData(new WxMpTemplateData("tomorrow_weather", tomorrowWeatherDay, "#DDA0DD"));
        } else {
            wxMpTemplateMessage.addData(new WxMpTemplateData("tomorrow_weather", tomorrowWeatherDay + "转" + tomorrowWeatherNight, "#DDA0DD"));
        }
        wxMpTemplateMessage.addData(new WxMpTemplateData("tomorrow_high", tomorrow.get("high").toString(), "#EE82EE"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("tomorrow_low", tomorrow.get("low").toString(), "#EE82EE"));
        // 相识天数，可以修改为恋爱天数，或者其他纪念意义天数

        Long meetDays = WxOpUtils.countDays(WxConstants.MEET_DATE, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        wxMpTemplateMessage.addData(new WxMpTemplateData("meet_days", String.valueOf(meetDays), "#C000C0"));
        Long nearWeekendDays = WxOpUtils.countDays(new SimpleDateFormat("yyyy-MM-dd").format(new Date()),getNearSunday());
        wxMpTemplateMessage.addData(new WxMpTemplateData("near_weekend_days", String.valueOf(nearWeekendDays), "#C000C0"));
        Map<String,String> holidayDetail = getNearHoliday();
        if(null != holidayDetail.get("holiday") && null != holidayDetail.get("holiday_name")) {
            Long nearHolidayDays = WxOpUtils.countDays(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), holidayDetail.get("holiday"));
            wxMpTemplateMessage.addData(new WxMpTemplateData("near_holiday_days", String.valueOf(nearHolidayDays), "#C000C0"));
            wxMpTemplateMessage.addData(new WxMpTemplateData("near_holiday_name", String.valueOf(holidayDetail.get("holiday_name")), "#C000C0"));
        }else {
            wxMpTemplateMessage.addData(new WxMpTemplateData("near_holiday_days", "一些天", "#C000C0"));
            wxMpTemplateMessage.addData(new WxMpTemplateData("near_holiday_name", "明年", "#C000C0"));
        }
        wxMpTemplateMessage.addData(new WxMpTemplateData("daily_english_en1", english1, "#FFCCFF"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("daily_english_en2", english2, "#FFCCFF"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("daily_english_cn1", chinese1, "#CCCCFF"));
        wxMpTemplateMessage.addData(new WxMpTemplateData("daily_english_cn2", chinese2, "#CCCCFF"));
    }


    public static String getNearSunday(){
        LocalDate today = LocalDate.now();
        LocalDate nextSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return nextSunday.toString();
    }

    public static Map<String,String> getNearHoliday(){
        Map<String,String> result = new HashMap<>();
        String today = LocalDate.now().toString();
//        System.out.println(today);
        String data = HttpUtil.get("https://timor.tech/api/holiday/year/2025?type=Y&week=Y");
        JSONObject jsonObject = JSONObject.parseObject(data).getJSONObject("type");
        Set<String> keySet = jsonObject.keySet();
        for (String key : keySet) {
            JSONObject jsonObjectData = jsonObject.getJSONObject(key);
            //类型2是节假日，1是周末
            if (jsonObjectData.getString("type").equals("2")){
                // 解析字符串为 LocalDate
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate todayTime = LocalDate.parse(today, formatter);
                LocalDate holidayTime = LocalDate.parse(key, formatter);
                if (!holidayTime.isBefore(todayTime)){
                    result.put("holiday",key);
                    result.put("holiday_name",jsonObjectData.getString("name"));
                    return result;
                }
                //打印了所有该年度的节假日时间的插入语句
//                System.out.println("INSERT INTO `sys_holiday` (`date`, `name`, `type`, `week`) VALUES ('"+key+" 00:00:00', '"+jsonObjectData.getString("name")+"', '"+jsonObjectData.getString("type")+"', '"+jsonObjectData.getString("week")+"');");
            }
        }
        return result;
    }

    public static void main(String[] args) {
//        System.out.println(getNearHoliday());
    }
}
