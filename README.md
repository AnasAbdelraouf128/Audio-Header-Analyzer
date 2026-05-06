# 🎧 Raw Audio Header Analyzer

A lightweight, pure Java command-line tool designed to identify and parse audio file headers (.WAV and .MP3) by reading raw hexadecimal bytes directly from the file stream. 

## 🚀 Features & Strict Rule Adherence

This project was built under strict academic constraints to demonstrate low-level byte manipulation:

* **Zero Audio Libraries:** Built entirely without `javax.sound` or any external audio parsing APIs.
* **No Ready-Made Parsers:** Completely avoids built-in endianness converters (like `ByteBuffer`). Uses custom mathematical bit-shifting for Little-Endian conversions and Syncsafe integer decoding.
* **Extension Independent:** Ignores file extensions completely. It relies strictly on reading "Magic Numbers" (Raw Hex) directly from the file. It will successfully catch spoofed files (e.g., a `.jpg` renamed to `.wav`).
* **Deep MP3 Scanning:** Manually hunts through raw bytes to find MPEG Audio Frame Sync Words (`0xFF 0xFB`) to extract exact Sample Rates natively.
* **Bulletproof Error Handling:** Gracefully manages missing files, corrupted data, and accidental Windows path quotes without crashing.

## 🛠️ How to Compile and Run

This program is entirely terminal-based. No GUI is required.

**1. Navigate to the source folder:**
```bash
cd src
2. Compile the Java file:

Bash
javac AudioHeaderAnalyzer.java

3. Run the program:

Bash
java AudioHeaderAnalyzer

4. Usage:
When prompted, paste the absolute path to your target file.
(Note: You can safely paste paths directly from Windows, even with surrounding quote marks ""). Type exit or quit to close the program safely.
=========================================
       Raw Audio Header Analyzer       
=========================================
Type 'exit' or 'quit' to close the program.

Enter the absolute path to the audio file: C:\Downloads\sample.wav

---------------------------------------------------
🔍 File Analysis Complete!
Hey there! I successfully identified this as a Canonical WAV file.
Here is the exact 44-byte structural breakdown you requested:

ChunkID        : RIFF
ChunkSize      : 352836 bytes
Format         : WAVE
Subchunk1ID    : fmt 
Subchunk1Size  : 16 bytes
AudioFormat    : 1 (1 = Uncompressed PCM)
NumChannels    : 2
SampleRate     : 44100 Hz
ByteRate       : 176400 bytes/sec
BlockAlign     : 4 bytes
BitsPerSample  : 16 bits
Subchunk2ID    : data
Subchunk2Size  : 352800 bytes
Total File Size: 352844 bytes (344.57 KB)
---------------------------------------------------
