![jSona screenshot](https://i.imgur.com/B039TAj.png "jSona logo")

jSona is a configuration file(JSON), [vlcj](https://github.com/caprica/vlcj) and [JavaFx](http://www.oracle.com/technetwork/java/javafx/overview/index.html) based music and media player. The aim of jSona is to always keep your playlists in synch with your music folders. For fast fulltext search jSona uses [Apache Lucene](http://lucene.apache.org/core/). The follwing features are fully supported:

  - Supports all common media formats that VLC [supports](https://wiki.videolan.org/VLC_Features_Formats/)
  - Load artist information and images via [last.fm](http://www.lastfm.de/api) and [MusicBrainz](http://musicbrainz.org/)
  - Include your music folders
  - Create multiple playlists
  - Fulltext search

## Screenhot
![jSona screenshot](https://i.imgur.com/BnNatPC.png "Hey dude...")

## New features
You want **new features**? On the following page you can vote for and submit new feature requests.
[http://jsona.idea.informer.com](http://jsona.idea.informer.com)

## Download
jSona has a dependency to VLC3, which only can be found here: [http://nightlies.videolan.org/build/](http://nightlies.videolan.org/).
* [Current version](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.5.jar)
* [Version 1.0.5](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.5.jar)
* [Version 1.0.4](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.4.jar)
* [Version 1.0.3](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.3.zip)
* [Version 1.0.2](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.2.zip)
* [Version 1.0.1](https://dl.dropboxusercontent.com/u/3669658/github/jsona/binary/jSona-1.0.1.zip)
* [Version 1.0.0](https://dl.dropboxusercontent.com/u/3669658/github/jSona/binary/jSona-1.0.0.zip)

### Install VLC3 on Ubuntu
```sudo add-apt-repository ppa:videolan/master-daily```

```sudo apt-get update```

```sudo apt-get install vlc```

## Developpement Information
If you want to develop under a linux system you need to [recompile the OpenJDK](http://stackoverflow.com/questions/18547362/javafx-and-openjdk) beacause JavaFX is not included, or you have to install Oracles JDK where JavaFX is included. To enjoy a good JavaFX support please use [Java SDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) within your IDE.

## Configuration file / config.json

Here is an example of the default configuration file. You have to setup your **VLC path** correctly. If you use Java 32-bit/64-bit you also have to use VLC-32-bit/64-bit.
```json
{
  "ALLOW_JSONA_TO_OVERWRITE_ME": true,
  "MAX_SEARCH_RESULT_AMOUNT": 512,
  "VOLUME": 100,
  "FOLDERS": [
    "D:/media/music",
    "C:/share"
  ],
  "INCLUDE_EXTENSIONS": [
    ".mp3",
    ".wav",
    ".wma",
    ".flc",
    ".aac",
  ],
  "PLAYBACK_MODE": "NORMAL",
  "RECENTLY_ADDED_UNITL_TIME_IN_DAYS": -7,
  "THEME": "grey",
  "KEY_SKIP_TIME": 10,
  "WINDOW_OS_DECORATION": true,
  "TITLE": "jSona - open source project by Frank Roth",
  "MIN_HEIGHT": 600,
  "MIN_WIDTH": 720,
  "COLORIZE_ITEMS": true,
  "SCANNER_AND_TAGGER_LOGGING_GRANULARITY": 128
}
```

And here an explanation of all possible attributes:

##### ALLOW_JSONA_TO_OVERWRITE_ME
If you set this value to true then jsona will overwrite your config.json with the current jSona config. For example if you change the volume in the player then the **VOLUME** value gets overwritten. There only a few configuration attributes that can be change during runtime (e.g.: PLAYBACK_MODE).  

##### MAX_SEARCH_RESULT_AMOUNT
Maximum search results of the lucene engine (smaller is faster)

##### VOLUME
Default startup volume. Will be overwritten by jSona (always save the recently changed volume).

##### FOLDERS
Your music folders. Care of JSON-Syntax and correct backslashes (/). jSona also supports the usage of the Uniform Naming Convention and folders in a network (e.g.: "\\\\servername\folder\path"). See example above... 

##### INCLUDE_EXTENSIONS
This array says what kind of files you want to add to jSona. If it is empty or this field is deleted then every file with every extension will be included to your playlists.

##### PLAYBACK_MODE
Playbackmode of jSona. Choose one of them: {NORMAL, SHUFFLE}. Will be overwritten by jSona (always save the recently changed playback mode).

##### RECENTLY_ADDED_UNITL_TIME_IN_DAYS
How long do you want to show new songs in the "New" tab. If you choose -7 then new songs will be displayed for one week in the "New" tab. This number should always be negative.

##### THEME
Currently there is only one theme available: {"grey"}.

##### KEY_SKIP_TIME
If you change the duration slider with the help of the arrow keys(left or right) or hotkeys the slider rewind or skips 10 seconds.

##### WINDOW_OS_DECORATION
If you set the value on **false** the normal OS window decoration will be used. If you set the value on **true** it has no os window decoration. It will look a little bit more beautiful!

##### TITLE
Window title of jSona. Will only be displayed if the **WINDOW_UNDECORATED** property is set to **false**.

##### HEIGHT
Height of the window in pixels.

##### WIDTH
Width of the window in pixels.

##### MIN_HEIGHT
Minimum window height.

##### MIN_WIDTH
Minimum window width.

##### COLORIZE_ITEMS
If the value is set to **true** the same music items will be displayed with a different smooth background color. If the value is set to **false** then the default JavaFX list background will be used (see screenshot above).

##### FILEPATH_BASED_MUSIC_INFORMATIONS
There is the possibility to define rules to detect music information with the help of the file path. Currently there are two kind of rules {"ROOT_SUBFOLDER_LEVEL_RULE", "FILENAME_RULE"}. The "ROOT_SUBFOLDER_LEVEL_RULE" is a rule based on the subfolder level according to the root directory. With the help of the "FILENAME_RULE" you can match everything according to the filename (not file path) of the file. It is possible to ignore file endings and to replace underscores with a space.

The follwing examples should help you with to use these rules.

If you have a folder structure like this and this is your music file name:
```
C:\media\music\Rock\ACDC\Highway to Hell\03 - Walk All Over You.mp3
```

Your root folder is
```
C:\media\music
```

With the follwing rules you can match the genre (%GENRE%), the artist (%ARTIST%), the title (%TITLE%), the album (%ALBUM%) and the track number (%TRACK_NO%):
```json
{

...

"FILEPATH_BASED_MUSIC_INFORMATIONS": [
    {
      "rule": "ROOT_SUBFOLDER_LEVEL_RULE",
      "params": {
        "PATTERN": "%GENRE%",
        "REPLACE_UNDERSCORES_WITH_SPACES": false,
        "FOLDER_LEVEL": 1
      }
    },
    {
      "rule": "ROOT_SUBFOLDER_LEVEL_RULE",
      "params": {
        "PATTERN": "%ARTIST%",
        "REPLACE_UNDERSCORES_WITH_SPACES": false,
        "FOLDER_LEVEL": 2
      }
    }, 
    {
      "rule": "ROOT_SUBFOLDER_LEVEL_RULE",
      "params": {
        "PATTERN": "%ALBUM%",
        "REPLACE_UNDERSCORES_WITH_SPACES": false,
        "FOLDER_LEVEL": 3
      }
    },    
    {
      "rule": "FILENAME_RULE",
      "params": {
        "PATTERN": "%TRACK_NO% - %TITLE%",
        "IGNORE_FILE_ENDING": true,
        "REPLACE_UNDERSCORES_WITH_SPACES": true
      }
    }
  ]
}
```
Every matching **%VARIABLE%** will be trimmed at the ending, so it does not mather if you choose **%TRACK_NO% - %TITLE%** or **%TRACK_NO%-%TITLE%** as a pattern. It is also possible to ignore areas in the path by producing non declared Variables like: %TMP%, %I_DONT_NEED_THAT%, %IGNORE%... you can create anything...

##### SCANNER_AND_TAGGER_LOGGING_GRANULARITY
Logging every file in the scanner and tagging process can be very time expensive. Because of that you can define the granularity of the scanner and tagging logging. If the value is set to 1 every file is logged (time expensive). If the value is set to 128 only every 128th and the last file will be logged. This value can be every number > 0.

##### HOTKEYS
Here is a list of all modifiers and keys that can be used: https://github.com/frankred/jSona/wiki/Key-Codes.
Currently only global hotkeys work. Supported application events are: {PLAYER_VOLUME_UP, PLAYER_VOLUME_DOWN, PLAYER_PLAY_PAUSE, VIEW_HIDE_SHOW, PLAYER_NEXT, PLAYER_PREVIOUS, PLAYER_TIME_UP, PLAYER_TIME_DOWN}

```json
{

...

"HOTKEYS": [
    {
      "key": 107,
      "event": "PLAYER_VOLUME_UP",
      "global": true
    },
    {
      "key": 109,
      "event": "PLAYER_VOLUME_DOWN",
      "global": true
    },
    {
      "key": 19,
      "event": "PLAYER_PLAY_PAUSE",
      "global": true
    },
    {
      "key": 49,
      "modifiers": [
        128
      ],
      "event": "VIEW_HIDE_SHOW",
      "global": true
    }
  ],
}
```

## Changelog

### 1.0.5
* Bugfixing {ListView, Layout}
* Repeat mode added (THX [Naios](https://github.com/Naios))
* File extension whitelist mechanism added (THX [Naios](https://github.com/Naios))

### 1.0.4
* Bugfixing {ListView, Title Information changed, ListView layout, fileCreated, fileModified}
* Equalizer implemented (for testing only, unstable, unsavable)

### 1.0.3
* Loading animation for each folder added in frontend.

### 1.0.2
* HOTKEYS can now be defined in the config.json.
* VOLUME_UP_DOWN_AMOUNT amount for hotkeys can be defined in the config.json.

### 1.0.1
* Music information like artist or title can now be detected from the filepath with the help of detector rules in the config.json.
* Logging granularity of file scanner and tagger can now be defined in the config.json.

## Installation and Start
Download the current zip file and extract it. Then put in your correct VLC path into the config.json file and start jSona with the following command.
```
java -jar jSona-1.0.4.jar
```
jSona uses JavaFX so a current Java virtual machine with JavaFX support should be installed.

If you want to run jSona without getting showed the console (only works on Windows) use the following command:
```
start javaw -jar jSona-1.0.4.jar
```

## Help / FAQ
Here is a screenshot of jSona that explains the easy to use user interface.
![jSona explaining the UI](https://dl.dropboxusercontent.com/u/3669658/github/jSona/jsona_explaining_the_ui.png)

## Thank you very much!
This project is based on a set of amazing projects. Thank you to all programmers!
* [VLCJ](https://github.com/caprica/vlcj)
* [Undecorator](https://github.com/in-sideFX/Undecorator)
* [java-string-similarity](https://github.com/rrice/java-string-similarity)
* [last.fm-java](https://code.google.com/p/lastfm-java/)
* [Apache Lucene](http://lucene.apache.org/core/)
* [JavaFX](http://www.oracle.com/technetwork/java/javafx/overview/index.html)
* [Yootheme](http://www.yootheme.com/icons)

## License
MIT - **Free Software, Hell Yeah!**

    
