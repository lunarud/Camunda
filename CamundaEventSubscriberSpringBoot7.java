

camunda:
  bpm:
    # Database configuration
    database:
      schema-update: true
      type: h2
    # Admin user
    admin-user:
      id: admin
      password: admin
      first-name: Admin
      last-name: User
      email: admin@company.com
    # History configuration
    history-level: FULL
    # Job executor
    job-execution:
      enabled: true
      core-pool-size: 3
      max-pool-size: 10
      queue-capacity: 25
    # Webapp configuration
    webapp:
      index-redirect-enabled: true
    # Metrics
    metrics:
      enabled: true
    # Generic properties
    generic-properties:
      properties:
        # Enable events
        enableExpressionsInAdhocQueries: true
        # Job executor configuration
        jobExecutorActivate: true

# Spring configuration
spring:
  datasource:
    url: jdbc:h2:mem:camunda;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

# Logging
logging:
  level:
    org.camunda: INFO
    com.yourpackage.camunda: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"











@Component
@Slf4j
public class CamundaEventSubscriber {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuditService auditService;
    
    // Camunda 7 Spring Boot Event Listeners
    @EventListener
    public void handleTaskCreate(TaskCreateEvent event) {
        DelegateTask task = event.getTask();
        log.info("Task Created - ID: {}, Name: {}, ProcessInstance: {}", 
                task.getId(), task.getName(), task.getProcessInstanceId());
        
        handleTaskCreate(task);
    }
    
    @EventListener
    public void handleTaskAssign(TaskAssignEvent event) {
        DelegateTask task = event.getTask();
        log.info("Task Assigned - ID: {}, Assignee: {}", task.getId(), task.getAssignee());
        
        handleTaskAssignment(task);
    }
    
    @EventListener
    public void handleTaskComplete(TaskCompleteEvent event) {
        DelegateTask task = event.getTask();
        log.info("Task Completed - ID: {}, CompletedBy: {}", task.getId(), task.getAssignee());
        
        handleTaskComplete(task);
    }
    
    @EventListener
    public void handleTaskDelete(TaskDeleteEvent event) {
        DelegateTask task = event.getTask();
        log.info("Task Deleted - ID: {}, Reason: {}", task.getId(), task.getDeleteReason());
        
        handleTaskDelete(task);
    }
    
    @EventListener
    public void handleExecutionStart(ExecutionStartEvent event) {
        DelegateExecution execution = event.getExecution();
        log.info("Execution Started - Activity: {}, ProcessInstance: {}", 
                execution.getCurrentActivityId(), execution.getProcessInstanceId());
        
        handleExecutionStart(execution);
    }
    
    @EventListener
    public void handleExecutionEnd(ExecutionEndEvent event) {
        DelegateExecution execution = event.getExecution();
        log.info("Execution Ended - Activity: {}, ProcessInstance: {}", 
                execution.getCurrentActivityId(), execution.getProcessInstanceId());
        
        handleExecutionEnd(execution);
    }
    
    @EventListener
    public void handleSequenceFlowTake(SequenceFlowTakeEvent event) {
        DelegateExecution execution = event.getExecution();
        log.info("Sequence Flow Taken - Transition: {}, ProcessInstance: {}", 
                execution.getCurrentTransitionId(), execution.getProcessInstanceId());
        
        handleExecutionTake(execution);
    }
    
    @EventListener
    public void handleProcessStart(ProcessStartEvent event) {
        DelegateExecution execution = event.getExecution();
        log.info("Process Started - ProcessInstance: {}, ProcessDefinition: {}", 
                execution.getProcessInstanceId(), execution.getProcessDefinitionId());
        
        handleProcessStart(execution);
    }
    
    @EventListener
    public void handleProcessEnd(ProcessEndEvent event) {
        DelegateExecution execution = event.getExecution();
        log.info("Process Ended - ProcessInstance: {}, ProcessDefinition: {}", 
                execution.getProcessInstanceId(), execution.getProcessDefinitionId());
        
        handleProcessEnd(execution);
    }
    
    // Business Logic Handlers
    private void handleTaskCreate(DelegateTask task) {
        // Log task creation
        auditService.logTaskCreation(task.getId(), task.getName(), task.getProcessInstanceId());
        
        // Set default due date for high priority tasks
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
        
        // Set task variables
        task.setVariable("createdDate", new Date());
        task.setVariable("taskStatus", "CREATED");
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
            task.setVariable("taskStatus", "ASSIGNED");
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
        
        // Update task status
        task.setVariable("completedDate", new Date());
        task.setVariable("taskStatus", "COMPLETED");
    }
    
    private void handleTaskDelete(DelegateTask task) {
        String deleteReason = task.getDeleteReason();
        auditService.logTaskDeletion(task.getId(), deleteReason);
        
        // Notify assignee if task was cancelled
        if (task.getAssignee() != null && !"completed".equals(deleteReason)) {
            notificationService.sendTaskCancellationNotification(task.getAssignee(), task, deleteReason);
        }
    }
    
    private void handleExecutionStart(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        // Skip if this is the process instance itself
        if (activityId == null) {
            return;
        }
        
        // Log activity start
        auditService.logActivityStart(execution.getProcessInstanceId(), activityId, activityName);
        
        // Set activity timing
        execution.setVariable(activityId + "_startTime", new Date());
        
        // Handle specific activities
        switch (activityId) {
            case "approvalTask":
                handleApprovalTaskStart(execution);
                break;
            case "reviewTask":
                handleReviewTaskStart(execution);
                break;
            case "notificationTask":
                handleNotificationTaskStart(execution);
                break;
        }
    }
    
    private void handleExecutionEnd(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String activityName = execution.getCurrentActivityName();
        
        // Skip if this is the process instance itself
        if (activityId == null) {
            return;
        }
        
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
            Integer rejectionCount = (Integer) execution.getVariable("rejectionCount");
            if (rejectionCount == null) rejectionCount = 0;
            execution.setVariable("rejectionCount", rejectionCount + 1);
            
            log.info("Process {} rejected {} times", execution.getProcessInstanceId(), rejectionCount + 1);
        } else if ("approvalApproved".equals(currentTransitionId)) {
            execution.setVariable("approvalDate", new Date());
            log.info("Process {} approved", execution.getProcessInstanceId());
        }
    }
    
    private void handleProcessStart(DelegateExecution execution) {
        String processDefinitionKey = execution.getProcessDefinitionId().split(":")[0];
        auditService.logProcessStart(execution.getProcessInstanceId(), processDefinitionKey);
        
        // Set process-level variables
        execution.setVariable("processStartTime", new Date());
        execution.setVariable("processStatus", "RUNNING");
        execution.setVariable("rejectionCount", 0);
        
        // Initialize process tracking
        notificationService.sendProcessStartNotification(execution.getProcessInstanceId(), processDefinitionKey);
    }
    
    private void handleProcessEnd(DelegateExecution execution) {
        String processDefinitionKey = execution.getProcessDefinitionId().split(":")[0];
        
        // Calculate process duration
        Date startTime = (Date) execution.getVariable("processStartTime");
        if (startTime != null) {
            long durationMs = new Date().getTime() - startTime.getTime();
            execution.setVariable("processDuration", durationMs);
            log.info("Process {} completed in {} ms", execution.getProcessInstanceId(), durationMs);
        }
        
        auditService.logProcessEnd(execution.getProcessInstanceId(), processDefinitionKey);
        
        // Set final status
        execution.setVariable("processEndTime", new Date());
        execution.setVariable("processStatus", "COMPLETED");
        
        // Send process completion notification
        notificationService.sendProcessEndNotification(execution.getProcessInstanceId(), processDefinitionKey);
    }
    
    // Helper methods
    private String getDefaultAssigneeForDepartment(String department) {
        switch (department.toLowerCase()) {
            case "hr": return "hr.manager";
            case "finance": return "finance.manager";
            case "it": return "it.manager";
            case "legal": return "legal.manager";
            default: return "default.manager";
        }
    }
    
    private void handleApprovalTaskStart(DelegateExecution execution) {
        // Set approval deadline (3 business days)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 3);
        execution.setVariable("approvalDeadline", cal.getTime());
        
        // Initialize approval tracking
        execution.setVariable("approvalLevel", 1);
        execution.setVariable("approvalStarted", true);
        
        log.info("Approval process started for {}", execution.getProcessInstanceId());
    }
    
    private void handleReviewTaskStart(DelegateExecution execution) {
        // Set review parameters
        execution.setVariable("reviewStarted", true);
        execution.setVariable("reviewerCount", 0);
        execution.setVariable("reviewDeadline", new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)); // 2 days
        
        log.info("Review process started for {}", execution.getProcessInstanceId());
    }
    
    private void handleNotificationTaskStart(DelegateExecution execution) {
        // Prepare notification data
        execution.setVariable("notificationSent", false);
        execution.setVariable("notificationAttempts", 0);
        
        log.info("Notification task started for {}", execution.getProcessInstanceId());
    }
    
    private void handleApprovalTaskEnd(DelegateExecution execution) {
        Boolean approved = (Boolean) execution.getVariable("approved");
        String approver = (String) execution.getVariable("approver");
        
        if (approved != null && approved) {
            execution.setVariable("finalApprovalDate", new Date());
            log.info("Process {} finally approved by {}", execution.getProcessInstanceId(), approver);
            notificationService.sendFinalApprovalNotification(execution.getProcessInstanceId(), approver);
        } else {
            log.info("Process {} rejected by {}", execution.getProcessInstanceId(), approver);
            notificationService.sendRejectionNotification(execution.getProcessInstanceId(), approver);
        }
    }
}


@Service
@Slf4j
public class NotificationService {
    
    public void sendTaskAssignmentNotification(String assignee, DelegateTask task) {
        log.info("📧 Sending assignment notification to {} for task '{}'", assignee, task.getName());
        // Implementation: Send email, SMS, or push notification
    }
    
    public void sendTaskCompletionNotification(String recipient, DelegateTask task) {
        log.info("✅ Sending completion notification to {} for task '{}'", recipient, task.getName());
    }
    
    public void sendTaskCancellationNotification(String assignee, DelegateTask task, String reason) {
        log.info("❌ Sending cancellation notification to {} for task '{}' - Reason: {}", 
                assignee, task.getName(), reason);
    }
    
    public void sendProcessStartNotification(String processInstanceId, String processDefinitionKey) {
        log.info("🚀 Process started - Instance: {}, Definition: {}", processInstanceId, processDefinitionKey);
    }
    
    public void sendProcessEndNotification(String processInstanceId, String processDefinitionKey) {
        log.info("🏁 Process completed - Instance: {}, Definition: {}", processInstanceId, processDefinitionKey);
    }
    
    public void sendFinalApprovalNotification(String processInstanceId, String approver) {
        log.info("✅ Final approval notification for process {} approved by {}", processInstanceId, approver);
    }
    
    public void sendRejectionNotification(String processInstanceId, String approver) {
        log.info("❌ Rejection notification for process {} rejected by {}", processInstanceId, approver);
    }
}

@Service
@Slf4j
public class AuditService {
    
    // Task audit methods
    public void logTaskCreation(String taskId, String taskName, String processInstanceId) {
        log.info("AUDIT: Task created - ID: {}, Name: '{}', ProcessInstance: {}", 
                taskId, taskName, processInstanceId);
    }
    
    public void logTaskAssignment(String taskId, String assignee) {
        log.info("AUDIT: Task assigned - ID: {}, Assignee: {}", taskId, assignee);
    }
    
    public void logTaskCompletion(String taskId, String assignee) {
        log.info("AUDIT: Task completed - ID: {}, CompletedBy: {}", taskId, assignee);
    }
    
    public void logTaskDeletion(String taskId, String deleteReason) {
        log.info("AUDIT: Task deleted - ID: {}, Reason: {}", taskId, deleteReason);
    }
    
    // Process audit methods
    public void logProcessStart(String processInstanceId, String processDefinitionKey) {
        log.info("AUDIT: Process started - Instance: {}, Definition: {}", 
                processInstanceId, processDefinitionKey);
    }
    
    public void logProcessEnd(String processInstanceId, String processDefinitionKey) {
        log.info("AUDIT: Process ended - Instance: {}, Definition: {}", 
                processInstanceId, processDefinitionKey);
    }
    
    // Activity audit methods
    public void logActivityStart(String processInstanceId, String activityId, String activityName) {
        log.info("AUDIT: Activity started - Process: {}, Activity: {} ('{}')", 
                processInstanceId, activityId, activityName);
    }
    
    public void logActivityEnd(String processInstanceId, String activityId, String activityName) {
        log.info("AUDIT: Activity ended - Process: {}, Activity: {} ('{}')", 
                processInstanceId, activityId, activityName);
    }
    
    public void logSequenceFlowTaken(String processInstanceId, String transitionId) {
        log.info("AUDIT: Sequence flow taken - Process: {}, Transition: {}", 
                processInstanceId, transitionId);
    }
}





<dependencies>
    <!-- Camunda 7 Spring Boot Starter -->
    <dependency>
        <groupId>org.camunda.bpm.springboot</groupId>
        <artifactId>camunda-bpm-spring-boot-starter</artifactId>
        <version>7.19.0</version>
    </dependency>
    
    <!-- Camunda 7 Web App -->
    <dependency>
        <groupId>org.camunda.bpm.springboot</groupId>
        <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
        <version>7.19.0</version>
    </dependency>
    
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.camunda.bpm.assert</groupId>
        <artifactId>camunda-bpm-assert</artifactId>
        <version>15.0.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

@SpringBootApplication
@EnableJpaRepositories
@Slf4j
public class CamundaApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CamundaApplication.class, args);
        log.info("🚀 Camunda 7 Spring Boot Application started successfully!");
        log.info("📊 Camunda Webapp: http://localhost:8080/");
        log.info("🗄️ H2 Console: http://localhost:8080/h2-console");
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("✅ Application is ready and Camunda Event Subscribers are active!");
    }
}


