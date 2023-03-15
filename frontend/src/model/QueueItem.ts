export default interface QueueItem {
  // todo: replace with only domain
  url: string;
  originalQuery: string;
  title?: string;
  thumbnail?: string;
  id: string;
}
