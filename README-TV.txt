milo. for smart TVs
===================

milo. has a TV mode: bigger layout, a clear focus ring, and full remote-control support
(arrows move, OK selects, Back goes back, play/pause/next/previous/rewind/fast-forward keys work).
It switches on by itself on Samsung (Tizen), LG (webOS), Android TV / Google TV, Fire TV, Hisense (VIDAA),
Philips and Sony browsers. Anywhere else: Profile > Insights & tools > TV mode, or add ?tv=1 to the address.

Easiest way on any TV with a browser (Android TV, Google TV, Fire TV, Xbox, Hisense, Philips, Sony)
  Host the milo-app folder on https (see milo-app/README.txt), open that address in the TV browser, add ?tv=1 once.

Samsung (Tizen) as an installed app
  1. Install Tizen Studio with the TV extensions, and create a Samsung certificate profile.
  2. File > Import > Tizen project > pick the "tizen" folder here.
  3. Right-click the project > Build Signed Package (gives milo.wgt), then run on your TV in Developer Mode
     or submit to the Samsung Seller Office.

LG (webOS) as an installed app
  1. Install the webOS TV CLI:  npm i -g @webos-tools/cli
  2. ares-package webos            (makes com.ubayanda.milotv_1.0.0_all.ipk)
  3. ares-setup-devicemanager, then ares-install --device tv com.ubayanda.milotv_1.0.0_all.ipk
     (turn on Developer Mode on the TV first) or submit through LG Seller Lounge.

Play songs from a USB stick or drive (TV mode)
  Library > "Play from USB / storage" (pick the folder) or "Choose songs". Plug the stick in first.
  Songs play from the stick, nothing is copied to the TV, and they are sorted A to Z.
  The TV browser only allows access for the current visit, so pick the folder again next time.
  "Remove USB songs" clears them. Unplugging the stick while a song plays will stop it.
  Not tested on real TVs: some TV browsers (older Tizen / webOS / VIDAA) may not show a folder picker.
  If "Play from USB / storage" opens nothing, try "Choose songs"; if that fails too, that TV's browser does not let web pages read USB.

Which TVs
  Installable app: Samsung (Tizen) and LG (webOS), packages in this folder.
  In the TV's browser (TV mode switches on by itself): Android TV, Google TV, Fire TV, Hisense (VIDAA), Philips, Sony.
  Not possible: Apple TV (use AirPlay). No Roku package yet.

Things to know
  - Service workers don't run inside Tizen/webOS packages, so the app does not work offline there (it works fully online).
  - YouTube needs a connection, and its player must stay on screen while it plays (your TV remote controls it through milo.).
  - Songs you uploaded on another device are not on the TV. Use Cloud sync (Profile) to share playlists and likes.
  - Apple TV cannot run web apps like this one. Use AirPlay from the iPhone version instead.
  - Remote keys covered: arrows, OK, Back (Tizen 10009 / webOS 461 / Backspace), play, pause, stop, rewind, fast-forward, next, previous.
