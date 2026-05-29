package com.flyfishxu.usage.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.PrinterConfig
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.ui.components.GeekTextField
import com.flyfishxu.usage.ui.components.MetricLine
import com.flyfishxu.usage.ui.components.Panel
import com.flyfishxu.usage.ui.theme.GeekTheme

@Composable
fun PrinterPanel(
    config: AppConfig,
    onPrinterChange: (PrinterConfig) -> Unit,
) {
    Panel(title = "Printer") {
        GeekTextField(
            value = config.printer.host,
            onValueChange = { onPrinterChange(config.printer.copy(host = it.trim())) },
            label = { Text("IP address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        GeekTextField(
            value = config.printer.port.toString(),
            onValueChange = { value -> onPrinterChange(config.printer.copy(port = value.toIntOrNull() ?: config.printer.port)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceiptWidth.entries.forEach { width ->
                Row(
                    modifier = Modifier
                        .border(1.dp, if (config.printer.width == width) GeekTheme.accent else GeekTheme.border)
                        .clickable { onPrinterChange(config.printer.copy(width = width)) }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = config.printer.width == width,
                        onClick = { onPrinterChange(config.printer.copy(width = width)) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = GeekTheme.accent,
                            unselectedColor = GeekTheme.textMuted,
                        ),
                    )
                    Text(width.label, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace)
                }
            }
        }
        MetricLine("WIDTH", "${config.printer.width.label} / ${config.printer.width.columns} cols")
    }
}
