csharpusing System.Text.Json;
using System.Collections.Generic;

// Create and populate dictionary
var dictionary = new Dictionary<string, object>
{
    ["name"] = "John",
    ["age"] = 30,
    ["city"] = "New York"
};

// Serialize to JSON
string jsonString = JsonSerializer.Serialize(dictionary);
// Save to file or send over network
File.WriteAllText("data.json", jsonString);


javaimport com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

// Read JSON string
String jsonString = new String(Files.readAllBytes(Paths.get("data.json")));

// Deserialize to Map
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> map = mapper.readValue(jsonString, 
    new TypeReference<Map<String, Object>>(){});


csharpusing System.Xml.Serialization;

[Serializable]
public class DictionaryContainer
{
    public Dictionary<string, string> Data { get; set; }
}

var container = new DictionaryContainer { Data = yourDictionary };
var serializer = new XmlSerializer(typeof(DictionaryContainer));

using (var writer = new StreamWriter("data.xml"))
{
    serializer.Serialize(writer, container);
}


javaimport javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.util.Map;

// Create corresponding Java class with JAXB annotations
JAXBContext context = JAXBContext.newInstance(DictionaryContainer.class);
Unmarshaller unmarshaller = context.createUnmarshaller();
DictionaryContainer container = (DictionaryContainer) unmarshaller.unmarshal(new File("data.xml"));
Map<String, String> map = container.getData();
