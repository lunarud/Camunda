import * as fs from 'fs';
import { DOMParser, XMLSerializer } from '@xmldom/xmldom';

interface BpmnElement {
  id: string;
  element: Element;
  type: string;
}

class BpmnServiceTaskInjector {
  private doc: Document;
  private process: Element;
  private idCounter: number = 1000;

  constructor(bpmnXml: string) {
    const parser = new DOMParser();
    this.doc = parser.parseFromString(bpmnXml, 'text/xml');
    this.process = this.doc.getElementsByTagName('bpmn:process')[0];
    if (!this.process) {
      throw new Error('No BPMN process found in the XML');
    }
  }

  /**
   * Generate a unique ID for new elements
   */
  private generateId(prefix: string): string {
    return `${prefix}_${this.idCounter++}`;
  }

  /**
   * Create a Service Task element
   */
  private createServiceTask(id: string, name: string): Element {
    const serviceTask = this.doc.createElement('bpmn:serviceTask');
    serviceTask.setAttribute('id', id);
    serviceTask.setAttribute('name', name);
    
    // Add implementation details (customize as needed)
    serviceTask.setAttribute('camunda:type', 'external');
    serviceTask.setAttribute('camunda:topic', 'service-task-topic');
    
    return serviceTask;
  }

  /**
   * Create a Sequence Flow element
   */
  private createSequenceFlow(id: string, sourceRef: string, targetRef: string): Element {
    const sequenceFlow = this.doc.createElement('bpmn:sequenceFlow');
    sequenceFlow.setAttribute('id', id);
    sequenceFlow.setAttribute('sourceRef', sourceRef);
    sequenceFlow.setAttribute('targetRef', targetRef);
    
    return sequenceFlow;
  }

  /**
   * Find all start events in the process
   */
  private findStartEvents(): BpmnElement[] {
    const startEvents: BpmnElement[] = [];
    const elements = this.process.getElementsByTagName('bpmn:startEvent');
    
    for (let i = 0; i < elements.length; i++) {
      const element = elements[i] as Element;
      startEvents.push({
        id: element.getAttribute('id')!,
        element: element,
        type: 'startEvent'
      });
    }
    
    return startEvents;
  }

  /**
   * Find all end events in the process
   */
  private findEndEvents(): BpmnElement[] {
    const endEvents: BpmnElement[] = [];
    const elements = this.process.getElementsByTagName('bpmn:endEvent');
    
    for (let i = 0; i < elements.length; i++) {
      const element = elements[i] as Element;
      endEvents.push({
        id: element.getAttribute('id')!,
        element: element,
        type: 'endEvent'
      });
    }
    
    return endEvents;
  }

  /**
   * Find outgoing sequence flows from a given element
   */
  private findOutgoingFlows(elementId: string): Element[] {
    const flows: Element[] = [];
    const sequenceFlows = this.process.getElementsByTagName('bpmn:sequenceFlow');
    
    for (let i = 0; i < sequenceFlows.length; i++) {
      const flow = sequenceFlows[i] as Element;
      if (flow.getAttribute('sourceRef') === elementId) {
        flows.push(flow);
      }
    }
    
    return flows;
  }

  /**
   * Find incoming sequence flows to a given element
   */
  private findIncomingFlows(elementId: string): Element[] {
    const flows: Element[] = [];
    const sequenceFlows = this.process.getElementsByTagName('bpmn:sequenceFlow');
    
    for (let i = 0; i < sequenceFlows.length; i++) {
      const flow = sequenceFlows[i] as Element;
      if (flow.getAttribute('targetRef') === elementId) {
        flows.push(flow);
      }
    }
    
    return flows;
  }

  /**
   * Inject Service Task after Start Events
   */
  public injectServiceTaskAfterStart(taskName: string = 'Pre-Process Service Task'): void {
    const startEvents = this.findStartEvents();
    
    startEvents.forEach(startEvent => {
      const serviceTaskId = this.generateId('ServiceTask');
      const newFlowId = this.generateId('Flow');
      
      // Create the service task
      const serviceTask = this.createServiceTask(serviceTaskId, taskName);
      this.process.appendChild(serviceTask);
      
      // Find existing outgoing flows from start event
      const outgoingFlows = this.findOutgoingFlows(startEvent.id);
      
      // Create new flow from start event to service task
      const startToServiceFlow = this.createSequenceFlow(newFlowId, startEvent.id, serviceTaskId);
      this.process.appendChild(startToServiceFlow);
      
      // Update existing flows to come from service task instead
      outgoingFlows.forEach(flow => {
        const newFlowFromServiceId = this.generateId('Flow');
        const targetRef = flow.getAttribute('targetRef')!;
        
        // Create new flow from service task to original target
        const serviceToTargetFlow = this.createSequenceFlow(newFlowFromServiceId, serviceTaskId, targetRef);
        this.process.appendChild(serviceToTargetFlow);
        
        // Remove the original flow
        this.process.removeChild(flow);
      });
    });
  }

  /**
   * Inject Service Task before End Events
   */
  public injectServiceTaskBeforeEnd(taskName: string = 'Post-Process Service Task'): void {
    const endEvents = this.findEndEvents();
    
    endEvents.forEach(endEvent => {
      const serviceTaskId = this.generateId('ServiceTask');
      const newFlowId = this.generateId('Flow');
      
      // Create the service task
      const serviceTask = this.createServiceTask(serviceTaskId, taskName);
      this.process.appendChild(serviceTask);
      
      // Find existing incoming flows to end event
      const incomingFlows = this.findIncomingFlows(endEvent.id);
      
      // Create new flow from service task to end event
      const serviceToEndFlow = this.createSequenceFlow(newFlowId, serviceTaskId, endEvent.id);
      this.process.appendChild(serviceToEndFlow);
      
      // Update existing flows to go to service task instead
      incomingFlows.forEach(flow => {
        flow.setAttribute('targetRef', serviceTaskId);
      });
    });
  }

  /**
   * Get the modified BPMN XML
   */
  public getModifiedXml(): string {
    const serializer = new XMLSerializer();
    return serializer.serializeToString(this.doc);
  }

  /**
   * Save the modified BPMN to a file
   */
  public saveToFile(filePath: string): void {
    const xml = this.getModifiedXml();
    fs.writeFileSync(filePath, xml, 'utf8');
  }
}

// Usage example
export function injectServiceTasks(
  inputBpmnPath: string, 
  outputBpmnPath: string,
  options: {
    addPreProcessTask?: boolean;
    addPostProcessTask?: boolean;
    preProcessTaskName?: string;
    postProcessTaskName?: string;
  } = {}
): void {
  try {
    // Read the BPMN file
    const bpmnXml = fs.readFileSync(inputBpmnPath, 'utf8');
    
    // Create injector instance
    const injector = new BpmnServiceTaskInjector(bpmnXml);
    
    // Inject service tasks based on options
    if (options.addPreProcessTask !== false) {
      injector.injectServiceTaskAfterStart(
        options.preProcessTaskName || 'Pre-Process Service Task'
      );
    }
    
    if (options.addPostProcessTask !== false) {
      injector.injectServiceTaskBeforeEnd(
        options.postProcessTaskName || 'Post-Process Service Task'
      );
    }
    
    // Save the modified BPMN
    injector.saveToFile(outputBpmnPath);
    
    console.log(`Successfully injected service tasks. Output saved to: ${outputBpmnPath}`);
  } catch (error) {
    console.error('Error injecting service tasks:', error);
    throw error;
  }
}

// Advanced usage with custom configuration
export class BpmnModifier extends BpmnServiceTaskInjector {
  /**
   * Add custom service task with specific properties
   */
  public addCustomServiceTask(
    afterElementId: string,
    beforeElementId: string,
    taskConfig: {
      name: string;
      implementation?: string;
      topic?: string;
      type?: string;
      [key: string]: any;
    }
  ): string {
    const serviceTaskId = this.generateId('CustomServiceTask');
    const serviceTask = this.createServiceTask(serviceTaskId, taskConfig.name);
    
    // Add custom attributes
    Object.keys(taskConfig).forEach(key => {
      if (key !== 'name') {
        if (key === 'type') {
          serviceTask.setAttribute('camunda:type', taskConfig[key]);
        } else if (key === 'topic') {
          serviceTask.setAttribute('camunda:topic', taskConfig[key]);
        } else {
          serviceTask.setAttribute(key, taskConfig[key]);
        }
      }
    });
    
    this.process.appendChild(serviceTask);
    
    // Create flows
    const flowToService = this.createSequenceFlow(
      this.generateId('Flow'),
      afterElementId,
      serviceTaskId
    );
    const flowFromService = this.createSequenceFlow(
      this.generateId('Flow'),
      serviceTaskId,
      beforeElementId
    );
    
    this.process.appendChild(flowToService);
    this.process.appendChild(flowFromService);
    
    return serviceTaskId;
  }
}

// Example usage:
/*
// Basic usage
injectServiceTasks('./input.bpmn', './output.bpmn', {
  addPreProcessTask: true,
  addPostProcessTask: true,
  preProcessTaskName: 'Initialize Process',
  postProcessTaskName: 'Cleanup Process'
});

// Advanced usage
const bpmnXml = fs.readFileSync('./process.bpmn', 'utf8');
const modifier = new BpmnModifier(bpmnXml);

modifier.injectServiceTaskAfterStart('Validation Task');
modifier.injectServiceTaskBeforeEnd('Notification Task');

// Add custom service task
modifier.addCustomServiceTask('StartEvent_1', 'Task_1', {
  name: 'Custom Logger',
  type: 'external',
  topic: 'logging-topic',
  'camunda:class': 'com.example.LoggerDelegate'
});

modifier.saveToFile('./modified-process.bpmn');
*/
