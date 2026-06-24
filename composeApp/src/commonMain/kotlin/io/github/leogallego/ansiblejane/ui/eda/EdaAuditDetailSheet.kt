package io.github.leogallego.ansiblejane.ui.eda

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.DetailRow
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdaAuditDetailSheet(
    auditRule: EdaRuleAudit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = auditRule.displayRuleName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = stringResource(Res.string.label_status), value = auditRule.status)
            DetailRow(label = stringResource(Res.string.eda_detail_fired_at), value = DateFormatter.formatDateTime(auditRule.firedAt))

            if (auditRule.displayRuleSetName.isNotEmpty()) {
                DetailRow(label = stringResource(Res.string.eda_detail_rule_set), value = auditRule.displayRuleSetName)
            }

            auditRule.activationName?.let {
                DetailRow(label = stringResource(Res.string.eda_detail_activation), value = it)
            }

            auditRule.activationInstanceId?.let {
                DetailRow(label = stringResource(Res.string.eda_detail_activation_instance), value = it.toString())
            }

            DetailRow(label = stringResource(Res.string.label_created), value = DateFormatter.formatDateTime(auditRule.createdAt))
        }
    }
}

