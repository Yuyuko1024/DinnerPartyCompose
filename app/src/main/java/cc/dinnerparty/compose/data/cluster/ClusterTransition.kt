package cc.dinnerparty.compose.data.cluster

class ClusterTransition internal constructor(val initial: Boolean = false) {
    val reuse = LinkedHashMap<Long, Long>()
    val splitFrom = LinkedHashMap<Long, Long>()
    val mergeInto = LinkedHashMap<Long, Long>()
    val splitSources = LinkedHashSet<Long>()
    val mergeTargets = LinkedHashSet<Long>()
}

fun planTransition(previous: ClusterSnapshot?, next: ClusterSnapshot): ClusterTransition {
    val plan = ClusterTransition(previous == null || previous.nodes.isEmpty())
    if (previous == null) return plan
    next.nodes.filter { it.nodeId in previous.byId }.forEach { plan.reuse[it.nodeId] = it.nodeId }
    if (previous.index === next.index) planZoom(plan, previous, next)
    else planData(plan, previous, next)
    return plan
}

private fun planZoom(plan: ClusterTransition, previous: ClusterSnapshot, next: ClusterSnapshot) {
    if (next.zoom == previous.zoom) return
    val expanding = next.zoom > previous.zoom
    val parents = if (expanding) previous.nodes else next.nodes
    val visible = if (expanding) next.byId else previous.byId
    val usedPrevious = plan.reuse.values.toMutableSet()
    val budget = NodeBudget(4000)
    for (parent in parents.filterIsInstance<MapClusterNode.Group>()) {
        if (budget.remaining <= 0) break
        if (expanding && parent.id in usedPrevious || !expanding && parent.id in plan.reuse) continue
        val targetZoom = if (expanding) next.zoom else previous.zoom
        val children = next.index?.descendantsAt(parent.id, targetZoom, budget) ?: continue
        val matched = children.filter { child ->
            child.nodeId in visible && if (expanding) child.nodeId !in plan.reuse
                else child.nodeId !in usedPrevious
        }
        if (matched.isEmpty()) continue
        if (matched.size == 1 && matched[0].pointCount == parent.count) {
            if (expanding) plan.reuse[matched[0].nodeId] = parent.id
            else plan.reuse[parent.id] = matched[0].nodeId
            usedPrevious += if (expanding) parent.id else matched[0].nodeId
        } else if (expanding) {
            plan.splitSources += parent.id
            matched.forEach { plan.splitFrom[it.nodeId] = parent.id }
        } else {
            plan.mergeTargets += parent.id
            matched.forEach { plan.mergeInto[it.nodeId] = parent.id; usedPrevious += it.nodeId }
        }
    }
}

private fun planData(plan: ClusterTransition, previous: ClusterSnapshot, next: ClusterSnapshot) {
    val usedPrevious = plan.reuse.values.toMutableSet()
    val oldNodes = previous.nodes.filter { it.nodeId !in usedPrevious }
    val newNodes = next.nodes.filter { it.nodeId !in plan.reuse }
    val oldMembers = oldNodes.associate { it.nodeId to previous.memberIds(it) }
    val newMembers = newNodes.associate { it.nodeId to next.memberIds(it) }
    val byMembers = oldNodes.filterIsInstance<MapClusterNode.Group>()
        .associateBy { oldMembers[it.id] }
    for (node in newNodes.filterIsInstance<MapClusterNode.Group>()) {
        val match = byMembers[newMembers[node.id]] ?: continue
        if (match.id in usedPrevious) continue
        plan.reuse[node.id] = match.id
        usedPrevious += match.id
    }
    val oldCount = oldNodes.filter { it.nodeId !in usedPrevious }.sumOf { it.pointCount }
    val newCount = newNodes.filter { it.nodeId !in plan.reuse }.sumOf { it.pointCount }
    if (oldCount > 6000 || newCount > 6000) return
    val memberToOld = HashMap<Long, Long>()
    oldMembers.filterKeys { it !in usedPrevious }.forEach { (id, members) ->
        members.forEach { memberToOld[it] = id }
    }
    val nextToOld = LinkedHashMap<Long, Set<Long>>()
    val oldToNext = LinkedHashMap<Long, MutableSet<Long>>()
    for ((id, members) in newMembers.filterKeys { it !in plan.reuse }) {
        val hits = members.mapNotNull { memberToOld[it] }.toSet()
        nextToOld[id] = hits
        hits.forEach { oldToNext.getOrPut(it) { LinkedHashSet() }.add(id) }
    }
    for ((id, hits) in nextToOld) {
        if (hits.isEmpty() || hits.any { oldToNext[it]?.size != 1 }) continue
        if (hits.size == 1) {
            val oldId = hits.first()
            if (oldId !in usedPrevious) { plan.reuse[id] = oldId; usedPrevious += oldId }
        } else {
            plan.mergeTargets += id
            hits.forEach { plan.mergeInto[it] = id; usedPrevious += it }
        }
    }
    for ((id, hits) in oldToNext) {
        if (id in usedPrevious || hits.size < 2) continue
        if (hits.any { nextToOld[it]?.size != 1 || it in plan.reuse }) continue
        plan.splitSources += id
        hits.forEach { plan.splitFrom[it] = id }
    }
}
