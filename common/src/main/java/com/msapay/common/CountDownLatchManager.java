package com.msapay.common;

import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Configuration
@Slf4j
public class CountDownLatchManager {
    
    private final Map<String, CountDownLatch> countDownLatchMap;
    private final Map<String, String> stringMap;

    public CountDownLatchManager() {
        this.countDownLatchMap = new HashMap<>();
        this.stringMap = new HashMap<>();
    }

    public void addCountDownLatch(String key) {
        log.debug("Adding CountDownLatch for key: {}", key);
        this.countDownLatchMap.put(key, new CountDownLatch(1));
    }

    public void setDataForKey(String key, String data){
        log.debug("Setting data for key: {} = {}", key, data);
        this.stringMap.put(key, data);
    }
    
    public String getDataForKey(String key){
        String data = this.stringMap.get(key);
        log.debug("Getting data for key: {} = {}", key, data);
        return data;
    }
    
    public CountDownLatch getCountDownLatch(String key) {
        CountDownLatch latch = this.countDownLatchMap.get(key);
        log.debug("Getting CountDownLatch for key: {} = {}", key, latch != null ? "found" : "null");
        return latch;
    }
    
    public void removeData(String key) {
        log.debug("Removing data for key: {}", key);
        this.countDownLatchMap.remove(key);
        this.stringMap.remove(key);
    }
    
    public boolean hasKey(String key) {
        return this.countDownLatchMap.containsKey(key);
    }
}
