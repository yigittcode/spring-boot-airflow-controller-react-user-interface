package com.yigit.airflow_spring_rest_controller.service;

import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRun;
import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRunCollection;
import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRunCreate;
import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRunStateUpdate;
import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRunClear;
import com.yigit.airflow_spring_rest_controller.dto.dagrun.DagRunNoteUpdate;
import com.yigit.airflow_spring_rest_controller.dto.dataset.DatasetEventCollection;
import com.yigit.airflow_spring_rest_controller.entity.AuditLogOperation;
import com.yigit.airflow_spring_rest_controller.exception.AirflowResourceNotFoundException;
import com.yigit.airflow_spring_rest_controller.exception.AirflowConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;

@Service
public class DagRunService {

    private final WebClient airflowWebClient;
    private final AuditLogService auditLogService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DagRunService.class);

    @Autowired
    public DagRunService(WebClient airflowWebClient, AuditLogService auditLogService) {
        this.airflowWebClient = airflowWebClient;
        this.auditLogService = auditLogService;
    }

    public Mono<DagRunCollection> getDagRuns(String dagId, Map<String, String> queryParams) {
        return airflowWebClient.get()
            .uri(uriBuilder -> {
                uriBuilder = uriBuilder.path("/dags/{dagId}/dagRuns");
                
                // Add query parameters if they exist
                if (queryParams != null) {
                    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                            uriBuilder = uriBuilder.queryParam(entry.getKey(), entry.getValue());
                        }
                    }
                }
                
                return uriBuilder.build(dagId);
            })
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .bodyToMono(DagRunCollection.class);
    }
    
    // For backward compatibility
    public Mono<DagRunCollection> getDagRuns(String dagId) {
        return getDagRuns(dagId, null);
    }

    public Mono<DagRun> createDagRun(String dagId, DagRunCreate dagRunCreate) {
        String details = "Triggered DAG run" + 
            (dagRunCreate.getLogicalDate() != null ? " for date: " + dagRunCreate.getLogicalDate() : "");
        
        return airflowWebClient.post()
            .uri("/dags/{dagId}/dagRuns", dagId)
            .bodyValue(dagRunCreate)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .onStatus(
                status -> status.value() == HttpStatus.CONFLICT.value(),
                response -> Mono.error(new AirflowConflictException("DAG Run already exists or conflict with execution date"))
            )
            .bodyToMono(DagRun.class)
            .flatMap(dagRun -> 
                auditLogService.logOperation(dagId, dagRun.getDagRunId(), AuditLogOperation.TRIGGER, details)
                    .thenReturn(dagRun)
            );
    }

    public Mono<DagRun> getDagRun(String dagId, String dagRunId) {
        return airflowWebClient.get()
            .uri("/dags/{dagId}/dagRuns/{dagRunId}", dagId, dagRunId)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException(
                    String.format("DAG Run not found: dagId=%s, dagRunId=%s", dagId, dagRunId)
                ))
            )
            .bodyToMono(DagRun.class);
    }

    public Mono<Void> deleteDagRun(String dagId, String dagRunId) {
        String details = "Deleted DAG run: " + dagRunId + " for DAG: " + dagId;
        
        // First create audit log before deleting the DAG Run
        return auditLogService.logOperation(dagId, dagRunId, AuditLogOperation.DELETE, details)
            .doOnSuccess(auditLog -> log.info("Audit log created for DELETE operation on DAG run {}: ID={}", dagRunId, auditLog.getId()))
            .doOnError(error -> log.error("Failed to create audit log for DELETE operation on DAG run {}: {}", dagRunId, error.getMessage()))
            .then(
                // Then delete the DAG Run
                airflowWebClient.delete()
                    .uri("/dags/{dagId}/dagRuns/{dagRunId}", dagId, dagRunId)
                    .retrieve()
                    .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new AirflowResourceNotFoundException("DAG run not found: " + dagRunId))
                    )
                    .bodyToMono(Void.class)
                    .doOnSuccess(v -> log.info("Successfully deleted DAG run: {} for DAG: {}", dagRunId, dagId))
                    .doOnError(error -> log.error("Failed to delete DAG run {}: {}", dagRunId, error.getMessage()))
            );
    }

    public Mono<DagRun> updateDagRunState(String dagId, String dagRunId, DagRunStateUpdate stateUpdate) {
        String details = "Updated state to: " + stateUpdate.getState();
        
        return airflowWebClient.patch()
            .uri("/dags/{dagId}/dagRuns/{dagRunId}", dagId, dagRunId)
            .bodyValue(stateUpdate)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG run not found: " + dagRunId))
            )
            .bodyToMono(DagRun.class)
            .flatMap(dagRun -> 
                auditLogService.logOperation(dagId, dagRunId, AuditLogOperation.UPDATE_STATE, details)
                    .thenReturn(dagRun)
            );
    }

    public Mono<DagRun> clearDagRun(String dagId, String dagRunId, DagRunClear clearRequest) {
        String details = "Cleared DAG run";
        
        return airflowWebClient.post()
            .uri("/dags/{dagId}/dagRuns/{dagRunId}/clear", dagId, dagRunId)
            .bodyValue(clearRequest)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG run not found: " + dagRunId))
            )
            .bodyToMono(DagRun.class)
            .flatMap(dagRun -> 
                auditLogService.logOperation(dagId, dagRunId, AuditLogOperation.CLEAR, details)
                    .thenReturn(dagRun)
            );
    }

    public Mono<DatasetEventCollection> getUpstreamDatasetEvents(String dagId, String dagRunId) {
        return airflowWebClient.get()
            .uri("/dags/{dagId}/dagRuns/{dagRunId}/upstreamDatasetEvents", dagId, dagRunId)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException(
                    String.format("DAG Run not found: dagId=%s, dagRunId=%s", dagId, dagRunId)
                ))
            )
            .bodyToMono(DatasetEventCollection.class);
    }

    public Mono<DagRun> setDagRunNote(String dagId, String dagRunId, DagRunNoteUpdate noteUpdate) {
        return airflowWebClient.patch()
            .uri("/dags/{dagId}/dagRuns/{dagRunId}/setNote", dagId, dagRunId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteUpdate)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException(
                    String.format("DAG Run not found: dagId=%s, dagRunId=%s", dagId, dagRunId)
                ))
            )
            .bodyToMono(DagRun.class);
    }
} 