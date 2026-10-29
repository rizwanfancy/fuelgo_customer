package pk.fuelgo.customer.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pk.fuelgo.customer.ui.components.EmptyState
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.RefreshBox
import pk.fuelgo.customer.ui.components.RefreshOnResume
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.theme.FuelAction

@Composable
fun NotificationsScreen(viewModel: NotificationViewModel, state: NotificationUiState) {
    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LoadingState("Loading notifications…")
            }
            state.items.isEmpty() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                state.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(16.dp), onRetry = viewModel::refresh) }
                EmptyState(title = "You're all caught up", message = "Order updates will show up here.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.unreadCount} unread", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.unreadCount > 0) TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
                    }
                }
                state.errorMessage?.let { msg -> item { ErrorBanner(msg, onRetry = viewModel::refresh) } }
                items(state.items, key = { it.id }) { n ->
                    SectionCard(modifier = Modifier.clickable { if (!n.isRead) viewModel.markRead(n.id) }) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(10.dp)
                                    .background(if (n.isRead) MaterialTheme.colorScheme.outline else FuelAction, CircleShape),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(n.title, fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold)
                                Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                n.createdAt?.let {
                                    Text(it.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
