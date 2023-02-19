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
      Queue videos by inputting a video URL from any publicly accessible site.
      It may not be possible to watch videos from everywhere but a wide range of
      websites are supported.
      <p>
        It is also possible to queue videos by typing search terms. In this case
        the first search result from YouTube will be added.
      </p>
      <h2>Supported sites</h2>
      <p>A list of sites that are known to work</p>
      <ul>
        <li>youtube.com</li>
        <li>vimeo.com</li>
        <li>ardmediathek.de</li>
        <li>zdf.de</li>
      </ul>
      <h2>Bookmarklet</h2>
      <p>
        Drag this button into your bookmark bar to open a vsync room for videos
        in any tab:
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
          <span className="shortcut">Space</span>{' '}
          <span>Click focussed element or toggle play/pause</span>
        </li>
        <li>
          <span className="shortcut">K</span> <span>Toggle play/pause</span>
        </li>
        <li>
          <span className="shortcut">🠔</span>{' '}
          <span>Seek 5 seconds backwards</span>
        </li>
        <li>
          <span className="shortcut">🠖</span>{' '}
          <span>Seek 5 seconds forwards</span>
        </li>
        <li>
          <span className="shortcut">J</span>{' '}
          <span>Seek 10 seconds backwards</span>
        </li>
        <li>
          <span className="shortcut">L</span>{' '}
          <span>Seek 10 seconds forwards</span>
        </li>
        <li>
          <span className="shortcut">M</span> <span>Toggle mute</span>
        </li>
        <li>
          <span className="shortcut">C</span> <span>Toggle subtitles</span>
        </li>
        <li>
          <span className="shortcut">🠗</span> <span>Decrease volume by 5%</span>
        </li>
        <li>
          <span className="shortcut">🠕</span> <span>Increase volume by 5%</span>
        </li>
        <li>
          <span className="shortcut">F</span> <span>Toggle fullscreen</span>
        </li>
        <li>
          <span className="shortcut">Shift</span> +{' '}
          <span className="shortcut">N</span>{' '}
          <span>Skip to next video in queue</span>
        </li>
      </ul>
    </>
  );
}

export default About;
