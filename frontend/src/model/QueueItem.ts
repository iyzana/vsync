import { VideoMetadata, VideoSource } from "./VideoSource";

export default interface QueueItem {
	source?: VideoSource;
	originalQuery: string;
	metadata?: VideoMetadata;
	startTimeSeconds?: number;
	thumbnail?: string;
	favicon?: string;
	loading: boolean;
	id: string;
}
