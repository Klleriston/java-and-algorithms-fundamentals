export type ArrayRangeState = 'DISCARDED' | 'SORTED' | 'ACTIVE';

export interface ArrayViewPayload {
  kind: 'ARRAY';
  items: number[];
  pointers: Record<string, number>;
  ranges: { from: number; to: number; state: ArrayRangeState }[];
  swapped: number[];
}

export type HashEntryState = 'NORMAL' | 'PROBED' | 'MATCHED' | 'INSERTED';

export interface HashTableViewPayload {
  kind: 'HASH_TABLE';
  capacity: number;
  // The backend serializes with non_null inclusion, so a null value, bucket or
  // outcome arrives as an absent field rather than as null.
  buckets: { index: number; entries: { key: number; value?: number | null; state: HashEntryState }[] }[];
  activeBucket?: number | null;
  outcome?: 'INSERTED' | 'UPDATED' | 'DUPLICATE' | null;
}

// The backend serializes with non_null inclusion, so a null child, cursor or
// outcome arrives as an absent field rather than as null. The optional markers
// are what the wire actually looks like.
export interface TreeViewPayload {
  kind: 'TREE';
  nodes: { id: number; value: number; left?: number | null; right?: number | null }[];
  activeNode?: number | null;
  path: number[];
  insertedNode?: number | null;
}

export interface LinkedListViewPayload {
  kind: 'LINKED_LIST';
  // The backend serializes with non_null inclusion, so a null next, cursor or
  // link end arrives as an absent field rather than as null.
  nodes: { id: number; value: number; next?: number | null }[];
  cursor?: number | null;
  changedLinks: { from?: number | null; to?: number | null }[];
  newNode?: number | null;
}

export interface UnknownViewPayload {
  kind: string;
  [key: string]: unknown;
}

export type ViewPayload =
  | ArrayViewPayload
  | HashTableViewPayload
  | TreeViewPayload
  | LinkedListViewPayload
  | UnknownViewPayload;

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
