import { useState } from "react";

import type { FeedbackRequest } from "../../domain/consultation";
import type { ConsultationExchange } from "./consultation-state";

interface FeedbackPanelProps {
  exchange: ConsultationExchange;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (exchange: ConsultationExchange, request: FeedbackRequest) => Promise<void>;
}

export function FeedbackPanel({ exchange, busy, onCancel, onSubmit }: FeedbackPanelProps) {
  const [reasonCode, setReasonCode] = useState("");
  const [comment, setComment] = useState("");
  const [validation, setValidation] = useState<string | null>(null);

  const submit = async () => {
    if (!reasonCode) {
      setValidation("请选择没有帮助的原因");
      return;
    }
    setValidation(null);
    try {
      await onSubmit(exchange, {
        rating: "NOT_HELPFUL",
        reasonCode,
        ...(comment.trim() ? { comment: comment.trim() } : {}),
      });
      onCancel();
    } catch {
      // The page renders the normalized API error and keeps this panel open.
    }
  };

  return (
    <section aria-label="反馈详情" className="action-panel">
      <div className="field-group">
        <label htmlFor="feedback-reason">没有帮助的原因</label>
        <select
          id="feedback-reason"
          onChange={(event) => setReasonCode(event.target.value)}
          value={reasonCode}
        >
          <option value="">请选择</option>
          <option value="ANSWER_INCOMPLETE">回答不完整</option>
          <option value="ANSWER_INACCURATE">信息不准确</option>
          <option value="SOURCE_NOT_HELPFUL">参考来源没有帮助</option>
          <option value="OTHER">其他原因</option>
        </select>
      </div>
      <div className="field-group">
        <label htmlFor="feedback-comment">补充说明（选填）</label>
        <textarea
          id="feedback-comment"
          maxLength={500}
          onChange={(event) => setComment(event.target.value)}
          rows={3}
          value={comment}
        />
      </div>
      {validation ? <p className="validation-error" role="alert">{validation}</p> : null}
      <PanelActions busy={busy} onCancel={onCancel} onConfirm={() => void submit()} confirmLabel="提交反馈" />
    </section>
  );
}

interface HandoffPanelProps {
  exchange: ConsultationExchange;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (exchange: ConsultationExchange, reasonCode: string) => Promise<void>;
}

export function HandoffPanel({ exchange, busy, onCancel, onSubmit }: HandoffPanelProps) {
  const [reasonCode, setReasonCode] = useState("");
  const [validation, setValidation] = useState<string | null>(null);

  const submit = async () => {
    if (!reasonCode) {
      setValidation("请选择转人工原因");
      return;
    }
    setValidation(null);
    try {
      await onSubmit(exchange, reasonCode);
      onCancel();
    } catch {
      // The page renders the normalized API error and keeps this panel open.
    }
  };

  return (
    <section aria-label="转人工详情" className="action-panel action-panel--handoff">
      <div className="field-group">
        <label htmlFor="handoff-reason">转人工原因</label>
        <select
          id="handoff-reason"
          onChange={(event) => setReasonCode(event.target.value)}
          value={reasonCode}
        >
          <option value="">请选择</option>
          <option value="REFUND_OVERDUE">退款超过预计时间</option>
          <option value="NEED_ORDER_CHECK">需要核对订单</option>
          <option value="REQUEST_HUMAN">希望人工处理</option>
        </select>
      </div>
      {validation ? <p className="validation-error" role="alert">{validation}</p> : null}
      <PanelActions busy={busy} onCancel={onCancel} onConfirm={() => void submit()} confirmLabel="确认转人工" />
    </section>
  );
}

function PanelActions({
  busy,
  onCancel,
  onConfirm,
  confirmLabel,
}: {
  busy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  confirmLabel: string;
}) {
  return (
    <div className="panel-actions">
      <button className="text-button" disabled={busy} onClick={onCancel} type="button">取消</button>
      <button className="primary-button" disabled={busy} onClick={onConfirm} type="button">
        {busy ? "提交中..." : confirmLabel}
      </button>
    </div>
  );
}
