package com.yigit.airflow_spring_rest_controller.service;

import com.yigit.airflow_spring_rest_controller.dto.dag.Dag;
import com.yigit.airflow_spring_rest_controller.dto.dag.DagCollection;
import com.yigit.airflow_spring_rest_controller.dto.dag.DagDetail;
import com.yigit.airflow_spring_rest_controller.dto.dag.DagUpdate;
import com.yigit.airflow_spring_rest_controller.dto.task.TaskCollection;
import com.yigit.airflow_spring_rest_controller.dto.task.TaskInstanceCollection;
import com.yigit.airflow_spring_rest_controller.dto.task.TaskInstanceStateUpdate;
import com.yigit.airflow_spring_rest_controller.entity.AuditLogOperation;
import com.yigit.airflow_spring_rest_controller.exception.AirflowResourceNotFoundException;
import com.yigit.airflow_spring_rest_controller.exception.AirflowConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DagService {

    private final WebClient airflowWebClient;
    private final AuditLogService auditLogService;
    private static final Logger log = LoggerFactory.getLogger(DagService.class);

    @Autowired
    public DagService(WebClient airflowWebClient, AuditLogService auditLogService) {
        this.airflowWebClient = airflowWebClient;
        this.auditLogService = auditLogService;
    }

    public Mono<DagCollection> getDags() {
        return airflowWebClient.get()
            .uri("/dags")
            .retrieve()
            .bodyToMono(DagCollection.class);
    }

    public Mono<Dag> getDag(String dagId) {
        return airflowWebClient.get()
            .uri("/dags/{dagId}", dagId)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .bodyToMono(Dag.class);
    }

    public Mono<Dag> updateDag(String dagId, DagUpdate dagUpdate) {
        String details = "Updated DAG: " + (dagUpdate.getIsPaused() != null ? 
            (dagUpdate.getIsPaused() ? "paused" : "unpaused") : "updated");
        
        AuditLogOperation operation = dagUpdate.getIsPaused() != null ? 
            (dagUpdate.getIsPaused() ? AuditLogOperation.PAUSE : AuditLogOperation.UNPAUSE) : 
            AuditLogOperation.UPDATE_STATE;
        
        return airflowWebClient.patch()
            .uri("/dags/{dagId}", dagId)
            .bodyValue(dagUpdate)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .onStatus(
                status -> status.value() == HttpStatus.CONFLICT.value(),
                response -> Mono.error(new AirflowConflictException("Conflict updating DAG: " + dagId))
            )
            .bodyToMono(Dag.class)
            .flatMap(dag -> 
                auditLogService.logOperation(dagId, operation, details)
                    .thenReturn(dag)
            );
    }

    public Mono<Void> deleteDag(String dagId) {
        String details = "Deleted DAG: " + dagId;
        
        return auditLogService.logOperation(dagId, AuditLogOperation.DELETE, details)
            .doOnSuccess(auditLog -> log.info("Audit log created for DELETE operation on DAG {}: ID={}", dagId, auditLog.getId()))
            .doOnError(error -> log.error("Failed to create audit log for DELETE operation on DAG {}: {}", dagId, error.getMessage()))
            .then(
                airflowWebClient.delete()
                    .uri("/dags/{dagId}", dagId)
                    .retrieve()
                    .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
                    )
                    .onStatus(
                        status -> status.value() == HttpStatus.CONFLICT.value(),
                        response -> Mono.error(new AirflowConflictException("Cannot delete DAG with running instances: " + dagId))
                    )
                    .bodyToMono(Void.class)
                    .doOnSuccess(v -> log.info("Successfully deleted DAG: {}", dagId))
                    .doOnError(error -> log.error("Failed to delete DAG {}: {}", dagId, error.getMessage()))
            );
    }

    public Mono<TaskCollection> getDagTasks(String dagId) {
        return airflowWebClient.get()
            .uri("/dags/{dagId}/tasks", dagId)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .bodyToMono(TaskCollection.class);
    }

    public Mono<DagDetail> getDagDetails(String dagId) {
        return airflowWebClient.get()
            .uri("/dags/{dagId}/details", dagId)
            .retrieve()
            .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> Mono.error(new AirflowResourceNotFoundException("DAG not found: " + dagId))
            )
            .bodyToMono(DagDetail.class);
    }
} 