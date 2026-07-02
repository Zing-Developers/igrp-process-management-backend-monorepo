package cv.igrp.framework.process.runtime.core.engine.process;

import cv.igrp.framework.process.runtime.core.engine.process.model.ProcessFilter;
import cv.igrp.framework.process.runtime.core.engine.process.model.ProcessInstance;
import cv.igrp.framework.process.runtime.core.engine.process.model.ProcessVariableInstance;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface ProcessManagerAdapter {

    @Deprecated
    ProcessInstance startProcess(String processDefinitionId, String businessKey, Map<String, Object> variables);

    ProcessInstance createProcess(String processDefinitionId, String businessKey);

    ProcessInstance startCreatedProcess(String processInstanceId, String processDefinitionId, String businessKey, Map<String, Object> variables);

    void suspendProcess(String processInstanceId);

    void resumeProcess(String processInstanceId);

    void terminateProcess(String processInstanceId, String reason);

    Optional<ProcessInstance> getProcessInstance(String processInstanceId);

    Optional<ProcessInstance> getProcessInstanceByBusinessKey(String businessKey);

    List<ProcessInstance> listProcessInstances(ProcessFilter filter);

    void setProcessVariables(String processInstanceId, Map<String, Object> variables);

    List<ProcessVariableInstance> getProcessVariables(String processInstanceId);

    /**
     * Retrieves process variables for multiple process instances in a single operation.
     * Default implementation falls back to individual calls per process instance.
     *
     * @param processInstanceIds the unique identifiers of the process instances
     * @return a map of processInstanceId to its list of variable instances
     */
    default Map<String, List<ProcessVariableInstance>> getProcessVariablesBatch(Collection<String> processInstanceIds) {
        Map<String, List<ProcessVariableInstance>> result = new HashMap<>();
        for (String id : processInstanceIds) {
            result.put(id, getProcessVariables(id));
        }
        return result;
    }

	List<ProcessVariableInstance> getRuntimeProcessVariables(String processInstanceId);

	List<ProcessVariableInstance> getHistoricProcessVariables(String processInstanceId);

	void correlateMessage(String businessKey, String messageName, Map<String, Object> variables);

	void signal(String processInstanceId, String taskId, Map<String, Object> processVariables);

	void rescheduleTimer(String processInstanceId, String timerEventId, long seconds);

	void rescheduleTimer(String processInstanceId, long seconds);

}