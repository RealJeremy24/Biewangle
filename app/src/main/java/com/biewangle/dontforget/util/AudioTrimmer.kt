package com.biewangle.dontforget.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

class AudioTrimmer(private val context: Context) {

    fun trim(inputUri: Uri, outputFile: File, startMs: Long, endMs: Long): Boolean {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, inputUri, null)

            val audioTrackIndex = findAudioTrack(extractor) ?: return false
            extractor.selectTrack(audioTrackIndex)

            val trackFormat = extractor.getTrackFormat(audioTrackIndex)

            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val trackIndex = muxer.addTrack(trackFormat)
            muxer.start()

            val maxBufferSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endMs * 1000) break

                if (presentationTimeUs >= startMs * 1000) {
                    info.set(0, sampleSize, presentationTimeUs, extractor.sampleFlags)
                    muxer.writeSampleData(trackIndex, buffer, info)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            extractor.release()
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }
}
