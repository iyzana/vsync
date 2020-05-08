import React from "react";
import "./Queue.css";
import QueueItem from "./QueueItem";

interface QueueProps {
  videos: QueueItem[];
  removeVideo: (videoId: string) => void;
  numUsers: number;
}

function Queue({ videos, removeVideo, numUsers }: QueueProps) {
  return (
    <div className="queue-list">
      <div className="header">
        <h3>Queue</h3>
        <div>{numUsers + " connected"} </div>
      </div>
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
