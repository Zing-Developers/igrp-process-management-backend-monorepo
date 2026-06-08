package cv.igrp.framework.process.runtime.activiti.engine.process;

import cv.igrp.framework.process.runtime.core.engine.process.ProcessManagerAdapter;
import cv.igrp.framework.process.runtime.core.engine.process.model.*;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.RuntimeServiceImpl;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TimerJobEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.*;

@Component
public class ActivitiProcessManagerAdapter implements ProcessManagerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiProcessManagerAdapter.class);

    private final ProcessRuntime processRuntime;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ManagementService managementService;

	public ActivitiProcessManagerAdapter(ProcessRuntime processRuntime,
										 RuntimeService runtimeService,
										 HistoryService historyService,
										 ManagementService managementService
	) {
        this.processRuntime = processRuntime;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
		this.managementService = managementService;
	}

    @Override
    public ProcessInstance createProcess(String processDefinitionId, String businessKey) {
        Objects.requireNonNull(processDefinitionId, "processDefinitionId cannot be null");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccessDeniedException("No authenticated user found");

        LOGGER.debug("Process created by {}", authentication.getName());

        LOGGER.info("Creating process with definition id: {}, business key: {}", processDefinitionId, businessKey);

		org.activiti.engine.impl.identity.Authentication
				.setAuthenticatedUserId(authentication.getName());

		var payload = ProcessPayloadBuilder
                .create()
                .withProcessDefinitionId(processDefinitionId)
                .withBusinessKey(businessKey)
                .build();

        LOGGER.debug("Process create payload built successfully: {}", payload);

        LOGGER.info("Authenticated User create: {}", Authentication.getAuthenticatedUserId());

        var activitiProcessInstance = processRuntime.create(payload);

        var igrpProcessInstance = new ProcessInstance(
                activitiProcessInstance.getId(),
                activitiProcessInstance.getName(),
                activitiProcessInstance.getStartDate(),
                activitiProcessInstance.getCompletedDate(),
                activitiProcessInstance.getInitiator(),
                activitiProcessInstance.getProcessDefinitionId(),
                activitiProcessInstance.getProcessDefinitionKey(),
                activitiProcessInstance.getBusinessKey(),
                activitiProcessInstance.getParentId(),
                activitiProcessInstance.getProcessDefinitionVersion(),
                activitiProcessInstance.getProcessDefinitionName(),
                IGRPProcessStatus.valueOf(activitiProcessInstance.getStatus().name())
        );

        LOGGER.debug("Process created by user: {}", activitiProcessInstance.getInitiator());
        LOGGER.debug("Process instance created. Details: {}", activitiProcessInstance);

        return igrpProcessInstance;
    }

    @Override
    public ProcessInstance startCreatedProcess(String processInstanceId, String processDefinitionId, String businessKey, Map<String, Object> variables) {

        Objects.requireNonNull(processInstanceId, "processInstanceId cannot be null");
        Objects.requireNonNull(processDefinitionId, "processDefinitionId cannot be null");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccessDeniedException("No authenticated user found");

        LOGGER.debug("Process started by {}", authentication.getName());

        LOGGER.info("Starting created process with definition id: {}, business key: {}", processDefinitionId, businessKey);

        LOGGER.debug("Process variables prepared, count: {}", variables.size());

		org.activiti.engine.impl.identity.Authentication
				.setAuthenticatedUserId(authentication.getName());

        var payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionId(processDefinitionId)
                .withBusinessKey(businessKey)
                .withVariables(variables)
                .withVariable("startedBy", authentication.getName())
                .build();

        LOGGER.debug("Process start created payload built successfully: {}", payload);

        var activitiProcessInstance = processRuntime.startCreatedProcess(processInstanceId, payload);

        LOGGER.info("Authenticated User start: {}", Authentication.getAuthenticatedUserId());

        var igrpProcessInstance = new ProcessInstance(
                activitiProcessInstance.getId(),
                activitiProcessInstance.getName(),
                activitiProcessInstance.getStartDate(),
                activitiProcessInstance.getCompletedDate(),
                activitiProcessInstance.getInitiator(),
                activitiProcessInstance.getProcessDefinitionId(),
                activitiProcessInstance.getProcessDefinitionKey(),
                activitiProcessInstance.getBusinessKey(),
                activitiProcessInstance.getParentId(),
                activitiProcessInstance.getProcessDefinitionVersion(),
                activitiProcessInstance.getProcessDefinitionName(),
                IGRPProcessStatus.valueOf(activitiProcessInstance.getStatus().name())
        );

        LOGGER.debug("Process instance started. Details: {}", activitiProcessInstance);

        return igrpProcessInstance;
    }

    @Override
    @Deprecated
    public ProcessInstance startProcess(String processDefinitionId, String businessKey, Map<String, Object> variables) {

        Objects.requireNonNull(processDefinitionId, "processDefinitionId cannot be null");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccessDeniedException("No authenticated user found");

        LOGGER.debug("Process started by {}", authentication.getName());

        LOGGER.info("Starting process with definition id: {}, business key: {}", processDefinitionId, businessKey);

        LOGGER.debug("Process variables prepared, count: {}", variables.size());

        var payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionId(processDefinitionId)
                .withBusinessKey(businessKey)
                .withVariables(variables)
                .withVariable("startedBy", authentication.getName())
                .build();

        LOGGER.debug("Process start payload built successfully: {}", payload);

        var activitiProcessInstance = processRuntime.start(payload);

        var igrpProcessInstance = new ProcessInstance(
                activitiProcessInstance.getId(),
                activitiProcessInstance.getName(),
                activitiProcessInstance.getStartDate(),
                activitiProcessInstance.getCompletedDate(),
                activitiProcessInstance.getInitiator(),
                activitiProcessInstance.getProcessDefinitionId(),
                activitiProcessInstance.getProcessDefinitionKey(),
                activitiProcessInstance.getBusinessKey(),
                activitiProcessInstance.getParentId(),
                activitiProcessInstance.getProcessDefinitionVersion(),
                activitiProcessInstance.getProcessDefinitionName(),
                IGRPProcessStatus.valueOf(activitiProcessInstance.getStatus().name())
        );

        LOGGER.debug("Process instance details: {}", activitiProcessInstance);

        return igrpProcessInstance;
    }

    @Override
    public void suspendProcess(String processInstanceId) {

        LOGGER.info("Suspending process instance with id: {}", processInstanceId);

        var payload = ProcessPayloadBuilder
                .suspend()
                .withProcessInstanceId(processInstanceId)
                .build();

        processRuntime.suspend(payload);

        LOGGER.info("Process instance with id: {} suspended successfully", processInstanceId);
    }

    @Override
    public void resumeProcess(String processInstanceId) {

        LOGGER.info("Resuming process instance with id: {}", processInstanceId);

        var payload = ProcessPayloadBuilder
                .resume()
                .withProcessInstanceId(processInstanceId)
                .build();

        processRuntime.resume(payload);

        LOGGER.info("Process instance with id: {} resumed successfully", processInstanceId);
    }

    @Override
    public void terminateProcess(String processInstanceId, String deleteReason) {

        LOGGER.info("Terminating process instance with id: {}, reason: {}", processInstanceId, deleteReason);

        var payload = ProcessPayloadBuilder
                .delete()
                .withProcessInstanceId(processInstanceId)
                .withReason(deleteReason)
                .build();

        processRuntime.delete(payload);

        LOGGER.info("Process instance with id: {} terminated successfully", processInstanceId);
    }


    @Override
	public Optional<ProcessInstance> getProcessInstance(String processInstanceId) {
		LOGGER.info("Retrieving process instance with id: {}", processInstanceId);

		try {

			var runtimeInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(processInstanceId)
					.singleResult();

			if (runtimeInstance != null) {
				IGRPProcessStatus status = IGRPProcessStatus.RUNNING;
				if (runtimeInstance.isSuspended()) {
					status = IGRPProcessStatus.SUSPENDED;
				}

				var processInstance = new ProcessInstance(
						runtimeInstance.getId(),
						runtimeInstance.getName(),
						runtimeInstance.getStartTime(),
						null, // still running, so no endDate
						runtimeInstance.getStartUserId(),
						runtimeInstance.getProcessDefinitionId(),
						runtimeInstance.getProcessDefinitionKey(),
						runtimeInstance.getBusinessKey(),
						runtimeInstance.getParentId(),
						runtimeInstance.getProcessDefinitionVersion(),
						runtimeInstance.getProcessDefinitionName(),
						status
				);

				LOGGER.debug("Active process instance retrieved: {}", processInstance);
				return Optional.of(processInstance);
			}

			var historicInstance = historyService.createHistoricProcessInstanceQuery()
					.processInstanceId(processInstanceId)
					.singleResult();

			if (historicInstance != null) {
				IGRPProcessStatus status = historicInstance.getEndTime() != null
						? IGRPProcessStatus.COMPLETED
						: IGRPProcessStatus.SUSPENDED;

				var processInstance = new ProcessInstance(
						historicInstance.getId(),
						historicInstance.getName(),
						historicInstance.getStartTime(),
						historicInstance.getEndTime(),
						historicInstance.getStartUserId(),
						historicInstance.getProcessDefinitionId(),
						historicInstance.getProcessDefinitionKey(),
						historicInstance.getBusinessKey(),
						historicInstance.getSuperProcessInstanceId(),
						historicInstance.getProcessDefinitionVersion(),
						historicInstance.getProcessDefinitionName(),
						status
				);

				LOGGER.debug("Historic process instance retrieved: {}", processInstance);
				return Optional.of(processInstance);
			}

			LOGGER.info("Process instance with id: {} not found", processInstanceId);
			return Optional.empty();

		} catch (Exception e) {
			LOGGER.error("Error getting process instance with id: {}", processInstanceId, e);
			return Optional.empty();
		}
	}

    @Override
    public Optional<ProcessInstance> getProcessInstanceByBusinessKey(String businessKey) {
        LOGGER.info("Retrieving process instance with business key: {}", businessKey);

        try {

            var runtimeInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .singleResult();

            if (runtimeInstance != null) {
                IGRPProcessStatus status = IGRPProcessStatus.RUNNING;
                if (runtimeInstance.isSuspended()) {
                    status = IGRPProcessStatus.SUSPENDED;
                }

                var processInstance = new ProcessInstance(
                        runtimeInstance.getId(),
                        runtimeInstance.getName(),
                        runtimeInstance.getStartTime(),
                        null, // still running, so no endDate
                        runtimeInstance.getStartUserId(),
                        runtimeInstance.getProcessDefinitionId(),
                        runtimeInstance.getProcessDefinitionKey(),
                        runtimeInstance.getBusinessKey(),
                        runtimeInstance.getParentId(),
                        runtimeInstance.getProcessDefinitionVersion(),
                        runtimeInstance.getProcessDefinitionName(),
                        status
                );

                LOGGER.debug("Active process instance retrieved: {}", processInstance);
                return Optional.of(processInstance);
            }

            var historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .singleResult();

            if (historicInstance != null) {
                IGRPProcessStatus status = historicInstance.getEndTime() != null
                        ? IGRPProcessStatus.COMPLETED
                        : IGRPProcessStatus.SUSPENDED;

                var processInstance = new ProcessInstance(
                        historicInstance.getId(),
                        historicInstance.getName(),
                        historicInstance.getStartTime(),
                        historicInstance.getEndTime(),
                        historicInstance.getStartUserId(),
                        historicInstance.getProcessDefinitionId(),
                        historicInstance.getProcessDefinitionKey(),
                        historicInstance.getBusinessKey(),
                        historicInstance.getSuperProcessInstanceId(),
                        historicInstance.getProcessDefinitionVersion(),
                        historicInstance.getProcessDefinitionName(),
                        status
                );

                LOGGER.debug("Historic process instance retrieved: {}", processInstance);
                return Optional.of(processInstance);
            }

            LOGGER.info("Process instance with business key: {} not found", businessKey);
            return Optional.empty();

        } catch (Exception e) {
            LOGGER.error("Error getting process instance with business key: {}", businessKey, e);
            return Optional.empty();
        }
    }

	@Override
	public List<ProcessInstance> listProcessInstances(ProcessFilter filter) {
		LOGGER.info("Listing process instances with filter: {}", filter);

		var runtime = listRuntimeInstances(filter);

		var historic = listHistoricInstances(filter);

		LOGGER.info("Runtime: {}, Historic: {}", runtime.size(), historic.size());

		return Stream.concat(runtime.stream(), historic.stream())
				.toList();
	}

	private List<ProcessInstance> listRuntimeInstances(ProcessFilter filter) {

		var query = runtimeService.createProcessInstanceQuery();
		LOGGER.debug("Creating runtime process instance query");

		applyCommonFilters(query, filter);
		applyStatusFilterRuntime(query, filter.getStatus());

		var results = query.list(); // TODO pagination

		LOGGER.info("Found {} runtime process instances", results.size());

		return results.stream()
				.map(this::mapRuntimeInstance)
				.toList();
	}

	private List<ProcessInstance> listHistoricInstances(ProcessFilter filter) {

		var query = historyService.createHistoricProcessInstanceQuery();
		LOGGER.debug("Creating historic process instance query");

		applyCommonFilters(query, filter);
		applyStatusFilterHistoric(query, filter.getStatus());

		var results = query.list();

		LOGGER.info("Found {} historic process instances", results.size());

		return results.stream()
				.map(this::mapHistoricInstance)
				.toList();
	}

	private void applyCommonFilters(ProcessInstanceQuery query, ProcessFilter filter) {

		applyVariablesFilter(query, filter);

		ofNullable(filter.getProcessDefinitionKey())
				.ifPresent(key -> {
					LOGGER.debug("Filtering by process definition key: {}", key);
					query.processDefinitionKey(key);
				});

		ofNullable(filter.getBusinessKey())
				.ifPresent(key -> {
					LOGGER.debug("Filtering by business key: {}", key);
					query.processInstanceBusinessKey(key);
				});

		ofNullable(filter.getStartUserId())
				.ifPresent(userId -> {
					LOGGER.debug("Filtering by start user id: {}", userId);
					query.startedBy(userId);
				});

		ofNullable(filter.getStartedAfter())
				.ifPresent(date -> {
					LOGGER.debug("Filtering by started after: {}", new Date(date));
					query.startedAfter(new Date(date));
				});

		ofNullable(filter.getStartedBefore())
				.ifPresent(date -> {
					LOGGER.debug("Filtering by started before: {}", new Date(date));
					query.startedBefore(new Date(date));
				});
	}

	private void applyCommonFilters(HistoricProcessInstanceQuery query, ProcessFilter filter) {

		applyVariablesFilter(query, filter);

		ofNullable(filter.getProcessDefinitionKey())
				.ifPresent(key -> {
					LOGGER.debug("Filtering by process definition key: {}", key);
					query.processDefinitionKey(key);
				});

		ofNullable(filter.getBusinessKey())
				.ifPresent(key -> {
					LOGGER.debug("Filtering by business key: {}", key);
					query.processInstanceBusinessKey(key);
				});

		ofNullable(filter.getStartUserId())
				.ifPresent(userId -> {
					LOGGER.debug("Filtering by start user id: {}", userId);
					query.startedBy(userId);
				});

		ofNullable(filter.getStartedAfter())
				.ifPresent(date -> {
					LOGGER.debug("Filtering by started after: {}", new Date(date));
					query.startedAfter(new Date(date));
				});

		ofNullable(filter.getStartedBefore())
				.ifPresent(date -> {
					LOGGER.debug("Filtering by started before: {}", new Date(date));
					query.startedBefore(new Date(date));
				});
	}

	private void applyVariablesFilter(ProcessInstanceQuery query, ProcessFilter filter) {
		filter.getVariablesExpressions().forEach(vExpression -> {
			VariablesOperator op = vExpression.getOperator();
			String name = vExpression.getName();
			Object value = vExpression.getValue();
			switch (op) {
				case EQUALS -> query.variableValueEquals(name, value);
				case EQUALS_IGNORE_CASE -> query.variableValueEqualsIgnoreCase(name, String.valueOf(value));
				case NOT_EQUALS -> query.variableValueNotEquals(name, value);
				case GREATER_THAN -> query.variableValueGreaterThan(name, value);
				case GREATER_THAN_OR_EQUAL -> query.variableValueGreaterThanOrEqual(name, value);
				case LESS_THAN -> query.variableValueLessThan(name, value);
				case LESS_THAN_OR_EQUAL -> query.variableValueLessThanOrEqual(name, value);
				case LIKE -> query.variableValueLike(name, value.toString());
				case LIKE_IGNORE_CASE -> query.variableValueLikeIgnoreCase(name, value.toString());
				default -> { }
			}
		});
	}

	private void applyVariablesFilter(HistoricProcessInstanceQuery query, ProcessFilter filter) {
		filter.getVariablesExpressions().forEach(vExpression -> {
			VariablesOperator op = vExpression.getOperator();
			String name = vExpression.getName();
			Object value = vExpression.getValue();

			switch (op) {
				case EQUALS -> query.variableValueEquals(name, value);
				case EQUALS_IGNORE_CASE -> query.variableValueEqualsIgnoreCase(name, String.valueOf(value));
				case NOT_EQUALS -> query.variableValueNotEquals(name, value);
				case GREATER_THAN -> query.variableValueGreaterThan(name, value);
				case GREATER_THAN_OR_EQUAL -> query.variableValueGreaterThanOrEqual(name, value);
				case LESS_THAN -> query.variableValueLessThan(name, value);
				case LESS_THAN_OR_EQUAL -> query.variableValueLessThanOrEqual(name, value);
				case LIKE -> query.variableValueLike(name, value.toString());
				case LIKE_IGNORE_CASE -> query.variableValueLikeIgnoreCase(name, value.toString());
				default -> { }
			}
		});
	}

	private void applyStatusFilterRuntime(ProcessInstanceQuery query, IGRPProcessStatus status) {
		if (status == IGRPProcessStatus.RUNNING) {
			query.active();
		} else if (status == IGRPProcessStatus.SUSPENDED) {
			query.suspended();
		}
	}

	private void applyStatusFilterHistoric(HistoricProcessInstanceQuery query, IGRPProcessStatus status) {
		if (status == null) return;
		switch (status) {
			case COMPLETED -> query.finished();
			case CANCELLED -> query.deleted();
			case CREATED   -> query.unfinished();
		}
	}

	private ProcessInstance mapRuntimeInstance(org.activiti.engine.runtime.ProcessInstance instance) {
		return new ProcessInstance(
				instance.getId(),
				instance.getName(),
				instance.getStartTime(),
				null,
				instance.getStartUserId(),
				instance.getProcessDefinitionId(),
				instance.getProcessDefinitionKey(),
				instance.getBusinessKey(),
				instance.getParentId(),
				instance.getProcessDefinitionVersion(),
				instance.getProcessDefinitionName(),
				instance.isSuspended() ? IGRPProcessStatus.SUSPENDED : IGRPProcessStatus.RUNNING
		);
	}

	private ProcessInstance mapHistoricInstance(HistoricProcessInstance instance) {
		return new ProcessInstance(
				instance.getId(),
				instance.getName(),
				instance.getStartTime(),
				instance.getEndTime(),
				instance.getStartUserId(),
				instance.getProcessDefinitionId(),
				instance.getProcessDefinitionKey(),
				instance.getBusinessKey(),
				null,
				instance.getProcessDefinitionVersion(),
				instance.getProcessDefinitionName(),
				resolveStatus(instance)
		);
	}

	private IGRPProcessStatus resolveStatus(HistoricProcessInstance instance) {

        LOGGER.debug("Resolving status for historic process instance: id={}", instance.getId());

        if (instance.getEndTime() != null) {
            LOGGER.debug("Process instance has end time, status: COMPLETED");
            return IGRPProcessStatus.COMPLETED;
        }

        if (instance.getDeleteReason() != null) {
            LOGGER.debug("Process instance has delete reason: {}, status: CANCELLED", instance.getDeleteReason());
            return IGRPProcessStatus.CANCELLED;
        }

        LOGGER.debug("Process instance is still running, status: RUNNING");
        return IGRPProcessStatus.RUNNING;
    }

    @Override
    public void setProcessVariables(String processInstanceId, Map<String, Object> variables) {

        LOGGER.info("Setting variables for process instance with id: {}", processInstanceId);

        LOGGER.debug("Variables to set: count={}, keys={}",
                variables != null ? variables.size() : 0,
                variables != null ? variables.keySet() : "null");

        LOGGER.debug("Building set variables payload for process instance id: {}", processInstanceId);

		runtimeService.setVariables(processInstanceId, variables);

        LOGGER.info("Variables set successfully for process instance with id: {}", processInstanceId);
    }

    @Override
	public List<ProcessVariableInstance> getProcessVariables(String processInstanceId) {
		LOGGER.info("Getting process variables for processInstanceId={}", processInstanceId);
		var runtimeInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.singleResult();
		if (runtimeInstance != null) {
			return getRuntimeProcessVariables(processInstanceId);
		}
		return getHistoricProcessVariables(processInstanceId);
	}

	@Override
	public List<ProcessVariableInstance> getRuntimeProcessVariables(String processInstanceId) {
		LOGGER.debug("Fetching runtime variables for processInstanceId={}", processInstanceId);
		Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
		return vars.entrySet().stream()
				.map(e -> new ProcessVariableInstance(
						e.getKey(),
						e.getValue() != null ? e.getValue().getClass().getSimpleName() : "null",
						processInstanceId,
						e.getValue()
				))
				.toList();
	}

	@Override
	public List<ProcessVariableInstance> getHistoricProcessVariables(String processInstanceId) {
		LOGGER.debug("Fetching historic variables for processInstanceId={}", processInstanceId);

		List<HistoricVariableInstance> vars = historyService
				.createHistoricVariableInstanceQuery()
				.processInstanceId(processInstanceId)
				.list();

		return vars.stream()
				.map(v -> new ProcessVariableInstance(
						v.getVariableName(),
						v.getVariableTypeName(),
						v.getProcessInstanceId(),
						v.getValue()
				))
				.toList();
	}

	@Override
	public Map<String, List<ProcessVariableInstance>> getProcessVariablesBatch(Collection<String> processInstanceIds) {
		if (processInstanceIds == null || processInstanceIds.isEmpty()) {
			return Map.of();
		}

		Set<String> uniqueIds = new LinkedHashSet<>(processInstanceIds);
		Map<String, List<ProcessVariableInstance>> result = new HashMap<>();

		// 1. Batch existence check — determine which processes are still active (runtime)
		Set<String> runtimeIds = runtimeService.createProcessInstanceQuery()
				.processInstanceIds(uniqueIds)
				.list()
				.stream()
				.map(org.activiti.engine.runtime.ProcessInstance::getProcessInstanceId)
				.collect(Collectors.toSet());

		// 2. Fetch runtime variables per-process
		for (String id : runtimeIds) {
			try {
				result.put(id, getRuntimeProcessVariables(id));
			} catch (Exception e) {
				LOGGER.warn("Failed to retrieve runtime variables for process {}: {}", id, e.getMessage());
				result.put(id, List.of());
			}
		}

		// 3. Fetch historic variables per-process (Activiti 9.x has no safe batch API for historic vars by process instance)
		for (String id : uniqueIds) {
			if (runtimeIds.contains(id)) continue;
			try {
				result.put(id, getHistoricProcessVariables(id));
			} catch (Exception e) {
				LOGGER.warn("Failed to retrieve historic variables for process {}: {}", id, e.getMessage());
				result.put(id, List.of());
			}
		}

		LOGGER.info("Batch fetched variables for {} processes ({} runtime, {} historic)",
				uniqueIds.size(), runtimeIds.size(), uniqueIds.size() - runtimeIds.size());

		return result;
	}

	@Override
	public void correlateMessage(String businessKey, String messageName, Map<String, Object> variables) {
		LOGGER.info("Correlating message with name: {} for businessKey: {}", messageName, businessKey);

        var runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if(runtimeInstance != null) {
            Execution execution = runtimeService.createExecutionQuery()
                    .processInstanceId(runtimeInstance.getProcessInstanceId())
                    .messageEventSubscriptionName(messageName)
                    .singleResult();

            if (execution != null) {
                runtimeService.messageEventReceived(messageName, execution.getId(), variables);
            } else {
                LOGGER.warn("No execution waiting for message {} and businessKey {}", messageName, businessKey);
            }

            LOGGER.info("Message with name: {} correlated for businessKey: {}", messageName, businessKey);

        } else {
            LOGGER.warn("Message {} not correlated. No running process instance found for businessKey {}", messageName, businessKey);
        }

	}

	public void signal(String processInstanceId, String taskId, Map<String, Object> processVariables) {
		LOGGER.info("Signaling process instance with id: {} with variables: {} and task ID {}", processInstanceId, processVariables, taskId);

        List<Execution> executions;

        if(taskId != null) {
            executions = this.runtimeService.createExecutionQuery()
                    .processInstanceId(processInstanceId)
                    .activityId(taskId)
                    .list();
        } else {
            executions = this.runtimeService.createExecutionQuery()
                    .processInstanceId(processInstanceId)
                    .list();
        }

		if (executions.isEmpty()) {
			LOGGER.warn("No executions found for process instance {}", processInstanceId);
			return;
		}

		boolean signaled = false;
		for (Execution execution : executions) {
			if (execution.getActivityId() != null) {
				((RuntimeServiceImpl) this.runtimeService).signal(execution.getId(), processVariables);
				LOGGER.info("Execution with id: {} at activity {} signaled successfully",
						execution.getId(), execution.getActivityId());
				signaled = true;
			}
		}
		if (!signaled) {
			LOGGER.warn("No signalable executions found for process instance {}", processInstanceId);
		}
	}

	@Override
	public void rescheduleTimer(String processInstanceId, long seconds) {
		LOGGER.info("Rescheduling first timer for process instance {} by {} seconds",
				processInstanceId, seconds);

		rescheduleTimerInternal(processInstanceId, null, seconds);
	}

	@Override
	public void rescheduleTimer(String processInstanceId, String timerEventId, long seconds) {
		LOGGER.info("Rescheduling timer '{}' for process instance {} by {} seconds",
				timerEventId, processInstanceId, seconds);

		rescheduleTimerInternal(processInstanceId, timerEventId, seconds);
	}

	private void rescheduleTimerInternal(String processInstanceId,
										 String timerEventId,
										 long seconds) {

		List<Job> timerJobs = managementService.createTimerJobQuery()
				.processInstanceId(processInstanceId)
				.list();

		if (timerJobs.isEmpty()) {
			throw new IllegalStateException(
					"No timer jobs found for process instance " + processInstanceId);
		}

		TimerJobEntity targetJob = managementService.executeCommand(commandContext -> {
			for (Job job : timerJobs) {

				TimerJobEntity jobEntity = commandContext.getDbSqlSession()
						.selectById(TimerJobEntity.class, job.getId());

				if (jobEntity == null) {
					continue;
				}

				// If no timerEventId provided → take first timer
				if (timerEventId == null) {
					return jobEntity;
				}

				if (jobEntity.getExecutionId() != null) {
					ExecutionEntity execution = commandContext.getExecutionEntityManager()
							.findById(jobEntity.getExecutionId());

					if (execution != null && timerEventId.equals(execution.getActivityId())) {
						return jobEntity;
					}
				}
			}
			return null;
		});

		if (targetJob == null) {
			throw new IllegalStateException(
					timerEventId != null
							? "No timer job found for timer event ID '" + timerEventId + "'"
							: "No timer job found for process instance " + processInstanceId
			);
		}

		Date newDueDate = new Date(System.currentTimeMillis() + seconds * 1000);
		Date oldDate = targetJob.getDuedate();

		managementService.executeCommand(commandContext -> {
			targetJob.setDuedate(newDueDate);
			commandContext.getDbSqlSession().update(targetJob);
			return null;
		});

		LOGGER.info(
				"Timer job {} for process {} successfully rescheduled. Old due date: {} → New due date: {}",
				targetJob.getId(),
				processInstanceId,
				oldDate,
				newDueDate
		);
	}

}
