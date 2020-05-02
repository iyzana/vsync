import React from "react";
import "./YtEmbed.css";

interface YtEmbedProps {
  videoId: string;
}

function YtEmbed({ videoId }: YtEmbedProps) {
  return (
    <div className="aspect-ratio">
      <div className="aspect-ratio-inner">
        <iframe
          title="YouTube Embed"
          width="100%"
          height="100%"
          src={`https://www.youtube.com/embed/${videoId}`}
          frameBorder="0"
          allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        ></iframe>
      </div>
    </div>
  );
}

export default YtEmbed;
