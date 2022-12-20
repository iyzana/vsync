import './VideoJsPlayer.css';
import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import 'video.js/dist/video-js.css';
import { EmbeddedPlayerProps } from './Player';

const opts: VideoJsPlayerOptions = {
  autoplay: true,
  controls: true,
  responsive: true,
  fluid: false,
};

export const VideoJsPlayer = ({
  messages,
  clearMessages,
  videoUrl,
  sendMessage,
  setOverlay,
  volume,
  setVolume,
  initialized,
  setInitialized,
}: EmbeddedPlayerProps) => {
  const placeholderRef = useRef<HTMLDivElement | null>(null);
  const playerRef = useRef<videojs.Player | null>(null);
  const waitReadyRef = useRef(false);
  const initializedRef = useRef(initialized);
  const videoUrlRef = useRef(videoUrl);

  useEffect(() => {
    const player = playerRef.current;
    if (!player || messages.length === 0) {
      return;
    }
    player.ready(function () {
      for (const msg of messages) {
        if (msg === 'play') {
          console.log('processing server message play');
          player.play();
        } else if (msg.startsWith('pause')) {
          console.log('processing server message pause');
          player.pause();
          const timestamp = parseFloat(msg.split(' ')[1]);
          const shouldSeek = Math.abs(player.currentTime() - timestamp) > 0.5;
          if (shouldSeek) {
            console.log('seeking due to pause');
            player.currentTime(timestamp);
          }
        } else if (msg.startsWith('ready?')) {
          console.log('processing server message ready');
          player.pause();
          const timestamp = parseFloat(msg.split(' ')[1]);
          const shouldSeek = Math.abs(player.currentTime() - timestamp) > 0.5;
          if (shouldSeek) {
            console.log('seeking due to ready');
            player.currentTime(timestamp);
          }

          waitReadyRef.current = true;
          console.log({ readyState: player.readyState() });
          if (player.readyState() >= 3) {
            sendMessage(`ready ${player.currentTime()}`);
          }
        }
      }
      clearMessages(messages.length);
    });
  }, [playerRef, messages, clearMessages, waitReadyRef, sendMessage]);

  useEffect(() => {
    // make sure video.js is only initialized once
    if (!playerRef.current) {
      const placeholderElement = placeholderRef.current;
      if (!placeholderElement) return;
      const videoElement = placeholderElement.appendChild(
        document.createElement('video-js'),
      );

      if (!videoElement) return;

      playerRef.current = videojs(videoElement, opts);

      const player = playerRef.current;
      player.ready(() => {
        console.log('registering player hooks');
        player.on('play', function () {
          console.log('player hook play');
          if (initializedRef.current) {
            sendMessage(`play ${player.currentTime()}`);
            waitReadyRef.current = false;
            setOverlay(null);
          } else {
            setInitialized(true);
            sendMessage('sync');
          }
        });
        player.on('pause', function () {
          console.log('player hook pause');
          if (!waitReadyRef.current) {
            sendMessage(`pause ${player.currentTime()}`);
            setOverlay('PAUSED');
          }
        });
        player.on('stalled', function () {
          console.log('player hook stalled');
          sendMessage(`buffer ${player.currentTime()}`);
        });
        player.on('seeked', function () {
          console.log('player hook seeked');
          if (player.paused()) {
            if (waitReadyRef.current) {
              sendMessage(`play ${player.currentTime()}`);
            } else {
              sendMessage(`pause ${player.currentTime()}`);
            }
          }
        });
        player.on('ended', function () {
          console.log('player hook ended');
          sendMessage(`end ${videoUrlRef.current}`);
        });
        player.on('ratechange', function () {
          console.log('player hook ratechange');
          sendMessage(`speed ${player.playbackRate()}`);
        });
        player.on('canplay', function () {
          console.log('player hook canplay');
          if (waitReadyRef.current) {
            console.log('canplay');
            sendMessage(`ready ${player.currentTime()}`);
          }
        });
        player.on('volumechange', function () {
          console.log('player hook volumechange');
          console.log(
            'storing videojs player volume ' +
              player.muted() +
              ' ' +
              player.volume(),
          );
          if (player.muted()) {
            setVolume(0);
          } else {
            setVolume(player.volume());
          }
        });

        console.log('setting videojs player volume ' + volume);
        if (volume === 0) {
          player.volume(0);
          player.muted(true);
        } else if (volume) {
          player.muted(false);
          player.volume(volume);
        }
      });
    }
  });

  useEffect(() => {
    initializedRef.current = initialized;
  }, [initialized]);

  useEffect(() => {
    if (!playerRef.current) {
      return;
    }
    const player = playerRef.current;
    player.ready(() => {
      videoUrlRef.current = videoUrl;
      player.src(videoUrl);
    });
  }, [playerRef, videoUrl]);

  // dispose video.js when the component unmounts
  useEffect(() => {
    return () => {
      const player = playerRef.current;

      if (player) {
        player.dispose();
        playerRef.current = null;
        waitReadyRef.current = false;
      }
    };
  }, [playerRef, setVolume]);

  // video.js </3 react
  return <div className="video-player" ref={placeholderRef}></div>;
};

export default VideoJsPlayer;
