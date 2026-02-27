package s.how.see.data.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        val progressSink = object : ForwardingSink(sink) {
            private var totalBytesWritten = 0L

            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                totalBytesWritten += byteCount
                onProgress(totalBytesWritten, contentLength)
            }
        }
        val bufferedSink = (progressSink as Sink).buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
