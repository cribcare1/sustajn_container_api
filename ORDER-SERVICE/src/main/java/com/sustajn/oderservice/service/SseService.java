package com.sustajn.oderservice.service;

import com.sustajn.oderservice.dto.AdminDashboardResponse;
import com.sustajn.oderservice.dto.ContainerChartResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    private final OrderService orderService;

    public SseService(OrderService orderService) {
        this.orderService = orderService;
    }

    public SseEmitter subscribe(Long restaurantId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes
        emitters.computeIfAbsent(restaurantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(restaurantId, emitter));
        emitter.onTimeout(() -> removeEmitter(restaurantId, emitter));
        emitter.onError((ex) -> removeEmitter(restaurantId, emitter));

        return emitter;
    }

    private void removeEmitter(Long restaurantId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(restaurantId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    public Set<Long> getRegisteredRestaurantIds() {
        return emitters.keySet();
    }

    public void sendUpdate(Long restaurantId, ContainerChartResponse stats) {
        List<SseEmitter> list = emitters.get(restaurantId);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chart-update")
                        .data(stats, MediaType.APPLICATION_JSON));
            } catch (IOException ex) {
                log.warn("Removing emitter due to send error for restaurant {}: {}", restaurantId, ex.getMessage());
                removeEmitter(restaurantId, emitter);
            }
        }
    }

    /**
     * Broadcast updated chart stats to all subscribed restaurants.
     * This will fetch latest stats from OrderService for each registered restaurant id.
     */
    public void notifyAllClients() {
        for (Long restaurantId : getRegisteredRestaurantIds()) {
            try {
                ContainerChartResponse stats = orderService.getChartStatistics(restaurantId, null, null, null, null);
                sendUpdate(restaurantId, stats);
            } catch (Exception ex) {
                log.warn("Failed to fetch/send chart stats for restaurant in notifyClients {}: {}", restaurantId, ex.getMessage());
            }
        }
    }

    public SseEmitter subscribeAdmin() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes
        adminEmitters.add(emitter);

        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError((ex) -> adminEmitters.remove(emitter));

        // Send initial data snapshot immediately upon connection
        try {
            AdminDashboardResponse stats = orderService.getAdminDashboardMetrics();
            emitter.send(SseEmitter.event()
                    .name("dashboard-metrics")
                    .data(stats, MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            log.warn("Error sending initial admin dashboard metrics: {}", ex.getMessage());
        }

        return emitter;
    }

    public void notifyAdminClients() {
        if (adminEmitters.isEmpty()) return;

        try {
            AdminDashboardResponse stats = orderService.getAdminDashboardMetrics();
            for (SseEmitter emitter : adminEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("dashboard-metrics")
                            .data(stats, MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    log.warn("Removing admin emitter due to send error: {}", ex.getMessage());
                    adminEmitters.remove(emitter);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch/send admin dashboard metrics: {}", ex.getMessage());
        }
    }

    // 🟢 3. Convenient single method to push updates to ALL subscribers (Restaurant + Admin)
    public void notifyAllDashboards() {
        notifyAllClients();     // Updates Restaurant Charts
        notifyAdminClients();   // Updates Admin Live Metrics
    }
}
