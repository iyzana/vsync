export default interface QueueItem {
  url: string;
  originalQuery: string;
  title: string | null;
  thumbnail: string | null;
  id: string;
}
