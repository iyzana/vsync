import React, { useState, useEffect } from "react";
import "./Queue.css";
import QueueItem from "./QueueItem";

interface QueueProps {
  ws: WebSocket;
  videos: QueueItem[];
  errors: string[];
  setErrors: (map: (errors: string[]) => string[]) => void;
}

function Queue({ ws, videos, errors, setErrors }: QueueProps) {
  const [message, setMessage] = useState("");

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && message !== "") {
      ws.send(`queue ${message}`);
      setMessage("");
    }
  };

  useEffect(() => {
    if (errors.length === 0) {
      return;
    }
    const timeout = setTimeout(() => {
      setErrors((errors) => errors.slice(1));
    }, 3000);
    return () => clearTimeout(timeout);
  }, [errors, setErrors]);

  return (
    <div className="queue">
      <div className="queue-list">
        <h3>Queue</h3>
        {videos.map(({ videoId, title }) => (
          <div key={videoId}>{title}</div>
        ))}
      </div>
      <div>
        {errors.map((error, index) => (
          <div key={index} className="error">{error}</div>
        ))}
        <div className="search">
          <input
            type="text"
            placeholder="Enter YouTube URL here"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyUp={onKey}
          />
        </div>
      </div>
    </div>
  );
}

export default Queue;
