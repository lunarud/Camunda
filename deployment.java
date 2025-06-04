// Java Spring Boot Application - Main Application Class
@SpringBootApplication
@EnableJpaRepositories
public class CamundaWorkflowApplication extends SpringBootProcessApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CamundaWorkflowApplication.class, args);
    }
    
    @Override
    public String getReference() {
        return "camunda-workflow-app";
    }
}

// DTO for receiving deployment requests
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowDeploymentRequest {
    private String bpmnXml;
    private String processName;
    private String processKey;
    private Map<String, Object> variables;
}

// DTO for deployment response
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowDeploymentResponse {
    private String deploymentId;
    private String processInstanceId;
    private String processDefinitionId;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> processVariables;
}

// REST Controller for handling workflow operations
@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
@Slf4j
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @PostMapping("/deploy-and-start")
    public ResponseEntity<WorkflowDeploymentResponse> deployAndStartProcess(
            @RequestBody WorkflowDeploymentRequest request) {
        
        try {
            log.info("Received deployment request for process: {}", request.getProcessName());
            
            WorkflowDeploymentResponse response = workflowService.deployAndStartProcess(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error deploying and starting process", e);
            WorkflowDeploymentResponse errorResponse = new WorkflowDeploymentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/process-instance/{id}")
    public ResponseEntity<Map<String, Object>> getProcessInstanceDetails(@PathVariable String id) {
        try {
            Map<String, Object> details = workflowService.getProcessInstanceDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("Error retrieving process instance details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/active-tasks/{processInstanceId}")
    public ResponseEntity<List<Map<String, Object>>> getActiveTasks(@PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> tasks = workflowService.getActiveTasks(processInstanceId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Error retrieving active tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// Service class for workflow operations
@Service
@Transactional
@Slf4j
public class WorkflowService {
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private HistoryService historyService;
    
    public WorkflowDeploymentResponse deployAndStartProcess(WorkflowDeploymentRequest request) {
        WorkflowDeploymentResponse response = new WorkflowDeploymentResponse();
        
        try {
            // Validate BPMN XML
            if (request.getBpmnXml() == null || request.getBpmnXml().trim().isEmpty()) {
                response.setSuccess(false);
                response.setErrorMessage("BPMN XML cannot be empty");
                return response;
            }
            
            // Deploy the BPMN process
            Deployment deployment = repositoryService.createDeployment()
                .name(request.getProcessName())
                .addString(request.getProcessKey() + ".bpmn", request.getBpmnXml())
                .enableDuplicateFiltering(false)
                .deploy();
            
            log.info("Process deployed successfully with deployment ID: {}", deployment.getId());
            response.setDeploymentId(deployment.getId());
            
            // Get the process definition
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
            
            if (processDefinition == null) {
                response.setSuccess(false);
                response.setErrorMessage("Failed to retrieve process definition after deployment");
                return response;
            }
            
            response.setProcessDefinitionId(processDefinition.getId());
            log.info("Process definition ID: {}", processDefinition.getId());
            
            // Start process instance
            Map<String, Object> variables = request.getVariables() != null ? 
                request.getVariables() : new HashMap<>();
            
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(
                processDefinition.getId(), variables);
            
            log.info("Process instance started with ID: {}", processInstance.getId());
            response.setProcessInstanceId(processInstance.getId());
            response.setProcessVariables(variables);
            response.setSuccess(true);
            
        } catch (Exception e) {
            log.error("Error deploying and starting process", e);
            response.setSuccess(false);
            response.setErrorMessage("Deployment failed: " + e.getMessage());
        }
        
        return response;
    }
    
    public Map<String, Object> getProcessInstanceDetails(String processInstanceId) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Get runtime process instance
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
            
            if (processInstance != null) {
                details.put("id", processInstance.getId());
                details.put("processDefinitionId", processInstance.getProcessDefinitionId());
                details.put("businessKey", processInstance.getBusinessKey());
                details.put("isActive", true);
                details.put("isSuspended", processInstance.isSuspended());
            } else {
                // Check historical process instance
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
                
                if (historicInstance != null) {
                    details.put("id", historicInstance.getId());
                    details.put("processDefinitionId", historicInstance.getProcessDefinitionId());
                    details.put("businessKey", historicInstance.getBusinessKey());
                    details.put("isActive", false);
                    details.put("startTime", historicInstance.getStartTime());
                    details.put("endTime", historicInstance.getEndTime());
                    details.put("durationInMillis", historicInstance.getDurationInMillis());
                }
            }
            
            // Get process variables
            Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
            details.put("variables", variables);
            
        } catch (Exception e) {
            log.error("Error retrieving process instance details", e);
            details.put("error", e.getMessage());
        }
        
        return details;
    }
    
    public List<Map<String, Object>> getActiveTasks(String processInstanceId) {
        List<Map<String, Object>> taskList = new ArrayList<>();
        
        try {
            List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
            
            for (Task task : tasks) {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("id", task.getId());
                taskInfo.put("name", task.getName());
                taskInfo.put("assignee", task.getAssignee());
                taskInfo.put("createTime", task.getCreateTime());
                taskInfo.put("dueDate", task.getDueDate());
                taskInfo.put("priority", task.getPriority());
                taskInfo.put("processInstanceId", task.getProcessInstanceId());
                taskList.add(taskInfo);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving active tasks", e);
        }
        
        return taskList;
    }
}

// Configuration class for Camunda
@Configuration
public class CamundaConfiguration {
    
    @Bean
    @Primary
    public ProcessEngineConfiguration processEngineConfiguration() {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDatabaseSchemaUpdate("true");
        config.setJobExecutorActivate(true);
        config.setHistory("full");
        return config;
    }
    
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }
}

// C# Controller Class (for the C# application)
/*
[ApiController]
[Route("api/[controller]")]
public class WorkflowController : ControllerBase
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _configuration;
    private readonly ILogger<WorkflowController> _logger;

    public WorkflowController(IHttpClientFactory httpClientFactory, 
                            IConfiguration configuration,
                            ILogger<WorkflowController> logger)
    {
        _httpClientFactory = httpClientFactory;
        _configuration = configuration;
        _logger = logger;
    }

    [HttpPost("deploy")]
    public async Task<IActionResult> DeployWorkflow([FromBody] WorkflowDeploymentDto request)
    {
        try
        {
            var javaServiceUrl = _configuration["JavaService:BaseUrl"];
            var httpClient = _httpClientFactory.CreateClient();
            
            var javaRequest = new
            {
                bpmnXml = request.BpmnXml,
                processName = request.ProcessName,
                processKey = request.ProcessKey,
                variables = request.Variables ?? new Dictionary<string, object>()
            };

            var json = JsonSerializer.Serialize(javaRequest);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await httpClient.PostAsync($"{javaServiceUrl}/api/workflow/deploy-and-start", content);
            var responseContent = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var result = JsonSerializer.Deserialize<WorkflowDeploymentResponse>(responseContent);
                return Ok(result);
            }
            else
            {
                _logger.LogError($"Java service returned error: {responseContent}");
                return BadRequest($"Failed to deploy workflow: {responseContent}");
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error calling Java service");
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }

    [HttpGet("process/{processInstanceId}")]
    public async Task<IActionResult> GetProcessDetails(string processInstanceId)
    {
        try
        {
            var javaServiceUrl = _configuration["JavaService:BaseUrl"];
            var httpClient = _httpClientFactory.CreateClient();
            
            var response = await httpClient.GetAsync($"{javaServiceUrl}/api/workflow/process-instance/{processInstanceId}");
            var responseContent = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                return Ok(responseContent);
            }
            else
            {
                return BadRequest($"Failed to get process details: {responseContent}");
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting process details");
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }
}

// C# DTOs
public class WorkflowDeploymentDto
{
    public string BpmnXml { get; set; }
    public string ProcessName { get; set; }
    public string ProcessKey { get; set; }
    public Dictionary<string, object> Variables { get; set; }
}

public class WorkflowDeploymentResponse
{
    public string DeploymentId { get; set; }
    public string ProcessInstanceId { get; set; }
    public string ProcessDefinitionId { get; set; }
    public bool Success { get; set; }
    public string ErrorMessage { get; set; }
    public Dictionary<string, object> ProcessVariables { get; set; }
}
*/

// TypeScript Service for Angular (workflow.service.ts)
/*
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkflowDeploymentRequest {
  bpmnXml: string;
  processName: string;
  processKey: string;
  variables?: { [key: string]: any };
}

export interface WorkflowDeploymentResponse {
  deploymentId: string;
  processInstanceId: string;
  processDefinitionId: string;
  success: boolean;
  errorMessage?: string;
  processVariables?: { [key: string]: any };
}

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  private baseUrl = 'https://your-csharp-api-url/api/workflow';

  constructor(private http: HttpClient) { }

  deployWorkflow(request: WorkflowDeploymentRequest): Observable<WorkflowDeploymentResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.post<WorkflowDeploymentResponse>(`${this.baseUrl}/deploy`, request, { headers });
  }

  getProcessDetails(processInstanceId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/process/${processInstanceId}`);
  }
}

// Angular Component Example (workflow-deployment.component.ts)
import { Component } from '@angular/core';
import { WorkflowService, WorkflowDeploymentRequest } from './workflow.service';

@Component({
  selector: 'app-workflow-deployment',
  template: `
    <div class="workflow-deployment">
      <h2>Deploy BPMN Workflow</h2>
      
      <form (ngSubmit)="deployWorkflow()">
        <div class="form-group">
          <label for="processName">Process Name:</label>
          <input type="text" id="processName" [(ngModel)]="processName" required>
        </div>
        
        <div class="form-group">
          <label for="processKey">Process Key:</label>
          <input type="text" id="processKey" [(ngModel)]="processKey" required>
        </div>
        
        <div class="form-group">
          <label for="bpmnXml">BPMN XML:</label>
          <textarea id="bpmnXml" [(ngModel)]="bpmnXml" rows="10" cols="80" required></textarea>
        </div>
        
        <button type="submit" [disabled]="isDeploying">
          {{ isDeploying ? 'Deploying...' : 'Deploy Workflow' }}
        </button>
      </form>
      
      <div *ngIf="deploymentResult" class="result">
        <h3>Deployment Result</h3>
        <p><strong>Success:</strong> {{ deploymentResult.success }}</p>
        <p *ngIf="deploymentResult.success">
          <strong>Process Instance ID:</strong> {{ deploymentResult.processInstanceId }}
        </p>
        <p *ngIf="!deploymentResult.success">
          <strong>Error:</strong> {{ deploymentResult.errorMessage }}
        </p>
      </div>
    </div>
  `
})
export class WorkflowDeploymentComponent {
  processName = '';
  processKey = '';
  bpmnXml = '';
  isDeploying = false;
  deploymentResult: WorkflowDeploymentResponse | null = null;

  constructor(private workflowService: WorkflowService) { }

  deployWorkflow() {
    if (!this.processName || !this.processKey || !this.bpmnXml) {
      alert('Please fill in all required fields');
      return;
    }

    this.isDeploying = true;
    this.deploymentResult = null;

    const request: WorkflowDeploymentRequest = {
      bpmnXml: this.bpmnXml,
      processName: this.processName,
      processKey: this.processKey,
      variables: {}
    };

    this.workflowService.deployWorkflow(request).subscribe({
      next: (response) => {
        this.deploymentResult = response;
        this.isDeploying = false;
      },
      error: (error) => {
        console.error('Deployment failed:', error);
        this.deploymentResult = {
          deploymentId: '',
          processInstanceId: '',
          processDefinitionId: '',
          success: false,
          errorMessage: error.message || 'Unknown error occurred'
        };
        this.isDeploying = false;
      }
    });
  }
}
*/
