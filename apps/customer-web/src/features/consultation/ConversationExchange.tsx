import {
  Bot,
  CircleUserRound,
  Headphones,
  RefreshCw,
  ThumbsDown,
  ThumbsUp,
} from "lucide-react";
import type { ReactNode } from "react";

import type { FeedbackRequest } from "../../domain/consultation";
import type { ConsultationExchange } from "./consultation-state";
import { MobileCitations } from "./CitationRail";

interface ConversationExchangeProps {
  exchange: ConsultationExchange;
  actionsDisabled: boolean;
  onHelpful: (exchange: ConsultationExchange) => void;
  onNotHelpful: (exchange: ConsultationExchange) => void;
  onRetry: (exchange: ConsultationExchange) => void;
  onHandoff: (exchange: ConsultationExchange) => void;
}

export function ConversationExchange({
  exchange,
  actionsDisabled,
  onHelpful,
  onNotHelpful,
  onRetry,
  onHandoff,
}: ConversationExchangeProps) {
  return (
    <article className="exchange">
      <div className="customer-message-row">
        <div className="customer-message">{exchange.question}</div>
        <CircleUserRound aria-hidden="true" className="customer-avatar" size={38} />
      </div>

      <div className="assistant-row">
        <div className="assistant-avatar"><Bot aria-hidden="true" size={22} /></div>
        <div className="assistant-content">
          <div className={`assistant-message assistant-message--${exchange.status}`}>
            {exchange.answer ? <p>{exchange.answer}</p> : null}
            {exchange.refused ? (
              <p className="refusal-message">{exchange.refusalReason ?? "当前信息不足，无法给出可靠回答。"}</p>
            ) : null}
            {exchange.status === "streaming" ? (
              <span className="stream-status"><i aria-hidden="true" />正在生成...</span>
            ) : null}
            {exchange.status === "error" || exchange.status === "cancelled" ? (
              <p className="inline-error" role="alert">{exchange.error}</p>
            ) : null}
            <MobileCitations citations={exchange.citations} />
          </div>

          {exchange.status === "completed" ? (
            <div aria-label="回答操作" className="answer-actions">
              <ActionButton
                active={exchange.feedback === "HELPFUL"}
                disabled={actionsDisabled || exchange.feedback !== null}
                icon={<ThumbsUp aria-hidden="true" size={17} />}
                label="有帮助"
                onClick={() => onHelpful(exchange)}
              />
              <ActionButton
                active={exchange.feedback === "NOT_HELPFUL"}
                disabled={actionsDisabled || exchange.feedback !== null}
                icon={<ThumbsDown aria-hidden="true" size={17} />}
                label="没帮助"
                onClick={() => onNotHelpful(exchange)}
              />
              <ActionButton
                disabled={actionsDisabled}
                icon={<RefreshCw aria-hidden="true" size={17} />}
                label="重新生成"
                onClick={() => onRetry(exchange)}
              />
              <ActionButton
                danger
                disabled={actionsDisabled || exchange.handoff !== null}
                icon={<Headphones aria-hidden="true" size={17} />}
                label="转人工"
                onClick={() => onHandoff(exchange)}
              />
            </div>
          ) : null}

          {exchange.feedback ? <p className="action-confirmation">感谢反馈</p> : null}
          {exchange.handoff ? (
            <p className="handoff-confirmation" role="status">
              已受理，工单编号 {exchange.handoff.taskId}
              {exchange.handoff.duplicate ? "（已存在）" : ""}
            </p>
          ) : null}

        </div>
      </div>
    </article>
  );
}

interface ActionButtonProps {
  label: string;
  icon: ReactNode;
  disabled: boolean;
  active?: boolean;
  danger?: boolean;
  onClick: () => void;
}

function ActionButton({ label, icon, disabled, active, danger, onClick }: ActionButtonProps) {
  return (
    <button
      aria-pressed={active ?? false}
      className={`action-button${danger ? " action-button--danger" : ""}`}
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      {icon}
      <span>{label}</span>
    </button>
  );
}

export type FeedbackRating = FeedbackRequest["rating"];
