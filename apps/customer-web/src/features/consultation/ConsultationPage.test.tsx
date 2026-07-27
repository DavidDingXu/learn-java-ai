import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { CustomerBffClient } from "../../api/customer-bff-client";
import type { CustomerStreamEvent } from "../../domain/consultation";
import { ConsultationPage } from "./ConsultationPage";

async function* answerEvents(): AsyncGenerator<CustomerStreamEvent> {
  yield { type: "session", conversationId: "c-1", attemptId: "a-1", retryOfAttemptId: null };
  yield { type: "metadata", traceId: "trace-1" };
  yield { type: "delta", text: "退款通常会在 3-5 个工作日内原路到账。" };
  yield {
    type: "citation",
    citation: {
      documentId: "refund-policy",
      version: "1",
      sectionId: "10",
      title: "退款政策",
    },
  };
  yield { type: "completed", refused: false, refusalReason: null };
}

function clientStub(): CustomerBffClient {
  return {
    streamAnswer: vi.fn(() => answerEvents()),
    recordFeedback: vi.fn().mockResolvedValue(undefined),
    retry: vi.fn().mockResolvedValue({
      conversationId: "c-1",
      attemptId: "a-2",
      retryOfAttemptId: "a-1",
      answer: "重新生成的回答",
      citations: [],
      refused: false,
      refusalReason: null,
      traceId: "trace-2",
    }),
    handoff: vi.fn().mockResolvedValue({ taskId: "task-1", status: "ACCEPTED", duplicate: false }),
  };
}

describe("ConsultationPage", () => {
  it("aborts the active stream when the page unmounts", async () => {
    const user = userEvent.setup();
    const capturedSignals: AbortSignal[] = [];
    const client = clientStub();
    client.streamAnswer = vi.fn(async function* (_request, signal) {
      capturedSignals.push(signal);
      await new Promise<void>((resolve) => signal.addEventListener("abort", () => resolve(), { once: true }));
    });
    const view = render(<ConsultationPage client={client} onSignOut={() => undefined} />);

    await user.click(screen.getByRole("button", { name: "退款多久到账？" }));
    await waitFor(() => expect(capturedSignals).toHaveLength(1));
    view.unmount();

    expect(capturedSignals[0]?.aborted).toBe(true);
  });

  it("renders the streaming answer, citation and completed-attempt actions", async () => {
    const user = userEvent.setup();
    const client = clientStub();
    render(<ConsultationPage client={client} onSignOut={() => undefined} />);

    expect(screen.getByRole("button", { name: "发送" })).toHaveAttribute("aria-label", "发送");
    await user.type(screen.getByLabelText("咨询问题"), "退款审核通过后多久到账？");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findByText("退款通常会在 3-5 个工作日内原路到账。")).toBeVisible();
    expect(screen.getAllByText("退款政策").length).toBeGreaterThan(0);
    expect(screen.getAllByText("章节 10 · 版本 1").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: "有帮助" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "重新生成" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "转人工" })).toBeEnabled();
    expect(screen.queryByText("技术详情")).not.toBeInTheDocument();
    expect(screen.queryByText(/trace-1/)).not.toBeInTheDocument();
  });

  it("renders the refusal reason from the completed event", async () => {
    const user = userEvent.setup();
    const client = clientStub();
    client.streamAnswer = vi.fn(async function* () {
      yield {
        type: "session" as const,
        conversationId: "c-1",
        attemptId: "a-1",
        retryOfAttemptId: null,
      };
      yield { type: "delta" as const, text: "当前资料无法确认这笔退款的审核状态。" };
      yield {
        type: "completed" as const,
        refused: true,
        refusalReason: "缺少退款审核状态",
      };
    });
    render(<ConsultationPage client={client} onSignOut={() => undefined} />);

    await user.type(screen.getByLabelText("咨询问题"), "我的退款通过了吗？");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findByText("当前资料无法确认这笔退款的审核状态。")).toBeVisible();
    expect(screen.getByText("缺少退款审核状态")).toBeVisible();
  });

  it("requires a reason for negative feedback and shows the accepted handoff receipt", async () => {
    const user = userEvent.setup();
    const client = clientStub();
    render(<ConsultationPage client={client} onSignOut={() => undefined} />);

    await user.type(screen.getByLabelText("咨询问题"), "退款审核通过后多久到账？");
    await user.click(screen.getByRole("button", { name: "发送" }));
    await screen.findByRole("button", { name: "没帮助" });

    await user.click(screen.getByRole("button", { name: "没帮助" }));
    await user.click(screen.getByRole("button", { name: "提交反馈" }));
    expect(screen.getByText("请选择没有帮助的原因")).toBeVisible();

    await user.selectOptions(screen.getByLabelText("没有帮助的原因"), "ANSWER_INCOMPLETE");
    await user.click(screen.getByRole("button", { name: "提交反馈" }));
    await waitFor(() => expect(client.recordFeedback).toHaveBeenCalledWith(
      "c-1",
      "a-1",
      expect.objectContaining({ rating: "NOT_HELPFUL", reasonCode: "ANSWER_INCOMPLETE" }),
    ));

    await user.click(screen.getByRole("button", { name: "转人工" }));
    await user.selectOptions(screen.getByLabelText("转人工原因"), "REFUND_OVERDUE");
    await user.click(screen.getByRole("button", { name: "确认转人工" }));
    expect(await screen.findByText("已受理，工单编号 task-1")).toBeVisible();
  });
});
