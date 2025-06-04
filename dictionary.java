// ============================================================================
// C# SIDE - Data Transfer Objects and Serialization
// ============================================================================

using System.Text.Json;
using System.Text.Json.Serialization;
using Newtonsoft.Json;

namespace WorkflowManagement.DTOs
{
    // Base class for complex dictionary values
    public abstract class WorkflowVariable
    {
        [JsonPropertyName("type")]
        public abstract string Type { get; }
        
        [JsonPropertyName("value")]
        public abstract object Value { get; set; }
    }

    // Strongly-typed variable classes
    public class StringVariable : WorkflowVariable
    {
        public override string Type => "string";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public StringVariable(string value) => Value = value;
    }

    public class NumberVariable : WorkflowVariable
    {
        public override string Type => "number";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public NumberVariable(decimal value) => Value = value;
    }

    public class BooleanVariable : WorkflowVariable
    {
        public override string Type => "boolean";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public BooleanVariable(bool value) => Value = value;
    }

    public class DateVariable : WorkflowVariable
    {
        public override string Type => "date";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public DateVariable(DateTime value) => Value = value.ToString("yyyy-MM-ddTHH:mm:ss.fffZ");
    }

    public class ListVariable : WorkflowVariable
    {
        public override string Type => "list";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public ListVariable(IEnumerable<object> value) => Value = value.ToList();
    }

    public class ObjectVariable : WorkflowVariable
    {
        public override string Type => "object";
        
        [JsonPropertyName("value")]
        public override object Value { get; set; }
        
        public ObjectVariable(object value) => Value = value;
    }

    // Enhanced workflow request with typed variables
    public class WorkflowRequest
    {
        [JsonPropertyName("bpmnXml")]
        public string BpmnXml { get; set; }

        [JsonPropertyName("processName")]
        public string ProcessName { get; set; }

        [JsonPropertyName("processKey")]
        public string ProcessKey { get; set; }

        // Simple variables (primitive types)
        [JsonPropertyName("variables")]
        public Dictionary<string, object> Variables { get; set; } = new();

        // Typed variables with metadata
        [JsonPropertyName("typedVariables")]
        public Dictionary<string, WorkflowVariable> TypedVariables { get; set; } = new();

        // Complex nested objects
        [JsonPropertyName("complexData")]
        public Dictionary<string, object> ComplexData { get; set; } = new();

        [JsonPropertyName("businessKey")]
        public string BusinessKey { get; set; }
    }

    // Specific DTOs for complex scenarios
    public class EmployeeData
    {
        [JsonPropertyName("employeeId")]
        public string EmployeeId { get; set; }

        [JsonPropertyName("personalInfo")]
        public PersonalInfo PersonalInfo { get; set; }

        [JsonPropertyName("jobDetails")]
        public JobDetails JobDetails { get; set; }

        [JsonPropertyName("permissions")]
        public List<Permission> Permissions { get; set; } = new();

        [JsonPropertyName("metadata")]
        public Dictionary<string, object> Metadata { get; set; } = new();
    }

    public class PersonalInfo
    {
        [JsonPropertyName("firstName")]
        public string FirstName { get; set; }

        [JsonPropertyName("lastName")]
        public string LastName { get; set; }

        [JsonPropertyName("email")]
        public string Email { get; set; }

        [JsonPropertyName("dateOfBirth")]
        public DateTime? DateOfBirth { get; set; }

        [JsonPropertyName("address")]
        public Address Address { get; set; }
    }

    public class Address
    {
        [JsonPropertyName("street")]
        public string Street { get; set; }

        [JsonPropertyName("city")]
        public string City { get; set; }

        [JsonPropertyName("state")]
        public string State { get; set; }

        [JsonPropertyName("zipCode")]
        public string ZipCode { get; set; }

        [JsonPropertyName("country")]
        public string Country { get; set; }
    }

    public class JobDetails
    {
        [JsonPropertyName("position")]
        public string Position { get; set; }

        [JsonPropertyName("department")]
        public string Department { get; set; }

        [JsonPropertyName("salary")]
        public decimal Salary { get; set; }

        [JsonPropertyName("startDate")]
        public DateTime StartDate { get; set; }

        [JsonPropertyName("manager")]
        public string Manager { get; set; }

        [JsonPropertyName("benefits")]
        public Dictionary<string, object> Benefits { get; set; } = new();
    }

    public class Permission
    {
        [JsonPropertyName("system")]
        public string System { get; set; }

        [JsonPropertyName("role")]
        public string Role { get; set; }

        [JsonPropertyName("permissions")]
        public List<string> Permissions { get; set; } = new();

        [JsonPropertyName("expiryDate")]
        public DateTime? ExpiryDate { get; set; }
    }

    // Financial data example
    public class PurchaseOrderData
    {
        [JsonPropertyName("poNumber")]
        public string PoNumber { get; set; }

        [JsonPropertyName("vendor")]
        public VendorInfo Vendor { get; set; }

        [JsonPropertyName("items")]
        public List<PurchaseItem> Items { get; set; } = new();

        [JsonPropertyName("totals")]
        public OrderTotals Totals { get; set; }

        [JsonPropertyName("approvals")]
        public Dictionary<string, ApprovalInfo> Approvals { get; set; } = new();

        [JsonPropertyName("customFields")]
        public Dictionary<string, object> CustomFields { get; set; } = new();
    }

    public class VendorInfo
    {
        [JsonPropertyName("vendorId")]
        public string VendorId { get; set; }

        [JsonPropertyName("name")]
        public string Name { get; set; }

        [JsonPropertyName("contactInfo")]
        public Dictionary<string, string> ContactInfo { get; set; } = new();

        [JsonPropertyName("paymentTerms")]
        public string PaymentTerms { get; set; }
    }

    public class PurchaseItem
    {
        [JsonPropertyName("itemCode")]
        public string ItemCode { get; set; }

        [JsonPropertyName("description")]
        public string Description { get; set; }

        [JsonPropertyName("quantity")]
        public int Quantity { get; set; }

        [JsonPropertyName("unitPrice")]
        public decimal UnitPrice { get; set; }

        [JsonPropertyName("totalPrice")]
        public decimal TotalPrice { get; set; }

        [JsonPropertyName("specifications")]
        public Dictionary<string, object> Specifications { get; set; } = new();
    }

    public class OrderTotals
    {
        [JsonPropertyName("subtotal")]
        public decimal Subtotal { get; set; }

        [JsonPropertyName("tax")]
        public decimal Tax { get; set; }

        [JsonPropertyName("shipping")]
        public decimal Shipping { get; set; }

        [JsonPropertyName("total")]
        public decimal Total { get; set; }

        [JsonPropertyName("currency")]
        public string Currency { get; set; } = "USD";
    }

    public class ApprovalInfo
    {
        [JsonPropertyName("approver")]
        public string Approver { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; }

        [JsonPropertyName("timestamp")]
        public DateTime? Timestamp { get; set; }

        [JsonPropertyName("comments")]
        public string Comments { get; set; }

        [JsonPropertyName("metadata")]
        public Dictionary<string, object> Metadata { get; set; } = new();
    }
}

namespace WorkflowManagement.Services
{
    // Service for creating and managing workflow requests
    public class WorkflowRequestBuilder
    {
        private readonly WorkflowRequest _request;

        public WorkflowRequestBuilder()
        {
            _request = new WorkflowRequest();
        }

        // Method to add simple variables
        public WorkflowRequestBuilder AddVariable(string key, object value)
        {
            _request.Variables[key] = value;
            return this;
        }

        // Method to add typed variables with metadata
        public WorkflowRequestBuilder AddTypedVariable(string key, WorkflowVariable variable)
        {
            _request.TypedVariables[key] = variable;
            return this;
        }

        // Method to add complex objects
        public WorkflowRequestBuilder AddComplexData(string key, object data)
        {
            _request.ComplexData[key] = data;
            return this;
        }

        // Batch add variables from existing dictionary
        public WorkflowRequestBuilder AddVariables(Dictionary<string, object> variables)
        {
            foreach (var kvp in variables)
            {
                _request.Variables[kvp.Key] = kvp.Value;
            }
            return this;
        }

        // Helper methods for common types
        public WorkflowRequestBuilder AddString(string key, string value)
        {
            return AddTypedVariable(key, new StringVariable(value));
        }

        public WorkflowRequestBuilder AddNumber(string key, decimal value)
        {
            return AddTypedVariable(key, new NumberVariable(value));
        }

        public WorkflowRequestBuilder AddBoolean(string key, bool value)
        {
            return AddTypedVariable(key, new BooleanVariable(value));
        }

        public WorkflowRequestBuilder AddDate(string key, DateTime value)
        {
            return AddTypedVariable(key, new DateVariable(value));
        }

        public WorkflowRequestBuilder AddList(string key, IEnumerable<object> value)
        {
            return AddTypedVariable(key, new ListVariable(value));
        }

        public WorkflowRequestBuilder AddObject(string key, object value)
        {
            return AddTypedVariable(key, new ObjectVariable(value));
        }

        public WorkflowRequest Build()
        {
            return _request;
        }
    }

    // Example service methods for common scenarios
    public class WorkflowExampleService
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly JsonSerializerOptions _jsonOptions;

        public WorkflowExampleService(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
            _jsonOptions = new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
                WriteIndented = true
            };
        }

        // Example 1: Employee onboarding with complex nested data
        public async Task<string> CreateEmployeeOnboardingWorkflow(EmployeeData employee)
        {
            var request = new WorkflowRequestBuilder()
                .AddVariable("processType", "employee-onboarding")
                .AddVariable("priority", 90)
                .AddVariable("urgent", true)
                .AddString("employeeId", employee.EmployeeId)
                .AddString("department", employee.JobDetails.Department)
                .AddNumber("salary", employee.JobDetails.Salary)
                .AddDate("startDate", employee.JobDetails.StartDate)
                .AddBoolean("backgroundCheckRequired", true)
                .AddList("requiredSystems", new List<string> { "email", "hr", "payroll" })
                .AddComplexData("employeeData", employee)
                .AddComplexData("onboardingChecklist", new Dictionary<string, object>
                {
                    ["documentsCollected"] = false,
                    ["workspaceSetup"] = false,
                    ["systemAccessGranted"] = false,
                    ["orientationScheduled"] = false,
                    ["managerMeeting"] = false
                })
                .Build();

            request.BpmnXml = GetEmployeeOnboardingBpmn();
            request.ProcessName = "Employee Onboarding Process";
            request.ProcessKey = "employee-onboarding";
            request.BusinessKey = $"EMP-{employee.EmployeeId}-{DateTime.Now:yyyyMMdd}";

            return await SendWorkflowRequest(request);
        }

        // Example 2: Purchase order approval with financial data
        public async Task<string> CreatePurchaseOrderWorkflow(PurchaseOrderData purchaseOrder)
        {
            var approvalThresholds = new Dictionary<string, object>
            {
                ["manager"] = 10000m,
                ["director"] = 50000m,
                ["cfo"] = 100000m,
                ["ceo"] = 500000m
            };

            var request = new WorkflowRequestBuilder()
                .AddVariable("processType", "purchase-order")
                .AddString("poNumber", purchaseOrder.PoNumber)
                .AddString("vendorName", purchaseOrder.Vendor.Name)
                .AddNumber("totalAmount", purchaseOrder.Totals.Total)
                .AddString("currency", purchaseOrder.Totals.Currency)
                .AddBoolean("rushOrder", purchaseOrder.CustomFields.ContainsKey("urgent") && 
                           (bool)purchaseOrder.CustomFields["urgent"])
                .AddList("itemCodes", purchaseOrder.Items.Select(i => i.ItemCode).ToList())
                .AddObject("approvalThresholds", approvalThresholds)
                .AddComplexData("purchaseOrder", purchaseOrder)
                .AddComplexData("approvalMatrix", GetApprovalMatrix(purchaseOrder.Totals.Total))
                .Build();

            request.BpmnXml = GetPurchaseOrderBpmn();
            request.ProcessName = "Purchase Order Approval";
            request.ProcessKey = "purchase-order-approval";
            request.BusinessKey = purchaseOrder.PoNumber;

            return await SendWorkflowRequest(request);
        }

        // Example 3: Document approval with metadata
        public async Task<string> CreateDocumentApprovalWorkflow(
            string documentId, 
            string documentType, 
            Dictionary<string, object> documentMetadata)
        {
            var request = new WorkflowRequestBuilder()
                .AddVariable("processType", "document-approval")
                .AddString("documentId", documentId)
                .AddString("documentType", documentType)
                .AddDate("submissionDate", DateTime.Now)
                .AddNumber("documentVersion", 1.0m)
                .AddBoolean("confidential", documentMetadata.ContainsKey("confidential") && 
                           (bool)documentMetadata["confidential"])
                .AddList("reviewers", documentMetadata.ContainsKey("reviewers") ? 
                         (List<string>)documentMetadata["reviewers"] : new List<string>())
                .AddComplexData("documentMetadata", documentMetadata)
                .AddComplexData("approvalSettings", new Dictionary<string, object>
                {
                    ["maxApprovalDays"] = 5,
                    ["escalationEnabled"] = true,
                    ["notifyOnCompletion"] = true,
                    ["requireAllApprovers"] = documentType == "contract"
                })
                .Build();

            request.BpmnXml = GetDocumentApprovalBpmn();
            request.ProcessName = "Document Approval Process";
            request.ProcessKey = "document-approval";
            request.BusinessKey = $"DOC-{documentId}-{DateTime.Now:yyyyMMdd}";

            return await SendWorkflowRequest(request);
        }

        // Generic method to send workflow request to Java service
        private async Task<string> SendWorkflowRequest(WorkflowRequest request)
        {
            try
            {
                var httpClient = _httpClientFactory.CreateClient();
                var json = JsonSerializer.Serialize(request, _jsonOptions);
                
                // Log the JSON for debugging
                Console.WriteLine("Sending JSON to Java service:");
                Console.WriteLine(json);
                
                var content = new StringContent(json, Encoding.UTF8, "application/json");
                var response = await httpClient.PostAsync("http://java-service/api/workflow/deploy-and-start", content);
                
                var responseContent = await response.Content.ReadAsStringAsync();
                
                if (response.IsSuccessStatusCode)
                {
                    return responseContent;
                }
                else
                {
                    throw new Exception($"Java service returned error: {responseContent}");
                }
            }
            catch (Exception ex)
            {
                throw new Exception($"Error calling Java service: {ex.Message}", ex);
            }
        }

        // Helper method to determine approval matrix based on amount
        private Dictionary<string, object> GetApprovalMatrix(decimal amount)
        {
            var matrix = new Dictionary<string, object>();
            
            if (amount < 1000m)
            {
                matrix["approvers"] = new List<string> { "supervisor" };
                matrix["approvalLevel"] = "low";
            }
            else if (amount < 10000m)
            {
                matrix["approvers"] = new List<string> { "manager" };
                matrix["approvalLevel"] = "medium";
            }
            else if (amount < 50000m)
            {
                matrix["approvers"] = new List<string> { "manager", "director" };
                matrix["approvalLevel"] = "high";
            }
            else
            {
                matrix["approvers"] = new List<string> { "director", "cfo", "ceo" };
                matrix["approvalLevel"] = "critical";
            }
            
            matrix["maxDays"] = amount > 50000m ? 3 : 5;
            matrix["requiresJustification"] = amount > 25000m;
            
            return matrix;
        }

        // BPMN XML methods (simplified for example)
        private string GetEmployeeOnboardingBpmn() => "<?xml version=\"1.0\"?>...";
        private string GetPurchaseOrderBpmn() => "<?xml version=\"1.0\"?>...";
        private string GetDocumentApprovalBpmn() => "<?xml version=\"1.0\"?>...";
    }

    // Best practices utility class
    public static class DictionarySerializationHelper
    {
        // Convert C# dictionary to Java-friendly format
        public static Dictionary<string, object> ToJavaFriendlyDictionary(Dictionary<string, object> original)
        {
            var converted = new Dictionary<string, object>();
            
            foreach (var kvp in original)
            {
                converted[kvp.Key] = ConvertValue(kvp.Value);
            }
            
            return converted;
        }

        private static object ConvertValue(object value)
        {
            return value switch
            {
                null => null,
                DateTime dt => dt.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
                decimal dec => dec,
                double dbl => dbl,
                float flt => (double)flt,
                Dictionary<string, object> dict => ToJavaFriendlyDictionary(dict),
                IDictionary<string, object> idict => ToJavaFriendlyDictionary(new Dictionary<string, object>(idict)),
                IEnumerable<object> enumerable => enumerable.Select(ConvertValue).ToList(),
                _ => value
            };
        }

        // Validate dictionary for JSON serialization
        public static List<string> ValidateForSerialization(Dictionary<string, object> dictionary)
        {
            var errors = new List<string>();
            
            foreach (var kvp in dictionary)
            {
                if (string.IsNullOrWhiteSpace(kvp.Key))
                {
                    errors.Add("Dictionary contains empty or null key");
                }
                
                if (!IsSerializable(kvp.Value))
                {
                    errors.Add($"Value for key '{kvp.Key}' is not serializable");
                }
            }
            
            return errors;
        }

        private static bool IsSerializable(object value)
        {
            if (value == null) return true;
            
            var type = value.GetType();
            
            // Check for basic serializable types
            if (type.IsPrimitive || type == typeof(string) || type == typeof(DateTime) || 
                type == typeof(decimal) || type == typeof(Guid))
            {
                return true;
            }
            
            // Check for collections
            if (value is IDictionary<string, object> dict)
            {
                return dict.All(kvp => IsSerializable(kvp.Value));
            }
            
            if (value is IEnumerable<object> enumerable)
            {
                return enumerable.All(IsSerializable);
            }
            
            // Check if object has JsonPropertyName attributes or is a simple POCO
            return type.GetProperties().All(prop => 
                prop.CanRead && IsSerializable(prop.GetValue(value)));
        }
    }
}

// ============================================================================
// JAVA SIDE - Receiving and Processing Dictionaries
// ============================================================================

/*
// Java DTOs for receiving C# data

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRequest {
    private String bpmnXml;
    private String processName;
    private String processKey;
    private Map<String, Object> variables = new HashMap<>();
    private Map<String, TypedVariable> typedVariables = new HashMap<>();
    private Map<String, Object> complexData = new HashMap<>();
    private String businessKey;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypedVariable {
    private String type;
    private Object value;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeData {
    private String employeeId;
    private PersonalInfo personalInfo;
    private JobDetails jobDetails;
    private List<Permission> permissions = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfo {
    private String firstName;
    private String lastName;
    private String email;
    private String dateOfBirth;
    private Address address;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDetails {
    private String position;
    private String department;
    private BigDecimal salary;
    private String startDate;
    private String manager;
    private Map<String, Object> benefits = new HashMap<>();
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private String system;
    private String role;
    private List<String> permissions = new ArrayList<>();
    private String expiryDate;
}

// Enhanced Java service for processing complex dictionaries
@Service
@Transactional
@Slf4j
public class EnhancedWorkflowService {
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public WorkflowDeploymentResponse deployAndStartProcess(WorkflowRequest request) {
        WorkflowDeploymentResponse response = new WorkflowDeploymentResponse();
        
        try {
            // Process and validate the request
            Map<String, Object> processedVariables = processVariables(request);
            
            // Deploy the BPMN process
            Deployment deployment = repositoryService.createDeployment()
                .name(request.getProcessName())
                .addString(request.getProcessKey() + ".bpmn", request.getBpmnXml())
                .enableDuplicateFiltering(false)
                .deploy();
            
            response.setDeploymentId(deployment.getId());
            log.info("Process deployed with ID: {}", deployment.getId());
            
            // Get process definition
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
            
            response.setProcessDefinitionId(processDefinition.getId());
            
            // Start process instance with processed variables
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(
                processDefinition.getId(),
                request.getBusinessKey(),
                processedVariables
            );
            
            response.setProcessInstanceId(processInstance.getId());
            response.setProcessVariables(processedVariables);
            response.setSuccess(true);
            
            log.info("Process instance started with ID: {}", processInstance.getId());
            
        } catch (Exception e) {
            log.error("Error processing workflow request", e);
            response.setSuccess(false);
            response.setErrorMessage("Failed to deploy and start process: " + e.getMessage());
        }
        
        return response;
    }
    
    private Map<String, Object> processVariables(WorkflowRequest request) {
        Map<String, Object> processedVariables = new HashMap<>();
        
        // Process simple variables
        if (request.getVariables() != null) {
            processedVariables.putAll(request.getVariables());
        }
        
        // Process typed variables
        if (request.getTypedVariables() != null) {
            for (Map.Entry<String, TypedVariable> entry : request.getTypedVariables().entrySet()) {
                Object convertedValue = convertTypedVariable(entry.getValue());
                processedVariables.put(entry.getKey(), convertedValue);
            }
        }
        
        // Process complex data
        if (request.getComplexData() != null) {
            for (Map.Entry<String, Object> entry : request.getComplexData().entrySet()) {
                Object processedData = processComplexObject(entry.getValue());
                processedVariables.put(entry.getKey(), processedData);
            }
        }
        
        return processedVariables;
    }
    
    private Object convertTypedVariable(TypedVariable typedVar) {
        if (typedVar == null || typedVar.getValue() == null) {
            return null;
        }
        
        switch (typedVar.getType().toLowerCase()) {
            case "string":
                return typedVar.getValue().toString();
            case "number":
                if (typedVar.getValue() instanceof Number) {
                    return ((Number) typedVar.getValue()).doubleValue();
                }
                return Double.parseDouble(typedVar.getValue().toString());
            case "boolean":
                if (typedVar.getValue() instanceof Boolean) {
                    return (Boolean) typedVar.getValue();
                }
                return Boolean.parseBoolean(typedVar.getValue().toString());
            case "date":
                try {
                    return LocalDateTime.parse(typedVar.getValue().toString(), 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    log.warn("Failed to parse date: {}", typedVar.getValue());
                    return typedVar.getValue().toString();
                }
            case "list":
                if (typedVar.getValue() instanceof List) {
                    return (List<?>) typedVar.getValue();
                }
                return Arrays.asList(typedVar.getValue());
            case "object":
            default:
                return processComplexObject(typedVar.getValue());
        }
    }
    
    private Object processComplexObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, Object> processedMap = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                processedMap.put(entry.getKey(), processComplexObject(entry.getValue()));
            }
            
            return processedMap;
        }
        
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return list.stream()
                .map(this::processComplexObject)
                .collect(Collectors.toList());
        }
        
        // For complex POJOs, try to convert via ObjectMapper
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to convert complex object, returning as-is: {}", e.getMessage());
            return obj;
        }
    }
    
    // Helper method to extract specific data types from variables
    public EmployeeData extractEmployeeData(Map<String, Object> variables) {
        try {
            Object employeeDataObj = variables.get("employeeData");
            if (employeeDataObj != null) {
                String json = objectMapper.writeValueAsString(employeeDataObj);
                return objectMapper.readValue(json, EmployeeData.class);
            }
        } catch (Exception e) {
            log.error("Failed to extract employee data", e);
        }
        return null;
    }
    
    // Method to safely get nested values from complex variables
    @SuppressWarnings("unchecked")
    public <T> T getNestedValue(Map<String, Object> variables, String path, Class<T> type) {
        String[] parts = path.split("\\.");
        Object current = variables;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        if (current != null && type.isAssignableFrom(current.getClass())) {
            return type.cast(current);
        }
        
        return null;
    }
}

// Utility class for variable validation and conversion
@Component
public class VariableProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(VariableProcessor.class);
    
    public boolean validateVariables(Map<String, Object> variables) {
        if (variables == null) {
            return true; // Empty variables are valid
        }
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (!isValidVariableName(entry.getKey())) {
                log.warn("Invalid variable name: {}", entry.getKey());
                return false;
            }
            
            if (!isSerializableValue(entry.getValue())) {
                log.warn("Non-serializable value for variable: {}", entry.getKey());
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isValidVariableName(String name) {
        return name != null && 
               !name.trim().isEmpty() && 
               name.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }
    
    private boolean isSerializableValue(Object value) {
        if (value == null) {
