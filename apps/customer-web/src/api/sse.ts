export interface ParsedSseEvent {
  event: string;
  data: unknown;
}

interface Boundary {
  index: number;
  length: number;
}

function nextBoundary(buffer: string): Boundary | null {
  const candidates = [
    { index: buffer.indexOf("\r\n\r\n"), length: 4 },
    { index: buffer.indexOf("\n\n"), length: 2 },
    { index: buffer.indexOf("\r\r"), length: 2 },
  ].filter((candidate) => candidate.index >= 0);

  if (candidates.length === 0) return null;
  return candidates.reduce((earliest, candidate) => (
    candidate.index < earliest.index ? candidate : earliest
  ));
}

function parseRecord(record: string): ParsedSseEvent | null {
  let event = "message";
  const data: string[] = [];

  for (const line of record.split(/\r\n|\r|\n/)) {
    if (line.length === 0 || line.startsWith(":")) continue;
    const colon = line.indexOf(":");
    const field = colon < 0 ? line : line.slice(0, colon);
    let value = colon < 0 ? "" : line.slice(colon + 1);
    if (value.startsWith(" ")) value = value.slice(1);
    if (field === "event") event = value;
    if (field === "data") data.push(value);
  }

  if (data.length === 0) return null;
  try {
    return { event, data: JSON.parse(data.join("\n")) as unknown };
  } catch (error) {
    throw new Error(`Invalid JSON in SSE event ${event}`, { cause: error });
  }
}

export async function* parseSseStream(
  stream: ReadableStream<Uint8Array>,
): AsyncGenerator<ParsedSseEvent> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let boundary = nextBoundary(buffer);
      while (boundary) {
        const record = buffer.slice(0, boundary.index);
        buffer = buffer.slice(boundary.index + boundary.length);
        const parsed = parseRecord(record);
        if (parsed) yield parsed;
        boundary = nextBoundary(buffer);
      }
    }
    buffer += decoder.decode();
    const parsed = parseRecord(buffer);
    if (parsed) yield parsed;
  } finally {
    reader.releaseLock();
  }
}
