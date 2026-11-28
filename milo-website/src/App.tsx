import {useEffect, useState} from 'react';
import {Button} from '@astryxdesign/core/Button';
import {Heading} from '@astryxdesign/core/Heading';
import {Text} from '@astryxdesign/core/Text';
import {SegmentedControl, SegmentedControlItem} from '@astryxdesign/core/SegmentedControl';
import {Collapsible} from '@astryxdesign/core/Collapsible';
import {
  MusicalNoteIcon, SparklesIcon, ArrowsRightLeftIcon, ChatBubbleBottomCenterTextIcon, ArrowDownTrayIcon,
  QueueListIcon, UsersIcon, AdjustmentsHorizontalIcon, MoonIcon, MicrophoneIcon, TruckIcon, PaintBrushIcon,
  DevicePhoneMobileIcon, LockOpenIcon, ClockIcon, ArrowUpTrayIcon,
} from '@heroicons/react/24/outline';
import {useAccent, swatches} from './accent';
import {site, links, detectOs} from './site';
import {HomeScreen, ProfileScreen, RealHome, RealPlayer, RealLyrics, RealLibrary} from './phones/phones';

const shotFiles = import.meta.glob('./assets/shots/*.{png,jpg,jpeg,webp}', {eager: true, query: '?url', import: 'default'}) as Record<string, string>;

function useReveal() {
  useEffect(() => {
    const els = document.querySelectorAll('.reveal');
    const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } }), {threshold: 0.12});
    els.forEach(e => io.observe(e));
    return () => io.disconnect();
  }, []);
}

/** The main download button. Android visitors get the APK; on a computer or iPhone it says what is (not) available instead of a dead link. */
function Cta({variant = 'primary' as 'primary' | 'secondary'}) {
  const os = detectOs();
  const apk = links.apk();
  if (os === 'windows' || os === 'other') {
    return (
      <div className="cta-stack">
        {apk ? <Button label="Download the Android APK" variant={variant} size="lg" href={apk} /> : <Button label="Android download · coming soon" variant={variant} size="lg" isDisabled />}
        <span className="cta-note">{os === 'windows' ? 'Open this page on your Android phone to install, or send the link to it. The Windows version is under production.' : 'Open this page on your Android phone to install.'}</span>
      </div>
    );
  }
  if (os === 'ios') {
    return <div className="cta-stack"><Button label="Android only for now" variant={variant} size="lg" isDisabled /><span className="cta-note">There is no iPhone version.</span></div>;
  }
  if (apk) return <Button label="Download for Android" variant={variant} size="lg" href={apk} />;
  return <Button label="Download · coming soon" variant={variant} size="lg" isDisabled />;
}

function Dock() {
  const {accent, auto, pick, setAuto} = useAccent();
  return (
    <div className="dock" role="group" aria-label="Change the accent colour">
      <span className="lbl">Accent</span>
      <div className="sws">
        {swatches.map(s => (
          <button key={s.hex} className={'sw-b' + (!auto && accent.toLowerCase() === s.hex.toLowerCase() ? ' on' : '')}
            style={{background: s.hex}} aria-label={s.name} title={s.name} onClick={() => pick(s.hex)} />
        ))}
      </div>
      <label className="sw-custom" title="Pick any colour"><input type="color" value={accent} onChange={e => pick(e.target.value)} aria-label="Custom colour" /></label>
      <button className={'sw-auto' + (auto ? ' on' : '')} onClick={() => setAuto(!auto)}>Auto</button>
    </div>
  );
}

function Top() {
  return (
    <header className="top"><div className="wrap in">
      <a className="logo" href="#top" style={{color: 'inherit', textDecoration: 'none'}}><img src="./icons/icon-192.png" alt="" />milo<i>.</i></a>
      <nav className="nav" aria-label="Sections">
        <a href="#features">Features</a><a href="#look">Look</a><a href="#crossmix">Crossmix</a><a href="#download">Download</a><a href="#faq">FAQ</a>
      </nav>
    </div></header>
  );
}

function Hero() {
  const {song, songs} = useAccent();
  return (
    <section className="sec hero" id="top">
      <div className="glow" style={{left: '-10%', top: '-10%'}} />
      <div className="wrap hero-grid">
        <div>
          <Text type="label" color="secondary">Music player for Android</Text>
          <div style={{margin: '14px 0 20px'}}>
            <Heading level={1} type="display">Music that wears <em>your colour.</em></Heading>
          </div>
          <div style={{maxWidth: 520, marginBottom: 28}}>
            <Text type="body" color="secondary">milo. plays your own library and milo. Stream with glass-smooth controls, synced lyrics, crossmix transitions and a look that follows the song, or the colour you pick. Try the swatches below: everything on this page changes with them.</Text>
          </div>
          <div style={{display: 'flex', gap: 12, flexWrap: 'wrap'}}>
            <Cta />
            <Button label="See features" variant="secondary" size="lg" href="#features" />
          </div>
          <p className="dlmeta">Android 8.0 and up · Free · No account needed{links.source() ? <> · <a href={links.source()} style={{color: 'inherit'}}>Source</a></> : null}</p>
          <p style={{marginTop: 14, color: '#a29a8b', fontSize: 14}}>Now showing: <b style={{color: 'var(--m)'}}>{songs[song].title}</b> · {songs[song].artist} (demo track)</p>
        </div>
        <div className="hero-phones">
          <div className="p1"><RealHome /></div>
          <div className="p2"><RealPlayer /></div>
          <div className="floaty" style={{left: '0%', bottom: '12%'}}><i />Synced lyrics</div>
          <div className="floaty" style={{right: '0%', top: '4%', animationDelay: '-2s'}}><i />Crossmix</div>
        </div>
      </div>
    </section>
  );
}

const marquee = ['Synced lyrics', 'Crossmix', 'Offline saves', 'Listen Together', 'Sleep timer', 'Car mode', 'Bedside clock', 'Identify song', 'Playlist import', 'Recap story', 'Equaliser', 'Spatial sound', 'Custom accents', 'Lite flavour'];
function Marquee() {
  const row = [...marquee, ...marquee];
  return <div className="marq" aria-hidden="true"><div className="track">{row.map((m, i) => <span key={i}>{m}</span>)}</div></div>;
}

const features = [
  [MusicalNoteIcon, 'Your library, plus milo. Stream', 'Local files and milo. Stream side by side, with searchable songs, albums, artists and playlists.'],
  [ArrowsRightLeftIcon, 'Crossmix', 'Harmonic glide, filter sweeps and continuous albums, so songs flow instead of stopping.'],
  [ChatBubbleBottomCenterTextIcon, 'Synced lyrics', 'Live highlighted lines, a per-song timing fix, and your own lyrics files when you want them.'],
  [ArrowDownTrayIcon, 'Offline saves', 'Save songs to listen without a connection, arranged by playlist, album, artist or single.'],
  [ArrowUpTrayIcon, 'Playlist import', 'Bring playlists in from .m3u, .csv or a link.'],
  [SparklesIcon, 'Recap story', 'A swipeable story of what you listened to, ready to share.'],
  [UsersIcon, 'Listen Together', 'Friends, chat and shared EQ settings, so listening is a bit less lonely.'],
  [AdjustmentsHorizontalIcon, 'Sound controls', 'Equaliser, bass, reverb and spatial sound.'],
  [MoonIcon, 'Sleep timer', 'Fade out gently and stop on your terms.'],
  [MicrophoneIcon, 'Identify song', 'Hold your phone up and find out what is playing.'],
  [TruckIcon, 'Car mode and Bedside', 'Big, calm screens for the dashboard and the nightstand.'],
  [PaintBrushIcon, 'Looks that follow you', 'Backgrounds from song art or colour, your own picture, themes and accents.'],
  [DevicePhoneMobileIcon, 'Android 8 and up', 'Runs on Android 8 or newer, with an even lighter Lite build for older or smaller phones.'],
  [LockOpenIcon, 'No account needed', 'Open it and play. Sign-ins are optional extras.'],
] as const;
function Features() {
  return (
    <section className="sec" id="features">
      <div className="wrap">
        <div className="reveal" style={{maxWidth: 640, marginBottom: 44}}>
          <Text type="label" color="secondary">Features</Text>
          <Heading level={2} type="display">Everything you reach for, nothing in the way.</Heading>
        </div>
        <div className="fgrid">
          {features.map(([Icon, t, d], i) => (
            <div className="fcard reveal" style={{transitionDelay: (i % 4) * 70 + 'ms'}} key={t}>
              <div className="fic"><Icon /></div>
              <Heading level={4}>{t}</Heading>
              <div style={{marginTop: 8}}><Text type="body" color="secondary">{d}</Text></div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Look() {
  const [mode, setMode] = useState('art');
  const copy: Record<string, string> = {
    black: 'Pure black, easy on the eyes and the battery.',
    art: 'A soft blur of the current cover wraps the whole app.',
    colour: 'The song’s own colour washes through every screen.',
    picture: 'Use any picture from your phone as the backdrop.',
  };
  return (
    <section className="sec" id="look">
      <div className="glow" style={{right: '-15%', top: '10%'}} />
      <div className="wrap split">
        <div className="reveal">
          <Text type="label" color="secondary">Backgrounds and accents</Text>
          <Heading level={2} type="display">Make it yours in one tap.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">Pick an accent from the dock at the bottom of this page, then choose a background below. The left phone is a real screenshot; the right one updates live.</Text></div>
          <SegmentedControl value={mode} onChange={setMode} label="Background">
            <SegmentedControlItem value="black" label="Black" />
            <SegmentedControlItem value="art" label="Song art" />
            <SegmentedControlItem value="colour" label="Song colour" />
            <SegmentedControlItem value="picture" label="My picture" />
          </SegmentedControl>
          <p style={{color: '#a29a8b', margin: '18px 0 26px'}}>{copy[mode]}</p>
          <ul className="bul">
            <li><span><b>Accent</b> colours the buttons, sliders and highlights.</span></li>
            <li><span><b>Auto</b> follows the colours of whatever is playing.</span></li>
            <li><span><b>Glass</b> controls stay readable on any background.</span></li>
          </ul>
        </div>
        <div className="phones-col pair reveal">
          <figure className="fig"><RealHome /><figcaption>The app, as it looks today</figcaption></figure>
          <figure className="fig"><HomeScreen bg={mode} scroll={false} /><figcaption>Try it live</figcaption></figure>
        </div>
      </div>
    </section>
  );
}

function Player() {
  return (
    <section className="sec">
      <div className="wrap split flip">
        <div className="reveal">
          <Text type="label" color="secondary">Now playing</Text>
          <Heading level={2} type="display">Edge-to-edge art, glass controls, lyrics below.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">A full-bleed cover, a strip of round glass buttons, a controls card and a black lyrics area that lights up line by line.</Text></div>
          <ul className="bul">
            <li><span><b>Output</b> switcher sits on the art, out of the way.</span></li>
            <li><span><b>Per-song timing fix</b> when lyrics run a little early or late.</span></li>
            <li><span><b>Like, download, queue</b> and more from one strip.</span></li>
          </ul>
        </div>
        <div className="phones-col pair reveal"><RealPlayer /><RealLyrics /></div>
      </div>
    </section>
  );
}

function Crossmix() {
  const bars = Array.from({length: 64}, (_, i) => i);
  const {accent} = useAccent();
  return (
    <section className="sec" id="crossmix">
      <div className="wrap split">
        <div className="reveal">
          <Text type="label" color="secondary">Crossmix</Text>
          <Heading level={2} type="display">Songs that flow into each other.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">Harmonic glide blends the end of one track into the start of the next. Filter sweeps and continuous albums keep the mood going.</Text></div>
          <ul className="bul">
            <li><span><b>Harmonic glide</b> for keys that fit together.</span></li>
            <li><span><b>Filter sweep</b> for a DJ-style handover.</span></li>
            <li><span><b>Continuous albums</b> play without gaps.</span></li>
          </ul>
        </div>
        <div className="xm reveal">
          <div className="xm-leg"><span><b>Low Tide</b> · fading out</span><span>fading in · <b>Amber Rooms</b></span></div>
          <div className="xm-stage">
            <div className="xm-bars">
              {bars.map(i => {
                const h = 20 + Math.abs(Math.sin(i * 0.7) * 55) + (i * 7 % 13);
                const t = i / 63;
                return <i key={i} style={{height: h + '%', background: `linear-gradient(${accent}, color-mix(in srgb, ${accent} 30%, #fff))`, opacity: 0.25 + 0.75 * (1 - Math.abs(t - 0.5) * 1.4)}} />;
              })}
            </div>
            <div className="xm-play" />
          </div>
          <div className="xm-leg"><span>Illustration, not a recording</span><span>Overlap 6s</span></div>
        </div>
      </div>
    </section>
  );
}

function Library() {
  return (
    <section className="sec">
      <div className="wrap split flip">
        <div className="reveal">
          <Text type="label" color="secondary">Library and offline</Text>
          <Heading level={2} type="display">Your music, tidied and saved.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">Liked and saved songs get their own tiles. Offline saves are grouped the way you think about music, and you can open any group to see what is inside.</Text></div>
          <ul className="bul">
            <li><span><b>Playlist, album, artist or single</b> arrangements for downloads.</span></li>
            <li><span><b>Pull down</b> to rescan your library.</span></li>
            <li><span><b>Import</b> playlists from files or links.</span></li>
          </ul>
        </div>
        <div className="phones-col reveal" style={{maxWidth: 320, margin: '0 auto', width: '100%'}}><RealLibrary /></div>
      </div>
    </section>
  );
}

const slides = [
  {k: 'YOUR YEAR IN SOUND', n: '4,218', p: 'minutes listened (example)'},
  {k: 'TOP SONG', n: 'Low Tide', p: 'on repeat all spring (example)'},
  {k: 'STREAK', n: '23 days', p: 'listened every single day (example)'},
];
function Recap() {
  const [i, setI] = useState(0);
  useEffect(() => { const t = setInterval(() => setI(x => (x + 1) % slides.length), 4000); return () => clearInterval(t); }, []);
  const s = slides[i];
  return (
    <section className="sec">
      <div className="glow" style={{left: '-10%', top: '20%'}} />
      <div className="wrap split">
        <div className="phones-col reveal">
          <div className="story">
            <div className="segs">{slides.map((_, j) => <i key={j} className={j < i ? 'done' : j === i ? 'cur' : ''}><b /></i>)}</div>
            <div className="slide" key={i}><span className="tag">Example</span><small>{s.k}</small><div className="num">{s.n}</div><p>{s.p}</p></div>
          </div>
        </div>
        <div className="reveal">
          <Text type="label" color="secondary">Recap story</Text>
          <Heading level={2} type="display">Your listening, as a story.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">Minutes, top songs and streaks, wrapped in your accent colour. Numbers here are made-up examples.</Text></div>
          <ul className="bul">
            <li><span><b>Listening stats</b> live on your Home screen.</span></li>
            <li><span><b>Stays on your device</b> unless you share it.</span></li>
          </ul>
        </div>
      </div>
    </section>
  );
}

function Download() {
  const apk = links.apk();
  const win = links.windows();
  const steps = [
    ['Download', 'Get the APK on your phone with the button below.'],
    ['Allow the install', 'Android asks once to let your browser install apps. Say yes, then open the file.'],
    ['Press play', 'Open milo., pick a colour and your music is there. No account.'],
  ];
  return (
    <section className="sec" id="download"><div className="wrap">
      <div className="reveal" style={{marginBottom: 28}}>
        <Text type="label" color="secondary">Download</Text>
        <Heading level={2} type="display">Get milo. for Android.</Heading>
      </div>
      <ol className="steps reveal">
        {steps.map(([t, d], i) => (
          <li key={t}><span className="n">{i + 1}</span><b>{t}</b><span>{d}</span></li>
        ))}
      </ol>
      <div className="reveal" style={{display: 'flex', gap: 12, flexWrap: 'wrap', margin: '26px 0 10px'}}>
        {apk ? <Button label="Download the Android APK" variant="primary" size="lg" href={apk} /> : <Button label="Download · coming soon" variant="primary" size="lg" isDisabled />}
        {links.releases() ? <Button label="All releases" variant="secondary" size="lg" href={links.releases()} /> : null}
      </div>
      <p className="dlmeta">{site.apkSha256 ? <>SHA-256 of milo.apk: <code>{site.apkSha256}</code></> : 'A SHA-256 checksum is published with every release so you can check the file.'}</p>
      <div className="plats reveal">
        <div className="plat on"><b>Android</b><span>Android 8 and up.</span></div>
        <div className={'plat' + (win ? ' on' : '')}><b>Windows</b><span>{win ? 'Installer available.' : 'Under production, no release date.'}</span>{win ? <Button label="Download for Windows" variant="secondary" size="md" href={win} /> : null}</div>
        <div className="plat"><b>iPhone</b><span>Not planned.</span></div>
      </div>
    </div></section>
  );
}

function Desktop() {
  return (
    <section className="sec" id="desktop">
      <div className="glow" style={{right: '-10%', top: '0%'}} />
      <div className="wrap split">
        <div className="reveal">
          <Text type="label" color="secondary">Desktop</Text>
          <Heading level={2} type="display">milo. for desktop is under production.</Heading>
          <div style={{margin: '16px 0 24px'}}><Text type="body" color="secondary">We are building a desktop version so your library, lyrics and look follow you to the big screen. It is not available yet and there is no release date.</Text></div>
          <div className="road">
            <div className="rrow"><span className="rdot on" /><b>In production</b><span style={{color: '#a29a8b'}}>Desktop player</span></div>
            <div className="rrow"><span className="rdot" /><b>Planned</b><span style={{color: '#a29a8b'}}>Synced lyrics and crossmix</span></div>
            <div className="rrow"><span className="rdot" /><b>Planned</b><span style={{color: '#a29a8b'}}>Same accents and backgrounds</span></div>
          </div>
        </div>
        <div className="laptop reveal">
          <div className="lid">
            <div className="wip"><i />Under production</div>
            <div className="dk">
              <div className="side"><b>milo<i>.</i></b><span className="on">Home</span><span>Library</span><span>Playlists</span><span>Profile</span></div>
              <div className="main">
                <div className="cv" style={{['--c' as any]: '#F99C00'}} />
                <div className="lines"><h6>Low Tide</h6><p>Wren Sable</p><p>Tide pulls the lights away</p><p className="on">Hold on to the blue</p><p>Carry me home again</p></div>
              </div>
              <div className="pbar"><span>⏮</span><span>▶</span><span>⏭</span><div className="seek2"><i /></div></div>
            </div>
          </div>
          <div className="base" />
        </div>
      </div>
    </section>
  );
}

function Gallery() {
  const urls = Object.values(shotFiles);
  if (!urls.length) return null;
  return (
    <section className="sec"><div className="wrap">
      <Heading level={2} type="display">In the app</Heading>
      <div className="gal" style={{marginTop: 28}}>{urls.map(u => <img key={u} src={u} alt="milo. app screenshot" loading="lazy" />)}</div>
    </div></section>
  );
}

const faqs = [
  ['Do I need an account?', 'No. Open milo. and play. Sign-ins are optional.'],
  ['Where does the music come from?', 'Your local files and milo. Stream.'],
  ['Which Android versions work?', 'Android 8 and up. The Lite build is lighter still, for older or smaller phones.'],
  ['How do I install it?', 'Download the APK on your phone, allow installs from your browser when Android asks, open the file and press play. It is not on the Play Store.'],
  ['Is it free?', 'Yes. No account, no subscription, no ads.'],
  ['Why does Android warn me?', 'Apps installed from outside the Play Store always get an "unknown source" prompt. Check the SHA-256 on this page against the file if you want to be sure it is untouched.'],
  ['Is there an iPhone or desktop version?', 'milo. is Android first. A desktop version is under production; there is no release date yet.'],
  ['What happens to error reports?', 'They stay on your device until you choose to share them.'],
];
function Faq() {
  return (
    <section className="sec" id="faq"><div className="wrap" style={{maxWidth: 760}}>
      <div className="reveal" style={{marginBottom: 28}}><Heading level={2} type="display">Questions</Heading></div>
      <div className="reveal">
        {faqs.map(([q, a]) => (
          <Collapsible key={q} trigger={q}><div style={{padding: '4px 0 16px'}}><Text type="body" color="secondary">{a}</Text></div></Collapsible>
        ))}
      </div>
    </div></section>
  );
}

function Final() {
  return (
    <section className="sec" style={{textAlign: 'center'}}>
      <div className="glow" style={{left: '15%', top: '0'}} />
      <div className="wrap reveal">
        <Heading level={2} type="display">Pick a colour. Press play.</Heading>
        <div style={{display: 'flex', justifyContent: 'center', gap: 12, marginTop: 26, flexWrap: 'wrap'}}><Cta /></div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="ft"><div className="wrap cols">
      <div><div className="logo">milo<i>.</i></div><p>Music that wears your colour.</p></div>
      <p style={{maxWidth: 520}}>milo. is a personal, non-commercial project made for me, my family and friends, and is not affiliated with any music service. Built with thanks to the open-source projects that inspired it: Mochy Music, Limusic and Musike. milo. is free software under the GPL. Demo songs and artists on this page are fictional.</p>
    </div></footer>
  );
}

export function App() {
  useReveal();
  return (
    <div className="site">
      <Top /><Hero /><Marquee /><Features /><Look /><Player /><Crossmix /><Library /><Recap /><Download /><Desktop /><Gallery /><Faq /><Final /><Footer /><Dock />
    </div>
  );
}
