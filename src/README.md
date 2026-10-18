# milo. source

`index.html` is **built** from the pieces in this folder. The look and behaviour are unchanged; the code is just in
small, named files instead of one 650 KB block.

- `src/head.html`, `src/body.html`, `src/foot.html` – page shell
- `src/css/NN-*.css` – styles, in order
- `src/js/NN-*.js` – scripts, in order (one section each: playback engine, lyrics, social, v6 push, v7 device music, v8 playback guard, v9 native audio…)
- `src/parts.json` – the order the pieces are joined in

## Workflow
1. Edit the file you want in `src/`.
2. Run `node build.mjs` – it rewrites `index.html` and the copies in `../milo-tv/tizen` and `../milo-tv/webos`.
3. Upload the whole folder to GitHub as before (GitHub Pages / Vercel / PWABuilder keep working, because `index.html` is still a normal file).

`node build.mjs --check` tells you if `index.html` is out of date with `src/`.
To add a new section: create `src/js/NN-name.js` and add it to `src/parts.json` before `foot.html`.
