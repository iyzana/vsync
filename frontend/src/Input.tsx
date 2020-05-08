import React, { useEffect, useState } from "react";
import "./Input.css";
import Error from "./Error";

interface InputProps {
  ws: WebSocket;
  errors: Error[];
  setErrors: (map: (errors: Error[]) => Error[]) => void;
}

function Input({ ws, errors, setErrors }: InputProps) {
  const [message, setMessage] = useState("");

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && message !== "") {
      ws.send(`queue add ${message}`);
      setMessage("");
    }
  };

  useEffect(() => {
    if (errors.length === 0) {
      return;
    }
    const timeout = setTimeout(() => {
      setErrors((errors) => errors.filter((error) => error.permanent));
    }, 3000);
    return () => clearTimeout(timeout);
  }, [errors, setErrors]);
  return (
    <div>
      {errors.map((error, index) => (
        <div key={index} className="error">
          {error.message}
        </div>
      ))}
      <input
        className="url"
        type="text"
        placeholder="Enter YouTube URL here"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyUp={onKey}
      />
    </div>
  );
}

export default Input;
