using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using Newtonsoft.Json;

namespace WorkflowManagement.DTOs
{
    /// <summary>
    /// Data Transfer Object for workflow deployment requests
    /// </summary>
    public class WorkflowDeploymentDto
    {
        /// <summary>
        /// The BPMN XML content as a string
        /// </summary>
        [Required(ErrorMessage = "BPMN XML is required")]
        [MinLength(1, ErrorMessage = "BPMN XML cannot be empty")]
        [JsonPropertyName("bpmnXml")]
        public string BpmnXml { get; set; }

        /// <summary>
        /// Human-readable name for the process
        /// </summary>
        [Required(ErrorMessage = "Process name is required")]
        [StringLength(100, MinimumLength = 3, ErrorMessage = "Process name must be between 3 and 100 characters")]
        [JsonPropertyName("processName")]
        public string ProcessName { get; set; }

        /// <summary>
        /// Unique key identifier for the process (used in BPMN)
        /// </summary>
        [Required(ErrorMessage = "Process key is required")]
        [RegularExpression(@"^[a-zA-Z][a-zA-Z0-9_-]*$", ErrorMessage = "Process key must start with a letter and contain only letters, numbers, underscores, and hyphens")]
        [JsonPropertyName("processKey")]
        public string ProcessKey { get; set; }

        /// <summary>
        /// Initial process variables to set when starting the process instance
        /// </summary>
        [JsonPropertyName("variables")]
        public Dictionary<string, object> Variables { get; set; }

        /// <summary>
        /// Optional business key for the process instance
        /// </summary>
        [JsonPropertyName("businessKey")]
        public string? BusinessKey { get; set; }

        /// <summary>
        /// Optional tenant ID for multi-tenancy support
        /// </summary>
        [JsonPropertyName("tenantId")]
        public string? TenantId { get; set; }

        /// <summary>
        /// Whether to start the process instance immediately after deployment
        /// </summary>
        [JsonPropertyName("startImmediately")]
        public bool StartImmediately { get; set; } = true;

        /// <summary>
        /// Optional description of the workflow
        /// </summary>
        [StringLength(500, ErrorMessage = "Description cannot exceed 500 characters")]
        [JsonPropertyName("description")]
        public string? Description { get; set; }

        /// <summary>
        /// Tags for categorizing the workflow
        /// </summary>
        [JsonPropertyName("tags")]
        public List<string>? Tags { get; set; }

        /// <summary>
        /// Priority level for the process (1-100, where 100 is highest priority)
        /// </summary>
        [Range(1, 100, ErrorMessage = "Priority must be between 1 and 100")]
        [JsonPropertyName("priority")]
        public int Priority { get; set; } = 50;

        /// <summary>
        /// Constructor
        /// </summary>
        public WorkflowDeploymentDto()
        {
            Variables = new Dictionary<string, object>();
            Tags = new List<string>();
        }
    }

    /// <summary>
    /// Extended DTO with additional metadata for complex scenarios
    /// </summary>
    public class ExtendedWorkflowDeploymentDto : WorkflowDeploymentDto
    {
        /// <summary>
        /// Associated form definitions (if using Camunda forms)
        /// </summary>
        [JsonPropertyName("formDefinitions")]
        public List<FormDefinitionDto>? FormDefinitions { get; set; }

        /// <summary>
        /// DMN decision tables associated with this workflow
        /// </summary>
        [JsonPropertyName("decisionTables")]
        public List<DecisionTableDto>? DecisionTables { get; set; }

        /// <summary>
        /// Custom properties for the deployment
        /// </summary>
        [JsonPropertyName("customProperties")]
        public Dictionary<string, string>? CustomProperties { get; set; }

        public ExtendedWorkflowDeploymentDto() : base()
        {
            FormDefinitions = new List<FormDefinitionDto>();
            DecisionTables = new List<DecisionTableDto>();
            CustomProperties = new Dictionary<string, string>();
        }
    }

    /// <summary>
    /// Form definition DTO
    /// </summary>
    public class FormDefinitionDto
    {
        [JsonPropertyName("formKey")]
        public string FormKey { get; set; }

        [JsonPropertyName("formContent")]
        public string FormContent { get; set; }

        [JsonPropertyName("formType")]
        public string FormType { get; set; } // "embedded", "external", "generated"
    }

    /// <summary>
    /// Decision table DTO
    /// </summary>
    public class DecisionTableDto
    {
        [JsonPropertyName("decisionKey")]
        public string DecisionKey { get; set; }

        [JsonPropertyName("dmnContent")]
        public string DmnContent { get; set; }

        [JsonPropertyName("decisionName")]
        public string DecisionName { get; set; }
    }
}

// USAGE EXAMPLES

namespace WorkflowManagement.Examples
{
    public class WorkflowDeploymentExamples
    {
        /// <summary>
        /// Example 1: Basic approval workflow
        /// </summary>
        public static WorkflowDeploymentDto CreateBasicApprovalWorkflow()
        {
            var bpmnXml = @"<?xml version='1.0' encoding='UTF-8'?>
<bpmn:definitions xmlns:bpmn='http://www.omg.org/spec/BPMN/20100524/MODEL' 
                  xmlns:bpmndi='http://www.omg.org/spec/BPMN/20100524/DI' 
                  xmlns:dc='http://www.omg.org/spec/DD/20100524/DC' 
                  xmlns:di='http://www.omg.org/spec/DD/20100524/DI'
                  id='Definitions_1' 
                  targetNamespace='http://bpmn.io/schema/bpmn'>
  <bpmn:process id='approval-process' name='Approval Process' isExecutable='true'>
    <bpmn:startEvent id='StartEvent_1' name='Request Submitted'>
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id='review-task' name='Review Request'>
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id='EndEvent_1' name='Process Complete'>
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id='Flow_1' sourceRef='StartEvent_1' targetRef='review-task' />
    <bpmn:sequenceFlow id='Flow_2' sourceRef='review-task' targetRef='EndEvent_1' />
  </bpmn:process>
</bpmn:definitions>";

            return new WorkflowDeploymentDto
            {
                BpmnXml = bpmnXml,
                ProcessName = "Document Approval Workflow",
                ProcessKey = "approval-process",
                BusinessKey = "DOC-APPROVAL-2024",
                Description = "Standard document approval process with manager review",
                Priority = 75,
                Variables = new Dictionary<string, object>
                {
                    {"requesterId", "user123"},
                    {"documentType", "contract"},
                    {"urgency", "normal"},
                    {"approvalRequired", true},
                    {"maxApprovalDays", 5}
                },
                Tags = new List<string> { "approval", "document", "review" }
            };
        }

        /// <summary>
        /// Example 2: Employee onboarding workflow with complex variables
        /// </summary>
        public static WorkflowDeploymentDto CreateEmployeeOnboardingWorkflow()
        {
            var bpmnXml = @"<?xml version='1.0' encoding='UTF-8'?>
<bpmn:definitions xmlns:bpmn='http://www.omg.org/spec/BPMN/20100524/MODEL' 
                  id='Definitions_1' 
                  targetNamespace='http://bpmn.io/schema/bpmn'>
  <bpmn:process id='employee-onboarding' name='Employee Onboarding' isExecutable='true'>
    <bpmn:startEvent id='StartEvent_1' name='New Employee'>
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id='setup-accounts' name='Setup IT Accounts'>
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id='orientation' name='Employee Orientation'>
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id='EndEvent_1' name='Onboarding Complete'>
      <bpmn:incoming>Flow_3</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id='Flow_1' sourceRef='StartEvent_1' targetRef='setup-accounts' />
    <bpmn:sequenceFlow id='Flow_2' sourceRef='setup-accounts' targetRef='orientation' />
    <bpmn:sequenceFlow id='Flow_3' sourceRef='orientation' targetRef='EndEvent_1' />
  </bpmn:process>
</bpmn:definitions>";

            return new WorkflowDeploymentDto
            {
                BpmnXml = bpmnXml,
                ProcessName = "Employee Onboarding Process",
                ProcessKey = "employee-onboarding",
                BusinessKey = $"ONBOARD-{DateTime.Now:yyyyMMdd}-{Guid.NewGuid().ToString()[..8]}",
                TenantId = "company-hr",
                Description = "Complete onboarding process for new employees including IT setup and orientation",
                Priority = 90,
                Variables = new Dictionary<string, object>
                {
                    {"employeeId", "EMP-2024-001"},
                    {"firstName", "John"},
                    {"lastName", "Doe"},
                    {"email", "john.doe@company.com"},
                    {"department", "Engineering"},
                    {"position", "Software Developer"},
                    {"startDate", DateTime.Now.AddDays(7).ToString("yyyy-MM-dd")},
                    {"manager", "manager@company.com"},
                    {"equipmentNeeded", new List<string> { "laptop", "monitor", "keyboard", "mouse" }},
                    {"accessRights", new Dictionary<string, object>
                    {
                        {"systems", new List<string> { "jira", "confluence", "github" }},
                        {"level", "standard"},
                        {"expiryDate", DateTime.Now.AddYears(1).ToString("yyyy-MM-dd")}
                    }},
                    {"hrChecklist", new Dictionary<string, bool>
                    {
                        {"contractSigned", false},
                        {"photoTaken", false},
                        {"emergencyContactsCollected", false},
                        {"bankDetailsProvided", false}
                    }}
                },
                Tags = new List<string> { "hr", "onboarding", "employee", "setup" }
            };
        }

        /// <summary>
        /// Example 3: Purchase order workflow with financial thresholds
        /// </summary>
        public static WorkflowDeploymentDto CreatePurchaseOrderWorkflow()
        {
            var bpmnXml = @"<?xml version='1.0' encoding='UTF-8'?>
<bpmn:definitions xmlns:bpmn='http://www.omg.org/spec/BPMN/20100524/MODEL' 
                  id='Definitions_1' 
                  targetNamespace='http://bpmn.io/schema/bpmn'>
  <bpmn:process id='purchase-order' name='Purchase Order Process' isExecutable='true'>
    <bpmn:startEvent id='StartEvent_1' name='PO Request'>
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:exclusiveGateway id='Gateway_1' name='Amount Check'>
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:userTask id='manager-approval' name='Manager Approval'>
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id='cfo-approval' name='CFO Approval'>
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id='EndEvent_1' name='PO Processed'>
      <bpmn:incoming>Flow_4</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id='Flow_1' sourceRef='StartEvent_1' targetRef='Gateway_1' />
    <bpmn:sequenceFlow id='Flow_2' sourceRef='Gateway_1' targetRef='manager-approval'>
      <bpmn:conditionExpression>${amount &lt; 10000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id='Flow_3' sourceRef='Gateway_1' targetRef='cfo-approval'>
      <bpmn:conditionExpression>${amount &gt;= 10000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id='Flow_4' sourceRef='manager-approval' targetRef='EndEvent_1' />
    <bpmn:sequenceFlow id='Flow_4b' sourceRef='cfo-approval' targetRef='EndEvent_1' />
  </bpmn:process>
</bpmn:definitions>";

            return new WorkflowDeploymentDto
            {
                BpmnXml = bpmnXml,
                ProcessName = "Purchase Order Approval",
                ProcessKey = "purchase-order",
                BusinessKey = $"PO-{DateTime.Now:yyyyMMdd}-{new Random().Next(1000, 9999)}",
                Description = "Purchase order approval workflow with amount-based routing",
                Priority = 85,
                Variables = new Dictionary<string, object>
                {
                    {"poNumber", "PO-2024-0042"},
                    {"requesterId", "emp123"},
                    {"requesterName", "Alice Johnson"},
                    {"department", "IT"},
                    {"supplier", "Tech Solutions Inc."},
                    {"amount", 15000.00m},
                    {"currency", "USD"},
                    {"description", "Server hardware upgrade"},
                    {"urgency", "high"},
                    {"budgetCode", "IT-CAPEX-2024"},
                    {"deliveryDate", DateTime.Now.AddDays(30).ToString("yyyy-MM-dd")},
                    {"items", new List<Dictionary<string, object>>
                    {
                        new Dictionary<string, object>
                        {
                            {"itemCode", "SRV-001"},
                            {"description", "Dell PowerEdge Server"},
                            {"quantity", 2},
                            {"unitPrice", 5000.00m},
                            {"totalPrice", 10000.00m}
                        },
                        new Dictionary<string, object>
                        {
                            {"itemCode", "MEM-001"},
                            {"description", "64GB RAM Module"},
                            {"quantity", 4},
                            {"unitPrice", 1250.00m},
                            {"totalPrice", 5000.00m}
                        }
                    }},
                    {"approvalThresholds", new Dictionary<string, object>
                    {
                        {"managerLimit", 10000.00m},
                        {"cfoLimit", 50000.00m},
                        {"boardLimit", 100000.00m}
                    }}
                },
                Tags = new List<string> { "procurement", "approval", "purchase-order", "finance" }
            };
        }

        /// <summary>
        /// Example 4: Extended workflow with forms and decision tables
        /// </summary>
        public static ExtendedWorkflowDeploymentDto CreateExtendedWorkflowExample()
        {
            var basicWorkflow = CreateBasicApprovalWorkflow();
            
            return new ExtendedWorkflowDeploymentDto
            {
                BpmnXml = basicWorkflow.BpmnXml,
                ProcessName = basicWorkflow.ProcessName,
                ProcessKey = basicWorkflow.ProcessKey,
                BusinessKey = basicWorkflow.BusinessKey,
                Description = basicWorkflow.Description,
                Priority = basicWorkflow.Priority,
                Variables = basicWorkflow.Variables,
                Tags = basicWorkflow.Tags,
                
                FormDefinitions = new List<FormDefinitionDto>
                {
                    new FormDefinitionDto
                    {
                        FormKey = "approval-form",
                        FormType = "embedded",
                        FormContent = @"{
                            'components': [
                                {
                                    'type': 'textfield',
                                    'key': 'comments',
                                    'label': 'Approval Comments'
                                },
                                {
                                    'type': 'select',
                                    'key': 'decision',
                                    'label': 'Decision',
                                    'values': [
                                        {'label': 'Approve', 'value': 'approve'},
                                        {'label': 'Reject', 'value': 'reject'},
                                        {'label': 'Request Changes', 'value': 'changes'}
                                    ]
                                }
                            ]
                        }"
                    }
                },
                
                DecisionTables = new List<DecisionTableDto>
                {
                    new DecisionTableDto
                    {
                        DecisionKey = "approval-rules",
                        DecisionName = "Approval Business Rules",
                        DmnContent = @"<?xml version='1.0' encoding='UTF-8'?>
<definitions xmlns='http://www.omg.org/spec/DMN/20151101/dmn.xsd' 
             id='approval-rules' name='Approval Rules'>
  <decision id='determine-approver' name='Determine Approver'>
    <decisionTable id='approver-table'>
      <input id='amount' label='Amount'>
        <inputExpression typeRef='double'>
          <text>amount</text>
        </inputExpression>
      </input>
      <output id='approver' label='Approver' typeRef='string'/>
      <rule id='rule1'>
        <inputEntry id='amountRange1'>
          <text>&lt; 1000</text>
        </inputEntry>
        <outputEntry id='approver1'>
          <text>'manager'</text>
        </outputEntry>
      </rule>
      <rule id='rule2'>
        <inputEntry id='amountRange2'>
          <text>&gt;= 1000</text>
        </inputEntry>
        <outputEntry id='approver2'>
          <text>'director'</text>
        </outputEntry>
      </rule>
    </decisionTable>
  </decision>
</definitions>"
                    }
                },
                
                CustomProperties = new Dictionary<string, string>
                {
                    {"version", "1.0"},
                    {"author", "Workflow Team"},
                    {"lastModified", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")},
                    {"environment", "production"},
                    {"slaHours", "24"}
                }
            };
        }

        /// <summary>
        /// Example 5: Minimal workflow for testing
        /// </summary>
        public static WorkflowDeploymentDto CreateMinimalTestWorkflow()
        {
            return new WorkflowDeploymentDto
            {
                BpmnXml = @"<?xml version='1.0' encoding='UTF-8'?>
<bpmn:definitions xmlns:bpmn='http://www.omg.org/spec/BPMN/20100524/MODEL' 
                  id='Definitions_1' 
                  targetNamespace='http://bpmn.io/schema/bpmn'>
  <bpmn:process id='test-process' name='Test Process' isExecutable='true'>
    <bpmn:startEvent id='start'>
      <bpmn:outgoing>flow</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:endEvent id='end'>
      <bpmn:incoming>flow</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id='flow' sourceRef='start' targetRef='end' />
  </bpmn:process>
</bpmn:definitions>",
                ProcessName = "Simple Test Process",
                ProcessKey = "test-process",
                Variables = new Dictionary<string, object>
                {
                    {"testMode", true},
                    {"timestamp", DateTime.Now}
                }
            };
        }
    }
}

// JSON SERIALIZATION EXAMPLES
namespace WorkflowManagement.Serialization
{
    public class SerializationExamples
    {
        /// <summary>
        /// Example of how the DTO would look when serialized to JSON
        /// </summary>
        public static string GetJsonExample()
        {
            var dto = WorkflowDeploymentExamples.CreateBasicApprovalWorkflow();
            return System.Text.Json.JsonSerializer.Serialize(dto, new JsonSerializerOptions 
            { 
                WriteIndented = true,
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            });
        }

        /// <summary>
        /// Expected JSON structure for the basic approval workflow
        /// </summary>
        public static readonly string ExpectedJsonStructure = @"{
  ""bpmnXml"": ""<?xml version='1.0' encoding='UTF-8'?>...[BPMN XML Content]..."",
  ""processName"": ""Document Approval Workflow"",
  ""processKey"": ""approval-process"",
  ""variables"": {
    ""requesterId"": ""user123"",
    ""documentType"": ""contract"",
    ""urgency"": ""normal"",
    ""approvalRequired"": true,
    ""maxApprovalDays"": 5
  },
  ""businessKey"": ""DOC-APPROVAL-2024"",
  ""tenantId"": null,
  ""startImmediately"": true,
  ""description"": ""Standard document approval process with manager review"",
  ""tags"": [""approval"", ""document"", ""review""],
  ""priority"": 75
}";
    }
}
