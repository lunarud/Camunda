vimport com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;

// RequestDTO class to hold JSON data
class RequestDTO {
    private JsonNode jsonData;
    private String jsonString;
    
    public RequestDTO() {}
    
    public RequestDTO(JsonNode jsonData) {
        this.jsonData = jsonData;
    }
    
    public RequestDTO(String jsonString) {
        this.jsonString = jsonString;
    }
    
    // Getters and setters
    public JsonNode getJsonData() {
        return jsonData;
    }
    
    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }
    
    public String getJsonString() {
        return jsonString;
    }
    
    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }
}

// Main handler class for JSON operations
public class JsonHandler {
    private final ObjectMapper objectMapper;
    private RequestDTO requestDTO;
    
    public JsonHandler() {
        this.objectMapper = new ObjectMapper();
        this.requestDTO = new RequestDTO();
    }
    
    /**
     * Accept and store a RequestDTO
     */
    public void acceptRequestDTO(RequestDTO dto) {
        this.requestDTO = dto;
    }
    
    /**
     * Store JSON object directly
     */
    public void storeJsonObject(JsonNode jsonObject) {
        this.requestDTO.setJsonData(jsonObject);
    }
    
    /**
     * Store JSON from string
     */
    public void storeJsonFromString(String jsonString) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        this.requestDTO.setJsonData(jsonNode);
        this.requestDTO.setJsonString(jsonString);
    }
    
    /**
     * Convert stored JSON object to string
     */
    public String jsonObjectToString() throws JsonProcessingException {
        if (requestDTO.getJsonData() == null) {
            throw new IllegalStateException("No JSON object stored");
        }
        String jsonString = objectMapper.writeValueAsString(requestDTO.getJsonData());
        requestDTO.setJsonString(jsonString);
        return jsonString;
    }
    
    /**
     * Convert stored JSON string to JsonNode object
     */
    public JsonNode jsonStringToObject() throws IOException {
        String jsonString = requestDTO.getJsonString();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalStateException("No JSON string stored");
        }
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        requestDTO.setJsonData(jsonNode);
        return jsonNode;
    }
    
    /**
     * Get the stored RequestDTO
     */
    public RequestDTO getRequestDTO() {
        return this.requestDTO;
    }
    
    /**
     * Get stored JSON as string
     */
    public String getStoredJsonString() {
        return requestDTO.getJsonString();
    }
    
    /**
     * Get stored JSON as object
     */
    public JsonNode getStoredJsonObject() {
        return requestDTO.getJsonData();
    }
    
    /**
     * Check if JSON data is stored
     */
    public boolean hasJsonData() {
        return requestDTO.getJsonData() != null || 
               (requestDTO.getJsonString() != null && !requestDTO.getJsonString().trim().isEmpty());
    }
    
    /**
     * Clear stored data
     */
    public void clear() {
        this.requestDTO = new RequestDTO();
    }
    
    // Example usage and testing
    public static void main(String[] args) {
        try {
            JsonHandler handler = new JsonHandler();
            
            // Example 1: Store JSON from string
            String sampleJson = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
            handler.storeJsonFromString(sampleJson);
            
            System.out.println("Stored JSON String: " + handler.getStoredJsonString());
            System.out.println("Stored JSON Object: " + handler.getStoredJsonObject());
            
            // Example 2: Convert object to string
            String convertedString = handler.jsonObjectToString();
            System.out.println("Converted to String: " + convertedString);
            
            // Example 3: Convert string back to object
            handler.clear();
            RequestDTO dto = new RequestDTO(sampleJson);
            handler.acceptRequestDTO(dto);
            JsonNode convertedObject = handler.jsonStringToObject();
            System.out.println("Converted to Object: " + convertedObject);
            
            // Example 4: Access specific values
            System.out.println("Name: " + convertedObject.get("name").asText());
            System.out.println("Age: " + convertedObject.get("age").asInt());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
