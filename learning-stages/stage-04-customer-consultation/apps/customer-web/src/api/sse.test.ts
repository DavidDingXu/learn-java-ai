import { describe, expect, it } from "vitest";

import { parseSseStream } from "./sse";

function streamOf(...chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk));
      controller.close();
    },
  });
}

describe("parseSseStream", () => {
  it("parses named JSON events across arbitrary network chunks", async () => {
    const events = [];

    for await (const event of parseSseStream(streamOf(
      "event: sess",
      "ion\r\ndata: {\"conversationId\":\"c-1\",\"attemptId\":\"a-1\"}\r\n\r",
      "\nevent: delta\ndata: {\"text\":\"退款将在\"}\n\n",
      ": heartbeat comment\n\nevent: completed\ndata: {}\n\n",
    ))) {
      events.push(event);
    }

    expect(events).toEqual([
      {
        event: "session",
        data: { conversationId: "c-1", attemptId: "a-1" },
      },
      { event: "delta", data: { text: "退款将在" } },
      { event: "completed", data: {} },
    ]);
  });

  it("rejects an event whose data is not valid JSON", async () => {
    const consume = async () => {
      for await (const ignored of parseSseStream(streamOf("event: delta\ndata: not-json\n\n"))) {
        void ignored;
      }
    };

    await expect(consume()).rejects.toThrow("Invalid JSON in SSE event delta");
  });

  it("dispatches the final event when the connection closes without a blank line", async () => {
    const events = [];

    for await (const event of parseSseStream(streamOf(
      "event: error\ndata: {\"code\":\"STREAM_FAILED\",\"message\":\"回答中断\"}",
    ))) {
      events.push(event);
    }

    expect(events).toEqual([{
      event: "error",
      data: { code: "STREAM_FAILED", message: "回答中断" },
    }]);
  });
});
