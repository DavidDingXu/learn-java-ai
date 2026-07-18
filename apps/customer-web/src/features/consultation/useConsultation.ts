import { useCallback, useEffect, useReducer, useRef, useState } from "react";

import { CustomerBffApiError, type CustomerBffClient } from "../../api/customer-bff-client";
import type { FeedbackRequest } from "../../domain/consultation";
import {
  consultationReducer,
  initialConsultationState,
  type ConsultationExchange,
} from "./consultation-state";

type PendingAction = "feedback" | "retry" | "handoff" | null;

export function useConsultation(client: CustomerBffClient) {
  const [state, dispatch] = useReducer(consultationReducer, initialConsultationState);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  const send = useCallback(async (question: string) => {
    const normalized = question.trim();
    if (!normalized || controllerRef.current) return;

    const localId = createLocalId();
    const controller = new AbortController();
    controllerRef.current = controller;
    setIsStreaming(true);
    setActionError(null);
    dispatch({ type: "stream-started", localId, question: normalized });

    try {
      for await (const event of client.streamAnswer({
        ...(state.conversationId ? { conversationId: state.conversationId } : {}),
        question: normalized,
      }, controller.signal)) {
        dispatch({ type: "stream-event", localId, event });
        if (event.type === "error") break;
      }
      if (controller.signal.aborted) {
        dispatch({ type: "stream-cancelled", localId });
      }
    } catch (error) {
      if (controller.signal.aborted || isAbortError(error)) {
        dispatch({ type: "stream-cancelled", localId });
      } else {
        dispatch({ type: "stream-failed", localId, message: errorMessage(error) });
      }
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setIsStreaming(false);
    }
  }, [client, state.conversationId]);

  const stop = useCallback(() => {
    controllerRef.current?.abort();
  }, []);

  const recordFeedback = useCallback(async (
    exchange: ConsultationExchange,
    request: FeedbackRequest,
  ) => {
    const identity = completedIdentity(exchange);
    setPendingAction("feedback");
    setActionError(null);
    try {
      await client.recordFeedback(identity.conversationId, identity.attemptId, request);
      dispatch({ type: "feedback-recorded", localId: exchange.localId, rating: request.rating });
    } catch (error) {
      setActionError(errorMessage(error));
      throw error;
    } finally {
      setPendingAction(null);
    }
  }, [client]);

  const retry = useCallback(async (exchange: ConsultationExchange) => {
    const identity = completedIdentity(exchange);
    setPendingAction("retry");
    setActionError(null);
    try {
      const answer = await client.retry(identity.conversationId, identity.attemptId);
      dispatch({
        type: "retry-succeeded",
        localId: createLocalId(),
        question: exchange.question,
        answer,
      });
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setPendingAction(null);
    }
  }, [client]);

  const handoff = useCallback(async (exchange: ConsultationExchange, reasonCode: string) => {
    const identity = completedIdentity(exchange);
    setPendingAction("handoff");
    setActionError(null);
    try {
      const receipt = await client.handoff(identity.conversationId, identity.attemptId, reasonCode);
      dispatch({ type: "handoff-accepted", localId: exchange.localId, receipt });
    } catch (error) {
      setActionError(errorMessage(error));
      throw error;
    } finally {
      setPendingAction(null);
    }
  }, [client]);

  return {
    state,
    pendingAction,
    actionError,
    isStreaming,
    send,
    stop,
    recordFeedback,
    retry,
    handoff,
    clearActionError: () => setActionError(null),
  };
}

function completedIdentity(exchange: ConsultationExchange): {
  conversationId: string;
  attemptId: string;
} {
  if (exchange.status !== "completed" || !exchange.conversationId || !exchange.attemptId) {
    throw new Error("Only a completed answer attempt can use this action");
  }
  return { conversationId: exchange.conversationId, attemptId: exchange.attemptId };
}

function createLocalId(): string {
  return globalThis.crypto?.randomUUID?.()
    ?? `exchange-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

function errorMessage(error: unknown): string {
  if (error instanceof CustomerBffApiError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return "请求失败，请稍后重试";
}
