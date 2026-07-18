import { Headphones, LogOut, MessageCircleQuestion } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import type { CustomerBffClient } from "../../api/customer-bff-client";
import type { FeedbackRequest } from "../../domain/consultation";
import { FeedbackPanel, HandoffPanel } from "./ActionPanels";
import { CitationRail } from "./CitationRail";
import { Composer } from "./Composer";
import { ConversationExchange } from "./ConversationExchange";
import type { ConsultationExchange } from "./consultation-state";
import { useConsultation } from "./useConsultation";

interface ConsultationPageProps {
  client: CustomerBffClient;
  onSignOut: () => void;
}

export function ConsultationPage({ client, onSignOut }: ConsultationPageProps) {
  const {
    state,
    pendingAction,
    actionError,
    isStreaming,
    send,
    stop,
    recordFeedback,
    retry,
    handoff,
    clearActionError,
  } = useConsultation(client);
  const [feedbackExchange, setFeedbackExchange] = useState<ConsultationExchange | null>(null);
  const [handoffExchange, setHandoffExchange] = useState<ConsultationExchange | null>(null);
  const conversationEnd = useRef<HTMLDivElement | null>(null);

  const latestCitations = useMemo(() => {
    for (let index = state.exchanges.length - 1; index >= 0; index -= 1) {
      const exchange = state.exchanges[index];
      if (exchange.citations.length > 0) return exchange.citations;
    }
    return [];
  }, [state.exchanges]);

  useEffect(() => {
    const anchor = conversationEnd.current;
    if (typeof anchor?.scrollIntoView === "function") {
      anchor.scrollIntoView({ block: "end", behavior: "smooth" });
    }
  }, [state.exchanges]);

  const submitHelpful = async (exchange: ConsultationExchange) => {
    await submitFeedback(exchange, { rating: "HELPFUL" });
  };

  const submitFeedback = async (exchange: ConsultationExchange, request: FeedbackRequest) => {
    clearActionError();
    await recordFeedback(exchange, request);
  };

  const submitHandoff = async (exchange: ConsultationExchange, reasonCode: string) => {
    clearActionError();
    await handoff(exchange, reasonCode);
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <Headphones aria-hidden="true" size={24} />
          <strong>客户服务</strong>
        </div>
        <div className="online-status"><i aria-hidden="true" />在线服务</div>
        <button className="icon-button signout-button" onClick={onSignOut} title="退出登录" type="button">
          <LogOut aria-hidden="true" size={19} />
          <span className="sr-only">退出登录</span>
        </button>
      </header>

      <div className="consultation-layout">
        <main className="conversation-column">
          <div className="conversation-heading">
            <span aria-hidden="true" />
            <h1>售后咨询</h1>
            <span aria-hidden="true" />
          </div>

          <div className="conversation-scroll" aria-live="polite">
            {state.exchanges.length === 0 ? (
              <div className="empty-conversation">
                <MessageCircleQuestion aria-hidden="true" size={34} />
                <h2>您好，请问有什么可以帮您？</h2>
                <div className="quick-questions">
                  {["退款多久到账？", "如何申请售后？", "怎么修改收货地址？"].map((question) => (
                    <button key={question} onClick={() => void send(question)} type="button">{question}</button>
                  ))}
                </div>
              </div>
            ) : null}

            {state.exchanges.map((exchange) => (
              <div key={exchange.localId}>
                <ConversationExchange
                  actionsDisabled={pendingAction !== null || isStreaming}
                  exchange={exchange}
                  onHandoff={setHandoffExchange}
                  onHelpful={(selected) => void submitHelpful(selected).catch(() => undefined)}
                  onNotHelpful={setFeedbackExchange}
                  onRetry={(selected) => void retry(selected)}
                />
                {feedbackExchange?.localId === exchange.localId ? (
                  <FeedbackPanel
                    busy={pendingAction === "feedback"}
                    exchange={exchange}
                    onCancel={() => setFeedbackExchange(null)}
                    onSubmit={submitFeedback}
                  />
                ) : null}
                {handoffExchange?.localId === exchange.localId ? (
                  <HandoffPanel
                    busy={pendingAction === "handoff"}
                    exchange={exchange}
                    onCancel={() => setHandoffExchange(null)}
                    onSubmit={submitHandoff}
                  />
                ) : null}
              </div>
            ))}
            {actionError ? <p className="global-error" role="alert">{actionError}</p> : null}
            <div ref={conversationEnd} />
          </div>

          <Composer onSend={send} onStop={stop} streaming={isStreaming} />
        </main>
        <CitationRail citations={latestCitations} />
      </div>
    </div>
  );
}
