import React from "react";
import "./Queue.css";
import QueueItem from "./QueueItem";

interface QueueProps {
  videos: QueueItem[];
  removeVideo: (videoId: string) => void;
}

function Queue({ videos, removeVideo }: QueueProps) {
  return (
    <div className="queue-list">
      <h3>Queue</h3>
      {videos.map(({ videoId, title, thumbnail }) => (
        <div key={videoId} className="queue-item">
          <div className="video-info">
            <img className="thumbnail" src={thumbnail} alt="" />
            <div>{title}</div>
          </div>
          <div className="remove" onClick={() => removeVideo(videoId)}>
            ✕
          </div>
        </div>
      ))}
    </div>
  );
}

export default Queue;
