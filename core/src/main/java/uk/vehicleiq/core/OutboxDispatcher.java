package uk.vehicleiq.core;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxDispatcher {
    private final OutboxEventRepository events; private final AnalysisService analyses; private final StringRedisTemplate redis;
    @Value("${vehicleiq.event-mode:local}") private String mode;
    OutboxDispatcher(OutboxEventRepository events,AnalysisService analyses,StringRedisTemplate redis){this.events=events;this.analyses=analyses;this.redis=redis;}
    @Scheduled(fixedDelay=1200)
    @Transactional
    void dispatch(){
        for(OutboxEventEntity event:events.findTop20ByStatusOrderByOccurredAtAsc("PENDING")){
            try {
                if("redis".equalsIgnoreCase(mode)){
                    redis.opsForStream().add(MapRecord.create("vehicleiq:events",Map.of("eventId",event.getId(),"type",event.getType(),"aggregateId",event.getAggregateId(),"payload",event.getPayload())));
                    event.setStatus("PUBLISHED");events.save(event);
                } else {
                    if("analysis.requested.v1".equals(event.getType())) analyses.complete(event.getAggregateId());
                    event.setStatus("PROCESSED");events.save(event);
                }
            } catch(Exception ignored) { /* leave pending; the next scheduled pass retries it */ }
        }
    }
}

