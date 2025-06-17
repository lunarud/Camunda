// APPROACH 1: HTTP Client Call (Most Common)
// External TaskHandler making HTTP calls to Web Controller

using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

// External TaskHandler (could be in a separate service/application)
public class ExternalTaskHandler
{
    private readonly HttpClient _httpClient;
    private readonly string _webApiBaseUrl;

    public ExternalTaskHandler(string webApiBaseUrl)
    {
        _webApiBaseUrl = webApiBaseUrl;
        _httpClient = new HttpClient();
        _httpClient.DefaultRequestHeaders.Add("Accept", "application/json");
    }

    // Task handler method that calls the web controller
    public async Task ProcessTaskAsync(TaskData taskData)
    {
        try
        {
            // Serialize task data
            var json = JsonSerializer.Serialize(taskData);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            // Call the web controller endpoint
            var response = await _httpClient.PostAsync($"{_webApiBaseUrl}/api/tasks/process", content);

            if (response.IsSuccessStatusCode)
            {
                var result = await response.Content.ReadAsStringAsync();
                Console.WriteLine($"Task processed successfully: {result}");
            }
            else
            {
                Console.WriteLine($"Task processing failed: {response.StatusCode}");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error calling web controller: {ex.Message}");
        }
    }

    // Example of calling different controller actions
    public async Task<TaskResult> GetTaskStatusAsync(int taskId)
    {
        var response = await _httpClient.GetAsync($"{_webApiBaseUrl}/api/tasks/{taskId}/status");
        
        if (response.IsSuccessStatusCode)
        {
            var json = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<TaskResult>(json);
        }
        
        return null;
    }

    public async Task CompleteTaskAsync(int taskId, TaskCompletionData completionData)
    {
        var json = JsonSerializer.Serialize(completionData);
        var content = new StringContent(json, Encoding.UTF8, "application/json");
        
        await _httpClient.PutAsync($"{_webApiBaseUrl}/api/tasks/{taskId}/complete", content);
    }
}

// APPROACH 2: Shared Service Layer (Better Architecture)
// Both TaskHandler and Controller use the same business service

// Shared business service
public interface ITaskService
{
    Task<TaskResult> ProcessTaskAsync(TaskData taskData);
    Task<TaskStatus> GetTaskStatusAsync(int taskId);
    Task CompleteTaskAsync(int taskId, TaskCompletionData completionData);
}

public class TaskService : ITaskService
{
    public async Task<TaskResult> ProcessTaskAsync(TaskData taskData)
    {
        // Business logic here
        await Task.Delay(1000); // Simulate processing
        
        return new TaskResult
        {
            TaskId = taskData.Id,
            Status = "Processed",
            ProcessedAt = DateTime.UtcNow
        };
    }

    public async Task<TaskStatus> GetTaskStatusAsync(int taskId)
    {
        // Implementation
        return new TaskStatus { Id = taskId, Status = "Running" };
    }

    public async Task CompleteTaskAsync(int taskId, TaskCompletionData completionData)
    {
        // Implementation
        await Task.CompletedTask;
    }
}

// Web Controller using the shared service
[ApiController]
[Route("api/[controller]")]
public class TasksController : ControllerBase
{
    private readonly ITaskService _taskService;

    public TasksController(ITaskService taskService)
    {
        _taskService = taskService;
    }

    [HttpPost("process")]
    public async Task<ActionResult<TaskResult>> ProcessTask([FromBody] TaskData taskData)
    {
        var result = await _taskService.ProcessTaskAsync(taskData);
        return Ok(result);
    }

    [HttpGet("{taskId}/status")]
    public async Task<ActionResult<TaskStatus>> GetTaskStatus(int taskId)
    {
        var status = await _taskService.GetTaskStatusAsync(taskId);
        return Ok(status);
    }

    [HttpPut("{taskId}/complete")]
    public async Task<ActionResult> CompleteTask(int taskId, [FromBody] TaskCompletionData completionData)
    {
        await _taskService.CompleteTaskAsync(taskId, completionData);
        return Ok();
    }
}

// External TaskHandler using the same shared service
public class ExternalTaskHandlerWithSharedService
{
    private readonly ITaskService _taskService;

    public ExternalTaskHandlerWithSharedService(ITaskService taskService)
    {
        _taskService = taskService;
    }

    public async Task HandleTaskAsync(TaskData taskData)
    {
        // Both the external handler and web controller use the same service
        var result = await _taskService.ProcessTaskAsync(taskData);
        Console.WriteLine($"Task {result.TaskId} processed with status: {result.Status}");
    }
}

// APPROACH 3: Message Queue Integration
// TaskHandler publishes messages, Controller subscribes

public class MessageBasedTaskHandler
{
    private readonly IMessagePublisher _messagePublisher;

    public MessageBasedTaskHandler(IMessagePublisher messagePublisher)
    {
        _messagePublisher = messagePublisher;
    }

    public async Task ProcessTaskAsync(TaskData taskData)
    {
        // Publish a message that the web controller can subscribe to
        var message = new TaskProcessedMessage
        {
            TaskId = taskData.Id,
            ProcessedAt = DateTime.UtcNow,
            Data = taskData
        };

        await _messagePublisher.PublishAsync("task.processed", message);
    }
}

// Controller with message subscription (using background service)
public class TaskMessageSubscriber : BackgroundService
{
    private readonly IMessageSubscriber _messageSubscriber;
    private readonly ITaskService _taskService;

    public TaskMessageSubscriber(IMessageSubscriber messageSubscriber, ITaskService taskService)
    {
        _messageSubscriber = messageSubscriber;
        _taskService = taskService;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await _messageSubscriber.SubscribeAsync<TaskProcessedMessage>("task.processed", async message =>
        {
            // Handle the message from external task handler
            await _taskService.ProcessTaskAsync(message.Data);
        });
    }
}

// Data Models
public class TaskData
{
    public int Id { get; set; }
    public string Name { get; set; }
    public string Description { get; set; }
    public Dictionary<string, object> Parameters { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class TaskResult
{
    public int TaskId { get; set; }
    public string Status { get; set; }
    public DateTime ProcessedAt { get; set; }
    public object Result { get; set; }
}

public class TaskStatus
{
    public int Id { get; set; }
    public string Status { get; set; }
    public DateTime LastUpdated { get; set; }
}

public class TaskCompletionData
{
    public string Result { get; set; }
    public bool Success { get; set; }
    public string ErrorMessage { get; set; }
}

public class TaskProcessedMessage
{
    public int TaskId { get; set; }
    public DateTime ProcessedAt { get; set; }
    public TaskData Data { get; set; }
}

// Example interfaces for message handling
public interface IMessagePublisher
{
    Task PublishAsync<T>(string topic, T message);
}

public interface IMessageSubscriber
{
    Task SubscribeAsync<T>(string topic, Func<T, Task> handler);
}

// APPROACH 4: Direct Controller Invocation (Same Process Only)
// Only works if TaskHandler runs in the same process as the web application

public class InProcessTaskHandler
{
    private readonly TasksController _controller;

    public InProcessTaskHandler(TasksController controller)
    {
        _controller = controller;
    }

    public async Task ProcessTaskDirectlyAsync(TaskData taskData)
    {
        // Directly call controller method (bypasses HTTP pipeline)
        var result = await _controller.ProcessTask(taskData);
        
        // Handle the ActionResult
        if (result.Result is OkObjectResult okResult)
        {
            var taskResult = okResult.Value as TaskResult;
            Console.WriteLine($"Task {taskResult.TaskId} processed successfully");
        }
    }
}

// Usage Examples
public class Program
{
    public static async Task Main(string[] args)
    {
        // Example 1: HTTP Client approach
        var httpTaskHandler = new ExternalTaskHandler("https://mywebapi.com");
        await httpTaskHandler.ProcessTaskAsync(new TaskData { Id = 1, Name = "Test Task" });

        // Example 2: Shared service approach (better for same solution)
        var taskService = new TaskService();
        var sharedServiceHandler = new ExternalTaskHandlerWithSharedService(taskService);
        await sharedServiceHandler.HandleTaskAsync(new TaskData { Id = 2, Name = "Shared Service Task" });

        // Example 3: Message-based approach
        // This would require actual message queue implementation (RabbitMQ, Azure Service Bus, etc.)
    }
}
