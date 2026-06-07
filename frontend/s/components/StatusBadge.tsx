export function StatusBadge({
  ok,
  okText,
  badText
}: {
  ok: boolean;
  okText: string;
  badText: string;
}) {
  return (
    <span className={ok ? "statusBadge statusBadgeOk" : "statusBadge statusBadgeBad"}>
      <i />
      {ok ? okText : badText}
    </span>
  );
}
