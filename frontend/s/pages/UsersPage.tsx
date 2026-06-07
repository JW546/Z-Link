import { ChangeEvent, useDeferredValue, useMemo, useState } from "react";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

const avatarPresets = ["#70d6ff", "#8cf1c8", "#f2c66d", "#f18aa6", "#9ea7ff", "#a7b1bc"];

type UserDraft = {
  displayName: string;
  email: string;
  phone: string;
  organization: string;
  title: string;
  bio: string;
  favoriteScene: string;
  avatarAccent: string;
  avatarImage: string;
};

export function UsersPage() {
  const currentUser = useAppStore((state) => state.currentUser);
  const users = useAppStore((state) => state.users);
  const locale = useAppStore((state) => state.preferences.locale);
  const updateCurrentUser = useAppStore((state) => state.updateCurrentUser);
  const [query, setQuery] = useState("");
  const [draft, setDraft] = useState<UserDraft>({
    displayName: currentUser?.displayName ?? "",
    email: currentUser?.email ?? "",
    phone: currentUser?.phone ?? "",
    organization: currentUser?.organization ?? "",
    title: currentUser?.title ?? "",
    bio: currentUser?.bio ?? "",
    favoriteScene: currentUser?.favoriteScene ?? "",
    avatarAccent: currentUser?.avatarAccent ?? "#70d6ff",
    avatarImage: currentUser?.avatarImage ?? ""
  });

  const deferredQuery = useDeferredValue(query);
  const filteredUsers = useMemo(() => {
    if (!deferredQuery.trim()) return users;
    return users.filter((user) =>
      `${user.displayName} ${user.username} ${user.organization} ${user.title}`
        .toLowerCase()
        .includes(deferredQuery.toLowerCase())
    );
  }, [deferredQuery, users]);

  const onAvatarFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result !== "string") return;
      setDraft((state) => ({ ...state, avatarImage: result }));
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">{t(locale, { "zh-CN": "Account", "en-US": "Account" })}</span>
            <h3>{t(locale, { "zh-CN": "用户中心", "en-US": "Users" })}</h3>
          </div>
          <label className="fieldLabel compactField searchField">
            {t(locale, { "zh-CN": "搜索", "en-US": "Search" })}
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t(locale, {
                "zh-CN": "搜索用户",
                "en-US": "Search users"
              })}
            />
          </label>
        </div>

        <div className="userCenterLayout userLayoutTight">
          <section className="profileEditor">
            <div className="profileHero">
              <div className="profileAvatar" style={{ background: draft.avatarAccent }}>
                {draft.avatarImage ? <img src={draft.avatarImage} alt={draft.displayName || "avatar"} /> : <span>{draft.displayName.slice(0, 1) || "Z"}</span>}
              </div>
              <div className="profileHeroText">
                <h4>{draft.displayName || currentUser?.displayName}</h4>
                <p>{draft.title || currentUser?.title}</p>
              </div>
            </div>

            <div className="avatarTools">
              <div className="avatarPalette">
                {avatarPresets.map((color) => (
                  <button
                    key={color}
                    type="button"
                    className={draft.avatarAccent === color ? "avatarSwatch active" : "avatarSwatch"}
                    style={{ background: color }}
                    onClick={() => setDraft((state) => ({ ...state, avatarAccent: color, avatarImage: state.avatarImage }))}
                    aria-label={`avatar-${color}`}
                  />
                ))}
              </div>
              <label className="ghostInline uploadAvatarButton">
                {t(locale, { "zh-CN": "上传头像", "en-US": "Upload Avatar" })}
                <input type="file" accept="image/*" onChange={onAvatarFileChange} hidden />
              </label>
            </div>

            <div className="formGrid">
              <label>
                {t(locale, { "zh-CN": "显示名", "en-US": "Display name" })}
                <input value={draft.displayName} onChange={(event) => setDraft((state) => ({ ...state, displayName: event.target.value }))} />
              </label>
              <label>
                {t(locale, { "zh-CN": "邮箱", "en-US": "Email" })}
                <input value={draft.email} onChange={(event) => setDraft((state) => ({ ...state, email: event.target.value }))} />
              </label>
              <label>
                {t(locale, { "zh-CN": "电话", "en-US": "Phone" })}
                <input value={draft.phone} onChange={(event) => setDraft((state) => ({ ...state, phone: event.target.value }))} />
              </label>
              <label>
                {t(locale, { "zh-CN": "组织", "en-US": "Organization" })}
                <input value={draft.organization} onChange={(event) => setDraft((state) => ({ ...state, organization: event.target.value }))} />
              </label>
              <label>
                {t(locale, { "zh-CN": "职位", "en-US": "Title" })}
                <input value={draft.title} onChange={(event) => setDraft((state) => ({ ...state, title: event.target.value }))} />
              </label>
              <label>
                {t(locale, { "zh-CN": "场景", "en-US": "Scene" })}
                <input value={draft.favoriteScene} onChange={(event) => setDraft((state) => ({ ...state, favoriteScene: event.target.value }))} />
              </label>
              <label className="fullWidth">
                {t(locale, { "zh-CN": "简介", "en-US": "Bio" })}
                <textarea value={draft.bio} onChange={(event) => setDraft((state) => ({ ...state, bio: event.target.value }))} />
              </label>
            </div>

            <button onClick={() => void updateCurrentUser(draft)}>
              {t(locale, { "zh-CN": "保存", "en-US": "Save" })}
            </button>
          </section>

          <section className="userDirectory">
            <h4>{t(locale, { "zh-CN": "全部用户", "en-US": "All Users" })}</h4>
            <div className="userGrid">
              {filteredUsers.map((user) => (
                <article key={user.username} className="userCardFlat">
                  <div className="userCardIdentity">
                    <div className="userMiniAvatar" style={{ background: user.avatarAccent }}>
                      {user.avatarImage ? <img src={user.avatarImage} alt={user.displayName} /> : <span>{user.displayName.slice(0, 1)}</span>}
                    </div>
                    <div>
                      <strong>{user.displayName}</strong>
                      <p>{user.title}</p>
                    </div>
                  </div>
                  <small>{user.organization}</small>
                </article>
              ))}
            </div>
          </section>
        </div>
      </section>
    </div>
  );
}
