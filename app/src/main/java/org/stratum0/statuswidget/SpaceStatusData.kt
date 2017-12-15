package org.stratum0.statuswidget


import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

enum class SpaceStatus {
    OPEN, CLOSED, ERROR, UPDATING
}

@PaperParcel
data class SpaceStatusData(
        val lastUpdate: Calendar,

        val status: SpaceStatus,
        val lastChange: Calendar?,
        val openedBy: String?,
        val since: Calendar?
) : PaperParcelable {
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

        @Suppress("unused") // used by Parcelable
        @JvmField val CREATOR = PaperParcelSpaceStatusData.CREATOR
    }
}