import { Send, Square } from "lucide-react";
import { useState, type KeyboardEvent } from "react";

interface ComposerProps {
  streaming: boolean;
  onSend: (question: string) => Promise<void>;
  onStop: () => void;
}

export function Composer({ streaming, onSend, onStop }: ComposerProps) {
  const [question, setQuestion] = useState("");

  const submit = async () => {
    const value = question.trim();
    if (!value || streaming) return;
    setQuestion("");
    await onSend(value);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    void submit();
  };

  return (
    <div className="composer-shell">
      <div className="composer">
        <textarea
          aria-label="咨询问题"
          maxLength={2000}
          onChange={(event) => setQuestion(event.target.value)}
          onKeyDown={onKeyDown}
          placeholder="请输入你的问题"
          rows={1}
          value={question}
        />
        {streaming ? (
          <button className="icon-button stop-button" onClick={onStop} title="停止生成" type="button">
            <Square aria-hidden="true" size={17} />
            <span className="sr-only">停止生成</span>
          </button>
        ) : null}
        <button
          aria-label="发送"
          className="primary-button send-button"
          disabled={streaming || question.trim().length === 0}
          onClick={() => void submit()}
          type="button"
        >
          <Send aria-hidden="true" size={18} />
          <span>发送</span>
        </button>
      </div>
      <span className="composer-hint">Enter 发送，Shift+Enter 换行</span>
    </div>
  );
}
