package com.hcwebhook.app

/**
 * One list of [HealthData]. The payload code walks all lists through [PARTS], so a new data
 * type needs only one more line here.
 */
private class Part<T>(
    val get: (HealthData) -> List<T>,
    val set: (HealthData, List<T>) -> HealthData,
    /** Health Connect record id of an item; null for a list without records. */
    val idOf: ((T) -> String?)?,
) {
    fun size(data: HealthData): Int = get(data).size

    fun clear(data: HealthData): HealthData = set(data, emptyList())

    /** [into] with items [from] until [to] of this list from [source]. */
    fun copyRange(source: HealthData, into: HealthData, from: Int, to: Int): HealthData =
        set(into, get(source).subList(from, to))

    fun ids(data: HealthData): List<String> = idOf?.let { id -> get(data).mapNotNull(id) }.orEmpty()
}

private val PARTS: List<Part<*>> = listOf(
    Part({ it.steps }, { d, l -> d.copy(steps = l) }, { it.metadata?.id }),
    Part({ it.sleep }, { d, l -> d.copy(sleep = l) }, { it.metadata?.id }),
    Part({ it.heartRate }, { d, l -> d.copy(heartRate = l) }, { it.metadata?.id }),
    Part({ it.heartRateVariability }, { d, l -> d.copy(heartRateVariability = l) }, { it.metadata?.id }),
    Part({ it.distance }, { d, l -> d.copy(distance = l) }, { it.metadata?.id }),
    Part({ it.activeCalories }, { d, l -> d.copy(activeCalories = l) }, { it.metadata?.id }),
    Part({ it.totalCalories }, { d, l -> d.copy(totalCalories = l) }, { it.metadata?.id }),
    Part({ it.weight }, { d, l -> d.copy(weight = l) }, { it.metadata?.id }),
    Part({ it.height }, { d, l -> d.copy(height = l) }, { it.metadata?.id }),
    Part({ it.bloodPressure }, { d, l -> d.copy(bloodPressure = l) }, { it.metadata?.id }),
    Part({ it.bloodGlucose }, { d, l -> d.copy(bloodGlucose = l) }, { it.metadata?.id }),
    Part({ it.oxygenSaturation }, { d, l -> d.copy(oxygenSaturation = l) }, { it.metadata?.id }),
    Part({ it.bodyTemperature }, { d, l -> d.copy(bodyTemperature = l) }, { it.metadata?.id }),
    Part({ it.skinTemperature }, { d, l -> d.copy(skinTemperature = l) }, { it.metadata?.id }),
    Part({ it.respiratoryRate }, { d, l -> d.copy(respiratoryRate = l) }, { it.metadata?.id }),
    Part({ it.restingHeartRate }, { d, l -> d.copy(restingHeartRate = l) }, { it.metadata?.id }),
    Part({ it.exercise }, { d, l -> d.copy(exercise = l) }, { it.metadata?.id }),
    Part({ it.hydration }, { d, l -> d.copy(hydration = l) }, { it.metadata?.id }),
    Part({ it.nutrition }, { d, l -> d.copy(nutrition = l) }, { it.metadata?.id }),
    Part({ it.basalMetabolicRate }, { d, l -> d.copy(basalMetabolicRate = l) }, { it.metadata?.id }),
    Part({ it.bodyFat }, { d, l -> d.copy(bodyFat = l) }, { it.metadata?.id }),
    Part({ it.leanBodyMass }, { d, l -> d.copy(leanBodyMass = l) }, { it.metadata?.id }),
    Part({ it.bodyWaterMass }, { d, l -> d.copy(bodyWaterMass = l) }, { it.metadata?.id }),
    Part({ it.vo2Max }, { d, l -> d.copy(vo2Max = l) }, { it.metadata?.id }),
    Part({ it.boneMass }, { d, l -> d.copy(boneMass = l) }, { it.metadata?.id }),
    Part({ it.menstruationFlow }, { d, l -> d.copy(menstruationFlow = l) }, { it.metadata?.id }),
    Part({ it.menstruationPeriod }, { d, l -> d.copy(menstruationPeriod = l) }, { it.metadata?.id }),
    Part({ it.intermenstrualBleeding }, { d, l -> d.copy(intermenstrualBleeding = l) }, { it.metadata?.id }),
    Part({ it.ovulationTest }, { d, l -> d.copy(ovulationTest = l) }, { it.metadata?.id }),
    Part({ it.cervicalMucus }, { d, l -> d.copy(cervicalMucus = l) }, { it.metadata?.id }),
    Part({ it.sexualActivity }, { d, l -> d.copy(sexualActivity = l) }, { it.metadata?.id }),
    Part({ it.basalBodyTemperature }, { d, l -> d.copy(basalBodyTemperature = l) }, { it.metadata?.id }),
    Part({ it.mindfulness }, { d, l -> d.copy(mindfulness = l) }, { it.metadata?.id }),
    Part({ it.deleted }, { d, l -> d.copy(deleted = l) }, null),
)

/** Number of items in all lists, deletions included. */
internal fun HealthData.itemCount(): Int = PARTS.sumOf { it.size(this) }

/**
 * Splits this data into payloads of at most [maxItems] items (records and deletions), in list
 * order. Data that fits stays one payload, so only syncs that are too large for one request
 * change. A receiver upserts by id or time, so each part is a valid payload of its own.
 */
internal fun HealthData.chunked(maxItems: Int): List<HealthData> {
    require(maxItems > 0) { "maxItems must be positive" }
    if (itemCount() <= maxItems) return listOf(this)
    val empty = PARTS.fold(this) { data, part -> part.clear(data) }
    val chunks = mutableListOf<HealthData>()
    var chunk = empty
    var room = maxItems
    for (part in PARTS) {
        val size = part.size(this)
        var from = 0
        while (from < size) {
            val to = minOf(size, from + room)
            chunk = part.copyRange(this, chunk, from, to)
            room -= to - from
            from = to
            if (room == 0) {
                chunks += chunk
                chunk = empty
                room = maxItems
            }
        }
    }
    if (room < maxItems) chunks += chunk
    return chunks
}

/**
 * This data without the deletions of records that it also holds. A source app that deletes a
 * record and writes it again with the same client record id gets the same record id back, so
 * the record exists after all and a receiver must keep it.
 */
internal fun HealthData.withoutDeletionsOfPresentRecords(): HealthData {
    if (deleted.isEmpty()) return this
    val present = PARTS.flatMapTo(HashSet()) { it.ids(this) }
    return copy(deleted = deleted.filterNot { it.id in present })
}
