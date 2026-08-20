export function format(template: string, args: Record<string, unknown> = {}): string {
  return template.replace(/\{(\w+)\}/g, (placeholder, name: string) =>
    name in args ? String(args[name]) : placeholder,
  );
}
