import { BookOpen, FileText } from "lucide-react";

import type { Citation } from "../../domain/consultation";

interface CitationRailProps {
  citations: Citation[];
}

export function CitationRail({ citations }: CitationRailProps) {
  return (
    <aside aria-label="参考来源" className="citation-rail">
      <div className="rail-heading">
        <BookOpen aria-hidden="true" size={19} />
        <h2>参考来源</h2>
      </div>
      {citations.length === 0 ? (
        <p className="empty-rail">暂无参考来源</p>
      ) : (
        <ol className="citation-list">
          {citations.map((citation) => (
            <li key={`${citation.documentId}:${citation.version}:${citation.sectionId}`}>
              <CitationContent citation={citation} />
            </li>
          ))}
        </ol>
      )}
    </aside>
  );
}

export function MobileCitations({ citations }: CitationRailProps) {
  if (citations.length === 0) return null;
  return (
    <details className="mobile-citations">
      <summary>{citations.length} 条参考来源</summary>
      <ol>
        {citations.map((citation) => (
          <li key={`${citation.documentId}:${citation.version}:${citation.sectionId}`}>
            <CitationContent citation={citation} />
          </li>
        ))}
      </ol>
    </details>
  );
}

function CitationContent({ citation }: { citation: Citation }) {
  return (
    <div className="citation-item">
      <FileText aria-hidden="true" size={18} />
      <div>
        <strong>{citation.title}</strong>
        <span>章节 {citation.sectionId} · 版本 {citation.version}</span>
      </div>
    </div>
  );
}
