/**
 * ============================================================================
 * Audio Header Analyzer
 * ============================================================================
 * * README:
 * This program reads the raw hexadecimal bytes of an audio file to identify
 * its format (WAV or MP3) and manually parses its header details. It completely
 * ignores file extensions (bypassing spoofed files) and strictly avoids using
 * any built-in Java audio libraries or ready-made byte-parsing functions.
 * * HOW TO USE:
 * 1. Compile the program in your terminal: `javac AudioHeaderAnalyzer.java`
 * 2. Run the program: `java AudioHeaderAnalyzer`
 * 3. When prompted, paste the absolute path to your audio file (WAV or MP3).
 * (Note: You can safely paste paths directly from Windows, even with
 * surrounding quote marks "").
 * 4. Review the extracted structural header data printed to the console.
 * 5. Type 'exit' or 'quit' at the prompt to safely close the program.
 * ============================================================================
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class AudioHeaderAnalyzer {

    // Magic Numbers for identification
    private static final String WAV_RIFF_MARKER = "RIFF";
    private static final String WAV_WAVE_MARKER = "WAVE";
    private static final byte[] MP3_ID3_MARKER = {0x49, 0x44, 0x33}; // "ID3"

    public static void main(String[] args) {
        Scanner terminalInput = new Scanner(System.in);
        System.out.println("=========================================");
        System.out.println("       Raw Audio Header Analyzer       ");
        System.out.println("=========================================");
        System.out.println("Type 'exit' or 'quit' to close the program.\n");

        while (true) {
            System.out.print("\nEnter the absolute path to the audio file: ");
            String targetPath = terminalInput.nextLine().trim().replace("\"", "");

            if (targetPath.equalsIgnoreCase("exit") || targetPath.equalsIgnoreCase("quit")) {
                System.out.println("Exiting program... Goodbye!");
                break;
            }

            if (targetPath.isEmpty()) {
                continue;
            }

            File targetFile = new File(targetPath);

            // Rule: Handle Errors
            if (!targetFile.exists() || !targetFile.isFile()) {
                System.err.println("[-] ERROR: The specified file does not exist or is invalid.");
                continue;
            }

            if (targetFile.length() < 4) {
                System.err.println("[-] ERROR: File is too small to contain a valid header.");
                continue;
            }

            try (FileInputStream fileStream = new FileInputStream(targetFile)) {
                // Read the first 44 bytes (covers both WAV header and MP3 ID3 header)
                byte[] headerBytes = new byte[44];
                int bytesRead = fileStream.read(headerBytes);

                identifyAndParseFormat(targetFile, headerBytes, bytesRead);

            } catch (FileNotFoundException e) {
                System.err.println("[-] ERROR: File access denied. " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[-] ERROR: An I/O error occurred during raw byte reading. " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[-] ERROR: An unexpected error occurred: " + e.getMessage());
            }
        }

        terminalInput.close();
    }

    /**
     * Identifies the file type strictly using magic numbers, ignoring extensions.
     */
    private static void identifyAndParseFormat(File targetFile, byte[] header, int bytesRead) {
        String firstFourChars = new String(header, 0, Math.min(bytesRead, 4));
        String formatChars = bytesRead >= 12 ? new String(header, 8, 4) : "";

        // Check for WAV signature (RIFF + WAVE)
        if (firstFourChars.equals(WAV_RIFF_MARKER) && formatChars.equals(WAV_WAVE_MARKER)) {
            System.out.println("\n[+] Identification: Valid Canonical WAV File Detected.");
            System.out.println("---------------------------------------------------");
            parseCanonicalWavHeader(targetFile, header);
        }
        // Check for MP3 signature (Starts with "ID3")
        else if (header[0] == MP3_ID3_MARKER[0] && header[1] == MP3_ID3_MARKER[1] && header[2] == MP3_ID3_MARKER[2]) {
            System.out.println("\n[+] Identification: Valid MP3 File Detected.");
            System.out.println("---------------------------------------------------");
            parseMP3Header(targetFile, header);
        }
        // Unknown or unsupported format
        else {
            System.out.println("\n[-] Identification Failed: The file is neither a standard WAV nor an MP3.");
            System.out.println("---------------------------------------------------");
            System.out.println("First 4 bytes  : " + bytesToHex(header, 0, Math.min(bytesRead, 4)));
            long fileSize = targetFile.length();
            System.out.println("File Size      : " + fileSize + " bytes (" + formatSize(fileSize) + ")");
            System.out.println("Status         : Unsupported structure.");
        }
    }

    /**
     * Parses the 44-byte Canonical WAVE format exactly as shown in the slide.
     */
    private static void parseCanonicalWavHeader(File file, byte[] h) {
        System.out.println("Reading Canonical WAVE Header...\n");

        System.out.println("ChunkID        : " + new String(h, 0, 4));
        System.out.println("ChunkSize      : " + littleEndianToInt(h, 4) + " bytes");
        System.out.println("Format         : " + new String(h, 8, 4));
        System.out.println("Subchunk1ID    : " + new String(h, 12, 4));
        System.out.println("Subchunk1Size  : " + littleEndianToInt(h, 16) + " bytes");
        System.out.println("AudioFormat    : " + littleEndianToShort(h, 20) + " (1 = PCM)");
        System.out.println("NumChannels    : " + littleEndianToShort(h, 22));
        System.out.println("SampleRate     : " + littleEndianToInt(h, 24) + " Hz");
        System.out.println("ByteRate       : " + littleEndianToInt(h, 28) + " bytes/sec");
        System.out.println("BlockAlign     : " + littleEndianToShort(h, 32) + " bytes");
        System.out.println("BitsPerSample  : " + littleEndianToShort(h, 34) + " bits");
        System.out.println("Subchunk2ID    : " + new String(h, 36, 4));
        System.out.println("Subchunk2Size  : " + littleEndianToInt(h, 40) + " bytes");

        long fileSize = file.length();
        System.out.println("Total File Size: " + fileSize + " bytes (" + formatSize(fileSize) + ")");
    }

    /**
     * Parses the MP3 ID3v2 Tag Header and hunts for the manual Sample Rate.
     */
    private static void parseMP3Header(File file, byte[] h) {
        System.out.println("Reading MP3 ID3v2 Metadata & Frame Structure...\n");

        System.out.println("Identifier     : " + new String(h, 0, 3));
        int majorVersion = h[3] & 0xFF;
        int minorVersion = h[4] & 0xFF;
        System.out.println("ID3 Version    : v2." + majorVersion + "." + minorVersion);
        System.out.println("Flags (Hex)    : 0x" + Integer.toHexString(h[5] & 0xFF).toUpperCase());

        // Manual Syncsafe calculation
        int tagSize = decodeSyncsafeInteger(h, 6);
        System.out.println("Metadata Size  : " + tagSize + " bytes");

        long fileSize = file.length();
        System.out.println("Total File Size: " + fileSize + " bytes (" + formatSize(fileSize) + ")");

        long audioStartOffset = tagSize + 10;
        System.out.println("Audio Offset   : Byte " + audioStartOffset);

        // Manually hunt for the Audio Sample Rate using raw file access
        int sampleRate = extractMP3SampleRate(file, audioStartOffset);
        if (sampleRate > 0) {
            System.out.println("SampleRate     : " + sampleRate + " Hz");
        } else {
            System.out.println("SampleRate     : (Encrypted or unidentifiable frame format)");
        }
    }

    // --- Custom Mathematical Extractors (Prohibited to use ready functions) ---

    /**
     * Manually scans inside an MP3 file to find the first audio frame and extracts the sample rate.
     */
    private static int extractMP3SampleRate(File file, long offset) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] frameHeader = new byte[4];

            // Search the next 8000 bytes for the MPEG Sync Word (11 continuous '1' bits)
            for(int i = 0; i < 8000; i++) {
                if (raf.read(frameHeader, 0, 1) != -1) {
                    if ((frameHeader[0] & 0xFF) == 0xFF) {
                        raf.read(frameHeader, 1, 3);
                        // Check if next bits are the second half of the sync word (111)
                        if ((frameHeader[1] & 0xE0) == 0xE0) {
                            // Extract Version and Sample Rate index manually via bitwise shifting
                            int versionIndex = (frameHeader[1] >> 3) & 0x03;
                            int sampleRateIndex = (frameHeader[2] >> 2) & 0x03;

                            if (versionIndex == 3) { // MPEG Version 1
                                int[] rates = {44100, 48000, 32000, 0};
                                return rates[sampleRateIndex];
                            } else if (versionIndex == 2) { // MPEG Version 2
                                int[] rates = {22050, 24000, 16000, 0};
                                return rates[sampleRateIndex];
                            }
                        } else {
                            raf.seek(raf.getFilePointer() - 3); // Step back if false alarm
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore stream errors during deep scan
        }
        return 0; // Not found
    }

    private static int decodeSyncsafeInteger(byte[] bytes, int offset) {
        return ((bytes[offset] & 0x7F) << 21) |
                ((bytes[offset + 1] & 0x7F) << 14) |
                ((bytes[offset + 2] & 0x7F) << 7) |
                (bytes[offset + 3] & 0x7F);
    }

    private static int littleEndianToInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
                ((bytes[offset + 1] & 0xFF) << 8) |
                ((bytes[offset + 2] & 0xFF) << 16) |
                ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static short littleEndianToShort(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF) |
                ((bytes[offset + 1] & 0xFF) << 8));
    }

    private static String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(" ");
        }
        return hexString.toString().trim().toUpperCase();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " Bytes";
        else if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        else return String.format("%.2f MB", (double) bytes / (1024 * 1024));
    }
}