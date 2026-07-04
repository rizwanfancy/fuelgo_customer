package pk.fuelgo.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Simple horizontal bar chart built entirely from Modifier.fillMaxWidth(fraction) inside a
 * fixed-width "track" Box — no Canvas math, no third-party charting dependency, so it's
 * guaranteed to render correctly and always match the app's theme colors.
 */
@Composable
fun HorizontalBarChart(
    data: List<Pair<String, Int>>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxValue = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.forEach { (label, value) ->
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Text(value.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor.copy(alpha = 0.15f)),
                ) {
                    val fraction = (value.toFloat() / maxValue).coerceIn(0.02f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor),
                    )
                }
            }
        }
    }
}

/**
 * Simple vertical bar chart for a small trend series (e.g. spend per month). Each bar's
 * height is a fraction of a fixed-height slot, so sizing is always unambiguous.
 */
@Composable
fun VerticalBarChart(
    data: List<Pair<String, Double>>,
    barColor: Color,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { it.toInt().toString() },
) {
    val maxValue = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val barAreaHeight = 110.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        data.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    valueFormatter(value),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.height(barAreaHeight).width(28.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction = (value / maxValue).toFloat().coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(fraction)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
