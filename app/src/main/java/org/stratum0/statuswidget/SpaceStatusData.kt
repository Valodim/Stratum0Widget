package org.stratum0.statuswidget


import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

enum class SpaceStatus {
    OPEN, CLOSED, UNKNOWN
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

        fun createUnknownStatus(): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.UNKNOWN, null, null, null)
        }

        fun createClosedStatus(lastChange: Calendar?): SpaceStatusData {
            return SpaceStatusData(Calendar.getInstance(), SpaceStatus.CLOSED, lastChange,null, null)
        }

        @Suppress("unused") // used by Parcelable
        @JvmField val CREATOR = PaperParcelSpaceStatusData.CREATOR
    }
}