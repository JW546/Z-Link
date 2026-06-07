export function AssistantAvatar({
  accent,
  name,
  compact = false
}: {
  accent: string;
  name: string;
  compact?: boolean;
}) {
  return (
    <div
      className={compact ? "assistantAvatar assistantAvatarCompact" : "assistantAvatar"}
      style={{ ["--assistant-accent" as string]: accent }}
      aria-label={`${name} assistant avatar`}
    >
      <div className="assistantSphereGlow" />
      <div className="assistantSphereCore" />
      <div className="assistantSphereRing" />
    </div>
  );
}
