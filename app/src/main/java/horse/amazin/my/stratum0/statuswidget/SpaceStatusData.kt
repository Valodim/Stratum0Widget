package horse.amazin.my.stratum0.statuswidget


import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

enum class SpaceStatus {
    OPEN, CLOSED, ERROR, UPDATING
}

@Parcelize
data class SpaceStatusData(
        val lastUpdate: Calendar,

        val status: SpaceStatus,
        val lastChange: Calendar?,
        val openedBy: String?,
        val since: Calendar?
) : Parcelable {
    companion object {
        fun createOpenStatus(openedBy: String, lastChange: Calendar, since: Calendar): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.OPEN, lastChange, openedBy, since)
        }

        fun createErrorStatus(): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.ERROR, null, null, null)
        }

        fun createClosedStatus(lastChange: Calendar?): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.CLOSED, lastChange,null, null)
        }

        fun createUpdatingStatus(): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.UPDATING, null,null, null)
        }
    }
}