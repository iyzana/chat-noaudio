package de.randomerror.chatnoaudio

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder
import com.amazonaws.services.transcribe.model.*
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

val s3 = AmazonS3ClientBuilder.defaultClient()
val bucket = "chat-noaudio"
val key = "audio.mp3"

fun transcribe(audio: ByteArray): String {
    if (audio.size > 50000) {
        return "too long audio ignored"
    }

    val oggFile = Files.createTempFile("chat-noaudio", ".ogg")
    Files.delete(oggFile)
    val mp3File = Files.createTempFile("chat-noaudio", ".mp3")
    Files.delete(mp3File)
    Files.copy(ByteArrayInputStream(audio), oggFile)

    val proc = ProcessBuilder("ffmpeg", "-i", oggFile.toAbsolutePath().toString(), mp3File.toAbsolutePath().toString())
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    if (proc.exitValue() != 0) {
        throw RuntimeException("could not convert to mp3")
    }

    s3.putObject(bucket, key, Files.newInputStream(mp3File), ObjectMetadata())
    val transcriber = AmazonTranscribeClientBuilder.defaultClient()
    val jobName = "chat-noaudio-" + UUID.randomUUID().toString()
    transcriber.startTranscriptionJob(StartTranscriptionJobRequest().apply {
        transcriptionJobName = jobName
        media = Media().withMediaFileUri("https://chat-noaudio.s3.eu-central-1.amazonaws.com/audio.mp3")
        mediaFormat = "mp3"
        languageCode = "de-DE"
    })

    var result: GetTranscriptionJobResult
    while (true) {
        result = transcriber.getTranscriptionJob(GetTranscriptionJobRequest().withTranscriptionJobName(jobName))
        if (result.transcriptionJob.transcriptionJobStatus in arrayOf("COMPLETED", "FAILED")) {
            break
        }
        println("waiting for transcription")
        Thread.sleep(5000, 0)
    }

    val input = URL(result.transcriptionJob.transcript.transcriptFileUri).openStream()
    val reader = BufferedReader(InputStreamReader(input))

    val transcript = JsonParser().parse(reader).asJsonObject
        .getAsJsonObject("results")
        .getAsJsonArray("transcripts")
        .get(0)
        .asJsonObject
        .getAsJsonPrimitive("transcript")
        .asString

    transcriber.deleteTranscriptionJob(DeleteTranscriptionJobRequest().withTranscriptionJobName(jobName))
    s3.deleteObject(DeleteObjectRequest("chat-noaudio", "audio.mp3"))

    println("transcript: $transcript")
    return transcript
}
