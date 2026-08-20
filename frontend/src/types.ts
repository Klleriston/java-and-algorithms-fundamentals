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
  messageKey: string;
  messageArgs: Record<string, unknown>;
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
  titleKey: string;
  sourceCode: string;
  steps: TraceStep[];
  result: TraceResult;
}

export type ParameterType = 'INT' | 'LONG' | 'INT_ARRAY' | 'STRING_ARRAY' | 'ENUM';

export interface ParameterSpec {
  name: string;
  type: ParameterType;
  labelKey: string;
  required: boolean;
  min?: number;
  max?: number;
  maxLength?: number;
  options?: string[];
  defaultValue?: unknown;
}

export interface DemoSummary {
  id: string;
  titleKey: string;
  category: 'ALGORITHMS' | 'DATA_STRUCTURES' | 'JVM' | 'OOP';
  descriptionKey: string;
  parameters: ParameterSpec[];
  sourceCode: string;
}
