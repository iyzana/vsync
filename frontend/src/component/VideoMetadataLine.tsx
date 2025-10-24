import { VideoMetadata } from "../model/VideoSource";

interface VideoMetadataLineProps {
  metadata?: VideoMetadata
}

function formatVideoMetadata(metadata?: VideoMetadata) {
  const series = metadata?.series && !metadata?.title.includes(metadata?.series) ? metadata?.series : null;
  const season = metadata?.seasonNumber ? `Season ${metadata?.seasonNumber}` : null;
  const episode = metadata?.seasonNumber ? `Episode ${metadata?.episodeNumber}` : null;
  const elements = [series, season, episode, metadata?.channel].filter(e => e != null);
  return elements.length === 0 ? null : elements.join(" · ");
}

function VideoMetadataLine({ metadata }: VideoMetadataLineProps) {
  if (!metadata) {
    return null;
  }
  const videoInfo = metadata ? formatVideoMetadata(metadata) : null;
  return videoInfo ? <div>{videoInfo}</div> : null;
}

export default VideoMetadataLine;
