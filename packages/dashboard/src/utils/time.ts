export function getLastHoursRange(hours = 24) {
  const end = new Date();
  const start = new Date(end.getTime() - hours * 3600 * 1000);
  const toLocalInput = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, "0");
    const yyyy = d.getFullYear();
    const MM = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const mm = pad(d.getMinutes());
    return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
  };
  return { start: toLocalInput(start), end: toLocalInput(end) };
}
