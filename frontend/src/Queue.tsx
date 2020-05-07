import React, { useState } from "react";
import "./Queue.css";

interface QueueProps {
  ws: WebSocket;
  videos: string[];
}

function Queue({ ws, videos }: QueueProps) {
  const [message, setMessage] = useState("");

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      ws.send(`queue ${message}`);
      setMessage("");
    }
  };

  return (
    <div className="queue">
      <div className="queue-list">
        <h3>Queue</h3>
        {videos.map((video) => (
          <div>{video}</div>
        ))}
      </div>
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
  );
}

export default Queue;
