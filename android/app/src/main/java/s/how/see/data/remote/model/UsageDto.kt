package s.how.see.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsageResponse(
    @SerialName("api_count_day") val apiCountDay: Int = 0,
    @SerialName("api_count_day_limit") val apiCountDayLimit: Int = 0,
    @SerialName("api_count_month") val apiCountMonth: Int = 0,
    @SerialName("api_count_month_limit") val apiCountMonthLimit: Int = 0,
    @SerialName("link_count_day") val linkCountDay: Int = 0,
    @SerialName("link_count_day_limit") val linkCountDayLimit: Int = 0,
    @SerialName("link_count_month") val linkCountMonth: Int = 0,
    @SerialName("link_count_month_limit") val linkCountMonthLimit: Int = 0,
    @SerialName("qrcode_count_day") val qrcodeCountDay: Int = 0,
    @SerialName("qrcode_count_day_limit") val qrcodeCountDayLimit: Int = 0,
    @SerialName("qrcode_count_month") val qrcodeCountMonth: Int = 0,
    @SerialName("qrcode_count_month_limit") val qrcodeCountMonthLimit: Int = 0,
    @SerialName("text_count_day") val textCountDay: Int = 0,
    @SerialName("text_count_day_limit") val textCountDayLimit: Int = 0,
    @SerialName("text_count_month") val textCountMonth: Int = 0,
    @SerialName("text_count_month_limit") val textCountMonthLimit: Int = 0,
    @SerialName("upload_count_day") val uploadCountDay: Int = 0,
    @SerialName("upload_count_day_limit") val uploadCountDayLimit: Int = 0,
    @SerialName("upload_count_month") val uploadCountMonth: Int = 0,
    @SerialName("upload_count_month_limit") val uploadCountMonthLimit: Int = 0,
    @SerialName("file_count") val fileCount: Int = 0,
    @SerialName("storage_usage_mb") val storageUsageMb: String = "0",
    @SerialName("storage_usage_limit_mb") val storageUsageLimitMb: String = "0",
)
