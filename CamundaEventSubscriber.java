
Enable Camunda events in your application.yml:
yamlcamunda:
  bpm:
    events:
      task: true
      execution: true
      process-instance: true
      history: true

 
@Component
@Slf4j
public class CamundaEventSubscriber {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Subscribe to Task Events
    @EventListener
    public void handleTaskEvent(TaskEvent taskEvent) {
        DelegateTask task = taskEvent.getTask();
        String eventName = taskEvent.getEventName();
        String taskId = task.getId();
        String taskName = task.getName();
        String assignee = task.getAssignee();
        
        log.info("Task Event - Type: {}, Task: {}, ID: {}", eventName, taskName, taskId);
        
        switch (eventName) {
            case TaskListener.EVENTNAME_CREATE:
                handleTaskCreate(task);
                break;
            case TaskListener.EVENTNAME_ASSIGNMENT:
                handleTaskAssignment(task);
                break;
            case TaskListener.EVENTNAME_COMPLETE:
                handleTaskComplete(task);
                break;
            case TaskListener.EVENTNAME_DELETE:
                handleTaskDelete(task);
                break;
        }
    }
    
    // Subscribe to Execution Events
    @EventListener
    public void handleExecutionEvent(ExecutionEvent executionEvent) {
        DelegateExecution execution = executionEvent.getExecution();
        String eventName = executionEvent.getEventName();
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();
        
        log.info("Execution Event - Type: {}, Activity: {}, ProcessInstance: {}", 
                eventName, activityId, processInstanceId);
        
        switch (eventName) {
            case ExecutionListener.EVENTNAME_START:
                handleExecutionStart(execution);
                break;
            case ExecutionListener.EVENTNAME_END:
                handleExecutionEnd(execution);
                break;
            case ExecutionListener.EVENTNAME_TAKE:
                handleExecutionTake(execution);
                break;
        }
    }
    
    // Subscribe to Process Instance Events
    @EventListener
    public void handleProcessInstanceEvent(ProcessInstanceEvent processInstanceEvent) {
        String eventName = processInstanceEvent.getEventName();
        String processInstanceId = processInstanceEvent.getProcessInstanceId();
        String processDefinitionKey = processInstanceEvent.getProcessDefinitionKey();
        
        log.info("Process Instance Event - Type: {}, ProcessInstance: {}, Definition: {}", 
                eventName, processInstanceId, processDefinitionKey);
        
        if ("start".equals(eventName)) {
            auditService.logProcessStart(processInstanceId, processDefinitionKey);
        } else if ("end".equals(eventName)) {
            auditService.logProcessEnd(processInstanceId, processDefinitionKey);
        }
    }
    
    // Specific Task Event Handlers
    private void handleTaskCreate(DelegateTask task) {
        // Log task creation
        auditService.logTaskCreation(task.getId(), task.getName(), task.getProcessInstanceId());
        
        // Set default properties
        if (task.getDueDate() == null && "important".equals(task.getVariable("priority"))) {
            Date dueDate = Date.from(Instant.now().plus(Duration.ofDays(1)));
            task.setDueDate(dueDate);
        }
    }
    
    private void handleTaskAssignment(DelegateTask task) {
        String assignee = task.getAssignee();
        if (assignee != null) {
            // Send notification to assignee
            notificationService.sendTaskAssignmentNotification(assignee, task);
            
            // Log assignment
            auditService.logTaskAssignment(task.getId(), assignee);
        }
    }
    
    private void handleTaskComplete(DelegateTask task) {
        // Log completion
        auditService.logTaskCompletion(task.getId(), task.getAssignee());
        
        // Send completion notification to process owner
        String processOwner = (String) task.getVariable("processOwner");
        if (processOwner != null) {
            notificationService.sendTaskCompletionNotification(processOwner, task);
        }
    }
    
    private void handleTaskDelete(DelegateTask task) {
        auditService.logTaskDeletion(task.getId(), task.getDeleteReason());
    }
    
    // Specific Execution Event Handlers
    private void handleExecutionStart(DelegateExecution execution) {
        String activityName = execution.getCurrentActivityName();
        
        // Log activity start
        auditService.logActivityStart(execution.getProcessInstanceId(), 
                                    execution.getCurrentActivityId(), activityName);
        
        // Set process variables based on activity
        if ("approvalTask".equals(execution.getCurrentActivityId())) {
            execution.setVariable("approvalStartTime", new Date());
        }
    }
    
    private void handleExecutionEnd(DelegateExecution execution) {
        String activityName = execution.getCurrentActivityName();
        
        // Log activity completion
        auditService.logActivityEnd(execution.getProcessInstanceId(), 
                                  execution.getCurrentActivityId(), activityName);
        
        // Calculate duration for specific activities
        if ("approvalTask".equals(execution.getCurrentActivityId())) {
            Date startTime = (Date) execution.getVariable("approvalStartTime");
            if (startTime != null) {
                long durationMs = new Date().getTime() - startTime.getTime();
                execution.setVariable("approvalDuration", durationMs);
            }
        }
    }
    
    private void handleExecutionTake(DelegateExecution execution) {
        // Log sequence flow taken
        auditService.logSequenceFlowTaken(execution.getProcessInstanceId(), 
                                        execution.getCurrentTransitionId());
    }
}


@Service
@Slf4j
public class NotificationService {
    
    public void sendTaskAssignmentNotification(String assignee, DelegateTask task) {
        log.info("Sending assignment notification to {} for task {}", assignee, task.getName());
        // Implementation for sending email/SMS/push notification
    }
    
    public void sendTaskCompletionNotification(String recipient, DelegateTask task) {
        log.info("Sending completion notification to {} for task {}", recipient, task.getName());
        // Implementation for sending notification
    }
}


@Service
@Slf4j
public class AuditService {
    
    public void logTaskCreation(String taskId, String taskName, String processInstanceId) {
        log.info("AUDIT: Task created - ID: {}, Name: {}, ProcessInstance: {}", 
                taskId, taskName, processInstanceId);
        // Save to audit database
    }
    
    public void logTaskAssignment(String taskId, String assignee) {
        log.info("AUDIT: Task assigned - ID: {}, Assignee: {}", taskId, assignee);
    }
    
    public void logTaskCompletion(String taskId, String assignee) {
        log.info("AUDIT: Task completed - ID: {}, CompletedBy: {}", taskId, assignee);
    }
    
    public void logProcessStart(String processInstanceId, String processDefinitionKey) {
        log.info("AUDIT: Process started - Instance: {}, Definition: {}", 
                processInstanceId, processDefinitionKey);
    }
    
    public void logProcessEnd(String processInstanceId, String processDefinitionKey) {
        log.info("AUDIT: Process ended - Instance: {}, Definition: {}", 
                processInstanceId, processDefinitionKey);
    }
    
    public void logActivityStart(String processInstanceId, String activityId, String activityName) {
        log.info("AUDIT: Activity started - Process: {}, Activity: {} ({})", 
                processInstanceId, activityId, activityName);
    }
    
    public void logActivityEnd(String processInstanceId, String activityId, String activityName) {
        log.info("AUDIT: Activity ended - Process: {}, Activity: {} ({})", 
                processInstanceId, activityId, activityName);
    }
    
    public void logTaskDeletion(String taskId, String deleteReason) {
        log.info("AUDIT: Task deleted - ID: {}, Reason: {}", taskId, deleteReason);
    }
    
    public void logSequenceFlowTaken(String processInstanceId, String transitionId) {
        log.info("AUDIT: Sequence flow taken - Process: {}, Transition: {}", 
                processInstanceId, transitionId);
    }
}



@SpringBootTest
@Slf4j
class CamundaEventSubscriberTest {
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Test
    void testEventSubscriberIntegration() {
        // Start a process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
        
        // Complete a task
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        
        if (task != null) {
            taskService.complete(task.getId());
        }
        
        // Events should be automatically captured by CamundaEventSubscriber
        log.info("Process instance completed: {}", processInstance.getId());
    }
}




















