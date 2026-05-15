package com.weekendbasket.app.scheduler;

import com.weekendbasket.app.service.WeeklyCycleService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyOrderScheduler {

    private static final Logger log = LogManager.getLogger(WeeklyOrderScheduler.class);

    private final WeeklyCycleService cycleService;

    @Scheduled(cron = "${scheduler.cycle.open.cron}")
    @Async("appTaskExecutor")
    public void openWeeklyCycle() {
        log.info("Scheduler: opening weekly cycle");
        cycleService.scheduledOpen();
    }

    @Scheduled(cron = "${scheduler.cycle.close.cron}")
    @Async("appTaskExecutor")
    public void closeWeeklyCycle() {
        log.info("Scheduler: closing weekly cycle");
        cycleService.scheduledClose();
    }
}
