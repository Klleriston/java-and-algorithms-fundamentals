export type ArrayRangeState = 'DISCARDED' | 'SORTED' | 'ACTIVE';

export interface ArrayViewPayload {
  kind: 'ARRAY';
  items: number[];
  pointers: Record<string, number>;
  ranges: { from: number; to: number; state: ArrayRangeState }[];
  swapped: number[];
}

export interface UnknownViewPayload {
  kind: string;
  [key: string]: unknown;
}

export type ViewPayload = ArrayViewPayload | UnknownViewPayload;

export interface TraceStep {
  index: number;
  line: number;
  message: string;
  vars: Record<string, unknown>;
  view: ViewPayload;
}

export interface TraceResult {
  returnValue: unknown;
  stepCount: number;
  measured: boolean;
}

export interface Trace {
  demoId: string;
  title: string;
  sourceCode: string;
  steps: TraceStep[];
  result: TraceResult;
}

export type ParameterType = 'INT' | 'LONG' | 'INT_ARRAY' | 'STRING_ARRAY' | 'ENUM';

export interface ParameterSpec {
  name: string;
  type: ParameterType;
  label: string;
  required: boolean;
  min?: number;
  max?: number;
  maxLength?: number;
  options?: string[];
  defaultValue?: unknown;
}

export interface DemoSummary {
  id: string;
  title: string;
  category: 'ALGORITHMS' | 'DATA_STRUCTURES' | 'JVM' | 'OOP';
  description: string;
  parameters: ParameterSpec[];
  sourceCode: string;
}
