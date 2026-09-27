export function normalizeText(value = "") {
  return String(value)
    .toLowerCase()
    .normalize("NFKC")
    .replace(/\s+/g, " ")
    .trim();
}

export function selectKeywordMatch(message, groups) {
  const text = normalizeText(message);
  if (!text) return null;

  const ranked = [...groups]
    .filter(group => group.enabled)
    .sort((a, b) => {
      if (a.priority !== b.priority) return a.priority - b.priority;
      const aLongest = Math.max(0, ...a.keywords.map(k => normalizeText(k).length));
      const bLongest = Math.max(0, ...b.keywords.map(k => normalizeText(k).length));
      return bLongest - aLongest;
    });

  for (const group of ranked) {
    const keywords = [...group.keywords]
      .map(normalizeText)
      .filter(Boolean)
      .sort((a, b) => b.length - a.length);

    for (const keyword of keywords) {
      const matched = group.matchMode === "exact"
        ? text === keyword
        : group.matchMode === "starts_with"
          ? text.startsWith(keyword)
          : text.includes(keyword);

      if (matched) {
        return {
          groupId: group.id,
          groupName: group.name,
          keyword,
          responseText: group.responseText,
          matchMode: group.matchMode,
        };
      }
    }
  }

  return null;
}
