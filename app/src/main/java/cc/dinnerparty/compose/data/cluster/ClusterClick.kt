package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place

sealed interface ClusterClick {
    data class Zoom(val zoom: Float) : ClusterClick
    data class Members(val places: List<Place>) : ClusterClick
}
