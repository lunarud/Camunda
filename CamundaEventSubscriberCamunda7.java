
camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    filter:
      create: All tasks
    webapp:
      index-redirect-enabled: true
    database:
      schema-update: true
    # Enable history for events
    history-level: full
    # Job executor configuration
    job-execution:
      enabled: true
      core-pool-size: 3
      max-pool-size: 10

<dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter</artifactId>
    <version>7.19.0</version>
</dependency>
<dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
    <version>7.19.0</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>


<bpmn:userTask id="approvalTask" name="Approval Task">
    <bpmn:extensionElements>
        <camunda:taskListener 
            event="create" 
            delegateExpression="${camundaEventSubscriber}" />
        <camunda:taskListener 
            event="assignment" 
            delegateExpression="${camundaEventSubscriber}" />
        <camunda:taskListener 
            event="complete" 
            delegateExpression="${camundaEventSubscriber}" />
    </bpmn:extensionElements>
</bpmn:userTask>

<!-- Execution Listener in BPMN -->
<bpmn:serviceTask id="processData" name="Process Data">
    <bpmn:extensionElements>
        <camunda:executionListener 
            event="start" 
            delegateExpression="${camundaEventSubscriber}" />
        <camunda:executionListener 
            event="end" 
            delegateExpression="${camundaEventSubscriber}" />
    </bpmn:extensionElements>
</bpmn:serviceTask>




@Component
@Slf4j
public class CamundaEventSubscriber {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Subscribe to Task Events in Camunda 7
    @EventListener
    public void handleTaskEvent(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String assignee = delegateTask.getAssignee();
        
        log.info("Task Event - Type: {}, Task: {}, ID: {}", eventName, taskName, taskId);
        
        switch (eventName) {
            case TaskListener.EVENTNAME_CREATE:
                handleTaskCreate(delegateTask);
                break;
            case TaskListener.EVENTNAME_ASSIGNMENT:
                handleTaskAssignment(delegateTask);
                break;
            case TaskListener.EVENTNAME_COMPLETE:
                handleTaskComplete(delegateTask);
                break;
            case TaskListener.EVENTNAME_DELETE:
                handleTaskDelete(delegateTask);
                break;
        }
    }
    
    // Subscribe to Execution Events in Camunda 7
    @EventListener
    public void handleExecutionEvent(DelegateExecution delegateExecution) {
        String eventName = delegateExecution.getEventName();
        String activityId = delegateExecution.getCurrentActivityId();
        String processInstanceId = delegateExecution.getProcessInstanceId();
        
        log.info("Execution Event - Type: {}, Activity: {}, ProcessInstance: {}", 
                eventName, activityId, processInstanceId);
        
        switch (eventName) {
            case ExecutionListener.EVENTNAME_START:
                handleExecutionStart(delegateExecution);
                break;
            case ExecutionListener.EVENTNAME_END:
                handleExecutionEnd(delegateExecution);
                break;
            case ExecutionListener.EVENTNAME_TAKE:
                handleExecutionTake(delegateExecution);
                break;
        }
    }
    
    // Specific Task Event Handlers
    private void handleTaskCreate(DelegateTask task) {
        // Log task creation
        auditService.logTaskCreation(task.getId(), task.getName(), task.getProcessInstanceId());
        
        // Set default properties
        if (task.getDueDate() == null && "high".equals(task.getVariable("priority"))) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            task.setDueDate(cal.getTime());
            log.info("Set due date for high priority task: {}", task.getId());
        }
        
        // Auto-assign based on department
        String department = (String) task.getVariable("department");
        if (department != null && task.getAssignee() == null) {
            String defaultAssignee = getDefaultAssigneeForDepartment(department);
            if (defaultAssignee != null) {
                task.setAssignee(defaultAssignee);
                log.info("Auto-assigned task {} to {}", task.getId(), defaultAssignee);
            }
        }
    }
    
    private void handleTaskAssignment(DelegateTask task) {
        String assignee = task.getAssignee();
        if (assignee != null) {
            // Send notification to assignee
            notificationService.sendTaskAssignmentNotification(assignee, task);
            
            // Log assignment
            auditService.logTaskAssignment(task.getId(), assignee);
            
            // Update task variables
            task.setVariable("assignedDate", new Date());
            task.setVariable("assignedBy", "system");
        }
    }
    
    private void handleTaskComplete(DelegateTask task) {
        // Calculate task duration
        Date assignedDate = (Date) task.getVariable("assignedDate");
        if (assignedDate != null) {
            long durationHours = (new Date().getTime() - assignedDate.getTime()) / (1000 * 60 * 60);
            task.setVariable("taskDurationHours", durationHours);
            log.info("Task {} completed in {} hours", task.getId(), durationHours);
        }
        
        // Log completion
        auditService.logTaskCompletion(task.getId(), task.getAssignee());
        
        // Send completion notification to process owner
        String processOwner = (String) task.getVariable("processOwner");
        if (processOwner != null) {
            notificationService.sendTaskCompletionNotification(processOwner, task);
        }
        
        // Archive task data
        archiveTaskData(task);
    }
    
    private void handleTaskDelete(DelegateTask task) {
        String deleteReason = task.getDeleteReason();
        auditService.logTaskDeletion(task.getId(), deleteReason);
        
        // Notify assignee if task was deleted
        if (task.getAssignee() != null && !"completed".equals(deleteReason)) {
            notificationService.sendTaskCancellationNotification(task.getAssignee(), task, deleteReason);
        }
    }
    
    // Specific Execution Event Handlers
    private void handleExecutionStart(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        // Log activity start
        auditService.logActivityStart(execution.getProcessInstanceId(), activityId, activityName);
        
        // Set activity-specific variables
        execution.setVariable(activityId + "_startTime", new Date());
        
        // Handle specific activities
        if ("approvalTask".equals(activityId)) {
            handleApprovalTaskStart(execution);
        } else if ("reviewTask".equals(activityId)) {
            handleReviewTaskStart(execution);
        }
    }
    
    private void handleExecutionEnd(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        // Calculate activity duration
        Date startTime = (Date) execution.getVariable(activityId + "_startTime");
        if (startTime != null) {
            long durationMs = new Date().getTime() - startTime.getTime();
            execution.setVariable(activityId + "_duration", durationMs);
            log.info("Activity {} completed in {} ms", activityId, durationMs);
        }
        
        // Log activity completion
        auditService.logActivityEnd(execution.getProcessInstanceId(), activityId, activityName);
        
        // Handle specific activity completions
        if ("approvalTask".equals(activityId)) {
            handleApprovalTaskEnd(execution);
        }
    }
    
    private void handleExecutionTake(DelegateExecution execution) {
        String currentTransitionId = execution.getCurrentTransitionId();
        
        // Log sequence flow taken
        auditService.logSequenceFlowTaken(execution.getProcessInstanceId(), currentTransitionId);
        
        // Handle conditional flows
        if ("approvalRejected".equals(currentTransitionId)) {
            execution.setVariable("rejectionCount", 
                ((Integer) execution.getVariable("rejectionCount")) + 1);
        }
    }
    
    // Helper methods
    private String getDefaultAssigneeForDepartment(String department) {
        switch (department.toLowerCase()) {
            case "hr": return "hr.manager";
            case "finance": return "finance.manager";
            case "it": return "it.manager";
            default: return null;
        }
    }
    
    private void handleApprovalTaskStart(DelegateExecution execution) {
        // Set approval deadline
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 3);
        execution.setVariable("approvalDeadline", cal.getTime());
        
        // Initialize approval tracking
        execution.setVariable("approvalLevel", 1);
        execution.setVariable("approvalHistory", new ArrayList<String>());
    }
    
    private void handleReviewTaskStart(DelegateExecution execution) {
        // Set review parameters
        execution.setVariable("reviewStarted", true);
        execution.setVariable("reviewerCount", 0);
    }
    
    private void handleApprovalTaskEnd(DelegateExecution execution) {
        Boolean approved = (Boolean) execution.getVariable("approved");
        String approver = (String) execution.getVariable("approver");
        
        if (approved != null && approved) {
            log.info("Process {} approved by {}", execution.getProcessInstanceId(), approver);
            // Send approval notifications
            notificationService.sendApprovalNotification(execution.getProcessInstanceId(), approver);
        }
    }
    
    private void archiveTaskData(DelegateTask task) {
        // Archive task data to external system
        log.info("Archiving data for completed task: {}", task.getId());
        // Implementation for archiving
    }
}



@Configuration
public class CamundaConfig {
    
    @Bean
    public ProcessEnginePlugin customListenerPlugin() {
        return new ProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                // Register global task listeners
                List<TaskListener> globalTaskListeners = new ArrayList<>();
                globalTaskListeners.add(new GlobalTaskListener());
                processEngineConfiguration.setCustomPreTaskListeners(globalTaskListeners);
                
                // Register global execution listeners
                List<ExecutionListener> globalExecutionListeners = new ArrayList<>();
                globalExecutionListeners.add(new GlobalExecutionListener());
                processEngineConfiguration.setCustomPreExecutionListeners(globalExecutionListeners);
            }
            
            @Override
            public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                // Post initialization logic
            }
            
            @Override
            public void postProcessEngineBuild(ProcessEngine processEngine) {
                // Post build logic
            }
        };
    }
}


@Service
@Slf4j
public class NotificationService {
    
    public void sendTaskAssignmentNotification(String assignee, DelegateTask task) {
        log.info("Sending assignment notification to {} for task {}", assignee, task.getName());
        // Implementation for sending notification
    }
    
    public void sendTaskCompletionNotification(String recipient, DelegateTask task) {
        log.info("Sending completion notification to {} for task {}", recipient, task.getName());
    }
    
    public void sendTaskCancellationNotification(String assignee, DelegateTask task, String reason) {
        log.info("Sending cancellation notification to {} for task {} - Reason: {}", 
                assignee, task.getName(), reason);
    }
    
    public void sendApprovalNotification(String processInstanceId, String approver) {
        log.info("Sending approval notification for process {} approved by {}", 
                processInstanceId, approver);
    }
}






