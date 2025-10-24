import './About.css';

function About() {
  const origin = window.location.origin;
  return (
    <>
      <h1>vsync</h1>
      <p>Synchronous watching of online-videos with other humans</p>
      <h2>Creating rooms</h2>
      <p>
        When navigating to the site a new room URL will be automatically
        generated, you can copy it from the browser or using the{' '}
        <em>Copy room link</em> Button.
      </p>
      <p>
        You can also create a custom room name by typing it after the slash in
        the URL-bar. Beware that other people may try to use the same room name.
      </p>
      <p>
        When a full URL is found behind the slash a new room with a random name
        will be created with that URL already queued.
      </p>
      <h2>Queuing videos</h2>
      <p>
        Queue videos by entering a video URL from any publicly accessible
        website where you can to watch without signing in. You may not be able
        to watch videos from all of these sites, but a wide range of sites are
        supported.
      </p>
      <p>
        It is also possible to queue videos by typing search terms. In this case
        the first search result from YouTube will be added.
      </p>
      <h2>Supported sites</h2>
      <p>A list of sites that are known to work</p>
      <ul>
        <li>youtube.com</li>
        <li>ardmediathek.de</li>
        <li>zdf.de</li>
        <li>arte.tv</li>
        <li>PeerTube instances</li>
        <li>reddit.com</li>
        <li>bsky.app</li>
        <li>soundcloud.com</li>
        <li>twitch.tv (clips)</li>
      </ul>
      <h2>Unsupported sites</h2>
      <p>A list of sites that are known not to work</p>
      <ul>
        <li>netflix.com</li>
        <li>tiktok.com</li>
        <li>twitch.tv (Livestream, VODs)</li>
        <li>instagram.com</li>
        <li>tvnow.de</li>
        <li>plus.rtl.de</li>
        <li>vimeo.com</li>
        <li>Mastodon instances</li>
        <li>Invidious instances</li>
      </ul>
      <h2>Bookmarklet</h2>
      <p>
        Drag this button into your bookmark bar to open a vsync room for videos
        in any tab
      </p>
      <p
        dangerouslySetInnerHTML={{
          __html: `
          <a
            href="javascript: location.href = '${origin}' + '/' + location.href"
            class="bookmarklet"
          >
            vsync
          </a>
        `,
        }}
      ></p>
      <h2>Player Shortcuts</h2>
      <ul className="shortcuts">
        <li>
          <kbd>Space</kbd>{' '}
          <span>Click focussed element or toggle play/pause</span>
        </li>
        <li>
          <kbd>K</kbd> <span>Toggle play/pause</span>
        </li>
        <li>
          <kbd>🠔</kbd> <span>Seek 5 seconds backwards</span>
        </li>
        <li>
          <kbd>🠖</kbd> <span>Seek 5 seconds forwards</span>
        </li>
        <li>
          <kbd>J</kbd> <span>Seek 10 seconds backwards</span>
        </li>
        <li>
          <kbd>L</kbd> <span>Seek 10 seconds forwards</span>
        </li>
        <li>
          <kbd>M</kbd> <span>Toggle mute</span>
        </li>
        <li>
          <kbd>C</kbd> <span>Toggle subtitles</span>
        </li>
        <li>
          <kbd>🠗</kbd> <span>Decrease volume by 5%</span>
        </li>
        <li>
          <kbd>🠕</kbd> <span>Increase volume by 5%</span>
        </li>
        <li>
          <kbd>F</kbd> <span>Toggle fullscreen</span>
        </li>
        <li>
          <kbd>Shift</kbd> + <kbd>N</kbd>{' '}
          <span>Skip to next video in queue</span>
        </li>
      </ul>
    </>
  );
}

export default About;
