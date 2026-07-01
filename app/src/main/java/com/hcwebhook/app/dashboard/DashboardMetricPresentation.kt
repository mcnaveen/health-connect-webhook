package com.hcwebhook.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hcwebhook.app.HealthDataType
import com.hcwebhook.app.ui.theme.IconBackgroundBlue
import com.hcwebhook.app.ui.theme.IconBackgroundGreen
import com.hcwebhook.app.ui.theme.IconBackgroundOrange
import com.hcwebhook.app.ui.theme.IconBackgroundPurple
import com.hcwebhook.app.ui.theme.IconBackgroundRed
import com.hcwebhook.app.ui.theme.IconTintBlue
import com.hcwebhook.app.ui.theme.IconTintGreen
import com.hcwebhook.app.ui.theme.IconTintOrange
import com.hcwebhook.app.ui.theme.IconTintPurple
import com.hcwebhook.app.ui.theme.IconTintRed

data class DashboardMetricStyle(
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
)

object DashboardMetricPresentation {

    fun styleFor(type: HealthDataType): DashboardMetricStyle = when (type) {
        HealthDataType.STEPS -> style(Icons.AutoMirrored.Filled.DirectionsWalk, IconBackgroundOrange, IconTintOrange)
        HealthDataType.SLEEP -> style(Icons.Filled.Bedtime, IconBackgroundPurple, IconTintPurple)
        HealthDataType.HEART_RATE,
        HealthDataType.RESTING_HEART_RATE -> style(Icons.Filled.Favorite, IconBackgroundRed, IconTintRed)
        HealthDataType.HEART_RATE_VARIABILITY -> style(Icons.Filled.MonitorHeart, IconBackgroundRed, IconTintRed)
        HealthDataType.DISTANCE -> style(Icons.Filled.Navigation, IconBackgroundBlue, IconTintBlue)
        HealthDataType.ACTIVE_CALORIES,
        HealthDataType.TOTAL_CALORIES -> style(Icons.Filled.LocalFireDepartment, IconBackgroundRed, IconTintRed)
        HealthDataType.WEIGHT,
        HealthDataType.LEAN_BODY_MASS,
        HealthDataType.BONE_MASS -> style(Icons.Filled.Scale, IconBackgroundBlue, IconTintBlue)
        HealthDataType.HEIGHT -> style(Icons.Filled.Speed, IconBackgroundGreen, IconTintGreen)
        HealthDataType.BLOOD_PRESSURE -> style(Icons.Filled.MonitorHeart, IconBackgroundRed, IconTintRed)
        HealthDataType.BLOOD_GLUCOSE -> style(Icons.Filled.WaterDrop, IconBackgroundBlue, IconTintBlue)
        HealthDataType.OXYGEN_SATURATION -> style(Icons.Filled.Opacity, IconBackgroundBlue, IconTintBlue)
        HealthDataType.BODY_TEMPERATURE,
        HealthDataType.SKIN_TEMPERATURE -> style(Icons.Filled.Thermostat, IconBackgroundOrange, IconTintOrange)
        HealthDataType.RESPIRATORY_RATE -> style(Icons.Filled.Waves, IconBackgroundBlue, IconTintBlue)
        HealthDataType.EXERCISE -> style(Icons.Filled.FitnessCenter, IconBackgroundGreen, IconTintGreen)
        HealthDataType.HYDRATION -> style(Icons.Filled.WaterDrop, IconBackgroundBlue, IconTintBlue)
        HealthDataType.NUTRITION -> style(Icons.Filled.Restaurant, IconBackgroundOrange, IconTintOrange)
        HealthDataType.BASAL_METABOLIC_RATE -> style(Icons.Filled.LocalFireDepartment, IconBackgroundOrange, IconTintOrange)
        HealthDataType.BODY_FAT -> style(Icons.Filled.Spa, IconBackgroundPurple, IconTintPurple)
        HealthDataType.VO2_MAX -> style(Icons.Filled.FitnessCenter, IconBackgroundGreen, IconTintGreen)
        HealthDataType.MENSTRUATION_FLOW,
        HealthDataType.MENSTRUATION_PERIOD,
        HealthDataType.INTERMENSTRUAL_BLEEDING,
        HealthDataType.OVULATION_TEST,
        HealthDataType.CERVICAL_MUCUS,
        HealthDataType.SEXUAL_ACTIVITY -> style(Icons.Filled.Favorite, IconBackgroundRed, IconTintRed)
        HealthDataType.BASAL_BODY_TEMPERATURE -> style(Icons.Filled.Thermostat, IconBackgroundOrange, IconTintOrange)
    }

    private fun style(icon: ImageVector, background: Color, tint: Color) =
        DashboardMetricStyle(icon, background, tint)
}
