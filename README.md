# CC: Tweaked - Russian Language (ccrulang)

NeoForge 1.21.1 addon for [CC: Tweaked](https://github.com/cc-tweaked/CC-Tweaked) that adds Cyrillic
support to the in-game computer/turtle terminal.

## What this actually fixes

CC: Tweaked's terminal is a single-byte (0-255) grid, rendered from a 256-glyph bitmap font
(`term_font.png`) and fed by Lua strings, which are byte strings (not UTF-8). Three separate things
had to be fixed for Russian text to work, and they're independent of each other:

1. **Rendering.** CC: Tweaked's own `term_font.png` is untouched by this mod - not one pixel of it
   changes. Cyrillic glyphs live in a completely separate texture, `assets/ccrulang/textures/gui/russian_font.png`,
   laid out as a flat А-to-я strip (same 8x11 cell pitch/6x9 glyph convention as term_font.png, just at
   native 1x resolution), drawn via its own `RenderType` - the same mechanism CC: Tweaked itself uses to
   draw printed-page borders from a texture separate from the glyph font.
   `FixedWidthFontRendererMixin` intercepts the ~66 byte values this addon treats as Cyrillic and, instead
   of letting them draw from term_font.png, records their position into `CyrillicRenderState`.
   `AbstractComputerScreenMixin` brackets each terminal screen's whole render with that state and, once
   everything on screen (including tooltips) has fully finished drawing, replays every recorded glyph as
   one quad batch from russian_font.png. (They can't be drawn inline as each byte is encountered:
   term_font.png and russian_font.png share a single "shared" GPU buffer slot, since `RenderType.text(...)`
   isn't a fixed buffer type - switching to it mid-draw would force-end term_font.png's still-in-progress
   buffer out from under CC: Tweaked's own code, and doing that any earlier than the very end of the
   screen's render - e.g. right as the terminal widget itself finishes - draws the terminal's background
   in its own out-of-order batch, which shows up as it compositing transparent against whatever's layered
   behind the GUI.) Everything else draws exactly as vanilla CC: Tweaked would.
2. **Input.** This was the actual blocker, independent of any font. CC: Tweaked's
   `StringUtil.unicodeToTerminal(int)` whitelists which typed/pasted Unicode codepoints are allowed
   into the terminal (ASCII, Latin-1 supplement, a few teletext symbols); Cyrillic isn't in that
   whitelist, so every keystroke was being silently dropped before this mod - `edit`, the shell,
   `paint`, etc. all had this problem no matter what font was loaded. `StringUtilMixin` injects a
   Cyrillic mapping in front of that whitelist, so typing or pasting (Ctrl+V) Russian text into an open
   terminal now produces a character instead of nothing.
3. **File loading.** CC: Tweaked reads `.lua` source (and any other text file, via `fs.open(path):readAll()`/
   `readLine()`) as raw bytes with no charset decoding anywhere - that's why a file has to actually contain
   CP1251 bytes for its Cyrillic string literals to show up correctly, which is not what any editor writes
   by default. `AbstractHandleMixin` rewrites the 2-byte UTF-8 sequence for each of the ~66 mapped Cyrillic
   letters into its single CP1251 byte as `AbstractHandle.readAll`/`readLine` return - covering both
   `.lua` program source (`loadfile`/`os.run`/`shell.run` all read the whole file this same way before
   compiling it) and ordinary text-file reads. So a `.lua` file just needs to be saved as plain UTF-8, the
   default nearly every editor uses, and its Cyrillic literals work with no manual re-encoding step.
   Binary-mode handles (`fs.open(path, "rb")`) are left alone.

All three agree on the same byte values for the Russian alphabet - the ones the Windows-1251 (CP1251)
codepage assigns to them (`0xC0-0xDF` = А-Я, `0xE0-0xFF` = а-я, `0xA8`/`0xB8` = Ё/ё) - so a typed letter,
a UTF-8 letter read from disk, and its on-screen glyph all refer to the same terminal byte. `CyrillicCodec`
(Unicode ↔ byte, plus the UTF-8 file transcoder) and `CyrillicFont` (byte → position in russian_font.png)
are the shared tables all three mixins draw from.

**Coverage:** interactive terminal screens (computer/pocket computer/turtle GUIs, via `TerminalWidget` /
`AbstractComputerScreen`) only. Printed pages (`PrintoutRenderer`) and monitors
(`DirectFixedWidthFontRenderer`) still draw those ~66 bytes as whatever term_font.png happens to have
there - not redirected. Extending to those would mean adding a parallel mixin per call site, following the
same pattern as `AbstractComputerScreenMixin`.

## What this does *not* do

- **Minecraft's own GUI** (item/block names, tooltips, JEI, key names) is **not** touched by this
  mod. CC: Tweaked already ships an official, fairly complete `ru_ru.json` translation upstream -
  just set Minecraft's language to Русский in Options → Language. Bundling a second copy here would
  only risk going stale against CC: Tweaked's own updates.
- Only the Russian alphabet (Ё, А-Я, а-я, 66 letters) is mapped - not the wider CP1251 punctuation/
  Cyrillic-adjacent-language set (№, Ukrainian/Serbian letters, smart quotes, etc.). A `.lua` file
  containing those, or any other non-Cyrillic non-ASCII text, round-trips untouched rather than being
  transcoded or corrupted - `AbstractHandleMixin` only ever rewrites the exact byte pairs that decode to
  one of the ~66 mapped letters.
- Files already saved as raw Windows-1251 (from before this mod could transcode UTF-8, or written that
  way on purpose) keep working exactly as before - the transcoder only touches byte pairs that are valid
  UTF-8 for a mapped Cyrillic codepoint, which plain CP1251 bytes essentially never coincidentally form.

## Building

Requires JDK 21.

```
./gradlew build
```

The output jar is at `build/libs/ccrulang-1.0.0.jar`. Drop it in `mods/` alongside CC: Tweaked
(1.120.2 or newer, for Minecraft 1.21.1, NeoForge) and NeoForge 21.1.x.

## Running in dev

```
./gradlew runClient
```

`build.gradle` pulls CC: Tweaked from `https://maven.squiddev.cc` automatically for the dev/test
classpath (see `cc_tweaked_version` in `gradle.properties` if a newer CC: Tweaked release needs to
be targeted - check that `StringUtil.unicodeToTerminal`, `FixedWidthFontRenderer.drawChar`, and
`AbstractComputerScreen.render` still have the same signatures after bumping it, since the mixins target
those internal, non-API methods directly). `AbstractHandleMixin` targets `AbstractHandle.readAll`/
`readLine` from CC: Tweaked's core module, which changes less often but is worth the same check.

## Project layout

- `src/main/java/ru/ziftech/ccrulang/mixin/StringUtilMixin.java` - keyboard/paste input fix.
- `src/main/java/ru/ziftech/ccrulang/mixin/AbstractHandleMixin.java` - transcodes UTF-8 Cyrillic byte
  pairs to CP1251 as text files (including `.lua` program source) are read.
- `src/main/java/ru/ziftech/ccrulang/CyrillicCodec.java` - Unicode codepoint ↔ terminal byte table,
  plus the UTF-8-to-terminal-bytes file transcoder `AbstractHandleMixin` uses.
- `src/main/java/ru/ziftech/ccrulang/mixin/client/AbstractComputerScreenMixin.java` - brackets each
  terminal screen's render with `CyrillicRenderState`.
- `src/main/java/ru/ziftech/ccrulang/mixin/client/FixedWidthFontRendererMixin.java` - redirects
  Cyrillic glyph draws into `CyrillicRenderState` instead of drawing them.
- `src/main/java/ru/ziftech/ccrulang/client/CyrillicFont.java` - terminal byte → position in
  russian_font.png.
- `src/main/java/ru/ziftech/ccrulang/client/CyrillicRenderState.java` - records Cyrillic glyphs during
  one screen's render (call-site mixin brackets it, render mixin records into it) and replays them as a
  russian_font.png quad batch once that screen has fully finished drawing.
- `src/main/resources/assets/ccrulang/textures/gui/russian_font.png` - the Cyrillic glyph strip.
- `src/main/resources/assets/computercraft/textures/gui/term_font.png` - CC: Tweaked's own font,
  byte-identical to upstream. This mod never modifies it; it's shipped here as-is.
- `src/main/templates/META-INF/neoforge.mods.toml` - mod metadata + mixin registration
  (`ccrulang.mixins.json` for the common input fix, `ccrulang-client.mixins.json` for the
  client-only rendering mixins).
