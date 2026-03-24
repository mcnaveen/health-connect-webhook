package com.hcwebhook.app

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import org.junit.Assert.*
import org.junit.Test

class HealthConnectManagerTest {

    @Test
    fun readPermissions_containsAllExpectedTypes() {
        val readPerms = HealthConnectManager.READ_PERMISSIONS

        val expectedRecordClasses = listOf(
            StepsRecord::class,
            SleepSessionRecord::class,
            HeartRateRecord::class,
            HeartRateVariabilityRmssdRecord::class,
            DistanceRecord::class,
            ActiveCaloriesBurnedRecord::class,
            TotalCaloriesBurnedRecord::class,
            WeightRecord::class,
            HeightRecord::class,
            BloodPressureRecord::class,
            BloodGlucoseRecord::class,
            OxygenSaturationRecord::class,
            BodyTemperatureRecord::class,
            RespiratoryRateRecord::class,
            RestingHeartRateRecord::class,
            ExerciseSessionRecord::class,
            HydrationRecord::class,
            NutritionRecord::class,
            SpeedRecord::class,
            PowerRecord::class,
            BodyFatRecord::class,
            BoneMassRecord::class,
            LeanBodyMassRecord::class,
            MenstruationPeriodRecord::class,
            Vo2MaxRecord::class,
            FloorsClimbedRecord::class,
            BasalMetabolicRateRecord::class
        )

        for (recordClass in expectedRecordClasses) {
            val perm = HealthPermission.getReadPermission(recordClass)
            assertTrue(
                "READ_PERMISSIONS should contain read permission for ${recordClass.simpleName}",
                perm in readPerms
            )
        }
    }

    @Test
    fun writePermissions_containsAllExpectedTypes() {
        val writePerms = HealthConnectManager.WRITE_PERMISSIONS

        val expectedRecordClasses = listOf(
            StepsRecord::class,
            SleepSessionRecord::class,
            HeartRateRecord::class,
            HeartRateVariabilityRmssdRecord::class,
            DistanceRecord::class,
            ActiveCaloriesBurnedRecord::class,
            TotalCaloriesBurnedRecord::class,
            WeightRecord::class,
            HeightRecord::class,
            BloodPressureRecord::class,
            BloodGlucoseRecord::class,
            OxygenSaturationRecord::class,
            BodyTemperatureRecord::class,
            RespiratoryRateRecord::class,
            RestingHeartRateRecord::class,
            ExerciseSessionRecord::class,
            HydrationRecord::class,
            NutritionRecord::class,
            SpeedRecord::class,
            PowerRecord::class,
            BodyFatRecord::class,
            BoneMassRecord::class,
            LeanBodyMassRecord::class,
            MenstruationPeriodRecord::class,
            Vo2MaxRecord::class,
            FloorsClimbedRecord::class,
            BasalMetabolicRateRecord::class
        )

        for (recordClass in expectedRecordClasses) {
            val perm = HealthPermission.getWritePermission(recordClass)
            assertTrue(
                "WRITE_PERMISSIONS should contain write permission for ${recordClass.simpleName}",
                perm in writePerms
            )
        }
    }

    @Test
    fun allPermissions_equalsReadPlusWrite() {
        val all = HealthConnectManager.ALL_PERMISSIONS
        val readPlusWrite = HealthConnectManager.READ_PERMISSIONS + HealthConnectManager.WRITE_PERMISSIONS
        assertEquals("ALL_PERMISSIONS should equal READ + WRITE", readPlusWrite, all)
    }

    @Test
    fun readAndWritePermissions_haveSameRecordTypeCount() {
        // READ has 27 record-type permissions + 1 background read permission = 28
        // WRITE has 27 record-type permissions
        val readCount = HealthConnectManager.READ_PERMISSIONS.size
        val writeCount = HealthConnectManager.WRITE_PERMISSIONS.size
        assertEquals("READ_PERMISSIONS should have 28 entries (27 types + background)", 28, readCount)
        assertEquals("WRITE_PERMISSIONS should have 27 entries", 27, writeCount)
    }

    @Test
    fun readPermissions_containsBackgroundReadPermission() {
        assertTrue(
            "READ_PERMISSIONS should contain background read permission",
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND" in HealthConnectManager.READ_PERMISSIONS
        )
    }

    @Test
    fun coreWritePermissions_containsNutritionHydrationWeight() {
        val coreWrite = HealthConnectManager.CORE_WRITE_PERMISSIONS
        assertTrue(
            "CORE_WRITE should contain NutritionRecord write",
            HealthPermission.getWritePermission(NutritionRecord::class) in coreWrite
        )
        assertTrue(
            "CORE_WRITE should contain HydrationRecord write",
            HealthPermission.getWritePermission(HydrationRecord::class) in coreWrite
        )
        assertTrue(
            "CORE_WRITE should contain WeightRecord write",
            HealthPermission.getWritePermission(WeightRecord::class) in coreWrite
        )
        assertEquals("CORE_WRITE_PERMISSIONS should have exactly 3 entries", 3, coreWrite.size)
    }

    @Test
    fun healthDataType_enumHasExpectedCount() {
        assertEquals(
            "HealthDataType should have 27 entries",
            27,
            HealthDataType.entries.size
        )
    }

    @Test
    fun healthDataType_allHaveUniqueDisplayNames() {
        val displayNames = HealthDataType.entries.map { it.displayName }
        assertEquals(
            "All display names should be unique",
            displayNames.size,
            displayNames.toSet().size
        )
    }

    @Test
    fun healthDataType_allHaveUniqueRecordClasses() {
        val classes = HealthDataType.entries.map { it.recordClass }
        assertEquals(
            "All record classes should be unique",
            classes.size,
            classes.toSet().size
        )
    }
}
