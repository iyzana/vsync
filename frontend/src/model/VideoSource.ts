export interface VideoSource {
	url: string;
	mimeType?: string;
}

export interface VideoCommand {
	source: VideoSource;
	originalQuery: string;
	metadata?: VideoMetadata;
	startTimeSeconds?: number;
	favicon?: string;
}

export interface VideoMetadata {
	title: string;
	series?: string;
	seasonNumber?: number;
	episodeNumber?: number;
	channel?: string;
}

