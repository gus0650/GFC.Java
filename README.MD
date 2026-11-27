GFC.Java MIDP Wrapper
=====================
(c) 2006-2025<br>
written by Gustaf Stechmann and Alex Knorr <br>
placed in the public domain.<br>

You have a game written against the Java 2 Micro Edition / MIDP (J2ME)?

With this wrapper library, you can make your J2ME game run on regular desktop Java SE!

Simply change your import statements from<br>
javax.microedition.* <br>
to<br>
gfc.microedition.* <br>
and you're ready to recompile.

You then need to configure the midlet.properties file with the specific parameters for your MIDlet,
and now you are ready to run.

The wrapper re-implements most classes found in MIDP.

The Font class uses bitmap fonts. The default font is configured in midlet.properties (font.image, font.width, ...).

Images and sounds are loaded from the classpath like on the handset ("/res/img.png"), so they also work from inside a .jar.
If a file is not found in the classpath, it is loaded from the working directory.

Sound: WAV, AU and AIFF files play via javax.sound.sampled, ToneControl sequences and Manager.playTone() are synthesized. MIDI files play via javax.sound.midi.

Network: Connector.open() supports http://, https:// and socket://host:port. MIDlet.platformRequest() opens the desktop browser.

Fullscreen mode is supported.

Keys are mapped to a Nokia handset:

| Keyboard                  | Handset            | key code |
|---------------------------|--------------------|----------|
| cursor keys               | cursor keys        | -1..-4   |
| Return, Space             | fire / select      | -5       |
| F1, F2                    | left / right soft key | -6, -7 |
| Backspace                 | clear              | -8       |
| 0..9, numpad 0..9, * #    | number keys        | '0'..'9', '*', '#' |

Alt-F4 closes the window.

This code was written in Visual Studio Code with the Oracle Java extension.
Load "TestMIDlet" and it should run out of the box. (F5)

Original MIDP documentation:
https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/index.html

This software is 95% complete -- see TODO file for what is missing.
