import { KeyRound } from "lucide-react";
import { useMemo, useState } from "react";

import { HttpCustomerBffClient } from "./api/customer-bff-client";
import { ConsultationPage } from "./features/consultation/ConsultationPage";

const TOKEN_STORAGE_KEY = "java-ai-customer-web.access-token";

export default function App() {
  const [accessToken, setAccessToken] = useState(() => sessionStorage.getItem(TOKEN_STORAGE_KEY));
  const client = useMemo(() => new HttpCustomerBffClient({
    baseUrl: import.meta.env.VITE_CUSTOMER_BFF_BASE_URL ?? "",
    accessToken: () => accessToken,
  }), [accessToken]);

  if (!accessToken) {
    return <AccessGate onAuthenticated={(token) => {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
      setAccessToken(token);
    }} />;
  }

  return <ConsultationPage client={client} onSignOut={() => {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    setAccessToken(null);
  }} />;
}

function AccessGate({ onAuthenticated }: { onAuthenticated: (token: string) => void }) {
  const [developmentToken, setDevelopmentToken] = useState("");
  const loginUrl = import.meta.env.VITE_CUSTOMER_LOGIN_URL ?? "/oauth2/authorization/customer";

  return (
    <main className="access-gate">
      <div className="access-gate__content">
        <KeyRound aria-hidden="true" size={28} />
        <h1>登录状态已失效</h1>
        <p>请重新登录后继续咨询。</p>
        <a className="primary-button" href={loginUrl}>重新登录</a>
        {import.meta.env.DEV ? (
          <details className="development-token">
            <summary>本地联调</summary>
            <label htmlFor="development-token">短时开发令牌</label>
            <textarea
              id="development-token"
              onChange={(event) => setDevelopmentToken(event.target.value)}
              rows={4}
              value={developmentToken}
            />
            <button
              className="secondary-button"
              disabled={!developmentToken.trim()}
              onClick={() => onAuthenticated(developmentToken.trim())}
              type="button"
            >
              进入咨询
            </button>
          </details>
        ) : null}
      </div>
    </main>
  );
}
