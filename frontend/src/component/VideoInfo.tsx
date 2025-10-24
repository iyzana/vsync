import './VideoInfo.css';
import { useState } from "react";
import { useWebsocketMessages } from "../hook/websocket-messages";
import { VideoCommand } from "../model/VideoSource";
import Host from './Host';
import VideoMetadataLine from './VideoMetadataLine';

function VideoInfo() {
  const [video, setVideo] = useState<VideoCommand | null>(null);

  useWebsocketMessages(
    (msg: string) => {
      if (msg === 'video') {
        setVideo(null);
      } else if (msg.startsWith('video')) {
        const video = JSON.parse(msg.split(' ').slice(1).join(' '));
        setVideo(video);
      }
    },
    [],
  );

  return (
    video?.metadata?.title ? <div className="card video-info">
      <h3 className="card-title">Currently Playing</h3>
      <div className="video-info-data">
        <div className="video-info-title">{video.metadata?.title}</div>
        <VideoMetadataLine metadata={video.metadata} />
        <Host element={video} />
      </div>
    </div > : null
  );
}

export default VideoInfo;
