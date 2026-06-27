import { useEffect, useMemo, useState } from "react";
import { showError } from "../components/ErrorModal";
import { apiFetch } from "../api";
import { useAuth } from "../auth";

type StudentDto = {
  id: number;
  realName: string | null;
  studentNo: string | null;
  major: string | null;
  grade: number | null;
  className: string | null;
};

type ChannelStat = { sent: number; failed: number };

type NotificationStats = {
  total: number;
  read: number;
  unread: number;
  failed: number;
  partialFailed: number;
  channels: Record<string, ChannelStat>;
};

type NotificationSummary = {
  id: number;
  title: string;
  content: string;
  tags: string[];
  channels: string[];
  attachmentName: string | null;
  attachmentUrl: string | null;
  expireAt: string | null;
  creator: { id: number; name: string };
  createdAt: string;
  stats: NotificationStats;
};

type DeliveryView = {
  channel: string;
  status: string;
  providerMessage: string | null;
  sentAt: string | null;
};

type RecipientView = {
  recipientId: number;
  studentId: number;
  studentName: string | null;
  studentNo: string | null;
  className: string | null;
  deliveryStatus: string;
  readStatus: string;
  readAt: string | null;
  deliveries: DeliveryView[];
};

type NotificationDetail = NotificationSummary & {
  recipients: RecipientView[];
};

type InboxItem = {
  id: number;
  title: string;
  content: string;
  tags: string[];
  channels: string[];
  attachmentName: string | null;
  attachmentUrl: string | null;
  expireAt: string | null;
  deliveryStatus: string;
  readStatus: string;
  readAt: string | null;
  creator: { id: number; name: string };
  createdAt: string;
  deliveries: DeliveryView[];
};

const CHANNEL_OPTIONS = [
  { code: "IN_APP", label: "站内" },
  { code: "EMAIL", label: "邮件" },
];

export function NotificationsPage() {
  const auth = useAuth();
  const canPublish = auth.hasRole("TEACHER", "LEADER", "CADRE");
  const isStudent = auth.hasRole("STUDENT");
  const [students, setStudents] = useState<StudentDto[]>([]);
  const [sentItems, setSentItems] = useState<NotificationSummary[]>([]);
  const [selectedDetail, setSelectedDetail] = useState<NotificationDetail | null>(null);
  const [inboxItems, setInboxItems] = useState<InboxItem[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [attachmentName, setAttachmentName] = useState("");
  const [attachmentUrl, setAttachmentUrl] = useState("");
  const [expireAt, setExpireAt] = useState("");
  const [tagsInput, setTagsInput] = useState("");
  const [gradesInput, setGradesInput] = useState("");
  const [classNamesInput, setClassNamesInput] = useState("");
  const [majorsInput, setMajorsInput] = useState("");
  const [selectedStudentIds, setSelectedStudentIds] = useState<number[]>([]);
  const [channels, setChannels] = useState<string[]>(["IN_APP", "EMAIL"]);
  const [submitting, setSubmitting] = useState(false);

  async function refreshTeacherData() {
    if (!canPublish) return;
    try {
      const [studentList, sentList] = await Promise.all([
        apiFetch<StudentDto[]>("/students", { method: "GET" }),
        apiFetch<NotificationSummary[]>("/notifications/sent", { method: "GET" }),
      ]);
      setStudents(studentList);
      setSentItems(sentList);
      if (selectedDetail) {
        const detail = await apiFetch<NotificationDetail>(`/notifications/${selectedDetail.id}`, { method: "GET" });
        setSelectedDetail(detail);
      }
    } catch (e) {
      showError((e as Error).message);
    }
  }

  async function refreshStudentData() {
    if (!isStudent) return;
    try {
      const list = await apiFetch<InboxItem[]>("/notifications/my", { method: "GET" });
      setInboxItems(list);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  useEffect(() => {
    void refreshTeacherData();
  }, [canPublish]);

  useEffect(() => {
    void refreshStudentData();
  }, [isStudent]);

  const availableGrades = useMemo(
    () => Array.from(new Set(students.map((item) => item.grade).filter((value): value is number => value != null))).sort(),
    [students],
  );
  const availableClasses = useMemo(
    () => Array.from(new Set(students.map((item) => item.className).filter((value): value is string => Boolean(value)))).sort(),
    [students],
  );
  const availableMajors = useMemo(
    () => Array.from(new Set(students.map((item) => item.major).filter((value): value is string => Boolean(value)))).sort(),
    [students],
  );

  async function createNotification() {
    if (!title.trim() || !content.trim()) {
      showError("请填写通知标题和正文。");
      return;
    }
    if (channels.length === 0) {
      showError("请至少选择一个发送渠道。");
      return;
    }
    setSubmitting(true);
    try {
      const detail = await apiFetch<NotificationDetail>("/notifications", {
        method: "POST",
        body: JSON.stringify({
          title: title.trim(),
          content: content.trim(),
          attachmentName: attachmentName.trim() || null,
          attachmentUrl: attachmentUrl.trim() || null,
          expireAt: expireAt ? new Date(expireAt).toISOString() : null,
          tags: parseCsv(tagsInput),
          channels,
          target: {
            studentIds: selectedStudentIds,
            grades: parseCsv(gradesInput).map((item) => Number(item)).filter((value) => Number.isFinite(value)),
            classNames: parseCsv(classNamesInput),
            majors: parseCsv(majorsInput),
          },
        }),
      });
      setTitle("");
      setContent("");
      setAttachmentName("");
      setAttachmentUrl("");
      setExpireAt("");
      setTagsInput("");
      setGradesInput("");
      setClassNamesInput("");
      setMajorsInput("");
      setSelectedStudentIds([]);
      setChannels(["IN_APP", "EMAIL"]);
      setSelectedDetail(detail);
      await refreshTeacherData();
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  async function openDetail(id: number) {
    try {
      const detail = await apiFetch<NotificationDetail>(`/notifications/${id}`, { method: "GET" });
      setSelectedDetail(detail);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  async function markRead(id: number) {
    try {
      await apiFetch(`/notifications/${id}/read`, { method: "POST" });
      await refreshStudentData();
    } catch (e) {
      showError((e as Error).message);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>通知中心</h2>
        <div className="row" style={{ justifyContent: "flex-end" }}>
          {canPublish ? <button className="btn" onClick={() => void refreshTeacherData()}>刷新发布记录</button> : null}
          {isStudent ? <button className="btn" onClick={() => void refreshStudentData()}>刷新收件箱</button> : null}
        </div>
      </div>

      {canPublish ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 700 }}>发布通知</div>
            <span className="badge">多渠道发送 + 状态追踪</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              <div className="row" style={{ alignItems: "stretch" }}>
                <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="通知标题" style={{ flex: 1 }} />
                <input className="input" type="datetime-local" value={expireAt} onChange={(e) => setExpireAt(e.target.value)} style={{ width: 240 }} />
              </div>
              <textarea className="input" rows={5} value={content} onChange={(e) => setContent(e.target.value)} placeholder="通知正文" />
              <div className="row" style={{ alignItems: "stretch" }}>
                <input className="input" value={attachmentName} onChange={(e) => setAttachmentName(e.target.value)} placeholder="附件名称（可选）" style={{ flex: 1 }} />
                <input className="input" value={attachmentUrl} onChange={(e) => setAttachmentUrl(e.target.value)} placeholder="附件链接（可选）" style={{ flex: 2 }} />
              </div>
              <div className="row" style={{ alignItems: "stretch" }}>
                <input className="input" value={tagsInput} onChange={(e) => setTagsInput(e.target.value)} placeholder="标签，逗号分隔，例如：就业,实习,奖助" style={{ flex: 1 }} />
                <input className="input" value={gradesInput} onChange={(e) => setGradesInput(e.target.value)} placeholder={`年级筛选，如 ${availableGrades.join(",") || "2026"}`} style={{ flex: 1 }} />
              </div>
              <div className="row" style={{ alignItems: "stretch" }}>
                <input className="input" value={classNamesInput} onChange={(e) => setClassNamesInput(e.target.value)} placeholder={`班级筛选，如 ${availableClasses.join(",") || "2026-1班"}`} style={{ flex: 1 }} />
                <input className="input" value={majorsInput} onChange={(e) => setMajorsInput(e.target.value)} placeholder={`专业筛选，如 ${availableMajors.join(",") || "计算机科学与技术"}`} style={{ flex: 1 }} />
              </div>

              <div className="grid" style={{ gap: 8 }}>
                <div style={{ fontWeight: 700, fontSize: 14 }}>发送渠道</div>
                <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                  {CHANNEL_OPTIONS.map((option) => (
                    <label key={option.code} className="badge" style={{ display: "inline-flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
                      <input
                        type="checkbox"
                        checked={channels.includes(option.code)}
                        onChange={(e) => {
                          setChannels((prev) =>
                            e.target.checked ? Array.from(new Set([...prev, option.code])) : prev.filter((item) => item !== option.code),
                          );
                        }}
                      />
                      {option.label}
                    </label>
                  ))}
                </div>
              </div>

              <div className="grid" style={{ gap: 8 }}>
                <div style={{ fontWeight: 700, fontSize: 14 }}>指定学生（可选，未选则按筛选条件命中全部）</div>
                <select
                  multiple
                  className="input"
                  value={selectedStudentIds.map(String)}
                  onChange={(e) => {
                    const values = Array.from(e.target.selectedOptions).map((item) => Number((item as HTMLOptionElement).value));
                    setSelectedStudentIds(values);
                  }}
                  style={{ minHeight: 140 }}
                >
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {(student.realName ?? "未命名") + " / " + (student.className ?? "-") + " / " + (student.studentNo ?? "-")}
                    </option>
                  ))}
                </select>
              </div>

              <div className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                <div className="muted" style={{ fontSize: 12 }}>
                  邮件渠道会使用发布人自己的 SMTP 配置发送；未配置收件邮箱的学生会在追踪中标记失败。
                </div>
                <button className="btn btnPrimary" onClick={() => void createNotification()} disabled={submitting}>
                  {submitting ? "发布中…" : "发布通知"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {canPublish ? (
        <div className="grid" style={{ gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 1fr)", gap: 12 }}>
          <div className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 700 }}>已发布通知</div>
              <span className="badge">{sentItems.length}</span>
            </div>
            <div className="cardBody">
              <div className="grid" style={{ gap: 10 }}>
                {sentItems.map((item) => (
                  <div key={item.id} className="card" style={{ boxShadow: "none" }}>
                    <div className="cardBody">
                      <div className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                        <div style={{ flex: 1, minWidth: 220 }}>
                          <div style={{ fontWeight: 700 }}>{item.title}</div>
                          <div className="muted" style={{ marginTop: 6, fontSize: 13, lineHeight: 1.5 }}>
                            {clip(item.content, 100)}
                          </div>
                          <div className="row" style={{ marginTop: 8, gap: 6, flexWrap: "wrap" }}>
                            <span className="badge">总计 {item.stats.total}</span>
                            <span className="badge badgeOk">已读 {item.stats.read}</span>
                            <span className="badge badgeWarn">未读 {item.stats.unread}</span>
                            {item.stats.failed ? <span className="badge badgeDanger">失败 {item.stats.failed}</span> : null}
                            {item.stats.partialFailed ? <span className="badge badgeWarn">部分失败 {item.stats.partialFailed}</span> : null}
                          </div>
                        </div>
                        <button className="btn" onClick={() => void openDetail(item.id)}>
                          查看追踪
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
                {sentItems.length === 0 ? <div className="muted">暂无已发布通知。</div> : null}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 700 }}>发送追踪</div>
              <span className="badge">{selectedDetail ? `#${selectedDetail.id}` : "未选择"}</span>
            </div>
            <div className="cardBody">
              {selectedDetail ? (
                <div className="grid" style={{ gap: 10 }}>
                  <div>
                    <div style={{ fontWeight: 700 }}>{selectedDetail.title}</div>
                    <div className="muted" style={{ marginTop: 4, fontSize: 13 }}>
                      发布人：{selectedDetail.creator.name}，时间：{formatDateTime(selectedDetail.createdAt)}
                    </div>
                  </div>
                  <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                    {selectedDetail.channels.map((channel) => {
                      const stat = selectedDetail.stats.channels[channel];
                      return (
                        <span key={channel} className="badge">
                          {channel}: 成功 {stat?.sent ?? 0} / 失败 {stat?.failed ?? 0}
                        </span>
                      );
                    })}
                  </div>
                  <div className="grid" style={{ gap: 8 }}>
                    {selectedDetail.recipients.map((recipient) => (
                      <div key={recipient.recipientId} className="card" style={{ boxShadow: "none" }}>
                        <div className="cardBody">
                          <div className="row" style={{ justifyContent: "space-between", gap: 8, flexWrap: "wrap" }}>
                            <div>
                              <div style={{ fontWeight: 700 }}>{recipient.studentName ?? "未命名学生"}</div>
                              <div className="row" style={{ marginTop: 6, gap: 6 }}>
                                {recipient.studentNo ? <span className="badge">{recipient.studentNo}</span> : null}
                                {recipient.className ? <span className="badge badgePrimary">{recipient.className}</span> : null}
                                <span className={badgeForDelivery(recipient.deliveryStatus)}>{recipient.deliveryStatus}</span>
                                <span className={recipient.readStatus === "READ" ? "badge badgeOk" : "badge badgeWarn"}>{recipient.readStatus}</span>
                              </div>
                            </div>
                            {recipient.readAt ? <div className="muted" style={{ fontSize: 12 }}>已读于 {formatDateTime(recipient.readAt)}</div> : null}
                          </div>
                          <div className="grid" style={{ gap: 6, marginTop: 10 }}>
                            {recipient.deliveries.map((delivery) => (
                              <div key={`${recipient.recipientId}-${delivery.channel}`} className="row" style={{ justifyContent: "space-between", gap: 8, flexWrap: "wrap" }}>
                                <span className="badge">{delivery.channel}</span>
                                <span className={delivery.status === "FAILED" ? "badge badgeDanger" : "badge badgeOk"}>{delivery.status}</span>
                                <span className="muted" style={{ fontSize: 12 }}>{delivery.providerMessage ?? "-"}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="muted">选择左侧一条通知后可查看每个学生、每个渠道的送达和已读状态。</div>
              )}
            </div>
          </div>
        </div>
      ) : null}

      {isStudent ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 700 }}>我的通知</div>
            <span className="badge">{inboxItems.length}</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              {inboxItems.map((item) => (
                <div key={item.id} className="card" style={{ boxShadow: "none" }}>
                  <div className="cardBody">
                    <div className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                      <div style={{ flex: 1, minWidth: 220 }}>
                        <div style={{ fontWeight: 700 }}>{item.title}</div>
                        <div className="muted" style={{ marginTop: 4, fontSize: 12 }}>
                          发布人：{item.creator.name}，时间：{formatDateTime(item.createdAt)}
                        </div>
                        <div style={{ marginTop: 10, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{item.content}</div>
                        <div className="row" style={{ marginTop: 10, gap: 6, flexWrap: "wrap" }}>
                          <span className={badgeForDelivery(item.deliveryStatus)}>{item.deliveryStatus}</span>
                          <span className={item.readStatus === "READ" ? "badge badgeOk" : "badge badgeWarn"}>{item.readStatus}</span>
                          {item.tags.map((tag) => <span key={tag} className="badge badgePrimary">{tag}</span>)}
                          {item.attachmentUrl ? <a href={item.attachmentUrl} target="_blank" rel="noreferrer" className="btn">打开附件</a> : null}
                        </div>
                        <div className="grid" style={{ gap: 6, marginTop: 10 }}>
                          {item.deliveries.map((delivery) => (
                            <div key={`${item.id}-${delivery.channel}`} className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                              <span className="badge">{delivery.channel}</span>
                              <span className={delivery.status === "FAILED" ? "badge badgeDanger" : "badge badgeOk"}>{delivery.status}</span>
                              <span className="muted" style={{ fontSize: 12 }}>{delivery.providerMessage ?? "-"}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                      {item.readStatus !== "READ" ? (
                        <button className="btn btnPrimary" onClick={() => void markRead(item.id)}>
                          标记已读
                        </button>
                      ) : null}
                    </div>
                  </div>
                </div>
              ))}
              {inboxItems.length === 0 ? <div className="muted">暂无通知。</div> : null}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function parseCsv(input: string) {
  return input
    .split(/[，,]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function clip(text: string, maxLen: number) {
  return text.length <= maxLen ? text : `${text.slice(0, maxLen)}...`;
}

function formatDateTime(value: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function badgeForDelivery(status: string) {
  if (status === "SENT") return "badge badgeOk";
  if (status === "FAILED") return "badge badgeDanger";
  if (status === "PARTIAL_FAILED") return "badge badgeWarn";
  return "badge";
}
