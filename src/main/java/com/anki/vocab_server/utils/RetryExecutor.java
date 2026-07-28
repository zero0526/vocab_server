package com.anki.vocab_server.utils;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryExecutor {
    private int maxAttempt;
    private int initDelay;
    private double backoffMultiplier;
    private long maxDelay;
    private boolean useJitter;

    public static final RetryExecutor DEFAULT = RetryExecutor.builder()
            .maxAttempt(3)
            .initDelay(1000)
            .backoffMultiplier(2.0)
            .maxDelay(5)
            .useJitter(true)
            .build();

    public <T> T execute(Callable<T> action) throws Exception{
        long currDelay= initDelay;
        Exception lastException= null;
        for(int attempt= 1; attempt<=maxAttempt; attempt++){
            try{
                return action.call();
            }catch (Exception e){
                lastException= e;
                if(attempt==maxAttempt)throw e;
                long sleepTime= computeDelay(currDelay);
                try{
                    Thread.sleep(sleepTime);
                }catch (InterruptedException ie){
                    Thread.currentThread().interrupt();
                    throw  new RuntimeException("Retry interrupted", ie);
                }
                currDelay= (long)(currDelay*backoffMultiplier);
                currDelay= Math.min(currDelay, maxDelay);
            }
        }
        if(maxAttempt==0)return null;
        else throw lastException;
    }

    public void execute(Runnable action) throws Exception{
        execute(()->{
            action.run();
            return null;
        });
    }

    private long computeDelay(long baseDelay){
        if(useJitter){
            long jitter= ThreadLocalRandom.current().nextLong(baseDelay);
            return baseDelay + jitter;
        }
        return baseDelay;
    }
}
