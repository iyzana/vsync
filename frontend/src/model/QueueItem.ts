import { VideoSource } from '../component/Player';

export default interface QueueItem {
  source: VideoSource;
  originalQuery: string;
  title?: string;
  thumbnail?: string;
  id: string;
}
