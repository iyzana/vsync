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
  msg,
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
    // make sure video.js is only initialized once
    if (!playerRef.current) {
      const placeholderElement = placeholderRef.current;
      if (!placeholderElement) return;
      const videoElement = placeholderElement.appendChild(
        document.createElement('video-js'),
      );

      if (!videoElement) return;

      playerRef.current = videojs(videoElement, opts, () => {
        videojs.log('player is ready');
        // onReady
      });
    } else {
      // You could update an existing player in the `else` block here
      // on prop change, for example:
      // const player = playerRef.current;
    }
  }, [placeholderRef]);

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
      if (volume === 0) {
        player.volume(0);
        player.muted(true);
      } else if (volume) {
        player.muted(false);
        player.volume(volume);
      }
    });
  }, [playerRef, videoUrl, volume]);

  // dispose video.js when the component unmounts
  useEffect(() => {
    const player = playerRef.current;

    return () => {
      if (player) {
        if (player.muted()) {
          setVolume(0);
        } else {
          setVolume(player.volume());
        }
        player.dispose();
        playerRef.current = null;
        waitReadyRef.current = false;
      }
    };
  }, [playerRef, setVolume]);

  useEffect(() => {
    const player = playerRef.current;
    if (!msg || !player) {
      return;
    }
    console.log({ msg });
    player.ready(function () {
      if (msg === 'play') {
        player.play();
      } else if (msg.startsWith('pause')) {
        player.pause();
        const timestamp = parseFloat(msg.split(' ')[1]);
        const shouldSeek = Math.abs(player.currentTime() - timestamp) > 1;
        if (shouldSeek) {
          player.currentTime(timestamp);
        }
      } else if (msg.startsWith('ready?')) {
        const timestamp = parseFloat(msg.split(' ')[1]);
        player.pause();
        player.currentTime(timestamp);
        waitReadyRef.current = true;
        console.log({ readyState: player.readyState() });
        if (player.readyState() >= 3) {
          sendMessage(`ready ${player.currentTime()}`);
        }
      }
    });
  }, [playerRef, msg, waitReadyRef, sendMessage]);

  useEffect(() => {
    const player = playerRef.current;
    if (!player) {
      return;
    }
    player.ready(function () {
      if (initializedRef.current) {
        // the user event listeners can't be removed without removing the internal videojs listeners as well
        // https://github.com/videojs/video.js/issues/5648
        console.warn('The player hooks should not be reregistered');
      }
      console.log('registering');
      player.on('play', function () {
        console.log('play');
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
        if (!waitReadyRef.current) {
          console.log('pause');
          sendMessage(`pause ${player.currentTime()}`);
          setOverlay('PAUSED');
        }
      });
      player.on('stalled', function () {
        console.log('stalled');
        sendMessage(`buffer ${player.currentTime()}`);
      });
      player.on('seeked', function () {
        console.log('seeked');
        if (player.paused() && !waitReadyRef.current) {
          sendMessage(`pause ${player.currentTime()}`);
        } else {
          sendMessage(`play ${player.currentTime()}`);
        }
      });
      player.on('ended', function () {
        console.log('ended');
        sendMessage(`end ${videoUrlRef.current}`);
      });
      player.on('ratechange', function () {
        console.log('ratechange');
        sendMessage(`speed ${player.playbackRate()}`);
      });
      player.on('canplay', function () {
        if (waitReadyRef.current) {
          console.log('canplay');
          sendMessage(`ready ${player.currentTime()}`);
        }
      });
    });
  }, [
    playerRef,
    sendMessage,
    waitReadyRef,
    videoUrlRef,
    setVolume,
    setOverlay,
    initializedRef,
    setInitialized,
  ]);

  // video.js </3 react
  return <div className="video-player" ref={placeholderRef}></div>;
};

export default VideoJsPlayer;
