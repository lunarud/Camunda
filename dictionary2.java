// ============================================================================
// C# SIDE - Serialization and Preparation for Java
// ============================================================================

using System.Text.Json;
using System.Text.Json.Serialization;
using Newtonsoft.Json;
using System.Collections;

namespace DictionaryConversion
{
    // Method 1: Direct JSON Serialization (Most Common Approach)
    public class JsonDictionaryConverter
    {
        private readonly JsonSerializerOptions _systemTextJsonOptions;
        private readonly JsonSerializerSettings _newtonsoftSettings;

        public JsonDictionaryConverter()
        {
            // System.Text.Json configuration
            _systemTextJsonOptions = new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                WriteIndented = false,
                DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
                NumberHandling = JsonNumberHandling.AllowReadingFromString
            };

            // Newtonsoft.Json configuration  
            _newtonsoftSettings = new JsonSerializerSettings
            {
                NullValueHandling = NullValueHandling.Ignore,
                DateFormatHandling = DateFormatHandling.IsoDateFormat,
                FloatFormatHandling = FloatFormatHandling.String
            };
        }

        // Convert C# Dictionary to JSON string (for HTTP transfer)
        public string SerializeDictionary(Dictionary<string, object> dictionary)
        {
            try
            {
                // Using System.Text.Json
                return JsonSerializer.Serialize(dictionary, _systemTextJsonOptions);
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException($"Failed to serialize dictionary: {ex.Message}", ex);
            }
        }

        // Convert C# Dictionary to JSON string using Newtonsoft (alternative)
        public string SerializeDictionaryNewtonsoft(Dictionary<string, object> dictionary)
        {
            try
            {
                return JsonConvert.SerializeObject(dictionary, _newtonsoftSettings);
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException($"Failed to serialize dictionary: {ex.Message}", ex);
            }
        }

        // Prepare dictionary for Java consumption with type safety
        public Dictionary<string, object> PrepareForJava(Dictionary<string, object> original)
        {
            var prepared = new Dictionary<string, object>();

            foreach (var kvp in original)
            {
                prepared[kvp.Key] = ConvertValueForJava(kvp.Value);
            }

            return prepared;
        }

        private object ConvertValueForJava(object value)
        {
            return value switch
            {
                null => null,
                
                // Primitive types - direct conversion
                bool b => b,
                byte b => (int)b,
                sbyte sb => (int)sb,
                short s => (int)s,
                ushort us => (int)us,
                int i => i,
                uint ui => (long)ui,
                long l => l,
                ulong ul => (decimal)ul, // Java doesn't have unsigned long
                float f => (double)f,
                double d => d,
                decimal dec => dec,
                string str => str,
                char c => c.ToString(),
                
                // Date/Time handling
                DateTime dt => dt.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
                DateTimeOffset dto => dto.ToString("yyyy-MM-ddTHH:mm:ss.fffK"),
                DateOnly dateOnly => dateOnly.ToString("yyyy-MM-dd"),
                TimeOnly timeOnly => timeOnly.ToString("HH:mm:ss.fff"),
                
                // GUID handling
                Guid guid => guid.ToString(),
                
                // Nested dictionaries
                Dictionary<string, object> dict => PrepareForJava(dict),
                IDictionary<string, object> idict => PrepareForJava(new Dictionary<string, object>(idict)),
                
                // Collections
                IEnumerable<object> enumerable => enumerable.Select(ConvertValueForJava).ToList(),
                IEnumerable enumerable => enumerable.Cast<object>().Select(ConvertValueForJava).ToList(),
                
                // Complex objects - serialize to dictionary
                _ => ConvertComplexObject(value)
            };
        }

        private object ConvertComplexObject(object obj)
        {
            if (obj == null) return null;

            try
            {
                // Convert complex object to dictionary via JSON
                var json = JsonSerializer.Serialize(obj, _systemTextJsonOptions);
                return JsonSerializer.Deserialize<Dictionary<string, object>>(json, _systemTextJsonOptions);
            }
            catch
            {
                // Fallback: return object as string
                return obj.ToString();
            }
        }
    }

    // Method 2: Explicit Type Mapping for Better Control
    public class TypedDictionaryConverter
    {
        // Create a wrapper that includes type information
        public class TypedValue
        {
            public string Type { get; set; }
            public object Value { get; set; }
            public string OriginalType { get; set; }

            public TypedValue(object value)
            {
                OriginalType = value?.GetType().FullName;
                (Type, Value) = DetermineTypeAndValue(value);
            }

            private (string type, object value) DetermineTypeAndValue(object obj)
            {
                return obj switch
                {
                    null => ("null", null),
                    bool => ("boolean", obj),
                    byte or sbyte or short or ushort or int => ("integer", Convert.ToInt32(obj)),
                    uint or long or ulong => ("long", Convert.ToInt64(obj)),
                    float or double => ("double", Convert.ToDouble(obj)),
                    decimal => ("decimal", obj.ToString()),
                    string => ("string", obj),
                    DateTime => ("datetime", ((DateTime)obj).ToString("O")),
                    Guid => ("guid", obj.ToString()),
                    IDictionary => ("map", obj),
                    IEnumerable => ("list", obj),
                    _ => ("object", obj)
                };
            }
        }

        public Dictionary<string, TypedValue> CreateTypedDictionary(Dictionary<string, object> original)
        {
            return original.ToDictionary(
                kvp => kvp.Key, 
                kvp => new TypedValue(kvp.Value)
            );
        }

        public string SerializeTypedDictionary(Dictionary<string, object> original)
        {
            var typed = CreateTypedDictionary(original);
            return JsonSerializer.Serialize(typed, new JsonSerializerOptions 
            { 
                WriteIndented = true,
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            });
        }
    }

    // Method 3: Examples of Different Dictionary Types and Their Conversion
    public class DictionaryExamples
    {
        private readonly JsonDictionaryConverter _converter;

        public DictionaryExamples()
        {
            _converter = new JsonDictionaryConverter();
        }

        // Example 1: Simple primitive types
        public string CreateSimpleDictionary()
        {
            var dictionary = new Dictionary<string, object>
            {
                ["stringValue"] = "Hello World",
                ["intValue"] = 42,
                ["doubleValue"] = 3.14159,
                ["boolValue"] = true,
                ["dateValue"] = DateTime.Now,
                ["nullValue"] = null
            };

            var prepared = _converter.PrepareForJava(dictionary);
            return _converter.SerializeDictionary(prepared);
        }

        // Example 2: Nested dictionaries
        public string CreateNestedDictionary()
        {
            var dictionary = new Dictionary<string, object>
            {
                ["user"] = new Dictionary<string, object>
                {
                    ["id"] = 123,
                    ["name"] = "John Doe",
                    ["email"] = "john@example.com",
                    ["profile"] = new Dictionary<string, object>
                    {
                        ["age"] = 30,
                        ["department"] = "Engineering",
                        ["skills"] = new List<string> { "C#", "Java", "Python" }
                    }
                },
                ["metadata"] = new Dictionary<string, object>
                {
                    ["createdAt"] = DateTime.Now,
                    ["version"] = "1.0",
                    ["tags"] = new List<string> { "user", "profile", "active" }
                }
            };

            var prepared = _converter.PrepareForJava(dictionary);
            return _converter.SerializeDictionary(prepared);
        }

        // Example 3: Complex objects with lists
        public string CreateComplexDictionary()
        {
            var employees = new List<object>
            {
                new { Id = 1, Name = "Alice", Salary = 75000.50m, StartDate = DateTime.Now.AddYears(-2) },
                new { Id = 2, Name = "Bob", Salary = 82000.75m, StartDate = DateTime.Now.AddYears(-1) }
            };

            var dictionary = new Dictionary<string, object>
            {
                ["company"] = "Tech Corp",
                ["employees"] = employees,
                ["departments"] = new List<object>
                {
                    new { Name = "Engineering", Budget = 1000000.00m, HeadCount = 50 },
                    new { Name = "Marketing", Budget = 500000.00m, HeadCount = 25 }
                },
                ["settings"] = new Dictionary<string, object>
                {
                    ["workingHours"] = new { Start = "09:00", End = "17:00" },
                    ["benefits"] = new List<string> { "Health", "Dental", "Vision", "401k" },
                    ["policies"] = new Dictionary<string, object>
                    {
                        ["vacation"] = new { MaxDays = 25, CarryOver = 5 },
                        ["remote"] = new { Allowed = true, MaxDaysPerWeek = 3 }
                    }
                }
            };

            var prepared = _converter.PrepareForJava(dictionary);
            return _converter.SerializeDictionary(prepared);
        }

        // Example 4: Financial data with precise decimal handling
        public string CreateFinancialDictionary()
        {
            var dictionary = new Dictionary<string, object>
            {
                ["accountId"] = "ACC-123456",
                ["balance"] = 1234.56m,
                ["transactions"] = new List<object>
                {
                    new { 
                        Id = "TXN-001", 
                        Amount = 500.00m, 
                        Type = "credit", 
                        Date = DateTime.Now.AddDays(-1),
                        Metadata = new Dictionary<string, object>
                        {
                            ["source"] = "wire_transfer",
                            ["fees"] = 15.00m,
                            ["exchangeRate"] = 1.0m
                        }
                    }
                },
                ["limits"] = new Dictionary<string, object>
                {
                    ["daily"] = 5000.00m,
                    ["monthly"] = 50000.00m,
                    ["currency"] = "USD"
                }
            };

            var prepared = _converter.PrepareForJava(dictionary);
            return _converter.SerializeDictionary(prepared);
        }
    }

    // Method 4: HTTP Client Extension for Sending to Java
    public static class HttpClientExtensions
    {
        public static async Task<HttpResponseMessage> PostDictionaryAsync(
            this HttpClient client, 
            string requestUri, 
            Dictionary<string, object> dictionary)
        {
            var converter = new JsonDictionaryConverter();
            var prepared = converter.PrepareForJava(dictionary);
            var json = converter.SerializeDictionary(prepared);
            
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            return await client.PostAsync(requestUri, content);
        }

        public static async Task<T> PostDictionaryAsync<T>(
            this HttpClient client, 
            string requestUri, 
            Dictionary<string, object> dictionary)
        {
            var response = await client.PostDictionaryAsync(requestUri, dictionary);
            response.EnsureSuccessStatusCode();
            
            var responseJson = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<T>(responseJson, new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            });
        }
    }

    // Method 5: Validation and Error Handling
    public class DictionaryValidator
    {
        public class ValidationResult
        {
            public bool IsValid { get; set; }
            public List<string> Errors { get; set; } = new();
            public List<string> Warnings { get; set; } = new();
        }

        public ValidationResult ValidateForJavaConversion(Dictionary<string, object> dictionary)
        {
            var result = new ValidationResult { IsValid = true };

            if (dictionary == null)
            {
                result.IsValid = false;
                result.Errors.Add("Dictionary cannot be null");
                return result;
            }

            foreach (var kvp in dictionary)
            {
                // Validate key
                if (string.IsNullOrWhiteSpace(kvp.Key))
                {
                    result.IsValid = false;
                    result.Errors.Add("Dictionary key cannot be null or empty");
                    continue;
                }

                if (!IsValidJavaIdentifier(kvp.Key))
                {
                    result.Warnings.Add($"Key '{kvp.Key}' may not be a valid Java identifier");
                }

                // Validate value
                var valueValidation = ValidateValue(kvp.Value, kvp.Key);
                result.Errors.AddRange(valueValidation.Errors);
                result.Warnings.AddRange(valueValidation.Warnings);

                if (valueValidation.Errors.Count > 0)
                {
                    result.IsValid = false;
                }
            }

            return result;
        }

        private ValidationResult ValidateValue(object value, string path)
        {
            var result = new ValidationResult { IsValid = true };

            if (value == null) return result;

            var type = value.GetType();

            // Check for problematic types
            if (type == typeof(IntPtr) || type == typeof(UIntPtr))
            {
                result.IsValid = false;
                result.Errors.Add($"Type {type.Name} at path '{path}' cannot be serialized to Java");
            }

            // Check for circular references
            if (value is IDictionary dict)
            {
                foreach (DictionaryEntry entry in dict)
                {
                    var nestedValidation = ValidateValue(entry.Value, $"{path}.{entry.Key}");
                    result.Errors.AddRange(nestedValidation.Errors);
                    result.Warnings.AddRange(nestedValidation.Warnings);
                    if (!nestedValidation.IsValid) result.IsValid = false;
                }
            }

            return result;
        }

        private bool IsValidJavaIdentifier(string identifier)
        {
            if (string.IsNullOrEmpty(identifier)) return false;
            if (!char.IsLetter(identifier[0]) && identifier[0] != '_' && identifier[0] != '$') return false;
            
            return identifier.All(c => char.IsLetterOrDigit(c) || c == '_' || c == '$');
        }
    }

    // Usage Examples
    public class UsageExamples
    {
        private readonly HttpClient _httpClient;
        private readonly JsonDictionaryConverter _converter;
        private readonly DictionaryValidator _validator;

        public UsageExamples(HttpClient httpClient)
        {
            _httpClient = httpClient;
            _converter = new JsonDictionaryConverter();
            _validator = new DictionaryValidator();
        }

        // Example: Send workflow data to Java service
        public async Task<string> SendWorkflowData()
        {
            var workflowData = new Dictionary<string, object>
            {
                ["processKey"] = "employee-onboarding",
                ["variables"] = new Dictionary<string, object>
                {
                    ["employeeId"] = "EMP-001",
                    ["startDate"] = DateTime.Now.AddDays(7),
                    ["department"] = "Engineering",
                    ["salary"] = 75000.50m,
                    ["benefits"] = new List<string> { "Health", "Dental", "401k" }
                },
                ["metadata"] = new Dictionary<string, object>
                {
                    ["requestId"] = Guid.NewGuid().ToString(),
                    ["timestamp"] = DateTime.UtcNow,
                    ["priority"] = "high"
                }
            };

            // Validate before sending
            var validation = _validator.ValidateForJavaConversion(workflowData);
            if (!validation.IsValid)
            {
                throw new ArgumentException($"Validation failed: {string.Join(", ", validation.Errors)}");
            }

            // Prepare and send
            var prepared = _converter.PrepareForJava(workflowData);
            var json = _converter.SerializeDictionary(prepared);

            var content = new StringContent(json, Encoding.UTF8, "application/json");
            var response = await _httpClient.PostAsync("http://java-service/api/workflow/deploy", content);

            return await response.Content.ReadAsStringAsync();
        }

        // Example: Handle response from Java service
        public async Task<Dictionary<string, object>> ReceiveJavaResponse(string javaResponseJson)
        {
            try
            {
                return JsonSerializer.Deserialize<Dictionary<string, object>>(javaResponseJson, 
                    new JsonSerializerOptions 
                    { 
                        PropertyNamingPolicy = JsonNamingPolicy.CamelCase 
                    });
            }
            catch (JsonException ex)
            {
                throw new InvalidOperationException($"Failed to deserialize Java response: {ex.Message}", ex);
            }
        }
    }
}

// ============================================================================
// JAVA SIDE - Receiving and Converting C# Dictionaries
// ============================================================================

/*
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// Java service to receive C# dictionary data
@RestController
@RequestMapping("/api/workflow")
public class DictionaryReceiveController {
    
    private final ObjectMapper objectMapper;
    
    public DictionaryReceiveController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // Method 1: Receive as generic Map
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, Object>> receiveWorkflowData(
            @RequestBody Map<String, Object> data) {
        
        try {
            // Process the received dictionary
            Map<String, Object> processedData = processCSharpDictionary(data);
            
            // Extract specific values with type safety
            String processKey = extractString(processedData, "processKey");
            Map<String, Object> variables = extractMap(processedData, "variables");
            Map<String, Object> metadata = extractMap(processedData, "metadata");
            
            // Use the data in your workflow
            String result = deployWorkflow(processKey, variables, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processInstanceId", result);
            response.put("receivedData", processedData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Method 2: Receive as JSON string and convert
    @PostMapping("/deploy-string")
    public ResponseEntity<Map<String, Object>> receiveWorkflowDataAsString(
            @RequestBody String jsonData) {
        
        try {
            // Convert JSON string to Map
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> data = objectMapper.readValue(jsonData, typeRef);
            
            // Process the data
            return receiveWorkflowData(data);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to parse JSON: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Helper method to process C# dictionary and handle type conversions
    private Map<String, Object> processCSharpDictionary(Map<String, Object> original) {
        Map<String, Object> processed = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Convert value to Java-friendly format
            Object convertedValue = convertCSharpValue(value);
            processed.put(key, convertedValue);
        }
        
        return processed;
    }
    
    @SuppressWarnings("unchecked")
    private Object convertCSharpValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Handle primitive types (usually already converted by Jackson)
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        
        // Handle date strings from C#
        if (value instanceof String && isDateString((String) value)) {
            try {
                return LocalDateTime.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // If parsing fails, return as string
                return value;
            }
        }
        
        // Handle nested maps
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> convertedMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                convertedMap.put(entry.getKey(), convertCSharpValue(entry.getValue()));
            }
            return convertedMap;
        }
        
        // Handle lists
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> convertedList = new ArrayList<>();
            for (Object item : list) {
                convertedList.add(convertCSharpValue(item));
            }
            return convertedList;
        }
        
        // For other complex objects, try to convert via ObjectMapper
        try {
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            // If conversion fails, return as-is
            return value;
        }
    }
    
    // Helper methods for safe type extraction
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    @SuppressWarnings("unchecked")
    private List<Object> extractList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }
    
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Double extractDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Boolean extractBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    // Helper method to check if string looks like a date
    private boolean isDateString(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }
    
    // Mock method for workflow deployment
    private String deployWorkflow(String processKey, Map<String, Object> variables, Map<String, Object> metadata) {
        // Your actual workflow deployment logic here
        return "PROCESS_INSTANCE_" + System.currentTimeMillis();
    }
}

// Utility class for advanced dictionary operations
@Component
public class DictionaryUtils {
    
    private final ObjectMapper objectMapper;
    
    public DictionaryUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // Convert any object to Map<String, Object>
    public Map<String, Object> objectToMap(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to map", e);
        }
    }
    
    // Convert Map to specific type
    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        try {
            String json = objectMapper.writeValueAsString(map);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to object", e);
        }
    }
    
    // Deep merge two maps
    @SuppressWarnings("unchecked")
    public Map<String, Object> mergeMaps(Map<String, Object> map1, Map<String, Object> map2) {
        Map<String, Object> result = new HashMap<>(map1);
        
        for (Map.Entry<String, Object> entry : map2.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (result.containsKey(key) && result.get(key) instanceof Map && value instanceof Map) {
                result.put(key, mergeMaps((Map<String, Object>) result.get(key), (Map<String, Object>) value));
            } else {
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    // Get nested value from map using dot notation
    @SuppressWarnings("unchecked")
    public Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
}
*/
